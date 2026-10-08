package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.event.special.HuntingSpecialRules;
import com.eotv.echoofthevoid.event.special.UncannyHuntingSpecialSystem;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Amphibious hunter whose own finite land reserve is a real, player-readable limitation. */
public class UncannyDrifterEntity extends AbstractUncannyAquaticSpecialEntity {
    private static final EntityDataAccessor<Byte> DRIFTER_STATE = SynchedEntityData.defineId(
            UncannyDrifterEntity.class,
            EntityDataSerializers.BYTE);

    private int dryTicks;
    private int waterRecoveryTicks;
    private int landPursuitTicks;
    private int stateTicks;
    private int nextLungeTicks;
    private int diveTicksRemaining;
    private int diveCooldownTicks;
    private boolean attackCryPlayed;

    public UncannyDrifterEntity(EntityType<? extends Drowned> type, Level level) {
        super(type, level, "Drifter?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DRIFTER_STATE, (byte) State.STALKING.id());
    }

    @Override
    protected void onTargetConfigured(ServerPlayer player) {
        setState(State.STALKING);
        this.dryTicks = 0;
        this.waterRecoveryTicks = 0;
        this.landPursuitTicks = 0;
        this.nextLungeTicks = 50 + this.random.nextInt(50);
        this.diveTicksRemaining = 0;
        this.diveCooldownTicks = 0;
        this.attackCryPlayed = false;
        trace(player, "spawn", "aquatic_stalk_started");
    }

    public State drifterState() {
        return State.byId(this.entityData.get(DRIFTER_STATE));
    }

    public void forceDryForDebug() {
        setState(State.LAND_PURSUIT);
        this.dryTicks = HuntingSpecialRules.DRIFTER_DRY_DAMAGE_START_TICKS - 20;
        this.waterRecoveryTicks = 0;
        this.landPursuitTicks = 0;
    }

    public void forceLungeForDebug() {
        setState(State.LUNGING);
        this.nextLungeTicks = 0;
        this.diveTicksRemaining = 0;
        this.diveCooldownTicks = 200;
        this.attackCryPlayed = false;
    }

    @Override
    protected void tickAquaticSpecial(ServerLevel level, ServerPlayer focus) {
        this.stateTicks++;
        updateDryReserve(level, focus);
        if (!this.isAlive()) {
            return;
        }
        if (this.distanceToSqr(focus) > 48.0D * 48.0D) {
            discardWithTrace(focus, "focus_beyond_range");
            return;
        }

        if (focus.getVehicle() instanceof Boat) {
            setState(State.RETURNING_TO_WATER);
            this.setTarget(null);
            Vec3 water = UncannyHuntingSpecialSystem.findNearestWater(level, this.blockPosition(), 16);
            if (water != null) {
                this.getNavigation().moveTo(water.x, water.y, water.z, 1.0D);
            } else {
                this.getNavigation().stop();
            }
            return;
        }

        if (!occupiesWater(level)) {
            tickLand(level, focus);
            return;
        }

        this.setTarget(focus);
        if (this.diveCooldownTicks > 0) {
            this.diveCooldownTicks--;
        }
        if (this.nextLungeTicks > 0) {
            this.nextLungeTicks--;
        }
        boolean focusOccupiesWater = focus.isInWaterOrBubble()
                || level.getFluidState(focus.blockPosition()).is(FluidTags.WATER)
                || level.getFluidState(focus.blockPosition().above()).is(FluidTags.WATER);
        if (!focusOccupiesWater) {
            if (this.distanceTo(focus) <= 6.0D
                    && UncannyHuntingSpecialSystem.isNearWater(level, focus.blockPosition(), 4)) {
                setState(State.LAND_PURSUIT);
                this.landPursuitTicks = 0;
                this.getNavigation().moveTo(focus, 1.0D);
                if (this.horizontalCollision
                        && focus.getY() > this.getY()
                        && focus.getY() - this.getY() <= 1.5D) {
                    this.getJumpControl().jump();
                }
            } else {
                setState(State.STALKING);
                patrolBelowTarget(level, focus);
            }
            return;
        }

        boolean observed = AbstractUncannyHuntingSpecialEntity.isDirectlyObserved(
                focus, this.getEyePosition(), 0.08D);
        if (drifterState() == State.DIVING && this.diveTicksRemaining > 0) {
            this.diveTicksRemaining--;
            Vec3 away = this.position().subtract(focus.position()).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() < 1.0E-4D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            }
            Vec3 dive = this.position().add(away.normalize().scale(3.0D)).add(0.0D, -1.6D, 0.0D);
            steerInWaterToward(level, dive, 0.17D, 0.08D, 0.24D);
            if (this.tickCount % 5 == 0) {
                level.sendParticles(ParticleTypes.BUBBLE, this.getX(), this.getEyeY(), this.getZ(),
                        3, 0.25D, 0.15D, 0.25D, 0.015D);
            }
            return;
        }
        if (drifterState() == State.DIVING) {
            setState(State.STALKING);
        }
        if (observed && this.diveCooldownTicks <= 0 && drifterState() != State.LUNGING) {
            setState(State.DIVING);
            this.diveTicksRemaining = 24;
            this.diveCooldownTicks = 90;
            trace(focus, "transition", "bounded_observation_dive",
                    "dive_ticks", this.diveTicksRemaining,
                    "cooldown_ticks", this.diveCooldownTicks);
            return;
        }
        if (this.nextLungeTicks <= 0 || drifterState() == State.LUNGING) {
            if (drifterState() != State.LUNGING) {
                setState(State.LUNGING);
                this.nextLungeTicks = 40 + this.random.nextInt(31);
                if (!this.attackCryPlayed) {
                    playPhysicalCue(level, UncannySoundRegistry.UNCANNY_DRIFTER_CRY.get(), 1.06F, 0.93F);
                    this.attackCryPlayed = true;
                }
                trace(focus, "transition", "offensive_surface_lunge");
            }
            steerInWaterToward(level, focus.position().add(0.0D, 0.35D, 0.0D), 0.24D, 0.09D, 0.28D);
            tryMelee(focus);
            // A lunge only ends once the swimmer has slipped out of reach.
            if (this.stateTicks > 50 && this.distanceToSqr(focus) > 4.0D * 4.0D) {
                setState(State.STALKING);
            }
        } else {
            setState(State.STALKING);
            patrolBelowTarget(level, focus);
        }
    }

