package com.eotv.echoofthevoid.event.paranoia.nativeevent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounded server tasks for water, lava, sculk and container anomalies. */
public final class ContextualWorldAnomalySystem {
    private static final int OBSERVER_RADIUS = 36;
    private static final List<CountercurrentTask> COUNTERCURRENT_TASKS = new ArrayList<>();
    private static final List<SculkTask> SCULK_TASKS = new ArrayList<>();
    private static final List<LavaWakeTask> LAVA_WAKE_TASKS = new ArrayList<>();
    private static final List<LidTask> LID_TASKS = new ArrayList<>();

    private ContextualWorldAnomalySystem() {
    }

    public static void tick(MinecraftServer server, long now) {
        tickCountercurrents(server, now);
        tickSculk(server, now);
        tickLavaWakes(server, now);
        tickLids(server, now);
    }

    public static boolean triggerCountercurrentColumn(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<BlockPos> columns = findNearbyBlocks(level, player.blockPosition(), 16,
                state -> state.is(Blocks.BUBBLE_COLUMN), 40);
        columns.removeIf(pos -> COUNTERCURRENT_TASKS.stream().anyMatch(task ->
                task.dimension().equals(level.dimension()) && task.pos().equals(pos))
                || level.players().stream().anyMatch(observer ->
                observer.position().distanceToSqr(Vec3.atCenterOf(pos)) <= 3.0D * 3.0D));
        if (columns.isEmpty()) {
            return false;
        }
        BlockPos pos = columns.get(level.random.nextInt(columns.size()));
        boolean actualDown = level.getBlockState(pos).getValue(BubbleColumnBlock.DRAG_DOWN);
        long now = player.getServer().getTickCount();
        COUNTERCURRENT_TASKS.add(new CountercurrentTask(
                player.getUUID(), level.dimension(), pos.immutable(), actualDown,
                now + 32L + level.random.nextInt(21), now + 2L));
        return true;
    }

    public static boolean triggerFalseSculkVibration(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.getEntitiesOfClass(Warden.class, player.getBoundingBox().inflate(48.0D), Entity::isAlive).isEmpty()) {
            return false;
        }
        List<BlockPos> sensors = findNearbyBlocks(level, player.blockPosition(), 18,
                state -> state.getBlock() instanceof SculkSensorBlock
                        && state.getValue(SculkSensorBlock.PHASE) == SculkSensorPhase.INACTIVE
                        && state.getValue(SculkSensorBlock.POWER) == 0, 32);
        sensors.removeIf(pos -> isObservedByAny(level, Vec3.atCenterOf(pos), 28.0D, 0.94D)
                || SCULK_TASKS.stream().anyMatch(task ->
                task.dimension().equals(level.dimension()) && task.pos().equals(pos))
                || hasDangerousSculkOrMachine(level, pos));
        if (sensors.isEmpty()) {
            return false;
        }
        BlockPos pos = sensors.get(level.random.nextInt(sensors.size()));
        Set<UUID> observers = observerIds(level, Vec3.atCenterOf(pos), OBSERVER_RADIUS);
        if (observers.isEmpty()) {
            return false;
        }
        BlockState actual = level.getBlockState(pos);
        BlockState apparent = actual
                .setValue(SculkSensorBlock.PHASE, SculkSensorPhase.ACTIVE)
                .setValue(SculkSensorBlock.POWER, 0);
        sendBlockState(player.getServer(), observers, pos, apparent);

