package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HuntingSpecialRulesTest {
    @Test
    void fixedCooldownsMatchApprovedSecondsExactly() {
        assertEquals(48_000L, HuntingSpecialRules.fixedCooldownTicks("echoer"));
        assertEquals(36_000L, HuntingSpecialRules.fixedCooldownTicks("drifter"));
        assertEquals(36_000L, HuntingSpecialRules.fixedCooldownTicks("ashwalker"));
        assertEquals(54_000L, HuntingSpecialRules.fixedCooldownTicks("dredger"));
        assertEquals(72_000L, HuntingSpecialRules.fixedCooldownTicks("flanker"));
    }

    /** Live QA 2026-10-09 (user): the lone Flanker? killed far too fast. */
    @Test
    void flankerBlowsLandAtTheCadenceTheirDamageWasCalibratedFor() {
        // The pair shares one rhythm: one landed blow per Attacker? cadence (1.8 s).
        assertEquals(36, HuntingSpecialRules.flankerAttackIntervalTicks(CombatParityRules.FLANKER_PAIR_MEMBER));
        // A player reaches 3 blocks: alone and face to face, it must come within that reach to strike.
        assertTrue(HuntingSpecialRules.FLANKER_SURVIVOR_STRIKE_DISTANCE < 3.0D);
    }

    @Test
    void adaptiveProfilesApplyApprovedScalingAndBounds() {
        AdaptiveSpecialCombatProfile attacker = new AdaptiveSpecialCombatProfile(30.0D, 10.0D, 0.50D, 12.0D, 6.0D);
        AdaptiveSpecialCombatProfile echoer = HuntingSpecialRules.echoerProfile(attacker);
        assertEquals(24.0D, echoer.maxHealth(), 1.0E-9D);
        assertEquals(8.0D, echoer.attackDamage(), 1.0E-9D);
        assertEquals(0.44D, echoer.movementSpeed(), 1.0E-9D);
        assertEquals(9.6D, echoer.armor(), 1.0E-9D);
        assertEquals(4.8D, echoer.armorToughness(), 1.0E-9D);
        AdaptiveSpecialCombatProfile flanker = HuntingSpecialRules.flankerProfile(attacker);
        assertEquals(15.0D, flanker.maxHealth(), 1.0E-9D);
        assertEquals(5.0D, flanker.attackDamage(), 1.0E-9D);
        assertEquals(0.48D, flanker.movementSpeed(), 1.0E-9D);
        assertEquals(6.0D, flanker.armor(), 1.0E-9D);
        assertEquals(3.0D, flanker.armorToughness(), 1.0E-9D);
        assertEquals(96.0D, HuntingSpecialRules.FLANKER_FOLLOW_RANGE, 1.0E-9D);
        assertTrue(HuntingSpecialRules.FLANKER_EVASION_NAVIGATION_SPEED
                > HuntingSpecialRules.FLANKER_ADVANCING_NAVIGATION_SPEED);
    }

    @Test
    void echoMemoryAcceptsMundaneSoundsAndRejectsWarnings() {
        assertTrue(HuntingSpecialRules.isSafeEchoSoundPath("entity.player.step"));
        assertTrue(HuntingSpecialRules.isSafeEchoSoundPath("block.wooden_door.open"));
        assertTrue(HuntingSpecialRules.isSafeEchoSoundPath("block.stone.break"));
        assertFalse(HuntingSpecialRules.isSafeEchoSoundPath("entity.creeper.primed"));
        assertFalse(HuntingSpecialRules.isSafeEchoSoundPath("entity.warden.sonic_boom"));
        assertFalse(HuntingSpecialRules.isSafeEchoSoundPath("entity.player.hurt"));
    }

    @Test
    void drifterAndAshwalkerTimersAreBounded() {
        assertEquals(0, HuntingSpecialRules.nextDrifterDryTicks(199, true));
        assertEquals(200, HuntingSpecialRules.nextDrifterDryTicks(199, false));
        assertTrue(HuntingSpecialRules.shouldDamageDryDrifter(200));
        assertTrue(HuntingSpecialRules.shouldDamageDryDrifter(220));
        assertFalse(HuntingSpecialRules.shouldDamageDryDrifter(219));
        assertEquals(60, HuntingSpecialRules.extendAshwalkerSubmergeTicks(1, 0));
        assertEquals(100, HuntingSpecialRules.extendAshwalkerSubmergeTicks(200, 80));
    }

    @Test
    void dredgerPullAndReleaseRemainGradual() {
        assertEquals(0.08D, HuntingSpecialRules.clampHorizontalPull(0.9D));
        assertEquals(-0.08D, HuntingSpecialRules.clampHorizontalPull(-0.9D));
        assertEquals(-0.06D, HuntingSpecialRules.clampDownwardPull(-0.9D));
        assertEquals(0.0D, HuntingSpecialRules.clampDownwardPull(0.9D));
        assertFalse(HuntingSpecialRules.releasesDredgerGrab(2));
        assertTrue(HuntingSpecialRules.releasesDredgerGrab(3));

        HuntingSpecialRules.HorizontalVelocity diagonal =
                HuntingSpecialRules.boundedDredgerHorizontalVelocity(9.0D, 9.0D, 9.0D, 9.0D);
        assertEquals(HuntingSpecialRules.DREDGER_MAX_HORIZONTAL_VELOCITY,
                diagonal.magnitude(), 1.0E-9D);
        assertEquals(-0.12D, HuntingSpecialRules.boundedDredgerVerticalVelocity(-9.0D, -9.0D));
        assertEquals(0.04D, HuntingSpecialRules.boundedDredgerVerticalVelocity(9.0D, 9.0D));
        assertEquals(0.10D, HuntingSpecialRules.boundedAquaticVerticalVelocity(8.0D));
        assertEquals(-0.10D, HuntingSpecialRules.boundedAquaticVerticalVelocity(-8.0D));
        assertEquals(0.0D, HuntingSpecialRules.boundedAquaticVerticalVelocity(Double.NaN));
        HuntingSpecialRules.HorizontalVelocity aquatic =
                HuntingSpecialRules.boundedAquaticHorizontalVelocity(10.0D, 10.0D);
        assertEquals(HuntingSpecialRules.AQUATIC_MAX_HORIZONTAL_SPEED,
                aquatic.magnitude(), 1.0E-9D);
        assertEquals(0.0D, HuntingSpecialRules.releasedDredgerVerticalVelocity(-0.12D), 1.0E-9D);
        assertEquals(0.04D, HuntingSpecialRules.releasedDredgerVerticalVelocity(0.09D), 1.0E-9D);
        assertTrue(HuntingSpecialRules.canMaintainDredgerGrab(6.5D, true, true));
        assertFalse(HuntingSpecialRules.canMaintainDredgerGrab(6.51D, true, true));
        assertFalse(HuntingSpecialRules.canMaintainDredgerGrab(4.0D, false, true));
        assertFalse(HuntingSpecialRules.canMaintainDredgerGrab(4.0D, true, false));
    }

    @Test
    void echoerAggroRequiresTheCloseRearWindowAndRealVisibility() {
        assertFalse(HuntingSpecialRules.canTriggerEchoerAggro(2.39D, true, true));
        assertTrue(HuntingSpecialRules.canTriggerEchoerAggro(2.4D, true, true));
        assertTrue(HuntingSpecialRules.canTriggerEchoerAggro(3.1D, true, true));
        assertFalse(HuntingSpecialRules.canTriggerEchoerAggro(3.11D, true, true));
        assertFalse(HuntingSpecialRules.canTriggerEchoerAggro(2.8D, false, true));
        assertFalse(HuntingSpecialRules.canTriggerEchoerAggro(2.8D, true, false));
    }

    @Test
    void flankerGeometryAndPairRewardRequireTwoPlayerKills() {
        assertEquals(180.0D, HuntingSpecialRules.angularSeparationDegrees(1, 0, -1, 0), 0.0001D);
        assertTrue(HuntingSpecialRules.hasValidFlankerSeparation(1, 0, -1, 0));
        assertFalse(HuntingSpecialRules.hasValidFlankerSeparation(1, 0, 0, 1));
        assertFalse(HuntingSpecialRules.isStableFlankerEncirclement(39, 20, 2.8D, true, 1, 0, -1, 0));
        assertFalse(HuntingSpecialRules.isStableFlankerEncirclement(40, 19, 2.8D, true, 1, 0, -1, 0));
        assertFalse(HuntingSpecialRules.isStableFlankerEncirclement(40, 20, 3.61D, true, 1, 0, -1, 0));
        assertTrue(HuntingSpecialRules.isStableFlankerEncirclement(40, 20, 2.8D, true, 1, 0, -1, 0));

        // Observed in game: a member parked at 10.45 blocks never staged and one at 3.70 never struck.
        assertTrue(HuntingSpecialRules.canAdvancingFlankerStage(10.45D));
        assertFalse(HuntingSpecialRules.canAdvancingFlankerStage(11.6D));
        assertTrue(HuntingSpecialRules.FLANKER_STAGING_HOLD_DISTANCE
                < HuntingSpecialRules.FLANKER_ENCIRCLEMENT_ATTACK_DISTANCE);
        assertFalse(HuntingSpecialRules.isFlankerHoldingStagedStrike(3.70D));
        assertTrue(HuntingSpecialRules.isFlankerHoldingStagedStrike(2.8D));
        assertTrue(HuntingSpecialRules.isFlankerTerrainDryEnough(0, 120));
        assertTrue(HuntingSpecialRules.isFlankerTerrainDryEnough(15, 85));
        assertFalse(HuntingSpecialRules.isFlankerTerrainDryEnough(30, 90));
        assertFalse(HuntingSpecialRules.isFlankerTerrainDryEnough(0, 0));

        HuntingSpecialRules.PairRewardProgress first = HuntingSpecialRules.recordFlankerTerminal(
                HuntingSpecialRules.PairRewardProgress.fresh(), true, false);
        HuntingSpecialRules.PairRewardProgress second = HuntingSpecialRules.recordFlankerTerminal(first, true, true);
        assertTrue(second.rewardEligible());

        HuntingSpecialRules.PairRewardProgress invalid = HuntingSpecialRules.recordFlankerTerminal(
                HuntingSpecialRules.PairRewardProgress.fresh(), false, false);
        invalid = HuntingSpecialRules.recordFlankerTerminal(invalid, true, true);
        assertFalse(invalid.rewardEligible());
        assertTrue(invalid.invalidated());
    }

    @Test
    void ashwalkerExcursionIsAGreatLeapButStaysShortAndReturnable() {
        // User feedback 2026-10-08: standing back three blocks kept it at bay; it now leaps.
        assertEquals(7.0D, HuntingSpecialRules.ASHWALKER_MAX_LUNGE_DISTANCE);
        assertEquals(3, HuntingSpecialRules.ASHWALKER_MAX_LUNGE_RISE);
        assertEquals(70, HuntingSpecialRules.ASHWALKER_MAX_LAND_TICKS);
        assertEquals(2, HuntingSpecialRules.ASHWALKER_LAND_BITES);
        assertEquals(100, HuntingSpecialRules.ASHWALKER_STRANDED_DAMAGE_START_TICKS);
        assertEquals(4.0F, HuntingSpecialRules.ASHWALKER_STRANDED_DAMAGE);
    }
}
