package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.entity.custom.UncannyPiglinBruteEntity;
import net.minecraft.client.model.PiglinModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class UncannyPiglinBruteRenderer extends HumanoidMobRenderer<UncannyPiglinBruteEntity, PiglinModel<UncannyPiglinBruteEntity>> {
    private static final ResourceLocation PIGLIN_BRUTE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/piglin/piglin_brute.png");

    public UncannyPiglinBruteRenderer(EntityRendererProvider.Context context) {
        super(context, new PiglinModel<>(context.bakeLayer(ModelLayers.PIGLIN_BRUTE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(UncannyPiglinBruteEntity entity) {
        return PIGLIN_BRUTE_TEXTURE;
    }
}
