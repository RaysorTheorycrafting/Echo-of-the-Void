package com.eotv.echoofthevoid.event.special;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyPercherEntity;
import com.eotv.echoofthevoid.world.ObserverSight;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Spawning and perch selection of Percher?: high, open, edge-of-something places above the player. */
public final class PercherSystem {
    private static final int SEARCH_ATTEMPTS = 120;
    /** A perch must stand at least this much above the player's eyes. */
    private static final double MIN_HEIGHT_ABOVE_EYES = 3.0D;
    /** Close enough for the player's own roof and the trees around the base. */
    private static final double NATURAL_MIN_DISTANCE = 16.0D;
    private static final double NATURAL_MAX_DISTANCE = 48.0D;
    private static final double SPAWN_CLEARANCE = 8.0D;

    private PercherSystem() {
    }

    public static boolean spawnNatural(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD || isDaylight(level) || hasActivePercher(level, player)) {
            return false;
        }
        BlockPos perch = findPerch(level, player, NATURAL_MIN_DISTANCE, NATURAL_MAX_DISTANCE, true);
        return perch != null && spawnAt(level, player, perch);
    }

    /** Dev path: same perch rules, closer and allowed in plain sight so the tester can find it. */
    public static boolean spawnForDebug(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos perch = findPerch(level, player, 14.0D, 36.0D, false);
        return perch != null && spawnAt(level, player, perch);
    }

    private static boolean spawnAt(ServerLevel level, ServerPlayer player, BlockPos perch) {
        UncannyPercherEntity percher = UncannyEntityRegistry.UNCANNY_PERCHER.get().create(level);
        if (percher == null) {
            return false;
        }
        percher.moveTo(perch.getX() + 0.5D, perch.getY(), perch.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        percher.setWatchedPlayer(player);
        boolean added = level.addFreshEntity(percher);
        if (added && UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info("[UncannyDebug/Percher] perched at {} for {}", perch, player.getGameProfile().getName());
        }
        return added;
    }

    private static boolean hasActivePercher(ServerLevel level, ServerPlayer player) {
        return !level.getEntitiesOfClass(UncannyPercherEntity.class, player.getBoundingBox().inflate(160.0D)).isEmpty();
    }

    public static boolean isDaylight(ServerLevel level) {
        long time = Math.floorMod(level.getDayTime(), 24000L);
        return time > 23500L || time < 12500L;
    }

    private record Candidate(BlockPos feet, int score) {
    }

    /**
     * A free spot on top of the highest block of a column (leaves count), well above the player's eyes,
     * open to the sky and on an edge or a peak, so that the crouched figure stands out against the sky.
     * The best perches come first: a roof or a built top, then a tree top, then a clear peak; a plain
     * hill is used only when nothing better is in reach.
     */
    public static BlockPos findPerch(ServerLevel level, ServerPlayer player, double minDistance, double maxDistance,
            boolean requireUnseen) {
        // Every column of the ring is considered: good perches (a lone tree top, a chimney) are a single
        // column wide and random sampling would miss them most of the time.
        List<Candidate> candidates = new ArrayList<>();
        int px = player.getBlockX();
        int pz = player.getBlockZ();
        int reach = (int) Math.ceil(maxDistance);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance < minDistance || distance > maxDistance || !level.hasChunk((px + dx) >> 4, (pz + dz) >> 4)) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, px + dx, pz + dz);
                if (top >= player.getEyeY() + MIN_HEIGHT_ABOVE_EYES) {
                    BlockPos feet = new BlockPos(px + dx, top, pz + dz);
                    int score = perchScore(level, feet);
                    if (score >= 0) {
                        candidates.add(new Candidate(feet, score));
                    }
                }
            }
        }
        // Random among equals, best first.
        Collections.shuffle(candidates, new java.util.Random(level.random.nextLong()));
        candidates.sort((a, b) -> Integer.compare(b.score(), a.score()));
        int checked = 0;
        for (Candidate candidate : candidates) {
            BlockPos feet = candidate.feet();
            if (++checked > SEARCH_ATTEMPTS) {
                break;
            }
            if (!level.getEntitiesOfClass(UncannyPercherEntity.class, new AABB(feet).inflate(4.0D)).isEmpty()) {
                continue;
            }
            if (requireUnseen && ObserverSight.isSeenByAny(level,
                    List.of(Vec3.atBottomCenterOf(feet).add(0.0D, 0.6D, 0.0D), Vec3.atBottomCenterOf(feet).add(0.0D, 1.5D, 0.0D)),
                    level.players(), SPAWN_CLEARANCE, pos -> false)) {
                continue;
            }
            return feet;
        }
        return null;
    }

    public static boolean isPerchable(ServerLevel level, BlockPos feet) {
        return perchScore(level, feet) >= 0
                && level.getEntitiesOfClass(UncannyPercherEntity.class, new AABB(feet).inflate(4.0D)).isEmpty();
    }

    /** {@link PercherRules#score} of a perch, or -1 when it cannot hold a Percher? at all. */
    static int perchScore(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        BlockState support = level.getBlockState(below);
        boolean treeTop = support.is(BlockTags.LEAVES) || support.is(BlockTags.LOGS);
        if (!(treeTop || support.isFaceSturdy(level, below, Direction.UP)) || !level.getFluidState(below).isEmpty()) {
            return -1;
        }
        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                || !level.canSeeSky(feet)) {
            return -1;
        }
        int lowerSides = 0;
        int drop = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = feet.relative(direction);
            int sideTop = level.getHeight(Heightmap.Types.MOTION_BLOCKING, side.getX(), side.getZ());
            if (sideTop < feet.getY()) {
                lowerSides++;
                drop = Math.max(drop, feet.getY() - sideTop);
            }
        }
        if (lowerSides < 2) {
            return -1;
        }
        return PercherRules.score(!treeTop && !isNaturalGround(support), treeTop, lowerSides, drop);
    }

    /** Terrain a hill is made of; anything else under a perch was built or grown on purpose. */
    private static boolean isNaturalGround(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.TERRACOTTA) || state.is(BlockTags.SNOW) || state.is(BlockTags.ICE)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(BlockTags.COAL_ORES)
                || state.is(BlockTags.IRON_ORES) || state.is(BlockTags.COPPER_ORES)
                || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE) || state.is(Blocks.MOSS_BLOCK);
    }
}
