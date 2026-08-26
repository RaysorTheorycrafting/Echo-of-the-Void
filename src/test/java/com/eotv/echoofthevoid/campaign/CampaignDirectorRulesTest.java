package com.eotv.echoofthevoid.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventCatalog;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventLane;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventSeverity;
import java.util.List;
import org.junit.jupiter.api.Test;

class CampaignDirectorRulesTest {
    private static final long DAY = CampaignDirectorRules.TICKS_PER_DAY;

    @Test
    void standardCampaignUsesFiftyDayActBoundaries() {
        assertEquals(CampaignAct.OPENING, CampaignDirectorRules.actAt(0L, 50));
        assertEquals(CampaignAct.DEEPENING, CampaignDirectorRules.actAt(5L * DAY, 50));
        assertEquals(CampaignAct.ENCROACHMENT, CampaignDirectorRules.actAt(14L * DAY, 50));
        assertEquals(CampaignAct.FRACTURE, CampaignDirectorRules.actAt(28L * DAY, 50));
        assertEquals(CampaignAct.CULMINATION, CampaignDirectorRules.actAt(41L * DAY, 50));
        assertEquals(CampaignAct.AFTERMATH, CampaignDirectorRules.actAt(50L * DAY, 50));
    }

    @Test
    void extraLongModeStretchesProgressionWithoutChangingLocalBeatDurations() {
        assertEquals(CampaignAct.DEEPENING, CampaignDirectorRules.actAt(10L * DAY, 100));
        assertEquals(CampaignAct.ENCROACHMENT, CampaignDirectorRules.actAt(50L * DAY, 100));
        assertEquals(CampaignAct.CULMINATION, CampaignDirectorRules.actAt(82L * DAY, 100));
        long pressureDuration = CampaignDirectorRules.beatDurationTicks(CampaignBeat.PRESSURE, 0.42D);
        assertTrue(pressureDuration >= Math.round(0.25D * DAY));
        assertTrue(pressureDuration <= Math.round(0.60D * DAY));
        assertThrows(IllegalArgumentException.class, () -> CampaignDirectorRules.actAt(0L, 75));
    }

    @Test
    void campaignClockCountsSleepButCannotRewindOrSkipMoreThanOneDayAtOnce() {
        assertEquals(0L, CampaignDirectorRules.elapsedDelta(Long.MIN_VALUE, 6_000L));
        assertEquals(1L, CampaignDirectorRules.elapsedDelta(6_000L, 6_000L));
        assertEquals(1L, CampaignDirectorRules.elapsedDelta(6_000L, 2_000L));
        assertEquals(11_000L, CampaignDirectorRules.elapsedDelta(6_000L, 17_000L));
        assertEquals(DAY, CampaignDirectorRules.elapsedDelta(6_000L, 300_000L));
    }

    @Test
    void beatsCreateBreathingPressureReleaseAndAftershockInsteadOfOneRamp() {
        double base = 0.10D;
        double restingSpecial = CampaignDirectorRules.triggerChance(
                base, ParanoiaEventLane.SPECIAL, CampaignAct.ENCROACHMENT, CampaignBeat.REST);
        double releaseSpecial = CampaignDirectorRules.triggerChance(
                base, ParanoiaEventLane.SPECIAL, CampaignAct.ENCROACHMENT, CampaignBeat.RELEASE);
        double uneasyAmbient = CampaignDirectorRules.triggerChance(
                base, ParanoiaEventLane.AMBIENT, CampaignAct.OPENING, CampaignBeat.UNEASE);
        double releaseAmbient = CampaignDirectorRules.triggerChance(
                base, ParanoiaEventLane.AMBIENT, CampaignAct.OPENING, CampaignBeat.RELEASE);

        assertTrue(releaseSpecial > restingSpecial);
        assertTrue(uneasyAmbient > releaseAmbient);
        assertEquals(CampaignBeat.UNEASE, CampaignDirectorRules.nextBeat(CampaignBeat.REST, 0.99D));
        assertEquals(CampaignBeat.AFTERSHOCK, CampaignDirectorRules.nextBeat(CampaignBeat.RELEASE, 0.01D));
        for (CampaignBeat beat : CampaignBeat.values()) {
            assertTrue(CampaignDirectorRules.beatDurationTicks(beat, 0.0D) > 0L);
            assertTrue(CampaignDirectorRules.beatDurationTicks(beat, Math.nextDown(1.0D))
                    >= CampaignDirectorRules.beatDurationTicks(beat, 0.0D));
        }
    }

