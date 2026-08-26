package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class UncannyShadowEntity extends Monster implements UncannyEntityMarker {
    private static final int LIGHT_SCAN_RADIUS = 14;
    private static final double FEAR_RADIUS = 7.0D;

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

        LivingEntity nearestThreat = findNearestFearSource((ServerLevel) this.level());
        if (!this.sinking && this.fleeTicks <= 0 && nearestThreat != null) {
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
            if (nearestThreat != null) {
                fleeFrom(nearestThreat);
                if (nearestThreat instanceof ServerPlayer player && isOutOfView(player)) {
                    this.unseenTicks++;
                } else {
                    this.unseenTicks = 0;
                }
            } else {
                randomFleeMovement();
                this.unseenTicks++;
            }

            if (this.fleeTicks <= 0 || this.unseenTicks >= 26) {
                startSinking();
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
        if (this.sinking) {
            this.setNoGravity(true);
            this.noPhysics = true;
        }
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
        this.sinkTicks = 60 + this.random.nextInt(31);
        this.fleeTicks = 0;
        this.targetLightPos = null;
        this.setNoGravity(true);
        this.noPhysics = true;
        this.getNavigation().stop();
    }

    private void tickSinking() {
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setDeltaMovement(0.0D, -0.035D, 0.0D);
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
                    if (!isDestroyableLight(candidate)) {
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
        return isDestroyableLight(pos);
    }

    private boolean isOutOfView(ServerPlayer player) {
        if (!player.hasLineOfSight(this)) {
            return true;
        }
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 toShadow = this.position().subtract(player.getEyePosition()).normalize();
        return look.dot(toShadow) < 0.14D;
    }

    private LivingEntity findNearestFearSource(ServerLevel level) {
        LivingEntity closest = null;
        double bestDistanceSqr = FEAR_RADIUS * FEAR_RADIUS;
        for (LivingEntity candidate : level.getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(FEAR_RADIUS),
                this::isFearSource)) {
            double distanceSqr = this.distanceToSqr(candidate);
            if (distanceSqr <= bestDistanceSqr) {
                bestDistanceSqr = distanceSqr;
                closest = candidate;
            }
        }
        return closest;
    }

    private boolean isFearSource(LivingEntity entity) {
        if (!entity.isAlive() || entity == this) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isSpectator();
        }
        if (entity instanceof Wolf wolf) {
            return wolf.isTame();
        }
        if (entity instanceof Cat cat) {
            return cat.isTame();
        }
        return entity instanceof IronGolem ironGolem && ironGolem.isPlayerCreated();
    }

    private void fleeFrom(LivingEntity threat) {
        Vec3 away = this.position().subtract(threat.position());
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
                    if (!isDestroyableLight(pos)) {
                        continue;
                    }
                    this.level().destroyBlock(pos, true, this);
                }
            }
        }
    }

    private boolean isDestroyableLight(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        return !state.isAir()
                && !state.is(UncannyBlockRegistry.UNCANNY_ALTAR.get())
                && !state.is(UncannyBlockRegistry.UNCANNY_ALTAR_PART.get())
                && state.getLightEmission(this.level(), pos) >= 11
                && state.getDestroySpeed(this.level(), pos) >= 0.0F;
    }
}


