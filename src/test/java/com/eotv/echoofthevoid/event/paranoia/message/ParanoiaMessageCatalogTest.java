package com.eotv.echoofthevoid.event.paranoia.message;

import static com.eotv.echoofthevoid.event.paranoia.ParanoiaEventIds.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ParanoiaMessageCatalogTest {
    @Test
    void everyLineIsUniqueAcrossEventsAndCorruptMessageContexts() {
        Set<String> all = new HashSet<>();
        for (ParanoiaMessageContext context : ParanoiaMessageContext.values()) {
            for (String message : ParanoiaMessageCatalog.messages(context)) {
                assertFalse(message.isBlank());
                assertTrue(all.add(message), "duplicate message: " + message);
            }
        }
        for (Map.Entry<String, ParanoiaMessageCatalog.MessageRule> entry
                : ParanoiaMessageCatalog.eventRules().entrySet()) {
            assertTrue(entry.getValue().lines().size() >= 2, entry.getKey());
            for (String message : entry.getValue().lines()) {
                assertFalse(message.isBlank());
                assertTrue(all.add(message), "duplicate message: " + message);
            }
        }
        assertEquals(all.size(), ParanoiaMessageCatalog.totalMessageCount());
    }

    @Test
    void eventsOnlyDrawTheirOwnLinesNeverAnotherEventsPool() {
        // Regression (2026-10-08): a lever answer drew "You already opened this." from the shared
        // container pool. A line may now only describe the event that produced it.
        List<String> lever = ParanoiaMessageCatalog.ruleForEvent(LEVER_ANSWER).orElseThrow().lines();
        assertFalse(lever.contains("You already opened this."));
        assertFalse(lever.stream().anyMatch(line -> line.toLowerCase().contains("chest")));
        List<String> container = ParanoiaMessageCatalog.ruleForEvent(FALSE_CONTAINER_OPEN).orElseThrow().lines();
        assertTrue(container.stream().noneMatch(lever::contains));
        assertTrue(ParanoiaMessageCatalog.ruleForEvent(FALSE_FALL).orElseThrow().lines().contains("You did not fall."));
    }

    @Test
    void delayedEventsSpeakOnlyAfterTheirEffect() {
        assertTrue(ParanoiaMessageCatalog.ruleForEvent(LEVER_ANSWER).orElseThrow().delayTicks() > 20,
                "the lever answers after 20 ticks");
        assertTrue(ParanoiaMessageCatalog.ruleForEvent(TOOL_ANSWER).orElseThrow().delayTicks() > 40,
                "the third tool swing plays at 40 ticks");
        assertTrue(ParanoiaMessageCatalog.ruleForEvent(GHOST_MINER).orElseThrow().delayTicks() >= 200,
                "several phantom swings must be heard first");
        assertTrue(ParanoiaMessageCatalog.ruleForEvent(LIVING_ORE).orElseThrow().delayTicks() > 25 * 20,
                "the ore reacts after 25 seconds");
    }

    @Test
    void corruptMessageContextsNeverAssertAConcreteCheckableChange() {
        assertTrue(ParanoiaMessageCatalog.messages(ParanoiaMessageContext.CONTAINER).isEmpty());
        assertTrue(ParanoiaMessageCatalog.messages(ParanoiaMessageContext.ANIMAL).isEmpty());
        for (String forbidden : List.of(
                "You did not break the last block.",
                "The furnace was cold when you left.",
                "Your door opened for a reason.",
                "You do not remember placing that.",
                "You left this open.")) {
            for (ParanoiaMessageContext context : ParanoiaMessageContext.values()) {
                assertFalse(ParanoiaMessageCatalog.messages(context).contains(forbidden), forbidden);
            }
        }
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
