package com.eotv.echoofthevoid.lore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eotv.echoofthevoid.campaign.CampaignDirectorRules;
import com.eotv.echoofthevoid.lore.UncannyJournalProgressRules.StarterMapTier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class UncannyJournalProgressRulesTest {
    private static final long DAY = CampaignDirectorRules.TICKS_PER_DAY;
    private static final int STANDARD = CampaignDirectorRules.STANDARD_LENGTH_DAYS;
    private static final int EXTRA_LONG = CampaignDirectorRules.EXTRA_LONG_LENGTH_DAYS;

    @Test
    void discoveryOrderFollowsTheStoryNotTheCatalog() {
        List<String> titles = UncannyJournalProgressRules.DISCOVERY_ORDER.stream()
                .map(volume -> UncannyJournalCatalog.journal(volume).title())
                .toList();
        assertEquals(List.of(
                        "five torches",
                        "A Longer Way Home",
                        "Room for One",
                        "only dan heard it",
                        "The Recipe Isn't There",
                        "Under the Third Warning"),
                titles);
        assertEquals(UncannyJournalCatalog.count(), UncannyJournalProgressRules.DISCOVERY_ORDER.size());
        assertEquals(List.of(2, 5, 9, 13, 18, 24), UncannyJournalProgressRules.MINIMUM_STORY_DAY);
    }

    @Test
    void nextVolumeIsTheEarliestMissingJournalInStoryOrder() {
        assertEquals(1, UncannyJournalProgressRules.nextVolume(0));
        int mask = 0;
        for (int volume : UncannyJournalProgressRules.DISCOVERY_ORDER) {
            assertEquals(volume, UncannyJournalProgressRules.nextVolume(mask));
            mask = UncannyJournalProgressRules.withFound(mask, volume);
        }
        assertEquals(-1, UncannyJournalProgressRules.nextVolume(mask));
        assertTrue(UncannyJournalProgressRules.isComplete(mask));
        assertEquals(UncannyJournalProgressRules.COMPLETE_MASK, mask);
    }

    @Test
    void outOfOrderOldWorldsResumeAtTheSmallestMissingRank() {
        int mask = maskOf(3, 5);
        assertEquals(1, UncannyJournalProgressRules.nextVolume(mask));
        mask = maskOf(1, 2, 3, 5);
        assertEquals(6, UncannyJournalProgressRules.nextVolume(mask), "only dan heard it comes before the Recipe");
        mask = maskOf(1, 2, 3, 6, 5);
        assertEquals(4, UncannyJournalProgressRules.nextVolume(mask));
    }

    @Test
    void minimumStoryDayIsInclusive() {
        for (int rank = 0; rank < UncannyJournalProgressRules.DISCOVERY_ORDER.size(); rank++) {
            int volume = UncannyJournalProgressRules.DISCOVERY_ORDER.get(rank);
            long minimum = UncannyJournalProgressRules.MINIMUM_STORY_DAY.get(rank) * DAY;
            assertFalse(UncannyJournalProgressRules.isEligible(volume, minimum - 1L, -1L, STANDARD));
            assertTrue(UncannyJournalProgressRules.isEligible(volume, minimum, -1L, STANDARD));
        }
    }

    @Test
    void threeStoryDaysMustSeparateTwoDiscoveries() {
        long lastRead = 40L * DAY;
        assertFalse(UncannyJournalProgressRules.isEligible(2, lastRead + 3L * DAY - 1L, lastRead, STANDARD));
        assertTrue(UncannyJournalProgressRules.isEligible(2, lastRead + 3L * DAY, lastRead, STANDARD));
    }

    @Test
    void fastPlayerInAnAdvancedWorldStillNeedsFifteenDaysForAllJournals() {
        // A world installed late starts its campaign at the cap: only the delay protects the pacing.
        long elapsed = STANDARD * DAY;
        int mask = 0;
        long lastRead = -1L;
        int readCount = 0;
        long firstRead = -1L;
        for (long tick = elapsed; tick <= elapsed + 30L * DAY; tick += DAY / 4L) {
            int next = UncannyJournalProgressRules.nextVolume(mask);
            if (next < 0) {
                break;
            }
            if (UncannyJournalProgressRules.isEligible(next, tick, lastRead, STANDARD)) {
                mask = UncannyJournalProgressRules.withFound(mask, next);
                lastRead = tick;
                if (firstRead < 0L) {
                    firstRead = tick;
                }
                readCount++;
            }
        }
        assertEquals(6, readCount);
        assertEquals(15L * DAY, lastRead - firstRead);
    }

    @Test
    void extraLongCampaignStretchesDaysAndDelay() {
        assertFalse(UncannyJournalProgressRules.isEligible(1, 4L * DAY - 1L, -1L, EXTRA_LONG));
        assertTrue(UncannyJournalProgressRules.isEligible(1, 4L * DAY, -1L, EXTRA_LONG));
        long lastRead = 20L * DAY;
        assertFalse(UncannyJournalProgressRules.isEligible(2, lastRead + 6L * DAY - 1L, lastRead, EXTRA_LONG));
        assertTrue(UncannyJournalProgressRules.isEligible(2, lastRead + 6L * DAY, lastRead, EXTRA_LONG));
    }

    @Test
    void unknownVolumesAreNeverEligible() {
        assertFalse(UncannyJournalProgressRules.isEligible(0, 50L * DAY, -1L, STANDARD));
        assertFalse(UncannyJournalProgressRules.isEligible(7, 50L * DAY, -1L, STANDARD));
        assertEquals(0, UncannyJournalProgressRules.withFound(0, 7));
        assertFalse(UncannyJournalProgressRules.isFound(-1, 9));
    }

    @Test
    void starterMapTiersWidenAfterThreeAndSixStoryDays() {
        long reserved = 2L * DAY;
        assertEquals(StarterMapTier.PLAYER_CHEST,
                UncannyJournalProgressRules.starterMapTier(reserved, reserved, STANDARD));
        assertEquals(StarterMapTier.PLAYER_CHEST,
                UncannyJournalProgressRules.starterMapTier(reserved, reserved + 3L * DAY - 1L, STANDARD));
        assertEquals(StarterMapTier.PLAYER_CHEST_OR_BARREL,
                UncannyJournalProgressRules.starterMapTier(reserved, reserved + 3L * DAY, STANDARD));
        assertEquals(StarterMapTier.ANY_UNOPENED_CONTAINER,
                UncannyJournalProgressRules.starterMapTier(reserved, reserved + 6L * DAY, STANDARD));
        assertEquals(StarterMapTier.PLAYER_CHEST_OR_BARREL,
                UncannyJournalProgressRules.starterMapTier(reserved, reserved + 6L * DAY, EXTRA_LONG));
    }

    @Test
    void starterMapOnlyLeadsToTheFirstJournal() {
        assertTrue(UncannyJournalProgressRules.needsStarterMap(0));
        assertTrue(UncannyJournalProgressRules.needsStarterMap(maskOf(3, 5)));
        assertFalse(UncannyJournalProgressRules.needsStarterMap(maskOf(1)));
    }

    @Test
    void firstPlaceIsCloserThanTheOthers() {
        assertEquals(new UncannyJournalProgressRules.DistanceBand(150, 400),
                UncannyJournalProgressRules.distanceBand(1));
        for (int volume : List.of(2, 3, 4, 5, 6)) {
            assertEquals(new UncannyJournalProgressRules.DistanceBand(300, 800),
                    UncannyJournalProgressRules.distanceBand(volume));
        }
    }

    @Test
    void migrationMergesEveryPlayersJournals() {
        assertEquals(0, UncannyJournalProgressRules.migrateMask(List.of()));
        assertEquals(0, UncannyJournalProgressRules.migrateMask(null));
        assertEquals(maskOf(1), UncannyJournalProgressRules.migrateMask(List.of(maskOf(1))));
        assertEquals(maskOf(3, 5), UncannyJournalProgressRules.migrateMask(Arrays.asList(maskOf(3), null, maskOf(5))));
        assertEquals(UncannyJournalProgressRules.COMPLETE_MASK,
                UncannyJournalProgressRules.migrateMask(List.of(maskOf(1, 2, 3), maskOf(4, 5, 6), 0xFFFF)));
    }

    @Test
    void journalsAreRecognisedByTheirUnchangedTitles() {
        for (UncannyJournalCatalog.Journal journal : UncannyJournalCatalog.journals()) {
            assertEquals(journal.index(), UncannyJournalProgressRules.volumeForTitle(journal.title()));
        }
        assertEquals(-1, UncannyJournalProgressRules.volumeForTitle("Five Torches"));
        assertEquals(-1, UncannyJournalProgressRules.volumeForTitle(null));
    }

    @Test
    void placeActionFollowsThePendingPlaceThenEligibility() {
        long day = 10L * DAY;
        assertEquals(UncannyJournalProgressRules.PlaceAction.WAIT_FOR_READER,
                UncannyJournalProgressRules.nextAction(0, UncannyJournalProgressRules.PendingState.BUILT, day, -1L, STANDARD));
        assertEquals(UncannyJournalProgressRules.PlaceAction.BUILD_RESERVED,
                UncannyJournalProgressRules.nextAction(0, UncannyJournalProgressRules.PendingState.RESERVED, day, -1L, STANDARD));
        assertEquals(UncannyJournalProgressRules.PlaceAction.SEARCH,
                UncannyJournalProgressRules.nextAction(0, UncannyJournalProgressRules.PendingState.NONE, day, -1L, STANDARD));
        assertEquals(UncannyJournalProgressRules.PlaceAction.WAIT_FOR_ELIGIBILITY,
                UncannyJournalProgressRules.nextAction(0, UncannyJournalProgressRules.PendingState.NONE, DAY, -1L, STANDARD));
        assertEquals(UncannyJournalProgressRules.PlaceAction.WAIT_FOR_ELIGIBILITY,
                UncannyJournalProgressRules.nextAction(maskOf(1), UncannyJournalProgressRules.PendingState.NONE, day, day - DAY, STANDARD));
        assertEquals(UncannyJournalProgressRules.PlaceAction.COMPLETE,
                UncannyJournalProgressRules.nextAction(UncannyJournalProgressRules.COMPLETE_MASK,
                        UncannyJournalProgressRules.PendingState.NONE, day, -1L, STANDARD));
    }

    @Test
    void candidatesStayInsideTheirDistanceBand() {
        for (int volume : List.of(1, 2)) {
            UncannyJournalProgressRules.DistanceBand band = UncannyJournalProgressRules.distanceBand(volume);
            for (int a = 0; a < 20; a++) {
                for (int d = 0; d <= 20; d++) {
                    int[] offset = UncannyJournalProgressRules.candidateOffset(a / 20.0D, Math.min(0.999999D, d / 20.0D), band);
                    double distance = Math.hypot(offset[0], offset[1]);
                    assertTrue(distance >= band.minBlocks() - 1.0D && distance <= band.maxBlocks() + 1.0D,
                            "volume " + volume + " offset " + offset[0] + "," + offset[1]);
                }
            }
        }
    }

    @Test
    void laterPlacesAlwaysLieFartherFromTheWorldCentre() {
        double farthest = 0.0D;
        for (int place = 0; place < 6; place++) {
            UncannyJournalProgressRules.DistanceBand band = UncannyJournalProgressRules.outwardBand(farthest, 0.0D);
            assertTrue(band.minBlocks() >= farthest + UncannyJournalProgressRules.OUTWARD_STEP_MIN_BLOCKS,
                    "place " + place + " band " + band);
            double next = 0.0D;
            for (int a = 0; a < 20; a++) {
                for (int d = 0; d <= 20; d++) {
                    int[] xz = UncannyJournalProgressRules.outwardCandidate(a / 20.0D, Math.min(0.999999D, d / 20.0D),
                            band, 0, 0);
                    double distance = Math.hypot(xz[0], xz[1]);
                    assertTrue(distance >= band.minBlocks() - 1.0D && distance <= band.maxBlocks() + 1.0D,
                            "candidate " + xz[0] + "," + xz[1] + " outside " + band);
                    assertTrue(UncannyJournalProgressRules.isFartherThanEveryPlace(xz[0], xz[1], farthest));
                    next = Math.max(next, distance);
                }
            }
            farthest = next;
        }
        assertFalse(UncannyJournalProgressRules.isFartherThanEveryPlace(30, 40, 50.0D),
                "A site at the same distance as an earlier place is not farther");
    }

    @Test
    void outwardCandidatesStayOnTheReadersSideOfTheCentre() {
        UncannyJournalProgressRules.DistanceBand band = UncannyJournalProgressRules.outwardBand(500.0D, 100.0D);
        assertEquals(600, band.minBlocks(), "Beyond the farthest place when the reader lives near the centre");
        for (int a = 0; a < 20; a++) {
            int[] xz = UncannyJournalProgressRules.outwardCandidate(a / 20.0D, 0.5D, band, 100, 0);
            double angle = Math.atan2(xz[1], xz[0]);
            assertTrue(Math.abs(angle) <= UncannyJournalProgressRules.OUTWARD_SPREAD_RADIANS + 0.01D,
                    "A reader living east gets an eastern place: " + xz[0] + "," + xz[1]);
        }
        boolean west = false;
        for (int a = 0; a < 20; a++) {
            int[] xz = UncannyJournalProgressRules.outwardCandidate(a / 20.0D, 0.5D, band, 10, -20);
            west |= xz[0] < -100;
        }
        assertTrue(west, "A reader living at the centre may get a place in any direction");
    }

    @Test
    void failedSearchesWidenTheOutwardWindowUntilItFindsLand() {
        double home = Math.hypot(20000, 20000);
        UncannyJournalProgressRules.DistanceBand narrow = UncannyJournalProgressRules.outwardBand(30000.0D, home, 0);
        UncannyJournalProgressRules.DistanceBand wide = UncannyJournalProgressRules.outwardBand(30000.0D, home, 3);
        assertEquals(narrow.minBlocks(), wide.minBlocks(), "Still farther than every place");
        assertTrue(wide.maxBlocks() - wide.minBlocks() == 4 * (narrow.maxBlocks() - narrow.minBlocks()),
                "Each failed search adds a band's depth");
        double narrowestAngle = 0.0D;
        double widestAngle = 0.0D;
        for (int a = 0; a <= 20; a++) {
            int[] n = UncannyJournalProgressRules.outwardCandidate(a / 20.0D, 0.5D, narrow, 20000, 20000, 0);
            int[] w = UncannyJournalProgressRules.outwardCandidate(a / 20.0D, 0.5D, wide, 20000, 20000, 6);
            double base = Math.atan2(20000, 20000);
            narrowestAngle = Math.max(narrowestAngle, Math.abs(Math.atan2(n[1], n[0]) - base));
            widestAngle = Math.max(widestAngle, Math.abs(Math.atan2(w[1], w[0]) - base));
            assertTrue(UncannyJournalProgressRules.isFartherThanEveryPlace(w[0], w[1], 30000.0D));
        }
        assertTrue(widestAngle > narrowestAngle * 5, "The arc opens up: " + narrowestAngle + " -> " + widestAngle);
    }

    @Test
    void aReaderFarFromTheCentreIsNotSentAcrossTheWorld() {
        int homeX = 7000;
        int homeZ = 6900;
        double home = Math.hypot(homeX, homeZ);
        UncannyJournalProgressRules.DistanceBand band = UncannyJournalProgressRules.outwardBand(800.0D, home);
        assertTrue(band.minBlocks() >= home + UncannyJournalProgressRules.OUTWARD_BEYOND_READER_BLOCKS,
                "The next place lies beyond the far reader's home, not back near the centre");
        for (int a = 0; a < 20; a++) {
            for (int d = 0; d <= 20; d++) {
                int[] xz = UncannyJournalProgressRules.outwardCandidate(a / 20.0D, Math.min(0.999999D, d / 20.0D),
                        band, homeX, homeZ);
                double fromHome = Math.hypot(xz[0] - homeX, xz[1] - homeZ);
                assertTrue(fromHome >= 250.0D && fromHome <= 800.0D,
                        "A far reader's next place is a trip away, not across the world: " + Math.round(fromHome));
            }
        }
    }

    private static int maskOf(int... volumes) {
        int mask = 0;
        for (int volume : volumes) {
            mask |= 1 << (volume - 1);
        }
        return mask;
    }
}
