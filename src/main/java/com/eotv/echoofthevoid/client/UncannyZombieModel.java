package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.entity.custom.UncannyZombieEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

public class UncannyZombieModel extends HumanoidModel<UncannyZombieEntity> {
    public UncannyZombieModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(
            UncannyZombieEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        UncannyZombieEntity.ZombieVariant variant = entity.getZombieVariant();

        float adjustedLimbSwing = limbSwing;
        float adjustedLimbSwingAmount = limbSwingAmount;

        if (variant == UncannyZombieEntity.ZombieVariant.DESYNC) {
            adjustedLimbSwing *= 5.0F;
            adjustedLimbSwingAmount = Math.min(1.35F, limbSwingAmount * 1.55F);
        } else if (variant == UncannyZombieEntity.ZombieVariant.BAIT && entity.isBaitTriggered()) {
            adjustedLimbSwing = 0.0F;
            adjustedLimbSwingAmount = 0.0F;
        }

        super.setupAnim(entity, adjustedLimbSwing, adjustedLimbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        if (variant == UncannyZombieEntity.ZombieVariant.BROKEN_NECK) {
            this.head.yRot += (float) (Math.PI / 2.0D);
            this.hat.yRot = this.head.yRot;
        }

        if (variant == UncannyZombieEntity.ZombieVariant.BAIT && entity.isBaitTriggered()) {
            this.rightArm.xRot = 0.08F;
            this.leftArm.xRot = 0.08F;
            this.rightArm.yRot = 0.0F;
            this.leftArm.yRot = 0.0F;
            this.rightArm.zRot = 0.0F;
            this.leftArm.zRot = 0.0F;
            this.rightLeg.xRot = 0.0F;
            this.leftLeg.xRot = 0.0F;
        } else {
            // Keep arms neutral to avoid rigid forward zombie pose.
            this.rightArm.zRot = 0.0F;
            this.leftArm.zRot = 0.0F;
        }

        if (variant == UncannyZombieEntity.ZombieVariant.RENDER_GLITCH) {
            float jitter = (float) Math.sin(ageInTicks * 2.7F) * 0.08F;
            this.head.xRot += jitter * 0.5F;
            this.rightArm.zRot += jitter;
            this.leftArm.zRot -= jitter;
        }
    }
}
