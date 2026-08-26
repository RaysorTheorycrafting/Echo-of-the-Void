package com.eotv.echoofthevoid.event.special;

/** Pure timing and eligibility rules shared by Special runtime code and lightweight tests. */
public final class ApprovedSpecialBehaviorRules {
    public static final int FERRYMAN_MIN_WATER_DEPTH = 4;
    public static final double FERRYMAN_VERTICAL_OFFSET = -2.35D;
    public static final double FERRYMAN_MAX_FEET_Y_OFFSET = -2.05D;
    public static final double FERRYMAN_TRAILING_DISTANCE = 1.8D;
    public static final double FERRYMAN_WATER_SAMPLE_RADIUS = 0.27D;
    public static final double FERRYMAN_MAX_HORIZONTAL_STEP = 0.42D;
    public static final double FERRYMAN_MAX_VERTICAL_STEP = 0.18D;
    public static final double FERRYMAN_MOVING_THRESHOLD_SQR = 0.0004D;
    public static final int FERRYMAN_MISSING_BOAT_RETIRE_TICKS = 55;
    public static final int FERRYMAN_IDLE_RISE_DELAY_TICKS = 28;
    public static final int FERRYMAN_REVEAL_TIMEOUT_TICKS = 70;
    public static final int FERRYMAN_REVEAL_HOLD_TICKS = 42;
    public static final int FERRYMAN_DEPARTURE_TICKS = 72;
    public static final double FERRYMAN_REVEAL_DISTANCE = 5.5D;
    public static final double FERRYMAN_REVEAL_FEET_Y_OFFSET = -0.90D;
    public static final double FERRYMAN_REVEAL_MAX_HORIZONTAL_STEP = 0.22D;
    public static final double FERRYMAN_REVEAL_MAX_VERTICAL_STEP = 0.13D;
    public static final double FERRYMAN_DEPARTURE_STEP = 0.11D;
    public static final float FERRYMAN_WAKE_VOLUME = 0.80F;

    public static final int MOURNER_MIN_OBSERVATION_TICKS = 70;
    public static final int MOURNER_REQUIRED_GAZE_TICKS = 18;
    public static final int MOURNER_ACKNOWLEDGEMENT_TICKS = 100;
    public static final int MOURNER_SINK_TICKS = 48;
    public static final double MOURNER_AUDIBLE_RANGE = 15.0D;
    public static final float MOURNER_SOB_VOLUME = 1.0F;

    private ApprovedSpecialBehaviorRules() {
    }

    public static boolean ferrymanBoatIsMoving(double xVelocity, double zVelocity) {
        return xVelocity * xVelocity + zVelocity * zVelocity > FERRYMAN_MOVING_THRESHOLD_SQR;
    }

    public static int mournerSobIntervalTicks(int boundedRandomValue) {
        return 90 + clamp(boundedRandomValue, 0, 110);
    }

    public static int ferrymanWakeIntervalTicks(int boundedRandomValue) {
        return 100 + clamp(boundedRandomValue, 0, 120);
    }

    /** Reflects motion across Doubler?'s vertical separation plane while preserving height. */
    public static MirroredMotion mirrorAcrossHorizontalPlane(
            double motionX,
            double motionY,
            double motionZ,
            double planeNormalX,
            double planeNormalZ) {
        double normalLength = Math.sqrt(
                planeNormalX * planeNormalX + planeNormalZ * planeNormalZ);
        if (normalLength < 1.0E-6D) {
            return new MirroredMotion(-motionX, motionY, -motionZ);
        }
        double normalX = planeNormalX / normalLength;
        double normalZ = planeNormalZ / normalLength;
        double projection = motionX * normalX + motionZ * normalZ;
        return new MirroredMotion(
                motionX - 2.0D * projection * normalX,
                motionY,
                motionZ - 2.0D * projection * normalZ);
    }

    /**
     * Attacker? intentionally has four equally selectable cue modes: silent, distant line-of-sight,
     * close line-of-sight, or a cue only after its first successful hit.
     */
    public static boolean shouldPlayAttackerCue(
            int mode,
            double distanceSquared,
            boolean hasLineOfSight,
            boolean successfulAttack) {
        return switch (mode) {
            case 1 -> hasLineOfSight && distanceSquared <= 14.0D * 14.0D;
            case 2 -> hasLineOfSight && distanceSquared <= 7.0D * 7.0D;
            case 3 -> successfulAttack;
            default -> false;
        };
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record MirroredMotion(double x, double y, double z) {
    }
}
