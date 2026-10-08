package com.eotv.echoofthevoid.diagnostics;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.campaign.UncannyCampaignDirector;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.event.UncannyParanoiaEventSystem;
import com.eotv.echoofthevoid.event.paranoia.nativeevent.MinecraftNativeAnomalySystem;
import com.eotv.echoofthevoid.event.passive.ApprovedVanillaVariantSystem;
import com.eotv.echoofthevoid.entity.variant.ReplacementVariantExpansionSystem;
import com.eotv.echoofthevoid.event.special.ApprovedSpecialSystem;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.ModList;

/**
 * Local-only diagnostic recorder. It deliberately records no chat, inventory contents, world seed,
 * account name or raw UUID. Runtime identities are replaced by session-local aliases.
 */
public final class UncannyDiagnostics {
    private static final long FLUSH_INTERVAL_TICKS = 20L;
    private static final long ENTITY_SCAN_INTERVAL_TICKS = 200L;
    private static final long SNAPSHOT_INTERVAL_TICKS = 1_200L;
    private static final long SLOW_TICK_RECORD_INTERVAL_TICKS = 100L;
    private static final long ANOMALY_REPEAT_INTERVAL_TICKS = 1_200L;
    private static final String LEGACY_PASSIVE_ENABLED = "UncannyPassiveEnabled";
    private static final String LEGACY_PASSIVE_TYPE = "UncannyPassiveType";
    private static final String LEGACY_PASSIVE_VARIANT = "UncannyPassiveVariant";

    private static final Map<UUID, String> PLAYER_ALIASES = new ConcurrentHashMap<>();
    private static final Map<String, String> PLAYER_NAME_ALIASES = new ConcurrentHashMap<>();
    private static final Map<UUID, String> ENTITY_ALIASES = new ConcurrentHashMap<>();
    private static final Map<UUID, MotionSample> MOTION_SAMPLES = new HashMap<>();
    private static final Map<String, Long> LAST_ANOMALY_TICKS = new HashMap<>();
    private static final Map<UUID, ClientReportWindow> CLIENT_REPORT_WINDOWS = new HashMap<>();
    private static final Map<UUID, ClientSyncMismatch> CLIENT_SYNC_MISMATCHES = new HashMap<>();
    private static volatile DiagnosticSessionStore store;
    private static volatile MinecraftServer activeServer;
    private static volatile long currentTick = -1L;
    private static volatile long tickStartedNanos;
    private static volatile long lastSlowTickRecord = Long.MIN_VALUE;
    private static volatile String internalFailure = "";

    private UncannyDiagnostics() {
    }

    public static synchronized void start(MinecraftServer server) {
        if (server == null || activeServer == server) {
            return;
        }
        if (store != null) {
            stop(activeServer, "server_replaced");
        }

        try {
            DiagnosticSessionStore.OpenResult opened = DiagnosticSessionStore.open(
                    server.getWorldPath(LevelResource.ROOT), environment(server));
            store = opened.store();
            activeServer = server;
            currentTick = server.getTickCount();
            internalFailure = "";
            PLAYER_ALIASES.clear();
            PLAYER_NAME_ALIASES.clear();
            ENTITY_ALIASES.clear();
            MOTION_SAMPLES.clear();
            LAST_ANOMALY_TICKS.clear();
            CLIENT_REPORT_WINDOWS.clear();
            CLIENT_SYNC_MISMATCHES.clear();
            lastSlowTickRecord = Long.MIN_VALUE;
            UncannyLogCaptureAppender.install();
            record(DiagnosticSeverity.INFO, "lifecycle", "session_start", fields(
                    "session_id", store.sessionId(),
                    "dedicated", server.isDedicatedServer(),
                    "interrupted_sessions", String.join(",", opened.interruptedSessions())));
            if (!opened.interruptedSessions().isEmpty()) {
                record(DiagnosticSeverity.ERROR, "lifecycle", "previous_session_interrupted", fields(
                        "sessions", String.join(",", opened.interruptedSessions()),
                        "meaning", "Previous server run did not close its diagnostic session cleanly"));
            }
            recordStateSnapshot(server, "startup");
            store.checkpoint("startup", true);
            EchoOfTheVoid.LOGGER.info(
                    "Echo of the Void diagnostics session {} started in {}",
                    store.sessionId(), store.root());
        } catch (Exception exception) {
            internalFailure = exception.getClass().getSimpleName() + ": " + safeMessage(exception);
            DiagnosticSessionStore failedStore = store;
            UncannyLogCaptureAppender.uninstall();
            if (failedStore != null) {
                try {
                    failedStore.finish(currentTick, "startup_failed");
                } catch (Exception closeFailure) {
                    exception.addSuppressed(closeFailure);
                }
            }
            store = null;
            activeServer = null;
            EchoOfTheVoid.LOGGER.error("Unable to start Echo of the Void diagnostics", exception);
        }
    }

