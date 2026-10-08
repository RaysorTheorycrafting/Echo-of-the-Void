package com.eotv.echoofthevoid.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PersistedTimerRebaseRulesTest {
    private static final long UNSET = Long.MIN_VALUE;

    @Test
    void clearsTheStaleActiveLockCapturedFromTheAffectedWorld() {
        PersistedTimerRebaseRules.TensionTimers timers = PersistedTimerRebaseRules.rebaseTensionTimers(
                82_860L,
                UNSET,
                UNSET,
                UNSET,
                UNSET,
                1_665L,
                UNSET,
                UNSET,
                0L);

        assertEquals(UNSET, timers.tensionEndTick());
        assertEquals(0L, timers.lastUpdateTick());
        assertTrue(timers.changed());
        assertTrue(timers.activeLockCleared());
    }

    @Test
    void preservesOnlyValidRemainingDurationsAcrossARealRestart() {
        PersistedTimerRebaseRules.TensionTimers timers = PersistedTimerRebaseRules.rebaseTensionTimers(
                108_000L,
                150_000L,
                101_200L,
                100_300L,
                99_000L,
                100_000L,
                100_140L,
                100_000L,
                40L);

        assertEquals(8_040L, timers.tensionEndTick());
        assertEquals(50_040L, timers.nextStartTick());
        assertEquals(1_240L, timers.grandBoostUntilTick());
        assertEquals(340L, timers.nextGrandRollTick());
        assertEquals(-960L, timers.lastGrandEventTick());
        assertEquals(180L, timers.pendingGrandStartTick());
        assertEquals(40L, timers.pendingGrandWarningTick());
        assertFalse(timers.activeLockCleared());
        assertFalse(timers.pendingGrandCleared());
    }

    @Test
    void discardsSessionDeadlinesWhenAnOlderSaveHasNoTickAnchor() {
        PersistedTimerRebaseRules.TensionTimers timers = PersistedTimerRebaseRules.rebaseTensionTimers(
                7_000L,
                25_000L,
                UNSET,
                UNSET,
                UNSET,
                UNSET,
                6_000L,
                5_900L,
                12L);

        assertEquals(UNSET, timers.tensionEndTick());
        assertEquals(UNSET, timers.nextStartTick());
        assertEquals(UNSET, timers.pendingGrandStartTick());
        assertEquals(12L, timers.lastUpdateTick());
        assertTrue(timers.activeLockCleared());
        assertTrue(timers.pendingGrandCleared());
    }

    @Test
    void leavesACompletelyLegacyStateUntouchedUntilTheRuntimeInitializesIt() {
        PersistedTimerRebaseRules.TensionTimers timers = PersistedTimerRebaseRules.rebaseTensionTimers(
                UNSET, UNSET, UNSET, UNSET, UNSET, UNSET, UNSET, UNSET, 0L);

        assertFalse(timers.changed());
        assertEquals(UNSET, timers.lastUpdateTick());
    }
}
