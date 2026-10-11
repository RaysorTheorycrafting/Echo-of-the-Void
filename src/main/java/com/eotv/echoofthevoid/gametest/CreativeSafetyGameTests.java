package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.event.UncannyParanoiaEventSystem;
import com.eotv.echoofthevoid.event.UncannyWatcherSystem;
import com.eotv.echoofthevoid.event.special.ApprovedSpecialSystem;
import com.eotv.echoofthevoid.event.special.BlurSystem;
import com.eotv.echoofthevoid.event.special.PercherSystem;
import com.eotv.echoofthevoid.event.special.SleeperSystem;
import com.eotv.echoofthevoid.event.special.UncannyDevourerSystem;
import com.eotv.echoofthevoid.event.special.UncannyHuntingSpecialSystem;
import com.eotv.echoofthevoid.event.special.UncannyMinerSystem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Creatures act on survival and adventure players only: never on creative (nor spectator, checked in game). */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class CreativeSafetyGameTests {
    private static final String TEMPLATE = "special_test_room";

    private CreativeSafetyGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void noCreatureIsSpawnedForACreativePlayerEvenFromTheDevMenu(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
        ServerPlayer creative = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(creative.connection.getConnection());
        creative.getAbilities().invulnerable = true;
        helper.assertTrue(!UncannyEntityUtil.isHuntablePlayer(creative), "Creative abilities make a player unhuntable");
        helper.assertTrue(!UncannyEntityUtil.huntablePlayers(helper.getLevel()).contains(creative),
                "Creative players are left out of every creature's candidates");

        helper.assertTrue(!UncannyHuntingSpecialSystem.spawnForDebug(creative, "echoer", null), "Echoer?");
        helper.assertTrue(!UncannyHuntingSpecialSystem.spawnForDebug(creative, "flanker", null), "Flanker?");
        helper.assertTrue(!ApprovedSpecialSystem.spawnForDebug(creative, "ambusher"), "Ambusher?");
        helper.assertTrue(!ApprovedSpecialSystem.spawnForDebug(creative, "doubler"), "Doubler?");
        helper.assertTrue(!ApprovedSpecialSystem.armFerrymanEncounter(creative), "Ferryman?");
        helper.assertTrue(!UncannyWatcherSystem.forceSpawnWatcher(creative), "Watcher?");
        helper.assertTrue(!UncannyDevourerSystem.spawnDebug(creative), "Devourer?");
        helper.assertTrue(!UncannyMinerSystem.spawnDebugTunnel(creative), "Miner?");
        helper.assertTrue(!SleeperSystem.spawnNatural(creative), "Sleeper?");
        helper.assertTrue(!BlurSystem.spawnNatural(creative), "Blur?");
        helper.assertTrue(!PercherSystem.spawnNatural(creative), "Percher?");
        helper.assertTrue(!UncannyParanoiaEventSystem.spawnStalker(creative), "Attacker?");
        helper.assertTrue(!UncannyParanoiaEventSystem.spawnFollower(creative), "Follower?");
        helper.assertTrue(!UncannyParanoiaEventSystem.spawnKnocker(creative), "Knocker?");
        helper.assertTrue(!UncannyParanoiaEventSystem.spawnUsher(creative), "Usher?");
        helper.assertTrue(!UncannyParanoiaEventSystem.spawnShadow(creative), "Shadow?");
        helper.assertTrue(UncannyEntityUtil.huntableOrNull(creative) == null, "No creature resolves a creative target");

        creative.getAbilities().invulnerable = false;
        helper.assertTrue(UncannyEntityUtil.huntableOrNull(creative) == creative,
                "The same player in survival is a valid target again");
        helper.succeed();
    }
}
