package com.eotv.echoofthevoid.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ElsewhereMusicSurfaceTest {
    @Test
    void trialMusicIsStreamedAndSelectedOnlyInsideElsewhere() throws IOException {
        Path root = Path.of("src", "main");
        String effects = Files.readString(root.resolve(Path.of(
                "java", "com", "eotv", "echoofthevoid", "client", "UncannyElsewhereClientEffects.java")),
                StandardCharsets.UTF_8);
        String client = Files.readString(root.resolve(Path.of(
                "java", "com", "eotv", "echoofthevoid", "EchoOfTheVoidClient.java")), StandardCharsets.UTF_8);
        String sounds = Files.readString(root.resolve(Path.of(
                "resources", "assets", "echoofthevoid", "sounds.json")), StandardCharsets.UTF_8);

        String player = Files.readString(root.resolve(Path.of(
                "java", "com", "eotv", "echoofthevoid", "client", "UncannyModMusic.java")), StandardCharsets.UTF_8);
        assertTrue(client.contains("UncannyElsewhereClientEffects::onSelectMusic"));
        assertTrue(client.contains("UncannyModMusic::onClientTick"));
        assertTrue(effects.contains("if (isInElsewhere())") && effects.contains("event.setMusic(null)"),
                "No Vanilla track may start inside Elsewhere");
        // User decision 2026-10-08: the score follows the average of every slider, not Music alone.
        assertTrue(player.contains("SoundSource.MASTER") && player.contains("ModMusicVolume.averageOf"));
        assertTrue(player.contains("UncannyDimensions.isElsewhere(minecraft.level)") && player.contains("sounds.stop(elsewhereLoop)"),
                "The loop plays exactly while the player is in Elsewhere and stops on leaving");
        assertTrue(sounds.contains("\"echoofthevoid:music/elsewhere_trial\"") && sounds.contains("\"stream\": true"));
        assertTrue(Files.size(root.resolve(Path.of(
                "resources", "assets", "echoofthevoid", "sounds", "music", "elsewhere_trial.ogg"))) > 100_000L);
    }
}
