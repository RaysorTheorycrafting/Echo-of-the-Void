package com.eotv.echoofthevoid.event.special;

import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import com.eotv.echoofthevoid.diagnostics.DiagnosticSeverity;
import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyArenaPursuerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyDevourerEntity;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import com.eotv.echoofthevoid.world.UncannyDimensions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Authoritative lifecycle, persistence and world-safety boundary for Elsewhere. */
public final class DevourerArenaSystem {
    private static final Set<Block> FORBIDDEN_PLACEMENTS = Set.of(
            Blocks.TNT,
            Blocks.FIRE,
            Blocks.SOUL_FIRE,
            Blocks.NETHER_PORTAL,
            Blocks.END_PORTAL,
            Blocks.END_GATEWAY,
            Blocks.PISTON,
            Blocks.STICKY_PISTON,
            Blocks.PISTON_HEAD,
            Blocks.MOVING_PISTON,
            Blocks.REDSTONE_WIRE,
            Blocks.REPEATER,
            Blocks.COMPARATOR,
            Blocks.OBSERVER,
            Blocks.DISPENSER,
            Blocks.DROPPER,
            Blocks.HOPPER);

    private DevourerArenaSystem() {
    }

    public static boolean capturePlayer(UncannyDevourerEntity source, ServerPlayer player) {
        return beginSession(source, player, "devourer_contact");
    }

    public static boolean enterDebugArena(ServerPlayer player) {
        return beginSession(null, player, "developer_direct");
    }

    public static boolean hasSession(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        MinecraftServer server = currentServerFor(playerId);
        return server != null && UncannyWorldState.get(server).getDevourerArenaSession(playerId) != null;
    }

    public static boolean hasSession(ServerLevel level, UUID playerId) {
        return level != null
                && playerId != null
                && UncannyWorldState.get(level.getServer()).getDevourerArenaSession(playerId) != null;
    }

    public static int activeSessionCount(MinecraftServer server) {
        return server == null ? 0 : UncannyWorldState.get(server).getDevourerArenaSessions().size();
    }

    public static boolean isActiveSession(UUID playerId, long cellIndex) {
        MinecraftServer server = currentServerFor(playerId);
        if (server == null) {
            return false;
        }
        DevourerArenaSession session = UncannyWorldState.get(server).getDevourerArenaSession(playerId);
        return session != null
                && session.cellIndex() == cellIndex
                && session.status() == DevourerArenaSession.Status.ACTIVE;
    }

    public static boolean isActiveSession(ServerLevel level, UUID playerId, long cellIndex) {
        if (level == null || playerId == null) {
            return false;
        }
        DevourerArenaSession session = UncannyWorldState.get(level.getServer())
                .getDevourerArenaSession(playerId);
        return session != null
                && session.cellIndex() == cellIndex
                && session.status() == DevourerArenaSession.Status.ACTIVE;
    }

