package com.eotv.echoofthevoid.event;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.campaign.UncannyCampaignDirector;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.diagnostics.DiagnosticSeverity;
import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import com.eotv.echoofthevoid.lore.UncannyJournalProgressRules.StarterMapTier;
import com.eotv.echoofthevoid.lore.UncannyJournalProgressRules;
import com.eotv.echoofthevoid.state.JournalProgressRecord.PendingPlace;
import com.eotv.echoofthevoid.state.JournalProgressRecord.PlaceStatus;
import com.eotv.echoofthevoid.state.JournalProgressRecord;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Treasure maps towards journal places (D-100): the single starter map that appears in a player's own container,
 * maps found in ordinary structures, and the cross that fades once a place has been read.
 */
public final class UncannyJournalMaps {
    /** Decoration key of the cross, as vanilla explorer maps use. */
    public static final String CROSS_KEY = "+";
    /** A container is "watched" while an awake player is this close. */
    public static final int OBSERVER_RADIUS = 16;
    /** Pre-D-100 chests carry no player flag: an unflagged chest this close to a bed is accepted too. */
    public static final int LEGACY_BED_CHEST_RADIUS = 16;
    private static final byte MAP_SCALE = 2;

    private UncannyJournalMaps() {
    }

    // ---------------------------------------------------------------- map items

    /** A treasure map with a red cross on the pending place, remembered so the cross can fade later. */
    public static ItemStack createMapTowards(ServerLevel level, PendingPlace place) {
        BlockPos target = BlockPos.of(place.posLong());
        ItemStack map = MapItem.create(level, target.getX(), target.getZ(), MAP_SCALE, true, true);
        MapItem.renderBiomePreviewMap(level, map);
        MapItemSavedData.addTargetDecoration(map, target, CROSS_KEY, MapDecorationTypes.RED_X);
        MapId id = map.get(DataComponents.MAP_ID);
        if (id != null) {
            UncannyWorldState state = UncannyWorldState.get(level.getServer());
            state.setJournalProgress(state.getJournalProgress().withIssuedMap(id.id(), place.volume()));
        }
        return map;
    }

    /** A map towards the place currently standing and unread, or empty when there is none (used by structures). */
    public static ItemStack mapTowardsPendingPlace(ServerLevel level) {
        PendingPlace pending = UncannyWorldState.get(level.getServer()).getJournalProgress().pending();
        if (pending == null || pending.status() != PlaceStatus.BUILT
                || !level.dimension().location().toString().equals(pending.dimension())) {
            return ItemStack.EMPTY;
        }
        return createMapTowards(level, pending);
    }

