package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApprovedSpecialBehaviorRulesTest {
    /** Live QA 2026-10-09: far away, a path search that cannot reach is no reason to hide. */
    @Test
    void attackerOnlyJudgesMissingPathsWithinSearchReach() {
        org.junit.jupiter.api.Assertions.assertTrue(ApprovedSpecialBehaviorRules.attackerJudgesPathAt(12.0D));
        org.junit.jupiter.api.Assertions.assertFalse(ApprovedSpecialBehaviorRules.attackerJudgesPathAt(86.0D),
                "the Flash event spawns it up to 120 blocks off; its follow range is 64");
        org.junit.jupiter.api.Assertions.assertTrue(ApprovedSpecialBehaviorRules.ATTACKER_PATH_JUDGEMENT_RANGE < 64.0D);
    }

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
    void deferredFerrymanNavigationAlwaysLastsTenToFifteenSeconds() {
        assertEquals(200, ApprovedSpecialBehaviorRules.ferrymanRequiredNavigationTicks(-4));
        assertEquals(200, ApprovedSpecialBehaviorRules.ferrymanRequiredNavigationTicks(0));
        assertEquals(250, ApprovedSpecialBehaviorRules.ferrymanRequiredNavigationTicks(50));
        assertEquals(300, ApprovedSpecialBehaviorRules.ferrymanRequiredNavigationTicks(100));
        assertEquals(300, ApprovedSpecialBehaviorRules.ferrymanRequiredNavigationTicks(999));
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

    @Test
    void followerMeleeRemainsPossibleButCannotRemoveItInOneHit() {
        assertEquals(0.0F, ApprovedSpecialBehaviorRules.followerPlayerMeleeDamage(-1.0F));
        assertEquals(2.5F, ApprovedSpecialBehaviorRules.followerPlayerMeleeDamage(2.5F));
        assertEquals(ApprovedSpecialBehaviorRules.FOLLOWER_PLAYER_MELEE_DAMAGE_CAP,
                ApprovedSpecialBehaviorRules.followerPlayerMeleeDamage(100.0F));
        assertTrue(ApprovedSpecialBehaviorRules.FOLLOWER_PLAYER_MELEE_DAMAGE_CAP < 20.0F);
    }

    @Test
    void followerOnlyRepositionsOffscreenWithBudgetAndCooldown() {
        long now = 1_000L;
        long enoughUnobserved = ApprovedSpecialBehaviorRules.FOLLOWER_UNOBSERVED_REPOSITION_TICKS;
        assertEquals(2, ApprovedSpecialBehaviorRules.FOLLOWER_INITIAL_REPOSITIONS);
        assertEquals(8, ApprovedSpecialBehaviorRules.FOLLOWER_UNOBSERVED_REPOSITION_TICKS);
        assertEquals(16.0D, ApprovedSpecialBehaviorRules.FOLLOWER_REPOSITION_MIN_DISTANCE);
        assertEquals(8.0D, ApprovedSpecialBehaviorRules.FOLLOWER_REPOSITION_DISTANCE_SPAN);
        assertEquals(60, ApprovedSpecialBehaviorRules.FOLLOWER_POST_REPOSITION_ATTACK_GRACE_TICKS);
        assertEquals(96.0D, ApprovedSpecialBehaviorRules.FOLLOWER_OBSERVER_RANGE);
        assertEquals(20, ApprovedSpecialBehaviorRules.FOLLOWER_REPOSITION_CLOAK_TICKS);
        assertEquals(3, ApprovedSpecialBehaviorRules.FOLLOWER_REPOSITION_TELEPORT_DELAY_TICKS);
        assertTrue(ApprovedSpecialBehaviorRules.followerCanAttemptReposition(
                1, true, false, enoughUnobserved, now, now));
        assertFalse(ApprovedSpecialBehaviorRules.followerCanAttemptReposition(
                0, true, false, enoughUnobserved, now, now));
        assertFalse(ApprovedSpecialBehaviorRules.followerCanAttemptReposition(
                1, false, false, enoughUnobserved, now, now));
        assertFalse(ApprovedSpecialBehaviorRules.followerCanAttemptReposition(
                1, true, true, enoughUnobserved, now, now));
        assertFalse(ApprovedSpecialBehaviorRules.followerCanAttemptReposition(
                1, true, false, enoughUnobserved - 1L, now, now));
        assertFalse(ApprovedSpecialBehaviorRules.followerCanAttemptReposition(
                1, true, false, enoughUnobserved, now, now + 1L));
    }

    @Test
    void followerNeverRaycastsAcrossAnUnboundedTeleport() {
        double range = ApprovedSpecialBehaviorRules.FOLLOWER_OBSERVER_RANGE;
        assertTrue(ApprovedSpecialBehaviorRules.followerOwnerWithinTrackingRange(range * range));
        assertFalse(ApprovedSpecialBehaviorRules.followerOwnerWithinTrackingRange((range + 0.01D) * (range + 0.01D)));
        assertFalse(ApprovedSpecialBehaviorRules.followerOwnerWithinTrackingRange(Double.POSITIVE_INFINITY));
        assertFalse(ApprovedSpecialBehaviorRules.followerOwnerWithinTrackingRange(Double.NaN));
    }

    @Test
    void followerTeleportRequiresEvidenceOfAnActualVisiblePursuit() {
        assertEquals(8, ApprovedSpecialBehaviorRules.FOLLOWER_PURSUIT_REQUIRED_TICKS);
        assertTrue(ApprovedSpecialBehaviorRules.followerPursuitEvidence(
                true, 12.0D, 0.08D, 0.04D));
        assertFalse(ApprovedSpecialBehaviorRules.followerPursuitEvidence(
                false, 12.0D, 0.08D, 0.04D));
        assertFalse(ApprovedSpecialBehaviorRules.followerPursuitEvidence(
                true, 18.01D, 0.08D, 0.04D));
        assertFalse(ApprovedSpecialBehaviorRules.followerPursuitEvidence(
                true, 12.0D, 0.0D, 0.04D));
        assertFalse(ApprovedSpecialBehaviorRules.followerPursuitEvidence(
                true, 12.0D, 0.08D, 0.0D));
        assertTrue(ApprovedSpecialBehaviorRules.FOLLOWER_UNSEEN_FAR_SPEED < 0.60D);
        assertTrue(ApprovedSpecialBehaviorRules.FOLLOWER_UNSEEN_NEAR_SPEED
                < ApprovedSpecialBehaviorRules.FOLLOWER_UNSEEN_FAR_SPEED);
    }

    @Test
    void followerPostRepositionGracePreventsAnImmediateUnseenAttack() {
        long now = 400L;
        assertFalse(ApprovedSpecialBehaviorRules.followerCanStartUnseenAttack(1.7D, now, now + 1L));
        assertTrue(ApprovedSpecialBehaviorRules.followerCanStartUnseenAttack(1.7D, now, now));
        assertFalse(ApprovedSpecialBehaviorRules.followerCanStartUnseenAttack(1.7001D, now, now));
    }
    @Test
    void surveyorStrikesOnceButNeverKills() {
        assertEquals(4.0F, ApprovedSpecialBehaviorRules.surveyorStrikeDamage(20.0F), 1.0E-6F);
        assertEquals(1.0F, ApprovedSpecialBehaviorRules.surveyorStrikeDamage(3.0F), 1.0E-6F);
        assertEquals(0.0F, ApprovedSpecialBehaviorRules.surveyorStrikeDamage(1.5F), 1.0E-6F);
        for (float health = 0.5F; health <= 20.0F; health += 0.5F) {
            assertTrue(health - ApprovedSpecialBehaviorRules.surveyorStrikeDamage(health) >= Math.min(health, 2.0F));
        }
    }
}
