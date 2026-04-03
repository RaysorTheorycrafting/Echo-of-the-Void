package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.entity.custom.UncannyZombieEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class UncannyZombieRenderer extends HumanoidMobRenderer<UncannyZombieEntity, HumanoidModel<UncannyZombieEntity>> {
    private static final ResourceLocation ZOMBIE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie/zombie.png");

    public UncannyZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new UncannyZombieModel(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(UncannyZombieEntity entity) {
        return ZOMBIE_TEXTURE;
    }

    @Override
    protected void scale(UncannyZombieEntity entity, PoseStack poseStack, float partialTickTime) {
        if (entity.isTallGlitchTestVariant()) {
            poseStack.scale(1.0F, UncannyZombieEntity.tallGlitchYScale(), 1.0F);
        }
        super.scale(entity, poseStack, partialTickTime);
    }
}
