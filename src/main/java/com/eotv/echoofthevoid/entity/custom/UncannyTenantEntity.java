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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class UncannyTenantEntity extends Monster implements UncannyEntityMarker {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_PLAYER =
            SynchedEntityData.defineId(UncannyTenantEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private BlockPos homeDoor;
    private BlockPos homeInterior;
    private boolean homeDoorInitiallyOpen = true;
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
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public void setupTenant(ServerPlayer owner, BlockPos doorPos) {
        setupTenant(owner, doorPos, null);
    }

    public void setupTenant(ServerPlayer owner, BlockPos doorPos, BlockPos interiorPos) {
        this.entityData.set(OWNER_PLAYER, Optional.of(owner.getUUID()));
        this.homeDoor = doorPos == null ? null : doorPos.immutable();
        this.homeInterior = interiorPos == null ? null : interiorPos.immutable();
        this.homeDoorInitiallyOpen = true;
        if (this.homeDoor != null) {
            BlockState doorState = this.level().getBlockState(this.homeDoor);
            if (doorState.getBlock() instanceof DoorBlock && doorState.hasProperty(BlockStateProperties.OPEN)) {
                this.homeDoorInitiallyOpen = doorState.getValue(BlockStateProperties.OPEN);
            }
        }
        this.reachedHome = false;
        this.lingerTicks = 0;
        this.noticedInside = false;
        this.vanishAfterNoticeTick = Long.MIN_VALUE;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);
        if (!this.reachedHome) {
            UncannyEntityUtil.enableDoorNavigation(this);
        }
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel level)) {
            return;
        }

        ServerPlayer owner = resolveOwner(level);
        if (owner == null || !owner.isAlive()) {
            discardAndRestoreDoor();
            return;
        }

        if (this.homeInterior != null && !this.reachedHome) {
            this.getNavigation().moveTo(
                    this.homeInterior.getX() + 0.5D,
                    this.homeInterior.getY(),
                    this.homeInterior.getZ() + 0.5D,
                    1.55D);
            if (this.position().distanceToSqr(Vec3.atBottomCenterOf(this.homeInterior)) <= 0.85D * 0.85D) {
                this.reachedHome = true;
                this.getNavigation().stop();
                this.lingerTicks = 0;
                restoreDoorState();
            }
            return;
        }

        this.getNavigation().stop();
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
            discardAndRestoreDoor();
            return;
        }

        if (this.lingerTicks > 20 * 80) {
            level.playSound(null, this.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1.0F, 0.82F);
            discardAndRestoreDoor();
        }
    }

    public boolean hasReachedHome() {
        return this.reachedHome;
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(OWNER_PLAYER).ifPresent(uuid -> tag.putUUID("OwnerPlayer", uuid));
        if (this.homeDoor != null) {
            tag.putInt("HomeDoorX", this.homeDoor.getX());
            tag.putInt("HomeDoorY", this.homeDoor.getY());
            tag.putInt("HomeDoorZ", this.homeDoor.getZ());
        }
        if (this.homeInterior != null) {
            tag.putInt("HomeInteriorX", this.homeInterior.getX());
            tag.putInt("HomeInteriorY", this.homeInterior.getY());
            tag.putInt("HomeInteriorZ", this.homeInterior.getZ());
        }
        tag.putBoolean("HomeDoorInitiallyOpen", this.homeDoorInitiallyOpen);
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
        if (tag.contains("HomeInteriorX") && tag.contains("HomeInteriorY") && tag.contains("HomeInteriorZ")) {
            this.homeInterior = new BlockPos(
                    tag.getInt("HomeInteriorX"),
                    tag.getInt("HomeInteriorY"),
                    tag.getInt("HomeInteriorZ"));
        }
        this.homeDoorInitiallyOpen = !tag.contains("HomeDoorInitiallyOpen")
                || tag.getBoolean("HomeDoorInitiallyOpen");
        this.reachedHome = tag.getBoolean("ReachedHome");
        this.lingerTicks = Math.max(0, tag.getInt("LingerTicks"));
        this.noticedInside = tag.getBoolean("NoticedInside");
        this.vanishAfterNoticeTick = tag.contains("VanishAfterNoticeTick") ? tag.getLong("VanishAfterNoticeTick") : Long.MIN_VALUE;
    }

    private ServerPlayer resolveOwner(ServerLevel level) {
        Optional<UUID> ownerUuid = this.entityData.get(OWNER_PLAYER);
        if (ownerUuid.isPresent()) {
            ServerPlayer bound = level.getServer().getPlayerList().getPlayer(ownerUuid.get());
            return bound != null
                            && bound.isAlive()
                            && !bound.isSpectator()
                            && bound.serverLevel() == level
                    ? bound
                    : null;
        }
        Player nearest = level.getNearestPlayer(this, 30.0D);
        if (nearest instanceof ServerPlayer owner && owner.isAlive() && !owner.isSpectator()) {
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

    private void discardAndRestoreDoor() {
        restoreDoorState();
        this.discard();
    }

    private void restoreDoorState() {
        if (this.homeDoorInitiallyOpen || this.homeDoor == null || this.level().isClientSide()) {
            return;
        }
        BlockState doorState = this.level().getBlockState(this.homeDoor);
        if (doorState.getBlock() instanceof DoorBlock door
                && doorState.hasProperty(BlockStateProperties.OPEN)
                && doorState.getValue(BlockStateProperties.OPEN)) {
            door.setOpen(this, this.level(), doorState, this.homeDoor, false);
        }
    }
}
