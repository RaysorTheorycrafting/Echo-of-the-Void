package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GrandWardenRulesTest {
    @Test
    void preSpawnWarningLeavesBetweenFiveAndSevenSecondsToReact() {
        assertEquals(5, GrandWardenRules.preSpawnDelaySeconds(0));
        assertEquals(6, GrandWardenRules.preSpawnDelaySeconds(1));
        assertEquals(7, GrandWardenRules.preSpawnDelaySeconds(2));
        assertThrows(IllegalArgumentException.class, () -> GrandWardenRules.preSpawnDelaySeconds(-1));
        assertThrows(IllegalArgumentException.class, () -> GrandWardenRules.preSpawnDelaySeconds(3));
    }

    @Test
    void tagAndDisplayNameStayCompatibleAcrossServerAndRenderer() {
        assertEquals("eotv_grand_warden", GrandWardenRules.ENTITY_TAG);
        assertEquals("Warden?", GrandWardenRules.DISPLAY_NAME);
    }

    @Test
    void everyRuntimeWarningUsesTheSharedRollAndAKillEndsTheActiveScene() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "eotv", "echoofthevoid", "event",
                "UncannyParanoiaEventSystem.java"), StandardCharsets.UTF_8);
        String runtimeCall = "rollGrandWardenPreSpawnDelayTicks(level)";

        assertEquals(3, occurrences(source, runtimeCall));
        assertTrue(source.contains("GrandWardenRules.preSpawnDelaySeconds(level.random.nextInt(size))"));
        assertFalse(source.contains(
                "rollRangeInclusive(level, GRAND_EVENT_PRESPAWN_DELAY_MIN_SECONDS, GRAND_EVENT_PRESPAWN_DELAY_MAX_SECONDS)"));
        assertTrue(source.contains("warden.getRemovalReason() == Entity.RemovalReason.KILLED"));
        assertTrue(source.contains("\"warden_killed\""));
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }
}
