package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.client.variant_mixin.UncannyShulkerPeekAccessor;
import com.eotv.echoofthevoid.network.UncannyVanillaVariantVisualPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Client presentation for shared Vanilla-variant cues; it never inserts a fake entity into the level. */
public final class UncannyVanillaVariantClientEffects {
    private static final ResourceLocation GUARDIAN_BEAM =
            ResourceLocation.withDefaultNamespace("textures/entity/guardian_beam.png");
    private static final RenderType GUARDIAN_BEAM_TYPE = RenderType.entityCutoutNoCull(GUARDIAN_BEAM);
    private static final Map<Integer, VisualState> ACTIVE = new HashMap<>();
    private static ClientLevel trackedLevel;

    private UncannyVanillaVariantClientEffects() {
    }

    public static void apply(UncannyVanillaVariantVisualPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        VisualState previous = ACTIVE.remove(payload.entityId());
        if (previous != null) {
            stopAnimation(level, previous);
        }
        if (!payload.active()) {
            return;
        }

        Entity source = level.getEntity(payload.entityId());
        Entity visual = null;
        if ("frog_empty_tongue".equals(payload.effectId()) && source instanceof Frog frog) {
            frog.tongueAnimationState.start(frog.tickCount);
        } else if ("sniffer_second_dig".equals(payload.effectId()) && source instanceof Sniffer sniffer) {
            sniffer.diggingAnimationState.start(sniffer.tickCount);
        } else if ("shulker_empty_aim".equals(payload.effectId()) && source instanceof Shulker shulker) {
            ((UncannyShulkerPeekAccessor) shulker).echoofthevoid$setRawPeekAmount(42);
        } else if ("glow_squid_light_lag".equals(payload.effectId())) {
            visual = EntityType.GLOW_SQUID.create(level);
            if (visual != null) {
                visual.setPos(payload.targetX(), payload.targetY(), payload.targetZ());
            }
        }

        int duration = Mth.clamp(payload.durationTicks(), 1, 160);
        ACTIVE.put(payload.entityId(), new VisualState(
                payload.entityId(), payload.effectId(),
                new Vec3(payload.targetX(), payload.targetY(), payload.targetZ()),
                level.getGameTime() + duration, payload.seed(), visual));
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != trackedLevel) {
            if (trackedLevel != null) {
                ACTIVE.values().forEach(state -> stopAnimation(trackedLevel, state));
            }
            ACTIVE.clear();
            trackedLevel = level;
        }
        if (level == null) {
            return;
        }
        Iterator<VisualState> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            VisualState state = iterator.next();
            if (level.getGameTime() >= state.endTick() || level.getEntity(state.entityId()) == null) {
                stopAnimation(level, state);
                iterator.remove();
            }
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || ACTIVE.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);

        for (VisualState state : ACTIVE.values()) {
            Entity source = level.getEntity(state.entityId());
            if (source == null) {
                continue;
            }
            if ("guardian_false_beam".equals(state.effectId())) {
                renderGuardianBeam(level, source, state.target(), cameraPosition, poseStack, buffers, partialTick);
            } else if ("glow_squid_light_lag".equals(state.effectId()) && state.visual() instanceof GlowSquid visual) {
                visual.tickCount = source.tickCount;
                visual.setYRot(source.getYRot());
                visual.setXRot(source.getXRot());
                visual.yBodyRot = source.getYRot();
                int light = LevelRenderer.getLightColor(level, visual.blockPosition());
                minecraft.getEntityRenderDispatcher().render(
                        visual,
                        visual.getX() - cameraPosition.x,
                        visual.getY() - cameraPosition.y,
                        visual.getZ() - cameraPosition.z,
                        source.getYRot(), partialTick, poseStack, buffers, light);
            }
        }
        buffers.endBatch();
    }

    private static void stopAnimation(ClientLevel level, VisualState state) {
        Entity source = level.getEntity(state.entityId());
        if (source instanceof Frog frog && "frog_empty_tongue".equals(state.effectId())) {
            frog.tongueAnimationState.stop();
        } else if (source instanceof Sniffer sniffer && "sniffer_second_dig".equals(state.effectId())) {
            sniffer.diggingAnimationState.stop();
        } else if (source instanceof Shulker shulker && "shulker_empty_aim".equals(state.effectId())) {
            ((UncannyShulkerPeekAccessor) shulker).echoofthevoid$setRawPeekAmount(0);
        }
    }

    private static void renderGuardianBeam(
            ClientLevel level,
            Entity source,
            Vec3 target,
            Vec3 cameraPosition,
            PoseStack poseStack,
            MultiBufferSource buffers,
            float partialTick) {
        Vec3 origin = source.getPosition(partialTick).add(0.0D, source.getEyeHeight(), 0.0D);
        Vec3 delta = target.subtract(origin);
        float length = (float) delta.length();
        if (length < 0.1F || length > 8.0F) {
            return;
        }
        Vec3 direction = delta.normalize();
        float pitch = (float) Math.acos(direction.y);
        float yaw = (float) Math.atan2(direction.z, direction.x);
        float phase = (level.getGameTime() + partialTick) * -0.075F;
        float radius = 0.13F;
        int red = 96;
        int green = 72;
        int blue = 160;

        poseStack.pushPose();
        poseStack.translate(origin.x - cameraPosition.x, origin.y - cameraPosition.y, origin.z - cameraPosition.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(((float) (Math.PI / 2.0D) - yaw) * Mth.RAD_TO_DEG));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch * Mth.RAD_TO_DEG));
        VertexConsumer consumer = buffers.getBuffer(GUARDIAN_BEAM_TYPE);
        PoseStack.Pose pose = poseStack.last();
        float u0 = phase - Mth.floor(phase);
        float u1 = u0 + length * 1.6F;
        beamQuad(consumer, pose, -radius, 0.0F, radius, length, red, green, blue, 0.0F, u0, 0.5F, u1);
        beamQuad(consumer, pose, 0.0F, -radius, 0.0F, length, red, green, blue, 0.5F, u0, 1.0F, u1);
        poseStack.popPose();
    }

    private static void beamQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x0,
            float z0,
            float x1,
            float length,
            int red,
            int green,
            int blue,
            float u0,
            float v0,
            float u1,
            float v1) {
        beamVertex(consumer, pose, x0, 0.0F, z0, red, green, blue, u0, v0);
        beamVertex(consumer, pose, x0, length, z0, red, green, blue, u0, v1);
        beamVertex(consumer, pose, x1, length, -z0, red, green, blue, u1, v1);
        beamVertex(consumer, pose, x1, 0.0F, -z0, red, green, blue, u1, v0);
    }

    private static void beamVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            int red,
            int green,
            int blue,
            float u,
            float v) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, 220)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private record VisualState(
            int entityId,
            String effectId,
            Vec3 target,
            long endTick,
            long seed,
            Entity visual) {
    }
}
