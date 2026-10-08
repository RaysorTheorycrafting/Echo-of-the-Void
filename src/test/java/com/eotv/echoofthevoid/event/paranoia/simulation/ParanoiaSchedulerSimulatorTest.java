package com.eotv.echoofthevoid.event.paranoia.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventLane;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class ParanoiaSchedulerSimulatorTest {
    private static final String EXPECTED_ACTIVE_REPORT_SHA256 =
            "8A7E10E2C834901D8E6055D4180863A96C676CA60004A22AA4A4A88341D3767F";
    private static final String EXPECTED_111_REPORT_SHA256 =
            "17F0D33F0317DC49E55CCDD3E30F7CF7D20B6ADA7185ED63486DAC0818AFE9AC";

    @Test
    void sameSeedProducesTheSameReport() {
        ParanoiaSchedulerSimulator.Scenario scenario =
                ParanoiaSchedulerSimulator.Scenario.reference(4, 3, 3, 25.0D, 0xE07F0111L);
        assertEquals(
                ParanoiaSchedulerSimulator.simulate(scenario),
                ParanoiaSchedulerSimulator.simulate(scenario));
    }

    @Test
    void activeReportWithRetirementsAndValidatedNativeEventsIsFrozen() {
        ParanoiaSchedulerSimulator.SimulationReport report = ParanoiaSchedulerSimulator.simulate(
                ParanoiaSchedulerSimulator.Scenario.reference(4, 3, 3, 25.0D, 0xE07F0111L));
        assertEquals(EXPECTED_ACTIVE_REPORT_SHA256, sha256(snapshot(report)));
    }

    @Test
    void historical111ComparisonReportRemainsFrozen() {
        ParanoiaSchedulerSimulator.SimulationReport report = ParanoiaSchedulerSimulator.simulate(
                ParanoiaSchedulerSimulator.Scenario.reference111(4, 3, 3, 25.0D, 0xE07F0111L));
        assertEquals(EXPECTED_111_REPORT_SHA256, sha256(snapshot(report)));
        assertTrue(report.effectiveWeights().containsKey("primary:armor_break"));
        assertTrue(report.effectiveWeights().containsKey("primary:forced_drop"));
        assertTrue(report.effectiveWeights().containsKey("primary:giant_sun"));
    }

    @Test
    void activeBlackoutIsAtLeastTwiceAsRareAsIn111() {
        ParanoiaSchedulerSimulator.SimulationReport active = ParanoiaSchedulerSimulator.simulate(
                ParanoiaSchedulerSimulator.Scenario.reference(4, 3, 3, 25.0D, 0xE07F0111L));
        ParanoiaSchedulerSimulator.SimulationReport historical = ParanoiaSchedulerSimulator.simulate(
                ParanoiaSchedulerSimulator.Scenario.reference111(4, 3, 3, 25.0D, 0xE07F0111L));
        long activeCount = active.countsByEvent().getOrDefault("blackout", 0L);
        long historicalCount = historical.countsByEvent().getOrDefault("blackout", 0L);
        assertTrue(historicalCount >= 3, "the reference scenario must exercise Blackout: " + historicalCount);
        // The simulation models weight and spacing only; the runtime also removes the forced-fallback
        // Blackout and adds a 30-minute join grace, which this scheduler model cannot represent.
        assertTrue(activeCount * 2 <= historicalCount,
                "Blackout must be at least twice as rare: " + activeCount + " vs " + historicalCount);
    }

    @Test
    void retiredEventsAreAbsentFromTheActiveSimulation() {
        ParanoiaSchedulerSimulator.SimulationReport report = ParanoiaSchedulerSimulator.simulate(
                ParanoiaSchedulerSimulator.Scenario.reference(4, 3, 3, 100.0D / 3.0D, 0xE07F0111L));
        assertFalse(report.effectiveWeights().containsKey("primary:armor_break"));
        assertFalse(report.effectiveWeights().containsKey("primary:forced_drop"));
        assertFalse(report.effectiveWeights().containsKey("primary:giant_sun"));
        assertFalse(report.effectiveWeights().containsKey("primary:corrupt_message"));
        assertFalse(report.effectiveWeights().containsKey("special:climber"));
        assertFalse(report.countsByEvent().containsKey("armor_break"));
        assertFalse(report.countsByEvent().containsKey("forced_drop"));
        assertFalse(report.countsByEvent().containsKey("giant_sun"));
        assertFalse(report.countsByEvent().containsKey("corrupt_message"));
        assertFalse(report.countsByEvent().containsKey("climber"));
    }

    @Test
    void approvedHuntingSpecialsAreRepresentedByTheLiveSimulation() {
        ParanoiaSchedulerSimulator.SimulationReport report = ParanoiaSchedulerSimulator.simulate(
                ParanoiaSchedulerSimulator.Scenario.reference(4, 3, 5, 50.0D, 0x48554E544552L));
        for (String id : java.util.List.of("echoer", "drifter", "ashwalker", "dredger", "flanker")) {
            assertTrue(report.effectiveWeights().containsKey("special:" + id), id);
            assertFalse(report.ineligibleEvents().contains("special:" + id), id);
        }
    }

    private static String snapshot(ParanoiaSchedulerSimulator.SimulationReport report) {
        return report.totalEvents() + "|"
                + report.eventsPerHour() + "|"
                + report.strongEventsPerHour() + "|"
                + report.averagePrimarySilenceSeconds() + "|"
                + report.maximumPrimarySilenceSeconds() + "|"
                + report.burstCount() + "|"
                + report.longEmptyPeriodCount() + "|"
                + report.countsByLane() + "|"
                + report.countsByEvent() + "|"
                + report.consecutiveRepeatsByFamily() + "|"
                + report.effectiveWeights() + "|"
                + report.ineligibleEvents();
    }

    @Test
    void everyPhaseProfileDangerCombinationCanBeSimulated() {
        for (int phase = 1; phase <= 4; phase++) {
            for (int profile = 1; profile <= 5; profile++) {
                for (int danger = 0; danger <= 5; danger++) {
                    ParanoiaSchedulerSimulator.SimulationReport report = ParanoiaSchedulerSimulator.simulate(
                            ParanoiaSchedulerSimulator.Scenario.reference(
                                    phase, profile, danger, 0.02D, 1000L * phase + 100L * profile + danger));
                    assertTrue(Double.isFinite(report.eventsPerHour()));
                    assertTrue(Double.isFinite(report.strongEventsPerHour()));
                    assertTrue(report.maximumPrimarySilenceSeconds() >= 0.0D);
                }
            }
        }
    }

    @Test
    void phaseAndDangerFilteringAreVisibleInReport() {
        ParanoiaSchedulerSimulator.SimulationReport report = ParanoiaSchedulerSimulator.simulate(
                ParanoiaSchedulerSimulator.Scenario.reference(1, 3, 0, 1.0D, 7L));
        assertTrue(report.ineligibleEvents().contains("primary:blackout"));
        assertTrue(report.ineligibleEvents().contains("primary:bell"));
        assertTrue(report.ineligibleEvents().contains("special:watcher"));
        assertEquals(0L, report.countsByLane().getOrDefault(ParanoiaEventLane.SPECIAL, 0L));
    }

    @Test
    void activePhaseTwoReportModelsTheAdditionalVarietyAndBellReduction() {
        ParanoiaSchedulerSimulator.SimulationReport active = ParanoiaSchedulerSimulator.simulate(
                ParanoiaSchedulerSimulator.Scenario.reference(2, 3, 3, 10.0D, 0xE07F0111L));
        ParanoiaSchedulerSimulator.SimulationReport historical = ParanoiaSchedulerSimulator.simulate(
                ParanoiaSchedulerSimulator.Scenario.reference111(2, 3, 3, 10.0D, 0xE07F0111L));

        assertEquals(6, active.effectiveWeights().get("primary:bell"));
        assertEquals(3, active.effectiveWeights().get("primary:hotbar_wrong_count"));
        assertEquals(1, active.effectiveWeights().get("primary:corrupt_toast"));
        assertEquals(2, active.effectiveWeights().get("special:hurler"));
        assertTrue(historical.ineligibleEvents().contains("primary:hotbar_wrong_count"));
        assertTrue(historical.ineligibleEvents().contains("primary:corrupt_toast"));
        assertTrue(historical.ineligibleEvents().contains("special:hurler"));
    }

    @Test
    void reportCollectionsAndScenarioLimitsAreProtected() {
        ParanoiaSchedulerSimulator.SimulationReport report = ParanoiaSchedulerSimulator.simulate(
                ParanoiaSchedulerSimulator.Scenario.reference(2, 2, 2, 0.1D, 11L));
        assertThrows(UnsupportedOperationException.class, () -> report.countsByEvent().put("x", 1L));
        assertThrows(UnsupportedOperationException.class, () -> report.effectiveWeights().put("x", 1));
        assertThrows(UnsupportedOperationException.class, () -> report.ineligibleEvents().add("x"));
        assertThrows(IllegalArgumentException.class,
                () -> ParanoiaSchedulerSimulator.Scenario.reference(0, 3, 3, 1.0D, 1L));
        assertThrows(IllegalArgumentException.class,
                () -> ParanoiaSchedulerSimulator.Scenario.reference(1, 6, 3, 1.0D, 1L));
        assertThrows(IllegalArgumentException.class,
                () -> ParanoiaSchedulerSimulator.Scenario.reference(1, 3, -1, 1.0D, 1L));
        assertThrows(IllegalArgumentException.class,
                () -> ParanoiaSchedulerSimulator.Scenario.reference(1, 3, 3, 0.0D, 1L));
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().withUpperCase().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
