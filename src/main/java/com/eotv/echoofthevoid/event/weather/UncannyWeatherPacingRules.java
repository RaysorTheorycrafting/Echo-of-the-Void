package com.eotv.echoofthevoid.event.weather;

/** Pure weather catalog and arithmetic shared by runtime, tests and campaign simulation. */
public final class UncannyWeatherPacingRules {
    private static final int TICKS_PER_SECOND = 20;
    public static final int VISUAL_MIN_DURATION_TICKS = TICKS_PER_SECOND * 30;
    public static final int VISUAL_MAX_DURATION_TICKS = TICKS_PER_SECOND * 60 * 2;
    public static final int VISUAL_LONG_THRESHOLD_TICKS = TICKS_PER_SECOND * 90;
    public static final int VISUAL_SHORT_MIN_TICKS = TICKS_PER_SECOND * 12;
    public static final int VISUAL_SHORT_MAX_TICKS = TICKS_PER_SECOND * 22;

    private UncannyWeatherPacingRules() {
    }

    public static IntRange nextCheckSecondsRange(int phase, int profile) {
        int minSeconds = switch (phase) {
            case 1 -> 12;
            case 2 -> 10;
            case 3 -> 8;
            case 4 -> 6;
            default -> 10;
        };
        int maxSeconds = switch (phase) {
            case 1 -> 28;
            case 2 -> 24;
            case 3 -> 20;
            case 4 -> 16;
            default -> 26;
        };
        float profileScale = switch (profile) {
            case 1 -> 1.20F;
            case 2 -> 1.08F;
            case 4 -> 0.88F;
            case 5 -> 0.72F;
            default -> 1.0F;
        };
        int minimum = Math.max(2, floor(minSeconds * profileScale));
        int maximum = Math.max(minimum + 1, floor(maxSeconds * profileScale));
        return new IntRange(minimum, maximum);
    }

    public static long nextCheckDelayTicks(
            int baseSeconds,
            boolean addLongDelay,
            int longDelaySeconds,
            boolean shortenDelay,
            int shortDelaySeconds) {
        long delay = baseSeconds * (long) TICKS_PER_SECOND;
        if (addLongDelay) {
            delay += longDelaySeconds * (long) TICKS_PER_SECOND;
        }
        if (shortenDelay) {
            delay = Math.max(4L * TICKS_PER_SECOND,
                    delay - shortDelaySeconds * (long) TICKS_PER_SECOND);
        }
        return Math.max(4L * TICKS_PER_SECOND, Math.round(delay * 1.25D));
    }

    public static double triggerChance(int phase, int profile) {
        double base = switch (phase) {
            case 1 -> 0.12D;
            case 2 -> 0.24D;
            case 3 -> 0.32D;
            case 4 -> 0.40D;
            default -> 0.0D;
        };
        return clamp(base * profileChanceMultiplier(profile), 0.02D, 0.62D);
    }

    public static IntRange cooldownSecondsRange(int phase, int profile, float severityScale) {
        int minSeconds = floor(70 * severityScale);
        int maxSeconds = floor(180 * severityScale);
        float phaseScale = switch (phase) {
            case 1 -> 1.15F;
            case 2 -> 1.00F;
            case 3 -> 0.86F;
            case 4 -> 0.74F;
            default -> 1.0F;
        };
        float profileScale = switch (profile) {
            case 1 -> 1.30F;
            case 2 -> 1.10F;
            case 4 -> 0.82F;
            case 5 -> 0.70F;
            default -> 1.0F;
        };
        int minimum = Math.max(24, floor(minSeconds * phaseScale * profileScale));
        int maximum = Math.max(minimum + 12, floor(maxSeconds * phaseScale * profileScale));
        return new IntRange(minimum, maximum);
    }

    public static long cooldownTicks(int baseSeconds, boolean addLongDelay, int longDelaySeconds) {
        long cooldown = baseSeconds * (long) TICKS_PER_SECOND;
        if (addLongDelay) {
            cooldown += longDelaySeconds * (long) TICKS_PER_SECOND;
        }
        return Math.max(24L * TICKS_PER_SECOND, Math.round(cooldown * 1.30D));
    }

    public static int effectiveWeight(Event event, int phase, int profile, int danger, boolean presentationLimits) {
        int weight = Math.max(0, Math.round(
                event.baseWeight * profileWeightMultiplier(profile) * dangerWeightMultiplier(event, danger)));
        if (!presentationLimits || weight <= 0) {
            return weight;
        }
        if (event.heavyVisual()) {
            float limiter = switch (phase) {
                case 1, 2 -> 0.35F;
                case 3 -> 0.42F;
                default -> 0.50F;
            };
            weight = Math.max(1, Math.round(weight * limiter));
        }
        if (phase >= 4 && event.minPhase >= 3) {
            weight += Math.max(1, weight / 4);
        }
        return weight;
    }

    public static IntRange visualDurationRange(int previousHeavyDurationTicks) {
        return previousHeavyDurationTicks >= VISUAL_LONG_THRESHOLD_TICKS
                ? new IntRange(VISUAL_SHORT_MIN_TICKS, VISUAL_SHORT_MAX_TICKS)
                : new IntRange(VISUAL_MIN_DURATION_TICKS, VISUAL_MAX_DURATION_TICKS);
    }

    private static float profileWeightMultiplier(int profile) {
        return switch (profile) {
            case 1 -> 0.82F;
            case 2 -> 0.92F;
            case 4 -> 1.20F;
            case 5 -> 1.42F;
            default -> 1.0F;
        };
    }