    /** Erases the cross of a carried map whose place has been read ("the map fixes itself when I pick it up"). */
    public static boolean fadeIfRead(ServerLevel level, ItemStack stack) {
        if (!stack.is(Items.FILLED_MAP)) {
            return false;
        }
        MapId id = stack.get(DataComponents.MAP_ID);
        if (id == null) {
            return false;
        }
        JournalProgressRecord progress = UncannyWorldState.get(level.getServer()).getJournalProgress();
        int volume = progress.volumeForMap(id.id());
        if (volume < 0 || !progress.isFound(volume)) {
            return false;
        }
        // Only when the cross leaves the item: the saved data would otherwise be resent every second.
        if (stack.remove(DataComponents.MAP_DECORATIONS) == null) {
            return false;
        }
        MapItemSavedData data = MapItem.getSavedData(id, level);
        if (data != null) {
            data.removeDecoration(CROSS_KEY);
        }
        return true;
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            fadeIfRead(level, player.getInventory().getItem(slot));
        }
    }

    // ---------------------------------------------------------------- starter map

    /** The starter map is waiting for a container: first place still unread and standing, no map delivered yet. */
    public static boolean starterMapWanted(ServerLevel level) {
        JournalProgressRecord progress = UncannyWorldState.get(level.getServer()).getJournalProgress();
        PendingPlace pending = progress.pending();
        return !progress.starterMapDelivered()
                && UncannyJournalProgressRules.needsStarterMap(progress.foundMask())
                && pending != null
                && pending.status() == PlaceStatus.BUILT
                && pending.volume() == UncannyJournalProgressRules.DISCOVERY_ORDER.get(0)
                && level.dimension().location().toString().equals(pending.dimension());
    }

    public static StarterMapTier currentTier(ServerLevel level) {
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        PendingPlace pending = state.getJournalProgress().pending();
        return UncannyJournalProgressRules.starterMapTier(pending == null ? 0L : pending.reservedElapsedTicks(),
                state.getCampaignElapsedTicks(), UncannyCampaignDirector.campaignLengthDays());
    }

    /** Called each second by the journal place system: tries the player containers near the beds. */
    public static void tickStarterMap(ServerLevel level) {
        if (!starterMapWanted(level)) {
            return;
        }
        MinecraftServer server = level.getServer();
        List<BlockPos> anchors = UncannyJournalPlaceSystem.starterMapAnchors(server);
        StarterMapTier tier = currentTier(level);
        BlockPos target = chooseHomeContainer(level, anchors, tier);
        if (target != null && !isObserved(level, target)) {
            deliverStarterMap(level, target, tier, "home");
        }
    }

    /** Player-placed chest (barrel too from the second tier), else an older unflagged chest right by a bed. */
    static BlockPos chooseHomeContainer(ServerLevel level, List<BlockPos> anchors, StarterMapTier tier) {
        boolean barrels = tier != StarterMapTier.PLAYER_CHEST;
        BlockPos flagged = UncannyJournalPlaceSystem.findPlayerContainerNear(level, anchors, barrels);
        if (flagged != null) {
            return flagged;
        }
        List<BlockPos> beds = new ArrayList<>();
        for (BlockPos anchor : anchors) {
            if (level.getBlockState(anchor).getBlock() instanceof BedBlock) {
                beds.add(anchor);
            }
        }
        return findLegacyChestNearBeds(level, beds);
    }

    /** Chests from before the player flag existed: unflagged, unlooted, not part of any structure. */
    public static BlockPos findLegacyChestNearBeds(ServerLevel level, List<BlockPos> beds) {
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        long radiusSq = (long) LEGACY_BED_CHEST_RADIUS * LEGACY_BED_CHEST_RADIUS;
        BlockPos best = null;
        long bestSq = Long.MAX_VALUE;
        for (BlockPos bed : beds) {
            int cx = bed.getX() >> 4;
            int cz = bed.getZ() >> 4;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                    if (chunk == null) {
                        continue;
                    }
                    for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                        if (!(blockEntity instanceof ChestBlockEntity chest)) {
                            continue;
                        }
                        BlockPos pos = chest.getBlockPos();
                        long sq = (long) pos.distSqr(bed);
                        if (sq > radiusSq || sq >= bestSq || chest.getLootTable() != null
                                || !chest.getPersistentData().isEmpty()
                                || state.hasAnyStructureMarkerNearby(level.dimension(), pos,
                                        UncannyJournalProgressRules.MIN_DISTANCE_FROM_STRUCTURE_MARKERS)
                                || !hasFreeSlot(chest)) {
                            continue;
                        }
                        best = pos;
                        bestSq = sq;
                    }
                }
            }
        }
        return best;
    }

    /** Someone awake nearby, or the container open: the map would appear under their eyes. */
    public static boolean isObserved(ServerLevel level, BlockPos pos) {
        long radiusSq = (long) OBSERVER_RADIUS * OBSERVER_RADIUS;
        for (ServerPlayer player : level.players()) {
            if (!player.isSleeping() && !player.isSpectator() && player.blockPosition().distSqr(pos) <= radiusSq) {
                return true;
            }
        }
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity) {
            return ChestBlockEntity.getOpenCount(level, pos) > 0;
        }
        return level.getBlockState(pos).hasProperty(BarrelBlock.OPEN) && level.getBlockState(pos).getValue(BarrelBlock.OPEN);
    }

    /** Puts the starter map in a free slot of the container at {@code pos}; once per world. */
    public static boolean deliverStarterMap(ServerLevel level, BlockPos pos, StarterMapTier tier, String route) {
        if (!(level.getBlockEntity(pos) instanceof Container container)) {
            return false;
        }
        return deliverStarterMapInto(level, container, pos.toShortString(), tier, route);
    }

    private static boolean deliverStarterMapInto(
            ServerLevel level, Container container, String where, StarterMapTier tier, String route) {
        if (!starterMapWanted(level)) {
            return false;
        }
        List<Integer> free = freeSlots(container);
        if (free.isEmpty()) {
            return false;
        }
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        PendingPlace pending = state.getJournalProgress().pending();
        ItemStack map = createMapTowards(level, pending);
        container.setItem(free.get(level.random.nextInt(free.size())), map);
        container.setChanged();
        state.setJournalProgress(state.getJournalProgress().withStarterMapDelivered());
        UncannyDiagnostics.record(DiagnosticSeverity.INFO, "journal", "starter_map", Map.of(
                "container", where, "tier", tier.name().toLowerCase(java.util.Locale.ROOT), "route", route,
                "place", BlockPos.of(pending.posLong()).toShortString()));
        debugLog("JOURNAL starter map delivered container={} tier={} route={}", where, tier, route);
        return true;
    }

    /**
     * Last tier: the next never-opened loot container the player opens (village, shipwreck, mineshaft...) holds the
     * map. The loot is rolled first so the map lies among it.
     */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player instanceof FakePlayer
                || player.isSpectator()
                || !(event.getLevel() instanceof ServerLevel level)
                || level.getServer() instanceof GameTestServer
                || (player.isSecondaryUseActive() && !player.getItemInHand(event.getHand()).isEmpty())
                || !(level.getBlockEntity(event.getPos()) instanceof RandomizableContainer loot)
                || loot.getLootTable() == null
                || !starterMapWanted(level)) {
            return;
        }
        onFirstLootOpen(player, loot, event.getPos(), currentTier(level));
    }

    /** @return whether the starter map was placed among the freshly rolled loot */
    public static boolean onFirstLootOpen(ServerPlayer player, RandomizableContainer loot, BlockPos pos, StarterMapTier tier) {
        if (tier != StarterMapTier.ANY_UNOPENED_CONTAINER || player.isSpectator() || loot.getLootTable() == null
                || !starterMapWanted(player.serverLevel())) {
            return false;
        }
        loot.unpackLootTable(player);
        return deliverStarterMapInto(player.serverLevel(), loot, pos.toShortString(), tier, "unopened-loot");
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player instanceof FakePlayer
                || player.isSpectator()
                || !(event.getLevel() instanceof ServerLevel level)
                || level.getServer() instanceof GameTestServer
                || !(event.getTarget() instanceof ContainerEntity cart)
                || cart.getLootTable() == null
                || !starterMapWanted(level)) {
            return;
        }
        StarterMapTier tier = currentTier(level);
        if (tier == StarterMapTier.ANY_UNOPENED_CONTAINER) {
            cart.unpackChestVehicleLootTable(player);
            deliverStarterMapInto(level, cart, event.getTarget().blockPosition().toShortString(), tier, "unopened-cart");
        }
    }

    /** Ordinary structure chests opened for the first time also count as never-opened containers (step 8). */
    public static boolean offerStarterMapToFreshContainer(ServerLevel level, Container container, BlockPos pos) {
        return currentTier(level) == StarterMapTier.ANY_UNOPENED_CONTAINER
                && deliverStarterMapInto(level, container, pos.toShortString(), StarterMapTier.ANY_UNOPENED_CONTAINER,
                        "fresh-structure-chest");
    }

    /** Empty slots of a container, in order. */
    public static List<Integer> freeSlots(Container container) {
        List<Integer> free = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).isEmpty()) {
                free.add(slot);
            }
        }
        return free;
    }

    public static boolean hasFreeSlot(Container container) {
        return !freeSlots(container).isEmpty();
    }

    private static void debugLog(String message, Object... args) {
        if (UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info("[UncannyDebug/Journal] " + message, args);
        }
    }
}
