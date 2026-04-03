package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class UncannyFollowerEntity extends Monster implements UncannyEntityMarker {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_PLAYER =
            SynchedEntityData.defineId(UncannyFollowerEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private long endTick = Long.MIN_VALUE;
    private boolean fleeing;
    private long vanishAtTick = Long.MIN_VALUE;
    private boolean sinking;
    private long sinkEndTick = Long.MIN_VALUE;
    private boolean attacking;
    private long attackEndTick = Long.MIN_VALUE;
    private int meleeCooldownTicks;

    public UncannyFollowerEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Follower?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_PLAYER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.82D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 20.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    public void setupFollower(ServerPlayer owner, long durationTicks) {
        this.entityData.set(OWNER_PLAYER, Optional.of(owner.getUUID()));
        this.endTick = owner.serverLevel().getGameTime() + Math.max(20L, durationTicks);
        this.fleeing = false;
        this.vanishAtTick = Long.MIN_VALUE;
        this.sinking = false;
        this.sinkEndTick = Long.MIN_VALUE;
        this.attacking = false;
        this.attackEndTick = Long.MIN_VALUE;
        this.meleeCooldownTicks = 0;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        boolean hurt = super.hurt(source, amount);
        if (attacker instanceof Player && !this.attacking) {
            this.getNavigation().stop();
            if (!this.isDeadOrDying()) {
                this.sinking = true;
                this.sinkEndTick = this.level() instanceof ServerLevel serverLevel
                        ? serverLevel.getGameTime() + 38L
                        : this.tickCount + 38L;
            }
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);
        UncannyEntityUtil.enableDoorNavigation(this);
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel level)) {
            return;
        }

        ServerPlayer owner = resolveOwner(level);
        if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
        }

        long now = level.getGameTime();
        if (this.meleeCooldownTicks > 0) {
            this.meleeCooldownTicks--;
        }

        if (this.sinking) {
            this.setTarget(null);
            this.setNoGravity(true);
            this.noPhysics = true;
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, -0.045D, 0.0D);
            this.setPos(this.getX(), this.getY() - 0.045D, this.getZ());
            if (now >= this.sinkEndTick) {
                this.discard();
            }
            return;
        }
        this.setNoGravity(false);
        this.noPhysics = false;

        if (this.fleeing && this.isInWaterOrBubble()) {
            startSinking(now, 34L);
            return;
        }

        if (this.vanishAtTick != Long.MIN_VALUE && now >= this.vanishAtTick) {
            this.discard();
            return;
        }

        if (!this.fleeing && !this.attacking && now >= this.endTick) {
            this.fleeing = true;
            level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 0.8F);
        }

        if (this.attacking) {
            if (now >= this.attackEndTick || !owner.isAlive()) {
                this.attacking = false;
                this.fleeing = true;
                level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 0.82F);
                return;
            }
            this.setTarget(owner);
            this.lookAt(owner, 80.0F, 80.0F);
            this.getNavigation().moveTo(owner, 1.28D);

            if (this.hasLineOfSight(owner)) {
                double reach = this.getBbWidth() * 2.1D;
                double allowedDistanceSqr = reach * reach + owner.getBbWidth();
                if (this.distanceToSqr(owner) <= allowedDistanceSqr && this.meleeCooldownTicks <= 0) {
                    this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    this.doHurtTarget(owner);
                    this.meleeCooldownTicks = 12;
                }
            }
            return;
        }

        if (this.fleeing) {
            Vec3 away = this.position().subtract(owner.position());
            if (away.lengthSqr() < 0.0001D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                away = away.normalize();
            }
            Vec3 target = this.position().add(away.scale(20.0D));
            this.getNavigation().moveTo(target.x, target.y, target.z, 1.78D);
            if (this.distanceToSqr(owner) > 34.0D * 34.0D || !owner.hasLineOfSight(this)) {
                this.discard();
            }
            return;
        }

        this.setTarget(null);
        this.lookAt(owner, 60.0F, 60.0F);
        this.getLookControl().setLookAt(owner.getX(), owner.getEyeY(), owner.getZ(), 60.0F, 60.0F);
        double distanceSqr = this.distanceToSqr(owner);
        double distance = Math.sqrt(distanceSqr);

        boolean visibleToOwner = isBroadlyVisibleTo(owner);
        if (!visibleToOwner) {
            if (distance <= 1.7D) {
                this.attacking = true;
                this.attackEndTick = now + 20L * 16L;
                this.setTarget(owner);
                this.getNavigation().stop();
                level.playSound(null, this.blockPosition(), UncannySoundRegistry.UNCANNY_HURLER_SCREAM.get(), SoundSource.HOSTILE, 2.7F, 0.88F);
                return;
            }

            // Match the Watcher-like pressure: noticeable approach speed when unseen.
            double unseenApproachSpeed = distance > 14.0D ? 0.38D : 0.30D;
            this.getNavigation().moveTo(owner, unseenApproachSpeed);
            return;
        }

        if (distance < 12.0D) {
            Vec3 away = this.position().subtract(owner.position());
            if (away.lengthSqr() < 0.0001D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                away = away.normalize();
            }
            Vec3 anchor = owner.position().add(away.scale(16.0D));
            this.getNavigation().moveTo(anchor.x, anchor.y, anchor.z, 1.08D);
            return;
        }

        if (distance <= 24.0D) {
            this.getNavigation().stop();
            return;
        }

        double speed = Mth.clamp(distance > 32.0D ? 1.00D : 0.88D, 0.78D, 1.04D);
        this.getNavigation().moveTo(owner, speed);
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
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        UncannyEntityUtil.suppressStepSound(this, pos, blockState);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        UncannyEntityUtil.dropPulseStyleRewards(level, this, this.random);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(OWNER_PLAYER).ifPresent(uuid -> tag.putUUID("OwnerPlayer", uuid));
        tag.putLong("EndTick", this.endTick);
        tag.putBoolean("Fleeing", this.fleeing);
        tag.putLong("VanishAtTick", this.vanishAtTick);
        tag.putBoolean("Sinking", this.sinking);
        tag.putLong("SinkEndTick", this.sinkEndTick);
        tag.putBoolean("Attacking", this.attacking);
        tag.putLong("AttackEndTick", this.attackEndTick);
        tag.putInt("MeleeCooldownTicks", this.meleeCooldownTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerPlayer")) {
            this.entityData.set(OWNER_PLAYER, Optional.of(tag.getUUID("OwnerPlayer")));
        }
        this.endTick = tag.getLong("EndTick");
        this.fleeing = tag.getBoolean("Fleeing");
        this.vanishAtTick = tag.contains("VanishAtTick") ? tag.getLong("VanishAtTick") : Long.MIN_VALUE;
        this.sinking = tag.getBoolean("Sinking");
        this.sinkEndTick = tag.contains("SinkEndTick") ? tag.getLong("SinkEndTick") : Long.MIN_VALUE;
        this.attacking = tag.getBoolean("Attacking");
        this.attackEndTick = tag.contains("AttackEndTick") ? tag.getLong("AttackEndTick") : Long.MIN_VALUE;
        this.meleeCooldownTicks = Math.max(0, tag.getInt("MeleeCooldownTicks"));
    }

    private ServerPlayer resolveOwner(ServerLevel level) {
        Optional<UUID> ownerUuid = this.entityData.get(OWNER_PLAYER);
        if (ownerUuid.isPresent()) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUuid.get());
            if (owner != null) {
                return owner;
            }
        }
        Player nearest = level.getNearestPlayer(this, 30.0D);
        if (nearest instanceof ServerPlayer owner) {
            this.entityData.set(OWNER_PLAYER, Optional.of(owner.getUUID()));
            return owner;
        }
        return null;
    }

    private boolean isBroadlyVisibleTo(ServerPlayer owner) {
        if (!owner.hasLineOfSight(this)) {
            return false;
        }
        Vec3 toEntity = this.getEyePosition().subtract(owner.getEyePosition());
        if (toEntity.lengthSqr() < 0.0001D) {
            return true;
        }
        Vec3 look = owner.getViewVector(1.0F).normalize();
        return look.dot(toEntity.normalize()) > 0.05D;
    }

    private void startSinking(long now, long durationTicks) {
        if (this.sinking) {
            return;
        }
        this.attacking = false;
        this.fleeing = false;
        this.sinking = true;
        this.sinkEndTick = now + Math.max(20L, durationTicks);
        this.getNavigation().stop();
        this.setTarget(null);
    }
}
