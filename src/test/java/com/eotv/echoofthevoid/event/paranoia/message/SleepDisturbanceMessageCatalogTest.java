package com.eotv.echoofthevoid.event.paranoia.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class SleepDisturbanceMessageCatalogTest {
    @Test
    void everyVariantIsACompleteThreeAttemptSequence() {
        assertEquals(12, SleepDisturbanceMessageCatalog.variantCount());
        assertTrue(SleepDisturbanceMessageCatalog.variants().stream()
                .allMatch(sequence -> sequence.size() == 3));
        assertEquals(36, SleepDisturbanceMessageCatalog.flattenedMessages().size());
        assertEquals(36, new HashSet<>(SleepDisturbanceMessageCatalog.flattenedMessages()).size());
    }

    @Test
    void originalMessageRemainsAvailableAndSelectionIsDeterministic() {
        assertEquals(
                "There is something in your bed.",
                SleepDisturbanceMessageCatalog.message(0, 0));
        assertEquals("It did not move.", SleepDisturbanceMessageCatalog.message(0, 1));
        assertEquals("There is still no room.", SleepDisturbanceMessageCatalog.message(0, 2));
        assertEquals(
                SleepDisturbanceMessageCatalog.message(0, 0),
                SleepDisturbanceMessageCatalog.message(12, -1));
    }
}
