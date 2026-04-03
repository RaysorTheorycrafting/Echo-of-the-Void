package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
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
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class UncannyHurlerEntity extends Monster implements UncannyEntityMarker {
    private static final int MODE_STALK = 0;
    private static final int MODE_FLEE = 1;
    private static final int MODE_ATTACK = 2;

    private static final EntityDataAccessor<Integer> MODE_DATA =
            SynchedEntityData.defineId(UncannyHurlerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> WATCHED_PLAYER =
            SynchedEntityData.defineId(UncannyHurlerEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private int mode = MODE_STALK;
    private int modeTicks = 140;
    private int unseenTicks;
    private int meleeCooldownTicks;
    private int attackChancePercent = 10;
    private int directLookTicks;
    private boolean sinking;
    private int sinkTicks;

    public UncannyHurlerEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Hurler?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MODE_DATA, MODE_STALK);
        builder.define(WATCHED_PLAYER, Optional.empty());
    }

    public void setWatchedPlayer(ServerPlayer player) {
        this.entityData.set(WATCHED_PLAYER, Optional.of(player.getUUID()));
    }

    public void setAttackChancePercent(int attackChancePercent) {
        this.attackChancePercent = Mth.clamp(attackChancePercent, 0, 100);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);
        UncannyEntityUtil.enableDoorNavigation(this);
        this.mode = this.entityData.get(MODE_DATA);

        if (this.mode == MODE_STALK) {
            this.setShiftKeyDown(true);
            this.setPose(Pose.CROUCHING);
        } else {
            this.setShiftKeyDown(false);
            this.setPose(Pose.STANDING);
        }

        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ServerPlayer player = resolveWatchedPlayer(serverLevel);
        if (player == null || !player.isAlive()) {
            Player nearest = this.level().getNearestPlayer(this, 26.0D);
            if (nearest instanceof ServerPlayer serverPlayer && serverPlayer.isAlive()) {
                setWatchedPlayer(serverPlayer);
                player = serverPlayer;
            }
        }

        if (player == null || !player.isAlive()) {
            return;
        }

        if (this.sinking) {
            tickSinking();
            return;
        }

        if (this.meleeCooldownTicks > 0) {
            this.meleeCooldownTicks--;
        }

        if (this.mode == MODE_STALK) {
            this.setTarget(null);
            if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.27D);
            }
            stalkAround(player);

            if (shouldScreamAt(player)) {
                triggerReaction(player);
                return;
            }

            if (--this.modeTicks <= 0) {
                this.modeTicks = 90 + this.random.nextInt(81);
            }
            return;
        }

        if (this.mode == MODE_ATTACK) {
            tickAttack(player);
            if (--this.modeTicks <= 0) {
                beginFlee();
            }
            return;
        }

        // FLEE
        this.setTarget(null);
        if (this.isInWaterOrBubble()) {
            startSinking();
            return;
        }
        fleeFrom(player);
        if (isOutOfView(player)) {
            this.unseenTicks++;
        } else {
            this.unseenTicks = 0;
        }

        if (--this.modeTicks <= 0 || this.unseenTicks >= 28) {
            this.discard();
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        UncannyEntityUtil.suppressStepSound(this, pos, state);
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(WATCHED_PLAYER).ifPresent(uuid -> tag.putUUID("WatchedPlayer", uuid));
        tag.putInt("Mode", this.mode);
        tag.putInt("ModeTicks", this.modeTicks);
        tag.putInt("UnseenTicks", this.unseenTicks);
        tag.putInt("MeleeCooldownTicks", this.meleeCooldownTicks);
        tag.putInt("AttackChancePercent", this.attackChancePercent);
        tag.putInt("DirectLookTicks", this.directLookTicks);
        tag.putBoolean("Sinking", this.sinking);
        tag.putInt("SinkTicks", this.sinkTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("WatchedPlayer")) {
            this.entityData.set(WATCHED_PLAYER, Optional.of(tag.getUUID("WatchedPlayer")));
        }

        if (tag.contains("Mode", 3)) {
            this.mode = Mth.clamp(tag.getInt("Mode"), MODE_STALK, MODE_ATTACK);
        } else if (tag.contains("Mode", 8)) {
            this.mode = switch (tag.getString("Mode")) {
                case "ATTACK" -> MODE_ATTACK;
                case "FLEE" -> MODE_FLEE;
                default -> MODE_STALK;
            };
        } else {
            this.mode = MODE_STALK;
        }

        this.modeTicks = Math.max(1, tag.getInt("ModeTicks"));
        this.unseenTicks = Math.max(0, tag.getInt("UnseenTicks"));
        this.meleeCooldownTicks = Math.max(0, tag.getInt("MeleeCooldownTicks"));
        this.directLookTicks = Math.max(0, tag.getInt("DirectLookTicks"));
        if (tag.contains("AttackChancePercent", 3)) {
            this.attackChancePercent = Mth.clamp(tag.getInt("AttackChancePercent"), 0, 100);
        } else {
            this.attackChancePercent = 10;
        }
        this.sinking = tag.getBoolean("Sinking");
        this.sinkTicks = Math.max(0, tag.getInt("SinkTicks"));
        this.entityData.set(MODE_DATA, this.mode);
    }

    private ServerPlayer resolveWatchedPlayer(ServerLevel level) {
        return this.entityData.get(WATCHED_PLAYER)
                .map(uuid -> level.getServer().getPlayerList().getPlayer(uuid))
                .orElse(null);
    }

    private void stalkAround(ServerPlayer player) {
        double verticalDelta = player.getY() - this.getY();
        if (Math.abs(verticalDelta) > 1.2D) {
            this.getNavigation().moveTo(player, 1.06D);
            this.lookAt(player, 70.0F, 60.0F);
            return;
        }

        double desiredDistance = 6.5D + this.random.nextDouble() * 4.5D;
        Vec3 offset = new Vec3(1.0D, 0.0D, 0.0D)
                .yRot((float) ((this.tickCount * 0.075D) + this.getId() * 0.55D))
                .scale(desiredDistance);
        Vec3 destination = player.position().add(offset);
        this.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.00D);
        this.lookAt(player, 70.0F, 60.0F);
    }

    private boolean shouldScreamAt(ServerPlayer player) {
        if (this.distanceToSqr(player) <= 9.0D) {
            return true;
        }

        if (!this.hasLineOfSight(player) || !player.hasLineOfSight(this)) {
            return false;
        }

        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 toHurler = this.position().subtract(player.getEyePosition()).normalize();
        boolean directlyLooked = this.distanceToSqr(player) <= 8.0D * 8.0D && look.dot(toHurler) > 0.985D;
        if (directlyLooked) {
            this.directLookTicks++;
        } else {
            this.directLookTicks = 0;
        }
        return this.directLookTicks >= 8;
    }

    private void triggerReaction(ServerPlayer player) {
        this.level().playSound(null, player, UncannySoundRegistry.UNCANNY_HURLER_SCREAM.get(), this.getSoundSource(), 2.8F, 0.85F);
        if (this.random.nextInt(100) < this.attackChancePercent) {
            this.setMode(MODE_ATTACK);
            this.modeTicks = 130 + this.random.nextInt(71);
            this.setTarget(player);
            if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.40D);
            }
            return;
        }
        beginFlee();
    }

    private void beginFlee() {
        this.setMode(MODE_FLEE);
        this.modeTicks = 120 + this.random.nextInt(61);
        this.unseenTicks = 0;
        this.meleeCooldownTicks = 0;
        this.directLookTicks = 0;
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.48D);
        }
    }

    private void startSinking() {
        if (this.sinking) {
            return;
        }
        this.sinking = true;
        this.sinkTicks = 34 + this.random.nextInt(14);
        this.getNavigation().stop();
        this.setTarget(null);
    }

    private void tickSinking() {
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setDeltaMovement(0.0D, -0.05D, 0.0D);
        this.setPos(this.getX(), this.getY() - 0.05D, this.getZ());
        if (--this.sinkTicks <= 0) {
            this.discard();
        }
    }

    private void tickAttack(ServerPlayer player) {
        this.setTarget(player);
        this.getNavigation().moveTo(player, 1.35D);
        this.lookAt(player, 80.0F, 80.0F);

        if (this.meleeCooldownTicks > 0 || !this.hasLineOfSight(player)) {
            return;
        }

        double reach = this.getBbWidth() * 2.1D;
        double allowedDistanceSqr = reach * reach + player.getBbWidth();
        if (this.distanceToSqr(player) <= allowedDistanceSqr) {
            this.swing(InteractionHand.MAIN_HAND);
            this.doHurtTarget(player);
            this.meleeCooldownTicks = 12;
        }
    }

    private void fleeFrom(ServerPlayer player) {
        Vec3 away = this.position().subtract(player.position());
        if (away.lengthSqr() < 0.001D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            away = away.normalize();
        }

        Vec3 destination = this.position().add(away.scale(15.0D));
        this.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.62D);
    }

    private boolean isOutOfView(ServerPlayer player) {
        if (!player.hasLineOfSight(this)) {
            return true;
        }
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 toHurler = this.position().subtract(player.getEyePosition()).normalize();
        return look.dot(toHurler) < 0.12D;
    }

    private void setMode(int mode) {
        this.mode = Mth.clamp(mode, MODE_STALK, MODE_ATTACK);
        this.entityData.set(MODE_DATA, this.mode);
    }
}


