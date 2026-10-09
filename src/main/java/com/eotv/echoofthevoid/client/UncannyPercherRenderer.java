package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.entity.custom.UncannyPercherEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

/** Standard black silhouette, crouched on its perch like a player sneaking on a ledge. */
public class UncannyPercherRenderer extends UncannySilhouetteRenderer<UncannyPercherEntity> {
    public UncannyPercherRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(UncannyPercherEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        // HumanoidMobRenderer never forwards the pose: without this the figure would stand upright.
        this.getModel().crouching = entity.isCrouching();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public Vec3 getRenderOffset(UncannyPercherEntity entity, float partialTicks) {
        return entity.isCrouching() ? new Vec3(0.0D, -0.125D, 0.0D) : super.getRenderOffset(entity, partialTicks);
    }
}
