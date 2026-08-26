package com.eotv.echoofthevoid.event.paranoia;

import static com.eotv.echoofthevoid.event.paranoia.ParanoiaEventIds.*;

/**
 * Pure phase × profile × danger calculations extracted from the 1.1.1 event facade.
 * Runtime and simulations must both call this class so their arithmetic cannot drift.
 */
public final class ParanoiaPacingRules {
    public static final int TICKS_PER_SECOND = 20;
    public static final int AUTO_CHECK_INTERVAL_MIN_TICKS = 10;
    public static final int AUTO_CHECK_INTERVAL_MAX_TICKS = 34;
    /** Prevents two independent scheduler lanes from producing an accidental combined scare. */
    public static final int CROSS_LANE_BURST_GUARD_TICKS = 10 * TICKS_PER_SECOND + 1;
    public static final int PHASE_2_BELL_COOLDOWN_SECONDS = 12 * 60;
    public static final int PHASE_2_HURLER_COOLDOWN_SECONDS = 20 * 60;

    private static final double[] PROFILE_TRIGGER_MULTIPLIER = {0.34D, 0.58D, 0.92D, 1.48D, 2.20D};
    private static final int[] PROFILE_BASE_COOLDOWN_SECONDS = {210, 150, 105, 72, 48};
    private static final double[] PROFILE_AMBIENT_TRIGGER_MULTIPLIER = {0.55D, 0.72D, 0.92D, 1.20D, 1.55D};
    private static final int[] PROFILE_AMBIENT_BASE_COOLDOWN_SECONDS = {190, 145, 105, 78, 58};
    private static final int[] PROFILE_MAX_SILENCE_SECONDS = {360, 240, 150, 80, 55};
    private static final double[] DANGER_TRIGGER_MULTIPLIER = {0.42D, 0.62D, 0.82D, 1.00D, 1.28D, 1.62D};
    private static final double[] DANGER_GLOBAL_COOLDOWN_MULTIPLIER = {1.90D, 1.45D, 1.15D, 1.00D, 0.82D, 0.64D};
    private static final double[] DANGER_AMBIENT_TRIGGER_MULTIPLIER = {0.78D, 0.88D, 0.95D, 1.00D, 1.08D, 1.18D};
    private static final double[] DANGER_AMBIENT_COOLDOWN_MULTIPLIER = {1.25D, 1.12D, 1.05D, 1.00D, 0.92D, 0.84D};
    private static final double[] DANGER_EVENT_COOLDOWN_MULTIPLIER = {2.05D, 1.55D, 1.20D, 1.00D, 0.78D, 0.58D};
    private static final double[] DANGER_MAX_SILENCE_MULTIPLIER = {1.60D, 1.30D, 1.10D, 1.00D, 0.85D, 0.70D};
    private static final double[] DANGER_HIGH_EVENT_MULTIPLIER = {0.00D, 0.20D, 0.50D, 1.00D, 2.00D, 3.40D};
    private static final double[] DANGER_MEDIUM_EVENT_MULTIPLIER = {0.65D, 0.78D, 0.90D, 1.00D, 1.30D, 1.75D};
    private static final double[] DANGER_LIGHT_EVENT_MULTIPLIER = {2.60D, 2.00D, 1.35D, 1.00D, 0.55D, 0.20D};
    private static final int[] PROFILE_SPECIAL_ENTITY_BASE_COOLDOWN_SECONDS = {960, 700, 500, 340, 240};
    private static final int[] PROFILE_SPECIAL_ENTITY_CHECK_INTERVAL_SECONDS = {16, 13, 11, 8, 7};
    private static final double[] PROFILE_SPECIAL_ENTITY_TRIGGER_CHANCE = {0.03D, 0.06D, 0.10D, 0.16D, 0.22D};
    private static final double[] DANGER_SPECIAL_ENTITY_COOLDOWN_MULTIPLIER = {1.35D, 1.22D, 1.10D, 1.00D, 0.92D, 0.84D};
    private static final double[] DANGER_SPECIAL_ENTITY_TRIGGER_MULTIPLIER = {0.70D, 0.82D, 0.92D, 1.00D, 1.08D, 1.16D};
    private static final double[] SLEEP_DISTURB_PHASE_CHANCE = {0.0D, 0.055D, 0.078D, 0.105D};
    private static final double[] SLEEP_DISTURB_PROFILE_MULTIPLIER = {0.72D, 0.88D, 1.00D, 1.14D, 1.30D};

