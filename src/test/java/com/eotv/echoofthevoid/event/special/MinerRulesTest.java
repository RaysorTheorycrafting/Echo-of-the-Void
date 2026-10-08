package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MinerRulesTest {
    @Test
    void cadenceIsExactlyGhostMinerCadenceUntilTheTargetFlees() {
        assertEquals(9, MinerRules.nextHitDelayTicks(0, false, 1, false));
        assertEquals(16, MinerRules.nextHitDelayTicks(7, false, 1, false));
        assertEquals(24, MinerRules.nextHitDelayTicks(2, true, 4, false));
        assertEquals(31, MinerRules.nextHitDelayTicks(7, true, 8, false));
    }

    @Test
    void fleeingUsesSixtyPercentWithAnAbsoluteFiveTickFloor() {
        assertEquals(5, MinerRules.nextHitDelayTicks(0, false, 1, true));
        assertEquals(10, MinerRules.nextHitDelayTicks(7, false, 1, true));
        assertEquals(14, MinerRules.nextHitDelayTicks(2, true, 4, true));
        assertEquals(19, MinerRules.nextHitDelayTicks(7, true, 8, true));
    }

    @Test
    void accelerationRequiresBothFortyTicksAndTwoBlocksGained() {
        assertFalse(MinerRules.confirmsFleeing(39, 12.0D, 14.1D));
        assertFalse(MinerRules.confirmsFleeing(40, 12.0D, 13.99D));
        assertTrue(MinerRules.confirmsFleeing(40, 12.0D, 14.0D));
        assertTrue(MinerRules.confirmsFleeing(80, 4.0D, 9.0D));
    }

    @Test
    void tunnelLimitsRemainBounded() {
        assertEquals(20, MinerRules.MAX_TUNNEL_SECTIONS);
        assertEquals(1024, MinerRules.MAX_SEARCHED_NODES);
        assertEquals(24, MinerRules.MAX_DEBUG_START_CANDIDATES);
        assertEquals(2, MinerRules.MAX_CONSTRUCTION_SECTIONS);
        assertEquals(160, MinerRules.RESTORE_DELAY_TICKS);
        assertEquals(10, MinerRules.RESTORE_RETRY_TICKS);
        assertEquals(4.0D, MinerRules.RESTORATION_OWNER_CLEARANCE);
        assertEquals(0.75D, MinerRules.RESTORATION_ENTITY_MARGIN);
        assertEquals(20, MinerRules.EMERGENCE_GRACE_TICKS);
        assertEquals(40, MinerRules.HUNT_PATH_VALIDATION_TICKS);
        assertEquals(32.0D, MinerRules.MAX_TARGET_DISTANCE);
        assertEquals(0.65D, MinerRules.MAX_PHYSICAL_DISPLACEMENT_PER_TICK);
        assertEquals(60, MinerRules.PHYSICAL_REPATH_TICKS);
        assertEquals(3, MinerRules.MAX_PHYSICAL_RECOVERY_ATTEMPTS);
        assertEquals(256, MinerRules.MAX_OPEN_CONNECTION_NODES);
        assertEquals(10, MinerRules.OPEN_CONNECTION_HORIZONTAL_RADIUS);
        assertEquals(4, MinerRules.OPEN_CONNECTION_VERTICAL_RADIUS);
    }

    @Test
    void physicalArrivalRequiresPositionSupportAndAFreeEntityVolume() {
        assertTrue(MinerRules.hasReachedPhysicalDestination(0.31D, -0.29D, true, true));
        assertFalse(MinerRules.hasReachedPhysicalDestination(0.33D, 0.0D, true, true));
        assertFalse(MinerRules.hasReachedPhysicalDestination(0.0D, 0.31D, true, true));
        assertFalse(MinerRules.hasReachedPhysicalDestination(0.0D, 0.0D, false, true));
        assertFalse(MinerRules.hasReachedPhysicalDestination(0.0D, 0.0D, true, false));
    }

    @Test
    void physicalRecoveryIsDelayedAndStrictlyBounded() {
        assertFalse(MinerRules.shouldAttemptPhysicalRecovery(59, 0));
        assertTrue(MinerRules.shouldAttemptPhysicalRecovery(60, 0));
        assertTrue(MinerRules.shouldAttemptPhysicalRecovery(120, 2));
        assertFalse(MinerRules.shouldAttemptPhysicalRecovery(120, 3));
    }

    @Test
    void sharedAttackerProfilePreservesTheExtractedArithmetic() {
        AdaptiveSpecialCombatProfile ordinary = AdaptiveSpecialCombatProfile.attacker(
                20.0D, 0.10D, 7.0D, 12.0D, 4.0D);
        assertEquals(20.0D, ordinary.maxHealth());
        assertEquals(7.0D, ordinary.attackDamage());
        assertEquals(0.34D, ordinary.movementSpeed());
        assertEquals(12.0D, ordinary.armor());
        assertEquals(4.0D, ordinary.armorToughness());

        AdaptiveSpecialCombatProfile clamped = AdaptiveSpecialCombatProfile.attacker(
                80.0D, 1.0D, 30.0D, 40.0D, 30.0D);
        assertEquals(72.0D, clamped.maxHealth());
        assertEquals(12.0D, clamped.attackDamage());
        assertEquals(0.50D, clamped.movementSpeed());
        assertEquals(20.0D, clamped.armor());
        assertEquals(12.0D, clamped.armorToughness());
        assertEquals(0.18D, clamped.withMovementSpeed(0.18D).movementSpeed());
    }
}