    private void tickLand(ServerLevel level, ServerPlayer focus) {
        if (drifterState() == State.LAND_PURSUIT
                && this.landPursuitTicks++ < HuntingSpecialRules.DRIFTER_LAND_PURSUIT_TICKS
                && this.distanceTo(focus) <= 8.0D) {
            this.setTarget(focus);
            this.getNavigation().moveTo(focus, 1.0D);
            tryMelee(focus);
            return;
        }

        setState(State.RETURNING_TO_WATER);
        this.setTarget(null);
        Vec3 water = UncannyHuntingSpecialSystem.findNearestWater(level, this.blockPosition(), 18);
        if (water == null) {
            this.getNavigation().stop();
            return;
        }
        this.getNavigation().moveTo(water.x, water.y, water.z, 1.22D);
    }

    private void patrolBelowTarget(ServerLevel level, ServerPlayer focus) {
        double targetY = Math.max(level.getMinBuildHeight() + 2.0D, focus.getY() - 3.0D);
        steerInWaterToward(level, new Vec3(focus.getX(), targetY, focus.getZ()), 0.15D, 0.07D, 0.20D);
        if (this.tickCount % 12 == 0) {
            level.sendParticles(ParticleTypes.BUBBLE, this.getX(), this.getEyeY(), this.getZ(),
                    2, 0.20D, 0.12D, 0.20D, 0.01D);
        }
    }

    private void tryMelee(ServerPlayer focus) {
        if (this.meleeCooldownTicks <= 0
                && this.distanceToSqr(focus) <= 2.4D * 2.4D
                && this.hasLineOfSight(focus)) {
            this.doHurtTarget(focus);
            this.meleeCooldownTicks = 18;
        }
    }

    private void updateDryReserve(ServerLevel level, ServerPlayer focus) {
        if (occupiesWater(level)) {
            this.waterRecoveryTicks++;
            if (this.waterRecoveryTicks >= HuntingSpecialRules.DRIFTER_WATER_RECOVERY_TICKS) {
                this.dryTicks = 0;
            }
            return;
        }
        this.waterRecoveryTicks = 0;
        this.dryTicks = HuntingSpecialRules.nextDrifterDryTicks(this.dryTicks, false);
        if (HuntingSpecialRules.shouldDamageDryDrifter(this.dryTicks)) {
            this.hurt(level.damageSources().dryOut(), HuntingSpecialRules.DRIFTER_DRY_DAMAGE);
            trace(focus, "limitation", "dry_damage", "dry_ticks", this.dryTicks);
        }
    }

    private void setState(State state) {
        if (drifterState() != state) {
            this.entityData.set(DRIFTER_STATE, (byte) state.id());
            this.stateTicks = 0;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("DrifterState", (byte) drifterState().id());
        tag.putInt("DrifterDryTicks", this.dryTicks);
        tag.putInt("DrifterWaterRecovery", this.waterRecoveryTicks);
        tag.putInt("DrifterLandPursuit", this.landPursuitTicks);
        tag.putInt("DrifterStateTicks", this.stateTicks);
        tag.putInt("DrifterNextLunge", this.nextLungeTicks);
        tag.putInt("DrifterDiveTicks", this.diveTicksRemaining);
        tag.putInt("DrifterDiveCooldown", this.diveCooldownTicks);
        tag.putBoolean("DrifterAttackCry", this.attackCryPlayed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DRIFTER_STATE, tag.getByte("DrifterState"));
        this.dryTicks = Math.max(0, tag.getInt("DrifterDryTicks"));
        this.waterRecoveryTicks = Math.max(0, tag.getInt("DrifterWaterRecovery"));
        this.landPursuitTicks = Math.max(0, tag.getInt("DrifterLandPursuit"));
        this.stateTicks = Math.max(0, tag.getInt("DrifterStateTicks"));
        this.nextLungeTicks = Math.max(1, tag.getInt("DrifterNextLunge"));
        this.diveTicksRemaining = Math.max(0, tag.getInt("DrifterDiveTicks"));
        this.diveCooldownTicks = Math.max(0, tag.getInt("DrifterDiveCooldown"));
        this.attackCryPlayed = tag.getBoolean("DrifterAttackCry");
    }

    @Override
    protected com.eotv.echoofthevoid.event.special.CombatParityRules.Profile combatParity() {
        return com.eotv.echoofthevoid.event.special.CombatParityRules.DRIFTER;
    }

    @Override
    protected String specialId() {
        return "drifter";
    }

    public enum State {
        STALKING(0), DIVING(1), LUNGING(2), LAND_PURSUIT(3), RETURNING_TO_WATER(4);

        private final int id;

        State(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static State byId(int id) {
            return switch (id) {
                case 1 -> DIVING;
                case 2 -> LUNGING;
                case 3 -> LAND_PURSUIT;
                case 4 -> RETURNING_TO_WATER;
                default -> STALKING;
            };
        }
    }
}
