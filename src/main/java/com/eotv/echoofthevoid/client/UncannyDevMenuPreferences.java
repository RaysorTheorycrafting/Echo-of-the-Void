package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.dev.UncannyDevCatalog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.neoforged.fml.loading.FMLPaths;

/** Local developer ergonomics only; never read by gameplay systems or synchronized to a server. */
final class UncannyDevMenuPreferences {
    private static final int MAX_RECENT = 16;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get()
            .resolve("echoofthevoid")
            .resolve("uncanny_dev_menu_preferences.json");
    private static final Set<String> FAVORITES = new LinkedHashSet<>();
    private static final List<String> RECENT = new ArrayList<>();
    private static boolean loaded;
    private static String lastEntryId = "";
    private static String lastTargetName = "";
    private static int spawnDistance = 4;

    private UncannyDevMenuPreferences() {
    }

    static synchronized Set<String> favorites() {
        ensureLoaded();
        return Set.copyOf(FAVORITES);
    }

    static synchronized boolean isFavorite(String entryId) {
        ensureLoaded();
        return FAVORITES.contains(normalize(entryId));
    }

    static synchronized void toggleFavorite(String entryId) {
        ensureLoaded();
        String normalized = normalize(entryId);
        if (normalized.isEmpty() || UncannyDevCatalog.byId(normalized) == null) {
            return;
        }
        if (!FAVORITES.remove(normalized)) {
            FAVORITES.add(normalized);
        }
        save();
    }

    static synchronized List<String> recentIds() {
        ensureLoaded();
        return List.copyOf(RECENT);
    }

    static synchronized void recordRun(String entryId, String targetName, int distance) {
        ensureLoaded();
        String normalized = normalize(entryId);
        if (UncannyDevCatalog.byId(normalized) == null) {
            return;
        }
        RECENT.remove(normalized);
        RECENT.add(0, normalized);
        while (RECENT.size() > MAX_RECENT) {
            RECENT.remove(RECENT.size() - 1);
        }
        lastEntryId = normalized;
        lastTargetName = targetName == null ? "" : targetName.trim();
        spawnDistance = Math.max(2, Math.min(24, distance));
        save();
    }

    static synchronized String lastEntryId() {
        ensureLoaded();
        return lastEntryId;
    }

    static synchronized String lastTargetName() {
        ensureLoaded();
        return lastTargetName;
    }

    static synchronized int spawnDistance() {
        ensureLoaded();
        return spawnDistance;
    }

    static synchronized void storeTargetAndDistance(String targetName, int distance) {
        ensureLoaded();
        lastTargetName = targetName == null ? "" : targetName.trim();
        spawnDistance = Math.max(2, Math.min(24, distance));
        save();
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.exists(PATH)) {
            return;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(PATH, StandardCharsets.UTF_8));
            if (!(parsed instanceof JsonObject root)) {
                return;
            }
            readIds(root.getAsJsonArray("favorites"), FAVORITES, Integer.MAX_VALUE);
            readIds(root.getAsJsonArray("recent"), RECENT, MAX_RECENT);
            if (root.has("lastEntryId")) {
                String id = normalize(root.get("lastEntryId").getAsString());
                lastEntryId = UncannyDevCatalog.byId(id) == null ? "" : id;
            }
            if (root.has("lastTargetName")) {
                lastTargetName = root.get("lastTargetName").getAsString().trim();
            }
            if (root.has("spawnDistance")) {
                spawnDistance = Math.max(2, Math.min(24, root.get("spawnDistance").getAsInt()));
            }
        } catch (Exception exception) {
            EchoOfTheVoid.LOGGER.warn("Failed to load dev menu preferences: {}", PATH, exception);
        }
    }

    private static void readIds(JsonArray array, java.util.Collection<String> destination, int maximum) {
        if (array == null) {
            return;
        }
        for (JsonElement element : array) {
            if (destination.size() >= maximum || !element.isJsonPrimitive()) {
                break;
            }
            String id = normalize(element.getAsString());
            if (UncannyDevCatalog.byId(id) != null && !destination.contains(id)) {
                destination.add(id);
            }
        }
    }

    private static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            JsonObject root = new JsonObject();
            JsonArray favorites = new JsonArray();
            FAVORITES.stream().sorted().forEach(favorites::add);
            JsonArray recent = new JsonArray();
            RECENT.forEach(recent::add);
            root.add("favorites", favorites);
            root.add("recent", recent);
            root.addProperty("lastEntryId", lastEntryId);
            root.addProperty("lastTargetName", lastTargetName);
            root.addProperty("spawnDistance", spawnDistance);
            Files.writeString(PATH, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            EchoOfTheVoid.LOGGER.warn("Failed to save dev menu preferences: {}", PATH, exception);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
