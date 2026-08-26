package com.eotv.echoofthevoid.event.paranoia.nativeevent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Shared, server-authored anomalies whose entire premise depends on real rail or redstone context.
 * Client block updates are presentations only and are always restored from the authoritative state.
 */
public final class RailAndSignalAnomalySystem {
    private static final int OBSERVER_RADIUS = 36;
    private static final long REDSTONE_CONTEXT_MAX_AGE_TICKS = 60L * 20L;
    private static final List<GhostCartTask> GHOST_CART_TASKS = new ArrayList<>();
    private static final List<FakeRailTask> FAKE_RAIL_TASKS = new ArrayList<>();
    private static final List<OrphanSignalTask> ORPHAN_SIGNAL_TASKS = new ArrayList<>();
    private static final Map<UUID, RedstoneContext> LAST_REDSTONE_CONTEXTS = new HashMap<>();

    private RailAndSignalAnomalySystem() {
    }

    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)
                || !event.getState().is(Blocks.REDSTONE_WIRE)
                || !event.getState().hasProperty(BlockStateProperties.POWER)) {
            return;
        }
        int power = event.getState().getValue(BlockStateProperties.POWER);
        if (power <= 0 || level.getServer() == null) {
            return;
        }
        long now = level.getServer().getTickCount();
        Vec3 center = Vec3.atCenterOf(event.getPos());
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator()
                    && player.position().distanceToSqr(center) <= 32.0D * 32.0D) {
                LAST_REDSTONE_CONTEXTS.put(player.getUUID(), new RedstoneContext(
                        level.dimension(), event.getPos().immutable(), power, now));
            }
        }
    }

    public static void tick(MinecraftServer server, long now) {
        tickGhostCarts(server, now);
        tickFakeRails(server, now);
        tickOrphanSignals(server, now);
        LAST_REDSTONE_CONTEXTS.entrySet().removeIf(
                entry -> now - entry.getValue().tick() > REDSTONE_CONTEXT_MAX_AGE_TICKS);
    }

    public static boolean triggerGhostCart(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<BlockPos> starts = findNearbyRails(level, player.blockPosition(), 18, 64);
        while (!starts.isEmpty()) {
            BlockPos start = starts.remove(level.random.nextInt(starts.size()));
            List<BlockPos> path = buildRailPath(level, start, 24);
            if (path.size() < 7 || hasMinecartNear(level, path, 4.0D)) {
                continue;
            }
            Set<UUID> observers = observerIds(level, Vec3.atCenterOf(path.get(path.size() / 2)), OBSERVER_RADIUS);
            if (observers.isEmpty()) {
                continue;
            }
            long now = player.getServer().getTickCount();
            GHOST_CART_TASKS.add(new GhostCartTask(
                    player.getUUID(), level.dimension(), List.copyOf(path), observers, 0, now + 4L));
            return true;
        }
        return false;
    }

    public static boolean triggerOrphanSignal(ServerPlayer player) {
        RedstoneContext context = LAST_REDSTONE_CONTEXTS.get(player.getUUID());
        if (context == null || !context.dimension().equals(player.serverLevel().dimension())) {
            return false;
        }
        MinecraftServer server = player.getServer();
        long now = server.getTickCount();
        ServerLevel level = player.serverLevel();
        BlockPos pos = context.pos();
        BlockState actual = level.hasChunkAt(pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
        if (now - context.tick() < 8L || now - context.tick() > REDSTONE_CONTEXT_MAX_AGE_TICKS
                || !actual.is(Blocks.REDSTONE_WIRE)
                || actual.getValue(BlockStateProperties.POWER) != 0
                || level.getBestNeighborSignal(pos) > 0
                || isObservedByAny(level, Vec3.atCenterOf(pos), 28.0D, 0.95D)) {
            return false;
        }
        Set<UUID> observers = observerIds(level, Vec3.atCenterOf(pos), OBSERVER_RADIUS);
        if (observers.isEmpty()) {
            return false;
        }
        BlockState apparent = actual.setValue(
                BlockStateProperties.POWER, Math.max(1, Math.min(15, context.power())));
        sendBlockState(server, observers, pos, apparent);
        ORPHAN_SIGNAL_TASKS.add(new OrphanSignalTask(
                player.getUUID(), level.dimension(), pos.immutable(), observers,
                now + 14L + level.random.nextInt(11)));
        LAST_REDSTONE_CONTEXTS.remove(player.getUUID());
        return true;
    }

    public static void clearForOwner(MinecraftServer server, UUID ownerId) {
        LAST_REDSTONE_CONTEXTS.remove(ownerId);
        GHOST_CART_TASKS.removeIf(task -> task.ownerId().equals(ownerId));

        Iterator<FakeRailTask> railIterator = FAKE_RAIL_TASKS.iterator();
        while (railIterator.hasNext()) {
            FakeRailTask task = railIterator.next();
            if (task.ownerId().equals(ownerId)) {
                restoreBlock(server, task.dimension(), task.pos(), task.observers());
                railIterator.remove();
            }
        }
        Iterator<OrphanSignalTask> signalIterator = ORPHAN_SIGNAL_TASKS.iterator();
        while (signalIterator.hasNext()) {
            OrphanSignalTask task = signalIterator.next();
            if (task.ownerId().equals(ownerId)) {
                restoreBlock(server, task.dimension(), task.pos(), task.observers());
                signalIterator.remove();
            }
        }
    }

    public static void clear(MinecraftServer server) {
        if (server != null) {
            for (FakeRailTask task : FAKE_RAIL_TASKS) {
                restoreBlock(server, task.dimension(), task.pos(), task.observers());
            }
            for (OrphanSignalTask task : ORPHAN_SIGNAL_TASKS) {
                restoreBlock(server, task.dimension(), task.pos(), task.observers());
            }
        }
        GHOST_CART_TASKS.clear();
        FAKE_RAIL_TASKS.clear();
        ORPHAN_SIGNAL_TASKS.clear();
        LAST_REDSTONE_CONTEXTS.clear();
    }

    private static void tickGhostCarts(MinecraftServer server, long now) {
        Iterator<GhostCartTask> iterator = GHOST_CART_TASKS.iterator();
        while (iterator.hasNext()) {
            GhostCartTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            if (level == null || owner == null || owner.serverLevel() != level
                    || task.index() >= task.path().size()) {
                iterator.remove();
                continue;
            }
            if (now < task.nextSoundTick()) {
                continue;
            }
            BlockPos pos = task.path().get(task.index());
            BlockState state = level.hasChunkAt(pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
            if (!(state.getBlock() instanceof BaseRailBlock)
                    || !level.getEntitiesOfClass(AbstractMinecart.class,
                    new AABB(pos).inflate(3.0D), Entity::isAlive).isEmpty()) {
                iterator.remove();
                continue;
            }
            level.playSound(null, pos, SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL,
                    0.30F, 0.92F + level.random.nextFloat() * 0.10F);
            if (state.getBlock() instanceof PoweredRailBlock
                    && state.hasProperty(BlockStateProperties.POWERED)
                    && !state.getValue(BlockStateProperties.POWERED)
                    && !level.hasNeighborSignal(pos)
                    && FAKE_RAIL_TASKS.stream().noneMatch(existing ->
                    existing.dimension().equals(level.dimension()) && existing.pos().equals(pos))) {
                sendBlockState(server, task.observers(), pos,
                        state.setValue(BlockStateProperties.POWERED, true));
                FAKE_RAIL_TASKS.add(new FakeRailTask(
                        task.ownerId(), task.dimension(), pos.immutable(), task.observers(), now + 8L));
            }
            task.advance(now + 6L + level.random.nextInt(4));
        }
    }

    private static void tickFakeRails(MinecraftServer server, long now) {
        Iterator<FakeRailTask> iterator = FAKE_RAIL_TASKS.iterator();
        while (iterator.hasNext()) {
            FakeRailTask task = iterator.next();
            if (now >= task.restoreTick()) {
                restoreBlock(server, task.dimension(), task.pos(), task.observers());
                iterator.remove();
            }
        }
    }

    private static void tickOrphanSignals(MinecraftServer server, long now) {
        Iterator<OrphanSignalTask> iterator = ORPHAN_SIGNAL_TASKS.iterator();
        while (iterator.hasNext()) {
            OrphanSignalTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            BlockState actual = level == null || !level.hasChunkAt(task.pos())
                    ? Blocks.AIR.defaultBlockState()
                    : level.getBlockState(task.pos());
            if (now >= task.restoreTick() || !actual.is(Blocks.REDSTONE_WIRE)
                    || actual.getValue(BlockStateProperties.POWER) > 0) {
                restoreBlock(server, task.dimension(), task.pos(), task.observers());
                iterator.remove();
            }
        }
    }

    private static List<BlockPos> findNearbyRails(ServerLevel level, BlockPos center, int radius, int limit) {
        List<BlockPos> result = new ArrayList<>();
        for (int y = -7; y <= 7 && result.size() < limit; y++) {
            for (int x = -radius; x <= radius && result.size() < limit; x++) {
                for (int z = -radius; z <= radius && result.size() < limit; z++) {
                    if (x * x + z * z < 36 || x * x + z * z > radius * radius) {
                        continue;
                    }
                    BlockPos pos = center.offset(x, y, z);
                    if (level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof BaseRailBlock) {
                        result.add(pos.immutable());
                    }
                }
            }
        }
        return result;
    }

    private static List<BlockPos> buildRailPath(ServerLevel level, BlockPos start, int limit) {
        List<BlockPos> path = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        BlockPos current = start;
        while (path.size() < limit && current != null && visited.add(current)) {
            path.add(current.immutable());
            List<BlockPos> candidates = connectedRailNeighbors(level, current).stream()
                    .filter(pos -> !visited.contains(pos))
                    .toList();
            current = candidates.isEmpty() ? null : candidates.get(level.random.nextInt(candidates.size()));
        }
        return path;
    }

    private static List<BlockPos> connectedRailNeighbors(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BaseRailBlock rail)) {
            return List.of();
        }
        EnumSet<Direction> exits = railExits(rail.getRailDirection(state, level, pos, null));
        List<BlockPos> result = new ArrayList<>(2);
        for (Direction direction : exits) {
            for (int dy : new int[]{0, 1, -1}) {
                BlockPos candidate = pos.relative(direction).offset(0, dy, 0);
                if (!level.hasChunkAt(candidate)) {
                    continue;
                }
                BlockState candidateState = level.getBlockState(candidate);
                if (candidateState.getBlock() instanceof BaseRailBlock candidateRail
                        && railExits(candidateRail.getRailDirection(candidateState, level, candidate, null))
                        .contains(direction.getOpposite())) {
                    result.add(candidate.immutable());
                    break;
                }
            }
        }
        return result;
    }

    private static EnumSet<Direction> railExits(RailShape shape) {
        return switch (shape) {
            case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> EnumSet.of(Direction.NORTH, Direction.SOUTH);
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> EnumSet.of(Direction.EAST, Direction.WEST);
            case SOUTH_EAST -> EnumSet.of(Direction.SOUTH, Direction.EAST);
            case SOUTH_WEST -> EnumSet.of(Direction.SOUTH, Direction.WEST);
            case NORTH_WEST -> EnumSet.of(Direction.NORTH, Direction.WEST);
            case NORTH_EAST -> EnumSet.of(Direction.NORTH, Direction.EAST);
        };
    }

    private static boolean hasMinecartNear(ServerLevel level, List<BlockPos> path, double radius) {
        AABB bounds = new AABB(path.getFirst());
        for (BlockPos pos : path) {
            bounds = bounds.minmax(new AABB(pos));
        }
        return !level.getEntitiesOfClass(AbstractMinecart.class, bounds.inflate(radius), Entity::isAlive).isEmpty();
    }

    private static boolean isObservedByAny(ServerLevel level, Vec3 point, double radius, double threshold) {
        for (ServerPlayer observer : level.players()) {
            if (!observer.isAlive() || observer.isSpectator()) {
                continue;
            }
            Vec3 delta = point.subtract(observer.getEyePosition());
            if (delta.lengthSqr() <= radius * radius
                    && observer.getLookAngle().normalize().dot(delta.normalize()) >= threshold) {
                return true;
            }
        }
        return false;
    }

    private static Set<UUID> observerIds(ServerLevel level, Vec3 center, double radius) {
        Set<UUID> result = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator()
                    && player.position().distanceToSqr(center) <= radius * radius) {
                result.add(player.getUUID());
            }
        }
        return Set.copyOf(result);
    }

    private static void sendBlockState(
            MinecraftServer server, Set<UUID> observers, BlockPos pos, BlockState state) {
        if (server == null) {
            return;
        }
        for (UUID observerId : observers) {
            ServerPlayer observer = server.getPlayerList().getPlayer(observerId);
            if (observer != null) {
                observer.connection.send(new ClientboundBlockUpdatePacket(pos, state));
            }
        }
    }

    private static void restoreBlock(
            MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos, Set<UUID> observers) {
        ServerLevel level = server == null ? null : server.getLevel(dimension);
        if (level != null && level.hasChunkAt(pos)) {
            sendBlockState(server, observers, pos, level.getBlockState(pos));
        }
    }

    private record RedstoneContext(ResourceKey<Level> dimension, BlockPos pos, int power, long tick) {
    }

    private static final class GhostCartTask {
        private final UUID ownerId;
        private final ResourceKey<Level> dimension;
        private final List<BlockPos> path;
        private final Set<UUID> observers;
        private int index;
        private long nextSoundTick;

        private GhostCartTask(UUID ownerId, ResourceKey<Level> dimension, List<BlockPos> path,
                              Set<UUID> observers, int index, long nextSoundTick) {
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.path = path;
            this.observers = observers;
            this.index = index;
            this.nextSoundTick = nextSoundTick;
        }

        private UUID ownerId() { return ownerId; }
        private ResourceKey<Level> dimension() { return dimension; }
        private List<BlockPos> path() { return path; }
        private Set<UUID> observers() { return observers; }
        private int index() { return index; }
        private long nextSoundTick() { return nextSoundTick; }
        private void advance(long nextTick) { index++; nextSoundTick = nextTick; }
    }

    private record FakeRailTask(UUID ownerId, ResourceKey<Level> dimension, BlockPos pos,
                                Set<UUID> observers, long restoreTick) {
    }

    private record OrphanSignalTask(UUID ownerId, ResourceKey<Level> dimension, BlockPos pos,
                                    Set<UUID> observers, long restoreTick) {
    }
}
