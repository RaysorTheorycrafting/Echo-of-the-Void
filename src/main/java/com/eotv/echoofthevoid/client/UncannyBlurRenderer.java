package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.custom.UncannyBlurEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

/**
 * Draws Blur? only at the edge of the screen, as a smeared, translucent black figure. Its position
 * is projected with the world projection matrix actually in use this frame, so the edge band is the
 * same on screen at any field of view, aspect ratio, sprint FOV or view bobbing.
 */
public class UncannyBlurRenderer extends EntityRenderer<UncannyBlurEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "textures/entity/color_black.png");
    private static final float MAX_ALPHA = 0.62F;
    private static final double BOX_MARGIN = 0.15D;
    private static final float MIN_CLIP_W = 0.05F;
    private static final float[][] SMEAR = {{0.0F, 1.0F}, {0.07F, 0.45F}, {-0.07F, 0.45F}};
    private final PlayerModel<UncannyBlurEntity> model;

    public UncannyBlurRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        this.shadowRadius = 0.0F;
    }

    /**
     * Screen-edge opacity of this entity for the current frame, from the screen extent of its whole
     * box (widened for the smear and swinging arms), so no part of it may reach towards the centre.
     */
    static float edgeAlpha(UncannyBlurEntity entity, float partialTicks) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 base = entity.getPosition(partialTicks).subtract(camera.getPosition());
        double half = entity.getBbWidth() * 0.5D + BOX_MARGIN;
        double height = entity.getBbHeight() + BOX_MARGIN;
        Matrix4f view = new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()));
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        Vector4f point = new Vector4f();
        for (int corner = 0; corner < 8; corner++) {
            point.set((float) (base.x + ((corner & 1) == 0 ? -half : half)),
                    (float) (base.y + ((corner & 2) == 0 ? 0.0D : height)),
                    (float) (base.z + ((corner & 4) == 0 ? -half : half)), 1.0F);
            view.transform(point);
            projection.transform(point);
            if (point.w <= MIN_CLIP_W) {
                // Part of it is level with or behind the eye: it is too close to stay at the edge.
                return 0.0F;
            }
            float x = point.x / point.w;
            float y = point.y / point.w;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        return BlurEdgeRules.alphaForExtent(minX, maxX, minY, maxY);
    }

    @Override
    public void render(UncannyBlurEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight) {
        float alpha = edgeAlpha(entity, partialTicks) * MAX_ALPHA;
        if (alpha <= 0.01F) {
            return;
        }
        float bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        float headYaw = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot) - bodyYaw;
        float pitch = entity.getViewXRot(partialTicks);
        float walkPosition = entity.walkAnimation.position(partialTicks);
        float walkSpeed = Math.min(1.0F, entity.walkAnimation.speed(partialTicks));
        this.model.attackTime = 0.0F;
        this.model.crouching = false;
        this.model.prepareMobModel(entity, walkPosition, walkSpeed, partialTicks);
        this.model.setupAnim(entity, walkPosition, walkSpeed, entity.tickCount + partialTicks, headYaw, pitch);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        for (float[] smear : SMEAR) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
            poseStack.translate(smear[0], 0.0F, 0.0F);
            poseStack.scale(-0.9375F, -0.9375F, 0.9375F);
            poseStack.translate(0.0F, -1.501F, 0.0F);
            this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                    FastColor.ARGB32.colorFromFloat(alpha * smear[1], 1.0F, 1.0F, 1.0F));
            poseStack.popPose();
        }
    }

    @Override
    protected boolean shouldShowName(UncannyBlurEntity entity) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(UncannyBlurEntity entity) {
        return TEXTURE;
    }
}
