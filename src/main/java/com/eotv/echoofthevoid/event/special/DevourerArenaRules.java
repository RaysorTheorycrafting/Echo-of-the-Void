package com.eotv.echoofthevoid.event.special;

/** Pure trial timing, isolation and lifecycle rules shared by runtime and tests. */
public final class DevourerArenaRules {
    public static final int MAX_SIMULTANEOUS_SESSIONS = 8;
    public static final int CELL_SPACING = 2048;
    public static final int ARENA_SURFACE_Y = 13;
    public static final int INITIAL_PURSUERS = 8;
    public static final int PURSUERS_PER_WAVE = 2;
    public static final int PURSUER_WAVE_INTERVAL_TICKS = 10 * 20;
    public static final int PURSUER_WAVES = 5;
    public static final int MAX_PURSUERS = INITIAL_PURSUERS + PURSUERS_PER_WAVE * PURSUER_WAVES;
    public static final int MAX_SPAWNS_PER_TICK = 2;
    public static final int SECTOR_COUNT = 8;
    public static final int COVERAGE_REEVALUATION_TICKS = 40;
    public static final int REPLACEMENT_RESPITE_TICKS = 60;
    public static final int MAX_REPLACEMENTS = 6;
    public static final int PURSUER_EMERGENCE_TICKS = 30;
    /** Vanilla Spider speed: roughly a walking player, so only sprinting buys distance. */
    public static final double PURSUER_SPEED = 0.30D;
    /** Elsewhere must stay lethal for an equipped player: pursuers hit twice as hard as Attacker?. */
    public static final double PURSUER_DAMAGE_MULTIPLIER = 2.0D;
    public static final double PURSUER_MIN_DAMAGE = 8.0D;
    public static final double PURSUER_MAX_DAMAGE = 24.0D;
    public static final double MIN_SPAWN_DISTANCE = 18.0D;
    public static final double MAX_SPAWN_DISTANCE = 26.0D;
    public static final double MIN_PURSUER_SEPARATION = 6.0D;
    public static final double REBALANCE_DISTANCE = 36.0D;
    public static final double FORWARD_SECTOR_CHANCE = 0.65D;
    public static final int SCRATCH_TELEGRAPH_TICKS = 20 * 3;
    /** Climbing silhouettes are the answer to a pillar: at least two are always on the field. */
    public static final int MIN_CLIMBING_PURSUERS = 2;
    /** A target standing this far above a walker is on a pillar or ledge: leave it to the climbers. */
    public static final double PILLAR_HEIGHT_THRESHOLD = 1.25D;
    public static final long OFFLINE_ABORT_MILLIS = 30L * 60L * 1000L;
    public static final int RETURN_PROTECTION_TICKS = 20 * 3;
    public static final int ORIGIN_UNREACHABLE_GRACE_TICKS = 100;
    public static final int ORIGIN_REACHABILITY_REFRESH_TICKS = 10;
    public static final int ORIGIN_FORCED_RETREAT_TICKS = 20 * 60 * 3;
    // Match the entity's ten-chunk client tracking range: anyone who can still receive and render
    // Devourer? must be able to prevent its unseen retreat.
    public static final double ORIGIN_OBSERVER_RANGE = 160.0D;

    private DevourerArenaRules() {
    }

    /**
     * Silhouette for a new pursuer: a Spider while fewer than {@link #MIN_CLIMBING_PURSUERS} are
     * alive, otherwise the next walking form so a fresh arena still shows a varied group.
     */
    public static ArenaPursuerAppearance appearanceFor(int visualIndex, int aliveClimbers) {
        if (aliveClimbers < MIN_CLIMBING_PURSUERS) {
            return ArenaPursuerAppearance.SPIDER;
        }
        ArenaPursuerAppearance[] walkers = java.util.Arrays.stream(ArenaPursuerAppearance.values())
                .filter(appearance -> !appearance.climbs())
                .toArray(ArenaPursuerAppearance[]::new);
        return walkers[Math.floorMod(visualIndex, walkers.length)];
    }

    /** Walkers never dismantle a pillar: they only clear placements at their own height. */
    public static boolean mayScratchTowards(double pursuerY, double targetY) {
        return targetY - pursuerY < PILLAR_HEIGHT_THRESHOLD;
    }

    public static int expectedPursuerCount(long elapsedTicks) {
        long nonNegativeTicks = Math.max(0L, elapsedTicks);
        int completedWaves = (int) Math.min(
                PURSUER_WAVES,
                nonNegativeTicks / PURSUER_WAVE_INTERVAL_TICKS);
        int count = INITIAL_PURSUERS + completedWaves * PURSUERS_PER_WAVE;
        return Math.min(MAX_PURSUERS, count);
    }

