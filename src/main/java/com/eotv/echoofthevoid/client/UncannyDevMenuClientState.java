package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.dev.UncannyDevCatalog;
import com.eotv.echoofthevoid.network.UncannyDevMenuActionPayload;
import com.eotv.echoofthevoid.network.UncannyDevMenuQaStatusPayload;
import com.eotv.echoofthevoid.network.UncannyDevMenuResultPayload;
import com.eotv.echoofthevoid.network.UncannyDevMenuRunPayload;
import com.eotv.echoofthevoid.network.UncannyDevMenuSyncPayload;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

public final class UncannyDevMenuClientState {
    private static final Set<String> GREEN_IDS = new HashSet<>();
    private static final Set<String> ORANGE_IDS = new HashSet<>();
    private static UncannyDevMenuResultPayload lastResult;

    private UncannyDevMenuClientState() {
    }

    public static synchronized void applySync(UncannyDevMenuSyncPayload payload) {
        GREEN_IDS.clear();
        ORANGE_IDS.clear();
        parseInto(payload.greenIds(), GREEN_IDS);
        parseInto(payload.orangeIds(), ORANGE_IDS);

        if (payload.openMenu()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                minecraft.setScreen(new UncannyDevMenuScreen());
            }
        } else {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.screen instanceof UncannyDevMenuScreen screen) {
                screen.onServerResult();
            }
        }
    }

    public static synchronized UncannyDevCatalog.QaStatus statusOf(String entryId) {
        String normalized = normalize(entryId);
        if (GREEN_IDS.contains(normalized)) {
            return UncannyDevCatalog.QaStatus.GREEN;
        }
        if (ORANGE_IDS.contains(normalized)) {
            return UncannyDevCatalog.QaStatus.ORANGE;
        }
        return UncannyDevCatalog.QaStatus.GRAY;
    }

    public static synchronized void applyResult(UncannyDevMenuResultPayload payload) {
        lastResult = payload;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.screen instanceof UncannyDevMenuScreen screen) {
            screen.onServerResult();
        }
    }

    public static synchronized UncannyDevMenuResultPayload lastResult() {
        return lastResult;
    }

    public static Set<String> favoriteIds() {
        return UncannyDevMenuPreferences.favorites();
    }

    public static boolean isFavorite(String entryId) {
        return UncannyDevMenuPreferences.isFavorite(entryId);
    }

    public static void toggleFavorite(String entryId) {
        UncannyDevMenuPreferences.toggleFavorite(entryId);
    }

    public static java.util.List<String> recentIds() {
        return UncannyDevMenuPreferences.recentIds();
    }

    public static String lastEntryId() {
        return UncannyDevMenuPreferences.lastEntryId();
    }

    public static String lastTargetName() {
        return UncannyDevMenuPreferences.lastTargetName();
    }

    public static int preferredSpawnDistance() {
        return UncannyDevMenuPreferences.spawnDistance();
    }

    public static void requestTrigger(String entryId) {
        PacketDistributor.sendToServer(new UncannyDevMenuActionPayload(entryId));
    }

    public static void requestRun(String entryId, String targetName, int spawnDistance) {
        UncannyDevMenuPreferences.recordRun(entryId, targetName, spawnDistance);
        PacketDistributor.sendToServer(new UncannyDevMenuRunPayload(
                entryId,
                targetName == null ? "" : targetName,
                Math.max(2, Math.min(24, spawnDistance))));
    }

    public static void storeTargetAndDistance(String targetName, int spawnDistance) {
        UncannyDevMenuPreferences.storeTargetAndDistance(targetName, spawnDistance);
    }

    public static void requestSetGreen(String entryId, boolean green) {
        PacketDistributor.sendToServer(new UncannyDevMenuQaStatusPayload(entryId, green));
    }

    private static void parseInto(String raw, Set<String> destination) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String[] split = raw.split(";");
        for (String part : split) {
            String normalized = normalize(part);
            if (!normalized.isEmpty()) {
                destination.add(normalized);
            }
        }
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
