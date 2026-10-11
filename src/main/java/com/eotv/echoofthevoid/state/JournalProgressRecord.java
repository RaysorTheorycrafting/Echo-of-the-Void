package com.eotv.echoofthevoid.state;

import com.eotv.echoofthevoid.lore.UncannyJournalProgressRules;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * World-level journal progression (D-100), saved under {@code journalProgress}. Volumes are the stable catalog
 * numbers; the discovery order lives in {@link UncannyJournalProgressRules}.
 *
 * @param foundMask journals opened by any player of this world
 * @param lastDiscoveryElapsedTicks campaign ticks of the latest discovery, {@code -1} when unknown
 * @param lastDiscoveryAnchor bed (else position) of the latest reader, centre of the next place; {@code null} if unknown
 * @param pending the single place waiting to be built or read, or {@code null}
 * @param places places already built in this world, at most one per volume
 * @param maps treasure maps issued towards a place, used to erase the cross once that place is read
 * @param lostMask journals lost for good by a player's own doing (D-101): skipped, never found
 * @param rerolledMask journals whose place was already found again once after a death (D-101)
 */
public record JournalProgressRecord(
        int foundMask,
        long lastDiscoveryElapsedTicks,
        BlockPos lastDiscoveryAnchor,
        boolean starterMapDelivered,
        PendingPlace pending,
        List<Place> places,
        List<IssuedMap> maps,
        int lostMask,
        int rerolledMask) {
    public static final String TAG = "journalProgress";
    static final int MAX_ISSUED_MAPS = 64;

    public JournalProgressRecord {
        foundMask &= UncannyJournalProgressRules.COMPLETE_MASK;
        lostMask &= UncannyJournalProgressRules.COMPLETE_MASK & ~foundMask;
        rerolledMask &= UncannyJournalProgressRules.COMPLETE_MASK;
        lastDiscoveryElapsedTicks = Math.max(-1L, lastDiscoveryElapsedTicks);
        places = List.copyOf(places);
        maps = List.copyOf(maps.size() > MAX_ISSUED_MAPS ? maps.subList(maps.size() - MAX_ISSUED_MAPS, maps.size()) : maps);
    }

    public static JournalProgressRecord empty() {
        return new JournalProgressRecord(0, -1L, null, false, null, List.of(), List.of(), 0, 0);
    }

    /** Worlds saved before D-100: every journal found by any player counts for the whole world. */
    public static JournalProgressRecord migrated(Collection<Integer> perPlayerMasks) {
        return new JournalProgressRecord(
                UncannyJournalProgressRules.migrateMask(perPlayerMasks), -1L, null, false, null, List.of(), List.of(), 0, 0);
    }

    public boolean isFound(int volume) {
        return UncannyJournalProgressRules.isFound(foundMask, volume);
    }

    public int nextVolume() {
        return UncannyJournalProgressRules.nextVolume(resolvedMask());
    }

    /** Journals that no longer block the progression: found, or lost by a player's own doing. */
    public int resolvedMask() {
        return foundMask | lostMask;
    }

    public boolean isLost(int volume) {
        return UncannyJournalProgressRules.isFound(lostMask, volume);
    }

    public boolean isRerolled(int volume) {
        return UncannyJournalProgressRules.isFound(rerolledMask, volume);
    }

    /**
     * The journal is gone for good (thrown away unread, or lost again after its place came back once): it is skipped
     * and the delay towards the next place starts now.
     */
    public JournalProgressRecord withLost(int volume, long elapsedTicks) {
        if (isFound(volume) || isLost(volume) || UncannyJournalProgressRules.rankOf(volume) < 0) {
            return this;
        }
        PendingPlace keptPending = pending != null && pending.volume() == volume ? null : pending;
        return new JournalProgressRecord(foundMask, elapsedTicks, lastDiscoveryAnchor, starterMapDelivered,
                keptPending, places, maps, UncannyJournalProgressRules.withFound(lostMask, volume), rerolledMask);
    }

    /** A player died with the unread journal and it was lost: its place may be found once more, elsewhere. */
    public JournalProgressRecord withRerolled(int volume) {
        PendingPlace keptPending = pending != null && pending.volume() == volume ? null : pending;
        return new JournalProgressRecord(foundMask, lastDiscoveryElapsedTicks, lastDiscoveryAnchor, starterMapDelivered,
                keptPending, places, maps, lostMask, UncannyJournalProgressRules.withFound(rerolledMask, volume));
    }

    /** Marks a journal as opened; a pending place of that volume stops being pending (a built one stays in {@link #places()}). */
    public JournalProgressRecord withFound(int volume, long elapsedTicks) {
        return withFound(volume, elapsedTicks, lastDiscoveryAnchor);
    }

    /** Same, remembering where the reader lives so the next place is placed around it. */
    public JournalProgressRecord withFound(int volume, long elapsedTicks, BlockPos readerAnchor) {
        if (isFound(volume) || UncannyJournalProgressRules.rankOf(volume) < 0) {
            return this;
        }
        PendingPlace keptPending = pending != null && pending.volume() == volume ? null : pending;
        return new JournalProgressRecord(
                UncannyJournalProgressRules.withFound(foundMask, volume), elapsedTicks,
                readerAnchor == null ? lastDiscoveryAnchor : readerAnchor.immutable(), starterMapDelivered,
                keptPending, places, maps, lostMask, rerolledMask);
    }

    public JournalProgressRecord withPending(PendingPlace newPending) {
        return new JournalProgressRecord(
                foundMask, lastDiscoveryElapsedTicks, lastDiscoveryAnchor, starterMapDelivered, newPending, places,
                maps, lostMask, rerolledMask);
    }

    /** Records the built place and keeps it pending, now in the BUILT state. */
    public JournalProgressRecord withBuilt(PendingPlace built) {
        List<Place> nextPlaces = new ArrayList<>();
        for (Place place : places) {
            if (place.volume() != built.volume()) {
                nextPlaces.add(place);
            }
        }
        nextPlaces.add(new Place(built.volume(), built.dimension(), built.posLong()));
        return new JournalProgressRecord(foundMask, lastDiscoveryElapsedTicks, lastDiscoveryAnchor,
                starterMapDelivered, built.withStatus(PlaceStatus.BUILT), nextPlaces, maps, lostMask, rerolledMask);
    }

    public JournalProgressRecord withStarterMapDelivered() {
        return new JournalProgressRecord(
                foundMask, lastDiscoveryElapsedTicks, lastDiscoveryAnchor, true, pending, places, maps, lostMask,
                rerolledMask);
    }

    public JournalProgressRecord withIssuedMap(int mapId, int volume) {
        List<IssuedMap> nextMaps = new ArrayList<>(maps);
        nextMaps.removeIf(map -> map.mapId() == mapId);
        nextMaps.add(new IssuedMap(mapId, volume));
        return new JournalProgressRecord(
                foundMask, lastDiscoveryElapsedTicks, lastDiscoveryAnchor, starterMapDelivered, pending, places,
                nextMaps, lostMask, rerolledMask);
    }

    /** Catalog volume an issued map points to, or {@code -1} for any other map. */
    public int volumeForMap(int mapId) {
        for (IssuedMap map : maps) {
            if (map.mapId() == mapId) {
                return map.volume();
            }
        }
        return -1;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("foundMask", foundMask);
        tag.putLong("lastDiscoveryElapsedTicks", lastDiscoveryElapsedTicks);
        if (lastDiscoveryAnchor != null) {
            tag.putLong("lastDiscoveryAnchor", lastDiscoveryAnchor.asLong());
        }
        tag.putBoolean("starterMapDelivered", starterMapDelivered);
        tag.putInt("lostMask", lostMask);
        tag.putInt("rerolledMask", rerolledMask);
        if (pending != null) {
            tag.put("pending", pending.save());
        }
        ListTag placeList = new ListTag();
        for (Place place : places) {
            CompoundTag item = new CompoundTag();
            item.putInt("volume", place.volume());
            item.putString("dimension", place.dimension());
            item.putLong("pos", place.posLong());
            placeList.add(item);
        }
        tag.put("places", placeList);
        ListTag mapList = new ListTag();
        for (IssuedMap map : maps) {
            CompoundTag item = new CompoundTag();
            item.putInt("mapId", map.mapId());
            item.putInt("volume", map.volume());
            mapList.add(item);
        }
        tag.put("maps", mapList);
        return tag;
    }

    public static JournalProgressRecord load(CompoundTag tag) {
        PendingPlace pending = tag.contains("pending", Tag.TAG_COMPOUND)
                ? PendingPlace.load(tag.getCompound("pending")) : null;
        List<Place> places = new ArrayList<>();
        ListTag placeList = tag.getList("places", Tag.TAG_COMPOUND);
        for (int i = 0; i < placeList.size(); i++) {
            CompoundTag item = placeList.getCompound(i);
            int volume = item.getInt("volume");
            String dimension = item.getString("dimension");
            if (UncannyJournalProgressRules.rankOf(volume) >= 0 && !dimension.isBlank()
                    && places.stream().noneMatch(place -> place.volume() == volume)) {
                places.add(new Place(volume, dimension, item.getLong("pos")));
            }
        }
        List<IssuedMap> maps = new ArrayList<>();
        ListTag mapList = tag.getList("maps", Tag.TAG_COMPOUND);
        for (int i = 0; i < mapList.size(); i++) {
            CompoundTag item = mapList.getCompound(i);
            int volume = item.getInt("volume");
            if (UncannyJournalProgressRules.rankOf(volume) >= 0) {
                maps.add(new IssuedMap(item.getInt("mapId"), volume));
            }
        }
        long lastDiscovery = tag.contains("lastDiscoveryElapsedTicks")
                ? tag.getLong("lastDiscoveryElapsedTicks") : -1L;
        JournalProgressRecord record = new JournalProgressRecord(
                tag.getInt("foundMask"), lastDiscovery,
                tag.contains("lastDiscoveryAnchor") ? BlockPos.of(tag.getLong("lastDiscoveryAnchor")) : null,
                tag.getBoolean("starterMapDelivered"), pending, places, maps, tag.getInt("lostMask"),
                tag.getInt("rerolledMask"));
        // A pending place for an already-read journal is stale (e.g. edited data); drop it.
        return pending != null && record.isFound(pending.volume()) ? record.withPending(null) : record;
    }

    public enum PlaceStatus {
        RESERVED,
        BUILT
    }

    /**
     * @param reservedElapsedTicks campaign ticks of the reservation, also the start of the starter map tiers
     */
    public record PendingPlace(int volume, String dimension, long posLong, PlaceStatus status, long reservedElapsedTicks,
            long holderPos, boolean journalTaken) {
        /** No known journal holder (place not built yet, or built before the holder was remembered). */
        public static final long NO_HOLDER = Long.MIN_VALUE;

        public PendingPlace(int volume, String dimension, long posLong, PlaceStatus status, long reservedElapsedTicks) {
            this(volume, dimension, posLong, status, reservedElapsedTicks, NO_HOLDER, false);
        }

        public PendingPlace withStatus(PlaceStatus newStatus) {
            return new PendingPlace(volume, dimension, posLong, newStatus, reservedElapsedTicks, holderPos, journalTaken);
        }

        /** Chest (or lectern) holding the journal. */
        public PendingPlace withHolder(BlockPos holder) {
            return new PendingPlace(volume, dimension, posLong, status, reservedElapsedTicks,
                    holder == null ? NO_HOLDER : holder.asLong(), journalTaken);
        }

        /** A player has had the journal in hand: from now on its fate is the player's (D-101). */
        public PendingPlace withTaken() {
            return new PendingPlace(volume, dimension, posLong, status, reservedElapsedTicks, holderPos, true);
        }

        public BlockPos holder() {
            return holderPos == NO_HOLDER ? null : BlockPos.of(holderPos);
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("volume", volume);
            tag.putString("dimension", dimension);
            tag.putLong("pos", posLong);
            tag.putString("status", status.name());
            tag.putLong("reservedElapsedTicks", reservedElapsedTicks);
            if (holderPos != NO_HOLDER) {
                tag.putLong("holder", holderPos);
            }
            tag.putBoolean("taken", journalTaken);
            return tag;
        }

        /** @return null for an absent or invalid entry */
        static PendingPlace load(CompoundTag tag) {
            int volume = tag.getInt("volume");
            String dimension = tag.getString("dimension");
            if (UncannyJournalProgressRules.rankOf(volume) < 0 || dimension.isBlank()) {
                return null;
            }
            PlaceStatus status;
            try {
                status = PlaceStatus.valueOf(tag.getString("status"));
            } catch (IllegalArgumentException exception) {
                status = PlaceStatus.RESERVED;
            }
            return new PendingPlace(volume, dimension, tag.getLong("pos"), status,
                    Math.max(0L, tag.getLong("reservedElapsedTicks")),
                    tag.contains("holder") ? tag.getLong("holder") : NO_HOLDER, tag.getBoolean("taken"));
        }
    }

    public record Place(int volume, String dimension, long posLong) {
    }

    public record IssuedMap(int mapId, int volume) {
    }
}
