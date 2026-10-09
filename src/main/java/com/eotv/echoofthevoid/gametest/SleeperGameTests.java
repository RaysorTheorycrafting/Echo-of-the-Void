package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannySleeperEntity;
import com.eotv.echoofthevoid.event.special.SleeperPins;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Sleeper? on a real bed: the warning, the pin, the bite through iron armour, then the fight. */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class SleeperGameTests {
    private static final String TEMPLATE = "special_test_room";

    private SleeperGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 320, batch = "sleeper_night")
    public static void warnsOncePinsTheSleeperBitesThenFights(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Its own batch: a Sleeper? sinks at dawn and a player cannot stay asleep in daylight.
        long previousTime = level.getDayTime();
        level.setDayTime(18000L);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
        BlockPos foot = new BlockPos(8, 1, 9);
        BlockPos head = foot.north();
        helper.setBlock(foot, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH)
                .setValue(BedBlock.PART, BedPart.FOOT));
        helper.setBlock(head, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH)
                .setValue(BedBlock.PART, BedPart.HEAD));
        BlockPos headAbs = helper.absolutePos(head);

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        player.setGameMode(GameType.SURVIVAL);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            player.setItemSlot(slot, new ItemStack(switch (slot) {
                case HEAD -> Items.IRON_HELMET;
                case CHEST -> Items.IRON_CHESTPLATE;
                case LEGS -> Items.IRON_LEGGINGS;
                default -> Items.IRON_BOOTS;
            }));
        }
        // Mock players are never ticked, so worn armour never reaches the attribute: set what iron gives.
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(15.0D);
        player.setHealth(player.getMaxHealth());

        BlockPos besideBed = helper.absolutePos(new BlockPos(9, 1, 8));
        player.moveTo(besideBed.getX() + 0.5D, besideBed.getY(), besideBed.getZ() + 0.5D, 90.0F, 0.0F);

        // Within Vanilla's 8-block "monsters nearby" box around the bed, where it usually waits.
        UncannySleeperEntity sleeper = UncannyEntityRegistry.UNCANNY_SLEEPER.get().create(level);
        BlockPos corner = helper.absolutePos(new BlockPos(3, 1, 3));
        sleeper.moveTo(corner.getX() + 0.5D, corner.getY(), corner.getZ() + 0.5D, 0.0F, 0.0F);
        sleeper.setupTarget(player, 20 * 600);
        level.addFreshEntity(sleeper);

        // The real path: Vanilla checks, then the NeoForge event, then sleeping (night needs a tick to apply).
        helper.runAfterDelay(2, () -> {
            var first = player.startSleepInBed(headAbs);
            helper.assertTrue(first.left().orElse(null) == Player.BedSleepingProblem.OTHER_PROBLEM,
                    "The first attempt must be refused by the warning, not by Vanilla: " + first.left().orElse(null));
            var second = player.startSleepInBed(headAbs);
            helper.assertTrue(second.right().isPresent(),
                    "The warning is given once; the next attempt sleeps: " + second.left().orElse(null));
            helper.assertTrue(player.isSleeping(), "The player is asleep");
        });
        helper.runAfterDelay(5, () -> helper.assertTrue(sleeper.state() == UncannySleeperEntity.State.IDLE,
                "It waits half a second before running"));

        boolean[] sawPin = {false};
        float[] afterBite = {-1.0F};
        boolean[] asleepAfterBite = {true};
        boolean[] pinnedAfterBite = {true};
        helper.onEachTick(() -> {
            UncannySleeperEntity.State state = sleeper.state();
            if (state == UncannySleeperEntity.State.RECOVER && afterBite[0] < 0.0F) {
                afterBite[0] = player.isAlive() ? player.getHealth() : 0.0F;
                asleepAfterBite[0] = player.isSleeping();
                pinnedAfterBite[0] = SleeperPins.isPinned(player);
            }
            if ((state == UncannySleeperEntity.State.MOUNT || state == UncannySleeperEntity.State.MOUTH) && !sawPin[0]) {
                sawPin[0] = true;
                player.stopSleepInBed(false, true);
                helper.assertTrue(player.isSleeping(), "A pinned sleeper cannot leave the bed");
                helper.assertTrue(!player.isSleepingLongEnough(), "A pinned sleeper cannot end the night");
                helper.assertTrue(SleeperPins.isPinned(player), "The pin is registered");
            }
        });

        helper.runAfterDelay(205, () -> {
            helper.assertTrue(sawPin[0], "It must have climbed on the sleeper and pinned them");
            helper.assertTrue(afterBite[0] > 0.0F, "Full iron armour survives the bite");
            helper.assertTrue(afterBite[0] < 4.0F, "The bite must nearly kill through iron, health " + afterBite[0]);
            helper.assertTrue(!asleepAfterBite[0], "The bite wakes the player");
            helper.assertTrue(!pinnedAfterBite[0], "The pin ends with the bite");
            UncannySleeperEntity.State state = sleeper.state();
            helper.assertTrue(state == UncannySleeperEntity.State.RECOVER || state == UncannySleeperEntity.State.FIGHT,
                    "After the bite it climbs off, then fights: " + state);
            sleeper.discard();
            player.setGameMode(GameType.SPECTATOR);
            level.setDayTime(previousTime);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "sleeper_night_rescue")
    public static void anotherPlayerTearsItOffTheSleeper(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long previousTime = level.getDayTime();
        level.setDayTime(18000L);
        BlockPos headAbs = buildBedroom(helper);
        ServerPlayer victim = mockPlayer(helper);
        ServerPlayer rescuer = mockPlayer(helper);
        BlockPos rescuerPos = helper.absolutePos(new BlockPos(10, 1, 8));
        rescuer.moveTo(rescuerPos.getX() + 0.5D, rescuerPos.getY(), rescuerPos.getZ() + 0.5D, 90.0F, 0.0F);
        UncannySleeperEntity sleeper = spawnSleeper(helper, victim, new BlockPos(3, 1, 3));
        victim.startSleeping(headAbs);

        boolean[] tornOff = {false};
        helper.onEachTick(() -> {
            UncannySleeperEntity.State state = sleeper.state();
            if (!tornOff[0] && (state == UncannySleeperEntity.State.MOUNT || state == UncannySleeperEntity.State.MOUTH)) {
                tornOff[0] = true;
                sleeper.hurt(level.damageSources().playerAttack(rescuer), 2.0F);
                helper.assertTrue(sleeper.state() == UncannySleeperEntity.State.RECOVER, "Struck by someone else, it lets go");
                helper.assertTrue(!victim.isSleeping(), "The sleeper wakes up");
                helper.assertTrue(!SleeperPins.isPinned(victim), "The pin ends");
                helper.assertTrue(!sleeper.isSinking(), "It does not flee: it turns on the rescuer");
            }
        });
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(tornOff[0], "It must have climbed on the sleeper");
            helper.assertTrue(victim.getHealth() >= victim.getMaxHealth(), "Torn off before the bite, the sleeper is unhurt");
            sleeper.discard();
            victim.setGameMode(GameType.SPECTATOR);
            rescuer.setGameMode(GameType.SPECTATOR);
            level.setDayTime(previousTime);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120, batch = "sleeper_night_wake")
    public static void wakingBeforeItArrivesSendsItBackUnderground(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long previousTime = level.getDayTime();
        level.setDayTime(18000L);
        BlockPos headAbs = buildBedroom(helper);
        ServerPlayer victim = mockPlayer(helper);
        UncannySleeperEntity sleeper = spawnSleeper(helper, victim, new BlockPos(1, 1, 14));
        victim.startSleeping(headAbs);

        boolean[] woke = {false};
        helper.onEachTick(() -> {
            if (!woke[0] && sleeper.state() == UncannySleeperEntity.State.RUN) {
                woke[0] = true;
                victim.stopSleepInBed(false, true);
                helper.assertTrue(!victim.isSleeping(), "Not pinned yet: the player can still get up");
            }
        });
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(woke[0], "It must have started running");
            helper.assertTrue(sleeper.isRemoved(), "Awake before it arrived: it sank away");
            helper.assertTrue(victim.getHealth() >= victim.getMaxHealth(), "No bite without a sleeper");
            victim.setGameMode(GameType.SPECTATOR);
            level.setDayTime(previousTime);
            helper.succeed();
        });
    }

    /** Stone floor and a north-facing bed whose head is at (8, 1, 8); returns the head's absolute position. */
    private static BlockPos buildBedroom(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
        BlockPos foot = new BlockPos(8, 1, 9);
        BlockPos head = foot.north();
        helper.setBlock(foot, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH)
                .setValue(BedBlock.PART, BedPart.FOOT));
        helper.setBlock(head, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH)
                .setValue(BedBlock.PART, BedPart.HEAD));
        return helper.absolutePos(head);
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        return player;
    }

    private static UncannySleeperEntity spawnSleeper(GameTestHelper helper, ServerPlayer target, BlockPos relative) {
        UncannySleeperEntity sleeper = UncannyEntityRegistry.UNCANNY_SLEEPER.get().create(helper.getLevel());
        BlockPos at = helper.absolutePos(relative);
        sleeper.moveTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        sleeper.setupTarget(target, 20 * 600);
        helper.getLevel().addFreshEntity(sleeper);
        return sleeper;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "sleeper_night")
    public static void struckBeforeTheAttackItSinks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long previousTime = level.getDayTime();
        level.setDayTime(18000L);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        UncannySleeperEntity sleeper = UncannyEntityRegistry.UNCANNY_SLEEPER.get().create(level);
        BlockPos corner = helper.absolutePos(new BlockPos(4, 1, 4));
        sleeper.moveTo(corner.getX() + 0.5D, corner.getY(), corner.getZ() + 0.5D, 0.0F, 0.0F);
        sleeper.setupTarget(player, 20 * 600);
        level.addFreshEntity(sleeper);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!sleeper.isSinking(), "At night it waits");
            sleeper.hurt(level.damageSources().playerAttack(player), 3.0F);
            helper.assertTrue(sleeper.isSinking(), "Found and struck, it must sink");
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(sleeper.isRemoved(), "It is gone after sinking");
            player.setGameMode(GameType.SPECTATOR);
            level.setDayTime(previousTime);
            helper.succeed();
        });
    }
}
