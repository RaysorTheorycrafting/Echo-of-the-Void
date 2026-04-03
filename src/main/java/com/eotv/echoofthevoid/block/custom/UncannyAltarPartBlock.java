package com.eotv.echoofthevoid.block.custom;

import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class UncannyAltarPartBlock extends Block {
    public static final IntegerProperty OFFSET_X = IntegerProperty.create("offset_x", 0, 2);
    public static final IntegerProperty OFFSET_Z = IntegerProperty.create("offset_z", 0, 2);
    public static final BooleanProperty UPPER = BooleanProperty.create("upper");
    public static final BooleanProperty HAS_CUBE = BooleanProperty.create("has_cube");

    public UncannyAltarPartBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(OFFSET_X, 1)
                .setValue(OFFSET_Z, 1)
                .setValue(UPPER, false)
                .setValue(HAS_CUBE, false));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OFFSET_X, OFFSET_Z, UPPER, HAS_CUBE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getPartShape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getPartShape(state);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        BlockPos center = getCenterPos(pos, state);
        BlockState centerState = level.getBlockState(center);
        if (centerState.getBlock() instanceof UncannyAltarBlock) {
            BlockHitResult centerHit = new BlockHitResult(hitResult.getLocation(), hitResult.getDirection(), center, hitResult.isInside());
            return centerState.useItemOn(stack, level, player, hand, centerHit);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos center = getCenterPos(pos, state);
        BlockState centerState = level.getBlockState(center);
        if (centerState.getBlock() instanceof UncannyAltarBlock) {
            BlockHitResult centerHit = new BlockHitResult(hitResult.getLocation(), hitResult.getDirection(), center, hitResult.isInside());
            return centerState.useWithoutItem(level, player, centerHit);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos center = getCenterPos(pos, state);
            BlockState centerState = level.getBlockState(center);
            if (centerState.is(UncannyBlockRegistry.UNCANNY_ALTAR.get())) {
                level.destroyBlock(center, true);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private static VoxelShape getPartShape(BlockState state) {
        int ox = state.getValue(OFFSET_X) - 1;
        int oz = state.getValue(OFFSET_Z) - 1;
        boolean upper = state.getValue(UPPER);
        boolean hasCube = state.getValue(HAS_CUBE);
        return UncannyAltarShapeData.getCellShape(ox, oz, upper, hasCube);
    }

    private static BlockPos getCenterPos(BlockPos pos, BlockState state) {
        int ox = state.getValue(OFFSET_X) - 1;
        int oz = state.getValue(OFFSET_Z) - 1;
        int y = state.getValue(UPPER) ? -1 : 0;
        return pos.offset(-ox, y, -oz);
    }
}

