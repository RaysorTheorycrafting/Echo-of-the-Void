package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyPercherEntity;
import com.eotv.echoofthevoid.event.special.PercherSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Percher?: where it may stand, and that it leaves exactly like a Watcher? when recognised or hit. */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class PercherGameTests {
    private static final String TEMPLATE = "special_test_room";

    private PercherGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void perchesAreTheTopsOfEdgesOpenToTheSkyNotTheMiddleOfAFloor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
        for (int y = 1; y <= 5; y++) {
            helper.setBlock(new BlockPos(8, y, 8), Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        }
        // The test room is roofed with barriers: open it so sky and heightmap are those of the outdoors.
        for (int x = -1; x <= 16; x++) {
            for (int z = -1; z <= 16; z++) {
                for (int y = 8; y <= 10; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
        helper.runAfterDelay(5, () -> {
            BlockPos top = helper.absolutePos(new BlockPos(8, 6, 8));
            helper.assertTrue(PercherSystem.isPerchable(level, top),
                    "The top of a lone column of leaves is a perch; sky=" + level.canSeeSky(top)
                            + " below=" + level.getBlockState(top.below())
                            + " feet=" + level.getBlockState(top) + " head=" + level.getBlockState(top.above())
                            + " hTop=" + level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, top.getX(), top.getZ())
                            + " hSide=" + level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, top.getX() + 1, top.getZ()));
            helper.assertTrue(!PercherSystem.isPerchable(level, helper.absolutePos(new BlockPos(3, 1, 3))),
                    "The middle of a flat floor is not a perch");
            helper.assertTrue(!PercherSystem.isPerchable(level, helper.absolutePos(new BlockPos(8, 3, 8))),
                    "Inside the column is not a perch");
            helper.succeed();
        });
    }

    /** Regression (live QA 2026-10-09): random sampling missed one-column perches almost every time. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void aLonePillarIsAlwaysFoundAsTheOnlyPerch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        pillar(helper);
        for (int x = -1; x <= 16; x++) {
            for (int z = -1; z <= 16; z++) {
                for (int y = 8; y <= 10; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
        ServerPlayer player = mockPlayer(helper);
        // Centre of the room: the barrier walls (test-only perches) stay outside the 2-6 block ring.
        Vec3 centre = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(8, 1, 8)));
        player.moveTo(centre.x, centre.y, centre.z, 0.0F, 0.0F);
        helper.runAfterDelay(5, () -> {
            BlockPos expected = helper.absolutePos(new BlockPos(11, 6, 11));
            for (int run = 0; run < 5; run++) {
                BlockPos found = PercherSystem.findPerch(level, player, 2.0D, 6.0D, false);
                helper.assertTrue(expected.equals(found), "Run " + run + " found " + found + " instead of " + expected);
            }
            player.setGameMode(GameType.SPECTATOR);
            helper.succeed();
        });
    }

    /** Live QA 2026-10-09 (user): it chose a hill while a roof and trees stood around the base. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void aBuiltPerchIsPreferredToAHillAtTheSameDistance(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        pillar(helper);
        // Same height and distance from the centre as the stone "hill" pillar, but built of planks.
        for (int y = 1; y <= 5; y++) {
            helper.setBlock(new BlockPos(5, y, 5), Blocks.OAK_PLANKS);
        }
        for (int x = -1; x <= 16; x++) {
            for (int z = -1; z <= 16; z++) {
                for (int y = 8; y <= 10; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
        ServerPlayer player = mockPlayer(helper);
        Vec3 centre = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(8, 1, 8)));
        player.moveTo(centre.x, centre.y, centre.z, 0.0F, 0.0F);
        helper.runAfterDelay(5, () -> {
            BlockPos roof = helper.absolutePos(new BlockPos(5, 6, 5));
            for (int run = 0; run < 8; run++) {
                BlockPos found = PercherSystem.findPerch(level, player, 2.0D, 6.0D, false);
                helper.assertTrue(roof.equals(found), "Run " + run + " chose " + found + " instead of the built top " + roof);
            }
            player.setGameMode(GameType.SPECTATOR);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void aBlowMakesItFallBackwardAndDissolve(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        pillar(helper);
        ServerPlayer player = mockPlayer(helper);
        UncannyPercherEntity percher = perchOnPillar(helper, player);
        Vec3 feet = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 1, 3)));
        // Watched from the start: an unseen Percher? leaves at dawn, whatever the test world's time.
        helper.onEachTick(() -> face(player, feet, percher.getEyePosition()));
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(percher.isCrouching(), "A perched Percher? crouches");
            percher.hurt(level.damageSources().playerAttack(player), 4.0F);
            helper.assertTrue(percher.isFalling(), "A blow must make it fall");
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(percher.isRemoved(), "It must dissolve before or as it lands");
            player.setGameMode(GameType.SPECTATOR);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void threeSecondsOfDirectLookMakeItLeave(GameTestHelper helper) {
        pillar(helper);
        ServerPlayer player = mockPlayer(helper);
        UncannyPercherEntity percher = perchOnPillar(helper, player);
        Vec3 feet = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 1, 3)));
        helper.onEachTick(() -> face(player, feet, percher.getEyePosition()));
        helper.runAfterDelay(40, () -> helper.assertTrue(!percher.isFalling() && !percher.isRemoved(),
                "Two seconds of look are not enough"));
        helper.runAfterDelay(95, () -> {
            helper.assertTrue(percher.isRemoved(), "Three seconds of direct look must make it leave");
            player.setGameMode(GameType.SPECTATOR);
            helper.succeed();
        });
    }

    private static void pillar(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
        for (int y = 1; y <= 5; y++) {
            helper.setBlock(new BlockPos(11, y, 11), Blocks.STONE);
        }
    }

    private static UncannyPercherEntity perchOnPillar(GameTestHelper helper, ServerPlayer player) {
        UncannyPercherEntity percher = UncannyEntityRegistry.UNCANNY_PERCHER.get().create(helper.getLevel());
        BlockPos top = helper.absolutePos(new BlockPos(11, 6, 11));
        percher.moveTo(top.getX() + 0.5D, top.getY(), top.getZ() + 0.5D, 0.0F, 0.0F);
        percher.setWatchedPlayer(player);
        helper.getLevel().addFreshEntity(percher);
        return percher;
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 feet = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 1, 3)));
        player.moveTo(feet.x, feet.y, feet.z, 180.0F, 0.0F);
        return player;
    }

    private static void face(ServerPlayer player, Vec3 feet, Vec3 target) {
        Vec3 to = target.subtract(feet.add(0.0D, player.getEyeHeight(), 0.0D));
        float yaw = (float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0D);
        float pitch = (float) -Math.toDegrees(Math.atan2(to.y, Math.hypot(to.x, to.z)));
        player.moveTo(feet.x, feet.y, feet.z, yaw, pitch);
        player.setYHeadRot(yaw);
    }
}
