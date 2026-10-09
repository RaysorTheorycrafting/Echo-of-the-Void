package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.event.special.PercherSystem;
import com.eotv.echoofthevoid.event.special.WatcherObservationRules;
import java.util.Optional;
import java.util.UUID;
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
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Percher?: a Watcher? that keeps to high places. It crouches on tree tops, cliff edges and roofs,
 * stares, and while the player looks elsewhere it moves to a closer perch, always above them. Exactly
 * like the Watcher? it never attacks: three seconds of direct look (or a blow) make it leave, falling
 * backward off its perch and dissolving into smoke before it lands.
 */
public class UncannyPercherEntity extends Monster implements UncannyEntityMarker {
    private static final int HIDE_MIN_TICKS = 20 * 60;
    private static final int HIDE_RANDOM_TICKS = 20 * 20;
    private static final int DIRECT_LOOK_TRIGGER_TICKS = 20 * 3;
    private static final int ORPHAN_DESPAWN_TICKS = 20 * 15;
    private static final int UNSEEN_BEFORE_RELOCATION_TICKS = 30;
    private static final int MIN_RELOCATION_COOLDOWN_TICKS = 20 * 10;
    private static final int RANDOM_RELOCATION_COOLDOWN_TICKS = 20 * 10;
    private static final int MAX_FALL_TICKS = 18;
    private static final double MIN_PERCH_DISTANCE = 10.0D;
    private static final ResourceLocation OBSERVED_ADVANCEMENT_ID =
            ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny/observed");
    private static final EntityDataAccessor<Optional<UUID>> WATCHED_PLAYER =
            SynchedEntityData.defineId(UncannyPercherEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private int hideTicks = HIDE_MIN_TICKS;
    private boolean approachMode;
    private int directLookTicks;
    private int unseenTicks;
    private int relocationCooldown;
    private int fallTicks = -1;
    private int orphanTicks;
    private int caveCueTicks;
    private boolean caveCuePlayed;
    private boolean observedAwarded;

    public UncannyPercherEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        UncannyEntityUtil.applyDisplayName(this, "Percher?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WATCHED_PLAYER, Optional.empty());
    }

    public void setWatchedPlayer(ServerPlayer player) {
        this.entityData.set(WATCHED_PLAYER, Optional.of(player.getUUID()));
        this.hideTicks = HIDE_MIN_TICKS + this.random.nextInt(HIDE_RANDOM_TICKS + 1);
    }

    public Optional<UUID> getWatchedPlayerUuid() {
        return this.entityData.get(WATCHED_PLAYER);
    }

    public boolean isFalling() {
        return this.fallTicks >= 0;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel level) || this.isDeadOrDying()) {
            return;
        }
        if (isFalling()) {
            tickFall();
            return;
        }
        this.setPose(Pose.CROUCHING);
        this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);

        ServerPlayer watched = resolveWatchedPlayer(level);
        if (watched == null) {
            if (++this.orphanTicks >= ORPHAN_DESPAWN_TICKS) {
                this.discard();
            }
            return;
        }
        this.orphanTicks = 0;
        tickCaveCue(watched);
        this.lookAt(watched, 90.0F, 90.0F);
        this.setYHeadRot(this.getYRot());
        this.yBodyRot = this.getYRot();

        if (PercherSystem.isDaylight(level) && !isSeenBy(watched)) {
            UncannySinkTransition.vanish(this);
            return;
        }

        if (!this.approachMode) {
            if (--this.hideTicks <= 0) {
                this.approachMode = true;
                this.relocationCooldown = 0;
            }
        } else {
            tickRelocation(level, watched);
        }

        if (isPlayerLookingAt(watched)) {
            this.directLookTicks++;
        } else {
            this.directLookTicks = 0;
        }
        if (this.directLookTicks >= DIRECT_LOOK_TRIGGER_TICKS) {
            onObserved(watched);
            startFall(watched);
        }
    }

    /** Like the Watcher?'s approach, but by perches: only while unseen, never below the player. */
    private void tickRelocation(ServerLevel level, ServerPlayer watched) {
        if (this.relocationCooldown > 0) {
            this.relocationCooldown--;
        }
        if (isSeenBy(watched)) {
            this.unseenTicks = 0;
            return;
        }
        if (++this.unseenTicks < UNSEEN_BEFORE_RELOCATION_TICKS || this.relocationCooldown > 0) {
            return;
        }
        double distance = Math.sqrt(this.distanceToSqr(watched));
        if (distance <= MIN_PERCH_DISTANCE + 2.0D) {
            return;
        }
        double nearest = Math.max(MIN_PERCH_DISTANCE, distance * 0.6D);
        double farthest = Math.max(nearest + 1.0D, distance * 0.82D);
        BlockPos perch = PercherSystem.findPerch(level, watched, nearest, farthest, true);
        this.relocationCooldown = MIN_RELOCATION_COOLDOWN_TICKS + this.random.nextInt(RANDOM_RELOCATION_COOLDOWN_TICKS + 1);
        if (perch != null) {
            this.moveTo(perch.getX() + 0.5D, perch.getY(), perch.getZ() + 0.5D, this.getYRot(), 0.0F);
            this.unseenTicks = 0;
        }
    }

    private void startFall(ServerPlayer watched) {
        if (isFalling()) {
            return;
        }
        this.fallTicks = 0;
        this.directLookTicks = 0;
        this.setPose(Pose.STANDING);
        Vec3 away = this.position().subtract(watched.position()).multiply(1.0D, 0.0D, 1.0D);
        away = away.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : away.normalize();
        this.setDeltaMovement(away.x * 0.32D, 0.18D, away.z * 0.32D);
        this.hasImpulse = true;
    }

    private void tickFall() {
        this.setXRot(Math.min(80.0F, this.getXRot() + 9.0F));
        if (++this.fallTicks >= MAX_FALL_TICKS || (this.fallTicks > 3 && this.onGround())) {
            UncannySinkTransition.vanish(this);
        }
    }

    private boolean isSeenBy(ServerPlayer player) {
        if (!player.hasLineOfSight(this)) {
            return false;
        }
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 to = this.position().add(0.0D, 1.0D, 0.0D).subtract(player.getEyePosition()).normalize();
        return look.dot(to) >= 0.17D;
    }

    private boolean isPlayerLookingAt(ServerPlayer player) {
        boolean lineOfSight = player.hasLineOfSight(this);
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 to = this.getEyePosition().subtract(player.getEyePosition());
        double dot = to.lengthSqr() < 1.0E-4D ? 1.0D : look.dot(to.normalize());
        return WatcherObservationRules.canAccumulateDirectLook(player.isSleeping(), lineOfSight, dot);
    }

    private ServerPlayer resolveWatchedPlayer(ServerLevel level) {
        ServerPlayer watched = getWatchedPlayerUuid()
                .map(uuid -> level.getServer().getPlayerList().getPlayer(uuid))
                .orElse(null);
        if (watched != null && watched.isAlive() && !watched.isSpectator() && watched.level() == level) {
            return watched;
        }
        return null;
    }

    private void onObserved(ServerPlayer watched) {
        if (!this.caveCuePlayed) {
            sendLocalSound(watched, SoundEvents.AMBIENT_CAVE.value(), 2.8F, 0.82F + this.random.nextFloat() * 0.2F);
            this.caveCuePlayed = true;
            this.caveCueTicks = 44;
        }
        if (this.observedAwarded || watched.getServer() == null) {
            return;
        }
        AdvancementHolder advancement = watched.getServer().getAdvancements().get(OBSERVED_ADVANCEMENT_ID);
        if (advancement != null) {
            AdvancementProgress progress = watched.getAdvancements().getOrStartProgress(advancement);
            for (String criterion : progress.getRemainingCriteria()) {
                watched.getAdvancements().award(advancement, criterion);
            }
            this.observedAwarded = watched.getAdvancements().getOrStartProgress(advancement).isDone();
        }
    }

    private void tickCaveCue(ServerPlayer watched) {
        if (this.caveCueTicks-- > 0 && this.caveCueTicks % 5 == 0) {
            sendLocalSound(watched, SoundEvents.AMBIENT_CAVE.value(), 0.9F, 0.93F + this.random.nextFloat() * 0.1F);
        }
    }

    private void sendLocalSound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(Holder.direct(sound), SoundSource.AMBIENT,
                player.getX(), player.getEyeY(), player.getZ(), volume, pitch, player.level().random.nextLong()));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // /kill and the void: administrative removal must always work.
            return super.hurt(source, amount);
        }
        // Reachable but never killed, like the Watcher?: a blow or an arrow makes it leave.
        if (this.level().isClientSide() || isFalling() || !(source.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        boolean hurt = super.hurt(source, Math.min(amount, Math.max(0.0F, this.getHealth() - 1.0F)));
        onObserved(player);
        startFall(player);
        return hurt;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return isFalling() || !(source.getEntity() instanceof net.minecraft.world.entity.player.Player)
                || super.isInvulnerableTo(source);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return !isFalling();
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return !isFalling();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        getWatchedPlayerUuid().ifPresent(uuid -> tag.putUUID("WatchedPlayer", uuid));
        tag.putInt("HideTicks", this.hideTicks);
        tag.putBoolean("ApproachMode", this.approachMode);
        tag.putInt("RelocationCooldown", this.relocationCooldown);
        tag.putInt("FallTicks", this.fallTicks);
        tag.putBoolean("CaveCuePlayed", this.caveCuePlayed);
        tag.putBoolean("ObservedAwarded", this.observedAwarded);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("WatchedPlayer")) {
            this.entityData.set(WATCHED_PLAYER, Optional.of(tag.getUUID("WatchedPlayer")));
        }
        this.hideTicks = Math.max(0, tag.getInt("HideTicks"));
        this.approachMode = tag.getBoolean("ApproachMode");
        this.relocationCooldown = Math.max(0, tag.getInt("RelocationCooldown"));
        this.fallTicks = tag.contains("FallTicks") ? tag.getInt("FallTicks") : -1;
        this.caveCuePlayed = tag.getBoolean("CaveCuePlayed");
        this.observedAwarded = tag.getBoolean("ObservedAwarded");
    }
}
