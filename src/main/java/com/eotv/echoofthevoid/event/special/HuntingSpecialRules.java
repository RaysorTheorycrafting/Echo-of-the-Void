package com.eotv.echoofthevoid.event.special;

import java.util.Locale;

/**
 * Minecraft-free contracts for Echoer?, Drifter?, Ashwalker?, Dredger? and Flanker?.
 * Runtime entities consume these values directly so the scheduler, QA tools and tests cannot
 * quietly acquire different tuning.
 */
public final class HuntingSpecialRules {
    public static final int TICKS_PER_SECOND = 20;

    public static final int ECHOER_MINIMUM_PHASE = 2;
    public static final int ECHOER_MINIMUM_DANGER = 3;
    public static final int ECHOER_WEIGHT = 3;
    public static final int ECHOER_COOLDOWN_SECONDS = 2_400;
    public static final int ECHOER_MEMORY_CAPACITY = 64;
    public static final int ECHOER_MEMORY_TTL_TICKS = 30 * TICKS_PER_SECOND;
    public static final int ECHOER_MAX_REPLAYS = 6;
    public static final int ECHOER_REPLAY_MIN_TICKS = 5 * TICKS_PER_SECOND;
    public static final int ECHOER_REPLAY_MAX_TICKS = 9 * TICKS_PER_SECOND;
    public static final int ECHOER_HIDDEN_MIN_TICKS = 8 * TICKS_PER_SECOND;
    public static final int ECHOER_HIDDEN_MAX_TICKS = 12 * TICKS_PER_SECOND;
    public static final int ECHOER_ATTACK_GRACE_TICKS = 20;
    public static final float ECHOER_PRE_AGGRO_PLAYER_DAMAGE_CAP = 4.0F;
    public static final double ECHOER_OUTER_REAR_DISTANCE = 6.5D;
    public static final double ECHOER_CLOSE_REAR_DISTANCE = 2.8D;
    public static final double ECHOER_AGGRO_MIN_DISTANCE = 2.4D;
    public static final double ECHOER_AGGRO_MAX_DISTANCE = 3.1D;
    public static final int ECHOER_FLEE_MIN_DISTANCE = 18;
    public static final int ECHOER_FLEE_MAX_DISTANCE = 24;
    public static final double ECHOER_FLEE_SPEED = 1.65D;
    public static final int ECHOER_FLEE_STAGGER_TICKS = 12;
    /** Decoys sit 4-8 blocks away: far enough to mislead, near enough to be heard. */
    public static final int ECHOER_DECOY_MIN_DISTANCE = 4;
    public static final int ECHOER_DECOY_MAX_DISTANCE = 8;
    public static final int ECHOER_STEP_REPLAY_COUNT = 4;
    public static final int ECHOER_STEP_REPLAY_INTERVAL_TICKS = 7;
    public static final float ECHOER_STEP_REPLAY_VOLUME = 0.55F;
    /** A second hit this close while fleeing, or a blocked escape, makes it fight back. */
    public static final double ECHOER_CORNERED_DISTANCE = 4.5D;
    public static final int ECHOER_STALLED_FLEE_TICKS = 20;
    public static final int ECHOER_CORNERED_GRACE_TICKS = 6;

    public static final int DRIFTER_MINIMUM_PHASE = 2;
    public static final int DRIFTER_MINIMUM_DANGER = 3;
    public static final int DRIFTER_WEIGHT = 3;
    public static final int DRIFTER_COOLDOWN_SECONDS = 1_800;
    public static final int DRIFTER_LAND_PURSUIT_TICKS = 8 * TICKS_PER_SECOND;
    public static final int DRIFTER_DRY_DAMAGE_START_TICKS = 10 * TICKS_PER_SECOND;
    public static final int DRIFTER_WATER_RECOVERY_TICKS = TICKS_PER_SECOND;
    public static final float DRIFTER_DRY_DAMAGE = 4.0F;
    public static final int DRIFTER_MIN_LIFETIME_TICKS = 3 * 60 * TICKS_PER_SECOND;
    public static final int DRIFTER_MAX_LIFETIME_TICKS = 5 * 60 * TICKS_PER_SECOND;

