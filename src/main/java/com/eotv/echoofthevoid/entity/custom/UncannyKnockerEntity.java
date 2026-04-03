package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.item.UncannyItemRegistry;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class UncannyKnockerEntity extends Monster implements UncannyEntityMarker {
    private enum KnockerState {
        APPROACH,
        KNOCK,
        ATTACK,
        FLEE
    }

    private static final EntityDataAccessor<Optional<UUID>> TARGET_PLAYER =
            SynchedEntityData.defineId(UncannyKnockerEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private BlockPos doorPos = BlockPos.ZERO;
    private KnockerState state = KnockerState.APPROACH;
    private int knockTicks;
    private int fleeTicks;
    private int attackCooldownTicks;
    private boolean droppedShard;
    private boolean knockPlayed;
    private boolean canAttack = true;
    private int openDoorAttackChancePercent = 20;
    private boolean sinking;
    private int sinkTicks;

    public UncannyKnockerEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Knocker?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET_PLAYER, Optional.empty());
    }

    public void setupKnockingTarget(ServerPlayer player, BlockPos doorPos) {
        this.entityData.set(TARGET_PLAYER, Optional.of(player.getUUID()));
        this.doorPos = doorPos.immutable();
        this.state = KnockerState.APPROACH;
        this.knockTicks = 0;
        this.fleeTicks = 0;
        this.attackCooldownTicks = 0;
        this.droppedShard = false;
        this.knockPlayed = false;
        this.sinking = false;
        this.sinkTicks = 0;
        this.setTarget(null);
    }

    public void setCanAttack(boolean canAttack) {
        this.canAttack = canAttack;
    }

    public void setOpenDoorAttackChancePercent(int openDoorAttackChancePercent) {
        this.openDoorAttackChancePercent = Mth.clamp(openDoorAttackChancePercent, 0, 100);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);

        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ServerPlayer targetPlayer = resolveTargetPlayer(serverLevel);
        if (targetPlayer == null || !targetPlayer.isAlive()) {
            this.discard();
            return;
        }

        if (this.sinking) {
            tickSinking();
            return;
        }

        switch (this.state) {
            case APPROACH -> tickApproach(targetPlayer);
            case KNOCK -> tickKnock(targetPlayer);
            case ATTACK -> tickAttack(targetPlayer);
            case FLEE -> tickFlee(targetPlayer);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (!hurt || this.level().isClientSide() || !this.isAlive() || !(this.level() instanceof ServerLevel serverLevel)) {
            return hurt;
        }

        // Getting hit must override knock/flee sequencing and force an immediate attack response.
        ServerPlayer attacker = source.getEntity() instanceof ServerPlayer player && player.isAlive()
                ? player
                : resolveTargetPlayer(serverLevel);
        if (attacker != null && attacker.isAlive()) {
            this.entityData.set(TARGET_PLAYER, Optional.of(attacker.getUUID()));
            this.knockTicks = 0;
            this.fleeTicks = 0;
            this.attackCooldownTicks = 0;
            beginAttack(attacker, true);
        }
        return hurt;
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
        this.entityData.get(TARGET_PLAYER).ifPresent(uuid -> tag.putUUID("TargetPlayer", uuid));
        tag.putInt("DoorX", this.doorPos.getX());
        tag.putInt("DoorY", this.doorPos.getY());
        tag.putInt("DoorZ", this.doorPos.getZ());
        tag.putString("KnockerState", this.state.name());
        tag.putInt("KnockTicks", this.knockTicks);
        tag.putInt("FleeTicks", this.fleeTicks);
        tag.putInt("AttackCooldownTicks", this.attackCooldownTicks);
        tag.putBoolean("DroppedShard", this.droppedShard);
        tag.putBoolean("KnockPlayed", this.knockPlayed);
        tag.putBoolean("CanAttack", this.canAttack);
        tag.putInt("OpenDoorAttackChancePercent", this.openDoorAttackChancePercent);
        tag.putBoolean("Sinking", this.sinking);
        tag.putInt("SinkTicks", this.sinkTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("TargetPlayer")) {
            this.entityData.set(TARGET_PLAYER, Optional.of(tag.getUUID("TargetPlayer")));
        }

        this.doorPos = new BlockPos(tag.getInt("DoorX"), tag.getInt("DoorY"), tag.getInt("DoorZ"));
        try {
            this.state = KnockerState.valueOf(tag.getString("KnockerState"));
        } catch (IllegalArgumentException ignored) {
            this.state = KnockerState.APPROACH;
        }
        this.knockTicks = Math.max(0, tag.getInt("KnockTicks"));
        this.fleeTicks = Math.max(0, tag.getInt("FleeTicks"));
        this.attackCooldownTicks = Math.max(0, tag.getInt("AttackCooldownTicks"));
        this.droppedShard = tag.getBoolean("DroppedShard");
        this.knockPlayed = tag.getBoolean("KnockPlayed");
        if (tag.contains("CanAttack", 1)) {
            this.canAttack = tag.getBoolean("CanAttack");
        } else {
            this.canAttack = true;
        }
        if (tag.contains("OpenDoorAttackChancePercent", 3)) {
            this.openDoorAttackChancePercent = Mth.clamp(tag.getInt("OpenDoorAttackChancePercent"), 0, 100);
        } else {
            this.openDoorAttackChancePercent = 20;
        }
        this.sinking = tag.getBoolean("Sinking");
        this.sinkTicks = Math.max(0, tag.getInt("SinkTicks"));
    }

    private void tickApproach(ServerPlayer targetPlayer) {
        if (this.attackCooldownTicks > 0) {
            this.attackCooldownTicks--;
        }

        this.getNavigation().moveTo(this.doorPos.getX() + 0.5D, this.doorPos.getY(), this.doorPos.getZ() + 0.5D, 1.15D);
        if (this.blockPosition().distSqr(this.doorPos) <= 4L) {
            if (isDoorCurrentlyOpen()) {
                startForcedAttack(targetPlayer);
                return;
            }
            this.state = KnockerState.KNOCK;
            this.knockTicks = 0;
            this.knockPlayed = false;
            this.getNavigation().stop();
        }
    }

    private void tickKnock(ServerPlayer targetPlayer) {
        BlockState doorState = this.level().getBlockState(this.doorPos);
        if (!(doorState.getBlock() instanceof DoorBlock)) {
            if (this.canAttack) {
                beginAttack(targetPlayer, true);
            } else {
                startFlee();
            }
            return;
        }

        boolean isOpen = doorState.hasProperty(BlockStateProperties.OPEN) && doorState.getValue(BlockStateProperties.OPEN);
        if (isOpen) {
            onDoorOpened(targetPlayer);
            return;
        }

        this.getNavigation().moveTo(this.doorPos.getX() + 0.5D, this.doorPos.getY(), this.doorPos.getZ() + 0.5D, 1.0D);
        if (!this.knockPlayed) {
            this.level().playSound(
                    null,
                    this.doorPos,
                    UncannySoundRegistry.UNCANNY_KNOCKER_KNOCK.get(),
                    SoundSource.HOSTILE,
                    3.4F,
                    0.94F + this.random.nextFloat() * 0.08F);
            this.knockPlayed = true;
        }

        if (++this.knockTicks >= 20 * 6) {
            if (isIronDoor(doorState)) {
                startFlee();
                return;
            }
            this.level().destroyBlock(this.doorPos, true, this);
            if (this.canAttack) {
                beginAttack(targetPlayer, true);
            } else {
                startFlee();
            }
        }
    }

    private void onDoorOpened(ServerPlayer targetPlayer) {
        if (this.canAttack && this.random.nextInt(100) < this.openDoorAttackChancePercent) {
            beginAttack(targetPlayer, true);
            return;
        }

        startFlee();
    }

    private void tickAttack(ServerPlayer targetPlayer) {
        this.setTarget(targetPlayer);
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.37D);
        }
        this.getNavigation().moveTo(targetPlayer, 1.32D);
        this.lookAt(targetPlayer, 80.0F, 80.0F);

        if (this.attackCooldownTicks > 0) {
            this.attackCooldownTicks--;
            return;
        }

        if (!this.hasLineOfSight(targetPlayer)) {
            return;
        }

        double reach = this.getBbWidth() * 2.1D;
        double allowedDistanceSqr = reach * reach + targetPlayer.getBbWidth();
        if (this.distanceToSqr(targetPlayer) <= allowedDistanceSqr) {
            this.swing(InteractionHand.MAIN_HAND);
            this.doHurtTarget(targetPlayer);
            this.attackCooldownTicks = 12;
        }
    }

    private void tickFlee(ServerPlayer targetPlayer) {
        if (this.isInWaterOrBubble()) {
            startSinking();
            return;
        }

        Vec3 away = this.position().subtract(targetPlayer.position());
        if (away.lengthSqr() < 0.001D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            away = away.normalize();
        }
        Vec3 destination = this.position().add(away.scale(14.0D));
        this.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.5D);

        if (--this.fleeTicks <= 0 || !targetPlayer.hasLineOfSight(this)) {
            this.discard();
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        if (!this.droppedShard) {
            ItemEntity reward = new ItemEntity(level, this.getX(), this.getY(), this.getZ(), new ItemStack(UncannyItemRegistry.UNCANNY_REALITY_SHARD.get()));
            level.addFreshEntity(reward);
            this.droppedShard = true;
        }
    }

    private void startForcedAttack(ServerPlayer targetPlayer) {
        if (!this.canAttack) {
            startFlee();
            return;
        }
        beginAttack(targetPlayer, true);
    }

    private void beginAttack(ServerPlayer targetPlayer, boolean playScream) {
        if (targetPlayer == null || !targetPlayer.isAlive()) {
            return;
        }
        this.state = KnockerState.ATTACK;
        this.attackCooldownTicks = 0;
        this.setTarget(targetPlayer);
        if (playScream) {
            this.level().playSound(
                    null,
                    this.blockPosition(),
                    UncannySoundRegistry.UNCANNY_HURLER_SCREAM.get(),
                    SoundSource.HOSTILE,
                    2.9F,
                    0.92F);
        }
    }

    private void startFlee() {
        this.state = KnockerState.FLEE;
        this.fleeTicks = 100;
        this.setTarget(null);
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

    private boolean isDoorCurrentlyOpen() {
        BlockState doorState = this.level().getBlockState(this.doorPos);
        return doorState.getBlock() instanceof DoorBlock
                && doorState.hasProperty(BlockStateProperties.OPEN)
                && doorState.getValue(BlockStateProperties.OPEN);
    }

    private boolean isIronDoor(BlockState state) {
        return state.is(Blocks.IRON_DOOR);
    }

    private ServerPlayer resolveTargetPlayer(ServerLevel level) {
        return this.entityData.get(TARGET_PLAYER)
                .map(uuid -> level.getServer().getPlayerList().getPlayer(uuid))
                .orElse(null);
    }
}
