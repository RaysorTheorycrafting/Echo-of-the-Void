package com.eotv.echoofthevoid.campaign;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Transient per-session guard preventing a campaign climax from starting while a player is
 * joining, respawning, or completing a dimension change. Nothing in this tracker is persisted:
 * an interrupted campaign attempt is postponed through the existing persistent retry deadline.
 */
public final class CampaignPlayerContextGrace {
    private final long graceTicks;
    private final Map<UUID, Long> stableAtTicks = new HashMap<>();

    public CampaignPlayerContextGrace(long graceTicks) {
        if (graceTicks < 0L) {
            throw new IllegalArgumentException("graceTicks must be non-negative");
        }
        this.graceTicks = graceTicks;
    }

    public void observe(UUID playerId, long now) {
        Objects.requireNonNull(playerId, "playerId");
        stableAtTicks.putIfAbsent(playerId, deadline(now));
    }

    public void defer(UUID playerId, long now) {
        Objects.requireNonNull(playerId, "playerId");
        long deadline = deadline(now);
        stableAtTicks.merge(playerId, deadline, Math::max);
    }

    public boolean isStable(UUID playerId, long now) {
        Objects.requireNonNull(playerId, "playerId");
        Long stableAt = stableAtTicks.get(playerId);
        return stableAt != null && now >= stableAt;
    }

    public void forget(UUID playerId) {
        stableAtTicks.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    public void clear() {
        stableAtTicks.clear();
    }

    private long deadline(long now) {
        if (graceTicks > 0L && now > Long.MAX_VALUE - graceTicks) {
            return Long.MAX_VALUE;
        }
        return now + graceTicks;
    }
}
