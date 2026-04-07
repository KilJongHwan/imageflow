package com.imageflow.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonTestUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonTestUtils() {
    }

    public static String read(String json, String fieldName) throws Exception {
        JsonNode node = OBJECT_MAPPER.readTree(json);
        JsonNode current = node;

        for (String segment : fieldName.split("\\.")) {
            if (segment.contains("[") && segment.endsWith("]")) {
                String property = segment.substring(0, segment.indexOf('['));
                int index = Integer.parseInt(segment.substring(segment.indexOf('[') + 1, segment.length() - 1));
                current = current.get(property).get(index);
            } else {
                current = current.get(segment);
            }
        }

        return current.asText();
    }
}
