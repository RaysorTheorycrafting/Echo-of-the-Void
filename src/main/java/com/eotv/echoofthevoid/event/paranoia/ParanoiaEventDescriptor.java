package com.eotv.echoofthevoid.event.paranoia;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Immutable scheduling metadata. Runtime conditions and effects remain in the compatibility facade. */
public record ParanoiaEventDescriptor(
        String id,
        String displayName,
        int minimumPhase,
        int minimumDanger,
        Set<ParanoiaEventLane> lanes,
        ParanoiaEventSeverity severity,
        int primaryWeight,
        int ambientWeight,
        int specialWeight,
        int eventCooldownSeconds,
        int ambientCooldownSeconds) {
    private static final Pattern STABLE_ID = Pattern.compile("[a-z][a-z0-9_]*");

    public ParanoiaEventDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(lanes, "lanes");
        Objects.requireNonNull(severity, "severity");
        if (!STABLE_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid stable event id: " + id);
        }
        if (minimumPhase < 1 || minimumPhase > 4) {
            throw new IllegalArgumentException("minimumPhase must be in [1, 4]");
        }
        if (minimumDanger < 0 || minimumDanger > 5) {
            throw new IllegalArgumentException("minimumDanger must be in [0, 5]");
        }
        if (primaryWeight < 0 || ambientWeight < 0 || specialWeight < 0) {
            throw new IllegalArgumentException("Weights cannot be negative");
        }
        if (eventCooldownSeconds < 0 || ambientCooldownSeconds < 0) {
            throw new IllegalArgumentException("Cooldowns cannot be negative");
        }
        lanes = Set.copyOf(lanes);
    }

    public boolean isAvailable(int phase, int danger) {
        return phase >= minimumPhase && danger >= minimumDanger;
    }

    public int baseWeight(ParanoiaEventLane lane) {
        return switch (lane) {
            case PRIMARY -> primaryWeight;
            case AMBIENT -> ambientWeight;
            case SPECIAL -> specialWeight;
            case CONTEXTUAL, CONTROL -> 0;
        };
    }
}
