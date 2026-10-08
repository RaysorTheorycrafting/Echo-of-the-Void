package com.eotv.echoofthevoid.event.special;

import com.eotv.echoofthevoid.event.paranoia.GhostMinerRules;

/** Pure timing and bounded-state rules for Miner?. */
public final class MinerRules {
    public static final int MAX_TUNNEL_SECTIONS = 20;
    public static final int MAX_SEARCHED_NODES = 1024;
    public static final int MAX_DEBUG_START_CANDIDATES = 24;
    public static final int MAX_CONSTRUCTION_SECTIONS = 2;
    public static final int RESTORE_DELAY_TICKS = 160;
    public static final int RESTORE_RETRY_TICKS = 10;
    public static final double RESTORATION_OWNER_CLEARANCE = 4.0D;
    public static final double RESTORATION_ENTITY_MARGIN = 0.75D;
    public static final int EMERGENCE_GRACE_TICKS = 20;
    public static final int HUNT_PATH_VALIDATION_TICKS = 40;
    public static final int FLEE_CONFIRM_TICKS = 40;
    public static final double FLEE_DISTANCE_GAIN = 2.0D;
    public static final double MAX_TARGET_DISTANCE = 32.0D;
    public static final double MAX_PHYSICAL_DISPLACEMENT_PER_TICK = 0.65D;
    public static final double TUNNEL_MOVEMENT_SPEED_MODIFIER = 1.0D;
    public static final double PHYSICAL_ARRIVAL_HORIZONTAL_TOLERANCE = 0.32D;
    public static final double PHYSICAL_ARRIVAL_VERTICAL_TOLERANCE = 0.30D;
    public static final int PHYSICAL_REPATH_TICKS = 60;
    public static final int MAX_PHYSICAL_RECOVERY_ATTEMPTS = 3;
    public static final int MAX_OPEN_CONNECTION_NODES = 256;
    public static final int OPEN_CONNECTION_HORIZONTAL_RADIUS = 10;
    public static final int OPEN_CONNECTION_VERTICAL_RADIUS = 4;

    private MinerRules() {
    }

    public static int nextHitDelayTicks(
            int boundedRandomValue,
            boolean completedSection,
            int sectionsAdvanced,
            boolean fleeing) {
        int normal = GhostMinerRules.nextHitDelayTicks(
                boundedRandomValue, completedSection, sectionsAdvanced);
        return fleeing ? Math.max(5, (int) Math.round(normal * 0.60D)) : normal;
    }

    public static boolean confirmsFleeing(int sustainedTicks, double startingDistance, double currentDistance) {
        return sustainedTicks >= FLEE_CONFIRM_TICKS
                && currentDistance - startingDistance >= FLEE_DISTANCE_GAIN;
    }

    public static boolean hasReachedPhysicalDestination(
            double horizontalDistance,
            double verticalDistance,
            boolean hasStableSupport,
            boolean destinationCollisionFree) {
        return horizontalDistance <= PHYSICAL_ARRIVAL_HORIZONTAL_TOLERANCE
                && Math.abs(verticalDistance) <= PHYSICAL_ARRIVAL_VERTICAL_TOLERANCE
                && hasStableSupport
                && destinationCollisionFree;
    }

    public static boolean shouldAttemptPhysicalRecovery(int stalledTicks, int attemptsAlreadyMade) {
        return stalledTicks >= PHYSICAL_REPATH_TICKS
                && attemptsAlreadyMade < MAX_PHYSICAL_RECOVERY_ATTEMPTS;
    }
}
