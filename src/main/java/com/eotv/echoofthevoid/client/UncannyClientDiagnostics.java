package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.diagnostics.ClientPerformanceRules;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.network.UncannyClientDiagnosticPayload;
import com.eotv.echoofthevoid.event.UncannyClientStateSync;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

/** Captures client-only failures and forwards bounded reports to the authoritative server session. */
public final class UncannyClientDiagnostics {
    private static final int MAX_QUEUED_REPORTS = 64;
    private static final int MAX_REPORTS_PER_TICK = 4;
    private static final Deque<ClientReport> REPORTS = new ArrayDeque<>();
    private static final Map<Integer, UUID> RENDERED_SPECIALS = new HashMap<>();
    private static final Map<Integer, UUID> DIRECTLY_OBSERVABLE_SPECIALS = new HashMap<>();
    private static final ThreadLocal<Boolean> CAPTURING = ThreadLocal.withInitial(() -> false);
    private static ClientLogAppender appender;
    private static boolean connected;
    private static long connectedTicks;
    private static long droppedReports;
    private static int fpsBaseline;
    private static int consecutiveLowFpsSamples;
    private static long nextFpsReportTick;

    private UncannyClientDiagnostics() {
    }

    public static synchronized void install() {
        if (appender != null) {
            return;
        }
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        appender = new ClientLogAppender();
        appender.start();
        configuration.addAppender(appender);
        configuration.getRootLogger().addAppender(appender, Level.WARN, null);
        context.updateLoggers();
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean nowConnected = minecraft.player != null && minecraft.getConnection() != null;
        if (!nowConnected) {
            if (connected) {
                connected = false;
                connectedTicks = 0L;
                fpsBaseline = 0;
                consecutiveLowFpsSamples = 0;
                nextFpsReportTick = 0L;
                synchronized (REPORTS) {
                    REPORTS.clear();
                }
                RENDERED_SPECIALS.clear();
                DIRECTLY_OBSERVABLE_SPECIALS.clear();
            }
            return;
        }

        if (!connected) {
            connected = true;
            connectedTicks = 0L;
            enqueue("INFO", "client_connected", "Client diagnostic channel connected", "");
        }
        connectedTicks++;
        if (connectedTicks % ClientPerformanceRules.SAMPLE_INTERVAL_TICKS == 0L) {
            sampleClientPerformance(minecraft);
        }
        if (connectedTicks % 1_200L == 0L) {
            Runtime runtime = Runtime.getRuntime();
            enqueue(
                    "INFO",
                    "client_snapshot",
                    "Periodic client health snapshot",
                    performanceContext(minecraft, runtime));
        }

        for (int sent = 0; sent < MAX_REPORTS_PER_TICK; sent++) {
            ClientReport report;
            synchronized (REPORTS) {
                report = REPORTS.pollFirst();
            }
            if (report == null) {
                return;
            }
            try {
                PacketDistributor.sendToServer(new UncannyClientDiagnosticPayload(
                        report.severity(), report.code(), report.message(), report.context()));
            } catch (RuntimeException exception) {
                System.err.println("[EchoOfTheVoid/ClientDiagnostics] Unable to send report: "
                        + exception.getClass().getSimpleName() + ": " + exception.getMessage());
                return;
            }
        }
    }

    private static void sampleClientPerformance(Minecraft minecraft) {
        int framesPerSecond = minecraft.getFps();
        int comparisonBaseline = fpsBaseline;
        boolean eligible = minecraft.isWindowActive() && !minecraft.isPaused();
        if (eligible && ClientPerformanceRules.isDegraded(framesPerSecond, comparisonBaseline)) {
            consecutiveLowFpsSamples++;
        } else {
            consecutiveLowFpsSamples = 0;
        }
        fpsBaseline = ClientPerformanceRules.updateBaseline(fpsBaseline, framesPerSecond);

        if (consecutiveLowFpsSamples < ClientPerformanceRules.REQUIRED_CONSECUTIVE_SAMPLES
                || connectedTicks < nextFpsReportTick) {
            return;
        }

        Runtime runtime = Runtime.getRuntime();
        enqueue(
                "WARNING",
                "client_fps_degradation",
                "Sustained client FPS degradation detected",
                "comparison_baseline_fps=" + comparisonBaseline
                        + ";consecutive_samples=" + consecutiveLowFpsSamples
                        + ";" + performanceContext(minecraft, runtime));
        consecutiveLowFpsSamples = 0;
        nextFpsReportTick = connectedTicks + ClientPerformanceRules.REPORT_COOLDOWN_TICKS;
    }

    private static String performanceContext(Minecraft minecraft, Runtime runtime) {
        String screen = minecraft.screen == null ? "none" : minecraft.screen.getClass().getSimpleName();
        String dimension = minecraft.level == null
                ? "none" : minecraft.level.dimension().location().toString();
        int loadedClientChunks = minecraft.level == null
                ? 0 : minecraft.level.getChunkSource().getLoadedChunksCount();
        return "fps=" + minecraft.getFps()
                + ";fps_baseline=" + fpsBaseline
                + ";window_active=" + minecraft.isWindowActive()
                + ";paused=" + minecraft.isPaused()
                + ";screen=" + screen
                + ";dimension=" + dimension
                + ";render_distance=" + minecraft.options.renderDistance().get()
                + ";simulation_distance=" + minecraft.options.simulationDistance().get()
                + ";graphics=" + minecraft.options.graphicsMode().get()
                + ";particles=" + minecraft.options.particles().get()
                + ";loaded_client_chunks=" + loadedClientChunks
                + ";synced_phase=" + UncannyClientStateSync.getClientPhaseIndex()
                + ";synced_weather=" + UncannyClientStateSync.getClientWeatherEventId()
                + ";hunter_fog=" + UncannyClientStateSync.isClientHunterFogActive()
                + ";giant_sun=" + UncannyClientStateSync.isClientGiantSunActive()
                + ";used_memory_bytes=" + (runtime.totalMemory() - runtime.freeMemory())
                + ";allocated_memory_bytes=" + runtime.totalMemory()
                + ";dropped_reports=" + droppedReports
                + ";audio={" + UncannyClientAudioEffects.diagnosticState() + "}"
                + ";localized_weather={" + UncannyLocalizedWeatherClientEffects.diagnosticState() + "}"
                + ";native_visuals={" + UncannyNativeAnomalyClientEffects.diagnosticState() + "}";
    }

