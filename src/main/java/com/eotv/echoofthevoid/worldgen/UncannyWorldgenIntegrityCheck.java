package com.eotv.echoofthevoid.worldgen;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.world.UncannyDimensions;
import com.mojang.serialization.Lifecycle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** One-shot release guard that detects data-driven structures excluded from the active Overworld generator. */
public final class UncannyWorldgenIntegrityCheck {
    private UncannyWorldgenIntegrityCheck() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
        validateDimensionLifecycle(event);
        ServerLevel overworld = event.getServer().overworld();
        if (overworld.getChunkSource().getGenerator() instanceof FlatLevelSource) {
            EchoOfTheVoid.LOGGER.debug(
                    "Skipping natural structure-placement audit for an explicitly configured flat world.");
            return;
        }
        Registry<Structure> structures = overworld.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var generatorState = overworld.getChunkSource().getGeneratorState();
        List<ResourceLocation> missingPlacements = new ArrayList<>();
        int registered = 0;

        for (ResourceKey<Structure> key : structures.registryKeySet()) {
            ResourceLocation id = key.location();
            if (!EchoOfTheVoid.MODID.equals(id.getNamespace())) {
                continue;
            }
            registered++;
            var holder = structures.getHolder(key).orElseThrow();
            if (generatorState.getPlacementsForStructure(holder).isEmpty()) {
                missingPlacements.add(id);
            }
        }

        if (missingPlacements.isEmpty()) {
            EchoOfTheVoid.LOGGER.info(
                    "Validated {} Echo of the Void structure placements in the active Overworld generator.",
                    registered);
        } else {
            EchoOfTheVoid.LOGGER.error(
                    "Echo of the Void structures are registered but unavailable to natural world generation: {}",
                    missingPlacements);
        }
    }

    private static void validateDimensionLifecycle(ServerStartedEvent event) {
        boolean elsewhereLoaded = event.getServer().getLevel(UncannyDimensions.ELSEWHERE) != null;
        Lifecycle lifecycle = event.getServer().getWorldData().worldGenSettingsLifecycle();
        if (!elsewhereLoaded) {
            if (event.getServer() instanceof GameTestServer) {
                // Vanilla's GameTestServer replaces the LEVEL_STEM registry with its dedicated
                // flat test preset. Elsewhere is deliberately absent there; normal dedicated and
                // integrated servers still treat the same absence as a release-blocking error.
                EchoOfTheVoid.LOGGER.debug(
                        "Elsewhere is intentionally unavailable in Vanilla's isolated GameTestServer preset.");
            } else {
                EchoOfTheVoid.LOGGER.error(
                        "The Elsewhere dimension is absent from the active server world; Devourer? arenas are unavailable.");
            }
        } else if (lifecycle == Lifecycle.stable()) {
            EchoOfTheVoid.LOGGER.info(
                    "Validated the Elsewhere dimension with a stable world-generation lifecycle.");
        } else {
            EchoOfTheVoid.LOGGER.warn(
                    "The active world-generation lifecycle remains experimental; check external datapacks or dimensions.");
        }
    }
}
