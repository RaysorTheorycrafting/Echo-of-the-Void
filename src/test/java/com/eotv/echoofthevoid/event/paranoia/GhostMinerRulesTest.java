package com.eotv.echoofthevoid.event.paranoia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GhostMinerRulesTest {
    @Test
    void startRingIsFarButStillWithinPhysicalSoundRange() {
        assertTrue(GhostMinerRules.isValidStartOffset(14, 0));
        assertTrue(GhostMinerRules.isValidStartOffset(18, 0));
        assertFalse(GhostMinerRules.isValidStartOffset(13, 0));
        assertFalse(GhostMinerRules.isValidStartOffset(19, 0));
        assertTrue(GhostMinerRules.isValidDebugStartOffset(8, 0));
        assertFalse(GhostMinerRules.isValidDebugStartOffset(7, 0));
        assertEquals(-12, GhostMinerRules.DEBUG_MIN_VERTICAL_OFFSET);
        assertEquals(4, GhostMinerRules.DEBUG_MIN_PLANNED_SECTIONS);
        assertEquals(192, GhostMinerRules.MAX_START_CANDIDATES_TO_EVALUATE);
    }

    @Test
    void twoVerticalSoundsCompleteExactlyOneTunnelSection() {
        assertEquals(0, GhostMinerRules.soundHeightOffset(0));
        assertEquals(1, GhostMinerRules.soundHeightOffset(1));
        assertFalse(GhostMinerRules.completesSection(1));
        assertTrue(GhostMinerRules.completesSection(2));
    }

    @Test
    void approachUsesOnlyOneBlockCardinalCandidates() {
        assertEquals(
                List.of(
                        new GhostMinerRules.HorizontalStep(1, 0),
                        new GhostMinerRules.HorizontalStep(0, 1)),
                GhostMinerRules.orderedApproachSteps(0, 0, 8, 3, false));
        assertEquals(
                List.of(
                        new GhostMinerRules.HorizontalStep(0, -1),
                        new GhostMinerRules.HorizontalStep(-1, 0)),
                GhostMinerRules.orderedApproachSteps(4, 4, 0, 0, false));
    }

    @Test
    void cadenceIsBoundedAndOccasionallyPausesAfterACompleteSection() {
        assertEquals(9, GhostMinerRules.nextHitDelayTicks(-4, false, 0));
        assertEquals(16, GhostMinerRules.nextHitDelayTicks(99, false, 2));
        assertEquals(24, GhostMinerRules.nextHitDelayTicks(2, true, 4));
        assertEquals(31, GhostMinerRules.nextHitDelayTicks(7, true, 8));
    }

    @Test
    void closestApproachStopsBeforeTheSoundSourceReachesThePlayer() {
        assertTrue(GhostMinerRules.hasReachedClosestApproach(4, 0, 0, 0));
        assertFalse(GhostMinerRules.hasReachedClosestApproach(5, 0, 0, 0));
    }

    @Test
    void runtimeRejectsBuiltMaterialsAndTheFormerRandomTeleportFallback() throws IOException {
        String runtime = Files.readString(Path.of(
                "src", "main", "java", "com", "eotv", "echoofthevoid",
                "event", "UncannyParanoiaEventSystem.java"), StandardCharsets.UTF_8);
        String blockPolicy = Files.readString(Path.of(
                "src", "main", "java", "com", "eotv", "echoofthevoid",
                "event", "paranoia", "GhostMinerBlockPolicy.java"), StandardCharsets.UTF_8);
        String devExecutor = Files.readString(Path.of(
                "src", "main", "java", "com", "eotv", "echoofthevoid",
                "dev", "UncannyDevActionExecutor.java"), StandardCharsets.UTF_8);
        String commands = Files.readString(Path.of(
                "src", "main", "java", "com", "eotv", "echoofthevoid",
                "command", "UncannyCommandRegistry.java"), StandardCharsets.UTF_8);

        assertTrue(runtime.contains("isNaturalGhostMinerSoundBlock"));
        assertTrue(runtime.contains("GhostMinerBlockPolicy.isNaturalUnderground(state)"));
        assertTrue(runtime.contains("level.hasChunkAt(pos)"));
        assertTrue(runtime.contains("GhostMinerRules.MAX_START_CANDIDATES_TO_EVALUATE"));
        assertTrue(runtime.contains("public static boolean triggerGhostMinerForDebug"));
        assertTrue(runtime.contains("? naturalColumns.size()"));
        assertTrue(devExecutor.contains("triggerGhostMinerForDebug(target)"));
        assertTrue(commands.contains("triggerGhostMinerForDebug(target)"));
        assertTrue(blockPolicy.contains("BlockTags.BASE_STONE_OVERWORLD"));
        assertTrue(blockPolicy.contains("BlockTags.BASE_STONE_NETHER"));
        assertTrue(blockPolicy.contains("state.is(Blocks.DIRT)"));
        assertFalse(blockPolicy.contains("PLANKS"));
        assertFalse(blockPolicy.contains("COBBLESTONE"));
        assertFalse(blockPolicy.contains("_LOG"));
        assertFalse(runtime.contains("findGhostMinerStrikeableNear"));
        assertFalse(runtime.contains("findGhostMinerStrikePos"));
        assertFalse(runtime.contains("orbitAngleDegrees"));
        assertFalse(runtime.contains("orbitRadius"));
    }
}
