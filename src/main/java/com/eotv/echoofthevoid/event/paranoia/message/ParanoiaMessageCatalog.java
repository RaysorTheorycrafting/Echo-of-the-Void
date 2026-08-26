package com.eotv.echoofthevoid.event.paranoia.message;

import static com.eotv.echoofthevoid.event.paranoia.ParanoiaEventIds.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Immutable, context-bound text data. No entry in this catalog is a free random claim. */
public final class ParanoiaMessageCatalog {
    private static final Map<ParanoiaMessageContext, List<String>> MESSAGES = Map.of(
            ParanoiaMessageContext.OBSERVATION, List.of(
                    "You were supposed to miss that.",
                    "It moved when you looked away.",
                    "Something changed while you were not looking.",
                    "Look again.",
                    "It is back where it was.",
                    "You remember it differently.",
                    "It stopped because you saw it.",
                    "You looked too late.",
                    "Don't turn around."),
            ParanoiaMessageContext.CAVE, List.of(
                    "The cave heard that.",
                    "That knock came from inside.",
                    "You did not break the last block.",
                    "It stopped when you stopped.",
                    "The wall answered.",
                    "The vein moved.",
                    "There was another swing.",
                    "Something is using your rhythm."),
            ParanoiaMessageContext.BASE, List.of(
                    "You left this open.",
                    "It was here before you returned.",
                    "This room was empty.",
                    "Something used the room.",
                    "You do not remember placing that.",
                    "Your door opened for a reason.",
                    "Something waited inside.",
                    "The furnace was cold when you left."),
            ParanoiaMessageContext.CONTAINER, List.of(
                    "Not this one.",
                    "You already opened this.",
                    "You closed it.",
                    "That was not the chest you heard.",
                    "The other one opened.",
                    "Something finished before you did.",
                    "Nothing was missing."),
            ParanoiaMessageContext.SLEEP, List.of(
                    "There is something in your bed.",
                    "You were not the first to wake.",
                    "This side is still warm.",
                    "Something stood here while you slept.",
                    "The room was different before morning."),
            ParanoiaMessageContext.WEATHER, List.of(
                    "The ground is still dry.",
                    "Only the sound stopped.",
                    "It stopped before the sky changed.",
                    "That was not weather.",
                    "The thunder came from below."),
            ParanoiaMessageContext.ANIMAL, List.of(
                    "It was looking before you turned.",
                    "Your pet is looking past you.",
                    "That sound did not come from it.",
                    "It moved after you stopped watching.",
                    "It was closer the second time.",
                    "It noticed that you noticed.",
                    "Not gone."),
            ParanoiaMessageContext.REPETITION, List.of(
                    "Again.",
                    "Not the same one.",
                    "You heard this before.",
                    "It took the same path.",
                    "It stopped earlier last time."));

    private static final Map<String, MessageRule> EVENT_RULES = buildEventRules();
    private static final List<String> FALSE_RECIPE_BODIES = List.of(
            "Recipe remembered.",
            "This recipe was removed.",
            "Result hidden.",
            "Not available in this world.",
            "You already made this.",
            "The ingredient was accepted.");

    private ParanoiaMessageCatalog() {
    }

    public static List<String> messages(ParanoiaMessageContext context) {
        return MESSAGES.getOrDefault(context, List.of());
    }

    public static Optional<MessageRule> ruleForEvent(String eventId) {
        return Optional.ofNullable(EVENT_RULES.get(eventId));
    }

    public static List<String> falseRecipeBodies() {
        return FALSE_RECIPE_BODIES;
    }

    public static int totalMessageCount() {
        return MESSAGES.values().stream().mapToInt(List::size).sum();
    }

    private static Map<String, MessageRule> buildEventRules() {
        Map<String, MessageRule> rules = new LinkedHashMap<>();

        add(rules, GHOST_MINER, ParanoiaMessageContext.CAVE, 0.16D);
        add(rules, CAVE_COLLAPSE, ParanoiaMessageContext.CAVE, 0.10D);
        add(rules, GHOST_BREAKING, ParanoiaMessageContext.CAVE, 0.16D);
        add(rules, LIVING_ORE, ParanoiaMessageContext.CAVE, 0.12D);
        add(rules, TOOL_ANSWER, ParanoiaMessageContext.CAVE, 0.18D);

        add(rules, BASE_REPLAY, ParanoiaMessageContext.BASE, 0.16D);
        add(rules, BEDSIDE_OPEN, ParanoiaMessageContext.SLEEP, 0.12D);
        add(rules, COLD_FURNACE, ParanoiaMessageContext.BASE, 0.18D);
        add(rules, MISPLACED_LIGHT, ParanoiaMessageContext.BASE, 0.14D);
        add(rules, PET_REFUSAL, ParanoiaMessageContext.ANIMAL, 0.12D);

        add(rules, FALSE_CONTAINER_OPEN, ParanoiaMessageContext.CONTAINER, 0.14D);
        add(rules, LEVER_ANSWER, ParanoiaMessageContext.CONTAINER, 0.10D);
        add(rules, PRESSURE_PLATE_REPLY, ParanoiaMessageContext.CONTAINER, 0.10D);
        add(rules, FURNACE_BREATH, ParanoiaMessageContext.BASE, 0.10D);
        add(rules, CAMPFIRE_COUGH, ParanoiaMessageContext.BASE, 0.08D);

        add(rules, ANIMAL_STARE_LOCK, ParanoiaMessageContext.ANIMAL, 0.18D);
        add(rules, FALSE_ANIMAL_HURT, ParanoiaMessageContext.ANIMAL, 0.16D);
        add(rules, STOLEN_POSE, ParanoiaMessageContext.ANIMAL, 0.14D);
        add(rules, EMPTY_CONGREGATION, ParanoiaMessageContext.ANIMAL, 0.18D);

        add(rules, ORPHAN_SHADOW, ParanoiaMessageContext.OBSERVATION, 0.12D);
        add(rules, EMPTY_TELEPORT, ParanoiaMessageContext.OBSERVATION, 0.10D);
        add(rules, PROJECTED_SHADOW, ParanoiaMessageContext.OBSERVATION, 0.12D);
        add(rules, FALSE_FALL, ParanoiaMessageContext.OBSERVATION, 0.08D);
        add(rules, FALSE_INJURY, ParanoiaMessageContext.OBSERVATION, 0.08D);

        return Map.copyOf(rules);
    }

    private static void add(
            Map<String, MessageRule> rules,
            String eventId,
            ParanoiaMessageContext context,
            double naturalChance) {
        if (rules.put(eventId, new MessageRule(context, naturalChance)) != null) {
            throw new IllegalStateException("Duplicate message rule: " + eventId);
        }
    }

    public record MessageRule(ParanoiaMessageContext context, double naturalChance) {
        public MessageRule {
            if (context == null || naturalChance < 0.0D || naturalChance > 1.0D) {
                throw new IllegalArgumentException("Invalid paranoia message rule");
            }
        }
    }
}