    public static final int ASHWALKER_MINIMUM_PHASE = 2;
    public static final int ASHWALKER_MINIMUM_DANGER = 3;
    public static final int ASHWALKER_WEIGHT = 4;
    public static final int ASHWALKER_COOLDOWN_SECONDS = 1_800;
    public static final int ASHWALKER_PATH_NODE_LIMIT = 512;
    public static final int ASHWALKER_SUBMERGE_MIN_TICKS = 3 * TICKS_PER_SECOND;
    public static final int ASHWALKER_SUBMERGE_MAX_TICKS = 5 * TICKS_PER_SECOND;
    public static final int ASHWALKER_FIRST_BITE_GRACE_MIN_TICKS = 12;
    public static final int ASHWALKER_FIRST_BITE_GRACE_MAX_TICKS = 16;
    /** A real leap out of the lava: a player cannot keep it at bay by standing back. */
    public static final double ASHWALKER_MAX_LUNGE_DISTANCE = 7.0D;
    public static final int ASHWALKER_MAX_LUNGE_RISE = 3;
    public static final int ASHWALKER_MAX_LAND_TICKS = 70;
    /** It bites at most twice on land before diving back. */
    public static final int ASHWALKER_LAND_BITES = 2;
    public static final int ASHWALKER_BITE_INTERVAL_TICKS = 16;
    public static final double ASHWALKER_LAND_SPEED = 0.24D;
    /** Lava boils for this long before every leap after the first, telegraphed one. */
    public static final int ASHWALKER_LEAP_TELEGRAPH_TICKS = 6;
    /** A return that has not closed on its lava for this long slips back in through the edge. */
    public static final int ASHWALKER_RETURN_STALL_TICKS = 10;
    private static final double LEAP_GRAVITY = 0.08D;
    private static final double LEAP_VERTICAL_DRAG = 0.98D;
    // Longer than a full leap, land hunt and return: only a truly stranded body dries out.
    public static final int ASHWALKER_STRANDED_DAMAGE_START_TICKS = 100;
    public static final float ASHWALKER_STRANDED_DAMAGE = 4.0F;

    public static final int DREDGER_MINIMUM_PHASE = 3;
    public static final int DREDGER_MINIMUM_DANGER = 4;
    public static final int DREDGER_WEIGHT = 2;
    public static final int DREDGER_COOLDOWN_SECONDS = 2_700;
    public static final int DREDGER_TELEGRAPH_TICKS = 30;
    public static final int DREDGER_HITS_TO_RELEASE = 3;
    public static final int DREDGER_REGRAB_COOLDOWN_TICKS = 60;
    public static final int DREDGER_DAMAGE_INTERVAL_TICKS = 20;
    public static final int DREDGER_BITE_INTERVAL_TICKS = 20;
    public static final double DREDGER_BITE_REACH = 2.6D;
    public static final double DREDGER_MAX_HORIZONTAL_ACCELERATION = 0.08D;
    public static final double DREDGER_MAX_DOWNWARD_ACCELERATION = 0.06D;
    public static final double DREDGER_MAX_HORIZONTAL_VELOCITY = 0.16D;
    public static final double DREDGER_MIN_VERTICAL_VELOCITY = -0.12D;
    public static final double DREDGER_MAX_VERTICAL_VELOCITY = 0.04D;
    public static final double DREDGER_GRAB_START_DISTANCE = 5.5D;
    public static final double DREDGER_GRAB_BREAK_DISTANCE = 6.5D;
    public static final double AQUATIC_MAX_VERTICAL_SPEED = 0.10D;
    public static final double AQUATIC_MAX_HORIZONTAL_SPEED = 0.32D;

