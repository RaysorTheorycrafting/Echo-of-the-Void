package com.eotv.echoofthevoid.event.paranoia.nativeevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MinecraftNativeAnomalyRulesTest {
    @Test
    void emptyLeadRemainsVisibleForAboutTenSeconds() {
        assertEquals(190, MinecraftNativeAnomalyRules.emptyLeadDurationTicks(-1));
        assertEquals(200, MinecraftNativeAnomalyRules.emptyLeadDurationTicks(10));
        assertEquals(210, MinecraftNativeAnomalyRules.emptyLeadDurationTicks(999));
        assertEquals(190, MinecraftNativeAnomalyRules.EMPTY_LEAD_MIN_VISIBLE_TICKS);
        assertTrue(MinecraftNativeAnomalyRules.EMPTY_LEAD_MIN_VISIBLE_TICKS
                <= MinecraftNativeAnomalyRules.EMPTY_LEAD_MIN_DURATION_TICKS);
    }

    @Test
    void emptyWakeRequiresAPathLongEnoughForAReadableApproach() {
        assertEquals(22, MinecraftNativeAnomalyRules.EMPTY_WAKE_MIN_POINTS);
        assertEquals(30, MinecraftNativeAnomalyRules.EMPTY_WAKE_TARGET_POINTS);
        assertEquals(7, MinecraftNativeAnomalyRules.EMPTY_WAKE_PULSE_INTERVAL_TICKS);
        assertEquals(154, MinecraftNativeAnomalyRules.emptyWakeDurationTicks(22));
        assertEquals(210, MinecraftNativeAnomalyRules.emptyWakeDurationTicks(30));
    }
}
