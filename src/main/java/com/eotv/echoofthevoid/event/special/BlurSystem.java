package com.eotv.echoofthevoid.event.special;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyBlurEntity;
import com.eotv.echoofthevoid.world.ObserverSight;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Spawning of Blur?: at night, outdoors, dry, somewhere behind the player and out of sight. */
public final class BlurSystem {
    private BlurSystem() {
    }

    public static boolean spawnNatural(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD || PercherSystem.isDaylight(level)
                || !level.canSeeSky(player.blockPosition()) || level.isRainingAt(player.blockPosition().above())
                || hasActiveBlur(level, player)) {
            return false;
        }
        BlockPos spot = findSpot(level, player, 14.0D, 22.0D, true);
        return spot != null && spawnAt(level, player, spot);
    }

    /** Dev path: beside the player, allowed in sight so the edge rendering can be checked at once. */
    public static boolean spawnForDebug(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos spot = findSpot(level, player, 8.0D, 14.0D, false);
        return spot != null && spawnAt(level, player, spot);
    }

    private static boolean spawnAt(ServerLevel level, ServerPlayer player, BlockPos spot) {
        UncannyBlurEntity blur = UncannyEntityRegistry.UNCANNY_BLUR.get().create(level);
        if (blur == null) {
            return false;
        }
        blur.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        blur.setup(player, BlurRules.lifetimeTicks(level.random.nextDouble()));
        boolean added = level.addFreshEntity(blur);
        if (added && UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info("[UncannyDebug/Blur] running near {} from {}", player.getGameProfile().getName(), spot);
        }
        return added;
    }

    private static boolean hasActiveBlur(ServerLevel level, ServerPlayer player) {
        return !level.getEntitiesOfClass(UncannyBlurEntity.class, player.getBoundingBox().inflate(96.0D)).isEmpty();
    }

    private static BlockPos findSpot(ServerLevel level, ServerPlayer player, double min, double max, boolean unseen) {
        Vec3 look = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        for (int attempt = 0; attempt < 60; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = min + level.random.nextDouble() * (max - min);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            Vec3 direction = new Vec3(x + 0.5D - player.getX(), 0.0D, z + 0.5D - player.getZ()).normalize();
            if (unseen && direction.dot(look) > -0.2D) {
                continue;
            }
            BlockPos feet = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (Math.abs(feet.getY() - player.getBlockY()) > 6 || !standable(level, feet)) {
                continue;
            }
            if (unseen && ObserverSight.isSeenByAny(level, List.of(Vec3.atBottomCenterOf(feet).add(0.0D, 1.0D, 0.0D)),
                    level.players(), 6.0D, pos -> false)) {
                continue;
            }
            return feet;
        }
        return null;
    }

    private static boolean standable(ServerLevel level, BlockPos feet) {
        return level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && level.getFluidState(feet).isEmpty();
    }
}
