package com.eotv.echoofthevoid.event.paranoia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class WeightedSelectorTest {
    private static final List<String> EXPECTED_SEEDED_SEQUENCE = List.of(
            "flash_red",
            "corrupt_message",
            "corrupt_message",
            "footsteps",
            "footsteps",
            "corrupt_message",
            "footsteps",
            "corrupt_message",
            "flash_red",
            "corrupt_message",
            "footsteps",
            "corrupt_message",
            "footsteps",
            "corrupt_message",
            "footsteps",
            "flash_red",
            "footsteps",
            "footsteps",
            "footsteps",
            "corrupt_message");

    @Test
    void boundaryRollsMatch111SubtractiveSelection() {
        List<WeightedChoice<String>> choices = List.of(
                new WeightedChoice<>("a", 2),
                new WeightedChoice<>("b", 3),
                new WeightedChoice<>("c", 1));

        assertEquals(6, WeightedSelector.totalWeight(choices));
        assertEquals("a", WeightedSelector.pick(choices, 0));
        assertEquals("a", WeightedSelector.pick(choices, 1));
        assertEquals("b", WeightedSelector.pick(choices, 2));
        assertEquals("b", WeightedSelector.pick(choices, 4));
        assertEquals("c", WeightedSelector.pick(choices, 5));
        assertNull(WeightedSelector.pick(choices, 6));

        assertEquals(0, WeightedSelector.pickIndex(new int[] {2, 3, 1}, 0));
        assertEquals(1, WeightedSelector.pickIndex(new int[] {2, 3, 1}, 4));
        assertEquals(2, WeightedSelector.pickIndex(new int[] {2, 3, 1}, 5));
        assertEquals(-1, WeightedSelector.pickIndex(new int[] {2, 3, 1}, 6));
    }

    @Test
    void controlledSeedProducesStableSelectionSequence() {
        List<WeightedChoice<String>> choices = List.of(
                new WeightedChoice<>("footsteps", 16),
                new WeightedChoice<>("corrupt_message", 18),
                new WeightedChoice<>("flash_red", 8),
                new WeightedChoice<>("false_fall", 9));
        Random random = new Random(0xE07F0111L);
        List<String> selected = new ArrayList<>();
        int total = WeightedSelector.totalWeight(choices);
        for (int index = 0; index < 20; index++) {
            selected.add(WeightedSelector.pick(choices, random.nextInt(total)));
        }
        assertEquals(EXPECTED_SEEDED_SEQUENCE, selected);
    }
}