        Direction sourceDirection = Direction.Plane.HORIZONTAL.getRandomDirection(level.random);
        BlockPos source = pos.relative(sourceDirection, 3 + level.random.nextInt(4)).above();
        level.sendParticles(
                new VibrationParticleOption(new BlockPositionSource(pos), 18),
                source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, pos, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS,
                0.28F, 0.90F + level.random.nextFloat() * 0.12F);
        SCULK_TASKS.add(new SculkTask(
                player.getUUID(), level.dimension(), pos.immutable(), observers,
                player.getServer().getTickCount() + 30L));
        return true;
    }

    public static boolean triggerLavaWake(ServerPlayer player) {
        if (player.serverLevel().dimension() != Level.NETHER) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        List<Vec3> points = findLavaWake(level, player);
        if (points.size() < 7 || !level.getEntitiesOfClass(
                Strider.class,
                bounds(points).inflate(10.0D),
                Entity::isAlive).isEmpty()) {
            return false;
        }
        LAVA_WAKE_TASKS.add(new LavaWakeTask(
                player.getUUID(), level.dimension(), List.copyOf(points), 0,
                player.getServer().getTickCount() + 3L));
        return true;
    }

    public static boolean triggerFalseLid(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<BlockPos> containers = findNearbyBlocks(level, player.blockPosition(), 16,
                ContextualWorldAnomalySystem::isEligibleLidBlock, 32);
        containers.removeIf(pos -> isObservedByAny(level, Vec3.atCenterOf(pos), 24.0D, 0.93D)
                || level.players().stream().anyMatch(observer ->
                observer.position().distanceToSqr(Vec3.atCenterOf(pos)) <= 6.0D * 6.0D)
                || LID_TASKS.stream().anyMatch(task ->
                task.dimension().equals(level.dimension()) && task.pos().equals(pos))
                || ChestBlockEntity.getOpenCount(level, pos) > 0);
        if (containers.isEmpty()) {
            return false;
        }
        BlockPos pos = containers.get(level.random.nextInt(containers.size()));
        Block block = level.getBlockState(pos).getBlock();
        Set<UUID> observers = observerIds(level, Vec3.atCenterOf(pos), OBSERVER_RADIUS);
        if (observers.isEmpty()) {
            return false;
        }
        sendBlockEvent(player.getServer(), observers, pos, block, 1, 1);
        long now = player.getServer().getTickCount();
        LID_TASKS.add(new LidTask(
                player.getUUID(), level.dimension(), pos.immutable(), block, observers,
                now + 8L, now + 24L + level.random.nextInt(17)));
        return true;
    }

    public static void clearForOwner(MinecraftServer server, UUID ownerId) {
        COUNTERCURRENT_TASKS.removeIf(task -> task.ownerId().equals(ownerId));
        LAVA_WAKE_TASKS.removeIf(task -> task.ownerId().equals(ownerId));
        Iterator<SculkTask> sculkIterator = SCULK_TASKS.iterator();
        while (sculkIterator.hasNext()) {
            SculkTask task = sculkIterator.next();
            if (task.ownerId().equals(ownerId)) {
                restoreBlock(server, task.dimension(), task.pos(), task.observers());
                sculkIterator.remove();
            }
        }
        Iterator<LidTask> lidIterator = LID_TASKS.iterator();
        while (lidIterator.hasNext()) {
            LidTask task = lidIterator.next();
            if (task.ownerId().equals(ownerId)) {
                sendBlockEvent(server, task.observers(), task.pos(), task.block(), 1, 0);
                lidIterator.remove();
            }
        }
    }

    public static void clear(MinecraftServer server) {
        if (server != null) {
            SCULK_TASKS.forEach(task -> restoreBlock(
                    server, task.dimension(), task.pos(), task.observers()));
            LID_TASKS.forEach(task -> sendBlockEvent(
                    server, task.observers(), task.pos(), task.block(), 1, 0));
        }
        COUNTERCURRENT_TASKS.clear();
        SCULK_TASKS.clear();
        LAVA_WAKE_TASKS.clear();
        LID_TASKS.clear();
    }

    private static void tickCountercurrents(MinecraftServer server, long now) {
        Iterator<CountercurrentTask> iterator = COUNTERCURRENT_TASKS.iterator();
        while (iterator.hasNext()) {
            CountercurrentTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            BlockState state = level == null || !level.hasChunkAt(task.pos())
                    ? Blocks.AIR.defaultBlockState() : level.getBlockState(task.pos());
            if (level == null || owner == null || owner.serverLevel() != level || now >= task.endTick()
                    || !state.is(Blocks.BUBBLE_COLUMN)
                    || state.getValue(BubbleColumnBlock.DRAG_DOWN) != task.actualDown()
                    || level.players().stream().anyMatch(observer ->
                    observer.isInWaterOrBubble()
                            && observer.position().distanceToSqr(Vec3.atCenterOf(task.pos())) < 2.25D)) {
                iterator.remove();
                continue;
            }
            if (now < task.nextPulseTick()) {
                continue;
            }
            ParticleOptions opposite = task.actualDown()
                    ? ParticleTypes.BUBBLE_COLUMN_UP
                    : ParticleTypes.CURRENT_DOWN;
            level.sendParticles(opposite,
                    task.pos().getX() + 0.5D, task.pos().getY() + 0.55D, task.pos().getZ() + 0.5D,
                    4, 0.22D, 0.35D, 0.22D, 0.015D);
            task.advance(now + 2L + level.random.nextInt(3));
        }
    }

    private static void tickSculk(MinecraftServer server, long now) {
        Iterator<SculkTask> iterator = SCULK_TASKS.iterator();
        while (iterator.hasNext()) {
            SculkTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            BlockState actual = level == null || !level.hasChunkAt(task.pos())
                    ? Blocks.AIR.defaultBlockState() : level.getBlockState(task.pos());
            if (now >= task.restoreTick() || !(actual.getBlock() instanceof SculkSensorBlock)
                    || actual.getValue(SculkSensorBlock.PHASE) != SculkSensorPhase.INACTIVE) {
                restoreBlock(server, task.dimension(), task.pos(), task.observers());
                iterator.remove();
            }
        }
    }

    private static void tickLavaWakes(MinecraftServer server, long now) {
        Iterator<LavaWakeTask> iterator = LAVA_WAKE_TASKS.iterator();
        while (iterator.hasNext()) {
            LavaWakeTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            if (level == null || owner == null || owner.serverLevel() != level
                    || task.index() >= task.points().size()) {
                iterator.remove();
                continue;
            }
            if (now < task.nextPulseTick()) {
                continue;
            }
            Vec3 point = task.points().get(task.index());
            BlockPos lavaPos = BlockPos.containing(point.x, point.y - 0.15D, point.z);
            if (!isLavaSurface(level, lavaPos)) {
                iterator.remove();
                continue;
            }
            level.sendParticles(ParticleTypes.LAVA, point.x, point.y, point.z,
                    task.index() + 1 == task.points().size() ? 4 : 2,
                    0.18D, 0.02D, 0.18D, 0.01D);
            if (task.index() % 2 == 0) {
                level.playSound(null, BlockPos.containing(point), SoundEvents.STRIDER_STEP_LAVA,
                        SoundSource.NEUTRAL, 0.25F, 0.88F + level.random.nextFloat() * 0.15F);
            }
            task.advance(now + 4L + level.random.nextInt(3));
        }
    }

    private static void tickLids(MinecraftServer server, long now) {
        Iterator<LidTask> iterator = LID_TASKS.iterator();
        while (iterator.hasNext()) {
            LidTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            boolean close = level == null || !level.hasChunkAt(task.pos())
                    || level.getBlockState(task.pos()).getBlock() != task.block()
                    || now >= task.endTick();
            if (!close && now >= task.minimumVisibleUntil()) {
                close = isObservedByAny(level, Vec3.atCenterOf(task.pos()), 24.0D, 0.96D);
            }
            if (close) {
                sendBlockEvent(server, task.observers(), task.pos(), task.block(), 1, 0);
                iterator.remove();
            }
        }
    }

    private static boolean isEligibleLidBlock(BlockState state) {
        if (state.getBlock() instanceof ChestBlock) {
            return state.getValue(ChestBlock.TYPE) == ChestType.SINGLE;
        }
        return state.getBlock() instanceof ShulkerBoxBlock;
    }

    private static boolean hasDangerousSculkOrMachine(ServerLevel level, BlockPos center) {
        List<BlockPos> nearby = findNearbyBlocks(level, center, 10,
                state -> state.is(Blocks.SCULK_SHRIEKER)
                        || state.is(Blocks.REDSTONE_WIRE)
                        || state.is(Blocks.REPEATER)
                        || state.is(Blocks.COMPARATOR), 1);
        return !nearby.isEmpty();
    }

    private static List<Vec3> findLavaWake(ServerLevel level, ServerPlayer player) {
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        BlockPos center = player.blockPosition();
        for (int attempt = 0; attempt < 100; attempt++) {
            BlockPos start = center.offset(level.random.nextInt(29) - 14, level.random.nextInt(11) - 5,
                    level.random.nextInt(29) - 14);
            if (start.distSqr(center) < 36.0D || !isLavaSurface(level, start)) {
                continue;
            }
            Direction first = directions[level.random.nextInt(directions.length)];
            Direction second = level.random.nextBoolean() ? first.getClockWise() : first.getCounterClockWise();
            int turnAfter = 3 + level.random.nextInt(3);
            int length = 8 + level.random.nextInt(5);
            List<Vec3> points = new ArrayList<>(length);
            BlockPos cursor = start;
            boolean valid = true;
            for (int index = 0; index < length; index++) {
                if (!isLavaSurface(level, cursor)
                        || player.position().distanceToSqr(Vec3.atCenterOf(cursor)) < 4.0D * 4.0D) {
                    valid = false;
                    break;
                }
                points.add(new Vec3(cursor.getX() + 0.5D, cursor.getY() + 1.02D, cursor.getZ() + 0.5D));
                cursor = cursor.relative(index + 1 < turnAfter ? first : second);
            }
            if (valid) {
                return points;
            }
        }
        return List.of();
    }

    private static boolean isLavaSurface(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getFluidState(pos).is(FluidTags.LAVA)
                && !level.getFluidState(pos.above()).is(FluidTags.LAVA)
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    private static AABB bounds(List<Vec3> points) {
        AABB result = new AABB(points.getFirst(), points.getFirst());
        for (Vec3 point : points) {
            result = result.minmax(new AABB(point, point));
        }
        return result;
    }

    private static List<BlockPos> findNearbyBlocks(
            ServerLevel level, BlockPos center, int radius, Predicate<BlockState> predicate, int limit) {
        List<BlockPos> result = new ArrayList<>();
        int vertical = Math.min(8, radius);
        for (int y = -vertical; y <= vertical && result.size() < limit; y++) {
            for (int x = -radius; x <= radius && result.size() < limit; x++) {
                for (int z = -radius; z <= radius && result.size() < limit; z++) {
                    if (x * x + z * z > radius * radius) {
                        continue;
                    }
                    BlockPos pos = center.offset(x, y, z);
                    if (level.hasChunkAt(pos) && predicate.test(level.getBlockState(pos))) {
                        result.add(pos.immutable());
                    }
                }
            }
        }
        return result;
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

    private static void sendBlockEvent(
            MinecraftServer server, Set<UUID> observers, BlockPos pos, Block block, int id, int param) {
        if (server == null) {
            return;
        }
        for (UUID observerId : observers) {
            ServerPlayer observer = server.getPlayerList().getPlayer(observerId);
            if (observer != null) {
                observer.connection.send(new ClientboundBlockEventPacket(pos, block, id, param));
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

    private static final class CountercurrentTask {
        private final UUID ownerId;
        private final ResourceKey<Level> dimension;
        private final BlockPos pos;
        private final boolean actualDown;
        private final long endTick;
        private long nextPulseTick;

        private CountercurrentTask(UUID ownerId, ResourceKey<Level> dimension, BlockPos pos,
                                   boolean actualDown, long endTick, long nextPulseTick) {
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.pos = pos;
            this.actualDown = actualDown;
            this.endTick = endTick;
            this.nextPulseTick = nextPulseTick;
        }

        private UUID ownerId() { return ownerId; }
        private ResourceKey<Level> dimension() { return dimension; }
        private BlockPos pos() { return pos; }
        private boolean actualDown() { return actualDown; }
        private long endTick() { return endTick; }
        private long nextPulseTick() { return nextPulseTick; }
        private void advance(long nextTick) { nextPulseTick = nextTick; }
    }

    private record SculkTask(UUID ownerId, ResourceKey<Level> dimension, BlockPos pos,
                             Set<UUID> observers, long restoreTick) {
    }

    private static final class LavaWakeTask {
        private final UUID ownerId;
        private final ResourceKey<Level> dimension;
        private final List<Vec3> points;
        private int index;
        private long nextPulseTick;

        private LavaWakeTask(UUID ownerId, ResourceKey<Level> dimension, List<Vec3> points,
                             int index, long nextPulseTick) {
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.points = points;
            this.index = index;
            this.nextPulseTick = nextPulseTick;
        }

        private UUID ownerId() { return ownerId; }
        private ResourceKey<Level> dimension() { return dimension; }
        private List<Vec3> points() { return points; }
        private int index() { return index; }
        private long nextPulseTick() { return nextPulseTick; }
        private void advance(long nextTick) { index++; nextPulseTick = nextTick; }
    }

    private record LidTask(UUID ownerId, ResourceKey<Level> dimension, BlockPos pos, Block block,
                           Set<UUID> observers, long minimumVisibleUntil, long endTick) {
    }
}
