package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.entity.custom.UncannyFriendEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/**
 * Renders the old friend exactly like a player: the skin comes from the friend's tab-list entry (the
 * same source the game uses for real players), with the slim or wide arms that skin asks for.
 */
public class UncannyFriendRenderer extends LivingEntityRenderer<UncannyFriendEntity, PlayerModel<UncannyFriendEntity>> {
    private final PlayerModel<UncannyFriendEntity> wideModel;
    private final PlayerModel<UncannyFriendEntity> slimModel;

    public UncannyFriendRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wideModel = this.model;
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    private static PlayerSkin skinOf(UncannyFriendEntity entity) {
        UUID id = entity.profileId().orElse(entity.getUUID());
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection == null ? null : connection.getPlayerInfo(id);
        return info != null ? info.getSkin() : DefaultPlayerSkin.get(id);
    }

    @Override
    public void render(UncannyFriendEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        PlayerSkin skin = skinOf(entity);
        this.model = skin.model() == PlayerSkin.Model.SLIM ? this.slimModel : this.wideModel;
        this.model.setAllVisible(true);
        this.model.crouching = entity.isCrouching();
        HumanoidModel.ArmPose main = armPose(entity, entity.getMainHandItem(), false);
        HumanoidModel.ArmPose off = armPose(entity, entity.getOffhandItem(), true);
        if (entity.getMainArm() == HumanoidArm.RIGHT) {
            this.model.rightArmPose = main;
            this.model.leftArmPose = off;
        } else {
            this.model.rightArmPose = off;
            this.model.leftArmPose = main;
        }
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private static HumanoidModel.ArmPose armPose(UncannyFriendEntity entity, ItemStack stack, boolean offhand) {
        if (stack.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        }
        boolean usingThisHand = entity.isUsingItem()
                && (entity.getUsedItemHand() == net.minecraft.world.InteractionHand.OFF_HAND) == offhand;
        if (usingThisHand && stack.getItem() instanceof ShieldItem) {
            return HumanoidModel.ArmPose.BLOCK;
        }
        return HumanoidModel.ArmPose.ITEM;
    }

    /** Lying flat while swimming, as in {@code PlayerRenderer.setupRotations}. */
    @Override
    protected void setupRotations(UncannyFriendEntity entity, PoseStack poseStack, float bob, float yBodyRot,
            float partialTick, float scale) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
        float swim = entity.getSwimAmount(partialTick);
        if (swim > 0.0F) {
            float pitch = entity.isInWater() ? -90.0F - entity.getXRot() : -90.0F;
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(swim, 0.0F, pitch)));
            if (entity.isVisuallySwimming()) {
                poseStack.translate(0.0F, -1.0F, 0.3F);
            }
        }
    }

    @Override
    protected void scale(UncannyFriendEntity entity, PoseStack poseStack, float partialTicks) {
        // Same scale as PlayerRenderer: a player model is drawn at 15/16.
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public ResourceLocation getTextureLocation(UncannyFriendEntity entity) {
        return skinOf(entity).texture();
    }
}
