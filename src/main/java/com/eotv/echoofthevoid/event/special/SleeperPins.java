package com.eotv.echoofthevoid.event.special;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;

/**
 * Sleepers pinned in their bed by a Sleeper?. While pinned a player cannot leave the bed and does not
 * count as "slept long enough", so the night cannot be skipped from under the attack.
 */
public final class SleeperPins {
    private static final Set<UUID> PINNED = ConcurrentHashMap.newKeySet();

    private SleeperPins() {
    }

    public static boolean isPinned(Player player) {
        return player != null && !player.level().isClientSide() && PINNED.contains(player.getUUID());
    }

    public static void pin(UUID player) {
        PINNED.add(player);
    }

    public static void release(UUID player) {
        PINNED.remove(player);
    }

    public static void clear() {
        PINNED.clear();
    }
}
