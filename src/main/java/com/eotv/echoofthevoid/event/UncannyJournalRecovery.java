package com.eotv.echoofthevoid.event;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.diagnostics.DiagnosticSeverity;
import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import com.eotv.echoofthevoid.lore.UncannyJournalProgressRules;
import com.eotv.echoofthevoid.state.JournalProgressRecord;
import com.eotv.echoofthevoid.state.JournalProgressRecord.PendingPlace;
import com.eotv.echoofthevoid.state.JournalProgressRecord.PlaceStatus;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * A journal place never costs the player a journal they did not lose themselves (D-101):
 * <ul>
 *     <li>gone without anyone having held it (explosion, fire, command, generation error): put back in its place;</li>
 *     <li>thrown away unread by a player, then destroyed: lost for good, the next place follows;</li>
 *     <li>dropped by a player's death, then destroyed: its place may be found once more, elsewhere.</li>
 * </ul>
 */
public final class UncannyJournalRecovery {
    /** Custom data marking the journal created for a place: the catalog volume. Copies given by commands lack it. */
    public static final String PLACE_JOURNAL_TAG = "eotv_place_journal";
    static final String THROWN_FLAG = "eotv_journal_thrown";
    static final String DEATH_DROP_FLAG = "eotv_journal_death_drop";
    /** Ground search radius around the place for a journal lying on the floor. */
    private static final int GROUND_SEARCH_RADIUS = 48;

    private UncannyJournalRecovery() {
    }

    public static ItemStack tag(ItemStack journal, int volume) {
        CustomData.update(DataComponents.CUSTOM_DATA, journal, data -> data.putInt(PLACE_JOURNAL_TAG, volume));
        return journal;
    }

    /** Volume of a place journal, or {@code -1} for any other stack. */
    public static int placeJournalVolume(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null || !data.copyTag().contains(PLACE_JOURNAL_TAG) ? -1 : data.copyTag().getInt(PLACE_JOURNAL_TAG);
    }

    // ---------------------------------------------------------------- events

