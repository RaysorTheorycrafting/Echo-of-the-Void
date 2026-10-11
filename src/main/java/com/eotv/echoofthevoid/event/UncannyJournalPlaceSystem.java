package com.eotv.echoofthevoid.event;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.campaign.UncannyCampaignDirector;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.diagnostics.DiagnosticSeverity;
import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import com.eotv.echoofthevoid.lore.UncannyJournalProgressRules.PendingState;
import com.eotv.echoofthevoid.lore.UncannyJournalProgressRules.PlaceAction;
import com.eotv.echoofthevoid.lore.UncannyJournalProgressRules;
import com.eotv.echoofthevoid.state.JournalProgressRecord.PendingPlace;
import com.eotv.echoofthevoid.state.JournalProgressRecord.PlaceStatus;
import com.eotv.echoofthevoid.state.JournalProgressRecord;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * World-level journal places (D-100): a journal counts as found when a player opens it, which unlocks the
 * reservation of the next place once its minimum story day and delay are reached.
 */
public final class UncannyJournalPlaceSystem {
    /** Block entity flag of a chest or barrel placed by a player (absent on generated containers). */
    public static final String PLAYER_PLACED_FLAG = "eotv_player_placed";
    /** Farthest a player container may be from a bed (or player) to receive the starter map. */
    public static final int STARTER_MAP_CONTAINER_RADIUS = 64;
    private static final int TICK_INTERVAL = 20;
    private static final long CANDIDATE_LOAD_TIMEOUT_TICKS = 600L;
    private static final int EARLY_REJECTIONS_PER_TICK = 8;
    private static final int EARLY_REJECTIONS_PER_SEARCH = 256;
    /** Origins tried inside a loaded candidate chunk; the widest place stays within its 3×3 chunks. */
    private static final int[][] ORIGIN_SHIFTS = {
            {0, 0}, {-4, -4}, {4, -4}, {-4, 4}, {4, 4}, {0, -4}, {0, 4}, {-4, 0}, {4, 0}};
    private static final TicketType<ChunkPos> PLACE_TICKET =
            TicketType.create("echoofthevoid_journal_place", Comparator.comparingLong(ChunkPos::toLong), 1_200);
    private static MinecraftServer trackedServer;
    private static Search search;
    private static boolean forcedSearch;
    private static long nextSearchTick;
    /** Player who asked for a map by command before the place existed: it is given as soon as the place stands. */
    private static UUID mapRequester;
    /** Searches in a row that found no site: each one widens the next outward window (see outwardBand). */
    private static int searchWidening;
    private static final int MAX_SEARCH_WIDENING = 8;
    /** A place just built: its chunks stay loaded a few seconds, then what fell meanwhile is picked up. */
    private static Cleanup pendingCleanup;
    private static final long CLEANUP_DELAY_TICKS = 100L;