    private ParanoiaPacingRules() {
    }

    public static double autoTriggerChance(int phase, int profile, int danger) {
        validate(phase, profile, danger);
        double base = switch (phase) {
            case 1 -> 0.010D;
            case 2 -> 0.016D;
            case 3 -> 0.022D;
            case 4 -> 0.030D;
            default -> throw new IllegalArgumentException("phase must be in [1, 4]");
        };
        return clamp(base * PROFILE_TRIGGER_MULTIPLIER[profile - 1] * DANGER_TRIGGER_MULTIPLIER[danger], 0.0030D, 0.30D);
    }

    public static double ambientTriggerChance(int phase, int profile, int danger) {
        validate(phase, profile, danger);
        double base = switch (phase) {
            case 1 -> 0.085D;
            case 2 -> 0.12D;
            case 3 -> 0.15D;
            case 4 -> 0.19D;
            default -> throw new IllegalArgumentException("phase must be in [1, 4]");
        };
        return clamp(base * PROFILE_AMBIENT_TRIGGER_MULTIPLIER[profile - 1]
                * DANGER_AMBIENT_TRIGGER_MULTIPLIER[danger], 0.06D, 0.82D);
    }

    public static long effectiveGlobalCooldownTicks(int phase, int profile, int danger, int configuredSeconds) {
        validate(phase, profile, danger);
        int configured = Math.max(20, configuredSeconds);
        int chosenSeconds = Math.min(configured, PROFILE_BASE_COOLDOWN_SECONDS[profile - 1]);
        double phaseMultiplier = switch (phase) {
            case 1 -> 1.15D;
            case 2 -> 0.92D;
            case 3 -> 0.78D;
            case 4 -> 0.66D;
            default -> throw new IllegalArgumentException("phase must be in [1, 4]");
        };
        int seconds = Math.max(10, (int) Math.round(
                chosenSeconds * phaseMultiplier * DANGER_GLOBAL_COOLDOWN_MULTIPLIER[danger]));
        return seconds * (long) TICKS_PER_SECOND;
    }

    public static long ambientGlobalCooldownTicks(int phase, int profile, int danger) {
        validate(phase, profile, danger);
        int baseSeconds = PROFILE_AMBIENT_BASE_COOLDOWN_SECONDS[profile - 1];
        double phaseMultiplier = switch (phase) {
            case 1 -> 1.12D;
            case 2 -> 1.00D;
            case 3 -> 0.85D;
            case 4 -> 0.72D;
            default -> throw new IllegalArgumentException("phase must be in [1, 4]");
        };
        int seconds = Math.max(10, (int) Math.round(
                baseSeconds * phaseMultiplier * DANGER_AMBIENT_COOLDOWN_MULTIPLIER[danger]));
        return seconds * (long) TICKS_PER_SECOND;
    }

    public static long maxSilenceTicks(int phase, int profile, int danger) {
        validate(phase, profile, danger);
        double phaseMultiplier = switch (phase) {
            case 1 -> 1.20D;
            case 2 -> 1.00D;
            case 3 -> 0.80D;
            case 4 -> 0.65D;
            default -> throw new IllegalArgumentException("phase must be in [1, 4]");
        };
        int seconds = Math.max(20, (int) Math.round(PROFILE_MAX_SILENCE_SECONDS[profile - 1]
                * phaseMultiplier * DANGER_MAX_SILENCE_MULTIPLIER[danger]));
        return seconds * (long) TICKS_PER_SECOND;
    }

