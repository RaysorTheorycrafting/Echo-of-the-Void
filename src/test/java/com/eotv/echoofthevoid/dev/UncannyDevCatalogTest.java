package com.eotv.echoofthevoid.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UncannyDevCatalogTest {
    @Test
    void everyEntryHasUniqueStableMetadataAndAnExecutableActionKind() {
        Set<String> ids = new HashSet<>();
        EnumSet<UncannyDevCatalog.ActionKind> representedKinds = EnumSet.noneOf(UncannyDevCatalog.ActionKind.class);

        for (UncannyDevCatalog.Entry entry : UncannyDevCatalog.entries()) {
            assertFalse(entry.id().isBlank());
            assertEquals(entry.id().toLowerCase(java.util.Locale.ROOT), entry.id(), entry.id());
            assertTrue(ids.add(entry.id()), "Duplicate dev entry ID: " + entry.id());
            assertSame(entry, UncannyDevCatalog.byId(entry.id()));
            representedKinds.add(entry.actionKind());

            UncannyDevMetadataCatalog.Info info = UncannyDevMetadataCatalog.describe(entry);
            assertNotNull(info, entry.id());
            assertTrue(info.minimumPhase() >= 1 && info.minimumPhase() <= 4, entry.id());
            assertFalse(info.description().isBlank(), entry.id());
            assertFalse(info.conditions().isBlank(), entry.id());
            assertFalse(info.restrictions().isBlank(), entry.id());
            assertFalse(info.multiplayer().isBlank(), entry.id());
            assertFalse(info.associated().isBlank(), entry.id());
            assertFalse(info.limitations().isBlank(), entry.id());
        }

        assertEquals(EnumSet.allOf(UncannyDevCatalog.ActionKind.class), representedKinds);
        assertTrue(UncannyDevCatalog.entries().size() > 150, "The workbench should expose the full current catalog.");
    }

    @Test
    void newAudioAndToolSurfacesArePresentWithoutRestoringRetiredGameplay() {
        assertNotNull(UncannyDevCatalog.byId("audio_mental_uncanny_tinnitus"));
        assertNotNull(UncannyDevCatalog.byId("audio_physical_uncanny_knocker_knock"));
        assertNotNull(UncannyDevCatalog.byId("tool_cleanup_entities"));
        assertNotNull(UncannyDevCatalog.byId("tool_reset_transient"));
        assertNotNull(UncannyDevCatalog.byId("tool_phase_4"));
        assertNotNull(UncannyDevCatalog.byId("entity_iron_golem_v1"));
        assertNotNull(UncannyDevCatalog.byId("entity_iron_golem_v5"));
        assertNotNull(UncannyDevCatalog.byId("entity_rabbit_spawn"));
        assertNotNull(UncannyDevCatalog.byId("entity_rabbit_v5"));

        Set<String> retired = Set.of("forced_drop", "armor_break", "giant_sun");
        assertTrue(UncannyDevCatalog.entries().stream()
                .noneMatch(entry -> retired.contains(entry.actionArg())));
    }

    @Test
    void historicalSpecialAliasesResolveToTheCorrectDetailedDescriptions() {
        UncannyDevMetadataCatalog.Info stalker =
                UncannyDevMetadataCatalog.describe(UncannyDevCatalog.byId("entity_attacker_spawn"));
        UncannyDevMetadataCatalog.Info pulse =
                UncannyDevMetadataCatalog.describe(UncannyDevCatalog.byId("entity_presence_spawn"));

        assertTrue(stalker.description().contains("Stalker"));
        assertTrue(pulse.description().contains("Pulse"));
        assertEquals(UncannyDevMetadataCatalog.ContentType.ENTITY, stalker.type());
        assertEquals(UncannyDevMetadataCatalog.ContentType.ENTITY, pulse.type());
    }

    @Test
    void categorySurfaceIncludesSearchableAudioAndDebugTools() {
        assertEquals(
                List.of(
                        UncannyDevCatalog.Category.ENTITIES,
                        UncannyDevCatalog.Category.EVENTS,
                        UncannyDevCatalog.Category.WEATHER,
                        UncannyDevCatalog.Category.STRUCTURES,
                        UncannyDevCatalog.Category.AUDIO,
                        UncannyDevCatalog.Category.TOOLS),
                UncannyDevCatalog.primaryCategories());
    }
}
