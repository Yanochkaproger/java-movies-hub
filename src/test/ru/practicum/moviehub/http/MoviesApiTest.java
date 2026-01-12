package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    private static MoviesServer server;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private static final Gson GSON = new Gson();
    private static MoviesStore store;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());
        assertEquals("application/json; charset=UTF-8",
                response.headers().firstValue("Content-Type").orElse(""));
        String body = response.body().trim();
        assertTrue(body.equals("[]") || body.equals("[ ]") || body.equals("[\n]") || body.equals("[\r\n]"));
    }

    @Test
    void getMoviesById_invalidId_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, response.statusCode());
        ErrorResponse error = GSON.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Некорректный ID", error.getError());
    }

    // Другие тесты можно добавить по необходимости
}