package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.entity.custom.UncannySpiderEntity;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class UncannySpiderRenderer extends MobRenderer<UncannySpiderEntity, SpiderModel<UncannySpiderEntity>> {
    private static final ResourceLocation SPIDER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/spider/spider.png");
    private static final ResourceLocation SHADOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("echoofthevoid", "textures/entity/color_black.png");

    public UncannySpiderRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)), 0.8F);
    }

    @Override
    public ResourceLocation getTextureLocation(UncannySpiderEntity entity) {
        if (entity.getSpiderVariant() == UncannySpiderEntity.SpiderVariant.CREEPING_SHADOW) {
            return SHADOW_TEXTURE;
        }
        return SPIDER_TEXTURE;
    }
}
