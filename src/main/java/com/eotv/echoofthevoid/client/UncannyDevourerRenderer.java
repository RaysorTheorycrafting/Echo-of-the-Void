package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.custom.UncannyDevourerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * The only Special with a dedicated model (D-84): an all-black body plus a self-lit chest portal and
 * face rift, the one part of the creature that is not black. The portal shows Elsewhere's grey fog
 * curling over a violet-black void, as two additive layers drifting in different directions.
 */
public final class UncannyDevourerRenderer extends MobRenderer<UncannyDevourerEntity, UncannyDevourerModel> {
    private static final ResourceLocation BLACK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EchoOfTheVoid.MODID, "textures/entity/color_black.png");
    private static final ResourceLocation PORTAL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EchoOfTheVoid.MODID, "textures/entity/devourer_portal.png");

    public UncannyDevourerRenderer(EntityRendererProvider.Context context) {
        super(context, new UncannyDevourerModel(context.bakeLayer(UncannyDevourerModel.LAYER)), 0.55F);
        addLayer(new PortalLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(UncannyDevourerEntity entity) {
        return BLACK_TEXTURE;
    }

    @Override
    public void render(
            UncannyDevourerEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight) {
        // Keep the portal surfaces out of the black pass (their black backing still draws); the
        // layer draws them additively over that backing.
        getModel().portal().skipDraw = true;
        getModel().faceRift().skipDraw = true;
        try {
            super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        } finally {
            getModel().portal().skipDraw = false;
            getModel().faceRift().skipDraw = false;
        }
    }

    private static final class PortalLayer extends RenderLayer<UncannyDevourerEntity, UncannyDevourerModel> {
        private PortalLayer(UncannyDevourerRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(
                PoseStack poseStack,
                MultiBufferSource bufferSource,
                int packedLight,
                UncannyDevourerEntity entity,
                float limbSwing,
                float limbSwingAmount,
                float partialTick,
                float ageInTicks,
                float netHeadYaw,
                float headPitch) {
            if (entity.isInvisible()) {
                return;
            }
            UncannyDevourerModel model = getParentModel();
            ModelPart root = model.root();
            // The portal flares as its victim comes closer, following the synchronized mouth value.
            float hunger = Mth.clamp((entity.smoothedMouthOpen(partialTick) - 0.25F) / 0.75F, 0.0F, 1.0F);
            int deep = grey(0.62F + 0.38F * hunger);
            int drift = grey(0.30F + 0.25F * hunger);
            // Walk the full hierarchy so the portal inherits body and head motion, but only draw
            // the two portal surfaces.
            root.getAllParts().forEach(part -> part.skipDraw = true);
            model.portal().skipDraw = false;
            model.faceRift().skipDraw = false;
            try {
                root.render(poseStack, bufferSource.getBuffer(RenderType.energySwirl(PORTAL_TEXTURE,
                                (ageInTicks * 0.0021F) % 1.0F, (ageInTicks * 0.0047F) % 1.0F)),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, deep);
                root.render(poseStack, bufferSource.getBuffer(RenderType.energySwirl(PORTAL_TEXTURE,
                                0.37F - (ageInTicks * 0.0063F) % 1.0F, 0.61F + (ageInTicks * 0.0029F) % 1.0F)),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, drift);
            } finally {
                root.getAllParts().forEach(part -> part.skipDraw = false);
            }
        }

        private static int grey(float intensity) {
            int c = Mth.clamp((int) (intensity * 255.0F), 0, 255);
            return FastColor.ARGB32.color(255, c, c, c);
        }
    }
}
