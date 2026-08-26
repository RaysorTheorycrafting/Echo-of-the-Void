package com.eotv.echoofthevoid.event.paranoia.nativeevent;

/** Pure timing and path-size rules shared by runtime code and lightweight tests. */
public final class MinecraftNativeAnomalyRules {
    public static final int EMPTY_LEAD_MIN_DURATION_TICKS = 190;
    public static final int EMPTY_LEAD_MAX_DURATION_TICKS = 210;
    public static final int EMPTY_LEAD_MIN_VISIBLE_TICKS = 190;

    public static final int EMPTY_WAKE_MIN_POINTS = 22;
    public static final int EMPTY_WAKE_TARGET_POINTS = 30;
    public static final int EMPTY_WAKE_PULSE_INTERVAL_TICKS = 7;
    public static final int EMPTY_WAKE_MAX_SEARCH_RADIUS = 26;

    private MinecraftNativeAnomalyRules() {
    }

    public static int emptyLeadDurationTicks(int boundedRandomValue) {
        int range = EMPTY_LEAD_MAX_DURATION_TICKS - EMPTY_LEAD_MIN_DURATION_TICKS;
        return EMPTY_LEAD_MIN_DURATION_TICKS + Math.max(0, Math.min(range, boundedRandomValue));
    }

    public static int emptyWakeDurationTicks(int pointCount) {
        return Math.max(0, pointCount) * EMPTY_WAKE_PULSE_INTERVAL_TICKS;
    }
}