    /** Confirms that a Special reached the client renderer; direct LOS is reported separately. */
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.level == null
                || entity.level() != minecraft.level
                || !UncannyEntityRegistry.isSpecialEntity(entity.getType())) {
            return;
        }

        UUID uuid = entity.getUUID();
        boolean directLineOfSight = minecraft.player.hasLineOfSight(entity);
        UUID renderedUuid = RENDERED_SPECIALS.put(entity.getId(), uuid);
        if (!uuid.equals(renderedUuid)) {
            enqueue(
                    "INFO",
                    "client_special_rendered",
                    "A Special reached the living-entity renderer",
                    specialRenderContext(minecraft, entity, directLineOfSight));
        }
        if (directLineOfSight) {
            UUID observableUuid = DIRECTLY_OBSERVABLE_SPECIALS.put(entity.getId(), uuid);
            if (!uuid.equals(observableUuid)) {
                enqueue(
                        "INFO",
                        "client_special_directly_observable",
                        "A rendered Special had direct client line of sight",
                        specialRenderContext(minecraft, entity, true));
            }
        }
    }

    static void enqueue(String severity, String code, String message, String context) {
        ClientReport report = new ClientReport(
                bounded(severity, 16),
                bounded(code, 96),
                bounded(redactLocalPaths(message), 8_192),
                bounded(redactLocalPaths(context), 2_048));
        synchronized (REPORTS) {
            if (REPORTS.size() >= MAX_QUEUED_REPORTS) {
                REPORTS.removeFirst();
                droppedReports++;
            }
            REPORTS.addLast(report);
        }
    }

    private static String specialRenderContext(
            Minecraft minecraft,
            LivingEntity entity,
            boolean directLineOfSight) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return "runtime_entity_id=" + entity.getId()
                + ";type=" + (type == null ? "unknown" : type)
                + ";distance=" + String.format(Locale.ROOT, "%.3f", minecraft.player.distanceTo(entity))
                + ";direct_line_of_sight=" + directLineOfSight
                + ";entity_position=" + entity.blockPosition().getX() + ","
                + entity.blockPosition().getY() + "," + entity.blockPosition().getZ();
    }

    private static boolean isClientLog(LogEvent event) {
        String logger = event.getLoggerName() == null ? "" : event.getLoggerName();
        String thread = event.getThreadName() == null
                ? "" : event.getThreadName().toLowerCase(Locale.ROOT);
        boolean clientNamespace = logger.startsWith("net.minecraft.client")
                || logger.startsWith("com.mojang.blaze3d")
                || logger.startsWith("org.lwjgl");
        boolean clientThread = thread.contains("render")
                || thread.contains("sound")
                || thread.contains("download")
                || thread.contains("resource reload");
        boolean modWarning = logger.startsWith("com.eotv.echoofthevoid")
                && event.getLevel().isMoreSpecificThan(Level.WARN)
                && !thread.contains("server");
        return modWarning || (event.getLevel().isMoreSpecificThan(Level.ERROR)
                && (clientNamespace || clientThread));
    }

    private static String stackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringWriter output = new StringWriter();
        throwable.printStackTrace(new PrintWriter(output));
        return output.toString();
    }

    private static String redactLocalPaths(String value) {
        String redacted = value == null ? "" : value;
        for (String property : new String[] {"user.home", "user.dir"}) {
            String path = System.getProperty(property, "");
            if (!path.isBlank()) {
                redacted = redacted.replace(path, "<" + property.replace('.', '-') + ">");
                redacted = redacted.replace(path.replace('\\', '/'), "<" + property.replace('.', '-') + ">");
            }
        }
        return redacted.replaceAll(
                "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
                "<uuid>");
    }

    private static String bounded(String value, int maximumLength) {
        String safe = value == null ? "" : value;
        return safe.length() <= maximumLength
                ? safe : safe.substring(0, maximumLength) + "...[truncated]";
    }

    private record ClientReport(String severity, String code, String message, String context) {
    }

    private static final class ClientLogAppender extends AbstractAppender {
        private ClientLogAppender() {
            super("EchoOfTheVoidClientDiagnosticCapture", null,
                    PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            if (event == null || CAPTURING.get() || !isClientLog(event)) {
                return;
            }
            CAPTURING.set(true);
            try {
                String severity = event.getLevel().isMoreSpecificThan(Level.FATAL)
                        ? "FATAL" : event.getLevel().isMoreSpecificThan(Level.ERROR) ? "ERROR" : "WARNING";
                String formatted = event.getMessage() == null ? "" : event.getMessage().getFormattedMessage();
                String stack = stackTrace(event.getThrown());
                enqueue(
                        severity,
                        event.getThrown() == null ? "client_log_message" : "client_exception",
                        stack.isBlank() ? formatted : formatted + "\n" + stack,
                        "logger=" + event.getLoggerName()
                                + ";thread=" + event.getThreadName()
                                + ";level=" + event.getLevel().name());
            } finally {
                CAPTURING.set(false);
            }
        }
    }
}
