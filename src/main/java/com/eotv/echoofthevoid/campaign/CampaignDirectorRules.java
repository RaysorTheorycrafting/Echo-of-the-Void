package com.eotv.echoofthevoid.campaign;

import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventLane;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventSeverity;
import java.util.List;

/** Pure campaign calculations shared by runtime, tests and future simulations. */
public final class CampaignDirectorRules {
    public static final int STANDARD_LENGTH_DAYS = 50;
    public static final int EXTRA_LONG_LENGTH_DAYS = 100;
    public static final long TICKS_PER_DAY = 24_000L;
    public static final int RECENT_FAMILY_LIMIT = 6;
    public static final double CULMINATION_NATURAL_SATISFACTION_PROGRESS = 0.80D;
    public static final double CULMINATION_SCHEDULE_MIN_PROGRESS = 0.84D;
    public static final double CULMINATION_SCHEDULE_MAX_PROGRESS = 0.94D;
    public static final double CULMINATION_FORCED_ATTEMPT_CUTOFF_PROGRESS = 0.99D;
    public static final long CULMINATION_RETRY_TICKS = Math.round(0.25D * TICKS_PER_DAY);
    public static final long CULMINATION_STRONG_EVENT_GAP_TICKS = Math.round(0.35D * TICKS_PER_DAY);
    private static final long CULMINATION_SCHEDULE_SALT = 0x43554C4D494E4154L;

    private CampaignDirectorRules() {
    }

    public static CampaignAct actAt(long elapsedTicks, int campaignLengthDays) {
        validateCampaignLength(campaignLengthDays);
        double progress = Math.max(0L, elapsedTicks) / (double) (campaignLengthDays * TICKS_PER_DAY);
        if (progress < 0.10D) return CampaignAct.OPENING;
        if (progress < 0.28D) return CampaignAct.DEEPENING;
        if (progress < 0.55D) return CampaignAct.ENCROACHMENT;
        if (progress < 0.82D) return CampaignAct.FRACTURE;
        if (progress < 1.0D) return CampaignAct.CULMINATION;
        return CampaignAct.AFTERMATH;
    }

    public static double campaignDay(long elapsedTicks) {
        return Math.max(0L, elapsedTicks) / (double) TICKS_PER_DAY;
    }

    public static double campaignProgress(long elapsedTicks, int campaignLengthDays) {
        validateCampaignLength(campaignLengthDays);
        double duration = campaignLengthDays * (double) TICKS_PER_DAY;
        return Math.max(0.0D, Math.min(1.0D, Math.max(0L, elapsedTicks) / duration));
    }

    /** Maps both supported campaign lengths onto the validated fifty-day story windows. */
    public static double logicalStoryDay(long elapsedTicks, int campaignLengthDays) {
        return campaignProgress(elapsedTicks, campaignLengthDays) * STANDARD_LENGTH_DAYS;
    }

    public static long culminationScheduledTick(long seed, int campaignLengthDays) {
        validateCampaignLength(campaignLengthDays);
        double roll = deterministicUnit(seed, 0, CULMINATION_SCHEDULE_SALT);
        double progress = CULMINATION_SCHEDULE_MIN_PROGRESS
                + (CULMINATION_SCHEDULE_MAX_PROGRESS - CULMINATION_SCHEDULE_MIN_PROGRESS) * roll;
        return Math.round(progress * campaignLengthDays * TICKS_PER_DAY);
    }

    public static CampaignCulminationAction culminationAction(
            long elapsedTicks,
            int campaignLengthDays,
            CampaignCulminationState state,
            long scheduledTick,
            long retryTick) {
        validateCampaignLength(campaignLengthDays);
        if (state == null || state == CampaignCulminationState.UNINITIALIZED || state.isTerminal()) {
            return CampaignCulminationAction.NONE;
        }
        double progress = campaignProgress(elapsedTicks, campaignLengthDays);
        if (progress >= 1.0D) {
            return CampaignCulminationAction.EXPIRE;
        }
        if (progress >= CULMINATION_FORCED_ATTEMPT_CUTOFF_PROGRESS) {
            return CampaignCulminationAction.NONE;
        }
        long nextAttempt = Math.max(scheduledTick, retryTick);
        return nextAttempt != Long.MIN_VALUE && elapsedTicks >= nextAttempt
                ? CampaignCulminationAction.TRY_TENSION_BUILDER
                : CampaignCulminationAction.NONE;
    }

