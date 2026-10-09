package com.eotv.echoofthevoid.event.paranoia.nativeevent;

import java.util.ArrayList;
import java.util.List;

/**
 * Minecraft-free rules of the wandering tree: how far it steps, where it stops, when it is armed and
 * what counts as being in a player's field of view. Kept pure so the unit tests can pin them.
 */
public final class WanderingTreeRules {
    /** From this many completed moves, cutting a log makes the whole tree vanish and scream. */
    public static final int TRAP_MOVE_THRESHOLD = 3;
    public static final int MIN_STEP_BLOCKS = 3;
    public static final int MAX_STEP_BLOCKS = 4;
    /** Horizontal distance from the base centre at which the tree settles for good. */
    public static final int STOP_DISTANCE_BLOCKS = 7;
    /** Ring around the base centre in which a natural tree may be chosen (enough room for three moves). */
    public static final int MIN_ADOPT_DISTANCE_BLOCKS = 18;
    public static final int MAX_ADOPT_DISTANCE_BLOCKS = 44;
    public static final long MIN_MOVE_INTERVAL_TICKS = 2_400L;
    public static final long MAX_MOVE_INTERVAL_TICKS = 6_000L;
    public static final long OBSERVED_RETRY_TICKS = 100L;
    public static final long BLOCKED_RETRY_TICKS = 600L;
    /** Consecutive blocked attempts after which the tree simply stays where it is. */
    public static final int MAX_BLOCKED_ATTEMPTS = 30;
    public static final int MAX_RECORDS_PER_OWNER = 2;
    public static final int MAX_RECORDS = 16;
    /** cos(80 deg): wider than any field of view setting, so peripheral vision also counts as seeing. */
    public static final double VIEW_CONE_DOT = 0.17D;
    /** Nobody may stand this close to the tree or to its destination when it moves, even facing away. */
    public static final double MIN_PLAYER_CLEARANCE_BLOCKS = 6.0D;

    private static final int[] DIRECTION_OFFSETS_DEGREES = {0, 30, -30, 55, -55};

    private WanderingTreeRules() {
    }

    public static boolean isArmed(int completedMoves) {
        return completedMoves >= TRAP_MOVE_THRESHOLD;
    }

    public static double horizontalDistance(int fromX, int fromZ, int toX, int toZ) {
        return Math.hypot(toX - fromX, toZ - fromZ);
    }

    /** True once no step of at least one block can be taken without entering the stop radius. */
    public static boolean hasArrived(int fromX, int fromZ, int baseX, int baseZ) {
        return Math.floor(horizontalDistance(fromX, fromZ, baseX, baseZ) - STOP_DISTANCE_BLOCKS) < 1.0D;
    }

    /**
     * Horizontal offsets to try, straight toward the base first, then progressively wider detours.
     * Every candidate brings the tree strictly closer to the base and never crosses the stop radius.
     */
    public static List<int[]> candidateSteps(int fromX, int fromZ, int baseX, int baseZ, int desiredLength) {
        List<int[]> steps = new ArrayList<>();
        double distance = horizontalDistance(fromX, fromZ, baseX, baseZ);
        int length = (int) Math.min(desiredLength, Math.floor(distance - STOP_DISTANCE_BLOCKS));
        if (length < 1) {
            return steps;
        }
        double heading = Math.atan2(baseZ - fromZ, baseX - fromX);
        for (int offsetDegrees : DIRECTION_OFFSETS_DEGREES) {
            double angle = heading + Math.toRadians(offsetDegrees);
            int dx = (int) Math.round(Math.cos(angle) * length);
            int dz = (int) Math.round(Math.sin(angle) * length);
            if (dx == 0 && dz == 0) {
                continue;
            }
            double after = horizontalDistance(fromX + dx, fromZ + dz, baseX, baseZ);
            if (after >= distance || after < STOP_DISTANCE_BLOCKS) {
                continue;
            }
            boolean duplicate = steps.stream().anyMatch(step -> step[0] == dx && step[1] == dz);
            if (!duplicate) {
                steps.add(new int[]{dx, dz});
            }
        }
        return steps;
    }

    /**
     * Ground columns the trunk crosses on its way, excluding the start and including the end, one
     * block apart, so the "only over dirt or grass" rule is checked for the whole step.
     */
    public static List<int[]> pathColumns(int dx, int dz) {
        List<int[]> columns = new ArrayList<>();
        int steps = Math.max(Math.abs(dx), Math.abs(dz));
        for (int i = 1; i <= steps; i++) {
            int x = (int) Math.round(dx * (double) i / steps);
            int z = (int) Math.round(dz * (double) i / steps);
            columns.add(new int[]{x, z});
        }
        return columns;
    }

    public static boolean isInViewCone(
            double lookX, double lookY, double lookZ,
            double toX, double toY, double toZ) {
        double lookLength = Math.sqrt(lookX * lookX + lookY * lookY + lookZ * lookZ);
        double toLength = Math.sqrt(toX * toX + toY * toY + toZ * toZ);
        if (lookLength < 1.0E-6D || toLength < 1.0E-6D) {
            return true;
        }
        double dot = (lookX * toX + lookY * toY + lookZ * toZ) / (lookLength * toLength);
        return dot >= VIEW_CONE_DOT;
    }

    /** @param unit a uniform random value in [0, 1) drawn by the caller */
    public static long nextMoveDelayTicks(double unit) {
        double clamped = Math.max(0.0D, Math.min(1.0D, unit));
        return MIN_MOVE_INTERVAL_TICKS + Math.round(clamped * (MAX_MOVE_INTERVAL_TICKS - MIN_MOVE_INTERVAL_TICKS));
    }
}
