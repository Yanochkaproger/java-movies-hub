package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private static final int CURRENT_YEAR = java.time.Year.now().getValue();

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        if ("/movies".equals(path)) {
            if ("GET".equals(method)) {
                handleGetMovies(exchange, query);
            } else if ("POST".equals(method)) {
                handlePostMovie(exchange);
            } else {
                sendErrorResponse(exchange, 405, "Метод не поддерживается");
            }
        } else if (path.startsWith("/movies/")) {
            // Обрабатываем ЛЮБОЙ путь после /movies/
            String idStr = path.substring("/movies/".length());

            if ("GET".equals(method)) {
                handleGetMovieById(exchange, idStr);
            } else if ("DELETE".equals(method)) {
                handleDeleteMovie(exchange, idStr);
            } else {
                sendErrorResponse(exchange, 405, "Метод не поддерживается");
            }
        } else {
            sendErrorResponse(exchange, 404, "Эндпоинт не найден");
        }
    }

    private void handleGetMovies(HttpExchange exchange, String query) throws IOException {
        List<Movie> movies;
        if (query != null && query.startsWith("year=")) {
            String yearParam = query.substring("year=".length());
            try {
                int year = Integer.parseInt(yearParam);
                if (year < 1888 || year > CURRENT_YEAR + 1) {
                    sendErrorResponse(exchange, 400, "Некорректный параметр запроса — 'year'");
                    return;
                }
                movies = store.findByYear(year);
            } catch (NumberFormatException e) {
                sendErrorResponse(exchange, 400, "Некорректный параметр запроса — 'year'");
                return;
            }
        } else if (query != null && !query.isEmpty()) {
            sendErrorResponse(exchange, 400, "Некорректный параметр запроса");
            return;
        } else {
            movies = store.findAll();
        }

        String json = GSON.toJson(movies);
        sendJsonResponse(exchange, 200, json);
    }

    private void handlePostMovie(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
            sendErrorResponse(exchange, 415, "Неподдерживаемый тип контента");
            return;
        }

        Movie movie;
        try (Reader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            movie = GSON.fromJson(reader, Movie.class);
        } catch (Exception e) {
            sendErrorResponse(exchange, 422, "Ошибка валидации", Arrays.asList("некорректный JSON"));
            return;
        }

        List<String> errors = new ArrayList<>();

        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            errors.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > 100) {
            errors.add("название не должно превышать 100 символов");
        }

        if (movie.getYear() < 1888 || movie.getYear() > CURRENT_YEAR + 1) {
            errors.add("год должен быть между 1888 и " + (CURRENT_YEAR + 1));
        }

        if (!errors.isEmpty()) {
            sendErrorResponse(exchange, 422, "Ошибка валидации", errors);
            return;
        }

        Movie saved = store.save(movie);
        String json = GSON.toJson(saved);
        sendJsonResponse(exchange, 201, json);
    }

    private void handleGetMovieById(HttpExchange exchange, String idStr) throws IOException {
        Long id = parseId(idStr, exchange);
        if (id == null) {
            return; // Ошибка 400 уже отправлена
        }

        Movie movie = store.findById(id);
        if (movie == null) {
            sendErrorResponse(exchange, 404, "Фильм не найден");
            return;
        }

        String json = GSON.toJson(movie);
        sendJsonResponse(exchange, 200, json);
    }

    private void handleDeleteMovie(HttpExchange exchange, String idStr) throws IOException {
        Long id = parseId(idStr, exchange);
        if (id == null) {
            return; // Ошибка 400 уже отправлена
        }

        boolean deleted = store.deleteById(id);
        if (!deleted) {
            sendErrorResponse(exchange, 404, "Фильм не найден");
            return;
        }

        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }
}