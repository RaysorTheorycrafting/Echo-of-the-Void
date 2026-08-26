package com.eotv.echoofthevoid.dev;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DedicatedMixinConfigTest {
    @Test
    void darknessMixinsAreLoadedOnlyOnTheClientDistribution() throws IOException {
        String metadata = Files.readString(
                Path.of("src", "main", "templates", "META-INF", "neoforge.mods.toml"),
                StandardCharsets.UTF_8);
        String mixin = Files.readString(
                Path.of("src", "main", "resources", "echoofthevoid.darkness.mixins.json"),
                StandardCharsets.UTF_8);

        assertTrue(metadata.contains("config=\"${mod_id}.darkness.mixins.json\""));
        assertFalse(metadata.contains("config=\"${mod_id}.client.mixins.json\""));
        assertTrue(mixin.contains("\"client\""));
        assertFalse(mixin.contains("\"mixins\""));
        assertTrue(mixin.contains("MixinGameRenderer"));
        assertTrue(mixin.contains("MixinLightTexture"));
        assertTrue(mixin.contains("MixinDynamicTexture"));
    }
}