    public static final int FLANKER_MINIMUM_PHASE = 3;
    public static final int FLANKER_MINIMUM_DANGER = 4;
    public static final int FLANKER_WEIGHT = 2;
    public static final int FLANKER_COOLDOWN_SECONDS = 3_600;
    public static final int FLANKER_MAX_PATH_PROBES = 16;
    public static final int FLANKER_REPLAN_INTERVAL_TICKS = 20;
    /** Without two routes the pair holds still and waits this long for workable terrain. */
    public static final int FLANKER_NO_GEOMETRY_TIMEOUT_TICKS = 400;
    public static final double FLANKER_MAX_WATER_SURFACE_FRACTION = 0.15D;
    public static final int FLANKER_SINK_TICKS = 40;
    public static final int FLANKER_SURVIVOR_CHASE_TICKS = 400;
    public static final int FLANKER_PINCER_CHASE_TICKS = 300;
    /** After this long without a ring, check whether one member is stranded below the target. */
    public static final int FLANKER_STRANDED_CHECK_TICKS = 100;
    /** Never plan a path that drops more than this many blocks. */
    public static final int FLANKER_MAX_PLANNED_DROP = 2;
    public static final int FLANKER_ATTACK_INTERVAL_TICKS = 16;
    public static final int FLANKER_FIRST_ENCIRCLEMENT_GRACE_TICKS = 16;
    public static final int FLANKER_RESPONSE_DELAY_TICKS = 8;
    public static final double FLANKER_MIN_SEPARATION_DEGREES = 140.0D;
    public static final int FLANKER_MIN_ENCOUNTER_AGE_TICKS = 40;
    public static final int FLANKER_STABLE_FORMATION_TICKS = 20;
    public static final double FLANKER_WATCHED_MIN_RADIUS = 11.0D;
    public static final double FLANKER_WATCHED_MAX_RADIUS = 13.0D;
    public static final double FLANKER_ADVANCING_MIN_RADIUS = 8.0D;
    public static final double FLANKER_ADVANCING_MAX_RADIUS = 10.0D;
    public static final double FLANKER_ATTACK_STAGING_RADIUS = 2.8D;
    public static final double FLANKER_ENCIRCLEMENT_ATTACK_DISTANCE = 3.6D;
    /** Navigation stops this far from a destination, so ring checks must accept the same slack. */
    public static final double FLANKER_ARRIVAL_TOLERANCE = 1.5D;
    /** The staging member keeps closing until it is safely inside melee reach, not merely near its node. */
    public static final double FLANKER_STAGING_HOLD_DISTANCE = 3.0D;
    public static final double FLANKER_ADVANCING_NAVIGATION_SPEED = 1.58D;
    public static final double FLANKER_WATCHED_NAVIGATION_SPEED = 1.30D;
    public static final double FLANKER_EVASION_NAVIGATION_SPEED = 1.70D;
    public static final double FLANKER_EVASION_TRIGGER_DISTANCE = 9.0D;
    public static final double FLANKER_EVASION_DESTINATION_RADIUS = 13.0D;
    public static final double FLANKER_FOLLOW_RANGE = 96.0D;
    public static final double FLANKER_MAX_FOCUS_DISTANCE = 96.0D;

    private HuntingSpecialRules() {
    }

    public static long fixedCooldownTicks(String id) {
        return switch (normalize(id)) {
            case "echoer" -> ECHOER_COOLDOWN_SECONDS * (long) TICKS_PER_SECOND;
            case "drifter" -> DRIFTER_COOLDOWN_SECONDS * (long) TICKS_PER_SECOND;
            case "ashwalker" -> ASHWALKER_COOLDOWN_SECONDS * (long) TICKS_PER_SECOND;
            case "dredger" -> DREDGER_COOLDOWN_SECONDS * (long) TICKS_PER_SECOND;
            case "flanker" -> FLANKER_COOLDOWN_SECONDS * (long) TICKS_PER_SECOND;
            default -> -1L;
        };
    }

