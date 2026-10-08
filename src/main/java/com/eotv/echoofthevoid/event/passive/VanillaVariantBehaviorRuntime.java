package com.eotv.echoofthevoid.event.passive;

import com.eotv.echoofthevoid.event.passive.ApprovedVanillaVariantCatalog.BehaviorKind;
import com.eotv.echoofthevoid.event.passive.ApprovedVanillaVariantCatalog.VisualStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Shared, bounded behavior primitives for the large Vanilla-variant catalog.
 *
 * <p>These primitives never alter health, attributes, loot, equipment, breeding, ownership or
 * blocks. They run only while the Vanilla mob is idle and surrender immediately to a real
 * target, injury, panic, rider, lead or reproduction.</p>
 */
public final class VanillaVariantBehaviorRuntime {
    public static final String VISUAL_TAG_PREFIX = "eotv_variant_visual_";

    private static final String TAG_NEXT = "UncannyExpandedVariantNext";
    private static final String TAG_MODE = "UncannyExpandedVariantMode";
    private static final String TAG_END = "UncannyExpandedVariantEnd";
    private static final String TAG_X = "UncannyExpandedVariantX";
    private static final String TAG_Y = "UncannyExpandedVariantY";
    private static final String TAG_Z = "UncannyExpandedVariantZ";
    private static final String TAG_MEMORY_X = "UncannyExpandedVariantMemoryX";
    private static final String TAG_MEMORY_Y = "UncannyExpandedVariantMemoryY";
    private static final String TAG_MEMORY_Z = "UncannyExpandedVariantMemoryZ";
    private static final String TAG_MEMORY_TIME = "UncannyExpandedVariantMemoryTime";
    private static final String TAG_TRACE_DUE = "UncannyExpandedVariantTraceDue";
    private static final String TAG_YAW = "UncannyExpandedVariantYaw";
    private static final String TAG_DEV = "UncannyExpandedVariantDev";
    private static final String TAG_OWNS_SILENCE = "UncannyExpandedVariantOwnsSilence";

    private VanillaVariantBehaviorRuntime() {
    }

    public static void initialize(
            Mob mob,
            String variantId,
            BehaviorKind behavior,
            VisualStyle visualStyle,
            boolean silent,
            long now,
            boolean dev) {
        CompoundTag data = mob.getPersistentData();
        data.putInt(TAG_MODE, 0);
        data.putLong(TAG_END, 0L);
        data.putLong(TAG_NEXT, now + (dev ? 20L : 260L + mob.getRandom().nextInt(520)));
        data.putLong(TAG_MEMORY_TIME, now);
        storeMemory(data, mob.position());
        data.putBoolean(TAG_DEV, dev);
        clearVisualTags(mob);
        if (visualStyle != VisualStyle.NORMAL) {
            mob.addTag(visualTag(visualStyle));
        }
        if (silent) {
            data.putBoolean(TAG_OWNS_SILENCE, true);
            mob.setSilent(true);
        } else if (data.getBoolean(TAG_OWNS_SILENCE)) {
            data.remove(TAG_OWNS_SILENCE);
            mob.setSilent(false);
        }
    }

    public static void tick(
            ServerLevel level,
            Mob mob,
            BehaviorKind behavior,
            boolean silent,
            long now) {
        if (silent) {
            mob.setSilent(true);
        }
        if (behavior == BehaviorKind.SPECIALIZED) {
            return;
        }

        CompoundTag data = mob.getPersistentData();
        if (!canRunIdleCue(mob)) {
            cancel(data, now);
            rememberCurrentPosition(data, mob, now);
            return;
        }

        switch (behavior) {
            case EMPTY_FOCUS -> tickEmptyFocus(level, mob, data, now);
            case EMPTY_APPROACH -> tickEmptyApproach(level, mob, data, now);
            case REARWARD_GAZE -> tickRearwardGaze(level, mob, data, now);
            case WATCHED_STILLNESS -> tickWatchedStillness(level, mob, data, now);
            case RETURN_TO_MEMORY -> tickReturnToMemory(level, mob, data, now);
            case DELAYED_TRACE -> tickDelayedTrace(level, mob, data, now);
            case WRONG_FACING -> tickWrongFacing(mob, data, now);
            case CIRCLE_EMPTY -> tickCircleEmpty(level, mob, data, now);
            case LOOK_ABOVE -> tickLookAbove(mob, data, now);
            case MIRROR_PLAYER -> tickMirrorPlayer(level, mob, data, now);
            case STAGGERED_PAUSE -> tickStaggeredPause(mob, data, now);
            case GROUP_CONVERGENCE -> tickGroupConvergence(level, mob, data, now);
            case FALSE_INTERACTION -> tickFalseInteraction(mob, data, now);
            case SPECIALIZED -> {
            }
        }
        rememberCurrentPosition(data, mob, now);
    }

