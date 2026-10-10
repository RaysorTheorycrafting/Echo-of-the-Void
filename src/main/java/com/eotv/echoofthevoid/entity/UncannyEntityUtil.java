package com.eotv.echoofthevoid.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
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

    /**
     * A player the mod's hunters may chase: alive, not a spectator, not in creative. Creative is read from
     * the abilities (as Vanilla targeting does) so GameTest mock players, always "creative", can opt in.
     */
    public static boolean isHuntablePlayer(net.minecraft.world.entity.player.Player player) {
        return player != null && player.isAlive() && !player.isSpectator() && !player.getAbilities().invulnerable;
    }

    /**
     * Land Specials that chase in water with {@link UncannySwimming}. The sea creatures steer
     * themselves, the old friend already swims as a player and the Ferryman? follows its own scene.
     */
    public static boolean isSpecialHunterThatSwims(net.minecraft.world.entity.Mob mob) {
        if (mob instanceof com.eotv.echoofthevoid.entity.custom.AbstractUncannyAquaticSpecialEntity
                || mob instanceof com.eotv.echoofthevoid.entity.custom.UncannyFriendEntity
                || mob instanceof com.eotv.echoofthevoid.entity.custom.UncannyApprovedSpecialEntity) {
            return false;
        }
        return UncannyEntityRegistry.isSpecialEntity(mob.getType());
    }

    /** Nearest {@link #isHuntablePlayer huntable} player within {@code distance}, or null. */
    @org.jetbrains.annotations.Nullable
    public static net.minecraft.world.entity.player.Player nearestHuntablePlayer(net.minecraft.world.entity.Entity from, double distance) {
        return from.level().getNearestPlayer(from.getX(), from.getY(), from.getZ(), distance,
                entity -> entity instanceof net.minecraft.world.entity.player.Player player && isHuntablePlayer(player));
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

}

