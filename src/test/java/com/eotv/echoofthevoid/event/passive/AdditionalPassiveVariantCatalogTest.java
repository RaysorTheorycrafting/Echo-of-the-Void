package com.eotv.echoofthevoid.event.passive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eotv.echoofthevoid.dev.UncannyDevCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdditionalPassiveVariantCatalogTest {
    @Test
    void rabbitVariantsHaveStableIdsLabelsAndQaEntries() {
        AdditionalPassiveVariantCatalog.Species rabbit =
                AdditionalPassiveVariantCatalog.species().stream()
                        .filter(species -> species.key().equals("rabbit"))
                        .findFirst()
                        .orElseThrow();

        assertEquals("Rabbit?", rabbit.displayName());
        assertEquals(List.of(1, 2, 3, 4, 5),
                rabbit.variants().stream().map(AdditionalPassiveVariantCatalog.Variant::index).toList());
        assertEquals(List.of("Delayed Hop", "Rearward Gaze", "False Burrow", "Empty Landing", "Watchbound"),
                rabbit.variants().stream().map(AdditionalPassiveVariantCatalog.Variant::label).toList());

        assertEquals("rabbit|0", UncannyDevCatalog.byId("entity_rabbit_spawn").actionArg());
        for (int variant = 1; variant <= 5; variant++) {
            assertEquals("rabbit|" + variant,
                    UncannyDevCatalog.byId("entity_rabbit_v" + variant).actionArg());
        }
    }
}
