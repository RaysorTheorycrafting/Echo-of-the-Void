package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WardenRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.warden.Warden;

public class UncannyWardenRenderer extends WardenRenderer {
    private static final String TAG_GRAND_WARDEN = "eotv_grand_warden";
    private static final String GRAND_WARDEN_DISPLAY_NAME = "Warden?";
    private static final ResourceLocation GRAND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "textures/entity/uncanny_grand_warden_special.png");

    public UncannyWardenRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Warden entity) {
        return isGrandWarden(entity) ? GRAND_TEXTURE : super.getTextureLocation(entity);
    }

    @Override
    public void render(
            Warden entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
        int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    protected boolean shouldShowName(Warden entity) {
        if (isGrandWarden(entity)) {
            return false;
        }
        return super.shouldShowName(entity);
    }

    private static boolean isGrandWarden(Warden entity) {
        if (entity.getTags().contains(TAG_GRAND_WARDEN)) {
            return true;
        }
        return entity.hasCustomName()
                && entity.getCustomName() != null
                && GRAND_WARDEN_DISPLAY_NAME.equals(entity.getCustomName().getString());
    }
}
