package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.network.UncannyLocalizedWeatherPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Client half of shared localized precipitation; it never changes level weather or blocks. */
public final class UncannyLocalizedWeatherClientEffects {
    private static final ResourceLocation RAIN_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/environment/rain.png");
    private static final RenderType RAIN_RENDER_TYPE = RenderType.entityTranslucent(RAIN_TEXTURE);
    private static LocalizedWeatherState active;
    private static ClientLevel trackedLevel;

    private UncannyLocalizedWeatherClientEffects() {
    }

    public static void apply(UncannyLocalizedWeatherPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !payload.active() || payload.eventId().isBlank()) {
            active = null;
            return;
        }
        long now = minecraft.level.getGameTime();
        active = new LocalizedWeatherState(
                payload.eventId(),
                new Vec3(payload.centerX(), payload.centerY(), payload.centerZ()),
                normalizedDirection(payload.directionX(), payload.directionZ()),
                Mth.clamp(payload.radius(), 1, 32),
                payload.seed(),
                now - Math.max(0, payload.elapsedTicks()),
                now + Math.max(1, payload.remainingTicks()),
                Math.max(1, payload.elapsedTicks() + payload.remainingTicks()),
                parsePositions(payload.data()));
    }

    public static Biome.Precipitation filterPrecipitation(
            BlockPos pos, Biome.Precipitation vanilla) {
        LocalizedWeatherState state = active;
        Minecraft minecraft = Minecraft.getInstance();
        if (state == null || minecraft.level == null
                || minecraft.level.getGameTime() >= state.endTick()) {
            return vanilla;
        }
        double dx = pos.getX() + 0.5D - state.center().x;
        double dz = pos.getZ() + 0.5D - state.center().z;
        double distanceSqr = dx * dx + dz * dz;
        long age = minecraft.level.getGameTime() - state.startTick();
        double progress = Mth.clamp((double) age / state.durationTicks(), 0.0D, 1.0D);

        return switch (state.eventId()) {
            case "rain_front" -> {
                if (vanilla == Biome.Precipitation.NONE) {
                    yield vanilla;
                }
                double signed = dx * state.direction().x + dz * state.direction().z;
                int mode = Math.floorMod((int) state.seed(), 3);
                double threshold = mode == 0 ? 4.0D * (1.0D - progress)
                        : mode == 1 ? 4.0D * progress : 2.0D;
                yield signed > threshold ? Biome.Precipitation.NONE : vanilla;
            }
            case "suspended_rain" -> distanceSqr <= state.radius() * state.radius() && age < 32L
                    ? Biome.Precipitation.NONE : vanilla;
            case "dry_eye" -> distanceSqr <= state.radius() * state.radius()
                    ? Biome.Precipitation.NONE : vanilla;
            case "wrong_snowline" -> {
                if (vanilla == Biome.Precipitation.NONE) {
                    yield vanilla;
                }
                double along = dx * state.direction().x + dz * state.direction().z;
                double across = -dx * state.direction().z + dz * state.direction().x;
                double movingCenter = (progress - 0.5D) * state.radius();
                if (Math.abs(along - movingCenter) <= 2.0D && Math.abs(across) <= state.radius()) {
                    yield vanilla == Biome.Precipitation.RAIN
                            ? Biome.Precipitation.SNOW : Biome.Precipitation.RAIN;
                }
                yield vanilla;
            }
            case "light_avoiding_rain" -> isNearSelectedLight(pos, state.selectedLights())
                    ? Biome.Precipitation.NONE : vanilla;
            case "converging_rain" -> distanceSqr <= state.radius() * state.radius()
                    ? Biome.Precipitation.NONE : vanilla;
            default -> vanilla;
        };
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != trackedLevel) {
            active = null;
            trackedLevel = level;
        }
        if (level == null || active == null) {
            return;
        }
        long now = level.getGameTime();
        if (now >= active.endTick()) {
            active = null;
            return;
        }
        if ((now & 1L) == 0L) {
            spawnImpacts(level, active, now);
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || active == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || level.getGameTime() >= active.endTick()) {
            return;
        }
        String id = active.eventId();
        if (!id.equals("suspended_rain") && !id.equals("clear_downpour")
                && !id.equals("converging_rain") && !id.equals("leaking_sky")) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        double cameraDx = camera.x - active.center().x;
        double cameraDz = camera.z - active.center().z;
        if (cameraDx * cameraDx + cameraDz * cameraDz > 48.0D * 48.0D) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RAIN_RENDER_TYPE);
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        renderLocalizedDrops(level, poseStack, consumer, camera, active, partialTick);
        buffers.endBatch(RAIN_RENDER_TYPE);
    }

    private static void renderLocalizedDrops(
            ClientLevel level,
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 camera,
            LocalizedWeatherState state,
            float partialTick) {
        double age = level.getGameTime() + partialTick - state.startTick();
        int count = switch (state.eventId()) {
            case "leaking_sky" -> 18;
            case "suspended_rain" -> 54;
            default -> 82;
        };
        RandomSource random = RandomSource.create(state.seed());
        for (int index = 0; index < count; index++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = Math.sqrt(random.nextDouble()) * state.radius();
            double x = state.center().x + Math.cos(angle) * distance;
            double z = state.center().z + Math.sin(angle) * distance;
            double phase = random.nextDouble() * 14.0D;
            Vec3 top;
            Vec3 bottom;
            if (state.eventId().equals("suspended_rain")) {
                double fixedY = state.center().y + 1.5D + random.nextDouble() * 7.0D;
                double released = Math.max(0.0D, age - 20.0D);
                double drop = released < 12.0D ? released * 0.68D : 100.0D;
                if (fixedY - drop < state.center().y) {
                    continue;
                }
                top = new Vec3(x, fixedY - drop, z);
                bottom = top.add(0.0D, -0.65D, 0.0D);
            } else if (state.eventId().equals("leaking_sky")) {
                double cycle = Math.floorMod(Mth.floor(age * 1.25D + phase), 8) / 8.0D;
                double topY = state.center().y + state.radius();
                double y = Mth.lerp(cycle, topY, state.center().y);
                top = new Vec3(state.center().x + Math.cos(angle) * 0.36D, y,
                        state.center().z + Math.sin(angle) * 0.36D);
                bottom = top.add(0.0D, -0.8D, 0.0D);
            } else if (state.eventId().equals("converging_rain")) {
                double cycle = Math.floorMod(Mth.floor(age + phase), 14) / 14.0D;
                Vec3 source = new Vec3(x, state.center().y + 9.0D, z);
                Vec3 focus = state.center().add(0.0D, 4.5D, 0.0D);
                Vec3 direction = focus.subtract(source).normalize();
                top = source.add(direction.scale(cycle * 7.0D));
                bottom = top.add(direction.scale(1.1D));
            } else {
                double cycle = Math.floorMod(Mth.floor(age * 1.15D + phase), 14) / 14.0D;
                double topY = state.center().y + 10.0D;
                top = new Vec3(x, topY - cycle * 11.0D, z);
                bottom = top.add(0.0D, -1.15D, 0.0D);
            }
            renderDrop(level, poseStack, consumer, camera, top, bottom, 150);
        }
    }

    private static void renderDrop(
            ClientLevel level,
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 camera,
            Vec3 top,
            Vec3 bottom,
            int alpha) {
        Vec3 direction = bottom.subtract(top);
        Vec3 toCamera = camera.subtract(top);
        Vec3 side = direction.cross(toCamera);
        if (side.lengthSqr() < 1.0E-5D) {
            side = new Vec3(0.035D, 0.0D, 0.0D);
        } else {
            side = side.normalize().scale(0.035D);
        }
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(bottom));
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        PoseStack.Pose pose = poseStack.last();
        weatherVertex(consumer, pose, top.subtract(side), 0.0F, 0.0F, alpha, light);
        weatherVertex(consumer, pose, top.add(side), 1.0F, 0.0F, alpha, light);
        weatherVertex(consumer, pose, bottom.add(side), 1.0F, 1.0F, alpha, light);
        weatherVertex(consumer, pose, bottom.subtract(side), 0.0F, 1.0F, alpha, light);
        poseStack.popPose();
    }

    private static void weatherVertex(
            VertexConsumer consumer, PoseStack.Pose pose, Vec3 point,
            float u, float v, int alpha, int light) {
        consumer.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void spawnImpacts(ClientLevel level, LocalizedWeatherState state, long now) {
        if (!state.eventId().equals("clear_downpour")
                && !state.eventId().equals("converging_rain")
                && !state.eventId().equals("leaking_sky")) {
            return;
        }
        RandomSource random = RandomSource.create(state.seed() ^ now * 0x9E3779B97F4A7C15L);
        int count = state.eventId().equals("leaking_sky") ? 2 : 7;
        for (int index = 0; index < count; index++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = state.eventId().equals("leaking_sky")
                    ? random.nextDouble() * 0.35D : Math.sqrt(random.nextDouble()) * state.radius();
            int x = Mth.floor(state.center().x + Math.cos(angle) * distance);
            int z = Mth.floor(state.center().z + Math.sin(angle) * distance);
            int y = state.eventId().equals("leaking_sky")
                    ? Mth.floor(state.center().y)
                    : level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            level.addParticle(ParticleTypes.RAIN,
                    x + random.nextDouble(), y + 0.05D, z + random.nextDouble(), 0.0D, 0.0D, 0.0D);
        }
    }

    private static boolean isNearSelectedLight(BlockPos pos, List<BlockPos> lights) {
        for (BlockPos light : lights) {
            int dx = pos.getX() - light.getX();
            int dz = pos.getZ() - light.getZ();
            if (dx * dx + dz * dz <= 2) {
                return true;
            }
        }
        return false;
    }

    private static Vec3 normalizedDirection(double x, double z) {
        double length = Math.sqrt(x * x + z * z);
        return length < 1.0E-5D ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(x / length, 0.0D, z / length);
    }

    private static List<BlockPos> parsePositions(String data) {
        if (data == null || data.isBlank()) {
            return List.of();
        }
        List<BlockPos> result = new ArrayList<>();
        for (String token : data.split(",")) {
            try {
                result.add(BlockPos.of(Long.parseLong(token)));
            } catch (NumberFormatException ignored) {
                // Malformed presentation data is ignored instead of affecting gameplay.
            }
        }
        return List.copyOf(result);
    }

    private record LocalizedWeatherState(
            String eventId,
            Vec3 center,
            Vec3 direction,
            int radius,
            long seed,
            long startTick,
            long endTick,
            int durationTicks,
            List<BlockPos> selectedLights) {
    }
}
