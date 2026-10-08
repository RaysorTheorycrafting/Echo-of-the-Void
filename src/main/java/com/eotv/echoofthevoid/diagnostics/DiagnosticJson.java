package com.eotv.echoofthevoid.diagnostics;

import java.util.Collection;
import java.util.Map;

final class DiagnosticJson {
    private DiagnosticJson() {
    }

    static String quote(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder json = new StringBuilder(value.length() + 16).append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (character < 0x20) {
                        json.append(String.format("\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        return json.append('"').toString();
    }

    static void appendStringMap(StringBuilder json, Map<String, String> values) {
        json.append('{');
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append(quote(entry.getKey())).append(':').append(quote(entry.getValue()));
        }
        json.append('}');
    }

    static void appendLongMap(StringBuilder json, Map<String, Long> values) {
        json.append('{');
        boolean first = true;
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append(quote(entry.getKey())).append(':').append(entry.getValue());
        }
        json.append('}');
    }

    static void appendStrings(StringBuilder json, Collection<String> values) {
        json.append('[');
        boolean first = true;
        for (String value : values) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append(quote(value));
        }
        json.append(']');
    }
}
