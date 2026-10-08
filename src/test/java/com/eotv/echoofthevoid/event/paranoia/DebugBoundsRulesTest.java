package com.eotv.echoofthevoid.event.paranoia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DebugBoundsRulesTest {
    @Test
    void naturalTriggerRequiresOneEligibleF3BActivation() {
        assertFalse(DebugBoundsRules.canStartNaturally(1, true, false, true, false, false, false));
        assertFalse(DebugBoundsRules.canStartNaturally(2, false, false, true, false, false, false));
        assertFalse(DebugBoundsRules.canStartNaturally(2, true, true, true, false, false, false));
        assertFalse(DebugBoundsRules.canStartNaturally(2, true, false, false, false, false, false));
        assertFalse(DebugBoundsRules.canStartNaturally(2, true, false, true, true, false, false));
        assertFalse(DebugBoundsRules.canStartNaturally(2, true, false, true, false, true, false));
        assertFalse(DebugBoundsRules.canStartNaturally(2, true, false, true, false, false, true));
        assertTrue(DebugBoundsRules.canStartNaturally(2, true, false, true, false, false, false));
    }

    @Test
    void allPresencesStartFarAndConvergeWithoutTouchingThePlayer() {
        for (int index = 0; index < DebugBoundsRules.PRESENCE_COUNT; index++) {
            double start = DebugBoundsRules.startRadius(42L, index);
            assertTrue(start >= DebugBoundsRules.MINIMUM_START_RADIUS);
            assertTrue(start <= DebugBoundsRules.MAXIMUM_START_RADIUS);
            assertEquals(start, DebugBoundsRules.radiusAt(start, 0, DebugBoundsRules.DURATION_TICKS), 1.0E-9D);
            assertEquals(DebugBoundsRules.MINIMUM_RADIUS,
                    DebugBoundsRules.radiusAt(start, DebugBoundsRules.DURATION_TICKS, DebugBoundsRules.DURATION_TICKS),
                    1.0E-9D);
        }
    }

    @Test
    void geometryIsSeededAndStable() {
        assertEquals(DebugBoundsRules.startRadius(98L, 7), DebugBoundsRules.startRadius(98L, 7));
        assertEquals(DebugBoundsRules.angleRadians(98L, 7, 36), DebugBoundsRules.angleRadians(98L, 7, 36));
        assertTrue(DebugBoundsRules.startRadius(98L, 7) != DebugBoundsRules.startRadius(99L, 7));
    }

    @Test
    void presencesOwnWorldPositionsAndAdvanceByABoundedStep() {
        double speed = DebugBoundsRules.approachSpeed(77L, 4);
        assertTrue(speed >= DebugBoundsRules.MINIMUM_APPROACH_SPEED);
        assertTrue(speed <= DebugBoundsRules.MAXIMUM_APPROACH_SPEED);
        DebugBoundsRules.Step first = DebugBoundsRules.stepToward(10.0D, -4.0D, 0.0D, 0.0D, speed);
        assertTrue(Math.hypot(first.x() - 10.0D, first.z() + 4.0D) <= speed + 1.0E-9D);
        DebugBoundsRules.Step stopped = DebugBoundsRules.stepToward(
                DebugBoundsRules.MINIMUM_RADIUS,
                0.0D,
                0.0D,
                0.0D,
                speed);
        assertEquals(DebugBoundsRules.MINIMUM_RADIUS, stopped.x(), 1.0E-9D);
        assertEquals(0.0D, stopped.z(), 1.0E-9D);
    }

    @Test
    void coarseRetargetsPreventAPlayerAttachedRing() {
        for (int index = 0; index < DebugBoundsRules.PRESENCE_COUNT; index++) {
            int interval = DebugBoundsRules.retargetInterval(81L, index, 0);
            assertTrue(interval >= DebugBoundsRules.MINIMUM_RETARGET_TICKS);
            assertTrue(interval <= DebugBoundsRules.MAXIMUM_RETARGET_TICKS);
            DebugBoundsRules.Step first = DebugBoundsRules.targetOffset(81L, index, 0);
            DebugBoundsRules.Step second = DebugBoundsRules.targetOffset(81L, index, 1);
            assertTrue(Math.hypot(first.x(), first.z()) >= 0.65D);
            assertTrue(Math.hypot(first.x(), first.z()) <= 3.05D + 1.0E-9D);
            assertTrue(first.x() != second.x() || first.z() != second.z());
        }
    }
    @Test
    void presencesNoticeTheirPreyOneByOneAndHuntAtVanillaSpeeds() {
        java.util.Set<DebugBoundsRules.Shape> shapes = java.util.EnumSet.noneOf(DebugBoundsRules.Shape.class);
        int earlyNoticers = 0;
        for (int index = 0; index < DebugBoundsRules.PRESENCE_COUNT; index++) {
            DebugBoundsRules.Shape shape = DebugBoundsRules.shape(42L, index);
            shapes.add(shape);
            int notice = DebugBoundsRules.noticeDelayTicks(42L, index);
            assertTrue(notice >= 0 && notice < DebugBoundsRules.MAXIMUM_NOTICE_DELAY_TICKS);
            if (notice < 60) {
                earlyNoticers++;
            }
            double speed = DebugBoundsRules.huntSpeed(42L, index, shape);
            if (shape.strikes()) {
                // Fast enough to reach a player from 24 blocks well inside the 36-second encounter.
                assertTrue(speed >= 0.07D && speed <= 0.21D, shape + " " + speed);
                assertTrue(24.0D / speed < DebugBoundsRules.DURATION_TICKS * 0.6D, shape + " " + speed);
            }
        }
        assertTrue(shapes.contains(DebugBoundsRules.Shape.HUMANOID), "most boxes are mob-sized walkers");
        assertTrue(earlyNoticers >= 3, "some presences must start hunting within three seconds");
        assertTrue(DebugBoundsRules.CONTACT_DISTANCE < DebugBoundsRules.STRIKE_DISTANCE);
        assertTrue(DebugBoundsRules.MAXIMUM_FAKE_HITS <= 6, "fake blows must stay rare");
    }
}
