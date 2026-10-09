package com.eotv.echoofthevoid.event.paranoia.nativeevent;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import com.eotv.echoofthevoid.state.WanderingTreeRecord;
import com.eotv.echoofthevoid.world.ObserverSight;
import com.eotv.echoofthevoid.world.UncannyBlockMutationSafety;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The wandering tree: a real, harvestable natural tree near a player's base that relocates a few
 * blocks toward it, whole (logs, leaves and the snow resting on them), only while nobody can see
 * it and only across dirt or grass. Once it has moved {@link WanderingTreeRules#TRAP_MOVE_THRESHOLD}
 * times, cutting any of its logs makes the entire tree vanish at once with a shriek.
 *
 * <p>Unlike the presentation-only anomalies, this one mutates real blocks, by explicit user
 * request. Every mutation is guarded: the tree must be isolated (no foreign logs, shared canopy,
 * vines, cocoa, hives, attached blocks or block entities), every destination cell must be free or
 * replaceable vegetation, and nothing living may stand where it lands.</p>
 */
public final class WanderingTreeSystem {
    private static final int TICK_INTERVAL = 20;
    private static final double OWNER_PRESENCE_RADIUS = 128.0D;
    private static final double NATURAL_TRIGGER_BASE_RADIUS = 96.0D;
    private static final int MAX_LOGS = 48;
    private static final int MAX_LEAVES = 360;
    private static final int MAX_HORIZONTAL_REACH = 7;
    private static final int MAX_TREE_HEIGHT = 32;
    private static final int MAX_LEAF_STEPS = 6;
    private static final int MAX_ADOPTION_CANDIDATES = 16;
    private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    // Range is 16 blocks per unit of volume: heard about 96 blocks around, barely softened nearby.
    private static final float SCREAM_VOLUME = 6.0F;
    /** Shown to whoever cut the tree, once the shriek has had time to ring out. */
    static final String WARNING_LINE = "You shouldn't have done that.";
    private static final long WARNING_DELAY_TICKS = 60L;
    private static final long WARNING_MAX_LATENESS_TICKS = 200L;
    private static final Map<UUID, PendingWarning> PENDING_WARNINGS = new HashMap<>();

    private WanderingTreeSystem() {
    }

    public enum MoveOutcome {
        MOVED,
        ARRIVED,
        OBSERVED,
        BLOCKED,
        NOT_DUE,
        UNLOADED,
        INVALID
    }

    // ------------------------------------------------------------------ scheduling and triggers

    public static void tick(MinecraftServer server, long now) {
        deliverWarnings(server, now);
        if (now % TICK_INTERVAL != 0L) {
            return;
        }
        UncannyWorldState state = UncannyWorldState.get(server);
        for (WanderingTreeRecord record : state.getWanderingTrees()) {
            if (record.settled()) {
                continue;
            }
            ServerLevel level = levelOf(server, record);
            ServerPlayer owner = server.getPlayerList().getPlayer(record.owner());
            if (level == null || owner == null || owner.isSpectator() || owner.serverLevel() != level
                    || level.getGameTime() < record.nextMoveGameTime()
                    || owner.position().distanceToSqr(Vec3.atCenterOf(record.base()))
                            > OWNER_PRESENCE_RADIUS * OWNER_PRESENCE_RADIUS) {
                continue;
            }
            MoveOutcome outcome = attemptMove(level, record, resolveBaseCenter(owner), level.players(), false);
            debug("tick tree={} outcome={}", record.id(), outcome);
        }
    }

    /**
     * Picks an isolated natural tree in a ring around the player's base and starts its walk. Nothing
     * visible happens yet: the first move comes minutes later, while the player looks elsewhere.
     */
    public static boolean trigger(ServerPlayer player, boolean debugImmediate) {
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD || player.getServer() == null) {
            return false;
        }
        UncannyWorldState state = UncannyWorldState.get(player.getServer());
        pruneVanishedTrees(player.getServer(), state, player.getUUID());
        List<WanderingTreeRecord> owned = state.getWanderingTrees().stream()
                .filter(record -> record.owner().equals(player.getUUID()))
                .toList();
        if (owned.stream().anyMatch(record -> !record.settled())
                || owned.size() >= WanderingTreeRules.MAX_RECORDS_PER_OWNER) {
            return false;
        }
        BlockPos baseCenter = resolveBaseCenter(player);
        if (!debugImmediate && player.position().distanceToSqr(Vec3.atCenterOf(baseCenter))
                > NATURAL_TRIGGER_BASE_RADIUS * NATURAL_TRIGGER_BASE_RADIUS) {
            return false;
        }
        TreeShape tree = findTreeInRing(level, state, baseCenter,
                WanderingTreeRules.MIN_ADOPT_DISTANCE_BLOCKS, WanderingTreeRules.MAX_ADOPT_DISTANCE_BLOCKS);
        if (tree == null && debugImmediate) {
            tree = findTreeInRing(level, state, player.blockPosition(), 4, WanderingTreeRules.MAX_ADOPT_DISTANCE_BLOCKS);
        }
        if (tree == null) {
            return false;
        }
        long delay = debugImmediate
                ? 40L
                : WanderingTreeRules.nextMoveDelayTicks(level.random.nextDouble());
        WanderingTreeRecord record = adopt(level, player.getUUID(), tree.base(), level.getGameTime() + delay);
        debug("adopted tree={} owner={} base={} baseCenter={} logs={} leaves={}",
                record == null ? "none" : record.id(), player.getGameProfile().getName(), tree.base(),
                baseCenter, tree.logs().size(), tree.cells().size() - tree.logs().size());
        return record != null;
    }

    /** Starts tracking the tree whose trunk contains {@code log}; null if it is not a suitable tree. */
    public static WanderingTreeRecord adopt(ServerLevel level, UUID owner, BlockPos log, long nextMoveGameTime) {
        TreeShape tree = TreeShape.scan(level, log);
        if (tree == null) {
            return null;
        }
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        if (isTracked(state, level, tree.base())) {
            return null;
        }
        WanderingTreeRecord record = new WanderingTreeRecord(
                UUID.randomUUID(), owner, level.dimension().location().toString(),
                tree.base().asLong(), 0, nextMoveGameTime, 0, false);
        return state.putWanderingTree(record) ? record : null;
    }

    /** Dev entry: one move attempt now for the player's walking tree, with every production rule. */
    public static MoveOutcome forceStep(ServerPlayer player) {
        WanderingTreeRecord record = latestOwned(player, true);
        ServerLevel level = record == null || player.getServer() == null ? null : levelOf(player.getServer(), record);
        if (level == null) {
            return MoveOutcome.INVALID;
        }
        return attemptMove(level, record, resolveBaseCenter(player), level.players(), true);
    }

    /** Dev entry: arms the player's most recent tree as if it had already moved three times. */
    public static boolean arm(ServerPlayer player) {
        WanderingTreeRecord record = latestOwned(player, false);
        if (record == null || player.getServer() == null) {
            return false;
        }
        UncannyWorldState.get(player.getServer()).putWanderingTree(
                record.withMoves(Math.max(record.moves(), WanderingTreeRules.TRAP_MOVE_THRESHOLD)));
        return true;
    }

    public static String describeFor(ServerPlayer player) {
        if (player.getServer() == null) {
            return "Wandering tree: server unavailable.";
        }
        List<WanderingTreeRecord> owned = UncannyWorldState.get(player.getServer()).getWanderingTrees().stream()
                .filter(record -> record.owner().equals(player.getUUID()))
                .toList();
        if (owned.isEmpty()) {
            return "Wandering tree: none.";
        }
        long gameTime = player.serverLevel().getGameTime();
        StringBuilder builder = new StringBuilder("Wandering tree:");
        for (WanderingTreeRecord record : owned) {
            builder.append(String.format(java.util.Locale.ROOT,
                    " [base=%s moves=%d armed=%s settled=%s nextIn=%ds blocked=%d]",
                    record.base().toShortString(), record.moves(), WanderingTreeRules.isArmed(record.moves()),
                    record.settled(), Math.max(0L, record.nextMoveGameTime() - gameTime) / 20L,
                    record.blockedAttempts()));
        }
        return builder.toString();
    }

    // ------------------------------------------------------------------ moving

    /**
     * One relocation attempt. Public for the GameTests, which pass their own observer list because
     * the shared test level contains mock players from other batches.
     */
    public static MoveOutcome attemptMove(
            ServerLevel level,
            WanderingTreeRecord record,
            BlockPos baseCenter,
            List<? extends Player> observers,
            boolean ignoreTimer) {
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        long gameTime = level.getGameTime();
        if (!ignoreTimer && gameTime < record.nextMoveGameTime()) {
            return MoveOutcome.NOT_DUE;
        }
        BlockPos base = record.base();
        if (!isAreaLoaded(level, base, MAX_HORIZONTAL_REACH + WanderingTreeRules.MAX_STEP_BLOCKS + 1)) {
            return MoveOutcome.UNLOADED;
        }
        TreeShape tree = TreeShape.scan(level, base);
        if (tree == null) {
            if (TreeShape.scanStanding(level, base) == null) {
                state.removeWanderingTree(record.id());
            } else {
                // Still standing but no longer isolated (a build, a sapling grown beside it...): it stops
                // walking for good, yet keeps its trap.
                state.putWanderingTree(record.retryAt(record.nextMoveGameTime(), record.blockedAttempts(), true));
            }
            return MoveOutcome.INVALID;
        }
        if (WanderingTreeRules.hasArrived(base.getX(), base.getZ(), baseCenter.getX(), baseCenter.getZ())) {
            state.putWanderingTree(record.retryAt(record.nextMoveGameTime(), 0, true));
            return MoveOutcome.ARRIVED;
        }
        int length = WanderingTreeRules.MIN_STEP_BLOCKS + level.random.nextInt(
                WanderingTreeRules.MAX_STEP_BLOCKS - WanderingTreeRules.MIN_STEP_BLOCKS + 1);
        for (int[] step : WanderingTreeRules.candidateSteps(
                base.getX(), base.getZ(), baseCenter.getX(), baseCenter.getZ(), length)) {
            Placement placement = planPlacement(level, tree, step[0], step[1]);
            if (placement == null) {
                continue;
            }
            if (isObserved(level, tree, placement.offset(), observers)) {
                state.putWanderingTree(record.retryAt(
                        gameTime + WanderingTreeRules.OBSERVED_RETRY_TICKS, record.blockedAttempts(), false));
                return MoveOutcome.OBSERVED;
            }
            apply(level, tree, placement);
            BlockPos newBase = base.offset(placement.offset());
            boolean arrived = WanderingTreeRules.hasArrived(
                    newBase.getX(), newBase.getZ(), baseCenter.getX(), baseCenter.getZ());
            state.putWanderingTree(record.movedTo(
                    newBase,
                    gameTime + WanderingTreeRules.nextMoveDelayTicks(level.random.nextDouble()),
                    arrived));
            debug("moved tree={} from={} to={} moves={} arrived={}",
                    record.id(), base, newBase, record.moves() + 1, arrived);
            return MoveOutcome.MOVED;
        }
        int blocked = record.blockedAttempts() + 1;
        state.putWanderingTree(record.retryAt(
                gameTime + WanderingTreeRules.BLOCKED_RETRY_TICKS,
                blocked,
                blocked >= WanderingTreeRules.MAX_BLOCKED_ATTEMPTS));
        return MoveOutcome.BLOCKED;
    }

    private static Placement planPlacement(ServerLevel level, TreeShape tree, int dx, int dz) {
        BlockPos base = tree.base();
        int groundY = base.getY() - 1;
        for (int[] column : WanderingTreeRules.pathColumns(dx, dz)) {
            Integer found = groundNear(level, tree, base.getX() + column[0], groundY, base.getZ() + column[1]);
            if (found == null) {
                return null;
            }
            groundY = found;
        }
        BlockPos offset = new BlockPos(dx, groundY - (base.getY() - 1), dz);

        Set<BlockPos> extraClears = new HashSet<>();
        for (BlockPos cell : tree.cells().keySet()) {
            BlockPos target = cell.offset(offset);
            if (tree.cells().containsKey(target)) {
                continue;
            }
            if (!level.isLoaded(target) || level.isOutsideBuildHeight(target)
                    || !level.getWorldBorder().isWithinBounds(target)) {
                return null;
            }
            BlockState existing = level.getBlockState(target);
            if (existing.isAir()) {
                continue;
            }
            if (existing.getBlock() instanceof DoublePlantBlock && existing.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                BlockPos other = existing.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                        ? target.above() : target.below();
                extraClears.add(target);
                if (!tree.cells().containsKey(other)) {
                    extraClears.add(other);
                }
                continue;
            }
            if (!existing.canBeReplaced() || !existing.getFluidState().isEmpty()
                    || UncannyBlockMutationSafety.isProtected(level, target, existing)) {
                return null;
            }
        }
        for (BlockPos contact : tree.groundContacts()) {
            BlockPos newGround = contact.below().offset(offset);
            if (tree.cells().containsKey(newGround) || !isWanderingGround(level.getBlockState(newGround))) {
                return null;
            }
        }
        // Where it lands it must be as free as where it was chosen: no foreign canopy, build or
        // hanging block against it. Otherwise it could no longer be read back as one tree.
        Set<BlockPos> targets = new HashSet<>();
        for (BlockPos cell : tree.cells().keySet()) {
            targets.add(cell.offset(offset));
        }
        BlockPos newBase = tree.base().offset(offset);
        for (BlockPos target : targets) {
            for (Direction direction : Direction.values()) {
                BlockPos next = target.relative(direction);
                if (targets.contains(next) || tree.cells().containsKey(next) || extraClears.contains(next)) {
                    continue;
                }
                if (!TreeShape.isAcceptableNeighbour(level, next, newBase)) {
                    return null;
                }
            }
        }
        if (hasLivingEntityInTarget(level, tree, offset)) {
            return null;
        }
        return new Placement(offset, extraClears);
    }

    /** Highest dirt or grass block within one block of {@code nearY} whose top face is free. */
    private static Integer groundNear(ServerLevel level, TreeShape tree, int x, int nearY, int z) {
        for (int y = nearY + 1; y >= nearY - 1; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isLoaded(pos)) {
                return null;
            }
            BlockPos above = pos.above();
            BlockState aboveState = level.getBlockState(above);
            boolean freeAbove = tree.cells().containsKey(above) || aboveState.isAir()
                    || (aboveState.canBeReplaced() && aboveState.getFluidState().isEmpty());
            if (isWanderingGround(level.getBlockState(pos)) && !tree.cells().containsKey(pos) && freeAbove) {
                return y;
            }
        }
        return null;
    }

    private static boolean hasLivingEntityInTarget(ServerLevel level, TreeShape tree, BlockPos offset) {
        AABB bounds = tree.bounds().move(offset.getX(), offset.getY(), offset.getZ());
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, bounds)) {
            AABB box = entity.getBoundingBox();
            for (BlockPos cell : tree.cells().keySet()) {
                if (box.intersects(new AABB(cell.offset(offset)))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void apply(ServerLevel level, TreeShape tree, Placement placement) {
        BlockPos offset = placement.offset();
        for (BlockPos cell : tree.cells().keySet()) {
            level.setBlock(cell, Blocks.AIR.defaultBlockState(), PLACE_FLAGS);
        }
        for (BlockPos clear : placement.extraClears()) {
            level.setBlock(clear, Blocks.AIR.defaultBlockState(), PLACE_FLAGS);
        }
        // Vanilla trees stand on dirt: the trunk leaves a bare patch behind and makes a new one.
        for (BlockPos contact : tree.groundContacts()) {
            BlockPos ground = contact.below().offset(offset);
            if (level.getBlockState(ground).is(Blocks.GRASS_BLOCK)) {
                level.setBlock(ground, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (Map.Entry<BlockPos, BlockState> entry : tree.cells().entrySet()) {
            level.setBlock(entry.getKey().offset(offset), entry.getValue(), PLACE_FLAGS);
        }
    }

    // ------------------------------------------------------------------ observation

    /** True when any observer could see the tree where it stands or where it would land. */
    public static boolean isObserved(ServerLevel level, BlockPos base, List<? extends Player> observers) {
        TreeShape tree = TreeShape.scan(level, base);
        return tree != null && isObserved(level, tree, BlockPos.ZERO, observers);
    }

    private static boolean isObserved(
            ServerLevel level,
            TreeShape tree,
            BlockPos offset,
            List<? extends Player> observers) {
        List<Vec3> points = new ArrayList<>(tree.samplePoints(BlockPos.ZERO));
        Set<BlockPos> reachable = new HashSet<>(tree.cells().keySet());
        if (!offset.equals(BlockPos.ZERO)) {
            points.addAll(tree.samplePoints(offset));
            for (BlockPos cell : tree.cells().keySet()) {
                reachable.add(cell.offset(offset));
            }
        }
        return ObserverSight.isSeenByAny(
                level, points, observers, WanderingTreeRules.MIN_PLAYER_CLEARANCE_BLOCKS, reachable::contains);
    }

    // ------------------------------------------------------------------ the trap

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getPlayer() instanceof ServerPlayer) || !event.getState().is(BlockTags.LOGS)) {
            return;
        }
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        if (!state.isSystemEnabled()) {
            return;
        }
        BlockPos pos = event.getPos();
        String dimension = level.dimension().location().toString();
        for (WanderingTreeRecord record : state.getWanderingTrees()) {
            BlockPos base = record.base();
            if (!record.dimension().equals(dimension)
                    || Math.abs(pos.getX() - base.getX()) > MAX_HORIZONTAL_REACH
                    || Math.abs(pos.getZ() - base.getZ()) > MAX_HORIZONTAL_REACH
                    || pos.getY() < base.getY() || pos.getY() > base.getY() + MAX_TREE_HEIGHT
                    || !isAreaLoaded(level, base, MAX_HORIZONTAL_REACH + 1)) {
                continue;
            }
            // Lenient read: whatever grew or was built around it since, a standing tree keeps its trap.
            TreeShape tree = TreeShape.scanStanding(level, base);
            if (tree == null) {
                state.removeWanderingTree(record.id());
                continue;
            }
            if (!tree.logs().contains(pos)) {
                continue;
            }
            // Cut before it was armed: from now on it is just an ordinary tree.
            state.removeWanderingTree(record.id());
            if (WanderingTreeRules.isArmed(record.moves())) {
                event.setCanceled(true);
                vanish(level, tree);
                ServerPlayer cutter = (ServerPlayer) event.getPlayer();
                PENDING_WARNINGS.put(cutter.getUUID(), new PendingWarning(
                        level.getServer().getTickCount() + WARNING_DELAY_TICKS, level.dimension()));
                debug("vanished tree={} base={} moves={} cutter={}",
                        record.id(), base, record.moves(), cutter.getGameProfile().getName());
            } else {
                debug("ordinary cut tree={} base={} moves={}", record.id(), base, record.moves());
            }
            return;
        }
    }

    private static void deliverWarnings(MinecraftServer server, long now) {
        if (PENDING_WARNINGS.isEmpty()) {
            return;
        }
        var iterator = PENDING_WARNINGS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            PendingWarning warning = entry.getValue();
            if (now < warning.deliverTick()) {
                continue;
            }
            iterator.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null && player.isAlive() && player.level().dimension().equals(warning.dimension())
                    && now - warning.deliverTick() <= WARNING_MAX_LATENESS_TICKS) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(WARNING_LINE)
                        .withStyle(net.minecraft.ChatFormatting.DARK_RED));
            }
        }
    }

    /** For the GameTests: true while the cutter's warning line is still waiting for its delay. */
    public static boolean isWarningPending(UUID playerId) {
        return PENDING_WARNINGS.containsKey(playerId);
    }

    private static void vanish(ServerLevel level, TreeShape tree) {
        Vec3 centre = tree.crownCentre(BlockPos.ZERO);
        for (BlockPos cell : tree.cells().keySet()) {
            level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.playSound(null, centre.x, centre.y, centre.z,
                UncannySoundRegistry.UNCANNY_WANDERING_TREE_SCREAM.get(), SoundSource.HOSTILE,
                SCREAM_VOLUME, 0.96F + level.random.nextFloat() * 0.08F);
    }

    // ------------------------------------------------------------------ helpers

    private static TreeShape findTreeInRing(
            ServerLevel level,
            UncannyWorldState state,
            BlockPos centre,
            int minRadius,
            int maxRadius) {
        Set<BlockPos> candidates = new LinkedHashSet<>();
        for (int dx = -maxRadius; dx <= maxRadius; dx++) {
            for (int dz = -maxRadius; dz <= maxRadius; dz++) {
                int distanceSqr = dx * dx + dz * dz;
                if (distanceSqr < minRadius * minRadius || distanceSqr > maxRadius * maxRadius) {
                    continue;
                }
                int x = centre.getX() + dx;
                int z = centre.getZ() + dz;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
                if (level.getBlockState(top).is(BlockTags.LOGS)) {
                    candidates.add(top);
                }
            }
        }
        List<BlockPos> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, new java.util.Random(level.random.nextLong()));
        Set<BlockPos> rejectedBases = new HashSet<>();
        int scanned = 0;
        for (BlockPos top : shuffled) {
            if (scanned >= MAX_ADOPTION_CANDIDATES) {
                break;
            }
            BlockPos base = TreeShape.trunkBase(level, top);
            if (base == null || !rejectedBases.add(base)) {
                continue;
            }
            scanned++;
            TreeShape tree = TreeShape.scan(level, base);
            if (tree != null && !isTracked(state, level, tree.base())) {
                return tree;
            }
        }
        return null;
    }

    /** Settled trees are never re-read by the tick: forget those burnt, exploded or cut by other means. */
    private static void pruneVanishedTrees(MinecraftServer server, UncannyWorldState state, UUID owner) {
        for (WanderingTreeRecord record : state.getWanderingTrees()) {
            ServerLevel level = levelOf(server, record);
            if (record.owner().equals(owner) && level != null
                    && isAreaLoaded(level, record.base(), MAX_HORIZONTAL_REACH + 1)
                    && TreeShape.scanStanding(level, record.base()) == null) {
                state.removeWanderingTree(record.id());
            }
        }
    }

    /** A tree is only judged on fully loaded terrain; a half-loaded tree is retried, never forgotten. */
    private static boolean isAreaLoaded(ServerLevel level, BlockPos centre, int radius) {
        return level.hasChunksAt(centre.offset(-radius, 0, -radius), centre.offset(radius, 0, radius));
    }

    private static boolean isTracked(UncannyWorldState state, ServerLevel level, BlockPos base) {
        String dimension = level.dimension().location().toString();
        return state.getWanderingTrees().stream()
                .anyMatch(record -> record.dimension().equals(dimension) && record.base().equals(base));
    }

    private static WanderingTreeRecord latestOwned(ServerPlayer player, boolean movingOnly) {
        if (player.getServer() == null) {
            return null;
        }
        WanderingTreeRecord latest = null;
        for (WanderingTreeRecord record : UncannyWorldState.get(player.getServer()).getWanderingTrees()) {
            if (record.owner().equals(player.getUUID()) && (!movingOnly || !record.settled())) {
                latest = record;
            }
        }
        return latest;
    }

    private static ServerLevel levelOf(MinecraftServer server, WanderingTreeRecord record) {
        ResourceLocation id = ResourceLocation.tryParse(record.dimension());
        return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }

    /** Same base definition as the rest of the mod: Overworld respawn point, else world spawn. */
    public static BlockPos resolveBaseCenter(ServerPlayer player) {
        BlockPos respawn = player.getRespawnPosition();
        if (respawn != null && player.getRespawnDimension() == Level.OVERWORLD) {
            return respawn;
        }
        return player.server.overworld().getSharedSpawnPos();
    }

    static boolean isWanderingGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL);
    }

    private static void debug(String message, Object... args) {
        if (UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info("[UncannyDebug/WanderingTree] " + message, args);
        }
    }

    private record Placement(BlockPos offset, Set<BlockPos> extraClears) {
    }

    private record PendingWarning(long deliverTick, ResourceKey<Level> dimension) {
    }

    /**
     * A natural tree read from the world: logs of one kind joined to a trunk standing on dirt or
     * grass, the non-persistent leaves they feed, and snow layers resting on them.
     */
    private record TreeShape(
            BlockPos base,
            Map<BlockPos, BlockState> cells,
            Set<BlockPos> logs,
            Set<BlockPos> groundContacts,
            AABB bounds) {

        static BlockPos trunkBase(ServerLevel level, BlockPos log) {
            BlockState state = level.getBlockState(log);
            if (!state.is(BlockTags.LOGS)) {
                return null;
            }
            BlockPos cursor = log;
            for (int i = 0; i < MAX_TREE_HEIGHT; i++) {
                BlockPos below = cursor.below();
                if (!level.getBlockState(below).is(state.getBlock())) {
                    return cursor;
                }
                cursor = below;
            }
            return null;
        }

        /** Strict read used to adopt and move: an isolated natural tree standing on dirt or grass. */
        static TreeShape scan(ServerLevel level, BlockPos anyLog) {
            return scan(level, anyLog, true);
        }

        /**
         * Lenient read used by the trap and the clean-up: the logs joined to the trunk and the leaves
         * Vanilla attributes to them, whatever now touches the tree. Null only once the trunk is gone.
         */
        static TreeShape scanStanding(ServerLevel level, BlockPos anyLog) {
            return scan(level, anyLog, false);
        }

        private static TreeShape scan(ServerLevel level, BlockPos anyLog, boolean strict) {
            BlockPos base = trunkBase(level, anyLog);
            if (base == null || (strict && !isWanderingGround(level.getBlockState(base.below())))) {
                return null;
            }
            Block logBlock = level.getBlockState(base).getBlock();

            Set<BlockPos> logs = new LinkedHashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            logs.add(base);
            queue.add(base);
            while (!queue.isEmpty()) {
                BlockPos current = queue.poll();
                for (BlockPos next : BlockPos.betweenClosed(current.offset(-1, -1, -1), current.offset(1, 1, 1))) {
                    if (logs.contains(next) || !level.isLoaded(next) || !level.getBlockState(next).is(logBlock)) {
                        continue;
                    }
                    if (next.getY() < base.getY()
                            || Math.abs(next.getX() - base.getX()) > MAX_HORIZONTAL_REACH
                            || Math.abs(next.getZ() - base.getZ()) > MAX_HORIZONTAL_REACH
                            || next.getY() - base.getY() > MAX_TREE_HEIGHT
                            || logs.size() >= MAX_LOGS) {
                        if (strict) {
                            return null;
                        }
                        continue;
                    }
                    BlockPos immutable = next.immutable();
                    logs.add(immutable);
                    queue.add(immutable);
                }
            }

            Map<BlockPos, BlockState> cells = new LinkedHashMap<>();
            for (BlockPos log : logs) {
                BlockState state = level.getBlockState(log);
                if (strict && isWaterlogged(state)) {
                    return null;
                }
                cells.put(log, state);
            }
            // Leaves reached through leaves only, each exactly as far from our logs as Vanilla says it is
            // from its nearest log; a nearer foreign log means the canopy is shared with another tree.
            Map<BlockPos, Integer> leafSteps = new HashMap<>();
            ArrayDeque<BlockPos> leafQueue = new ArrayDeque<>(logs);
            int leafCount = 0;
            while (!leafQueue.isEmpty()) {
                BlockPos current = leafQueue.poll();
                int steps = leafSteps.getOrDefault(current, 0);
                if (steps >= MAX_LEAF_STEPS) {
                    continue;
                }
                for (Direction direction : Direction.values()) {
                    BlockPos next = current.relative(direction);
                    if (cells.containsKey(next) || !level.isLoaded(next)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(next);
                    if (!isNaturalLeaves(state)) {
                        continue;
                    }
                    int distance = state.getValue(LeavesBlock.DISTANCE);
                    if (distance < steps + 1 || isWaterlogged(state) || leafCount >= MAX_LEAVES) {
                        if (strict) {
                            return null;
                        }
                        // Closer to another log: that leaf belongs to someone else and is left alone.
                        continue;
                    }
                    leafCount++;
                    cells.put(next, state);
                    leafSteps.put(next, steps + 1);
                    leafQueue.add(next);
                }
            }
            // Snow resting on the tree travels with it.
            for (BlockPos cell : List.copyOf(cells.keySet())) {
                BlockPos above = cell.above();
                BlockState state = level.getBlockState(above);
                if (!cells.containsKey(above) && state.getBlock() instanceof SnowLayerBlock) {
                    cells.put(above, state);
                }
            }
            for (BlockPos cell : strict ? cells.keySet() : Set.<BlockPos>of()) {
                for (Direction direction : Direction.values()) {
                    BlockPos next = cell.relative(direction);
                    if (!cells.containsKey(next) && !isAcceptableNeighbour(level, next, base)) {
                        return null;
                    }
                }
            }

            Set<BlockPos> contacts = new LinkedHashSet<>();
            for (BlockPos log : logs) {
                BlockPos below = log.below();
                if (cells.containsKey(below)) {
                    continue;
                }
                BlockState belowState = level.getBlockState(below);
                if (isWanderingGround(belowState)) {
                    contacts.add(log);
                } else if (strict && !belowState.isAir() && !belowState.canBeReplaced()) {
                    return null;
                }
            }
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (BlockPos cell : cells.keySet()) {
                minX = Math.min(minX, cell.getX());
                minY = Math.min(minY, cell.getY());
                minZ = Math.min(minZ, cell.getZ());
                maxX = Math.max(maxX, cell.getX());
                maxY = Math.max(maxY, cell.getY());
                maxZ = Math.max(maxZ, cell.getZ());
            }
            AABB bounds = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
            return new TreeShape(base, Collections.unmodifiableMap(cells), Collections.unmodifiableSet(logs),
                    Collections.unmodifiableSet(contacts), bounds);
        }

        private static boolean isNaturalLeaves(BlockState state) {
            return state.is(BlockTags.LEAVES)
                    && state.hasProperty(LeavesBlock.PERSISTENT)
                    && state.hasProperty(LeavesBlock.DISTANCE)
                    && !state.getValue(LeavesBlock.PERSISTENT);
        }

        private static boolean isWaterlogged(BlockState state) {
            return state.hasProperty(BlockStateProperties.WATERLOGGED)
                    && state.getValue(BlockStateProperties.WATERLOGGED);
        }

        /**
         * Around the tree: open air, plain terrain at ground level, or loose vegetation. Anything that
         * hangs on the tree or was built against it keeps the tree where it is.
         */
        static boolean isAcceptableNeighbour(ServerLevel level, BlockPos pos, BlockPos base) {
            if (!level.isLoaded(pos)) {
                return false;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                return true;
            }
            if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                return false;
            }
            Block block = state.getBlock();
            if (block instanceof VineBlock || block instanceof MultifaceBlock || block instanceof CocoaBlock
                    || block instanceof BeehiveBlock || block instanceof WallTorchBlock
                    || block instanceof LadderBlock || block instanceof FaceAttachedHorizontalDirectionalBlock
                    || state.is(BlockTags.WALL_SIGNS) || state.is(BlockTags.WALL_HANGING_SIGNS)
                    || state.hasBlockEntity() || level.getBlockEntity(pos) != null) {
                return false;
            }
            return pos.getY() <= base.getY() + 1 || state.canBeReplaced();
        }

        List<Vec3> samplePoints(BlockPos offset) {
            List<Vec3> points = new ArrayList<>();
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (BlockPos log : logs) {
                if (log.getX() == base.getX() && log.getZ() == base.getZ()) {
                    minY = Math.min(minY, log.getY());
                    maxY = Math.max(maxY, log.getY());
                }
            }
            points.add(Vec3.atCenterOf(base.offset(offset)));
            points.add(Vec3.atCenterOf(new BlockPos(base.getX(), maxY, base.getZ()).offset(offset)));
            Vec3 crown = crownCentre(offset);
            points.add(crown);
            BlockPos highest = base;
            BlockPos west = base;
            BlockPos east = base;
            BlockPos north = base;
            BlockPos south = base;
            for (BlockPos cell : cells.keySet()) {
                if (cell.getY() > highest.getY()) {
                    highest = cell;
                }
                if (cell.getX() < west.getX()) {
                    west = cell;
                }
                if (cell.getX() > east.getX()) {
                    east = cell;
                }
                if (cell.getZ() < north.getZ()) {
                    north = cell;
                }
                if (cell.getZ() > south.getZ()) {
                    south = cell;
                }
            }
            for (BlockPos extreme : List.of(highest, west, east, north, south)) {
                points.add(Vec3.atCenterOf(extreme.offset(offset)));
            }
            return points;
        }

        Vec3 crownCentre(BlockPos offset) {
            double x = 0.0D;
            double y = 0.0D;
            double z = 0.0D;
            for (BlockPos cell : cells.keySet()) {
                x += cell.getX() + 0.5D;
                y += cell.getY() + 0.5D;
                z += cell.getZ() + 0.5D;
            }
            int count = cells.size();
            return new Vec3(x / count + offset.getX(), y / count + offset.getY(), z / count + offset.getZ());
        }
    }
}
