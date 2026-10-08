package com.eotv.echoofthevoid.event.paranoia;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FalseFallFollowupRulesTest {
    @Test
    void ambusherUsesOneExactPercentOnlyFromPhaseTwoWithDangerEnabled() {
        assertTrue(FalseFallFollowupRules.shouldAttemptAmbusher(2, 1, true, 0));
        assertTrue(FalseFallFollowupRules.shouldAttemptAmbusher(4, 5, true, 100));
        assertFalse(FalseFallFollowupRules.shouldAttemptAmbusher(1, 5, true, 0));
        assertFalse(FalseFallFollowupRules.shouldAttemptAmbusher(2, 0, true, 0));
        assertFalse(FalseFallFollowupRules.shouldAttemptAmbusher(2, 1, false, 0));
        for (int roll = 1; roll < FalseFallFollowupRules.AMBUSH_ROLL_BOUND; roll++) {
            assertFalse(FalseFallFollowupRules.shouldAttemptAmbusher(4, 5, true, roll));
        }
    }
}
