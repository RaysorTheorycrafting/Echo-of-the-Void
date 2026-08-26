package com.eotv.echoofthevoid.event.passive;

import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Five bounded Rabbit? behaviours. None changes loot, attributes, breeding data or block state. */
public final class RabbitVariantBehaviorSystem {
    private static final String PREFIX = "UncannyRabbit";
    private static final String TAG_WAS_GROUNDED = PREFIX + "WasGrounded";
    private static final String TAG_NEXT_ACTION = PREFIX + "NextAction";
    private static final String TAG_EFFECT_TICK = PREFIX + "EffectTick";
    private static final String TAG_EFFECT_X = PREFIX + "EffectX";
    private static final String TAG_EFFECT_Y = PREFIX + "EffectY";
    private static final String TAG_EFFECT_Z = PREFIX + "EffectZ";
    private static final String TAG_ACTION_END = PREFIX + "ActionEnd";
    private static final String TAG_TARGET = PREFIX + "Target";
    private static final String TAG_SOUND_PLAYED = PREFIX + "SoundPlayed";

    private RabbitVariantBehaviorSystem() {
    }

    public static void reset(Rabbit rabbit, int variant, long now) {
        CompoundTag tag = rabbit.getPersistentData();
        tag.getAllKeys().stream()
                .filter(key -> key.startsWith(PREFIX))
                .toList()
                .forEach(tag::remove);
        tag.putBoolean(TAG_WAS_GROUNDED, rabbit.onGround());
        tag.putLong(TAG_NEXT_ACTION, now + initialDelay(rabbit, variant));
    }

    public static void tick(ServerLevel level, Rabbit rabbit, int variant) {
        switch (variant) {
            case 1 -> tickDelayedHop(level, rabbit);
            case 2 -> tickRearwardGaze(level, rabbit);
            case 3 -> tickFalseBurrow(level, rabbit);
            case 4 -> tickEmptyLanding(level, rabbit);
            case 5 -> tickWatchbound(level, rabbit);
            default -> {
            }
        }
    }

    private static void tickDelayedHop(ServerLevel level, Rabbit rabbit) {
        CompoundTag tag = rabbit.getPersistentData();
        long now = level.getGameTime();
        if (startedJump(rabbit, tag) && tag.getLong(TAG_EFFECT_TICK) <= now
                && rabbit.getRandom().nextFloat() < 0.38F) {
            scheduleAtCurrentPosition(rabbit, tag, now + 8L + rabbit.getRandom().nextInt(13));
        }
        if (tag.getLong(TAG_EFFECT_TICK) > 0L && now >= tag.getLong(TAG_EFFECT_TICK)) {
            level.playSound(null,
                    tag.getDouble(TAG_EFFECT_X), tag.getDouble(TAG_EFFECT_Y), tag.getDouble(TAG_EFFECT_Z),
                    SoundEvents.RABBIT_JUMP, SoundSource.NEUTRAL,
                    0.24F, 0.76F + rabbit.getRandom().nextFloat() * 0.18F);
            tag.putLong(TAG_EFFECT_TICK, 0L);
        }
        tag.putBoolean(TAG_WAS_GROUNDED, rabbit.onGround());
    }

    private static void tickRearwardGaze(ServerLevel level, Rabbit rabbit) {
        CompoundTag tag = rabbit.getPersistentData();
        long now = level.getGameTime();
        ServerPlayer target = taggedPlayer(level, tag);
        if (target != null && now < tag.getLong(TAG_ACTION_END)
                && eligibleObserver(rabbit, target, 18.0D)
                && !isLookingAt(target, rabbit, 0.91D)) {
            rabbit.getLookControl().setLookAt(target, 28.0F, 24.0F);
            return;
        }
        if (target != null) {
            clearTarget(tag);
            tag.putLong(TAG_NEXT_ACTION, now + 280L + rabbit.getRandom().nextInt(361));
        }
        if (now < tag.getLong(TAG_NEXT_ACTION)) {
            return;
        }
        ServerPlayer candidate = nearestObserver(level, rabbit, 18.0D, false);
        tag.putLong(TAG_NEXT_ACTION, now + 160L + rabbit.getRandom().nextInt(241));
        if (candidate == null || isLookingAt(candidate, rabbit, 0.91D)) {
            return;
        }
        tag.putUUID(TAG_TARGET, candidate.getUUID());
        tag.putLong(TAG_ACTION_END, now + 45L + rabbit.getRandom().nextInt(46));
    }

