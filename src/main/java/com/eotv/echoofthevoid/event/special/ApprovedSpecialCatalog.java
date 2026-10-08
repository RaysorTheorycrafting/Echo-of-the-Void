package com.eotv.echoofthevoid.event.special;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Immutable active Special definitions. */
public final class ApprovedSpecialCatalog {
    private static final List<Definition> DEFINITIONS = List.of(
            special("surveyor", "Surveyor?", 2, 0, 4, Status.WORKING,
                    "Walks the outside of a known base and measures doors, windows and corners without entering."),
            special("mourner", "Mourner?", 3, 0, 1, Status.WORKING,
                    "Waits at one persisted player death site, acknowledges the returning player, then sinks."),
            special("doubler", "Doubler?", 3, 2, 2, Status.WORKING,
                    "Mirrors a player's broad movements across a real separation, with one rare deliberate mismatch."),
            special("ferryman", "Ferryman?", 3, 1, 3, Status.WORKING,
                    "Accompanies a moving boat from below without breaking it or ejecting its passengers."),
            special("listener", "Listener?", 2, 0, 3, Status.WORKING,
                    "Moves only between recent physical sound sources and leaves after sustained silence."),
            special("bystander", "Bystander?", 2, 0, 4, Status.WORKING,
                    "Observes a real fight, looking toward each current blow without helping either side."),
            special("miner", "Miner?", 3, 2, 2, Status.WORKING,
                    "Physically tunnels through a strictly safe temporary route, then emerges as an adaptive Attacker?."),
            special("devourer", "Devourer?", 4, 2, 1, Status.WORKING,
                    "A slow contact threat whose mouth opens an isolated one-minute survival trial."),
            special("echoer", "Echoer?", HuntingSpecialRules.ECHOER_MINIMUM_PHASE,
                    HuntingSpecialRules.ECHOER_MINIMUM_DANGER, HuntingSpecialRules.ECHOER_WEIGHT, Status.WORKING,
                    "Replays safe remembered sounds away from its real route, then hunts from behind."),
            special("drifter", "Drifter?", HuntingSpecialRules.DRIFTER_MINIMUM_PHASE,
                    HuntingSpecialRules.DRIFTER_MINIMUM_DANGER, HuntingSpecialRules.DRIFTER_WEIGHT, Status.WORKING,
                    "Stalks from deep water and risks a short pursuit ashore before its own air runs out."),
            special("ashwalker", "Ashwalker?", HuntingSpecialRules.ASHWALKER_MINIMUM_PHASE,
                    HuntingSpecialRules.ASHWALKER_MINIMUM_DANGER, HuntingSpecialRules.ASHWALKER_WEIGHT, Status.WORKING,
                    "Follows from connected Nether lava with only its head visible above the surface."),
            special("dredger", "Dredger?", HuntingSpecialRules.DREDGER_MINIMUM_PHASE,
                    HuntingSpecialRules.DREDGER_MINIMUM_DANGER, HuntingSpecialRules.DREDGER_WEIGHT, Status.WORKING,
                    "Telegraphs a grab, then drags swimmers toward the ocean floor until struck three times."),
            special("flanker", "Flanker?", HuntingSpecialRules.FLANKER_MINIMUM_PHASE,
                    HuntingSpecialRules.FLANKER_MINIMUM_DANGER, HuntingSpecialRules.FLANKER_WEIGHT, Status.WORKING,
                    "A linked pair alternates pressure from opposite sides and attacks only after a readable enclosure."));
    private static final Map<String, Definition> BY_ID;

    static {
        Map<String, Definition> byId = new LinkedHashMap<>();
        for (Definition definition : DEFINITIONS) {
            if (byId.put(definition.id(), definition) != null) {
                throw new IllegalStateException("Duplicate approved Special id: " + definition.id());
            }
        }
        BY_ID = Map.copyOf(byId);
    }

    private ApprovedSpecialCatalog() {
    }

    public static List<Definition> definitions() {
        return DEFINITIONS;
    }

    public static Definition byId(String id) {
        return id == null ? null : BY_ID.get(id.trim().toLowerCase(Locale.ROOT));
    }

    private static Definition special(
            String id,
            String displayName,
            int minimumPhase,
            int danger,
            int weight,
            Status status,
            String description) {
        return new Definition(id, displayName, minimumPhase, danger, weight, status, description);
    }

    public enum Status {
        WORKING,
        PROTOTYPE
    }

    public record Definition(
            String id,
            String displayName,
            int minimumPhase,
            int danger,
            int weight,
            Status status,
            String description) {
    }
}
