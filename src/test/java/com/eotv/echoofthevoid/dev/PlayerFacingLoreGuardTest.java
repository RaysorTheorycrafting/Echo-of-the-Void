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
}
