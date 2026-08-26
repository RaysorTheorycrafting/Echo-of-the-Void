package com.eotv.echoofthevoid.event.passive;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RabbitVariantBehaviorSurfaceTest {
    private static final Path PASSIVE_FACADE = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "event",
            "UncannyPassiveVariantSystem.java");
    private static final Path SOURCE = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "event", "passive",
            "RabbitVariantBehaviorSystem.java");

    @Test
    void allFiveBehavioursAreBoundedSharedAndNonPunitive() throws IOException {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        String facade = Files.readString(PASSIVE_FACADE, StandardCharsets.UTF_8);

        assertTrue(source.contains("case 1 -> tickDelayedHop"));
        assertTrue(source.contains("case 2 -> tickRearwardGaze"));
        assertTrue(source.contains("case 3 -> tickFalseBurrow"));
        assertTrue(source.contains("case 4 -> tickEmptyLanding"));
        assertTrue(source.contains("case 5 -> tickWatchbound"));
        assertTrue(source.contains("level.playSound(null"));
        assertTrue(source.contains("level.sendParticles("));
        assertTrue(source.contains("!rabbit.isLeashed()"));
        assertTrue(source.contains("!rabbit.isInLove()"));
        assertTrue(source.contains("rabbit.hurtTime <= 0"));
        assertTrue(source.contains("rabbit.getNavigation().stop()"));
        assertTrue(facade.contains("case \"rabbit\" -> EntityType.RABBIT"));
        assertTrue(facade.contains("|| type == EntityType.RABBIT"));
        assertTrue(facade.contains("RabbitVariantBehaviorSystem.tick(serverLevel, rabbit, variant)"));
        assertTrue(facade.contains("RabbitVariantBehaviorSystem.reset(rabbit, clampedVariant"));

        assertFalse(source.contains(".hurt("));
        assertFalse(source.contains(".kill("));
        assertFalse(source.contains(".discard("));
        assertFalse(source.contains("destroyBlock("));
        assertFalse(source.contains("setBlock("));
        assertFalse(source.contains("setNoAi("));
        assertFalse(source.contains("teleport"));
        assertFalse(source.contains("setPos("));
        assertFalse(source.contains("playNotifySound"));
    }
}
