package com.eotv.echoofthevoid.event.paranoia;

import java.util.Set;

/** Minecraft-free dimension eligibility rules shared by runtime and unit tests. */
public final class UncannyDimensionRules {
    private static final Set<String> WATER_EVENTS = Set.of(
            ParanoiaEventIds.EMPTY_WAKE,
            ParanoiaEventIds.COUNTERCURRENT_COLUMN,
            ParanoiaEventIds.FISHING_TUG,
            ParanoiaEventIds.AQUATIC_STEPS);
    private static final Set<String> OVERWORLD_EVENT_ONLY = Set.of(
            ParanoiaEventIds.COMPASS_LIAR,
            ParanoiaEventIds.BEACON_FRAGMENT,
            ParanoiaEventIds.WANDERING_TREE,
            ParanoiaEventIds.ANIMAL_CIRCLE,
            ParanoiaEventIds.ANIMAL_GRID,
            ParanoiaEventIds.ANIMAL_DEATH_CIRCLE,
            ParanoiaEventIds.ANIMAL_WAKE_CIRCLE);
    private static final Set<String> END_SPECIALS = Set.of(
            ParanoiaEventIds.FOLLOWER,
            ParanoiaEventIds.PULSE,
            ParanoiaEventIds.HURLER,
            ParanoiaEventIds.STALKER,
            ParanoiaEventIds.SHADOW,
            ParanoiaEventIds.LISTENER,
            ParanoiaEventIds.BYSTANDER,
            ParanoiaEventIds.MOURNER,
            ParanoiaEventIds.DOUBLER,
            ParanoiaEventIds.DEVOURER);
    private static final Set<String> OVERWORLD_SPECIAL_ONLY = Set.of(
            ParanoiaEventIds.WATCHER,
            ParanoiaEventIds.USHER,
            ParanoiaEventIds.FERRYMAN,
            ParanoiaEventIds.PERCHER,
            ParanoiaEventIds.BLUR,
            ParanoiaEventIds.SLEEPER);

    private UncannyDimensionRules() {
    }

    public static boolean runsOrdinarySchedulerIn(DimensionKind dimension) {
        return dimension != DimensionKind.END
                && dimension != DimensionKind.ARENA;
    }

    public static boolean allowsNaturalEventIn(
            DimensionKind dimension, String eventId) {
        if (!runsOrdinarySchedulerIn(dimension) || eventId == null) {
            return false;
        }
        if (ParanoiaEventIds.LAVA_WAKE.equals(eventId)) {
            return dimension == DimensionKind.NETHER
                    || dimension == DimensionKind.OTHER_ULTRAWARM;
        }
        if (WATER_EVENTS.contains(eventId)
                && (dimension == DimensionKind.NETHER
                        || dimension == DimensionKind.OTHER_ULTRAWARM)) {
            return false;
        }
        return !OVERWORLD_EVENT_ONLY.contains(eventId)
                || dimension == DimensionKind.OVERWORLD;
    }

    public static boolean allowsNaturalSpecialIn(
            DimensionKind dimension, String specialId) {
        if (specialId == null || dimension == DimensionKind.ARENA) {
            return false;
        }
        if (ParanoiaEventIds.MINER.equals(specialId)) {
            return dimension == DimensionKind.OVERWORLD
                    || dimension == DimensionKind.NETHER;
        }
        if (ParanoiaEventIds.DEVOURER.equals(specialId)) {
            return dimension == DimensionKind.OVERWORLD
                    || dimension == DimensionKind.NETHER
                    || dimension == DimensionKind.END;
        }
        if (ParanoiaEventIds.ECHOER.equals(specialId)
                || ParanoiaEventIds.FLANKER.equals(specialId)) {
            return dimension == DimensionKind.OVERWORLD
                    || dimension == DimensionKind.NETHER;
        }
        if (ParanoiaEventIds.DRIFTER.equals(specialId)
                || ParanoiaEventIds.DREDGER.equals(specialId)) {
            return dimension == DimensionKind.OVERWORLD;
        }
        if (ParanoiaEventIds.ASHWALKER.equals(specialId)) {
            return dimension == DimensionKind.NETHER;
        }
        if (dimension == DimensionKind.OVERWORLD) {
            return true;
        }
        if (dimension == DimensionKind.END) {
            return END_SPECIALS.contains(specialId);
        }
        return !OVERWORLD_SPECIAL_ONLY.contains(specialId);
    }

    /** Minecraft-free classification used by both runtime adapters and pure tests. */
    public enum DimensionKind {
        OVERWORLD,
        NETHER,
        END,
        ARENA,
        OTHER,
        OTHER_ULTRAWARM
    }
}
