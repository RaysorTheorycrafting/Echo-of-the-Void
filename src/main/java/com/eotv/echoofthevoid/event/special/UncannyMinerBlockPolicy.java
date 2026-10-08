package com.eotv.echoofthevoid.event.special;

import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/** Strict allow-list for temporary world mutations made by Miner?. */
public final class UncannyMinerBlockPolicy {
    private static final Set<Block> NATURAL_SOILS = Set.of(
            Blocks.DIRT,
            Blocks.COARSE_DIRT,
            Blocks.ROOTED_DIRT,
            Blocks.PODZOL,
            Blocks.CLAY,
            Blocks.MUD,
            Blocks.PACKED_MUD,
            Blocks.TUFF,
            Blocks.CALCITE,
            Blocks.DRIPSTONE_BLOCK,
            Blocks.SOUL_SAND,
            Blocks.SOUL_SOIL);
    private static final Set<Block> SIMPLE_CONSTRUCTION = Set.of(
            Blocks.COBBLESTONE,
            Blocks.MOSSY_COBBLESTONE,
            Blocks.STONE_BRICKS,
            Blocks.MOSSY_STONE_BRICKS,
            Blocks.CRACKED_STONE_BRICKS,
            Blocks.CHISELED_STONE_BRICKS,
            Blocks.DEEPSLATE_BRICKS,
            Blocks.CRACKED_DEEPSLATE_BRICKS,
            Blocks.DEEPSLATE_TILES,
            Blocks.CRACKED_DEEPSLATE_TILES,
            Blocks.POLISHED_DEEPSLATE,
            Blocks.BRICKS,
            Blocks.MUD_BRICKS,
            Blocks.NETHER_BRICKS,
            Blocks.RED_NETHER_BRICKS);

    private UncannyMinerBlockPolicy() {
    }

    public static MaterialKind classify(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
            return MaterialKind.BLOCKED;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()
                || !state.getFluidState().isEmpty()
                || state.hasBlockEntity()
                || level.getBlockEntity(pos) != null
                || state.is(UncannyBlockRegistry.UNCANNY_BLOCK.get())
                || state.is(UncannyBlockRegistry.UNCANNY_ALTAR.get())
                || state.is(UncannyBlockRegistry.UNCANNY_ALTAR_PART.get())
                || state.is(UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get())
                || state.getBlock() instanceof FallingBlock
                || state.getDestroySpeed(level, pos) < 0.0F
                || !hasSafeNeighbours(level, pos)) {
            return MaterialKind.BLOCKED;
        }
        if (isNatural(state)) {
            return MaterialKind.NATURAL;
        }
        if (state.is(BlockTags.PLANKS) || SIMPLE_CONSTRUCTION.contains(state.getBlock())) {
            return MaterialKind.CONSTRUCTION;
        }
        return MaterialKind.BLOCKED;
    }

    /**
     * Classifies one complete two-block-high tunnel section. A construction block in either
     * half consumes one of Miner?'s two construction-section allowances.
     */
    public static MaterialKind classifyColumn(Level level, BlockPos base) {
        MaterialKind lower = classify(level, base);
        MaterialKind upper = classify(level, base.above());
        if (lower == MaterialKind.BLOCKED || upper == MaterialKind.BLOCKED) {
            return MaterialKind.BLOCKED;
        }
        return lower == MaterialKind.CONSTRUCTION || upper == MaterialKind.CONSTRUCTION
                ? MaterialKind.CONSTRUCTION
                : MaterialKind.NATURAL;
    }

    /**
     * Classifies a diagonal upward step. Unlike an ordinary tunnel column, one half may already be
     * air at the edge of the surface; at least one actual safe block must still be excavated.
     */
    public static MaterialKind classifyAscendingColumn(Level level, BlockPos base) {
        MaterialKind lower = classifyPresentBlock(level, base);
        MaterialKind upper = classifyPresentBlock(level, base.above());
        if (lower == MaterialKind.BLOCKED || upper == MaterialKind.BLOCKED) {
            return MaterialKind.BLOCKED;
        }
        if (lower == null && upper == null) {
            return MaterialKind.BLOCKED;
        }
        return lower == MaterialKind.CONSTRUCTION || upper == MaterialKind.CONSTRUCTION
                ? MaterialKind.CONSTRUCTION
                : MaterialKind.NATURAL;
    }

    /**
     * Classifies every block required to walk a real one-block ascent. A two-block destination is
     * not sufficient underground: the mob's head still overlaps the ceiling above the column it
     * is leaving during the jump. That transition headroom therefore belongs to the same safety
     * decision and construction allowance as the ascending section.
     */
    public static MaterialKind classifyAscendingTransition(Level level, BlockPos fromBase, BlockPos nextBase) {
        if (level == null || fromBase == null || nextBase == null) {
            return MaterialKind.BLOCKED;
        }
        int horizontal = Math.abs(fromBase.getX() - nextBase.getX())
                + Math.abs(fromBase.getZ() - nextBase.getZ());
        if (horizontal != 1 || nextBase.getY() != fromBase.getY() + 1) {
            return MaterialKind.BLOCKED;
        }

        MaterialKind destination = classifyAscendingColumn(level, nextBase);
        MaterialKind transitionHeadroom = classifyPresentBlock(level, fromBase.above(2));
        if (destination == MaterialKind.BLOCKED || transitionHeadroom == MaterialKind.BLOCKED) {
            return MaterialKind.BLOCKED;
        }
        return destination == MaterialKind.CONSTRUCTION || transitionHeadroom == MaterialKind.CONSTRUCTION
                ? MaterialKind.CONSTRUCTION
                : MaterialKind.NATURAL;
    }

    /** Classifies the extra ceiling cell needed to jump from the last stair into an open exit. */
    public static MaterialKind classifyAscendingExitTransition(
            Level level, BlockPos fromBase, BlockPos openExitBase) {
        if (level == null || fromBase == null || openExitBase == null) {
            return MaterialKind.BLOCKED;
        }
        int horizontal = Math.abs(fromBase.getX() - openExitBase.getX())
                + Math.abs(fromBase.getZ() - openExitBase.getZ());
        if (horizontal != 1 || openExitBase.getY() != fromBase.getY() + 1) {
            return MaterialKind.BLOCKED;
        }
        MaterialKind transitionHeadroom = classifyPresentBlock(level, fromBase.above(2));
        return transitionHeadroom == null ? MaterialKind.NATURAL : transitionHeadroom;
    }

    public static boolean isNatural(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || NATURAL_SOILS.contains(state.getBlock());
    }

    private static MaterialKind classifyPresentBlock(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
            return MaterialKind.BLOCKED;
        }
        return level.getBlockState(pos).isAir() ? null : classify(level, pos);
    }

    private static boolean hasSafeNeighbours(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbourPos = pos.relative(direction);
            if (!level.hasChunkAt(neighbourPos)) {
                return false;
            }
            BlockState neighbour = level.getBlockState(neighbourPos);
            boolean restorationPlaceholder = neighbour.is(
                    UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get());
            if ((!restorationPlaceholder
                            && (neighbour.hasBlockEntity() || level.getBlockEntity(neighbourPos) != null))
                    || neighbour.getBlock() instanceof FallingBlock
                    || !neighbour.getFluidState().isEmpty()
                    || (!neighbour.isAir()
                            && neighbour.getPistonPushReaction() == PushReaction.DESTROY)) {
                return false;
            }
        }
        return true;
    }

    public enum MaterialKind {
        NATURAL,
        CONSTRUCTION,
        BLOCKED
    }
}