    public static boolean naturalMajorCanSatisfy(long elapsedTicks, int campaignLengthDays) {
        double progress = campaignProgress(elapsedTicks, campaignLengthDays);
        return progress >= CULMINATION_NATURAL_SATISFACTION_PROGRESS && progress < 1.0D;
    }

    public static boolean hasStrongEventGap(long elapsedTicks, long lastStrongEventTick) {
        return lastStrongEventTick == Long.MIN_VALUE
                || (elapsedTicks >= lastStrongEventTick
                        && elapsedTicks - lastStrongEventTick >= CULMINATION_STRONG_EVENT_GAP_TICKS);
    }

    /** Sleep advances the clock; frozen/backwards time still advances; one tick cannot skip over one day. */
    public static long elapsedDelta(long previousDayTime, long currentDayTime) {
        if (previousDayTime == Long.MIN_VALUE) return 0L;
        long raw;
        try {
            raw = Math.subtractExact(currentDayTime, previousDayTime);
        } catch (ArithmeticException ignored) {
            raw = TICKS_PER_DAY;
        }
        if (raw <= 0L) return 1L;
        return Math.min(raw, TICKS_PER_DAY);
    }

    public static CampaignBeat nextBeat(CampaignBeat current, double roll) {
        validateUnit(roll);
        return switch (current) {
            case REST -> CampaignBeat.UNEASE;
            case UNEASE -> roll < 0.76D ? CampaignBeat.PRESSURE : CampaignBeat.REST;
            case PRESSURE -> roll < 0.62D ? CampaignBeat.RELEASE : CampaignBeat.UNEASE;
            case RELEASE -> CampaignBeat.AFTERSHOCK;
            case AFTERSHOCK -> roll < 0.68D ? CampaignBeat.REST : CampaignBeat.UNEASE;
        };
    }

    public static long beatDurationTicks(CampaignBeat beat, double roll) {
        validateUnit(roll);
        double minDays = switch (beat) {
            case REST -> 0.55D;
            case UNEASE -> 0.35D;
            case PRESSURE -> 0.25D;
            case RELEASE -> 0.08D;
            case AFTERSHOCK -> 0.35D;
        };
        double maxDays = switch (beat) {
            case REST -> 1.25D;
            case UNEASE -> 0.80D;
            case PRESSURE -> 0.60D;
            case RELEASE -> 0.18D;
            case AFTERSHOCK -> 0.85D;
        };
        return Math.max(1L, Math.round((minDays + (maxDays - minDays) * roll) * TICKS_PER_DAY));
    }

    public static double triggerChance(
            double baseChance, ParanoiaEventLane lane, CampaignAct act, CampaignBeat beat) {
        if (!Double.isFinite(baseChance) || baseChance < 0.0D) {
            throw new IllegalArgumentException("baseChance must be finite and non-negative");
        }
        double adjusted = baseChance * actLaneMultiplier(act, lane) * beatLaneMultiplier(beat, lane);
        return Math.max(0.0D, Math.min(0.85D, adjusted));
    }

    public static int adjustedWeight(
            int baseWeight,
            ParanoiaEventLane lane,
            ParanoiaEventSeverity severity,
            CampaignEventFamily family,
            List<String> recentFamilies,
            long elapsedTicks,
            long lastStrongEventTick,
            CampaignAct act,
            CampaignBeat beat) {
        if (baseWeight <= 0) return 0;
        double multiplier = familyNoveltyMultiplier(family, recentFamilies);
        if (severity == ParanoiaEventSeverity.HIGH || severity == ParanoiaEventSeverity.EXTREME) {
            multiplier *= strongEventMultiplier(elapsedTicks, lastStrongEventTick, act, beat);
        }
        if (lane != ParanoiaEventLane.AMBIENT && severity != ParanoiaEventSeverity.LIGHT) {
            multiplier *= switch (act) {
                case OPENING -> 0.88D;
                case DEEPENING -> 0.94D;
                case ENCROACHMENT -> 1.00D;
                case FRACTURE -> 1.06D;
                case CULMINATION -> 1.12D;
                case AFTERMATH -> 0.94D;
            };
        }
        if (lane == ParanoiaEventLane.AMBIENT && beat == CampaignBeat.UNEASE) multiplier *= 1.08D;
        if (lane != ParanoiaEventLane.AMBIENT && beat == CampaignBeat.RELEASE
                && severity != ParanoiaEventSeverity.HIGH && severity != ParanoiaEventSeverity.EXTREME) {
            multiplier *= 0.88D;
        }
        return Math.max(1, (int) Math.round(baseWeight * multiplier));
    }

