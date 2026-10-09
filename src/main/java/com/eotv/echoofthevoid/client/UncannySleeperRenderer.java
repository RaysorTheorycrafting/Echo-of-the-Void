package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.custom.UncannySleeperEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * Sleeper? is almost entirely invisible, at any distance: its body is drawn at a fixed, faint
 * opacity that never depends on how close the player is. Only the mouth (teeth, palate, tongue)
 * comes into view while it opens, bites and fights.
 */
public class UncannySleeperRenderer extends EntityRenderer<UncannySleeperEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "textures/entity/sleeper.png");
    /** Fixed: no distance term, by design. */
    static final float BODY_ALPHA = 0.07F;
    private final UncannySleeperModel model;

    public UncannySleeperRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new UncannySleeperModel(context.bakeLayer(UncannySleeperModel.LAYER));
        this.shadowRadius = 0.0F;
    }

    /**
     * The white teeth would show even at the body's faint opacity, which the black body does not: the
     * mouth is not drawn at all until it is on the sleeper, so nothing gives it away while it waits.
     */
    static float mouthAlpha(UncannySleeperEntity.State state, float age) {
        return switch (state) {
            case IDLE, RUN -> 0.0F;
            case MOUNT -> Mth.lerp(Mth.clamp(age / 15.0F, 0.0F, 1.0F), 0.0F, 0.35F);
            case MOUTH -> Mth.lerp(Mth.clamp(age / 10.0F, 0.0F, 1.0F), 0.35F, 0.95F);
            case BITE, FIGHT -> 0.95F;
            case RECOVER -> Mth.lerp(Mth.clamp(age / 30.0F, 0.0F, 1.0F), 0.95F, 0.85F);
        };
    }

    @Override
    public void render(UncannySleeperEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight) {
        float bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        float headYaw = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot) - bodyYaw;
        float ageInTicks = entity.tickCount + partialTicks;
        float walkPosition = entity.walkAnimation.position(partialTicks);
        float walkSpeed = Math.min(1.0F, entity.walkAnimation.speed(partialTicks));
        this.model.attackTime = entity.getAttackAnim(partialTicks);
        this.model.setupAnim(entity, walkPosition, walkSpeed, ageInTicks, headYaw, entity.getViewXRot(partialTicks));

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, Mth.sqrt((entity.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(fall * 90.0F));
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        this.model.selectPass(false);
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                FastColor.ARGB32.colorFromFloat(BODY_ALPHA, 1.0F, 1.0F, 1.0F));
        float mouth = mouthAlpha(entity.state(), entity.stateAge(partialTicks));
        if (mouth > 0.0F) {
            this.model.selectPass(true);
            this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                    FastColor.ARGB32.colorFromFloat(mouth, 1.0F, 1.0F, 1.0F));
            this.model.selectPass(false);
        }
        poseStack.popPose();
    }

    @Override
    protected boolean shouldShowName(UncannySleeperEntity entity) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(UncannySleeperEntity entity) {
        return TEXTURE;
    }
}
