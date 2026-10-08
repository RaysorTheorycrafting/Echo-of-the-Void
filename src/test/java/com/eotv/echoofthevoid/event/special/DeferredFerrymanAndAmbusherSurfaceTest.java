package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DeferredFerrymanAndAmbusherSurfaceTest {
    private static final Path ROOT = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid");

    @Test
    void ferrymanSelectionPersistsThenMeasuresRealBoatDisplacement() throws IOException {
        String state = read("state/UncannyWorldState.java");
        String system = read("event/special/ApprovedSpecialSystem.java");
        String scheduler = read("event/UncannyParanoiaEventSystem.java");
        String devCatalog = read("dev/UncannyDevCatalog.java");
        String devExecutor = read("dev/UncannyDevActionExecutor.java");

        assertTrue(state.contains("pendingFerrymanEncounters"));
        assertTrue(state.contains("requiredNavigationTicks"));
        assertTrue(state.contains("progressTicks"));
        assertTrue(system.contains("armFerrymanEncounter"));
        assertTrue(system.contains("tickPendingFerrymanEncounter"));
        assertTrue(system.contains("boat.getX() - previous.position().x"));
        assertTrue(system.contains("isEligibleFerrymanBoat"));
        assertTrue(scheduler.contains("case \"ferryman\" -> ApprovedSpecialSystem.armFerrymanEncounter(player)"));
        assertFalse(scheduler.contains("phase.index() >= UncannyPhase.PHASE_3.index() && player.getVehicle() instanceof Boat"));
        assertTrue(devCatalog.contains("entity_ferryman_arm_deferred"));
        assertTrue(devCatalog.contains("ActionKind.SPAWN_SPECIAL, \"ferryman_deferred\""));
        assertTrue(devExecutor.contains("case \"ferryman_deferred\" -> ApprovedSpecialSystem.armFerrymanEncounter(target)"));
    }

    @Test
    void ambusherIsLinkedOnlyKillableAndAttemptsExactlyOneHitBeforeSinking() throws IOException {
        String entity = read("entity/custom/UncannyAmbusherEntity.java");
        String registry = read("entity/UncannyEntityRegistry.java");
        String event = read("event/UncannyParanoiaEventSystem.java");
        String dev = read("dev/UncannyDevCatalog.java");

        assertTrue(registry.contains("UNCANNY_AMBUSHER"));
        assertTrue(registry.contains("type == UNCANNY_AMBUSHER.get()"));
        assertTrue(dev.contains("entity_ambusher_spawn"));
        assertTrue(event.contains("tryTriggerFalseFallFollowup(player)"));
        assertTrue(entity.contains("tag.putBoolean(\"AttackAttempted\", attackAttempted)"));
        assertTrue(entity.contains("super.doHurtTarget(focus);\n            beginSinking();"));
        assertFalse(entity.contains("isInvulnerableTo"));
        assertTrue(entity.contains("UncannySinkTransition.step(this, 0.065D, SINK_TICKS)"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(Path.of(relative)), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }
}
