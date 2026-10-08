package com.eotv.echoofthevoid.world;

import java.util.Set;

/**
 * Narrow policy for correcting Vanilla's blanket experimental classification of every
 * additional level stem. It deliberately refuses worlds containing any other custom or
 * already-unstable dimension.
 */
public final class UncannyDimensionLifecyclePolicy {
    private static final Set<String> STANDARD_EOTV_DIMENSIONS = Set.of(
            "minecraft:overworld",
            "minecraft:the_nether",
            "minecraft:the_end",
            "echoofthevoid:elsewhere");
    private static final Set<String> EXPECTED_UNSTABLE_DIMENSIONS =
            Set.of("echoofthevoid:elsewhere");

    private UncannyDimensionLifecyclePolicy() {
    }

    public static boolean canMarkAsStable(
            Set<String> dimensionIds,
            Set<String> unstableDimensionIds) {
        return STANDARD_EOTV_DIMENSIONS.equals(dimensionIds)
                && EXPECTED_UNSTABLE_DIMENSIONS.equals(unstableDimensionIds);
    }
}