    public static int effectiveWeight(String eventId, int baseWeight, int profile, int danger) {
        validateProfileAndDanger(profile, danger);
        double profileMultiplier = switch (eventId) {
            case FOOTSTEPS -> 0.90D - (profile - 1) * 0.08D;
            case BASE_REPLAY -> 1.90D - (profile - 1) * 0.10D;
            case BLACKOUT -> 0.22D + (profile - 1) * 0.12D;
            case BELL -> 0.62D + (profile - 1) * 0.24D;
            case WATCHER -> 0.92D + (profile - 1) * 0.18D;
            case KNOCKER -> 0.45D + (profile - 1) * 0.22D;
            case FLASH -> 0.12D + (profile - 1) * 0.28D;
            case STALKER -> 0.12D + (profile - 1) * 0.20D;
            case SHADOW -> 0.32D + (profile - 1) * 0.34D;
            case HURLER -> 0.38D + (profile - 1) * 0.33D;
            case PULSE -> 0.08D + (profile - 1) * 0.14D;
            case USHER -> 0.10D + (profile - 1) * 0.08D;
            case KEEPER -> 0.30D + (profile - 1) * 0.22D;
            case TENANT -> 0.28D + (profile - 1) * 0.24D;
            case FOLLOWER -> 0.56D + (profile - 1) * 0.22D;
            case ANIMAL_STARE_LOCK, MISPLACED_LIGHT, HOTBAR_WRONG_COUNT -> 0.42D + (profile - 1) * 0.22D;
            case COMPASS_LIAR, PET_REFUSAL, CORRUPT_TOAST, FALSE_RECIPE_TOAST -> 0.26D + (profile - 1) * 0.20D;
            case TOOL_ANSWER -> 0.20D + (profile - 1) * 0.10D;
            case FURNACE_BREATH -> 0.42D + (profile - 1) * 0.12D;
            case FALSE_CONTAINER_OPEN, LEVER_ANSWER, PRESSURE_PLATE_REPLY, CAMPFIRE_COUGH, BUCKET_DRIP ->
                    0.54D + (profile - 1) * 0.14D;
            case WORKBENCH_REJECT -> 0.12D + (profile - 1) * 0.10D;
            case FLASH_RED, VOID_SILENCE, FALSE_FALL, GHOST_MINER, CAVE_COLLAPSE -> 0.65D + (profile - 1) * 0.22D;
            case ARMOR_BREAK, AQUATIC_STEPS, LIVING_ORE -> 0.50D + (profile - 1) * 0.28D;
            case DOOR_INVERSION, PHANTOM_HARVEST, PROJECTED_SHADOW, GIANT_SUN, HUNTER_FOG ->
                    0.24D + (profile - 1) * 0.30D;
            case ASPHYXIA -> 0.14D + (profile - 1) * 0.26D;
            case FALSE_INJURY, FORCED_DROP -> 0.08D + (profile - 1) * 0.40D;
            case CORRUPT_MESSAGE -> 1.32D - (profile - 1) * 0.12D;
            default -> 1.0D;
        };
        double weighted = baseWeight * profileMultiplier * dangerWeightMultiplier(eventId, danger);
        return weighted <= 0.0D ? 0 : Math.max(1, (int) Math.round(weighted));
    }

    /**
     * Active work-build weighting. The immutable {@link #effectiveWeight} method remains the
     * shipped 1.1.1 arithmetic used by historical comparisons.
     */
    public static int activeEffectiveWeight(
            String eventId,
            int catalogBaseWeight,
            int phase,
            int profile,
            int danger) {
        validate(phase, profile, danger);
        int activeBaseWeight = catalogBaseWeight;
        if (phase == 2) {
            if (BELL.equals(eventId)) {
                activeBaseWeight = Math.min(activeBaseWeight, 7);
            } else if (HURLER.equals(eventId)) {
                activeBaseWeight = Math.min(activeBaseWeight, 2);
            }
        }
        return effectiveWeight(eventId, activeBaseWeight, profile, danger);
    }

