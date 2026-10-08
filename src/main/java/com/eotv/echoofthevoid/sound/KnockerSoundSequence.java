package com.eotv.echoofthevoid.sound;

/**
 * Pure timing and mix contract for the two-part Knocker? cue.
 *
 * <p>The source file lasts a little over 2.2 seconds at normal pitch. At the deliberately
 * lower playback pitch below, reserving 53 ticks covers the complete first knock before the
 * requested two-to-three-second silence begins.</p>
 */
public final class KnockerSoundSequence {
    public static final int FIRST_KNOCK_TICK = 0;
    public static final int SLOWED_SOUND_DURATION_TICKS = 53;
    public static final int MINIMUM_PAUSE_TICKS = 40;
    public static final int MAXIMUM_PAUSE_TICKS = 60;
    public static final float VOLUME = 1.35F;
    public static final float MINIMUM_PITCH = 0.86F;
    public static final float PITCH_SPREAD = 0.04F;

    private KnockerSoundSequence() {
    }

    public static int secondKnockTick(int pauseRoll) {
        int boundedPause = Math.max(0, Math.min(MAXIMUM_PAUSE_TICKS - MINIMUM_PAUSE_TICKS, pauseRoll));
        return SLOWED_SOUND_DURATION_TICKS + MINIMUM_PAUSE_TICKS + boundedPause;
    }

    public static int sequenceEndTick(int secondKnockTick) {
        return Math.max(secondKnockTick, FIRST_KNOCK_TICK) + SLOWED_SOUND_DURATION_TICKS;
    }
}
