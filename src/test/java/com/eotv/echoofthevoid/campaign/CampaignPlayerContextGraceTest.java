package com.eotv.echoofthevoid.campaign;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CampaignPlayerContextGraceTest {
    private static final long GRACE_TICKS = 20L * 18L;

    @Test
    void newSessionIsIneligibleUntilItsGraceHasElapsed() {
        CampaignPlayerContextGrace tracker = new CampaignPlayerContextGrace(GRACE_TICKS);
        UUID player = UUID.randomUUID();

        assertFalse(tracker.isStable(player, 100L));
        tracker.observe(player, 100L);
        assertFalse(tracker.isStable(player, 100L + GRACE_TICKS - 1L));
        assertTrue(tracker.isStable(player, 100L + GRACE_TICKS));

        tracker.observe(player, 200L);
        assertTrue(tracker.isStable(player, 200L + GRACE_TICKS - 1L));
    }

    @Test
    void transitionExtendsGraceAndLogoutForgetsThePlayer() {
        CampaignPlayerContextGrace tracker = new CampaignPlayerContextGrace(GRACE_TICKS);
        UUID player = UUID.randomUUID();

        tracker.observe(player, 100L);
        tracker.defer(player, 500L);
        assertFalse(tracker.isStable(player, 500L + GRACE_TICKS - 1L));
        assertTrue(tracker.isStable(player, 500L + GRACE_TICKS));

        tracker.forget(player);
        assertFalse(tracker.isStable(player, Long.MAX_VALUE));
    }

    @Test
    void clearResetsAllSessionsAndDeadlineSaturatesSafely() {
        CampaignPlayerContextGrace tracker = new CampaignPlayerContextGrace(GRACE_TICKS);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        tracker.observe(first, Long.MAX_VALUE - 10L);
        tracker.observe(second, 0L);
        assertFalse(tracker.isStable(first, Long.MAX_VALUE - 1L));
        assertTrue(tracker.isStable(first, Long.MAX_VALUE));

        tracker.clear();
        assertFalse(tracker.isStable(first, Long.MAX_VALUE));
        assertFalse(tracker.isStable(second, Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> new CampaignPlayerContextGrace(-1L));
    }
}
