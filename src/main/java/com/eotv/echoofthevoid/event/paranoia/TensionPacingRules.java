package com.eotv.echoofthevoid.event.paranoia;

/**
 * Pure timing and probability contract shared by the live Tension Builder and release simulator.
 */
public final class TensionPacingRules {
    public static final int TICKS_PER_SECOND = 20;
    public static final int TENSION_MIN_SECONDS = 5 * 60;
    public static final int TENSION_MAX_SECONDS = 10 * 60;
    public static final int BREAK_MIN_SECONDS = 25 * 60;
    public static final int BREAK_MAX_SECONDS = 50 * 60;
    public static final int GRAND_BOOST_MIN_SECONDS = 45;
    public static final int GRAND_BOOST_MAX_SECONDS = 110;
    public static final int GRAND_ROLL_MIN_SECONDS = 10;
    public static final int GRAND_ROLL_MAX_SECONDS = 24;
    public static final int GRAND_COOLDOWN_SECONDS = 35 * 60;
    public static final double GRAND_BASE_CHANCE = 0.0D;
    public static final double GRAND_POST_TENSION_CHANCE = 0.22D;

    private TensionPacingRules() {
    }

    public static int sampleInclusive(int minimum, int maximum, int randomSample) {
        if (maximum <= minimum) {
            return minimum;
        }
        return minimum + Math.floorMod(randomSample, maximum - minimum + 1);
    }

    public static long secondsToTicks(int seconds) {
        return seconds * (long) TICKS_PER_SECOND;
    }
}