    public static AdaptiveSpecialCombatProfile echoerProfile(AdaptiveSpecialCombatProfile attacker) {
        if (attacker == null) {
            throw new IllegalArgumentException("attacker profile is required");
        }
        return new AdaptiveSpecialCombatProfile(
                attacker.maxHealth() * 0.80D,
                attacker.attackDamage() * 0.80D,
                clamp(attacker.movementSpeed() * 0.90D, 0.32D, 0.44D),
                attacker.armor() * 0.80D,
                attacker.armorToughness() * 0.80D);
    }

    public static AdaptiveSpecialCombatProfile flankerProfile(AdaptiveSpecialCombatProfile attacker) {
        if (attacker == null) {
            throw new IllegalArgumentException("attacker profile is required");
        }
        return new AdaptiveSpecialCombatProfile(
                Math.max(1.0D, attacker.maxHealth() * 0.50D),
                Math.max(1.0D, attacker.attackDamage() * 0.50D),
                clamp(attacker.movementSpeed() * 1.12D, 0.40D, 0.48D),
                attacker.armor() * 0.50D,
                attacker.armorToughness() * 0.50D);
    }

    public static float echoerPreAggroDamage(float requestedDamage, boolean directPlayerAttack) {
        if (!Float.isFinite(requestedDamage) || requestedDamage <= 0.0F) {
            return 0.0F;
        }
        return directPlayerAttack
                ? Math.min(ECHOER_PRE_AGGRO_PLAYER_DAMAGE_CAP, requestedDamage)
                : requestedDamage;
    }

    public static boolean isSafeEchoSoundPath(String soundPath) {
        String path = normalize(soundPath);
        if (path.isEmpty() || isDangerousEchoSoundPath(path)) {
            return false;
        }
        return path.contains("step")
                || path.contains("door")
                || path.contains("trapdoor")
                || path.contains("chest")
                || path.contains("barrel")
                || path.contains("shulker")
                || path.contains("block.break")
                || path.contains("block.place")
                || path.contains("stone.break")
                || path.contains("deepslate.break")
                || path.contains("dirt.break")
                || path.contains("gravel.break")
                || path.contains("sand.break");
    }

    public static boolean isDangerousEchoSoundPath(String soundPath) {
        String path = normalize(soundPath);
        return path.contains("explode")
                || path.contains("explosion")
                || path.contains("hurt")
                || path.contains("death")
                || path.contains("creeper")
                || path.contains("warden")
                || path.contains("shrieker")
                || path.contains("sonic_boom")
                || path.contains("raid")
                || path.contains("totem")
                || path.contains("uncanny_echoer_replay");
    }

    /** Flight time of an Ashwalker leap: longer leaps take a little longer, never sluggish. */
    public static int ashwalkerLeapTicks(double horizontalDistance) {
        return (int) Math.round(Math.max(7.0D, Math.min(13.0D, 4.0D + horizontalDistance * 1.2D)));
    }

