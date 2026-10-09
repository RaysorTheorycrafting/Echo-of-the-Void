package com.eotv.echoofthevoid.event.special;

/** Minecraft-free timing and combat rules of the old friend. */
public final class OldFriendRules {
    /** Online time of the target between the friend joining and the attack: two to three game days. */
    public static final long MIN_QUIET_TICKS = 24_000L * 2L;
    public static final long MAX_QUIET_TICKS = 24_000L * 3L;
    public static final double MIN_SPAWN_DISTANCE = 16.0D;
    public static final double MAX_SPAWN_DISTANCE = 26.0D;
    /** Delay between the friend's death and its "left the game" line. */
    public static final int MIN_LEAVE_DELAY_TICKS = 60;
    public static final int MAX_LEAVE_DELAY_TICKS = 100;
    /** Self "joined the game": how long before the matching "left the game". */
    public static final int MIN_SELF_ECHO_TICKS = 20 * 40;
    public static final int MAX_SELF_ECHO_TICKS = 20 * 120;
    /** Players without a local name asked to Mojang per trigger; it throttles bursts of lookups. */
    public static final int MAX_NAME_LOOKUPS = 5;
    /** With at least this much air (of 300) it stays under a target that surfaced to breathe. */
    public static final int MIN_AIR_TO_STAY_UNDER = 100;
    /** How far under a breathing target it holds, close enough for a blow from below. */
    public static final double HOLD_DEPTH_UNDER_TARGET = 2.2D;

    private OldFriendRules() {
    }

    /** @param unit uniform random value in [0, 1) */
    public static long quietTicks(double unit) {
        double clamped = Math.max(0.0D, Math.min(1.0D, unit));
        return MIN_QUIET_TICKS + Math.round(clamped * (MAX_QUIET_TICKS - MIN_QUIET_TICKS));
    }

    /** Ticks between two full-strength swings, as for a player: 20 / attack speed, at least one tick. */
    public static int attackCooldownTicks(double attackSpeed) {
        if (attackSpeed <= 0.0D) {
            return 20;
        }
        return Math.max(1, (int) Math.ceil(20.0D / attackSpeed));
    }

    /**
     * Fraction of a block broken per tick, Vanilla's player formula without effects: tool speed over
     * hardness, divided by 30 with the right tool and by 100 without it.
     */
    public static float miningProgressPerTick(float toolSpeed, float hardness, boolean correctTool) {
        if (hardness < 0.0F) {
            return 0.0F;
        }
        if (hardness == 0.0F) {
            return 1.0F;
        }
        return Math.max(0.0F, toolSpeed) / hardness / (correctTool ? 30.0F : 100.0F);
    }
}
