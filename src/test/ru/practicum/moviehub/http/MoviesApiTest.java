package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.model.MovieSpec;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesStore store;
    private static MoviesServer server;
    private static HttpClient client;
    private static Gson gson;

    @BeforeAll
    static void beforeAll() {
        if (server == null) {
            store = new MoviesStore();
            server = new MoviesServer(store);
            server.start();
        }
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        gson = new GsonBuilder().create();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @Test
    public void getMovies_whenEmpty_returnsEmptyArray() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = response.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    public void postMovies_whenValid_returns201AndMovie() throws Exception {
        Movie movieExample = new Movie("Пираты Карибского Моря", 2003);
        String json = gson.toJson(movieExample);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, response.statusCode(), "POST /movies должен вернуть 201");

        Movie movie = gson.fromJson(response.body(), Movie.class);
        assertEquals(movieExample.getTitle(), movie.getTitle());
        assertEquals(movieExample.getYear(), movie.getYear());
        assertNotNull(movie.getId());
    }

    @Test
    public void postMovies_whenEmptyTitle_returns422() throws Exception {
        Movie movieExample = new Movie("", 2000);
        String json = gson.toJson(movieExample);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, response.statusCode(),
                "POST /movies должен вернуть 422 (Название фильма отсутствует)");
    }

    @Test
    public void postMovies_whenTooLongTitle_returns422() throws Exception {
        Movie movieExample = new Movie("Т".repeat(MovieSpec.MAX_TITLE_LENGTH + 1), 2000);
        String json = gson.toJson(movieExample);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, response.statusCode(),
                "POST /movies должен вернуть 422 (Название фильма больше " + (MovieSpec.MAX_TITLE_LENGTH) + ")");
    }

    @Test
    public void postMovies_whenYearLessThan1888_returns422() throws Exception {
        Movie movieExample = new Movie("Древний", MovieSpec.MIN_YEAR - 1);
        String json = gson.toJson(movieExample);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, response.statusCode(),
                "POST /movies должен вернуть 422 (Год фильма меньше " + (MovieSpec.MIN_YEAR) + ")");
    }

    @Test
    public void postMovies_whenYearMoreThanActualPlusOne_returns422() throws Exception {
        Movie movieExample = new Movie("Слишком новый", MovieSpec.getMaxYear() + 1);
        String json = gson.toJson(movieExample);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, response.statusCode(),
                "POST /movies должен вернуть 422 (Год фильма больше " + (MovieSpec.getMaxYear()) + ")");
    }

    @Test
    public void postMovies_whenWrongContentType_returns415() throws Exception {
        Movie movieExample = new Movie("Титаник", 1998);
        String json = gson.toJson(movieExample);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "text/plain")
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, response.statusCode(),
                "POST /movies с Content-Type: text/plain должен вернуть 415");
    }

    @Test
    public void getMovies_whenHasMovies_returnsMoviesArray() throws Exception {
        store.add(new Movie("Бойцовский клуб", 1999));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode(), "GET /movies должен вернуть 200");
        Movie[] movies = gson.fromJson(response.body(), Movie[].class);
        assertEquals(1, movies.length, "Должен быть ровно один фильм в списке");
    }

    @Test
    public void getMovieById_whenMovieExists_returnsMovie() throws Exception {
        Movie addedMovie = store.add(new Movie("Бойцовский клуб", 1999));
        int movieId = addedMovie.getId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movieId))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode(), "GET /movies/" + movieId + " должен вернуть 200");
    }

    @Test
    public void getMovieById_whenMovieDoesNotExist_returns404() throws Exception {
        int noExistId = 9999;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + noExistId))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, response.statusCode(),
                "GET /movies/" + noExistId + " должен вернуть 404 (фильм не найден)");
    }

    @Test
    public void getMovieById_whenIdNotNumber_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, response.statusCode(), "GET /movies abc должен вернуть 400 (abc - не число)");
    }

    @Test
    public void deleteMovieById_whenMovieExists_returns204() throws Exception {
        Movie addedMovie = store.add(new Movie("Бойцовский клуб", 1999));
        int movieId = addedMovie.getId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movieId))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, response.statusCode(),
                "DELETE /movies/" + movieId + " должен вернуть 204");

        assertNull(store.getById(movieId), "Фильм должен быть удалён");
    }

    @Test
    public void deleteMovieById_whenMovieNotFound_returns404() throws Exception {
        int noExistId = 9999;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + noExistId))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, response.statusCode(),
                "DELETE /movies/" + noExistId + " должен вернуть 404");
    }

    @Test
    public void deleteMovieById_whenIdNotNumber_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, response.statusCode(),
                "DELETE /movies/abc должен вернуть 400");
    }

    @Test
    public void getMoviesByYear_whenMoviesExist_returnsFilteredMovies() throws Exception {
        store.add(new Movie("Бойцовский клуб", 1999));
        store.add(new Movie("Пираты Карибского Моря", 2003));
        store.add(new Movie("Очень Страшное Кино 3", 2003));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2003"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());

        Movie[] movies = gson.fromJson(response.body(), Movie[].class);
        assertEquals(2, movies.length);
        for (Movie movie : movies) {
            assertEquals(2003, movie.getYear());
        }
    }

    @Test
    public void getMoviesByYear_whenNoMovies_returnsEmptyArray() throws Exception {
        store.add(new Movie("Бойцовский клуб", 1999));
        store.add(new Movie("Пираты Карибского Моря", 2003));
        store.add(new Movie("Очень Страшное Кино 3", 2003));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2000"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());

        Movie[] movies = gson.fromJson(response.body(), Movie[].class);
        assertEquals(0, movies.length, "Должен быть пустой массив");
    }

    @Test
    public void getMoviesByYear_whenYearNotNumber_returns400() throws Exception {
        store.add(new Movie("Бойцовский клуб", 1999));
        store.add(new Movie("Пираты Карибского Моря", 2003));
        store.add(new Movie("Очень Страшное Кино 3", 2003));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, response.statusCode(),
                "GET /movies?year=abc должен вернуть 400 (abc не число)");
    }

    @Test
    public void whenUnsupportedMethod_returns405() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .method("PUT", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(405, response.statusCode());
    }
}