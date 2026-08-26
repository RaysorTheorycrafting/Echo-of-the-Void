package com.eotv.echoofthevoid.event.passive;

import java.util.List;

/**
 * Metadata for passive species added after the original five-variant matrix.
 * Keeping this list outside the legacy runtime makes future species additive
 * without extending every hard-coded switch in the monolith.
 */
public final class AdditionalPassiveVariantCatalog {
    private static final List<Species> SPECIES = List.of(
            new Species(
                    "rabbit",
                    "Rabbit?",
                    List.of(
                            new Variant(1, "Delayed Hop"),
                            new Variant(2, "Rearward Gaze"),
                            new Variant(3, "False Burrow"),
                            new Variant(4, "Empty Landing"),
                            new Variant(5, "Watchbound"))));

    private AdditionalPassiveVariantCatalog() {
    }

    public static List<Species> species() {
        return SPECIES;
    }

    public record Species(
            String key,
            String displayName,
            List<Variant> variants) {

        public Species {
            variants = List.copyOf(variants);
        }
    }

    public record Variant(int index, String label) {
    }
}
