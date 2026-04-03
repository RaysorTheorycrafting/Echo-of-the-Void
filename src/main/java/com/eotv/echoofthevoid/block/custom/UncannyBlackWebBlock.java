package com.eotv.echoofthevoid.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class UncannyBlackWebBlock extends Block {
    public UncannyBlackWebBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof Spider) {
            return;
        }
        entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));
    }
}
