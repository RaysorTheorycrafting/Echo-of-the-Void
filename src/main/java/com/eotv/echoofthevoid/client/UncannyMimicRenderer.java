package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.entity.custom.UncannyDoubleDormantEntity;
import com.eotv.echoofthevoid.event.UncannyParanoiaEventSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class UncannyMimicRenderer extends HumanoidMobRenderer<UncannyDoubleDormantEntity, PlayerModel<UncannyDoubleDormantEntity>> {
    public UncannyMimicRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(UncannyDoubleDormantEntity entity) {
        UUID copied = entity.getCopiedTargetUuid().orElse(null);
        if (copied != null && Minecraft.getInstance().level != null) {
            Player player = Minecraft.getInstance().level.getPlayerByUUID(copied);
            if (player instanceof AbstractClientPlayer clientPlayer) {
                return clientPlayer.getSkin().texture();
            }
            return DefaultPlayerSkin.get(copied).texture();
        }
        return DefaultPlayerSkin.getDefaultTexture();
    }

    @Override
    public void render(
            UncannyDoubleDormantEntity entity,
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

    private static void applyGrandPauseJitter(UncannyDoubleDormantEntity entity, float partialTicks, PoseStack poseStack) {
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
