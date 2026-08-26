package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WatcherObservationRulesTest {
    @Test
    void sleepingPlayersCannotSpawnOrValidateAWatcherEncounter() {
        assertTrue(WatcherObservationRules.blocksEncounter(true, false, false));
        assertFalse(WatcherObservationRules.canAccumulateDirectLook(true, true, 1.0D));
    }

    @Test
    void waterAndBoatsKeepTheirExistingSpawnExclusions() {
        assertTrue(WatcherObservationRules.blocksEncounter(false, true, false));
        assertTrue(WatcherObservationRules.blocksEncounter(false, false, true));
        assertFalse(WatcherObservationRules.blocksEncounter(false, false, false));
    }

    @Test
    void awakeObservationStillRequiresLineOfSightAndTheStrictLookCone() {
        assertFalse(WatcherObservationRules.canAccumulateDirectLook(false, false, 1.0D));
        assertFalse(WatcherObservationRules.canAccumulateDirectLook(
                false, true, WatcherObservationRules.DIRECT_LOOK_DOT_THRESHOLD));
        assertTrue(WatcherObservationRules.canAccumulateDirectLook(false, true, 0.931D));
        assertFalse(WatcherObservationRules.canAccumulateDirectLook(false, true, Double.NaN));
    }
}
