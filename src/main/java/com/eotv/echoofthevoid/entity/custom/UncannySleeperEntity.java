package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.event.special.CombatParityRules;
import com.eotv.echoofthevoid.event.special.PercherSystem;
import com.eotv.echoofthevoid.event.special.SleeperPins;
import com.eotv.echoofthevoid.event.special.SleeperRules;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Sleeper?: almost invisible, it waits motionless in a dark corner of a base. The first time someone
 * tries to sleep nearby they are told that something is in the room. If they sleep anyway, half a
 * second in it rushes to the bed, climbs onto the sleeper, holds them in their sleep, opens a mouth
 * full of teeth and bites (deadly in golden armour, survivable in iron or better). Then it climbs off,
 * giving the player a moment to strike back, and fights with Attacker?'s duel parity.
 */
public class UncannySleeperEntity extends AbstractUncannyHuntingSpecialEntity {
    public enum State {
        IDLE,
        RUN,
        MOUNT,
        MOUTH,
        BITE,
        RECOVER,
        FIGHT
    }

    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> BITE =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("echoofthevoid", "sleeper_bite"));
    private static final EntityDataAccessor<Byte> STATE =
            SynchedEntityData.defineId(UncannySleeperEntity.class, EntityDataSerializers.BYTE);

    private int stateTicks;
    private int growlTicks;
    private BlockPos bedHead;
    private UUID victim;
    private final Set<UUID> warned = new HashSet<>();
    private final java.util.Map<UUID, Integer> watchedAsleep = new java.util.HashMap<>();
    private int clientStateStart;
    private State clientState = State.IDLE;

    public UncannySleeperEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level, "Sleeper?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, (byte) State.IDLE.ordinal());
    }

    public State state() {
        return State.values()[Math.floorMod(this.entityData.get(STATE), State.values().length)];
    }

    /** Client: ticks (with fraction) since the current state began, for animations. */
    public float stateAge(float partialTicks) {
        State current = state();
        if (current != this.clientState) {
            this.clientState = current;
            this.clientStateStart = this.tickCount;
        }
        return this.tickCount - this.clientStateStart + partialTicks;
    }

    private void setState(State state) {
        this.entityData.set(STATE, (byte) state.ordinal());
        this.stateTicks = 0;
    }

    /** First attempt to sleep near it: the warning, once per player. Returns true when it must refuse. */
    public boolean warnOnce(UUID player) {
        return state() == State.IDLE && this.warned.add(player);
    }

    /**
     * While it waits, Vanilla's "monsters nearby" refusal would both give it away and keep the player
     * from ever falling asleep, which is what it waits for. Once it has moved, it is a monster like any other.
     */
    @Override
    public boolean isPreventingPlayerRest(Player player) {
        return state() != State.IDLE;
    }

    public boolean isAttacking(UUID player) {
        return player.equals(this.victim) && state().ordinal() >= State.RUN.ordinal() && state().ordinal() <= State.BITE.ordinal();
    }

    @Override
    protected CombatParityRules.Profile combatParity() {
        return CombatParityRules.ATTACKER;
    }

    @Override
    protected String specialId() {
        return "sleeper";
    }

    @Override
    protected void tickSpecial(ServerLevel level, ServerPlayer focus) {
        this.stateTicks++;
        switch (state()) {
            case IDLE -> tickIdle(level, focus);
            case RUN -> tickRun(level, focus);
            case MOUNT -> {
                holdOnBed(level);
                if (this.stateTicks >= SleeperRules.MOUNT_TICKS) {
                    setState(State.MOUTH);
                    playPhysicalCue(level, UncannySoundRegistry.UNCANNY_SLEEPER_JAW.get(), 1.4F, 0.9F);
                }
            }
            case MOUTH -> {
                holdOnBed(level);
                if (this.stateTicks >= SleeperRules.MOUTH_TICKS) {
                    setState(State.BITE);
                }
            }
            case BITE -> tickBite(level);
            case RECOVER -> {
                this.setNoGravity(false);
                this.noPhysics = false;
                if (this.stateTicks == 4) {
                    playPhysicalCue(level, UncannySoundRegistry.UNCANNY_SLEEPER_GROWL.get(), 1.2F, 0.9F);
                }
                if (this.stateTicks >= SleeperRules.RECOVER_TICKS) {
                    setState(State.FIGHT);
                }
            }
            case FIGHT -> tickFight(level, focus);
        }
    }

    private void tickIdle(ServerLevel level, ServerPlayer focus) {
        this.getNavigation().stop();
        if (PercherSystem.isDaylight(level)) {
            beginSinking(focus, "dawn");
            return;
        }
        if (this.stateTicks % 5 != 0) {
            return;
        }
        // It counts for itself how long it has watched each sleeper: half a second, then it runs.
        this.watchedAsleep.keySet().removeIf(id -> {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            return player == null || !player.isSleeping();
        });
        for (ServerPlayer player : level.players()) {
            if (player.isSleeping() && player.getSleepingPos().isPresent()
                    && player.getSleepingPos().get().distSqr(this.blockPosition()) <= SleeperRules.BED_RADIUS * SleeperRules.BED_RADIUS
                    && this.watchedAsleep.merge(player.getUUID(), 5, Integer::sum) >= SleeperRules.ASLEEP_BEFORE_RUN_TICKS) {
                this.victim = player.getUUID();
                this.bedHead = player.getSleepingPos().get();
                setupTarget(player, SleeperRules.FIGHT_TICKS + 20 * 30);
                setState(State.RUN);
                playPhysicalCue(level, UncannySoundRegistry.UNCANNY_SLEEPER_RUSH.get(), 1.0F, 1.0F);
                return;
            }
        }
    }

    private void tickRun(ServerLevel level, ServerPlayer focus) {
        ServerPlayer sleeper = victimPlayer(level);
        if (sleeper == null || !sleeper.isSleeping()) {
            // Woke up before it arrived: it goes back into the ground.
            beginSinking(focus, "woke_before_arrival");
            return;
        }
        Vec3 perch = bedFootPerch(level);
        this.getNavigation().moveTo(perch.x, perch.y, perch.z, 2.6D);
        if (this.position().distanceToSqr(perch) < 1.6D * 1.6D || this.stateTicks >= SleeperRules.MAX_RUN_TICKS) {
            SleeperPins.pin(sleeper.getUUID());
            this.getNavigation().stop();
            setState(State.MOUNT);
            holdOnBed(level);
        }
    }

    /** On the foot half of the bed, facing the sleeper's face: where a lying player's camera looks. */
    private Vec3 bedFootPerch(ServerLevel level) {
        BlockState bed = level.getBlockState(this.bedHead);
        Direction facing = bed.getBlock() instanceof BedBlock ? bed.getValue(BedBlock.FACING) : Direction.NORTH;
        BlockPos foot = this.bedHead.relative(facing.getOpposite());
        return new Vec3(foot.getX() + 0.5D, foot.getY() + 0.5625D, foot.getZ() + 0.5D);
    }

    private void holdOnBed(ServerLevel level) {
        Vec3 perch = bedFootPerch(level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setDeltaMovement(Vec3.ZERO);
        Vec3 head = Vec3.atBottomCenterOf(this.bedHead);
        float yaw = (float) (Math.toDegrees(Math.atan2(head.z - perch.z, head.x - perch.x)) - 90.0D);
        this.moveTo(perch.x, perch.y, perch.z, yaw, 30.0F);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
        ServerPlayer sleeper = victimPlayer(level);
        if (sleeper == null || !sleeper.isAlive()) {
            release(level, false);
            setState(State.RECOVER);
        }
    }

    private void tickBite(ServerLevel level) {
        holdOnBed(level);
        if (this.stateTicks == SleeperRules.BITE_IMPACT_TICK) {
            ServerPlayer sleeper = victimPlayer(level);
            playPhysicalCue(level, UncannySoundRegistry.UNCANNY_SLEEPER_BITE.get(), 1.5F, 1.0F);
            release(level, false);
            if (sleeper != null && sleeper.isAlive()) {
                // Own damage type without difficulty scaling: gold dies and iron lives on every difficulty.
                sleeper.hurt(new DamageSource(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(BITE), this), SleeperRules.BITE_DAMAGE);
                if (sleeper.isAlive() && sleeper.isSleeping()) {
                    sleeper.stopSleepInBed(true, true);
                }
            }
        }
        if (this.stateTicks >= SleeperRules.BITE_TICKS) {
            setState(State.RECOVER);
            stepOffBed(level);
        }
    }

    private void stepOffBed(ServerLevel level) {
        this.setNoGravity(false);
        this.noPhysics = false;
        Vec3 perch = bedFootPerch(level);
        BlockPos foot = BlockPos.containing(perch);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos spot = foot.relative(side);
            if (level.getBlockState(spot).getCollisionShape(level, spot).isEmpty()
                    && level.getBlockState(spot.above()).getCollisionShape(level, spot.above()).isEmpty()) {
                this.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, this.getYRot(), 0.0F);
                return;
            }
        }
    }

    private void tickFight(ServerLevel level, ServerPlayer focus) {
        if (this.stateTicks >= SleeperRules.FIGHT_TICKS || this.distanceToSqr(focus) > 40.0D * 40.0D) {
            beginSinking(focus, "fight_over");
            return;
        }
        this.setTarget(focus);
        this.lookAt(focus, 80.0F, 80.0F);
        this.getNavigation().moveTo(focus, 1.15D);
        if (this.hasLineOfSight(focus) && this.distanceToSqr(focus) <= 2.7D * 2.7D && this.meleeCooldownTicks <= 0) {
            playPhysicalCue(level, UncannySoundRegistry.UNCANNY_SLEEPER_SNAP.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
            this.doHurtTarget(focus);
            this.meleeCooldownTicks = SleeperRules.MELEE_COOLDOWN_TICKS;
        }
        if (--this.growlTicks <= 0) {
            playPhysicalCue(level, UncannySoundRegistry.UNCANNY_SLEEPER_GROWL.get(), 1.0F, 0.85F + this.random.nextFloat() * 0.25F);
            this.growlTicks = SleeperRules.MIN_GROWL_TICKS + this.random.nextInt(SleeperRules.MAX_GROWL_TICKS - SleeperRules.MIN_GROWL_TICKS + 1);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return UncannySoundRegistry.UNCANNY_SLEEPER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return UncannySoundRegistry.UNCANNY_SLEEPER_DEATH.get();
    }

    private ServerPlayer victimPlayer(ServerLevel level) {
        return this.victim == null ? null : level.getServer().getPlayerList().getPlayer(this.victim);
    }

    private void release(ServerLevel level, boolean wakeUp) {
        if (this.victim == null) {
            return;
        }
        SleeperPins.release(this.victim);
        ServerPlayer sleeper = victimPlayer(level);
        if (wakeUp && sleeper != null && sleeper.isSleeping()) {
            sleeper.stopSleepInBed(true, true);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel level)) {
            return false;
        }
        State current = state();
        if ((current == State.IDLE || current == State.RUN) && source.getEntity() instanceof ServerPlayer player) {
            // Found and struck before it could reach anyone: it sinks away.
            boolean hurt = super.hurt(source, Math.min(amount, Math.max(0.0F, this.getHealth() - 1.0F)));
            release(level, false);
            beginSinking(player, "found_and_hit");
            return hurt;
        }
        if ((current == State.MOUNT || current == State.MOUTH)
                && source.getEntity() instanceof ServerPlayer rescuer && !rescuer.getUUID().equals(this.victim)) {
            // Someone else tears it off the sleeper: the sleeper wakes and it turns on the rescuer.
            release(level, true);
            setupTarget(rescuer, SleeperRules.FIGHT_TICKS);
            setState(State.RECOVER);
            stepOffBed(level);
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        // Its body is part of the encounter, not of the terrain: no suffocation in the bed it sits on.
        return source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)
                || source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)
                || super.isInvulnerableTo(source);
    }

    @Override
    protected void onBeginSinking(ServerPlayer focus, String reason) {
        if (this.level() instanceof ServerLevel level) {
            release(level, false);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.victim != null) {
            SleeperPins.release(this.victim);
        }
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("SleeperState", this.entityData.get(STATE));
        tag.putInt("SleeperStateTicks", this.stateTicks);
        if (this.bedHead != null) {
            tag.putLong("SleeperBed", this.bedHead.asLong());
        }
        if (this.victim != null) {
            tag.putUUID("SleeperVictim", this.victim);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        State saved = State.values()[Math.floorMod(tag.getByte("SleeperState"), State.values().length)];
        // A pin never survives a reload: an interrupted attack resumes as a fight.
        if (saved.ordinal() >= State.RUN.ordinal() && saved.ordinal() <= State.BITE.ordinal()) {
            saved = State.RECOVER;
        }
        this.entityData.set(STATE, (byte) saved.ordinal());
        this.stateTicks = tag.getInt("SleeperStateTicks");
        this.bedHead = tag.contains("SleeperBed") ? BlockPos.of(tag.getLong("SleeperBed")) : null;
        this.victim = tag.hasUUID("SleeperVictim") ? tag.getUUID("SleeperVictim") : null;
    }
}