    public static synchronized void stop(MinecraftServer server, String reason) {
        if (store == null || (server != null && activeServer != null && server != activeServer)) {
            return;
        }
        DiagnosticSessionStore closing = store;
        MinecraftServer closingServer = activeServer;
        try {
            if (closingServer != null) {
                currentTick = closingServer.getTickCount();
                recordStateSnapshot(closingServer, "shutdown");
            }
            closing.finish(currentTick, reason == null ? "server_stopping" : reason);
            EchoOfTheVoid.LOGGER.info("Echo of the Void diagnostics session {} closed", closing.sessionId());
        } catch (Exception exception) {
            internalFailure = exception.getClass().getSimpleName() + ": " + safeMessage(exception);
            EchoOfTheVoid.LOGGER.error("Unable to close Echo of the Void diagnostics cleanly", exception);
        } finally {
            UncannyLogCaptureAppender.uninstall();
            store = null;
            activeServer = null;
            PLAYER_ALIASES.clear();
            PLAYER_NAME_ALIASES.clear();
            ENTITY_ALIASES.clear();
            MOTION_SAMPLES.clear();
            LAST_ANOMALY_TICKS.clear();
            CLIENT_REPORT_WINDOWS.clear();
            CLIENT_SYNC_MISMATCHES.clear();
        }
    }

    public static void serverStopping(MinecraftServer server) {
        if (server == null || server != activeServer || store == null) {
            return;
        }
        currentTick = server.getTickCount();
        record(DiagnosticSeverity.INFO, "lifecycle", "server_stopping", Map.of());
        checkpoint(server, "server_stopping");
    }

    public static void onServerTickPre(MinecraftServer server) {
        if (server == activeServer && store != null) {
            currentTick = server.getTickCount();
            tickStartedNanos = System.nanoTime();
        }
    }

    public static void onServerTickPost(MinecraftServer server) {
        DiagnosticSessionStore currentStore = store;
        if (server != activeServer || currentStore == null) {
            return;
        }
        currentTick = server.getTickCount();
        long started = tickStartedNanos;
        long elapsedNanos = started == 0L ? 0L : Math.max(0L, System.nanoTime() - started);
        currentStore.recordTick(elapsedNanos);

        if (elapsedNanos >= 100_000_000L
                && (elapsedNanos >= 250_000_000L
                        || lastSlowTickRecord == Long.MIN_VALUE
                        || currentTick - lastSlowTickRecord >= SLOW_TICK_RECORD_INTERVAL_TICKS)) {
            lastSlowTickRecord = currentTick;
            DiagnosticSeverity severity = elapsedNanos >= 1_000_000_000L
                    ? DiagnosticSeverity.ERROR : DiagnosticSeverity.WARNING;
            record(severity, "performance", "slow_server_tick", fields(
                    "duration_ms", String.format(Locale.ROOT, "%.3f", elapsedNanos / 1_000_000.0D),
                    "online_players", server.getPlayerCount()));
        }

        if (currentTick % ENTITY_SCAN_INTERVAL_TICKS == 0L) {
            scanTrackedEntities(server);
        }
        if (currentTick % SNAPSHOT_INTERVAL_TICKS == 0L) {
            recordStateSnapshot(server, "periodic");
        }
        if (currentTick % FLUSH_INTERVAL_TICKS == 0L) {
            tryIo(currentStore::flushEvents, "periodic_flush");
        }
    }

    public static void record(
            DiagnosticSeverity severity,
            String category,
            String code,
            Map<String, String> fields) {
        DiagnosticSessionStore currentStore = store;
        if (currentStore == null) {
            return;
        }
        try {
            boolean written = currentStore.record(
                    currentTick,
                    severity == null ? DiagnosticSeverity.INFO : severity,
                    normalizeToken(category, "unknown"),
                    normalizeToken(code, "unknown"),
                    fields == null ? Map.of() : new TreeMap<>(fields));
            if (!written && internalFailure.isEmpty()) {
                internalFailure = "Diagnostic capacity reached; additional events are counted but not written";
            }
        } catch (Exception exception) {
            reportInternalFailure("record", exception);
        }
    }

    public static void recordForPlayer(
            ServerPlayer player,
            DiagnosticSeverity severity,
            String category,
            String code,
            Map<String, String> fields) {
        Map<String, String> context = new TreeMap<>();
        if (fields != null) {
            context.putAll(fields);
        }
        if (player != null) {
            context.put("player", playerAlias(player));
            context.put("dimension", player.serverLevel().dimension().location().toString());
            context.put("position", position(player.blockPosition()));
            context.put("game_mode", player.gameMode.getGameModeForPlayer().getName());
        }
        record(severity, category, code, context);
    }

    public static void recordEventOutcome(
            ServerPlayer player,
            String lane,
            String eventId,
            String outcome,
            Map<String, String> details) {
        Map<String, String> fields = new TreeMap<>();
        if (details != null) {
            fields.putAll(details);
        }
        fields.put("lane", normalizeToken(lane, "unknown"));
        fields.put("event_id", normalizeToken(eventId, "none"));
        fields.put("outcome", normalizeToken(outcome, "unknown"));
        recordForPlayer(
                player,
                "failed".equals(outcome) ? DiagnosticSeverity.WARNING : DiagnosticSeverity.INFO,
                "event",
                "event_outcome",
                fields);
    }

