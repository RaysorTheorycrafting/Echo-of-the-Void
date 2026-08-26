package com.eotv.echoofthevoid.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UncannyDevQueryEngineTest {
    @Test
    void globalSearchUsesDescriptionsAndNotOnlyVisibleLabels() {
        List<UncannyDevCatalog.Entry> result = query(new UncannyDevQueryEngine.Query(
                "no arbitrary invisible burst damage",
                UncannyDevCatalog.Category.ALL,
                0,
                null,
                UncannyDevQueryEngine.QaFilter.ALL,
                UncannyDevQueryEngine.Scope.ALL,
                UncannyDevQueryEngine.SortMode.NAME));

        assertTrue(result.stream().anyMatch(entry -> entry.id().equals("entity_presence_spawn")));
    }

    @Test
    void phaseDangerFavoriteAndQaFiltersComposeDeterministically() {
        UncannyDevCatalog.Entry shadow = UncannyDevCatalog.byId("event_orphan_shadow");
        UncannyDevMetadataCatalog.Danger danger = UncannyDevMetadataCatalog.describe(shadow).danger();
        Map<String, UncannyDevCatalog.QaStatus> statuses = Map.of(shadow.id(), UncannyDevCatalog.QaStatus.GREEN);
        UncannyDevQueryEngine.Query query = new UncannyDevQueryEngine.Query(
                "shadow",
                UncannyDevCatalog.Category.EVENTS,
                2,
                danger,
                UncannyDevQueryEngine.QaFilter.GREEN,
                UncannyDevQueryEngine.Scope.FAVORITES,
                UncannyDevQueryEngine.SortMode.PHASE);

        List<UncannyDevCatalog.Entry> result = UncannyDevQueryEngine.query(
                UncannyDevCatalog.entries(),
                query,
                Set.of(shadow.id()),
                List.of(),
                id -> statuses.getOrDefault(id, UncannyDevCatalog.QaStatus.GRAY));

        assertEquals(List.of(shadow), result);
        assertTrue(result.stream().allMatch(entry -> UncannyDevMetadataCatalog.describe(entry).minimumPhase() <= 2));
    }

    @Test
    void recentSortPreservesMostRecentFirstAndExcludesOtherEntries() {
        List<String> recent = List.of("tool_phase_4", "audio_mental_uncanny_tinnitus", "event_orphan_shadow");
        UncannyDevQueryEngine.Query query = new UncannyDevQueryEngine.Query(
                "",
                UncannyDevCatalog.Category.ALL,
                0,
                null,
                UncannyDevQueryEngine.QaFilter.ALL,
                UncannyDevQueryEngine.Scope.RECENT,
                UncannyDevQueryEngine.SortMode.RECENT);

        List<UncannyDevCatalog.Entry> result = UncannyDevQueryEngine.query(
                UncannyDevCatalog.entries(), query, Set.of(), recent, ignored -> UncannyDevCatalog.QaStatus.GRAY);

        assertEquals(recent, result.stream().map(UncannyDevCatalog.Entry::id).toList());
    }

    @Test
    void aPhaseFilterMeansAvailableByThatPhaseRatherThanExactMinimumPhase() {
        List<UncannyDevCatalog.Entry> phaseTwo = query(new UncannyDevQueryEngine.Query(
                "",
                UncannyDevCatalog.Category.EVENTS,
                2,
                null,
                UncannyDevQueryEngine.QaFilter.ALL,
                UncannyDevQueryEngine.Scope.ALL,
                UncannyDevQueryEngine.SortMode.NAME));

        assertFalse(phaseTwo.isEmpty());
        assertTrue(phaseTwo.stream().allMatch(entry -> UncannyDevMetadataCatalog.describe(entry).minimumPhase() <= 2));
        assertFalse(phaseTwo.stream().anyMatch(entry -> entry.id().equals("event_blackout")));
    }

    private static List<UncannyDevCatalog.Entry> query(UncannyDevQueryEngine.Query query) {
        return UncannyDevQueryEngine.query(
                UncannyDevCatalog.entries(), query, Set.of(), List.of(), ignored -> UncannyDevCatalog.QaStatus.GRAY);
    }
}
