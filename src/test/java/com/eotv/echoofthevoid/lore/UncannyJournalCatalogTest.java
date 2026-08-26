package com.eotv.echoofthevoid.lore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eotv.echoofthevoid.campaign.CampaignDirectorRules;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class UncannyJournalCatalogTest {
    @Test
    void validatedTitlesWindowsAndDistinctVoicesAreStable() {
        assertEquals(List.of(
                        "five torches",
                        "A Longer Way Home",
                        "Room for One",
                        "The Recipe Isn't There",
                        "Under the Third Warning",
                        "only dan heard it"),
                UncannyJournalCatalog.journals().stream().map(UncannyJournalCatalog.Journal::title).toList());
        assertEquals(List.of(2, 4, 8, 15, 22, 10),
                UncannyJournalCatalog.journals().stream()
                        .map(UncannyJournalCatalog.Journal::windowStartDay).toList());
        assertEquals(List.of(14, 20, 28, 40, 50, 45),
                UncannyJournalCatalog.journals().stream()
                        .map(UncannyJournalCatalog.Journal::windowEndDay).toList());
        assertTrue(UncannyJournalCatalog.journal(2).pages().get(2)
                .contains("There was no wind mod. I checked my mod list twice anyway."));
        assertTrue(UncannyJournalCatalog.journal(4).pages().stream().anyMatch(page -> page.contains("NEI")));
        assertTrue(UncannyJournalCatalog.journal(1).pages().get(0).startsWith("i had 5 torches"));
        assertTrue(UncannyJournalCatalog.journal(6).pages().get(8).equals("lou did not write on this page"));
        assertFalse(UncannyJournalCatalog.journals().stream()
                .flatMap(journal -> journal.pages().stream())
                .map(page -> page.toLowerCase(Locale.ROOT))
                .anyMatch(page -> page.contains("concordant")));
    }

    @Test
    void everyValidatedTitleAndPageIsLockedAsOneExactCatalog() throws NoSuchAlgorithmException {
        byte[] content = UncannyJournalCatalog.journals().toString().getBytes(StandardCharsets.UTF_8);
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        assertEquals("2c3948f524563eb3173f2ec316b06339187507015e07dc9a0b7412419340d23f", digest);
    }

    @Test
    void temporalWeightsOverlapWithoutCreatingHardGates() {
        UncannyJournalCatalog.Journal first = UncannyJournalCatalog.journal(1);
        assertEquals(1, UncannyJournalCatalog.discoveryWeight(first, 1.99D));
        assertEquals(6, UncannyJournalCatalog.discoveryWeight(first, 2.0D));
        assertEquals(6, UncannyJournalCatalog.discoveryWeight(first, 14.0D));
        assertEquals(3, UncannyJournalCatalog.discoveryWeight(first, 14.01D));

        assertEquals(2, UncannyJournalCatalog.selectMissing(0, 6, 1.0D, 0.20D));
        assertEquals(3, UncannyJournalCatalog.selectMissing(0, 6, 10.0D, 0.50D));
        assertEquals(6, UncannyJournalCatalog.selectMissing(0b01_1111, 6, 0.0D, 0.0D));
        assertEquals(-1, UncannyJournalCatalog.selectMissing(0b11_1111, 6, 25.0D, 0.5D));
        assertThrows(IllegalArgumentException.class,
                () -> UncannyJournalCatalog.selectMissing(0, 6, 5.0D, 1.0D));
    }

    @Test
    void extraLongCampaignUsesTheSameNormalizedStoryWindows() {
        long standardMidpoint = 25L * CampaignDirectorRules.TICKS_PER_DAY;
        long extraLongMidpoint = 50L * CampaignDirectorRules.TICKS_PER_DAY;
        assertEquals(25.0D, CampaignDirectorRules.logicalStoryDay(standardMidpoint, 50), 0.0D);
        assertEquals(25.0D, CampaignDirectorRules.logicalStoryDay(extraLongMidpoint, 100), 0.0D);
        assertEquals(50.0D, CampaignDirectorRules.logicalStoryDay(Long.MAX_VALUE, 50), 0.0D);
    }

    @Test
    void runtimeUsesExactChanceAndMarksOnlyAfterSuccessfulInsertion() throws IOException {
        String source = Files.readString(Path.of("src", "main", "java", "com", "eotv", "echoofthevoid",
                "event", "UncannyStructureFeatureSystem.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("HISTORY_BOOK_CHANCE = 0.50D"));
        assertTrue(source.contains("if (roll >= HISTORY_BOOK_CHANCE)"));
        assertTrue(source.contains("findWeightedMissingHistoryTome"));
        int insert = source.indexOf("insertIntoChestOrDrop(level, pos, chest, historyPiece)");
        int mark = source.indexOf("state.markHistoryTomeFound(player.getUUID(), nextTome)");
        assertTrue(insert >= 0 && mark > insert, "Discovery must be persisted only after insertion succeeds");
    }
}
