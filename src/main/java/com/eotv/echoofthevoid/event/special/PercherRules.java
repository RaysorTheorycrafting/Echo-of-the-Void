package com.eotv.echoofthevoid.event.special;

/** Minecraft-free ranking of Percher? perches: a roof or a tree top reads as "something up there". */
public final class PercherRules {
    /** Below this score a perch is only a hill: used when nothing better is in reach. */
    public static final int HILL = 0;

    private PercherRules() {
    }

    /**
     * @param built      the support is a placed or structural block (planks, bricks, stairs, glass...)
     * @param treeTop    the support is leaves or a log
     * @param lowerSides how many of the four neighbouring columns are lower than the perch
     * @param drop       how far the lowest neighbouring column falls below the perch, in blocks
     */
    public static int score(boolean built, boolean treeTop, int lowerSides, int drop) {
        int score = HILL;
        if (built) {
            score += 6;
        } else if (treeTop) {
            score += 4;
        }
        score += Math.max(0, lowerSides - 2);
        if (drop >= 3) {
            score += 1;
        }
        return score;
    }
}