    /**
     * Initial vertical speed so that, under Vanilla gravity and drag, the body is {@code rise}
     * blocks higher after {@code ticks} ticks and has cleared the shore edge on the way.
     */
    public static double ashwalkerLeapVerticalVelocity(double rise, int ticks) {
        double low = 0.0D;
        double high = 2.0D;
        for (int iteration = 0; iteration < 40; iteration++) {
            double middle = (low + high) * 0.5D;
            if (simulatedLeapHeight(middle, ticks) < rise + 0.35D) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return high;
    }

    static double simulatedLeapHeight(double verticalVelocity, int ticks) {
        double y = 0.0D;
        double velocity = verticalVelocity;
        for (int tick = 0; tick < ticks; tick++) {
            y += velocity;
            velocity = (velocity - LEAP_GRAVITY) * LEAP_VERTICAL_DRAG;
        }
        return y;
    }

    public static int nextDrifterDryTicks(int currentDryTicks, boolean continuouslyInWater) {
        return continuouslyInWater ? 0 : Math.max(0, currentDryTicks) + 1;
    }

    public static boolean shouldDamageDryDrifter(int dryTicks) {
        return dryTicks >= DRIFTER_DRY_DAMAGE_START_TICKS
                && (dryTicks - DRIFTER_DRY_DAMAGE_START_TICKS) % TICKS_PER_SECOND == 0;
    }

    public static int extendAshwalkerSubmergeTicks(int rolledDurationTicks, int currentRemainingTicks) {
        int requested = clamp(
                rolledDurationTicks,
                ASHWALKER_SUBMERGE_MIN_TICKS,
                ASHWALKER_SUBMERGE_MAX_TICKS);
        return Math.min(ASHWALKER_SUBMERGE_MAX_TICKS, Math.max(currentRemainingTicks, requested));
    }

    public static double clampHorizontalPull(double value) {
        return clamp(value, -DREDGER_MAX_HORIZONTAL_ACCELERATION, DREDGER_MAX_HORIZONTAL_ACCELERATION);
    }

    public static double clampDownwardPull(double value) {
        return clamp(value, -DREDGER_MAX_DOWNWARD_ACCELERATION, 0.0D);
    }

    public static double boundedDredgerHorizontalVelocity(double currentVelocity, double requestedAcceleration) {
        double current = Double.isFinite(currentVelocity) ? currentVelocity : 0.0D;
        double acceleration = clampHorizontalPull(requestedAcceleration);
        return clamp(
                current * 0.82D + acceleration,
                -DREDGER_MAX_HORIZONTAL_VELOCITY,
                DREDGER_MAX_HORIZONTAL_VELOCITY);
    }

    public static HorizontalVelocity boundedDredgerHorizontalVelocity(
            double currentX,
            double currentZ,
            double requestedX,
            double requestedZ) {
        double x = boundedDredgerHorizontalVelocity(currentX, requestedX);
        double z = boundedDredgerHorizontalVelocity(currentZ, requestedZ);
        double magnitude = Math.hypot(x, z);
        if (magnitude > DREDGER_MAX_HORIZONTAL_VELOCITY) {
            double scale = DREDGER_MAX_HORIZONTAL_VELOCITY / magnitude;
            x *= scale;
            z *= scale;
        }
        return new HorizontalVelocity(x, z);
    }

    public static double boundedDredgerVerticalVelocity(double currentVelocity, double requestedAcceleration) {
        double current = Double.isFinite(currentVelocity) ? currentVelocity : 0.0D;
        double acceleration = clampDownwardPull(requestedAcceleration);
        return clamp(
                current * 0.75D + acceleration,
                DREDGER_MIN_VERTICAL_VELOCITY,
                DREDGER_MAX_VERTICAL_VELOCITY);
    }

    public static double boundedAquaticVerticalVelocity(double velocity) {
        double finite = Double.isFinite(velocity) ? velocity : 0.0D;
        return clamp(finite, -AQUATIC_MAX_VERTICAL_SPEED, AQUATIC_MAX_VERTICAL_SPEED);
    }

    public static HorizontalVelocity boundedAquaticHorizontalVelocity(double x, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(z)) {
            return new HorizontalVelocity(0.0D, 0.0D);
        }
        double magnitude = Math.hypot(x, z);
        if (magnitude <= AQUATIC_MAX_HORIZONTAL_SPEED || magnitude < 1.0E-9D) {
            return new HorizontalVelocity(x, z);
        }
        double scale = AQUATIC_MAX_HORIZONTAL_SPEED / magnitude;
        return new HorizontalVelocity(x * scale, z * scale);
    }

    public static double releasedDredgerVerticalVelocity(double currentVerticalVelocity) {
        if (!Double.isFinite(currentVerticalVelocity)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(DREDGER_MAX_VERTICAL_VELOCITY, currentVerticalVelocity));
    }

    public static boolean canTriggerEchoerAggro(double distance, boolean behind, boolean lineOfSight) {
        return Double.isFinite(distance)
                && distance >= ECHOER_AGGRO_MIN_DISTANCE
                && distance <= ECHOER_AGGRO_MAX_DISTANCE
                && behind
                && lineOfSight;
    }

