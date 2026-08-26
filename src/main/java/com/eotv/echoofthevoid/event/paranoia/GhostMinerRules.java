package com.eotv.echoofthevoid.event.paranoia;

import java.util.ArrayList;
import java.util.List;

/** Deterministic geometry and cadence shared by Ghost Miner's runtime and tests. */
public final class GhostMinerRules {
    public static final int MIN_START_DISTANCE = 14;
    public static final int MAX_START_DISTANCE = 18;
    public static final int DEBUG_MIN_START_DISTANCE = 8;
    public static final int DEBUG_MIN_VERTICAL_OFFSET = -12;
    public static final double STOP_DISTANCE = 4.0D;
    public static final int SOUNDS_PER_TUNNEL_SECTION = 2;
    public static final int MIN_PLANNED_SECTIONS = 6;
    public static final int DEBUG_MIN_PLANNED_SECTIONS = 4;
    public static final int MAX_START_CANDIDATES_TO_EVALUATE = 192;

    private GhostMinerRules() {
    }

    public static boolean isValidStartOffset(int dx, int dz) {
        return isValidStartOffset(dx, dz, MIN_START_DISTANCE);
    }

    public static boolean isValidDebugStartOffset(int dx, int dz) {
        return isValidStartOffset(dx, dz, DEBUG_MIN_START_DISTANCE);
    }

    private static boolean isValidStartOffset(int dx, int dz, int minimumDistance) {
        int distanceSquared = dx * dx + dz * dz;
        return distanceSquared >= minimumDistance * minimumDistance
                && distanceSquared <= MAX_START_DISTANCE * MAX_START_DISTANCE;
    }

    public static int soundHeightOffset(int soundsAtCurrentSection) {
        return Math.floorMod(soundsAtCurrentSection, SOUNDS_PER_TUNNEL_SECTION);
    }

    public static boolean completesSection(int soundsAtCurrentSection) {
        return soundsAtCurrentSection >= SOUNDS_PER_TUNNEL_SECTION;
    }

    public static boolean hasReachedClosestApproach(int currentX, int currentZ, int targetX, int targetZ) {
        double dx = currentX - targetX;
        double dz = currentZ - targetZ;
        return dx * dx + dz * dz <= STOP_DISTANCE * STOP_DISTANCE;
    }

    /**
     * Returns at most two cardinal, one-block steps toward the target. The first is the straightest
     * candidate; the second lets the runtime avoid one unsuitable natural column without jumping.
     */
    public static List<HorizontalStep> orderedApproachSteps(
            int currentX,
            int currentZ,
            int targetX,
            int targetZ,
            boolean preferXOnTie) {
        int deltaX = targetX - currentX;
        int deltaZ = targetZ - currentZ;
        int stepX = Integer.signum(deltaX);
        int stepZ = Integer.signum(deltaZ);
        boolean xFirst = Math.abs(deltaX) > Math.abs(deltaZ)
                || (Math.abs(deltaX) == Math.abs(deltaZ) && preferXOnTie);

        List<HorizontalStep> steps = new ArrayList<>(2);
        if (xFirst) {
            addIfMoving(steps, stepX, 0);
            addIfMoving(steps, 0, stepZ);
        } else {
            addIfMoving(steps, 0, stepZ);
            addIfMoving(steps, stepX, 0);
        }
        return List.copyOf(steps);
    }

    public static int nextHitDelayTicks(int boundedRandomValue, boolean completedSection, int sectionsAdvanced) {
        int delay = 9 + Math.max(0, Math.min(7, boundedRandomValue));
        if (completedSection && sectionsAdvanced > 0 && sectionsAdvanced % 4 == 0) {
            delay += 12 + Math.max(0, Math.min(3, boundedRandomValue / 2));
        }
        return delay;
    }

    private static void addIfMoving(List<HorizontalStep> steps, int dx, int dz) {
        if (dx != 0 || dz != 0) {
            steps.add(new HorizontalStep(dx, dz));
        }
    }

    public record HorizontalStep(int dx, int dz) {
        public HorizontalStep {
            if (Math.abs(dx) + Math.abs(dz) != 1) {
                throw new IllegalArgumentException("Ghost Miner steps must move exactly one cardinal block");
            }
        }
    }
}
