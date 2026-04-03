package com.eotv.echoofthevoid.entity;

import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;

public final class UncannyEntityUtil {
    private UncannyEntityUtil() {
    }

    public static void applyDisplayName(LivingEntity entity, String unusedDisplayName) {
        // Intentionally ignore provided display text: uncanny entities stay nametag-less like vanilla monsters.
        entity.setCustomName(null);
        entity.setCustomNameVisible(false);
    }

    public static void forceSilent(LivingEntity entity) {
        entity.setSilent(true);
    }

    public static void suppressStepSound(LivingEntity entity, BlockPos pos, BlockState state) {
        // Intentionally no-op for uncanny silent movement.
    }

    public static void enableDoorNavigation(Mob mob) {
        if (mob.getNavigation() instanceof GroundPathNavigation groundPathNavigation) {
            groundPathNavigation.setCanOpenDoors(true);
            groundPathNavigation.setCanPassDoors(true);
        }

        if (mob.level().isClientSide()) {
            return;
        }

        BlockPos origin = mob.blockPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos doorPos = origin.offset(dx, dy, dz);
                    BlockState doorState = mob.level().getBlockState(doorPos);
                    if (!(doorState.getBlock() instanceof DoorBlock)
                            || !doorState.hasProperty(BlockStateProperties.OPEN)
                            || doorState.getValue(BlockStateProperties.OPEN)) {
                        continue;
                    }

                    mob.level().setBlock(doorPos, doorState.setValue(BlockStateProperties.OPEN, true), 10);
                    mob.level().gameEvent(mob, GameEvent.BLOCK_OPEN, doorPos);
                    return;
                }
            }
        }
    }

    public static void dropPulseStyleRewards(ServerLevel level, Entity entity, RandomSource random) {
        if (random.nextFloat() < 0.10F) {
            level.addFreshEntity(new ItemEntity(
                    level,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    new ItemStack(UncannyItemRegistry.UNCANNY_REALITY_SHARD.get())));
        }
        if (random.nextFloat() < 0.50F) {
            level.addFreshEntity(new ItemEntity(
                    level,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    new ItemStack(UncannyItemRegistry.UNCANNY_REALITY_SHARD_PIECE.get())));
        }
    }
}

