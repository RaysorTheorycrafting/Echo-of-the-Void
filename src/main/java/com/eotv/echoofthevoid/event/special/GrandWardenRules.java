package com.eotv.echoofthevoid.event.special;

/** Stable identifiers and bounded, deterministic values shared by every Grand Warden surface. */
public final class GrandWardenRules {
    public static final String ENTITY_TAG = "eotv_grand_warden";
    public static final String DISPLAY_NAME = "Warden?";
    public static final int PRESPAWN_DELAY_MIN_SECONDS = 5;
    public static final int PRESPAWN_DELAY_MAX_SECONDS = 7;
    public static final int MAX_RUNTIME_SECONDS = 5 * 60 - PRESPAWN_DELAY_MAX_SECONDS;

    private GrandWardenRules() {
    }

    public static int preSpawnDelaySeconds(int boundedRoll) {
        int size = PRESPAWN_DELAY_MAX_SECONDS - PRESPAWN_DELAY_MIN_SECONDS + 1;
        if (boundedRoll < 0 || boundedRoll >= size) {
            throw new IllegalArgumentException("boundedRoll must be in [0, " + size + ")");
        }
        return PRESPAWN_DELAY_MIN_SECONDS + boundedRoll;
    }
}
