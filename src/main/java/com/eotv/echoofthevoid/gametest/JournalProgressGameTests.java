package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.event.UncannyJournalPlaceSystem;
import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import com.eotv.echoofthevoid.lore.UncannyLoreBookLibrary;
import com.eotv.echoofthevoid.state.JournalProgressRecord;
import com.eotv.echoofthevoid.state.JournalProgressRecord.PendingPlace;
import com.eotv.echoofthevoid.state.JournalProgressRecord.PlaceStatus;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** World-level journal progression (D-100) through real Minecraft NBT. */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class JournalProgressGameTests {
    private static final String TEMPLATE = "special_test_room";
    private static final String OVERWORLD = "minecraft:overworld";

    private JournalProgressGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void newWorldStartsWithoutAnyJournal(GameTestHelper helper) {
        UncannyWorldState fresh = UncannyWorldState.load(new CompoundTag(), helper.getLevel().registryAccess());
        JournalProgressRecord progress = fresh.getJournalProgress();
        helper.assertTrue(progress.equals(JournalProgressRecord.empty()), "A world without data must start empty");
        helper.assertTrue(progress.nextVolume() == 1, "The first place must be five torches");
        helper.assertTrue(progress.lastDiscoveryElapsedTicks() == -1L, "No discovery time may be invented");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void worldsFrom221MergeEveryPlayersJournals(GameTestHelper helper) {
        UncannyWorldState old = UncannyWorldState.create();
        UUID host = UUID.fromString("00000000-0000-0000-0000-00000000a001");
        UUID guest = UUID.fromString("00000000-0000-0000-0000-00000000a002");
        old.markHistoryTomeFound(host, 3);
        old.markHistoryTomeFound(guest, 5);
        CompoundTag saved = old.save(new CompoundTag(), helper.getLevel().registryAccess());
        saved.remove(JournalProgressRecord.TAG);

        UncannyWorldState migrated = UncannyWorldState.load(saved, helper.getLevel().registryAccess());
        JournalProgressRecord progress = migrated.getJournalProgress();
        helper.assertTrue(progress.isFound(3) && progress.isFound(5), "Both players' journals must count for the world");
        helper.assertTrue(!progress.isFound(1), "Unfound journals stay unfound");
        helper.assertTrue(progress.nextVolume() == 1, "Migration resumes at the smallest missing journal in story order");
        helper.assertTrue(progress.lastDiscoveryElapsedTicks() == -1L, "Migration must not invent a discovery time");
        helper.assertTrue(!progress.starterMapDelivered() && progress.pending() == null,
                "Migration starts without a map or a pending place");
        helper.assertTrue(migrated.findFirstMissingHistoryTome(host, 6) == 1
                        && migrated.findFirstMissingHistoryTome(guest, 6) == 1,
                "Per-player masks remain readable after migration");

        CompoundTag resaved = migrated.save(new CompoundTag(), helper.getLevel().registryAccess());
        helper.assertTrue(resaved.contains(JournalProgressRecord.TAG), "The migrated block must be written back");
        UncannyWorldState reloaded = UncannyWorldState.load(resaved, helper.getLevel().registryAccess());
        reloaded.markHistoryTomeFound(host, 1);
        helper.assertTrue(!reloaded.getJournalProgress().isFound(1),
                "Migration runs once: later per-player marks never rewrite the world block");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void fullProgressRoundTripsThroughMinecraftNbt(GameTestHelper helper) {
        PendingPlace pending = new PendingPlace(3, OVERWORLD, new BlockPos(412, 71, -388).asLong(),
                PlaceStatus.RESERVED, 360_000L);
        JournalProgressRecord progress = JournalProgressRecord.empty()
                .withFound(1, 48_000L)
                .withFound(2, 240_000L, new BlockPos(5, 64, -7))
                .withStarterMapDelivered()
                .withIssuedMap(17, 1)
                .withIssuedMap(42, 3)
                .withPending(pending)
                .withBuilt(pending);
        UncannyWorldState original = UncannyWorldState.create();
        original.setJournalProgress(progress);

        CompoundTag saved = original.save(new CompoundTag(), helper.getLevel().registryAccess());
        JournalProgressRecord restored = UncannyWorldState.load(saved, helper.getLevel().registryAccess())
                .getJournalProgress();
        helper.assertTrue(restored.equals(progress), "Journal progress must survive NBT unchanged: " + restored);
        helper.assertTrue(restored.pending().status() == PlaceStatus.BUILT, "The built state must survive NBT");
        helper.assertTrue(restored.places().size() == 1 && restored.places().get(0).volume() == 3,
                "The built place must be remembered");
        helper.assertTrue(restored.volumeForMap(42) == 3 && restored.volumeForMap(17) == 1
                        && restored.volumeForMap(99) == -1,
                "Issued maps must keep their target journal");
        helper.assertTrue(restored.lastDiscoveryElapsedTicks() == 240_000L, "The latest discovery time must survive");
        helper.assertTrue(new BlockPos(5, 64, -7).equals(restored.lastDiscoveryAnchor()),
                "The reader's anchor must survive NBT");

        JournalProgressRecord read = restored.withFound(3, 400_000L);
        helper.assertTrue(read.pending() == null && read.places().size() == 1,
                "Reading the pending journal clears the pending place but keeps the built place");
        helper.assertTrue(read.withFound(3, 500_000L) == read, "Reading a journal twice changes nothing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void openingAJournalBookMarksItFoundForTheWholeWorld(GameTestHelper helper) {
        UncannyWorldState state = UncannyWorldState.get(helper.getLevel().getServer());
        JournalProgressRecord saved = state.getJournalProgress();
        try {
            PendingPlace pendingTorches = new PendingPlace(1, OVERWORLD, BlockPos.ZERO.asLong(), PlaceStatus.BUILT, 0L);
            state.setJournalProgress(JournalProgressRecord.empty().withPending(pendingTorches));
            ServerPlayer reader = mockPlayer(helper);

            ItemStack holding = openBook(helper, reader, 1);
            JournalProgressRecord afterFirst = state.getJournalProgress();
            helper.assertTrue(afterFirst.isFound(1), "Opening five torches must mark it found");
            helper.assertTrue(afterFirst.lastDiscoveryElapsedTicks() == state.getCampaignElapsedTicks(),
                    "The discovery time is the current campaign time");
            helper.assertTrue(afterFirst.pending() == null, "Reading the pending journal clears the pending place");
            helper.assertTrue(state.findFirstMissingHistoryTome(reader.getUUID(), 6) == 2,
                    "The per-player mask is still kept for older code paths");
            helper.assertTrue(holding.getCount() == 1, "Reading must never consume the book");

            openBook(helper, reader, 1);
            helper.assertTrue(state.getJournalProgress().equals(afterFirst), "Reading the same journal again changes nothing");

            ServerPlayer friend = mockPlayer(helper);
            PendingPlace pendingWay = new PendingPlace(2, OVERWORLD, BlockPos.ZERO.asLong(), PlaceStatus.RESERVED, 0L);
            state.setJournalProgress(state.getJournalProgress().withPending(pendingWay));
            openBook(helper, friend, 5);
            JournalProgressRecord afterOld = state.getJournalProgress();
            helper.assertTrue(afterOld.isFound(5), "A journal read out of order (older world) still counts");
            helper.assertTrue(afterOld.pending() != null && afterOld.pending().volume() == 2,
                    "Another journal never cancels the pending place");
            helper.assertTrue(afterOld.nextVolume() == 2, "Progression resumes at the smallest missing journal");

            ItemStack stranger = new ItemStack(UncannyItemRegistry.UNCANNY_LORE_PIECE.get());
            stranger.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                    Filterable.passThrough("Five Torches"), "Unknown", 0, List.of(), true));
            helper.assertTrue(UncannyJournalPlaceSystem.onJournalOpened(friend, stranger) == -1
                            && state.getJournalProgress().equals(afterOld),
                    "A book whose title is not exactly a journal title is ignored");
        } finally {
            state.setJournalProgress(saved);
        }
        helper.succeed();
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /** Opens a journal through the real item route, as a right click in hand does. */
    private static ItemStack openBook(GameTestHelper helper, ServerPlayer player, int volume) {
        ItemStack book = new ItemStack(UncannyItemRegistry.UNCANNY_LORE_PIECE.get());
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, UncannyLoreBookLibrary.contentForVolume(volume));
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        book.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void invalidOrStaleJournalDataIsDropped(GameTestHelper helper) {
        CompoundTag block = JournalProgressRecord.empty().withFound(1, 10L).save();
        block.putInt("foundMask", 0xFFFF);
        CompoundTag stalePending = new CompoundTag();
        stalePending.putInt("volume", 1);
        stalePending.putString("dimension", OVERWORLD);
        stalePending.putString("status", "BUILT");
        block.put("pending", stalePending);
        ListTag maps = new ListTag();
        for (int id = 0; id < 80; id++) {
            CompoundTag map = new CompoundTag();
            map.putInt("mapId", id);
            map.putInt("volume", id == 5 ? 9 : 2);
            maps.add(map);
        }
        block.put("maps", maps);
        CompoundTag world = new CompoundTag();
        world.put(JournalProgressRecord.TAG, block);

        JournalProgressRecord loaded = UncannyWorldState.load(world, helper.getLevel().registryAccess())
                .getJournalProgress();
        helper.assertTrue(loaded.foundMask() == 0b111111, "Unknown journal bits must be ignored");
        helper.assertTrue(loaded.pending() == null, "A pending place for an already-read journal is stale");
        helper.assertTrue(loaded.maps().size() == 64, "Issued maps stay bounded");
        helper.assertTrue(loaded.volumeForMap(5) == -1 && loaded.volumeForMap(79) == 2,
                "Invalid volumes are dropped and the newest maps kept");
        helper.succeed();
    }
}
