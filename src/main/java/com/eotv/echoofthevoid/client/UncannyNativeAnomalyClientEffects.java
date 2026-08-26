package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.network.UncannyArmorStandPosePayload;
import com.eotv.echoofthevoid.network.UncannyArrowGazePayload;
import com.eotv.echoofthevoid.network.UncannyBeaconFragmentPayload;
import com.eotv.echoofthevoid.network.UncannyEmptyLeadPayload;
import com.eotv.echoofthevoid.network.UncannyExtraHerdAnimalPayload;
import com.eotv.echoofthevoid.network.UncannyFishingTugPayload;
import com.eotv.echoofthevoid.network.UncannyMapIntruderPayload;
import com.eotv.echoofthevoid.network.UncannyOrphanShadowPayload;
import com.eotv.echoofthevoid.network.UncannyPaintingVariantPayload;
import com.eotv.echoofthevoid.network.UncannyReturnedItemPayload;
import com.eotv.echoofthevoid.network.UncannyStrayExperiencePayload;
import com.eotv.echoofthevoid.network.UncannySuspendedFallPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** Client renderer/state for shared Minecraft-native illusions which never mutate server gameplay state. */
public final class UncannyNativeAnomalyClientEffects {
    private static final RenderType SHADOW_RENDER_TYPE =
            RenderType.entityShadow(ResourceLocation.withDefaultNamespace("textures/misc/shadow.png"));
    private static final Map<Integer, OrphanShadowState> ORPHAN_SHADOWS = new HashMap<>();
    private static final Map<Integer, ArmorPoseState> ARMOR_POSES = new HashMap<>();
    private static final Map<Integer, EmptyLeadState> EMPTY_LEADS = new HashMap<>();
    private static final Map<Integer, PaintingVariantState> PAINTING_VARIANTS = new HashMap<>();
    private static final Map<Integer, ReturnedItemState> RETURNED_ITEMS = new HashMap<>();
    private static final Map<Integer, ArrowGazeState> ARROW_GAZES = new HashMap<>();
    private static final Map<Integer, SuspendedFallState> SUSPENDED_FALLS = new HashMap<>();
    private static final Map<Integer, BeaconFragmentState> BEACON_FRAGMENTS = new HashMap<>();
    private static final Map<Integer, StrayExperienceState> STRAY_EXPERIENCE = new HashMap<>();
    private static final Map<Integer, ExtraHerdAnimalState> EXTRA_HERD_ANIMALS = new HashMap<>();
    private static final String MAP_INTRUDER_DECORATION_ID = "echoofthevoid-map-intruder";
    private static MapIntruderState mapIntruder;
    private static ClientLevel trackedLevel;

    private UncannyNativeAnomalyClientEffects() {
    }

    public static void applyOrphanShadow(UncannyOrphanShadowPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (!payload.visible()) {
            ORPHAN_SHADOWS.remove(payload.shadowId());
            return;
        }
        long endTick = minecraft.level.getGameTime() + Math.max(1, payload.durationTicks());
        ORPHAN_SHADOWS.put(payload.shadowId(), new OrphanShadowState(
                payload.x(),
                payload.y(),
                payload.z(),
                Mth.clamp(payload.radius(), 0.2F, 1.4F),
                endTick));
    }

    public static void applyArmorStandPose(UncannyArmorStandPosePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (payload.variant() < 0) {
            restoreArmorPose(minecraft.level, payload.entityId());
            return;
        }
        Entity entity = minecraft.level.getEntity(payload.entityId());
        if (!(entity instanceof ArmorStand stand) || stand.isInvisible() || stand.isMarker()) {
            return;
        }

        restoreArmorPose(minecraft.level, payload.entityId());
        ArmorPoseState state = new ArmorPoseState(
                stand.getHeadPose(),
                stand.getLeftArmPose(),
                stand.getRightArmPose(),
                minecraft.level.getGameTime() + Math.max(1, payload.durationTicks()));
        ARMOR_POSES.put(payload.entityId(), state);

        int variant = Math.floorMod(payload.variant(), stand.isShowArms() ? 4 : 2);
        switch (variant) {
            case 0 -> stand.setHeadPose(offset(state.head(), 0.0F, 18.0F, 0.0F));
            case 1 -> stand.setHeadPose(offset(state.head(), 0.0F, -18.0F, 0.0F));
            case 2 -> stand.setLeftArmPose(offset(state.leftArm(), -13.0F, 0.0F, 5.0F));
            case 3 -> stand.setRightArmPose(offset(state.rightArm(), -13.0F, 0.0F, -5.0F));
            default -> {
            }
        }
    }

