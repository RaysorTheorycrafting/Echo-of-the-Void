package com.eotv.echoofthevoid.event.paranoia;

import static com.eotv.echoofthevoid.event.paranoia.ParanoiaEventIds.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ParanoiaPacingRulesTest {
    private static final String EXPECTED_FULL_MATRIX_SHA256 =
            "76A4FC4BB9A84A3375DD41C9AC883ECC3C60DE84FF096AD5695B798EBC7FB71B";

    @Test
    void triggerChanceBoundsAndReferencePointsMatch111() {
        assertEquals(0.003D, ParanoiaPacingRules.autoTriggerChance(1, 1, 0), 0.0D);
        assertEquals(0.10692D, ParanoiaPacingRules.autoTriggerChance(4, 5, 5), 1.0E-12D);
        assertEquals(0.06D, ParanoiaPacingRules.ambientTriggerChance(1, 1, 0), 0.0D);
        assertEquals(0.34751D, ParanoiaPacingRules.ambientTriggerChance(4, 5, 5), 1.0E-12D);
        assertEquals(0.0D, ParanoiaPacingRules.specialTriggerChance(1, 5, 5), 0.0D);
        assertEquals(0.02D, ParanoiaPacingRules.specialTriggerChance(2, 1, 0), 0.0D);
        assertEquals(0.285824D, ParanoiaPacingRules.specialTriggerChance(4, 5, 5), 1.0E-12D);
    }

    @Test
    void globalAmbientAndSilenceTicksMatch111() {
        assertEquals(9180L, ParanoiaPacingRules.effectiveGlobalCooldownTicks(1, 1, 0, 480));
        assertEquals(400L, ParanoiaPacingRules.effectiveGlobalCooldownTicks(4, 5, 5, 480));
        assertEquals(200L, ParanoiaPacingRules.effectiveGlobalCooldownTicks(4, 5, 5, 0));
        assertEquals(5320L, ParanoiaPacingRules.ambientGlobalCooldownTicks(1, 1, 0));
        assertEquals(700L, ParanoiaPacingRules.ambientGlobalCooldownTicks(4, 5, 5));
        assertEquals(13820L, ParanoiaPacingRules.maxSilenceTicks(1, 1, 0));
        assertEquals(500L, ParanoiaPacingRules.maxSilenceTicks(4, 5, 5));
    }

    @Test
    void weightClassesDangerExclusionsAndRoundingMatch111() {
        assertEquals(0, ParanoiaPacingRules.effectiveWeight(BLACKOUT, 7, 5, 0));
        assertEquals(37, ParanoiaPacingRules.effectiveWeight(FOOTSTEPS, 16, 1, 0));
        assertEquals(3, ParanoiaPacingRules.effectiveWeight(CORRUPT_MESSAGE, 18, 5, 5));
        assertEquals(17, ParanoiaPacingRules.effectiveWeight(BLACKOUT, 7, 5, 5));
        assertEquals(1, ParanoiaPacingRules.effectiveWeight(USHER, 1, 3, 3));
        assertEquals(1, ParanoiaPacingRules.effectiveWeight(STALKER, 11, 1, 1));
    }

    @Test
    void cooldownRangesAndSampledExtremesMatch111() {
        assertEquals(new ParanoiaPacingRules.IntRange(16, 45),
                ParanoiaPacingRules.eventCooldownSecondsRange(ParanoiaEventSeverity.LIGHT));
        assertEquals(new ParanoiaPacingRules.IntRange(35, 95),
                ParanoiaPacingRules.eventCooldownSecondsRange(ParanoiaEventSeverity.MEDIUM));
        assertEquals(new ParanoiaPacingRules.IntRange(70, 180),
                ParanoiaPacingRules.eventCooldownSecondsRange(ParanoiaEventSeverity.HIGH));
        assertEquals(new ParanoiaPacingRules.IntRange(120, 300),
                ParanoiaPacingRules.eventCooldownSecondsRange(ParanoiaEventSeverity.EXTREME));
        assertEquals(900L, ParanoiaPacingRules.eventCooldownTicks(
                1, 1, 0, ParanoiaEventSeverity.LIGHT, 16, 0.0D));
        assertEquals(1260L, ParanoiaPacingRules.eventCooldownTicks(
                4, 5, 5, ParanoiaEventSeverity.EXTREME, 300, Math.nextDown(1.0D)));
    }

    @Test
    void checkRangesAndSpecialCooldownsMatch111() {
        assertEquals(new ParanoiaPacingRules.IntRange(8, 32),
                ParanoiaPacingRules.autoCheckIntervalTicksRange(1, 1));
        assertEquals(new ParanoiaPacingRules.IntRange(6, 18),
                ParanoiaPacingRules.autoCheckIntervalTicksRange(4, 5));
        assertEquals(new ParanoiaPacingRules.IntRange(11, 25),
                ParanoiaPacingRules.specialCheckIntervalSecondsRange(2, 1));
        assertEquals(new ParanoiaPacingRules.IntRange(3, 9),
                ParanoiaPacingRules.specialCheckIntervalSecondsRange(4, 5));
        assertEquals(29800L, ParanoiaPacingRules.specialGlobalCooldownTicks(2, 1, 0));
        assertEquals(3140L, ParanoiaPacingRules.specialGlobalCooldownTicks(4, 5, 5));
        assertEquals(14400L, ParanoiaPacingRules.specialPerKeyCooldownTicks(FOLLOWER, 4, 5, 5));
        assertEquals(72000L, ParanoiaPacingRules.specialPerKeyCooldownTicks(USHER, 4, 5, 5));
    }

    @Test
    void activePhaseTwoTuningIsNarrowAndKeepsHistoricalMethodsFrozen() {
        assertEquals(11500L, ParanoiaPacingRules.specialGlobalCooldownTicks(2, 3, 3));
        assertEquals(10000L, ParanoiaPacingRules.activeSpecialGlobalCooldownTicks(2, 3, 3));
        assertEquals(
                ParanoiaPacingRules.specialGlobalCooldownTicks(3, 3, 3),
                ParanoiaPacingRules.activeSpecialGlobalCooldownTicks(3, 3, 3));

        assertEquals(15, ParanoiaPacingRules.effectiveWeight(BELL, 14, 3, 3));
        assertEquals(8, ParanoiaPacingRules.activeEffectiveWeight(BELL, 14, 2, 3, 3));
        assertEquals(15, ParanoiaPacingRules.activeEffectiveWeight(BELL, 14, 3, 3, 3));
        assertEquals(2, ParanoiaPacingRules.activeEffectiveWeight(HURLER, 12, 2, 3, 3));
        assertEquals(12, ParanoiaPacingRules.activeEffectiveWeight(HURLER, 12, 3, 3, 3));

        assertEquals(14400L, ParanoiaPacingRules.activeEventCooldownTicks(BELL, 2, 900L));
        assertEquals(900L, ParanoiaPacingRules.activeEventCooldownTicks(BELL, 3, 900L));
        assertEquals(24000L, ParanoiaPacingRules.activeSpecialPerKeyCooldownTicks(HURLER, 2, 3, 3));
    }

    @Test
    void phaseTwoHurlerRequiresTheIntendedMiningBuildUp() {
        assertFalse(ParanoiaPacingRules.allowsHurler(1, true, true));
        assertFalse(ParanoiaPacingRules.allowsHurler(2, false, true));
        assertFalse(ParanoiaPacingRules.allowsHurler(2, true, false));
        assertTrue(ParanoiaPacingRules.allowsHurler(2, true, true));
        assertTrue(ParanoiaPacingRules.allowsHurler(3, false, false));
    }

    @Test
    void sleepRulesAndInputLimitsMatch111() {
        assertEquals(0.0D, ParanoiaPacingRules.sleepDisturbChance(1, 5), 0.0D);
        assertEquals(0.1365D, ParanoiaPacingRules.sleepDisturbChance(4, 5), 1.0E-12D);
        assertEquals(35000L, ParanoiaPacingRules.sleepDisturbCooldownTicks(1, 1, 960));
        assertEquals(25340L, ParanoiaPacingRules.sleepDisturbCooldownTicks(4, 5, 1680));
        assertThrows(IllegalArgumentException.class, () -> ParanoiaPacingRules.autoTriggerChance(0, 3, 3));
        assertThrows(IllegalArgumentException.class, () -> ParanoiaPacingRules.autoTriggerChance(1, 0, 3));
        assertThrows(IllegalArgumentException.class, () -> ParanoiaPacingRules.autoTriggerChance(1, 3, 6));
    }

    @Test
    void completePhaseProfileDangerAndWeightMatrixIsFrozen() {
        StringBuilder snapshot = new StringBuilder();
        for (int phase = 1; phase <= 4; phase++) {
            for (int profile = 1; profile <= 5; profile++) {
                for (int danger = 0; danger <= 5; danger++) {
                    snapshot.append(String.format(Locale.ROOT,
                            "%d|%d|%d|%.12f|%.12f|%d|%d|%d|%.12f|%d%n",
                            phase,
                            profile,
                            danger,
                            ParanoiaPacingRules.autoTriggerChance(phase, profile, danger),
                            ParanoiaPacingRules.ambientTriggerChance(phase, profile, danger),
                            ParanoiaPacingRules.effectiveGlobalCooldownTicks(phase, profile, danger, 480),
                            ParanoiaPacingRules.ambientGlobalCooldownTicks(phase, profile, danger),
                            ParanoiaPacingRules.maxSilenceTicks(phase, profile, danger),
                            ParanoiaPacingRules.specialTriggerChance(phase, profile, danger),
                            ParanoiaPacingRules.specialGlobalCooldownTicks(phase, profile, danger)));
                }
            }
        }
        for (ParanoiaEventDescriptor event : ParanoiaEventCatalog.byId().values()) {
            if (ParanoiaEventCatalog.post111EventIds().contains(event.id())) {
                continue;
            }
            int baseWeight = Math.max(event.primaryWeight(), Math.max(event.ambientWeight(), event.specialWeight()));
            if (baseWeight == 0) {
                continue;
            }
            for (int profile = 1; profile <= 5; profile++) {
                for (int danger = 0; danger <= 5; danger++) {
                    snapshot.append(event.id()).append('|').append(profile).append('|').append(danger).append('|')
                            .append(ParanoiaPacingRules.effectiveWeight(event.id(), baseWeight, profile, danger))
                            .append('\n');
                }
            }
        }
        assertEquals(EXPECTED_FULL_MATRIX_SHA256, sha256(snapshot.toString()));
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().withUpperCase().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
