package com.eotv.echoofthevoid;

import com.eotv.echoofthevoid.client.UncannyHoglinRenderer;
import com.eotv.echoofthevoid.client.UncannyMimicRenderer;
import com.eotv.echoofthevoid.client.UncannyPassiveClientEffects;
import com.eotv.echoofthevoid.client.UncannyPiglinBruteRenderer;
import com.eotv.echoofthevoid.client.UncannySpiderRenderer;
import com.eotv.echoofthevoid.client.UncannySpiderlingRenderer;
import com.eotv.echoofthevoid.client.UncannySilhouetteRenderer;
import com.eotv.echoofthevoid.client.UncannyCreeperRenderer;
import com.eotv.echoofthevoid.client.UncannyCatRenderer;
import com.eotv.echoofthevoid.client.UncannyLlamaRenderer;
import com.eotv.echoofthevoid.client.UncannyAltarScreen;
import com.eotv.echoofthevoid.client.UncannyAtmosphereClientEffects;
import com.eotv.echoofthevoid.client.UncannyClientEventEffects;
import com.eotv.echoofthevoid.client.UncannyClientUiEffects;
import com.eotv.echoofthevoid.client.UncannyWardenRenderer;
import com.eotv.echoofthevoid.client.UncannyWatcherRenderer;
import com.eotv.echoofthevoid.client.UncannyWolfRenderer;
import com.eotv.echoofthevoid.client.UncannyZombieRenderer;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.menu.UncannyMenuRegistry;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.DrownedRenderer;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EndermiteRenderer;
import net.minecraft.client.renderer.entity.EvokerRenderer;
import net.minecraft.client.renderer.entity.GhastRenderer;
import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.MagmaCubeRenderer;
import net.minecraft.client.renderer.entity.PillagerRenderer;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.RavagerRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.StrayRenderer;
import net.minecraft.client.renderer.entity.VindicatorRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.WitherSkeletonRenderer;
import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.world.entity.EntityType;

@Mod(value = EchoOfTheVoid.MODID, dist = Dist.CLIENT)
public class EchoOfTheVoidClient {
    public EchoOfTheVoidClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::onRegisterEntityRenderers);
        modEventBus.addListener(this::onRegisterMenuScreens);
        NeoForge.EVENT_BUS.addListener(UncannyPassiveClientEffects::onRenderLivingPre);
        NeoForge.EVENT_BUS.addListener(UncannyPassiveClientEffects::onRenderLivingPost);
        NeoForge.EVENT_BUS.addListener(UncannyPassiveClientEffects::onRenderNameTag);
        NeoForge.EVENT_BUS.addListener(UncannyPassiveClientEffects::onClientTick);
        NeoForge.EVENT_BUS.addListener(UncannyClientEventEffects::onRenderGuiPost);
        NeoForge.EVENT_BUS.addListener(UncannyClientUiEffects::onRenderGuiPost);
        NeoForge.EVENT_BUS.addListener(UncannyClientUiEffects::onRenderGuiLayerPost);
        NeoForge.EVENT_BUS.addListener(UncannyClientUiEffects::onClientTick);
        NeoForge.EVENT_BUS.addListener(UncannyAtmosphereClientEffects::onRenderGuiPost);
        NeoForge.EVENT_BUS.addListener(UncannyAtmosphereClientEffects::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(UncannyAtmosphereClientEffects::onRenderFog);
        NeoForge.EVENT_BUS.addListener(UncannyAtmosphereClientEffects::onComputeFogColor);
    }

    private void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_ZOMBIE.get(), UncannyZombieRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_HUSK.get(), HuskRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_DROWNED.get(), DrownedRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_ZOMBIE_VILLAGER.get(), ZombieVillagerRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_SKELETON.get(), SkeletonRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_STRAY.get(), StrayRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_CREEPER.get(), UncannyCreeperRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_SPIDER.get(), UncannySpiderRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_SPIDERLING.get(), UncannySpiderlingRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_ENDERMAN.get(), EndermanRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_ENDERMITE.get(), EndermiteRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_GHAST.get(), GhastRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_PHANTOM.get(), PhantomRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_DOUBLE_DORMANT.get(), UncannyMimicRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_IRON_GOLEM.get(), IronGolemRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_PILLAGER.get(), PillagerRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_VINDICATOR.get(), VindicatorRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_EVOKER.get(), EvokerRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_RAVAGER.get(), RavagerRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_BLAZE.get(), BlazeRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_WITHER_SKELETON.get(), WitherSkeletonRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_PIGLIN_BRUTE.get(), UncannyPiglinBruteRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_HOGLIN.get(), UncannyHoglinRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_SLIME.get(), SlimeRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_MAGMA_CUBE.get(), MagmaCubeRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_WATCHER.get(), UncannyWatcherRenderer::new);
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_STALKER.get(), context -> new UncannySilhouetteRenderer<>(context));
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_HURLER.get(), context -> new UncannySilhouetteRenderer<>(context));
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_SHADOW.get(), context -> new UncannySilhouetteRenderer<>(context));
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_KNOCKER.get(), context -> new UncannySilhouetteRenderer<>(context));
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_PULSE.get(), context -> new UncannySilhouetteRenderer<>(context));
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_TERROR.get(), context -> new UncannySilhouetteRenderer<>(context));
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_USHER.get(), context -> new UncannySilhouetteRenderer<>(context));
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_KEEPER.get(), context -> new UncannySilhouetteRenderer<>(context));
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_TENANT.get(), context -> new UncannySilhouetteRenderer<>(context));
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_FOLLOWER.get(), context -> new UncannySilhouetteRenderer<>(context));
        event.registerEntityRenderer(UncannyEntityRegistry.UNCANNY_STRUCTURE_VILLAGER.get(), VillagerRenderer::new);
        event.registerEntityRenderer(EntityType.LLAMA, UncannyLlamaRenderer::new);
        event.registerEntityRenderer(EntityType.WOLF, UncannyWolfRenderer::new);
        event.registerEntityRenderer(EntityType.CAT, UncannyCatRenderer::new);
        event.registerEntityRenderer(EntityType.WARDEN, UncannyWardenRenderer::new);
    }

    private void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(UncannyMenuRegistry.UNCANNY_ALTAR.get(), UncannyAltarScreen::new);
    }
}

