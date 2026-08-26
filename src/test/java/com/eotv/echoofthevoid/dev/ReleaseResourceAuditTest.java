package com.eotv.echoofthevoid.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ReleaseResourceAuditTest {
    private static final Path JAVA_ROOT = Path.of("src", "main", "java", "com", "eotv", "echoofthevoid");
    private static final Path ASSET_ROOT = Path.of("src", "main", "resources", "assets", "echoofthevoid");

    @Test
    void everyDeclaredCustomSoundHasARegistryEntryAndEveryReferencedOggExists() throws IOException {
        String soundsJson = read(ASSET_ROOT.resolve("sounds.json"));
        String registry = read(JAVA_ROOT.resolve(Path.of("sound", "UncannySoundRegistry.java")));

        Set<String> jsonEvents = captures(
                soundsJson, Pattern.compile("(?m)^  \"([a-z0-9_./-]+)\": \\{"));
        Set<String> registryEvents = captures(
                registry, Pattern.compile("SOUND_EVENTS\\.register\\(\\s*\"([^\"]+)\""));
        assertEquals(jsonEvents, registryEvents,
                "sounds.json and the SoundEvent registry must expose the same stable IDs");

        Set<String> referencedSounds = captures(
                soundsJson, Pattern.compile("\"echoofthevoid:([^\"]+)\""));
        assertFalse(referencedSounds.isEmpty(), "The sound audit unexpectedly found no custom files");
        for (String sound : referencedSounds) {
            Path file = ASSET_ROOT.resolve(Path.of("sounds", sound + ".ogg"));
            assertTrue(Files.isRegularFile(file), "Missing OGG referenced by sounds.json: " + file);
            assertTrue(Files.size(file) > 0L, "Empty OGG referenced by sounds.json: " + file);
        }
    }

    @Test
    void everyPayloadClassIsRegisteredWithTheFrozenProtocol() throws IOException {
        Path networkRoot = JAVA_ROOT.resolve("network");
        String network = read(networkRoot.resolve("UncannyNetwork.java"));
        assertTrue(network.contains("private static final String PROTOCOL_VERSION = \"1\";"),
                "The public network protocol must remain exactly version 1");

        try (var files = Files.list(networkRoot)) {
            for (Path path : files.filter(file -> file.getFileName().toString().endsWith("Payload.java")).toList()) {
                String className = path.getFileName().toString().replace(".java", "");
                String payload = read(path);
                assertTrue(payload.contains("CustomPacketPayload.Type<")
                                || payload.contains("public static final Type<"),
                        className + " must declare a typed payload ID");
                assertTrue(payload.contains("STREAM_CODEC"), className + " must declare its frozen wire codec");
                assertTrue(network.contains(className + ".TYPE"), className + " is not registered in UncannyNetwork");
                assertTrue(network.contains(className + ".STREAM_CODEC"), className + " codec is not registered");
            }
        }
    }

    @Test
    void wrongVillageHouseHasAResolvableStructureAndPlacementSurface() throws IOException {
        Path data = Path.of("src", "main", "resources", "data", "echoofthevoid", "worldgen");
        String structure = read(data.resolve(Path.of("structure", "wrong_village_house.json")));
        String structureSet = read(data.resolve(Path.of("structure_set", "wrong_village_house.json")));
        assertTrue(structure.contains("\"type\"") && structure.contains("echoofthevoid:uncanny_feature"));
        assertTrue(structure.contains("\"feature\"") && structure.contains("wrong_village_house"));
        assertTrue(structureSet.contains("echoofthevoid:wrong_village_house"));
        assertTrue(structureSet.contains("\"spacing\"") && structureSet.contains("94"));
        assertTrue(structureSet.contains("\"separation\"") && structureSet.contains("33"));
        assertTrue(structureSet.contains("\"salt\"") && structureSet.contains("17204"));
    }

    private static Set<String> captures(String source, Pattern pattern) {
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
