package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.diagnostics.DiagnosticSeverity;
import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.event.special.ApprovedSpecialBehaviorRules;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

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
    private boolean everBroadlyVisibleToOwner;
    private boolean terminalDiagnosticRecorded;
    private long ownerUnavailableSinceTick = Long.MIN_VALUE;
    private int evasiveRepositionsRemaining = ApprovedSpecialBehaviorRules.FOLLOWER_INITIAL_REPOSITIONS;
    private boolean evasiveRepositionArmed;
    private long evasiveBurstEndTick = Long.MIN_VALUE;
    private long unobservedSinceTick = Long.MIN_VALUE;
    private long nextEvasiveRepositionTick = Long.MIN_VALUE;
    private long attackSuppressedUntilTick = Long.MIN_VALUE;
    private boolean projectileRejectionRecorded;
    private int pursuitEvidenceTicks;
    private long pursuitArmedUntilTick = Long.MIN_VALUE;
    private Vec3 lastOwnerPosition;
    private double lastOwnerDistance = Double.NaN;
    private Vec3 pendingRepositionTarget;
    private Vec3 pendingRepositionFrom;
    private long pendingRepositionTeleportTick = Long.MIN_VALUE;
    private long revealAfterRepositionTick = Long.MIN_VALUE;
    private int ownerEntityId = -1;

    public UncannyFollowerEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        UncannyEntityUtil.applyDisplayName(this, "Follower?");
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FollowerSurfaceNavigation(this, level);
    }

    @Override
    public boolean canStandOnFluid(FluidState fluidState) {
        return fluidState.is(FluidTags.WATER);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (!this.sinking && isAtWalkableWaterSurface()) {
            // Ground movement must be selected before LivingEntity computes its
            // friction. Marking the support afterwards leaves the mob at the
            // airborne 0.02 movement factor even though water is its floor.
            this.setOnGround(true);
        }
        super.travel(travelVector);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_PLAYER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 20.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    public void setupFollower(ServerPlayer owner, long durationTicks) {
        this.setPersistenceRequired();
        this.entityData.set(OWNER_PLAYER, Optional.of(owner.getUUID()));
        this.ownerEntityId = owner.getId();
        this.endTick = owner.serverLevel().getGameTime() + Math.max(20L, durationTicks);
        this.fleeing = false;
        this.vanishAtTick = Long.MIN_VALUE;
        this.sinking = false;
        this.sinkEndTick = Long.MIN_VALUE;
        this.attacking = false;
        this.attackEndTick = Long.MIN_VALUE;
        this.meleeCooldownTicks = 0;
        this.everBroadlyVisibleToOwner = false;
        this.terminalDiagnosticRecorded = false;
        this.ownerUnavailableSinceTick = Long.MIN_VALUE;
        this.evasiveRepositionsRemaining = ApprovedSpecialBehaviorRules.FOLLOWER_INITIAL_REPOSITIONS;
        this.evasiveRepositionArmed = false;
        this.evasiveBurstEndTick = Long.MIN_VALUE;
        this.unobservedSinceTick = Long.MIN_VALUE;
        this.nextEvasiveRepositionTick = Long.MIN_VALUE;
        this.attackSuppressedUntilTick = Long.MIN_VALUE;
        this.projectileRejectionRecorded = false;
        this.pursuitEvidenceTicks = 0;
        this.pursuitArmedUntilTick = Long.MIN_VALUE;
        this.lastOwnerPosition = owner.position();
        this.lastOwnerDistance = this.distanceTo(owner);
        clearPendingReposition();
        this.setInvisible(false);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            if (!this.projectileRejectionRecorded && this.level() instanceof ServerLevel) {
                this.projectileRejectionRecorded = true;
                UncannyDiagnostics.recordSpecialLifecycle(
                        attacker instanceof ServerPlayer serverPlayer ? serverPlayer : null,
                        this,
                        "follower",
                        "defence",
                        "projectile_ignored",
                        DiagnosticSeverity.INFO,
                        UncannyDiagnostics.fields("damage_type", source.getMsgId()));
            }
            return false;
        }

        boolean directPlayerMelee = attacker instanceof Player && source.getDirectEntity() == attacker;
        float appliedDamage = directPlayerMelee
                ? ApprovedSpecialBehaviorRules.followerPlayerMeleeDamage(amount)
                : amount;
        boolean hurt = super.hurt(source, appliedDamage);
        if (hurt && directPlayerMelee && !this.isDeadOrDying()) {
            this.getNavigation().stop();
            this.setTarget(null);
            this.attacking = false;
            long now = this.level() instanceof ServerLevel serverLevel
                    ? serverLevel.getGameTime()
                    : this.tickCount;
            armEvasion(now, "direct_melee");
            this.attackSuppressedUntilTick = Math.max(
                    this.attackSuppressedUntilTick,
                    now + ApprovedSpecialBehaviorRules.FOLLOWER_POST_REPOSITION_ATTACK_GRACE_TICKS);
            UncannyDiagnostics.recordSpecialLifecycle(
                    attacker instanceof ServerPlayer serverPlayer ? serverPlayer : null,
                    this,
                    "follower",
                    "transition",
                    "melee_hit_evaded",
                    DiagnosticSeverity.INFO,
                    UncannyDiagnostics.fields(
                            "requested_damage", amount,
                            "applied_damage", appliedDamage,
                            "repositions_remaining", this.evasiveRepositionsRemaining));
        }
        return hurt;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.sinking) {
            floatOnWaterSurface();
        }
        UncannyEntityUtil.forceSilent(this);
        UncannyEntityUtil.enableDoorNavigation(this);
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel level)) {
            return;
        }

        long now = level.getGameTime();
        ServerPlayer owner = resolveOwner(level);
        if (owner == null) {
            this.setTarget(null);
            this.getNavigation().stop();
            if (this.ownerUnavailableSinceTick == Long.MIN_VALUE) {
                this.ownerUnavailableSinceTick = now;
                UncannyDiagnostics.recordSpecialLifecycle(
                        null,
                        this,
                        "follower",
                        "transition",
                        "owner_temporarily_unavailable",
                        DiagnosticSeverity.INFO,
                        UncannyDiagnostics.fields("grace_ticks", 100));
            } else if (now - this.ownerUnavailableSinceTick >= 100L) {
                discardWithReason(null, "owner_unavailable_after_grace", now);
            }
            return;
        }
        this.ownerUnavailableSinceTick = Long.MIN_VALUE;
        if (!owner.isAlive()) {
            discardWithReason(owner, "owner_not_alive", now);
            return;
        }
        if (owner.serverLevel() != level) {
            discardWithReason(owner, "owner_changed_dimension", now);
            return;
        }
        if (!ApprovedSpecialBehaviorRules.followerOwnerWithinTrackingRange(this.distanceToSqr(owner))) {
            discardWithReason(owner, "owner_beyond_encounter_range", now);
            return;
        }
        if (this.meleeCooldownTicks > 0) {
            this.meleeCooldownTicks--;
        }

        if (this.tickCount % 20 == 0) {
            recordSurfaceNavigationSample(owner);
        }

        if (tickRepositionCloak(level, owner, now)) {
            return;
        }

        if (this.sinking) {
            this.setTarget(null);
            this.setNoGravity(true);
            this.noPhysics = true;
            this.getNavigation().stop();
            double step = UncannySinkTransition.step(this, 0.045D, 38);
            this.setDeltaMovement(0.0D, -step, 0.0D);
            this.setPos(this.getX(), this.getY() - step, this.getZ());
            if (now >= this.sinkEndTick || UncannySinkTransition.breaksIntoOpenSpace(this)) {
                UncannySinkTransition.effects(this);
                discardWithReason(owner, "sinking_complete", now);
            }
            return;
        }
        this.setNoGravity(false);
        this.noPhysics = false;

        if (this.vanishAtTick != Long.MIN_VALUE && now >= this.vanishAtTick) {
            discardWithReason(owner, "vanish_deadline", now);
            return;
        }

        if (!this.fleeing && !this.attacking && now >= this.endTick) {
            this.fleeing = true;
            UncannyDiagnostics.recordSpecialLifecycle(
                    owner,
                    this,
                    "follower",
                    "transition",
                    "duration_complete_fleeing",
                    DiagnosticSeverity.INFO,
                    UncannyDiagnostics.fields("encounter_age_ticks", this.tickCount));
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
            if (this.distanceToSqr(owner) > 34.0D * 34.0D) {
                discardWithReason(owner, "fled_beyond_range", now);
            } else if (!hasVisualLineOfSight(owner, this.getEyePosition())) {
                discardWithReason(owner, "fled_out_of_sight", now);
            }
            return;
        }

        this.setTarget(null);
        this.lookAt(owner, 60.0F, 60.0F);
        this.getLookControl().setLookAt(owner.getX(), owner.getEyeY(), owner.getZ(), 60.0F, 60.0F);
        double distanceSqr = this.distanceToSqr(owner);
        double distance = Math.sqrt(distanceSqr);

        boolean visibleToOwner = isBroadlyVisibleTo(owner);
        if (visibleToOwner && !this.everBroadlyVisibleToOwner) {
            this.everBroadlyVisibleToOwner = true;
            UncannyDiagnostics.recordSpecialLifecycle(
                    owner,
                    this,
                    "follower",
                    "observable",
                    "server_line_of_sight_and_view_cone",
                    DiagnosticSeverity.INFO,
                    UncannyDiagnostics.fields("encounter_age_ticks", this.tickCount));
        }

        boolean visibleToAnyPlayer = isBroadlyVisibleToAnyPlayer(level);
        if (visibleToAnyPlayer) {
            this.unobservedSinceTick = Long.MIN_VALUE;
        } else if (this.unobservedSinceTick == Long.MIN_VALUE) {
            this.unobservedSinceTick = now;
        }

        updatePursuitEvidence(owner, visibleToOwner, distance, now);

        if (this.evasiveRepositionArmed && now > this.pursuitArmedUntilTick) {
            clearEvasionIntent(now);
        }

        long unobservedTicks = this.unobservedSinceTick == Long.MIN_VALUE
                ? 0L
                : now - this.unobservedSinceTick;
        if (ApprovedSpecialBehaviorRules.followerCanAttemptReposition(
                this.evasiveRepositionsRemaining,
                this.evasiveRepositionArmed,
                visibleToAnyPlayer,
                unobservedTicks,
                now,
                this.nextEvasiveRepositionTick)) {
            if (beginEvasiveReposition(level, owner, now)) {
                return;
            }
            this.nextEvasiveRepositionTick = now + ApprovedSpecialBehaviorRules.FOLLOWER_REPOSITION_RETRY_TICKS;
        }

        if (this.evasiveRepositionArmed
                && visibleToOwner
                && distance >= ApprovedSpecialBehaviorRules.FOLLOWER_EVASION_RELEASE_DISTANCE
                && now >= this.evasiveBurstEndTick) {
            clearEvasionIntent(now);
        }

        if (this.evasiveRepositionArmed && now < this.evasiveBurstEndTick) {
            moveAwayFrom(owner,
                    ApprovedSpecialBehaviorRules.FOLLOWER_EVASION_ANCHOR_DISTANCE,
                    ApprovedSpecialBehaviorRules.FOLLOWER_EVASION_SPEED);
            return;
        }

        if (!visibleToOwner) {
            if (ApprovedSpecialBehaviorRules.followerCanStartUnseenAttack(
                    distance, now, this.attackSuppressedUntilTick)) {
                this.attacking = true;
                this.attackEndTick = now + 20L * 16L;
                this.setTarget(owner);
                this.getNavigation().stop();
                UncannyDiagnostics.recordSpecialLifecycle(
                        owner,
                        this,
                        "follower",
                        "transition",
                        "unseen_attack_started",
                        DiagnosticSeverity.INFO,
                        UncannyDiagnostics.fields("encounter_age_ticks", this.tickCount));
                level.playSound(null, this.blockPosition(), UncannySoundRegistry.UNCANNY_HURLER_SCREAM.get(), SoundSource.HOSTILE, 1.05F, 0.88F);
                return;
            }

            moveTowardOwnerRear(owner, distance);
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
        tag.putBoolean("EverBroadlyVisibleToOwner", this.everBroadlyVisibleToOwner);
        tag.putInt("EvasiveRepositionsRemaining", this.evasiveRepositionsRemaining);
        tag.putBoolean("EvasiveRepositionArmed", this.evasiveRepositionArmed);
        tag.putLong("EvasiveBurstEndTick", this.evasiveBurstEndTick);
        tag.putLong("NextEvasiveRepositionTick", this.nextEvasiveRepositionTick);
        tag.putLong("AttackSuppressedUntilTick", this.attackSuppressedUntilTick);
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
        this.everBroadlyVisibleToOwner = tag.getBoolean("EverBroadlyVisibleToOwner");
        this.evasiveRepositionsRemaining = tag.contains("EvasiveRepositionsRemaining")
                ? Mth.clamp(tag.getInt("EvasiveRepositionsRemaining"), 0,
                        ApprovedSpecialBehaviorRules.FOLLOWER_INITIAL_REPOSITIONS)
                : ApprovedSpecialBehaviorRules.FOLLOWER_INITIAL_REPOSITIONS;
        // A restart must never turn an old, unseen state into a fresh teleport.
        // Re-arming requires a new, directly observed pursuit in the current session.
        this.evasiveRepositionArmed = false;
        this.evasiveBurstEndTick = Long.MIN_VALUE;
        this.nextEvasiveRepositionTick = tag.contains("NextEvasiveRepositionTick")
                ? tag.getLong("NextEvasiveRepositionTick")
                : Long.MIN_VALUE;
        this.attackSuppressedUntilTick = tag.contains("AttackSuppressedUntilTick")
                ? tag.getLong("AttackSuppressedUntilTick")
                : Long.MIN_VALUE;
        this.unobservedSinceTick = Long.MIN_VALUE;
        this.projectileRejectionRecorded = false;
        this.terminalDiagnosticRecorded = false;
        this.ownerUnavailableSinceTick = Long.MIN_VALUE;
        this.pursuitEvidenceTicks = 0;
        this.pursuitArmedUntilTick = Long.MIN_VALUE;
        this.lastOwnerPosition = null;
        this.lastOwnerDistance = Double.NaN;
        this.ownerEntityId = -1;
        resetRepositionCloak();
    }

    private ServerPlayer resolveOwner(ServerLevel level) {
        Optional<UUID> ownerUuid = this.entityData.get(OWNER_PLAYER);
        if (this.ownerEntityId >= 0
                && level.getEntity(this.ownerEntityId) instanceof ServerPlayer runtimeOwner
                && ownerUuid.filter(runtimeOwner.getUUID()::equals).isPresent()) {
            return runtimeOwner;
        }
        if (ownerUuid.isPresent()) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUuid.get());
            if (owner != null) {
                this.ownerEntityId = owner.getId();
                return owner;
            }
        }
        Player nearest = level.getNearestPlayer(this, 30.0D);
        if (nearest instanceof ServerPlayer owner) {
            this.entityData.set(OWNER_PLAYER, Optional.of(owner.getUUID()));
            this.ownerEntityId = owner.getId();
            return owner;
        }
        return null;
    }

    private boolean isBroadlyVisibleTo(ServerPlayer owner) {
        Vec3 toEntity = this.getEyePosition().subtract(owner.getEyePosition());
        if (toEntity.lengthSqr() < 0.0001D) {
            return true;
        }
        Vec3 look = owner.getViewVector(1.0F).normalize();
        return look.dot(toEntity.normalize()) > 0.05D
                && hasVisualLineOfSight(owner, this.getEyePosition());
    }

    private boolean hasVisualLineOfSight(ServerPlayer observer, Vec3 target) {
        if (!ApprovedSpecialBehaviorRules.followerOwnerWithinTrackingRange(
                observer.getEyePosition().distanceToSqr(target))) {
            return false;
        }
        return observer.level().clip(new ClipContext(
                observer.getEyePosition(),
                target,
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                observer)).getType() == HitResult.Type.MISS;
    }

    private boolean isBroadlyVisibleToAnyPlayer(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive()
                    || player.isSpectator()
                    || player.distanceToSqr(this) > ApprovedSpecialBehaviorRules.FOLLOWER_OBSERVER_RANGE
                            * ApprovedSpecialBehaviorRules.FOLLOWER_OBSERVER_RANGE) {
                continue;
            }
            if (isBroadlyVisibleTo(player)) {
                return true;
            }
        }
        return false;
    }

    private void armEvasion(long now, String reason) {
        boolean newlyArmed = !this.evasiveRepositionArmed;
        this.evasiveRepositionArmed = true;
        this.evasiveBurstEndTick = Math.max(
                this.evasiveBurstEndTick,
                now + ApprovedSpecialBehaviorRules.FOLLOWER_EVASION_BURST_TICKS);
        this.pursuitArmedUntilTick = Math.max(
                this.pursuitArmedUntilTick,
                now + ApprovedSpecialBehaviorRules.FOLLOWER_PURSUIT_ARM_LIFETIME_TICKS);
        if (newlyArmed && this.level() instanceof ServerLevel level) {
            UncannyDiagnostics.recordSpecialLifecycle(
                    resolveOwner(level),
                    this,
                    "follower",
                    "transition",
                    "pursuit_evasion_armed",
                    DiagnosticSeverity.INFO,
                    UncannyDiagnostics.fields(
                            "reason", reason,
                            "repositions_remaining", this.evasiveRepositionsRemaining));
        }
    }

    private void updatePursuitEvidence(
            ServerPlayer owner,
            boolean visibleToOwner,
            double distance,
            long now) {
        Vec3 ownerPosition = owner.position();
        double forwardProgress = 0.0D;
        double closingDistance = 0.0D;
        if (this.lastOwnerPosition != null && !Double.isNaN(this.lastOwnerDistance)) {
            Vec3 ownerMotion = ownerPosition.subtract(this.lastOwnerPosition);
            Vec3 toFollower = this.position().subtract(ownerPosition);
            ownerMotion = new Vec3(ownerMotion.x, 0.0D, ownerMotion.z);
            toFollower = new Vec3(toFollower.x, 0.0D, toFollower.z);
            if (toFollower.lengthSqr() > 0.0001D) {
                forwardProgress = ownerMotion.dot(toFollower.normalize());
            }
            closingDistance = this.lastOwnerDistance - distance;
        }
        this.lastOwnerPosition = ownerPosition;
        this.lastOwnerDistance = distance;

        if (ApprovedSpecialBehaviorRules.followerPursuitEvidence(
                visibleToOwner,
                distance,
                forwardProgress,
                closingDistance)) {
            this.pursuitEvidenceTicks = Math.min(
                    ApprovedSpecialBehaviorRules.FOLLOWER_PURSUIT_REQUIRED_TICKS,
                    this.pursuitEvidenceTicks + 1);
            if (this.pursuitEvidenceTicks >= ApprovedSpecialBehaviorRules.FOLLOWER_PURSUIT_REQUIRED_TICKS) {
                armEvasion(now, "owner_pursuit");
            }
        } else if (visibleToOwner) {
            this.pursuitEvidenceTicks = Math.max(0, this.pursuitEvidenceTicks - 1);
        } else if (!this.evasiveRepositionArmed) {
            this.pursuitEvidenceTicks = 0;
        }
    }

    private void clearEvasionIntent(long now) {
        this.evasiveRepositionArmed = false;
        this.evasiveBurstEndTick = Long.MIN_VALUE;
        this.pursuitArmedUntilTick = Long.MIN_VALUE;
        this.pursuitEvidenceTicks = 0;
        this.nextEvasiveRepositionTick = Math.max(
                this.nextEvasiveRepositionTick,
                now + ApprovedSpecialBehaviorRules.FOLLOWER_REPOSITION_COOLDOWN_TICKS);
    }

    private void moveAwayFrom(ServerPlayer owner, double distance, double speed) {
        Vec3 away = this.position().subtract(owner.position());
        away = new Vec3(away.x, 0.0D, away.z);
        if (away.lengthSqr() < 0.0001D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            away = away.normalize();
        }
        Vec3 target = owner.position().add(away.scale(distance));
        this.getNavigation().moveTo(target.x, target.y, target.z, speed);
    }

    private void moveTowardOwnerRear(ServerPlayer owner, double distance) {
        Vec3 look = owner.getViewVector(1.0F);
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 0.0001D) {
            horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontalLook = horizontalLook.normalize();
        }
        Vec3 rear = owner.position().subtract(
                horizontalLook.scale(ApprovedSpecialBehaviorRules.FOLLOWER_REAR_APPROACH_DISTANCE));
        double speed = distance > 14.0D
                ? ApprovedSpecialBehaviorRules.FOLLOWER_UNSEEN_FAR_SPEED
                : ApprovedSpecialBehaviorRules.FOLLOWER_UNSEEN_NEAR_SPEED;
        if (!this.getNavigation().moveTo(rear.x, rear.y, rear.z, speed)) {
            this.getNavigation().moveTo(owner, speed);
        }
    }

    private boolean beginEvasiveReposition(ServerLevel level, ServerPlayer owner, long now) {
        Vec3 ownerLook = owner.getViewVector(1.0F);
        Vec3 horizontalLook = new Vec3(ownerLook.x, 0.0D, ownerLook.z);
        if (horizontalLook.lengthSqr() < 0.0001D) {
            horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontalLook = horizontalLook.normalize();
        }
        double behindAngle = Math.atan2(-horizontalLook.z, -horizontalLook.x);

        for (int attempt = 0; attempt < ApprovedSpecialBehaviorRules.FOLLOWER_REPOSITION_ATTEMPTS; attempt++) {
            double angle = behindAngle + (this.random.nextDouble() - 0.5D) * Math.toRadians(130.0D);
            double distance = ApprovedSpecialBehaviorRules.FOLLOWER_REPOSITION_MIN_DISTANCE
                    + this.random.nextDouble() * ApprovedSpecialBehaviorRules.FOLLOWER_REPOSITION_DISTANCE_SPAN;
            double x = owner.getX() + Math.cos(angle) * distance;
            double y = owner.getY() + this.random.nextInt(5) - 2;
            double z = owner.getZ() + Math.sin(angle) * distance;
            Vec3 candidate = new Vec3(x, y, z);
            BlockPos candidateBlock = BlockPos.containing(candidate);
            if (!level.hasChunkAt(candidateBlock) || !isCandidateOutsideEveryPlayerView(level, candidate)) {
                continue;
            }

            this.pendingRepositionFrom = this.position();
            this.pendingRepositionTarget = candidate;
            this.pendingRepositionTeleportTick = now
                    + ApprovedSpecialBehaviorRules.FOLLOWER_REPOSITION_TELEPORT_DELAY_TICKS;
            this.revealAfterRepositionTick = now
                    + ApprovedSpecialBehaviorRules.FOLLOWER_REPOSITION_CLOAK_TICKS;
            this.setInvisible(true);
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
            UncannyDiagnostics.recordSpecialLifecycle(
                    owner,
                    this,
                    "follower",
                    "transition",
                    "reposition_cloaked",
                    DiagnosticSeverity.INFO,
                    UncannyDiagnostics.fields(
                            "cloak_ticks", ApprovedSpecialBehaviorRules.FOLLOWER_REPOSITION_CLOAK_TICKS,
                            "teleport_delay_ticks", ApprovedSpecialBehaviorRules.FOLLOWER_REPOSITION_TELEPORT_DELAY_TICKS,
                            "repositions_remaining", this.evasiveRepositionsRemaining));
            return true;
        }
        return false;
    }

    private boolean tickRepositionCloak(ServerLevel level, ServerPlayer owner, long now) {
        if (this.pendingRepositionTarget != null) {
            this.getNavigation().stop();
            this.setTarget(null);
            this.setDeltaMovement(Vec3.ZERO);
            if (now < this.pendingRepositionTeleportTick) {
                return true;
            }

            Vec3 target = this.pendingRepositionTarget;
            Vec3 from = this.pendingRepositionFrom == null ? this.position() : this.pendingRepositionFrom;
            if (!this.randomTeleport(target.x, target.y, target.z, false)) {
                resetRepositionCloak();
                this.nextEvasiveRepositionTick = now + ApprovedSpecialBehaviorRules.FOLLOWER_REPOSITION_RETRY_TICKS;
                return false;
            }

            this.pendingRepositionTarget = null;
            this.pendingRepositionFrom = null;
            this.pendingRepositionTeleportTick = Long.MIN_VALUE;
            this.evasiveRepositionsRemaining--;
            this.evasiveRepositionArmed = false;
            this.evasiveBurstEndTick = Long.MIN_VALUE;
            this.pursuitArmedUntilTick = Long.MIN_VALUE;
            this.pursuitEvidenceTicks = 0;
            this.unobservedSinceTick = Long.MIN_VALUE;
            this.nextEvasiveRepositionTick = now + ApprovedSpecialBehaviorRules.FOLLOWER_REPOSITION_COOLDOWN_TICKS;
            this.attackSuppressedUntilTick = Math.max(
                    this.attackSuppressedUntilTick,
                    now + ApprovedSpecialBehaviorRules.FOLLOWER_POST_REPOSITION_ATTACK_GRACE_TICKS);
            this.attacking = false;
            UncannyDiagnostics.recordSpecialLifecycle(
                    owner,
                    this,
                    "follower",
                    "transition",
                    "offscreen_repositioned",
                    DiagnosticSeverity.INFO,
                    UncannyDiagnostics.fields(
                            "from_x", from.x,
                            "from_y", from.y,
                            "from_z", from.z,
                            "to_x", this.getX(),
                            "to_y", this.getY(),
                            "to_z", this.getZ(),
                            "repositions_remaining", this.evasiveRepositionsRemaining));
            return true;
        }

        if (this.revealAfterRepositionTick != Long.MIN_VALUE) {
            if (now < this.revealAfterRepositionTick) {
                this.getNavigation().stop();
                this.setDeltaMovement(Vec3.ZERO);
                return true;
            }
            this.setInvisible(false);
            this.revealAfterRepositionTick = Long.MIN_VALUE;
        }
        return false;
    }

    private void clearPendingReposition() {
        this.pendingRepositionTarget = null;
        this.pendingRepositionFrom = null;
        this.pendingRepositionTeleportTick = Long.MIN_VALUE;
        this.revealAfterRepositionTick = Long.MIN_VALUE;
    }

    private void resetRepositionCloak() {
        clearPendingReposition();
        this.setInvisible(false);
    }

    private boolean isCandidateOutsideEveryPlayerView(ServerLevel level, Vec3 candidate) {
        Vec3 candidateEye = candidate.add(0.0D, this.getEyeHeight(), 0.0D);
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            Vec3 toCandidate = candidateEye.subtract(player.getEyePosition());
            double distanceSqr = toCandidate.lengthSqr();
            if (distanceSqr < ApprovedSpecialBehaviorRules.FOLLOWER_EVASION_TRIGGER_DISTANCE
                    * ApprovedSpecialBehaviorRules.FOLLOWER_EVASION_TRIGGER_DISTANCE) {
                return false;
            }
            if (distanceSqr <= ApprovedSpecialBehaviorRules.FOLLOWER_OBSERVER_RANGE
                            * ApprovedSpecialBehaviorRules.FOLLOWER_OBSERVER_RANGE
                    && player.getViewVector(1.0F).normalize().dot(toCandidate.normalize()) > 0.05D
                    && hasVisualLineOfSight(player, candidateEye)) {
                return false;
            }
        }
        return true;
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

    private void floatOnWaterSurface() {
        BlockPos feet = this.blockPosition();
        BlockPos waterPos;
        FluidState fluid = this.level().getFluidState(feet);
        if (fluid.is(FluidTags.WATER)) {
            waterPos = feet;
        } else {
            waterPos = feet.below();
            fluid = this.level().getFluidState(waterPos);
        }
        if (!fluid.is(FluidTags.WATER)) {
            return;
        }

        boolean surfaceLayer = !this.level().getFluidState(waterPos.above()).is(FluidTags.WATER);
        double surfaceY = waterPos.getY() + fluid.getHeight(this.level(), waterPos);
        Vec3 movement = this.getDeltaMovement();
        CollisionContext collision = CollisionContext.of(this);
        if (surfaceLayer && (this.getY() >= surfaceY - 0.05D
                || collision.isAbove(LiquidBlock.STABLE_SHAPE, waterPos, true))) {
            this.setOnGround(true);
            this.setDeltaMovement(movement.x, Mth.clamp(movement.y, -0.02D, 0.04D), movement.z);
        } else {
            double vertical = Mth.clamp(movement.y * 0.5D + 0.08D, -0.04D, 0.14D);
            // Water is the Follower?'s walkable surface. Damping X/Z here used to
            // erase the navigation controller's movement every tick at a bank.
            this.setDeltaMovement(movement.x, vertical, movement.z);
        }
    }

    private boolean isAtWalkableWaterSurface() {
        BlockPos feet = this.blockPosition();
        FluidState fluidAtFeet = this.level().getFluidState(feet);
        if (fluidAtFeet.is(FluidTags.WATER)) {
            return !this.level().getFluidState(feet.above()).is(FluidTags.WATER);
        }
        return this.level().getFluidState(feet.below()).is(FluidTags.WATER)
                && !this.level().getFluidState(feet).is(FluidTags.WATER);
    }

    private void recordSurfaceNavigationSample(ServerPlayer owner) {
        BlockPos feet = this.blockPosition();
        BlockPos target = this.getNavigation().getTargetPos();
        net.minecraft.world.level.pathfinder.Path path = this.getNavigation().getPath();
        BlockPos next = path == null || path.isDone() ? null : path.getNextNodePos();
        Vec3 movement = this.getDeltaMovement();
        Vec3 toFollower = this.getEyePosition().subtract(owner.getEyePosition()).normalize();
        double ownerViewDot = owner.getViewVector(1.0F).normalize().dot(toFollower);
        UncannyDiagnostics.recordSpecialLifecycle(
                owner,
                this,
                "follower",
                "surface_navigation",
                "physical_water_surface_sample",
                DiagnosticSeverity.INFO,
                UncannyDiagnostics.fields(
                        "in_water", this.isInWater(),
                        "under_water", this.isUnderWater(),
                        "water_at_feet", this.level().getFluidState(feet).is(FluidTags.WATER),
                        "water_below", this.level().getFluidState(feet.below()).is(FluidTags.WATER),
                        "velocity_x", movement.x,
                        "velocity_y", movement.y,
                        "velocity_z", movement.z,
                        "owner_view_dot", ownerViewDot,
                        "target_x", target == null ? Integer.MIN_VALUE : target.getX(),
                        "target_y", target == null ? Integer.MIN_VALUE : target.getY(),
                        "target_z", target == null ? Integer.MIN_VALUE : target.getZ(),
                        "next_x", next == null ? Integer.MIN_VALUE : next.getX(),
                        "next_y", next == null ? Integer.MIN_VALUE : next.getY(),
                        "next_z", next == null ? Integer.MIN_VALUE : next.getZ()));
    }

    private static final class FollowerSurfaceNavigation extends GroundPathNavigation {
        private FollowerSurfaceNavigation(UncannyFollowerEntity follower, Level level) {
            super(follower, level);
        }

        @Override
        protected PathFinder createPathFinder(int maximumVisitedNodes) {
            this.nodeEvaluator = new WalkNodeEvaluator();
            this.nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(this.nodeEvaluator, maximumVisitedNodes);
        }

        @Override
        protected boolean hasValidPathType(PathType pathType) {
            return pathType == PathType.WATER || pathType == PathType.WATER_BORDER
                    || super.hasValidPathType(pathType);
        }

        @Override
        public boolean isStableDestination(BlockPos pos) {
            FluidState fluid = this.level.getFluidState(pos);
            if (fluid.is(FluidTags.WATER)
                    && !this.level.getFluidState(pos.above()).is(FluidTags.WATER)
                    && this.level.getBlockState(pos.above()).getCollisionShape(this.level, pos.above()).isEmpty()) {
                return true;
            }
            return super.isStableDestination(pos);
        }
    }

    private void discardWithReason(ServerPlayer owner, String reason, long now) {
        if (!this.terminalDiagnosticRecorded) {
            this.terminalDiagnosticRecorded = true;
            UncannyDiagnostics.recordSpecialLifecycle(
                    owner,
                    this,
                    "follower",
                    "ended",
                    reason,
                    DiagnosticSeverity.INFO,
                    UncannyDiagnostics.fields(
                            "encounter_age_ticks", this.tickCount,
                            "remaining_duration_ticks", this.endTick == Long.MIN_VALUE
                                    ? Long.MIN_VALUE : this.endTick - now,
                            "ever_server_observable", this.everBroadlyVisibleToOwner,
                            "fleeing", this.fleeing,
                            "attacking", this.attacking,
                            "sinking", this.sinking,
                            "evasive_repositions_remaining", this.evasiveRepositionsRemaining,
                            "evasive_reposition_armed", this.evasiveRepositionArmed));
        }
        this.discard();
    }
}
