package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class UncannyShadowEntity extends Monster implements UncannyEntityMarker {
    private static final int LIGHT_SCAN_RADIUS = 14;

    private BlockPos targetLightPos;
    private int fleeTicks;
    private int unseenTicks;
    private int scanCooldown;
    private boolean sinking;
    private int sinkTicks;

    public UncannyShadowEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Shadow?");
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);
        UncannyEntityUtil.enableDoorNavigation(this);

        if (this.level().isClientSide()) {
            return;
        }

        ServerPlayer nearestPlayer = null;
        Player nearest = this.level().getNearestPlayer(this, 28.0D);
        if (nearest instanceof ServerPlayer serverPlayer && serverPlayer.isAlive()) {
            nearestPlayer = serverPlayer;
        }

        if (this.fleeTicks <= 0 && nearestPlayer != null && (this.distanceToSqr(nearestPlayer) <= 16.0D || isPlayerLookingAtShadow(nearestPlayer))) {
            startFlee();
        }

        if (this.sinking) {
            tickSinking();
            return;
        }

        if (this.fleeTicks > 0) {
            if (this.isInWaterOrBubble()) {
                startSinking();
                return;
            }
            this.fleeTicks--;
            if (nearestPlayer != null) {
                fleeFrom(nearestPlayer);
                if (isOutOfView(nearestPlayer)) {
                    this.unseenTicks++;
                } else {
                    this.unseenTicks = 0;
                }
            } else {
                randomFleeMovement();
                this.unseenTicks++;
            }

            if (this.fleeTicks <= 0 || this.unseenTicks >= 26) {
                this.discard();
            }
            return;
        }

        this.unseenTicks = 0;
        huntLightSources();
        if (this.tickCount % 6 == 0) {
            destroyNearbyLights(this.blockPosition());
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        ItemEntity reward = new ItemEntity(level, this.getX(), this.getY(), this.getZ(), new ItemStack(UncannyItemRegistry.UNCANNY_REALITY_SHARD.get()));
        level.addFreshEntity(reward);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        UncannyEntityUtil.suppressStepSound(this, pos, state);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FleeTicks", this.fleeTicks);
        tag.putInt("UnseenTicks", this.unseenTicks);
        tag.putInt("ScanCooldown", this.scanCooldown);
        tag.putBoolean("Sinking", this.sinking);
        tag.putInt("SinkTicks", this.sinkTicks);
        if (this.targetLightPos != null) {
            tag.putInt("TargetLightX", this.targetLightPos.getX());
            tag.putInt("TargetLightY", this.targetLightPos.getY());
            tag.putInt("TargetLightZ", this.targetLightPos.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.fleeTicks = Math.max(0, tag.getInt("FleeTicks"));
        this.unseenTicks = Math.max(0, tag.getInt("UnseenTicks"));
        this.scanCooldown = Math.max(0, tag.getInt("ScanCooldown"));
        this.sinking = tag.getBoolean("Sinking");
        this.sinkTicks = Math.max(0, tag.getInt("SinkTicks"));
        if (tag.contains("TargetLightX") && tag.contains("TargetLightY") && tag.contains("TargetLightZ")) {
            this.targetLightPos = new BlockPos(tag.getInt("TargetLightX"), tag.getInt("TargetLightY"), tag.getInt("TargetLightZ"));
        }
    }

    private void startFlee() {
        this.fleeTicks = 120 + this.random.nextInt(71);
        this.unseenTicks = 0;
        this.targetLightPos = null;
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.46D);
        }
    }

    private void startSinking() {
        if (this.sinking) {
            return;
        }
        this.sinking = true;
        this.sinkTicks = 34 + this.random.nextInt(14);
        this.getNavigation().stop();
    }

    private void tickSinking() {
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setDeltaMovement(0.0D, -0.05D, 0.0D);
        this.setPos(this.getX(), this.getY() - 0.05D, this.getZ());
        if (--this.sinkTicks <= 0) {
            this.discard();
        }
    }

    private void huntLightSources() {
        if (this.scanCooldown-- <= 0 || !isValidLightTarget(this.targetLightPos)) {
            this.targetLightPos = findNearestLightSource();
            this.scanCooldown = 16 + this.random.nextInt(14);
        }

        if (this.targetLightPos != null) {
            this.getNavigation().moveTo(
                    this.targetLightPos.getX() + 0.5D,
                    this.targetLightPos.getY(),
                    this.targetLightPos.getZ() + 0.5D,
                    1.38D);

            if (this.blockPosition().distSqr(this.targetLightPos) <= 9) {
                destroyNearbyLights(this.targetLightPos);
                this.targetLightPos = null;
            }
            return;
        }

        if (this.tickCount % 30 == 0) {
            randomFleeMovement();
        }
    }

    private BlockPos findNearestLightSource() {
        BlockPos origin = this.blockPosition();
        BlockPos bestPos = null;
        int bestDistance = Integer.MAX_VALUE;

        for (int dx = -LIGHT_SCAN_RADIUS; dx <= LIGHT_SCAN_RADIUS; dx++) {
            for (int dz = -LIGHT_SCAN_RADIUS; dz <= LIGHT_SCAN_RADIUS; dz++) {
                for (int dy = -6; dy <= 6; dy++) {
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    BlockState state = this.level().getBlockState(candidate);
                    if (state.isAir() || state.getLightEmission(this.level(), candidate) < 11) {
                        continue;
                    }
                    if (state.getDestroySpeed(this.level(), candidate) < 0.0F) {
                        continue;
                    }

                    int distance = candidate.distManhattan(origin);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestPos = candidate.immutable();
                    }
                }
            }
        }

        return bestPos;
    }

    private boolean isValidLightTarget(BlockPos pos) {
        if (pos == null) {
            return false;
        }
        BlockState state = this.level().getBlockState(pos);
        return !state.isAir() && state.getLightEmission(this.level(), pos) >= 11 && state.getDestroySpeed(this.level(), pos) >= 0.0F;
    }

    private boolean isPlayerLookingAtShadow(ServerPlayer player) {
        if (!player.hasLineOfSight(this) || !this.hasLineOfSight(player)) {
            return false;
        }

        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 toShadow = this.position().subtract(player.getEyePosition()).normalize();
        return look.dot(toShadow) > 0.955D;
    }

    private boolean isOutOfView(ServerPlayer player) {
        if (!player.hasLineOfSight(this)) {
            return true;
        }
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 toShadow = this.position().subtract(player.getEyePosition()).normalize();
        return look.dot(toShadow) < 0.14D;
    }

    private void fleeFrom(ServerPlayer player) {
        Vec3 away = this.position().subtract(player.position());
        if (away.lengthSqr() < 0.001D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            away = away.normalize();
        }
        Vec3 destination = this.position().add(away.scale(14.0D));
        this.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.58D);
    }

    private void randomFleeMovement() {
        Vec3 direction = new Vec3(this.random.nextDouble() - 0.5D, 0.0D, this.random.nextDouble() - 0.5D);
        if (direction.lengthSqr() < 0.001D) {
            direction = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            direction = direction.normalize();
        }
        Vec3 destination = this.position().add(direction.scale(10.0D));
        this.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.45D);
    }

    private void destroyNearbyLights(BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = this.level().getBlockState(pos);
                    if (state.isAir() || state.getLightEmission(this.level(), pos) < 11) {
                        continue;
                    }
                    if (state.getDestroySpeed(this.level(), pos) < 0.0F) {
                        continue;
                    }
                    this.level().destroyBlock(pos, true, this);
                }
            }
        }
    }
}


