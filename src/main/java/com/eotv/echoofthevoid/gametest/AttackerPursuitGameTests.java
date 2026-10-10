package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyStalkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * Live QA 2026-10-09 (user): an Attacker? spawned far away by the Flash event went into hiding (its
 * path search could not reach that far) and lurked around the player for thirty seconds without ever
 * attacking. Hidden or not, a reachable target must be attacked at once.
 */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class AttackerPursuitGameTests {
    private static final String TEMPLATE = "special_test_room";

    private AttackerPursuitGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "attacker_pursuit")
    public static void aHiddenAttackerAttacksAsSoonAsItsTargetIsReachable(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
        for (ServerPlayer other : level.players()) {
            other.setGameMode(GameType.SPECTATOR);
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        player.setGameMode(GameType.SURVIVAL);
        BlockPos spot = helper.absolutePos(new BlockPos(12, 1, 12));
        player.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, 0.0F, 0.0F);

        UncannyStalkerEntity attacker = UncannyEntityRegistry.UNCANNY_STALKER.get().create(level);
        BlockPos start = helper.absolutePos(new BlockPos(3, 1, 3));
        attacker.moveTo(start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D, 0.0F, 0.0F);
        attacker.setHuntTarget(player);
        // The state it was left in after its far approach: thirty seconds of hiding ahead.
        CompoundTag state = new CompoundTag();
        attacker.saveWithoutId(state);
        state.putInt("HiddenTicks", 20 * 30);
        attacker.load(state);
        level.addFreshEntity(attacker);

        float[] startHealth = {-1.0F};
        boolean[] hit = {false};
        helper.onEachTick(() -> {
            // Mock players are never ticked: reset invulnerability frames and keep the target still.
            player.invulnerableTime = 0;
            player.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, 0.0F, 0.0F);
            if (startHealth[0] < 0.0F) {
                startHealth[0] = player.getHealth();
            }
            hit[0] |= player.getHealth() < startHealth[0] - 0.01F;
        });
        // A new player is protected for 60 ticks; the hidden wait would have lasted 600.
        helper.runAtTickTime(180, () -> {
            helper.assertTrue(hit[0], "A reachable target must be attacked long before the thirty-second hiding ends");
            attacker.discard();
            player.setGameMode(GameType.SPECTATOR);
            helper.succeed();
        });
    }

    /** User, 2026-10-10: the Attacker? stopped at the shore. It walks in, dives and strikes like the old friend. */
    @GameTest(template = TEMPLATE, timeoutTicks = 320, batch = "attacker_swim")
    public static void anAttackerSwimsDownToATargetInTheWater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                for (int y = 1; y <= 5; y++) {
                    // A bank on the west side, a five-deep pool everywhere else.
                    helper.setBlock(x, y, z, x <= 3 ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState());
                }
            }
        }
        for (ServerPlayer other : level.players()) {
            other.setGameMode(GameType.SPECTATOR);
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        player.setGameMode(GameType.SURVIVAL);
        BlockPos spot = helper.absolutePos(new BlockPos(11, 1, 8));
        player.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, 0.0F, 0.0F);

        UncannyStalkerEntity attacker = UncannyEntityRegistry.UNCANNY_STALKER.get().create(level);
        BlockPos start = helper.absolutePos(new BlockPos(2, 6, 8));
        attacker.moveTo(start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D, 0.0F, 0.0F);
        attacker.setHuntTarget(player);
        level.addFreshEntity(attacker);
        double startY = attacker.getY();

        float[] startHealth = {-1.0F};
        boolean[] hit = {false};
        boolean[] dived = {false};
        helper.onEachTick(() -> {
            player.invulnerableTime = 0;
            player.setAirSupply(player.getMaxAirSupply());
            player.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, 0.0F, 0.0F);
            if (startHealth[0] < 0.0F) {
                startHealth[0] = player.getHealth();
            }
            hit[0] |= player.getHealth() < startHealth[0] - 0.01F;
            dived[0] |= attacker.isInWater() && attacker.getY() <= startY - 3.0D;
        });
        helper.runAtTickTime(300, () -> {
            helper.assertTrue(dived[0], "It must walk into the water and dive, not wait on the bank; y " + attacker.getY());
            helper.assertTrue(hit[0] || attacker.distanceTo(player) <= 2.5D,
                    "It must reach and strike its target at the bottom; distance " + attacker.distanceTo(player));
            attacker.discard();
            player.setGameMode(GameType.SPECTATOR);
            helper.succeed();
        });
    }
}
