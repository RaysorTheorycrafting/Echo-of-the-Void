package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.event.UncannyJournalPlaceSystem;
import com.eotv.echoofthevoid.event.UncannyStructureFeatureSystem;
import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import com.eotv.echoofthevoid.state.JournalProgressRecord;
import com.eotv.echoofthevoid.state.JournalProgressRecord.PendingPlace;
import com.eotv.echoofthevoid.state.JournalProgressRecord.PlaceStatus;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Ordinary structure chests after D-100: maps and fragments, decided once, never a journal. */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class StructureChestGameTests {
    private static final String TEMPLATE = "special_test_room";

    private StructureChestGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void firstOpeningAddsAMapTowardsTheWaitingPlaceAndFragments(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        JournalProgressRecord saved = state.getJournalProgress();
        try {
            ServerPlayer player = mockPlayer(helper);
            ChestBlockEntity withMap = structureChest(helper, new BlockPos(4, 1, 4));
            ChestBlockEntity withoutMap = structureChest(helper, new BlockPos(8, 1, 4));
            ChestBlockEntity noPlace = structureChest(helper, new BlockPos(12, 1, 4));
            state.setJournalProgress(JournalProgressRecord.empty().withFound(1, 0L)
                    .withPending(new PendingPlace(2, level.dimension().location().toString(),
                            withMap.getBlockPos().offset(400, 0, 0).asLong(), PlaceStatus.BUILT, 0L)));

            String first = UncannyStructureFeatureSystem.resolveStructureChestOnFirstOpen(player, withMap, 0.10D, 2, 0.0D);
            helper.assertTrue(first.contains("map") && first.contains("fragments=2"), "Map and fragments: " + first);
            helper.assertTrue(count(withMap, Items.FILLED_MAP) == 1, "One map lies in the chest");
            helper.assertTrue(count(withMap, UncannyItemRegistry.UNCANNY_REALITY_SHARD_PIECE.get()) == 2, "Two fragments");
            helper.assertTrue(state.getJournalProgress().volumeForMap(mapId(withMap)) == 2,
                    "The structure map leads to the waiting place");
            helper.assertTrue(!first.contains("behind-you"), "No Behind you before the sixth journal");

            String second = UncannyStructureFeatureSystem.resolveStructureChestOnFirstOpen(player, withoutMap, 0.90D, 3, 0.0D);
            helper.assertTrue(!second.contains("map") && count(withoutMap, Items.FILLED_MAP) == 0
                            && count(withoutMap, UncannyItemRegistry.UNCANNY_REALITY_SHARD_PIECE.get()) == 3,
                    "A missed map roll still gives fragments: " + second);

            state.setJournalProgress(JournalProgressRecord.empty().withFound(1, 0L));
            String third = UncannyStructureFeatureSystem.resolveStructureChestOnFirstOpen(player, noPlace, 0.0D, 1, 0.0D);
            helper.assertTrue(!third.contains("map") && count(noPlace, Items.FILLED_MAP) == 0,
                    "No map when no place is waiting: " + third);
            for (ChestBlockEntity chest : new ChestBlockEntity[] {withMap, withoutMap, noPlace}) {
                helper.assertTrue(count(chest, UncannyItemRegistry.UNCANNY_LORE_PIECE.get()) == 0,
                        "Ordinary structures never hold a journal");
            }
        } finally {
            state.setJournalProgress(saved);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void realOpeningResolvesTheChestOnlyOnce(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        JournalProgressRecord saved = state.getJournalProgress();
        try {
            state.setJournalProgress(JournalProgressRecord.empty());
            ServerPlayer player = mockPlayer(helper);
            ChestBlockEntity chest = structureChest(helper, new BlockPos(6, 1, 6));
            open(level, player, chest.getBlockPos());
            int fragments = count(chest, UncannyItemRegistry.UNCANNY_REALITY_SHARD_PIECE.get());
            helper.assertTrue(fragments >= 1 && fragments <= 3, "The first opening adds 1 to 3 fragments, got " + fragments);
            for (int again = 0; again < 5; again++) {
                open(level, player, chest.getBlockPos());
            }
            helper.assertTrue(count(chest, UncannyItemRegistry.UNCANNY_REALITY_SHARD_PIECE.get()) == fragments,
                    "Reopening never rolls the chest again");
        } finally {
            state.setJournalProgress(saved);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void aDoubleStructureChestIsResolvedOnceForBothHalves(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper);
        BlockPos left = new BlockPos(6, 1, 12);
        BlockPos right = left.east();
        helper.setBlock(left.below(), Blocks.STONE);
        helper.setBlock(right.below(), Blocks.STONE);
        helper.setBlock(left, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH)
                .setValue(ChestBlock.TYPE, ChestType.LEFT));
        helper.setBlock(right, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH)
                .setValue(ChestBlock.TYPE, ChestType.RIGHT));
        ChestBlockEntity leftChest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(left));
        ChestBlockEntity rightChest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(right));
        leftChest.getPersistentData().putBoolean(UncannyStructureFeatureSystem.STRUCTURE_CHEST_FLAG, true);
        rightChest.getPersistentData().putBoolean(UncannyStructureFeatureSystem.STRUCTURE_CHEST_FLAG, true);

        UncannyStructureFeatureSystem.resolveStructureChestOnFirstOpen(player, leftChest, 0.9D, 2, 0.9D);
        helper.assertTrue(UncannyStructureFeatureSystem.isStructureChestResolved(rightChest.getPersistentData()),
                "Opening one half decides the content of the whole double chest");
        open(level, player, rightChest.getBlockPos());
        helper.assertTrue(count(rightChest, UncannyItemRegistry.UNCANNY_REALITY_SHARD_PIECE.get()) == 0
                        && count(leftChest, UncannyItemRegistry.UNCANNY_REALITY_SHARD_PIECE.get()) == 2,
                "The second half adds nothing more");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void playerAndPlaceChestsAreNotStructureChests(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChestBlockEntity player = plainChest(helper, new BlockPos(4, 1, 10));
        player.getPersistentData().putBoolean(UncannyJournalPlaceSystem.PLAYER_PLACED_FLAG, true);
        ChestBlockEntity place = plainChest(helper, new BlockPos(8, 1, 10));
        place.getPersistentData().putInt(com.eotv.echoofthevoid.event.UncannyJournalPlaces.JOURNAL_PLACE_CHEST_FLAG, 2);
        ChestBlockEntity lore = plainChest(helper, new BlockPos(12, 1, 10));
        lore.getPersistentData().putBoolean("eotv_secret_history_chest", true);
        helper.assertTrue(!UncannyStructureFeatureSystem.isOrdinaryStructureChest(level, player), "A player chest is left alone");
        helper.assertTrue(!UncannyStructureFeatureSystem.isOrdinaryStructureChest(level, place), "A journal place chest is left alone");
        helper.assertTrue(UncannyStructureFeatureSystem.isOrdinaryStructureChest(level, lore),
                "Former lore chests of older worlds now follow the new rules");
        ChestBlockEntity unflagged = plainChest(helper, new BlockPos(14, 1, 10));
        helper.assertTrue(!UncannyStructureFeatureSystem.isOrdinaryStructureChest(level, unflagged),
                "An unflagged chest could be a player's own from an older version: it is never rolled");
        helper.succeed();
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static ChestBlockEntity structureChest(GameTestHelper helper, BlockPos relative) {
        ChestBlockEntity chest = plainChest(helper, relative);
        chest.getPersistentData().putBoolean(UncannyStructureFeatureSystem.STRUCTURE_CHEST_FLAG, true);
        return chest;
    }

    private static ChestBlockEntity plainChest(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative.below(), Blocks.STONE);
        helper.setBlock(relative, Blocks.CHEST);
        return (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(relative));
    }

    private static void open(ServerLevel level, ServerPlayer player, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        player.closeContainer();
    }

    private static int count(ChestBlockEntity chest, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static int mapId(ChestBlockEntity chest) {
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.is(Items.FILLED_MAP) && stack.get(net.minecraft.core.component.DataComponents.MAP_ID) != null) {
                return stack.get(net.minecraft.core.component.DataComponents.MAP_ID).id();
            }
        }
        return -1;
    }
}
