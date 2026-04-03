package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.event.UncannyClientStateSync;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

public final class UncannyAtmosphereClientEffects {
    private static final ResourceLocation VANILLA_SUN_TEXTURE = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    private static float hunterFogIntensity;
    private static long hunterFogLastTick = Long.MIN_VALUE;

    private UncannyAtmosphereClientEffects() {
    }

    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isAlive() || player.isSpectator()) {
            return;
        }

        int phase = UncannyClientStateSync.getClientPhaseIndex();
        if (phase <= 0) {
            return;
        }

        updateHunterFogIntensity(player);
        float alpha = computeWeatherOverlayAlpha();
        if (hunterFogIntensity > 0.0F) {
            alpha = Math.max(alpha, Mth.lerp(hunterFogIntensity, 0.0F, 0.09F));
        }
        if (alpha <= 0.01F) {
            return;
        }

        int width = event.getGuiGraphics().guiWidth();
        int height = event.getGuiGraphics().guiHeight();
        int color = (((int) (alpha * 255.0F)) << 24);
        event.getGuiGraphics().fill(0, 0, width, height, color);
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        if (!UncannyClientStateSync.isClientGiantSunActive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isAlive() || player.isSpectator()) {
            return;
        }

        // Intentional: giant_sun keeps gameplay pressure only (no giant visual overlay).
    }

    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isAlive() || player.isSpectator()) {
            return;
        }

        int phase = UncannyClientStateSync.getClientPhaseIndex();
        if (phase <= 0) {
            return;
        }

        updateHunterFogIntensity(player);

        float near = event.getNearPlaneDistance();
        float far = event.getFarPlaneDistance();
        boolean adjusted = false;

        if (UncannyClientStateSync.isClientWeather("fog_breathing")) {
            float tick = player.tickCount * 0.055F;
            float breatheFactor = 0.90F + (float) Math.sin(tick) * 0.10F;
            far *= breatheFactor;
            near *= 0.94F;
            adjusted = true;
        }
        if (UncannyClientStateSync.isClientWeather("fog_black")
                || UncannyClientStateSync.isClientWeather("sky_empty")) {
            far *= 0.62F;
            near *= 0.80F;
            adjusted = true;
        }
        if (UncannyClientStateSync.isClientWeather("fog_static_wall")) {
            far *= 0.52F;
            near *= 0.78F;
            adjusted = true;
        }
        if (UncannyClientStateSync.isClientWeather("sky_pressure")) {
            far *= 0.84F;
            adjusted = true;
        }
        if (hunterFogIntensity > 0.0F) {
            float hunterFarScale = Mth.lerp(hunterFogIntensity, 1.0F, 0.52F);
            float hunterNearScale = Mth.lerp(hunterFogIntensity, 1.0F, 0.75F);
            far *= hunterFarScale;
            near *= hunterNearScale;
            adjusted = true;
        }

        if (!adjusted) {
            return;
        }

        event.setNearPlaneDistance(Math.max(0.01F, near));
        event.setFarPlaneDistance(Math.max(event.getNearPlaneDistance() + 0.20F, far));
        event.setCanceled(true);
    }

    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isAlive()) {
            return;
        }

        int phase = UncannyClientStateSync.getClientPhaseIndex();
        if (phase <= 0) {
            return;
        }

        updateHunterFogIntensity(player);

        float red = event.getRed();
        float green = event.getGreen();
        float blue = event.getBlue();
        boolean adjusted = false;

        if (UncannyClientStateSync.isClientWeather("rain_ash")) {
            red = Math.min(red * 0.38F, 0.24F);
            green = Math.min(green * 0.38F, 0.24F);
            blue = Math.min(blue * 0.38F, 0.24F);
            adjusted = true;
        }
        if (UncannyClientStateSync.isClientWeather("rain_sobbing")) {
            red *= 0.82F;
            green *= 0.78F;
            blue *= 0.90F;
            adjusted = true;
        }
        if (UncannyClientStateSync.isClientWeather("fog_black")
                || UncannyClientStateSync.isClientWeather("sky_empty")) {
            red *= 0.16F;
            green *= 0.16F;
            blue *= 0.18F;
            adjusted = true;
        }
        if (UncannyClientStateSync.isClientWeather("sky_pressure")) {
            red = Math.min(1.0F, red * 0.66F + 0.06F);
            green = Math.min(1.0F, green * 0.80F + 0.12F);
            blue = Math.min(1.0F, blue * 0.50F + 0.05F);
            adjusted = true;
        }
        if (hunterFogIntensity > 0.0F) {
            float factor = Mth.lerp(hunterFogIntensity, 1.0F, 0.18F);
            red *= factor;
            green *= factor;
            blue *= Math.min(1.0F, factor + 0.04F);
            adjusted = true;
        }

        if (!adjusted) {
            return;
        }

        event.setRed(red);
        event.setGreen(green);
        event.setBlue(blue);
    }

    private static float computeWeatherOverlayAlpha() {
        float alpha = 0.0F;
        if (UncannyClientStateSync.isClientWeather("fog_black")) {
            alpha = Math.max(alpha, 0.14F);
        }
        if (UncannyClientStateSync.isClientWeather("sky_empty")) {
            alpha = Math.max(alpha, 0.22F);
        }
        if (UncannyClientStateSync.isClientWeather("fog_static_wall")) {
            alpha = Math.max(alpha, 0.10F);
        }
        if (UncannyClientStateSync.isClientWeather("rain_ash")) {
            alpha = Math.max(alpha, 0.08F);
        }
        return alpha;
    }

    private static void updateHunterFogIntensity(LocalPlayer player) {
        if (player == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (hunterFogLastTick == now) {
            return;
        }
        hunterFogLastTick = now;

        boolean hunterFogActive = UncannyClientStateSync.isClientHunterFogActive();
        double horizontalMotionSqr = player.getDeltaMovement().horizontalDistanceSqr();
        boolean hunterMovingRaw = hunterFogActive && horizontalMotionSqr > 0.003D;
        boolean hunterSprintingRaw = hunterFogActive && player.isSprinting() && horizontalMotionSqr > 0.01D;
        float hunterTargetIntensity = hunterSprintingRaw ? 1.0F : (hunterMovingRaw ? 0.58F : 0.0F);
        float riseRate = hunterTargetIntensity > hunterFogIntensity ? 0.18F : 0.08F;
        hunterFogIntensity += (hunterTargetIntensity - hunterFogIntensity) * riseRate;
        if (!hunterFogActive && hunterFogIntensity < 0.015F) {
            hunterFogIntensity = 0.0F;
        }
        hunterFogIntensity = Mth.clamp(hunterFogIntensity, 0.0F, 1.0F);
    }

    private static void renderGiantSunInSky(RenderLevelStageEvent event, LocalPlayer player) {
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float dayAngle = player.level().getTimeOfDay(partialTick);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(dayAngle * 360.0F));

        Matrix4f matrix = poseStack.last().pose();
        float size = 118.0F;
        float y = 100.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, VANILLA_SUN_TEXTURE);
        RenderSystem.setShaderColor(1.0F, 0.24F, 0.24F, 1.0F);

        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(matrix, -size, y, -size).setUv(0.0F, 0.0F);
        bufferBuilder.addVertex(matrix, size, y, -size).setUv(1.0F, 0.0F);
        bufferBuilder.addVertex(matrix, size, y, size).setUv(1.0F, 1.0F);
        bufferBuilder.addVertex(matrix, -size, y, size).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }
}
