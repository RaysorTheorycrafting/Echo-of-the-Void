package com.eotv.echoofthevoid.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClientPerformanceRulesTest {
    @Test
    void absoluteAndRelativeDropsAreDetectedWithoutTreatingInvalidSamplesAsLag() {
        assertTrue(ClientPerformanceRules.isDegraded(45, 60));
        assertTrue(ClientPerformanceRules.isDegraded(50, 100));
        assertFalse(ClientPerformanceRules.isDegraded(56, 100));
        assertFalse(ClientPerformanceRules.isDegraded(50, 55));
        assertFalse(ClientPerformanceRules.isDegraded(0, 120));
    }

    @Test
    void baselineRecoversQuicklyButDoesNotCollapseOnOneBadSecond() {
        assertEquals(80, ClientPerformanceRules.updateBaseline(0, 80));
        assertEquals(90, ClientPerformanceRules.updateBaseline(80, 120));
        assertEquals(89, ClientPerformanceRules.updateBaseline(90, 25));
    }
}
