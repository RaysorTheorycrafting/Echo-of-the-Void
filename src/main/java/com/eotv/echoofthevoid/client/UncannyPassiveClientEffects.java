package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.entity.custom.UncannyStructureVillagerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyUsherEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

public final class UncannyPassiveClientEffects {
    private static final String TAG_FOX_BLACK = "eotv_passive_fox_v1";
    private static final String TAG_LLAMA_BLACK = "eotv_passive_llama_v3";
    private static final String TAG_LLAMA_BLACK_MARKER = "eotv_black_llama";
    private static final String TEAM_PET_REFUSAL_BLACK = "eotv_pet_refusal_black";
    private static final String TAG_COD_INVERTED = "eotv_passive_cod_v1";
    private static final String TAG_SALMON_INVERTED = "eotv_passive_salmon_v1";

    private static final Set<Integer> DARK_RENDER_ENTITIES = new HashSet<>();
    private static final Set<Integer> INVERTED_RENDER_ENTITIES = new HashSet<>();
    private static final Set<Integer> STRETCHED_RENDER_ENTITIES = new HashSet<>();
    private static final Map<Integer, Long> PET_REFUSAL_VISUAL_UNTIL = new HashMap<>();

    private UncannyPassiveClientEffects() {
    }

    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        int id = entity.getId();

        if (shouldRenderPitchBlack(entity)) {
            RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
            DARK_RENDER_ENTITIES.add(id);
        }

        if (shouldRenderUpsideDown(entity)) {
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(0.0F, entity.getBbHeight() + 0.1F, 0.0F);
            event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(180.0F));
            INVERTED_RENDER_ENTITIES.add(id);
        }

        if (entity instanceof UncannyStructureVillagerEntity structureVillager) {
            event.getPoseStack().pushPose();
            event.getPoseStack().scale(
                    structureVillager.visualScaleX(),
                    structureVillager.visualScaleY(),
                    structureVillager.visualScaleZ());
            STRETCHED_RENDER_ENTITIES.add(id);
        } else if (entity instanceof UncannyUsherEntity) {
            event.getPoseStack().pushPose();
            event.getPoseStack().scale(1.0F, 2.0F, 1.0F);
            STRETCHED_RENDER_ENTITIES.add(id);
        }
    }

    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        int id = event.getEntity().getId();
        if (STRETCHED_RENDER_ENTITIES.remove(id)) {
            event.getPoseStack().popPose();
        }
        if (INVERTED_RENDER_ENTITIES.remove(id)) {
            event.getPoseStack().popPose();
        }
        if (DARK_RENDER_ENTITIES.remove(id)) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public static void onRenderNameTag(RenderNameTagEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Cod || entity instanceof Salmon)) {
            return;
        }

        if (entity.getTags().contains(TAG_COD_INVERTED)
                || entity.getTags().contains(TAG_SALMON_INVERTED)
                || hasDinnerboneName(entity)) {
            event.setCanRender(TriState.FALSE);
        }
    }

    public static void applyPetRefusalVisual(int entityId, boolean active, int durationTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            return;
        }
        if (!active) {
            if (entityId < 0) {
                PET_REFUSAL_VISUAL_UNTIL.clear();
                if (UncannyConfig.DEBUG_LOGS.get()) {
                    EchoOfTheVoid.LOGGER.info("[UncannyDebug/PetRefusalClient] clear-all");
                }
                return;
            }
            PET_REFUSAL_VISUAL_UNTIL.remove(entityId);
            if (UncannyConfig.DEBUG_LOGS.get()) {
                EchoOfTheVoid.LOGGER.info("[UncannyDebug/PetRefusalClient] remove entityId={}", entityId);
            }
            return;
        }
        if (entityId <= 0) {
            return;
        }
        long endTick = minecraft.level.getGameTime() + Math.max(1, durationTicks);
        PET_REFUSAL_VISUAL_UNTIL.put(entityId, endTick);
        if (UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info("[UncannyDebug/PetRefusalClient] apply entityId={} duration={} endTick={}", entityId, durationTicks, endTick);
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            PET_REFUSAL_VISUAL_UNTIL.clear();
            return;
        }
        long now = minecraft.level.getGameTime();
        Iterator<Map.Entry<Integer, Long>> it = PET_REFUSAL_VISUAL_UNTIL.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Long> entry = it.next();
            if (entry.getValue() <= now || minecraft.level.getEntity(entry.getKey()) == null) {
                it.remove();
            }
        }
    }

    private static boolean hasDinnerboneName(Entity entity) {
        Component name = entity.getCustomName();
        return name != null && "Dinnerbone".equals(name.getString());
    }

    private static boolean shouldRenderPitchBlack(LivingEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : Long.MIN_VALUE;
        return PET_REFUSAL_VISUAL_UNTIL.getOrDefault(entity.getId(), Long.MIN_VALUE) > now
                || entity.getTags().contains("eotv_pet_refusal_black")
                || isOnTeam(entity, TEAM_PET_REFUSAL_BLACK)
                || (entity instanceof Fox && entity.getTags().contains(TAG_FOX_BLACK))
                || (entity instanceof Llama && (entity.getTags().contains(TAG_LLAMA_BLACK) || entity.getTags().contains(TAG_LLAMA_BLACK_MARKER)));
    }

    private static boolean isOnTeam(Entity entity, String teamName) {
        PlayerTeam team = entity.getTeam();
        return team != null && teamName.equals(team.getName());
    }

    private static boolean shouldRenderUpsideDown(LivingEntity entity) {
        return (entity instanceof Cod && entity.getTags().contains(TAG_COD_INVERTED))
                || (entity instanceof Salmon && entity.getTags().contains(TAG_SALMON_INVERTED));
    }

}
