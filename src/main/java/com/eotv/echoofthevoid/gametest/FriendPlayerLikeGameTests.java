package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyFriendEntity;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** The old friend fights, swims and handles lava as a player would (user feedback, 2026-10-09). */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class FriendPlayerLikeGameTests {
    private static final String TEMPLATE = "special_test_room";
    private static final UUID FRIEND = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    private FriendPlayerLikeGameTests() {
    }

    /** On Hard a mob's blow is multiplied by 1.5; a player's sword never is. */
    @GameTest(template = TEMPLATE, timeoutTicks = 120, batch = "friend_hard")
    public static void itsSwordHitsLikeAPlayersOnHard(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        Difficulty previous = level.getDifficulty();
        level.getServer().setDifficulty(Difficulty.HARD, true);
        // Its target stays out of reach so that the only blow is the measured one.
        ServerPlayer target = player(helper, new BlockPos(14, 1, 14));
        target.setInvulnerable(true);
        target.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        ServerPlayer victim = player(helper, new BlockPos(2, 1, 3));
        UncannyFriendEntity friend = friend(helper, target, new BlockPos(2, 1, 2));
        // A new player is protected for 60 ticks after joining (ServerPlayer.spawnInvulnerableTime).
        helper.runAfterDelay(70, () -> {
            victim.setHealth(20.0F);
            victim.invulnerableTime = 0;
            friend.setOnGround(true);
            friend.resetFallDistance();
            friend.doHurtTarget(victim);
            float lost = 20.0F - victim.getHealth();
            helper.assertTrue(Math.abs(lost - 7.0F) < 0.01F,
                    "A diamond sword deals 7 without armour, whatever the difficulty; lost " + lost);
            level.getServer().setDifficulty(previous, true);
            friend.discard();
            target.setGameMode(GameType.SPECTATOR);
            victim.setGameMode(GameType.SPECTATOR);
            helper.succeed();
        });
    }

    /** A pool: the friend starts at the surface and its target waits at the bottom. */
    @GameTest(template = TEMPLATE, timeoutTicks = 160, batch = "friend_swim")
    public static void itDivesAfterATargetUnderwater(GameTestHelper helper) {
        floor(helper);
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2, 1, 2), new BlockPos(13, 6, 13))) {
            helper.setBlock(pos, Blocks.WATER);
        }
        ServerPlayer target = player(helper, new BlockPos(8, 1, 11));
        target.setInvulnerable(true);
        UncannyFriendEntity friend = friend(helper, target, new BlockPos(8, 6, 5));
        double startY = friend.getY();
        boolean[] swam = {false};
        helper.onEachTick(() -> swam[0] |= friend.getPose() == Pose.SWIMMING);
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(friend.getY() <= startY - 2.5D,
                    "It must dive toward a target below instead of bobbing at the surface: y " + friend.getY() + " from " + startY);
            helper.assertTrue(swam[0], "Underwater and sprinting, it swims flat like a player");
            friend.discard();
            target.setGameMode(GameType.SPECTATOR);
            helper.succeed();
        });
    }

    /** Lava poured on its head: it scoops the source with a bucket. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "friend_lava")
    public static void itScoopsLavaPouredOnItsHead(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        ServerPlayer target = player(helper, new BlockPos(14, 1, 14));
        target.setInvulnerable(true);
        UncannyFriendEntity friend = friend(helper, target, new BlockPos(3, 1, 3));
        BlockPos head = helper.absolutePos(new BlockPos(3, 2, 3));
        boolean[] bucket = {false};
        helper.runAfterDelay(2, () -> level.setBlock(head, Blocks.LAVA.defaultBlockState(), 3));
        helper.onEachTick(() -> bucket[0] |= friend.getMainHandItem().is(Items.LAVA_BUCKET));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(!level.getFluidState(head).is(Fluids.LAVA) || !level.getFluidState(head).isSource(),
                    "The lava source must have been scooped");
            helper.assertTrue(bucket[0], "It holds the filled bucket like a player");
            friend.discard();
            target.setGameMode(GameType.SPECTATOR);
            for (BlockPos pos : BlockPos.betweenClosed(head.offset(-3, -1, -3), head.offset(3, 2, 3))) {
                if (level.getFluidState(pos).is(Fluids.LAVA)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            helper.succeed();
        });
    }

    /** Pillaring up to a target: cobblestone in hand and the head down toward its feet. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "friend_pillar")
    public static void itPillarsWithBlocksInHandLookingDown(GameTestHelper helper) {
        floor(helper);
        for (int y = 1; y <= 4; y++) {
            helper.setBlock(new BlockPos(8, y, 8), Blocks.STONE);
        }
        ServerPlayer target = player(helper, new BlockPos(8, 5, 8));
        target.setInvulnerable(true);
        UncannyFriendEntity friend = friend(helper, target, new BlockPos(9, 1, 8));
        boolean[] blocksInHand = {false};
        boolean[] lookingDown = {false};
        helper.onEachTick(() -> {
            blocksInHand[0] |= friend.getMainHandItem().is(Items.COBBLESTONE);
            lookingDown[0] |= friend.getMainHandItem().is(Items.COBBLESTONE) && friend.getXRot() > 60.0F;
        });
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(friend.editCount() > 0, "It must have pillared");
            helper.assertTrue(blocksInHand[0], "It holds the blocks it places");
            helper.assertTrue(lookingDown[0], "It looks down at its feet while placing");
            friend.discard();
            target.setGameMode(GameType.SPECTATOR);
            helper.succeed();
        });
    }

    /** Hurt and hungry with room to breathe, it eats a meal and its hunger bar fills (user, 2026-10-09). */
    @GameTest(template = TEMPLATE, timeoutTicks = 120, batch = "friend_eat")
    public static void itEatsToHealWhenHurtAndHungry(GameTestHelper helper) {
        floor(helper);
        ServerPlayer target = player(helper, new BlockPos(13, 1, 13));
        target.setInvulnerable(true);
        UncannyFriendEntity friend = friend(helper, target, new BlockPos(2, 1, 2));
        friend.setHealth(8.0F);
        friend.hunger().restore(12, 0.0F, 0.0F);
        boolean[] ate = {false};
        helper.onEachTick(() -> ate[0] |= friend.isEating());
        helper.runAfterDelay(70, () -> {
            helper.assertTrue(ate[0], "Hurt and hungry with the target far away, it must start a meal");
            helper.assertTrue(friend.hunger().food() > 12,
                    "The finished meal must feed its hunger bar; food " + friend.hunger().food());
            friend.discard();
            target.setGameMode(GameType.SPECTATOR);
            helper.succeed();
        });
    }

    /** Leaves over its head no longer freeze its pillar: it cuts through them as a player does. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "friend_leaves")
    public static void itCutsThroughLeavesToPillarUp(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(new BlockPos(8, 3, 8), Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        helper.setBlock(new BlockPos(8, 3, 9), Blocks.STONE);
        helper.setBlock(new BlockPos(8, 2, 9), Blocks.STONE);
        helper.setBlock(new BlockPos(8, 1, 9), Blocks.STONE);
        ServerPlayer target = player(helper, new BlockPos(8, 4, 9));
        target.setInvulnerable(true);
        UncannyFriendEntity friend = friend(helper, target, new BlockPos(8, 1, 8));
        double startY = friend.getY();
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(8, 3, 8)).isAir() || friend.getY() >= startY + 0.9D,
                    "Under leaves it must cut through or climb, not freeze; y " + friend.getY() + " from " + startY);
            friend.discard();
            target.setGameMode(GameType.SPECTATOR);
            helper.succeed();
        });
    }

    /** Started at the very edge of a block, it lines up in the middle and pillars straight up. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "friend_center")
    public static void itCentersItselfBeforePillaring(GameTestHelper helper) {
        floor(helper);
        for (int y = 1; y <= 4; y++) {
            helper.setBlock(new BlockPos(8, y, 9), Blocks.STONE);
        }
        ServerPlayer target = player(helper, new BlockPos(8, 5, 9));
        target.setInvulnerable(true);
        UncannyFriendEntity friend = friend(helper, target, new BlockPos(8, 1, 8));
        BlockPos column = helper.absolutePos(new BlockPos(8, 1, 8));
        // At the edge of its column, its body overlapping the next one (user, 2026-10-09).
        friend.moveTo(column.getX() + 0.88D, column.getY(), column.getZ() + 0.5D, 0.0F, 0.0F);
        double startY = friend.getY();
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(friend.getY() >= startY + 1.9D,
                    "It must line up and pillar at least two blocks; y " + friend.getY() + " from " + startY);
            helper.assertTrue(helper.getBlockState(new BlockPos(8, 1, 8)).is(Blocks.COBBLESTONE),
                    "Its first block goes straight under its starting column, not beside it");
            friend.discard();
            target.setGameMode(GameType.SPECTATOR);
            helper.succeed();
        });
    }

    /** A target that walls itself in is dug out; the wall comes back once the friend is gone. */
    @GameTest(template = TEMPLATE, timeoutTicks = 260, batch = "friend_wall")
    public static void itDigsThroughAWallBuiltAgainstIt(GameTestHelper helper) {
        floor(helper);
        // A one-block cell of planks (player-made blocks) around the target, roof included.
        BlockPos cell = new BlockPos(8, 1, 10);
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(7, 1, 9), new BlockPos(9, 3, 11))) {
            if (!pos.equals(cell) && !pos.equals(cell.above())) {
                helper.setBlock(pos, Blocks.OAK_PLANKS);
            }
        }
        ServerPlayer target = player(helper, cell);
        target.setInvulnerable(true);
        UncannyFriendEntity friend = friend(helper, target, new BlockPos(8, 1, 6));
        boolean[] breached = {false};
        helper.onEachTick(() -> breached[0] |= helper.getBlockState(new BlockPos(8, 1, 9)).isAir()
                || helper.getBlockState(new BlockPos(8, 2, 9)).isAir());
        helper.runAfterDelay(220, () -> {
            helper.assertTrue(breached[0], "It must dig through the planks between it and its target");
            friend.discard();
            helper.assertTrue(helper.getBlockState(new BlockPos(8, 1, 9)).is(Blocks.OAK_PLANKS)
                            && helper.getBlockState(new BlockPos(8, 2, 9)).is(Blocks.OAK_PLANKS),
                    "Every block it broke is put back once it is gone");
            target.setGameMode(GameType.SPECTATOR);
            helper.succeed();
        });
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
    }

    private static ServerPlayer player(GameTestHelper helper, BlockPos relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(relative);
        player.moveTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        return player;
    }

    private static UncannyFriendEntity friend(GameTestHelper helper, ServerPlayer target, BlockPos relative) {
        UncannyFriendEntity friend = UncannyEntityRegistry.UNCANNY_FRIEND.get().create(helper.getLevel());
        BlockPos at = helper.absolutePos(relative);
        friend.moveTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        friend.setup(FRIEND, "Notch", target);
        helper.getLevel().addFreshEntity(friend);
        return friend;
    }
}
