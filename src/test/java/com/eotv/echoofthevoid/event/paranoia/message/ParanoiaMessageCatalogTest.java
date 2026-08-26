package com.eotv.echoofthevoid.event.paranoia.message;

import static com.eotv.echoofthevoid.event.paranoia.ParanoiaEventIds.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ParanoiaMessageCatalogTest {
    @Test
    void everyNaturalMessageBelongsToAnExplicitContextAndIsUnique() {
        Set<String> all = new HashSet<>();
        for (ParanoiaMessageContext context : ParanoiaMessageContext.values()) {
            List<String> messages = ParanoiaMessageCatalog.messages(context);
            assertFalse(messages.isEmpty(), context.name());
            for (String message : messages) {
                assertFalse(message.isBlank());
                assertTrue(all.add(message), "duplicate message: " + message);
            }
        }
        assertEquals(54, ParanoiaMessageCatalog.totalMessageCount());
    }

    @Test
    void eventRulesUseSparseApprovedProbabilities() {
        assertEquals(ParanoiaMessageContext.CAVE,
                ParanoiaMessageCatalog.ruleForEvent(GHOST_MINER).orElseThrow().context());
        assertEquals(ParanoiaMessageContext.BASE,
                ParanoiaMessageCatalog.ruleForEvent(BASE_REPLAY).orElseThrow().context());
        assertEquals(ParanoiaMessageContext.ANIMAL,
                ParanoiaMessageCatalog.ruleForEvent(FALSE_ANIMAL_HURT).orElseThrow().context());
        assertTrue(ParanoiaMessageCatalog.ruleForEvent(GHOST_MINER).orElseThrow().naturalChance() >= 0.08D);
        assertTrue(ParanoiaMessageCatalog.ruleForEvent(GHOST_MINER).orElseThrow().naturalChance() <= 0.20D);
        assertTrue(ParanoiaMessageCatalog.ruleForEvent(CAMPFIRE_COUGH).orElseThrow().naturalChance() >= 0.08D);
        assertTrue(ParanoiaMessageCatalog.ruleForEvent(FOOTSTEPS).isEmpty());
        assertTrue(ParanoiaMessageCatalog.ruleForEvent(CORRUPT_MESSAGE).isEmpty());
    }

    @Test
    void falseRecipeToastNoLongerUsesThreatMessages() {
        assertEquals(6, ParanoiaMessageCatalog.falseRecipeBodies().size());
        assertFalse(ParanoiaMessageCatalog.falseRecipeBodies().contains("Don't turn around."));
    }
}