    private static float profileChanceMultiplier(int profile) {
        return switch (profile) {
            case 1 -> 0.92F;
            case 2 -> 1.06F;
            case 4 -> 1.34F;
            case 5 -> 1.62F;
            default -> 1.0F;
        };
    }

    private static float dangerWeightMultiplier(Event event, int danger) {
        if (danger <= 0) {
            return switch (event) {
                case THUNDER_TARGET_STRIKE, THUNDER_STROBOSCOPIC, SKY_PRESSURE -> 0.0F;
                case THUNDER_SILENT, THUNDER_ARTIFICIAL, FOG_BLACK, FOG_STATIC_WALL, SKY_EMPTY -> 0.45F;
                default -> 1.45F;
            };
        }
        float dangerBoost = switch (danger) {
            case 1 -> 0.72F;
            case 2 -> 0.88F;
            case 4 -> 1.20F;
            case 5 -> 1.42F;
            default -> 1.0F;
        };
        return switch (event) {
            case THUNDER_TARGET_STRIKE, THUNDER_STROBOSCOPIC, SKY_PRESSURE -> dangerBoost;
            case THUNDER_SILENT, THUNDER_ARTIFICIAL, FOG_BLACK, FOG_STATIC_WALL, SKY_EMPTY ->
                    0.82F + (dangerBoost - 1.0F) * 0.85F;
            default -> 1.16F - (dangerBoost - 1.0F) * 0.55F;
        };
    }

    private static int floor(float value) {
        return (int) Math.floor(value);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
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
    }

    public enum Event {
        RAIN_SILENT("rain_silent", 2, 20 * 90, 20 * 180, 12, 1.0F),
        RAIN_DRY_STORM(
                "rain_dry_storm", 2,
                UncannyWeatherTimingRules.DRY_RAIN_MIN_DURATION_TICKS,
                UncannyWeatherTimingRules.DRY_RAIN_MAX_DURATION_TICKS, 10, 1.0F),
        RAIN_ASH("rain_ash", 2, 20 * 90, 20 * 180, 11, 1.05F),
        RAIN_SOBBING("rain_sobbing", 2, 20 * 90, 20 * 180, 10, 1.0F),
        THUNDER_SILENT("thunder_silent", 3, 20 * 70, 20 * 150, 8, 1.2F),
        THUNDER_ARTIFICIAL(
                "thunder_artificial", 3,
                UncannyWeatherTimingRules.ARTIFICIAL_THUNDER_MIN_DURATION_TICKS,
                UncannyWeatherTimingRules.ARTIFICIAL_THUNDER_MAX_DURATION_TICKS, 7, 1.2F),
        THUNDER_TARGET_STRIKE("thunder_target_strike", 3, 20 * 20, 20 * 45, 6, 1.25F),
        THUNDER_STROBOSCOPIC("thunder_stroboscopic", 3, 20 * 25, 20 * 55, 5, 1.45F),
        FOG_BREATHING("fog_breathing", 2, 20 * 45, 20 * 120, 9, 1.0F),
        FOG_BLACK("fog_black", 3, 20 * 45, 20 * 120, 6, 1.1F),
        FOG_STATIC_WALL("fog_static_wall", 3, 20 * 45, 20 * 120, 7, 1.1F),
        SKY_FAKE_MORNING("sky_fake_morning", 3, 20 * 6, 20 * 15, 4, 1.3F),
        SKY_EMPTY("sky_empty", 3, 20 * 70, 20 * 150, 5, 1.2F),
        SKY_PRESSURE("sky_pressure", 3, 20 * 35, 20 * 60, 5, 1.35F),
        RAIN_FRONT("rain_front", 2, 20 * 10, 20 * 18, 4, 1.0F),
        SUSPENDED_RAIN("suspended_rain", 3, 20 * 2, 20 * 3, 1, 1.15F),
        DRY_EYE("dry_eye", 2, 20 * 8, 20 * 14, 4, 1.0F),
        CLEAR_DOWNPOUR("clear_downpour", 1, 20 * 4, 20 * 8, 2, 1.1F),
        WRONG_SNOWLINE("wrong_snowline", 2, 20 * 8, 20 * 13, 3, 1.0F),
        LIGHT_AVOIDING_RAIN("light_avoiding_rain", 3, 20 * 8, 20 * 13, 1, 1.15F),
        CONVERGING_RAIN("converging_rain", 3, 20 * 4, 20 * 7, 1, 1.15F),
        LEAKING_SKY("leaking_sky", 2, 20 * 4, 20 * 7, 3, 1.0F);

        public final String id;
        public final int minPhase;
        public final int minDurationTicks;
        public final int maxDurationTicks;
        public final int baseWeight;
        public final float severityMultiplier;

        Event(String id, int minPhase, int minDurationTicks, int maxDurationTicks, int baseWeight,
                float severityMultiplier) {
            this.id = id;
            this.minPhase = minPhase;
            this.minDurationTicks = minDurationTicks;
            this.maxDurationTicks = maxDurationTicks;
            this.baseWeight = baseWeight;
            this.severityMultiplier = severityMultiplier;
        }

        public boolean heavyVisual() {
            return this == FOG_BREATHING || this == FOG_BLACK || this == FOG_STATIC_WALL
                    || this == THUNDER_STROBOSCOPIC;
        }

        public static Event byId(String id) {
            if (id == null || id.isBlank()) {
                return null;
            }
            for (Event value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            return null;
        }
    }
}
