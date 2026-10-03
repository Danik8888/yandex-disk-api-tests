package ru.yadisk.qa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record ApiResponse(int statusCode, String body, JsonNode json) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ApiResponse(int statusCode, String body) {
        this(statusCode, body, parseJson(body));
    }

    private static JsonNode parseJson(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(body);
        } catch (Exception e) {
            return null;
        }
    }
}