package com.eotv.echoofthevoid.diagnostics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class DiagnosticSessionStore implements AutoCloseable {
    static final String DIAGNOSTICS_DIRECTORY = "echoofthevoid-diagnostics";
    static final long MAX_EVENT_BYTES = 128L * 1024L * 1024L;
    static final long MAX_EVENT_COUNT = 250_000L;
    static final int MAX_RETAINED_SESSIONS = 32;
    private static final DateTimeFormatter SESSION_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);

    private final Path root;
    private final Path sessionsRoot;
    private final Path bundlesRoot;
    private final Path sessionDirectory;
    private final String sessionId;
    private final Instant startedAt;
    private final DiagnosticSessionSummary summary;
    private final BufferedWriter eventsWriter;
    private long sequence;
    private long eventBytes;
    private boolean closed;
    private boolean capacityWarningWritten;

    private DiagnosticSessionStore(
            Path root,
            Path sessionsRoot,
            Path bundlesRoot,
            Path sessionDirectory,
            String sessionId,
            Instant startedAt,
            BufferedWriter eventsWriter) {
        this.root = root;
        this.sessionsRoot = sessionsRoot;
        this.bundlesRoot = bundlesRoot;
        this.sessionDirectory = sessionDirectory;
        this.sessionId = sessionId;
        this.startedAt = startedAt;
        this.summary = new DiagnosticSessionSummary(sessionId, startedAt);
        this.eventsWriter = eventsWriter;
    }

    public static OpenResult open(Path worldRoot, Map<String, String> environment) throws IOException {
        return open(worldRoot, environment, Instant.now(), UUID.randomUUID());
    }

    static OpenResult open(
            Path worldRoot,
            Map<String, String> environment,
            Instant startedAt,
            UUID sessionUuid) throws IOException {
        Path normalizedWorldRoot = worldRoot.toAbsolutePath().normalize();
        Path root = normalizedWorldRoot.resolve(DIAGNOSTICS_DIRECTORY).normalize();
        if (!root.startsWith(normalizedWorldRoot)) {
            throw new IOException("Diagnostic root escaped the world directory");
        }

        Path sessionsRoot = root.resolve("sessions");
        Path bundlesRoot = root.resolve("bundles");
        Files.createDirectories(sessionsRoot);
        Files.createDirectories(bundlesRoot);

        List<String> interrupted = markInterruptedSessions(sessionsRoot, startedAt);
        pruneOldSessions(sessionsRoot, MAX_RETAINED_SESSIONS - 1);

        String suffix = sessionUuid.toString().replace("-", "").substring(0, 8);
        String sessionId = SESSION_TIME.format(startedAt) + "_" + suffix;
        Path sessionDirectory = sessionsRoot.resolve(sessionId).normalize();
        if (!sessionDirectory.getParent().equals(sessionsRoot)) {
            throw new IOException("Invalid diagnostic session path");
        }
        Files.createDirectory(sessionDirectory);
        Files.writeString(
                sessionDirectory.resolve("open.marker"),
                startedAt + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);

        Map<String, String> sortedEnvironment = new TreeMap<>(environment);
        StringBuilder environmentJson = new StringBuilder(1024).append('{')
                .append("\"schema\":1")
                .append(",\"session_id\":").append(DiagnosticJson.quote(sessionId))
                .append(",\"started_utc\":").append(DiagnosticJson.quote(startedAt.toString()))
                .append(",\"environment\":");
        DiagnosticJson.appendStringMap(environmentJson, sortedEnvironment);
        environmentJson.append(",\"interrupted_sessions_detected\":");
        DiagnosticJson.appendStrings(environmentJson, interrupted);
        environmentJson.append('}');
        writeAtomically(sessionDirectory.resolve("environment.json"), environmentJson.toString());
        writeAtomically(root.resolve("LATEST.txt"), sessionId + System.lineSeparator());

        BufferedWriter writer = Files.newBufferedWriter(
                sessionDirectory.resolve("events.jsonl"),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
        DiagnosticSessionStore store = new DiagnosticSessionStore(
                root, sessionsRoot, bundlesRoot, sessionDirectory, sessionId, startedAt, writer);
        store.checkpoint("active", true);
        return new OpenResult(store, List.copyOf(interrupted));
    }

    public synchronized boolean record(
            long serverTick,
            DiagnosticSeverity severity,
            String category,
            String code,
            Map<String, String> fields) throws IOException {
        ensureOpen();
        if (sequence >= MAX_EVENT_COUNT || eventBytes >= MAX_EVENT_BYTES) {
            summary.recordDropped();
            capacityWarningWritten = true;
            return false;
        }

        Instant occurredAt = Instant.now();
        DiagnosticEvent event = new DiagnosticEvent(
                ++sequence,
                occurredAt,
                Math.max(0L, java.time.Duration.between(startedAt, occurredAt).toMillis()),
                serverTick,
                severity,
                category,
                code,
                fields);
        String line = event.toJsonLine();
        int bytes = line.getBytes(StandardCharsets.UTF_8).length + 1;
        if (eventBytes + bytes > MAX_EVENT_BYTES) {
            summary.recordDropped();
            capacityWarningWritten = true;
            return false;
        }
        eventsWriter.write(line);
        eventsWriter.newLine();
        eventBytes += bytes;
        summary.record(event);
        if (severity == DiagnosticSeverity.ERROR || severity == DiagnosticSeverity.FATAL) {
            eventsWriter.flush();
        }
        return true;
    }

    public synchronized void recordTick(long elapsedNanos) {
        if (!closed) {
            summary.recordTick(elapsedNanos);
        }
    }

    public synchronized void flushEvents() throws IOException {
        ensureOpen();
        eventsWriter.flush();
    }

    public synchronized void checkpoint(String reason, boolean active) throws IOException {
        ensureOpen();
        eventsWriter.flush();
        Instant now = Instant.now();
        writeAtomically(sessionDirectory.resolve("summary.json"), summary.toJson(now, reason, active));
        writeAtomically(sessionDirectory.resolve("REPORT.md"), summary.toMarkdown(now, reason, active));
    }

    public synchronized Path createBundle() throws IOException {
        ensureOpen();
        checkpoint("bundle_created", true);
        Files.createDirectories(bundlesRoot);
        String bundleName = "echoofthevoid-diagnostics-" + SESSION_TIME.format(Instant.now()) + ".zip";
        Path bundle = uniqueBundlePath(bundleName);
        Path temporary = bundle.resolveSibling(bundle.getFileName() + ".tmp");

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(
                temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE), StandardCharsets.UTF_8);
             var paths = Files.walk(sessionsRoot)) {
            for (Path source : paths.filter(Files::isRegularFile).sorted().toList()) {
                String entryName = "sessions/" + sessionsRoot.relativize(source).toString().replace('\\', '/');
                zip.putNextEntry(new ZipEntry(entryName));
                Files.copy(source, zip);
                zip.closeEntry();
            }
        } catch (IOException exception) {
            Files.deleteIfExists(temporary);
            throw exception;
        }
        moveReplacing(temporary, bundle);
        return bundle;
    }

    public synchronized PurgeResult purgeClosedSessionsAndBundles() throws IOException {
        ensureOpen();
        int removedSessions = 0;
        for (Path session : listChildDirectories(sessionsRoot)) {
            if (!session.equals(sessionDirectory)) {
                deleteTreeInside(sessionsRoot, session);
                removedSessions++;
            }
        }
        int removedBundles = 0;
        if (Files.isDirectory(bundlesRoot)) {
            try (var paths = Files.list(bundlesRoot)) {
                for (Path bundle : paths.toList()) {
                    if (Files.isRegularFile(bundle)) {
                        Files.delete(bundle);
                        removedBundles++;
                    }
                }
            }
        }
        return new PurgeResult(removedSessions, removedBundles);
    }

    public synchronized List<SessionInfo> listSessions() throws IOException {
        List<SessionInfo> sessions = new ArrayList<>();
        for (Path directory : listChildDirectories(sessionsRoot)) {
            String state;
            if (Files.exists(directory.resolve("open.marker"))) {
                state = directory.equals(sessionDirectory) ? "active" : "open";
            } else if (Files.exists(directory.resolve("interrupted.marker"))) {
                state = "interrupted";
            } else if (Files.exists(directory.resolve("closed.marker"))) {
                state = "closed";
            } else {
                state = "unknown";
            }
            sessions.add(new SessionInfo(directory.getFileName().toString(), state));
        }
        sessions.sort(Comparator.comparing(SessionInfo::sessionId).reversed());
        return List.copyOf(sessions);
    }

    public synchronized Status status() {
        return new Status(
                sessionId,
                root,
                summary.eventCount(),
                summary.warningCount(),
                summary.errorCount(),
                summary.droppedEventCount(),
                eventBytes,
                capacityWarningWritten);
    }

    public synchronized void finish(long serverTick, String reason) throws IOException {
        if (closed) {
            return;
        }
        record(serverTick, DiagnosticSeverity.INFO, "lifecycle", "session_stop", Map.of("reason", reason));
        checkpoint(reason, false);
        eventsWriter.close();
        closed = true;
        Files.deleteIfExists(sessionDirectory.resolve("open.marker"));
        Files.writeString(
                sessionDirectory.resolve("closed.marker"),
                Instant.now() + " " + reason + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    @Override
    public void close() throws IOException {
        finish(-1L, "closed");
    }

    public String sessionId() {
        return sessionId;
    }

    public Path root() {
        return root;
    }

    private Path uniqueBundlePath(String preferredName) throws IOException {
        Path preferred = bundlesRoot.resolve(preferredName);
        if (!Files.exists(preferred)) {
            return preferred;
        }
        for (int suffix = 2; suffix < 1000; suffix++) {
            Path candidate = bundlesRoot.resolve(preferredName.replace(".zip", "-" + suffix + ".zip"));
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IOException("Too many diagnostic bundles with the same timestamp");
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Diagnostic session is already closed");
        }
    }

    private static List<String> markInterruptedSessions(Path sessionsRoot, Instant detectedAt) throws IOException {
        List<String> interrupted = new ArrayList<>();
        for (Path session : listChildDirectories(sessionsRoot)) {
            Path openMarker = session.resolve("open.marker");
            if (!Files.exists(openMarker)) {
                continue;
            }
            String sessionId = session.getFileName().toString();
            Files.delete(openMarker);
            Files.writeString(
                    session.resolve("interrupted.marker"),
                    detectedAt + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            interrupted.add(sessionId);
        }
        interrupted.sort(String::compareTo);
        return interrupted;
    }

    private static void pruneOldSessions(Path sessionsRoot, int maximum) throws IOException {
        List<Path> sessions = listChildDirectories(sessionsRoot);
        sessions.sort(Comparator.comparing((Path path) -> path.getFileName().toString()).reversed());
        for (int index = maximum; index < sessions.size(); index++) {
            deleteTreeInside(sessionsRoot, sessions.get(index));
        }
    }

    private static List<Path> listChildDirectories(Path parent) throws IOException {
        if (!Files.isDirectory(parent)) {
            return List.of();
        }
        try (var paths = Files.list(parent)) {
            return new ArrayList<>(paths.filter(Files::isDirectory).toList());
        }
    }

    private static void deleteTreeInside(Path allowedRoot, Path target) throws IOException {
        Path normalizedRoot = allowedRoot.toAbsolutePath().normalize();
        Path normalizedTarget = target.toAbsolutePath().normalize();
        if (normalizedTarget.equals(normalizedRoot) || !normalizedTarget.startsWith(normalizedRoot)) {
            throw new IOException("Refusing to delete outside diagnostic sessions: " + normalizedTarget);
        }
        try (var paths = Files.walk(normalizedTarget)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void writeAtomically(Path target, String content) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(
                temporary,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        moveReplacing(temporary, target);
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public record OpenResult(DiagnosticSessionStore store, List<String> interruptedSessions) {
    }

    public record PurgeResult(int removedSessions, int removedBundles) {
    }

    public record SessionInfo(String sessionId, String state) {
    }

    public record Status(
            String sessionId,
            Path root,
            long eventCount,
            long warningCount,
            long errorCount,
            long droppedEventCount,
            long eventBytes,
            boolean capacityReached) {
    }
}
