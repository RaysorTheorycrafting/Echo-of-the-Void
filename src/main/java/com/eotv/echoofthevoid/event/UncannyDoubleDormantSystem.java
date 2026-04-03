package com.eotv.echoofthevoid.event;

import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyDoubleDormantEntity;
import com.eotv.echoofthevoid.phase.UncannyPhase;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class UncannyDoubleDormantSystem {
    private UncannyDoubleDormantSystem() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (level.dimension() == Level.END) {
            return;
        }
        if (UncannyParanoiaEventSystem.isGrandEventAutoPauseActive(level)) {
            return;
        }

        UncannyWorldState state = UncannyWorldState.get(server);
        if (!state.isSystemEnabled()) {
            return;
        }
        if (state.getPhase().index() < UncannyPhase.PHASE_3.index()) {
            return;
        }

        long now = server.getTickCount();
        UUID playerId = player.getUUID();

        BaseContext baseContext = resolveBaseContext(player, server);

        int radius = UncannyConfig.BASE_RADIUS_BLOCKS.get();
        boolean inBase = player.blockPosition().distSqr(baseContext.baseCenter) <= (long) radius * radius;
        Long leftSince = state.getLeftBaseSinceTick(playerId);
        if (leftSince != null && now < leftSince) {
            state.setLeftBaseSinceTick(playerId, now);
            leftSince = now;
        }

        if (!inBase) {
            if (leftSince == null) {
                state.setLeftBaseSinceTick(playerId, now);
            }
            return;
        }

        if (leftSince == null) {
            return;
        }

        long requiredAbsenceTicks = UncannyConfig.ABSENCE_SECONDS.get() * 20L;
        if (now - leftSince < requiredAbsenceTicks) {
            state.clearLeftBaseSinceTick(playerId);
            return;
        }

        // "Return to base" is consumed once checked: no delayed trigger while staying in base.
        state.clearLeftBaseSinceTick(playerId);

        if (!baseContext.hasValidBed) {
            return;
        }

        if (isBlockedByRecentDeathOrRespawn(state, playerId, now)) {
            return;
        }

        Long playerCooldown = state.getLastDoubleDormantTick(playerId);
        long playerCooldownTicks = UncannyConfig.DOUBLE_DORMANT_COOLDOWN_SECONDS.get() * 20L;
        if (isCooldownActive(playerCooldown, now, playerCooldownTicks)) {
            return;
        }

        long globalCooldownTicks = UncannyParanoiaEventSystem.getEffectiveGlobalCooldownTicks(state.getPhase());
        if (isCooldownActive(state.getLastGlobalEventTick(), now, globalCooldownTicks)) {
            return;
        }

        if (level.random.nextDouble() > UncannyConfig.DOUBLE_DORMANT_TRIGGER_CHANCE.get()) {
            return;
        }

        spawnMimic(player, baseContext.baseCenter, level);
        state.setLastDoubleDormantTick(playerId, now);
        state.setLastGlobalEventTick(now);
    }

    public static void forceMimic(ServerPlayer player) {
        if (player.getServer() == null) {
            return;
        }

        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return;
        }
        BaseContext context = resolveBaseContext(player, player.getServer());
        spawnMimic(player, context.baseCenter, player.serverLevel());

        UncannyWorldState state = UncannyWorldState.get(player.getServer());
        long now = player.getServer().getTickCount();
        state.setLastDoubleDormantTick(player.getUUID(), now);
        state.setLastGlobalEventTick(now);
    }

    public static String getMimicDebugReport(ServerPlayer player) {
        if (player.getServer() == null) {
            return "Mimic debug unavailable: server is null.";
        }

        MinecraftServer server = player.getServer();
        UncannyWorldState state = UncannyWorldState.get(server);
        long now = server.getTickCount();
        UUID playerId = player.getUUID();

        BaseContext baseContext = resolveBaseContext(player, server);
        int radius = UncannyConfig.BASE_RADIUS_BLOCKS.get();
        boolean inBase = player.blockPosition().distSqr(baseContext.baseCenter) <= (long) radius * radius;
        Long leftSince = state.getLeftBaseSinceTick(playerId);
        long requiredAbsenceTicks = UncannyConfig.ABSENCE_SECONDS.get() * 20L;
        long awayTicks = leftSince == null || now < leftSince ? 0L : Math.max(0L, now - leftSince);

        Long lastDoubleDormant = state.getLastDoubleDormantTick(playerId);
        long playerCooldownTicks = UncannyConfig.DOUBLE_DORMANT_COOLDOWN_SECONDS.get() * 20L;
        long playerCooldownLeft = remainingCooldownTicks(lastDoubleDormant, now, playerCooldownTicks);

        long globalCooldownTicks = UncannyParanoiaEventSystem.getEffectiveGlobalCooldownTicks(state.getPhase());
        long lastGlobal = state.getLastGlobalEventTick();
        long globalCooldownLeft = remainingCooldownTicks(lastGlobal, now, globalCooldownTicks);

        boolean blockedByRecent = isBlockedByRecentDeathOrRespawn(state, playerId, now);
        boolean phaseOk = state.getPhase().index() >= UncannyPhase.PHASE_3.index();
        boolean dimensionOk = player.serverLevel().dimension() != Level.END;
        boolean absenceReady = leftSince != null && awayTicks >= requiredAbsenceTicks;

        boolean eligibleNow = phaseOk
                && dimensionOk
                && baseContext.hasValidBed
                && inBase
                && absenceReady
                && !blockedByRecent
                && playerCooldownLeft <= 0
                && globalCooldownLeft <= 0;

        return "Mimic debug | phase=" + state.getCurrentPhaseIndex()
                + " | systemEnabled=" + state.isSystemEnabled()
                + " | dim=" + player.serverLevel().dimension().location()
                + " | inBase=" + inBase
                + " | hasBed=" + baseContext.hasValidBed
                + " | away=" + ticksToSeconds(awayTicks) + "s/" + ticksToSeconds(requiredAbsenceTicks) + "s"
                + " | playerCd=" + ticksToSeconds(playerCooldownLeft) + "s"
                + " | globalCd=" + ticksToSeconds(globalCooldownLeft) + "s"
                + " | blockedRecent=" + blockedByRecent
                + " | triggerChance=" + String.format(Locale.ROOT, "%.0f%%", UncannyConfig.DOUBLE_DORMANT_TRIGGER_CHANCE.get() * 100.0D)
                + " | eligibleNow=" + eligibleNow;
    }

    private static boolean isBlockedByRecentDeathOrRespawn(UncannyWorldState state, UUID playerId, long now) {
        long recentDeathTicks = UncannyConfig.BLOCK_ON_RECENT_DEATH_SECONDS.get() * 20L;
        Long lastDeath = state.getLastDeathTick(playerId);
        if (isCooldownActive(lastDeath, now, recentDeathTicks)) {
            return true;
        }

        long respawnGraceTicks = UncannyConfig.RESPAWN_GRACE_SECONDS.get() * 20L;
        Long lastRespawn = state.getLastRespawnTick(playerId);
        return isCooldownActive(lastRespawn, now, respawnGraceTicks);
    }

    private static void spawnMimic(ServerPlayer player, BlockPos baseCenter, ServerLevel level) {
        UncannyDoubleDormantEntity doubleDormant = UncannyEntityRegistry.UNCANNY_DOUBLE_DORMANT.get().create(level);
        if (doubleDormant == null) {
            return;
        }

        doubleDormant.moveTo(baseCenter.getX() + 0.5D, baseCenter.getY() + 1.0D, baseCenter.getZ() + 0.5D, player.getYRot(), 0.0F);
        doubleDormant.copyTarget(player, baseCenter, baseCenter);
        level.addFreshEntity(doubleDormant);
    }

    private static BaseContext resolveBaseContext(ServerPlayer player, MinecraftServer server) {
        BlockPos respawnPos = player.getRespawnPosition();
        if (respawnPos == null) {
            return new BaseContext(server.overworld().getSharedSpawnPos(), false);
        }

        ServerLevel respawnLevel = server.getLevel(player.getRespawnDimension());
        if (respawnLevel == null) {
            return new BaseContext(server.overworld().getSharedSpawnPos(), false);
        }

        boolean hasValidBed = respawnLevel.getBlockState(respawnPos).is(BlockTags.BEDS);
        return new BaseContext(respawnPos, hasValidBed);
    }

    private static long ticksToSeconds(long ticks) {
        return ticks / 20L;
    }

    private static boolean isCooldownActive(Long lastTick, long now, long cooldownTicks) {
        return cooldownTicks > 0 && lastTick != null && now >= lastTick && (now - lastTick) < cooldownTicks;
    }

    private static boolean isCooldownActive(long lastTick, long now, long cooldownTicks) {
        return cooldownTicks > 0 && lastTick != Long.MIN_VALUE && now >= lastTick && (now - lastTick) < cooldownTicks;
    }

    private static long remainingCooldownTicks(Long lastTick, long now, long cooldownTicks) {
        if (cooldownTicks <= 0 || lastTick == null || now < lastTick) {
            return 0L;
        }
        return Math.max(0L, cooldownTicks - (now - lastTick));
    }

    private static long remainingCooldownTicks(long lastTick, long now, long cooldownTicks) {
        if (cooldownTicks <= 0 || lastTick == Long.MIN_VALUE || now < lastTick) {
            return 0L;
        }
        return Math.max(0L, cooldownTicks - (now - lastTick));
    }

    private record BaseContext(BlockPos baseCenter, boolean hasValidBed) {
    }
}