    /** Applies deliberate family spacing without changing the generic 1.1.1 cooldown sampler. */
    public static long activeEventCooldownTicks(String eventId, int phase, long sampledCooldownTicks) {
        validatePhase(phase);
        long nonNegativeCooldown = Math.max(0L, sampledCooldownTicks);
        if (phase == 2 && BELL.equals(eventId)) {
            return Math.max(nonNegativeCooldown, PHASE_2_BELL_COOLDOWN_SECONDS * (long) TICKS_PER_SECOND);
        }
        return nonNegativeCooldown;
    }

    public static double dangerWeightMultiplier(String eventId, int danger) {
        validateDanger(danger);
        if (danger == 0) {
            return switch (eventId) {
                case WATCHER, HURLER, KNOCKER, FOOTSTEPS, CORRUPT_MESSAGE, BASE_REPLAY, GHOST_MINER,
                        CAVE_COLLAPSE, FLASH_RED, VOID_SILENCE, FALSE_FALL, ARMOR_BREAK, AQUATIC_STEPS,
                        DOOR_INVERSION, LIVING_ORE -> DANGER_LIGHT_EVENT_MULTIPLIER[0];
                case BLACKOUT, BELL, FLASH, STALKER, SHADOW, PULSE, FALSE_INJURY, FORCED_DROP,
                        ASPHYXIA, PHANTOM_HARVEST, PROJECTED_SHADOW, GIANT_SUN, HUNTER_FOG -> 0.0D;
                default -> DANGER_MEDIUM_EVENT_MULTIPLIER[0];
            };
        }
        return switch (eventId) {
            case STALKER, SHADOW, FLASH, BLACKOUT, BELL, PULSE, FALSE_INJURY, FORCED_DROP,
                    ASPHYXIA, PHANTOM_HARVEST, PROJECTED_SHADOW, GIANT_SUN, HUNTER_FOG ->
                    DANGER_HIGH_EVENT_MULTIPLIER[danger];
            case HURLER, KNOCKER, VOID_SILENCE, FALSE_FALL, DOOR_INVERSION, LIVING_ORE ->
                    DANGER_MEDIUM_EVENT_MULTIPLIER[danger];
            default -> DANGER_LIGHT_EVENT_MULTIPLIER[danger];
        };
    }

    public static IntRange eventCooldownSecondsRange(ParanoiaEventSeverity severity) {
        return switch (severity) {
            case LIGHT -> new IntRange(16, 45);
            case MEDIUM -> new IntRange(35, 95);
            case HIGH -> new IntRange(70, 180);
            case EXTREME -> new IntRange(120, 300);
        };
    }

    public static long eventCooldownTicks(
            int phase,
            int profile,
            int danger,
            ParanoiaEventSeverity severity,
            int sampledBaseSeconds,
            double sampledJitterUnit) {
        validate(phase, profile, danger);
        IntRange range = eventCooldownSecondsRange(severity);
        if (!range.contains(sampledBaseSeconds)) {
            throw new IllegalArgumentException("sampledBaseSeconds is outside the severity range");
        }
        if (sampledJitterUnit < 0.0D || sampledJitterUnit >= 1.0D) {
            throw new IllegalArgumentException("sampledJitterUnit must be in [0, 1)");
        }
        double profileScale = switch (profile) {
            case 1 -> 1.45D;
            case 2 -> 1.00D;
            case 3 -> 0.72D;
            case 4 -> 0.52D;
            case 5 -> 0.38D;
            default -> throw new IllegalArgumentException("profile must be in [1, 5]");
        };
        double phaseScale = switch (phase) {
            case 1 -> 1.20D;
            case 2 -> 1.02D;
            case 3 -> 0.86D;
            case 4 -> 0.72D;
            default -> throw new IllegalArgumentException("phase must be in [1, 4]");
        };
        double jitter = 0.78D + sampledJitterUnit * 0.54D;
        int seconds = Math.max(8, (int) Math.round(sampledBaseSeconds * profileScale
                * DANGER_EVENT_COOLDOWN_MULTIPLIER[danger] * phaseScale * jitter));
        return seconds * (long) TICKS_PER_SECOND;
    }

