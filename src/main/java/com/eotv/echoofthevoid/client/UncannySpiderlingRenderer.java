package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.entity.custom.UncannySpiderlingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;

public class UncannySpiderlingRenderer extends SpiderRenderer<UncannySpiderlingEntity> {
    public UncannySpiderlingRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.12F;
    }

    @Override
    protected void scale(UncannySpiderlingEntity spiderling, PoseStack poseStack, float partialTickTime) {
        float scale = 0.18F;
        poseStack.scale(scale, scale, scale);
    }
}