    private UncannyJournalPlaceSystem() {
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD
                || !(event.getEntity() instanceof ServerPlayer)
                || event.getEntity() instanceof FakePlayer
                || containerKind(event.getPlacedBlock()) == ContainerKind.NONE) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return;
        }
        blockEntity.getPersistentData().putBoolean(PLAYER_PLACED_FLAG, true);
        blockEntity.setChanged();
        UncannyWorldState.get(level.getServer()).rememberPlayerPlacedContainer(pos);
    }

    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.dimension() == Level.OVERWORLD
                && containerKind(event.getState()) != ContainerKind.NONE) {
            UncannyWorldState.get(level.getServer()).forgetPlayerPlacedContainer(event.getPos());
        }
    }

    /** Reading a journal left open on a lectern (Jules' place) counts like opening it in hand. */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof LecternBlockEntity lectern)
                || lectern.getBook().isEmpty()
                || (player.isSecondaryUseActive() && !player.getItemInHand(event.getHand()).isEmpty())) {
            return;
        }
        onJournalOpened(player, lectern.getBook());
    }

    public static ContainerKind containerKind(BlockState state) {
        if (state == null) {
            return ContainerKind.NONE;
        }
        if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)) {
            return ContainerKind.CHEST;
        }
        return state.is(Blocks.BARREL) ? ContainerKind.BARREL : ContainerKind.NONE;
    }

    /** A chest or barrel still standing where a player placed it, with its flag intact. */
    public static boolean isPlayerPlacedContainer(ServerLevel level, BlockPos pos) {
        if (containerKind(level.getBlockState(pos)) == ContainerKind.NONE) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && blockEntity.getPersistentData().getBoolean(PLAYER_PLACED_FLAG);
    }

    /**
     * Where the starter map may appear: each online Overworld player's bed, or the player when no bed is set.
     */
    public static List<BlockPos> starterMapAnchors(MinecraftServer server) {
        List<BlockPos> anchors = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player instanceof FakePlayer) {
                continue;
            }
            BlockPos anchor = starterMapAnchor(player);
            if (anchor != null) {
                anchors.add(anchor);
            }
        }
        return anchors;
    }

    /** The player's Overworld bed, else the player's own position in the Overworld, else {@code null}. */
    public static BlockPos starterMapAnchor(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        BlockPos respawn = player.getRespawnPosition();
        if (server != null && respawn != null && player.getRespawnDimension() == Level.OVERWORLD
                && server.overworld().getBlockState(respawn).getBlock() instanceof BedBlock) {
            return respawn;
        }
        return player.level().dimension() == Level.OVERWORLD ? player.blockPosition() : null;
    }

    /**
     * Nearest loaded player-placed container to any anchor, with a free slot. Entries whose block disappeared
     * without a player breaking it (explosion, piston, command) are forgotten on the way.
     */
    public static BlockPos findPlayerContainerNear(ServerLevel level, List<BlockPos> anchors, boolean allowBarrels) {
        return findPlayerContainerAmong(level,
                UncannyWorldState.get(level.getServer()).getPlayerPlacedContainers(), anchors, allowBarrels);
    }

    /** Same search restricted to the given indexed positions. */
    public static BlockPos findPlayerContainerAmong(
            ServerLevel level, List<BlockPos> candidates, List<BlockPos> anchors, boolean allowBarrels) {
        if (anchors.isEmpty()) {
            return null;
        }
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        long maxDistanceSq = (long) STARTER_MAP_CONTAINER_RADIUS * STARTER_MAP_CONTAINER_RADIUS;
        BlockPos best = null;
        long bestDistanceSq = Long.MAX_VALUE;
        List<BlockPos> vanished = new ArrayList<>();
        for (BlockPos pos : candidates) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (!isPlayerPlacedContainer(level, pos)) {
                vanished.add(pos);
                continue;
            }
            if (!allowBarrels && containerKind(level.getBlockState(pos)) == ContainerKind.BARREL) {
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof Container container)
                    || !UncannyJournalMaps.hasFreeSlot(container)) {
                continue;
            }
            for (BlockPos anchor : anchors) {
                long distanceSq = (long) anchor.distSqr(pos);
                if (distanceSq <= maxDistanceSq && distanceSq < bestDistanceSq) {
                    best = pos;
                    bestDistanceSq = distanceSq;
                }
            }
        }
        vanished.forEach(state::forgetPlayerPlacedContainer);
        return best;
    }


    /**
     * Server side of opening a journal book, whatever its origin (journal place, old structure chest, command).
     *
     * @return the catalog volume newly marked as found, or {@code -1} when nothing changed
     */
    public static int onJournalOpened(ServerPlayer player, ItemStack book) {
        if (player == null || player.getServer() == null || book == null || book.isEmpty()) {
            return -1;
        }
        WrittenBookContent content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
        int volume = content == null ? -1 : UncannyJournalProgressRules.volumeForTitle(content.title().raw());
        if (volume < 0) {
            return -1;
        }
        UncannyWorldState state = UncannyWorldState.get(player.getServer());
        state.markHistoryTomeFound(player.getUUID(), volume);
        JournalProgressRecord progress = state.getJournalProgress();
        if (progress.isFound(volume)) {
            return -1;
        }
        long elapsed = state.getCampaignElapsedTicks();
        JournalProgressRecord next = progress.withFound(volume, elapsed, starterMapAnchor(player));
        state.setJournalProgress(next);
        debugLog("JOURNAL opened player={} volume={} elapsed={} next={} pendingCleared={}",
                player.getGameProfile().getName(), volume, elapsed, next.nextVolume(),
                progress.pending() != null && next.pending() == null);
        UncannyDiagnostics.recordForPlayer(player, DiagnosticSeverity.INFO, "journal", "opened", Map.of(
                "volume", Integer.toString(volume),
                "rank", Integer.toString(UncannyJournalProgressRules.rankOf(volume) + 1),
                "campaign_ticks", Long.toString(elapsed),
                "campaign_length_days", Integer.toString(UncannyCampaignDirector.campaignLengthDays()),
                "next_volume", Integer.toString(next.nextVolume())));
        return volume;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        // The GameTest server runs isolated scenarios with mock players: places are only driven by their own tests.
        if (server == null || server instanceof GameTestServer) {
            return;
        }
        if (trackedServer != server) {
            trackedServer = server;
            search = null;
            forcedSearch = false;
            nextSearchTick = 0L;
            mapRequester = null;
            pendingCleanup = null;
            searchWidening = 0;
        }
        if (pendingCleanup != null && server.getTickCount() >= pendingCleanup.dueTick()) {
            finishCleanup(server.overworld());
        }
        // A running search steps every tick, so a place is found in seconds; everything else once a second.
        boolean cadence = server.getTickCount() % TICK_INTERVAL == 0L;
        if (!cadence && search == null && !forcedSearch) {
            return;
        }
        UncannyWorldState state = UncannyWorldState.get(server);
        if (!state.isSystemEnabled()) {
            return;
        }
        ServerLevel overworld = server.overworld();
        JournalProgressRecord progress = state.getJournalProgress();
        PendingPlace pending = progress.pending();
        PendingState pendingState = pending == null ? PendingState.NONE
                : pending.status() == PlaceStatus.BUILT ? PendingState.BUILT : PendingState.RESERVED;
        PlaceAction action = UncannyJournalProgressRules.nextAction(progress.resolvedMask(), pendingState,
                state.getCampaignElapsedTicks(), progress.lastDiscoveryElapsedTicks(),
                UncannyCampaignDirector.campaignLengthDays());
        if (action == PlaceAction.WAIT_FOR_ELIGIBILITY && forcedSearch) {
            action = PlaceAction.SEARCH;
        }
        switch (action) {
            case SEARCH -> stepSearch(overworld, state, progress);
            case BUILD_RESERVED -> {
                if (cadence) {
                    stepBuildReserved(overworld, state, progress, pending);
                }
            }
            case WAIT_FOR_READER -> {
                dropSearch(overworld);
                forcedSearch = false;
                if (cadence) {
                    UncannyJournalRecovery.tick(overworld);
                    UncannyJournalMaps.tickStarterMap(overworld);
                }
            }
            default -> {
                dropSearch(overworld);
                forcedSearch = false;
            }
        }
    }

    /** Development shortcut: look for the next place now, ignoring the minimum day and the delay. */
    public static boolean forceSearchNow(MinecraftServer server) {
        JournalProgressRecord progress = UncannyWorldState.get(server).getJournalProgress();
        if (progress.pending() != null || progress.nextVolume() < 0) {
            return false;
        }
        // A search already running keeps its progress; an exhausted one restarts at once.
        forcedSearch = true;
        nextSearchTick = 0L;
        return true;
    }

    /**
     * Development shortcut behind {@code /uncanny journal map}: a map towards the waiting place now, or, when the
     * next place does not stand yet, a search started at once and the map handed over as soon as it is built.
     *
     * @return the map when given now, else {@link ItemStack#EMPTY}; see {@link #isMapRequested}
     */
    public static ItemStack requestMap(ServerPlayer player) {
        ItemStack map = UncannyJournalMaps.mapTowardsPendingPlace(player.serverLevel());
        if (!map.isEmpty()) {
            return map;
        }
        MinecraftServer server = player.getServer();
        JournalProgressRecord progress = UncannyWorldState.get(server).getJournalProgress();
        if (progress.pending() != null || forceSearchNow(server)) {
            mapRequester = player.getUUID();
        }
        return ItemStack.EMPTY;
    }

    public static boolean isMapRequested(ServerPlayer player) {
        return player.getUUID().equals(mapRequester);
    }

    /**
     * Development preview: builds the place of {@code volume} ten blocks in front of the player, door towards them,
     * with the same terrain checks; the world progression is untouched.
     */
    public static String buildInFrontForDebug(ServerPlayer player, int volume) {
        ServerLevel level = player.serverLevel();
        Direction ahead = player.getDirection();
        int distance = UncannyJournalPlaces.footprintRadius(volume) + UncannyJournalPlaces.GROUND_MARGIN + 4;
        BlockPos column = player.blockPosition().relative(ahead, distance);
        BlockPos origin = UncannyJournalPlaces.levelledOrigin(level, volume, column.getX(), column.getZ());
        String reason = UncannyJournalPlaces.rejectionReason(level, volume, origin);
        Direction facing = volume == 5 ? UncannyJournalPlaces.buildFacing(level, volume, origin, level.random)
                : ahead.getOpposite();
        if (reason != null || facing == null) {
            return "Cannot build journal place " + volume + " at " + origin.toShortString() + ": "
                    + (reason == null ? "no-facing" : reason);
        }
        BlockPos chest = UncannyJournalPlaces.build(level, volume, origin, facing);
        return chest == null
                ? "Journal place " + volume + " could not be filled at " + origin.toShortString()
                : "Built journal place " + volume + " at " + origin.toShortString() + " (chest " + chest.toShortString() + ")";
    }

    /** Development reset of the world progression (per-player masks are kept). */
    public static void resetProgress(MinecraftServer server) {
        UncannyWorldState.get(server).setJournalProgress(JournalProgressRecord.empty());
        dropSearch(server.overworld());
        forcedSearch = false;
        nextSearchTick = 0L;
        mapRequester = null;
        searchWidening = 0;
    }

    private static void stepSearch(ServerLevel level, UncannyWorldState state, JournalProgressRecord progress) {
        long now = level.getServer().getTickCount();
        if (now < nextSearchTick) {
            return;
        }
        int volume = progress.nextVolume();
        if (search == null || search.volume != volume) {
            dropSearch(level);
            BlockPos anchor = progress.lastDiscoveryAnchor();
            if (anchor == null) {
                List<BlockPos> anchors = starterMapAnchors(level.getServer());
                if (anchors.isEmpty()) {
                    return;
                }
                anchor = anchors.get(level.random.nextInt(anchors.size()));
            }
            // The first place lies around the reader's home; every later one farther from (0, 0) than all before
            // (and than the reader's home), on the reader's side.
            boolean outward = UncannyJournalProgressRules.rankOf(volume) > 0;
            double farthest = farthestPlaceDistance(progress);
            search = new Search(volume, anchor.immutable(), outward, farthest);
            debugLog("JOURNAL search start volume={} anchor={} outward={} farthest={}", volume, anchor, outward, farthest);
        }
        if (search.candidate == null) {
            if (search.attempts >= UncannyJournalProgressRules.CANDIDATES_PER_SEARCH
                    || search.earlyRejections >= EARLY_REJECTIONS_PER_SEARCH) {
                recordJournal("search_exhausted", Map.of("volume", Integer.toString(volume),
                        "anchor", search.anchor.toShortString(),
                        "loaded_candidates", Integer.toString(search.attempts),
                        "early_rejections", Integer.toString(search.earlyRejections),
                        "widening", Integer.toString(searchWidening)));
                debugLog("JOURNAL search exhausted volume={} anchor={} loaded={} early={}",
                        volume, search.anchor, search.attempts, search.earlyRejections);
                search = null;
                // The window may lie over an ocean: the next search looks wider (and at once for a dev command).
                searchWidening = Math.min(searchWidening + 1, MAX_SEARCH_WIDENING);
                nextSearchTick = forcedSearch ? now : now + UncannyJournalProgressRules.SEARCH_COOLDOWN_TICKS;
                return;
            }
            // Columns refused without loading anything (water biome, players, other places) cost no load attempt.
            for (int tries = 0; tries < EARLY_REJECTIONS_PER_TICK; tries++) {
                ChunkPos chunk = new ChunkPos(candidateColumn(level, search));
                BlockPos column = new BlockPos(chunk.getMiddleBlockX(), search.anchor.getY(), chunk.getMiddleBlockZ());
                String early = earlyRejection(level, state, progress, column);
                if (early == null) {
                    search.attempts++;
                    level.getChunkSource().addRegionTicket(PLACE_TICKET, chunk, 1, chunk);
                    search.candidate = new Candidate(chunk, now);
                    return;
                }
                search.earlyRejections++;
                debugLog("JOURNAL column rejected early volume={} column={} reason={}", volume, column, early);
            }
            return;
        }
        Candidate candidate = search.candidate;
        if (!isAreaLoaded(level, candidate.chunk)) {
            if (now - candidate.requestedTick > CANDIDATE_LOAD_TIMEOUT_TICKS) {
                releaseCandidate(level, "load-timeout");
            }
            return;
        }
        // The 3×3 chunks are loaded anyway: try several origins in the centre chunk, not only its middle.
        BlockPos origin = null;
        String reason = "visited";
        for (int[] shift : ORIGIN_SHIFTS) {
            int x = candidate.chunk.getMiddleBlockX() + shift[0];
            int z = candidate.chunk.getMiddleBlockZ() + shift[1];
            if (search.outward && !UncannyJournalProgressRules.isFartherThanEveryPlace(x, z, search.farthest)) {
                reason = "not-outward";
                continue;
            }
            BlockPos tried = UncannyJournalPlaces.levelledOrigin(level, volume, x, z);
            if (!isFootprintUnvisited(level, tried)) {
                reason = "visited";
                continue;
            }
            String refused = UncannyJournalPlaces.rejectionReason(level, volume, tried);
            if (refused == null) {
                origin = tried;
                break;
            }
            reason = refused;
        }
        if (origin == null) {
            releaseCandidate(level, reason);
            return;
        }
        PendingPlace reserved = new PendingPlace(volume, level.dimension().location().toString(), origin.asLong(),
                PlaceStatus.RESERVED, state.getCampaignElapsedTicks());
        JournalProgressRecord next = progress.withPending(reserved);
        state.setJournalProgress(next);
        recordJournal("reserved", Map.of("volume", Integer.toString(volume), "position", origin.toShortString(),
                "attempts", Integer.toString(search.attempts), "forced", Boolean.toString(forcedSearch)));
        debugLog("JOURNAL reserved volume={} origin={} attempts={}", volume, origin, search.attempts);
        search = null;
        forcedSearch = false;
        searchWidening = 0;
        stepBuildReserved(level, state, next, reserved);
    }

    /** A random column for the next candidate: around the reader for the first place, outward for the others. */
    private static BlockPos candidateColumn(ServerLevel level, Search search) {
        if (search.outward) {
            UncannyJournalProgressRules.DistanceBand band = UncannyJournalProgressRules.outwardBand(search.farthest,
                    Math.hypot(search.anchor.getX(), search.anchor.getZ()), searchWidening);
            int[] xz = UncannyJournalProgressRules.outwardCandidate(level.random.nextDouble(), level.random.nextDouble(),
                    band, search.anchor.getX(), search.anchor.getZ(), searchWidening);
            return new BlockPos(xz[0], search.anchor.getY(), xz[1]);
        }
        UncannyJournalProgressRules.DistanceBand around = UncannyJournalProgressRules.distanceBand(search.volume);
        // A home on a small island: failed searches reach farther out, as for the outward places.
        UncannyJournalProgressRules.DistanceBand widened = new UncannyJournalProgressRules.DistanceBand(around.minBlocks(),
                around.maxBlocks() + (around.maxBlocks() - around.minBlocks()) * searchWidening);
        int[] offset = UncannyJournalProgressRules.candidateOffset(level.random.nextDouble(),
                level.random.nextDouble(), widened);
        return search.anchor.offset(offset[0], 0, offset[1]);
    }

    private static double farthestPlaceDistance(JournalProgressRecord progress) {
        double farthest = 0.0D;
        for (JournalProgressRecord.Place place : progress.places()) {
            BlockPos pos = BlockPos.of(place.posLong());
            farthest = Math.max(farthest, Math.hypot(pos.getX(), pos.getZ()));
        }
        return farthest;
    }

    private static String earlyRejection(
            ServerLevel level, UncannyWorldState state, JournalProgressRecord progress, BlockPos column) {
        long minPlayerSq = sq(UncannyJournalProgressRules.MIN_PLAYER_DISTANCE_AT_BUILD);
        for (ServerPlayer player : level.players()) {
            if (horizontalDistanceSq(player.blockPosition(), column) < minPlayerSq) {
                return "player-near";
            }
        }
        long placeSq = sq(UncannyJournalProgressRules.MIN_DISTANCE_BETWEEN_PLACES);
        for (JournalProgressRecord.Place place : progress.places()) {
            if (horizontalDistanceSq(BlockPos.of(place.posLong()), column) < placeSq) {
                return "other-place";
            }
        }
        long markerSq = sq(UncannyJournalProgressRules.MIN_DISTANCE_FROM_STRUCTURE_MARKERS);
        for (UncannyWorldState.StructureMarker marker : state.getStructureMarkers()) {
            if (level.dimension().location().toString().equals(marker.dimension())
                    && horizontalDistanceSq(BlockPos.of(marker.posLong()), column) < markerSq) {
                return "uncanny-structure";
            }
        }
        if (!level.getWorldBorder().isWithinBounds(column)) {
            return "world-border";
        }
        // The biome is known from the generator without generating the chunk: reject water before loading it.
        return UncannyJournalPlaces.isWaterBiome(level, column) ? "biome" : null;
    }

    private static void stepBuildReserved(
            ServerLevel level, UncannyWorldState state, JournalProgressRecord progress, PendingPlace pending) {
        if (!level.dimension().location().toString().equals(pending.dimension())) {
            return;
        }
        BlockPos origin = BlockPos.of(pending.posLong());
        long minPlayerSq = sq(UncannyJournalProgressRules.MIN_PLAYER_DISTANCE_AT_BUILD);
        for (ServerPlayer player : level.players()) {
            if (horizontalDistanceSq(player.blockPosition(), origin) < minPlayerSq) {
                return;
            }
        }
        ChunkPos chunk = new ChunkPos(origin);
        if (!isAreaLoaded(level, chunk)) {
            level.getChunkSource().addRegionTicket(PLACE_TICKET, chunk, 1, chunk);
            return;
        }
        String reason = UncannyJournalPlaces.rejectionReason(level, pending.volume(), origin);
        Direction facing = reason == null
                ? UncannyJournalPlaces.buildFacing(level, pending.volume(), origin, level.random) : null;
        if (reason == null && facing == null) {
            reason = "no-facing";
        }
        BlockPos chest = reason == null ? UncannyJournalPlaces.build(level, pending.volume(), origin, facing) : null;
        if (chest != null) {
            if (pendingCleanup != null) {
                finishCleanup(level);
            }
            pendingCleanup = new Cleanup(chunk, origin.immutable(), level.getServer().getTickCount() + CLEANUP_DELAY_TICKS);
        } else {
            level.getChunkSource().removeRegionTicket(PLACE_TICKET, chunk, 1, chunk);
        }
        if (chest == null) {
            state.setJournalProgress(progress.withPending(null));
            recordJournal("reservation_dropped", Map.of("volume", Integer.toString(pending.volume()),
                    "position", origin.toShortString(), "reason", reason == null ? "build-failed" : reason));
            debugLog("JOURNAL reservation dropped volume={} origin={} reason={}", pending.volume(), origin, reason);
            return;
        }
        state.setJournalProgress(progress.withBuilt(pending.withHolder(chest)));
        recordJournal("built", Map.of("volume", Integer.toString(pending.volume()), "position", origin.toShortString(),
                "chest", chest.toShortString()));
        debugLog("JOURNAL built volume={} origin={} chest={}", pending.volume(), origin, chest);
        deliverRequestedMap(level);
    }

    private static void finishCleanup(ServerLevel level) {
        Cleanup cleanup = pendingCleanup;
        pendingCleanup = null;
        int removed = UncannyJournalPlaces.clearLooseItems(level, cleanup.origin());
        level.getChunkSource().removeRegionTicket(PLACE_TICKET, cleanup.chunk(), 1, cleanup.chunk());
        if (removed > 0) {
            debugLog("JOURNAL cleanup origin={} removed={}", cleanup.origin(), removed);
        }
    }

    private record Cleanup(ChunkPos chunk, BlockPos origin, long dueTick) {
    }

    private static void deliverRequestedMap(ServerLevel level) {
        ServerPlayer player = mapRequester == null ? null : level.getServer().getPlayerList().getPlayer(mapRequester);
        mapRequester = null;
        if (player == null) {
            return;
        }
        ItemStack map = UncannyJournalMaps.mapTowardsPendingPlace(level);
        if (map.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(map)) {
            player.drop(map, false);
        }
        player.sendSystemMessage(Component.literal("The journal place is built: here is the map towards it."));
    }

    /** Every chunk under the widest footprint is loaded and has never been near a player. */
    private static boolean isFootprintUnvisited(ServerLevel level, BlockPos origin) {
        int reach = UncannyJournalPlaces.MAX_REACH;
        for (int dx = -reach; dx <= reach; dx += reach) {
            for (int dz = -reach; dz <= reach; dz += reach) {
                LevelChunk chunk = level.getChunkSource().getChunkNow((origin.getX() + dx) >> 4, (origin.getZ() + dz) >> 4);
                if (chunk == null || chunk.getInhabitedTime() > 0L) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isAreaLoaded(ServerLevel level, ChunkPos chunk) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (level.getChunkSource().getChunkNow(chunk.x + dx, chunk.z + dz) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Forgets the current search, releasing the chunks of a candidate still being loaded. */
    private static void dropSearch(ServerLevel level) {
        if (search != null && search.candidate != null) {
            level.getChunkSource().removeRegionTicket(PLACE_TICKET, search.candidate.chunk, 1, search.candidate.chunk);
        }
        search = null;
    }

    private static void releaseCandidate(ServerLevel level, String reason) {
        Candidate candidate = search.candidate;
        level.getChunkSource().removeRegionTicket(PLACE_TICKET, candidate.chunk, 1, candidate.chunk);
        search.candidate = null;
        rejectCandidate(search.volume, candidate.chunk.getMiddleBlockPosition(0), reason);
    }

    private static void rejectCandidate(int volume, BlockPos column, String reason) {
        debugLog("JOURNAL candidate rejected volume={} column={} reason={}", volume, column, reason);
        recordJournal("candidate_rejected", Map.of("volume", Integer.toString(volume),
                "column", column.getX() + "," + column.getZ(), "reason", reason));
    }

    private static void recordJournal(String code, Map<String, String> fields) {
        UncannyDiagnostics.record(DiagnosticSeverity.INFO, "journal", code, fields);
    }

    private static long horizontalDistanceSq(BlockPos a, BlockPos b) {
        long dx = a.getX() - b.getX();
        long dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private static long sq(int value) {
        return (long) value * value;
    }

    private static final class Search {
        private final int volume;
        private final BlockPos anchor;
        private final boolean outward;
        private final double farthest;
        private int attempts;
        private int earlyRejections;
        private Candidate candidate;

        private Search(int volume, BlockPos anchor, boolean outward, double farthest) {
            this.volume = volume;
            this.anchor = anchor;
            this.outward = outward;
            this.farthest = farthest;
        }
    }

    private record Candidate(ChunkPos chunk, long requestedTick) {
    }

    public enum ContainerKind {
        NONE,
        CHEST,
        BARREL
    }

    private static void debugLog(String message, Object... args) {
        if (UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info("[UncannyDebug/Journal] " + message, args);
        }
    }
}
