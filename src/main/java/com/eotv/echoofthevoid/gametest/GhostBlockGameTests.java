package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.event.paranoia.nativeevent.GhostBlockSystem;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Ghost blocks: what the client is told, and that the server block never changes. */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class GhostBlockGameTests {
    private static final String TEMPLATE = "special_test_room";

    private GhostBlockGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void minedBlockReturnsOnlyOnTheClientAndVanishesWhenTouched(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        buildTunnel(helper);
        // Let the light engine settle under the new roof, as in a real tunnel.
        helper.runAfterDelay(10, () -> {
            ServerPlayer player = mockPlayer(helper);
            BlockPos mined = helper.absolutePos(new BlockPos(3, 1, 8));
            BlockState stone = level.getBlockState(mined);
            helper.assertTrue(stone.is(Blocks.STONE), "Precondition: the tunnel face is stone");
            GhostBlockSystem.onBlockBreak(new BlockEvent.BreakEvent(level, mined, stone, player));
            level.setBlock(mined, Blocks.AIR.defaultBlockState(), 3);

            // Eight blocks down the tunnel, facing away from the mined block.
            Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(11, 1, 8)));
            Vec3 away = stand.add(stand.subtract(Vec3.atCenterOf(mined)));
            face(player, stand, away);
            drain(player);
            helper.assertTrue(GhostBlockSystem.trigger(player, GhostBlockSystem.Kind.RESTORED, false),
                    "A block mined out of sight must be restorable");
            helper.assertTrue(lastUpdate(player, mined) != null && lastUpdate(player, mined).is(Blocks.STONE),
                    "The client must be told the stone is back");
            helper.assertTrue(level.getBlockState(mined).isAir(), "The server block must stay mined");

            Vec3 close = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 1, 8)));
            face(player, close, Vec3.atCenterOf(mined));
            helper.runAfterDelay(6, () -> {
                BlockState resent = lastUpdate(player, mined);
                helper.assertTrue(resent != null && resent.isAir(), "Touching the ghost must send the true air back");
                helper.assertTrue(GhostBlockSystem.ghostCount(player.getUUID()) == 0, "The ghost must be gone");
                player.setGameMode(GameType.SPECTATOR);
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void hittingAGhostBlockEndsItAtOnce(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        buildTunnel(helper);
        // Let the light engine settle under the new roof, as in a real tunnel.
        helper.runAfterDelay(10, () -> {
            ServerPlayer player = mockPlayer(helper);
            BlockPos mined = helper.absolutePos(new BlockPos(3, 1, 8));
            GhostBlockSystem.onBlockBreak(new BlockEvent.BreakEvent(level, mined, level.getBlockState(mined), player));
            level.setBlock(mined, Blocks.AIR.defaultBlockState(), 3);
            Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(11, 1, 8)));
            face(player, stand, stand.add(stand.subtract(Vec3.atCenterOf(mined))));
            helper.assertTrue(GhostBlockSystem.trigger(player, GhostBlockSystem.Kind.RESTORED, false), "Ghost expected");
            drain(player);

            GhostBlockSystem.onLeftClickBlock(new PlayerInteractEvent.LeftClickBlock(
                    player, mined, Direction.EAST, PlayerInteractEvent.LeftClickBlock.Action.START));
            BlockState resent = lastUpdate(player, mined);
            helper.assertTrue(resent != null && resent.isAir(), "The first hit must reveal the truth");
            helper.assertTrue(GhostBlockSystem.ghostCount(player.getUUID()) == 0, "The ghost must be gone");
            player.setGameMode(GameType.SPECTATOR);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void missingWallBlockComesBackOneSecondAfterBeingLookedAt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        buildRoom(helper);
        // Let the light engine settle under the new roof before reading the sky.
        helper.runAfterDelay(10, () -> {
            ServerPlayer player = mockPlayer(helper);
            Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(5, 1, 8)));
            BlockPos wallProbe = helper.absolutePos(new BlockPos(10, 2, 8));
            face(player, stand, stand.add(stand.subtract(Vec3.atCenterOf(wallProbe))));
            drain(player);
            helper.assertTrue(GhostBlockSystem.trigger(player, GhostBlockSystem.Kind.MISSING, true),
                    "An outer wall block of a roofed room must be hideable");
            BlockPos hidden = hiddenPosition(player);
            helper.assertTrue(hidden != null, "The client must be told one wall block is air");
            helper.assertTrue(level.getBlockState(hidden).is(Blocks.STONE), "The server wall must stay whole");

            face(player, stand, Vec3.atCenterOf(hidden));
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(GhostBlockSystem.ghostCount(player.getUUID()) == 1,
                        "Half a second after the look the hole must still be there");
            });
            helper.runAfterDelay(30, () -> {
                BlockState resent = lastUpdate(player, hidden);
                helper.assertTrue(resent != null && resent.is(Blocks.STONE), "One second after the look the wall must be back");
                helper.assertTrue(GhostBlockSystem.ghostCount(player.getUUID()) == 0, "The ghost must be gone");
                player.setGameMode(GameType.SPECTATOR);
                helper.succeed();
            });
        });
    }

    /** A stone corridor along x, three blocks high, roofed so that it counts as underground. */
    private static void buildTunnel(GameTestHelper helper) {
        for (int x = 1; x <= 14; x++) {
            for (int z = 6; z <= 10; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 4, z), Blocks.STONE);
                for (int y = 1; y <= 3; y++) {
                    boolean wall = z == 6 || z == 10 || x == 1 || x == 3 && y <= 2;
                    helper.setBlock(new BlockPos(x, y, z), wall ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        for (int z = 7; z <= 9; z++) {
            helper.setBlock(new BlockPos(3, 1, z), Blocks.STONE);
            helper.setBlock(new BlockPos(3, 2, z), Blocks.STONE);
        }
    }

    /** A roofed room x 2..9 whose east wall (x = 10) opens onto the open air. */
    private static void buildRoom(GameTestHelper helper) {
        for (int x = 1; x <= 14; x++) {
            for (int z = 4; z <= 12; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
        for (int x = 1; x <= 10; x++) {
            for (int z = 4; z <= 12; z++) {
                boolean edge = x == 1 || x == 10 || z == 4 || z == 12;
                for (int y = 1; y <= 3; y++) {
                    if (edge) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                    }
                }
                helper.setBlock(new BlockPos(x, 4, z), Blocks.STONE);
            }
        }
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        return player;
    }

    private static void face(ServerPlayer player, Vec3 feet, Vec3 target) {
        Vec3 eye = feet.add(0.0D, player.getEyeHeight(), 0.0D);
        Vec3 to = target.subtract(eye);
        float yaw = (float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0D);
        float pitch = (float) -Math.toDegrees(Math.atan2(to.y, Math.hypot(to.x, to.z)));
        player.moveTo(feet.x, feet.y, feet.z, yaw, pitch);
        player.setYHeadRot(yaw);
    }

    private static void drain(ServerPlayer player) {
        EmbeddedChannel channel = (EmbeddedChannel) player.connection.getConnection().channel();
        while (channel.readOutbound() != null) {
            // Earlier traffic is irrelevant.
        }
    }

    private static final java.util.Map<java.util.UUID, List<ClientboundBlockUpdatePacket>> SEEN = new java.util.HashMap<>();

    private static List<ClientboundBlockUpdatePacket> blockUpdates(ServerPlayer player) {
        List<ClientboundBlockUpdatePacket> seen = SEEN.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>());
        EmbeddedChannel channel = (EmbeddedChannel) player.connection.getConnection().channel();
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof ClientboundBlockUpdatePacket packet) {
                seen.add(packet);
            }
        }
        return seen;
    }

    private static BlockState lastUpdate(ServerPlayer player, BlockPos pos) {
        BlockState last = null;
        for (ClientboundBlockUpdatePacket packet : blockUpdates(player)) {
            if (packet.getPos().equals(pos)) {
                last = packet.getBlockState();
            }
        }
        return last;
    }

    private static BlockPos hiddenPosition(ServerPlayer player) {
        for (ClientboundBlockUpdatePacket packet : blockUpdates(player)) {
            if (packet.getBlockState().isAir() && player.serverLevel().getBlockState(packet.getPos()).is(Blocks.STONE)) {
                return packet.getPos();
            }
        }
        return null;
    }
}
