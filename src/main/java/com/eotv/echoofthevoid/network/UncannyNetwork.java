package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.client.UncannyClientAudioEffects;
import com.eotv.echoofthevoid.client.UncannyNativeAnomalyClientEffects;
import com.eotv.echoofthevoid.client.UncannyLocalizedWeatherClientEffects;
import com.eotv.echoofthevoid.client.UncannyDevMenuClientState;
import com.eotv.echoofthevoid.client.UncannyClientUiEffects;
import com.eotv.echoofthevoid.client.UncannyPassiveClientEffects;
import com.eotv.echoofthevoid.client.UncannyVanillaVariantClientEffects;
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
                UncannyDevMenuResultPayload.TYPE,
                UncannyDevMenuResultPayload.STREAM_CODEC,
                UncannyNetwork::handleDevMenuResult);
        registrar.playToClient(
                UncannyZombieRalePayload.TYPE,
                UncannyZombieRalePayload.STREAM_CODEC,
                UncannyNetwork::handleZombieRale);
        registrar.playToClient(
                UncannyMentalSoundPayload.TYPE,
                UncannyMentalSoundPayload.STREAM_CODEC,
                UncannyNetwork::handleMentalSound);
        registrar.playToClient(
                UncannyOrphanShadowPayload.TYPE,
                UncannyOrphanShadowPayload.STREAM_CODEC,
                UncannyNetwork::handleOrphanShadow);
        registrar.playToClient(
                UncannyArmorStandPosePayload.TYPE,
                UncannyArmorStandPosePayload.STREAM_CODEC,
                UncannyNetwork::handleArmorStandPose);
        registrar.playToClient(
                UncannyFishingTugPayload.TYPE,
                UncannyFishingTugPayload.STREAM_CODEC,
                UncannyNetwork::handleFishingTug);
        registrar.playToClient(
                UncannyEmptyLeadPayload.TYPE,
                UncannyEmptyLeadPayload.STREAM_CODEC,
                UncannyNetwork::handleEmptyLead);
        registrar.playToClient(
                UncannyPaintingVariantPayload.TYPE,
                UncannyPaintingVariantPayload.STREAM_CODEC,
                UncannyNetwork::handlePaintingVariant);
        registrar.playToClient(
                UncannyReturnedItemPayload.TYPE,
                UncannyReturnedItemPayload.STREAM_CODEC,
                UncannyNetwork::handleReturnedItem);
        registrar.playToClient(
                UncannyMapIntruderPayload.TYPE,
                UncannyMapIntruderPayload.STREAM_CODEC,
                UncannyNetwork::handleMapIntruder);
        registrar.playToClient(
                UncannyArrowGazePayload.TYPE,
                UncannyArrowGazePayload.STREAM_CODEC,
                UncannyNetwork::handleArrowGaze);
        registrar.playToClient(
                UncannySuspendedFallPayload.TYPE,
                UncannySuspendedFallPayload.STREAM_CODEC,
                UncannyNetwork::handleSuspendedFall);
        registrar.playToClient(
                UncannyBeaconFragmentPayload.TYPE,
                UncannyBeaconFragmentPayload.STREAM_CODEC,
                UncannyNetwork::handleBeaconFragment);
        registrar.playToClient(
                UncannyStrayExperiencePayload.TYPE,
                UncannyStrayExperiencePayload.STREAM_CODEC,
                UncannyNetwork::handleStrayExperience);
        registrar.playToClient(
                UncannyExtraHerdAnimalPayload.TYPE,
                UncannyExtraHerdAnimalPayload.STREAM_CODEC,
                UncannyNetwork::handleExtraHerdAnimal);
        registrar.playToClient(
                UncannyLocalizedWeatherPayload.TYPE,
                UncannyLocalizedWeatherPayload.STREAM_CODEC,
                UncannyNetwork::handleLocalizedWeather);
        registrar.playToClient(
                UncannyVanillaVariantVisualPayload.TYPE,
                UncannyVanillaVariantVisualPayload.STREAM_CODEC,
                UncannyNetwork::handleVanillaVariantVisual);
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
                UncannyDevMenuRunPayload.TYPE,
                UncannyDevMenuRunPayload.STREAM_CODEC,
                UncannyNetwork::handleDevMenuRun);
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

    private static void handleDevMenuResult(final UncannyDevMenuResultPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyDevMenuClientState.applyResult(payload));
    }

    private static void handleZombieRale(final UncannyZombieRalePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyClientAudioEffects.playZombieRaleInHead(payload.volume(), payload.pitch()));
    }

    private static void handleMentalSound(final UncannyMentalSoundPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyClientAudioEffects.playInHead(
                payload.soundId(),
                payload.sourceName(),
                payload.volume(),
                payload.pitch(),
                payload.maximumDurationTicks()));
    }

    private static void handleOrphanShadow(final UncannyOrphanShadowPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyNativeAnomalyClientEffects.applyOrphanShadow(payload));
    }

    private static void handleArmorStandPose(
            final UncannyArmorStandPosePayload payload,
            final IPayloadContext context) {
        context.enqueueWork(() -> UncannyNativeAnomalyClientEffects.applyArmorStandPose(payload));
    }

    private static void handleFishingTug(final UncannyFishingTugPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyNativeAnomalyClientEffects.applyFishingTug(payload));
    }

    private static void handleEmptyLead(final UncannyEmptyLeadPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyNativeAnomalyClientEffects.applyEmptyLead(payload));
    }

    private static void handlePaintingVariant(
            final UncannyPaintingVariantPayload payload,
            final IPayloadContext context) {
        context.enqueueWork(() -> UncannyNativeAnomalyClientEffects.applyPaintingVariant(payload));
    }

    private static void handleReturnedItem(final UncannyReturnedItemPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyNativeAnomalyClientEffects.applyReturnedItem(payload));
    }

    private static void handleMapIntruder(final UncannyMapIntruderPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyNativeAnomalyClientEffects.applyMapIntruder(payload));
    }

    private static void handleArrowGaze(final UncannyArrowGazePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyNativeAnomalyClientEffects.applyArrowGaze(payload));
    }

    private static void handleSuspendedFall(final UncannySuspendedFallPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyNativeAnomalyClientEffects.applySuspendedFall(payload));
    }

    private static void handleBeaconFragment(final UncannyBeaconFragmentPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyNativeAnomalyClientEffects.applyBeaconFragment(payload));
    }

    private static void handleStrayExperience(final UncannyStrayExperiencePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyNativeAnomalyClientEffects.applyStrayExperience(payload));
    }

    private static void handleExtraHerdAnimal(final UncannyExtraHerdAnimalPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyNativeAnomalyClientEffects.applyExtraHerdAnimal(payload));
    }

    private static void handleLocalizedWeather(final UncannyLocalizedWeatherPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> UncannyLocalizedWeatherClientEffects.apply(payload));
    }

    private static void handleVanillaVariantVisual(
            final UncannyVanillaVariantVisualPayload payload,
            final IPayloadContext context) {
        context.enqueueWork(() -> UncannyVanillaVariantClientEffects.apply(payload));
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

    private static void handleDevMenuRun(final UncannyDevMenuRunPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            UncannyDevQaStateService.handleRun(
                    player,
                    payload.entryId(),
                    payload.targetName(),
                    payload.spawnDistance());
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
