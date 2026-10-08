package com.eotv.echoofthevoid.event.paranoia;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import com.eotv.echoofthevoid.world.UncannyDimensions;

/** Central dimension contract for naturally scheduled horror content. */
public final class UncannyDimensionPolicy {
    private UncannyDimensionPolicy() {
    }

    public static UncannyDimensionRules.DimensionKind classify(ServerLevel level) {
        if (UncannyDimensions.isElsewhere(level)) {
            return UncannyDimensionRules.DimensionKind.ARENA;
        }
        if (level.dimension() == Level.OVERWORLD) {
            return UncannyDimensionRules.DimensionKind.OVERWORLD;
        }
        if (level.dimension() == Level.NETHER) {
            return UncannyDimensionRules.DimensionKind.NETHER;
        }
        if (level.dimension() == Level.END) {
            return UncannyDimensionRules.DimensionKind.END;
        }
        return level.dimensionType().ultraWarm()
                ? UncannyDimensionRules.DimensionKind.OTHER_ULTRAWARM
                : UncannyDimensionRules.DimensionKind.OTHER;
    }

    public static boolean runsOrdinaryScheduler(ServerLevel level) {
        return runsOrdinarySchedulerIn(classify(level));
    }

    public static boolean runsOrdinarySchedulerIn(UncannyDimensionRules.DimensionKind dimension) {
        return UncannyDimensionRules.runsOrdinarySchedulerIn(dimension);
    }

    public static boolean allowsNaturalEvent(ServerLevel level, String eventId) {
        return allowsNaturalEventIn(classify(level), eventId);
    }

    public static boolean allowsNaturalEventIn(UncannyDimensionRules.DimensionKind dimension, String eventId) {
        return UncannyDimensionRules.allowsNaturalEventIn(dimension, eventId);
    }

    public static boolean allowsNaturalSpecial(ServerLevel level, String specialId) {
        return allowsNaturalSpecialIn(classify(level), specialId);
    }

    public static boolean allowsNaturalSpecialIn(UncannyDimensionRules.DimensionKind dimension, String specialId) {
        return UncannyDimensionRules.allowsNaturalSpecialIn(dimension, specialId);
    }

    public static boolean allowsMajorEvent(ServerLevel level) {
        return classify(level) == UncannyDimensionRules.DimensionKind.OVERWORLD;
    }
}
