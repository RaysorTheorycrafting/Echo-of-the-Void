package com.eotv.echoofthevoid.event.paranoia.nativeevent;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.world.ObserverSight;
import com.eotv.echoofthevoid.world.UncannyBlockMutationSafety;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Ghost blocks, the Vanilla desync bug used on purpose: one player's client is told something about
 * a single block that the server knows to be false. The server state is never touched.
 *
 * <ul>
 *   <li>{@link Kind#RESTORED}: a block just mined in a tunnel is back when the player turns around.
 *       Harmless on the client (an obstacle that is not there): it vanishes at the first touch, hit
 *       or interaction, or after a while.</li>
 *   <li>{@link Kind#MISSING}: at night a wall block of the base shows a hole onto the dark outside.
 *       Since the client would otherwise need to place a block to fix it, the true block is sent
 *       back one second after the player has looked at it, or as soon as they come close.</li>
 * </ul>
 */
public final class GhostBlockSystem {
    private static final int MEMORY_PER_PLAYER = 16;
    private static final long MEMORY_TICKS = 20L * 60L * 3L;
    private static final long RESTORED_LIFETIME_TICKS = 20L * 90L;
    private static final long MISSING_LIFETIME_TICKS = 20L * 120L;
    private static final long MISSING_RESYNC_AFTER_LOOK_TICKS = 20L;
    private static final double RESTORED_TOUCH_DISTANCE = 2.2D;
    private static final double MISSING_APPROACH_DISTANCE = 3.0D;
    private static final double LOOK_DOT = 0.95D;
    private static final Map<UUID, Deque<MinedBlock>> RECENTLY_MINED = new HashMap<>();
    private static final List<Ghost> GHOSTS = new ArrayList<>();

    private GhostBlockSystem() {
    }

    public enum Kind {
        RESTORED,
        MISSING
    }

    private record MinedBlock(ResourceKey<Level> dimension, BlockPos pos, BlockState state, long tick) {
    }

    private static final class Ghost {
        private final UUID owner;
        private final ResourceKey<Level> dimension;
        private final BlockPos pos;
        private final Kind kind;
        private final long expireTick;
        private long resyncTick = Long.MAX_VALUE;

        private Ghost(UUID owner, ResourceKey<Level> dimension, BlockPos pos, Kind kind, long expireTick) {
            this.owner = owner;
            this.dimension = dimension;
            this.pos = pos.immutable();
            this.kind = kind;
            this.expireTick = expireTick;
        }
    }

    // ------------------------------------------------------------------ memory of mined blocks

    /** Runs late so a cancelled break is never remembered. */
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level) || player.getServer() == null) {
            return;
        }
        BlockPos pos = event.getPos().immutable();
        BlockState state = event.getState();
        if (level.canSeeSky(pos) || !isPlainTerrain(level, pos, state)) {
            return;
        }
        Deque<MinedBlock> memory = RECENTLY_MINED.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>());
        memory.addFirst(new MinedBlock(level.dimension(), pos, state, player.getServer().getTickCount()));
        while (memory.size() > MEMORY_PER_PLAYER) {
            memory.removeLast();
        }
    }

    private static boolean isPlainTerrain(ServerLevel level, BlockPos pos, BlockState state) {
        return state.isSolidRender(level, pos) && !UncannyBlockMutationSafety.isProtected(level, pos, state)
                && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    // ------------------------------------------------------------------ triggers

    public static boolean trigger(ServerPlayer player, Kind kind, boolean debugImmediate) {
        if (player.getServer() == null || GHOSTS.stream().anyMatch(ghost -> ghost.owner.equals(player.getUUID()))) {
            return false;
        }
        return kind == Kind.RESTORED ? triggerRestored(player) : triggerMissing(player, debugImmediate);
    }

    private static boolean triggerRestored(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = player.getServer().getTickCount();
        Deque<MinedBlock> memory = RECENTLY_MINED.getOrDefault(player.getUUID(), new ArrayDeque<>());
        for (MinedBlock mined : memory) {
            if (now - mined.tick() > MEMORY_TICKS || !mined.dimension().equals(level.dimension())) {
                continue;
            }
            Vec3 centre = Vec3.atCenterOf(mined.pos());
            double distance = player.getEyePosition().distanceTo(centre);
            if (distance < 4.0D || distance > 12.0D || !level.getBlockState(mined.pos()).isAir()
                    || isInSight(player, centre)) {
                continue;
            }
            show(player, mined.pos(), mined.state(), Kind.RESTORED, now + RESTORED_LIFETIME_TICKS);
            return true;
        }
        return false;
    }

    private static boolean triggerMissing(ServerPlayer player, boolean debugImmediate) {
        ServerLevel level = player.serverLevel();
        if (!debugImmediate && (!AnimalFormationRules.isNight(level.getDayTime())
                || level.canSeeSky(player.blockPosition())
                || player.blockPosition().distSqr(WanderingTreeSystem.resolveBaseCenter(player)) > 32 * 32)) {
            return false;
        }
        BlockPos feet = player.blockPosition();
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-10, 0, -10), feet.offset(10, 2, 10))) {
            double distanceSqr = pos.distSqr(feet);
            if (distanceSqr < 16 || distanceSqr > 100 || !level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!isPlainTerrain(level, pos, state) || !isOuterWall(level, pos, player)) {
                continue;
            }
            if (!debugImmediate && isInSight(player, Vec3.atCenterOf(pos))) {
                continue;
            }
            candidates.add(pos.immutable());
        }
        if (candidates.isEmpty()) {
            return false;
        }
        BlockPos chosen = candidates.get(level.random.nextInt(candidates.size()));
        show(player, chosen, Blocks.AIR.defaultBlockState(), Kind.MISSING,
                player.getServer().getTickCount() + MISSING_LIFETIME_TICKS);
        return true;
    }

    /** A wall block with the room on the player's side and the open night behind it. */
    private static boolean isOuterWall(ServerLevel level, BlockPos pos, ServerPlayer player) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos inside = pos.relative(direction);
            BlockPos outside = pos.relative(direction.getOpposite());
            boolean facesPlayer = Vec3.atCenterOf(inside).distanceToSqr(player.position())
                    < Vec3.atCenterOf(outside).distanceToSqr(player.position());
            if (facesPlayer && level.getBlockState(inside).isAir() && !level.canSeeSky(inside)
                    && level.getBlockState(outside).isAir() && level.canSeeSky(outside)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInSight(ServerPlayer player, Vec3 point) {
        return ObserverSight.isSeenByAny(player.serverLevel(), List.of(point), List.of(player), 0.0D, pos -> false);
    }

    private static void show(ServerPlayer player, BlockPos pos, BlockState fake, Kind kind, long expireTick) {
        player.connection.send(new ClientboundBlockUpdatePacket(pos, fake));
        GHOSTS.add(new Ghost(player.getUUID(), player.serverLevel().dimension(), pos, kind, expireTick));
        debug("{} ghost for {} at {}", kind, player.getGameProfile().getName(), pos);
    }

    // ------------------------------------------------------------------ resolving

    public static void tick(MinecraftServer server, long now) {
        if (GHOSTS.isEmpty() || now % 2L != 0L) {
            return;
        }
        Iterator<Ghost> iterator = GHOSTS.iterator();
        while (iterator.hasNext()) {
            Ghost ghost = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(ghost.owner);
            if (player == null || !player.serverLevel().dimension().equals(ghost.dimension)) {
                // A client leaving the level forgets the ghost with the rest of its chunks.
                iterator.remove();
                continue;
            }
            if (shouldResolve(player, ghost, now)) {
                resync(player, ghost);
                iterator.remove();
            }
        }
    }

    private static boolean shouldResolve(ServerPlayer player, Ghost ghost, long now) {
        if (now >= ghost.expireTick || now >= ghost.resyncTick) {
            return true;
        }
        Vec3 centre = Vec3.atCenterOf(ghost.pos);
        double distance = player.position().add(0.0D, 0.9D, 0.0D).distanceTo(centre);
        if (ghost.kind == Kind.RESTORED) {
            return distance <= RESTORED_TOUCH_DISTANCE;
        }
        if (distance <= MISSING_APPROACH_DISTANCE) {
            return true;
        }
        if (ghost.resyncTick == Long.MAX_VALUE && ObserverSight.isStaredAt(player, centre, LOOK_DOT, 48.0D)) {
            ghost.resyncTick = now + MISSING_RESYNC_AFTER_LOOK_TICKS;
        }
        return false;
    }

    private static void resync(ServerPlayer player, Ghost ghost) {
        player.connection.send(new ClientboundBlockUpdatePacket(ghost.pos, player.serverLevel().getBlockState(ghost.pos)));
        debug("{} ghost resolved for {} at {}", ghost.kind, player.getGameProfile().getName(), ghost.pos);
    }

    /** Hitting or using the block the client believes in ends the illusion at once. */
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        resolveAt(event.getEntity() instanceof ServerPlayer player ? player : null, event.getPos());
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            resolveAt(player, event.getPos());
            resolveAt(player, event.getPos().relative(event.getFace() == null ? Direction.UP : event.getFace()));
        }
    }

    private static void resolveAt(ServerPlayer player, BlockPos pos) {
        if (player == null) {
            return;
        }
        Iterator<Ghost> iterator = GHOSTS.iterator();
        while (iterator.hasNext()) {
            Ghost ghost = iterator.next();
            if (ghost.owner.equals(player.getUUID()) && ghost.pos.equals(pos)) {
                resync(player, ghost);
                iterator.remove();
            }
        }
    }

    public static void clearForOwner(MinecraftServer server, UUID owner) {
        RECENTLY_MINED.remove(owner);
        Iterator<Ghost> iterator = GHOSTS.iterator();
        while (iterator.hasNext()) {
            Ghost ghost = iterator.next();
            if (!ghost.owner.equals(owner)) {
                continue;
            }
            ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(owner);
            if (player != null && player.serverLevel().dimension().equals(ghost.dimension)) {
                resync(player, ghost);
            }
            iterator.remove();
        }
    }

    public static void clear(MinecraftServer server) {
        for (UUID owner : GHOSTS.stream().map(ghost -> ghost.owner).distinct().toList()) {
            clearForOwner(server, owner);
        }
        GHOSTS.clear();
        RECENTLY_MINED.clear();
    }

    /** For GameTests: number of ghosts currently shown to this player. */
    public static int ghostCount(UUID owner) {
        return (int) GHOSTS.stream().filter(ghost -> ghost.owner.equals(owner)).count();
    }

    private static void debug(String message, Object... args) {
        if (UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info("[UncannyDebug/GhostBlock] " + message, args);
        }
    }
}
