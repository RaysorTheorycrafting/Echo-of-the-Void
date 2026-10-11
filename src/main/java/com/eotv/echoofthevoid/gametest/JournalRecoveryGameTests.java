package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.event.UncannyJournalPlaces;
import com.eotv.echoofthevoid.event.UncannyJournalRecovery;
import com.eotv.echoofthevoid.lore.UncannyLoreBookLibrary;
import com.eotv.echoofthevoid.state.JournalProgressRecord;
import com.eotv.echoofthevoid.state.JournalProgressRecord.PendingPlace;
import com.eotv.echoofthevoid.state.JournalProgressRecord.PlaceStatus;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** D-101: a place journal is only ever lost by the player's own doing. Shared progress is restored after each test. */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class JournalRecoveryGameTests {
    private static final String TEMPLATE = "special_test_room";

    private JournalRecoveryGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void aJournalThatVanishesUnheldIsPutBackInItsPlace(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        JournalProgressRecord saved = state.getJournalProgress();
        try {
            BlockPos holder = buildPendingPlace(helper, state, 1);
            Container chest = (Container) level.getBlockEntity(holder);
            helper.assertTrue("in-place".equals(UncannyJournalRecovery.tick(level)), "The journal is in its chest");

            chest.clearContent();
            helper.assertTrue("restored".equals(UncannyJournalRecovery.tick(level)),
                    "Gone without anyone holding it (command, generation error): it comes back");
            helper.assertTrue(countJournals(chest) == 1, "Back in the same chest");

            level.setBlock(holder, Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue("in-place".equals(UncannyJournalRecovery.tick(level)),
                    "Chest broken: its journal lies on the ground and may still be picked up, nothing is added");
            for (ItemEntity dropped : level.getEntitiesOfClass(ItemEntity.class,
                    new net.minecraft.world.phys.AABB(holder).inflate(8.0D))) {
                dropped.discard();
            }
            helper.assertTrue("restored".equals(UncannyJournalRecovery.tick(level)),
                    "Chest and journal both gone: a chest is put back where it stood");
            helper.assertTrue(level.getBlockEntity(holder) instanceof Container restored && countJournals(restored) == 1,
                    "The new chest holds the journal");
        } finally {
            state.setJournalProgress(saved);
            clear(helper);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void onceAPlayerHoldsItTheJournalIsNoLongerPutBack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        JournalProgressRecord saved = state.getJournalProgress();
        try {
            BlockPos holder = buildPendingPlace(helper, state, 1);
            Container chest = (Container) level.getBlockEntity(holder);
            ServerPlayer player = mockPlayer(helper);
            int slot = journalSlot(chest);
            player.getInventory().add(chest.removeItemNoUpdate(slot));
            helper.assertTrue("taken".equals(UncannyJournalRecovery.tick(level)), "A player holding it is noticed");
            helper.assertTrue(state.getJournalProgress().pending().journalTaken(), "Taken is remembered");

            player.getInventory().clearContent();
            helper.assertTrue("player-has-it".equals(UncannyJournalRecovery.tick(level)) && countJournals(chest) == 0,
                    "From then on its fate is the player's: no copy appears in the chest");
        } finally {
            state.setJournalProgress(saved);
            clear(helper);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void destroyedJournalsFollowWhoLostThem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        JournalProgressRecord saved = state.getJournalProgress();
        try {
            // Explosion or fire on a journal nobody threw: back in the place.
            BlockPos holder = buildPendingPlace(helper, state, 1);
            Container chest = (Container) level.getBlockEntity(holder);
            ItemEntity blownOut = groundJournal(level, holder, chest.removeItemNoUpdate(journalSlot(chest)));
            helper.assertTrue("restored".equals(UncannyJournalRecovery.onJournalItemDestroyed(level, blownOut)),
                    "Not the player's doing: restored");
            helper.assertTrue(countJournals(chest) == 1, "Restored into its chest");

            // Thrown away unread by the player, then burnt: lost for good, the next place follows.
            ItemEntity thrown = groundJournal(level, holder, chest.removeItemNoUpdate(journalSlot(chest)));
            thrown.getPersistentData().putBoolean("eotv_journal_thrown", true);
            helper.assertTrue("lost-thrown".equals(UncannyJournalRecovery.onJournalItemDestroyed(level, thrown)),
                    "Thrown away by the player: lost");
            JournalProgressRecord afterThrow = state.getJournalProgress();
            helper.assertTrue(afterThrow.isLost(1) && !afterThrow.isFound(1) && afterThrow.pending() == null,
                    "Lost, never found, no longer pending");
            helper.assertTrue(afterThrow.nextVolume() == 2, "The story goes on with the next journal");
            helper.assertTrue(afterThrow.lastDiscoveryElapsedTicks() == state.getCampaignElapsedTicks(),
                    "The delay towards the next place starts at the loss");

            // Died with it, then the drop despawned: the place may be found again once, elsewhere.
            state.setJournalProgress(JournalProgressRecord.empty());
            holder = buildPendingPlace(helper, state, 1);
            chest = (Container) level.getBlockEntity(holder);
            ItemEntity deathDrop = groundJournal(level, holder, chest.removeItemNoUpdate(journalSlot(chest)));
            deathDrop.getPersistentData().putBoolean("eotv_journal_death_drop", true);
            helper.assertTrue("place-found-again".equals(UncannyJournalRecovery.onJournalItemDestroyed(level, deathDrop)),
                    "First loss after a death: the place comes back");
            JournalProgressRecord afterDeath = state.getJournalProgress();
            helper.assertTrue(afterDeath.isRerolled(1) && afterDeath.pending() == null && afterDeath.nextVolume() == 1,
                    "Same journal searched again, once");

            holder = buildPendingPlace(helper, state, 1);
            chest = (Container) level.getBlockEntity(holder);
            ItemEntity secondDeath = groundJournal(level, holder, chest.removeItemNoUpdate(journalSlot(chest)));
            secondDeath.getPersistentData().putBoolean("eotv_journal_death_drop", true);
            helper.assertTrue("lost-after-second-chance".equals(UncannyJournalRecovery.onJournalItemDestroyed(level, secondDeath))
                            && state.getJournalProgress().isLost(1) && state.getJournalProgress().nextVolume() == 2,
                    "Lost again after its second chance: gone for good");

            ItemStack copy = new ItemStack(com.eotv.echoofthevoid.item.UncannyItemRegistry.UNCANNY_LORE_PIECE.get());
            copy.set(DataComponents.WRITTEN_BOOK_CONTENT, UncannyLoreBookLibrary.contentForVolume(2));
            helper.assertTrue("not-a-place-journal".equals(UncannyJournalRecovery.onJournalItemDestroyed(level,
                            groundJournal(level, holder, copy))),
                    "Copies given by commands are not place journals");
        } finally {
            state.setJournalProgress(saved);
            clear(helper);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void recoveryStateRoundTripsThroughMinecraftNbt(GameTestHelper helper) {
        PendingPlace pending = new PendingPlace(3, "minecraft:overworld", new BlockPos(10, 70, 10).asLong(),
                PlaceStatus.BUILT, 5L).withHolder(new BlockPos(11, 75, 9)).withTaken();
        JournalProgressRecord progress = JournalProgressRecord.empty().withFound(1, 1L).withLost(2, 2L)
                .withRerolled(3).withPending(pending);
        UncannyWorldState original = UncannyWorldState.create();
        original.setJournalProgress(progress);
        CompoundTag saved = original.save(new CompoundTag(), helper.getLevel().registryAccess());
        JournalProgressRecord restored = UncannyWorldState.load(saved, helper.getLevel().registryAccess()).getJournalProgress();
        helper.assertTrue(restored.equals(progress), "Lost, rerolled, holder and taken survive NBT: " + restored);
        helper.assertTrue(restored.nextVolume() == 3, "Found 1, lost 2: the next journal is 3");
        helper.assertTrue(UncannyJournalPlaces.createJournal(4) != null
                        && UncannyJournalRecovery.placeJournalVolume(UncannyJournalPlaces.createJournal(4)) == 4,
                "Place journals carry their volume");
        helper.succeed();
    }

    /** Builds Eli's place in the room as the pending, built place of the world; returns its chest. */
    private static BlockPos buildPendingPlace(GameTestHelper helper, UncannyWorldState state, int volume) {
        clear(helper);
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(8, 1, 8));
        BlockPos holder = UncannyJournalPlaces.build(level, volume, origin, Direction.SOUTH);
        PendingPlace pending = new PendingPlace(volume, level.dimension().location().toString(), origin.asLong(),
                PlaceStatus.RESERVED, 0L).withHolder(holder);
        JournalProgressRecord progress = state.getJournalProgress();
        state.setJournalProgress(progress.withPending(pending).withBuilt(pending));
        return holder;
    }

    private static ItemEntity groundJournal(ServerLevel level, BlockPos near, ItemStack stack) {
        return new ItemEntity(level, near.getX() + 0.5D, near.getY() + 1.0D, near.getZ() + 0.5D, stack);
    }

    private static int journalSlot(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (UncannyJournalRecovery.placeJournalVolume(container.getItem(slot)) > 0) {
                return slot;
            }
        }
        throw new IllegalStateException("No place journal in the chest");
    }

    private static int countJournals(Container container) {
        int journals = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (UncannyJournalRecovery.placeJournalVolume(container.getItem(slot)) > 0) {
                journals++;
            }
        }
        return journals;
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void clear(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, -1, z, Blocks.STONE);
                helper.setBlock(x, 0, z, Blocks.STONE);
                for (int y = 1; y <= 10; y++) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }
    }
}
