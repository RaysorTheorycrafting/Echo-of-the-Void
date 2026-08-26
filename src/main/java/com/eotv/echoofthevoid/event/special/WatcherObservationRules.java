package com.eotv.echoofthevoid.event.special;

/** Pure guards for Watcher? spawning and observation. */
public final class WatcherObservationRules {
    public static final double DIRECT_LOOK_DOT_THRESHOLD = 0.93D;

    private WatcherObservationRules() {
    }

    public static boolean blocksEncounter(boolean sleeping, boolean inWaterOrBubble, boolean ridingBoat) {
        return sleeping || inWaterOrBubble || ridingBoat;
    }

    public static boolean canAccumulateDirectLook(
            boolean sleeping,
            boolean hasLineOfSight,
            double normalizedLookDot) {
        return !sleeping
                && hasLineOfSight
                && Double.isFinite(normalizedLookDot)
                && normalizedLookDot > DIRECT_LOOK_DOT_THRESHOLD;
    }
}
