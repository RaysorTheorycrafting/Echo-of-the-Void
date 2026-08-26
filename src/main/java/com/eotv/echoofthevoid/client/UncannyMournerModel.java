package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.entity.custom.UncannyApprovedSpecialEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Mourner?'s grounded pose: knees folded behind it and both hands resting on the floor. */
public final class UncannyMournerModel extends PlayerModel<UncannyApprovedSpecialEntity> {
    public UncannyMournerModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void setupAnim(
            UncannyApprovedSpecialEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        super.setupAnim(entity, 0.0F, 0.0F, ageInTicks, netHeadYaw, headPitch);

        float slowBreath = Mth.sin(ageInTicks * 0.075F) * 0.018F;
        float unevenSob = Mth.sin(ageInTicks * 0.19F + 0.8F) * 0.026F;

        this.body.y = 11.0F;
        this.body.xRot = 0.52F + slowBreath;
        this.body.yRot = 0.0F;
        this.body.zRot = unevenSob * 0.12F;

        this.head.y = 10.5F;
        this.head.xRot = 0.56F + slowBreath * 0.55F + unevenSob * 0.20F;
        this.head.yRot *= 0.35F;
        this.head.zRot = -unevenSob * 0.18F;

        this.rightArm.x = -5.0F;
        this.rightArm.y = 13.2F;
        this.rightArm.z = 0.0F;
        this.rightArm.xRot = -0.40F - unevenSob * 0.45F;
        this.rightArm.yRot = -0.08F;
        this.rightArm.zRot = -0.06F;

        this.leftArm.x = 5.0F;
        this.leftArm.y = 13.2F;
        this.leftArm.z = 0.0F;
        this.leftArm.xRot = -0.40F - unevenSob * 0.45F;
        this.leftArm.yRot = 0.08F;
        this.leftArm.zRot = 0.06F;

        this.rightLeg.y = 20.5F;
        this.rightLeg.z = 2.8F;
        this.rightLeg.xRot = 1.47F;
        this.rightLeg.yRot = 0.14F;
        this.rightLeg.zRot = 0.04F;

        this.leftLeg.y = 20.5F;
        this.leftLeg.z = 2.8F;
        this.leftLeg.xRot = 1.47F;
        this.leftLeg.yRot = -0.14F;
        this.leftLeg.zRot = -0.04F;

        // PlayerModel copies overlays before this custom pose is applied.
        this.hat.copyFrom(this.head);
        this.jacket.copyFrom(this.body);
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftPants.copyFrom(this.leftLeg);
    }
}
