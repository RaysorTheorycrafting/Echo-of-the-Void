package com.eotv.echoofthevoid.event.special;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minecraft-free, read-only file scanning behind {@link OldFriendFinder}. Every failure means "nobody". */
public final class OldFriendFiles {
    private static final int MAX_WORLDS = 64;
    private static final int MAX_PLAYER_FILES = 512;

    private OldFriendFiles() {
    }

    /** A player of another save; {@code name} is null when no local file knows it (to be asked to Mojang). */
    public record Friend(UUID id, String name) {
        public boolean named() {
            return this.name != null;
        }
    }

    /**
     * Players of the host's other worlds ({@code root/saves/*} and world folders next to the current
     * one), minus the excluded ids and minus anyone who already has a player file in the current world.
     * Names come from Vanilla's {@code usercache.json} (entries expire after a month) and NeoForge's
     * {@code usernamecache.json}. A player without a local name is kept only with an online (version 4)
     * id, which Mojang's session service can name; offline ids cannot be. Named first (by name), then the
     * nameless ones, those who played the most first.
     */
    public static List<Friend> candidates(Path root, Path current, Set<UUID> excluded) {
        Map<UUID, String> names = readUsernameCache(root.resolve("usernamecache.json"));
        names.putAll(readUserCache(root.resolve("usercache.json")));
        Set<UUID> currentMembers = playerIds(current);
        Set<UUID> others = new HashSet<>();
        // How much each player has played, from the size of their advancement file: a friend who really
        // played has tens of kilobytes, a test or one-off join almost nothing. Ranks the nameless ones.
        Map<UUID, Long> played = new HashMap<>();
        List<Path> worlds = new ArrayList<>(worldFolders(root.resolve("saves")));
        worlds.addAll(worldFolders(root));
        for (Path world : worlds) {
            if (!world.equals(current)) {
                for (UUID id : playerIds(world)) {
                    others.add(id);
                    played.merge(id, advancementBytes(world, id), Math::max);
                }
            }
        }
        List<Friend> friends = new ArrayList<>();
        for (UUID id : others) {
            if (excluded.contains(id) || currentMembers.contains(id)) {
                continue;
            }
            String name = names.get(id);
            if (name != null && !name.isBlank()) {
                friends.add(new Friend(id, name));
            } else if (id.version() == 4) {
                friends.add(new Friend(id, null));
            }
        }
        friends.sort((a, b) -> {
            if (a.named() != b.named()) {
                return a.named() ? -1 : 1;
            }
            if (a.named()) {
                return a.name().compareToIgnoreCase(b.name());
            }
            int byPlay = Long.compare(played.getOrDefault(b.id(), 0L), played.getOrDefault(a.id(), 0L));
            return byPlay != 0 ? byPlay : a.id().compareTo(b.id());
        });
        return friends;
    }

    private static long advancementBytes(Path world, UUID id) {
        try {
            Path file = world.resolve("advancements").resolve(id + ".json");
            return Files.isRegularFile(file) ? Files.size(file) : 0L;
        } catch (IOException ignored) {
            return 0L;
        }
    }

    /** Folders directly under {@code parent} that hold a world ({@code level.dat}). */
    public static List<Path> worldFolders(Path parent) {
        List<Path> worlds = new ArrayList<>();
        if (!Files.isDirectory(parent)) {
            return worlds;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent)) {
            for (Path child : stream) {
                if (worlds.size() >= MAX_WORLDS) {
                    break;
                }
                if (Files.isDirectory(child) && Files.isRegularFile(child.resolve("level.dat"))) {
                    worlds.add(child.toAbsolutePath().normalize());
                }
            }
        } catch (IOException ignored) {
            // An unreadable folder simply offers no friend.
        }
        return worlds;
    }

    public static Set<UUID> playerIds(Path world) {
        Set<UUID> ids = new HashSet<>();
        Path playerData = world.resolve("playerdata");
        if (!Files.isDirectory(playerData)) {
            return ids;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(playerData, "*.dat")) {
            for (Path file : stream) {
                if (ids.size() >= MAX_PLAYER_FILES) {
                    break;
                }
                String fileName = file.getFileName().toString();
                try {
                    ids.add(UUID.fromString(fileName.substring(0, fileName.length() - 4)));
                } catch (IllegalArgumentException ignored) {
                    // Not a player file.
                }
            }
        } catch (IOException ignored) {
            // Unreadable: no player from this world.
        }
        return ids;
    }

    private static final Pattern ENTRY = Pattern.compile("\\{[^{}]*}");
    private static final Pattern NAME = Pattern.compile("\"name\"\\s*:\\s*\"([A-Za-z0-9_]{1,16})\"");
    private static final Pattern ID = Pattern.compile("\"uuid\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");
    private static final long MAX_CACHE_BYTES = 4L * 1024L * 1024L;

    private static final Pattern PAIR = Pattern.compile(
            "\"([0-9a-fA-F-]{36})\"\\s*:\\s*\"([A-Za-z0-9_]{1,16})\"");

    /** Names by id from NeoForge's {@code usernamecache.json} (one flat object {id: name}); never expires. */
    public static Map<UUID, String> readUsernameCache(Path file) {
        Map<UUID, String> names = new HashMap<>();
        try {
            if (!Files.isRegularFile(file) || Files.size(file) > MAX_CACHE_BYTES) {
                return names;
            }
            Matcher pairs = PAIR.matcher(Files.readString(file, StandardCharsets.UTF_8));
            while (pairs.find()) {
                try {
                    names.put(UUID.fromString(pairs.group(1)), pairs.group(2));
                } catch (IllegalArgumentException ignored) {
                    // Malformed id.
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // A damaged cache only means fewer names.
        }
        return names;
    }

    /** Names by id from Vanilla's {@code usercache.json} (an array of flat objects); damaged entries are skipped. */
    public static Map<UUID, String> readUserCache(Path file) {
        Map<UUID, String> names = new HashMap<>();
        try {
            if (!Files.isRegularFile(file) || Files.size(file) > MAX_CACHE_BYTES) {
                return names;
            }
            Matcher entries = ENTRY.matcher(Files.readString(file, StandardCharsets.UTF_8));
            while (entries.find()) {
                Matcher name = NAME.matcher(entries.group());
                Matcher id = ID.matcher(entries.group());
                if (name.find() && id.find()) {
                    try {
                        names.put(UUID.fromString(id.group(1)), name.group(1));
                    } catch (IllegalArgumentException ignored) {
                        // Malformed id.
                    }
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // A damaged cache only means fewer candidates.
        }
        return names;
    }
}