    /** Records the meaningful lifecycle of a Special without exposing raw player or entity identities. */
    public static void recordSpecialLifecycle(
            ServerPlayer player,
            Entity entity,
            String specialId,
            String stage,
            String reason,
            DiagnosticSeverity severity,
            Map<String, String> details) {
        if (entity == null) {
            return;
        }
        Map<String, String> context = new TreeMap<>(entityContext(entity));
        String entityPosition = context.remove("position");
        context.put("entity_position", entityPosition == null ? "unknown" : entityPosition);
        context.put("special_id", normalizeToken(specialId, specialId(entity)));
        context.put("stage", normalizeToken(stage, "unknown"));
        context.put("reason", normalizeToken(reason, "unknown"));
        if (details != null) {
            context.putAll(details);
        }
        if (entity instanceof Mob mob) {
            context.put("persistence_required", String.valueOf(mob.isPersistenceRequired()));
        }
        if (player != null) {
            context.put("distance_to_player", formatDouble(entity.distanceTo(player)));
            boolean sameDimension = player.level() == entity.level();
            context.put("same_dimension", String.valueOf(sameDimension));
            context.put("direct_line_of_sight", String.valueOf(sameDimension && player.hasLineOfSight(entity)));
        }
        recordForPlayer(
                player,
                severity == null ? DiagnosticSeverity.INFO : severity,
                "special",
                "special_lifecycle",
                context);
    }

    public static void specialTrackingChanged(ServerPlayer player, Entity entity, boolean tracking) {
        if (player == null || entity == null || !UncannyEntityRegistry.isSpecialEntity(entity.getType())) {
            return;
        }
        Map<String, String> context = new TreeMap<>(entityContext(entity));
        String entityPosition = context.remove("position");
        context.put("entity_position", entityPosition == null ? "unknown" : entityPosition);
        context.put("special_id", specialId(entity));
        context.put("distance_to_player", formatDouble(entity.distanceTo(player)));
        context.put("direct_line_of_sight", String.valueOf(
                player.level() == entity.level() && player.hasLineOfSight(entity)));
        recordForPlayer(
                player,
                DiagnosticSeverity.INFO,
                "network",
                tracking ? "special_client_tracking_started" : "special_client_tracking_stopped",
                context);
    }

    /** Records whether the server actually accepted a Special into the level. */
    public static void specialSpawnResult(
            ServerPlayer player,
            Entity entity,
            boolean added,
            String route) {
        if (entity == null || !UncannyEntityRegistry.isSpecialEntity(entity.getType())) {
            return;
        }
        recordSpecialLifecycle(
                player,
                entity,
                null,
                added ? "spawned" : "spawn_failed",
                added ? "level_accepted" : "level_rejected",
                added ? DiagnosticSeverity.INFO : DiagnosticSeverity.WARNING,
                fields("spawn_route", route));
    }

    public static Map<String, String> fields(Object... keyValues) {
        Map<String, String> result = new TreeMap<>();
        if (keyValues == null) {
            return result;
        }
        if ((keyValues.length & 1) != 0) {
            throw new IllegalArgumentException("Diagnostic fields require key/value pairs");
        }
        for (int index = 0; index < keyValues.length; index += 2) {
            String key = normalizeToken(String.valueOf(keyValues[index]), "field_" + index / 2);
            Object value = keyValues[index + 1];
            result.put(key, value == null ? "null" : String.valueOf(value));
        }
        return result;
    }

    public static String statusText() {
        DiagnosticSessionStore currentStore = store;
        if (currentStore == null) {
            return internalFailure.isEmpty()
                    ? "Diagnostics inactive: no server session is running."
                    : "Diagnostics inactive; internal failure: " + internalFailure;
        }
        DiagnosticSessionStore.Status status = currentStore.status();
        String capacity = status.capacityReached() ? ", CAPACITY REACHED" : "";
        String failure = internalFailure.isEmpty() ? "" : ", internalFailure=" + internalFailure;
        return "session=" + status.sessionId()
                + ", events=" + status.eventCount()
                + ", warnings=" + status.warningCount()
                + ", errors=" + status.errorCount()
                + ", dropped=" + status.droppedEventCount()
                + ", size=" + humanBytes(status.eventBytes())
                + capacity + failure
                + ", root=" + status.root();
    }

    public static boolean checkpoint(MinecraftServer server, String reason) {
        DiagnosticSessionStore currentStore = store;
        if (currentStore == null || server != activeServer) {
            return false;
        }
        recordStateSnapshot(server, reason == null ? "manual" : reason);
        try {
            currentStore.checkpoint(reason == null ? "manual" : reason, true);
            return true;
        } catch (IOException exception) {
            reportInternalFailure("checkpoint", exception);
            return false;
        }
    }

    public static Path createBundle(MinecraftServer server) throws IOException {
        DiagnosticSessionStore currentStore = requireActive(server);
        record(DiagnosticSeverity.INFO, "operator", "diagnostic_bundle_requested", Map.of());
        return currentStore.createBundle();
    }

