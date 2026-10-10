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
    public static final int FERRYMAN_PENDING_MIN_NAVIGATION_TICKS = 20 * 10;
    public static final int FERRYMAN_PENDING_MAX_NAVIGATION_TICKS = 20 * 15;
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
    /** When the boat stops, one chance in two that it climbs aboard instead of rising beside it (user, 2026-10-09). */
    public static final double FERRYMAN_BOARD_CHANCE = 0.5D;
    /** Aboard it lets the player notice it before the first blow. */
    public static final int FERRYMAN_FIRST_STRIKE_DELAY_TICKS = 30;
    public static final int FERRYMAN_STRIKE_INTERVAL_TICKS = 36;
    public static final double FERRYMAN_REACH = 2.4D;
    /** Off the boat it hunts the player on for this long at most, then sinks away. */
    public static final int FERRYMAN_PURSUIT_MAX_TICKS = 20 * 45;
    public static final double FERRYMAN_PURSUIT_GIVE_UP_DISTANCE = 40.0D;

    /** @param roll uniform value in [0, 1); a boat needs a free seat behind its driver. */
    public static boolean ferrymanBoards(double roll, int passengers, int maxPassengers) {
        return roll < FERRYMAN_BOARD_CHANCE && passengers < maxPassengers;
    }

    public static final int MOURNER_MIN_OBSERVATION_TICKS = 70;
    public static final int MOURNER_REQUIRED_GAZE_TICKS = 18;
    public static final int MOURNER_ACKNOWLEDGEMENT_TICKS = 100;
    public static final int MOURNER_SINK_TICKS = 48;
    public static final double MOURNER_AUDIBLE_RANGE = 15.0D;
    public static final float MOURNER_SOB_VOLUME = 1.0F;

    public static final float FOLLOWER_PLAYER_MELEE_DAMAGE_CAP = 4.0F;
    public static final double FOLLOWER_EVASION_TRIGGER_DISTANCE = 10.0D;
    public static final double FOLLOWER_EVASION_RELEASE_DISTANCE = 18.0D;
    public static final double FOLLOWER_EVASION_ANCHOR_DISTANCE = 20.0D;
    public static final double FOLLOWER_EVASION_SPEED = 1.68D;
    public static final int FOLLOWER_EVASION_BURST_TICKS = 70;
    public static final int FOLLOWER_INITIAL_REPOSITIONS = 2;
    public static final int FOLLOWER_UNOBSERVED_REPOSITION_TICKS = 8;
    public static final int FOLLOWER_REPOSITION_COOLDOWN_TICKS = 100;
    public static final int FOLLOWER_REPOSITION_RETRY_TICKS = 20;
    public static final int FOLLOWER_POST_REPOSITION_ATTACK_GRACE_TICKS = 60;
    public static final double FOLLOWER_REPOSITION_MIN_DISTANCE = 16.0D;
    public static final double FOLLOWER_REPOSITION_DISTANCE_SPAN = 8.0D;
    public static final int FOLLOWER_REPOSITION_ATTEMPTS = 20;
    public static final double FOLLOWER_OBSERVER_RANGE = 96.0D;
    public static final int FOLLOWER_PURSUIT_REQUIRED_TICKS = 8;
    public static final int FOLLOWER_PURSUIT_ARM_LIFETIME_TICKS = 100;
    public static final double FOLLOWER_PURSUIT_MAX_DISTANCE = 18.0D;
    public static final double FOLLOWER_PURSUIT_MIN_FORWARD_PROGRESS = 0.015D;
    public static final double FOLLOWER_PURSUIT_MIN_CLOSING_DISTANCE = 0.008D;
    public static final int FOLLOWER_REPOSITION_CLOAK_TICKS = 20;
    public static final int FOLLOWER_REPOSITION_TELEPORT_DELAY_TICKS = 3;
    public static final double FOLLOWER_REAR_APPROACH_DISTANCE = 3.25D;
    public static final double FOLLOWER_UNSEEN_FAR_SPEED = 0.52D;
    public static final double FOLLOWER_UNSEEN_NEAR_SPEED = 0.42D;

    public static final double SURVEYOR_RUSH_SPEED = 1.35D;
    public static final int SURVEYOR_RUSH_TICKS = 50;
    public static final double SURVEYOR_STRIKE_REACH = 2.0D;
    public static final double SURVEYOR_CLOSE_TRIGGER = 3.5D;
    public static final float SURVEYOR_STRIKE_DAMAGE = 4.0F;
    public static final float SURVEYOR_LAUGH_VOLUME = 0.9F;
    public static final int SURVEYOR_QUICK_SINK_TICKS = 20;

    /** One real blow that always leaves the player at least one heart. */
    public static float surveyorStrikeDamage(float playerHealth) {
        return Math.max(0.0F, Math.min(SURVEYOR_STRIKE_DAMAGE, playerHealth - 2.0F));
    }

    private ApprovedSpecialBehaviorRules() {
    }

    public static boolean ferrymanBoatIsMoving(double xVelocity, double zVelocity) {
        return xVelocity * xVelocity + zVelocity * zVelocity > FERRYMAN_MOVING_THRESHOLD_SQR;
    }

    public static int ferrymanRequiredNavigationTicks(int boundedRandomValue) {
        return FERRYMAN_PENDING_MIN_NAVIGATION_TICKS
                + clamp(
                        boundedRandomValue,
                        0,
                        FERRYMAN_PENDING_MAX_NAVIGATION_TICKS - FERRYMAN_PENDING_MIN_NAVIGATION_TICKS);
    }

    public static int mournerSobIntervalTicks(int boundedRandomValue) {
        return 90 + clamp(boundedRandomValue, 0, 110);
    }

    public static int ferrymanWakeIntervalTicks(int boundedRandomValue) {
        return 100 + clamp(boundedRandomValue, 0, 120);
    }

    public static float followerPlayerMeleeDamage(float requestedDamage) {
        return Math.max(0.0F, Math.min(FOLLOWER_PLAYER_MELEE_DAMAGE_CAP, requestedDamage));
    }

    /**
     * A Follower? encounter is local. Keeping an owner after an in-dimension teleport would make
     * visibility/path probes traverse unloaded chunks and can stall the server thread.
     */
    public static boolean followerOwnerWithinTrackingRange(double distanceSqr) {
        return Double.isFinite(distanceSqr)
                && distanceSqr <= FOLLOWER_OBSERVER_RANGE * FOLLOWER_OBSERVER_RANGE;
    }

    public static boolean followerCanAttemptReposition(
            int repositionsRemaining,
            boolean evasionArmed,
            boolean observedByAnyPlayer,
            long unobservedTicks,
            long now,
            long nextAttemptTick) {
        return repositionsRemaining > 0
                && evasionArmed
                && !observedByAnyPlayer
                && unobservedTicks >= FOLLOWER_UNOBSERVED_REPOSITION_TICKS
                && now >= nextAttemptTick;
    }

    public static boolean followerPursuitEvidence(
            boolean visibleToOwner,
            double distance,
            double ownerForwardProgress,
            double closingDistance) {
        return visibleToOwner
                && distance <= FOLLOWER_PURSUIT_MAX_DISTANCE
                && ownerForwardProgress >= FOLLOWER_PURSUIT_MIN_FORWARD_PROGRESS
                && closingDistance >= FOLLOWER_PURSUIT_MIN_CLOSING_DISTANCE;
    }

    public static boolean followerCanStartUnseenAttack(
            double distance,
            long now,
            long attackSuppressedUntilTick) {
        return distance <= 1.7D && now >= attackSuppressedUntilTick;
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
     * Within this distance a missing full path means the target is out of reach (a pillar, a moat) and
     * Attacker? may go into hiding. Farther away no path search can reach it anyway: it keeps coming.
     */
    public static final double ATTACKER_PATH_JUDGEMENT_RANGE = 32.0D;

    public static boolean attackerJudgesPathAt(double distanceToTarget) {
        return distanceToTarget <= ATTACKER_PATH_JUDGEMENT_RANGE;
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
