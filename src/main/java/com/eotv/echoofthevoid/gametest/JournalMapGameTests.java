package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.event.UncannyJournalMaps;
import com.eotv.echoofthevoid.event.UncannyJournalPlaces;
import com.eotv.echoofthevoid.lore.UncannyJournalProgressRules.StarterMapTier;
import com.eotv.echoofthevoid.state.JournalProgressRecord;
import com.eotv.echoofthevoid.state.JournalProgressRecord.PendingPlace;
import com.eotv.echoofthevoid.state.JournalProgressRecord.PlaceStatus;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Treasure maps towards journal places (D-100). Shared world progress is restored after each test. */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class JournalMapGameTests {
    private static final String TEMPLATE = "special_test_room";

    private JournalMapGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void mapCrossPointsAtThePlaceAndFadesOnceItsJournalIsRead(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        JournalProgressRecord saved = state.getJournalProgress();
        try {
            BlockPos place = helper.absolutePos(new BlockPos(8, 1, 8)).offset(250, 0, -120);
            state.setJournalProgress(JournalProgressRecord.empty().withPending(builtPlace(level, 1, place)));
            ItemStack map = UncannyJournalMaps.mapTowardsPendingPlace(level);
            helper.assertTrue(map.is(Items.FILLED_MAP), "A filled map is made");
            helper.assertTrue(map.get(DataComponents.MAP_DECORATIONS) != null
                            && map.get(DataComponents.MAP_DECORATIONS).decorations().containsKey(UncannyJournalMaps.CROSS_KEY),
                    "The map carries the cross");
            MapId id = map.get(DataComponents.MAP_ID);
            helper.assertTrue(state.getJournalProgress().volumeForMap(id.id()) == 1, "The map is remembered as leading to place 1");
            helper.assertTrue(!UncannyJournalMaps.fadeIfRead(level, map), "The cross stays while the journal is unread");

            state.setJournalProgress(state.getJournalProgress().withFound(1, state.getCampaignElapsedTicks()));
            helper.assertTrue(UncannyJournalMaps.fadeIfRead(level, map), "Once read, the cross fades");
            helper.assertTrue(map.get(DataComponents.MAP_DECORATIONS) == null, "The cross is gone from the item");
            helper.assertTrue(!UncannyJournalMaps.fadeIfRead(level, map), "Fading happens once");
            helper.assertTrue(UncannyJournalMaps.mapTowardsPendingPlace(level).isEmpty(), "No pending place, no map");
        } finally {
            state.setJournalProgress(saved);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void starterMapIsDeliveredOncePerWorld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        JournalProgressRecord saved = state.getJournalProgress();
        try {
            BlockPos first = chest(helper, new BlockPos(4, 1, 4));
            BlockPos second = chest(helper, new BlockPos(11, 1, 4));
            PendingPlace reserved = new PendingPlace(1, level.dimension().location().toString(),
                    first.offset(200, 0, 0).asLong(), PlaceStatus.RESERVED, 0L);
            state.setJournalProgress(JournalProgressRecord.empty().withPending(reserved));
            helper.assertTrue(!UncannyJournalMaps.deliverStarterMap(level, first, StarterMapTier.PLAYER_CHEST, "test"),
                    "No starter map before the first place stands");

            state.setJournalProgress(JournalProgressRecord.empty().withPending(builtPlace(level, 1, first.offset(200, 0, 0))));
            helper.assertTrue(UncannyJournalMaps.deliverStarterMap(level, first, StarterMapTier.PLAYER_CHEST, "test"),
                    "The starter map goes into the chest");
            helper.assertTrue(countMaps((Container) level.getBlockEntity(first)) == 1, "Exactly one map in the chest");
            helper.assertTrue(state.getJournalProgress().starterMapDelivered(), "Delivery is remembered");
            helper.assertTrue(!UncannyJournalMaps.deliverStarterMap(level, second, StarterMapTier.PLAYER_CHEST, "test")
                            && countMaps((Container) level.getBlockEntity(second)) == 0,
                    "Only one starter map per world, even with several players and chests");

            state.setJournalProgress(JournalProgressRecord.empty().withFound(1, 0L)
                    .withPending(builtPlace(level, 2, first.offset(300, 0, 0))));
            helper.assertTrue(!UncannyJournalMaps.starterMapWanted(level),
                    "A world that already read five torches never gets a starter map");
        } finally {
            state.setJournalProgress(saved);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void olderUnflaggedChestByABedIsAcceptedButNotLootOrPlaceChests(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos foot = helper.absolutePos(new BlockPos(3, 1, 8));
        BlockPos head = foot.north();
        level.setBlock(foot, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH)
                .setValue(BedBlock.PART, BedPart.FOOT), 3);
        level.setBlock(head, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH)
                .setValue(BedBlock.PART, BedPart.HEAD), 3);
        BlockPos loot = chest(helper, new BlockPos(4, 1, 6));
        ((ChestBlockEntity) level.getBlockEntity(loot)).setLootTable(BuiltInLootTables.SIMPLE_DUNGEON, 1L);
        BlockPos placeChest = chest(helper, new BlockPos(5, 1, 6));
        level.getBlockEntity(placeChest).getPersistentData().putInt(UncannyJournalPlaces.JOURNAL_PLACE_CHEST_FLAG, 1);
        BlockPos plain = chest(helper, new BlockPos(9, 1, 9));

        helper.assertTrue(plain.equals(UncannyJournalMaps.findLegacyChestNearBeds(level, List.of(head))),
                "A plain chest by the bed is the player's, even without the new flag");
        level.setBlock(plain, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(UncannyJournalMaps.findLegacyChestNearBeds(level, List.of(head)) == null,
                "Unlooted loot chests and journal place chests are never taken for the player's");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void lastTierPutsTheMapAmongFreshLootOnly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        JournalProgressRecord saved = state.getJournalProgress();
        try {
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            NetworkRegistry.configureMockConnection(player.connection.getConnection());
            player.setGameMode(GameType.SURVIVAL);
            BlockPos lootPos = chest(helper, new BlockPos(6, 1, 6));
            ChestBlockEntity loot = (ChestBlockEntity) level.getBlockEntity(lootPos);
            loot.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON, 7L);
            state.setJournalProgress(JournalProgressRecord.empty().withPending(builtPlace(level, 1, lootPos.offset(0, 0, 220))));

            helper.assertTrue(!UncannyJournalMaps.onFirstLootOpen(player, loot, lootPos, StarterMapTier.PLAYER_CHEST_OR_BARREL)
                            && loot.getLootTable() != null,
                    "Before the last tier, loot chests are left alone");
            // Spectators are refused too, but a GameTest mock player always reports isSpectator() == false: checked in game.
            helper.assertTrue(UncannyJournalMaps.onFirstLootOpen(player, loot, lootPos, StarterMapTier.ANY_UNOPENED_CONTAINER),
                    "At the last tier, the next fresh loot chest holds the map");
            helper.assertTrue(loot.getLootTable() == null && countMaps(loot) == 1 && countNonMaps(loot) > 0,
                    "The map lies among the rolled loot");
            BlockPos opened = chest(helper, new BlockPos(9, 1, 6));
            helper.assertTrue(!UncannyJournalMaps.onFirstLootOpen(player, (ChestBlockEntity) level.getBlockEntity(opened),
                            opened, StarterMapTier.ANY_UNOPENED_CONTAINER),
                    "A chest without pending loot is not a fresh container");
        } finally {
            state.setJournalProgress(saved);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void anAwakePlayerNearbyWatchesTheContainer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos chestPos = chest(helper, new BlockPos(8, 1, 8));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        player.moveTo(chestPos.getX() + 3.5D, chestPos.getY(), chestPos.getZ() + 0.5D);
        helper.assertTrue(UncannyJournalMaps.isObserved(level, chestPos), "An awake player three blocks away is watching");
        helper.succeed();
    }

    private static PendingPlace builtPlace(ServerLevel level, int volume, BlockPos pos) {
        return new PendingPlace(volume, level.dimension().location().toString(), pos.asLong(), PlaceStatus.BUILT, 0L);
    }

    private static BlockPos chest(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative.below(), Blocks.STONE);
        helper.setBlock(relative, Blocks.CHEST);
        return helper.absolutePos(relative);
    }

    private static int countMaps(Container container) {
        int maps = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).is(Items.FILLED_MAP)) {
                maps++;
            }
        }
        return maps;
    }

    private static int countNonMaps(Container container) {
        int items = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && !stack.is(Items.FILLED_MAP)) {
                items++;
            }
        }
        return items;
    }
}
