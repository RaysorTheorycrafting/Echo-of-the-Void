package com.eotv.echoofthevoid.diagnostics;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

record DiagnosticEvent(
        long sequence,
        Instant occurredAt,
        long elapsedMillis,
        long serverTick,
        DiagnosticSeverity severity,
        String category,
        String code,
        Map<String, String> fields) {

    DiagnosticEvent {
        fields = Map.copyOf(new TreeMap<>(fields));
    }

    String toJsonLine() {
        StringBuilder json = new StringBuilder(256);
        json.append('{')
                .append("\"schema\":1")
                .append(",\"sequence\":").append(sequence)
                .append(",\"utc\":").append(DiagnosticJson.quote(occurredAt.toString()))
                .append(",\"elapsed_ms\":").append(elapsedMillis)
                .append(",\"server_tick\":").append(serverTick)
                .append(",\"severity\":").append(DiagnosticJson.quote(severity.name()))
                .append(",\"category\":").append(DiagnosticJson.quote(category))
                .append(",\"code\":").append(DiagnosticJson.quote(code))
                .append(",\"fields\":");
        DiagnosticJson.appendStringMap(json, fields);
        return json.append('}').toString();
    }
}
