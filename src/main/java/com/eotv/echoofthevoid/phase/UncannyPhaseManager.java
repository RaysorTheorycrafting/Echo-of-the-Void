package com.eotv.echoofthevoid.phase;

import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.event.UncannyClientStateSync;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class UncannyPhaseManager {
    private UncannyPhaseManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }

        UncannyWorldState state = UncannyWorldState.get(server);
        // World-persistent debug flag wins on world load/rejoin.
        if (UncannyConfig.DEBUG_LOGS.get() != state.isDebugLogsEnabled()) {
            UncannyConfig.DEBUG_LOGS.set(state.isDebugLogsEnabled());
            UncannyConfig.SPEC.save();
        }

        List<ServerPlayer> activePlayers = server.getPlayerList().getPlayers().stream()
                .filter(player -> !player.isSpectator())
                .toList();

        if (activePlayers.isEmpty()) {
            return;
        }

        for (ServerPlayer player : activePlayers) {
            UncannyClientStateSync.syncPhase(player, state.getCurrentPhaseIndex());
        }

        if (!state.isSystemEnabled() || state.isPhaseLockActive()) {
            return;
        }

        UncannyPhase phase = state.getPhase();
        if (phase.isFinal()) {
            return;
        }

        long durationTicks = Math.max(1L, phase.durationTicks());
        state.addProgress(1.0D / durationTicks);

        if (state.tryAdvancePhaseOneStep()) {
            broadcastPhase(server, state.getPhase());
        }
    }

    public static void applyDeathAcceleration(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        UncannyWorldState state = UncannyWorldState.get(server);
        if (!state.isSystemEnabled() || state.isPhaseLockActive() || state.getPhase().isFinal()) {
            return;
        }

        long now = server.getTickCount();
        UUID playerId = player.getUUID();
        Long lastBoost = state.getLastDeathBoostTick(playerId);
        long cooldownTicks = UncannyConfig.DEATH_PROGRESS_COOLDOWN_SECONDS.get() * 20L;

        if (isCooldownActive(lastBoost, now, cooldownTicks)) {
            return;
        }

        state.setLastDeathBoostTick(playerId, now);
        state.addProgress(UncannyConfig.DEATH_PROGRESS_BOOST.get());

        if (state.tryAdvancePhaseOneStep()) {
            broadcastPhase(server, state.getPhase());
        }
    }

    public static void setPhase(MinecraftServer server, int phaseIndex) {
        UncannyWorldState state = UncannyWorldState.get(server);
        if (phaseIndex <= 0) {
            purgeWorld(server);
            return;
        }

        state.setPurgeActive(false);
        state.setPhaseLockActive(false);
        state.setLockedPhaseIndex(phaseIndex);
        state.setPhase(UncannyPhase.fromIndex(phaseIndex));
        state.setProgressToNextPhase(state.getPhase().isFinal() ? 1.0D : 0.0D);
        syncAllPlayerPhaseTags(server, state.getCurrentPhaseIndex());
        broadcastPhase(server, state.getPhase());
    }

    public static void addPhaseProgress(MinecraftServer server, double amount) {
        UncannyWorldState state = UncannyWorldState.get(server);
        if (!state.isSystemEnabled() || state.isPhaseLockActive()) {
            return;
        }
        state.addProgress(amount);
        if (state.tryAdvancePhaseOneStep()) {
            broadcastPhase(server, state.getPhase());
        }
    }

    public static UncannyPhase getCurrentPhase(MinecraftServer server) {
        return UncannyWorldState.get(server).getPhase();
    }

    public static int getCurrentPhaseIndex(MinecraftServer server) {
        return UncannyWorldState.get(server).getCurrentPhaseIndex();
    }

    public static boolean isSystemEnabled(MinecraftServer server) {
        return UncannyWorldState.get(server).isSystemEnabled();
    }

    public static void lockPhase(MinecraftServer server, int phaseIndex) {
        UncannyWorldState state = UncannyWorldState.get(server);
        int clampedPhase = Math.max(1, Math.min(4, phaseIndex));
        state.setPurgeActive(false);
        state.setPhaseLockActive(true);
        state.setLockedPhaseIndex(clampedPhase);
        state.setPhase(UncannyPhase.fromIndex(clampedPhase));
        state.setProgressToNextPhase(state.getPhase().isFinal() ? 1.0D : 0.0D);
        syncAllPlayerPhaseTags(server, state.getCurrentPhaseIndex());
        broadcastPhase(server, state.getPhase());
    }

    public static void clearPhaseLock(MinecraftServer server) {
        UncannyWorldState state = UncannyWorldState.get(server);
        if (!state.isPhaseLockActive()) {
            return;
        }

        state.setPhaseLockActive(false);
        state.setProgressToNextPhase(state.getPhase().isFinal() ? 1.0D : 0.0D);
    }

    public static void purgeWorld(MinecraftServer server) {
        UncannyWorldState state = UncannyWorldState.get(server);
        state.setPurgeActive(true);
        state.setPhaseLockActive(false);
        state.setProgressToNextPhase(0.0D);
        syncAllPlayerPhaseTags(server, 0);
        server.getPlayerList().broadcastSystemMessage(Component.translatable("message.echoofthevoid.phase0"), false);
    }

    public static void restartFromPurge(MinecraftServer server) {
        UncannyWorldState state = UncannyWorldState.get(server);
        state.setPurgeActive(false);
        state.setPhaseLockActive(false);
        state.setLockedPhaseIndex(1);
        state.setPhase(UncannyPhase.PHASE_1);
        state.setProgressToNextPhase(0.0D);
        syncAllPlayerPhaseTags(server, 1);
        broadcastPhase(server, UncannyPhase.PHASE_1);
    }

    private static void broadcastPhase(MinecraftServer server, UncannyPhase phase) {
        server.getPlayerList().broadcastSystemMessage(Component.translatable(phase.messageKey()), false);
    }

    private static boolean isCooldownActive(Long lastTick, long now, long cooldownTicks) {
        return cooldownTicks > 0 && lastTick != null && now >= lastTick && (now - lastTick) < cooldownTicks;
    }

    private static void syncAllPlayerPhaseTags(MinecraftServer server, int phaseIndex) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UncannyClientStateSync.syncPhase(player, phaseIndex);
        }
    }
}

