package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.custom.UncannyFriendEntity;
import com.eotv.echoofthevoid.event.special.OldFriendSystem;
import com.eotv.echoofthevoid.state.OldFriendRecord;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * The old friend end to end on one shared world record, so everything runs in a single test:
 * tab entry and join line, the attack with copied gear, pillaring with restoration, the Sorry sign.
 */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class OldFriendGameTests {
    private static final String TEMPLATE = "special_test_room";
    private static final UUID FRIEND = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    private OldFriendGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void oldFriendJoinsAttacksWithCopiedGearPillarsAndLeavesASign(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        OldFriendSystem.reset(level.getServer());
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
        // The target waits on a 4-block pillar: the friend has to build its way up.
        for (int y = 1; y <= 4; y++) {
            helper.setBlock(new BlockPos(8, y, 8), Blocks.STONE);
        }
        ServerPlayer target = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(target.connection.getConnection());
        target.setGameMode(GameType.SURVIVAL);
        target.setInvulnerable(true);
        target.getInventory().setItem(3, new ItemStack(Items.DIAMOND_SWORD));
        target.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        target.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        BlockPos top = helper.absolutePos(new BlockPos(8, 5, 8));
        target.moveTo(top.getX() + 0.5D, top.getY(), top.getZ() + 0.5D, 0.0F, 0.0F);
        drain(target);

        OldFriendSystem.joinForTest(target, FRIEND, "Notch", 0L);
        helper.assertTrue(sawTabEntry(target), "Joining must add the friend to the tab list like a player");
        helper.assertTrue(state.getOldFriend() != null && state.getOldFriend().stage() == OldFriendRecord.Stage.PRESENT,
                "The friend is present but not yet visible");
        helper.assertTrue(level.getEntitiesOfClass(UncannyFriendEntity.class, new AABB(top).inflate(64.0D)).isEmpty(),
                "Nothing of the friend may be seen before the attack");

        helper.assertTrue(OldFriendSystem.attackFromForTest(target, helper.absolutePos(new BlockPos(12, 1, 12))),
                "The attack must start");
        UncannyFriendEntity friend = level.getEntitiesOfClass(UncannyFriendEntity.class, new AABB(top).inflate(64.0D)).get(0);
        helper.assertTrue(friend.getMainHandItem().is(Items.DIAMOND_SWORD) && friend.getOffhandItem().is(Items.SHIELD)
                        && friend.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE),
                "The friend must carry copies of the target's sword, shield and armour");
        helper.assertTrue(target.getInventory().getItem(3).is(Items.DIAMOND_SWORD)
                        && target.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE),
                "The target keeps its own gear");
        helper.assertTrue("Notch".equals(friend.getCustomName().getString()) && friend.isCustomNameVisible(),
                "It shows the friend's name like a player's name tag");
        // Bring it next to the pillar so the test does not depend on the path across the room.
        BlockPos foot = helper.absolutePos(new BlockPos(9, 1, 8));
        friend.moveTo(foot.getX() + 0.5D, foot.getY(), foot.getZ() + 0.5D, 0.0F, 0.0F);

        helper.runAfterDelay(200, () -> {
            helper.assertTrue(friend.editCount() > 0, "It must have pillared toward a target above it");
            int cobble = countCobble(level, helper);
            helper.assertTrue(cobble > 0, "Its pillar must stand in the world while it fights");

            BlockPos death = helper.absolutePos(new BlockPos(4, 1, 4));
            target.moveTo(death.getX() + 0.5D, death.getY(), death.getZ() + 0.5D, 0.0F, 0.0F);
            OldFriendSystem.onLivingDeath(new LivingDeathEvent(target, level.damageSources().mobAttack(friend)));
            helper.assertTrue(state.getOldFriend().stage() == OldFriendRecord.Stage.DONE, "A killed target ends the event");
            helper.assertTrue(friend.isRemoved(), "The friend leaves the game");
            helper.assertTrue(countCobble(level, helper) == 0, "Its pillar is taken back when it leaves");
            BlockPos sign = findSign(level, death);
            helper.assertTrue(sign != null, "A sign must stand where the target fell");
            SignBlockEntity entity = (SignBlockEntity) level.getBlockEntity(sign);
            helper.assertTrue("Sorry".equals(entity.getFrontText().getMessage(1, false).getString()), "The sign says Sorry");

            OldFriendSystem.reset(level.getServer());
            target.setGameMode(GameType.SPECTATOR);
            helper.succeed();
        });
    }

    private static int countCobble(ServerLevel level, GameTestHelper helper) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(15, 7, 15)))) {
            if (level.getBlockState(pos).is(Blocks.COBBLESTONE)) {
                count++;
            }
        }
        return count;
    }

    private static BlockPos findSign(ServerLevel level, BlockPos around) {
        for (BlockPos pos : BlockPos.betweenClosed(around.offset(-1, 0, -1), around.offset(1, 1, 1))) {
            if (level.getBlockEntity(pos) instanceof SignBlockEntity) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static void drain(ServerPlayer player) {
        EmbeddedChannel channel = (EmbeddedChannel) player.connection.getConnection().channel();
        while (channel.readOutbound() != null) {
            // Login traffic.
        }
    }

    private static boolean sawTabEntry(ServerPlayer player) {
        EmbeddedChannel channel = (EmbeddedChannel) player.connection.getConnection().channel();
        boolean tab = false;
        boolean line = false;
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof ClientboundPlayerInfoUpdatePacket packet) {
                List<ClientboundPlayerInfoUpdatePacket.Entry> entries = packet.newEntries();
                tab |= entries.stream().anyMatch(entry -> FRIEND.equals(entry.profileId())
                        && entry.profile() != null && "Notch".equals(entry.profile().getName()) && entry.listed());
            }
            if (outbound instanceof ClientboundSystemChatPacket packet && packet.content().getString().contains("Notch")) {
                line = true;
            }
        }
        return tab && line;
    }
}
