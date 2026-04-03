package com.eotv.echoofthevoid.block.custom;

import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import com.eotv.echoofthevoid.block.entity.custom.UncannyAltarBlockEntity;
import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import com.eotv.echoofthevoid.phase.UncannyPhaseManager;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class UncannyAltarBlock extends Block implements EntityBlock {
    public static final BooleanProperty HAS_CUBE = BooleanProperty.create("has_cube");
    private static final long RESTART_CONFIRM_WINDOW_TICKS = 200L;

    public UncannyAltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HAS_CUBE, false));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_CUBE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        if (!canPlaceMulti(context.getLevel(), context.getClickedPos())) {
            return null;
        }
        return state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return UncannyAltarShapeData.getCellShape(0, 0, false, state.getValue(HAS_CUBE));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return UncannyAltarShapeData.getCellShape(0, 0, false, state.getValue(HAS_CUBE));
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
        boolean hasCube = state.getValue(HAS_CUBE);

        if (!hasCube && stack.is(UncannyItemRegistry.REALITY_CUBE.get())) {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }

            if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
                return ItemInteractionResult.CONSUME;
            }

            UncannyWorldState worldState = UncannyWorldState.get(serverPlayer.getServer());
            if (worldState.isPurgeActive()) {
                long now = serverPlayer.getServer().getTickCount();
                Long confirmUntilTick = worldState.getRestartConfirmUntilTick(serverPlayer.getUUID());
                if (confirmUntilTick == null || now > confirmUntilTick) {
                    worldState.setRestartConfirmUntilTick(serverPlayer.getUUID(), now + RESTART_CONFIRM_WINDOW_TICKS);
                    serverPlayer.sendSystemMessage(Component.translatable("message.echoofthevoid.altar.restart_warning"));
                    return ItemInteractionResult.CONSUME;
                }

                worldState.clearRestartConfirmUntilTick(serverPlayer.getUUID());
                UncannyPhaseManager.restartFromPurge(serverPlayer.getServer());
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            level.setBlock(pos, state.setValue(HAS_CUBE, true), 3);
            syncPartCubeState(level, pos, true);
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.90F, 0.90F);
            return ItemInteractionResult.CONSUME;
        }

        if (hasCube) {
            return openMenu(state, level, pos, player);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!state.getValue(HAS_CUBE)) {
            return InteractionResult.PASS;
        }
        ItemInteractionResult result = openMenu(state, level, pos, player);
        return result.consumesAction() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide) {
            return;
        }

        boolean oldIsAltar = oldState.is(this);
        if (!oldIsAltar) {
            placePartBlocks(level, pos, state.getValue(HAS_CUBE));
            return;
        }

        if (oldState.getValue(HAS_CUBE) != state.getValue(HAS_CUBE)) {
            syncPartCubeState(level, pos, state.getValue(HAS_CUBE));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && state.getValue(HAS_CUBE) && !level.isClientSide) {
            popResource(level, pos, new ItemStack(UncannyItemRegistry.REALITY_CUBE.get()));
        }
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            clearPartBlocks(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);

        double y = pos.getY() + (26.0D / 16.0D);
        spawnCandleParticles(level, random, pos.getX() + (24.9D / 16.0D), y, pos.getZ() + (8.325D / 16.0D));   // East
        spawnCandleParticles(level, random, pos.getX() + (-8.9D / 16.0D), y, pos.getZ() + (8.325D / 16.0D));   // West
        spawnCandleParticles(level, random, pos.getX() + (8.0D / 16.0D), y, pos.getZ() + (25.225D / 16.0D));   // South
        spawnCandleParticles(level, random, pos.getX() + (8.0D / 16.0D), y, pos.getZ() + (-8.575D / 16.0D));   // North
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UncannyAltarBlockEntity(pos, state);
    }

    private ItemInteractionResult openMenu(BlockState state, Level level, BlockPos pos, Player player) {
        if (!state.getValue(HAS_CUBE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof UncannyAltarBlockEntity altarBlockEntity) {
                serverPlayer.openMenu(altarBlockEntity, pos);
                return ItemInteractionResult.CONSUME;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static void spawnCandleParticles(Level level, RandomSource random, double x, double y, double z) {
        if (random.nextFloat() < 0.85F) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0.0D, 0.004D, 0.0D);
        }
        if (random.nextFloat() < 0.28F) {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.003D, 0.0D);
        }
    }

    private static boolean canPlaceMulti(Level level, BlockPos center) {
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dy == 0 && dx == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos target = center.offset(dx, dy, dz);
                    if (!level.getWorldBorder().isWithinBounds(target)) {
                        return false;
                    }
                    if (!level.getBlockState(target).canBeReplaced()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void placePartBlocks(Level level, BlockPos center, boolean hasCube) {
        BlockState template = UncannyBlockRegistry.UNCANNY_ALTAR_PART.get().defaultBlockState();
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dy == 0 && dx == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos partPos = center.offset(dx, dy, dz);
                    BlockState partState = template
                            .setValue(UncannyAltarPartBlock.OFFSET_X, dx + 1)
                            .setValue(UncannyAltarPartBlock.OFFSET_Z, dz + 1)
                            .setValue(UncannyAltarPartBlock.UPPER, dy == 1)
                            .setValue(UncannyAltarPartBlock.HAS_CUBE, hasCube);
                    level.setBlock(partPos, partState, 2);
                }
            }
        }
    }

    private static void syncPartCubeState(Level level, BlockPos center, boolean hasCube) {
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dy == 0 && dx == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos partPos = center.offset(dx, dy, dz);
                    BlockState current = level.getBlockState(partPos);
                    if (current.is(UncannyBlockRegistry.UNCANNY_ALTAR_PART.get())) {
                        level.setBlock(partPos, current.setValue(UncannyAltarPartBlock.HAS_CUBE, hasCube), 2);
                    }
                }
            }
        }
    }

    private static void clearPartBlocks(Level level, BlockPos center) {
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dy == 0 && dx == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos partPos = center.offset(dx, dy, dz);
                    if (level.getBlockState(partPos).is(UncannyBlockRegistry.UNCANNY_ALTAR_PART.get())) {
                        level.removeBlock(partPos, false);
                    }
                }
            }
        }
    }
}
