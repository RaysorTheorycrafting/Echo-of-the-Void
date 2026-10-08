package com.eotv.echoofthevoid.event.weather;

/** Shared particle budgets for weather effects whose cost otherwise scales per player. */
public final class UncannyWeatherRenderingBudget {
    public static final int ASH_NEAR_INTERVAL_TICKS = 6;
    public static final int ASH_NEAR_PARTICLE_COUNT = 24;
    public static final int ASH_WIDE_INTERVAL_TICKS = 2;
    public static final int ASH_WIDE_PARTICLE_COUNT = 36;

    private UncannyWeatherRenderingBudget() {
    }

    public static int ashParticlesPerSecondPerPlayer() {
        return (20 * ASH_NEAR_PARTICLE_COUNT) / ASH_NEAR_INTERVAL_TICKS
                + (20 * ASH_WIDE_PARTICLE_COUNT) / ASH_WIDE_INTERVAL_TICKS;
    }
}