    public static int clampPersistedPursuerCount(int count) {
        return Math.max(0, Math.min(MAX_PURSUERS, count));
    }

    public static int clampReplacementCount(int count) {
        return Math.max(0, Math.min(MAX_REPLACEMENTS, count));
    }

    public static int spawnAllowance(int missing, int alreadySpawnedThisTick) {
        return Math.max(0, Math.min(
                Math.max(0, missing),
                MAX_SPAWNS_PER_TICK - Math.max(0, alreadySpawnedThisTick)));
    }

    public static int sectorForDirection(double x, double z) {
        if (x * x + z * z < 1.0E-8D) {
            return 0;
        }
        double normalized = Math.atan2(z, x);
        if (normalized < 0.0D) {
            normalized += Math.PI * 2.0D;
        }
        return Math.min(SECTOR_COUNT - 1, (int) Math.floor(normalized / (Math.PI * 2.0D / SECTOR_COUNT)));
    }

    public static int chooseSpawnSector(int[] coverage, int forwardSector, double forwardRoll, int tieOffset) {
        if (coverage == null || coverage.length != SECTOR_COUNT) {
            throw new IllegalArgumentException("coverage must contain exactly eight sectors");
        }
        int safeForward = Math.floorMod(forwardSector, SECTOR_COUNT);
        if (coverage[safeForward] == 0 && forwardRoll < FORWARD_SECTOR_CHANCE) {
            return safeForward;
        }
        int minimum = Integer.MAX_VALUE;
        for (int count : coverage) {
            minimum = Math.min(minimum, Math.max(0, count));
        }
        int start = Math.floorMod(tieOffset, SECTOR_COUNT);
        for (int offset = 0; offset < SECTOR_COUNT; offset++) {
            int sector = (start + offset) % SECTOR_COUNT;
            if (Math.max(0, coverage[sector]) == minimum) {
                return sector;
            }
        }
        return safeForward;
    }

    public static boolean replacementReady(
            long elapsedTicks,
            long lossDetectedElapsedTick,
            int replacementsUsed) {
        return replacementsUsed < MAX_REPLACEMENTS
                && lossDetectedElapsedTick >= 0L
                && elapsedTicks - lossDetectedElapsedTick >= REPLACEMENT_RESPITE_TICKS;
    }

    public static boolean offlineGraceExpired(long lastOnlineEpochMillis, long nowEpochMillis) {
        return lastOnlineEpochMillis > 0L
                && nowEpochMillis >= lastOnlineEpochMillis
                && nowEpochMillis - lastOnlineEpochMillis >= OFFLINE_ABORT_MILLIS;
    }

    public static boolean canBeginOriginRetreat(
            int unreachableTicks,
            boolean anyReachablePlayer,
            boolean anyObserver) {
        return unreachableTicks >= ORIGIN_UNREACHABLE_GRACE_TICKS
                && !anyReachablePlayer
                && !anyObserver;
    }

    public static long originForcedRetreatDeadline(long currentGameTime, int elapsedLifetimeTicks) {
        long elapsed = Math.max(0L, elapsedLifetimeTicks);
        long remaining = Math.max(0L, ORIGIN_FORCED_RETREAT_TICKS - elapsed);
        return currentGameTime > Long.MAX_VALUE - remaining
                ? Long.MAX_VALUE
                : currentGameTime + remaining;
    }

    public static boolean originForcedRetreatDue(long currentGameTime, long deadlineGameTime) {
        return deadlineGameTime != Long.MIN_VALUE && currentGameTime >= deadlineGameTime;
    }

    /**
     * Three minutes is a hard end to a failed encounter, not a reason to remove a Devourer that
     * still has a valid route to an uncaptured player. Once that opportunity is gone, an expired
     * deadline permits the visible sinking fallback even if a player keeps watching it.
     */
    public static boolean originForcedRetreatRequired(
            long currentGameTime,
            long deadlineGameTime,
            boolean anyReachablePlayer) {
        return !anyReachablePlayer && originForcedRetreatDue(currentGameTime, deadlineGameTime);
    }

    public static int cellCenterX(long cellIndex) {
        long safeIndex = Math.max(0L, cellIndex) % 13_000L;
        return (int) (safeIndex * CELL_SPACING);
    }

    public static AdaptiveSpecialCombatProfile pursuerProfile(AdaptiveSpecialCombatProfile attacker) {
        if (attacker == null) {
            throw new IllegalArgumentException("attacker profile is required");
        }
        double damage = Math.min(PURSUER_MAX_DAMAGE,
                Math.max(PURSUER_MIN_DAMAGE, attacker.attackDamage() * PURSUER_DAMAGE_MULTIPLIER));
        return new AdaptiveSpecialCombatProfile(
                attacker.maxHealth(), damage, PURSUER_SPEED, attacker.armor(), attacker.armorToughness());
    }
}
