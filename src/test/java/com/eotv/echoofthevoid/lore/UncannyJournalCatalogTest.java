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
        assertTrue(UncannyJournalCatalog.journal(4).pages().stream().anyMatch(page -> page.contains("JEI"))
                && UncannyJournalCatalog.journal(4).pages().stream().noneMatch(page -> page.contains("NEI")));
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
        // 2026-10-06: journal IV "NEI" -> "JEI", explicitly validated by the user (D-80).
        assertEquals("33e87e409f733d3e4c4c9ead2469f9b9001f8b7faac232e130e4c2c906773468", digest);
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
    void ordinaryStructureChestsNeverGiveJournalsAndDecideTheirContentOnce() throws IOException {
        String source = Files.readString(Path.of("src", "main", "java", "com", "eotv", "echoofthevoid",
                "event", "UncannyStructureFeatureSystem.java"), StandardCharsets.UTF_8);
        assertFalse(source.contains("UNCANNY_LORE_PIECE"), "D-100: ordinary structures hold no journal");
        assertFalse(source.contains("HISTORY_BOOK_CHANCE"));
        assertTrue(source.contains("STRUCTURE_CHEST_MAP_CHANCE = 0.40D"));
        assertTrue(source.contains("STRUCTURE_CHEST_MIN_FRAGMENTS = 1"));
        assertTrue(source.contains("STRUCTURE_CHEST_MAX_FRAGMENTS = 3"));
        int resolved = source.indexOf("data.putBoolean(STRUCTURE_CHEST_RESOLVED, true);");
        int map = source.indexOf("UncannyJournalMaps.mapTowardsPendingPlace(level)");
        assertTrue(resolved >= 0 && map > resolved, "The chest is marked resolved before anything is rolled into it");
    }
}
