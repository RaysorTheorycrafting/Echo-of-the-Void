package com.eotv.echoofthevoid.event;

import com.eotv.echoofthevoid.campaign.UncannyCampaignDirector;
import com.eotv.echoofthevoid.diagnostics.DiagnosticSeverity;
import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import com.eotv.echoofthevoid.event.paranoia.DebugBoundsRules;
import com.eotv.echoofthevoid.network.UncannyDebugBoundsPayload;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server authority for the private F3+B easter egg; it never creates a world entity. */
public final class UncannyDebugBoundsEventSystem {
    private static final Set<UUID> CONSUMED_THIS_SERVER_RUN = new HashSet<>();
    private static final Map<UUID, Long> ACTIVE_UNTIL_TICK = new HashMap<>();

    private UncannyDebugBoundsEventSystem() {
    }

    public static void onClientHitboxState(ServerPlayer player, boolean enabled) {
        if (player == null || player.getServer() == null) {
            return;
        }
        UUID playerId = player.getUUID();
        if (!enabled) {
            if (ACTIVE_UNTIL_TICK.remove(playerId) != null) {
                sendStop(player);
            }
            return;
        }

        UncannyWorldState state = UncannyWorldState.get(player.getServer());
        ServerLevel overworld = player.getServer().overworld();
        boolean majorPause = UncannyParanoiaEventSystem.isGrandEventAutoPauseActive(overworld)
                || UncannyParanoiaEventSystem.isTensionBuilderAutoPauseActive(overworld);
        boolean eligible = state.isSystemEnabled() && DebugBoundsRules.canStartNaturally(
                state.getCurrentPhaseIndex(), true, CONSUMED_THIS_SERVER_RUN.contains(playerId),
                player.isAlive(), player.isSpectator(), player.isSleeping(), majorPause);
        if (!eligible) {
            UncannyDiagnostics.recordForPlayer(
                    player, DiagnosticSeverity.INFO, "event", "debug_bounds_not_started",
                    UncannyDiagnostics.fields(
                            "phase", state.getCurrentPhaseIndex(),
                            "already_consumed", CONSUMED_THIS_SERVER_RUN.contains(playerId),
                            "major_pause", majorPause));
            return;
        }
        CONSUMED_THIS_SERVER_RUN.add(playerId);
        start(player, false);
    }

    public static boolean triggerForDebug(ServerPlayer player) {
        if (player == null || player.getServer() == null || !player.isAlive() || player.isSpectator()) {
            return false;
        }
        start(player, true);
        return true;
    }

    public static void onPlayerLogout(ServerPlayer player) {
        if (player != null) {
            ACTIVE_UNTIL_TICK.remove(player.getUUID());
        }
    }

    public static void onPlayerChangedDimension(ServerPlayer player) {
        if (player != null && ACTIVE_UNTIL_TICK.remove(player.getUUID()) != null) {
            sendStop(player);
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE_UNTIL_TICK.isEmpty()) {
            return;
        }
        long now = event.getServer().getTickCount();
        ACTIVE_UNTIL_TICK.entrySet().removeIf(entry -> {
            if (now < entry.getValue()) {
                return false;
            }
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                sendStop(player);
            }
            return true;
        });
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        CONSUMED_THIS_SERVER_RUN.clear();
        ACTIVE_UNTIL_TICK.clear();
    }

    public static boolean wasConsumed(UUID playerId) {
        return CONSUMED_THIS_SERVER_RUN.contains(playerId);
    }

    private static void start(ServerPlayer player, boolean forced) {
        long now = player.getServer().getTickCount();
        long seed = player.getRandom().nextLong() ^ player.getUUID().getMostSignificantBits() ^ now;
        ACTIVE_UNTIL_TICK.put(player.getUUID(), now + DebugBoundsRules.DURATION_TICKS);
        PacketDistributor.sendToPlayer(player, new UncannyDebugBoundsPayload(
                true, seed, DebugBoundsRules.DURATION_TICKS, DebugBoundsRules.PRESENCE_COUNT));

        UncannyWorldState state = UncannyWorldState.get(player.getServer());
        if (!forced) {
            state.setLastGlobalEventTick(now);
            UncannyCampaignDirector.recordEvent(state, "debug_bounds");
        }
        UncannyDiagnostics.recordEventOutcome(
                player, "contextual", "debug_bounds", "started",
                UncannyDiagnostics.fields(
                        "forced", forced,
                        "duration_ticks", DebugBoundsRules.DURATION_TICKS,
                        "presence_count", DebugBoundsRules.PRESENCE_COUNT,
                        "real_entities_created", 0));
    }

    private static void sendStop(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new UncannyDebugBoundsPayload(false, 0L, 0, 0));
    }
}
