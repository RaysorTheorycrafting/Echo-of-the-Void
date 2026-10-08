package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.diagnostics.DiagnosticSeverity;
import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.event.special.HuntingSpecialRules;
import com.eotv.echoofthevoid.sound.UncannyPhysicalSoundDelivery;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Drowned navigation without its equipment, voice, targets or daylight rules. */
public abstract class AbstractUncannyAquaticSpecialEntity extends Drowned implements UncannyEntityMarker {
    private static final EntityDataAccessor<Optional<UUID>> FOCUS_PLAYER = SynchedEntityData.defineId(
            AbstractUncannyAquaticSpecialEntity.class,
            EntityDataSerializers.OPTIONAL_UUID);

    private int remainingLifetimeTicks = 20 * 300;
    private int unavailableTicks;
    protected int meleeCooldownTicks;
    private final PathNavigation uncannyAmphibiousNavigation;

    protected AbstractUncannyAquaticSpecialEntity(
            EntityType<? extends Drowned> type,
            Level level,
            String displayName) {
        super(type, level);
        // DrownedMoveControl adds an unconditional upward impulse while searching for land. That
        // behavior is useful to a Vanilla Drowned surfacing at night, but it made Drifter? and
        // Dredger? accelerate vertically instead of following their explicit three-dimensional
        // targets. Keep the robust amphibious node evaluator while separating that Vanilla policy.
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 1.0F, 0.95F, false);
        this.uncannyAmphibiousNavigation = new AmphibiousPathNavigation(this, level);
        this.navigation = this.uncannyAmphibiousNavigation;
        UncannyEntityUtil.applyDisplayName(this, displayName);
        this.xpReward = 0;
        this.setCanPickUpLoot(false);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FOCUS_PLAYER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        // FloatGoal continuously calls JumpControl in water. Together with Drowned's swimming
        // controller it was the second source of the unexplained vertical launch. These Specials
        // own their depth explicitly, so no generic surface-seeking goal belongs here.
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public final void setupTarget(ServerPlayer player, int lifetimeTicks) {
        this.entityData.set(FOCUS_PLAYER, Optional.of(player.getUUID()));
        this.remainingLifetimeTicks = Math.max(100, lifetimeTicks);
        this.unavailableTicks = 0;
        this.meleeCooldownTicks = 0;
        this.setPersistenceRequired();
        onTargetConfigured(player);
        refreshCombatParity(player, true);
    }

    protected void onTargetConfigured(ServerPlayer player) {
    }

    /** Called every tick of the death animation instead of the hunt. */
    protected void onDeathAnimation() {
    }

    /** Duel parity profile of this creature, or {@code null} when it does not fight. */
    protected com.eotv.echoofthevoid.event.special.CombatParityRules.Profile combatParity() {
        return null;
    }

    /** Re-reads the player's equipment; the first call of a hunt starts at full health. */
    protected final void refreshCombatParity(ServerPlayer player, boolean refill) {
        com.eotv.echoofthevoid.event.special.CombatParityRules.Profile profile = combatParity();
        if (profile != null) {
            com.eotv.echoofthevoid.event.special.CombatParity.apply(this, player, profile, refill);
        }
    }

    public final Optional<UUID> focusPlayerId() {
        return this.entityData.get(FOCUS_PLAYER);
    }

