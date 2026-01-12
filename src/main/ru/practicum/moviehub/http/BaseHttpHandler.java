package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final Gson GSON = new Gson();

    protected void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendErrorResponse(HttpExchange exchange, int statusCode, String error) throws IOException {
        sendErrorResponse(exchange, statusCode, error, null);
    }

    protected void sendErrorResponse(HttpExchange exchange, int statusCode, String error, java.util.List<String> details) throws IOException {
        ErrorResponse response = details == null ? new ErrorResponse(error) : new ErrorResponse(error, details);
        String json = GSON.toJson(response);
        sendJsonResponse(exchange, statusCode, json);
    }

    protected Long parseId(String idStr, HttpExchange exchange) throws IOException {
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Некорректный ID");
            return null;
        }
    }
}