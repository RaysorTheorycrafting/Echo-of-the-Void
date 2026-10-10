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

    /** Pillaring that has not gained a block in this long is abandoned for a while (user, 2026-10-09). */
    public static final int PILLAR_STALL_TICKS = 50;
    public static final int PILLAR_RETRY_COOLDOWN_TICKS = 100;
    /** It eats when this much health is missing and its hunger bar is not full, as a player would. */
    public static final float EAT_WHEN_MISSING_HEALTH = 4.0F;
    /** It does not start a meal with the target closer than this (eating leaves it open). */
    public static final double MIN_EAT_DISTANCE = 4.5D;
    /** A meal takes as long as a player's (32 ticks for ordinary food). */
    public static final int EAT_TICKS = 32;

    /** Within this of the block's centre it counts as standing in the middle (its body is 0.6 wide). */
    public static final double PILLAR_CENTER_TOLERANCE = 0.12D;
    /** Largest sideways step per tick while lining up, a careful player's shuffle. */
    public static final double PILLAR_CENTERING_SPEED = 0.15D;

    private OldFriendRules() {
    }

    public static boolean isCentered(double offsetX, double offsetZ) {
        return offsetX * offsetX + offsetZ * offsetZ <= PILLAR_CENTER_TOLERANCE * PILLAR_CENTER_TOLERANCE;
    }

    /** Velocity along one axis to close {@code offset} towards the block's centre without overshooting. */
    public static double centeringStep(double offset) {
        return Math.max(-PILLAR_CENTERING_SPEED, Math.min(PILLAR_CENTERING_SPEED, offset * 0.5D));
    }

    /**
     * A copy of the player's hunger ({@code FoodData}): food and saturation feed natural regeneration,
     * healing and fighting cost exhaustion. Mutable, server-side, one per old friend.
     */
    public static final class Hunger {
        public static final int MAX_FOOD = 20;
        private int food = MAX_FOOD;
        private float saturation = 5.0F;
        private float exhaustion;
        private int timer;

        public int food() {
            return food;
        }

        public float saturation() {
            return saturation;
        }

        public void restore(int food, float saturation, float exhaustion) {
            this.food = Math.max(0, Math.min(MAX_FOOD, food));
            this.saturation = Math.max(0.0F, Math.min(this.food, saturation));
            this.exhaustion = Math.max(0.0F, exhaustion);
        }

        public float exhaustion() {
            return exhaustion;
        }

        public boolean wantsToEat(float missingHealth) {
            return food < MAX_FOOD && missingHealth >= EAT_WHEN_MISSING_HEALTH;
        }

        /** Vanilla {@code FoodData.eat}: nutrition fills the bar, saturation is capped by it. */
        public void eat(int nutrition, float saturationGain) {
            food = Math.min(MAX_FOOD, food + Math.max(0, nutrition));
            saturation = Math.min(food, saturation + Math.max(0.0F, saturationGain));
        }

        public void addExhaustion(float amount) {
            exhaustion = Math.min(40.0F, exhaustion + Math.max(0.0F, amount));
        }

        /**
         * One tick of Vanilla's {@code FoodData.tick} (peaceful hunger rules excluded).
         * @return health to restore this tick
         */
        public float tick(float missingHealth) {
            if (exhaustion > 4.0F) {
                exhaustion -= 4.0F;
                if (saturation > 0.0F) {
                    saturation = Math.max(saturation - 1.0F, 0.0F);
                } else {
                    food = Math.max(food - 1, 0);
                }
            }
            if (missingHealth <= 0.0F) {
                timer = 0;
                return 0.0F;
            }
            if (saturation > 0.0F && food >= MAX_FOOD) {
                if (++timer >= 10) {
                    float spent = Math.min(saturation, 6.0F);
                    addExhaustion(spent);
                    timer = 0;
                    return spent / 6.0F;
                }
            } else if (food >= 18) {
                if (++timer >= 80) {
                    addExhaustion(6.0F);
                    timer = 0;
                    return 1.0F;
                }
            } else {
                timer = 0;
            }
            return 0.0F;
        }
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