    public static double deterministicUnit(long seed, int sequence, long salt) {
        long mixed = seed + 0x9E3779B97F4A7C15L * (sequence + 1L) + salt;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return (mixed >>> 11) * 0x1.0p-53;
    }

    private static double familyNoveltyMultiplier(CampaignEventFamily family, List<String> recentFamilies) {
        int occurrences = 0;
        int firstIndex = Integer.MAX_VALUE;
        String familyName = family.name();
        for (int i = 0; i < recentFamilies.size(); i++) {
            if (familyName.equals(recentFamilies.get(i))) {
                occurrences++;
                firstIndex = Math.min(firstIndex, i);
            }
        }
        if (firstIndex == 0) return 0.32D;
        if (occurrences >= 2) return 0.55D;
        if (occurrences == 1) return 0.78D;
        return 1.0D;
    }

    private static double strongEventMultiplier(
            long elapsedTicks, long lastStrongEventTick, CampaignAct act, CampaignBeat beat) {
        double multiplier = switch (beat) {
            case REST -> 0.42D;
            case UNEASE -> 0.68D;
            case PRESSURE -> 1.00D;
            case RELEASE -> 1.38D;
            case AFTERSHOCK -> 0.38D;
        };
        multiplier *= switch (act) {
            case OPENING -> 0.72D;
            case DEEPENING -> 0.88D;
            case ENCROACHMENT -> 1.00D;
            case FRACTURE -> 1.10D;
            case CULMINATION -> 1.22D;
            case AFTERMATH -> 0.90D;
        };
        if (lastStrongEventTick != Long.MIN_VALUE && elapsedTicks >= lastStrongEventTick) {
            long gap = elapsedTicks - lastStrongEventTick;
            if (gap < Math.round(0.35D * TICKS_PER_DAY)) multiplier *= 0.18D;
            else if (gap < Math.round(0.75D * TICKS_PER_DAY)) multiplier *= 0.48D;
        }
        return multiplier;
    }

    private static double actLaneMultiplier(CampaignAct act, ParanoiaEventLane lane) {
        return switch (lane) {
            case PRIMARY -> switch (act) {
                case OPENING, DEEPENING -> 1.00D;
                case ENCROACHMENT -> 1.03D;
                case FRACTURE -> 1.00D;
                case CULMINATION -> 0.96D;
                case AFTERMATH -> 0.86D;
            };
            case AMBIENT -> switch (act) {
                case OPENING -> 1.08D;
                case DEEPENING -> 1.04D;
                case ENCROACHMENT -> 0.98D;
                case FRACTURE -> 0.92D;
                case CULMINATION -> 0.84D;
                case AFTERMATH -> 0.92D;
            };
            case SPECIAL -> switch (act) {
                case OPENING -> 0.88D;
                case DEEPENING -> 0.96D;
                case ENCROACHMENT -> 1.04D;
                case FRACTURE -> 1.12D;
                case CULMINATION -> 1.18D;
                case AFTERMATH -> 0.96D;
            };
            case CONTEXTUAL, CONTROL -> 1.00D;
        };
    }

    private static double beatLaneMultiplier(CampaignBeat beat, ParanoiaEventLane lane) {
        return switch (lane) {
            case PRIMARY -> switch (beat) {
                case REST -> 0.72D; case UNEASE -> 0.92D; case PRESSURE -> 1.08D;
                case RELEASE -> 0.96D; case AFTERSHOCK -> 0.66D;
            };
            case AMBIENT -> switch (beat) {
                case REST -> 0.90D; case UNEASE -> 1.18D; case PRESSURE -> 1.02D;
                case RELEASE -> 0.74D; case AFTERSHOCK -> 0.82D;
            };
            case SPECIAL -> switch (beat) {
                case REST -> 0.56D; case UNEASE -> 0.78D; case PRESSURE -> 1.06D;
                case RELEASE -> 1.32D; case AFTERSHOCK -> 0.54D;
            };
            case CONTEXTUAL, CONTROL -> 1.00D;
        };
    }

    private static void validateCampaignLength(int days) {
        if (days != STANDARD_LENGTH_DAYS && days != EXTRA_LONG_LENGTH_DAYS) {
            throw new IllegalArgumentException("campaignLengthDays must be 50 or 100");
        }
    }

    private static void validateUnit(double value) {
        if (!Double.isFinite(value) || value < 0.0D || value >= 1.0D) {
            throw new IllegalArgumentException("roll must be in [0, 1)");
        }
    }
}
