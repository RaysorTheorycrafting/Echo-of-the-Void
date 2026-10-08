package com.eotv.echoofthevoid.block.custom;

import com.eotv.echoofthevoid.block.entity.UncannyBlockEntityRegistry;
import com.eotv.echoofthevoid.block.entity.custom.UncannyRestorationPlaceholderBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible, collision-free marker used while Miner?'s tunnel is open. Keeping the original state
 * in the same chunk as the replacement makes restoration survive normal save/reload cycles.
 */
public final class UncannyRestorationPlaceholderBlock extends Block implements EntityBlock {
    public UncannyRestorationPlaceholderBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UncannyRestorationPlaceholderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != UncannyBlockEntityRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) ->
                UncannyRestorationPlaceholderBlockEntity.serverTick(
                        tickerLevel,
                        pos,
                        tickerState,
                        (UncannyRestorationPlaceholderBlockEntity) blockEntity);
    }
}
