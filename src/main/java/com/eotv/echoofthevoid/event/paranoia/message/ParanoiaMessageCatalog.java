package com.eotv.echoofthevoid.event.paranoia.message;

import static com.eotv.echoofthevoid.event.paranoia.ParanoiaEventIds.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable, context-bound text data. No entry in this catalog is a free random claim.
 *
 * <p>An event message describes what that specific event has just done, and is delivered only
 * once its effect has had time to be perceived. The context pools below are reserved for the
 * standalone Corrupt Message, so they only hold lines that stay ambiguous in their situation and
 * never assert a concrete change the player could check and find false.</p>
 */
public final class ParanoiaMessageCatalog {
    private static final Map<ParanoiaMessageContext, List<String>> MESSAGES = Map.of(
            ParanoiaMessageContext.OBSERVATION, List.of(
                    "Look again.",
                    "Something changed while you were not looking.",
                    "You looked too late.",
                    "Don't turn around."),
            ParanoiaMessageContext.CAVE, List.of(
                    "The cave heard you.",
                    "Not every echo down here is yours.",
                    "Something else is down here."),
            ParanoiaMessageContext.BASE, List.of(
                    "This room was empty.",
                    "Something used the room.",
                    "Something waited inside.",
                    "It was here before you returned."),
            ParanoiaMessageContext.SLEEP, List.of(
                    "You were not the first to wake.",
                    "Something stood here while you slept.",
                    "The room was different before morning."),
            ParanoiaMessageContext.WEATHER, List.of(
                    "That was not weather.",
                    "The thunder came from below."));

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

    /** Context pool for the standalone Corrupt Message; empty for event-only contexts. */
    public static List<String> messages(ParanoiaMessageContext context) {
        return MESSAGES.getOrDefault(context, List.of());
    }

    public static Optional<MessageRule> ruleForEvent(String eventId) {
        return Optional.ofNullable(EVENT_RULES.get(eventId));
    }

    public static Map<String, MessageRule> eventRules() {
        return EVENT_RULES;
    }

    public static List<String> falseRecipeBodies() {
        return FALSE_RECIPE_BODIES;
    }

    public static int totalMessageCount() {
        return MESSAGES.values().stream().mapToInt(List::size).sum()
                + EVENT_RULES.values().stream().mapToInt(rule -> rule.lines().size()).sum();
    }

