package com.eotv.echoofthevoid.event.special;

import java.util.function.DoubleUnaryOperator;

/**
 * Duel parity between one player and one attacking Special (user decision, 2026-10-08): the
 * creature is as tough as the weapon that strikes it and hits as hard as the player's protection
 * allows, so every fight stays close but leans towards the player.
 *
 * <p>Toughness: the player's best carried weapon, at full charge, empties the creature's health
 * in {@link Profile#playerSeconds()}. Lethality: after the player's real armour, enchantments and
 * effects, the creature needs {@link #PLAYER_ADVANTAGE} times as long to kill a full-health
 * player at its own attack cadence. Pure arithmetic only; {@code CombatParity} reads the game.</p>
 */
public final class CombatParityRules {
    /** The creature needs half again as long as the player: close, but the player wins. */
    public static final double PLAYER_ADVANTAGE = 1.5D;
    public static final double MIN_HEALTH = 16.0D;
    public static final double MAX_HEALTH = 400.0D;
    public static final double MIN_RAW_DAMAGE = 1.0D;
    public static final double MAX_RAW_DAMAGE = 80.0D;
    public static final int MIN_HITS_TO_KILL_PLAYER = 3;
    /** Full-charge attacks per second above this are not sustained in a real fight. */
    public static final double MAX_SUSTAINED_ATTACKS_PER_SECOND = 2.0D;
    public static final int REFRESH_INTERVAL_TICKS = 40;

    // Cadence is the time between hits that actually land in a real fight: the player's
    // knockback, strafing and the creature's own lunges or retreats are included, so each blow is
    // heavy (about two hearts for Attacker?) rather than a stream of weak taps (user, 2026-10-08).
    public static final Profile ATTACKER = new Profile("attacker", 6.0D, 1.8D, 1.0D);
    public static final Profile MINER = new Profile("miner", 6.0D, 1.8D, 1.0D);
    public static final Profile ECHOER = new Profile("echoer", 5.0D, 1.4D, 1.0D);
    /**
     * The pair together is one Attacker? (rework, 2026-10-09): each member carries half its toughness
     * and the same blow, and the pair shares one strike rhythm at the Attacker? cadence. Its danger is
     * the pincer, not its numbers. A lone survivor keeps its half.
     */
    public static final Profile FLANKER_PAIR_MEMBER = new Profile("flanker", 6.0D, 1.8D, 0.5D);
    public static final Profile DRIFTER = new Profile("drifter", 6.0D, 1.8D, 1.0D);
    public static final Profile ASHWALKER = new Profile("ashwalker", 5.0D, 2.0D, 1.0D);
    public static final Profile DREDGER = new Profile("dredger", 7.0D, 1.5D, 1.0D);

    /** Devourer? never strikes: only its toughness scales, and it is built to be hard to put down. */
    public static final double DEVOURER_PLAYER_SECONDS = 14.0D;
    public static final double DEVOURER_MIN_HEALTH = 80.0D;

    /** Health for a creature that does not fight back: the player's sustained damage over a time. */
    public static double toughnessOnly(double playerHitDamage, double playerAttacksPerSecond, double seconds, double minimum) {
        double rate = clamp(playerAttacksPerSecond, 0.25D, MAX_SUSTAINED_ATTACKS_PER_SECOND);
        return clamp(Math.max(1.0D, playerHitDamage) * rate * seconds, minimum, MAX_HEALTH);
    }

    private CombatParityRules() {
    }

    /**
     * @param playerSeconds          full-charge combat time the player needs to win
     * @param monsterCadenceSeconds  average seconds between the creature's landed hits
     * @param healthShare            share of the toughness carried by this body (pairs split it)
     */
    public record Profile(String id, double playerSeconds, double monsterCadenceSeconds, double healthShare) {
        public Profile {
            if (playerSeconds <= 0.0D || monsterCadenceSeconds <= 0.0D || healthShare <= 0.0D || healthShare > 1.0D) {
                throw new IllegalArgumentException("Invalid combat parity profile " + id);
            }
        }
    }

    public static double maxHealth(Profile profile, double playerHitDamage, double playerAttacksPerSecond) {
        double rate = clamp(playerAttacksPerSecond, 0.25D, MAX_SUSTAINED_ATTACKS_PER_SECOND);
        double damagePerSecond = Math.max(1.0D, playerHitDamage) * rate;
        double health = damagePerSecond * profile.playerSeconds() * profile.healthShare();
        return clamp(health, MIN_HEALTH * profile.healthShare(), MAX_HEALTH);
    }

    public static int hitsToKillPlayer(Profile profile) {
        double creatureSeconds = profile.playerSeconds() * PLAYER_ADVANTAGE;
        return Math.max(MIN_HITS_TO_KILL_PLAYER, (int) Math.round(creatureSeconds / profile.monsterCadenceSeconds()));
    }

    /** Damage that should actually reach the player per landed hit, after all protection. */
    public static double effectiveDamagePerHit(Profile profile, double playerMaxHealth) {
        return Math.max(0.5D, playerMaxHealth) / hitsToKillPlayer(profile);
    }

    /**
     * Inverts the player's damage reduction: the raw hit whose reduced value equals the wanted
     * effective damage. {@code reduction} must be monotonic, as Vanilla armour and enchantments are.
     */
    public static double rawDamageFor(double effectiveTarget, DoubleUnaryOperator reduction) {
        if (reduction.applyAsDouble(MAX_RAW_DAMAGE) <= effectiveTarget) {
            return MAX_RAW_DAMAGE;
        }
        double low = 0.0D;
        double high = MAX_RAW_DAMAGE;
        for (int iteration = 0; iteration < 40; iteration++) {
            double middle = (low + high) * 0.5D;
            if (reduction.applyAsDouble(middle) < effectiveTarget) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return clamp(high, MIN_RAW_DAMAGE, MAX_RAW_DAMAGE);
    }

    /** New health after the maximum changes: the creature keeps the same share of its life. */
    public static double rescaledHealth(double currentHealth, double previousMaximum, double newMaximum) {
        if (previousMaximum <= 0.0D) {
            return newMaximum;
        }
        double ratio = clamp(currentHealth / previousMaximum, 0.0D, 1.0D);
        return Math.max(currentHealth > 0.0D ? 1.0D : 0.0D, ratio * newMaximum);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
