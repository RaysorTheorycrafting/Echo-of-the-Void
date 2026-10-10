package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eotv.echoofthevoid.event.paranoia.DebugBoundsRules;
import org.junit.jupiter.api.Test;

/** Rules introduced by the 2.2.1 playtest fixes. */
class Fixes221RulesTest {
    @Test
    void oldFriendHealsFromSaturationLikeAPlayer() {
        OldFriendRules.Hunger hunger = new OldFriendRules.Hunger();
        float healed = 0.0F;
        for (int tick = 0; tick < 10; tick++) {
            healed += hunger.tick(6.0F);
        }
        assertTrue(healed > 0.0F, "full bar with saturation heals every half second");
        assertTrue(hunger.exhaustion() > 0.0F, "healing costs exhaustion");
        assertFalse(hunger.wantsToEat(6.0F), "a full bar cannot eat");
    }

    @Test
    void oldFriendEatsWhenHungryAndHurt() {
        OldFriendRules.Hunger hunger = new OldFriendRules.Hunger();
        hunger.restore(14, 0.0F, 0.0F);
        assertTrue(hunger.wantsToEat(OldFriendRules.EAT_WHEN_MISSING_HEALTH));
        assertFalse(hunger.wantsToEat(1.0F), "a scratch is not worth a meal");
        assertEquals(0.0F, hunger.tick(6.0F), "below 18 food it does not regenerate");
        hunger.eat(8, 12.8F);
        assertEquals(20, hunger.food());
        assertEquals(12.8F, hunger.saturation(), 1.0E-4F);
    }

    @Test
    void oldFriendHungerDrainsSaturationThenFood() {
        OldFriendRules.Hunger hunger = new OldFriendRules.Hunger();
        hunger.restore(20, 1.0F, 0.0F);
        hunger.addExhaustion(4.5F);
        hunger.tick(0.0F);
        assertEquals(0.0F, hunger.saturation(), 1.0E-4F);
        assertEquals(20, hunger.food());
        hunger.addExhaustion(4.5F);
        hunger.tick(0.0F);
        assertEquals(19, hunger.food());
    }

    @Test
    void oldFriendLinesUpInTheMiddleOfTheBlockBeforePillaring() {
        assertFalse(OldFriendRules.isCentered(0.4D, 0.0D), "standing at the edge straddles two columns");
        assertTrue(OldFriendRules.isCentered(0.05D, -0.05D));
        double offset = 0.45D;
        int ticks = 0;
        while (!OldFriendRules.isCentered(offset, 0.0D) && ticks < 40) {
            offset -= OldFriendRules.centeringStep(offset);
            ticks++;
        }
        assertTrue(ticks <= 10, "it lines up within half a second, took " + ticks);
        assertEquals(-OldFriendRules.PILLAR_CENTERING_SPEED, OldFriendRules.centeringStep(-3.0D), 1.0E-9D);
    }

    @Test
    void dredgerDrainsAirFastButLeavesDrowningToVanilla() {
        int air = 300;
        int ticks = 0;
        while (air > 0) {
            // Vanilla's own loss of one per tick plus the grip.
            air = HuntingSpecialRules.dredgerDrainedAir(air - 1, HuntingSpecialRules.DREDGER_GRAB_AIR_DRAIN_PER_TICK);
            ticks++;
        }
        assertTrue(ticks <= 80, "a full breath is gone in about four seconds, took " + ticks);
        assertEquals(0, HuntingSpecialRules.dredgerDrainedAir(10, 45));
        assertEquals(255, HuntingSpecialRules.dredgerDrainedAir(300, HuntingSpecialRules.DREDGER_BITE_AIR_DRAIN));
    }

    @Test
    void ferrymanBoardsHalfTheTimeWhenASeatIsFree() {
        assertTrue(ApprovedSpecialBehaviorRules.ferrymanBoards(0.10D, 1, 2));
        assertFalse(ApprovedSpecialBehaviorRules.ferrymanBoards(0.60D, 1, 2));
        assertFalse(ApprovedSpecialBehaviorRules.ferrymanBoards(0.10D, 2, 2), "no seat left");
        assertFalse(ApprovedSpecialBehaviorRules.ferrymanBoards(0.10D, 1, 1), "chest boat: driver only");
    }

    @Test
    void ferrymanStrikesAtHalfAnAttacker() {
        double attacker = CombatParityRules.effectiveDamagePerHit(CombatParityRules.ATTACKER, 20.0D);
        double ferryman = CombatParityRules.effectiveDamagePerHit(CombatParityRules.FERRYMAN, 20.0D);
        assertEquals(attacker / 2.0D, ferryman, 1.0E-9D);
        assertEquals(CombatParityRules.maxHealth(CombatParityRules.ATTACKER, 7.0D, 1.6D),
                CombatParityRules.maxHealth(CombatParityRules.FERRYMAN, 7.0D, 1.6D), 1.0E-9D);
    }

    @Test
    void everyFightingSpecialUsesTheAttackerStandard() {
        for (CombatParityRules.Profile profile : new CombatParityRules.Profile[] {
                CombatParityRules.HURLER, CombatParityRules.FOLLOWER, CombatParityRules.KNOCKER,
                CombatParityRules.KEEPER, CombatParityRules.USHER, CombatParityRules.AMBUSHER}) {
            assertEquals(CombatParityRules.hitsToKillPlayer(CombatParityRules.ATTACKER),
                    CombatParityRules.hitsToKillPlayer(profile), profile.id());
            assertEquals(CombatParityRules.maxHealth(CombatParityRules.ATTACKER, 7.0D, 1.6D),
                    CombatParityRules.maxHealth(profile, 7.0D, 1.6D), 1.0E-9D, profile.id());
        }
    }

    @Test
    void watcherRetreatEndsFarAwayOrAfterTrying() {
        assertTrue(WatcherObservationRules.retreatFinished(WatcherObservationRules.FAR_WATCH_DISTANCE, 300));
        assertTrue(WatcherObservationRules.retreatFinished(20.0D, 0));
        assertFalse(WatcherObservationRules.retreatFinished(20.0D, 300));
        assertTrue(WatcherObservationRules.CLOSE_LINGER_MIN_TICKS + WatcherObservationRules.CLOSE_LINGER_RANDOM_TICKS
                <= 20 * 15, "it no longer lingers near the player");
    }

    @Test
    void grandWardenEventIsTwentySecondsShorter() {
        assertEquals(5 * 60 - 20 - GrandWardenRules.PRESPAWN_DELAY_MAX_SECONDS, GrandWardenRules.MAX_RUNTIME_SECONDS);
    }

    @Test
    void debugBoundsStillWaitsForAMajorEventToEnd() {
        assertFalse(DebugBoundsRules.canStartNaturally(4, true, false, true, false, false, true));
        assertTrue(DebugBoundsRules.canStartNaturally(4, true, false, true, false, false, false));
    }
}
