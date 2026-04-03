package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LlamaRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.scores.Team;

public class UncannyLlamaRenderer extends LlamaRenderer {
    private static final String TAG_LLAMA_BLACK = "eotv_passive_llama_v3";
    private static final String TAG_LLAMA_BLACK_MARKER = "eotv_black_llama";
    private static final String TEAM_LLAMA_BLACK = "eotv_black_llama";
    private static final ResourceLocation BLACK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "textures/entity/color_black.png");

    public UncannyLlamaRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.LLAMA);
    }

    @Override
    public ResourceLocation getTextureLocation(Llama entity) {
        return shouldRenderBlack(entity) ? BLACK_TEXTURE : super.getTextureLocation(entity);
    }

    @Override
    public void render(
            Llama entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight) {
        boolean shouldRenderBlack = shouldRenderBlack(entity);

        if (shouldRenderBlack) {
            RenderSystem.setShaderColor(0.02F, 0.02F, 0.02F, 1.0F);
        }

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);

        if (shouldRenderBlack) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private boolean shouldRenderBlack(Llama entity) {
        Team team = entity.getTeam();
        return entity.getTags().contains(TAG_LLAMA_BLACK)
                || entity.getTags().contains(TAG_LLAMA_BLACK_MARKER)
                || (team != null && TEAM_LLAMA_BLACK.equals(team.getName()));
    }
}
