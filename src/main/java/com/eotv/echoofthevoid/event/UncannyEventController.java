package com.eotv.echoofthevoid.event;

import com.eotv.echoofthevoid.dev.UncannyDevQaStateService;
import com.eotv.echoofthevoid.phase.UncannyPhaseManager;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class UncannyEventController {
    private UncannyEventController() {
    }

    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.getServer() == null) {
            return;
        }

        UncannyWorldState state = UncannyWorldState.get(player.getServer());
        state.setLastDeathTick(player.getUUID(), player.getServer().getTickCount());

        UncannyPhaseManager.applyDeathAcceleration(player);
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.getServer() == null) {
            return;
        }

        UncannyWorldState state = UncannyWorldState.get(player.getServer());
        state.setLastRespawnTick(player.getUUID(), player.getServer().getTickCount());
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UncannyClientStateSync.clearPlayerCache(player);
        }
        UncannyDevQaStateService.onPlayerLogout(event);
    }
}

