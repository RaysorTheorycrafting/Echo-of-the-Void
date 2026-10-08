package com.eotv.echoofthevoid.event.paranoia;

/** Pure eligibility and probability contract for False Fall's rare physical follow-up. */
public final class FalseFallFollowupRules {
    public static final int MINIMUM_PHASE = 2;
    public static final int MINIMUM_DANGER = 1;
    public static final int AMBUSH_ROLL_BOUND = 100;

    private FalseFallFollowupRules() {
    }

    public static boolean shouldAttemptAmbusher(
            int phaseIndex,
            int dangerLevel,
            boolean stableGroundContext,
            int randomRoll) {
        return phaseIndex >= MINIMUM_PHASE
                && dangerLevel >= MINIMUM_DANGER
                && stableGroundContext
                && Math.floorMod(randomRoll, AMBUSH_ROLL_BOUND) == 0;
    }
}
