package com.eotv.echoofthevoid.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KnockerSoundSequenceTest {
    @Test
    void secondCueStartsOnlyAfterTheSlowedSoundAndTwoToThreeSecondsOfSilence() {
        int earliest = KnockerSoundSequence.secondKnockTick(0);
        int latest = KnockerSoundSequence.secondKnockTick(Integer.MAX_VALUE);

        assertEquals(
                KnockerSoundSequence.SLOWED_SOUND_DURATION_TICKS
                        + KnockerSoundSequence.MINIMUM_PAUSE_TICKS,
                earliest);
        assertEquals(
                KnockerSoundSequence.SLOWED_SOUND_DURATION_TICKS
                        + KnockerSoundSequence.MAXIMUM_PAUSE_TICKS,
                latest);
        assertEquals(40, earliest - KnockerSoundSequence.SLOWED_SOUND_DURATION_TICKS);
        assertEquals(60, latest - KnockerSoundSequence.SLOWED_SOUND_DURATION_TICKS);
    }

    @Test
    void sequenceKeepsTheWholeSecondCueBeforeDoorResolution() {
        int second = KnockerSoundSequence.secondKnockTick(11);
        assertEquals(
                KnockerSoundSequence.SLOWED_SOUND_DURATION_TICKS,
                KnockerSoundSequence.sequenceEndTick(second) - second);
    }

    @Test
    void mixIsClearlyStrongerButStillBoundedAndSlightlySlower() {
        assertTrue(KnockerSoundSequence.VOLUME > 1.0F);
        assertTrue(KnockerSoundSequence.VOLUME <= 1.5F);
        assertTrue(KnockerSoundSequence.MINIMUM_PITCH >= 0.8F);
        assertTrue(KnockerSoundSequence.MINIMUM_PITCH + KnockerSoundSequence.PITCH_SPREAD < 1.0F);
    }
}
