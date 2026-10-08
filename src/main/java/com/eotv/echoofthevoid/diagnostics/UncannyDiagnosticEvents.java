package com.eotv.echoofthevoid.diagnostics;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class UncannyDiagnosticEvents {
    private UncannyDiagnosticEvents() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
        UncannyDiagnostics.start(event.getServer());
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        UncannyDiagnostics.serverStopping(event.getServer());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        UncannyDiagnostics.stop(event.getServer(), "server_stopped");
    }

    public static void onServerTickPre(ServerTickEvent.Pre event) {
        UncannyDiagnostics.onServerTickPre(event.getServer());
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        UncannyDiagnostics.onServerTickPost(event.getServer());
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UncannyDiagnostics.playerJoined(player);
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UncannyDiagnostics.playerLeft(player);
        }
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UncannyDiagnostics.playerChangedDimension(
                    player,
                    event.getFrom().location().toString(),
                    event.getTo().location().toString());
        }
    }

    public static void onPlayerStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UncannyDiagnostics.specialTrackingChanged(player, event.getTarget(), true);
        }
    }

    public static void onPlayerStopTracking(PlayerEvent.StopTracking event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UncannyDiagnostics.specialTrackingChanged(player, event.getTarget(), false);
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        UncannyDiagnostics.livingDeath(
                event.getEntity(),
                event.getSource().getMsgId(),
                event.getSource().getEntity());
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            UncannyDiagnostics.entityJoined(event.getEntity());
        }
    }

    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            UncannyDiagnostics.entityLeft(event.getEntity());
        }
    }

    public static void onCommand(CommandEvent event) {
        UncannyDiagnostics.uncannyCommand(
                event.getParseResults().getContext().getSource().getServer(),
                event.getParseResults().getReader().getString());
    }

    public static void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            UncannyDiagnostics.levelSaved(level);
        }
    }
}
