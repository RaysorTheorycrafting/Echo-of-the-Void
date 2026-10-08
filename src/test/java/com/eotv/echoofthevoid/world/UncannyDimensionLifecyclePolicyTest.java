package com.eotv.echoofthevoid.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UncannyDimensionLifecyclePolicyTest {
    private static final Set<String> STANDARD_IDS = Set.of(
            "minecraft:overworld",
            "minecraft:the_nether",
            "minecraft:the_end",
            "echoofthevoid:elsewhere");

    @Test
    void acceptsOnlyTheStandardVanillaDimensionsPlusElsewhere() {
        assertTrue(UncannyDimensionLifecyclePolicy.canMarkAsStable(
                STANDARD_IDS, Set.of("echoofthevoid:elsewhere")));
    }

    @Test
    void refusesForeignDimensionsAndUnstableVanillaStems() {
        assertFalse(UncannyDimensionLifecyclePolicy.canMarkAsStable(
                Set.of(
                        "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end",
                        "echoofthevoid:elsewhere", "example:foreign_dimension"),
                Set.of("echoofthevoid:elsewhere", "example:foreign_dimension")));
        assertFalse(UncannyDimensionLifecyclePolicy.canMarkAsStable(
                STANDARD_IDS,
                Set.of("minecraft:overworld", "echoofthevoid:elsewhere")));
    }

    @Test
    void refusesVanillaOnlyWorldsBecauseNoCorrectionIsRequired() {
        assertFalse(UncannyDimensionLifecyclePolicy.canMarkAsStable(
                Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"),
                Set.of()));
    }

    @Test
    void commonMixinIsDeclaredAndNeverClassifiedAsClientOnly() throws IOException {
        String metadata = Files.readString(Path.of("src/main/templates/META-INF/neoforge.mods.toml"));
        String config = Files.readString(Path.of("src/main/resources/echoofthevoid.worldgen.mixins.json"));
        assertTrue(metadata.contains("config=\"${mod_id}.worldgen.mixins.json\""));
        assertTrue(config.contains("\"mixins\""));
        assertTrue(config.contains("WorldDimensionsLifecycleMixin"));
        assertTrue(config.contains(
                "\"package\": \"com.eotv.echoofthevoid.world.lifecycle_mixin\""));
        assertFalse(config.contains("\"package\": \"com.eotv.echoofthevoid.world\""));
        assertFalse(config.contains("\"client\""));
    }

    @Test
    void vanillaGameTestPresetCannotReportAFalseMissingElsewhereError() throws IOException {
        String integrityCheck = Files.readString(Path.of(
                "src/main/java/com/eotv/echoofthevoid/worldgen/UncannyWorldgenIntegrityCheck.java"));
        int harnessGuard = integrityCheck.indexOf("instanceof GameTestServer");
        int hardError = integrityCheck.indexOf("Elsewhere dimension is absent from the active server world");
        assertTrue(harnessGuard >= 0);
        assertTrue(hardError > harnessGuard);
        assertTrue(integrityCheck.substring(harnessGuard, hardError).contains("LOGGER.debug"));
    }
}
