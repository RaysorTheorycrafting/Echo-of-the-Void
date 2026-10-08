package com.eotv.echoofthevoid.event.paranoia.message;

/**
 * Cooldown family of a player-facing uncanny message. Only OBSERVATION, CAVE, BASE, SLEEP and
 * WEATHER also own a pool for the standalone Corrupt Message; the others are event-only.
 */
public enum ParanoiaMessageContext {
    OBSERVATION,
    CAVE,
    BASE,
    CONTAINER,
    SLEEP,
    WEATHER,
    ANIMAL
}
