package com.eotv.echoofthevoid.world;

/**
 * Pure fog contract for Elsewhere, shared by the client renderer and tests.
 *
 * <p>Pursuers and the arena floor are pure black, which no brightness setting can lighten. The fog
 * is a slightly lighter, fixed grey: silhouettes therefore surface as darker shapes from the murk
 * at the same distance for every player, whatever their gamma, night vision or render distance.
 * Vanilla fog uses a smoothstep between start and end; with these values a pursuer is a faint
 * shape near {@value #FOG_END_BLOCKS} blocks, readable around 7 blocks, leaving roughly two seconds
 * of reaction at its walking pace before melee range.</p>
 */
public final class ElsewhereFogRules {
    public static final float FOG_START_BLOCKS = 2.0F;
    public static final float FOG_END_BLOCKS = 11.0F;
    public static final float FOG_RED = 0.095F;
    public static final float FOG_GREEN = 0.095F;
    public static final float FOG_BLUE = 0.105F;

    private ElsewhereFogRules() {
    }

    /** Vanilla's linear-fog shader factor: 0 = fully visible, 1 = fully hidden in the fog colour. */
    public static double fogFactor(double distanceBlocks) {
        if (distanceBlocks <= FOG_START_BLOCKS) {
            return 0.0D;
        }
        if (distanceBlocks >= FOG_END_BLOCKS) {
            return 1.0D;
        }
        double t = (distanceBlocks - FOG_START_BLOCKS) / (FOG_END_BLOCKS - FOG_START_BLOCKS);
        return t * t * (3.0D - 2.0D * t);
    }
}