    @Override
    public void aiStep() {
        // These entities inherit Drowned only for its amphibious body/navigation. Their custom
        // EntityTypes are not present in every Vanilla breathing tag, which previously let both
        // Specials die from drowning after roughly 24 seconds in their required habitat.
        this.setAirSupply(this.getMaxAirSupply());
        Vec3 before = this.getDeltaMovement();
        this.setDeltaMovement(
                before.x,
                HuntingSpecialRules.boundedAquaticVerticalVelocity(before.y),
                before.z);
        super.aiStep();
        Vec3 movement = this.getDeltaMovement();
        double boundedY = HuntingSpecialRules.boundedAquaticVerticalVelocity(movement.y);
        if (boundedY != movement.y) {
            this.setDeltaMovement(movement.x, boundedY, movement.z);
        }
        this.setAirSupply(this.getMaxAirSupply());
        this.setSilent(true);
        this.setCanPickUpLoot(false);
        if (this.meleeCooldownTicks > 0) {
            this.meleeCooldownTicks--;
        }
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.isDeadOrDying()) {
            // A dying Dredger? lets go at once; nothing lands during the death animation.
            onDeathAnimation();
            return;
        }
        ServerPlayer focus = resolveFocus(level);
        if (focus == null) {
            this.setTarget(null);
            this.getNavigation().stop();
            if (++this.unavailableTicks >= 100) {
                discardWithTrace(null, "focus_unavailable");
            }
            return;
        }
        this.unavailableTicks = 0;
        if (!focus.isAlive() || focus.isSpectator() || focus.serverLevel() != level) {
            discardWithTrace(focus, "focus_invalid");
            return;
        }
        if (--this.remainingLifetimeTicks <= 0) {
            discardWithTrace(focus, "encounter_timeout");
            return;
        }
        if (this.tickCount % com.eotv.echoofthevoid.event.special.CombatParityRules.REFRESH_INTERVAL_TICKS == 0) {
            refreshCombatParity(focus, false);
        }
        if (this.tickCount % 40 == 0) {
            Vec3 velocity = this.getDeltaMovement();
            trace(focus, "movement_sample", "bounded_amphibious_navigation",
                    "velocity_x", velocity.x,
                    "velocity_y", velocity.y,
                    "velocity_z", velocity.z,
                    "occupies_water", occupiesWater(level),
                    "navigation_done", this.getNavigation().isDone());
        }
        tickAquaticSpecial(level, focus);
    }

    @Override
    public void updateSwimming() {
        if (!this.level().isClientSide && this.isEffectiveAi()) {
            this.navigation = this.uncannyAmphibiousNavigation;
            this.setSwimming(this.isInWater());
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        boolean waterCell = this.isInWaterOrBubble()
                || this.level().getFluidState(this.blockPosition()).is(FluidTags.WATER)
                || this.level().getFluidState(this.blockPosition().above()).is(FluidTags.WATER);
        if (this.isEffectiveAi() && waterCell) {
            // Drowned#travel adds its own buoyancy/surface policy after MoveControl. That second
            // policy used to consume the destination selected by Drifter?/Dredger? and made both
            // bodies sink or climb independently of their visible target. Move the already
            // bounded custom velocity exactly once, then apply gentle water drag.
            this.moveRelative(0.01F, travelVector);
            HuntingSpecialRules.HorizontalVelocity horizontal =
                    HuntingSpecialRules.boundedAquaticHorizontalVelocity(
                            this.getDeltaMovement().x,
                            this.getDeltaMovement().z);
            Vec3 movement = new Vec3(
                    horizontal.x(),
                    HuntingSpecialRules.boundedAquaticVerticalVelocity(this.getDeltaMovement().y),
                    horizontal.z());
            this.setDeltaMovement(movement);
            this.move(MoverType.SELF, movement);
            this.setDeltaMovement(
                    movement.x * 0.91D,
                    HuntingSpecialRules.boundedAquaticVerticalVelocity(movement.y * 0.91D),
                    movement.z * 0.91D);
            return;
        }
        super.travel(travelVector);
    }

    protected abstract void tickAquaticSpecial(ServerLevel level, ServerPlayer focus);

    protected final ServerPlayer resolveFocus(ServerLevel level) {
        return focusPlayerId().map(level.getServer().getPlayerList()::getPlayer).orElse(null);
    }

    protected final boolean occupiesWater(ServerLevel level) {
        return this.isInWaterOrBubble()
                || level.getFluidState(this.blockPosition()).is(FluidTags.WATER)
                || level.getFluidState(this.blockPosition().above()).is(FluidTags.WATER);
    }

    /**
     * Bounded three-dimensional steering for water. AmphibiousPathNavigation frequently reports
     * no path when both endpoints are valid open water at different depths; relying on it left a
     * Dredger motionless on the seabed and made Drifter's attack dependent on the player swimming
     * into it. This controller changes velocity gradually and never teleports or loads chunks.
     */
    protected final boolean steerInWaterToward(
            ServerLevel level,
            Vec3 destination,
            double maximumHorizontalSpeed,
            double maximumVerticalSpeed,
            double blend) {
        if (destination == null || !occupiesWater(level)) {
            return false;
        }
        this.getNavigation().stop();
        Vec3 offset = destination.subtract(this.position());
        double horizontalDistance = Math.hypot(offset.x, offset.z);
        Vec3 current = this.getDeltaMovement();
        double safeBlend = Math.max(0.02D, Math.min(1.0D, blend));
        double desiredX = horizontalDistance < 1.0E-5D
                ? 0.0D : offset.x / horizontalDistance * maximumHorizontalSpeed;
        double desiredZ = horizontalDistance < 1.0E-5D
                ? 0.0D : offset.z / horizontalDistance * maximumHorizontalSpeed;
        double nextX = current.x + (desiredX - current.x) * safeBlend;
        double nextZ = current.z + (desiredZ - current.z) * safeBlend;
        double nextHorizontal = Math.hypot(nextX, nextZ);
        if (nextHorizontal > maximumHorizontalSpeed) {
            double scale = maximumHorizontalSpeed / nextHorizontal;
            nextX *= scale;
            nextZ *= scale;
        }
        double desiredY = Math.max(
                -maximumVerticalSpeed,
                Math.min(maximumVerticalSpeed, offset.y * 0.10D));
        double nextY = current.y + (desiredY - current.y) * safeBlend;
        nextY = Math.max(-maximumVerticalSpeed, Math.min(maximumVerticalSpeed, nextY));
        this.setDeltaMovement(nextX, nextY, nextZ);
        this.hasImpulse = true;
        if (nextHorizontal > 0.01D) {
            float yaw = (float) (Math.toDegrees(Math.atan2(nextZ, nextX)) - 90.0D);
            this.setYRot(yaw);
            this.yBodyRot = yaw;
        }
        return true;
    }

    protected final void playPhysicalCue(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        UncannyPhysicalSoundDelivery.playFromEntity(
                level, this, sound, SoundSource.HOSTILE, volume, pitch);
    }

    protected final void trace(ServerPlayer focus, String event, String outcome, Object... keyValues) {
        UncannyDiagnostics.recordSpecialLifecycle(
                focus,
                this,
                specialId(),
                event,
                outcome,
                DiagnosticSeverity.INFO,
                UncannyDiagnostics.fields(keyValues));
    }

    protected final void discardWithTrace(ServerPlayer focus, String reason) {
        trace(focus, "removal", reason, "encounter_age_ticks", this.tickCount);
        this.discard();
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
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
    protected SoundEvent getStepSound() {
        return null;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return null;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        UncannyEntityUtil.suppressStepSound(this, pos, state);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        focusPlayerId().ifPresent(uuid -> tag.putUUID("AquaticFocusPlayer", uuid));
        tag.putInt("AquaticRemainingLifetime", this.remainingLifetimeTicks);
        tag.putInt("AquaticUnavailableTicks", this.unavailableTicks);
        tag.putInt("AquaticMeleeCooldown", this.meleeCooldownTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("AquaticFocusPlayer")) {
            this.entityData.set(FOCUS_PLAYER, Optional.of(tag.getUUID("AquaticFocusPlayer")));
        }
        this.remainingLifetimeTicks = tag.contains("AquaticRemainingLifetime")
                ? Math.max(1, tag.getInt("AquaticRemainingLifetime"))
                : tag.contains("AquaticMaximumLifetime")
                        ? Math.max(1, tag.getInt("AquaticMaximumLifetime"))
                        : 20 * 300;
        this.unavailableTicks = Math.max(0, tag.getInt("AquaticUnavailableTicks"));
        this.meleeCooldownTicks = Math.max(0, tag.getInt("AquaticMeleeCooldown"));
    }

    protected abstract String specialId();
}
