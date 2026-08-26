package com.eotv.echoofthevoid.world;

import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Shared release guard for effects which temporarily replace or permanently destroy one block. */
public final class UncannyBlockMutationSafety {
    private UncannyBlockMutationSafety() {
    }

    public static boolean isProtected(LevelReader level, BlockPos pos, BlockState state) {
        return state.hasBlockEntity()
                || level.getBlockEntity(pos) != null
                || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || state.is(UncannyBlockRegistry.UNCANNY_ALTAR.get())
                || state.is(UncannyBlockRegistry.UNCANNY_ALTAR_PART.get());
    }
}
