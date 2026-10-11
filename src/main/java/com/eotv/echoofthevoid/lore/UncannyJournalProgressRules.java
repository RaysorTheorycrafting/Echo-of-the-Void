package com.eotv.echoofthevoid.lore;

import com.eotv.echoofthevoid.campaign.CampaignDirectorRules;
import java.util.Collection;
import java.util.List;

/**
 * Pure world-level journal progression (D-100): one journal place at a time, in story order, gated by a
 * minimum logical story day and by a minimum delay since the previous discovery.
 *
 * <p>Journals keep their stable catalog volume numbers (persisted masks use them); only the discovery order
 * differs from the catalog order.
 */
public final class UncannyJournalProgressRules {
    /** Catalog volumes in discovery order: five torches, A Longer Way Home, Room for One, only dan heard it,
     * The Recipe Isn't There, Under the Third Warning. */
    public static final List<Integer> DISCOVERY_ORDER = List.of(1, 2, 3, 6, 4, 5);
    /** Minimum logical story day for each rank of {@link #DISCOVERY_ORDER}. */
    public static final List<Integer> MINIMUM_STORY_DAY = List.of(2, 5, 9, 13, 18, 24);
    public static final double MINIMUM_STORY_DAYS_BETWEEN_DISCOVERIES = 3.0D;
    public static final double STARTER_MAP_BARREL_DELAY_STORY_DAYS = 3.0D;
    public static final double STARTER_MAP_ANY_CONTAINER_DELAY_STORY_DAYS = 6.0D;
    public static final DistanceBand FIRST_PLACE_DISTANCE = new DistanceBand(150, 400);
    public static final DistanceBand LATER_PLACE_DISTANCE = new DistanceBand(300, 800);
    /**
     * After the first, each place lies farther from the world centre (0, 0) than every earlier place (D-103): between
     * these many blocks beyond the farthest one.
     */
    public static final int OUTWARD_STEP_MIN_BLOCKS = 100;
    public static final int OUTWARD_STEP_MAX_BLOCKS = 350;
    /** An outward place also lies at least this far beyond the reader's home, so it stays a trip away from it. */
    public static final int OUTWARD_BEYOND_READER_BLOCKS = 300;
    /**
     * An outward place is drawn on the reader's side of the world centre: within this angle of it, and never more
     * than {@link #OUTWARD_MAX_ARC_BLOCKS} along the ring, so a reader far from the centre is not sent across the world.
     */
    public static final double OUTWARD_SPREAD_RADIANS = Math.toRadians(60.0D);
    public static final int OUTWARD_MAX_ARC_BLOCKS = 400;
    /** A reader living closer than this to the world centre has no side: any direction is drawn. */
    public static final int CENTRED_ANCHOR_RADIUS = 64;
    public static final int COMPLETE_MASK = (1 << DISCOVERY_ORDER.size()) - 1;
    /** A place is only built while no player is this close, so it never appears in front of someone. */
    public static final int MIN_PLAYER_DISTANCE_AT_BUILD = 96;
    /** Keeps a place clear of the generated Uncanny structures. */
    public static final int MIN_DISTANCE_FROM_STRUCTURE_MARKERS = 48;
    /** Two journal places are never neighbours. */
    public static final int MIN_DISTANCE_BETWEEN_PLACES = 150;
    public static final int CANDIDATES_PER_SEARCH = 16;
    public static final long SEARCH_COOLDOWN_TICKS = 2_400L;

    private UncannyJournalProgressRules() {
    }

    /** Next catalog volume to place, or {@code -1} once every journal has been found. */
    public static int nextVolume(int foundMask) {
        for (int volume : DISCOVERY_ORDER) {
            if (!isFound(foundMask, volume)) {
                return volume;
            }
        }
        return -1;
    }

    /** Zero-based position of a catalog volume in the discovery order, or {@code -1}. */
    public static int rankOf(int volume) {
        return DISCOVERY_ORDER.indexOf(volume);
    }

    public static boolean isFound(int foundMask, int volume) {
        return isValidVolume(volume) && (foundMask & bit(volume)) != 0;
    }

    public static boolean isComplete(int foundMask) {
        return (foundMask & COMPLETE_MASK) == COMPLETE_MASK;
    }

    public static int withFound(int foundMask, int volume) {
        return isValidVolume(volume) ? (foundMask | bit(volume)) & COMPLETE_MASK : foundMask & COMPLETE_MASK;
    }

