package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.model.MovieSpec;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

class MoviesHandler extends BaseHttpHandler {
    private final Gson gson = new GsonBuilder().create();
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");

        if (method.equalsIgnoreCase("GET")) {

            String path = ex.getRequestURI().getPath();

            if (path.startsWith("/movies/")) {
                String idStr = path.substring("/movies/".length());
                try {
                    int id = Integer.parseInt(idStr);
                    Movie movie = store.getById(id);
                    if (movie != null) {
                        sendJson(ex, 200, gson.toJson(movie));
                    } else {
                        sendError(ex, 404, "Фильм не найден", List.of("Фильм с id " + id + " не существует"));
                    }
                } catch (NumberFormatException e) {
                    sendError(ex, 400, "Некорректный ID", List.of("ID должен быть числом"));
                }
                ex.close();
                return;
            }

            String query = ex.getRequestURI().getQuery();

            if (query != null && query.startsWith("year=")) {
                String year = query.substring(query.indexOf("=") + 1);
                try {
                    int yearInt = Integer.parseInt(year);
                    List<Movie> filteredMovies = store.getByYear(yearInt);
                    String json = gson.toJson(filteredMovies);
                    sendJson(ex, 200, json);
                } catch (NumberFormatException e) {
                    sendError(ex, 400, "Некорректный параметр запроса",
                            List.of("Параметр 'year' должен быть числом"));
                }
                ex.close();
                return;
            }

            List<Movie> movies = store.getAll();
            String json = gson.toJson(movies);
            sendJson(ex, 200, json);
            ex.close();
            return;

        } else if (method.equalsIgnoreCase("POST")) {

            if (contentType == null || !contentType.startsWith("application/json")) {
                sendError(ex, 415, "Неподдерживаемый Content-Type", List.of(
                        "Ожидается application/json"));
                ex.close();
                return;
            }

            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            Movie movie;
            try {
                movie = gson.fromJson(body, Movie.class);
            } catch (Exception e) {
                sendError(ex, 400, "Некорректный JSON", List.of(
                        "Тело запроса должно быть валидным Json"));
                ex.close();
                return;
            }

            if (movie.getTitle() == null || movie.getTitle().isBlank()) {
                sendError(ex, 422, "Ошибка валидации", List.of("название не должно быть пустым"));
                ex.close();
                return;
            }

            if (movie.getTitle().length() > MovieSpec.MAX_TITLE_LENGTH) {
                sendError(ex, 422, "Ошибка валидации", List.of("Название фильма не может быть больше " + (MovieSpec.MAX_TITLE_LENGTH) + ")"));
                ex.close();
                return;
            }

            if (movie.getYear() == null) {
                sendError(ex, 422, "Ошибка валидации", List.of("Год фильма должен быть указан"));
                ex.close();
                return;
            }


            if (movie.getYear() < MovieSpec.MIN_YEAR) {
                sendError(ex, 422, "Ошибка валидации", List.of(
                        "Год должен быть не меньше " + MovieSpec.MIN_YEAR));
                ex.close();
                return;
            }

            if (movie.getYear() > MovieSpec.getMaxYear()) {
                sendError(ex, 422, "Ошибка валидации", List.of(
                        "Год должен быть не больше " + MovieSpec.getMaxYear()));
                ex.close();
                return;
            }

            Movie savedMovie = store.add(movie);
            String response = gson.toJson(savedMovie);
            sendJson(ex, 201, response);
            ex.close();
            return;

        } else if (method.equalsIgnoreCase("DELETE")) {
            String path = ex.getRequestURI().getPath();

            if (path.startsWith("/movies/")) {
                String idStr = path.substring("/movies/".length());
                try {
                    int id = Integer.parseInt(idStr);
                    boolean deleted = store.delete(id);

                    if (deleted) {
                        sendNoContent(ex);
                    } else {
                        sendError(ex, 404, "Фильм не найден", List.of("Фильм с id " + id + " не существует"));
                    }
                } catch (NumberFormatException e) {
                    sendError(ex, 400, "Некорректный ID", List.of("ID должен быть числом"));
                }
                ex.close();
                return;
            }

            sendError(ex, 400, "Некорректный запрос", List.of("Неверный формат пути"));
            ex.close();
            return;

        } else {
            ex.sendResponseHeaders(405, -1);
            ex.close();
        }
        ex.close();
    }
}