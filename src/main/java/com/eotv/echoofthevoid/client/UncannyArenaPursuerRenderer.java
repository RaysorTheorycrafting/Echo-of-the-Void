package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.custom.UncannyArenaPursuerEntity;
import com.eotv.echoofthevoid.event.special.ArenaPursuerAppearance;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renders each arena threat as an all-black copy of a familiar Minecraft creature. The server
 * matches locomotion to the model (only the Spider climbs), and no emissive layer is drawn: the
 * silhouettes surface from Elsewhere's fog with no eyes and no outline.
 */
public final class UncannyArenaPursuerRenderer
        extends MobRenderer<UncannyArenaPursuerEntity, EntityModel<UncannyArenaPursuerEntity>> {
    private static final ResourceLocation BLACK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EchoOfTheVoid.MODID, "textures/entity/color_black.png");

    private final EntityModel<UncannyArenaPursuerEntity> humanoidModel;
    private final EntityModel<UncannyArenaPursuerEntity> zombieModel;
    private final EntityModel<UncannyArenaPursuerEntity> skeletonModel;
    private final EntityModel<UncannyArenaPursuerEntity> villagerModel;
    private final EntityModel<UncannyArenaPursuerEntity> ironGolemModel;
    private final EntityModel<UncannyArenaPursuerEntity> spiderModel;
    private final EntityModel<UncannyArenaPursuerEntity> sheepModel;
    private final EntityModel<UncannyArenaPursuerEntity> cowModel;
    private final EntityModel<UncannyArenaPursuerEntity> pigModel;

    public UncannyArenaPursuerRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.45F);
        this.humanoidModel = this.model;
        this.zombieModel = new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE));
        this.skeletonModel = new HumanoidModel<>(context.bakeLayer(ModelLayers.SKELETON));
        this.villagerModel = new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER));
        this.ironGolemModel = new ArenaIronGolemModel(context.bakeLayer(ModelLayers.IRON_GOLEM));
        this.spiderModel = new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER));
        // The woolly layer gives the recognisable sheep outline; SheepModel itself requires a Sheep.
        this.sheepModel = new QuadrupedModel<>(context.bakeLayer(ModelLayers.SHEEP_FUR), false, 8.0F, 4.0F, 2.0F, 2.0F, 24) {
        };
        this.cowModel = new CowModel<>(context.bakeLayer(ModelLayers.COW));
        this.pigModel = new PigModel<>(context.bakeLayer(ModelLayers.PIG));
    }

    @Override
    public ResourceLocation getTextureLocation(UncannyArenaPursuerEntity entity) {
        return BLACK_TEXTURE;
    }

    @Override
    public void render(
            UncannyArenaPursuerEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight) {
        EntityModel<UncannyArenaPursuerEntity> previous = this.model;
        this.model = modelFor(entity.appearance());
        try {
            super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        } finally {
            this.model = previous;
        }
    }

    @Override
    protected void scale(UncannyArenaPursuerEntity entity, PoseStack poseStack, float partialTickTime) {
        if (entity.appearance() == ArenaPursuerAppearance.IRON_GOLEM) {
            poseStack.scale(0.72F, 0.72F, 0.72F);
        } else if (entity.appearance() == ArenaPursuerAppearance.VILLAGER) {
            poseStack.scale(0.94F, 0.94F, 0.94F);
        }
    }

    private EntityModel<UncannyArenaPursuerEntity> modelFor(ArenaPursuerAppearance appearance) {
        return switch (appearance) {
            case ZOMBIE -> zombieModel;
            case SKELETON -> skeletonModel;
            case VILLAGER -> villagerModel;
            case IRON_GOLEM -> ironGolemModel;
            case SPIDER -> spiderModel;
            case SHEEP -> sheepModel;
            case COW -> cowModel;
            case PIG -> pigModel;
            case HUMANOID -> humanoidModel;
        };
    }

    /** Iron Golem geometry with generic locomotion, avoiding casts to a real Vanilla golem. */
    private static final class ArenaIronGolemModel extends HierarchicalModel<UncannyArenaPursuerEntity> {
        private final ModelPart root;
        private final ModelPart head;
        private final ModelPart rightArm;
        private final ModelPart leftArm;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;

        private ArenaIronGolemModel(ModelPart root) {
            this.root = root;
            this.head = root.getChild("head");
            this.rightArm = root.getChild("right_arm");
            this.leftArm = root.getChild("left_arm");
            this.rightLeg = root.getChild("right_leg");
            this.leftLeg = root.getChild("left_leg");
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void prepareMobModel(
                UncannyArenaPursuerEntity entity,
                float limbSwing,
                float limbSwingAmount,
                float partialTick) {
            rightArm.xRot = (-0.2F + 1.5F * Mth.triangleWave(limbSwing, 13.0F)) * limbSwingAmount;
            leftArm.xRot = (-0.2F - 1.5F * Mth.triangleWave(limbSwing, 13.0F)) * limbSwingAmount;
        }

        @Override
        public void setupAnim(
                UncannyArenaPursuerEntity entity,
                float limbSwing,
                float limbSwingAmount,
                float ageInTicks,
                float netHeadYaw,
                float headPitch) {
            head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
            head.xRot = headPitch * Mth.DEG_TO_RAD;
            rightLeg.xRot = -1.5F * Mth.triangleWave(limbSwing, 13.0F) * limbSwingAmount;
            leftLeg.xRot = 1.5F * Mth.triangleWave(limbSwing, 13.0F) * limbSwingAmount;
            rightLeg.yRot = 0.0F;
            leftLeg.yRot = 0.0F;
        }
    }
}
