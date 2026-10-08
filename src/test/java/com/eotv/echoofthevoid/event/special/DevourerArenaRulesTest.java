package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DevourerArenaRulesTest {
    @Test
    void pursuerWavesStartAtEightAndAddTwoEveryTenSeconds() {
        assertEquals(8, DevourerArenaRules.expectedPursuerCount(-1));
        assertEquals(8, DevourerArenaRules.expectedPursuerCount(0));
        assertEquals(8, DevourerArenaRules.expectedPursuerCount(199));
        assertEquals(10, DevourerArenaRules.expectedPursuerCount(200));
        assertEquals(10, DevourerArenaRules.expectedPursuerCount(399));
        assertEquals(12, DevourerArenaRules.expectedPursuerCount(400));
        assertEquals(16, DevourerArenaRules.expectedPursuerCount(999));
        assertEquals(18, DevourerArenaRules.expectedPursuerCount(1_000));
        assertEquals(18, DevourerArenaRules.expectedPursuerCount(Long.MAX_VALUE));
    }

    @Test
    void arenaPursuerAppearanceIdsRemainStableAndBounded() {
        assertEquals(9, ArenaPursuerAppearance.count());
        assertEquals(ArenaPursuerAppearance.HUMANOID, ArenaPursuerAppearance.byId(-1));
        assertEquals(ArenaPursuerAppearance.HUMANOID, ArenaPursuerAppearance.byId(0));
        assertEquals(ArenaPursuerAppearance.ZOMBIE, ArenaPursuerAppearance.byId(1));
        assertEquals(ArenaPursuerAppearance.SKELETON, ArenaPursuerAppearance.byId(2));
        assertEquals(ArenaPursuerAppearance.VILLAGER, ArenaPursuerAppearance.byId(3));
        assertEquals(ArenaPursuerAppearance.IRON_GOLEM, ArenaPursuerAppearance.byId(4));
        assertEquals(ArenaPursuerAppearance.SPIDER, ArenaPursuerAppearance.byId(5));
        assertEquals(ArenaPursuerAppearance.SHEEP, ArenaPursuerAppearance.byId(6));
        assertEquals(ArenaPursuerAppearance.COW, ArenaPursuerAppearance.byId(7));
        assertEquals(ArenaPursuerAppearance.PIG, ArenaPursuerAppearance.byId(8));
        assertEquals(ArenaPursuerAppearance.HUMANOID, ArenaPursuerAppearance.byId(9));
    }

    @Test
    void persistedPursuerCounterAcceptsTheWholeBoundedArenaPopulation() {
        assertEquals(0, DevourerArenaRules.clampPersistedPursuerCount(-1));
        assertEquals(6, DevourerArenaRules.clampPersistedPursuerCount(6));
        assertEquals(DevourerArenaRules.MAX_PURSUERS,
                DevourerArenaRules.clampPersistedPursuerCount(DevourerArenaRules.MAX_PURSUERS));
        assertEquals(DevourerArenaRules.MAX_PURSUERS,
                DevourerArenaRules.clampPersistedPursuerCount(Integer.MAX_VALUE));
    }

    @Test
    void offlineAbortUsesTheExactThirtyMinuteBoundary() {
        long start = 10_000L;
        assertFalse(DevourerArenaRules.offlineGraceExpired(start, start - 1L));
        assertFalse(DevourerArenaRules.offlineGraceExpired(start, start + DevourerArenaRules.OFFLINE_ABORT_MILLIS - 1L));
        assertTrue(DevourerArenaRules.offlineGraceExpired(start, start + DevourerArenaRules.OFFLINE_ABORT_MILLIS));
        assertFalse(DevourerArenaRules.offlineGraceExpired(0L, Long.MAX_VALUE));
    }

    @Test
    void originRetreatRequiresBothUnreachabilityAndCompleteLossOfObservation() {
        int grace = DevourerArenaRules.ORIGIN_UNREACHABLE_GRACE_TICKS;
        assertFalse(DevourerArenaRules.canBeginOriginRetreat(grace - 1, false, false));
        assertFalse(DevourerArenaRules.canBeginOriginRetreat(grace, true, false));
        assertFalse(DevourerArenaRules.canBeginOriginRetreat(grace, false, true));
        assertTrue(DevourerArenaRules.canBeginOriginRetreat(grace, false, false));
        assertEquals(10, DevourerArenaRules.ORIGIN_REACHABILITY_REFRESH_TICKS);
        assertEquals(160.0D, DevourerArenaRules.ORIGIN_OBSERVER_RANGE);
    }

    @Test
    void originForcedRetreatUsesAnExactPersistableThreeMinuteDeadline() {
        long now = 50_000L;
        assertEquals(20 * 60 * 3, DevourerArenaRules.ORIGIN_FORCED_RETREAT_TICKS);
        long freshDeadline = DevourerArenaRules.originForcedRetreatDeadline(now, 0);
        assertEquals(now + 3_600L, freshDeadline);
        assertFalse(DevourerArenaRules.originForcedRetreatDue(freshDeadline - 1L, freshDeadline));
        assertTrue(DevourerArenaRules.originForcedRetreatDue(freshDeadline, freshDeadline));
        assertFalse(DevourerArenaRules.originForcedRetreatDue(Long.MAX_VALUE, Long.MIN_VALUE));
        assertFalse(DevourerArenaRules.originForcedRetreatRequired(freshDeadline, freshDeadline, true),
                "A reachable uncaptured player must keep the multiplayer hunt alive");
        assertTrue(DevourerArenaRules.originForcedRetreatRequired(freshDeadline, freshDeadline, false),
                "After three minutes a Devourer with no possible target must sink");

        long migratedDeadline = DevourerArenaRules.originForcedRetreatDeadline(now, 900);
        assertEquals(now + 2_700L, migratedDeadline);
        assertEquals(now, DevourerArenaRules.originForcedRetreatDeadline(now, Integer.MAX_VALUE));
    }

    @Test
    void cellCoordinatesStaySeparatedAndCannotOverflow() {
        assertEquals(0, DevourerArenaRules.cellCenterX(0));
        assertEquals(2048, DevourerArenaRules.cellCenterX(1));
        assertEquals(4096, DevourerArenaRules.cellCenterX(2));
        int wrapped = DevourerArenaRules.cellCenterX(Long.MAX_VALUE);
        assertTrue(wrapped >= 0 && wrapped < 29_000_000);
        assertEquals(0, DevourerArenaRules.cellCenterX(-1));
    }

    @Test
    void arenaSpawnWorkIsStrictlyBoundedPerTick() {
        assertEquals(0, DevourerArenaRules.spawnAllowance(0, 0));
        assertEquals(2, DevourerArenaRules.spawnAllowance(18, 0));
        assertEquals(1, DevourerArenaRules.spawnAllowance(18, 1));
        assertEquals(0, DevourerArenaRules.spawnAllowance(18, 2));
        assertEquals(18, DevourerArenaRules.MAX_PURSUERS);
        assertEquals(6, DevourerArenaRules.clampReplacementCount(Integer.MAX_VALUE));
    }

    @Test
    void sectorSelectionPrefersForwardOnlyWhenItIsEmptyAndOtherwiseFillsAGap() {
        int[] empty = new int[DevourerArenaRules.SECTOR_COUNT];
        assertEquals(2, DevourerArenaRules.chooseSpawnSector(empty, 2, 0.64D, 5));
        assertEquals(5, DevourerArenaRules.chooseSpawnSector(empty, 2, 0.65D, 5));

        int[] coveredForward = {2, 2, 2, 0, 1, 1, 1, 1};
        assertEquals(3, DevourerArenaRules.chooseSpawnSector(coveredForward, 2, 0.0D, 0));
        assertEquals(0, DevourerArenaRules.sectorForDirection(1.0D, 0.0D));
        assertEquals(2, DevourerArenaRules.sectorForDirection(0.0D, 1.0D));
        assertEquals(4, DevourerArenaRules.sectorForDirection(-1.0D, 0.0D));
        assertEquals(6, DevourerArenaRules.sectorForDirection(0.0D, -1.0D));
    }

    @Test
    void replacementBudgetAndBreathingWindowSurviveExactBoundaries() {
        assertFalse(DevourerArenaRules.replacementReady(59, 0, 0));
        assertTrue(DevourerArenaRules.replacementReady(60, 0, 0));
        assertFalse(DevourerArenaRules.replacementReady(600, -1, 0));
        assertFalse(DevourerArenaRules.replacementReady(600, 0, DevourerArenaRules.MAX_REPLACEMENTS));
    }

    @Test
    void elsewherePursuersOutclassAttackerDamageAndKeepWalkingPace() {
        AdaptiveSpecialCombatProfile unarmed = AdaptiveSpecialCombatProfile.attacker(20.0D, 0.1D, 1.0D, 0.0D, 0.0D);
        AdaptiveSpecialCombatProfile netherite = AdaptiveSpecialCombatProfile.attacker(20.0D, 0.1D, 10.0D, 20.0D, 12.0D);
        assertEquals(8.0D, DevourerArenaRules.pursuerProfile(unarmed).attackDamage(), 1.0E-9D);
        assertEquals(20.0D, DevourerArenaRules.pursuerProfile(netherite).attackDamage(), 1.0E-9D);
        assertEquals(20.0D, DevourerArenaRules.pursuerProfile(netherite).armor(), 1.0E-9D);
        assertEquals(0.30D, DevourerArenaRules.pursuerProfile(unarmed).movementSpeed(), 1.0E-9D);
    }

    @Test
    void elsewhereFogHidesPursuersUntilCloseButLeavesReactionTime() {
        assertEquals(0.0D, com.eotv.echoofthevoid.world.ElsewhereFogRules.fogFactor(1.5D), 1.0E-9D);
        assertTrue(com.eotv.echoofthevoid.world.ElsewhereFogRules.fogFactor(10.0D) > 0.95D);
        double readable = com.eotv.echoofthevoid.world.ElsewhereFogRules.fogFactor(7.0D);
        assertTrue(readable > 0.45D && readable < 0.75D);
        assertEquals(1.0D, com.eotv.echoofthevoid.world.ElsewhereFogRules.fogFactor(12.0D), 1.0E-9D);
        // Black silhouettes need a fog that is lighter than black to stand out at every brightness.
        assertTrue(com.eotv.echoofthevoid.world.ElsewhereFogRules.FOG_RED > 0.02F);
    }

    @Test
    void onlyTheSpiderSilhouetteClimbsAndEveryFormHasItsOwnHitbox() {
        for (ArenaPursuerAppearance appearance : ArenaPursuerAppearance.values()) {
            assertEquals(appearance == ArenaPursuerAppearance.SPIDER, appearance.climbs());
            assertEquals(appearance, ArenaPursuerAppearance.byId(appearance.id()));
            assertTrue(appearance.width() > 0.0F && appearance.height() > 0.0F);
        }
        assertTrue(ArenaPursuerAppearance.count() > DevourerArenaRules.INITIAL_PURSUERS);
    }
    @Test
    void twoClimbersComeFirstThenWalkingFormsCycle() {
        assertEquals(ArenaPursuerAppearance.SPIDER, DevourerArenaRules.appearanceFor(0, 0));
        assertEquals(ArenaPursuerAppearance.SPIDER, DevourerArenaRules.appearanceFor(7, 1));
        java.util.Set<ArenaPursuerAppearance> walkers = java.util.EnumSet.noneOf(ArenaPursuerAppearance.class);
        for (int index = 0; index < 8; index++) {
            ArenaPursuerAppearance appearance = DevourerArenaRules.appearanceFor(index, 2);
            assertFalse(appearance.climbs(), "with two climbers alive new pursuers must walk");
            walkers.add(appearance);
        }
        assertEquals(ArenaPursuerAppearance.count() - 1, walkers.size());
    }

    @Test
    void walkersNeverDigAPillarAway() {
        assertTrue(DevourerArenaRules.mayScratchTowards(13.0D, 13.0D), "a wall at their height may be cleared");
        assertTrue(DevourerArenaRules.mayScratchTowards(13.0D, 14.0D), "one step up is still a wall");
        assertFalse(DevourerArenaRules.mayScratchTowards(13.0D, 15.0D), "a target two blocks up is on a pillar");
    }
}