    public static DiagnosticSessionStore.PurgeResult purge(MinecraftServer server) throws IOException {
        DiagnosticSessionStore currentStore = requireActive(server);
        DiagnosticSessionStore.PurgeResult result = currentStore.purgeClosedSessionsAndBundles();
        record(DiagnosticSeverity.INFO, "operator", "diagnostic_history_purged", fields(
                "removed_sessions", result.removedSessions(),
                "removed_bundles", result.removedBundles()));
        currentStore.checkpoint("history_purged", true);
        return result;
    }

    public static List<DiagnosticSessionStore.SessionInfo> listSessions(MinecraftServer server) throws IOException {
        return requireActive(server).listSessions();
    }

    public static void mark(ServerPlayer player, String note) {
        String normalized = note == null ? "" : note.strip().replaceAll("[\\r\\n\\t]+", " ");
        if (normalized.length() > 240) {
            normalized = normalized.substring(0, 240);
        }
        recordForPlayer(player, DiagnosticSeverity.INFO, "operator", "manual_observation", Map.of(
                "note", normalized.isBlank() ? "(empty)" : normalized));
        DiagnosticSessionStore currentStore = store;
        if (currentStore != null) {
            tryIo(currentStore::flushEvents, "manual_mark_flush");
        }
    }

    public static void playerJoined(ServerPlayer player) {
        recordForPlayer(player, DiagnosticSeverity.INFO, "player", "player_joined", fields(
                "online_players", player.getServer() == null ? -1 : player.getServer().getPlayerCount(),
                "health", formatDouble(player.getHealth())));
    }

    public static void playerLeft(ServerPlayer player) {
        recordForPlayer(player, DiagnosticSeverity.INFO, "player", "player_left", fields(
                "health", formatDouble(player.getHealth()),
                "alive", player.isAlive()));
        CLIENT_REPORT_WINDOWS.remove(player.getUUID());
        CLIENT_SYNC_MISMATCHES.remove(player.getUUID());
    }

    public static void playerChangedDimension(ServerPlayer player, String from, String to) {
        recordForPlayer(player, DiagnosticSeverity.INFO, "player", "dimension_changed", fields(
                "from", from,
                "to", to));
    }

    public static void levelSaved(ServerLevel level) {
        if (level == null || level.getServer() != activeServer) {
            return;
        }
        record(DiagnosticSeverity.INFO, "persistence", "level_saved", fields(
                "dimension", level.dimension().location(),
                "game_time", level.getGameTime(),
                "day_time", level.getDayTime(),
                "loaded_chunks", level.getChunkSource().getLoadedChunksCount()));
        if (level == level.getServer().overworld()) {
            checkpoint(level.getServer(), "world_save");
        }
    }

    public static void livingDeath(LivingEntity living, String damageType, Entity causingEntity) {
        if (!(living.level() instanceof ServerLevel) || (!(living instanceof ServerPlayer) && !isTracked(living))) {
            return;
        }
        Map<String, String> fields = new TreeMap<>(entityContext(living));
        fields.put("damage_type", damageType == null ? "unknown" : damageType);
        fields.put("causing_entity", causingEntity == null ? "none" : entityTypeId(causingEntity));
        fields.put("causing_entity_alias", causingEntity == null ? "none" : entityAlias(causingEntity));
        if (living instanceof ServerPlayer player) {
            fields.put("player", playerAlias(player));
        }
        record(DiagnosticSeverity.INFO, "entity", "living_death", fields);
    }

    public static void entityJoined(Entity entity) {
        if (!isTracked(entity)) {
            return;
        }
        record(DiagnosticSeverity.INFO, "entity", "tracked_entity_joined", entityContext(entity));
    }

    public static void entityLeft(Entity entity) {
        if (!isTracked(entity)) {
            return;
        }
        Map<String, String> fields = new TreeMap<>(entityContext(entity));
        fields.put("removal_reason", entity.getRemovalReason() == null
                ? "unknown" : entity.getRemovalReason().toString());
        record(DiagnosticSeverity.INFO, "entity", "tracked_entity_left", fields);
        MOTION_SAMPLES.remove(entity.getUUID());
    }

    public static void mentalSoundSent(
            ServerPlayer player,
            String soundId,
            String source,
            float volume,
            float pitch,
            int maximumDurationTicks) {
        recordForPlayer(player, DiagnosticSeverity.INFO, "sound", "mental_sound_sent", fields(
                "sound_id", soundId,
                "source", source,
                "volume", formatDouble(volume),
                "pitch", formatDouble(pitch),
                "maximum_duration_ticks", maximumDurationTicks,
                "delivery", "private_non_positional"));
    }

    public static void physicalSoundPlayed(
            Entity sourceEntity,
            String soundId,
            String source,
            float volume,
            float pitch) {
        if (sourceEntity == null) {
            return;
        }
        Map<String, String> fields = new TreeMap<>(entityContext(sourceEntity));
        fields.put("sound_id", soundId);
        fields.put("source", source);
        fields.put("volume", formatDouble(volume));
        fields.put("pitch", formatDouble(pitch));
        fields.put("delivery", "shared_positional");
        record(DiagnosticSeverity.INFO, "sound", "physical_entity_sound_played", fields);
    }

