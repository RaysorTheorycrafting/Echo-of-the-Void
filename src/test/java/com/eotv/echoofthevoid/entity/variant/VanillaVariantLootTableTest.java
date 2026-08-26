package com.eotv.echoofthevoid.entity.variant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VanillaVariantLootTableTest {
    private static final Path DATA_ROOT = Path.of("src", "main", "resources", "data", "echoofthevoid");
    private static final Path ENTITY_LOOT_ROOT = DATA_ROOT.resolve(Path.of("loot_table", "entities"));
    private static final Map<String, String> VANILLA_EQUIVALENTS = Map.ofEntries(
            Map.entry("uncanny_blaze", "blaze"),
            Map.entry("uncanny_creeper", "creeper"),
            Map.entry("uncanny_drowned", "drowned"),
            Map.entry("uncanny_enderman", "enderman"),
            Map.entry("uncanny_endermite", "endermite"),
            Map.entry("uncanny_evoker", "evoker"),
            Map.entry("uncanny_ghast", "ghast"),
            Map.entry("uncanny_hoglin", "hoglin"),
            Map.entry("uncanny_husk", "husk"),
            Map.entry("uncanny_iron_golem", "iron_golem"),
            Map.entry("uncanny_magma_cube", "magma_cube"),
            Map.entry("uncanny_phantom", "phantom"),
            Map.entry("uncanny_piglin_brute", "piglin_brute"),
            Map.entry("uncanny_pillager", "pillager"),
            Map.entry("uncanny_ravager", "ravager"),
            Map.entry("uncanny_skeleton", "skeleton"),
            Map.entry("uncanny_slime", "slime"),
            Map.entry("uncanny_spider", "spider"),
            Map.entry("uncanny_stray", "stray"),
            Map.entry("uncanny_vindicator", "vindicator"),
            Map.entry("uncanny_wither_skeleton", "wither_skeleton"),
            Map.entry("uncanny_zombie", "zombie"),
            Map.entry("uncanny_zombie_villager", "zombie_villager"));

    @Test
    void everyVanillaDerivedReplacementDelegatesToItsVanillaLootTable() throws IOException {
        assertEquals(23, VANILLA_EQUIVALENTS.size());
        try (var files = Files.list(ENTITY_LOOT_ROOT)) {
            assertEquals(23L, files.filter(path -> path.getFileName().toString().endsWith(".json")).count());
        }

        for (Map.Entry<String, String> mapping : VANILLA_EQUIVALENTS.entrySet()) {
            Path table = ENTITY_LOOT_ROOT.resolve(mapping.getKey() + ".json");
            assertTrue(Files.isRegularFile(table), "Missing loot table for " + mapping.getKey());
            String compact = Files.readString(table, StandardCharsets.UTF_8).replaceAll("\\s+", "");
            assertTrue(compact.contains("\"type\":\"minecraft:entity\""), mapping.getKey());
            assertTrue(compact.contains("\"type\":\"minecraft:loot_table\""), mapping.getKey());
            assertTrue(
                    compact.contains("\"value\":\"minecraft:entities/" + mapping.getValue() + "\""),
                    mapping.getKey());
        }
    }

    @Test
    void noLootTableRemainsUnderThePre121PluralDirectory() throws IOException {
        Path obsoleteRoot = DATA_ROOT.resolve("loot_tables");
        if (!Files.exists(obsoleteRoot)) {
            return;
        }
        try (var files = Files.walk(obsoleteRoot)) {
            assertFalse(files.anyMatch(Files::isRegularFile));
        }
    }

    @Test
    void blockLootTablesAlsoUseTheMinecraft121Directory() {
        assertTrue(Files.isRegularFile(DATA_ROOT.resolve(Path.of("loot_table", "blocks", "uncanny_altar.json"))));
        assertTrue(Files.isRegularFile(DATA_ROOT.resolve(Path.of("loot_table", "blocks", "uncanny_void_door.json"))));
    }
}
