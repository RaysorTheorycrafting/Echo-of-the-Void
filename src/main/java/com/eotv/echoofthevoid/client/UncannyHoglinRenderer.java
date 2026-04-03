package com.eotv.echoofthevoid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HoglinRenderer;
import net.minecraft.world.entity.monster.hoglin.Hoglin;

public class UncannyHoglinRenderer extends HoglinRenderer {
    public UncannyHoglinRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void setupRotations(Hoglin entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float scale) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick, scale);
        poseStack.translate(0.0F, entity.getBbHeight() + 0.15F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
    }
}
