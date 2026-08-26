package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApprovedSpecialBehaviorRulesTest {
    @Test
    void ferrymanRequiresRealMovementAndDeepWater() {
        assertEquals(4, ApprovedSpecialBehaviorRules.FERRYMAN_MIN_WATER_DEPTH);
        assertEquals(-2.35D, ApprovedSpecialBehaviorRules.FERRYMAN_VERTICAL_OFFSET);
        assertEquals(-2.05D, ApprovedSpecialBehaviorRules.FERRYMAN_MAX_FEET_Y_OFFSET);
        assertEquals(0.27D, ApprovedSpecialBehaviorRules.FERRYMAN_WATER_SAMPLE_RADIUS);
        assertEquals(0.42D, ApprovedSpecialBehaviorRules.FERRYMAN_MAX_HORIZONTAL_STEP);
        assertFalse(ApprovedSpecialBehaviorRules.ferrymanBoatIsMoving(0.02D, 0.0D));
        assertTrue(ApprovedSpecialBehaviorRules.ferrymanBoatIsMoving(0.021D, 0.0D));
        assertEquals(28, ApprovedSpecialBehaviorRules.FERRYMAN_IDLE_RISE_DELAY_TICKS);
        assertEquals(42, ApprovedSpecialBehaviorRules.FERRYMAN_REVEAL_HOLD_TICKS);
        assertEquals(72, ApprovedSpecialBehaviorRules.FERRYMAN_DEPARTURE_TICKS);
        assertEquals(-0.90D, ApprovedSpecialBehaviorRules.FERRYMAN_REVEAL_FEET_Y_OFFSET);
        assertEquals(0.80F, ApprovedSpecialBehaviorRules.FERRYMAN_WAKE_VOLUME);
    }

    @Test
    void physicalSoundCadencesStayShortButSparse() {
        assertEquals(90, ApprovedSpecialBehaviorRules.mournerSobIntervalTicks(-4));
        assertEquals(200, ApprovedSpecialBehaviorRules.mournerSobIntervalTicks(999));
        assertEquals(100, ApprovedSpecialBehaviorRules.ferrymanWakeIntervalTicks(-4));
        assertEquals(220, ApprovedSpecialBehaviorRules.ferrymanWakeIntervalTicks(999));
    }

    @Test
    void mournerAlwaysHasTimeToSobBeforeAcknowledgingItsObserver() {
        assertTrue(25 < ApprovedSpecialBehaviorRules.MOURNER_MIN_OBSERVATION_TICKS);
        assertEquals(18, ApprovedSpecialBehaviorRules.MOURNER_REQUIRED_GAZE_TICKS);
        assertEquals(100, ApprovedSpecialBehaviorRules.MOURNER_ACKNOWLEDGEMENT_TICKS);
        assertEquals(48, ApprovedSpecialBehaviorRules.MOURNER_SINK_TICKS);
        assertEquals(15.0D, ApprovedSpecialBehaviorRules.MOURNER_AUDIBLE_RANGE);
        assertEquals(1.0F, ApprovedSpecialBehaviorRules.MOURNER_SOB_VOLUME);
    }

    @Test
    void doublerMirrorsAcrossTheActualBarrierPlane() {
        ApprovedSpecialBehaviorRules.MirroredMotion mirrored =
                ApprovedSpecialBehaviorRules.mirrorAcrossHorizontalPlane(
                        0.30D, 0.20D, 0.12D, 1.0D, 0.0D);
        assertEquals(-0.30D, mirrored.x(), 1.0E-9D);
        assertEquals(0.20D, mirrored.y(), 1.0E-9D);
        assertEquals(0.12D, mirrored.z(), 1.0E-9D);

        mirrored = ApprovedSpecialBehaviorRules.mirrorAcrossHorizontalPlane(
                0.30D, -0.10D, 0.12D, 0.0D, 1.0D);
        assertEquals(0.30D, mirrored.x(), 1.0E-9D);
        assertEquals(-0.10D, mirrored.y(), 1.0E-9D);
        assertEquals(-0.12D, mirrored.z(), 1.0E-9D);
    }

    @Test
    void attackerCueTimingCannotBecomeASystematicWarning() {
        assertFalse(ApprovedSpecialBehaviorRules.shouldPlayAttackerCue(0, 1.0D, true, true));
        assertTrue(ApprovedSpecialBehaviorRules.shouldPlayAttackerCue(1, 14.0D * 14.0D, true, false));
        assertFalse(ApprovedSpecialBehaviorRules.shouldPlayAttackerCue(1, 14.1D * 14.1D, true, false));
        assertFalse(ApprovedSpecialBehaviorRules.shouldPlayAttackerCue(2, 6.0D * 6.0D, false, false));
        assertTrue(ApprovedSpecialBehaviorRules.shouldPlayAttackerCue(2, 7.0D * 7.0D, true, false));
        assertFalse(ApprovedSpecialBehaviorRules.shouldPlayAttackerCue(3, 1.0D, true, false));
        assertTrue(ApprovedSpecialBehaviorRules.shouldPlayAttackerCue(3, 1.0D, true, true));
    }
}
