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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    private static MoviesServer server;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private static final Gson GSON = new Gson();
    private static final java.lang.reflect.Type LIST_OF_MOVIES = new ListOfMoviesTypeToken().getType();
    private static MoviesStore store;

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080" + path))
                .GET()
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> post(String path, String body, boolean withJsonContentType) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080" + path))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (withJsonContentType) {
            builder.header("Content-Type", "application/json");
        }
        HttpRequest request = builder.build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> delete(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080" + path))
                .DELETE()
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private long createMovieAndGetId(String title, int year) throws Exception {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setYear(year);
        String json = GSON.toJson(movie);
        HttpResponse<String> resp = post("/movies", json, true);
        assertEquals(201, resp.statusCode());
        Movie m = GSON.fromJson(resp.body(), Movie.class);
        return m.getId();
    }

    private void assertStartLine(HttpResponse<String> resp, int expectedStatus) {
        assertEquals(expectedStatus, resp.statusCode());
    }

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
        HttpResponse<String> resp = get("/movies");
        assertStartLine(resp, 200);
        List<Movie> movies = GSON.fromJson(resp.body().trim(), LIST_OF_MOVIES);
        assertTrue(movies.isEmpty(), "Ожидается пустой список");
    }

    @Test
    void postMovie_whenValid_returns201AndObject() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("FilmA");
        movie.setYear(2010);
        String json = GSON.toJson(movie);
        HttpResponse<String> resp = post("/movies", json, true);
        assertStartLine(resp, 201);
        Movie created = GSON.fromJson(resp.body(), Movie.class);
        assertNotNull(created.getId());
        assertEquals("FilmA", created.getTitle());
        assertEquals(2010, created.getYear());
    }

    @Test
    void postMovie_whenEmptyTitle_returns422() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("");
        movie.setYear(2010);
        String json = GSON.toJson(movie);
        HttpResponse<String> resp = post("/movies", json, true);
        assertStartLine(resp, 422);
        ErrorResponse err = GSON.fromJson(resp.body(), ErrorResponse.class);
        assertNotNull(err.getError());
        assertTrue(err.getDetails().stream().anyMatch(d -> d.toLowerCase().contains("название")));
    }

    @Test
    void postMovie_whenTooLongTitle_returns422() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("A".repeat(101));
        movie.setYear(2010);
        String json = GSON.toJson(movie);
        HttpResponse<String> resp = post("/movies", json, true);
        assertStartLine(resp, 422);
        ErrorResponse err = GSON.fromJson(resp.body(), ErrorResponse.class);
        assertTrue(err.getDetails().stream().anyMatch(d -> d.toLowerCase().contains("название")));
    }

    @Test
    void postMovie_whenYearTooSmall_returns422() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("Old");
        movie.setYear(1800);
        String json = GSON.toJson(movie);
        HttpResponse<String> resp = post("/movies", json, true);
        assertStartLine(resp, 422);
        ErrorResponse err = GSON.fromJson(resp.body(), ErrorResponse.class);
        assertTrue(err.getDetails().stream().anyMatch(d -> d.toLowerCase().contains("год")));
    }

    @Test
    void postMovie_whenYearTooBig_returns422() throws Exception {
        int farFuture = java.time.LocalDate.now().getYear() + 10;
        Movie movie = new Movie();
        movie.setTitle("Future");
        movie.setYear(farFuture);
        String json = GSON.toJson(movie);
        HttpResponse<String> resp = post("/movies", json, true);
        assertStartLine(resp, 422);
        ErrorResponse err = GSON.fromJson(resp.body(), ErrorResponse.class);
        assertTrue(err.getDetails().stream().anyMatch(d -> d.toLowerCase().contains("год")));
    }

    @Test
    void postMovie_whenMissingContentType_returns415() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("NoCT");
        movie.setYear(2020);
        String json = GSON.toJson(movie);
        HttpResponse<String> resp = post("/movies", json, false);
        assertStartLine(resp, 415);
    }

    @Test
    void postMovie_whenInvalidJson_returns422() throws Exception {
        HttpResponse<String> resp = post("/movies", "{\"title\":\"Bad\",\"year\":2020", true);
        assertStartLine(resp, 422);
    }

    @Test
    void getMovies_whenNotEmpty_returnsList() throws Exception {
        createMovieAndGetId("One", 2010);
        createMovieAndGetId("Two", 2014);
        HttpResponse<String> resp = get("/movies");
        assertStartLine(resp, 200);
        List<Movie> movies = GSON.fromJson(resp.body(), LIST_OF_MOVIES);
        assertEquals(2, movies.size());
    }

    @Test
    void getMovies_filterByYear_returnsOnlyThatYear() throws Exception {
        createMovieAndGetId("A2010", 2010);
        createMovieAndGetId("B2014", 2014);
        createMovieAndGetId("C2010", 2010);
        HttpResponse<String> resp = get("/movies?year=2010");
        assertStartLine(resp, 200);
        List<Movie> movies = GSON.fromJson(resp.body(), LIST_OF_MOVIES);
        assertEquals(2, movies.size());
        assertTrue(movies.stream().allMatch(m -> m.getYear() == 2010));
        assertTrue(movies.stream().anyMatch(m -> "A2010".equals(m.getTitle())));
        assertTrue(movies.stream().anyMatch(m -> "C2010".equals(m.getTitle())));
    }

    @Test
    void getMovies_filterByYear_whenNoMatches_returnsEmpty() throws Exception {
        createMovieAndGetId("Only2014", 2014);
        HttpResponse<String> resp = get("/movies?year=1999");
        assertStartLine(resp, 200);
        List<Movie> movies = GSON.fromJson(resp.body(), LIST_OF_MOVIES);
        assertTrue(movies.isEmpty());
    }

    @Test
    void getMovies_filterByYear_invalidParam_returns400() throws Exception {
        HttpResponse<String> resp = get("/movies?year=twenty");
        assertStartLine(resp, 400);
    }

    @Test
    void getMovieById_whenExists_returns200() throws Exception {
        long id = createMovieAndGetId("FindMe", 2015);
        HttpResponse<String> resp = get("/movies/" + id);
        assertStartLine(resp, 200);
        Movie m = GSON.fromJson(resp.body(), Movie.class);
        assertEquals(id, m.getId());
        assertEquals("FindMe", m.getTitle());
    }

    @Test
    void getMovieById_whenNotFound_returns404() throws Exception {
        HttpResponse<String> resp = get("/movies/99999");
        assertStartLine(resp, 404);
    }

    @Test
    void getMovieById_whenInvalidId_returns400() throws Exception {
        HttpResponse<String> resp = get("/movies/abc");
        assertStartLine(resp, 400);
    }

    @Test
    void deleteMovie_whenExists_returns204_then404OnGet() throws Exception {
        long id = createMovieAndGetId("DeleteMe", 2011);
        HttpResponse<String> del = delete("/movies/" + id);
        assertEquals(204, del.statusCode());
        HttpResponse<String> getAfter = get("/movies/" + id);
        assertEquals(404, getAfter.statusCode());
    }

    @Test
    void deleteMovie_whenNotFound_returns404() throws Exception {
        HttpResponse<String> del = delete("/movies/123456");
        assertEquals(404, del.statusCode());
    }

    @Test
    void deleteMovie_whenInvalidId_returns400() throws Exception {
        HttpResponse<String> del = delete("/movies/NaN");
        assertStartLine(del, 400);
    }
}