    public static void applyFishingTug(UncannyFishingTugPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(payload.entityId());
        if (!(entity instanceof FishingHook hook) || hook.getHookedIn() != null) {
            return;
        }
        Vec3 motion = hook.getDeltaMovement();
        hook.setDeltaMovement(motion.add(
                Mth.clamp(payload.xImpulse(), -0.22F, 0.22F),
                0.015D,
                Mth.clamp(payload.zImpulse(), -0.22F, 0.22F)));
    }

    public static void applyEmptyLead(UncannyEmptyLeadPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (!payload.visible()) {
            EMPTY_LEADS.remove(payload.visualId());
            return;
        }
        EMPTY_LEADS.put(payload.visualId(), new EmptyLeadState(
                payload.visualId(),
                new Vec3(payload.anchorX(), payload.anchorY(), payload.anchorZ()),
                new Vec3(payload.endX(), payload.endY(), payload.endZ()),
                minecraft.level.getGameTime() + Math.max(1, payload.durationTicks())));
    }

    public static void applyPaintingVariant(UncannyPaintingVariantPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(payload.entityId());
        if (!(entity instanceof Painting painting)) {
            PAINTING_VARIANTS.remove(payload.entityId());
            return;
        }
        if (payload.durationTicks() <= 0) {
            painting.setVariant(payload.variant());
            PAINTING_VARIANTS.remove(payload.entityId());
            return;
        }
        PaintingVariantState previous = PAINTING_VARIANTS.remove(payload.entityId());
        Holder<PaintingVariant> original = previous == null ? painting.getVariant() : previous.original();
        painting.setVariant(payload.variant());
        PAINTING_VARIANTS.put(payload.entityId(), new PaintingVariantState(
                original,
                minecraft.level.getGameTime() + Math.max(1, payload.durationTicks())));
    }

    public static void applyReturnedItem(UncannyReturnedItemPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (!payload.visible() || payload.stack().isEmpty()) {
            RETURNED_ITEMS.remove(payload.visualId());
            return;
        }
        ItemStack visualStack = payload.stack().copy();
        visualStack.setCount(1);
        RETURNED_ITEMS.put(payload.visualId(), new ReturnedItemState(
                visualStack,
                new Vec3(payload.x(), payload.y(), payload.z()),
                minecraft.level.getGameTime() + Math.max(1, payload.durationTicks())));
    }

    public static void applyMapIntruder(UncannyMapIntruderPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        removeMapIntruderDecoration(minecraft.level);
        long now = minecraft.level.getGameTime();
        int duration = Mth.clamp(payload.durationTicks(), 20, 240);
        mapIntruder = new MapIntruderState(
                payload.mapId(), payload.x(), payload.y(), payload.moveX(), payload.moveY(),
                now + Math.max(20L, duration * 3L / 5L), now + duration);
        updateMapIntruderDecoration(minecraft.level, now);
    }

    public static void applyArrowGaze(UncannyArrowGazePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (!payload.active()) {
            restoreArrowGaze(minecraft.level, payload.entityId());
            return;
        }
        Entity raw = minecraft.level.getEntity(payload.entityId());
        if (!(raw instanceof AbstractArrow arrow)) {
            return;
        }
        restoreArrowGaze(minecraft.level, payload.entityId());
        ARROW_GAZES.put(payload.entityId(), new ArrowGazeState(
                arrow.getYRot(), arrow.getXRot(), payload.yaw(), payload.pitch(),
                minecraft.level.getGameTime() + Math.max(1, payload.durationTicks())));
        arrow.setYRot(payload.yaw());
        arrow.setXRot(payload.pitch());
    }