    public static void uncannyCommand(MinecraftServer server, String rawCommand) {
        if (server != activeServer || rawCommand == null) {
            return;
        }
        String command = rawCommand.startsWith("/") ? rawCommand.substring(1) : rawCommand;
        if (!command.startsWith("uncanny")) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            command = command.replace(player.getGameProfile().getName(), playerAlias(player));
        }
        if (command.startsWith("uncanny diagnostics mark")) {
            command = "uncanny diagnostics mark <redacted; recorded as manual_observation>";
        }
        if (command.length() > 320) {
            command = command.substring(0, 320) + "...[truncated]";
        }
        record(DiagnosticSeverity.INFO, "command", "uncanny_command", Map.of("command", command));
    }

    public static synchronized void clientReport(
            ServerPlayer player,
            String rawSeverity,
            String rawCode,
            String rawMessage,
            String rawContext) {
        if (player == null || player.getServer() != activeServer || store == null) {
            return;
        }
        long now = player.getServer().getTickCount();
        ClientReportWindow window = CLIENT_REPORT_WINDOWS.get(player.getUUID());
        if (window == null || now - window.startedTick() >= 1_200L) {
            window = new ClientReportWindow(now, 0, false);
        }
        if (window.reportCount() >= 120) {
            if (!window.limitReported()) {
                recordForPlayer(player, DiagnosticSeverity.WARNING, "client", "client_report_rate_limited", fields(
                        "limit", 120,
                        "window_ticks", 1_200));
                window = new ClientReportWindow(window.startedTick(), window.reportCount(), true);
            }
            CLIENT_REPORT_WINDOWS.put(player.getUUID(), window);
            return;
        }
        CLIENT_REPORT_WINDOWS.put(player.getUUID(), new ClientReportWindow(
                window.startedTick(), window.reportCount() + 1, window.limitReported()));

        DiagnosticSeverity severity;
        try {
            severity = DiagnosticSeverity.valueOf(rawSeverity == null
                    ? "INFO" : rawSeverity.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            severity = DiagnosticSeverity.INFO;
        }
        String message = sanitizeClientText(player.getServer(), rawMessage, 8_192);
        String context = sanitizeClientText(player.getServer(), rawContext, 2_048);
        recordForPlayer(player, severity, "client", "client_report", fields(
                "client_code", normalizeToken(rawCode, "unknown"),
                "message", message,
                "context", context));
        if ("client_snapshot".equals(normalizeToken(rawCode, "unknown"))) {
            checkClientSync(player, context);
        }
    }

    private static void recordStateSnapshot(MinecraftServer server, String reason) {
        try {
            UncannyWorldState state = UncannyWorldState.get(server);
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            Map<String, String> fields = fields(
                    "reason", reason,
                    "phase", state.getCurrentPhaseIndex(),
                    "phase_progress", formatDouble(state.getProgressToNextPhase()),
                    "system_enabled", state.isSystemEnabled(),
                    "campaign", UncannyCampaignDirector.debugSummary(state),
                    "campaign_elapsed_ticks", state.getCampaignElapsedTicks(),
                    "campaign_length_days", UncannyCampaignDirector.campaignLengthDays(),
                    "event_profile", UncannyConfig.EVENT_INTENSITY_PROFILE.get(),
                    "danger_level", UncannyConfig.EVENT_DANGER_LEVEL.get(),
                    "weather", state.getActiveWeatherEventId(),
                    "weather_end_tick", state.getWeatherEventEndTick(),
                    "tension_builder_end_tick", state.getTensionBuilderEndTick(),
                    "pending_grand_event_tick", state.getTensionBuilderPendingGrandEventStartTick(),
                    "online_players", server.getPlayerCount(),
                    "loaded_levels", count(server.getAllLevels()),
                    "tracked_structure_markers", state.getStructureMarkers().size(),
                    "tracked_player_lights", state.getPlayerPlacedLights().size(),
                    "pending_ferryman_encounters", state.getPendingFerrymanEncounterCount(),
                    "used_memory_bytes", usedMemory,
                    "allocated_memory_bytes", runtime.totalMemory(),
                    "maximum_memory_bytes", runtime.maxMemory());
            flattenCounts(fields, "paranoia_", UncannyParanoiaEventSystem.diagnosticStateCounts());
            flattenCounts(fields, "native_", MinecraftNativeAnomalySystem.diagnosticStateCounts());
            flattenCounts(fields, "special_", ApprovedSpecialSystem.diagnosticStateCounts());
            record(DiagnosticSeverity.INFO, "snapshot", "world_state", fields);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                recordForPlayer(player, DiagnosticSeverity.INFO, "snapshot", "player_scheduler_state", Map.of(
                        "scheduler", UncannyParanoiaEventSystem.getAutoEventDebugReport(player)));
            }
            checkStateInvariants(server, state);
        } catch (Exception exception) {
            record(DiagnosticSeverity.ERROR, "diagnostics", "snapshot_failed", fields(
                    "exception", exception.getClass().getName(),
                    "message", safeMessage(exception)));
        }
    }

    private static void scanTrackedEntities(MinecraftServer server) {
        Set<UUID> seen = new HashSet<>();
        Map<String, Integer> counts = new TreeMap<>();
        int loadedChunks = 0;
        int trackedEntities = 0;
        for (ServerLevel level : server.getAllLevels()) {
            loadedChunks += level.getChunkSource().getLoadedChunksCount();
            for (Entity entity : level.getAllEntities()) {
                if (!isTracked(entity)) {
                    continue;
                }
                trackedEntities++;
                seen.add(entity.getUUID());
                String key = trackedIdentity(entity);
                counts.merge(key, 1, Integer::sum);
                validateEntity(entity);
                trackNavigation(entity);
            }
        }
        MOTION_SAMPLES.keySet().removeIf(uuid -> !seen.contains(uuid));
        record(DiagnosticSeverity.INFO, "snapshot", "entity_census", fields(
                "tracked_entity_count", trackedEntities,
                "loaded_chunk_count", loadedChunks,
                "counts", counts.toString()));
    }

    private static void validateEntity(Entity entity) {
        if (!Double.isFinite(entity.getX()) || !Double.isFinite(entity.getY()) || !Double.isFinite(entity.getZ())) {
            anomaly("entity_non_finite_position:" + entityAlias(entity), DiagnosticSeverity.ERROR,
                    "entity_non_finite_position", entityContext(entity));
        }
        if (entity instanceof LivingEntity living) {
            float health = living.getHealth();
            float maximum = living.getMaxHealth();
            if (!Float.isFinite(health) || !Float.isFinite(maximum) || health < 0.0F || maximum <= 0.0F
                    || health > maximum + 0.01F) {
                Map<String, String> fields = new TreeMap<>(entityContext(entity));
                fields.put("health", formatDouble(health));
                fields.put("maximum_health", formatDouble(maximum));
                anomaly("entity_invalid_health:" + entityAlias(entity), DiagnosticSeverity.ERROR,
                        "entity_invalid_health", fields);
            }
        }
    }

    private static void trackNavigation(Entity entity) {
        if (!(entity instanceof Mob mob) || !mob.getNavigation().isInProgress() || !entity.isAlive()) {
            MOTION_SAMPLES.remove(entity.getUUID());
            return;
        }
        MotionSample previous = MOTION_SAMPLES.get(entity.getUUID());
        if (previous != null && entity.tickCount <= previous.entityTickCount()) {
            // Entity objects remain enumerable while their chunk is not ticking. A frozen tick counter
            // is therefore an unloaded-chunk observation, not evidence that pathfinding is blocked.
            MOTION_SAMPLES.remove(entity.getUUID());
            return;
        }
        int stationarySamples = previous != null && previous.position().distSqr(entity.blockPosition()) <= 0.25D
                ? previous.stationarySamples() + 1 : 0;
        MOTION_SAMPLES.put(entity.getUUID(), new MotionSample(entity.blockPosition(), stationarySamples, entity.tickCount));
        if (stationarySamples >= 3) {
            Map<String, String> fields = new TreeMap<>(entityContext(entity));
            fields.put("stationary_samples", String.valueOf(stationarySamples));
            fields.put("has_target", String.valueOf(mob.getTarget() != null));
            fields.put("target_type", mob.getTarget() == null ? "none" : entityTypeId(mob.getTarget()));
            fields.put("entity_tick_delta", String.valueOf(
                    previous == null ? 0 : entity.tickCount - previous.entityTickCount()));
            ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            boolean modOwnedNavigation = entityTypeId != null
                    && EchoOfTheVoid.MODID.equals(entityTypeId.getNamespace());
            anomaly(
                    "entity_navigation_stall:" + entityAlias(entity),
                    modOwnedNavigation ? DiagnosticSeverity.WARNING : DiagnosticSeverity.INFO,
                    modOwnedNavigation ? "entity_navigation_stall" : "variant_navigation_stationary",
                    fields);
        }
    }

    private static void checkStateInvariants(MinecraftServer server, UncannyWorldState state) {
        double progress = state.getProgressToNextPhase();
        if (!Double.isFinite(progress) || progress < 0.0D || progress > 1.0D) {
            anomaly("phase_progress", DiagnosticSeverity.ERROR, "invalid_phase_progress", fields(
                    "value", progress));
        }
        if (state.getCampaignElapsedTicks() < 0L || state.getCampaignBeat() == null || state.getCampaignBeat().isBlank()) {
            anomaly("campaign_state", DiagnosticSeverity.ERROR, "invalid_campaign_state", fields(
                    "elapsed_ticks", state.getCampaignElapsedTicks(),
                    "beat", state.getCampaignBeat()));
        }
        boolean weatherActive = state.getActiveWeatherEventId() != null
                && !state.getActiveWeatherEventId().isBlank();
        if (weatherActive && state.getWeatherEventEndTick() == Long.MIN_VALUE) {
            anomaly("weather_missing_end", DiagnosticSeverity.ERROR, "weather_missing_end_tick", fields(
                    "weather", state.getActiveWeatherEventId()));
        }
        if (weatherActive && state.getWeatherEventEndTick() + 200L < server.getTickCount()) {
            anomaly("weather_overdue", DiagnosticSeverity.WARNING, "weather_end_overdue", fields(
                    "weather", state.getActiveWeatherEventId(),
                    "end_tick", state.getWeatherEventEndTick(),
                    "current_tick", server.getTickCount()));
        }
        if (state.getTensionBuilderPendingGrandEventStartTick() != Long.MIN_VALUE
                && state.getTensionBuilderPendingGrandEventDimension().isBlank()) {
            anomaly("grand_event_dimension", DiagnosticSeverity.ERROR,
                    "pending_grand_event_missing_dimension", fields(
                            "start_tick", state.getTensionBuilderPendingGrandEventStartTick()));
        }
    }

    private static void anomaly(
            String identity,
            DiagnosticSeverity severity,
            String code,
            Map<String, String> fields) {
        long last = LAST_ANOMALY_TICKS.getOrDefault(identity, Long.MIN_VALUE);
        if (last != Long.MIN_VALUE && currentTick - last < ANOMALY_REPEAT_INTERVAL_TICKS) {
            return;
        }
        LAST_ANOMALY_TICKS.put(identity, currentTick);
        record(severity, "invariant", code, fields);
    }

    private static boolean isTracked(Entity entity) {
        if (entity == null) {
            return false;
        }
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return EchoOfTheVoid.MODID.equals(typeId.getNamespace())
                || !ApprovedVanillaVariantSystem.variantId(entity).isBlank()
                || entity.getPersistentData().getBoolean(LEGACY_PASSIVE_ENABLED);
    }

    private static String trackedIdentity(Entity entity) {
        String approved = ApprovedVanillaVariantSystem.variantId(entity);
        if (!approved.isBlank()) {
            return approved;
        }
        String replacement = ReplacementVariantExpansionSystem.variantId(entity);
        if (!replacement.isBlank()) {
            return replacement;
        }
        if (entity.getPersistentData().getBoolean(LEGACY_PASSIVE_ENABLED)) {
            return entity.getPersistentData().getString(LEGACY_PASSIVE_TYPE)
                    + "_v" + entity.getPersistentData().getInt(LEGACY_PASSIVE_VARIANT);
        }
        return entityTypeId(entity);
    }

    private static String specialId(Entity entity) {
        ResourceLocation id = entity == null ? null : BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null) {
            return "unknown";
        }
        String path = id.getPath();
        return path.startsWith("uncanny_") ? path.substring("uncanny_".length()) : path;
    }

    private static Map<String, String> entityContext(Entity entity) {
        Map<String, String> fields = fields(
                "entity", entityAlias(entity),
                "runtime_entity_id", entity.getId(),
                "type", entityTypeId(entity),
                "identity", trackedIdentity(entity),
                "dimension", entity.level().dimension().location(),
                "position", position(entity.blockPosition()),
                "tick_count", entity.tickCount,
                "alive", entity.isAlive(),
                "on_ground", entity.onGround());
        if (entity instanceof Mob mob) {
            fields.put("no_ai", String.valueOf(mob.isNoAi()));
            fields.put("navigation_active", String.valueOf(mob.getNavigation().isInProgress()));
        }
        return fields;
    }

    private static Map<String, String> environment(MinecraftServer server) {
        String modVersion = ModList.get()
                .getModContainerById(EchoOfTheVoid.MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
        List<String> mods = new ArrayList<>();
        ModList.get().getMods().forEach(mod -> mods.add(mod.getModId() + "@" + mod.getVersion()));
        mods.sort(String::compareTo);
        return fields(
                "mod_id", EchoOfTheVoid.MODID,
                "mod_version", modVersion,
                "minecraft_version", SharedConstants.getCurrentVersion().getName(),
                "java_version", System.getProperty("java.version", "unknown"),
                "java_vendor", System.getProperty("java.vendor", "unknown"),
                "operating_system", System.getProperty("os.name", "unknown"),
                "os_version", System.getProperty("os.version", "unknown"),
                "architecture", System.getProperty("os.arch", "unknown"),
                "dedicated_server", server.isDedicatedServer(),
                "installed_mods", String.join(",", mods),
                "recorder_started_utc", Instant.now());
    }

    private static DiagnosticSessionStore requireActive(MinecraftServer server) throws IOException {
        DiagnosticSessionStore currentStore = store;
        if (currentStore == null || server == null || server != activeServer) {
            throw new IOException("No active diagnostic session for this server");
        }
        return currentStore;
    }

    private static void flattenCounts(Map<String, String> destination, String prefix, Map<String, Integer> counts) {
        counts.forEach((key, value) -> destination.put(prefix + normalizeToken(key, "unknown"), String.valueOf(value)));
    }

    private static String playerAlias(ServerPlayer player) {
        String alias = PLAYER_ALIASES.computeIfAbsent(
                player.getUUID(), ignored -> "P" + (PLAYER_ALIASES.size() + 1));
        PLAYER_NAME_ALIASES.putIfAbsent(player.getGameProfile().getName(), alias);
        return alias;
    }

    private static String entityAlias(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return playerAlias(player);
        }
        return ENTITY_ALIASES.computeIfAbsent(
                entity.getUUID(), ignored -> "E" + (ENTITY_ALIASES.size() + 1));
    }

    private static String entityTypeId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    private static String position(BlockPos position) {
        return position.getX() + "," + position.getY() + "," + position.getZ();
    }

    private static String normalizeToken(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.:-]+", "_");
        return normalized.length() > 96 ? normalized.substring(0, 96) : normalized;
    }

    private static String safeMessage(Exception exception) {
        return exception.getMessage() == null ? "(no message)" : exception.getMessage();
    }

    private static String sanitizeClientText(MinecraftServer server, String value, int maximumLength) {
        String sanitized = sanitizeCapturedText(value);
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            sanitized = sanitized.replace(online.getGameProfile().getName(), playerAlias(online));
        }
        sanitized = sanitized.replaceAll("[\\r\\n]{4,}", "\n\n\n");
        return sanitized.length() <= maximumLength
                ? sanitized : sanitized.substring(0, maximumLength) + "...[truncated]";
    }

    static String sanitizeCapturedText(String value) {
        String sanitized = value == null ? "" : value;
        for (String property : new String[] {"user.home", "user.dir"}) {
            String path = System.getProperty(property, "");
            if (!path.isBlank()) {
                String placeholder = "<" + property.replace('.', '-') + ">";
                sanitized = sanitized.replace(path, placeholder);
                sanitized = sanitized.replace(path.replace('\\', '/'), placeholder);
            }
        }
        for (Map.Entry<String, String> player : PLAYER_NAME_ALIASES.entrySet()) {
            sanitized = sanitized.replace(player.getKey(), player.getValue());
        }
        sanitized = sanitized.replaceAll(
                "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
                "<uuid>");
        return sanitized.replaceAll(
                "(?<![0-9])(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?::[0-9]{1,5})?",
                "<network-address>");
    }

    private static void checkClientSync(ServerPlayer player, String context) {
        Map<String, String> values = parseClientContext(context);
        String actualPhase = values.get("synced_phase");
        String actualWeather = values.get("synced_weather");
        UncannyWorldState state = UncannyWorldState.get(player.getServer());
        String expectedPhase = String.valueOf(state.getCurrentPhaseIndex());
        String expectedWeather = state.getActiveWeatherEventId() == null ? "" : state.getActiveWeatherEventId();
        boolean mismatch = actualPhase == null || actualWeather == null
                || !expectedPhase.equals(actualPhase)
                || !expectedWeather.equals(actualWeather);
        if (!mismatch) {
            CLIENT_SYNC_MISMATCHES.remove(player.getUUID());
            return;
        }

        String signature = expectedPhase + "/" + actualPhase + "/" + expectedWeather + "/" + actualWeather;
        ClientSyncMismatch previous = CLIENT_SYNC_MISMATCHES.get(player.getUUID());
        int consecutive = previous != null && previous.signature().equals(signature)
                ? previous.consecutiveSamples() + 1 : 1;
        CLIENT_SYNC_MISMATCHES.put(player.getUUID(), new ClientSyncMismatch(signature, consecutive));
        if (consecutive >= 2) {
            recordForPlayer(player, DiagnosticSeverity.WARNING, "network", "client_state_desynchronized", fields(
                    "expected_phase", expectedPhase,
                    "client_phase", actualPhase,
                    "expected_weather", expectedWeather,
                    "client_weather", actualWeather,
                    "consecutive_samples", consecutive));
        }
    }

    private static Map<String, String> parseClientContext(String context) {
        Map<String, String> values = new HashMap<>();
        if (context == null) {
            return values;
        }
        for (String part : context.split(";")) {
            int separator = part.indexOf('=');
            if (separator > 0) {
                values.put(part.substring(0, separator), part.substring(separator + 1));
            }
        }
        return values;
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String humanBytes(long bytes) {
        if (bytes < 1_024L) {
            return bytes + " B";
        }
        if (bytes < 1_048_576L) {
            return String.format(Locale.ROOT, "%.1f KiB", bytes / 1_024.0D);
        }
        return String.format(Locale.ROOT, "%.1f MiB", bytes / 1_048_576.0D);
    }

    private static int count(Iterable<?> values) {
        int count = 0;
        for (Object ignored : values) {
            count++;
        }
        return count;
    }

    private static void tryIo(IoAction action, String operation) {
        try {
            action.run();
        } catch (Exception exception) {
            reportInternalFailure(operation, exception);
        }
    }

    private static synchronized void reportInternalFailure(String operation, Exception exception) {
        internalFailure = operation + " failed: " + exception.getClass().getSimpleName() + ": " + safeMessage(exception);
        System.err.println("[EchoOfTheVoid/Diagnostics] " + internalFailure);
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }

    private record MotionSample(BlockPos position, int stationarySamples, int entityTickCount) {
    }

    private record ClientReportWindow(long startedTick, int reportCount, boolean limitReported) {
    }

    private record ClientSyncMismatch(String signature, int consecutiveSamples) {
    }
}
