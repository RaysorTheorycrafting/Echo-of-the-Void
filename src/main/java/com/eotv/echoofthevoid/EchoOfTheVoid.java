package com.eotv.echoofthevoid;

import com.eotv.echoofthevoid.command.UncannyCommandRegistry;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import com.eotv.echoofthevoid.block.entity.UncannyBlockEntityRegistry;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.event.UncannyDoubleDormantSystem;
import com.eotv.echoofthevoid.event.UncannyEventController;
import com.eotv.echoofthevoid.event.UncannyPassiveVariantSystem;
import com.eotv.echoofthevoid.event.UncannyParanoiaEventSystem;
import com.eotv.echoofthevoid.event.UncannySpawnController;
import com.eotv.echoofthevoid.event.UncannyStructureFeatureSystem;
import com.eotv.echoofthevoid.event.UncannyWeatherSystem;
import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import com.eotv.echoofthevoid.menu.UncannyMenuRegistry;
import com.eotv.echoofthevoid.network.UncannyNetwork;
import com.eotv.echoofthevoid.phase.UncannyPhaseManager;
import com.eotv.echoofthevoid.sound.UncannySoundRefs;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import com.eotv.echoofthevoid.worldgen.UncannyStructureWorldgenRegistry;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(EchoOfTheVoid.MODID)
public class EchoOfTheVoid {
    public static final String MODID = "echoofthevoid";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoOfTheVoid(IEventBus modEventBus, ModContainer modContainer) {
        UncannyBlockRegistry.register(modEventBus);
        UncannyItemRegistry.register(modEventBus);
        UncannyEntityRegistry.register(modEventBus);
        UncannyBlockEntityRegistry.register(modEventBus);
        UncannyMenuRegistry.register(modEventBus);
        UncannySoundRegistry.register(modEventBus);
        UncannyStructureWorldgenRegistry.register(modEventBus);
        modEventBus.addListener(UncannyNetwork::register);

        modContainer.registerConfig(ModConfig.Type.COMMON, UncannyConfig.SPEC);

        NeoForge.EVENT_BUS.addListener(UncannyPhaseManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(UncannyEventController::onPlayerDeath);
        NeoForge.EVENT_BUS.addListener(UncannyEventController::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(UncannyEventController::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(UncannyDoubleDormantSystem::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(UncannyWeatherSystem::onServerTick);
        NeoForge.EVENT_BUS.addListener(UncannyStructureFeatureSystem::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(UncannyParanoiaEventSystem::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(UncannyParanoiaEventSystem::onCanPlayerSleep);
        NeoForge.EVENT_BUS.addListener(UncannyParanoiaEventSystem::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(UncannyParanoiaEventSystem::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(UncannyParanoiaEventSystem::onPlayerEntityInteract);
        NeoForge.EVENT_BUS.addListener(UncannyParanoiaEventSystem::onPlayerEntityInteractSpecific);
        NeoForge.EVENT_BUS.addListener(UncannyParanoiaEventSystem::onLivingUseItemFinish);
        NeoForge.EVENT_BUS.addListener(UncannyParanoiaEventSystem::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(UncannyParanoiaEventSystem::onLivingIncomingDamage);
        NeoForge.EVENT_BUS.addListener(UncannyParanoiaEventSystem::onEntityLeaveLevel);
        NeoForge.EVENT_BUS.addListener(UncannyParanoiaEventSystem::onEntityMount);
        NeoForge.EVENT_BUS.addListener(UncannyParanoiaEventSystem::onEntityTick);
        NeoForge.EVENT_BUS.addListener(UncannySpawnController::onFinalizeSpawn);
        NeoForge.EVENT_BUS.addListener(UncannySpawnController::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(UncannyPassiveVariantSystem::onFinalizeSpawn);
        NeoForge.EVENT_BUS.addListener(UncannyPassiveVariantSystem::onEntityTick);
        NeoForge.EVENT_BUS.addListener(UncannyPassiveVariantSystem::onLivingIncomingDamage);
        NeoForge.EVENT_BUS.addListener(UncannyPassiveVariantSystem::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(UncannyPassiveVariantSystem::onPlayerEntityInteract);
        NeoForge.EVENT_BUS.addListener(UncannyPassiveVariantSystem::onPlayerEntityInteractSpecific);
        NeoForge.EVENT_BUS.addListener(UncannyPassiveVariantSystem::onTradeWithVillager);
        NeoForge.EVENT_BUS.addListener(UncannyPassiveVariantSystem::onAnimalTame);
        NeoForge.EVENT_BUS.addListener(UncannyCommandRegistry::onRegisterCommands);

        UncannySoundRefs.logRegisteredPlaceholders();
        LOGGER.info("Echo Of The Void initialized.");
    }
}

