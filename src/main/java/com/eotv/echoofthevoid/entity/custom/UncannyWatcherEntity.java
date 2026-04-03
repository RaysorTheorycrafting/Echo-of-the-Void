package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class UncannyWatcherEntity extends Monster implements UncannyEntityMarker {
    private static final int HIDE_MIN_TICKS = 20 * 60;
    private static final int HIDE_RANDOM_TICKS = 20 * 20;
    private static final int DIRECT_LOOK_TRIGGER_TICKS = 20 * 3;
    private static final int ORPHAN_DESPAWN_TICKS = 20 * 15;

    private static final EntityDataAccessor<Optional<UUID>> WATCHED_PLAYER =
            SynchedEntityData.defineId(UncannyWatcherEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final ResourceLocation OBSERVED_ADVANCEMENT_ID =
            ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny/observed");
    private int fleeTicks;
    private int unseenTicks;
    private int sinkTicks;
    private int hideTicks = HIDE_MIN_TICKS;
    private int directLookTicks;
    private boolean sinking;
    private boolean approachMode;
    private boolean caveCuePlayed;
    private boolean observedAwarded;
    private int caveCueTicks;
    private int orphanTicks;

    public UncannyWatcherEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        UncannyEntityUtil.applyDisplayName(this, "Watcher?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WATCHED_PLAYER, Optional.empty());
    }

    public void setWatchedPlayer(ServerPlayer player) {
        this.entityData.set(WATCHED_PLAYER, Optional.of(player.getUUID()));
    }

    public Optional<UUID> getWatchedPlayerUuid() {
        return this.entityData.get(WATCHED_PLAYER);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);
        UncannyEntityUtil.enableDoorNavigation(this);

        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ServerPlayer watched = resolveWatchedPlayer(serverLevel);
        if (watched == null || !watched.isAlive() || watched.isSpectator()) {
            if (++this.orphanTicks >= ORPHAN_DESPAWN_TICKS) {
                this.discard();
            } else {
                this.getNavigation().stop();
            }
            return;
        }
        this.orphanTicks = 0;

        if (!this.sinking) {
            this.noPhysics = false;
            this.setNoGravity(false);
        }

        tickCaveCue(watched);

        if (this.sinking) {
            tickSinking(watched);
            return;
        }

        if (this.fleeTicks > 0) {
            tickFlee(watched);
            return;
        }

        if (!this.approachMode) {
            tickHiddenObserve(watched);
            if (this.hideTicks > 0) {
                this.hideTicks--;
            }
            if (this.hideTicks <= 0) {
                this.approachMode = true;
                this.directLookTicks = 0;
                this.unseenTicks = 0;
            }
        } else {
            tickApproachObserve(watched);
        }

        if (isPlayerLookingAtWatcher(watched)) {
            this.directLookTicks++;
        } else {
            this.directLookTicks = 0;
        }

        if (this.directLookTicks >= DIRECT_LOOK_TRIGGER_TICKS) {
            this.fleeTicks = 165 + this.random.nextInt(91);
            this.unseenTicks = 0;
            this.directLookTicks = 0;
            onObservedTrigger(watched);
        }
    }

    private void tickHiddenObserve(ServerPlayer watched) {
        this.lookAt(watched, 80.0F, 80.0F);
        this.setYHeadRot(this.getYRot());

        double distanceSq = this.distanceToSqr(watched);
        if (distanceSq < 68.0D * 68.0D) {
            this.getNavigation().stop();
            return;
        }

        if (distanceSq > 112.0D * 112.0D) {
            this.getNavigation().moveTo(watched, 0.86D);
            return;
        }

        this.getNavigation().stop();
    }

    private void tickApproachObserve(ServerPlayer watched) {
        this.lookAt(watched, 90.0F, 80.0F);
        this.setYHeadRot(this.getYRot());

        if (!isPlayerFacingAway(watched)) {
            this.getNavigation().stop();
            return;
        }

        double distanceSq = this.distanceToSqr(watched);
        if (distanceSq > 26.0D * 26.0D) {
            this.getNavigation().moveTo(watched, 0.38D);
            return;
        }
        if (distanceSq > 14.0D * 14.0D) {
            this.getNavigation().moveTo(watched, 0.30D);
            return;
        }
        if (distanceSq < 8.0D * 8.0D) {
            this.getNavigation().stop();
            return;
        }

        this.getNavigation().stop();
    }

    private void tickFlee(ServerPlayer watched) {
        if (this.isInWaterOrBubble()) {
            startSinking(watched, false);
            return;
        }

        fleeFrom(watched, 1.95D, 16.0D);

        if (isOutOfView(watched)) {
            this.unseenTicks++;
        } else {
            this.unseenTicks = 0;
        }

        this.fleeTicks--;
        if (this.unseenTicks >= 18 || this.fleeTicks <= 0) {
            startSinking(watched, false);
        }
    }

    private void tickSinking(ServerPlayer watched) {
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(this.getX(), this.getY() - 0.09D, this.getZ());
        if (--this.sinkTicks <= 0 || this.getY() < watched.getY() - 6.5D) {
            this.discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.getWatchedPlayerUuid().ifPresent(uuid -> tag.putUUID("WatchedPlayer", uuid));
        tag.putInt("FleeTicks", fleeTicks);
        tag.putInt("UnseenTicks", unseenTicks);
        tag.putInt("SinkTicks", sinkTicks);
        tag.putInt("HideTicks", hideTicks);
        tag.putInt("DirectLookTicks", directLookTicks);
        tag.putBoolean("ApproachMode", approachMode);
        tag.putBoolean("Sinking", sinking);
        tag.putBoolean("CaveCuePlayed", caveCuePlayed);
        tag.putBoolean("ObservedAwarded", observedAwarded);
        tag.putInt("CaveCueTicks", caveCueTicks);
        tag.putInt("OrphanTicks", orphanTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("WatchedPlayer")) {
            this.entityData.set(WATCHED_PLAYER, Optional.of(tag.getUUID("WatchedPlayer")));
        }
        this.fleeTicks = tag.getInt("FleeTicks");
        this.unseenTicks = tag.getInt("UnseenTicks");
        this.sinkTicks = tag.getInt("SinkTicks");
        this.hideTicks = Math.max(0, tag.getInt("HideTicks"));
        this.directLookTicks = tag.getInt("DirectLookTicks");
        this.approachMode = tag.getBoolean("ApproachMode");
        this.sinking = tag.getBoolean("Sinking");
        this.caveCuePlayed = tag.getBoolean("CaveCuePlayed");
        this.observedAwarded = tag.getBoolean("ObservedAwarded");
        this.caveCueTicks = tag.getInt("CaveCueTicks");
        this.orphanTicks = Math.max(0, tag.getInt("OrphanTicks"));
        if (this.hideTicks <= 0 && !this.approachMode && this.fleeTicks <= 0 && !this.sinking) {
            this.hideTicks = HIDE_MIN_TICKS + this.random.nextInt(HIDE_RANDOM_TICKS + 1);
        }
    }

    private void fleeFrom(ServerPlayer watched, double speed, double distance) {
        Vec3 away = this.position().subtract(watched.position());
        if (away.lengthSqr() < 0.001D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            away = away.normalize();
        }

        Vec3 destination = this.position().add(away.scale(distance));
        this.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
        Vec3 boosted = this.getDeltaMovement().scale(0.25D).add(away.scale(0.38D));
        this.setDeltaMovement(boosted.x, this.getDeltaMovement().y, boosted.z);
        this.hasImpulse = true;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        UncannyEntityUtil.suppressStepSound(this, pos, blockState);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    private boolean isPlayerLookingAtWatcher(ServerPlayer player) {
        if (!player.hasLineOfSight(this)) {
            return false;
        }
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 toWatcher = this.getEyePosition().subtract(player.getEyePosition());
        if (toWatcher.lengthSqr() < 0.0001D) {
            return true;
        }
        return look.dot(toWatcher.normalize()) > 0.93D;
    }

    private boolean isOutOfView(ServerPlayer player) {
        if (!player.hasLineOfSight(this)) {
            return true;
        }
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 toWatcher = this.position().subtract(player.getEyePosition()).normalize();
        return look.dot(toWatcher) < 0.18D;
    }

    private boolean isPlayerFacingAway(ServerPlayer player) {
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 toWatcher = this.position().subtract(player.getEyePosition());
        if (toWatcher.lengthSqr() < 0.0001D) {
            return false;
        }
        return look.dot(toWatcher.normalize()) < 0.05D;
    }

    private ServerPlayer resolveWatchedPlayer(ServerLevel level) {
        Optional<UUID> watchedUuid = this.getWatchedPlayerUuid();
        ServerPlayer watched = watchedUuid.map(uuid -> level.getServer().getPlayerList().getPlayer(uuid)).orElse(null);
        if (watched != null && watched.isAlive() && !watched.isSpectator() && watched.level() == level) {
            return watched;
        }

        ServerPlayer fallback = null;
        double nearest = Double.MAX_VALUE;
        for (ServerPlayer candidate : level.players()) {
            if (candidate.isSpectator() || !candidate.isAlive()) {
                continue;
            }
            double distance = this.distanceToSqr(candidate);
            if (distance < nearest) {
                nearest = distance;
                fallback = candidate;
            }
        }

        if (fallback != null) {
            this.setWatchedPlayer(fallback);
            return fallback;
        }
        return null;
    }

    private void onObservedTrigger(ServerPlayer watched) {
        playCaveCue(watched);
        if (this.observedAwarded) {
            return;
        }

        if (watched.getServer() == null) {
            return;
        }

        AdvancementHolder advancement = watched.getServer().getAdvancements().get(OBSERVED_ADVANCEMENT_ID);
        if (advancement != null) {
            AdvancementProgress progress = watched.getAdvancements().getOrStartProgress(advancement);
            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    watched.getAdvancements().award(advancement, criterion);
                }
            }

            if (watched.getAdvancements().getOrStartProgress(advancement).isDone()) {
                this.observedAwarded = true;
                if (UncannyConfig.DEBUG_LOGS.get()) {
                    EchoOfTheVoid.LOGGER.info("Watcher observed advancement granted to {}", watched.getGameProfile().getName());
                }
                return;
            }
        }

        // Fallback path if criteria awarding failed for any reason.
        CommandSourceStack source = watched.getServer()
                .createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput();
        watched.getServer().getCommands().performPrefixedCommand(
                source,
                "advancement grant " + watched.getGameProfile().getName() + " only " + OBSERVED_ADVANCEMENT_ID);

        if (advancement != null && watched.getAdvancements().getOrStartProgress(advancement).isDone()) {
            this.observedAwarded = true;
            if (UncannyConfig.DEBUG_LOGS.get()) {
                EchoOfTheVoid.LOGGER.info("Watcher observed advancement granted via fallback to {}", watched.getGameProfile().getName());
            }
        }
    }

    private void startSinking(ServerPlayer watched, boolean withCue) {
        if (this.sinking) {
            return;
        }

        this.sinking = true;
        this.sinkTicks = 36 + this.random.nextInt(15);
        this.getNavigation().stop();

        if (withCue) {
            onObservedTrigger(watched);
        }
    }

    private void playCaveCue(ServerPlayer watched) {
        if (this.caveCuePlayed) {
            return;
        }

        sendLocalSound(watched, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 2.8F, 0.82F + this.random.nextFloat() * 0.2F);
        this.caveCuePlayed = true;
        this.caveCueTicks = 44;
    }

    private void tickCaveCue(ServerPlayer watched) {
        if (this.caveCueTicks-- <= 0) {
            return;
        }

        if ((this.caveCueTicks % 5) != 0) {
            return;
        }

        sendLocalSound(watched, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.9F, 0.93F + this.random.nextFloat() * 0.1F);
    }

    private void sendLocalSound(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(
                Holder.direct(sound),
                source,
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                volume,
                pitch,
                player.level().random.nextLong()));
    }
}
