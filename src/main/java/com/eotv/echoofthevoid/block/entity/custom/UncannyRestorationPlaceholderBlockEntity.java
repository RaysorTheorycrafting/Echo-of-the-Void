package com.eotv.echoofthevoid.block.entity.custom;

import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import com.eotv.echoofthevoid.block.entity.UncannyBlockEntityRegistry;
import com.eotv.echoofthevoid.event.special.MinerRules;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Server-only restoration payload for one temporary Miner? tunnel cell. */
public final class UncannyRestorationPlaceholderBlockEntity extends BlockEntity {
    private BlockState originalState = Blocks.AIR.defaultBlockState();
    private long restoreAtGameTime = Long.MIN_VALUE;
    private UUID ownerEntityId;

    public UncannyRestorationPlaceholderBlockEntity(BlockPos pos, BlockState blockState) {
        super(UncannyBlockEntityRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get(), pos, blockState);
    }

    public void initialize(BlockState originalState, long restoreAtGameTime, UUID ownerEntityId) {
        this.originalState = originalState == null ? Blocks.AIR.defaultBlockState() : originalState;
        this.restoreAtGameTime = restoreAtGameTime;
        this.ownerEntityId = ownerEntityId;
        setChanged();
    }

    public BlockState originalState() {
        return originalState;
    }

    public long restoreAtGameTime() {
        return restoreAtGameTime;
    }

    public UUID ownerEntityId() {
        return ownerEntityId;
    }

    public void requestImmediateRestore() {
        this.restoreAtGameTime = Long.MIN_VALUE;
        setChanged();
    }

    /** Keeps all blocks from one mining section on the same safe restoration boundary. */
    public void deferRestoreUntil(long gameTime) {
        if (restoreAtGameTime == Long.MIN_VALUE || restoreAtGameTime < gameTime) {
            restoreAtGameTime = gameTime;
            setChanged();
        }
    }

    /** Restores immediately when the volume is free; otherwise preserves the normal safe retry. */
    public boolean restoreNowIfPossible(ServerLevel level) {
        if (!getBlockState().is(UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get())) {
            return true;
        }
        if (isUnsafeToRestore(level, getBlockPos(), ownerEntityId)) {
            this.restoreAtGameTime = level.getGameTime() + MinerRules.RESTORE_RETRY_TICKS;
            setChanged();
            return false;
        }
        BlockState restored = normalizedOriginalState();
        return level.setBlock(getBlockPos(), restored, Block.UPDATE_ALL);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            UncannyRestorationPlaceholderBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)
                || !state.is(UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get())) {
            return;
        }
        if (blockEntity.restoreAtGameTime != Long.MIN_VALUE
                && serverLevel.getGameTime() < blockEntity.restoreAtGameTime) {
            return;
        }
        blockEntity.restoreNowIfPossible(serverLevel);
    }

    private BlockState normalizedOriginalState() {
        return originalState.is(UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get())
                ? Blocks.AIR.defaultBlockState()
                : originalState;
    }

    private static boolean isUnsafeToRestore(ServerLevel level, BlockPos pos, UUID ownerEntityId) {
        AABB protectedVolume = new AABB(pos).inflate(MinerRules.RESTORATION_ENTITY_MARGIN);
        if (!level.getEntities(
                        (Entity) null,
                        protectedVolume,
                        entity -> entity.isAlive() && !(entity instanceof ItemEntity))
                .isEmpty()) {
            return true;
        }
        Entity owner = ownerEntityId == null ? null : level.getEntity(ownerEntityId);
        return owner != null
                && owner.isAlive()
                && owner.distanceToSqr(pos.getCenter())
                        <= MinerRules.RESTORATION_OWNER_CLEARANCE
                                * MinerRules.RESTORATION_OWNER_CLEARANCE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("OriginalState", NbtUtils.writeBlockState(originalState));
        tag.putLong("RestoreAtGameTime", restoreAtGameTime);
        if (ownerEntityId != null) {
            tag.putUUID("OwnerEntity", ownerEntityId);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.originalState = tag.contains("OriginalState")
                ? NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("OriginalState"))
                : Blocks.AIR.defaultBlockState();
        this.restoreAtGameTime = tag.contains("RestoreAtGameTime")
                ? tag.getLong("RestoreAtGameTime")
                : Long.MIN_VALUE;
        this.ownerEntityId = tag.hasUUID("OwnerEntity") ? tag.getUUID("OwnerEntity") : null;
    }
}
