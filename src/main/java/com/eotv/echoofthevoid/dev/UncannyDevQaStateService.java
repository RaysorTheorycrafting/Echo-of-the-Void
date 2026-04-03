package com.eotv.echoofthevoid.dev;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.network.UncannyDevMenuSyncPayload;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class UncannyDevQaStateService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path QA_STATE_PATH = FMLPaths.CONFIGDIR.get()
            .resolve("echoofthevoid")
            .resolve("uncanny_qa_state.json");

    private static final Set<String> GREEN_IDS = new HashSet<>();
    private static final Map<UUID, Set<String>> SESSION_ORANGE_IDS = new HashMap<>();
    private static boolean loaded;

    private UncannyDevQaStateService() {
    }

    public static synchronized void openMenu(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ensureLoaded();
        syncToPlayer(player, true);
    }

    public static synchronized void handleAction(ServerPlayer player, String entryId) {
        if (player == null || entryId == null) {
            return;
        }
        ensureLoaded();

        String normalized = normalizeId(entryId);
        UncannyDevCatalog.Entry entry = UncannyDevCatalog.byId(normalized);
        if (entry == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Unknown dev entry: " + entryId));
            syncToPlayer(player, false);
            return;
        }

        // Any click makes it orange for this session unless already validated green.
        if (!GREEN_IDS.contains(normalized)) {
            SESSION_ORANGE_IDS.computeIfAbsent(player.getUUID(), uuid -> new HashSet<>()).add(normalized);
        }

        boolean success = UncannyDevActionExecutor.execute(player, entry);
        if (!success) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Dev action failed: " + entry.label()));
        }
        syncToPlayer(player, false);
    }

    public static synchronized void updateStatus(ServerPlayer player, String entryId, boolean validatedGreen) {
        if (player == null || entryId == null) {
            return;
        }
        ensureLoaded();

        String normalized = normalizeId(entryId);
        if (UncannyDevCatalog.byId(normalized) == null) {
            return;
        }

        if (validatedGreen) {
            GREEN_IDS.add(normalized);
            SESSION_ORANGE_IDS.computeIfAbsent(player.getUUID(), uuid -> new HashSet<>()).remove(normalized);
        } else {
            GREEN_IDS.remove(normalized);
            SESSION_ORANGE_IDS.computeIfAbsent(player.getUUID(), uuid -> new HashSet<>()).remove(normalized);
        }
        save();
        syncToPlayer(player, false);
    }

    public static synchronized void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        SESSION_ORANGE_IDS.remove(player.getUUID());
    }

    public static synchronized void syncToPlayer(ServerPlayer player, boolean openMenu) {
        if (player == null) {
            return;
        }
        ensureLoaded();

        Set<String> orange = SESSION_ORANGE_IDS.getOrDefault(player.getUUID(), Collections.emptySet());
        PacketDistributor.sendToPlayer(
                player,
                new UncannyDevMenuSyncPayload(serializeIds(GREEN_IDS), serializeIds(orange), openMenu));
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;

        GREEN_IDS.clear();
        if (!Files.exists(QA_STATE_PATH)) {
            save();
            return;
        }

        try {
            String raw = Files.readString(QA_STATE_PATH, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(raw);
            if (!(parsed instanceof JsonObject object)) {
                return;
            }

            JsonArray green = object.getAsJsonArray("green");
            if (green != null) {
                for (JsonElement element : green) {
                    if (!element.isJsonPrimitive()) {
                        continue;
                    }
                    String id = normalizeId(element.getAsString());
                    if (UncannyDevCatalog.byId(id) != null) {
                        GREEN_IDS.add(id);
                    }
                }
            }
        } catch (Exception exception) {
            EchoOfTheVoid.LOGGER.warn("Failed to load dev QA state: {}", QA_STATE_PATH, exception);
        }
    }

    private static void save() {
        try {
            Files.createDirectories(QA_STATE_PATH.getParent());
            JsonObject root = new JsonObject();
            JsonArray green = new JsonArray();
            GREEN_IDS.stream()
                    .sorted()
                    .forEach(green::add);
            root.add("green", green);

            Files.writeString(QA_STATE_PATH, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            EchoOfTheVoid.LOGGER.warn("Failed to save dev QA state: {}", QA_STATE_PATH, exception);
        }
    }

    private static String serializeIds(Set<String> ids) {
        return ids.stream()
                .sorted()
                .collect(Collectors.joining(";"));
    }

    private static String normalizeId(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }
}
