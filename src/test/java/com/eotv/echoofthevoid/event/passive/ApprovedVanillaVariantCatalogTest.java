package com.eotv.echoofthevoid.event.passive;

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
import java.util.Set;
import org.junit.jupiter.api.Test;

class ApprovedVanillaVariantCatalogTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid");
    private static final List<String> ORIGINAL_APPROVED_IDS = List.of(
            "bee_false_hive", "bat_wrong_roost", "rabbit_return_to_cover", "goat_echo_ram",
            "horse_empty_rider", "allay_wrong_recipient", "axolotl_healthy_feign",
            "dolphin_blindside_escort", "frog_empty_tongue", "turtle_false_nest",
            "sniffer_second_dig", "armadillo_empty_threat", "glow_squid_light_lag",
            "breeze_returned_wind", "cave_spider_ceiling_wait", "shulker_empty_aim",
            "guardian_false_beam", "vex_caught_between", "silverfish_wrong_stone",
            "zombified_piglin_procession");

    @Test
    void everyAdditiveSpeciesHasFiveDistinctVariantsAndTheOriginalIdsRemainStable() {
        assertEquals(41, ApprovedVanillaVariantCatalog.species().size());
        assertEquals(205, ApprovedVanillaVariantCatalog.variants().size());
        assertEquals(41, new HashSet<>(ApprovedVanillaVariantCatalog.species().stream()
                .map(ApprovedVanillaVariantCatalog.Species::typeKey)
                .toList()).size());

        for (ApprovedVanillaVariantCatalog.Species species : ApprovedVanillaVariantCatalog.species()) {
            assertEquals(5, species.variants().size(), species.typeKey());
            assertEquals(List.of(1, 2, 3, 4, 5),
                    species.variants().stream().map(ApprovedVanillaVariantCatalog.Variant::index).toList(),
                    species.typeKey());
            assertEquals(5, new HashSet<>(species.variants().stream()
                    .map(ApprovedVanillaVariantCatalog.Variant::behavior)
                    .toList()).size(), species.typeKey());
            assertTrue(species.variants().stream().allMatch(variant -> !variant.description().isBlank()));
        }

        for (String id : ORIGINAL_APPROVED_IDS) {
            ApprovedVanillaVariantCatalog.Variant variant = ApprovedVanillaVariantCatalog.byId(id);
            assertNotNull(variant, id);
            assertEquals(1, variant.index(), id);
            assertEquals(ApprovedVanillaVariantCatalog.BehaviorKind.SPECIALIZED, variant.behaviorKind(), id);
        }
    }

    @Test
    void phaseGateAndRarityChancesAreDeterministicAndMonotone() {
        for (ApprovedVanillaVariantCatalog.Variant variant : ApprovedVanillaVariantCatalog.variants()) {
            double previous = 0.0D;
            for (int phase = 1; phase <= 4; phase++) {
                double chance = ApprovedVanillaVariantCatalog.naturalChance(variant, phase);
                if (phase < variant.minimumPhase()) {
                    assertEquals(0.0D, chance, variant.id());
                } else {
                    assertTrue(chance > 0.0D && chance <= 0.05D, variant.id());
                    assertTrue(chance >= previous, variant.id());
                }
                previous = chance;
            }
        }
        ApprovedVanillaVariantCatalog.Variant rare = ApprovedVanillaVariantCatalog.byId("bee_false_hive");
        ApprovedVanillaVariantCatalog.Variant veryRare = ApprovedVanillaVariantCatalog.byId("allay_wrong_recipient");
        assertTrue(ApprovedVanillaVariantCatalog.naturalChance(rare, 4)
                > ApprovedVanillaVariantCatalog.naturalChance(veryRare, 4));
    }

    @Test
    void everyVariantHasOneInspectableWorkingDevAction() {
        Set<String> arguments = new HashSet<>();
        for (UncannyDevCatalog.Entry entry : UncannyDevCatalog.entries()) {
            if (entry.actionKind() == UncannyDevCatalog.ActionKind.SPAWN_PASSIVE_FORCED
                    && entry.actionArg().startsWith("approved|")) {
                assertTrue(arguments.add(entry.actionArg()), entry.actionArg());
            }
        }
        assertEquals(205, arguments.size());
        for (ApprovedVanillaVariantCatalog.Variant variant : ApprovedVanillaVariantCatalog.variants()) {
            String id = variant.id();
            UncannyDevCatalog.Entry entry = UncannyDevCatalog.byId("entity_vv_" + id);
            assertNotNull(entry, id);
            assertEquals("approved|" + id, entry.actionArg());
            UncannyDevMetadataCatalog.Info info = UncannyDevMetadataCatalog.describe(entry);
            assertEquals(UncannyDevMetadataCatalog.ImplementationStatus.WORKING_BUILD, info.implementation(), id);
            assertEquals(UncannyDevMetadataCatalog.Authority.SHARED, info.authority(), id);
        }
        for (ApprovedVanillaVariantCatalog.Species species : ApprovedVanillaVariantCatalog.species()) {
            UncannyDevCatalog.Entry random =
                    UncannyDevCatalog.byId("entity_vv_" + species.typeKey() + "_spawn");
            assertNotNull(random, species.typeKey());
            assertEquals("approved_random|" + species.typeKey(), random.actionArg());
        }
    }

    @Test
    void weightedSelectionIsDeterministicAndPitchBlackSilenceStaysExceptionalAndHarmless() {
        for (ApprovedVanillaVariantCatalog.Species species : ApprovedVanillaVariantCatalog.species()) {
            for (long ticket = -25; ticket <= 25; ticket++) {
                assertEquals(
                        ApprovedVanillaVariantCatalog.selectVariant(species.typeKey(), 4, ticket),
                        ApprovedVanillaVariantCatalog.selectVariant(species.typeKey(), 4, ticket));
            }
        }

        List<ApprovedVanillaVariantCatalog.Variant> black =
                ApprovedVanillaVariantCatalog.pitchBlackSilentVariants();
        assertEquals(10, black.size());
        assertTrue(black.size() * 20 < ApprovedVanillaVariantCatalog.variants().size());
        assertTrue(black.stream().allMatch(ApprovedVanillaVariantCatalog.Variant::silent));
        assertTrue(black.stream().allMatch(variant -> variant.danger() == 0));
        assertTrue(black.stream().allMatch(variant -> variant.naturalWeight() == 1));
        assertTrue(ApprovedVanillaVariantCatalog.variants().stream()
                .filter(ApprovedVanillaVariantCatalog.Variant::silent)
                .allMatch(variant -> variant.visualStyle()
                        == ApprovedVanillaVariantCatalog.VisualStyle.PITCH_BLACK));
    }

    @Test
    void runtimeKeepsVanillaEntityIdentityAndGameplaySurfaces() throws IOException {
        String runtime = read(JAVA_ROOT.resolve(Path.of(
                "event", "passive", "ApprovedVanillaVariantSystem.java")));
        assertTrue(runtime.contains("type.create(level)"));
        assertTrue(runtime.contains("isNaturalSpawn(mob, event.getSpawnType())"));
        assertTrue(runtime.contains("SPAWN_EGG, COMMAND, DISPENSER, TRIAL_SPAWNER, BUCKET, BREEDING"));
        assertTrue(runtime.contains("mob.getPersistentData().getBoolean(LEGACY_PASSIVE_TAG)"));
        assertFalse(runtime.contains("setCustomName("));
        assertFalse(runtime.contains("setHealth("));
        assertFalse(runtime.contains("setAttributeBaseValue"));
        assertFalse(runtime.contains("setDropChance("));
        assertFalse(runtime.contains("discard("));
        assertFalse(runtime.contains("kill("));
        assertFalse(runtime.contains("setBlock("));
        assertFalse(runtime.contains("setBlockAndUpdate("));

        String genericRuntime = read(JAVA_ROOT.resolve(Path.of(
                "event", "passive", "VanillaVariantBehaviorRuntime.java")));
        assertFalse(genericRuntime.contains("setHealth("));
        assertFalse(genericRuntime.contains("setAttributeBaseValue"));
        assertFalse(genericRuntime.contains("discard("));
        assertFalse(genericRuntime.contains("kill("));
        assertFalse(genericRuntime.contains("setBlock("));
        assertFalse(genericRuntime.contains("destroyBlock("));
    }

    @Test
    void presentationIsSharedAndUsesARequiredDedicatedClientMixin() throws IOException {
        String runtime = read(JAVA_ROOT.resolve(Path.of(
                "event", "passive", "ApprovedVanillaVariantSystem.java")));
        String network = read(JAVA_ROOT.resolve(Path.of("network", "UncannyNetwork.java")));
        String config = read(Path.of("src", "main", "resources", "echoofthevoid.variant.mixins.json"));
        String mods = read(Path.of("src", "main", "templates", "META-INF", "neoforge.mods.toml"));

        assertTrue(runtime.contains("for (ServerPlayer player : level.players())"));
        assertTrue(runtime.contains("PacketDistributor.sendToPlayer(player, payload)"));
        assertTrue(network.contains("UncannyVanillaVariantVisualPayload.TYPE"));
        assertTrue(config.contains("\"required\": true"));
        assertTrue(config.contains("\"package\": \"com.eotv.echoofthevoid.client.variant_mixin\""));
        assertTrue(config.contains("\"UncannyShulkerPeekAccessor\""));
        assertTrue(mods.contains("config=\"${mod_id}.variant.mixins.json\""));
    }

    @Test
    void visualMorphologiesNeverExtendBeyondTheVanillaHitbox() {
        for (ApprovedVanillaVariantCatalog.VisualStyle style
                : ApprovedVanillaVariantCatalog.VisualStyle.values()) {
            ApprovedVanillaVariantCatalog.VisualScale scale =
                    ApprovedVanillaVariantCatalog.visualScale(style);
            assertTrue(scale.x() > 0.7F && scale.x() <= 1.0F, style.name());
            assertTrue(scale.y() > 0.7F && scale.y() <= 1.0F, style.name());
            assertTrue(scale.z() > 0.7F && scale.z() <= 1.0F, style.name());
        }
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