    private static Map<String, MessageRule> buildEventRules() {
        Map<String, MessageRule> rules = new LinkedHashMap<>();

        // Phantom mining approaches for 15-35 s: speak once several swings have been heard.
        add(rules, GHOST_MINER, ParanoiaMessageContext.CAVE, 0.16D, 200,
                "Someone else is mining down here.",
                "There was another swing.",
                "It came closer with every swing.");
        // Only an image of a falling ceiling: nothing actually moved.
        add(rules, CAVE_COLLAPSE, ParanoiaMessageContext.CAVE, 0.10D, 60,
                "Nothing fell.",
                "The ceiling is still there.",
                "Look up again.");
        // Cracks and impacts on a block that is never broken.
        add(rules, GHOST_BREAKING, ParanoiaMessageContext.CAVE, 0.16D, 80,
                "You did not break that block.",
                "Something was mining that block.",
                "The cracks are gone.");
        // A mined ore arms, then reacts about 25 s later.
        add(rules, LIVING_ORE, ParanoiaMessageContext.CAVE, 0.12D, 540,
                "That ore was not empty.",
                "The vein moved.",
                "Something was still inside the stone.");
        // Three swings of the player's own tool, 0/20/40 ticks after mining.
        add(rules, TOOL_ANSWER, ParanoiaMessageContext.CAVE, 0.18D, 60,
                "Your tool answered.",
                "Something is using your rhythm.",
                "That swing was not yours.");

        // Sounds of the player's own workstations being used near the base.
        add(rules, BASE_REPLAY, ParanoiaMessageContext.BASE, 0.16D, 80,
                "Someone used your things.",
                "Someone is working in your base.",
                "That was not you.");
        // The door nearest the bed opens while the player sleeps.
        add(rules, BEDSIDE_OPEN, ParanoiaMessageContext.SLEEP, 0.12D, 40,
                "Your door opened for a reason.",
                "The door was closed when you lay down.",
                "Something let itself in.");
        // A cold furnace shows the lit state without any fuel.
        add(rules, COLD_FURNACE, ParanoiaMessageContext.BASE, 0.18D, 40,
                "The furnace was cold when you left.",
                "Nothing is burning in there.",
                "Someone lit it for you.");
        // Exactly one light source moves, then returns within 15 s.
        add(rules, MISPLACED_LIGHT, ParanoiaMessageContext.BASE, 0.14D, 40,
                "That light was somewhere else.",
                "You do not remember placing that.",
                "It will be back where it was.");
        // A tamed pet refuses to follow for 8-15 s.
        add(rules, PET_REFUSAL, ParanoiaMessageContext.ANIMAL, 0.12D, 60,
                "Your pet is looking past you.",
                "It will not come to you.",
                "It is waiting for someone else.");

        // A container opening is heard behind the player.
        add(rules, FALSE_CONTAINER_OPEN, ParanoiaMessageContext.CONTAINER, 0.14D, 30,
                "That was not the chest you heard.",
                "The other one opened.",
                "Nothing was missing.");
        // A lever click answers about one second later; there may be no lever in sight.
        add(rules, LEVER_ANSWER, ParanoiaMessageContext.CONTAINER, 0.10D, 40,
                "Something answered.",
                "That click was not yours.",
                "It is copying you.");
        // A pressure plate is heard after the player's own step.
        add(rules, PRESSURE_PLATE_REPLY, ParanoiaMessageContext.CONTAINER, 0.10D, 30,
                "Something stepped on it after you.",
                "Someone is following your steps.",
                "That plate was already pressed.");
        // One or two breaths from an idle furnace.
        add(rules, FURNACE_BREATH, ParanoiaMessageContext.BASE, 0.10D, 50,
                "The furnace breathed.",
                "Something is breathing in there.",
                "It is still warm inside.");
        // A cough from a campfire.
        add(rules, CAMPFIRE_COUGH, ParanoiaMessageContext.BASE, 0.08D, 50,
                "The fire coughed.",
                "Something is sitting by the fire.",
                "That was not the smoke.");

        // Nearby animals stop and stare for 15 s.
        add(rules, ANIMAL_STARE_LOCK, ParanoiaMessageContext.ANIMAL, 0.18D, 60,
                "It was looking before you turned.",
                "They are not looking at you.",
                "It noticed that you noticed.");
        // An unharmed animal flinches and faces an empty point.
        add(rules, FALSE_ANIMAL_HURT, ParanoiaMessageContext.ANIMAL, 0.16D, 30,
                "Nothing touched it.",
                "It is not hurt.",
                "It saw something you did not.");
        // Three villagers stop in sequence and look at the same empty air.
        add(rules, EMPTY_CONGREGATION, ParanoiaMessageContext.ANIMAL, 0.18D, 120,
                "They are all looking at the same place.",
                "There is nothing there.",
                "They know where it is standing.");

        // A real entity shadow on empty ground, until someone looks at it.
        add(rules, ORPHAN_SHADOW, ParanoiaMessageContext.OBSERVATION, 0.12D, 20,
                "Look at the ground.",
                "There is a shadow with no one above it.",
                "Something is standing there.");
        // An Enderman teleport trace with no Enderman.
        add(rules, EMPTY_TELEPORT, ParanoiaMessageContext.OBSERVATION, 0.10D, 20,
                "Something just arrived.",
                "It was right there.",
                "Not gone.");
        // A shadow copy of the player mimics or strikes.
        add(rules, PROJECTED_SHADOW, ParanoiaMessageContext.OBSERVATION, 0.12D, 40,
                "Your shadow moved without you.",
                "It was copying you.",
                "That was your shape.");
        // Only the sound of a small fall: no movement, effect or damage.
        add(rules, FALSE_FALL, ParanoiaMessageContext.OBSERVATION, 0.08D, 10,
                "You did not fall.",
                "That was not your landing.",
                "Your feet never left the ground.");
        // Hurt feedback with no damage at all.
        add(rules, FALSE_INJURY, ParanoiaMessageContext.OBSERVATION, 0.08D, 10,
                "You are not hurt.",
                "Nothing touched you.",
                "Check again.");

        return Map.copyOf(rules);
    }

    private static void add(
            Map<String, MessageRule> rules,
            String eventId,
            ParanoiaMessageContext context,
            double naturalChance,
            int delayTicks,
            String... lines) {
        if (rules.put(eventId, new MessageRule(context, naturalChance, delayTicks, List.of(lines))) != null) {
            throw new IllegalStateException("Duplicate message rule: " + eventId);
        }
    }

    /**
     * @param context    cooldown family shared with neighbouring events
     * @param delayTicks time for the event's effect to be perceived before its line appears
     * @param lines      lines that are true of this event's own effect
     */
    public record MessageRule(
            ParanoiaMessageContext context,
            double naturalChance,
            int delayTicks,
            List<String> lines) {
        public MessageRule {
            if (context == null || naturalChance < 0.0D || naturalChance > 1.0D
                    || delayTicks < 0 || lines == null || lines.isEmpty()) {
                throw new IllegalArgumentException("Invalid paranoia message rule");
            }
            lines = List.copyOf(lines);
        }
    }
}
