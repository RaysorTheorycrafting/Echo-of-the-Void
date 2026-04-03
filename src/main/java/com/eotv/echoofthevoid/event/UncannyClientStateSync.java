package com.eotv.echoofthevoid.event;

import com.eotv.echoofthevoid.network.UncannyPhaseSyncPayload;
import com.eotv.echoofthevoid.network.UncannyParanoiaSyncPayload;
import com.eotv.echoofthevoid.network.UncannyWeatherSyncPayload;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class UncannyClientStateSync {
    private static final Map<UUID, Integer> LAST_PHASE_SENT = new ConcurrentHashMap<>();
    private static final Map<UUID, String> LAST_WEATHER_SENT = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_PARANOIA_MASK_SENT = new ConcurrentHashMap<>();

    private static volatile int clientPhaseIndex;
    private static volatile String clientWeatherEventId = "";
    private static volatile boolean clientHunterFogActive;
    private static volatile boolean clientGiantSunActive;

    private UncannyClientStateSync() {
    }

    public static void syncPhase(ServerPlayer player, int phaseIndex) {
        int clamped = clampPhase(phaseIndex);
        UUID uuid = player.getUUID();
        Integer previous = LAST_PHASE_SENT.get(uuid);
        if (previous != null && previous == clamped) {
            return;
        }
        LAST_PHASE_SENT.put(uuid, clamped);
        PacketDistributor.sendToPlayer(player, new UncannyPhaseSyncPayload(clamped));
    }

    public static void syncWeather(ServerPlayer player, String weatherEventId) {
        String normalized = normalizeWeather(weatherEventId);
        UUID uuid = player.getUUID();
        String previous = LAST_WEATHER_SENT.get(uuid);
        if (normalized.equals(previous)) {
            return;
        }
        LAST_WEATHER_SENT.put(uuid, normalized);
        PacketDistributor.sendToPlayer(player, new UncannyWeatherSyncPayload(normalized));
    }

    public static void clearWeather(ServerPlayer player) {
        syncWeather(player, "");
    }

    public static void syncParanoiaState(ServerPlayer player, boolean hunterFogActive, boolean giantSunActive) {
        int mask = (hunterFogActive ? 1 : 0) | (giantSunActive ? 2 : 0);
        UUID uuid = player.getUUID();
        Integer previous = LAST_PARANOIA_MASK_SENT.get(uuid);
        if (previous != null && previous == mask) {
            return;
        }
        LAST_PARANOIA_MASK_SENT.put(uuid, mask);
        PacketDistributor.sendToPlayer(player, new UncannyParanoiaSyncPayload(hunterFogActive, giantSunActive));
    }

    public static void clearPlayerCache(ServerPlayer player) {
        UUID uuid = player.getUUID();
        LAST_PHASE_SENT.remove(uuid);
        LAST_WEATHER_SENT.remove(uuid);
        LAST_PARANOIA_MASK_SENT.remove(uuid);
    }

    public static void applyClientPhase(int phaseIndex) {
        clientPhaseIndex = clampPhase(phaseIndex);
    }

    public static void applyClientWeather(String weatherEventId) {
        clientWeatherEventId = normalizeWeather(weatherEventId);
    }

    public static void applyClientParanoiaState(boolean hunterFogActive, boolean giantSunActive) {
        clientHunterFogActive = hunterFogActive;
        clientGiantSunActive = giantSunActive;
    }

    public static int getClientPhaseIndex() {
        return clientPhaseIndex;
    }

    public static String getClientWeatherEventId() {
        return clientWeatherEventId;
    }

    public static boolean isClientWeather(String weatherEventId) {
        return normalizeWeather(weatherEventId).equals(clientWeatherEventId);
    }

    public static boolean isClientHunterFogActive() {
        return clientHunterFogActive;
    }

    public static boolean isClientGiantSunActive() {
        return clientGiantSunActive;
    }

    private static int clampPhase(int phaseIndex) {
        return Math.max(0, Math.min(4, phaseIndex));
    }

    private static String normalizeWeather(String weatherEventId) {
        return weatherEventId == null ? "" : weatherEventId;
    }
}
