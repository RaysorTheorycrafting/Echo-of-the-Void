package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class UncannyTenantEntity extends Monster implements UncannyEntityMarker {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_PLAYER =
            SynchedEntityData.defineId(UncannyTenantEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private BlockPos homeDoor;
    private boolean reachedHome;
    private int lingerTicks;
    private boolean noticedInside;
    private long vanishAfterNoticeTick = Long.MIN_VALUE;

    public UncannyTenantEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Tenant?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_PLAYER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.75D));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public void setupTenant(ServerPlayer owner, BlockPos doorPos) {
        this.entityData.set(OWNER_PLAYER, Optional.of(owner.getUUID()));
        this.homeDoor = doorPos == null ? null : doorPos.immutable();
        this.reachedHome = false;
        this.lingerTicks = 0;
        this.noticedInside = false;
        this.vanishAfterNoticeTick = Long.MIN_VALUE;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);
        UncannyEntityUtil.enableDoorNavigation(this);
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel level)) {
            return;
        }

        ServerPlayer owner = resolveOwner(level);
        if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
        }

        if (this.homeDoor != null && !this.reachedHome) {
            this.getNavigation().moveTo(this.homeDoor.getX() + 0.5D, this.homeDoor.getY(), this.homeDoor.getZ() + 0.5D, 1.55D);
            if (this.blockPosition().distSqr(this.homeDoor) <= 4) {
                this.reachedHome = true;
                this.getNavigation().stop();
                this.lingerTicks = 0;
            }
            return;
        }

        this.lingerTicks++;
        if (this.lingerTicks % 20 == 0) {
            this.lookAt(owner, 40.0F, 40.0F);
            this.getLookControl().setLookAt(owner.getX(), owner.getEyeY(), owner.getZ(), 40.0F, 40.0F);
        }

        long now = level.getGameTime();
        if (!this.noticedInside && isDirectlyObservedBy(owner)) {
            this.noticedInside = true;
            this.vanishAfterNoticeTick = now + 55L;
        }

        if (this.noticedInside && now >= this.vanishAfterNoticeTick) {
            level.playSound(null, this.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1.0F, 0.82F);
            this.discard();
            return;
        }

        if (this.lingerTicks > 20 * 80) {
            level.playSound(null, this.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1.0F, 0.82F);
            this.discard();
        }
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
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        UncannyEntityUtil.dropPulseStyleRewards(level, this, this.random);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(OWNER_PLAYER).ifPresent(uuid -> tag.putUUID("OwnerPlayer", uuid));
        if (this.homeDoor != null) {
            tag.putInt("HomeDoorX", this.homeDoor.getX());
            tag.putInt("HomeDoorY", this.homeDoor.getY());
            tag.putInt("HomeDoorZ", this.homeDoor.getZ());
        }
        tag.putBoolean("ReachedHome", this.reachedHome);
        tag.putInt("LingerTicks", this.lingerTicks);
        tag.putBoolean("NoticedInside", this.noticedInside);
        tag.putLong("VanishAfterNoticeTick", this.vanishAfterNoticeTick);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerPlayer")) {
            this.entityData.set(OWNER_PLAYER, Optional.of(tag.getUUID("OwnerPlayer")));
        }
        if (tag.contains("HomeDoorX") && tag.contains("HomeDoorY") && tag.contains("HomeDoorZ")) {
            this.homeDoor = new BlockPos(tag.getInt("HomeDoorX"), tag.getInt("HomeDoorY"), tag.getInt("HomeDoorZ"));
        }
        this.reachedHome = tag.getBoolean("ReachedHome");
        this.lingerTicks = Math.max(0, tag.getInt("LingerTicks"));
        this.noticedInside = tag.getBoolean("NoticedInside");
        this.vanishAfterNoticeTick = tag.contains("VanishAfterNoticeTick") ? tag.getLong("VanishAfterNoticeTick") : Long.MIN_VALUE;
    }

    private ServerPlayer resolveOwner(ServerLevel level) {
        Optional<UUID> ownerUuid = this.entityData.get(OWNER_PLAYER);
        if (ownerUuid.isPresent()) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUuid.get());
            if (owner != null) {
                return owner;
            }
        }
        Player nearest = level.getNearestPlayer(this, 30.0D);
        if (nearest instanceof ServerPlayer owner) {
            this.entityData.set(OWNER_PLAYER, Optional.of(owner.getUUID()));
            return owner;
        }
        return null;
    }

    private boolean isDirectlyObservedBy(ServerPlayer owner) {
        if (!owner.hasLineOfSight(this)) {
            return false;
        }
        Vec3 toEntity = this.position().add(0.0D, this.getEyeHeight(), 0.0D).subtract(owner.getEyePosition()).normalize();
        Vec3 look = owner.getViewVector(1.0F).normalize();
        return look.dot(toEntity) > 0.94D;
    }
}
