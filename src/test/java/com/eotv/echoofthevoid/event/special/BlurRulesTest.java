package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlurRulesTest {
    @Test
    void itStaysAShorterWhileThanBefore() {
        assertEquals(20 * 70, BlurRules.lifetimeTicks(0.0D));
        assertEquals(20 * 110, BlurRules.lifetimeTicks(1.0D));
    }

    @Test
    void theFlurryComesInTheMiddleAndLeavesTimeToFlee() {
        for (double roll = 0.0D; roll <= 1.0D; roll += 0.25D) {
            int lifetime = BlurRules.lifetimeTicks(roll);
            for (double strike = 0.0D; strike <= 1.0D; strike += 0.25D) {
                int delay = BlurRules.strikeDelayTicks(lifetime, strike);
                assertTrue(delay >= BlurRules.MIN_STRIKE_DELAY_TICKS, "not at once");
                assertTrue(delay <= lifetime - BlurRules.STRIKE_MARGIN_TICKS, "before the end: " + delay + " / " + lifetime);
            }
        }
    }

    @Test
    void threeOrFourLightBlowsThatNeverLeaveThePlayerTooLow() {
        assertEquals(3, BlurRules.strikeCount(0.1D));
        assertEquals(4, BlurRules.strikeCount(0.9D));
        assertEquals(2.0F, BlurRules.strikeDamage(20.0F));
        assertEquals(1.0F, BlurRules.strikeDamage(5.0F), "only down to two hearts");
        assertEquals(0.0F, BlurRules.strikeDamage(4.0F));
        assertEquals(0.0F, BlurRules.strikeDamage(1.0F));
    }
}
