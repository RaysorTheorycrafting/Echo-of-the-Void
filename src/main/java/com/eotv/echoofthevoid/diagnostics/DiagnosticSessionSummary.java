package com.eotv.echoofthevoid.diagnostics;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

final class DiagnosticSessionSummary {
    private final String sessionId;
    private final Instant startedAt;
    private final Map<String, Long> bySeverity = new TreeMap<>();
    private final Map<String, Long> byCategory = new TreeMap<>();
    private final Map<String, Long> byCode = new TreeMap<>();
    private final Map<String, Long> problemCodes = new TreeMap<>();
    private final Map<String, Long> eventOutcomes = new TreeMap<>();
    private final Map<String, Long> soundDeliveries = new TreeMap<>();
    private final Map<String, Long> entityLifecycle = new TreeMap<>();
    private final Map<String, Long> specialLifecycle = new TreeMap<>();
    private final Map<String, Long> clientReports = new TreeMap<>();
    private long eventCount;
    private long droppedEventCount;
    private long tickCount;
    private long slowTickCount;
    private long verySlowTickCount;
    private long totalTickNanos;
    private long maximumTickNanos;

    DiagnosticSessionSummary(String sessionId, Instant startedAt) {
        this.sessionId = sessionId;
        this.startedAt = startedAt;
    }

    void record(DiagnosticEvent event) {
        eventCount++;
        bySeverity.merge(event.severity().name(), 1L, Long::sum);
        byCategory.merge(event.category(), 1L, Long::sum);
        byCode.merge(event.code(), 1L, Long::sum);
        if (event.severity() == DiagnosticSeverity.WARNING
                || event.severity() == DiagnosticSeverity.ERROR
                || event.severity() == DiagnosticSeverity.FATAL) {
            problemCodes.merge(event.code(), 1L, Long::sum);
        }
        if ("event_outcome".equals(event.code())) {
            eventOutcomes.merge(
                    event.fields().getOrDefault("lane", "unknown") + "/"
                            + event.fields().getOrDefault("event_id", "none") + "/"
                            + event.fields().getOrDefault("outcome", "unknown"),
                    1L,
                    Long::sum);
        } else if (event.code().startsWith("weather_")) {
            eventOutcomes.merge(
                    "weather/" + event.fields().getOrDefault("event_id", "none") + "/" + event.code(),
                    1L,
                    Long::sum);
        }
        if ("sound".equals(event.category())) {
            soundDeliveries.merge(
                    event.code() + "/" + event.fields().getOrDefault("sound_id", "unknown"),
                    1L,
                    Long::sum);
        }
        if (event.code().equals("tracked_entity_joined") || event.code().equals("tracked_entity_left")) {
            entityLifecycle.merge(
                    event.code() + "/" + event.fields().getOrDefault("identity", "unknown"),
                    1L,
                    Long::sum);
        }
        if (event.code().equals("special_lifecycle")) {
            specialLifecycle.merge(
                    event.fields().getOrDefault("special_id", "unknown") + "/"
                            + event.fields().getOrDefault("stage", "unknown") + "/"
                            + event.fields().getOrDefault("reason", "unknown"),
                    1L,
                    Long::sum);
        } else if (event.code().equals("special_client_tracking_started")
                || event.code().equals("special_client_tracking_stopped")) {
            specialLifecycle.merge(
                    event.fields().getOrDefault("special_id", "unknown") + "/network/" + event.code(),
                    1L,
                    Long::sum);
        }
        if (event.code().equals("client_report")) {
            clientReports.merge(event.fields().getOrDefault("client_code", "unknown"), 1L, Long::sum);
        }
    }

    void recordDropped() {
        droppedEventCount++;
    }

    void recordTick(long elapsedNanos) {
        long safeElapsed = Math.max(0L, elapsedNanos);
        tickCount++;
        totalTickNanos += safeElapsed;
        maximumTickNanos = Math.max(maximumTickNanos, safeElapsed);
        if (safeElapsed >= 100_000_000L) {
            slowTickCount++;
        }
        if (safeElapsed >= 250_000_000L) {
            verySlowTickCount++;
        }
    }

    long eventCount() {
        return eventCount;
    }

    long droppedEventCount() {
        return droppedEventCount;
    }

    long warningCount() {
        return bySeverity.getOrDefault(DiagnosticSeverity.WARNING.name(), 0L);
    }

    long errorCount() {
        return bySeverity.getOrDefault(DiagnosticSeverity.ERROR.name(), 0L)
                + bySeverity.getOrDefault(DiagnosticSeverity.FATAL.name(), 0L);
    }

