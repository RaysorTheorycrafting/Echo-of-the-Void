package com.eotv.echoofthevoid.event.paranoia;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RetiredGameplaySurfaceTest {
    private static final Path EVENT_SYSTEM = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "event", "UncannyParanoiaEventSystem.java");
    private static final Path COMMANDS = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "command", "UncannyCommandRegistry.java");
    private static final Path DEV_EXECUTOR = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "dev", "UncannyDevActionExecutor.java");
    private static final Path CLIENT_BOOTSTRAP = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "EchoOfTheVoidClient.java");

    @Test
    void retiredEventsKeepReservedIdsButHaveNoExecutableSurface() throws IOException {
        assertTrue(ParanoiaEventCatalog.retiredEventIds().containsAll(
                Set.of(ParanoiaEventIds.FORCED_DROP, ParanoiaEventIds.ARMOR_BREAK, ParanoiaEventIds.GIANT_SUN)));

        String eventSource = read(EVENT_SYSTEM);
        assertFalse(eventSource.contains("triggerForcedDrop("));
        assertFalse(eventSource.contains("triggerArmorBreak("));
        assertFalse(eventSource.contains("triggerGiantSun("));

        String commandSource = read(COMMANDS);
        assertFalse(commandSource.contains("Commands.literal(\"forceDrop\")"));
        assertFalse(commandSource.contains("Commands.literal(\"armorBreak\")"));
        assertFalse(commandSource.contains("Commands.literal(\"giantSun\")"));

        String executorSource = read(DEV_EXECUTOR);
        assertFalse(executorSource.contains("case \"forced_drop\""));
        assertFalse(executorSource.contains("case \"armor_break\""));
        assertFalse(executorSource.contains("case \"giant_sun\""));

        assertFalse(read(CLIENT_BOOTSTRAP).contains("UncannyAtmosphereClientEffects::onRenderLevelStage"));
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
