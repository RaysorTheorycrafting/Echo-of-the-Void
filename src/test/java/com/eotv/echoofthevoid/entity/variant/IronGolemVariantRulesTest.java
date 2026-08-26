package com.eotv.echoofthevoid.entity.variant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IronGolemVariantRulesTest {
    @Test
    void phaseBoundariesSelectTheIntendedVariants() {
        assertEquals(1, IronGolemVariantRules.variantIdForPhaseRoll(1, 0));
        assertEquals(1, IronGolemVariantRules.variantIdForPhaseRoll(1, 99));

        assertEquals(2, IronGolemVariantRules.variantIdForPhaseRoll(2, 67));
        assertEquals(1, IronGolemVariantRules.variantIdForPhaseRoll(2, 68));

        assertEquals(3, IronGolemVariantRules.variantIdForPhaseRoll(3, 57));
        assertEquals(2, IronGolemVariantRules.variantIdForPhaseRoll(3, 58));
        assertEquals(1, IronGolemVariantRules.variantIdForPhaseRoll(3, 86));

        assertEquals(5, IronGolemVariantRules.variantIdForPhaseRoll(4, 31));
        assertEquals(4, IronGolemVariantRules.variantIdForPhaseRoll(4, 32));
        assertEquals(3, IronGolemVariantRules.variantIdForPhaseRoll(4, 64));
        assertEquals(2, IronGolemVariantRules.variantIdForPhaseRoll(4, 86));
    }

    @Test
    void phaseDistributionsUseAllFiveBoundedVariants() {
        int[][] counts = new int[5][6];
        for (int phase = 1; phase <= 4; phase++) {
            for (int roll = 0; roll < 100; roll++) {
                counts[phase][IronGolemVariantRules.variantIdForPhaseRoll(phase, roll)]++;
            }
        }

        assertArrayEquals(new int[] {0, 100, 0, 0, 0, 0}, counts[1]);
        assertArrayEquals(new int[] {0, 32, 68, 0, 0, 0}, counts[2]);
        assertArrayEquals(new int[] {0, 14, 28, 58, 0, 0}, counts[3]);
        assertArrayEquals(new int[] {0, 0, 14, 22, 32, 32}, counts[4]);
    }

    @Test
    void playerBuiltOriginCanNeverReceiveBoundarySentinel() {
        for (int variant = 0; variant <= 5; variant++) {
            int allowed = IronGolemVariantRules.allowedVariantForOrigin(variant, true);
            assertEquals(variant == 5 ? 4 : variant, allowed);
        }
        assertEquals(5, IronGolemVariantRules.allowedVariantForOrigin(5, false));
    }
}
