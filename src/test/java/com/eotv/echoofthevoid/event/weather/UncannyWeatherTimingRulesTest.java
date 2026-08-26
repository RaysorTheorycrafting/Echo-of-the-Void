package com.eotv.echoofthevoid.event.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UncannyWeatherTimingRulesTest {
    @Test
    void dryRainDurationIsStronglyShorterThanTheDeliveredWeatherWindow() {
        assertEquals(240, UncannyWeatherTimingRules.DRY_RAIN_MIN_DURATION_TICKS);
        assertEquals(500, UncannyWeatherTimingRules.DRY_RAIN_MAX_DURATION_TICKS);
        assertTrue(UncannyWeatherTimingRules.DRY_RAIN_MAX_DURATION_TICKS < 20 * 90);
    }

    @Test
    void artificialThunderIsNowABriefAnomaly() {
        assertEquals(20 * 15, UncannyWeatherTimingRules.ARTIFICIAL_THUNDER_MIN_DURATION_TICKS);
        assertEquals(20 * 30, UncannyWeatherTimingRules.ARTIFICIAL_THUNDER_MAX_DURATION_TICKS);
        assertTrue(UncannyWeatherTimingRules.ARTIFICIAL_THUNDER_MAX_DURATION_TICKS < 20 * 70);
    }

    @Test
    void dryRainPulseRulesStayInsideTheApprovedBounds() {
        int[] samples = {Integer.MIN_VALUE, -1, 0, 1, 2, 3, 10_000, Integer.MAX_VALUE};
        for (int sample : samples) {
            int count = UncannyWeatherTimingRules.dryRainPulseCount(sample);
            int gap = UncannyWeatherTimingRules.dryRainPulseGapTicks(sample);
            float volume = UncannyWeatherTimingRules.dryRainVolume(sample);
            float pitch = UncannyWeatherTimingRules.dryRainPitch(sample);

            assertTrue(count >= 2 && count <= 5);
            assertTrue(gap >= 50 && gap <= 100);
            assertTrue(volume >= 0.28F && volume <= 0.46F);
            assertTrue(pitch >= 0.90F && pitch <= 1.00F);
        }
    }

    @Test
    void sobbingRainAlternatesBetweenSharedAndSinglePlayerOccurrences() {
        int players = 4;
        for (int sample = 0; sample < players; sample++) {
            UncannyWeatherTimingRules.SobbingRainAudience audience =
                    UncannyWeatherTimingRules.sobbingRainAudience(players, sample);
            assertTrue(audience.shared());
            assertEquals(-1, audience.targetIndex());
        }
        for (int sample = players; sample < players * 2; sample++) {
            UncannyWeatherTimingRules.SobbingRainAudience audience =
                    UncannyWeatherTimingRules.sobbingRainAudience(players, sample);
            assertTrue(!audience.shared());
            assertEquals(sample - players, audience.targetIndex());
        }
    }

    @Test
    void sobbingRainCannotSelectAnAbsentPlayer() {
        UncannyWeatherTimingRules.SobbingRainAudience noPlayers =
                UncannyWeatherTimingRules.sobbingRainAudience(0, Integer.MIN_VALUE);
        UncannyWeatherTimingRules.SobbingRainAudience solo =
                UncannyWeatherTimingRules.sobbingRainAudience(1, Integer.MAX_VALUE);
        assertTrue(noPlayers.shared());
        assertTrue(solo.shared());
        assertEquals(-1, noPlayers.targetIndex());
        assertEquals(-1, solo.targetIndex());
    }
}
