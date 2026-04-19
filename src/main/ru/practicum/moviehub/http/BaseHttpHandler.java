package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected final Gson gson = new GsonBuilder().create();

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status, bytes.length);

        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    protected void sendNoContent(HttpExchange ex) throws java.io.IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(204, -1);
    }

    protected void sendError(HttpExchange ex, int status, String error, List<String> details) throws IOException {
        ErrorResponse err = new ErrorResponse(error, details);
        String json = gson.toJson(err);
        sendJson(ex, status, json);
    }
}