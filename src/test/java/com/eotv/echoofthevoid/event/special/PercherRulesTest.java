package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PercherRulesTest {
    @Test
    void aRoofOrATreeTopBeatsAnyHill() {
        int bestHill = PercherRules.score(false, false, 4, 10);
        int plainRoof = PercherRules.score(true, false, 2, 1);
        int plainTree = PercherRules.score(false, true, 2, 1);
        assertTrue(plainRoof > bestHill, "the player's roof first, even a flat edge of it");
        assertTrue(plainTree > bestHill, "a tree top before the steepest hill");
        assertTrue(plainRoof > plainTree, "a built perch reads as more deliberate than a tree");
    }

    @Test
    void amongEqualsThePeakWithTheDeepestDropWins() {
        assertTrue(PercherRules.score(false, true, 4, 4) > PercherRules.score(false, true, 2, 4));
        assertTrue(PercherRules.score(true, false, 3, 3) > PercherRules.score(true, false, 3, 2));
        assertEquals(PercherRules.HILL, PercherRules.score(false, false, 2, 1));
    }
}
