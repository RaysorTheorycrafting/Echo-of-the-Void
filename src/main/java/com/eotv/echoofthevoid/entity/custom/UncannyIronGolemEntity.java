package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.entity.variant.IronGolemVariantRules;
import com.eotv.echoofthevoid.phase.UncannyPhase;
import com.eotv.echoofthevoid.state.UncannyWorldState;
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
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Five bounded village-native anomalies replacing the former indiscriminate attacker. */
public class UncannyIronGolemEntity extends IronGolem implements UncannyEntityMarker {
    private static final EntityDataAccessor<Integer> IRON_GOLEM_VARIANT =
            SynchedEntityData.defineId(UncannyIronGolemEntity.class, EntityDataSerializers.INT);

    private boolean frozenByVariant;
    private BlockPos rememberedPatrolTarget;
    private int nextPatrolMemoryTick;
    private int boundaryAggroUntilTick;
    private UUID boundaryTargetUuid;

    public UncannyIronGolemEntity(EntityType<? extends IronGolem> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Iron Golem?");
        this.setPlayerCreated(false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IRON_GOLEM_VARIANT, IronGolemVariant.UNASSIGNED.id());
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor levelAccessor,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(levelAccessor, difficulty, spawnType, spawnGroupData);
        if (levelAccessor instanceof ServerLevel serverLevel) {
            ensureVariantAssigned(serverLevel);
        }
        return data;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (this.level() instanceof ServerLevel serverLevel) {
            ensureVariantAssigned(serverLevel);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ensureVariantAssigned(serverLevel);
        IronGolemVariant variant = getIronGolemVariant();
        // Variant movement can differ, but Vanilla hurt and crack-level feedback must remain audible.
        // Applying false every tick also repairs the Silent flag saved by older work builds.
        this.setSilent(false);
        this.frozenByVariant = false;

        switch (variant) {
            case STILL_WITNESS -> tickStillWitness(serverLevel);
            case REAR_GUARD -> tickRearGuard(serverLevel);
            case BORROWED_GAZE -> tickBorrowedGaze(serverLevel);
            case EMPTY_PATROL -> tickEmptyPatrol(serverLevel);
            case BOUNDARY_SENTINEL -> tickBoundarySentinel(serverLevel);
            default -> {
            }
        }
    }

    @Override
    protected boolean isImmobile() {
        return this.frozenByVariant || super.isImmobile();
    }

    @Override
    public boolean canSpawnSprintParticle() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Guarantee Vanilla impact and crack sounds even on the first tick after loading old Silent data.
        this.setSilent(false);
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof Player && !this.isPlayerCreated()) {
            // An actual aggressor is owned by Vanilla's anger system, not by variant 5's short proximity target.
            this.frozenByVariant = false;
            this.boundaryTargetUuid = null;
            this.boundaryAggroUntilTick = 0;
        }
        return hurt;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    public void setPlayerCreated(boolean playerCreated) {
        super.setPlayerCreated(playerCreated);
        if (playerCreated && getIronGolemVariant() == IronGolemVariant.BOUNDARY_SENTINEL) {
            setIronGolemVariant(IronGolemVariant.EMPTY_PATROL);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("UncannyIronGolemVariant", getIronGolemVariant().id());
        tag.putInt("UncannyIronGolemBoundaryAggro", Math.max(0, this.boundaryAggroUntilTick - this.tickCount));
        if (this.boundaryTargetUuid != null) {
            tag.putUUID("UncannyIronGolemBoundaryTarget", this.boundaryTargetUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setIronGolemVariant(IronGolemVariant.fromId(tag.getInt("UncannyIronGolemVariant")));
        this.boundaryAggroUntilTick = this.tickCount + Math.max(0, tag.getInt("UncannyIronGolemBoundaryAggro"));
        this.boundaryTargetUuid = tag.hasUUID("UncannyIronGolemBoundaryTarget")
                ? tag.getUUID("UncannyIronGolemBoundaryTarget")
                : null;
    }

    public IronGolemVariant getIronGolemVariant() {
        return IronGolemVariant.fromId(this.entityData.get(IRON_GOLEM_VARIANT));
    }

    public void setVariantForDebug(int variantId) {
        setIronGolemVariant(IronGolemVariant.fromId(variantId));
    }

    private void setIronGolemVariant(IronGolemVariant variant) {
        int allowedId = IronGolemVariantRules.allowedVariantForOrigin(variant.id(), this.isPlayerCreated());
        this.entityData.set(IRON_GOLEM_VARIANT, IronGolemVariant.fromId(allowedId).id());
    }

    private void ensureVariantAssigned(ServerLevel serverLevel) {
        IronGolemVariant current = getIronGolemVariant();
        if (current != IronGolemVariant.UNASSIGNED) {
            if (this.isPlayerCreated() && current == IronGolemVariant.BOUNDARY_SENTINEL) {
                setIronGolemVariant(IronGolemVariant.EMPTY_PATROL);
            }
            return;
        }
        UncannyPhase phase = UncannyWorldState.get(serverLevel.getServer()).getPhase();
        setIronGolemVariant(rollVariantForPhase(phase));
    }

    private IronGolemVariant rollVariantForPhase(UncannyPhase phase) {
        return IronGolemVariant.fromId(
                IronGolemVariantRules.variantIdForPhaseRoll(phase.index(), this.random.nextInt(100)));
    }

    private void tickStillWitness(ServerLevel level) {
        if (this.getTarget() != null) {
            return;
        }
        if (isDirectlyWatchedByAny(level, 28.0D, 0.91D)) {
            freezeInPlace();
        }
    }

    private void tickRearGuard(ServerLevel level) {
        if (this.getTarget() != null) {
            return;
        }
        ServerPlayer player = nearestEligiblePlayer(level, 32.0D);
        if (player == null) {
            return;
        }
        if (isDirectlyWatchedBy(player, 0.92D)) {
            freezeInPlace();
            return;
        }

        double distanceSqr = this.distanceToSqr(player);
        if (distanceSqr < 25.0D) {
            this.getNavigation().stop();
            return;
        }
        if (distanceSqr <= 18.0D * 18.0D && this.tickCount % 16 == 0) {
            Vec3 behind = player.position().subtract(player.getViewVector(1.0F).multiply(7.0D, 0.0D, 7.0D));
            this.getNavigation().moveTo(behind.x, player.getY(), behind.z, 0.72D);
        }
    }

    private void tickBorrowedGaze(ServerLevel level) {
        if (this.getTarget() != null) {
            return;
        }
        ServerPlayer player = nearestEligiblePlayer(level, 24.0D);
        if (player == null) {
            return;
        }
        this.getNavigation().stop();
        this.setYHeadRot(player.getYHeadRot());
        this.setXRot(Mth.clamp(player.getXRot(), -30.0F, 30.0F));
        if (isDirectlyWatchedBy(player, 0.94D)) {
            freezeInPlace();
        }
    }

    private void tickEmptyPatrol(ServerLevel level) {
        if (this.getTarget() != null) {
            return;
        }
        ServerPlayer player = nearestEligiblePlayer(level, 36.0D);
        if (player == null) {
            return;
        }
        if (isDirectlyWatchedBy(player, 0.92D)) {
            freezeInPlace();
            return;
        }

        if (this.rememberedPatrolTarget == null && this.tickCount >= this.nextPatrolMemoryTick) {
            this.rememberedPatrolTarget = player.blockPosition().immutable();
            this.nextPatrolMemoryTick = this.tickCount + 60 + this.random.nextInt(61);
            return;
        }
        if (this.rememberedPatrolTarget != null && this.tickCount >= this.nextPatrolMemoryTick) {
            if (this.blockPosition().distSqr(this.rememberedPatrolTarget) <= 5.0D) {
                this.getNavigation().stop();
                this.rememberedPatrolTarget = null;
                this.nextPatrolMemoryTick = this.tickCount + 80 + this.random.nextInt(81);
            } else if (this.tickCount % 20 == 0) {
                this.getNavigation().moveTo(
                        this.rememberedPatrolTarget.getX() + 0.5D,
                        this.rememberedPatrolTarget.getY(),
                        this.rememberedPatrolTarget.getZ() + 0.5D,
                        0.68D);
            }
        }
    }

    private void tickBoundarySentinel(ServerLevel level) {
        if (this.boundaryTargetUuid != null) {
            if (this.getTarget() instanceof Player player && this.boundaryTargetUuid.equals(player.getUUID())) {
                if (this.tickCount >= this.boundaryAggroUntilTick || !player.isAlive()) {
                    this.boundaryTargetUuid = null;
                    this.boundaryAggroUntilTick = 0;
                    this.setTarget(null);
                }
                return;
            }
            this.boundaryTargetUuid = null;
            this.boundaryAggroUntilTick = 0;
        }
        if (this.getTarget() != null) {
            // Preserve Vanilla targets, notably the player who actually attacked this golem.
            return;
        }
        if (this.isPlayerCreated()) {
            return;
        }

        ServerPlayer player = nearestEligiblePlayer(level, 30.0D);
        if (player == null) {
            return;
        }
        double distanceSqr = this.distanceToSqr(player);
        if (distanceSqr <= 2.6D * 2.6D) {
            this.setTarget(player);
            this.boundaryTargetUuid = player.getUUID();
            this.boundaryAggroUntilTick = this.tickCount + 80;
            return;
        }
        if (isDirectlyWatchedBy(player, 0.92D)) {
            freezeInPlace();
            return;
        }
        if (distanceSqr <= 6.0D * 6.0D) {
            this.getNavigation().stop();
        } else if (distanceSqr <= 24.0D * 24.0D && this.tickCount % 12 == 0) {
            this.getNavigation().moveTo(player, 0.78D);
        }
    }

    private void freezeInPlace() {
        this.frozenByVariant = true;
        this.getNavigation().stop();
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(0.0D, Math.min(0.0D, movement.y), 0.0D);
    }

    @Nullable
    private ServerPlayer nearestEligiblePlayer(ServerLevel level, double radius) {
        Player nearest = level.getNearestPlayer(this, radius);
        if (nearest instanceof ServerPlayer player && player.isAlive() && !player.isSpectator() && !player.isCreative()) {
            return player;
        }
        return null;
    }

    private boolean isDirectlyWatchedByAny(ServerLevel level, double radius, double threshold) {
        double radiusSqr = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator()
                    && this.distanceToSqr(player) <= radiusSqr
                    && isDirectlyWatchedBy(player, threshold)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDirectlyWatchedBy(ServerPlayer player, double threshold) {
        if (!this.hasLineOfSight(player) || !player.hasLineOfSight(this)) {
            return false;
        }
        Vec3 toGolem = this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D)
                .subtract(player.getEyePosition())
                .normalize();
        return player.getViewVector(1.0F).normalize().dot(toGolem) > threshold;
    }

    public enum IronGolemVariant {
        UNASSIGNED(0),
        STILL_WITNESS(1),
        REAR_GUARD(2),
        BORROWED_GAZE(3),
        EMPTY_PATROL(4),
        BOUNDARY_SENTINEL(5);

        private final int id;

        IronGolemVariant(int id) {
            this.id = id;
        }

        public int id() {
            return this.id;
        }

        public static IronGolemVariant fromId(int id) {
            for (IronGolemVariant variant : values()) {
                if (variant.id == id) {
                    return variant;
                }
            }
            return UNASSIGNED;
        }
    }
}
