package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SleeperRulesTest {
    private static final float FULL_HEALTH = 20.0F;

    @Test
    void biteKillsInGoldenArmourButNotInIronOrBetter() {
        assertTrue(SleeperRules.biteKills(FULL_HEALTH, 0.0F, 0.0F), "no armour");
        assertTrue(SleeperRules.biteKills(FULL_HEALTH, 7.0F, 0.0F), "full leather");
        assertTrue(SleeperRules.biteKills(FULL_HEALTH, 11.0F, 0.0F), "full gold");
        assertFalse(SleeperRules.biteKills(FULL_HEALTH, 15.0F, 0.0F), "full iron");
        assertFalse(SleeperRules.biteKills(FULL_HEALTH, 20.0F, 8.0F), "full diamond");
        assertFalse(SleeperRules.biteKills(FULL_HEALTH, 20.0F, 12.0F), "full netherite");
    }

    @Test
    void armourFormulaMatchesVanillaReferenceValues() {
        // 22 raw through 11 armour (gold): armour term clamps at 11 * 0.2 = 2.2 -> 22 * (1 - 2.2 / 25).
        assertEquals(22.0F * (1.0F - 2.2F / 25.0F), SleeperRules.damageAfterArmor(22.0F, 11.0F, 0.0F), 1.0E-4F);
        // 22 raw through 15 armour (iron): max(15 - 11, 3) = 4 -> 22 * (1 - 4 / 25).
        assertEquals(22.0F * (1.0F - 4.0F / 25.0F), SleeperRules.damageAfterArmor(22.0F, 15.0F, 0.0F), 1.0E-4F);
    }

    @Test
    void warningIsTheTextTheUserChose() {
        assertEquals("You can’t sleep. Something is in the room.", SleeperRules.WARNING);
    }

    @Test
    void attackStartsHalfASecondIntoSleepAndBitesBeforeTheNightCouldEnd() {
        assertEquals(10, SleeperRules.ASLEEP_BEFORE_RUN_TICKS);
        int latestBite = SleeperRules.ASLEEP_BEFORE_RUN_TICKS + SleeperRules.MAX_RUN_TICKS + SleeperRules.MOUNT_TICKS
                + SleeperRules.MOUTH_TICKS + SleeperRules.BITE_IMPACT_TICK;
        assertTrue(latestBite < 120, "bite at " + latestBite + " ticks; the pin also blocks the night skip");
    }
}