    /**
     * Whether the place of {@code volume} may be reserved now.
     *
     * @param lastDiscoveryElapsedTicks campaign ticks of the previous discovery, negative when none is known
     *     (new world or migrated world): the delay is then considered elapsed.
     */
    public static boolean isEligible(
            int volume, long elapsedTicks, long lastDiscoveryElapsedTicks, int campaignLengthDays) {
        int rank = rankOf(volume);
        if (rank < 0) {
            return false;
        }
        double storyDay = CampaignDirectorRules.logicalStoryDay(elapsedTicks, campaignLengthDays);
        if (storyDay < MINIMUM_STORY_DAY.get(rank)) {
            return false;
        }
        if (lastDiscoveryElapsedTicks < 0L) {
            return true;
        }
        return storyDaysBetween(lastDiscoveryElapsedTicks, elapsedTicks, campaignLengthDays)
                >= MINIMUM_STORY_DAYS_BETWEEN_DISCOVERIES;
    }

    /** Logical story days between two campaign tick counts; 100-day campaigns count half as fast. */
    public static double storyDaysBetween(long fromElapsedTicks, long toElapsedTicks, int campaignLengthDays) {
        long delta = Math.max(0L, toElapsedTicks - Math.max(0L, fromElapsedTicks));
        return CampaignDirectorRules.logicalStoryDay(delta, campaignLengthDays);
    }

    /** The starter map exists only to lead to the first place; a world that already read it never gets one. */
    public static boolean needsStarterMap(int foundMask) {
        return !isFound(foundMask, DISCOVERY_ORDER.get(0));
    }

    /** Containers allowed to receive the starter map, widening with the time since the first place was reserved. */
    public static StarterMapTier starterMapTier(
            long reservationElapsedTicks, long elapsedTicks, int campaignLengthDays) {
        double waited = storyDaysBetween(reservationElapsedTicks, elapsedTicks, campaignLengthDays);
        if (waited >= STARTER_MAP_ANY_CONTAINER_DELAY_STORY_DAYS) {
            return StarterMapTier.ANY_UNOPENED_CONTAINER;
        }
        if (waited >= STARTER_MAP_BARREL_DELAY_STORY_DAYS) {
            return StarterMapTier.PLAYER_CHEST_OR_BARREL;
        }
        return StarterMapTier.PLAYER_CHEST;
    }

    public static DistanceBand distanceBand(int volume) {
        return rankOf(volume) == 0 ? FIRST_PLACE_DISTANCE : LATER_PLACE_DISTANCE;
    }

    /** What the server should do for journal places at this moment. */
    public static PlaceAction nextAction(
            int foundMask, PendingState pending, long elapsedTicks, long lastDiscoveryElapsedTicks,
            int campaignLengthDays) {
        if (pending == PendingState.BUILT) {
            return PlaceAction.WAIT_FOR_READER;
        }
        if (pending == PendingState.RESERVED) {
            return PlaceAction.BUILD_RESERVED;
        }
        int next = nextVolume(foundMask);
        if (next < 0) {
            return PlaceAction.COMPLETE;
        }
        return isEligible(next, elapsedTicks, lastDiscoveryElapsedTicks, campaignLengthDays)
                ? PlaceAction.SEARCH : PlaceAction.WAIT_FOR_ELIGIBILITY;
    }

    /**
     * Horizontal offset of a candidate inside the distance band, uniform over the ring area.
     *
     * @param angleUnit random value in [0, 1)
     * @param distanceUnit random value in [0, 1)
     */
    public static int[] candidateOffset(double angleUnit, double distanceUnit, DistanceBand band) {
        double angle = angleUnit * Math.PI * 2.0D;
        double min = band.minBlocks();
        double max = band.maxBlocks();
        double distance = Math.sqrt(min * min + distanceUnit * (max * max - min * min));
        return new int[] {(int) Math.round(Math.cos(angle) * distance), (int) Math.round(Math.sin(angle) * distance)};
    }

    /**
     * Distances from the world centre allowed for the next place: beyond the farthest place so far, and beyond the
     * reader's home (its distance from the centre) by {@link #OUTWARD_BEYOND_READER_BLOCKS}.
     */
    public static DistanceBand outwardBand(double farthestPlaceDistance, double anchorDistance) {
        return outwardBand(farthestPlaceDistance, anchorDistance, 0);
    }