    /**
     * Replacement mobs already carry a defining combat behavior. Only three primitives are
     * allowed to run during combat: a presentation-only delayed trace, or short hesitations
     * that make the threat easier rather than less fair. Every other primitive remains idle-only.
     */
    public static void tickReplacement(
            ServerLevel level,
            Mob mob,
            BehaviorKind behavior,
            long now) {
        if (behavior == BehaviorKind.SPECIALIZED) {
            return;
        }
        CompoundTag data = mob.getPersistentData();
        boolean combatSafe = behavior == BehaviorKind.DELAYED_TRACE
                || behavior == BehaviorKind.WATCHED_STILLNESS
                || behavior == BehaviorKind.STAGGERED_PAUSE;
        if (!combatSafe && !canRunIdleCue(mob)) {
            cancel(data, now);
            rememberCurrentPosition(data, mob, now);
            return;
        }
        if (combatSafe && (!mob.isAlive() || mob.hurtTime > 0 || mob.isOnFire()
                || mob.isPassenger() || mob.isVehicle() || mob.isLeashed())) {
            cancel(data, now);
            rememberCurrentPosition(data, mob, now);
            return;
        }

        switch (behavior) {
            case DELAYED_TRACE -> tickDelayedTrace(level, mob, data, now);
            case WATCHED_STILLNESS -> tickWatchedStillness(level, mob, data, now);
            case STAGGERED_PAUSE -> tickStaggeredPause(mob, data, now);
            default -> tick(level, mob, behavior, false, now);
        }
        rememberCurrentPosition(data, mob, now);
    }

    public static String visualTag(VisualStyle style) {
        return VISUAL_TAG_PREFIX + style.name().toLowerCase(java.util.Locale.ROOT);
    }

    public static void clearVisualTags(Mob mob) {
        mob.getTags().removeIf(tag -> tag.startsWith(VISUAL_TAG_PREFIX));
    }

    private static void tickEmptyFocus(ServerLevel level, Mob mob, CompoundTag data, long now) {
        if (startWhenReady(mob, data, now, 34, 58)) {
            storeTarget(data, randomNearbyPoint(mob, 2.5D, 4.5D));
        }
        if (data.getInt(TAG_MODE) == 1) {
            Vec3 target = target(data);
            mob.getLookControl().setLookAt(target.x, target.y, target.z, 45.0F, 35.0F);
            finishAtEnd(mob, data, now);
        }
    }

    private static void tickEmptyApproach(ServerLevel level, Mob mob, CompoundTag data, long now) {
        if (ready(data, now)) {
            Vec3 candidate = randomNearbyPoint(mob, 2.8D, 5.2D);
            if (!level.isLoaded(BlockPos.containing(candidate))) {
                defer(data, now, mob, 120, 220);
                return;
            }
            storeTarget(data, candidate);
            begin(data, now + 55L + mob.getRandom().nextInt(35));
        }
        if (data.getInt(TAG_MODE) == 1) {
            Vec3 target = target(data);
            mob.getLookControl().setLookAt(target.x, target.y, target.z, 45.0F, 35.0F);
            mob.getNavigation().moveTo(target.x, target.y, target.z, 0.72D);
            if (mob.position().distanceToSqr(target) <= 1.7D || now >= data.getLong(TAG_END)) {
                finish(mob, data, now);
            }
        }
    }

    private static void tickRearwardGaze(ServerLevel level, Mob mob, CompoundTag data, long now) {
        ServerPlayer player = nearestPlayer(level, mob, 18.0D);
        if (player == null || isLookingAt(player, mob, 0.78D)) {
            if (data.getInt(TAG_MODE) != 0) {
                finish(mob, data, now);
            }
            return;
        }
        if (startWhenReady(mob, data, now, 36, 66) || data.getInt(TAG_MODE) == 1) {
            mob.getLookControl().setLookAt(player, 55.0F, 45.0F);
            finishAtEnd(mob, data, now);
        }
    }

