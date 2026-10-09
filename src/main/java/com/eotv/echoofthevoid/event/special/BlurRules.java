package com.eotv.echoofthevoid.event.special;

/**
 * Minecraft-free timing and strength of Blur? (user, 2026-10-09): it stays a shorter while, and at
 * one moment of each appearance lands three or four light blows before running off without a sound.
 * Meant to stress the player, never to put them in real danger.
 */
public final class BlurRules {
    public static final int MIN_LIFETIME_TICKS = 20 * 70;
    public static final int MAX_LIFETIME_TICKS = 20 * 110;
    /** The flurry comes after this long at the earliest, and leaves time to flee before the end. */
    public static final int MIN_STRIKE_DELAY_TICKS = 20 * 20;
    public static final int STRIKE_MARGIN_TICKS = 20 * 15;
    public static final double STRIKE_START_DISTANCE = 20.0D;
    public static final double STRIKE_REACH = 1.8D;
    public static final int MIN_STRIKE_GAP_TICKS = 14;
    public static final int MAX_STRIKE_TICKS = 20 * 15;
    /** One heart before armour. */
    public static final float STRIKE_DAMAGE = 2.0F;
    /** A blow never takes the player below two hearts. */
    public static final float MIN_HEALTH_LEFT = 4.0F;
    public static final double FLEE_DISTANCE = 32.0D;
    public static final int MAX_FLEE_TICKS = 20 * 12;

    private BlurRules() {
    }

    public static int lifetimeTicks(double roll) {
        return MIN_LIFETIME_TICKS + (int) Math.round(clamp01(roll) * (MAX_LIFETIME_TICKS - MIN_LIFETIME_TICKS));
    }

    public static int strikeDelayTicks(int lifetimeTicks, double roll) {
        int latest = Math.max(MIN_STRIKE_DELAY_TICKS, lifetimeTicks - STRIKE_MARGIN_TICKS);
        return MIN_STRIKE_DELAY_TICKS + (int) Math.round(clamp01(roll) * (latest - MIN_STRIKE_DELAY_TICKS));
    }

    /** Three or four blows. */
    public static int strikeCount(double roll) {
        return roll < 0.5D ? 3 : 4;
    }

    /** The raw damage of one blow for a player at this health; 0 when it would leave them too low. */
    public static float strikeDamage(float playerHealth) {
        return Math.max(0.0F, Math.min(STRIKE_DAMAGE, playerHealth - MIN_HEALTH_LEFT));
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
