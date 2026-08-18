package com.example.schedulebook.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonMessageUtils {
    private JsonMessageUtils() {
    }

    public static String extractEventId(ObjectMapper objectMapper, String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            JsonNode eventIdNode = root.get("eventId");

            if (eventIdNode == null || eventIdNode.isNull()) {
                return null;
            }

            return eventIdNode.asText();

        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