    String toJson(Instant endedAt, String closeReason, boolean active) {
        double averageTickMillis = tickCount == 0L ? 0.0D : (totalTickNanos / 1_000_000.0D) / tickCount;
        StringBuilder json = new StringBuilder(1024);
        json.append('{')
                .append("\"schema\":1")
                .append(",\"session_id\":").append(DiagnosticJson.quote(sessionId))
                .append(",\"started_utc\":").append(DiagnosticJson.quote(startedAt.toString()))
                .append(",\"updated_utc\":").append(DiagnosticJson.quote(endedAt.toString()))
                .append(",\"active\":").append(active)
                .append(",\"close_reason\":").append(DiagnosticJson.quote(closeReason))
                .append(",\"event_count\":").append(eventCount)
                .append(",\"dropped_event_count\":").append(droppedEventCount)
                .append(",\"tick_count\":").append(tickCount)
                .append(",\"slow_tick_count_100ms\":").append(slowTickCount)
                .append(",\"very_slow_tick_count_250ms\":").append(verySlowTickCount)
                .append(",\"average_tick_ms\":").append(String.format(java.util.Locale.ROOT, "%.3f", averageTickMillis))
                .append(",\"maximum_tick_ms\":").append(String.format(java.util.Locale.ROOT, "%.3f", maximumTickNanos / 1_000_000.0D))
                .append(",\"counts_by_severity\":");
        DiagnosticJson.appendLongMap(json, bySeverity);
        json.append(",\"counts_by_category\":");
        DiagnosticJson.appendLongMap(json, byCategory);
        json.append(",\"counts_by_code\":");
        DiagnosticJson.appendLongMap(json, byCode);
        json.append(",\"problem_codes\":");
        DiagnosticJson.appendLongMap(json, problemCodes);
        json.append(",\"event_outcomes\":");
        DiagnosticJson.appendLongMap(json, eventOutcomes);
        json.append(",\"sound_deliveries\":");
        DiagnosticJson.appendLongMap(json, soundDeliveries);
        json.append(",\"entity_lifecycle\":");
        DiagnosticJson.appendLongMap(json, entityLifecycle);
        json.append(",\"special_lifecycle\":");
        DiagnosticJson.appendLongMap(json, specialLifecycle);
        json.append(",\"client_reports\":");
        DiagnosticJson.appendLongMap(json, clientReports);
        return json.append('}').toString();
    }

    String toMarkdown(Instant updatedAt, String closeReason, boolean active) {
        double averageTickMillis = tickCount == 0L ? 0.0D : (totalTickNanos / 1_000_000.0D) / tickCount;
        StringBuilder report = new StringBuilder(2048);
        report.append("# Echo of the Void - diagnostic session\n\n")
                .append("- Session: `").append(sessionId).append("`\n")
                .append("- Started (UTC): ").append(startedAt).append("\n")
                .append("- Updated (UTC): ").append(updatedAt).append("\n")
                .append("- State: ").append(active ? "active" : "closed").append(" (`")
                .append(closeReason).append("`)\n")
                .append("- Events recorded: ").append(eventCount).append("\n")
                .append("- Warnings: ").append(warningCount()).append("\n")
                .append("- Errors/Fatal: ").append(errorCount()).append("\n")
                .append("- Dropped records: ").append(droppedEventCount).append("\n")
                .append("- Server ticks: ").append(tickCount)
                .append("; average ").append(String.format(java.util.Locale.ROOT, "%.3f", averageTickMillis))
                .append(" ms; maximum ").append(String.format(java.util.Locale.ROOT, "%.3f", maximumTickNanos / 1_000_000.0D))
                .append(" ms\n")
                .append("- Slow ticks >=100 ms: ").append(slowTickCount)
                .append("; >=250 ms: ").append(verySlowTickCount).append("\n\n");

        report.append("## Findings\n\n");
        if (errorCount() == 0L && warningCount() == 0L && droppedEventCount == 0L) {
            report.append("No recorder-level error or warning has been detected so far.\n\n");
        } else {
            appendMarkdownCounts(report, "Error and warning codes", problemCodes);
            if (droppedEventCount > 0L) {
                report.append("- Diagnostic capacity was exceeded; ")
                        .append(droppedEventCount).append(" record(s) could not be written.\n\n");
            }
        }
        appendMarkdownCounts(report, "Scheduler and weather outcomes", eventOutcomes);
        appendMarkdownCounts(report, "Client reports", clientReports);
        appendMarkdownCounts(report, "Sound deliveries", soundDeliveries);
        appendMarkdownCounts(report, "Tracked entity lifecycle", entityLifecycle);
        appendMarkdownCounts(report, "Special lifecycle evidence", specialLifecycle);
        report.append("## Interpretation limit\n\n")
                .append("This report detects exceptions, warnings, invalid states, failed contexts, lifecycle mismatches and performance symptoms. ")
                .append("It cannot decide whether a scene was subtle, frightening or aesthetically correct; correlate those observations with `manual_observation` records in `events.jsonl`.\n");
        return report.toString();
    }

    private static void appendMarkdownCounts(StringBuilder report, String title, Map<String, Long> values) {
        report.append("### ").append(title).append("\n\n");
        if (values.isEmpty()) {
            report.append("None recorded.\n\n");
            return;
        }
        values.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .forEach(entry -> report.append("- `").append(entry.getKey()).append("`: ")
                        .append(entry.getValue()).append("\n"));
        report.append('\n');
    }
}
