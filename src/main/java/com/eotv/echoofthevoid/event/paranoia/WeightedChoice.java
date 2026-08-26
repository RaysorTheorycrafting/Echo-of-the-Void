package com.eotv.echoofthevoid.event.paranoia;

import java.util.Objects;

/** Immutable candidate used by the runtime scheduler and deterministic simulator. */
public record WeightedChoice<T>(T value, int weight) {
    public WeightedChoice {
        Objects.requireNonNull(value, "value");
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be positive");
        }
    }
}
