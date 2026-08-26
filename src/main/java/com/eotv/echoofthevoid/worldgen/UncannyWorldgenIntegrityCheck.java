package com.eotv.echoofthevoid.worldgen;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** One-shot release guard that detects data-driven structures excluded from the active Overworld generator. */
public final class UncannyWorldgenIntegrityCheck {
    private UncannyWorldgenIntegrityCheck() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
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
}