    /** A player throwing it away (Q key, inventory screen) is the player's own doing; death drops do not fire this. */
    public static void onItemToss(ItemTossEvent event) {
        if (placeJournalVolume(event.getEntity().getItem()) > 0) {
            event.getEntity().getPersistentData().putBoolean(THROWN_FLAG, true);
        }
    }

    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }
        for (ItemEntity drop : event.getDrops()) {
            if (placeJournalVolume(drop.getItem()) > 0) {
                drop.getPersistentData().putBoolean(DEATH_DROP_FLAG, true);
            }
        }
    }

    /** A place journal lying on the ground is destroyed (lava, fire, cactus, void, despawn); a pickup empties it first. */
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity item)
                || !(event.getLevel() instanceof ServerLevel level)
                || level.getServer() instanceof GameTestServer) {
            return;
        }
        Entity.RemovalReason reason = item.getRemovalReason();
        if ((reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED)
                || item.getItem().isEmpty()) {
            return;
        }
        onJournalItemDestroyed(level, item);
    }

    /**
     * Decides the fate of a destroyed place journal.
     *
     * @return the outcome, for diagnostics and tests
     */
    public static String onJournalItemDestroyed(ServerLevel level, ItemEntity item) {
        int volume = placeJournalVolume(item.getItem());
        if (volume < 0) {
            return "not-a-place-journal";
        }
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        JournalProgressRecord progress = state.getJournalProgress();
        PendingPlace pending = progress.pending();
        if (pending == null || pending.volume() != volume || progress.isFound(volume)) {
            return "not-pending";
        }
        if (anyPlayerCarries(level, volume)) {
            return "carried";
        }
        long now = state.getCampaignElapsedTicks();
        String outcome;
        if (item.getPersistentData().getBoolean(THROWN_FLAG)) {
            state.setJournalProgress(progress.withLost(volume, now));
            outcome = "lost-thrown";
        } else if (item.getPersistentData().getBoolean(DEATH_DROP_FLAG)) {
            if (!progress.isRerolled(volume)) {
                state.setJournalProgress(progress.withRerolled(volume));
                outcome = "place-found-again";
            } else {
                state.setJournalProgress(progress.withLost(volume, now));
                outcome = "lost-after-second-chance";
            }
        } else {
            outcome = restore(level, pending) ? "restored" : "restore-failed";
        }
        record(outcome, volume, item.blockPosition());
        return outcome;
    }

    // ---------------------------------------------------------------- periodic check

    /**
     * Called each second while a built place waits for its reader: notes when a player first holds the journal, and
     * puts it back if it vanished from the place without anyone having held it.
     */
    public static String tick(ServerLevel level) {
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        JournalProgressRecord progress = state.getJournalProgress();
        PendingPlace pending = progress.pending();
        if (pending == null || pending.status() != PlaceStatus.BUILT
                || !level.dimension().location().toString().equals(pending.dimension())) {
            return "idle";
        }
        int volume = pending.volume();
        if (!pending.journalTaken() && anyPlayerCarries(level, volume)) {
            state.setJournalProgress(progress.withPending(pending.withTaken()));
            record("taken", volume, BlockPos.of(pending.posLong()));
            return "taken";
        }
        if (pending.journalTaken()) {
            return "player-has-it";
        }
        BlockPos origin = BlockPos.of(pending.posLong());
        // Entity-ticking only: a chunk still listed as loaded while it unloads has already dropped its block entities,
        // and a journal would look gone (Morgan's place got a second copy that way, a few seconds after being built).
        if (!level.isPositionEntityTicking(origin)
                || (pending.holder() != null && !level.isPositionEntityTicking(pending.holder()))) {
            return "unloaded";
        }
        if (placeHoldsJournal(level, pending) || journalOnTheGround(level, origin, volume)) {
            return "in-place";
        }
        boolean restored = restore(level, pending);
        record(restored ? "restored" : "restore-failed", volume, origin);
        return restored ? "restored" : "restore-failed";
    }

    // ---------------------------------------------------------------- helpers

    /** A place journal in an online player's inventory or under their cursor. */
    public static boolean anyPlayerCarries(ServerLevel level, int volume) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (placeJournalVolume(player.containerMenu.getCarried()) == volume) {
                return true;
            }
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                if (placeJournalVolume(player.getInventory().getItem(slot)) == volume) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean placeHoldsJournal(ServerLevel level, PendingPlace pending) {
        BlockPos origin = BlockPos.of(pending.posLong());
        for (BlockEntity blockEntity : placeBlockEntities(level, origin)) {
            if (holdsJournal(blockEntity, pending.volume())) {
                return true;
            }
        }
        return false;
    }

    private static boolean holdsJournal(BlockEntity blockEntity, int volume) {
        if (blockEntity instanceof LecternBlockEntity lectern) {
            return isJournalOf(lectern.getBook(), volume);
        }
        if (blockEntity instanceof Container container) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                if (isJournalOf(container.getItem(slot), volume)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Tagged, or (places built before the tag existed) the journal with that title. */
    private static boolean isJournalOf(ItemStack stack, int volume) {
        if (placeJournalVolume(stack) == volume) {
            return true;
        }
        WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        return content != null && UncannyJournalProgressRules.volumeForTitle(content.title().raw()) == volume;
    }

    private static boolean journalOnTheGround(ServerLevel level, BlockPos origin, int volume) {
        AABB area = new AABB(origin).inflate(GROUND_SEARCH_RADIUS)
                .expandTowards(0.0D, -UncannyJournalPlaces.MAX_DEPTH_BELOW, 0.0D);
        return !level.getEntitiesOfClass(ItemEntity.class, area,
                item -> item.isAlive() && placeJournalVolume(item.getItem()) == volume).isEmpty();
    }

    /** Block entities within the widest place (Morgan's deep mine included) around the origin, in loaded chunks. */
    private static List<BlockEntity> placeBlockEntities(ServerLevel level, BlockPos origin) {
        return UncannyJournalPlaces.blockEntitiesAround(level, origin, UncannyJournalPlaces.MAX_DEPTH_BELOW,
                UncannyJournalPlaces.MAX_HEIGHT_ABOVE);
    }

    /**
     * Puts a fresh journal back: in its holder if it still stands, else in another container of the place, else in a
     * new chest where the holder was.
     */
    static boolean restore(ServerLevel level, PendingPlace pending) {
        int volume = pending.volume();
        BlockPos origin = BlockPos.of(pending.posLong());
        BlockPos holder = pending.holder();
        if (holder != null && putInto(level, level.getBlockEntity(holder), volume)) {
            return true;
        }
        for (BlockEntity blockEntity : placeBlockEntities(level, origin)) {
            if (blockEntity.getPersistentData().contains(UncannyJournalPlaces.JOURNAL_PLACE_CHEST_FLAG)
                    && putInto(level, blockEntity, volume)) {
                rememberHolder(level, blockEntity.getBlockPos());
                return true;
            }
        }
        for (BlockEntity blockEntity : placeBlockEntities(level, origin)) {
            if (blockEntity instanceof ChestBlockEntity
                    && blockEntity.getPersistentData().getBoolean(UncannyJournalPlaces.PLACE_CONTAINER_FLAG)
                    && putInto(level, blockEntity, volume)) {
                rememberHolder(level, blockEntity.getBlockPos());
                return true;
            }
        }
        BlockPos chestPos = holder != null ? holder : origin;
        if (!level.getBlockState(chestPos).canBeReplaced()) {
            return false;
        }
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        BlockEntity chest = level.getBlockEntity(chestPos);
        if (chest == null) {
            return false;
        }
        chest.getPersistentData().putInt(UncannyJournalPlaces.JOURNAL_PLACE_CHEST_FLAG, volume);
        rememberHolder(level, chestPos);
        return putInto(level, chest, volume);
    }

    private static boolean putInto(ServerLevel level, BlockEntity blockEntity, int volume) {
        if (blockEntity instanceof LecternBlockEntity lectern) {
            if (!lectern.getBook().isEmpty()) {
                return false;
            }
            lectern.setBook(UncannyJournalPlaces.createJournal(volume));
            BlockPos pos = lectern.getBlockPos();
            if (level.getBlockState(pos).hasProperty(LecternBlock.HAS_BOOK)) {
                level.setBlock(pos, level.getBlockState(pos).setValue(LecternBlock.HAS_BOOK, true), 3);
            }
            lectern.setChanged();
            return true;
        }
        if (!(blockEntity instanceof Container container)) {
            return false;
        }
        List<Integer> free = UncannyJournalMaps.freeSlots(container);
        if (free.isEmpty()) {
            return false;
        }
        container.setItem(free.get(level.random.nextInt(free.size())), UncannyJournalPlaces.createJournal(volume));
        container.setChanged();
        return true;
    }

    private static void rememberHolder(ServerLevel level, BlockPos holder) {
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        JournalProgressRecord progress = state.getJournalProgress();
        if (progress.pending() != null) {
            state.setJournalProgress(progress.withPending(progress.pending().withHolder(holder)));
        }
    }

    private static void record(String outcome, int volume, BlockPos pos) {
        UncannyDiagnostics.record(DiagnosticSeverity.INFO, "journal", "recovery", Map.of(
                "volume", Integer.toString(volume), "outcome", outcome, "position", pos.toShortString()));
        if (UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info("[UncannyDebug/Journal] JOURNAL recovery volume={} outcome={} pos={}",
                    volume, outcome, pos);
        }
    }
}
