package com.eotv.echoofthevoid.event.special;

/** Pure guards for Watcher? spawning and observation. */
public final class WatcherObservationRules {
    public static final double DIRECT_LOOK_DOT_THRESHOLD = 0.93D;
    /** Close enough to count as "standing near the player" during an approach. */
    public static final double CLOSE_DISTANCE = 18.0D;
    /** How long it may stand near the player before backing far away (user, 2026-10-09: it lingered). */
    public static final int CLOSE_LINGER_MIN_TICKS = 20 * 8;
    public static final int CLOSE_LINGER_RANDOM_TICKS = 20 * 5;
    /** It backs off until this far, then keeps watching from there. */
    public static final double FAR_WATCH_DISTANCE = 64.0D;
    public static final int RETREAT_MAX_TICKS = 20 * 40;

    /** True when the retreat is over: far enough, or it has tried long enough. */
    public static boolean retreatFinished(double distance, int retreatTicksLeft) {
        return distance >= FAR_WATCH_DISTANCE || retreatTicksLeft <= 0;
    }

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
