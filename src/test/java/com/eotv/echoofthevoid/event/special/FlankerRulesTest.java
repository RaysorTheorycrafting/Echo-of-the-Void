package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FlankerRulesTest {
    @Test
    void anglesAreMeasuredFromTheViewAndRotationIsTheirInverse() {
        // Looking along +z.
        assertEquals(0.0D, FlankerRules.signedAngleFromView(0.0D, 1.0D, 0.0D, 5.0D), 1.0E-9D);
        assertEquals(180.0D, Math.abs(FlankerRules.signedAngleFromView(0.0D, 1.0D, 0.0D, -5.0D)), 1.0E-9D);
        for (double angle = -170.0D; angle <= 180.0D; angle += 10.0D) {
            double[] direction = FlankerRules.rotate(0.6D, 0.8D, angle);
            assertEquals(angle, FlankerRules.signedAngleFromView(0.6D, 0.8D, direction[0], direction[1]), 1.0E-6D);
        }
    }

    @Test
    void theBladeOnlyStrikesFromBehindAndIsSeenOnlyInFront() {
        assertTrue(FlankerRules.isInView(40.0D));
        assertFalse(FlankerRules.isInView(70.0D), "a player does not see that far to the side");
        assertFalse(FlankerRules.canStrikeFrom(90.0D), "not from the side");
        assertTrue(FlankerRules.canStrikeFrom(-150.0D));
        assertTrue(FlankerRules.canStrikeFrom(180.0D));
    }

    @Test
    void circlingWorksItsWayToTheBackOnTheSideItIsAlreadyOn() {
        assertEquals(100.0D, FlankerRules.orbitAngle(60.0D), 1.0E-9D);
        assertEquals(-100.0D, FlankerRules.orbitAngle(-60.0D), 1.0E-9D);
        assertEquals(180.0D, FlankerRules.orbitAngle(170.0D), 1.0E-9D);
        // Not yet behind: it keeps its distance; behind: it closes in.
        assertEquals(FlankerRules.DASH_DISTANCE, FlankerRules.orbitRadius(5.0D, 60.0D), 1.0E-9D);
        assertTrue(FlankerRules.orbitRadius(8.0D, 170.0D) < 8.0D);
        assertTrue(FlankerRules.orbitRadius(20.0D, 0.0D) <= FlankerRules.MAX_ORBIT_RADIUS);
    }

    @Test
    void turningClearlyTowardTheBladeSwapsTheRoles() {
        assertEquals(1, FlankerRules.chooseBait(1, 0.10D, 0.30D), "member 1 stays the bait");
        assertEquals(1, FlankerRules.chooseBait(1, 0.40D, 0.30D), "a slight turn is not enough");
        assertEquals(0, FlankerRules.chooseBait(1, 0.90D, -0.50D), "facing member 0 makes it the bait");
        assertEquals(1, FlankerRules.chooseBait(0, -0.9D, 0.95D));
    }

    @Test
    void theHuntStartsWithTheBladeBehindAndTheBaitAhead() {
        assertTrue(FlankerRules.isFormation(160.0D, 10.0D, 10.0D, 10.0D));
        assertFalse(FlankerRules.isFormation(90.0D, 10.0D, 10.0D, 10.0D), "the blade must be behind");
        assertFalse(FlankerRules.isFormation(160.0D, 100.0D, 10.0D, 10.0D), "the bait must be ahead");
        assertFalse(FlankerRules.isFormation(160.0D, 10.0D, 20.0D, 10.0D), "close enough to matter");
    }

    @Test
    void speedsAreMultiplesOfASprintingPlayer() {
        // A mob's ground speed grows with (attribute × modifier)²; a sprinting player is 0.13 at full input.
        double modifier = FlankerRules.navigationModifier(0.44D, 1.0D);
        double speed = 0.44D * modifier;
        assertEquals(0.13D, speed * speed, 1.0E-9D);
        double dash = 0.44D * FlankerRules.navigationModifier(0.44D, FlankerRules.DASH_SPRINT_RATIO);
        assertEquals(FlankerRules.DASH_SPRINT_RATIO, dash * dash / 0.13D, 1.0E-9D);
        assertTrue(FlankerRules.BAIT_SPRINT_RATIO < FlankerRules.ORBIT_SPRINT_RATIO);
        assertTrue(FlankerRules.ORBIT_SPRINT_RATIO < FlankerRules.DASH_SPRINT_RATIO);
    }

    @Test
    void thePairSharesTheAttackerRhythm() {
        assertEquals(36, FlankerRules.pairStrikeGapTicks());
    }
}
