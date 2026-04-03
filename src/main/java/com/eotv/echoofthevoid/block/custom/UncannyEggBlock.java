package com.eotv.echoofthevoid.block.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannySpiderlingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class UncannyEggBlock extends Block {
    public static final int HATCH_DELAY_TICKS = 20 * 60 * 5;

    public UncannyEggBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        UncannySpiderlingEntity spiderling = UncannyEntityRegistry.UNCANNY_SPIDERLING.get().create(level);
        if (spiderling != null) {
            spiderling.moveTo(pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(spiderling);
        }

        // Egg breaks when the spiderling is born.
        level.destroyBlock(pos, false);
    }
}