    public static IntRange autoCheckIntervalTicksRange(int phase, int profile) {
        validatePhase(phase);
        validateProfile(profile);
        int profileReduction = profile * 2;
        int phaseReduction = switch (phase) {
            case 1 -> 0;
            case 2 -> 2;
            case 3 -> 4;
            case 4 -> 6;
            default -> throw new IllegalArgumentException("phase must be in [1, 4]");
        };
        int min = Math.max(6, AUTO_CHECK_INTERVAL_MIN_TICKS - profileReduction - phaseReduction / 2);
        int max = Math.max(min + 4, AUTO_CHECK_INTERVAL_MAX_TICKS - profileReduction - phaseReduction);
        return new IntRange(min, max);
    }

    public static IntRange specialCheckIntervalSecondsRange(int phase, int profile) {
        validatePhase(phase);
        validateProfile(profile);
        int base = PROFILE_SPECIAL_ENTITY_CHECK_INTERVAL_SECONDS[profile - 1];
        double phaseMultiplier = switch (phase) {
            case 1 -> 1.35D;
            case 2 -> 1.15D;
            case 3 -> 1.00D;
            case 4 -> 0.85D;
            default -> throw new IllegalArgumentException("phase must be in [1, 4]");
        };
        int min = Math.max(1, (int) Math.floor(base * phaseMultiplier * 0.65D));
        int max = Math.max(min, (int) Math.ceil(base * phaseMultiplier * 1.35D));
        return new IntRange(min, max);
    }

    public static long specialGlobalCooldownTicks(int phase, int profile, int danger) {
        validate(phase, profile, danger);
        double phaseMultiplier = switch (phase) {
            case 1 -> 1.30D;
            case 2 -> 1.15D;
            case 3 -> 0.94D;
            case 4 -> 0.78D;
            default -> throw new IllegalArgumentException("phase must be in [1, 4]");
        };
        int seconds = Math.max(35, (int) Math.round(PROFILE_SPECIAL_ENTITY_BASE_COOLDOWN_SECONDS[profile - 1]
                * phaseMultiplier * DANGER_SPECIAL_ENTITY_COOLDOWN_MULTIPLIER[danger]));
        return seconds * (long) TICKS_PER_SECOND;
    }

    /** Active work-build spacing; phase 1 and phases 3-4 retain their 1.1.1 values. */
    public static long activeSpecialGlobalCooldownTicks(int phase, int profile, int danger) {
        validate(phase, profile, danger);
        if (phase != 2) {
            return specialGlobalCooldownTicks(phase, profile, danger);
        }
        int seconds = Math.max(35, (int) Math.round(PROFILE_SPECIAL_ENTITY_BASE_COOLDOWN_SECONDS[profile - 1]
                * DANGER_SPECIAL_ENTITY_COOLDOWN_MULTIPLIER[danger]));
        return seconds * (long) TICKS_PER_SECOND;
    }

    public static long specialPerKeyCooldownTicks(String eventId, int phase, int profile, int danger) {
        long base = specialGlobalCooldownTicks(phase, profile, danger);
        return specialPerKeyCooldownTicks(eventId, base);
    }

    public static long activeSpecialPerKeyCooldownTicks(String eventId, int phase, int profile, int danger) {
        long cooldown = specialPerKeyCooldownTicks(
                eventId, activeSpecialGlobalCooldownTicks(phase, profile, danger));
        if (phase == 2 && HURLER.equals(eventId)) {
            cooldown = Math.max(cooldown, PHASE_2_HURLER_COOLDOWN_SECONDS * (long) TICKS_PER_SECOND);
        }
        return cooldown;
    }

