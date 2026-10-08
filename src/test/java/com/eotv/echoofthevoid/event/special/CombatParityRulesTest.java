package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CombatParityRulesTest {
    /** Vanilla armour formula, enough to check the inversion against a realistic reduction. */
    private static double armour(double damage, double armor, double toughness) {
        double effectiveArmor = Math.max(armor / 5.0D,
                armor - damage / (2.0D + toughness / 4.0D));
        return damage * (1.0D - Math.min(20.0D, effectiveArmor) / 25.0D);
    }

    @Test
    void toughnessFollowsTheWeaponThatStrikes() {
        double ironSword = CombatParityRules.maxHealth(CombatParityRules.DRIFTER, 6.0D, 1.6D);
        double netheriteSharpness = CombatParityRules.maxHealth(CombatParityRules.DRIFTER, 11.0D, 1.6D);
        assertEquals(6.0D * 1.6D * 6.0D, ironSword, 1.0E-6D);
        assertTrue(netheriteSharpness > ironSword * 1.8D, "a better weapon meets a tougher creature");
        // Whatever the weapon, a perfect player needs the same time: the fight stays close.
        assertEquals(netheriteSharpness / (11.0D * 1.6D), ironSword / (6.0D * 1.6D), 1.0E-6D);
    }

    @Test
    void bareHandsStillFaceAMinimumOfToughness() {
        assertEquals(CombatParityRules.MIN_HEALTH,
                CombatParityRules.maxHealth(CombatParityRules.ECHOER, 1.0D, 4.0D), 1.0E-6D);
    }

    @Test
    void lethalityIsInvertedThroughThePlayersRealProtection() {
        double effective = CombatParityRules.effectiveDamagePerHit(CombatParityRules.DRIFTER, 20.0D);
        double unarmoured = CombatParityRules.rawDamageFor(effective, damage -> damage);
        double diamond = CombatParityRules.rawDamageFor(effective, damage -> armour(damage, 20.0D, 8.0D));
        assertEquals(effective, unarmoured, 1.0E-3D);
        assertTrue(diamond > unarmoured * 2.0D, "full diamond armour is answered by harder raw hits");
        assertEquals(effective, armour(diamond, 20.0D, 8.0D), 1.0E-3D);
    }

    @Test
    void thePlayerAlwaysWinsAPerfectlyTradedDuel() {
        for (CombatParityRules.Profile profile : new CombatParityRules.Profile[] {
                CombatParityRules.ATTACKER, CombatParityRules.MINER, CombatParityRules.ECHOER,
                CombatParityRules.FLANKER_SURVIVOR, CombatParityRules.DRIFTER,
                CombatParityRules.ASHWALKER, CombatParityRules.DREDGER}) {
            double creatureSeconds = CombatParityRules.hitsToKillPlayer(profile) * profile.monsterCadenceSeconds();
            assertTrue(creatureSeconds > profile.playerSeconds() * 1.2D, profile.id());
            assertTrue(creatureSeconds < profile.playerSeconds() * 1.9D, profile.id() + " must stay a close fight");
        }
    }

    @Test
    void attackerBlowsAreHeavyNotTaps() {
        // User feedback 2026-10-08: about one heart per hit felt harmless.
        assertTrue(CombatParityRules.effectiveDamagePerHit(CombatParityRules.ATTACKER, 20.0D) >= 3.9D);
        assertTrue(CombatParityRules.hitsToKillPlayer(CombatParityRules.ATTACKER) <= 5);
    }

    @Test
    void aFlankerPairSplitsOneHuntersToughness() {
        double member = CombatParityRules.maxHealth(CombatParityRules.FLANKER_PAIR_MEMBER, 7.0D, 1.6D);
        double whole = 7.0D * 1.6D * CombatParityRules.FLANKER_PAIR_MEMBER.playerSeconds();
        assertEquals(whole / 2.0D, member, 1.0E-6D);
    }

    @Test
    void aRefreshKeepsTheShareOfLifeAlreadyLost() {
        assertEquals(60.0D, CombatParityRules.rescaledHealth(30.0D, 60.0D, 120.0D), 1.0E-6D);
        assertEquals(1.0D, CombatParityRules.rescaledHealth(0.1D, 100.0D, 20.0D), 1.0E-6D);
    }

    @Test
    void ashwalkerLeapsReachTheShoreUnderVanillaGravity() {
        for (double distance = 2.0D; distance <= HuntingSpecialRules.ASHWALKER_MAX_LUNGE_DISTANCE; distance += 1.0D) {
            int ticks = HuntingSpecialRules.ashwalkerLeapTicks(distance);
            for (int rise = 0; rise <= HuntingSpecialRules.ASHWALKER_MAX_LUNGE_RISE; rise++) {
                double velocity = HuntingSpecialRules.ashwalkerLeapVerticalVelocity(rise, ticks);
                double height = HuntingSpecialRules.simulatedLeapHeight(velocity, ticks);
                assertEquals(rise + 0.35D, height, 0.01D, "distance " + distance + " rise " + rise);
                assertTrue(velocity < 1.6D, "the leap must stay a leap, not a launch");
            }
            assertTrue(distance / ticks >= 0.2D, "the creature crosses the gap quickly");
        }
    }
}
