package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyBlurEntity;
import com.eotv.echoofthevoid.event.special.BlurRules;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Blur?'s flurry (user, 2026-10-09): three or four light blows, never fatal, then it is gone. */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class BlurStrikeGameTests {
    private static final String TEMPLATE = "special_test_room";

    private BlurStrikeGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 520, batch = "blur_strike")
    public static void itLandsAFewLightBlowsThenVanishes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Difficulty previous = level.getDifficulty();
        level.getServer().setDifficulty(Difficulty.HARD, true);
        // Rain makes it sink at once (its weakness): this test is about the flurry.
        level.setWeatherParameters(6000, 0, false, false);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        player.setGameMode(GameType.SURVIVAL);
        BlockPos centre = helper.absolutePos(new BlockPos(8, 1, 8));
        player.moveTo(centre.getX() + 0.5D, centre.getY(), centre.getZ() + 0.5D, 0.0F, 0.0F);
        player.setOnGround(true);
        player.setHealth(9.0F);

        UncannyBlurEntity blur = UncannyEntityRegistry.UNCANNY_BLUR.get().create(level);
        BlockPos start = helper.absolutePos(new BlockPos(3, 1, 3));
        blur.moveTo(start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D, 0.0F, 0.0F);
        blur.setup(player, BlurRules.MIN_LIFETIME_TICKS);
        level.addFreshEntity(blur);
        // A new player is protected for 60 ticks after joining: the flurry starts once it can land.
        helper.runAfterDelay(65, blur::strikeNow);

        float[] lowest = {player.getHealth()};
        float[] biggestBlow = {0.0F};
        boolean[] fled = {false};
        helper.onEachTick(() -> {
            // A mock player is never ticked, so its invulnerability frames would never run out.
            player.invulnerableTime = 0;
            player.setOnGround(true);
            float health = player.getHealth();
            biggestBlow[0] = Math.max(biggestBlow[0], lowest[0] - health);
            lowest[0] = Math.min(lowest[0], health);
            fled[0] |= !blur.isRemoved() && blur.isFleeing();
        });
        helper.runAfterDelay(500, () -> {
            int dealt = blur.strikesDealt();
            helper.assertTrue(dealt >= 3 && dealt <= 4, "Three or four blows, dealt " + dealt + " (" + blur.describeState() + ")");
            helper.assertTrue(lowest[0] >= BlurRules.MIN_HEALTH_LEFT, "Never below two hearts: " + lowest[0]);
            helper.assertTrue(lowest[0] < 9.0F, "The blows must actually land");
            helper.assertTrue(biggestBlow[0] <= BlurRules.STRIKE_DAMAGE + 0.01F,
                    "A light blow, not scaled by Hard: " + biggestBlow[0]);
            helper.assertTrue(fled[0], "After the flurry it runs away");
            helper.assertTrue(blur.isRemoved(), "Then it is simply gone");
            level.getServer().setDifficulty(previous, true);
            player.setGameMode(GameType.SPECTATOR);
            helper.succeed();
        });
    }
}
