package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.scores.Team;

public class UncannyWolfRenderer extends WolfRenderer {
    private static final String TAG_PET_REFUSAL_BLACK = "eotv_pet_refusal_black";
    private static final String TEAM_PET_REFUSAL_BLACK = "eotv_pet_refusal_black";
    private static final ResourceLocation BLACK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "textures/entity/color_black.png");

    public UncannyWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Wolf entity) {
        return shouldRenderBlack(entity) ? BLACK_TEXTURE : super.getTextureLocation(entity);
    }

    @Override
    public void render(
            Wolf entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight) {
        boolean renderBlack = shouldRenderBlack(entity);
        if (UncannyConfig.DEBUG_LOGS.get() && (entity.tickCount % 40) == 0) {
            EchoOfTheVoid.LOGGER.info(
                    "[UncannyDebug/PetRefusalClient] wolf id={} team={} tags={} renderBlack={}",
                    entity.getId(),
                    entity.getTeam() != null ? entity.getTeam().getName() : "none",
                    entity.getTags(),
                    renderBlack);
        }
        if (renderBlack) {
            RenderSystem.setShaderColor(0.02F, 0.02F, 0.02F, 1.0F);
        }

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);

        if (renderBlack) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private boolean shouldRenderBlack(Wolf entity) {
        Team team = entity.getTeam();
        return entity.getTags().contains(TAG_PET_REFUSAL_BLACK)
                || (team != null && TEAM_PET_REFUSAL_BLACK.equals(team.getName()));
    }
}
