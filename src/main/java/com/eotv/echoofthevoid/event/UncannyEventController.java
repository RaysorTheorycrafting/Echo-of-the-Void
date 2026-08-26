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
        state.recordDeathSite(
                player.getUUID(), player.serverLevel().dimension(), player.blockPosition(),
                player.getServer().getTickCount());

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
        UncannyParanoiaEventSystem.deferCampaignCulminationForPlayer(player);
        UncannyClientStateSync.clearPlayerCache(player);
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UncannyParanoiaEventSystem.forgetCampaignCulminationPlayer(player);
            UncannyClientStateSync.clearPlayerCache(player);
        }
        UncannyDevQaStateService.onPlayerLogout(event);
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UncannyParanoiaEventSystem.deferCampaignCulminationForPlayer(player);
            // Force phase/weather/paranoia payloads to be evaluated again for the new dimension.
            // In particular, an Overworld-only presentation must be cleared immediately in the Nether or End.
            UncannyClientStateSync.clearPlayerCache(player);
        }
    }
}

