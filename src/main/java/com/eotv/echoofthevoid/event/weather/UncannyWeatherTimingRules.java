package com.eotv.echoofthevoid.event.weather;

/** Pure timing and audio bounds shared by the weather scheduler and its tests. */
public final class UncannyWeatherTimingRules {
    public static final int DRY_RAIN_MIN_DURATION_TICKS = 20 * 12;
    public static final int DRY_RAIN_MAX_DURATION_TICKS = 20 * 25;
    public static final int DRY_RAIN_MIN_PULSES = 2;
    public static final int DRY_RAIN_MAX_PULSES = 5;
    public static final int DRY_RAIN_MIN_GAP_TICKS = 20 * 5 / 2;
    public static final int DRY_RAIN_MAX_GAP_TICKS = 20 * 5;
    public static final int DRY_RAIN_MAX_PULSE_DURATION_TICKS = 20 * 2;
    public static final int ARTIFICIAL_THUNDER_MIN_DURATION_TICKS = 20 * 15;
    public static final int ARTIFICIAL_THUNDER_MAX_DURATION_TICKS = 20 * 30;

    private static final float DRY_RAIN_MIN_VOLUME = 0.28F;
    private static final float DRY_RAIN_MAX_VOLUME = 0.46F;
    private static final float DRY_RAIN_MIN_PITCH = 0.90F;
    private static final float DRY_RAIN_MAX_PITCH = 1.00F;

    private UncannyWeatherTimingRules() {
    }

    public static int dryRainPulseCount(int randomSample) {
        return DRY_RAIN_MIN_PULSES
                + Math.floorMod(randomSample, DRY_RAIN_MAX_PULSES - DRY_RAIN_MIN_PULSES + 1);
    }

    public static int dryRainPulseGapTicks(int randomSample) {
        return DRY_RAIN_MIN_GAP_TICKS
                + Math.floorMod(randomSample, DRY_RAIN_MAX_GAP_TICKS - DRY_RAIN_MIN_GAP_TICKS + 1);
    }

    public static float dryRainVolume(int randomSample) {
        return interpolateBounded(DRY_RAIN_MIN_VOLUME, DRY_RAIN_MAX_VOLUME, randomSample);
    }

    public static float dryRainPitch(int randomSample) {
        return interpolateBounded(DRY_RAIN_MIN_PITCH, DRY_RAIN_MAX_PITCH, randomSample);
    }

    /** Chooses one occurrence-wide audience: everyone, or one stable eligible player. */
    public static SobbingRainAudience sobbingRainAudience(int eligiblePlayerCount, int randomSample) {
        if (eligiblePlayerCount <= 1) {
            return new SobbingRainAudience(true, -1);
        }
        int slot = Math.floorMod(randomSample, eligiblePlayerCount * 2);
        if (slot < eligiblePlayerCount) {
            return new SobbingRainAudience(true, -1);
        }
        return new SobbingRainAudience(false, slot - eligiblePlayerCount);
    }

    private static float interpolateBounded(float minimum, float maximum, int randomSample) {
        int normalized = Math.floorMod(randomSample, 10_001);
        return minimum + (maximum - minimum) * (normalized / 10_000.0F);
    }

    public record SobbingRainAudience(boolean shared, int targetIndex) {
    }
}
