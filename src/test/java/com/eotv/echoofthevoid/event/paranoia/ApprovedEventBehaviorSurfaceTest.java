package com.eotv.echoofthevoid.event.paranoia;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ApprovedEventBehaviorSurfaceTest {
    private static final Path EVENT_SYSTEM = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "event", "UncannyParanoiaEventSystem.java");
    private static final Path WEATHER_SYSTEM = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "event", "UncannyWeatherSystem.java");
    private static final Path ZOMBIE = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "entity", "custom", "UncannyZombieEntity.java");
    private static final Path FOLLOWER = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "entity", "custom", "UncannyFollowerEntity.java");

    @Test
    void blackoutRetainsItsCloseHostileConversion() throws IOException {
        String source = read(EVENT_SYSTEM);
        String method = between(source, "private static void tickBlackout", "private static void tickFootsteps");
        assertTrue(method.contains("spawnStalkerEntity(player, 14, 28, true, false)"));
        assertTrue(method.contains("spawnShadow(player)"));
        assertTrue(method.contains("spawnHurler(player)"));
        assertTrue(method.contains("fallbackSpawned"));
    }

    @Test
    void falseFallIsSoundOnlyAndRemainsAllowedDuringARealFall() throws IOException {
        String source = read(EVENT_SYSTEM);
        String method = between(source, "public static boolean triggerFalseFall", "public static boolean triggerFalseInjury");
        assertTrue(method.contains("SoundEvents.PLAYER_SMALL_FALL"));
        assertFalse(method.contains("addEffect"));
        assertFalse(method.contains("MobEffects"));
        assertFalse(method.contains("onGround"));
        assertFalse(method.contains("fallDistance"));
    }

    @Test
    void sobbingRainUsesAStableOccurrenceAudienceAndMentalWhisper() throws IOException {
        String source = read(WEATHER_SYSTEM);
        assertTrue(source.contains("setWeatherTargetPlayerUuid"));
        assertTrue(source.contains("getWeatherTargetPlayerUuid"));
        assertTrue(source.contains("UncannySoundDelivery.playMental"));
        String branch = between(source, "case RAIN_SOBBING -> {", "case THUNDER_SILENT ->");
        assertFalse(branch.contains("sendLocalSound(player, UncannySoundRegistry.UNCANNY_WHISPER"));
    }

    @Test
    void revisedOneShotVolumesStayBelowThePreviousClippingValues() throws IOException {
        String zombie = read(ZOMBIE);
        String follower = read(FOLLOWER);
        assertTrue(zombie.contains("new UncannyZombieRalePayload(0.62F, pitch)"));
        assertFalse(zombie.contains("new UncannyZombieRalePayload(1.85F, pitch)"));
        assertTrue(follower.contains("SoundSource.HOSTILE, 1.05F, 0.88F"));
        assertFalse(follower.contains("SoundSource.HOSTILE, 2.7F, 0.88F"));
    }

    private static String between(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());
        assertTrue(start >= 0, "Missing start marker: " + startMarker);
        assertTrue(end > start, "Missing end marker: " + endMarker);
        return source.substring(start, end);
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
