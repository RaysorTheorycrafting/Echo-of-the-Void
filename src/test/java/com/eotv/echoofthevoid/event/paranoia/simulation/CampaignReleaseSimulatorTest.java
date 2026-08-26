package com.eotv.echoofthevoid.event.paranoia.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eotv.echoofthevoid.campaign.CampaignDirectorRules;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventIds;
import com.eotv.echoofthevoid.event.paranoia.TensionPacingRules;
import com.eotv.echoofthevoid.event.weather.UncannyWeatherPacingRules;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CampaignReleaseSimulatorTest {
    @Test
    void standardCampaignUsesAllFourPhasesAndOneHiddenCulmination() {
        CampaignReleaseSimulator.CampaignScenario scenario =
                CampaignReleaseSimulator.CampaignScenario.standard(3, 3, 0xE07F0200L);
        CampaignReleaseSimulator.CampaignReport report = CampaignReleaseSimulator.simulate(scenario);

        assertEquals(50L * CampaignDirectorRules.TICKS_PER_DAY, report.durationTicks());
        assertEquals(30L * 60L * 20L, report.phaseTwoStartTick());
        assertEquals((30L + 45L) * 60L * 20L, report.phaseThreeStartTick());
        assertEquals((30L + 45L + 60L) * 60L * 20L, report.phaseFourStartTick());
        assertEquals(1L, report.culminationStateTransitions());
        assertEquals(0L, report.doubleCulminationCount());
        assertFalse(report.culminationExpired());
        assertTrue(report.culminationTick() >= Math.round(report.durationTicks() * 0.80D));
        assertTrue(report.culminationTick() < report.durationTicks());
        assertEquals(0L, report.unjustifiedMajorBurstCount());
        assertEquals(0L, report.strongEventBurstCount());
        assertTrue(report.tensionBuilderCount() > 0L);
        assertTrue(report.grandWardenCount() > 0L);
        assertTrue(report.weather().totalEvents() > 0L);
    }

    @Test
    void extraLongCampaignUsesTheSameNormalizedArc() {
        long seed = 0xE07F0201L;
        CampaignReleaseSimulator.CampaignReport standard = CampaignReleaseSimulator.simulate(
                CampaignReleaseSimulator.CampaignScenario.standard(3, 3, seed));
        CampaignReleaseSimulator.CampaignReport extraLong = CampaignReleaseSimulator.simulate(
                CampaignReleaseSimulator.CampaignScenario.extraLong(3, 3, seed));

        double standardProgress = standard.culminationTick() / (double) standard.durationTicks();
        double extraLongProgress = extraLong.culminationTick() / (double) extraLong.durationTicks();
        assertTrue(standardProgress >= 0.80D && standardProgress < 1.0D);
        assertTrue(extraLongProgress >= 0.80D && extraLongProgress < 1.0D);
        assertEquals(0L, extraLong.doubleCulminationCount());
        assertEquals(0L, extraLong.unjustifiedMajorBurstCount());
    }

    @Test
    void matrixCoversFiveProfilesSixDangersAndManySeedsWithoutReleaseViolations() {
        Set<String> alwaysIneligibleEvents = null;
        Set<String> alwaysIneligibleWeather = null;
        long virtualTicks = 0L;

        for (int profile = 1; profile <= 5; profile++) {
            for (int danger = 0; danger <= 5; danger++) {
                for (long seed = 1L; seed <= 4L; seed++) {
                    CampaignReleaseSimulator.CampaignReport report = CampaignReleaseSimulator.simulate(
                            CampaignReleaseSimulator.CampaignScenario.standard(
                                    profile, danger, 0xE07F2000L + seed * 100L + profile * 10L + danger));
                    virtualTicks += report.durationTicks();
                    assertEquals(1L, report.culminationStateTransitions());
                    assertEquals(0L, report.doubleCulminationCount());
                    assertEquals(0L, report.unjustifiedMajorBurstCount());
                    assertEquals(
                            0L,
                            report.strongEventBurstCount(),
                            "unexpected strong burst for profile=" + profile
                                    + ", danger=" + danger + ", seedIndex=" + seed);
                    assertFalse(report.events().countsByEvent().containsKey(ParanoiaEventIds.ARMOR_BREAK));
                    assertFalse(report.events().countsByEvent().containsKey(ParanoiaEventIds.FORCED_DROP));
                    assertFalse(report.events().countsByEvent().containsKey(ParanoiaEventIds.GIANT_SUN));
                    assertFalse(report.events().countsByEvent().containsKey("climber"));
                    if (alwaysIneligibleEvents == null) {
                        alwaysIneligibleEvents = new LinkedHashSet<>(report.permanentlyIneligibleEventIds());
                        alwaysIneligibleWeather = new LinkedHashSet<>(report.weather().ineligibleEventIds());
                    } else {
                        alwaysIneligibleEvents.retainAll(report.permanentlyIneligibleEventIds());
                        alwaysIneligibleWeather.retainAll(report.weather().ineligibleEventIds());
                    }
                }
            }
        }

        double virtualHours = virtualTicks / (double) (20L * 60L * 60L);
        assertTrue(virtualHours >= 2_000.0D, "The release matrix must cover thousands of virtual hours");
        assertTrue(alwaysIneligibleEvents != null && alwaysIneligibleEvents.isEmpty(),
                "No active event may remain numerically unreachable across every supported danger");
        assertTrue(alwaysIneligibleWeather != null && alwaysIneligibleWeather.isEmpty(),
                "No weather may remain numerically unreachable across every supported danger");
    }

    @Test
    void weatherAndTensionRulesExposeTheExactRuntimeBounds() {
        assertEquals(300, TensionPacingRules.TENSION_MIN_SECONDS);
        assertEquals(600, TensionPacingRules.TENSION_MAX_SECONDS);
        assertEquals(1_500, TensionPacingRules.BREAK_MIN_SECONDS);
        assertEquals(3_000, TensionPacingRules.BREAK_MAX_SECONDS);
        assertEquals(0.22D, TensionPacingRules.GRAND_POST_TENSION_CHANCE);

        UncannyWeatherPacingRules.IntRange phaseOne =
                UncannyWeatherPacingRules.nextCheckSecondsRange(1, 3);
        assertEquals(12, phaseOne.minInclusive());
        assertEquals(28, phaseOne.maxInclusive());
        assertEquals(475L, UncannyWeatherPacingRules.nextCheckDelayTicks(12, true, 8, true, 1));
        assertEquals(600, UncannyWeatherPacingRules.visualDurationRange(0).minInclusive());
        assertEquals(2_400, UncannyWeatherPacingRules.visualDurationRange(0).maxInclusive());
        assertEquals(240, UncannyWeatherPacingRules.visualDurationRange(1_800).minInclusive());
        assertEquals(440, UncannyWeatherPacingRules.visualDurationRange(1_800).maxInclusive());
    }

    @Test
    void sameSeedProducesAnIdenticalWholeCampaignReport() {
        CampaignReleaseSimulator.CampaignScenario scenario =
                CampaignReleaseSimulator.CampaignScenario.standard(4, 2, 77123L);
        assertEquals(
                CampaignReleaseSimulator.simulate(scenario),
                CampaignReleaseSimulator.simulate(scenario));
    }
}