    private static void tickWatchedStillness(ServerLevel level, Mob mob, CompoundTag data, long now) {
        ServerPlayer player = nearestPlayer(level, mob, 16.0D);
        boolean watched = player != null && isLookingAt(player, mob, 0.92D);
        if (!watched) {
            if (data.getInt(TAG_MODE) == 1) {
                finish(mob, data, now);
            }
            return;
        }
        if (startWhenReady(mob, data, now, 24, 44) || data.getInt(TAG_MODE) == 1) {
            mob.getNavigation().stop();
            mob.setDeltaMovement(mob.getDeltaMovement().multiply(0.15D, 1.0D, 0.15D));
            if (player != null) {
                mob.getLookControl().setLookAt(player, 40.0F, 35.0F);
            }
            finishAtEnd(mob, data, now);
        }
    }

    private static void tickReturnToMemory(ServerLevel level, Mob mob, CompoundTag data, long now) {
        if (ready(data, now)) {
            Vec3 memory = memory(data);
            double distance = mob.position().distanceToSqr(memory);
            if (data.getLong(TAG_MEMORY_TIME) <= 0L || distance < 2.5D * 2.5D || distance > 12.0D * 12.0D
                    || !level.isLoaded(BlockPos.containing(memory))) {
                storeMemory(data, mob.position());
                data.putLong(TAG_MEMORY_TIME, now);
                defer(data, now, mob, 220, 520);
                return;
            }
            storeTarget(data, memory);
            begin(data, now + 65L + mob.getRandom().nextInt(35));
        }
        if (data.getInt(TAG_MODE) == 1) {
            Vec3 destination = target(data);
            mob.getNavigation().moveTo(destination.x, destination.y, destination.z, 0.68D);
            mob.getLookControl().setLookAt(destination.x, destination.y, destination.z, 40.0F, 35.0F);
            if (mob.position().distanceToSqr(destination) <= 1.8D || now >= data.getLong(TAG_END)) {
                finish(mob, data, now);
                storeMemory(data, mob.position());
                data.putLong(TAG_MEMORY_TIME, now);
            }
        }
    }

