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
                    "Observes a real fight, looking toward each current blow without helping either side."));
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
