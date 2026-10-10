package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Rare False Fall follow-up: one readable ambush attempt, then a retreat through the ground. */
public final class UncannyAmbusherEntity extends Monster implements UncannyEntityMarker {
    private static final EntityDataAccessor<Optional<UUID>> FOCUS_PLAYER =
            SynchedEntityData.defineId(UncannyAmbusherEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final int TELEGRAPH_TICKS = 18;
    private static final int MAX_CHASE_TICKS = 20 * 9;
    private static final int SINK_TICKS = 42;

    private int state;
    private int stateTicks;
    private int lifetime;
    private boolean attackAttempted;

    public UncannyAmbusherEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        UncannyEntityUtil.applyDisplayName(this, "Ambusher?");
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FOCUS_PLAYER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    public void setup(ServerPlayer focus) {
        this.entityData.set(FOCUS_PLAYER, Optional.of(focus.getUUID()));
        this.state = 0;
        this.stateTicks = 0;
        this.lifetime = 0;
        this.attackAttempted = false;
        this.setTarget(focus);
    }

    public boolean isFocusedOn(UUID playerId) {
        return playerId != null && this.entityData.get(FOCUS_PLAYER).filter(playerId::equals).isPresent();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.setSilent(true);
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        if (state == 2) {
            tickSinking();
            return;
        }

        ServerPlayer focus = resolveFocus(level);
        if (focus == null || ++lifetime > TELEGRAPH_TICKS + MAX_CHASE_TICKS) {
            beginSinking();
            return;
        }
        com.eotv.echoofthevoid.event.special.CombatParity.maintain(this, focus, com.eotv.echoofthevoid.event.special.CombatParityRules.AMBUSHER);
        if (this.isPassenger()) {
            this.stopRiding();
        }
        this.lookAt(focus, 90.0F, 70.0F);

        if (state == 0) {
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            if (++stateTicks >= TELEGRAPH_TICKS) {
                state = 1;
                stateTicks = 0;
            }
            return;
        }

        stateTicks++;
        if (this.distanceToSqr(focus) > 28.0D * 28.0D || stateTicks > MAX_CHASE_TICKS) {
            beginSinking();
            return;
        }
        this.getNavigation().moveTo(focus, 1.24D);
        if (this.distanceToSqr(focus) <= 2.05D * 2.05D && this.hasLineOfSight(focus)) {
            this.attackAttempted = true;
            super.doHurtTarget(focus);
            beginSinking();
        }
    }

    private ServerPlayer resolveFocus(ServerLevel level) {
        UUID focusId = this.entityData.get(FOCUS_PLAYER).orElse(null);
        if (focusId == null) {
            return null;
        }
        if (this.getTarget() instanceof ServerPlayer current
                && focusId.equals(current.getUUID())
                && current.isAlive()
                && !current.isSpectator()
                && current.serverLevel() == level) {
            return current;
        }
        ServerPlayer resolved = level.getServer().getPlayerList().getPlayer(focusId);
        return Optional.ofNullable(resolved)
                .filter(player -> player.isAlive()
                        && !player.isSpectator()
                        && player.serverLevel() == level)
                .orElse(null);
    }

    private void beginSinking() {
        if (state == 2) {
            return;
        }
        state = 2;
        stateTicks = SINK_TICKS;
        this.getNavigation().stop();
        this.setTarget(null);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    private void tickSinking() {
        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(this.getX(), this.getY() - UncannySinkTransition.step(this, 0.065D, SINK_TICKS), this.getZ());
        if (UncannySinkTransition.breaksIntoOpenSpace(this)) {
            UncannySinkTransition.vanish(this);
            return;
        }
        if (--stateTicks <= 0) {
            UncannySinkTransition.vanish(this);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(FOCUS_PLAYER).ifPresent(uuid -> tag.putUUID("FocusPlayer", uuid));
        tag.putInt("State", state);
        tag.putInt("StateTicks", stateTicks);
        tag.putInt("Lifetime", lifetime);
        tag.putBoolean("AttackAttempted", attackAttempted);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("FocusPlayer")) {
            this.entityData.set(FOCUS_PLAYER, Optional.of(tag.getUUID("FocusPlayer")));
        }
        this.state = Math.max(0, Math.min(2, tag.getInt("State")));
        this.stateTicks = Math.max(0, tag.getInt("StateTicks"));
        this.lifetime = Math.max(0, tag.getInt("Lifetime"));
        this.attackAttempted = tag.getBoolean("AttackAttempted");
        if (state == 2) {
            this.setNoGravity(true);
            this.noPhysics = true;
        }
    }
}