    @Test
    void recentFamiliesAndRecentStrongEventsArePenalizedButNeverMadeImpossible() {
        int novel = CampaignDirectorRules.adjustedWeight(
                20, ParanoiaEventLane.PRIMARY, ParanoiaEventSeverity.LIGHT,
                CampaignEventFamily.SOUND_TRAIL, List.of(), 10L * DAY, Long.MIN_VALUE,
                CampaignAct.ENCROACHMENT, CampaignBeat.PRESSURE);
        int repeated = CampaignDirectorRules.adjustedWeight(
                20, ParanoiaEventLane.PRIMARY, ParanoiaEventSeverity.LIGHT,
                CampaignEventFamily.SOUND_TRAIL, List.of("SOUND_TRAIL"), 10L * DAY, Long.MIN_VALUE,
                CampaignAct.ENCROACHMENT, CampaignBeat.PRESSURE);
        int strongTooSoon = CampaignDirectorRules.adjustedWeight(
                20, ParanoiaEventLane.SPECIAL, ParanoiaEventSeverity.HIGH,
                CampaignEventFamily.PRESENCE, List.of(), 10L * DAY, 10L * DAY - 100L,
                CampaignAct.ENCROACHMENT, CampaignBeat.AFTERSHOCK);

        assertTrue(novel > repeated);
        assertTrue(repeated > 0);
        assertTrue(strongTooSoon > 0);
    }

    @Test
    void deterministicRollsAreStableAndActiveSchedulerIdsHaveKnownFamilies() {
        double first = CampaignDirectorRules.deterministicUnit(42L, 7, 9L);
        assertEquals(first, CampaignDirectorRules.deterministicUnit(42L, 7, 9L), 0.0D);
        assertNotEquals(first, CampaignDirectorRules.deterministicUnit(42L, 8, 9L));
        assertTrue(first >= 0.0D && first < 1.0D);
        assertEquals(CampaignEventFamily.PRESENCE, CampaignEventFamily.forEvent("double_dormant"));

        ParanoiaEventCatalog.primaryEvents().forEach(event ->
                assertNotEquals(CampaignEventFamily.UNKNOWN, CampaignEventFamily.forEvent(event.id()), event.id()));
        ParanoiaEventCatalog.ambientEvents().forEach(event ->
                assertNotEquals(CampaignEventFamily.UNKNOWN, CampaignEventFamily.forEvent(event.id()), event.id()));
        ParanoiaEventCatalog.specialEvents().forEach(event ->
                assertNotEquals(CampaignEventFamily.UNKNOWN, CampaignEventFamily.forEvent(event.id()), event.id()));
    }

    @Test
    void culminationIsDeterministicBoundedAndNeverForcedAtTheLastPercent() {
        long standard = CampaignDirectorRules.culminationScheduledTick(42L, 50);
        long extraLong = CampaignDirectorRules.culminationScheduledTick(42L, 100);
        assertTrue(standard >= Math.round(42.0D * DAY));
        assertTrue(standard <= Math.round(47.0D * DAY));
        assertTrue(Math.abs(standard * 2L - extraLong) <= 1L);
        assertEquals(standard, CampaignDirectorRules.culminationScheduledTick(42L, 50));

        assertEquals(CampaignCulminationAction.NONE, CampaignDirectorRules.culminationAction(
                standard - 1L, 50, CampaignCulminationState.PENDING, standard, standard));
        assertEquals(CampaignCulminationAction.TRY_TENSION_BUILDER,
                CampaignDirectorRules.culminationAction(
                        standard, 50, CampaignCulminationState.PENDING, standard, standard));
        assertEquals(CampaignCulminationAction.NONE, CampaignDirectorRules.culminationAction(
                Math.round(49.5D * DAY), 50, CampaignCulminationState.PENDING, standard, standard));
        assertEquals(CampaignCulminationAction.EXPIRE, CampaignDirectorRules.culminationAction(
                50L * DAY, 50, CampaignCulminationState.PENDING, standard, standard));
        assertEquals(CampaignCulminationAction.NONE, CampaignDirectorRules.culminationAction(
                standard, 50, CampaignCulminationState.SATISFIED, standard, standard));
    }

    @Test
    void naturalMajorsAndStrongSpacingUseCampaignTime() {
        assertFalse(CampaignDirectorRules.naturalMajorCanSatisfy(
                Math.round(39.99D * DAY), 50));
        assertTrue(CampaignDirectorRules.naturalMajorCanSatisfy(40L * DAY, 50));
        assertTrue(CampaignDirectorRules.naturalMajorCanSatisfy(80L * DAY, 100));
        assertFalse(CampaignDirectorRules.naturalMajorCanSatisfy(50L * DAY, 50));

        assertTrue(CampaignDirectorRules.hasStrongEventGap(42L * DAY, Long.MIN_VALUE));
        assertFalse(CampaignDirectorRules.hasStrongEventGap(
                42L * DAY, 42L * DAY - CampaignDirectorRules.CULMINATION_STRONG_EVENT_GAP_TICKS + 1L));
        assertTrue(CampaignDirectorRules.hasStrongEventGap(
                42L * DAY, 42L * DAY - CampaignDirectorRules.CULMINATION_STRONG_EVENT_GAP_TICKS));
    }
}