    private static MinecraftServer currentServerFor(UUID playerId) {
        // Runtime callers always originate from an attached player or arena entity. The explicit
        // level overloads below are preferred for block mutations; this lookup only supports the
        // entity's read-only session guard.
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }
        UncannyWorldState state = UncannyWorldState.get(server);
        List<DevourerArenaSession> sessions = state.getDevourerArenaSessions();
        if (sessions.isEmpty()) {
            return;
        }
        long wallClock = System.currentTimeMillis();
        for (DevourerArenaSession session : sessions) {
            ServerPlayer player = server.getPlayerList().getPlayer(session.playerId());
            if (player == null) {
                continue;
            }
            if (DevourerArenaRules.offlineGraceExpired(session.lastOnlineEpochMillis(), wallClock)) {
                if (!player.isAlive()
                        || session.status() == DevourerArenaSession.Status.AWAITING_RESPAWN
                        || session.status() == DevourerArenaSession.Status.RETURN_PENDING) {
                    session.setStatus(DevourerArenaSession.Status.RETURN_PENDING);
                    state.markDevourerArenaSessionsDirty();
                    continue;
                }
                finishSession(server, state, session, player, FinishReason.OFFLINE_TIMEOUT);
                continue;
            }
            session.markOnline(wallClock);
            if (session.status() == DevourerArenaSession.Status.RETURN_PENDING) {
                if (player.isAlive()) {
                    finishSession(server, state, session, player, FinishReason.DIED_AND_RESPAWNED);
                } else {
                    state.markDevourerArenaSessionsDirty();
                }
                continue;
            }
            if (session.status() == DevourerArenaSession.Status.AWAITING_RESPAWN) {
                if (player.isAlive()) {
                    finishSession(server, state, session, player, FinishReason.DIED_AND_RESPAWNED);
                } else {
                    state.markDevourerArenaSessionsDirty();
                }
                continue;
            }
            if (session.status() != DevourerArenaSession.Status.ACTIVE || !player.isAlive()) {
                state.markDevourerArenaSessionsDirty();
                continue;
            }
            if (!UncannyDimensions.isElsewhere(player.level())) {
                finishSession(server, state, session, player, FinishReason.EXTERNAL_ESCAPE);
                continue;
            }
            // A cross-dimension player ticket needs a few server turns before the surrounding
            // chunks can actually tick entities. Do not consume the player's sixty seconds while
            // the initial pursuers are merely scheduled or waiting in a non-ticking chunk.
            boolean initialPopulationReady = ensurePursuerWaves(player, session);
            if (initialPopulationReady) {
                session.tickRemaining();
            }
            state.markDevourerArenaSessionsDirty();
            if (session.remainingTicks() <= 0L) {
                finishSession(server, state, session, player, FinishReason.SURVIVED);
            }
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.getServer() == null
                || !UncannyDimensions.isElsewhere(player.level())) {
            return;
        }
        UncannyWorldState state = UncannyWorldState.get(player.getServer());
        DevourerArenaSession session = state.getDevourerArenaSession(player.getUUID());
        if (session == null || session.status() != DevourerArenaSession.Status.ACTIVE) {
            return;
        }
        session.setStatus(DevourerArenaSession.Status.AWAITING_RESPAWN);
        state.markDevourerArenaSessionsDirty();
        UncannyDiagnostics.recordForPlayer(
                player,
                DiagnosticSeverity.INFO,
                "arena",
                "devourer_trial_failed",
                UncannyDiagnostics.fields(
                        "remaining_ticks", session.remainingTicks(),
                        "keep_inventory", player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)));
    }

    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof UncannyArenaPursuerEntity) {
            event.getDrops().clear();
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player
                && UncannyDimensions.isElsewhere(player.level())
                && hasSession(player.getUUID())) {
            event.getDrops().clear();
        }
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        UncannyWorldState state = UncannyWorldState.get(player.getServer());
        DevourerArenaSession session = state.getDevourerArenaSession(player.getUUID());
        if (session == null
                || (session.status() != DevourerArenaSession.Status.AWAITING_RESPAWN
                        && session.status() != DevourerArenaSession.Status.RETURN_PENDING)) {
            return;
        }
        finishSession(player.getServer(), state, session, player, FinishReason.DIED_AND_RESPAWNED);
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        DevourerArenaSession session = UncannyWorldState.get(player.getServer())
                .getDevourerArenaSession(player.getUUID());
        if (session != null
                && (session.status() == DevourerArenaSession.Status.AWAITING_RESPAWN
                        || session.status() == DevourerArenaSession.Status.RETURN_PENDING)) {
            if (player.isAlive()) {
                finishSession(player.getServer(), UncannyWorldState.get(player.getServer()), session, player,
                        FinishReason.DIED_AND_RESPAWNED);
            }
        } else if (session != null && session.status() == DevourerArenaSession.Status.ACTIVE) {
            UncannyDiagnostics.recordForPlayer(
                    player,
                    DiagnosticSeverity.INFO,
                    "arena",
                    "devourer_trial_reconnected",
                    UncannyDiagnostics.fields("remaining_ticks", session.remainingTicks(), "cell", session.cellIndex()));
        }
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !UncannyDimensions.isElsewhere(player.level())
                || !event.getState().is(UncannyBlockRegistry.UNCANNY_BLOCK.get())
                || !hasSession(player.getUUID())) {
            return;
        }
        event.setNewSpeed(Math.max(event.getNewSpeed(), 75.0F));
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !UncannyDimensions.isElsewhere(level)
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        DevourerArenaSession session = state.getDevourerArenaSession(player.getUUID());
        if (session == null || session.status() != DevourerArenaSession.Status.ACTIVE) {
            event.setCanceled(true);
            return;
        }
        BlockState placed = event.getPlacedBlock();
        if (event instanceof BlockEvent.EntityMultiPlaceEvent
                || !isAllowedPlacement(level, event.getPos(), placed)) {
            event.setCanceled(true);
            UncannyDiagnostics.recordForPlayer(
                    player,
                    DiagnosticSeverity.INFO,
                    "arena",
                    "arena_placement_refused",
                    UncannyDiagnostics.fields("block", placed.getBlock(), "position", event.getPos().toShortString()));
            return;
        }
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(placed.getBlock());
        session.rememberPlacedBlock(event.getPos().asLong(), blockId.toString());
        state.markDevourerArenaSessionsDirty();
    }

    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !UncannyDimensions.isElsewhere(level)
                || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        DevourerArenaSession session = state.getDevourerArenaSession(player.getUUID());
        if (session == null || session.status() != DevourerArenaSession.Status.ACTIVE) {
            event.setCanceled(true);
            return;
        }
        long packed = event.getPos().asLong();
        if (session.containsPlacement(packed)) {
            session.forgetPlayerBrokenPlacement(packed);
            state.markDevourerArenaSessionsDirty();
            return;
        }
        if (event.getState().is(UncannyBlockRegistry.UNCANNY_BLOCK.get())) {
            session.rememberBrokenArenaBlock(packed);
            state.markDevourerArenaSessionsDirty();
            return;
        }
        event.setCanceled(true);
    }

    public static void onBlockDrops(BlockDropsEvent event) {
        if (UncannyDimensions.isElsewhere(event.getLevel())
                && event.getState().is(UncannyBlockRegistry.UNCANNY_BLOCK.get())) {
            event.setCanceled(true);
        }
    }

    public static void onFluidPlaced(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level && UncannyDimensions.isElsewhere(level)) {
            event.setCanceled(true);
        }
    }

    public static void onPortalSpawn(BlockEvent.PortalSpawnEvent event) {
        if (event.getLevel() instanceof ServerLevel level && UncannyDimensions.isElsewhere(level)) {
            event.setCanceled(true);
        }
    }

    public static void onRestrictedArenaInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer)
                || !UncannyDimensions.isElsewhere(event.getLevel())) {
            return;
        }
        var item = event.getItemStack().getItem();
        if (item instanceof BucketItem
                || item instanceof SolidBucketItem
                || item instanceof FlintAndSteelItem
                || item instanceof FireChargeItem) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static boolean isTrackedPlacement(
            ServerLevel level, UUID playerId, long cellIndex, BlockPos pos) {
        if (level == null || !UncannyDimensions.isElsewhere(level)) {
            return false;
        }
        DevourerArenaSession session = UncannyWorldState.get(level.getServer())
                .getDevourerArenaSession(playerId);
        return session != null
                && session.cellIndex() == cellIndex
                && session.status() == DevourerArenaSession.Status.ACTIVE
                && session.containsPlacement(pos.asLong());
    }

    public static boolean removeTrackedPlacement(
            UUID playerId,
            long cellIndex,
            BlockPos pos,
            UncannyArenaPursuerEntity pursuer) {
        if (!(pursuer.level() instanceof ServerLevel level)
                || !isTrackedPlacement(level, playerId, cellIndex, pos)) {
            return false;
        }
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        DevourerArenaSession session = state.getDevourerArenaSession(playerId);
        DevourerArenaSession.PlacedBlock tracked = session.placedBlocks().get(pos.asLong());
        if (tracked == null || level.getBlockState(pos).isAir()) {
            return false;
        }
        ResourceLocation currentId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        if (!tracked.blockId().equals(currentId.toString())) {
            return false;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        session.markPlacementRemovedByPursuer(pos.asLong());
        state.markDevourerArenaSessionsDirty();
        level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.HOSTILE, 1.10F, 0.62F);
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        UncannyDiagnostics.recordForPlayer(
                player,
                DiagnosticSeverity.INFO,
                "arena",
                "pursuer_removed_placement",
                UncannyDiagnostics.fields("position", pos.toShortString(), "block", tracked.blockId()));
        return true;
    }

    public static boolean abandonSession(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return false;
        }
        UncannyWorldState state = UncannyWorldState.get(player.getServer());
        DevourerArenaSession session = state.getDevourerArenaSession(player.getUUID());
        if (session == null) {
            return false;
        }
        if (session.status() == DevourerArenaSession.Status.AWAITING_RESPAWN) {
            session.setStatus(DevourerArenaSession.Status.RETURN_PENDING);
            state.markDevourerArenaSessionsDirty();
            return true;
        }
        finishSession(player.getServer(), state, session, player, FinishReason.ADMIN_ABORT);
        return true;
    }

    private static boolean beginSession(Entity source, ServerPlayer player, String route) {
        MinecraftServer server = player == null ? null : player.getServer();
        if (server == null || !player.isAlive() || player.isSpectator()) {
            return false;
        }
        UncannyWorldState state = UncannyWorldState.get(server);
        if (state.getDevourerArenaSession(player.getUUID()) != null
                || state.getDevourerArenaSessions().size() >= DevourerArenaRules.MAX_SIMULTANEOUS_SESSIONS) {
            return false;
        }
        ServerLevel elsewhere = server.getLevel(UncannyDimensions.ELSEWHERE);
        if (elsewhere == null) {
            UncannyDiagnostics.recordForPlayer(
                    player,
                    DiagnosticSeverity.ERROR,
                    "arena",
                    "elsewhere_dimension_missing",
                    UncannyDiagnostics.fields("route", route));
            return false;
        }
        long cell = state.allocateDevourerArenaCell();
        DevourerArenaSession session = new DevourerArenaSession(
                player.getUUID(),
                source == null ? null : source.getUUID(),
                player.level().dimension().location().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                cell,
                snapshotInventory(player),
                System.currentTimeMillis());
        if (!state.addDevourerArenaSession(session)) {
            return false;
        }
        int centerX = DevourerArenaRules.cellCenterX(cell);
        prepareArenaSpawnArea(elsewhere, centerX);
        player.teleportTo(
                elsewhere,
                centerX + 0.5D,
                DevourerArenaRules.ARENA_SURFACE_Y,
                0.5D,
                player.getYRot(),
                player.getXRot());
        // Use Minecraft's own short-lived portal ticket for the destination. Loading chunks with
        // getChunk() only makes their blocks available; it does not make newly inserted mobs tick.
        // The ticket covers the 18-26 block spawn ring and expires by Vanilla policy.
        player.placePortalTicket(player.blockPosition());
        ensurePursuerWaves(player, session);
        UncannyDiagnostics.recordForPlayer(
                player,
                DiagnosticSeverity.INFO,
                "arena",
                "devourer_trial_started",
                UncannyDiagnostics.fields("cell", cell, "route", route, "remaining_ticks", session.remainingTicks()));
        return true;
    }

    /**
     * The cross-dimension player ticket is installed as part of teleportation, one entity-manager
     * turn too late for the initial wave created below. Generate/load only the small, fixed ring
     * used by the arena before moving the player so the trial never begins empty. The player's
     * ordinary level ticket then keeps the same chunks available for the encounter.
     */
    private static void prepareArenaSpawnArea(ServerLevel level, int centerX) {
        int margin = (int) Math.ceil(DevourerArenaRules.MAX_SPAWN_DISTANCE) + 2;
        int minimumChunkX = Math.floorDiv(centerX - margin, 16);
        int maximumChunkX = Math.floorDiv(centerX + margin, 16);
        int minimumChunkZ = Math.floorDiv(-margin, 16);
        int maximumChunkZ = Math.floorDiv(margin, 16);
        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }
    }

    private static boolean ensurePursuerWaves(ServerPlayer player, DevourerArenaSession session) {
        if (!(player.level() instanceof ServerLevel level) || !UncannyDimensions.isElsewhere(level)) {
            return false;
        }
        int expected = DevourerArenaRules.expectedPursuerCount(session.elapsedTicks());
        List<UncannyArenaPursuerEntity> live = arenaPursuers(level, session);
        if (live.isEmpty()
                && session.spawnedPursuers() > 0
                && session.elapsedTicks() <= DevourerArenaRules.PURSUER_EMERGENCE_TICKS + 10L
                && level.isPositionEntityTicking(player.blockPosition())) {
            // Pursuers are invulnerable throughout this window, so an empty initial group cannot
            // be a legitimate player kill. Recover counters written by the former global-server
            // session lookup, which could discard every entity on an integrated server while
            // leaving SpawnedPursuers at eight.
            UncannyDiagnostics.recordForPlayer(
                    player,
                    DiagnosticSeverity.ERROR,
                    "arena",
                    "initial_population_missing_recovered",
                    UncannyDiagnostics.fields(
                            "cell", session.cellIndex(),
                            "persisted_spawned", session.spawnedPursuers(),
                            "elapsed_ticks", session.elapsedTicks()));
            session.setSpawnedPursuers(0);
            session.setLastKnownAlivePursuers(0);
            session.clearLossDetected();
        }
        int spawnedThisTick = 0;
        int scheduledMissing = Math.max(0, Math.min(
                DevourerArenaRules.MAX_PURSUERS,
                expected - session.spawnedPursuers()));
        int missing = DevourerArenaRules.spawnAllowance(scheduledMissing, spawnedThisTick);
        for (int attempt = 0; attempt < missing; attempt++) {
            int index = session.spawnedPursuers();
            int[] coverage = sectorCoverage(player, live);
            int sector = chooseSpawnSector(player, coverage);
            UncannyArenaPursuerEntity spawned = spawnPursuer(level, player, session, index, sector, live);
            if (spawned == null) {
                if (session.spawnedPursuers() == 0
                        && session.elapsedTicks() % DevourerArenaRules.COVERAGE_REEVALUATION_TICKS == 0L) {
                    UncannyDiagnostics.recordForPlayer(
                            player,
                            DiagnosticSeverity.WARNING,
                            "arena",
                            "initial_pursuer_spawn_unavailable",
                            UncannyDiagnostics.fields(
                                    "cell", session.cellIndex(),
                                    "elapsed_ticks", session.elapsedTicks(),
                                    "requested_sector", sector,
                                    "player_block", player.blockPosition().toShortString()));
                }
                break;
            }
            session.setSpawnedPursuers(index + 1);
            live.add(spawned);
            spawnedThisTick++;
        }

        int alive = live.size();
        if (session.spawnedPursuers() < DevourerArenaRules.INITIAL_PURSUERS
                || alive < DevourerArenaRules.INITIAL_PURSUERS) {
            session.setLastKnownAlivePursuers(alive);
            if (player.tickCount % DevourerArenaRules.COVERAGE_REEVALUATION_TICKS == 0) {
                UncannyDiagnostics.recordForPlayer(
                        player,
                        DiagnosticSeverity.INFO,
                        "arena",
                        "initial_population_waiting",
                        UncannyDiagnostics.fields(
                                "cell", session.cellIndex(),
                                "persisted_spawned", session.spawnedPursuers(),
                                "physically_alive", alive,
                                "player_chunk_entity_ticking",
                                level.isPositionEntityTicking(player.blockPosition())));
            }
            return false;
        }
        int targetPopulation = Math.min(expected, session.spawnedPursuers());
        if (alive < session.lastKnownAlivePursuers()) {
            session.markLossDetected(session.elapsedTicks());
        }
        if (alive >= targetPopulation) {
            session.clearLossDetected();
        } else if (session.lossDetectedElapsedTick() < 0L) {
            // Legacy sessions do not persist an old death timestamp. They receive the full
            // breathing window instead of an immediate reconnect burst.
            session.markLossDetected(session.elapsedTicks());
        }

        if (alive < targetPopulation
                && DevourerArenaRules.replacementReady(
                        session.elapsedTicks(),
                        session.lossDetectedElapsedTick(),
                        session.replacementsUsed())) {
            int replacementMissing = DevourerArenaRules.spawnAllowance(
                    targetPopulation - alive,
                    spawnedThisTick);
            for (int attempt = 0; attempt < replacementMissing; attempt++) {
                int[] coverage = sectorCoverage(player, live);
                int sector = chooseSpawnSector(player, coverage);
                int visualIndex = session.spawnedPursuers() + session.replacementsUsed();
                UncannyArenaPursuerEntity replacement = spawnPursuer(
                        level, player, session, visualIndex, sector, live);
                if (replacement == null) {
                    break;
                }
                live.add(replacement);
                session.recordReplacement();
                spawnedThisTick++;
                alive++;
            }
            session.clearLossDetected();
            if (alive < targetPopulation) {
                session.markLossDetected(session.elapsedTicks());
            }
        }

        topUpClimbers(level, player, session, live, spawnedThisTick);

        if (session.elapsedTicks() >= session.nextCoverageElapsedTick()) {
            rebalanceClusteredPopulation(level, player, session, live, spawnedThisTick);
            session.scheduleNextCoverage(session.elapsedTicks());
        }
        session.setLastKnownAlivePursuers(live.size());
        return true;
    }

    /**
     * Keeps at least two Spider silhouettes alive for the whole trial, outside the replacement
     * budget: killing them only buys the ordinary breathing window, never a safe pillar.
     */
    private static void topUpClimbers(
            ServerLevel level,
            ServerPlayer player,
            DevourerArenaSession session,
            List<UncannyArenaPursuerEntity> live,
            int spawnedThisTick) {
        long climbers = live.stream().filter(p -> p.isAlive() && p.appearance().climbs()).count();
        if (climbers >= DevourerArenaRules.MIN_CLIMBING_PURSUERS) {
            session.clearClimberLoss();
            return;
        }
        if (session.climberLossElapsedTick() < 0L) {
            session.markClimberLoss(session.elapsedTicks());
            return;
        }
        if (session.elapsedTicks() - session.climberLossElapsedTick() < DevourerArenaRules.REPLACEMENT_RESPITE_TICKS
                || DevourerArenaRules.spawnAllowance(1, spawnedThisTick) <= 0) {
            return;
        }
        int[] coverage = sectorCoverage(player, live);
        int sector = chooseSpawnSector(player, coverage);
        int visualIndex = session.spawnedPursuers() + session.replacementsUsed();
        UncannyArenaPursuerEntity climber = spawnPursuer(level, player, session, visualIndex, sector, live);
        if (climber != null) {
            live.add(climber);
            session.clearClimberLoss();
            UncannyDiagnostics.recordForPlayer(
                    player,
                    DiagnosticSeverity.INFO,
                    "arena",
                    "climber_topped_up",
                    UncannyDiagnostics.fields("climbers_before", climbers, "cell", session.cellIndex()));
        }
    }

    private static UncannyArenaPursuerEntity spawnPursuer(
            ServerLevel level,
            ServerPlayer player,
            DevourerArenaSession session,
            int index,
            int sector,
            List<UncannyArenaPursuerEntity> existing) {
        UncannyArenaPursuerEntity pursuer = UncannyEntityRegistry.UNCANNY_ARENA_PURSUER.get().create(level);
        if (pursuer == null) {
            return null;
        }
        // The silhouette decides the hitbox, so it must be chosen before the collision probe. Two
        // climbers always come first (they answer a pillar), then the walking forms cycle so every
        // fresh arena shows a varied group; later waves continue the same sequence.
        int aliveClimbers = (int) existing.stream().filter(p -> p.isAlive() && p.appearance().climbs()).count();
        pursuer.setAppearance(DevourerArenaRules.appearanceFor(index, aliveClimbers));
        BlockPos spawn = findPursuerSpawn(level, player, pursuer, sector, existing);
        if (spawn == null) {
            return null;
        }
        double angle = Math.atan2(player.getZ() - (spawn.getZ() + 0.5D), player.getX() - (spawn.getX() + 0.5D));
        pursuer.moveTo(
                spawn.getX() + 0.5D,
                DevourerArenaRules.ARENA_SURFACE_Y,
                spawn.getZ() + 0.5D,
                (float) Math.toDegrees(angle),
                0.0F);
        pursuer.initializeFor(player, session.cellIndex());
        pursuer.beginEmergence(DevourerArenaRules.ARENA_SURFACE_Y);
        boolean added = level.addFreshEntity(pursuer);
        UncannyDiagnostics.recordSpecialLifecycle(
                player,
                pursuer,
                "arena_pursuer",
                added ? "spawned" : "spawn_failed",
                "arena_wave",
                added ? DiagnosticSeverity.INFO : DiagnosticSeverity.WARNING,
                UncannyDiagnostics.fields(
                        "wave_index", index,
                        "cell", session.cellIndex(),
                        "sector", sector,
                        "emergence_ticks", DevourerArenaRules.PURSUER_EMERGENCE_TICKS,
                        "appearance", pursuer.appearance().name().toLowerCase(java.util.Locale.ROOT)));
        return added ? pursuer : null;
    }

    private static List<UncannyArenaPursuerEntity> arenaPursuers(
            ServerLevel level,
            DevourerArenaSession session) {
        double centerX = DevourerArenaRules.cellCenterX(session.cellIndex()) + 0.5D;
        AABB bounds = new AABB(centerX - 128.0D, -64.0D, -127.5D, centerX + 128.0D, 128.0D, 128.5D);
        return new ArrayList<>(level.getEntitiesOfClass(
                UncannyArenaPursuerEntity.class,
                bounds,
                candidate -> candidate.isAlive() && candidate.cellIndex() == session.cellIndex()));
    }

    private static int[] sectorCoverage(
            ServerPlayer player,
            List<UncannyArenaPursuerEntity> pursuers) {
        int[] coverage = new int[DevourerArenaRules.SECTOR_COUNT];
        for (UncannyArenaPursuerEntity pursuer : pursuers) {
            int sector = DevourerArenaRules.sectorForDirection(
                    pursuer.getX() - player.getX(),
                    pursuer.getZ() - player.getZ());
            coverage[sector]++;
        }
        return coverage;
    }

    private static int chooseSpawnSector(ServerPlayer player, int[] coverage) {
        Vec3 motion = player.getDeltaMovement();
        Vec3 forward = motion.horizontalDistanceSqr() >= 0.01D
                ? new Vec3(motion.x, 0.0D, motion.z)
                : player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        int forwardSector = DevourerArenaRules.sectorForDirection(forward.x, forward.z);
        return DevourerArenaRules.chooseSpawnSector(
                coverage,
                forwardSector,
                player.getRandom().nextDouble(),
                player.getRandom().nextInt(DevourerArenaRules.SECTOR_COUNT));
    }

    private static BlockPos findPursuerSpawn(
            ServerLevel level,
            ServerPlayer player,
            UncannyArenaPursuerEntity pursuer,
            int sector,
            List<UncannyArenaPursuerEntity> existing) {
        double sectorWidth = Math.PI * 2.0D / DevourerArenaRules.SECTOR_COUNT;
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = sector * sectorWidth
                    + sectorWidth * (0.18D + player.getRandom().nextDouble() * 0.64D);
            double distance = DevourerArenaRules.MIN_SPAWN_DISTANCE
                    + player.getRandom().nextDouble()
                    * (DevourerArenaRules.MAX_SPAWN_DISTANCE - DevourerArenaRules.MIN_SPAWN_DISTANCE);
            BlockPos candidate = BlockPos.containing(
                    player.getX() + Math.cos(angle) * distance,
                    DevourerArenaRules.ARENA_SURFACE_Y,
                    player.getZ() + Math.sin(angle) * distance);
            if (canSpawnPursuerAt(level, pursuer, candidate, existing)) {
                return candidate.immutable();
            }
        }

        // Random probes are useful for varied encounters but cannot be the sole correctness
        // path. The arena is a deterministic flat plane, so scan stable points inside the chosen
        // sector before reporting a failure.
        double centerAngle = (sector + 0.5D) * sectorWidth;
        for (int radius = (int) DevourerArenaRules.MIN_SPAWN_DISTANCE;
                radius <= (int) DevourerArenaRules.MAX_SPAWN_DISTANCE;
                radius += 2) {
            for (int offsetIndex = 0; offsetIndex < 7; offsetIndex++) {
                int signed = offsetIndex == 0
                        ? 0
                        : ((offsetIndex + 1) / 2) * (offsetIndex % 2 == 0 ? -1 : 1);
                double angle = centerAngle + signed * sectorWidth * 0.09D;
                BlockPos candidate = BlockPos.containing(
                        player.getX() + Math.cos(angle) * radius,
                        DevourerArenaRules.ARENA_SURFACE_Y,
                        player.getZ() + Math.sin(angle) * radius);
                if (canSpawnPursuerAt(level, pursuer, candidate, existing)) {
                    return candidate.immutable();
                }
            }
        }
        return null;
    }

    private static boolean canSpawnPursuerAt(
            ServerLevel level,
            UncannyArenaPursuerEntity pursuer,
            BlockPos candidate,
            List<UncannyArenaPursuerEntity> existing) {
        if (!level.hasChunkAt(candidate)
                || !level.isPositionEntityTicking(candidate)
                || !level.getBlockState(candidate).isAir()
                || !level.getBlockState(candidate.above()).isAir()
                || !level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), Direction.UP)) {
            return false;
        }
        Vec3 center = Vec3.atBottomCenterOf(candidate);
        for (UncannyArenaPursuerEntity other : existing) {
            if (other.position().distanceToSqr(center)
                    < DevourerArenaRules.MIN_PURSUER_SEPARATION
                    * DevourerArenaRules.MIN_PURSUER_SEPARATION) {
                return false;
            }
        }
        pursuer.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
        if (!level.noCollision(pursuer, pursuer.getBoundingBox())) {
            return false;
        }
        // PathNavigation is not valid as a pre-insertion probe: on a normal Elsewhere level it
        // returns no path until the Spider has entered the entity manager. The arena floor is
        // deliberately uniform; loaded chunk, free volume, support, collision and separation are
        // the complete pre-insertion contract.
        return true;
    }

    private static void rebalanceClusteredPopulation(
            ServerLevel level,
            ServerPlayer player,
            DevourerArenaSession session,
            List<UncannyArenaPursuerEntity> live,
            int spawnedThisTick) {
        int[] coverage = sectorCoverage(player, live);
        UncannyDiagnostics.recordForPlayer(
                player,
                DiagnosticSeverity.INFO,
                "arena",
                "arena_sector_coverage",
                UncannyDiagnostics.fields(
                        "cell", session.cellIndex(),
                        "coverage", Arrays.toString(coverage),
                        "alive", live.size(),
                        "replacements_used", session.replacementsUsed()));
        if (live.size() != DevourerArenaRules.MAX_PURSUERS
                || spawnedThisTick >= DevourerArenaRules.MAX_SPAWNS_PER_TICK
                || session.replacementsUsed() >= DevourerArenaRules.MAX_REPLACEMENTS) {
            return;
        }
        boolean hasEmptySector = Arrays.stream(coverage).anyMatch(count -> count == 0);
        if (!hasEmptySector) {
            return;
        }
        UncannyArenaPursuerEntity retired = null;
        double retiredDistance = DevourerArenaRules.REBALANCE_DISTANCE
                * DevourerArenaRules.REBALANCE_DISTANCE;
        for (UncannyArenaPursuerEntity candidate : live) {
            int sector = DevourerArenaRules.sectorForDirection(
                    candidate.getX() - player.getX(),
                    candidate.getZ() - player.getZ());
            double distance = candidate.distanceToSqr(player);
            if (coverage[sector] > 1
                    && distance > retiredDistance
                    && !player.hasLineOfSight(candidate)) {
                retired = candidate;
                retiredDistance = distance;
            }
        }
        if (retired == null) {
            return;
        }
        int retiredSector = DevourerArenaRules.sectorForDirection(
                retired.getX() - player.getX(),
                retired.getZ() - player.getZ());
        retired.discard();
        live.remove(retired);
        coverage[retiredSector]--;
        int replacementSector = chooseSpawnSector(player, coverage);
        int visualIndex = session.spawnedPursuers() + session.replacementsUsed();
        UncannyArenaPursuerEntity replacement = spawnPursuer(
                level, player, session, visualIndex, replacementSector, live);
        if (replacement != null) {
            live.add(replacement);
            session.recordReplacement();
            UncannyDiagnostics.recordForPlayer(
                    player,
                    DiagnosticSeverity.INFO,
                    "arena",
                    "arena_sector_rebalanced",
                    UncannyDiagnostics.fields(
                            "retired_sector", retiredSector,
                            "replacement_sector", replacementSector,
                            "replacements_used", session.replacementsUsed()));
        }
    }

    private static void finishSession(
            MinecraftServer server,
            UncannyWorldState state,
            DevourerArenaSession session,
            ServerPlayer player,
            FinishReason reason) {
        boolean restoreEntryInventory = reason == FinishReason.DIED_AND_RESPAWNED;
        List<ItemStack> restitution = cleanupArena(server, session, !restoreEntryInventory);
        if (restoreEntryInventory) {
            restoreInventory(player, session.entryInventory());
        }
        ServerLevel origin = resolveOriginLevel(server, session.originDimension());
        Vec3 safe = findSafeReturn(origin, player, new Vec3(session.originX(), session.originY(), session.originZ()));
        player.teleportTo(origin, safe.x, safe.y, safe.z, session.originYaw(), session.originPitch());
        for (ItemStack stack : restitution) {
            if (!player.getInventory().add(stack.copy())) {
                player.drop(stack.copy(), false);
            }
        }
        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                DevourerArenaRules.RETURN_PROTECTION_TICKS,
                4,
                false,
                false,
                true));
        state.removeDevourerArenaSession(session.playerId());
        UncannyDiagnostics.recordForPlayer(
                player,
                DiagnosticSeverity.INFO,
                "arena",
                "devourer_trial_finished",
                UncannyDiagnostics.fields(
                        "reason", reason.name().toLowerCase(java.util.Locale.ROOT),
                        "return_dimension", origin.dimension().location(),
                        "restituted_blocks", restitution.size()));
    }

    private static List<ItemStack> cleanupArena(
            MinecraftServer server,
            DevourerArenaSession session,
            boolean restitutePlacedBlocks) {
        ServerLevel level = server.getLevel(UncannyDimensions.ELSEWHERE);
        if (level == null) {
            return List.of();
        }
        List<ItemStack> restitution = new ArrayList<>();
        for (Map.Entry<Long, DevourerArenaSession.PlacedBlock> entry : session.placedBlocks().entrySet()) {
            BlockPos pos = BlockPos.of(entry.getKey());
            DevourerArenaSession.PlacedBlock placed = entry.getValue();
            ResourceLocation id = ResourceLocation.tryParse(placed.blockId());
            Block expected = id == null ? Blocks.AIR : BuiltInRegistries.BLOCK.get(id);
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            if (restitutePlacedBlocks && expected != Blocks.AIR && expected.asItem() != net.minecraft.world.item.Items.AIR) {
                restitution.add(new ItemStack(expected));
            }
        }
        for (long packed : session.brokenArenaBlocks()) {
            BlockPos pos = BlockPos.of(packed);
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, UncannyBlockRegistry.UNCANNY_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        int centerX = DevourerArenaRules.cellCenterX(session.cellIndex());
        AABB cleanupBounds = new AABB(
                centerX - 192.0D, 0.0D, -192.0D,
                centerX + 192.0D, 64.0D, 192.0D);
        for (UncannyArenaPursuerEntity pursuer : level.getEntitiesOfClass(
                UncannyArenaPursuerEntity.class,
                cleanupBounds,
                candidate -> candidate.cellIndex() == session.cellIndex())) {
            pursuer.discard();
        }
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, cleanupBounds)) {
            item.discard();
        }
        return restitution;
    }

    private static CompoundTag snapshotInventory(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        CompoundTag snapshot = new CompoundTag();
        CompoundTag main = new CompoundTag();
        CompoundTag armor = new CompoundTag();
        CompoundTag offhand = new CompoundTag();
        ContainerHelper.saveAllItems(main, inventory.items, player.registryAccess());
        ContainerHelper.saveAllItems(armor, inventory.armor, player.registryAccess());
        ContainerHelper.saveAllItems(offhand, inventory.offhand, player.registryAccess());
        snapshot.put("Main", main);
        snapshot.put("Armor", armor);
        snapshot.put("Offhand", offhand);
        snapshot.putInt("Selected", inventory.selected);
        return snapshot;
    }

    private static void restoreInventory(ServerPlayer player, CompoundTag snapshot) {
        Inventory inventory = player.getInventory();
        inventory.items.clear();
        inventory.armor.clear();
        inventory.offhand.clear();
        ContainerHelper.loadAllItems(snapshot.getCompound("Main"), inventory.items, player.registryAccess());
        ContainerHelper.loadAllItems(snapshot.getCompound("Armor"), inventory.armor, player.registryAccess());
        ContainerHelper.loadAllItems(snapshot.getCompound("Offhand"), inventory.offhand, player.registryAccess());
        inventory.selected = Math.max(0, Math.min(8, snapshot.getInt("Selected")));
        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    private static ServerLevel resolveOriginLevel(MinecraftServer server, String dimensionId) {
        ResourceLocation id = ResourceLocation.tryParse(dimensionId);
        if (id != null) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            if (level != null && !UncannyDimensions.isElsewhere(level)) {
                return level;
            }
        }
        return server.overworld();
    }

    private static Vec3 findSafeReturn(ServerLevel level, ServerPlayer player, Vec3 requested) {
        BlockPos base = BlockPos.containing(requested);
        for (int radius = 0; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dy = 2; dy >= -3; dy--) {
                        BlockPos feet = base.offset(dx, dy, dz);
                        if (!level.hasChunkAt(feet)) {
                            continue;
                        }
                        Vec3 candidate = Vec3.atBottomCenterOf(feet);
                        AABB bounds = player.getBoundingBox().move(candidate.subtract(player.position()));
                        BlockPos support = feet.below();
                        if (level.noCollision(player, bounds)
                                && level.getBlockState(support).isFaceSturdy(level, support, net.minecraft.core.Direction.UP)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return Vec3.atBottomCenterOf(level.getSharedSpawnPos());
    }

    private static boolean isAllowedPlacement(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        return level.hasChunkAt(pos)
                && !state.hasBlockEntity()
                && level.getBlockEntity(pos) == null
                && state.getFluidState().isEmpty()
                && !(block instanceof LiquidBlock)
                && !(block instanceof FallingBlock)
                && !FORBIDDEN_PLACEMENTS.contains(block);
    }

    private enum FinishReason {
        SURVIVED,
        DIED_AND_RESPAWNED,
        OFFLINE_TIMEOUT,
        EXTERNAL_ESCAPE,
        ADMIN_ABORT
    }
}
