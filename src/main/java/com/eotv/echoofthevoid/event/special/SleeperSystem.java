package com.eotv.echoofthevoid.event.special;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannySleeperEntity;
import com.eotv.echoofthevoid.world.ObserverSight;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;

/** Places Sleeper? in a dark corner of a base at night and delivers its one warning. */
public final class SleeperSystem {
    private SleeperSystem() {
    }

    public static boolean spawnNatural(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos bed = homeBed(player);
        if (level.dimension() != Level.OVERWORLD || PercherSystem.isDaylight(level) || bed == null
                || player.blockPosition().distSqr(bed) > 32 * 32 || hasSleeperNear(level, bed)) {
            return false;
        }
        BlockPos spot = findCorner(level, bed, 4, 14, true);
        return spot != null && spawnAt(level, player, spot);
    }

    /** Dev path: near the player's bed if any, else near the player, allowed in sight. */
    public static boolean spawnForDebug(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos bed = homeBed(player);
        BlockPos centre = bed != null && bed.distSqr(player.blockPosition()) < 48 * 48 ? bed : player.blockPosition();
        if (hasSleeperNear(level, centre)) {
            return false;
        }
        BlockPos spot = findCorner(level, centre, 3, 10, false);
        return spot != null && spawnAt(level, player, spot);
    }

    private static boolean spawnAt(ServerLevel level, ServerPlayer player, BlockPos spot) {
        UncannySleeperEntity sleeper = UncannyEntityRegistry.UNCANNY_SLEEPER.get().create(level);
        if (sleeper == null) {
            return false;
        }
        sleeper.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        sleeper.setupTarget(player, 20 * 60 * 12);
        boolean added = level.addFreshEntity(sleeper);
        if (added && UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info("[UncannyDebug/Sleeper] waiting at {} near {}", spot, player.getGameProfile().getName());
        }
        return added;
    }

    private static BlockPos homeBed(ServerPlayer player) {
        BlockPos respawn = player.getRespawnPosition();
        if (respawn == null || player.getRespawnDimension() != Level.OVERWORLD
                || !(player.server.overworld().getBlockState(respawn).getBlock() instanceof BedBlock)) {
            return null;
        }
        return respawn;
    }

    /** One per base: none may already be within reach of these beds. */
    private static boolean hasSleeperNear(ServerLevel level, BlockPos centre) {
        return !level.getEntitiesOfClass(UncannySleeperEntity.class, new AABB(centre).inflate(48.0D)).isEmpty();
    }

    /** A dark, roofed, standable spot near the bed, out of everyone's sight when it matters. */
    private static BlockPos findCorner(ServerLevel level, BlockPos centre, int min, int max, boolean unseen) {
        BlockPos best = null;
        int bestLight = Integer.MAX_VALUE;
        for (int attempt = 0; attempt < 120; attempt++) {
            int dx = level.random.nextInt(max * 2 + 1) - max;
            int dz = level.random.nextInt(max * 2 + 1) - max;
            int dy = level.random.nextInt(5) - 2;
            if (dx * dx + dz * dz < min * min) {
                continue;
            }
            BlockPos feet = centre.offset(dx, dy, dz);
            if (!level.isLoaded(feet) || !standable(level, feet) || (unseen && level.canSeeSky(feet))) {
                continue;
            }
            if (unseen && ObserverSight.isSeenByAny(level, List.of(Vec3.atBottomCenterOf(feet).add(0.0D, 1.2D, 0.0D)),
                    level.players(), 4.0D, pos -> false)) {
                continue;
            }
            int light = level.getBrightness(LightLayer.BLOCK, feet);
            if (light < bestLight) {
                bestLight = light;
                best = feet;
            }
        }
        return best;
    }

    private static boolean standable(ServerLevel level, BlockPos feet) {
        return level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && level.getFluidState(feet).isEmpty();
    }

    /** A Sleeper? waiting within reach of this bed owns the night: other bed disturbances stand aside. */
    public static boolean isWaitingNear(ServerLevel level, BlockPos bed) {
        return !level.getEntitiesOfClass(UncannySleeperEntity.class, new AABB(bed).inflate(SleeperRules.BED_RADIUS),
                sleeper -> sleeper.isAlive() && !sleeper.isSinking()).isEmpty();
    }

    /** The first attempt to sleep within its reach is refused with the warning; the next is allowed. */
    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        ServerPlayer player = event.getEntity();
        if (event.getVanillaProblem() != null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        double radius = SleeperRules.BED_RADIUS;
        for (UncannySleeperEntity sleeper : level.getEntitiesOfClass(UncannySleeperEntity.class,
                new AABB(event.getPos()).inflate(radius))) {
            if (sleeper.isAlive() && !sleeper.isSinking() && sleeper.warnOnce(player.getUUID())) {
                event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
                player.displayClientMessage(Component.literal(SleeperRules.WARNING), true);
                return;
            }
        }
    }
}
