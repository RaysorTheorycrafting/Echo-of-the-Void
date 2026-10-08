package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.event.special.AdaptiveSpecialCombatProfile;
import com.eotv.echoofthevoid.event.special.HuntingSpecialRules;
import com.eotv.echoofthevoid.event.special.HuntingSpecialSoundMemory;
import com.eotv.echoofthevoid.event.special.UncannyHuntingSpecialSystem;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A physical rear hunter that uses remembered mundane sounds as spatial decoys. */
public class UncannyEchoerEntity extends AbstractUncannyHuntingSpecialEntity {
    private static final EntityDataAccessor<Byte> ECHOER_STATE = SynchedEntityData.defineId(
            UncannyEchoerEntity.class,
            EntityDataSerializers.BYTE);

    private int stateTicks;
    private int hideTicksRemaining;
    private int aggroGraceTicks;
    private int replaysRemaining;
    private int nextReplayTicks;
    private int noPathTicks;
    private int fleeCoverAttempts;
    private int fleeStaggerTicks;
    private boolean aggroCryPlayed;
    private long replayCursorTick = Long.MAX_VALUE;
    private Vec3 fleeTarget;
    // A replayed footstep is heard as a short walk coming up behind the player.
    private int stepReplaysRemaining;
    private int nextStepReplayTicks;
    private Vec3 stepReplayPosition;
    private net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> stepReplaySound;

