package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.client.UncannyClientAudioEffects;
import com.eotv.echoofthevoid.client.UncannyDevMenuClientState;
import com.eotv.echoofthevoid.client.UncannyClientUiEffects;
import com.eotv.echoofthevoid.client.UncannyPassiveClientEffects;
import com.eotv.echoofthevoid.dev.UncannyDevQaStateService;
import net.minecraft.server.level.ServerPlayer;
import com.eotv.echoofthevoid.event.UncannyClientStateSync;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class UncannyNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private UncannyNetwork() {
    }

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(
                UncannyPhaseSyncPayload.TYPE,
                UncannyPhaseSyncPayload.STREAM_CODEC,
                UncannyNetwork::handlePhaseSync);
        registrar.playToClient(
                UncannyWeatherSyncPayload.TYPE,
                UncannyWeatherSyncPayload.STREAM_CODEC,
                UncannyNetwork::handleWeatherSync);
        registrar.playToClient(
                UncannyParanoiaSyncPayload.TYPE,
                UncannyParanoiaSyncPayload.STREAM_CODEC,
                UncannyNetwork::handleParanoiaSync);
        registrar.playToClient(
                UncannyDevMenuSyncPayload.TYPE,
                UncannyDevMenuSyncPayload.STREAM_CODEC,
                UncannyNetwork::handleDevMenuSync);
        registrar.playToClient(
                UncannyZombieRalePayload.TYPE,
                UncannyZombieRalePayload.STREAM_CODEC,
                UncannyNetwork::handleZombieRale);
        registrar.playToClient(
                UncannyHotbarWrongCountPayload.TYPE,
                UncannyHotbarWrongCountPayload.STREAM_CODEC,
                UncannyNetwork::handleHotbarWrongCount);
        registrar.playToClient(
                UncannyFalseRecipeToastPayload.TYPE,
                UncannyFalseRecipeToastPayload.STREAM_CODEC,
                UncannyNetwork::handleFalseRecipeToast);
        registrar.playToClient(
                UncannyPetRefusalVisualPayload.TYPE,
                UncannyPetRefusalVisualPayload.STREAM_CODEC,
                UncannyNetwork::handlePetRefusalVisual);
        registrar.playToServer(
                UncannyDevMenuActionPayload.TYPE,
                UncannyDevMenuActionPayload.STREAM_CODEC,
                UncannyNetwork::handleDevMenuAction);
        registrar.playToServer(
                UncannyDevMenuQaStatusPayload.TYPE,
                UncannyDevMenuQaStatusPayload.STREAM_CODEC,
                UncannyNetwork::handleDevMenuQaStatus);
    }

    private static void handlePhaseSync(final UncannyPhaseSyncPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyClientStateSync.applyClientPhase(payload.phaseIndex()));
    }

    private static void handleWeatherSync(final UncannyWeatherSyncPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyClientStateSync.applyClientWeather(payload.weatherEventId()));
    }

    private static void handleParanoiaSync(final UncannyParanoiaSyncPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyClientStateSync.applyClientParanoiaState(payload.hunterFogActive(), payload.giantSunActive()));
    }

    private static void handleDevMenuSync(final UncannyDevMenuSyncPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyDevMenuClientState.applySync(payload));
    }

    private static void handleZombieRale(final UncannyZombieRalePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyClientAudioEffects.playZombieRaleInHead(payload.volume(), payload.pitch()));
    }

    private static void handleHotbarWrongCount(final UncannyHotbarWrongCountPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyClientUiEffects.showHotbarWrongCount(payload.slot(), payload.fakeCount(), payload.durationTicks()));
    }

    private static void handleFalseRecipeToast(final UncannyFalseRecipeToastPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyClientUiEffects.showFalseRecipeToast(payload.title(), payload.subtitle()));
    }

    private static void handlePetRefusalVisual(final UncannyPetRefusalVisualPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyPassiveClientEffects.applyPetRefusalVisual(payload.entityId(), payload.active(), payload.durationTicks()));
    }

    private static void handleDevMenuAction(final UncannyDevMenuActionPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            UncannyDevQaStateService.handleAction(player, payload.entryId());
        });
    }

    private static void handleDevMenuQaStatus(final UncannyDevMenuQaStatusPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            UncannyDevQaStateService.updateStatus(player, payload.entryId(), payload.validatedGreen());
        });
    }
}