    public static boolean isFlankerTerrainDryEnough(int waterSurfaceSamples, int solidSurfaceSamples) {
        int total = Math.max(0, waterSurfaceSamples) + Math.max(0, solidSurfaceSamples);
        return total > 0 && waterSurfaceSamples <= total * FLANKER_MAX_WATER_SURFACE_FRACTION;
    }

    public static boolean canAdvancingFlankerStage(double distanceToFocus) {
        return distanceToFocus <= FLANKER_ADVANCING_MAX_RADIUS + FLANKER_ARRIVAL_TOLERANCE;
    }

    public static boolean isFlankerHoldingStagedStrike(double distanceToFocus) {
        return distanceToFocus <= FLANKER_STAGING_HOLD_DISTANCE;
    }

    public static boolean isStableFlankerEncirclement(
            int encounterAgeTicks,
            int stableTicks,
            double attackerDistance,
            boolean attackerBehind,
            double firstX,
            double firstZ,
            double secondX,
            double secondZ) {
        return encounterAgeTicks >= FLANKER_MIN_ENCOUNTER_AGE_TICKS
                && stableTicks >= FLANKER_STABLE_FORMATION_TICKS
                && attackerDistance <= FLANKER_ENCIRCLEMENT_ATTACK_DISTANCE
                && attackerBehind
                && hasValidFlankerSeparation(firstX, firstZ, secondX, secondZ);
    }

    public static boolean releasesDredgerGrab(int acceptedHits) {
        return acceptedHits >= DREDGER_HITS_TO_RELEASE;
    }

    public static boolean canMaintainDredgerGrab(
            double distance,
            boolean lineOfSight,
            boolean clearWaterRoute) {
        return Double.isFinite(distance)
                && distance <= DREDGER_GRAB_BREAK_DISTANCE
                && lineOfSight
                && clearWaterRoute;
    }

    public static double angularSeparationDegrees(
            double firstX,
            double firstZ,
            double secondX,
            double secondZ) {
        double firstLength = Math.hypot(firstX, firstZ);
        double secondLength = Math.hypot(secondX, secondZ);
        if (firstLength < 1.0E-6D || secondLength < 1.0E-6D) {
            return 0.0D;
        }
        double cosine = (firstX * secondX + firstZ * secondZ) / (firstLength * secondLength);
        return Math.toDegrees(Math.acos(clamp(cosine, -1.0D, 1.0D)));
    }

    public static boolean hasValidFlankerSeparation(
            double firstX,
            double firstZ,
            double secondX,
            double secondZ) {
        return angularSeparationDegrees(firstX, firstZ, secondX, secondZ)
                >= FLANKER_MIN_SEPARATION_DEGREES;
    }

    public static PairRewardProgress recordFlankerTerminal(
            PairRewardProgress current,
            boolean killedByPlayer,
            boolean secondMember) {
        PairRewardProgress safe = current == null ? PairRewardProgress.fresh() : current;
        if (safe.resolved()) {
            return safe;
        }
        int playerKills = safe.playerKills() + (killedByPlayer ? 1 : 0);
        boolean invalidated = safe.invalidated() || !killedByPlayer;
        boolean resolved = secondMember;
        boolean rewardEligible = resolved && !invalidated && playerKills == 2;
        return new PairRewardProgress(playerKills, invalidated, resolved, rewardEligible);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record PairRewardProgress(
            int playerKills,
            boolean invalidated,
            boolean resolved,
            boolean rewardEligible) {
        public PairRewardProgress {
            playerKills = Math.max(0, Math.min(2, playerKills));
            if (!resolved) {
                rewardEligible = false;
            }
        }

        public static PairRewardProgress fresh() {
            return new PairRewardProgress(0, false, false, false);
        }
    }

    public record HorizontalVelocity(double x, double z) {
        public double magnitude() {
            return Math.hypot(x, z);
        }
    }
}
