package com.eotv.echoofthevoid.event.paranoia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eotv.echoofthevoid.dev.UncannyDevCatalog;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DebugBoundsAndUncannyBlockSurfaceTest {
    private static final Path CLIENT = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "client", "UncannyDebugBoundsClientEffects.java");
    private static final Path NETWORK = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "network", "UncannyNetwork.java");
    private static final Path SERVER = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "event", "UncannyDebugBoundsEventSystem.java");
    private static final Path CONTROLLER = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "event", "UncannyEventController.java");
    private static final Path BLOCKS = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "block", "UncannyBlockRegistry.java");

    @Test
    void debugBoundsIsCataloguedButNeverAddedToAWeightedClock() {
        ParanoiaEventDescriptor descriptor = ParanoiaEventCatalog.require(ParanoiaEventIds.DEBUG_BOUNDS);
        assertEquals(2, descriptor.minimumPhase());
        assertEquals(ParanoiaEventSeverity.HIGH, descriptor.severity());
        assertEquals(0, descriptor.primaryWeight());
        assertEquals(0, descriptor.ambientWeight());
        assertEquals(0, descriptor.specialWeight());
        assertNotNull(UncannyDevCatalog.byId("event_debug_bounds"));
    }

    @Test
    void debugBoundsUsesPrivateRenderingAndBothNetworkDirections() throws IOException {
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);
        String network = Files.readString(NETWORK, StandardCharsets.UTF_8);
        String server = Files.readString(SERVER, StandardCharsets.UTF_8);
        String controller = Files.readString(CONTROLLER, StandardCharsets.UTF_8);
        assertTrue(client.contains("PRESENCE_COUNT"));
        assertTrue(client.contains("LevelRenderer.renderLineBox"));
        assertTrue(client.contains("shouldRenderHitBoxes"));
        assertTrue(client.contains("SoundInstance.Attenuation.NONE"));
        assertTrue(client.contains("SoundInstance.Attenuation.LINEAR"));
        assertTrue(client.contains("initialPosition"));
        assertTrue(client.contains("presence.advanceToward(minecraft.level, player)"));
        assertTrue(client.contains("retargetInterval"));
        assertTrue(client.contains("findGroundY"));
        assertFalse(client.contains("addFreshEntity"));
        assertFalse(client.contains("EntityType."));
        assertTrue(network.contains("registrar.playToClient(\n                UncannyDebugBoundsPayload.TYPE"));
        assertTrue(network.contains("registrar.playToServer(\n                UncannyDebugHitboxStatePayload.TYPE"));
        assertTrue(server.contains("ACTIVE_UNTIL_TICK.entrySet().removeIf"));
        assertTrue(controller.contains("UncannyDebugBoundsEventSystem.onPlayerChangedDimension(player)"));
    }

    @Test
    void uncannyBlockHasExactRecipeResistanceAndSilentSurface() throws IOException {
        String blocks = Files.readString(BLOCKS, StandardCharsets.UTF_8);
        String recipe = Files.readString(Path.of(
                "src", "main", "resources", "data", "echoofthevoid", "recipe", "uncanny_block.json"));
        String loot = Files.readString(Path.of(
                "src", "main", "resources", "data", "echoofthevoid", "loot_table", "blocks", "uncanny_block.json"));
        String pickaxe = Files.readString(Path.of(
                "src", "main", "resources", "data", "minecraft", "tags", "block", "mineable", "pickaxe.json"));
        String diamond = Files.readString(Path.of(
                "src", "main", "resources", "data", "minecraft", "tags", "block", "needs_diamond_tool.json"));

        assertTrue(blocks.contains(".strength(50.0F, 1200.0F)"));
        assertTrue(blocks.contains(".requiresCorrectToolForDrops()"));
        assertTrue(blocks.contains(".sound(SoundType.EMPTY)"));
        assertTrue(recipe.contains("\"SS\"") && recipe.contains("echoofthevoid:uncanny_reality_shard"));
        assertEquals(2, count(recipe, "\"SS\""));
        assertTrue(loot.contains("echoofthevoid:uncanny_block"));
        assertTrue(pickaxe.contains("echoofthevoid:uncanny_block"));
        assertTrue(diamond.contains("echoofthevoid:uncanny_block"));
    }

    private static int count(String value, String needle) {
        int count = 0;
        int from = 0;
        while ((from = value.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }
}