    private static void tickFalseBurrow(ServerLevel level, Rabbit rabbit) {
        CompoundTag tag = rabbit.getPersistentData();
        long now = level.getGameTime();
        long end = tag.getLong(TAG_ACTION_END);
        if (end > now) {
            if (!safeToPause(rabbit)) {
                tag.putLong(TAG_ACTION_END, 0L);
                return;
            }
            rabbit.getNavigation().stop();
            rabbit.setDeltaMovement(rabbit.getDeltaMovement().multiply(0.10D, 1.0D, 0.10D));
            BlockPos supportPos = rabbit.blockPosition().below();
            BlockState support = level.getBlockState(supportPos);
            if (!support.isAir() && now % 4L == 0L) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, support),
                        rabbit.getX(), rabbit.getY() + 0.05D, rabbit.getZ(),
                        3, 0.22D, 0.03D, 0.22D, 0.025D);
            }
            if (!tag.getBoolean(TAG_SOUND_PLAYED)) {
                level.playSound(null, rabbit.blockPosition(), support.getSoundType().getHitSound(),
                        SoundSource.NEUTRAL, 0.18F, 0.72F + rabbit.getRandom().nextFloat() * 0.14F);
                tag.putBoolean(TAG_SOUND_PLAYED, true);
            }
            return;
        }
        if (end != 0L) {
            tag.putLong(TAG_ACTION_END, 0L);
            tag.putBoolean(TAG_SOUND_PLAYED, false);
        }
        if (now < tag.getLong(TAG_NEXT_ACTION)) {
            return;
        }
        tag.putLong(TAG_NEXT_ACTION, now + 900L + rabbit.getRandom().nextInt(1201));
        ServerPlayer witness = nearestObserver(level, rabbit, 15.0D, true);
        if (witness != null && safeToPause(rabbit)) {
            tag.putLong(TAG_ACTION_END, now + 22L + rabbit.getRandom().nextInt(17));
            tag.putBoolean(TAG_SOUND_PLAYED, false);
        }
    }

    private static void tickEmptyLanding(ServerLevel level, Rabbit rabbit) {
        CompoundTag tag = rabbit.getPersistentData();
        long now = level.getGameTime();
        if (startedJump(rabbit, tag) && tag.getLong(TAG_EFFECT_TICK) <= now
                && rabbit.getRandom().nextFloat() < 0.32F) {
            scheduleAtCurrentPosition(rabbit, tag, now + 12L + rabbit.getRandom().nextInt(13));
        }
        if (tag.getLong(TAG_EFFECT_TICK) > 0L && now >= tag.getLong(TAG_EFFECT_TICK)) {
            BlockPos point = BlockPos.containing(
                    tag.getDouble(TAG_EFFECT_X), tag.getDouble(TAG_EFFECT_Y), tag.getDouble(TAG_EFFECT_Z));
            BlockPos supportPos = point.below();
            if (level.hasChunkAt(point)) {
                BlockState support = level.getBlockState(supportPos);
                if (!support.isAir()) {
                    level.playSound(null, point, support.getSoundType().getStepSound(),
                            SoundSource.NEUTRAL, 0.16F, 1.02F);
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, support),
                            point.getX() + 0.5D, point.getY() + 0.08D, point.getZ() + 0.5D,
                            4, 0.18D, 0.02D, 0.18D, 0.015D);
                }
            }
            tag.putLong(TAG_EFFECT_TICK, 0L);
        }
        tag.putBoolean(TAG_WAS_GROUNDED, rabbit.onGround());
    }

    private static void tickWatchbound(ServerLevel level, Rabbit rabbit) {
        CompoundTag tag = rabbit.getPersistentData();
        long now = level.getGameTime();
        ServerPlayer target = taggedPlayer(level, tag);
        if (target != null && now < tag.getLong(TAG_ACTION_END)
                && safeToPause(rabbit)
                && eligibleObserver(rabbit, target, 14.0D)
                && isLookingAt(target, rabbit, 0.94D)) {
            rabbit.getNavigation().stop();
            rabbit.setDeltaMovement(rabbit.getDeltaMovement().multiply(0.08D, 1.0D, 0.08D));
            rabbit.getLookControl().setLookAt(target, 30.0F, 28.0F);
            return;
        }
        if (target != null) {
            clearTarget(tag);
            tag.putLong(TAG_NEXT_ACTION, now + 900L + rabbit.getRandom().nextInt(901));
        }
        if (now < tag.getLong(TAG_NEXT_ACTION) || !safeToPause(rabbit)) {
            return;
        }
        tag.putLong(TAG_NEXT_ACTION, now + 400L + rabbit.getRandom().nextInt(401));
        ServerPlayer witness = nearestObserver(level, rabbit, 13.0D, true);
        if (witness == null) {
            return;
        }
        tag.putUUID(TAG_TARGET, witness.getUUID());
        tag.putLong(TAG_ACTION_END, now + 45L + rabbit.getRandom().nextInt(46));
    }

    private static boolean startedJump(Rabbit rabbit, CompoundTag tag) {
        return tag.getBoolean(TAG_WAS_GROUNDED)
                && !rabbit.onGround()
                && rabbit.getDeltaMovement().y > 0.02D;
    }

    private static void scheduleAtCurrentPosition(Rabbit rabbit, CompoundTag tag, long tick) {
        tag.putLong(TAG_EFFECT_TICK, tick);
        tag.putDouble(TAG_EFFECT_X, rabbit.getX());
        tag.putDouble(TAG_EFFECT_Y, rabbit.getY());
        tag.putDouble(TAG_EFFECT_Z, rabbit.getZ());
    }

    private static long initialDelay(Rabbit rabbit, int variant) {
        return switch (variant) {
            case 1, 4 -> 0L;
            case 2 -> 120L + rabbit.getRandom().nextInt(181);
            case 3 -> 500L + rabbit.getRandom().nextInt(601);
            case 5 -> 600L + rabbit.getRandom().nextInt(601);
            default -> 200L;
        };
    }

    private static ServerPlayer nearestObserver(
            ServerLevel level,
            Rabbit rabbit,
            double range,
            boolean mustBeLooking) {
        double maxDistance = range * range;
        return level.players().stream()
                .filter(player -> eligibleObserver(rabbit, player, range))
                .filter(player -> !mustBeLooking || isLookingAt(player, rabbit, 0.91D))
                .filter(player -> player.distanceToSqr(rabbit) <= maxDistance)
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(rabbit)))
                .orElse(null);
    }

    private static boolean eligibleObserver(Rabbit rabbit, ServerPlayer player, double range) {
        return player.isAlive()
                && !player.isSpectator()
                && player.distanceToSqr(rabbit) <= range * range
                && player.hasLineOfSight(rabbit)
                && !rabbit.isFood(player.getMainHandItem())
                && !rabbit.isFood(player.getOffhandItem());
    }

    private static boolean isLookingAt(ServerPlayer player, Rabbit rabbit, double threshold) {
        Vec3 direction = rabbit.getEyePosition().subtract(player.getEyePosition());
        if (direction.lengthSqr() < 0.0001D) {
            return true;
        }
        return player.getViewVector(1.0F).normalize().dot(direction.normalize()) >= threshold;
    }

    private static boolean safeToPause(Rabbit rabbit) {
        return rabbit.isAlive()
                && rabbit.onGround()
                && !rabbit.isPassenger()
                && !rabbit.isLeashed()
                && !rabbit.isInWaterOrBubble()
                && !rabbit.isOnFire()
                && !rabbit.isInLove()
                && rabbit.hurtTime <= 0;
    }

    private static ServerPlayer taggedPlayer(ServerLevel level, CompoundTag tag) {
        if (!tag.hasUUID(TAG_TARGET)) {
            return null;
        }
        UUID id = tag.getUUID(TAG_TARGET);
        return level.getServer().getPlayerList().getPlayer(id);
    }

    private static void clearTarget(CompoundTag tag) {
        tag.remove(TAG_TARGET);
        tag.putLong(TAG_ACTION_END, 0L);
    }
}
