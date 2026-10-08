package com.eotv.echoofthevoid.event.paranoia.message;

import java.util.List;

/** Three-step, internally coherent messages for the rare bed disturbance. */
public final class SleepDisturbanceMessageCatalog {
    private static final List<List<String>> VARIANTS = List.of(
            List.of(
                    "There is something in your bed.",
                    "It did not move.",
                    "There is still no room."),
            List.of(
                    "The bed is already occupied.",
                    "It moved when you touched the bed.",
                    "It is waiting for you to leave."),
            List.of(
                    "Your side is still warm.",
                    "The other side is warmer.",
                    "Something just got up."),
            List.of(
                    "This is not where you placed your bed.",
                    "The room is right. The bed is not.",
                    "It was facing the other way."),
            List.of(
                    "You are already sleeping.",
                    "You can see yourself from here.",
                    "Do not wake the other one."),
            List.of(
                    "The bed creaked before you touched it.",
                    "It moved again.",
                    "There is no room left."),
            List.of(
                    "Something is breathing under the bed.",
                    "It stopped when you moved.",
                    "It is holding its breath."),
            List.of(
                    "The blanket moved.",
                    "It moved back.",
                    "It knows you noticed."),
            List.of(
                    "Someone used this bed.",
                    "They did not leave.",
                    "They are lying very still."),
            List.of(
                    "You heard the bed creak behind you.",
                    "It creaked again.",
                    "You are not touching it."),
            List.of(
                    "The pillow is lower than before.",
                    "Something lifted its head.",
                    "It is looking at you now."),
            List.of(
                    "This bed is occupied.",
                    "There is no one here.",
                    "The bed still disagrees."));

    private SleepDisturbanceMessageCatalog() {
    }

    public static int variantCount() {
        return VARIANTS.size();
    }

    public static List<List<String>> variants() {
        return VARIANTS;
    }

    public static String message(int variant, int attemptIndex) {
        List<String> sequence = VARIANTS.get(Math.floorMod(variant, VARIANTS.size()));
        return sequence.get(Math.max(0, Math.min(sequence.size() - 1, attemptIndex)));
    }

    public static List<String> flattenedMessages() {
        return VARIANTS.stream().flatMap(List::stream).toList();
    }
}
