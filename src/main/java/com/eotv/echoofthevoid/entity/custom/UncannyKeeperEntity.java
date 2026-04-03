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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class UncannyKeeperEntity extends Monster implements UncannyEntityMarker {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_PLAYER =
            SynchedEntityData.defineId(UncannyKeeperEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private BlockPos trackedContainer;
    private BlockPos standPosition;
    private int nextContainerActionTick;
    private int lastContainerActionTick;
    private int nextContainerRetargetTick;
    private boolean containerAccessActive;
    private BlockPos activeContainer;
    private boolean reactionPending;
    private int reactionStartTick;
    private boolean sinking;
    private int sinkEndTick;
    private boolean aggressive;
    private int reactionRoll;

    public UncannyKeeperEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Keeper?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_PLAYER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.32D, false));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    public void setupKeeper(ServerPlayer owner, BlockPos initialContainer) {
        this.entityData.set(OWNER_PLAYER, Optional.of(owner.getUUID()));
        this.trackedContainer = initialContainer == null ? null : initialContainer.immutable();
        this.standPosition = null;
        this.nextContainerActionTick = this.tickCount + 20;
        this.lastContainerActionTick = Integer.MIN_VALUE;
        this.nextContainerRetargetTick = this.tickCount + 60;
        this.containerAccessActive = false;
        this.activeContainer = null;
        this.reactionPending = false;
        this.reactionStartTick = 0;
        this.sinking = false;
        this.sinkEndTick = 0;
        this.aggressive = false;
        this.reactionRoll = this.random.nextInt(100);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.enableDoorNavigation(this);
        this.setSilent(true);
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel level)) {
            return;
        }

        ServerPlayer owner = resolveOwner(level);
        if (owner == null || !owner.isAlive()) {
            stopContainerAccess(level);
            this.discard();
            return;
        }

        if (this.sinking) {
            tickSinking(level);
            return;
        }

        if (this.aggressive) {
            stopContainerAccess(level);
            this.setTarget(owner);
            this.getNavigation().moveTo(owner, 1.38D);
            return;
        }

        this.setTarget(null);
        if (this.trackedContainer == null || !isContainerValid(level, this.trackedContainer)) {
            stopContainerAccess(level);
            this.trackedContainer = findNearestContainer(level, this.blockPosition(), 14, null);
            this.standPosition = null;
        }
        if (this.trackedContainer != null
                && owner.distanceToSqr(this) <= 16.0D * 16.0D
                && this.tickCount >= this.nextContainerRetargetTick) {
            BlockPos alternate = findNearestContainer(level, this.blockPosition(), 16, this.trackedContainer);
            this.nextContainerRetargetTick = this.tickCount + 80;
            if (alternate != null) {
                stopContainerAccess(level);
                this.trackedContainer = alternate;
                this.standPosition = null;
            }
        }

        if (this.trackedContainer != null) {
            if (this.standPosition == null || !isKeeperStandPositionValid(level, this.trackedContainer, this.standPosition)) {
                this.standPosition = computeKeeperStandPosition(level, this.trackedContainer, owner);
            }
            if (this.standPosition == null) {
                stopContainerAccess(level);
                BlockPos alternate = findNearestContainer(level, this.blockPosition(), 14, this.trackedContainer);
                if (alternate != null) {
                    this.trackedContainer = alternate;
                    this.standPosition = computeKeeperStandPosition(level, this.trackedContainer, owner);
                }
                if (this.standPosition == null) {
                    this.getNavigation().stop();
                    return;
                }
            }
            BlockPos targetPos = this.standPosition;
            this.getNavigation().moveTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, 1.0D);
            this.lookAt(owner, 35.0F, 35.0F);
            this.getLookControl().setLookAt(this.trackedContainer.getX() + 0.5D, this.trackedContainer.getY() + 0.8D, this.trackedContainer.getZ() + 0.5D, 40.0F, 30.0F);

            double distToContainer = this.distanceToSqr(
                    this.trackedContainer.getX() + 0.5D,
                    this.trackedContainer.getY() + 0.5D,
                    this.trackedContainer.getZ() + 0.5D);
            boolean atStandSpot = this.standPosition != null
                    && this.distanceToSqr(
                            this.standPosition.getX() + 0.5D,
                            this.standPosition.getY(),
                            this.standPosition.getZ() + 0.5D) <= 1.75D * 1.75D;
            boolean closeEnough = distToContainer <= 2.1D * 2.1D;
            boolean clearAccess = hasContainerLineOfSight(level, this.trackedContainer);
            if (atStandSpot && closeEnough && clearAccess) {
                if (!this.containerAccessActive
                        || this.activeContainer == null
                        || !this.activeContainer.equals(this.trackedContainer)
                        || this.tickCount >= this.nextContainerActionTick) {
                    stopContainerAccess(level);
                    startContainerAccess(level, this.trackedContainer);
                    manipulateContainer(level, this.trackedContainer);
                    this.lastContainerActionTick = this.tickCount;
                    this.nextContainerActionTick = this.tickCount + 200 + this.random.nextInt(81);
                } else {
                    keepContainerOpen(level, this.trackedContainer);
                }
            } else {
                stopContainerAccess(level);
            }
        } else {
            this.getNavigation().stop();
            stopContainerAccess(level);
        }

        boolean canReactToObservation = this.containerAccessActive
                && this.activeContainer != null
                && this.trackedContainer != null
                && this.activeContainer.equals(this.trackedContainer)
                && this.lastContainerActionTick > Integer.MIN_VALUE / 2
                && (this.tickCount - this.lastContainerActionTick) <= 80
                && this.distanceToSqr(this.trackedContainer.getX() + 0.5D, this.trackedContainer.getY() + 0.5D, this.trackedContainer.getZ() + 0.5D) <= 6.25D;
        if (canReactToObservation && isDirectlyObservedBy(owner)) {
            if (!this.reactionPending) {
                this.reactionPending = true;
                this.reactionStartTick = this.tickCount;
            }
        } else if (!canReactToObservation) {
            this.reactionPending = false;
        }

        if (this.reactionPending) {
            this.getNavigation().stop();
            this.lookAt(owner, 120.0F, 120.0F);
            this.getLookControl().setLookAt(owner.getX(), owner.getEyeY(), owner.getZ(), 120.0F, 120.0F);
            if ((this.tickCount - this.reactionStartTick) >= 20) {
                this.reactionPending = false;
                stopContainerAccess(level);
                if (this.reactionRoll < 95) {
                    startSinking(level);
                    return;
                }
                this.aggressive = true;
                if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                    this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.38D);
                }
                owner.playNotifySound(UncannySoundRegistry.UNCANNY_HURLER_SCREAM.get(), SoundSource.HOSTILE, 1.1F, 0.95F);
            }
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
        if (this.trackedContainer != null) {
            tag.putInt("ContainerX", this.trackedContainer.getX());
            tag.putInt("ContainerY", this.trackedContainer.getY());
            tag.putInt("ContainerZ", this.trackedContainer.getZ());
        }
        if (this.standPosition != null) {
            tag.putInt("StandX", this.standPosition.getX());
            tag.putInt("StandY", this.standPosition.getY());
            tag.putInt("StandZ", this.standPosition.getZ());
        }
        tag.putInt("NextContainerTick", this.nextContainerActionTick);
        tag.putInt("LastContainerActionTick", this.lastContainerActionTick);
        tag.putInt("NextContainerRetargetTick", this.nextContainerRetargetTick);
        tag.putBoolean("ContainerAccessActive", this.containerAccessActive);
        if (this.activeContainer != null) {
            tag.putInt("ActiveContainerX", this.activeContainer.getX());
            tag.putInt("ActiveContainerY", this.activeContainer.getY());
            tag.putInt("ActiveContainerZ", this.activeContainer.getZ());
        }
        tag.putBoolean("ReactionPending", this.reactionPending);
        tag.putInt("ReactionStartTick", this.reactionStartTick);
        tag.putBoolean("Sinking", this.sinking);
        tag.putInt("SinkEndTick", this.sinkEndTick);
        tag.putBoolean("Aggressive", this.aggressive);
        tag.putInt("ReactionRoll", this.reactionRoll);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerPlayer")) {
            this.entityData.set(OWNER_PLAYER, Optional.of(tag.getUUID("OwnerPlayer")));
        }
        if (tag.contains("ContainerX") && tag.contains("ContainerY") && tag.contains("ContainerZ")) {
            this.trackedContainer = new BlockPos(tag.getInt("ContainerX"), tag.getInt("ContainerY"), tag.getInt("ContainerZ"));
        }
        if (tag.contains("StandX") && tag.contains("StandY") && tag.contains("StandZ")) {
            this.standPosition = new BlockPos(tag.getInt("StandX"), tag.getInt("StandY"), tag.getInt("StandZ"));
        }
        this.nextContainerActionTick = tag.getInt("NextContainerTick");
        this.lastContainerActionTick = tag.contains("LastContainerActionTick") ? tag.getInt("LastContainerActionTick") : Integer.MIN_VALUE;
        this.nextContainerRetargetTick = tag.contains("NextContainerRetargetTick") ? tag.getInt("NextContainerRetargetTick") : (this.tickCount + 60);
        this.containerAccessActive = tag.getBoolean("ContainerAccessActive");
        if (tag.contains("ActiveContainerX") && tag.contains("ActiveContainerY") && tag.contains("ActiveContainerZ")) {
            this.activeContainer = new BlockPos(tag.getInt("ActiveContainerX"), tag.getInt("ActiveContainerY"), tag.getInt("ActiveContainerZ"));
        } else {
            this.activeContainer = null;
        }
        this.reactionPending = tag.getBoolean("ReactionPending");
        this.reactionStartTick = tag.getInt("ReactionStartTick");
        this.sinking = tag.getBoolean("Sinking");
        this.sinkEndTick = tag.getInt("SinkEndTick");
        this.aggressive = tag.getBoolean("Aggressive");
        this.reactionRoll = tag.getInt("ReactionRoll");
    }

    private ServerPlayer resolveOwner(ServerLevel level) {
        Optional<UUID> ownerUuid = this.entityData.get(OWNER_PLAYER);
        if (ownerUuid.isPresent()) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUuid.get());
            if (owner != null) {
                return owner;
            }
        }
        Player nearest = level.getNearestPlayer(this, 24.0D);
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

    private static boolean isContainerValid(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof Container;
    }

    private static BlockPos findNearestContainer(ServerLevel level, BlockPos origin, int radius, BlockPos avoid) {
        BlockPos nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    if (avoid != null && avoid.equals(candidate)) {
                        continue;
                    }
                    BlockEntity blockEntity = level.getBlockEntity(candidate);
                    if (!(blockEntity instanceof Container)) {
                        continue;
                    }
                    double dist = candidate.distSqr(origin);
                    if (dist < bestDist) {
                        bestDist = dist;
                        nearest = candidate.immutable();
                    }
                }
            }
        }
        return nearest;
    }

    private static void manipulateContainer(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof Container container)) {
            return;
        }

        int size = container.getContainerSize();
        if (size <= 1) {
            return;
        }

        java.util.List<Integer> nonEmpty = new java.util.ArrayList<>();
        java.util.List<Integer> empty = new java.util.ArrayList<>();
        for (int slot = 0; slot < size; slot++) {
            if (container.getItem(slot).isEmpty()) {
                empty.add(slot);
            } else {
                nonEmpty.add(slot);
            }
        }
        if (nonEmpty.isEmpty()) {
            return;
        }

        int source = nonEmpty.get(level.random.nextInt(nonEmpty.size()));
        int destination = -1;
        if (!empty.isEmpty()) {
            destination = empty.get(level.random.nextInt(empty.size()));
        } else {
            ItemStack sourceStack = container.getItem(source);
            for (int attempt = 0; attempt < size * 3; attempt++) {
                int candidate = level.random.nextInt(size);
                if (candidate == source) {
                    continue;
                }
                ItemStack candidateStack = container.getItem(candidate);
                if (!ItemStack.isSameItemSameComponents(sourceStack, candidateStack)
                        || sourceStack.getCount() != candidateStack.getCount()) {
                    destination = candidate;
                    break;
                }
            }
            if (destination < 0) {
                destination = (source + 1) % size;
            }
        }

        ItemStack first = container.getItem(source).copy();
        ItemStack second = container.getItem(destination).copy();
        container.setItem(source, second);
        container.setItem(destination, first);

        if (nonEmpty.size() > 1 && level.random.nextBoolean()) {
            int a = nonEmpty.get(level.random.nextInt(nonEmpty.size()));
            int b = nonEmpty.get(level.random.nextInt(nonEmpty.size()));
            if (a != b) {
                ItemStack stackA = container.getItem(a).copy();
                ItemStack stackB = container.getItem(b).copy();
                container.setItem(a, stackB);
                container.setItem(b, stackA);
            }
        }

        container.setChanged();
    }

    private void startContainerAccess(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            level.blockEvent(pos, chestBlock, 1, 1);
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.85F, 0.92F + level.random.nextFloat() * 0.12F);
        } else if (state.hasProperty(BlockStateProperties.OPEN)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, true), 3);
            level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.85F, 0.92F + level.random.nextFloat() * 0.12F);
        } else {
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.85F, 0.92F + level.random.nextFloat() * 0.12F);
        }
        this.containerAccessActive = true;
        this.activeContainer = pos.immutable();
    }

    private void keepContainerOpen(ServerLevel level, BlockPos pos) {
        if ((this.tickCount % 10) != 0) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            level.blockEvent(pos, chestBlock, 1, 1);
        } else if (state.hasProperty(BlockStateProperties.OPEN) && !state.getValue(BlockStateProperties.OPEN)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, true), 3);
        }
    }

    private void stopContainerAccess(ServerLevel level) {
        if (!this.containerAccessActive || this.activeContainer == null) {
            this.containerAccessActive = false;
            this.activeContainer = null;
            return;
        }
        BlockState state = level.getBlockState(this.activeContainer);
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            level.blockEvent(this.activeContainer, chestBlock, 1, 0);
        } else if (state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN)) {
            level.setBlock(this.activeContainer, state.setValue(BlockStateProperties.OPEN, false), 3);
        }
        this.containerAccessActive = false;
        this.activeContainer = null;
    }

    private void startSinking(ServerLevel level) {
        this.sinking = true;
        this.sinkEndTick = this.tickCount + 24;
        this.setNoGravity(true);
        level.playSound(null, this.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 0.95F, 0.88F);
    }

    private void tickSinking(ServerLevel level) {
        this.setDeltaMovement(0.0D, -0.08D, 0.0D);
        this.setPos(this.getX(), this.getY() - 0.08D, this.getZ());
        if (this.tickCount >= this.sinkEndTick) {
            this.sinking = false;
            this.setNoGravity(false);
            this.discard();
        }
    }

    private static boolean isKeeperStandPositionValid(ServerLevel level, BlockPos containerPos, BlockPos standPos) {
        return standPos != null
                && standPos.distSqr(containerPos) <= 4.0D
                && level.getBlockState(standPos).isAir()
                && level.getBlockState(standPos.above()).isAir()
                && level.getBlockState(standPos.below()).isSolidRender(level, standPos.below());
    }

    private static BlockPos computeKeeperStandPosition(ServerLevel level, BlockPos containerPos, ServerPlayer owner) {
        BlockState state = level.getBlockState(containerPos);
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            BlockPos inFront = containerPos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
            if (isKeeperStandPositionValid(level, containerPos, inFront)) {
                return inFront.immutable();
            }
        }

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos candidate = containerPos.relative(direction);
            if (!isKeeperStandPositionValid(level, containerPos, candidate)) {
                continue;
            }
            double dist = candidate.distSqr(owner.blockPosition());
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate.immutable();
            }
        }
        return best;
    }

    private boolean hasContainerLineOfSight(ServerLevel level, BlockPos containerPos) {
        Vec3 eye = this.position().add(0.0D, this.getEyeHeight(), 0.0D);
        Vec3 target = Vec3.atCenterOf(containerPos);
        BlockHitResult hit = level.clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS || containerPos.equals(hit.getBlockPos());
    }
}
