package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.event.special.HuntingSpecialRules;
import com.eotv.echoofthevoid.event.special.UncannyHuntingSpecialSystem;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Deep-ocean grappler with a gradual, interruptible pull instead of forced riding. */
public class UncannyDredgerEntity extends AbstractUncannyAquaticSpecialEntity {
    private static final EntityDataAccessor<Byte> DREDGER_STATE = SynchedEntityData.defineId(
            UncannyDredgerEntity.class,
            EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<UUID>> GRABBED_PLAYER = SynchedEntityData.defineId(
            UncannyDredgerEntity.class,
            EntityDataSerializers.OPTIONAL_UUID);

    private int stateTicks;
    private int acceptedReleaseHits;
    private int regrabCooldownTicks;
    private int grappleDamageTicks;
    private int waterExitConfirmationTicks;
    private Vec3 pullAnchor;

    public UncannyDredgerEntity(EntityType<? extends Drowned> type, Level level) {
        super(type, level, "Dredger?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DREDGER_STATE, (byte) State.CHASING.id());
        builder.define(GRABBED_PLAYER, Optional.empty());
    }

    @Override
    protected void onTargetConfigured(ServerPlayer player) {
        setState(State.CHASING);
        this.acceptedReleaseHits = 0;
        this.regrabCooldownTicks = 0;
        this.grappleDamageTicks = 0;
        this.waterExitConfirmationTicks = 0;
        this.pullAnchor = null;
        this.entityData.set(GRABBED_PLAYER, Optional.empty());
        trace(player, "spawn", "deep_ocean_hunt_started");
    }

    public State dredgerState() {
        return State.byId(this.entityData.get(DREDGER_STATE));
    }

    public int acceptedReleaseHits() {
        return this.acceptedReleaseHits;
    }

    public Optional<UUID> grabbedPlayerId() {
        return this.entityData.get(GRABBED_PLAYER);
    }

    public boolean forceGrabForDebug(ServerLevel level, ServerPlayer focus) {
        Vec3 anchor = UncannyHuntingSpecialSystem.findSeabedPullAnchor(level, this.blockPosition(), 10);
        // A freshly positioned test player (and, briefly, a player restored after a
        // dimension transfer) may not have refreshed Entity#wasTouchingWater yet.
        // The authoritative block fluid is the stable source of truth here.
        boolean occupiesWater = focus.isInWaterOrBubble()
                || level.getFluidState(focus.blockPosition()).is(net.minecraft.tags.FluidTags.WATER);
        if (anchor == null || !occupiesWater
                || this.distanceToSqr(focus) > HuntingSpecialRules.DREDGER_GRAB_BREAK_DISTANCE
                        * HuntingSpecialRules.DREDGER_GRAB_BREAK_DISTANCE) {
            return false;
        }
        this.pullAnchor = anchor;
        this.acceptedReleaseHits = 0;
        this.grappleDamageTicks = 0;
        this.entityData.set(GRABBED_PLAYER, Optional.of(focus.getUUID()));
        setState(State.GRABBING);
        return true;
    }

    @Override
    protected void tickAquaticSpecial(ServerLevel level, ServerPlayer focus) {
        this.stateTicks++;
        if (this.regrabCooldownTicks > 0) {
            this.regrabCooldownTicks--;
        }
        if (this.distanceToSqr(focus) > 56.0D * 56.0D) {
            release(focus, "focus_beyond_range");
            discardWithTrace(focus, "focus_beyond_range");
            return;
        }

        if (focus.getVehicle() instanceof Boat) {
            this.waterExitConfirmationTicks = 0;
            release(focus, "boat_refuge");
            this.setTarget(null);
            this.getNavigation().stop();
            return;
        }
        boolean focusOccupiesWater = focus.isInWaterOrBubble()
                || level.getFluidState(focus.blockPosition()).is(net.minecraft.tags.FluidTags.WATER);
        boolean dredgerOccupiesWater = occupiesWater(level);
        if (!focusOccupiesWater || !dredgerOccupiesWater) {
            if (++this.waterExitConfirmationTicks >= 2) {
                release(focus, "left_water_confirmed");
                this.setTarget(null);
                this.getNavigation().stop();
            }
            return;
        }
        this.waterExitConfirmationTicks = 0;

        switch (dredgerState()) {
            case CHASING -> tickChasing(level, focus);
            case TELEGRAPHING -> tickTelegraph(level, focus);
            case GRABBING -> tickGrab(level, focus);
        }
    }

    private void tickChasing(ServerLevel level, ServerPlayer focus) {
        this.setTarget(focus);
        double pursuitDepth = Math.max(
                level.getMinBuildHeight() + 2.0D,
                Math.min(focus.getY() - 1.5D, this.getY() + 0.5D));
        steerInWaterToward(level, new Vec3(focus.getX(), pursuitDepth, focus.getZ()), 0.14D, 0.07D, 0.20D);
        if (this.meleeCooldownTicks <= 0
                && this.distanceToSqr(focus) <= HuntingSpecialRules.DREDGER_BITE_REACH * HuntingSpecialRules.DREDGER_BITE_REACH
                && this.hasLineOfSight(focus)) {
            // Between grabs it still bites whatever swims within reach.
            this.doHurtTarget(focus);
            this.meleeCooldownTicks = HuntingSpecialRules.DREDGER_BITE_INTERVAL_TICKS;
        }
        if (this.regrabCooldownTicks <= 0
                && this.distanceToSqr(focus) <= 6.0D * 6.0D
                && this.hasLineOfSight(focus)) {
            setState(State.TELEGRAPHING);
            this.getNavigation().stop();
            playPhysicalCue(level, UncannySoundRegistry.UNCANNY_DREDGER_CRY.get(), 1.14F, 0.82F);
            trace(focus, "transition", "grab_telegraph", "telegraph_ticks", HuntingSpecialRules.DREDGER_TELEGRAPH_TICKS);
        }
    }

    private void tickTelegraph(ServerLevel level, ServerPlayer focus) {
        this.setTarget(focus);
        this.lookAt(focus, 70.0F, 70.0F);
        double pursuitDepth = Math.max(
                level.getMinBuildHeight() + 2.0D,
                Math.min(focus.getY() - 1.5D, this.getY() + 0.35D));
        steerInWaterToward(level, new Vec3(focus.getX(), pursuitDepth, focus.getZ()), 0.11D, 0.06D, 0.18D);
        level.sendParticles(
                this.stateTicks % 2 == 0 ? ParticleTypes.BUBBLE : ParticleTypes.CLOUD,
                this.getX(), this.getY() + 0.35D, this.getZ(),
                3, 0.55D, 0.20D, 0.55D, 0.015D);
        if (this.stateTicks < HuntingSpecialRules.DREDGER_TELEGRAPH_TICKS) {
            return;
        }
        if (this.distanceToSqr(focus) > HuntingSpecialRules.DREDGER_GRAB_START_DISTANCE
                        * HuntingSpecialRules.DREDGER_GRAB_START_DISTANCE
                || !this.hasLineOfSight(focus)) {
            setState(State.CHASING);
            return;
        }
        Vec3 anchor = UncannyHuntingSpecialSystem.findSeabedPullAnchor(level, this.blockPosition(), 10);
        if (anchor == null) {
            setState(State.CHASING);
            trace(focus, "grab", "no_valid_seabed_anchor");
            return;
        }
        this.pullAnchor = anchor;
        this.acceptedReleaseHits = 0;
        this.grappleDamageTicks = 0;
        this.entityData.set(GRABBED_PLAYER, Optional.of(focus.getUUID()));
        setState(State.GRABBING);
        trace(focus, "transition", "grab_started", "anchor_y", anchor.y);
    }

    private void tickGrab(ServerLevel level, ServerPlayer focus) {
        Vec3 grapplePoint = this.position().add(0.0D, 0.55D, 0.0D);
        double grappleDistance = this.distanceTo(focus);
        boolean clearWaterRoute = UncannyHuntingSpecialSystem.isWaterRouteClear(
                level, focus.position(), grapplePoint);
        if (grabbedPlayerId().filter(focus.getUUID()::equals).isEmpty()
                || this.pullAnchor == null
                || !HuntingSpecialRules.canMaintainDredgerGrab(
                        grappleDistance, this.hasLineOfSight(focus), clearWaterRoute)) {
            release(focus, "grab_path_obstructed");
            return;
        }
        Vec3 offset = grapplePoint.subtract(focus.position());
        Vec3 previous = focus.getDeltaMovement();
        HuntingSpecialRules.HorizontalVelocity horizontal = HuntingSpecialRules.boundedDredgerHorizontalVelocity(
                previous.x,
                previous.z,
                offset.x * 0.018D,
                offset.z * 0.018D);
        Vec3 boundedVelocity = new Vec3(
                horizontal.x(),
                HuntingSpecialRules.boundedDredgerVerticalVelocity(previous.y, offset.y * 0.016D),
                horizontal.z());
        focus.setDeltaMovement(boundedVelocity);
        focus.hurtMarked = true;
        steerInWaterToward(level, this.pullAnchor, 0.10D, 0.06D, 0.18D);
        level.sendParticles(ParticleTypes.BUBBLE,
                focus.getX(), focus.getY() + 0.8D, focus.getZ(),
                2, 0.3D, 0.5D, 0.3D, 0.01D);
        if (++this.grappleDamageTicks >= HuntingSpecialRules.DREDGER_DAMAGE_INTERVAL_TICKS) {
            this.grappleDamageTicks = 0;
            // Damage is part of the grapple, not a melee impact. Restore the already bounded pull
            // velocity so LivingEntity's ordinary hurt knockback cannot launch the victim away
            // from the visible source while the grab remains active.
            // Duel parity sets this creature's attack damage from the victim's protection.
            focus.hurt(level.damageSources().mobAttack(this), (float) this.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
            focus.setDeltaMovement(boundedVelocity);
            focus.hurtMarked = true;
        }
        if (this.grappleDamageTicks % 20 == 0) {
            trace(focus, "grab_velocity", "bounded_pull",
                    "horizontal_speed", boundedVelocity.horizontalDistance(),
                    "vertical_speed", boundedVelocity.y);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        Entity attacker = source.getEntity();
        if (hurt
                && dredgerState() == State.GRABBING
                && attacker instanceof Player
                && amount > 0.0F
                && this.level() instanceof ServerLevel level) {
            this.acceptedReleaseHits++;
            ServerPlayer focus = resolveFocus(level);
            trace(focus, "grab_resistance", "accepted_hit",
                    "accepted_hits", this.acceptedReleaseHits,
                    "required_hits", HuntingSpecialRules.DREDGER_HITS_TO_RELEASE);
            if (HuntingSpecialRules.releasesDredgerGrab(this.acceptedReleaseHits)) {
                release(focus, "three_accepted_hits");
            }
        }
        return hurt;
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (dredgerState() == State.GRABBING) {
            // The victim breaks a grip through three accepted hits. Letting those same hits move
            // the source several blocks made the visible tether detach before its state released.
            return;
        }
        super.knockback(strength, x, z);
    }

    @Override
    protected void onDeathAnimation() {
        if (this.level() instanceof ServerLevel level) {
            release(resolveFocus(level), "died_while_grabbing");
        }
    }

    public void release(ServerPlayer focus, String reason) {
        if (dredgerState() != State.GRABBING && grabbedPlayerId().isEmpty()) {
            return;
        }
        this.entityData.set(GRABBED_PLAYER, Optional.empty());
        this.pullAnchor = null;
        this.acceptedReleaseHits = 0;
        this.grappleDamageTicks = 0;
        this.regrabCooldownTicks = HuntingSpecialRules.DREDGER_REGRAB_COOLDOWN_TICKS;
        setState(State.CHASING);
        if (focus != null) {
            // A released player must not keep the downward impulse accumulated by the grapple.
            // Horizontal momentum is softened, while a natural upward swim input is preserved.
            Vec3 movement = focus.getDeltaMovement();
            focus.setDeltaMovement(
                    movement.x * 0.55D,
                    HuntingSpecialRules.releasedDredgerVerticalVelocity(movement.y),
                    movement.z * 0.55D);
            focus.hurtMarked = true;
        }
        trace(focus, "transition", "grab_released", "reason", reason,
                "regrab_cooldown_ticks", this.regrabCooldownTicks);
    }

    private void setState(State state) {
        this.entityData.set(DREDGER_STATE, (byte) state.id());
        this.stateTicks = 0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("DredgerState", (byte) dredgerState().id());
        tag.putInt("DredgerStateTicks", this.stateTicks);
        tag.putInt("DredgerAcceptedHits", this.acceptedReleaseHits);
        tag.putInt("DredgerRegrabCooldown", this.regrabCooldownTicks);
        tag.putInt("DredgerDamageTicks", this.grappleDamageTicks);
        tag.putInt("DredgerWaterExitConfirmation", this.waterExitConfirmationTicks);
        grabbedPlayerId().ifPresent(uuid -> tag.putUUID("DredgerGrabbedPlayer", uuid));
        if (this.pullAnchor != null) {
            tag.putDouble("DredgerAnchorX", this.pullAnchor.x);
            tag.putDouble("DredgerAnchorY", this.pullAnchor.y);
            tag.putDouble("DredgerAnchorZ", this.pullAnchor.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DREDGER_STATE, tag.getByte("DredgerState"));
        this.stateTicks = Math.max(0, tag.getInt("DredgerStateTicks"));
        this.acceptedReleaseHits = Math.max(0, tag.getInt("DredgerAcceptedHits"));
        this.regrabCooldownTicks = Math.max(0, tag.getInt("DredgerRegrabCooldown"));
        this.grappleDamageTicks = Math.max(0, tag.getInt("DredgerDamageTicks"));
        this.waterExitConfirmationTicks = Math.max(0, tag.getInt("DredgerWaterExitConfirmation"));
        if (tag.hasUUID("DredgerGrabbedPlayer")) {
            this.entityData.set(GRABBED_PLAYER, Optional.of(tag.getUUID("DredgerGrabbedPlayer")));
        }
        if (tag.contains("DredgerAnchorX")) {
            this.pullAnchor = new Vec3(
                    tag.getDouble("DredgerAnchorX"),
                    tag.getDouble("DredgerAnchorY"),
                    tag.getDouble("DredgerAnchorZ"));
        }
    }

    @Override
    protected com.eotv.echoofthevoid.event.special.CombatParityRules.Profile combatParity() {
        return com.eotv.echoofthevoid.event.special.CombatParityRules.DREDGER;
    }

    @Override
    protected String specialId() {
        return "dredger";
    }

    public enum State {
        CHASING(0), TELEGRAPHING(1), GRABBING(2);

        private final int id;

        State(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static State byId(int id) {
            return switch (id) {
                case 1 -> TELEGRAPHING;
                case 2 -> GRABBING;
                default -> CHASING;
            };
        }
    }
}
