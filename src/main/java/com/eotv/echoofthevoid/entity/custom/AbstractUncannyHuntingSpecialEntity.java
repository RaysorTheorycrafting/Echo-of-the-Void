package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.diagnostics.DiagnosticSeverity;
import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.sound.UncannyPhysicalSoundDelivery;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Shared persistence, targeting, silent movement and graceful removal for hunting Specials. */
public abstract class AbstractUncannyHuntingSpecialEntity extends Monster implements UncannyEntityMarker {
    private static final EntityDataAccessor<Optional<UUID>> FOCUS_PLAYER = SynchedEntityData.defineId(
            AbstractUncannyHuntingSpecialEntity.class,
            EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> SINKING = SynchedEntityData.defineId(
            AbstractUncannyHuntingSpecialEntity.class,
            EntityDataSerializers.BOOLEAN);

    private int remainingLifetimeTicks = 20 * 300;
    private int unavailableTicks;
    private int sinkTicks;
    protected int meleeCooldownTicks;
    private boolean terminalDiagnosticRecorded;

    protected AbstractUncannyHuntingSpecialEntity(
            EntityType<? extends Monster> type,
            Level level,
            String displayName) {
        super(type, level);
        UncannyEntityUtil.applyDisplayName(this, displayName);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FOCUS_PLAYER, Optional.empty());
        builder.define(SINKING, false);
    }

    public final void setupTarget(ServerPlayer player, int lifetimeTicks) {
        this.entityData.set(FOCUS_PLAYER, Optional.of(player.getUUID()));
        this.remainingLifetimeTicks = Math.max(100, lifetimeTicks);
        this.unavailableTicks = 0;
        this.sinkTicks = 0;
        this.entityData.set(SINKING, false);
        this.terminalDiagnosticRecorded = false;
        this.setPersistenceRequired();
        onTargetConfigured(player);
        refreshCombatParity(player, true);
    }

    protected void onTargetConfigured(ServerPlayer player) {
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

    public final boolean isSinking() {
        return this.entityData.get(SINKING);
    }

    @Override
    public final void aiStep() {
        super.aiStep();
        this.setSilent(true);
        if (this.meleeCooldownTicks > 0) {
            this.meleeCooldownTicks--;
        }
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.isDeadOrDying()) {
            // No bite, grab or pull may land during the death animation.
            return;
        }
        if (isSinking()) {
            tickSinking(level);
            return;
        }
        ServerPlayer focus = resolveFocus(level);
        if (focus == null) {
            if (++this.unavailableTicks >= 100) {
                beginSinking(null, "focus_unavailable");
            }
            return;
        }
        this.unavailableTicks = 0;
        if (!focus.isAlive() || focus.isSpectator() || focus.serverLevel() != level) {
            beginSinking(focus, "focus_invalid");
            return;
        }
        if (--this.remainingLifetimeTicks <= 0) {
            beginSinking(focus, "encounter_timeout");
            return;
        }
        if (this.tickCount % com.eotv.echoofthevoid.event.special.CombatParityRules.REFRESH_INTERVAL_TICKS == 0) {
            refreshCombatParity(focus, false);
        }
        tickSpecial(level, focus);
    }

    protected abstract void tickSpecial(ServerLevel level, ServerPlayer focus);

    protected final ServerPlayer resolveFocus(ServerLevel level) {
        return focusPlayerId()
                .map(level.getServer().getPlayerList()::getPlayer)
                .orElse(null);
    }

    protected final void beginSinking(ServerPlayer focus, String reason) {
        if (isSinking()) {
            return;
        }
        this.entityData.set(SINKING, true);
        this.sinkTicks = 0;
        this.setTarget(null);
        this.getNavigation().stop();
        this.setNoGravity(true);
        this.noPhysics = true;
        onBeginSinking(focus, reason);
        recordTerminal(focus, reason);
    }

    protected void onBeginSinking(ServerPlayer focus, String reason) {
    }

    private void tickSinking(ServerLevel level) {
        this.setTarget(null);
        this.getNavigation().stop();
        this.setNoGravity(true);
        this.noPhysics = true;
        double step = UncannySinkTransition.step(this, 0.045D, 40);
        this.setDeltaMovement(0.0D, -step, 0.0D);
        this.setPos(this.getX(), this.getY() - step, this.getZ());
        if (UncannySinkTransition.breaksIntoOpenSpace(this)) {
            UncannySinkTransition.vanish(this);
            return;
        }
        if (++this.sinkTicks >= 40) {
            UncannySinkTransition.vanish(this);
        }
    }

