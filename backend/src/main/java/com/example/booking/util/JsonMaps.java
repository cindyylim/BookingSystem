package com.example.booking.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonMaps {
    private JsonMaps() {
    }

    public static Map<String, String> ofNullable(String... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("Expected alternating keys and values");
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            String value = keysAndValues[i + 1];
            map.put(keysAndValues[i], value != null ? value : "");
        }
        return map;
    }
}
