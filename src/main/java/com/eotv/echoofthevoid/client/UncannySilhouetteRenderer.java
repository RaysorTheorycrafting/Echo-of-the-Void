package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.event.UncannyParanoiaEventSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

public class UncannySilhouetteRenderer<T extends Mob> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    private static final ResourceLocation SILHOUETTE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "textures/entity/color_black.png");

    public UncannySilhouetteRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return SILHOUETTE_TEXTURE;
    }

    @Override
    public void render(
            T entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight) {
        poseStack.pushPose();
        applyGrandPauseJitter(entity, partialTicks, poseStack);
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    private static void applyGrandPauseJitter(Mob entity, float partialTicks, PoseStack poseStack) {
        if (entity == null || !entity.getTags().contains(UncannyParanoiaEventSystem.getGrandPauseSpecialTag())) {
            return;
        }

        float time = entity.tickCount + partialTicks;
        float offsetX = Mth.sin(time * 0.95F) * 0.018F;
        float offsetZ = Mth.cos(time * 1.13F) * 0.018F;
        poseStack.translate(offsetX, 0.0D, offsetZ);
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 1.70F) * 1.4F));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.cos(time * 1.40F) * 1.0F));
    }
}
