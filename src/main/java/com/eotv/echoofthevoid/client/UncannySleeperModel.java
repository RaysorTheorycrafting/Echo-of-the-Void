package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.custom.UncannySleeperEntity;
import java.util.List;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Sleeper?: a gaunt figure about 2.3 blocks tall with arms down to its knees. The head is split at
 * a hinge behind the ears: the skull tilts back while a jaw drops far too low, showing two rows of
 * teeth, a palate and a tongue. Those mouth parts are drawn in a separate, more opaque pass.
 */
public final class UncannySleeperModel extends HierarchicalModel<UncannySleeperEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "sleeper"), "main");
    private static final float JAW_WIDE = 1.25F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart skull;
    private final ModelPart jaw;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final List<ModelPart> mouthParts;
    private final List<ModelPart> bodyParts;

    public UncannySleeperModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.skull = this.head.getChild("skull");
        this.jaw = this.head.getChild("jaw");
        this.leftArm = this.body.getChild("left_arm");
        this.rightArm = this.body.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.mouthParts = List.of(this.skull.getChild("upper_teeth"), this.skull.getChild("palate"),
                this.jaw.getChild("lower_teeth"), this.jaw.getChild("tongue"));
        this.bodyParts = List.of(this.body, this.head, this.skull, this.jaw, this.leftArm, this.rightArm,
                this.leftLeg, this.rightLeg);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 24).addBox(-4.0F, -14.0F, -2.0F, 8.0F, 14.0F, 4.0F),
                PartPose.offset(0.0F, 9.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create(),
                PartPose.offset(0.0F, -14.0F, 0.0F));
        PartDefinition skull = head.addOrReplaceChild("skull",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -5.0F, -8.0F, 8.0F, 5.0F, 8.0F),
                PartPose.offset(0.0F, -3.0F, 4.0F));
        PartDefinition jaw = head.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 13).addBox(-4.0F, 0.0F, -8.0F, 8.0F, 3.0F, 8.0F),
                PartPose.offset(0.0F, -3.0F, 4.0F));
        skull.addOrReplaceChild("upper_teeth", teeth(0.0F), PartPose.ZERO);
        jaw.addOrReplaceChild("lower_teeth", teeth(-1.6F), PartPose.ZERO);
        skull.addOrReplaceChild("palate",
                CubeListBuilder.create().texOffs(36, 40).addBox(-3.5F, -0.2F, -7.5F, 7.0F, 0.2F, 7.0F), PartPose.ZERO);
        jaw.addOrReplaceChild("tongue",
                CubeListBuilder.create().texOffs(36, 40).addBox(-3.5F, 0.0F, -7.5F, 7.0F, 0.2F, 7.0F), PartPose.ZERO);
        body.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(32, 0).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 22.0F, 3.0F),
                PartPose.offset(5.5F, -12.0F, 0.0F));
        body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(32, 0).mirror().addBox(-1.5F, -1.0F, -1.5F, 3.0F, 22.0F, 3.0F),
                PartPose.offset(-5.5F, -12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(44, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 15.0F, 4.0F),
                PartPose.offset(2.0F, 9.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(44, 0).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 15.0F, 4.0F),
                PartPose.offset(-2.0F, 9.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** A front row and two side rows of thin teeth along the mouth edge, growing up or down from {@code y}. */
    private static CubeListBuilder teeth(float y) {
        CubeListBuilder builder = CubeListBuilder.create().texOffs(40, 56);
        for (int i = 0; i < 6; i++) {
            builder.addBox(-3.4F + i * 1.36F, y, -7.6F, 0.8F, 1.6F, 0.8F);
        }
        for (int i = 0; i < 5; i++) {
            builder.addBox(-3.6F, y, -6.4F + i * 1.3F, 0.8F, 1.6F, 0.8F);
            builder.addBox(2.8F, y, -6.4F + i * 1.3F, 0.8F, 1.6F, 0.8F);
        }
        return builder;
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    /** Mouth-only pass (teeth, palate, tongue) or body-only pass. */
    public void selectPass(boolean mouth) {
        for (ModelPart part : this.bodyParts) {
            part.skipDraw = mouth;
        }
        for (ModelPart part : this.mouthParts) {
            part.skipDraw = !mouth;
        }
    }

    @Override
    public void setupAnim(UncannySleeperEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float age = entity.stateAge(ageInTicks - (int) ageInTicks);
        switch (entity.state()) {
            case IDLE -> idle(ageInTicks);
            case RUN -> run(limbSwing, limbSwingAmount, 1.0F);
            case MOUNT -> crouch(ease(age / 15.0F), 0.0F, ageInTicks);
            case MOUTH -> crouch(1.0F, ease(age / 30.0F), ageInTicks);
            case BITE -> bite(age, ageInTicks);
            case RECOVER -> {
                crouch(1.0F - ease(age / 30.0F), 0.25F * (1.0F - ease(age / 30.0F)), ageInTicks);
                this.jaw.xRot = 0.3F;
            }
            case FIGHT -> fight(limbSwing, limbSwingAmount, ageInTicks);
        }
        if (entity.state() == UncannySleeperEntity.State.IDLE || entity.state() == UncannySleeperEntity.State.FIGHT) {
            this.head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        }
    }

    private void idle(float age) {
        this.body.xRot = 0.18F;
        this.body.zRot = Mth.sin(age * 0.03F) * 0.03F;
        this.head.xRot = 0.25F;
        this.head.zRot = 0.22F;
        this.leftArm.xRot = 0.1F;
        this.rightArm.xRot = 0.08F;
        this.leftArm.zRot = -0.06F;
        this.rightArm.zRot = 0.06F;
    }

    private void run(float limbSwing, float limbSwingAmount, float lean) {
        float swing = limbSwing * 1.2F;
        float amount = Math.min(1.0F, limbSwingAmount * 1.4F);
        this.body.xRot = 0.55F * lean;
        this.head.xRot = -0.5F * lean;
        this.leftLeg.xRot = Mth.sin(swing) * 1.3F * amount;
        this.rightLeg.xRot = -Mth.sin(swing) * 1.3F * amount;
        this.leftArm.xRot = -0.9F - Mth.cos(swing) * 1.0F * amount;
        this.rightArm.xRot = -0.9F + Mth.cos(swing) * 1.0F * amount;
    }

    /**
     * Crouched on the foot of the bed, leaning far over the sleeper's face, hands planted either side.
     * {@code amount} blends from standing; {@code open} opens the jaw.
     */
    private void crouch(float amount, float open, float age) {
        this.body.y = 9.0F + 8.0F * amount;
        this.leftLeg.y = 9.0F + 8.0F * amount;
        this.rightLeg.y = 9.0F + 8.0F * amount;
        this.leftLeg.xRot = -1.45F * amount;
        this.rightLeg.xRot = -1.45F * amount;
        this.body.xRot = 1.1F * amount;
        this.head.xRot = -0.95F * amount;
        this.leftArm.xRot = -1.0F * amount;
        this.rightArm.xRot = -1.0F * amount;
        this.leftArm.zRot = -0.28F * amount;
        this.rightArm.zRot = 0.28F * amount;
        this.jaw.xRot = JAW_WIDE * open;
        this.skull.xRot = -0.45F * open;
        this.head.zRot = Mth.sin(age * 1.7F) * 0.05F * open;
    }

    private void bite(float age, float ageInTicks) {
        crouch(1.0F, 0.0F, ageInTicks);
        if (age < 3.0F) {
            float snap = 1.0F - age / 3.0F;
            this.jaw.xRot = JAW_WIDE * snap;
            this.skull.xRot = -0.45F * snap;
            this.body.xRot += 0.3F * (1.0F - snap);
        } else {
            this.body.xRot += 0.3F;
            this.jaw.xRot = 0.15F + Mth.sin(ageInTicks * 2.3F) * 0.05F;
        }
    }

    private void fight(float limbSwing, float limbSwingAmount, float age) {
        run(limbSwing, limbSwingAmount, 0.6F);
        this.jaw.xRot = 0.45F + Mth.sin(age * 0.9F) * 0.08F;
        this.skull.xRot = -0.15F;
        if (this.attackTime > 0.0F) {
            float strike = Mth.sin(this.attackTime * Mth.PI);
            this.leftArm.xRot = -2.0F * strike - 0.4F;
            this.rightArm.xRot = -2.0F * strike - 0.4F;
        }
    }

    private static float ease(float t) {
        float clamped = Mth.clamp(t, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }
}
