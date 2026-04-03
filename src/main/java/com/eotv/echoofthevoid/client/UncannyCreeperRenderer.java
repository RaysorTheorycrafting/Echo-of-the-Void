package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.custom.UncannyCreeperEntity;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Creeper;

public class UncannyCreeperRenderer extends CreeperRenderer {
    private static final ResourceLocation VANILLA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/creeper/creeper.png");
    private static final ResourceLocation SILHOUETTE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "textures/entity/color_black.png");

    public UncannyCreeperRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Creeper entity) {
        if (entity instanceof UncannyCreeperEntity uncannyCreeper
                && uncannyCreeper.getCreeperVariant() == UncannyCreeperEntity.CreeperVariant.SILHOUETTE) {
            return SILHOUETTE_TEXTURE;
        }
        return VANILLA_TEXTURE;
    }
}
