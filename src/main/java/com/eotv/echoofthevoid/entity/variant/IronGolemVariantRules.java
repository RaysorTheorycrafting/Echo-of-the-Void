package com.eotv.echoofthevoid.entity.variant;

/** Pure phase distribution for the five replacement Iron Golem behaviours. */
public final class IronGolemVariantRules {
    private IronGolemVariantRules() {
    }

    public static int variantIdForPhaseRoll(int phase, int rawRoll) {
        int normalizedPhase = Math.max(1, Math.min(4, phase));
        int roll = Math.floorMod(rawRoll, 100);
        return switch (normalizedPhase) {
            case 1 -> 1;
            case 2 -> roll < 68 ? 2 : 1;
            case 3 -> roll < 58 ? 3 : (roll < 86 ? 2 : 1);
            default -> roll < 32 ? 5 : (roll < 64 ? 4 : (roll < 86 ? 3 : 2));
        };
    }

    /** Player-built golems keep Vanilla's player-safe origin and can never become the attacking variant 5. */
    public static int allowedVariantForOrigin(int variantId, boolean playerCreated) {
        return playerCreated && variantId == 5 ? 4 : variantId;
    }
}