    private static void tickDelayedTrace(ServerLevel level, Mob mob, CompoundTag data, long now) {
        if (ready(data, now) && mob.getDeltaMovement().horizontalDistanceSqr() > 0.0025D) {
            Vec3 previous = memory(data);
            storeTarget(data, previous);
            data.putLong(TAG_TRACE_DUE, now + 18L + mob.getRandom().nextInt(25));
            begin(data, now + 55L);
        }
        if (data.getInt(TAG_MODE) != 1 || now < data.getLong(TAG_TRACE_DUE)) {
            return;
        }
        Vec3 trace = target(data);
        BlockPos tracePos = BlockPos.containing(trace);
        if (level.isLoaded(tracePos)) {
            if (mob.isInWaterOrBubble()) {
                level.sendParticles(ParticleTypes.BUBBLE, trace.x, trace.y + 0.25D, trace.z,
                        5, 0.18D, 0.12D, 0.18D, 0.01D);
            } else {
                BlockPos supportPos = tracePos.below();
                BlockState support = level.getBlockState(supportPos);
                if (!support.isAir()) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, support),
                            trace.x, trace.y + 0.08D, trace.z, 5, 0.18D, 0.04D, 0.18D, 0.012D);
                    level.playSound(null, trace.x, trace.y, trace.z,
                            support.getSoundType().getStepSound(), SoundSource.NEUTRAL,
                            0.16F, 0.72F + mob.getRandom().nextFloat() * 0.16F);
                }
            }
        }
        finish(mob, data, now);
    }

    private static void tickWrongFacing(Mob mob, CompoundTag data, long now) {
        if (ready(data, now)) {
            data.putFloat(TAG_YAW, Mth.wrapDegrees(mob.getYRot() + 155.0F + mob.getRandom().nextFloat() * 50.0F));
            begin(data, now + 30L + mob.getRandom().nextInt(35));
        }
        if (data.getInt(TAG_MODE) == 1) {
            float yaw = data.getFloat(TAG_YAW);
            mob.setYBodyRot(yaw);
            mob.setYHeadRot(yaw);
            finishAtEnd(mob, data, now);
        }
    }

    private static void tickCircleEmpty(ServerLevel level, Mob mob, CompoundTag data, long now) {
        if (ready(data, now)) {
            Vec3 center = randomNearbyPoint(mob, 2.2D, 3.6D);
            if (!level.isLoaded(BlockPos.containing(center))) {
                defer(data, now, mob, 100, 180);
                return;
            }
            storeTarget(data, center);
            begin(data, now + 70L + mob.getRandom().nextInt(35));
        }
        if (data.getInt(TAG_MODE) == 1) {
            Vec3 center = target(data);
            double angle = (now + mob.getId() * 11L) * 0.12D;
            Vec3 waypoint = center.add(Math.cos(angle) * 1.8D, 0.0D, Math.sin(angle) * 1.8D);
            mob.getNavigation().moveTo(waypoint.x, waypoint.y, waypoint.z, 0.66D);
            mob.getLookControl().setLookAt(center.x, center.y + 0.5D, center.z, 40.0F, 35.0F);
            finishAtEnd(mob, data, now);
        }
    }

    private static void tickLookAbove(Mob mob, CompoundTag data, long now) {
        if (startWhenReady(mob, data, now, 34, 62)) {
            Vec3 point = randomNearbyPoint(mob, 1.0D, 2.5D).add(0.0D, 3.0D + mob.getRandom().nextDouble() * 3.0D, 0.0D);
            storeTarget(data, point);
        }
        if (data.getInt(TAG_MODE) == 1) {
            Vec3 point = target(data);
            mob.getLookControl().setLookAt(point.x, point.y, point.z, 45.0F, 45.0F);
            finishAtEnd(mob, data, now);
        }
    }

    private static void tickMirrorPlayer(ServerLevel level, Mob mob, CompoundTag data, long now) {
        ServerPlayer player = nearestPlayer(level, mob, 16.0D);
        if (player == null) {
            return;
        }
        if (startWhenReady(mob, data, now, 36, 68) || data.getInt(TAG_MODE) == 1) {
            mob.setYHeadRot(player.getYHeadRot());
            mob.setYBodyRot(player.getYRot());
            mob.setXRot(Mth.clamp(player.getXRot(), -55.0F, 55.0F));
            finishAtEnd(mob, data, now);
        }
    }

    private static void tickStaggeredPause(Mob mob, CompoundTag data, long now) {
        if (startWhenReady(mob, data, now, 45, 72) || data.getInt(TAG_MODE) == 1) {
            if ((now + mob.getId()) % 11L < 5L) {
                mob.getNavigation().stop();
                mob.setDeltaMovement(mob.getDeltaMovement().multiply(0.22D, 1.0D, 0.22D));
            }
            finishAtEnd(mob, data, now);
        }
    }

    private static void tickGroupConvergence(ServerLevel level, Mob mob, CompoundTag data, long now) {
        if (ready(data, now)) {
            Vec3 point = randomNearbyPoint(mob, 2.5D, 4.5D);
            if (!level.isLoaded(BlockPos.containing(point))) {
                defer(data, now, mob, 100, 180);
                return;
            }
            storeTarget(data, point);
            begin(data, now + 65L + mob.getRandom().nextInt(30));
        }
        if (data.getInt(TAG_MODE) != 1) {
            return;
        }
        Vec3 point = target(data);
        int moved = 0;
        for (Mob member : level.getEntitiesOfClass(
                Mob.class,
                mob.getBoundingBox().inflate(8.0D),
                candidate -> candidate.getType() == mob.getType() && canRunIdleCue(candidate))) {
            member.getLookControl().setLookAt(point.x, point.y + 0.4D, point.z, 35.0F, 30.0F);
            member.getNavigation().moveTo(point.x, point.y, point.z, 0.62D);
            if (++moved >= 4) {
                break;
            }
        }
        finishAtEnd(mob, data, now);
    }

    private static void tickFalseInteraction(Mob mob, CompoundTag data, long now) {
        if (!ready(data, now)) {
            return;
        }
        mob.swing(InteractionHand.MAIN_HAND);
        defer(data, now, mob, 420, 980);
    }

    private static boolean startWhenReady(Mob mob, CompoundTag data, long now, int minimumDuration, int spread) {
        if (!ready(data, now)) {
            return false;
        }
        begin(data, now + minimumDuration + mob.getRandom().nextInt(Math.max(1, spread)));
        return true;
    }

    private static boolean ready(CompoundTag data, long now) {
        return data.getInt(TAG_MODE) == 0 && now >= data.getLong(TAG_NEXT);
    }

    private static void begin(CompoundTag data, long end) {
        data.putInt(TAG_MODE, 1);
        data.putLong(TAG_END, end);
    }

    private static void finishAtEnd(Mob mob, CompoundTag data, long now) {
        if (now >= data.getLong(TAG_END)) {
            finish(mob, data, now);
        }
    }

    private static void finish(Mob mob, CompoundTag data, long now) {
        data.putInt(TAG_MODE, 0);
        data.putLong(TAG_END, 0L);
        defer(data, now, mob, 620, 1300);
    }

    private static void cancel(CompoundTag data, long now) {
        if (data.getInt(TAG_MODE) != 0) {
            data.putInt(TAG_MODE, 0);
            data.putLong(TAG_END, 0L);
            data.putLong(TAG_NEXT, Math.max(data.getLong(TAG_NEXT), now + 100L));
        }
    }

    private static void defer(CompoundTag data, long now, Mob mob, int minimum, int spread) {
        int adjustedMinimum = data.getBoolean(TAG_DEV) ? Math.max(35, minimum / 6) : minimum;
        int adjustedSpread = data.getBoolean(TAG_DEV) ? Math.max(25, spread / 6) : spread;
        data.putLong(TAG_NEXT, now + adjustedMinimum + mob.getRandom().nextInt(Math.max(1, adjustedSpread)));
    }

    private static boolean canRunIdleCue(Mob mob) {
        if (!mob.isAlive() || mob.getTarget() != null || mob.isAggressive() || mob.hurtTime > 0
                || mob.isOnFire() || mob.isPassenger() || mob.isVehicle() || mob.isLeashed()) {
            return false;
        }
        return !(mob instanceof Animal animal && animal.isInLove());
    }

    private static ServerPlayer nearestPlayer(ServerLevel level, Entity entity, double radius) {
        return level.getNearestPlayer(
                entity.getX(), entity.getY(), entity.getZ(), radius,
                candidate -> candidate.isAlive() && !candidate.isSpectator()) instanceof ServerPlayer player
                ? player
                : null;
    }

    private static boolean isLookingAt(ServerPlayer player, Entity entity, double threshold) {
        if (!player.hasLineOfSight(entity)) {
            return false;
        }
        Vec3 delta = entity.getEyePosition().subtract(player.getEyePosition());
        return delta.lengthSqr() < 0.0001D
                || player.getViewVector(1.0F).normalize().dot(delta.normalize()) >= threshold;
    }

    private static Vec3 randomNearbyPoint(Mob mob, double minimumDistance, double maximumDistance) {
        double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
        double distance = minimumDistance + mob.getRandom().nextDouble() * Math.max(0.1D, maximumDistance - minimumDistance);
        return mob.position().add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
    }

    private static void rememberCurrentPosition(CompoundTag data, Mob mob, long now) {
        if (data.getInt(TAG_MODE) != 0 || now - data.getLong(TAG_MEMORY_TIME) < 120L) {
            return;
        }
        storeMemory(data, mob.position());
        data.putLong(TAG_MEMORY_TIME, now);
    }

    private static void storeTarget(CompoundTag data, Vec3 point) {
        data.putDouble(TAG_X, point.x);
        data.putDouble(TAG_Y, point.y);
        data.putDouble(TAG_Z, point.z);
    }

    private static Vec3 target(CompoundTag data) {
        return new Vec3(data.getDouble(TAG_X), data.getDouble(TAG_Y), data.getDouble(TAG_Z));
    }

    private static void storeMemory(CompoundTag data, Vec3 point) {
        data.putDouble(TAG_MEMORY_X, point.x);
        data.putDouble(TAG_MEMORY_Y, point.y);
        data.putDouble(TAG_MEMORY_Z, point.z);
    }

    private static Vec3 memory(CompoundTag data) {
        return new Vec3(
                data.getDouble(TAG_MEMORY_X),
                data.getDouble(TAG_MEMORY_Y),
                data.getDouble(TAG_MEMORY_Z));
    }

}
