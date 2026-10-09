package com.eotv.echoofthevoid.event.special;

/** Minecraft-free timings and damage of Sleeper?. */
public final class SleeperRules {
    /** The line shown, once per player and per Sleeper?, the first time that player tries to sleep. */
    public static final String WARNING = "You can’t sleep. Something is in the room.";
    /** Beds of the base it can reach. */
    public static final double BED_RADIUS = 24.0D;
    /** It starts running once a player has been asleep this long (half a second). */
    public static final int ASLEEP_BEFORE_RUN_TICKS = 10;
    public static final int MAX_RUN_TICKS = 50;
    public static final int MOUNT_TICKS = 15;
    public static final int MOUTH_TICKS = 30;
    public static final int BITE_TICKS = 8;
    public static final int BITE_IMPACT_TICK = 3;
    /** Time the player gets to strike back while it climbs off the bed. */
    public static final int RECOVER_TICKS = 30;
    public static final int FIGHT_TICKS = 20 * 120;
    /** Attacker?'s parity cadence: one hit every 1.8 s. */
    public static final int MELEE_COOLDOWN_TICKS = 36;
    /** Rattling breath while it fights, every 2 to 4 seconds. */
    public static final int MIN_GROWL_TICKS = 40;
    public static final int MAX_GROWL_TICKS = 80;
    /**
     * Raw bite damage: kills a full-health player in full golden armour (11 points) but not in full
     * iron armour (15 points) or anything stronger, by Vanilla's armour formula.
     */
    public static final float BITE_DAMAGE = 22.0F;

    private SleeperRules() {
    }

    /** Vanilla {@code CombatRules.getDamageAfterAbsorb}, without enchantments. */
    public static float damageAfterArmor(float damage, float armor, float toughness) {
        float breakdown = 2.0F + toughness / 4.0F;
        float effective = Math.min(Math.max(armor - damage / breakdown, armor * 0.2F), 20.0F);
        return damage * (1.0F - effective / 25.0F);
    }

    public static boolean biteKills(float health, float armor, float toughness) {
        return damageAfterArmor(BITE_DAMAGE, armor, toughness) >= health;
    }
}
