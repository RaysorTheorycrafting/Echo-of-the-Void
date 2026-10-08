package com.eotv.echoofthevoid.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiagnosticSessionStoreTest {
    @TempDir
    Path temporaryWorld;

    @Test
    void jsonEscapesControlCharactersWithoutLosingUnicode() {
        assertEquals("\"line\\n\\t\\\"\\\\é\"", DiagnosticJson.quote("line\n\t\"\\é"));
        assertEquals("\"\\u0001\"", DiagnosticJson.quote("\u0001"));
        assertEquals("null", DiagnosticJson.quote(null));
    }

    @Test
    void sessionWritesStructuredEventsSummaryAndCleanMarker() throws Exception {
        Instant started = Instant.parse("2026-08-26T10:15:30Z");
        DiagnosticSessionStore.OpenResult opened = DiagnosticSessionStore.open(
                temporaryWorld,
                Map.of("mod_version", "2.0.0", "installed_mods", "minecraft@1.21.1"),
                started,
                UUID.fromString("12345678-1234-1234-1234-1234567890ab"));
        DiagnosticSessionStore store = opened.store();

        assertTrue(store.record(12L, DiagnosticSeverity.WARNING, "event", "context_failed", Map.of(
                "event_id", "empty_lead",
                "detail", "line\nquoted\"")));
        assertTrue(store.record(13L, DiagnosticSeverity.ERROR, "log", "exception", Map.of(
                "message", "boom")));
        assertTrue(store.record(13L, DiagnosticSeverity.INFO, "event", "event_outcome", Map.of(
                "lane", "primary",
                "event_id", "empty_lead",
                "outcome", "started")));
        assertTrue(store.record(13L, DiagnosticSeverity.INFO, "special", "special_lifecycle", Map.of(
                "special_id", "follower",
                "stage", "ended",
                "reason", "fled_out_of_sight")));
        store.recordTick(120_000_000L);
        store.recordTick(300_000_000L);
        store.finish(14L, "test_complete");

        Path session = temporaryWorld.resolve(DiagnosticSessionStore.DIAGNOSTICS_DIRECTORY)
                .resolve("sessions")
                .resolve("20260826-101530-000_12345678");
        String events = Files.readString(session.resolve("events.jsonl"), StandardCharsets.UTF_8);
        String summary = Files.readString(session.resolve("summary.json"), StandardCharsets.UTF_8);
        String report = Files.readString(session.resolve("REPORT.md"), StandardCharsets.UTF_8);
        assertTrue(events.contains("\"server_tick\":12"));
        assertTrue(events.contains("\"event_id\":\"empty_lead\""));
        assertTrue(events.contains("line\\nquoted\\\""));
        assertTrue(events.contains("\"code\":\"session_stop\""));
        assertTrue(summary.contains("\"active\":false"));
        assertTrue(summary.contains("\"slow_tick_count_100ms\":2"));
        assertTrue(summary.contains("\"very_slow_tick_count_250ms\":1"));
        assertTrue(summary.contains("\"primary/empty_lead/started\":1"));
        assertTrue(summary.contains("\"follower/ended/fled_out_of_sight\":1"));
        assertTrue(report.contains("Scheduler and weather outcomes"));
        assertTrue(report.contains("`primary/empty_lead/started`: 1"));
        assertTrue(report.contains("Special lifecycle evidence"));
        assertTrue(report.contains("`follower/ended/fled_out_of_sight`: 1"));
        assertTrue(Files.exists(session.resolve("closed.marker")));
        assertFalse(Files.exists(session.resolve("open.marker")));
    }

    @Test
    void startupMarksAnUnclosedPriorSessionAsInterrupted() throws Exception {
        Path stale = temporaryWorld.resolve(DiagnosticSessionStore.DIAGNOSTICS_DIRECTORY)
                .resolve("sessions")
                .resolve("20260825-010203-000_deadbeef");
        Files.createDirectories(stale);
        Files.writeString(stale.resolve("open.marker"), "stale", StandardCharsets.UTF_8);

        DiagnosticSessionStore.OpenResult opened = DiagnosticSessionStore.open(
                temporaryWorld,
                Map.of(),
                Instant.parse("2026-08-26T10:15:30Z"),
                UUID.fromString("12345678-1234-1234-1234-1234567890ab"));
        try {
            assertEquals(1, opened.interruptedSessions().size());
            assertEquals("20260825-010203-000_deadbeef", opened.interruptedSessions().getFirst());
            assertTrue(Files.exists(stale.resolve("interrupted.marker")));
            assertFalse(Files.exists(stale.resolve("open.marker")));
        } finally {
            opened.store().finish(0L, "test_complete");
        }
    }

    @Test
    void bundleContainsAllSessionsAndPurgePreservesTheActiveOne() throws Exception {
        Path old = temporaryWorld.resolve(DiagnosticSessionStore.DIAGNOSTICS_DIRECTORY)
                .resolve("sessions")
                .resolve("20260824-010203-000_old00000");
        Files.createDirectories(old);
        Files.writeString(old.resolve("closed.marker"), "closed", StandardCharsets.UTF_8);
        Files.writeString(old.resolve("events.jsonl"), "{}\n", StandardCharsets.UTF_8);

        DiagnosticSessionStore store = DiagnosticSessionStore.open(
                temporaryWorld,
                Map.of(),
                Instant.parse("2026-08-26T10:15:30Z"),
                UUID.fromString("12345678-1234-1234-1234-1234567890ab")).store();
        try {
            store.record(1L, DiagnosticSeverity.INFO, "test", "sample", Map.of());
            Path bundle = store.createBundle();
            assertTrue(Files.isRegularFile(bundle));
            try (ZipFile zip = new ZipFile(bundle.toFile(), StandardCharsets.UTF_8)) {
                assertTrue(zip.stream().anyMatch(entry -> entry.getName().endsWith("/environment.json")));
                assertTrue(zip.stream().anyMatch(entry -> entry.getName().contains("old00000/events.jsonl")));
            }

            DiagnosticSessionStore.PurgeResult result = store.purgeClosedSessionsAndBundles();
            assertEquals(1, result.removedSessions());
            assertEquals(1, result.removedBundles());
            assertTrue(Files.isDirectory(temporaryWorld.resolve(DiagnosticSessionStore.DIAGNOSTICS_DIRECTORY)
                    .resolve("sessions").resolve(store.sessionId())));
            assertFalse(Files.exists(old));
            assertFalse(Files.exists(bundle));
        } finally {
            store.finish(2L, "test_complete");
        }
    }

    @Test
    void retentionKeepsAtMostThirtyTwoSessionsIncludingTheNewActiveOne() throws Exception {
        Path sessions = temporaryWorld.resolve(DiagnosticSessionStore.DIAGNOSTICS_DIRECTORY).resolve("sessions");
        Files.createDirectories(sessions);
        for (int index = 0; index < 40; index++) {
            Path session = sessions.resolve(String.format("20260801-000000-%03d_fake%04d", index, index));
            Files.createDirectory(session);
            Files.writeString(session.resolve("closed.marker"), "closed", StandardCharsets.UTF_8);
        }

        DiagnosticSessionStore store = DiagnosticSessionStore.open(
                temporaryWorld,
                Map.of(),
                Instant.parse("2026-08-26T10:15:30Z"),
                UUID.fromString("12345678-1234-1234-1234-1234567890ab")).store();
        try (var children = Files.list(sessions)) {
            assertEquals(DiagnosticSessionStore.MAX_RETAINED_SESSIONS, children.filter(Files::isDirectory).count());
        } finally {
            store.finish(0L, "test_complete");
        }
    }
}
