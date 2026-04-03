package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class UncannyTerrorEntity extends Monster implements UncannyEntityMarker {
    private static final EntityDataAccessor<Optional<UUID>> TARGET_PLAYER =
            SynchedEntityData.defineId(UncannyTerrorEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final int ENGAGED_DURATION_TICKS = 20 * 5;
    private static final int PROXIMITY_SOUND_INTERVAL = 8;
    private static final double NORMAL_APPROACH_SPEED = 0.28D;
    private static final double FLOAT_CHASE_SPEED = 0.16D;
    private static final double TOUCH_DISTANCE_SQR = 1.35D * 1.35D;
    private static final double TARGET_ACQUIRE_RANGE = 28.0D;

    private int engagedTicks;
    private int soundCooldownTicks;
    private boolean touchedPlayer;
    private float shakeBaseYaw;
    private float shakeBasePitch;
    private double lockedPlayerX;
    private double lockedPlayerY;
    private double lockedPlayerZ;

    public UncannyTerrorEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Terror?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET_PLAYER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);
        UncannyEntityUtil.enableDoorNavigation(this);

        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ServerPlayer target = resolveTargetPlayer(serverLevel);
        if (target == null || !target.isAlive()) {
            return;
        }

        if (this.engagedTicks > 0) {
            tickEngaged(target);
            return;
        }

        // Normal behavior until direct eye-contact triggers the "engaged" phase.
        this.setNoGravity(false);
        this.noPhysics = false;
        this.getNavigation().moveTo(target, NORMAL_APPROACH_SPEED);

        if (isDirectlyLookedAt(target)) {
            beginEngaged(target);
        }
    }

    private void tickEngaged(ServerPlayer target) {
        this.setNoGravity(true);
        this.noPhysics = true;
        this.getNavigation().stop();
        lockCameraOnEntity(target);
        immobilizePlayer(target);

        double distanceSqr = this.distanceToSqr(target);
        playProximitySound(target, distanceSqr);

        if (!this.touchedPlayer) {
            Vec3 destination = new Vec3(target.getX(), target.getY(), target.getZ());
            Vec3 direction = destination.subtract(this.position());
            if (direction.lengthSqr() > 1.0E-6D) {
                Vec3 velocity = direction.normalize().scale(FLOAT_CHASE_SPEED);
                this.setDeltaMovement(velocity);
                this.move(MoverType.SELF, velocity);
                float yaw = (float) (Mth.atan2(velocity.z, velocity.x) * (180.0D / Math.PI)) - 90.0F;
                this.setYRot(yaw);
                this.yBodyRot = yaw;
                this.yHeadRot = yaw;
            } else {
                this.setDeltaMovement(Vec3.ZERO);
            }

            if (distanceSqr <= TOUCH_DISTANCE_SQR) {
                this.touchedPlayer = true;
                this.shakeBaseYaw = this.getYRot();
                this.shakeBasePitch = this.getXRot();
                freezePosition();
            }
        } else {
            freezePosition();
            shakeHead();
        }

        this.engagedTicks--;
        if (this.engagedTicks <= 0) {
            this.discard();
        }
    }

    private void lockCameraOnEntity(ServerPlayer player) {
        player.lookAt(EntityAnchorArgument.Anchor.EYES, this.getEyePosition());
    }

    private void immobilizePlayer(ServerPlayer player) {
        if (this.engagedTicks == ENGAGED_DURATION_TICKS) {
            this.lockedPlayerX = player.getX();
            this.lockedPlayerY = player.getY();
            this.lockedPlayerZ = player.getZ();
        }
        player.teleportTo(this.lockedPlayerX, this.lockedPlayerY, this.lockedPlayerZ);
        player.setDeltaMovement(Vec3.ZERO);
        player.hasImpulse = true;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 9, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 6, 4, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 6, 4, false, false, true));
    }

    private void shakeHead() {
        float yawJitter = (this.random.nextFloat() - 0.5F) * 42.0F;
        float pitchJitter = (this.random.nextFloat() - 0.5F) * 24.0F;
        float jitteredYaw = this.shakeBaseYaw + yawJitter;
        this.setYRot(jitteredYaw);
        this.yBodyRot = jitteredYaw;
        this.yHeadRot = jitteredYaw;
        this.setXRot(this.shakeBasePitch + pitchJitter);
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
        this.entityData.get(TARGET_PLAYER).ifPresent(uuid -> tag.putUUID("TargetPlayer", uuid));
        tag.putInt("EngagedTicks", this.engagedTicks);
        tag.putInt("SoundCooldownTicks", this.soundCooldownTicks);
        tag.putBoolean("TouchedPlayer", this.touchedPlayer);
        tag.putFloat("ShakeBaseYaw", this.shakeBaseYaw);
        tag.putFloat("ShakeBasePitch", this.shakeBasePitch);
        tag.putDouble("LockedPlayerX", this.lockedPlayerX);
        tag.putDouble("LockedPlayerY", this.lockedPlayerY);
        tag.putDouble("LockedPlayerZ", this.lockedPlayerZ);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("TargetPlayer")) {
            this.entityData.set(TARGET_PLAYER, Optional.of(tag.getUUID("TargetPlayer")));
        }
        this.engagedTicks = Math.max(0, tag.getInt("EngagedTicks"));
        this.soundCooldownTicks = Math.max(0, tag.getInt("SoundCooldownTicks"));
        this.touchedPlayer = tag.getBoolean("TouchedPlayer");
        this.shakeBaseYaw = tag.getFloat("ShakeBaseYaw");
        this.shakeBasePitch = tag.getFloat("ShakeBasePitch");
        this.lockedPlayerX = tag.getDouble("LockedPlayerX");
        this.lockedPlayerY = tag.getDouble("LockedPlayerY");
        this.lockedPlayerZ = tag.getDouble("LockedPlayerZ");
    }

    private void beginEngaged(ServerPlayer player) {
        if (this.engagedTicks > 0) {
            return;
        }
        this.entityData.set(TARGET_PLAYER, Optional.of(player.getUUID()));
        this.engagedTicks = ENGAGED_DURATION_TICKS;
        this.touchedPlayer = false;
        this.soundCooldownTicks = 0;
        this.shakeBaseYaw = this.getYRot();
        this.shakeBasePitch = this.getXRot();
        this.lockedPlayerX = player.getX();
        this.lockedPlayerY = player.getY();
        this.lockedPlayerZ = player.getZ();
        this.level().playSound(null, player, UncannySoundRegistry.UNCANNY_TINNITUS.get(), SoundSource.HOSTILE, 1.95F, 0.48F);
    }

    private void playProximitySound(ServerPlayer player, double distanceSqr) {
        if (this.soundCooldownTicks-- > 0) {
            return;
        }
        double distance = Math.sqrt(distanceSqr);
        float volume = computeProximityVolume(distance);
        float pitch = (float) Mth.clamp(0.56D + (1.0D - Math.min(distance, 22.0D) / 22.0D) * 0.22D, 0.52D, 0.84D);
        this.level().playSound(null, player, UncannySoundRegistry.UNCANNY_TINNITUS.get(), SoundSource.HOSTILE, volume, pitch);
        this.soundCooldownTicks = PROXIMITY_SOUND_INTERVAL;
    }

    private float computeProximityVolume(double distance) {
        double clamped = Mth.clamp(distance, 1.0D, 26.0D);
        double factor = 1.0D - ((clamped - 1.0D) / 25.0D);
        return (float) (0.28D + factor * 1.68D);
    }

    private void freezePosition() {
        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = true;
    }

    private ServerPlayer resolveTargetPlayer(ServerLevel level) {
        ServerPlayer bound = this.entityData.get(TARGET_PLAYER)
                .map(uuid -> level.getServer().getPlayerList().getPlayer(uuid))
                .orElse(null);
        if (bound != null && bound.isAlive()) {
            return bound;
        }

        ServerPlayer nearest = level.getNearestPlayer(this, TARGET_ACQUIRE_RANGE) instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        if (nearest != null && nearest.isAlive()) {
            this.entityData.set(TARGET_PLAYER, Optional.of(nearest.getUUID()));
            return nearest;
        }
        return null;
    }

    private boolean isDirectlyLookedAt(ServerPlayer player) {
        if (!this.hasLineOfSight(player) || !player.hasLineOfSight(this)) {
            return false;
        }
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 toTerror = this.position().subtract(player.getEyePosition()).normalize();
        double dot = look.dot(toTerror);
        return dot > 0.985D;
    }
}