    protected final void playPhysicalCue(
            ServerLevel level,
            SoundEvent sound,
            float volume,
            float pitch) {
        UncannyPhysicalSoundDelivery.playFromEntity(
                level, this, sound, SoundSource.HOSTILE, volume, pitch);
    }

    protected final boolean isDirectlyObservedBy(ServerPlayer player) {
        return isDirectlyObserved(player, this.getEyePosition(), 0.05D);
    }

    protected final boolean isDirectlyObservedByAnyPlayer(ServerLevel level, double radius) {
        double radiusSqr = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.isAlive()
                    && !player.isSpectator()
                    && player.distanceToSqr(this) <= radiusSqr
                    && isDirectlyObservedBy(player)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDirectlyObserved(ServerPlayer observer, Vec3 target, double minimumDot) {
        Vec3 offset = target.subtract(observer.getEyePosition());
        if (offset.lengthSqr() < 1.0E-6D) {
            return true;
        }
        if (observer.getViewVector(1.0F).normalize().dot(offset.normalize()) <= minimumDot) {
            return false;
        }
        Vec3 direction = offset.normalize();
        Vec3 start = observer.getEyePosition();
        for (int transparentIntersections = 0; transparentIntersections < 32; transparentIntersections++) {
            HitResult result = observer.level().clip(new ClipContext(
                    start,
                    target,
                    ClipContext.Block.VISUAL,
                    ClipContext.Fluid.NONE,
                    observer));
            if (result.getType() == HitResult.Type.MISS) {
                return true;
            }
            if (!(result instanceof BlockHitResult blockHit)) {
                return false;
            }
            String blockPath = BuiltInRegistries.BLOCK.getKey(
                    observer.level().getBlockState(blockHit.getBlockPos()).getBlock()).getPath();
            if (!blockPath.contains("glass")) {
                return false;
            }
            start = blockHit.getLocation().add(direction.scale(0.05D));
            if (start.distanceToSqr(target) <= 0.01D) {
                return true;
            }
        }
        return false;
    }

    protected final boolean isBehind(ServerPlayer observer, double maximumForwardDot) {
        Vec3 offset = this.position().subtract(observer.position());
        Vec3 view = observer.getViewVector(1.0F);
        Vec3 horizontalOffset = new Vec3(offset.x, 0.0D, offset.z);
        Vec3 horizontalView = new Vec3(view.x, 0.0D, view.z);
        return horizontalOffset.lengthSqr() > 1.0E-4D
                && horizontalView.lengthSqr() > 1.0E-4D
                && horizontalOffset.normalize().dot(horizontalView.normalize()) <= maximumForwardDot;
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
    protected void playStepSound(BlockPos pos, BlockState state) {
        UncannyEntityUtil.suppressStepSound(this, pos, state);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        focusPlayerId().ifPresent(uuid -> tag.putUUID("HuntingFocusPlayer", uuid));
        tag.putBoolean("HuntingSinking", isSinking());
        tag.putInt("HuntingSinkTicks", this.sinkTicks);
        tag.putInt("HuntingRemainingLifetime", this.remainingLifetimeTicks);
        tag.putInt("HuntingMeleeCooldown", this.meleeCooldownTicks);
        tag.putInt("HuntingUnavailableTicks", this.unavailableTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("HuntingFocusPlayer")) {
            this.entityData.set(FOCUS_PLAYER, Optional.of(tag.getUUID("HuntingFocusPlayer")));
        }
        this.entityData.set(SINKING, tag.getBoolean("HuntingSinking"));
        this.sinkTicks = Math.max(0, tag.getInt("HuntingSinkTicks"));
        this.remainingLifetimeTicks = tag.contains("HuntingRemainingLifetime")
                ? Math.max(1, tag.getInt("HuntingRemainingLifetime"))
                : tag.contains("HuntingMaximumLifetime")
                        ? Math.max(1, tag.getInt("HuntingMaximumLifetime"))
                        : 20 * 300;
        this.meleeCooldownTicks = Math.max(0, tag.getInt("HuntingMeleeCooldown"));
        this.unavailableTicks = Math.max(0, tag.getInt("HuntingUnavailableTicks"));
        if (isSinking()) {
            this.setNoGravity(true);
            this.noPhysics = true;
        }
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

    private void recordTerminal(ServerPlayer focus, String reason) {
        if (this.terminalDiagnosticRecorded) {
            return;
        }
        this.terminalDiagnosticRecorded = true;
        trace(focus, "removal", reason, "encounter_age_ticks", this.tickCount);
    }

    protected abstract String specialId();
}
