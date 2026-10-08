package com.eotv.echoofthevoid.event.paranoia;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UncannyDimensionPolicyTest {
    @Test
    void endUsesOnlyItsCuratedSpecialPool() {
        assertFalse(UncannyDimensionRules.runsOrdinarySchedulerIn(UncannyDimensionRules.DimensionKind.END));
        assertFalse(UncannyDimensionRules.allowsNaturalEventIn(
                UncannyDimensionRules.DimensionKind.END, ParanoiaEventIds.FOOTSTEPS));
        assertTrue(UncannyDimensionRules.allowsNaturalSpecialIn(
                UncannyDimensionRules.DimensionKind.END, ParanoiaEventIds.FOLLOWER));
        assertTrue(UncannyDimensionRules.allowsNaturalSpecialIn(
                UncannyDimensionRules.DimensionKind.END, ParanoiaEventIds.MOURNER));
        assertTrue(UncannyDimensionRules.allowsNaturalSpecialIn(
                UncannyDimensionRules.DimensionKind.END, ParanoiaEventIds.DEVOURER));
        assertFalse(UncannyDimensionRules.allowsNaturalSpecialIn(
                UncannyDimensionRules.DimensionKind.END, ParanoiaEventIds.MINER));
        assertFalse(UncannyDimensionRules.allowsNaturalSpecialIn(
                UncannyDimensionRules.DimensionKind.END, ParanoiaEventIds.WATCHER));
        assertFalse(UncannyDimensionRules.allowsNaturalSpecialIn(
                UncannyDimensionRules.DimensionKind.END, ParanoiaEventIds.FERRYMAN));
    }

    @Test
    void netherUsesLavaAndUndergroundGrammarInsteadOfWaterEvents() {
        var nether = UncannyDimensionRules.DimensionKind.NETHER;
        assertTrue(UncannyDimensionRules.runsOrdinarySchedulerIn(nether));
        assertTrue(UncannyDimensionRules.allowsNaturalEventIn(nether, ParanoiaEventIds.LAVA_WAKE));
        assertTrue(UncannyDimensionRules.allowsNaturalEventIn(nether, ParanoiaEventIds.GHOST_MINER));
        assertFalse(UncannyDimensionRules.allowsNaturalEventIn(nether, ParanoiaEventIds.EMPTY_WAKE));
        assertFalse(UncannyDimensionRules.allowsNaturalEventIn(nether, ParanoiaEventIds.FISHING_TUG));
        assertFalse(UncannyDimensionRules.allowsNaturalSpecialIn(nether, ParanoiaEventIds.USHER));
        assertTrue(UncannyDimensionRules.allowsNaturalSpecialIn(nether, ParanoiaEventIds.STALKER));
        assertTrue(UncannyDimensionRules.allowsNaturalSpecialIn(nether, ParanoiaEventIds.MINER));
        assertTrue(UncannyDimensionRules.allowsNaturalSpecialIn(nether, ParanoiaEventIds.DEVOURER));
        assertTrue(UncannyDimensionRules.allowsNaturalSpecialIn(nether, ParanoiaEventIds.ECHOER));
        assertTrue(UncannyDimensionRules.allowsNaturalSpecialIn(nether, ParanoiaEventIds.ASHWALKER));
        assertTrue(UncannyDimensionRules.allowsNaturalSpecialIn(nether, ParanoiaEventIds.FLANKER));
        assertFalse(UncannyDimensionRules.allowsNaturalSpecialIn(nether, ParanoiaEventIds.DRIFTER));
        assertFalse(UncannyDimensionRules.allowsNaturalSpecialIn(nether, ParanoiaEventIds.DREDGER));
    }

    @Test
    void overworldKeepsTheFullExistingSurfaceButNotLavaWake() {
        var overworld = UncannyDimensionRules.DimensionKind.OVERWORLD;
        assertTrue(UncannyDimensionRules.runsOrdinarySchedulerIn(overworld));
        assertTrue(UncannyDimensionRules.allowsNaturalEventIn(overworld, ParanoiaEventIds.EMPTY_WAKE));
        assertFalse(UncannyDimensionRules.allowsNaturalEventIn(overworld, ParanoiaEventIds.LAVA_WAKE));
        assertTrue(UncannyDimensionRules.allowsNaturalSpecialIn(overworld, ParanoiaEventIds.WATCHER));
        assertTrue(UncannyDimensionRules.allowsNaturalSpecialIn(overworld, ParanoiaEventIds.FERRYMAN));
        assertTrue(UncannyDimensionRules.allowsNaturalSpecialIn(overworld, ParanoiaEventIds.MINER));
        assertTrue(UncannyDimensionRules.allowsNaturalSpecialIn(overworld, ParanoiaEventIds.DEVOURER));
        assertTrue(UncannyDimensionRules.allowsNaturalSpecialIn(overworld, ParanoiaEventIds.ECHOER));
        assertTrue(UncannyDimensionRules.allowsNaturalSpecialIn(overworld, ParanoiaEventIds.DRIFTER));
        assertTrue(UncannyDimensionRules.allowsNaturalSpecialIn(overworld, ParanoiaEventIds.DREDGER));
        assertTrue(UncannyDimensionRules.allowsNaturalSpecialIn(overworld, ParanoiaEventIds.FLANKER));
        assertFalse(UncannyDimensionRules.allowsNaturalSpecialIn(overworld, ParanoiaEventIds.ASHWALKER));
    }

    @Test
    void customDimensionsRespectTheirThermalPhysics() {
        assertTrue(UncannyDimensionRules.allowsNaturalEventIn(
                UncannyDimensionRules.DimensionKind.OTHER, ParanoiaEventIds.EMPTY_WAKE));
        assertFalse(UncannyDimensionRules.allowsNaturalEventIn(
                UncannyDimensionRules.DimensionKind.OTHER, ParanoiaEventIds.LAVA_WAKE));
        assertFalse(UncannyDimensionRules.allowsNaturalEventIn(
                UncannyDimensionRules.DimensionKind.OTHER_ULTRAWARM, ParanoiaEventIds.EMPTY_WAKE));
        assertTrue(UncannyDimensionRules.allowsNaturalEventIn(
                UncannyDimensionRules.DimensionKind.OTHER_ULTRAWARM, ParanoiaEventIds.LAVA_WAKE));
    }
}
