package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.entity.custom.UncannyStalkerEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** The two retained visual forms of Attacker?; its server hitbox and combat remain unchanged. */
public final class UncannyAttackerModel extends PlayerModel<UncannyStalkerEntity> {
    public UncannyAttackerModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void setupAnim(
            UncannyStalkerEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        switch (entity.getAnimationStyle()) {
            case CRAWL -> setupCrawl(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            case OUTSTRETCHED -> setupOutstretched(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        }
        copyOuterLayers();
    }

    private void setupCrawl(
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        float movement = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float foreStep = Mth.cos(limbSwing * 1.28F) * movement;
        float hindStep = Mth.cos(limbSwing * 1.28F + Mth.PI) * movement;
        float unevenLurch = Mth.sin(ageInTicks * 0.47F) * Mth.sin(ageInTicks * 0.113F);
        float shoulderDrop = Math.abs(Mth.sin(limbSwing * 1.28F)) * movement;

        this.body.y = 13.8F + shoulderDrop * 0.65F;
        this.body.z = -3.2F;
        this.body.xRot = 1.50F + unevenLurch * 0.045F;
        this.body.yRot = unevenLurch * 0.055F;
        this.body.zRot = Mth.sin(ageInTicks * 0.31F) * 0.035F;

        this.head.y = 17.2F + shoulderDrop * 0.30F;
        this.head.z = -5.3F;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD * 0.28F + 0.08F + unevenLurch * 0.075F;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.62F
                + Mth.sin(ageInTicks * 0.67F) * Mth.sin(ageInTicks * 0.071F) * 0.10F;
        this.head.zRot = -this.body.zRot * 0.70F;

        this.rightArm.x = -5.0F;
        this.rightArm.y = 12.4F + shoulderDrop * 0.45F;
        this.rightArm.z = -4.1F;
        this.rightArm.xRot = -0.10F + foreStep * 0.42F + unevenLurch * 0.07F;
        this.rightArm.yRot = -0.10F;
        this.rightArm.zRot = -0.08F + unevenLurch * 0.04F;

        this.leftArm.x = 5.0F;
        this.leftArm.y = 12.4F + shoulderDrop * 0.45F;
        this.leftArm.z = -4.1F;
        this.leftArm.xRot = -0.10F - foreStep * 0.42F - unevenLurch * 0.05F;
        this.leftArm.yRot = 0.10F;
        this.leftArm.zRot = 0.08F - unevenLurch * 0.04F;

        this.rightLeg.x = -3.0F;
        this.rightLeg.y = 12.0F;
        this.rightLeg.z = 7.0F;
        this.rightLeg.xRot = hindStep * 0.32F;
        this.rightLeg.yRot = -0.07F;
        this.rightLeg.zRot = -0.04F;

        this.leftLeg.x = 3.0F;
        this.leftLeg.y = 12.0F;
        this.leftLeg.z = 7.0F;
        this.leftLeg.xRot = -hindStep * 0.32F;
        this.leftLeg.yRot = 0.07F;
        this.leftLeg.zRot = 0.04F;
    }

    private void setupOutstretched(
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        float movement = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float gait = Mth.cos(limbSwing * 0.82F) * movement;
        float irregularPulse = Mth.sin(ageInTicks * 0.53F) * Mth.sin(ageInTicks * 0.097F);
        float armShudder = Mth.sin(ageInTicks * 0.91F) * 0.045F
                + Mth.sin(ageInTicks * 0.37F) * 0.035F;

        this.body.y = 0.8F;
        this.body.z = 0.0F;
        this.body.xRot = 0.18F + Math.abs(gait) * 0.055F;
        this.body.yRot = irregularPulse * 0.055F;
        this.body.zRot = Mth.sin(ageInTicks * 0.43F) * Mth.sin(ageInTicks * 0.079F) * 0.045F;

        this.head.y = 0.5F;
        this.head.z = -0.6F;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD * 0.45F - 0.08F
                + irregularPulse * 0.075F;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.72F
                + Mth.sin(ageInTicks * 0.73F) * Mth.sin(ageInTicks * 0.061F) * 0.115F;
        this.head.zRot = -this.body.zRot * 0.65F;

        this.rightArm.x = -5.0F;
        this.rightArm.y = 2.7F;
        this.rightArm.z = -0.8F;
        this.rightArm.xRot = -1.48F + armShudder + gait * 0.035F;
        this.rightArm.yRot = -0.12F + irregularPulse * 0.035F;
        this.rightArm.zRot = -0.08F;

        this.leftArm.x = 5.0F;
        this.leftArm.y = 2.7F;
        this.leftArm.z = -0.8F;
        this.leftArm.xRot = -1.48F - armShudder - gait * 0.025F;
        this.leftArm.yRot = 0.12F - irregularPulse * 0.035F;
        this.leftArm.zRot = 0.08F;

        this.rightLeg.xRot = gait * 1.02F + Math.max(0.0F, irregularPulse) * 0.11F;
        this.leftLeg.xRot = -gait * 0.88F - Math.min(0.0F, irregularPulse) * 0.08F;
        this.rightLeg.z = 0.9F;
        this.leftLeg.z = 0.9F;
        this.rightLeg.yRot = irregularPulse * 0.025F;
        this.leftLeg.yRot = -irregularPulse * 0.025F;
    }

    private void copyOuterLayers() {
        this.hat.copyFrom(this.head);
        this.jacket.copyFrom(this.body);
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftPants.copyFrom(this.leftLeg);
    }
}