    public UncannyEchoerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "Echoer?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ECHOER_STATE, (byte) State.LISTENING.id());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void onTargetConfigured(ServerPlayer player) {
        HuntingSpecialRules.echoerProfile(AdaptiveSpecialCombatProfile.attacker(player)).applyTo(this, true);
        setState(State.LISTENING);
        this.stateTicks = 0;
        this.hideTicksRemaining = 0;
        this.aggroGraceTicks = 0;
        this.replaysRemaining = HuntingSpecialRules.ECHOER_MAX_REPLAYS;
        this.nextReplayTicks = rollReplayDelay();
        this.noPathTicks = 0;
        this.fleeCoverAttempts = 0;
        this.fleeStaggerTicks = 0;
        this.aggroCryPlayed = false;
        this.replayCursorTick = Long.MAX_VALUE;
        this.fleeTarget = null;
        trace(player, "spawn", "listening", "memory_capacity", HuntingSpecialRules.ECHOER_MEMORY_CAPACITY);
    }

    public State echoerState() {
        return State.byId(this.entityData.get(ECHOER_STATE));
    }

    public void forceAggroForDebug() {
        setState(State.AGGRO);
        this.aggroGraceTicks = HuntingSpecialRules.ECHOER_ATTACK_GRACE_TICKS;
        this.aggroCryPlayed = false;
    }

    @Override
    protected void tickSpecial(ServerLevel level, ServerPlayer focus) {
        this.stateTicks++;
        if (this.fleeStaggerTicks > 0) {
            this.fleeStaggerTicks--;
        }
        if (this.distanceToSqr(focus) > 48.0D * 48.0D) {
            beginSinking(focus, "focus_beyond_range");
            return;
        }
        maybeReplaySound(level, focus);

        switch (echoerState()) {
            case LISTENING -> tickListening(level, focus);
            case APPROACHING -> tickApproach(level, focus);
            case FLEEING -> tickFlee(level, focus);
            case AGGRO -> tickAggro(focus);
            case REMOVING -> beginSinking(focus, "state_removing");
        }
    }

    private void tickListening(ServerLevel level, ServerPlayer focus) {
        this.setTarget(null);
        this.getNavigation().stop();
        this.lookAt(focus, 35.0F, 35.0F);
        if (this.stateTicks >= 30) {
            int listenedTicks = this.stateTicks;
            // A dev-menu encounter often has no eligible recent world sound to replay. Emit one
            // restrained, physical cue from the actual body so Echoer? can never complete an
            // entirely mute encounter merely because its sound memory started empty. The louder
            // aggressive cry remains reserved for the successful rear approach.
            playPhysicalCue(level, UncannySoundRegistry.UNCANNY_ECHOER_CRY.get(), 1.16F, 1.18F);
            setState(State.APPROACHING);
            trace(focus, "transition", "approaching", "listen_ticks", listenedTicks);
        }
    }

    private void tickApproach(ServerLevel level, ServerPlayer focus) {
        this.setTarget(null);
        boolean observed = isDirectlyObservedBy(focus);
        if (observed) {
            moveAwayFrom(focus, 12.0D, 1.05D);
            return;
        }

        double currentDistance = this.distanceTo(focus);
        boolean closing = currentDistance <= 7.5D && isBehind(focus);
        Vec3 rear = rearPoint(
                focus,
                closing
                        ? HuntingSpecialRules.ECHOER_CLOSE_REAR_DISTANCE
                        : HuntingSpecialRules.ECHOER_OUTER_REAR_DISTANCE);
        boolean moving = this.getNavigation().moveTo(rear.x, rear.y, rear.z, 1.0D);
        updateNoPath(moving);
        if (this.noPathTicks >= 80) {
            beginSinking(focus, "approach_path_unavailable");
            return;
        }

        double distance = this.distanceTo(focus);
        if (HuntingSpecialRules.canTriggerEchoerAggro(
                distance, isBehind(focus), this.hasLineOfSight(focus))) {
            setState(State.AGGRO);
            this.aggroGraceTicks = HuntingSpecialRules.ECHOER_ATTACK_GRACE_TICKS;
            playAggroCry(level);
            trace(focus, "transition", "rear_approach_succeeded",
                    "reaction_ticks", this.aggroGraceTicks,
                    "distance", distance);
        }
    }

    private void tickFlee(ServerLevel level, ServerPlayer focus) {
        this.setTarget(null);
        if (this.hideTicksRemaining > 0) {
            if (this.distanceToSqr(focus) < 10.0D * 10.0D || isDirectlyObservedBy(focus)) {
                this.hideTicksRemaining = 0;
                this.fleeTarget = null;
                this.noPathTicks = 0;
                this.getNavigation().stop();
                trace(focus, "flee", "cover_compromised_repositioning",
                        "cover_attempts", this.fleeCoverAttempts);
                if (this.fleeCoverAttempts >= 2) {
                    beginSinking(focus, "both_flee_covers_compromised");
                    return;
                }
            } else {
            this.getNavigation().stop();
            this.hideTicksRemaining--;
            if (this.hideTicksRemaining == 0) {
                this.fleeTarget = null;
                setState(State.APPROACHING);
                trace(focus, "transition", "hunt_resumed_after_cover");
            }
            return;
            }
        }

        if (this.fleeTarget == null) {
            if (this.fleeCoverAttempts >= 2) {
                beginSinking(focus, "flee_cover_unavailable_after_fallback");
                return;
            }
            this.fleeTarget = UncannyHuntingSpecialSystem.findGroundCover(
                    level,
                    focus,
                    this.position(),
                    HuntingSpecialRules.ECHOER_FLEE_MIN_DISTANCE,
                    HuntingSpecialRules.ECHOER_FLEE_MAX_DISTANCE);
            this.fleeCoverAttempts++;
            if (this.fleeTarget == null) {
                if (this.fleeCoverAttempts >= 2) {
                    beginSinking(focus, "flee_cover_unavailable_after_fallback");
                }
                return;
            }
        }
        boolean moving = this.getNavigation().moveTo(
                this.fleeTarget.x,
                this.fleeTarget.y,
                this.fleeTarget.z,
                HuntingSpecialRules.ECHOER_FLEE_SPEED);
        updateNoPath(moving);
        if (this.noPathTicks >= HuntingSpecialRules.ECHOER_STALLED_FLEE_TICKS
                && this.distanceToSqr(focus) <= HuntingSpecialRules.ECHOER_CORNERED_DISTANCE
                        * HuntingSpecialRules.ECHOER_CORNERED_DISTANCE * 4.0D) {
            // The escape route is blocked and the player is close: fight instead of freezing.
            turnOnAttacker(focus, "flee_route_blocked_counterattack");
            return;
        }
        if (this.noPathTicks >= 60) {
            this.getNavigation().stop();
            this.fleeTarget = null;
            this.noPathTicks = 0;
            if (this.fleeCoverAttempts >= 2) {
                beginSinking(focus, "flee_path_unavailable_after_fallback");
            } else {
                trace(focus, "flee", "first_cover_path_unavailable_retrying");
            }
            return;
        }
        if (this.position().distanceToSqr(this.fleeTarget) <= 2.5D
                && this.distanceToSqr(focus) >= 10.0D * 10.0D
                && !isDirectlyObservedBy(focus)) {
            this.getNavigation().stop();
            this.hideTicksRemaining = HuntingSpecialRules.ECHOER_HIDDEN_MIN_TICKS
                    + this.random.nextInt(HuntingSpecialRules.ECHOER_HIDDEN_MAX_TICKS
                            - HuntingSpecialRules.ECHOER_HIDDEN_MIN_TICKS + 1);
            this.noPathTicks = 0;
            trace(focus, "transition", "hidden_in_cover", "hidden_ticks", this.hideTicksRemaining);
        }
    }

    private void tickAggro(ServerPlayer focus) {
        if (!this.aggroCryPlayed && this.level() instanceof ServerLevel level) {
            // The dev-menu aggro state is configured before insertion, so its cue must be emitted
            // on the first authoritative tick. Natural transitions set the same guard immediately
            // and therefore never double-play it.
            playAggroCry(level);
        }
        this.setTarget(focus);
        this.lookAt(focus, 80.0F, 80.0F);
        if (this.aggroGraceTicks > 0) {
            this.aggroGraceTicks--;
            this.getNavigation().moveTo(focus, 1.18D);
            return;
        }
        this.getNavigation().moveTo(focus, 1.15D);
        if (this.hasLineOfSight(focus)
                && this.distanceToSqr(focus) <= 2.7D * 2.7D
                && this.meleeCooldownTicks <= 0) {
            this.doHurtTarget(focus);
            this.meleeCooldownTicks = 16;
        }
    }

    private void maybeReplaySound(ServerLevel level, ServerPlayer focus) {
        if (echoerState() == State.AGGRO || echoerState() == State.REMOVING) {
            this.stepReplaysRemaining = 0;
            return;
        }
        tickStepReplay(level, focus);
        if (this.replaysRemaining <= 0 || this.stepReplaysRemaining > 0) {
            return;
        }
        if (--this.nextReplayTicks > 0) {
            return;
        }
        this.nextReplayTicks = rollReplayDelay();
        HuntingSpecialSoundMemory.Memory memory = HuntingSpecialSoundMemory.newestBefore(
                level, focus.position(), this.replayCursorTick, 42.0D);
        if (memory == null) {
            return;
        }
        Vec3 decoy = UncannyHuntingSpecialSystem.findOppositeSoundPosition(
                level,
                focus,
                this.position(),
                HuntingSpecialRules.ECHOER_DECOY_MIN_DISTANCE,
                HuntingSpecialRules.ECHOER_DECOY_MAX_DISTANCE);
        if (decoy == null) {
            return;
        }
        boolean steps = HuntingSpecialSoundMemory.isStepSound(memory.sound());
        if (steps) {
            // A single faint step is lost in the world; a short walk towards the player is not.
            this.stepReplaySound = memory.sound();
            this.stepReplayPosition = decoy;
            this.stepReplaysRemaining = HuntingSpecialRules.ECHOER_STEP_REPLAY_COUNT;
            this.nextStepReplayTicks = 0;
            tickStepReplay(level, focus);
        } else {
            HuntingSpecialSoundMemory.replay(
                    level,
                    decoy,
                    memory.sound(),
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    Math.max(1.0F, memory.volume()),
                    memory.pitch() * (0.97F + this.random.nextFloat() * 0.06F));
        }
        this.replayCursorTick = memory.tick();
        this.replaysRemaining--;
        trace(focus, "sound_decoy", "replayed",
                "remaining", this.replaysRemaining,
                "sound", memory.sound().unwrapKey().map(key -> key.location().toString()).orElse("?"),
                "player_made", memory.playerMade(),
                "steps", steps,
                "decoy_distance", decoy.distanceTo(focus.position()));
    }

    private void tickStepReplay(ServerLevel level, ServerPlayer focus) {
        if (this.stepReplaysRemaining <= 0 || this.stepReplayPosition == null || this.stepReplaySound == null) {
            return;
        }
        if (--this.nextStepReplayTicks > 0) {
            return;
        }
        HuntingSpecialSoundMemory.replay(
                level,
                this.stepReplayPosition,
                this.stepReplaySound,
                net.minecraft.sounds.SoundSource.PLAYERS,
                HuntingSpecialRules.ECHOER_STEP_REPLAY_VOLUME,
                0.92F + this.random.nextFloat() * 0.16F);
        // Each step lands a little closer, as if someone walked up behind the player.
        Vec3 toward = focus.position().subtract(this.stepReplayPosition).multiply(1.0D, 0.0D, 1.0D);
        if (toward.lengthSqr() > 4.0D) {
            this.stepReplayPosition = this.stepReplayPosition.add(toward.normalize().scale(0.7D));
        }
        this.stepReplaysRemaining--;
        this.nextStepReplayTicks = HuntingSpecialRules.ECHOER_STEP_REPLAY_INTERVAL_TICKS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        boolean playerAttack = attacker instanceof Player;
        State before = echoerState();
        float applied = before == State.AGGRO
                ? amount
                : HuntingSpecialRules.echoerPreAggroDamage(amount, playerAttack);
        boolean hurt = super.hurt(source, applied);
        if (hurt && playerAttack && before == State.FLEEING && !this.isDeadOrDying()
                && this.distanceToSqr(attacker) <= HuntingSpecialRules.ECHOER_CORNERED_DISTANCE
                        * HuntingSpecialRules.ECHOER_CORNERED_DISTANCE) {
            // Caught again before it could get away: a hunter never stands still to be killed.
            turnOnAttacker(attacker, "cornered_counterattack");
            return hurt;
        }
        if (hurt && playerAttack && before != State.AGGRO && !this.isDeadOrDying()) {
            if (before != State.FLEEING) {
                setState(State.FLEEING);
                this.fleeTarget = null;
                this.hideTicksRemaining = 0;
                this.noPathTicks = 0;
                this.fleeCoverAttempts = 0;
                this.fleeStaggerTicks = HuntingSpecialRules.ECHOER_FLEE_STAGGER_TICKS;
                this.setTarget(null);
                this.getNavigation().stop();
                Vec3 away = this.position().subtract(attacker.position());
                Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
                if (horizontal.lengthSqr() < 1.0E-4D) {
                    horizontal = new Vec3(1.0D, 0.0D, 0.0D);
                }
                this.setDeltaMovement(this.getDeltaMovement()
                        .add(horizontal.normalize().scale(0.42D))
                        .add(0.0D, 0.16D, 0.0D));
                this.hasImpulse = true;
                if (this.level() instanceof ServerLevel level) {
                    // A single startled cue confirms that the visible hunter is a physical
                    // creature. It is not repeated by follow-up hits and remains distinct from
                    // the lower, aggressive cry used when the rear approach succeeds.
                    playPhysicalCue(level, UncannySoundRegistry.UNCANNY_ECHOER_CRY.get(), 1.28F, 1.08F);
                }
            }
            if (attacker instanceof ServerPlayer player) {
                trace(player, before == State.FLEEING ? "flee" : "transition",
                        before == State.FLEEING ? "repeat_hit_kept_existing_route" : "pre_aggro_hit_flee",
                        "applied_damage", applied);
            }
        }
        return hurt;
    }

    private void turnOnAttacker(Entity attacker, String reason) {
        setState(State.AGGRO);
        this.aggroGraceTicks = HuntingSpecialRules.ECHOER_CORNERED_GRACE_TICKS;
        this.aggroCryPlayed = false;
        this.fleeTarget = null;
        this.hideTicksRemaining = 0;
        this.noPathTicks = 0;
        this.getNavigation().stop();
        if (attacker instanceof ServerPlayer player) {
            trace(player, "transition", reason);
        }
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (echoerState() == State.FLEEING && this.fleeStaggerTicks > 0) {
            return;
        }
        super.knockback(strength, x, z);
    }

    private void moveAwayFrom(ServerPlayer focus, double distance, double speed) {
        Vec3 away = this.position().subtract(focus.position());
        away = new Vec3(away.x, 0.0D, away.z);
        if (away.lengthSqr() < 1.0E-4D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 destination = focus.position().add(away.normalize().scale(distance));
        this.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
    }

    private static Vec3 rearPoint(ServerPlayer focus, double distance) {
        Vec3 view = focus.getViewVector(1.0F);
        Vec3 horizontal = new Vec3(view.x, 0.0D, view.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        return focus.position().subtract(horizontal.normalize().scale(distance));
    }

    private boolean isBehind(ServerPlayer focus) {
        Vec3 toEntity = this.position().subtract(focus.position());
        Vec3 view = focus.getViewVector(1.0F);
        Vec3 horizontalEntity = new Vec3(toEntity.x, 0.0D, toEntity.z);
        Vec3 horizontalView = new Vec3(view.x, 0.0D, view.z);
        return horizontalEntity.lengthSqr() > 1.0E-4D
                && horizontalView.lengthSqr() > 1.0E-4D
                && horizontalEntity.normalize().dot(horizontalView.normalize()) < -0.45D;
    }

    private int rollReplayDelay() {
        return HuntingSpecialRules.ECHOER_REPLAY_MIN_TICKS
                + this.random.nextInt(HuntingSpecialRules.ECHOER_REPLAY_MAX_TICKS
                        - HuntingSpecialRules.ECHOER_REPLAY_MIN_TICKS + 1);
    }

    private void playAggroCry(ServerLevel level) {
        if (this.aggroCryPlayed) {
            return;
        }
        this.aggroCryPlayed = true;
        playPhysicalCue(level, UncannySoundRegistry.UNCANNY_ECHOER_CRY.get(), 1.62F, 0.88F);
    }

    private void updateNoPath(boolean moving) {
        if (moving && !this.getNavigation().isDone()) {
            this.noPathTicks = 0;
        } else {
            this.noPathTicks++;
        }
    }

    private void setState(State state) {
        this.entityData.set(ECHOER_STATE, (byte) state.id());
        this.stateTicks = 0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("EchoerState", (byte) echoerState().id());
        tag.putInt("EchoerStateTicks", this.stateTicks);
        tag.putInt("EchoerHideTicks", this.hideTicksRemaining);
        tag.putInt("EchoerAggroGrace", this.aggroGraceTicks);
        tag.putInt("EchoerReplaysRemaining", this.replaysRemaining);
        tag.putInt("EchoerNextReplay", this.nextReplayTicks);
        tag.putInt("EchoerNoPathTicks", this.noPathTicks);
        tag.putInt("EchoerFleeCoverAttempts", this.fleeCoverAttempts);
        tag.putInt("EchoerFleeStaggerTicks", this.fleeStaggerTicks);
        tag.putBoolean("EchoerAggroCryPlayed", this.aggroCryPlayed);
        tag.putLong("EchoerReplayCursorTick", this.replayCursorTick);
        if (this.fleeTarget != null) {
            tag.putDouble("EchoerFleeX", this.fleeTarget.x);
            tag.putDouble("EchoerFleeY", this.fleeTarget.y);
            tag.putDouble("EchoerFleeZ", this.fleeTarget.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setState(State.byId(tag.getByte("EchoerState")));
        this.stateTicks = Math.max(0, tag.getInt("EchoerStateTicks"));
        this.hideTicksRemaining = Math.max(0, tag.getInt("EchoerHideTicks"));
        this.aggroGraceTicks = Math.max(0, tag.getInt("EchoerAggroGrace"));
        this.replaysRemaining = Mth.clamp(tag.getInt("EchoerReplaysRemaining"), 0,
                HuntingSpecialRules.ECHOER_MAX_REPLAYS);
        this.nextReplayTicks = Math.max(1, tag.getInt("EchoerNextReplay"));
        this.noPathTicks = Math.max(0, tag.getInt("EchoerNoPathTicks"));
        this.fleeCoverAttempts = Math.max(0, tag.getInt("EchoerFleeCoverAttempts"));
        this.fleeStaggerTicks = Math.max(0, tag.getInt("EchoerFleeStaggerTicks"));
        this.aggroCryPlayed = tag.getBoolean("EchoerAggroCryPlayed");
        this.replayCursorTick = tag.contains("EchoerReplayCursorTick")
                ? tag.getLong("EchoerReplayCursorTick")
                : tag.contains("EchoerLastMemoryTick")
                        ? tag.getLong("EchoerLastMemoryTick")
                        : Long.MAX_VALUE;
        if (tag.contains("EchoerFleeX")) {
            this.fleeTarget = new Vec3(
                    tag.getDouble("EchoerFleeX"),
                    tag.getDouble("EchoerFleeY"),
                    tag.getDouble("EchoerFleeZ"));
        }
    }

    @Override
    protected com.eotv.echoofthevoid.event.special.CombatParityRules.Profile combatParity() {
        return com.eotv.echoofthevoid.event.special.CombatParityRules.ECHOER;
    }

    @Override
    protected String specialId() {
        return "echoer";
    }

    public enum State {
        LISTENING(0),
        APPROACHING(1),
        FLEEING(2),
        AGGRO(3),
        REMOVING(4);

        private final int id;

        State(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static State byId(int id) {
            return switch (id) {
                case 1 -> APPROACHING;
                case 2 -> FLEEING;
                case 3 -> AGGRO;
                case 4 -> REMOVING;
                default -> LISTENING;
            };
        }
    }
}