    private static long specialPerKeyCooldownTicks(String eventId, long base) {
        double keyMultiplier = switch (eventId) {
            case WATCHER -> 0.65D;
            case PULSE -> 2.10D;
            case FOLLOWER -> 720.0D / Math.max(1.0D, base / 20.0D);
            case USHER -> 3600.0D / Math.max(1.0D, base / 20.0D);
            case TENANT -> 1800.0D / Math.max(1.0D, base / 20.0D);
            case KEEPER -> 2400.0D / Math.max(1.0D, base / 20.0D);
            case KNOCKER, HURLER, SHADOW -> 0.85D;
            case STALKER -> 1.80D;
            default -> 1.00D;
        };
        return Math.max(30L * TICKS_PER_SECOND, (long) Math.round(base * keyMultiplier));
    }

    /** Phase 2 admits Hurler? only as a rare release after recent natural underground mining. */
    public static boolean allowsHurler(int phase, boolean underground, boolean recentNaturalMining) {
        validatePhase(phase);
        return phase >= 3 || (phase == 2 && underground && recentNaturalMining);
    }

    public static double specialTriggerChance(int phase, int profile, int danger) {
        validate(phase, profile, danger);
        if (phase < 2) {
            return 0.0D;
        }
        double phaseMultiplier = switch (phase) {
            case 2 -> 0.90D;
            case 3 -> 1.00D;
            case 4 -> 1.12D;
            default -> 0.0D;
        };
        return clamp(PROFILE_SPECIAL_ENTITY_TRIGGER_CHANCE[profile - 1]
                * DANGER_SPECIAL_ENTITY_TRIGGER_MULTIPLIER[danger] * phaseMultiplier, 0.02D, 0.70D);
    }

    public static double sleepDisturbChance(int phase, int profile) {
        validatePhase(phase);
        validateProfile(profile);
        return clamp(SLEEP_DISTURB_PHASE_CHANCE[phase - 1]
                * SLEEP_DISTURB_PROFILE_MULTIPLIER[profile - 1], 0.0D, 0.24D);
    }

    public static long sleepDisturbCooldownTicks(int phase, int profile, int sampledBaseSeconds) {
        validatePhase(phase);
        validateProfile(profile);
        if (sampledBaseSeconds < 16 * 60 || sampledBaseSeconds > 28 * 60) {
            throw new IllegalArgumentException("sampledBaseSeconds must be in [960, 1680]");
        }
        double phaseScale = switch (phase) {
            case 1 -> 1.35D;
            case 2 -> 1.12D;
            case 3 -> 1.00D;
            case 4 -> 0.92D;
            default -> throw new IllegalArgumentException("phase must be in [1, 4]");
        };
        double profileScale = switch (profile) {
            case 1 -> 1.35D;
            case 2 -> 1.15D;
            case 3 -> 1.00D;
            case 4 -> 0.90D;
            case 5 -> 0.82D;
            default -> throw new IllegalArgumentException("profile must be in [1, 5]");
        };
        int seconds = Math.max(9 * 60, (int) Math.round(sampledBaseSeconds * phaseScale * profileScale));
        return seconds * (long) TICKS_PER_SECOND;
    }

    private static void validate(int phase, int profile, int danger) {
        validatePhase(phase);
        validateProfile(profile);
        validateDanger(danger);
    }

    private static void validateProfileAndDanger(int profile, int danger) {
        validateProfile(profile);
        validateDanger(danger);
    }

    private static void validatePhase(int phase) {
        if (phase < 1 || phase > 4) {
            throw new IllegalArgumentException("phase must be in [1, 4]");
        }
    }

    private static void validateProfile(int profile) {
        if (profile < 1 || profile > 5) {
            throw new IllegalArgumentException("profile must be in [1, 5]");
        }
    }

    private static void validateDanger(int danger) {
        if (danger < 0 || danger > 5) {
            throw new IllegalArgumentException("danger must be in [0, 5]");
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record IntRange(int minInclusive, int maxInclusive) {
        public IntRange {
            if (maxInclusive < minInclusive) {
                throw new IllegalArgumentException("maxInclusive must be >= minInclusive");
            }
        }

        public int size() {
            return maxInclusive - minInclusive + 1;
        }

        public boolean contains(int value) {
            return value >= minInclusive && value <= maxInclusive;
        }
    }
}
