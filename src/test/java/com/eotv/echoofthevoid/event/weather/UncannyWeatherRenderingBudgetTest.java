package com.eotv.echoofthevoid.event.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UncannyWeatherRenderingBudgetTest {
    @Test
    void ashRainKeepsADenseButBoundedPerPlayerBudget() {
        assertEquals(440, UncannyWeatherRenderingBudget.ashParticlesPerSecondPerPlayer());
        assertTrue(UncannyWeatherRenderingBudget.ashParticlesPerSecondPerPlayer() <= 500);
    }
}
