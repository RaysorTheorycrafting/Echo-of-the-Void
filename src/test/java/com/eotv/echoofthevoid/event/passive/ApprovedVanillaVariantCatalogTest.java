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
    private static final List<String> IDS = List.of(
            "bee_false_hive", "bat_wrong_roost", "rabbit_return_to_cover", "goat_echo_ram",
            "horse_empty_rider", "allay_wrong_recipient", "axolotl_healthy_feign",
            "dolphin_blindside_escort", "frog_empty_tongue", "turtle_false_nest",
            "sniffer_second_dig", "armadillo_empty_threat", "glow_squid_light_lag",
            "breeze_returned_wind", "cave_spider_ceiling_wait", "shulker_empty_aim",
            "guardian_false_beam", "vex_caught_between", "silverfish_wrong_stone",
            "zombified_piglin_procession");

    @Test
    void catalogContainsTheTwentyApprovedIdsExactlyOnce() {
        assertEquals(20, ApprovedVanillaVariantCatalog.variants().size());
        assertEquals(IDS, ApprovedVanillaVariantCatalog.variants().stream()
                .map(ApprovedVanillaVariantCatalog.Variant::id)
                .toList());
        assertEquals(20, new HashSet<>(ApprovedVanillaVariantCatalog.variants().stream()
                .map(ApprovedVanillaVariantCatalog.Variant::typeKey)
                .toList()).size());
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
        assertEquals(20, arguments.size());
        for (String id : IDS) {
            UncannyDevCatalog.Entry entry = UncannyDevCatalog.byId("entity_vv_" + id);
            assertNotNull(entry, id);
            assertEquals("approved|" + id, entry.actionArg());
            UncannyDevMetadataCatalog.Info info = UncannyDevMetadataCatalog.describe(entry);
            assertEquals(UncannyDevMetadataCatalog.ImplementationStatus.WORKING_BUILD, info.implementation(), id);
            assertEquals(UncannyDevMetadataCatalog.Authority.SHARED, info.authority(), id);
        }
    }

    @Test
    void runtimeKeepsVanillaEntityIdentityAndGameplaySurfaces() throws IOException {
        String runtime = read(JAVA_ROOT.resolve(Path.of(
                "event", "passive", "ApprovedVanillaVariantSystem.java")));
        assertTrue(runtime.contains("type.create(level)"));
        assertTrue(runtime.contains("isNaturalSpawn(event.getSpawnType())"));
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

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