    /**
     * Same band, deeper after {@code widening} searches that found no site (an ocean, a mountain range): each one
     * adds another band's depth, so the place always ends up somewhere.
     */
    public static DistanceBand outwardBand(double farthestPlaceDistance, double anchorDistance, int widening) {
        double floor = Math.max(Math.max(0.0D, farthestPlaceDistance) + OUTWARD_STEP_MIN_BLOCKS,
                Math.max(0.0D, anchorDistance) + OUTWARD_BEYOND_READER_BLOCKS);
        int min = (int) Math.ceil(floor);
        int depth = (OUTWARD_STEP_MAX_BLOCKS - OUTWARD_STEP_MIN_BLOCKS) * (1 + Math.max(0, widening));
        return new DistanceBand(min, min + depth);
    }

    /**
     * Absolute x, z of an outward candidate: uniform over the band's ring around (0, 0), restricted to the side of
     * the anchor (the reader's home) unless it lives near the centre.
     *
     * @param angleUnit random value in [0, 1)
     * @param distanceUnit random value in [0, 1)
     */
    public static int[] outwardCandidate(
            double angleUnit, double distanceUnit, DistanceBand band, int anchorX, int anchorZ) {
        return outwardCandidate(angleUnit, distanceUnit, band, anchorX, anchorZ, 0);
    }

    /** Same draw, on a wider arc after {@code widening} searches that found no site (up to the whole ring). */
    public static int[] outwardCandidate(
            double angleUnit, double distanceUnit, DistanceBand band, int anchorX, int anchorZ, int widening) {
        int wider = 1 + Math.max(0, widening);
        double spread = Math.min(Math.min(Math.PI, OUTWARD_SPREAD_RADIANS * wider),
                OUTWARD_MAX_ARC_BLOCKS * wider / (double) band.minBlocks());
        double angle = Math.hypot(anchorX, anchorZ) < CENTRED_ANCHOR_RADIUS
                ? angleUnit * Math.PI * 2.0D
                : Math.atan2(anchorZ, anchorX) + (angleUnit * 2.0D - 1.0D) * spread;
        double min = band.minBlocks();
        double max = band.maxBlocks();
        double distance = Math.sqrt(min * min + distanceUnit * (max * max - min * min));
        return new int[] {(int) Math.round(Math.cos(angle) * distance), (int) Math.round(Math.sin(angle) * distance)};
    }

    /** Whether a site lies strictly farther from the world centre than every earlier place. */
    public static boolean isFartherThanEveryPlace(int x, int z, double farthestPlaceDistance) {
        return Math.hypot(x, z) > farthestPlaceDistance;
    }

    /** World mask for worlds that predate D-100: every journal found by anyone counts for everyone. */
    public static int migrateMask(Collection<Integer> perPlayerMasks) {
        int merged = 0;
        if (perPlayerMasks != null) {
            for (Integer mask : perPlayerMasks) {
                if (mask != null) {
                    merged |= mask;
                }
            }
        }
        return merged & COMPLETE_MASK;
    }

    /** Catalog volume of a journal recognised by its unchanged title, or {@code -1}. */
    public static int volumeForTitle(String title) {
        if (title == null) {
            return -1;
        }
        for (UncannyJournalCatalog.Journal journal : UncannyJournalCatalog.journals()) {
            if (journal.title().equals(title)) {
                return journal.index();
            }
        }
        return -1;
    }

    private static boolean isValidVolume(int volume) {
        return rankOf(volume) >= 0;
    }

    private static int bit(int volume) {
        return 1 << (volume - 1);
    }

    public enum PendingState {
        NONE,
        RESERVED,
        BUILT
    }

    public enum PlaceAction {
        /** Every journal has been found. */
        COMPLETE,
        /** The next journal is not yet allowed (minimum day or delay). */
        WAIT_FOR_ELIGIBILITY,
        /** Look for a site for the next journal. */
        SEARCH,
        /** A site is reserved but not built yet (no player far enough, chunk not loaded, server restart). */
        BUILD_RESERVED,
        /** The place stands; nothing happens until its journal is opened. */
        WAIT_FOR_READER
    }

    public enum StarterMapTier {
        PLAYER_CHEST,
        PLAYER_CHEST_OR_BARREL,
        ANY_UNOPENED_CONTAINER
    }

    public record DistanceBand(int minBlocks, int maxBlocks) {
        public DistanceBand {
            if (minBlocks <= 0 || maxBlocks <= minBlocks) {
                throw new IllegalArgumentException("Invalid distance band");
            }
        }
    }
}
