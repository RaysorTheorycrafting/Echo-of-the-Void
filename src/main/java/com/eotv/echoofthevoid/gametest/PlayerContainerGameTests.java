package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.event.UncannyJournalPlaceSystem;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Chests and barrels placed by a player (D-100 starter map), through the real placement and break routes. */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class PlayerContainerGameTests {
    private static final String TEMPLATE = "special_test_room";

    private PlayerContainerGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void onlyPlayerPlacedContainersAreRememberedUntilBroken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        floor(helper);
        ServerPlayer player = mockPlayer(helper);
        List<BlockPos> touched = new ArrayList<>();
        try {
            BlockPos chest = place(helper, player, Items.CHEST, new BlockPos(3, 1, 3), touched);
            BlockPos barrel = place(helper, player, Items.BARREL, new BlockPos(6, 1, 3), touched);
            BlockPos trapped = place(helper, player, Items.TRAPPED_CHEST, new BlockPos(9, 1, 3), touched);
            BlockPos generated = helper.absolutePos(new BlockPos(12, 1, 3));
            touched.add(generated);
            level.setBlock(generated, Blocks.CHEST.defaultBlockState(), 3);

            for (BlockPos placed : List.of(chest, barrel, trapped)) {
                helper.assertTrue(UncannyJournalPlaceSystem.isPlayerPlacedContainer(level, placed),
                        "A container placed by a player must carry the flag at " + placed);
                helper.assertTrue(state.getPlayerPlacedContainers().contains(placed),
                        "A container placed by a player must be indexed at " + placed);
            }
            helper.assertTrue(!UncannyJournalPlaceSystem.isPlayerPlacedContainer(level, generated)
                            && !state.getPlayerPlacedContainers().contains(generated),
                    "A generated chest is never a player chest");
            CompoundTag saved = level.getBlockEntity(chest).saveWithFullMetadata(level.registryAccess());
            helper.assertTrue(saved.getCompound("NeoForgeData").getBoolean(UncannyJournalPlaceSystem.PLAYER_PLACED_FLAG),
                    "The flag must be written with the block entity");

            player.gameMode.destroyBlock(chest);
            helper.assertTrue(level.getBlockState(chest).isAir(), "The test player must break the chest");
            helper.assertTrue(!state.getPlayerPlacedContainers().contains(chest), "A broken chest is forgotten");
        } finally {
            touched.forEach(state::forgetPlayerPlacedContainer);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void starterMapContainerIsTheNearestFreePlayerChestToAnAnchor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        floor(helper);
        ServerPlayer player = mockPlayer(helper);
        List<BlockPos> touched = new ArrayList<>();
        try {
            BlockPos anchor = helper.absolutePos(new BlockPos(1, 1, 1));
            BlockPos far = place(helper, player, Items.CHEST, new BlockPos(13, 1, 13), touched);
            BlockPos near = place(helper, player, Items.CHEST, new BlockPos(4, 1, 2), touched);
            BlockPos barrel = place(helper, player, Items.BARREL, new BlockPos(2, 1, 2), touched);
            List<BlockPos> anchors = List.of(anchor);

            helper.assertTrue(near.equals(UncannyJournalPlaceSystem.findPlayerContainerAmong(level, touched, anchors, false)),
                    "Chests only: the nearest chest wins");
            helper.assertTrue(barrel.equals(UncannyJournalPlaceSystem.findPlayerContainerAmong(level, touched, anchors, true)),
                    "Once barrels are allowed, the nearer barrel wins");

            Container nearChest = (Container) level.getBlockEntity(near);
            for (int slot = 0; slot < nearChest.getContainerSize(); slot++) {
                nearChest.setItem(slot, new ItemStack(Items.COBBLESTONE));
            }
            helper.assertTrue(far.equals(UncannyJournalPlaceSystem.findPlayerContainerAmong(level, touched, anchors, false)),
                    "A full chest is skipped");

            level.setBlock(far, Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(UncannyJournalPlaceSystem.findPlayerContainerAmong(level, touched, anchors, false) == null,
                    "No free chest is left");
            helper.assertTrue(!state.getPlayerPlacedContainers().contains(far),
                    "A chest removed without a player break (explosion, command) is forgotten on the next search");

            BlockPos distantAnchor = anchor.offset(UncannyJournalPlaceSystem.STARTER_MAP_CONTAINER_RADIUS + 20, 0, 0);
            helper.assertTrue(UncannyJournalPlaceSystem.findPlayerContainerAmong(level, touched, List.of(distantAnchor), true) == null,
                    "Containers farther than the radius from every anchor are ignored");
            nearChest.clearContent();
        } finally {
            touched.forEach(state::forgetPlayerPlacedContainer);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void starterMapAnchorIsTheBedElseThePlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        ServerPlayer player = mockPlayer(helper);
        BlockPos standing = helper.absolutePos(new BlockPos(8, 1, 8));
        player.moveTo(standing.getX() + 0.5D, standing.getY(), standing.getZ() + 0.5D);
        helper.assertTrue(standing.equals(UncannyJournalPlaceSystem.starterMapAnchor(player)),
                "Without a bed, the player's position is the anchor");

        BlockPos foot = helper.absolutePos(new BlockPos(3, 1, 10));
        BlockPos head = foot.north();
        level.setBlock(foot, Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT), 3);
        level.setBlock(head, Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD), 3);
        player.setRespawnPosition(level.dimension(), head, 0.0F, false, false);
        helper.assertTrue(head.equals(UncannyJournalPlaceSystem.starterMapAnchor(player)),
                "With an Overworld bed, the bed is the anchor");

        level.setBlock(head, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(standing.equals(UncannyJournalPlaceSystem.starterMapAnchor(player)),
                "A destroyed bed falls back to the player's position");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void playerContainerIndexRoundTripsAndStaysBounded(GameTestHelper helper) {
        UncannyWorldState original = UncannyWorldState.create();
        int total = UncannyWorldState.MAX_PLAYER_PLACED_CONTAINERS + 10;
        for (int i = 0; i < total; i++) {
            original.rememberPlayerPlacedContainer(new BlockPos(i, 64, -i));
        }
        original.rememberPlayerPlacedContainer(new BlockPos(10, 64, -10));
        CompoundTag saved = original.save(new CompoundTag(), helper.getLevel().registryAccess());
        UncannyWorldState restored = UncannyWorldState.load(saved, helper.getLevel().registryAccess());
        List<BlockPos> containers = restored.getPlayerPlacedContainers();
        helper.assertTrue(containers.size() == UncannyWorldState.MAX_PLAYER_PLACED_CONTAINERS,
                "The index stays bounded: " + containers.size());
        helper.assertTrue(!containers.contains(new BlockPos(0, 64, 0)), "The oldest entries are evicted first");
        helper.assertTrue(containers.get(containers.size() - 1).equals(new BlockPos(10, 64, -10)),
                "Placing again refreshes an entry");
        helper.assertTrue(UncannyWorldState.load(new CompoundTag(), helper.getLevel().registryAccess())
                        .getPlayerPlacedContainers().isEmpty(),
                "Older worlds start with an empty index");
        helper.succeed();
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /** Places a block item through the real item route (fires the NeoForge placement event). */
    private static BlockPos place(
            GameTestHelper helper, ServerPlayer player, net.minecraft.world.item.Item item, BlockPos relative,
            List<BlockPos> touched) {
        BlockPos target = helper.absolutePos(relative);
        BlockPos support = target.below();
        touched.add(target);
        ItemStack stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support).add(0.0D, 0.5D, 0.0D), Direction.UP, support, false);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(!helper.getLevel().getBlockState(target).isAir(), "The test player must place " + item);
        return target;
    }
}
