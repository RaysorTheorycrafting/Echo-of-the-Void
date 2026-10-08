package com.eotv.echoofthevoid.state;

import com.eotv.echoofthevoid.event.paranoia.TensionPacingRules;

/**
 * Pure migration rules for persisted deadlines that use MinecraftServer#getTickCount().
 * That counter restarts at zero for every integrated or dedicated server session.
 */
public final class PersistedTimerRebaseRules {
    public static final long UNSET = Long.MIN_VALUE;

    private static final long MAX_TENSION_REMAINING_TICKS = TensionPacingRules.secondsToTicks(
            TensionPacingRules.TENSION_MAX_SECONDS);
    private static final long MAX_BREAK_REMAINING_TICKS = TensionPacingRules.secondsToTicks(
            TensionPacingRules.BREAK_MAX_SECONDS);
    private static final long MAX_BOOST_REMAINING_TICKS = TensionPacingRules.secondsToTicks(
            TensionPacingRules.GRAND_BOOST_MAX_SECONDS);
    private static final long MAX_GRAND_ROLL_REMAINING_TICKS = TensionPacingRules.secondsToTicks(
            TensionPacingRules.GRAND_COOLDOWN_SECONDS);
    private static final long MAX_PENDING_GRAND_REMAINING_TICKS = TensionPacingRules.secondsToTicks(30);

    private PersistedTimerRebaseRules() {
    }

    public static TensionTimers rebaseTensionTimers(
            long tensionEndTick,
            long nextStartTick,
            long grandBoostUntilTick,
            long nextGrandRollTick,
            long lastGrandEventTick,
            long lastUpdateTick,
            long pendingGrandStartTick,
            long pendingGrandWarningTick,
            long currentSessionTick) {
        long current = Math.max(0L, currentSessionTick);
        if (lastUpdateTick == UNSET) {
            boolean hasUnsafeTimer = tensionEndTick != UNSET
                    || nextStartTick != UNSET
                    || grandBoostUntilTick != UNSET
                    || nextGrandRollTick != UNSET
                    || lastGrandEventTick != UNSET
                    || pendingGrandStartTick != UNSET
                    || pendingGrandWarningTick != UNSET;
            if (hasUnsafeTimer) {
                return new TensionTimers(
                        UNSET,
                        UNSET,
                        UNSET,
                        UNSET,
                        UNSET,
                        current,
                        UNSET,
                        UNSET,
                        true,
                        tensionEndTick != UNSET,
                        pendingGrandStartTick != UNSET);
            }
            return new TensionTimers(
                    tensionEndTick,
                    nextStartTick,
                    grandBoostUntilTick,
                    nextGrandRollTick,
                    lastGrandEventTick,
                    lastUpdateTick,
                    pendingGrandStartTick,
                    pendingGrandWarningTick,
                    false,
                    false,
                    false);
        }

        long rebasedEnd = rebaseDeadline(
                tensionEndTick, lastUpdateTick, current, MAX_TENSION_REMAINING_TICKS);
        long rebasedNextStart = rebaseDeadline(
                nextStartTick, lastUpdateTick, current, MAX_BREAK_REMAINING_TICKS);
        long rebasedBoost = rebaseDeadline(
                grandBoostUntilTick, lastUpdateTick, current, MAX_BOOST_REMAINING_TICKS);
        long rebasedNextRoll = rebaseDeadline(
                nextGrandRollTick, lastUpdateTick, current, MAX_GRAND_ROLL_REMAINING_TICKS);
        long rebasedPendingStart = rebaseDeadline(
                pendingGrandStartTick, lastUpdateTick, current, MAX_PENDING_GRAND_REMAINING_TICKS);
        long rebasedLastGrand = rebasePastTick(
                lastGrandEventTick, lastUpdateTick, current, MAX_GRAND_ROLL_REMAINING_TICKS);
        long rebasedWarning = rebasedPendingStart == UNSET
                ? UNSET
                : rebasePastTick(
                        pendingGrandWarningTick,
                        lastUpdateTick,
                        current,
                        MAX_PENDING_GRAND_REMAINING_TICKS);

        boolean activeLockCleared = tensionEndTick != UNSET
                && tensionEndTick > lastUpdateTick
                && rebasedEnd == UNSET;
        boolean pendingGrandCleared = pendingGrandStartTick != UNSET && rebasedPendingStart == UNSET;
        boolean changed = rebasedEnd != tensionEndTick
                || rebasedNextStart != nextStartTick
                || rebasedBoost != grandBoostUntilTick
                || rebasedNextRoll != nextGrandRollTick
                || rebasedLastGrand != lastGrandEventTick
                || current != lastUpdateTick
                || rebasedPendingStart != pendingGrandStartTick
                || rebasedWarning != pendingGrandWarningTick;

        return new TensionTimers(
                rebasedEnd,
                rebasedNextStart,
                rebasedBoost,
                rebasedNextRoll,
                rebasedLastGrand,
                current,
                rebasedPendingStart,
                rebasedWarning,
                changed,
                activeLockCleared,
                pendingGrandCleared);
    }

    private static long rebaseDeadline(long deadline, long previousTick, long currentTick, long maximumRemaining) {
        if (deadline == UNSET) {
            return UNSET;
        }
        long remaining = safeDifference(deadline, previousTick);
        if (remaining <= 0L || remaining > maximumRemaining) {
            return UNSET;
        }
        return saturatingAdd(currentTick, remaining);
    }

    private static long rebasePastTick(long timestamp, long previousTick, long currentTick, long maximumAge) {
        if (timestamp == UNSET) {
            return UNSET;
        }
        long age = safeDifference(previousTick, timestamp);
        if (age < 0L || age > maximumAge) {
            return UNSET;
        }
        return currentTick - age;
    }

    private static long safeDifference(long later, long earlier) {
        if (earlier < 0L && later > Long.MAX_VALUE + earlier) {
            return Long.MAX_VALUE;
        }
        if (earlier > 0L && later < Long.MIN_VALUE + earlier) {
            return Long.MIN_VALUE;
        }
        return later - earlier;
    }

    private static long saturatingAdd(long left, long right) {
        return right > 0L && left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    public record TensionTimers(
            long tensionEndTick,
            long nextStartTick,
            long grandBoostUntilTick,
            long nextGrandRollTick,
            long lastGrandEventTick,
            long lastUpdateTick,
            long pendingGrandStartTick,
            long pendingGrandWarningTick,
            boolean changed,
            boolean activeLockCleared,
            boolean pendingGrandCleared) {
    }
}