    public static void applySuspendedFall(UncannySuspendedFallPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (!payload.active()) {
            SUSPENDED_FALLS.remove(payload.visualId());
            return;
        }
        int duration = Mth.clamp(payload.durationTicks(), 20, 80);
        long now = minecraft.level.getGameTime();
        SUSPENDED_FALLS.put(payload.visualId(), new SuspendedFallState(
                Block.stateById(payload.blockStateId()),
                new Vec3(payload.x(), payload.y(), payload.z()), now, now + duration, duration));
    }

    public static void applyBeaconFragment(UncannyBeaconFragmentPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (!payload.active()) {
            BEACON_FRAGMENTS.remove(payload.visualId());
            return;
        }
        int duration = Mth.clamp(payload.durationTicks(), 20, 240);
        BEACON_FRAGMENTS.put(payload.visualId(), new BeaconFragmentState(
                new Vec3(payload.x(), payload.y(), payload.z()),
                Mth.clamp(payload.height(), 4, 48), payload.color(),
                minecraft.level.getGameTime() + duration));
    }

    public static void applyStrayExperience(UncannyStrayExperiencePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (!payload.active()) {
            STRAY_EXPERIENCE.remove(payload.visualId());
            return;
        }
        int duration = Mth.clamp(payload.durationTicks(), 20, 120);
        long now = minecraft.level.getGameTime();
        STRAY_EXPERIENCE.put(payload.visualId(), new StrayExperienceState(
                payload.visualId(),
                new Vec3(payload.startX(), payload.startY(), payload.startZ()),
                new Vec3(payload.targetX(), payload.targetY(), payload.targetZ()),
                Mth.clamp(payload.orbCount(), 2, 3), now, now + duration, duration));
    }

    public static void applyExtraHerdAnimal(UncannyExtraHerdAnimalPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (!payload.active()) {
            EXTRA_HERD_ANIMALS.remove(payload.visualId());
            return;
        }
        Entity visual = EntityType.byString(payload.entityTypeId())
                .map(type -> type.create(minecraft.level))
                .orElse(null);
        if (!(visual instanceof AgeableMob animal)) {
            return;
        }
        animal.setBaby(false);
        int duration = Mth.clamp(payload.durationTicks(), 40, 400);
        EXTRA_HERD_ANIMALS.put(payload.visualId(), new ExtraHerdAnimalState(
                payload.anchorEntityId(), animal,
                new Vec3(payload.offsetX(), payload.offsetY(), payload.offsetZ()),
                minecraft.level.getGameTime() + duration));
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != trackedLevel) {
            if (trackedLevel != null) {
                removeMapIntruderDecoration(trackedLevel);
                restoreAllArrowGazes(trackedLevel);
            }
            ORPHAN_SHADOWS.clear();
            ARMOR_POSES.clear();
            EMPTY_LEADS.clear();
            PAINTING_VARIANTS.clear();
            RETURNED_ITEMS.clear();
            ARROW_GAZES.clear();
            SUSPENDED_FALLS.clear();
            BEACON_FRAGMENTS.clear();
            STRAY_EXPERIENCE.clear();
            EXTRA_HERD_ANIMALS.clear();
            mapIntruder = null;
            trackedLevel = level;
        }
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        ORPHAN_SHADOWS.entrySet().removeIf(entry -> now >= entry.getValue().endTick());
        EMPTY_LEADS.entrySet().removeIf(entry -> now >= entry.getValue().endTick());
        RETURNED_ITEMS.entrySet().removeIf(entry -> now >= entry.getValue().endTick());
        SUSPENDED_FALLS.entrySet().removeIf(entry -> now >= entry.getValue().endTick());
        BEACON_FRAGMENTS.entrySet().removeIf(entry -> now >= entry.getValue().endTick());
        STRAY_EXPERIENCE.entrySet().removeIf(entry -> now >= entry.getValue().endTick());
        EXTRA_HERD_ANIMALS.entrySet().removeIf(entry ->
                now >= entry.getValue().endTick()
                        || level.getEntity(entry.getValue().anchorEntityId()) == null);
        if (mapIntruder != null) {
            boolean lowered = minecraft.player == null
                    || !isHoldingMap(minecraft.player.getMainHandItem(), mapIntruder.mapId())
                    && !isHoldingMap(minecraft.player.getOffhandItem(), mapIntruder.mapId());
            if (now >= mapIntruder.endTick() || lowered) {
                removeMapIntruderDecoration(level);
                mapIntruder = null;
            } else {
                updateMapIntruderDecoration(level, now);
            }
        }

