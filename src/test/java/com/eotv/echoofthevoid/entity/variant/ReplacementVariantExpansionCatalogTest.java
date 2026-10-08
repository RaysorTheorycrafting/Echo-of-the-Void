package com.eotv.echoofthevoid.entity.variant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eotv.echoofthevoid.dev.UncannyDevCatalog;
import com.eotv.echoofthevoid.dev.UncannyDevMetadataCatalog;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ReplacementVariantExpansionCatalogTest {
    private static final List<String> HISTORICAL_PRIMARY_IDS = List.of(
            "husk_parched", "drowned_undertow", "zombie_villager_borrowed_voice",
            "stray_distant_watch", "endermite_stasis_burst", "ghast_hunter",
            "phantom_grounded", "pillager_patient_volley", "vindicator_glitch_step",
            "evoker_delayed_fangs", "ravager_broken_charge", "blaze_vertical_fault",
            "wither_skeleton_archer", "piglin_brute_observed_statue", "hoglin_nameless",
            "slime_crawler", "magma_cube_lava_crawler");

    @Test
    void everyHistoricalReplacementNowHasFiveDistinctProfiles() {
        assertEquals(17, ReplacementVariantExpansionCatalog.species().size());
        assertEquals(85, ReplacementVariantExpansionCatalog.variants().size());
        assertEquals(17, new HashSet<>(ReplacementVariantExpansionCatalog.species().stream()
                .map(ReplacementVariantExpansionCatalog.Species::entityTypePath)
                .toList()).size());

        for (ReplacementVariantExpansionCatalog.Species species
                : ReplacementVariantExpansionCatalog.species()) {
            assertEquals(5, species.variants().size(), species.entityTypePath());
            assertEquals(List.of(1, 2, 3, 4, 5),
                    species.variants().stream()
                            .map(ReplacementVariantExpansionCatalog.Variant::index)
                            .toList(),
                    species.entityTypePath());
            assertEquals(5, new HashSet<>(species.variants().stream()
                    .map(ReplacementVariantExpansionCatalog.Variant::label)
                    .toList()).size(), species.entityTypePath());
            assertTrue(species.variants().stream().allMatch(variant -> !variant.description().isBlank()));
        }

        assertEquals(HISTORICAL_PRIMARY_IDS,
                ReplacementVariantExpansionCatalog.species().stream()
                        .map(species -> species.variants().getFirst().id())
                        .toList());
    }

    @Test
    void phaseSelectionKeepsTheEstablishedFiveWayDistribution() {
        for (ReplacementVariantExpansionCatalog.Species species
                : ReplacementVariantExpansionCatalog.species()) {
            assertEquals(Map.of(1, 100L), distribution(species, 1), species.entityTypePath());
            assertEquals(Map.of(1, 32L, 2, 68L), distribution(species, 2), species.entityTypePath());
            assertEquals(Map.of(1, 14L, 2, 28L, 3, 58L), distribution(species, 3), species.entityTypePath());
            assertEquals(Map.of(2, 14L, 3, 22L, 4, 32L, 5, 32L),
                    distribution(species, 4), species.entityTypePath());
        }
    }

    @Test
    void everyProfileHasOneForcedDevMenuRouteAndInspectableMetadata() {
        long replacementActions = UncannyDevCatalog.entries().stream()
                .filter(entry -> entry.actionKind() == UncannyDevCatalog.ActionKind.SPAWN_UNCANNY_FORCED)
                .filter(entry -> entry.actionArg().endsWith("|replacement"))
                .count();
        assertEquals(85L, replacementActions);

        for (ReplacementVariantExpansionCatalog.Variant variant
                : ReplacementVariantExpansionCatalog.variants()) {
            String entryId = variant.entityTypePath().equals("uncanny_wither_skeleton")
                    && variant.index() == 1
                    ? "entity_wither_skeleton_archer"
                    : variant.entityTypePath().equals("uncanny_wither_skeleton")
                            && variant.index() == 2
                    ? "entity_wither_skeleton_melee"
                    : "entity_" + variant.vanillaTypeKey() + "_v" + variant.index();
            UncannyDevCatalog.Entry entry = UncannyDevCatalog.byId(entryId);
            assertNotNull(entry, variant.id());
            assertEquals(
                    variant.entityTypePath() + "|UncannyReplacementVariant|"
                            + variant.index() + "|replacement",
                    entry.actionArg(),
                    variant.id());
            UncannyDevMetadataCatalog.Info info = UncannyDevMetadataCatalog.describe(entry);
            assertEquals(UncannyDevMetadataCatalog.ImplementationStatus.WORKING_BUILD,
                    info.implementation(), variant.id());
            assertTrue(info.description().contains(variant.label()), variant.id());
        }
    }

    @Test
    void expansionNeverMakesADangerousReplacementSilentOrMutatesVanillaRewards() throws IOException {
        String catalog = read(Path.of("src", "main", "java", "com", "eotv", "echoofthevoid",
                "entity", "variant", "ReplacementVariantExpansionCatalog.java"));
        String system = read(Path.of("src", "main", "java", "com", "eotv", "echoofthevoid",
                "entity", "variant", "ReplacementVariantExpansionSystem.java"));
        String runtime = read(Path.of("src", "main", "java", "com", "eotv", "echoofthevoid",
                "event", "passive", "VanillaVariantBehaviorRuntime.java"));

        assertFalse(catalog.contains("PITCH_BLACK"));
        assertFalse(system.contains("setSilent(true)"));
        for (String forbidden : List.of(
                "setHealth(", "setAttributeBaseValue", "setDropChance(", "setItemSlot(",
                "destroyBlock(", "setBlock(", "kill(", "discard(")) {
            assertFalse(system.contains(forbidden), forbidden);
            assertFalse(runtime.contains(forbidden), forbidden);
        }
        assertTrue(system.contains("setLanternEaterMode(variant.index() == 2)"));
        assertTrue(system.contains("setArcherVariantForExpansion((variant.index() & 1) == 1)"));
    }

    @Test
    void runtimeIsConnectedToNaturalJoinTickDiagnosticsAndTheOldLanternRoute() throws IOException {
        String mod = read(Path.of("src", "main", "java", "com", "eotv", "echoofthevoid",
                "EchoOfTheVoid.java"));
        String paranoia = read(Path.of("src", "main", "java", "com", "eotv", "echoofthevoid",
                "event", "UncannyParanoiaEventSystem.java"));
        String diagnostics = read(Path.of("src", "main", "java", "com", "eotv", "echoofthevoid",
                "diagnostics", "UncannyDiagnostics.java"));

        assertTrue(mod.contains("ReplacementVariantExpansionSystem::onEntityJoinLevel"));
        assertTrue(mod.contains("ReplacementVariantExpansionSystem::onEntityTick"));
        assertTrue(paranoia.contains("ReplacementVariantExpansionSystem.applyForcedVariant(phantom, 2"));
        assertTrue(diagnostics.contains("ReplacementVariantExpansionSystem.variantId(entity)"));
    }

    private static Map<Integer, Long> distribution(
            ReplacementVariantExpansionCatalog.Species species,
            int phase) {
        return java.util.stream.LongStream.range(0L, 100L)
                .mapToObj(ticket -> ReplacementVariantExpansionCatalog.selectVariant(
                        species.entityTypePath(), phase, ticket).index())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
