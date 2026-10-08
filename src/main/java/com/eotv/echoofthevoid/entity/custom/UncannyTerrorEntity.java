package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.sound.UncannySoundDelivery;
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
    /**
     * Screamer (user, 2026-10-08): the instant it is looked at, it is in the player's face with a
     * scream, shaking violently, then it bursts away. Short, loud and sudden rather than a long lock.
     */
    private static final int ENGAGED_DURATION_TICKS = 32;
    private static final int LUNGE_TICKS = 3;
    private static final double FACE_DISTANCE = 0.62D;
    private static final double NORMAL_APPROACH_SPEED = 0.28D;
    private static final double TARGET_ACQUIRE_RANGE = 28.0D;
    /** Lurking right behind an unaware player this long also sets it off. */
    private static final double BEHIND_TRIGGER_DISTANCE = 2.2D;
    private static final int BEHIND_TRIGGER_TICKS = 30;

    private int engagedTicks;
    private boolean touchedPlayer;
    private float shakeBaseYaw;
    private float shakeBasePitch;
    private double lockedPlayerX;
    private double lockedPlayerY;
    private double lockedPlayerZ;
    private int lurkingTicks;
    private Vec3 lungeStart;
    private Vec3 screamDirection;

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
            // A personal scene must never jump to another nearby player after a death,
            // disconnect or dimension change, nor remain orphaned in the world.
            this.discard();
            return;
        }

        if (this.engagedTicks > 0) {
            if (this.screamDirection == null) {
                // Reloaded in the middle of a scream: the moment is gone, never replay it late.
                this.discard();
                return;
            }
            tickEngaged(target);
            return;
        }

        // Silent approach until it is looked at, or until it has lurked right behind the player.
        this.setNoGravity(false);
        this.noPhysics = false;
        this.getNavigation().moveTo(target, NORMAL_APPROACH_SPEED);
        this.lurkingTicks = this.distanceToSqr(target) <= BEHIND_TRIGGER_DISTANCE * BEHIND_TRIGGER_DISTANCE
                ? this.lurkingTicks + 1
                : 0;
        if (isDirectlyLookedAt(target) || this.lurkingTicks >= BEHIND_TRIGGER_TICKS) {
            beginEngaged(target);
        }
    }

    private void tickEngaged(ServerPlayer target) {
        this.setNoGravity(true);
        this.noPhysics = true;
        this.getNavigation().stop();
        immobilizePlayer(target);

        int elapsed = ENGAGED_DURATION_TICKS - this.engagedTicks;
        // Its face lands right in front of the player's eyes within three ticks, then stays there.
        Vec3 face = target.getEyePosition()
                .add(this.screamDirection.scale(FACE_DISTANCE))
                .subtract(0.0D, this.getEyeHeight(), 0.0D);
        Vec3 position;
        if (elapsed < LUNGE_TICKS) {
            double progress = (elapsed + 1.0D) / LUNGE_TICKS;
            position = this.lungeStart.lerp(face, progress * progress);
        } else {
            this.touchedPlayer = true;
            position = face.add(
                    (this.random.nextDouble() - 0.5D) * 0.12D,
                    (this.random.nextDouble() - 0.5D) * 0.10D,
                    (this.random.nextDouble() - 0.5D) * 0.12D);
        }
        this.setPos(position.x, position.y, position.z);
        this.setDeltaMovement(Vec3.ZERO);
        faceThePlayer(target);
        if (this.touchedPlayer) {
            shakeHead();
        }
        lockCameraOnEntity(target);

        if (--this.engagedTicks <= 0) {
            burstAway(target);
        }
    }

    private void faceThePlayer(ServerPlayer target) {
        Vec3 toPlayer = target.getEyePosition().subtract(this.getEyePosition());
        float yaw = (float) (Mth.atan2(toPlayer.z, toPlayer.x) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) -(Mth.atan2(toPlayer.y, Math.sqrt(toPlayer.x * toPlayer.x + toPlayer.z * toPlayer.z))
                * (180.0D / Math.PI));
        this.shakeBaseYaw = yaw;
        this.shakeBasePitch = pitch;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.setXRot(pitch);
    }

    /** It vanishes in a burst and leaves the player half-blind with a racing heart. */
    private void burstAway(ServerPlayer target) {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getEyeY(), this.getZ(), 40, 0.35D, 0.45D, 0.35D, 0.02D);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                    this.getX(), this.getEyeY(), this.getZ(), 24, 0.3D, 0.4D, 0.3D, 0.05D);
        }
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 10, 0, false, false, false));
        UncannySoundDelivery.playMental(
                target, UncannySoundRegistry.UNCANNY_HEARTBEAT.get(), SoundSource.HOSTILE, 0.9F, 1.15F, 80);
        this.discard();
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
        float yawJitter = (this.random.nextFloat() - 0.5F) * 70.0F;
        float pitchJitter = (this.random.nextFloat() - 0.5F) * 45.0F;
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
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(TARGET_PLAYER).ifPresent(uuid -> tag.putUUID("TargetPlayer", uuid));
        tag.putInt("EngagedTicks", this.engagedTicks);
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
        this.shakeBaseYaw = this.getYRot();
        this.shakeBasePitch = this.getXRot();
        this.lockedPlayerX = player.getX();
        this.lockedPlayerY = player.getY();
        this.lockedPlayerZ = player.getZ();
        this.lungeStart = this.position();
        Vec3 toward = this.getEyePosition().subtract(player.getEyePosition());
        Vec3 horizontal = new Vec3(toward.x, 0.0D, toward.z);
        this.screamDirection = horizontal.lengthSqr() < 1.0E-4D
                ? player.getViewVector(1.0F).multiply(1.0D, 0.0D, 1.0D).normalize()
                : horizontal.normalize();
        UncannySoundDelivery.playMental(
                player, UncannySoundRegistry.UNCANNY_TERROR_SCREAM.get(), SoundSource.HOSTILE,
                1.0F, 0.94F + this.random.nextFloat() * 0.12F, ENGAGED_DURATION_TICKS + 10);
    }

    private void freezePosition() {
        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = true;
    }

    private ServerPlayer resolveTargetPlayer(ServerLevel level) {
        Optional<UUID> targetId = this.entityData.get(TARGET_PLAYER);
        if (targetId.isPresent()) {
            ServerPlayer bound = level.getServer().getPlayerList().getPlayer(targetId.get());
            return bound != null
                            && bound.isAlive()
                            && !bound.isSpectator()
                            && bound.serverLevel() == level
                    ? bound
                    : null;
        }

        ServerPlayer nearest = level.getNearestPlayer(this, TARGET_ACQUIRE_RANGE) instanceof ServerPlayer serverPlayer
                ? serverPlayer
                : null;
        if (nearest != null && nearest.isAlive() && !nearest.isSpectator()) {
            this.entityData.set(TARGET_PLAYER, Optional.of(nearest.getUUID()));
            return nearest;
        }
        return null;
    }

    private boolean isDirectlyLookedAt(ServerPlayer player) {
        if (!this.hasLineOfSight(player) || !player.hasLineOfSight(this)) {
            return false;
        }
        // Looking at its face or its body counts; aiming at its feet was the old, accidental rule.
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 eyes = player.getEyePosition();
        double toFace = look.dot(this.getEyePosition().subtract(eyes).normalize());
        double toBody = look.dot(this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D).subtract(eyes).normalize());
        return Math.max(toFace, toBody) > 0.985D;
    }
}