        Iterator<Map.Entry<Integer, ArrowGazeState>> arrowIterator = ARROW_GAZES.entrySet().iterator();
        while (arrowIterator.hasNext()) {
            Map.Entry<Integer, ArrowGazeState> entry = arrowIterator.next();
            Entity raw = level.getEntity(entry.getKey());
            if (!(raw instanceof AbstractArrow arrow) || now >= entry.getValue().endTick()) {
                if (raw instanceof AbstractArrow arrow) {
                    arrow.setYRot(entry.getValue().originalYaw());
                    arrow.setXRot(entry.getValue().originalPitch());
                }
                arrowIterator.remove();
                continue;
            }
            arrow.setYRot(entry.getValue().apparentYaw());
            arrow.setXRot(entry.getValue().apparentPitch());
        }

        Iterator<Map.Entry<Integer, ArmorPoseState>> iterator = ARMOR_POSES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ArmorPoseState> entry = iterator.next();
            if (now < entry.getValue().endTick()) {
                continue;
            }
            restoreArmorPose(level, entry.getKey(), entry.getValue());
            iterator.remove();
        }

        Iterator<Map.Entry<Integer, PaintingVariantState>> paintingIterator = PAINTING_VARIANTS.entrySet().iterator();
        while (paintingIterator.hasNext()) {
            Map.Entry<Integer, PaintingVariantState> entry = paintingIterator.next();
            if (now < entry.getValue().endTick()) {
                continue;
            }
            restorePaintingVariant(level, entry.getKey(), entry.getValue());
            paintingIterator.remove();
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES
                || (ORPHAN_SHADOWS.isEmpty() && EMPTY_LEADS.isEmpty()
                && RETURNED_ITEMS.isEmpty() && SUSPENDED_FALLS.isEmpty()
                && BEACON_FRAGMENTS.isEmpty() && STRAY_EXPERIENCE.isEmpty()
                && EXTRA_HERD_ANIMALS.isEmpty())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        if (!ORPHAN_SHADOWS.isEmpty() && minecraft.options.entityShadows().get()) {
            VertexConsumer consumer = buffers.getBuffer(SHADOW_RENDER_TYPE);
            for (OrphanShadowState shadow : ORPHAN_SHADOWS.values()) {
                BlockPos supportPos = BlockPos.containing(shadow.x(), shadow.y() - 0.05D, shadow.z());
                BlockPos lightPos = supportPos.above();
                BlockState support = level.getBlockState(supportPos);
                if (support.getRenderShape() == RenderShape.INVISIBLE
                        || !support.isCollisionShapeFullBlock(level, supportPos)) {
                    continue;
                }
                int rawBrightness = level.getMaxLocalRawBrightness(lightPos);
                if (rawBrightness <= 3) {
                    continue;
                }
                float lightFactor = LightTexture.getBrightness(level.dimensionType(), rawBrightness);
                int alpha = Mth.clamp((int) (255.0F * 0.54F * lightFactor), 24, 142);
                float radius = shadow.radius();

                poseStack.pushPose();
                poseStack.translate(
                        shadow.x() - cameraPosition.x,
                        shadow.y() - cameraPosition.y + 0.0125D,
                        shadow.z() - cameraPosition.z);
                PoseStack.Pose pose = poseStack.last();
                shadowVertex(consumer, pose, -radius, -radius, 0.0F, 0.0F, alpha);
                shadowVertex(consumer, pose, -radius, radius, 0.0F, 1.0F, alpha);
                shadowVertex(consumer, pose, radius, radius, 1.0F, 1.0F, alpha);
                shadowVertex(consumer, pose, radius, -radius, 1.0F, 0.0F, alpha);
                poseStack.popPose();
            }
            buffers.endBatch(SHADOW_RENDER_TYPE);
        }

        if (!EMPTY_LEADS.isEmpty()) {
            VertexConsumer consumer = buffers.getBuffer(RenderType.leash());
            for (EmptyLeadState lead : EMPTY_LEADS.values()) {
                renderEmptyLead(level, poseStack, consumer, cameraPosition, lead);
            }
            buffers.endBatch(RenderType.leash());
        }

        if (!RETURNED_ITEMS.isEmpty()) {
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
            for (Map.Entry<Integer, ReturnedItemState> entry : RETURNED_ITEMS.entrySet()) {
                renderReturnedItem(minecraft, level, poseStack, buffers, cameraPosition,
                        entry.getKey(), entry.getValue(), partialTick);
            }
            buffers.endBatch();
        }

        if (!SUSPENDED_FALLS.isEmpty()) {
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
            for (SuspendedFallState fall : SUSPENDED_FALLS.values()) {
                renderSuspendedFall(minecraft, level, poseStack, buffers, cameraPosition, fall, partialTick);
            }
            buffers.endBatch();
        }

        if (!BEACON_FRAGMENTS.isEmpty()) {
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
            for (BeaconFragmentState fragment : BEACON_FRAGMENTS.values()) {
                poseStack.pushPose();
                poseStack.translate(
                        fragment.position().x - cameraPosition.x,
                        fragment.position().y - cameraPosition.y,
                        fragment.position().z - cameraPosition.z);
                BeaconRenderer.renderBeaconBeam(
                        poseStack, buffers, BeaconRenderer.BEAM_LOCATION,
                        partialTick, 1.0F, level.getGameTime(), 0, fragment.height(),
                        fragment.color(), 0.2F, 0.25F);
                poseStack.popPose();
            }
            buffers.endBatch();
        }

        if (!STRAY_EXPERIENCE.isEmpty()) {
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
            for (StrayExperienceState experience : STRAY_EXPERIENCE.values()) {
                renderStrayExperience(minecraft, level, poseStack, buffers,
                        cameraPosition, experience, partialTick);
            }
            buffers.endBatch();
        }

        if (!EXTRA_HERD_ANIMALS.isEmpty()) {
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
            for (ExtraHerdAnimalState animal : EXTRA_HERD_ANIMALS.values()) {
                renderExtraHerdAnimal(minecraft, level, poseStack, buffers,
                        cameraPosition, animal, partialTick);
            }
            buffers.endBatch();
        }
    }

    private static void renderEmptyLead(
            ClientLevel level,
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 cameraPosition,
            EmptyLeadState lead) {
        double phase = level.getGameTime() * 0.105D + Math.floorMod(lead.visualId(), 97) * 0.173D;
        Vec3 movingEnd = lead.end().add(
                Math.sin(phase) * 0.12D,
                Math.sin(phase * 0.61D) * 0.035D,
                Math.cos(phase * 0.83D) * 0.12D);
        Vec3 delta = movingEnd.subtract(lead.anchor());
        float dx = (float) delta.x;
        float dy = (float) delta.y;
        float dz = (float) delta.z;
        float inverseHorizontal = Mth.invSqrt(dx * dx + dz * dz) * 0.0125F;
        float sideX = dz * inverseHorizontal;
        float sideZ = dx * inverseHorizontal;
        BlockPos anchorPos = BlockPos.containing(lead.anchor());
        BlockPos endPos = BlockPos.containing(movingEnd);
        int anchorBlockLight = level.getBrightness(LightLayer.BLOCK, anchorPos);
        int endBlockLight = level.getBrightness(LightLayer.BLOCK, endPos);
        int anchorSkyLight = level.getBrightness(LightLayer.SKY, anchorPos);
        int endSkyLight = level.getBrightness(LightLayer.SKY, endPos);

        poseStack.pushPose();
        poseStack.translate(
                lead.anchor().x - cameraPosition.x,
                lead.anchor().y - cameraPosition.y,
                lead.anchor().z - cameraPosition.z);
        Matrix4f matrix = poseStack.last().pose();
        for (int index = 0; index <= 24; index++) {
            leashVertexPair(consumer, matrix, dx, dy, dz,
                    anchorBlockLight, endBlockLight, anchorSkyLight, endSkyLight,
                    0.025F, 0.025F, sideX, sideZ, index, false);
        }
        for (int index = 24; index >= 0; index--) {
            leashVertexPair(consumer, matrix, dx, dy, dz,
                    anchorBlockLight, endBlockLight, anchorSkyLight, endSkyLight,
                    0.025F, 0.0F, sideX, sideZ, index, true);
        }
        poseStack.popPose();
    }

    private static void leashVertexPair(
            VertexConsumer consumer,
            Matrix4f matrix,
            float startX,
            float startY,
            float startZ,
            int anchorBlockLight,
            int endBlockLight,
            int anchorSkyLight,
            int endSkyLight,
            float yOffset,
            float verticalOffset,
            float sideX,
            float sideZ,
            int index,
            boolean reverse) {
        float progress = (float) index / 24.0F;
        int blockLight = (int) Mth.lerp(progress, (float) anchorBlockLight, (float) endBlockLight);
        int skyLight = (int) Mth.lerp(progress, (float) anchorSkyLight, (float) endSkyLight);
        int packedLight = LightTexture.pack(blockLight, skyLight);
        float shade = index % 2 == (reverse ? 1 : 0) ? 0.7F : 1.0F;
        float red = 0.5F * shade;
        float green = 0.4F * shade;
        float blue = 0.3F * shade;
        float x = startX * progress;
        float y = startY > 0.0F
                ? startY * progress * progress
                : startY - startY * (1.0F - progress) * (1.0F - progress);
        float z = startZ * progress;
        consumer.addVertex(matrix, x - sideX, y + verticalOffset, z + sideZ)
                .setColor(red, green, blue, 1.0F)
                .setLight(packedLight);
        consumer.addVertex(matrix, x + sideX, y + yOffset - verticalOffset, z - sideZ)
                .setColor(red, green, blue, 1.0F)
                .setLight(packedLight);
    }

    private static void renderReturnedItem(
            Minecraft minecraft,
            ClientLevel level,
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Vec3 cameraPosition,
            int visualId,
            ReturnedItemState item,
            float partialTick) {
        double age = level.getGameTime() + partialTick + Math.floorMod(visualId, 37);
        float bob = Mth.sin((float) age / 10.0F) * 0.05F + 0.10F;
        poseStack.pushPose();
        poseStack.translate(
                item.position().x - cameraPosition.x,
                item.position().y - cameraPosition.y + bob,
                item.position().z - cameraPosition.z);
        poseStack.mulPose(Axis.YP.rotation((float) age / 20.0F));
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(item.position()));
        minecraft.getItemRenderer().renderStatic(
                item.stack(), ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY,
                poseStack, buffers, level, visualId);
        poseStack.popPose();
    }

    private static void renderSuspendedFall(
            Minecraft minecraft,
            ClientLevel level,
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Vec3 cameraPosition,
            SuspendedFallState fall,
            float partialTick) {
        double age = level.getGameTime() + partialTick - fall.startTick();
        double offset;
        if (age < 10.0D) {
            offset = -0.45D * Mth.clamp(age / 10.0D, 0.0D, 1.0D);
        } else if (age < 24.0D) {
            offset = -0.45D;
        } else {
            offset = -0.45D * (1.0D - Mth.clamp(
                    (age - 24.0D) / Math.max(1.0D, fall.durationTicks() - 24.0D), 0.0D, 1.0D));
        }
        poseStack.pushPose();
        poseStack.translate(
                fall.position().x - cameraPosition.x,
                fall.position().y - cameraPosition.y + offset,
                fall.position().z - cameraPosition.z);
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(fall.position()));
        minecraft.getBlockRenderer().renderSingleBlock(
                fall.state(), poseStack, buffers, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static void renderStrayExperience(
            Minecraft minecraft,
            ClientLevel level,
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Vec3 cameraPosition,
            StrayExperienceState state,
            float partialTick) {
        double age = level.getGameTime() + partialTick - state.startTick();
        for (int index = 0; index < state.orbCount(); index++) {
            double localAge = Math.max(0.0D, age - index * 2.5D);
            double localDuration = Math.max(12.0D, state.durationTicks() - index * 3.0D);
            if (localAge >= localDuration) {
                continue;
            }
            double progress = Mth.clamp(localAge / localDuration, 0.0D, 1.0D);
            double eased = progress * progress * (3.0D - 2.0D * progress);
            double angle = Math.floorMod(state.visualId() * 31 + index * 97, 360) * Mth.DEG_TO_RAD;
            Vec3 offset = new Vec3(Math.cos(angle) * 0.34D, 0.10D + index * 0.08D,
                    Math.sin(angle) * 0.34D).scale(1.0D - eased);
            Vec3 position = state.start().lerp(state.target(), eased).add(offset);
            ExperienceOrb orb = new ExperienceOrb(level, position.x, position.y, position.z, 1);
            orb.tickCount = Mth.floor(localAge) + index * 5;
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(position));
            minecraft.getEntityRenderDispatcher().render(
                    orb,
                    position.x - cameraPosition.x,
                    position.y - cameraPosition.y,
                    position.z - cameraPosition.z,
                    0.0F,
                    partialTick,
                    poseStack,
                    buffers,
                    light);
        }
    }

    private static void renderExtraHerdAnimal(
            Minecraft minecraft,
            ClientLevel level,
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Vec3 cameraPosition,
            ExtraHerdAnimalState state,
            float partialTick) {
        Entity anchor = level.getEntity(state.anchorEntityId());
        if (anchor == null) {
            return;
        }
        AgeableMob visual = state.visual();
        Vec3 position = anchor.getPosition(partialTick).add(state.offset());
        visual.setPos(position);
        visual.setYRot(anchor.getYRot());
        visual.setXRot(anchor.getXRot());
        visual.yBodyRot = anchor.getYRot();
        visual.yHeadRot = anchor.getYHeadRot();
        visual.tickCount = anchor.tickCount;
        float movement = (float) Math.min(1.0D, anchor.getDeltaMovement().horizontalDistance() * 6.0D);
        visual.walkAnimation.update(movement, 0.4F);
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(position));
        minecraft.getEntityRenderDispatcher().render(
                visual,
                position.x - cameraPosition.x,
                position.y - cameraPosition.y,
                position.z - cameraPosition.z,
                anchor.getYRot(),
                partialTick,
                poseStack,
                buffers,
                light);
    }

    private static void shadowVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float z,
            float u,
            float v,
            int alpha) {
        consumer.addVertex(pose, x, 0.0F, z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static Rotations offset(Rotations original, float x, float y, float z) {
        return new Rotations(original.getX() + x, original.getY() + y, original.getZ() + z);
    }

    private static void restoreArmorPose(ClientLevel level, int entityId) {
        ArmorPoseState state = ARMOR_POSES.remove(entityId);
        if (state != null) {
            restoreArmorPose(level, entityId, state);
        }
    }

    private static void restoreArmorPose(ClientLevel level, int entityId, ArmorPoseState state) {
        Entity entity = level.getEntity(entityId);
        if (entity instanceof ArmorStand stand) {
            stand.setHeadPose(state.head());
            stand.setLeftArmPose(state.leftArm());
            stand.setRightArmPose(state.rightArm());
        }
    }

    private static void restorePaintingVariant(ClientLevel level, int entityId, PaintingVariantState state) {
        Entity entity = level.getEntity(entityId);
        if (entity instanceof Painting painting) {
            painting.setVariant(state.original());
        }
    }

    private static boolean isHoldingMap(ItemStack stack, int mapId) {
        MapId heldId = stack.get(DataComponents.MAP_ID);
        return heldId != null && heldId.id() == mapId;
    }

    private static void updateMapIntruderDecoration(ClientLevel level, long now) {
        if (mapIntruder == null) {
            return;
        }
        MapItemSavedData data = MapItem.getSavedData(new MapId(mapIntruder.mapId()), level);
        if (data == null) {
            return;
        }
        boolean moved = now >= mapIntruder.moveTick();
        int x = mapIntruder.x() + (moved ? mapIntruder.moveX() : 0);
        int y = mapIntruder.y() + (moved ? mapIntruder.moveY() : 0);
        int scale = 1 << data.scale;
        double worldX = data.centerX + x * 0.5D * scale;
        double worldZ = data.centerZ + y * 0.5D * scale;
        data.addDecoration(
                MapDecorationTypes.TARGET_POINT,
                level,
                MAP_INTRUDER_DECORATION_ID,
                worldX,
                worldZ,
                0.0D,
                null);
    }

    private static void removeMapIntruderDecoration(ClientLevel level) {
        if (mapIntruder == null) {
            return;
        }
        MapItemSavedData data = MapItem.getSavedData(new MapId(mapIntruder.mapId()), level);
        if (data != null) {
            data.removeDecoration(MAP_INTRUDER_DECORATION_ID);
        }
    }

    private static void restoreArrowGaze(ClientLevel level, int entityId) {
        ArrowGazeState state = ARROW_GAZES.remove(entityId);
        Entity raw = level.getEntity(entityId);
        if (state != null && raw instanceof AbstractArrow arrow) {
            arrow.setYRot(state.originalYaw());
            arrow.setXRot(state.originalPitch());
        }
    }

    private static void restoreAllArrowGazes(ClientLevel level) {
        for (Map.Entry<Integer, ArrowGazeState> entry : ARROW_GAZES.entrySet()) {
            Entity raw = level.getEntity(entry.getKey());
            if (raw instanceof AbstractArrow arrow) {
                arrow.setYRot(entry.getValue().originalYaw());
                arrow.setXRot(entry.getValue().originalPitch());
            }
        }
    }

    private record OrphanShadowState(double x, double y, double z, float radius, long endTick) {
    }

    private record ArmorPoseState(Rotations head, Rotations leftArm, Rotations rightArm, long endTick) {
    }

    private record EmptyLeadState(int visualId, Vec3 anchor, Vec3 end, long endTick) {
    }

    private record PaintingVariantState(Holder<PaintingVariant> original, long endTick) {
    }

    private record ReturnedItemState(ItemStack stack, Vec3 position, long endTick) {
    }

    private record MapIntruderState(int mapId, byte x, byte y, byte moveX, byte moveY,
                                    long moveTick, long endTick) {
    }

    private record ArrowGazeState(float originalYaw, float originalPitch,
                                  float apparentYaw, float apparentPitch, long endTick) {
    }

    private record SuspendedFallState(BlockState state, Vec3 position, long startTick,
                                      long endTick, int durationTicks) {
    }

    private record BeaconFragmentState(Vec3 position, int height, int color, long endTick) {
    }

    private record StrayExperienceState(int visualId, Vec3 start, Vec3 target, int orbCount,
                                        long startTick, long endTick, int durationTicks) {
    }

    private record ExtraHerdAnimalState(int anchorEntityId, AgeableMob visual, Vec3 offset,
                                        long endTick) {
    }
}
