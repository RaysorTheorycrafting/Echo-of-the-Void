package com.eotv.echoofthevoid.dev;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlayerFacingLoreGuardTest {
    private static final Set<String> TEXT_EXTENSIONS = Set.of("json", "mcmeta", "toml", "txt", "lang");

    @Test
    void internalConcordantNameNeverLeaksIntoPlayerFacingResources() throws IOException {
        Path resources = Path.of("src", "main", "resources");
        List<String> violations = new ArrayList<>();
        try (var paths = Files.walk(resources)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String relative = resources.relativize(path).toString();
                if (relative.toLowerCase(Locale.ROOT).contains("concordant")) {
                    violations.add(relative + " (path)");
                }
                String filename = path.getFileName().toString();
                int dot = filename.lastIndexOf('.');
                String extension = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (!TEXT_EXTENSIONS.contains(extension)) {
                    continue;
                }
                String content = Files.readString(path, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                if (content.contains("concordant")) {
                    violations.add(relative + " (content)");
                }
            }
        }
        assertTrue(violations.isEmpty(), "Internal lore term leaked into player resources: " + violations);
    }

    @Test
    void specialNamesNeverReachPlayers() throws IOException {
        // Death messages, subtitles and info mods read these keys; Vanilla-derived "X?" variants stay named.
        String lang = Files.readString(
                Path.of("src", "main", "resources", "assets", "echoofthevoid", "lang", "en_us.json"),
                StandardCharsets.UTF_8);
        List<String> specials = List.of(
                "ambusher", "arena_pursuer", "ashwalker", "bystander", "devourer", "double_dormant", "doubler",
                "dredger", "drifter", "echoer", "ferryman", "flanker", "follower", "hurler", "keeper", "knocker",
                "listener", "miner", "mourner", "pulse", "shadow", "stalker", "surveyor", "tenant", "terror",
                "usher", "watcher", "percher", "blur", "sleeper");
        List<String> violations = new ArrayList<>();
        for (String id : specials) {
            if (!lang.contains("\"entity.echoofthevoid.uncanny_" + id + "\": \"something\"")) {
                violations.add(id);
            }
        }
        for (String name : List.of("Attacker?", "Echoer?", "Drifter?", "Ashwalker?", "Dredger?", "Flanker?",
                "Mourner?", "Terror?", "Watcher?", "Pursuer?", "Devourer?", "Mimic\"")) {
            if (lang.contains(name)) {
                violations.add(name);
            }
        }
        assertTrue(violations.isEmpty(), "Special names visible to players: " + violations);
    }
}
