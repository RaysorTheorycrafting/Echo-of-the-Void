package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.entity.custom.UncannyAshwalkerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Standard black silhouette held in the lava: only its head breaks the surface while stalking, the
 * whole body bursts out during a leap, and nothing renders while submerged. The leap leans the
 * body along its flight and throws both arms forward, so it reads as a lunge, not a lift.
 */
public final class UncannyAshwalkerRenderer extends UncannySilhouetteRenderer<UncannyAshwalkerEntity> {
    // The humanoid neck sits 1.5 blocks above the feet: sinking by that much leaves only the head.
    private static final double HEAD_ONLY_DEPTH = -1.5D;
    /** Ticks over which the body rises out of the lava instead of popping up whole. */
    private static final float EMERGE_TICKS = 5.0F;
    private static final float MAX_LEAN_DEGREES = 55.0F;

    public UncannyAshwalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new LungingModel(context));
    }

    @Override
    public void render(
            UncannyAshwalkerEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight) {
        if (entity.motionState() == UncannyAshwalkerEntity.MotionState.SUBMERGED) {
            // The physical entity may route through lava below a bridge; never draw it through one.
            return;
        }
        poseStack.pushPose();
        if (entity.motionState() == UncannyAshwalkerEntity.MotionState.SURFACE) {
            poseStack.translate(0.0D, HEAD_ONLY_DEPTH, 0.0D);
        } else if (entity.motionState() == UncannyAshwalkerEntity.MotionState.LUNGING) {
            float emerge = Mth.clamp(entity.clientMotionAge(partialTicks) / EMERGE_TICKS, 0.0F, 1.0F);
            poseStack.translate(0.0D, HEAD_ONLY_DEPTH * (1.0F - emerge * emerge), 0.0D);
        }
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    protected void setupRotations(
            UncannyAshwalkerEntity entity,
            PoseStack poseStack,
            float bob,
            float yBodyRot,
            float partialTick,
            float scale) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
        if (entity.motionState() != UncannyAshwalkerEntity.MotionState.LUNGING || entity.onGround()) {
            return;
        }
        // Pitch the body along the flight: diving forward while rising, falling onto the prey.
        Vec3 motion = entity.getDeltaMovement();
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float lean = (float) Math.toDegrees(Math.atan2(horizontal, Math.abs(motion.y) + 0.05D));
        lean = Mth.clamp(lean, 15.0F, MAX_LEAN_DEGREES);
        poseStack.translate(0.0F, 1.0F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(lean));
        poseStack.translate(0.0F, -1.0F, 0.0F);
    }

    /** Player-shaped model whose arms reach forward for the whole excursion out of the lava. */
    private static final class LungingModel extends PlayerModel<UncannyAshwalkerEntity> {
        private LungingModel(EntityRendererProvider.Context context) {
            super(context.bakeLayer(ModelLayers.PLAYER), false);
        }

        @Override
        public void setupAnim(
                UncannyAshwalkerEntity entity,
                float limbSwing,
                float limbSwingAmount,
                float ageInTicks,
                float netHeadYaw,
                float headPitch) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            UncannyAshwalkerEntity.MotionState state = entity.motionState();
            if (state == UncannyAshwalkerEntity.MotionState.LUNGING
                    || state == UncannyAshwalkerEntity.MotionState.RETURNING) {
                AnimationUtils.animateZombieArms(leftArm, rightArm, true, attackTime, ageInTicks);
                leftSleeve.copyFrom(leftArm);
                rightSleeve.copyFrom(rightArm);
            }
        }
    }
}
