package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.custom.UncannyDevourerEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Hunched black humanoid whose chest is a portal barred by ribs, with a portal rift down its face.
 * Geometry and keyframes come from {@code Ressources/Modeles/devourer.bbmodel}. The portal parts
 * are drawn by the renderer's portal layer, never in the black pass; a black backing plate travels
 * with the chest portal so it can only ever be seen from the front.
 */
public final class UncannyDevourerModel extends HierarchicalModel<UncannyDevourerEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "devourer"), "main");
    private static final float RIB_OPEN_RADIANS = 70.0F * Mth.DEG_TO_RAD;
    private static final float ARM_REACH_RADIANS = 50.0F * Mth.DEG_TO_RAD;
    /** 0.6 s of keyframes over the 0.8 s seize ({@code UncannyDevourerEntity.SEIZE_TICKS}). */
    private static final float CAPTURE_PLAYBACK_SPEED = 0.75F;
    // Widest chest opening that still sits inside the frame and back plate.
    private static final float MAX_PORTAL_X_SCALE = 1.55F;
    private static final float MAX_PORTAL_Y_SCALE = 1.4F;

    private final ModelPart root;
    private final ModelPart frameLeft;
    private final ModelPart frameRight;
    private final ModelPart ribsLeft;
    private final ModelPart ribsRight;
    private final ModelPart portal;
    private final ModelPart faceRift;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;

    public UncannyDevourerModel(ModelPart root) {
        this.root = root;
        ModelPart devourer = root.getChild("devourer");
        ModelPart body = devourer.getChild("body");
        this.frameLeft = body.getChild("frame_left");
        this.frameRight = body.getChild("frame_right");
        this.ribsLeft = body.getChild("ribs_left");
        this.ribsRight = body.getChild("ribs_right");
        this.portal = body.getChild("portal");
        this.head = body.getChild("head");
        this.faceRift = this.head.getChild("face_rift");
        this.leftArm = body.getChild("left_arm");
        this.rightArm = body.getChild("right_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition devourer = partdefinition.addOrReplaceChild("devourer", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition left_leg = devourer.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 21.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.5F, -21.0F, 0.0F));

        PartDefinition right_leg = devourer.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 21.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(2.5F, -21.0F, 0.0F));

        PartDefinition body = devourer.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -3.0F, -2.0F, 7.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-5.0F, -19.0F, 1.5F, 10.0F, 16.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-5.0F, -4.5F, -2.5F, 10.0F, 1.5F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-7.0F, -20.0F, -2.5F, 14.0F, 2.5F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -21.0F, 0.0F, 0.2443F, 0.0F, 0.0F));

        PartDefinition frame_left = body.addOrReplaceChild("frame_left", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -6.5F, -2.5F, 2.0F, 13.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, -11.0F, 0.0F));

        PartDefinition frame_right = body.addOrReplaceChild("frame_right", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -6.5F, -2.5F, 2.0F, 13.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, -11.0F, 0.0F));

        PartDefinition ribs_left = body.addOrReplaceChild("ribs_left", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 3.2F, -0.4F, 3.5F, 0.8F, 0.8F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(0.0F, -0.4F, -0.4F, 3.5F, 0.8F, 0.8F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(0.0F, -4.0F, -0.4F, 2.8F, 0.8F, 0.8F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, -11.0F, -2.0F));

        PartDefinition ribs_right = body.addOrReplaceChild("ribs_right", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 3.2F, -0.4F, 3.5F, 0.8F, 0.8F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-3.5F, -0.4F, -0.4F, 3.5F, 0.8F, 0.8F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-2.8F, -4.0F, -0.4F, 2.8F, 0.8F, 0.8F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, -11.0F, -2.0F));

        PartDefinition portal = body.addOrReplaceChild("portal", CubeListBuilder.create().texOffs(40, 44).addBox(-4.0F, -6.5F, 0.0F, 8.0F, 13.0F, 0.5F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -11.0F, 0.5F));

        PartDefinition portal_backing = portal.addOrReplaceChild("portal_backing", CubeListBuilder.create().texOffs(0, 0).addBox(-4.4F, -6.9F, 0.5F, 8.8F, 13.8F, 0.5F, new CubeDeformation(0.0F)), PartPose.ZERO);

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.5F, -2.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-3.0F, -6.5F, -7.0F, 6.0F, 6.5F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -20.0F, -1.5F, -0.2793F, 0.0F, 0.0F));

        PartDefinition face_rift = head.addOrReplaceChild("face_rift", CubeListBuilder.create().texOffs(40, 44).addBox(-0.6F, -2.5F, -0.1F, 1.2F, 4.5F, 0.2F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -3.0F, -7.1F));

        PartDefinition left_arm = body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -0.5F, -1.25F, 2.5F, 14.0F, 2.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.5F, -18.5F, 0.0F, -0.3142F, 0.0F, -0.0873F));

        PartDefinition left_forearm = left_arm.addOrReplaceChild("left_forearm", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 14.5F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.25F, 13.0F, 0.0F, 0.1745F, 0.0F, 0.0F));

        PartDefinition left_hand = left_forearm.addOrReplaceChild("left_hand", CubeListBuilder.create().texOffs(0, 0).addBox(0.45F, 0.0F, -1.1F, 0.7F, 7.5F, 0.7F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.35F, 0.0F, -0.35F, 0.7F, 8.5F, 0.7F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-1.15F, 0.0F, 0.4F, 0.7F, 6.5F, 0.7F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 14.0F, 0.0F));

        PartDefinition right_arm = body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -0.5F, -1.25F, 2.5F, 14.0F, 2.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.5F, -18.5F, 0.0F, -0.3142F, 0.0F, 0.0873F));

        PartDefinition right_forearm = right_arm.addOrReplaceChild("right_forearm", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 14.5F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.25F, 13.0F, 0.0F, 0.1745F, 0.0F, 0.0F));

        PartDefinition right_hand = right_forearm.addOrReplaceChild("right_hand", CubeListBuilder.create().texOffs(0, 0).addBox(-1.15F, 0.0F, -1.1F, 0.7F, 7.5F, 0.7F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.35F, 0.0F, -0.35F, 0.7F, 8.5F, 0.7F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(0.45F, 0.0F, 0.4F, 0.7F, 6.5F, 0.7F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 14.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    public ModelPart portal() {
        return portal;
    }

    public ModelPart faceRift() {
        return faceRift;
    }

    @Override
    public void setupAnim(
            UncannyDevourerEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        if (!entity.isSinking()) {
            animateWalk(UncannyDevourerAnimations.WALK, limbSwing, limbSwingAmount, 2.0F, 2.5F);
        }
        animate(entity.idleAnimationState, UncannyDevourerAnimations.IDLE, ageInTicks);
        animate(entity.emergeAnimationState, UncannyDevourerAnimations.EMERGE, ageInTicks);
        // The 0.6 s clip is slowed to span the whole seize before the victim disappears.
        animate(entity.captureAnimationState, UncannyDevourerAnimations.CAPTURE, ageInTicks, CAPTURE_PLAYBACK_SPEED);
        animate(entity.sinkAnimationState, UncannyDevourerAnimations.SINK, ageInTicks);

        // The server's synchronized mouth value (0.25 far, 1.0 touching) opens the chest portal as
        // the victim comes closer: ribs swing out like doors, the frame widens and the arms rise.
        float hunger = Mth.clamp((entity.smoothedMouthOpen(ageInTicks - entity.tickCount) - 0.25F) / 0.75F, 0.0F, 1.0F);
        ribsLeft.yRot += hunger * RIB_OPEN_RADIANS;
        ribsRight.yRot -= hunger * RIB_OPEN_RADIANS;
        frameLeft.x -= hunger * 1.2F;
        frameRight.x += hunger * 1.2F;
        portal.xScale *= 1.0F + hunger * 0.35F;
        portal.yScale *= 1.0F + hunger * 0.25F;
        portal.xScale = Math.min(portal.xScale, MAX_PORTAL_X_SCALE);
        portal.yScale = Math.min(portal.yScale, MAX_PORTAL_Y_SCALE);
        leftArm.xRot -= hunger * ARM_REACH_RADIANS;
        rightArm.xRot -= hunger * ARM_REACH_RADIANS;

        head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        head.xRot += headPitch * Mth.DEG_TO_RAD * 0.5F;
    }
}
