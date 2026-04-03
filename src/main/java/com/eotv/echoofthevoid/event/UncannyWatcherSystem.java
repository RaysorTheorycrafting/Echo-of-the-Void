package com.eotv.echoofthevoid.event;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyWatcherEntity;
import com.eotv.echoofthevoid.phase.UncannyPhase;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class UncannyWatcherSystem {
    private static final double[] PROFILE_WATCHER_CHANCE_MULTIPLIER = {0.65D, 0.85D, 1.00D, 1.25D, 1.55D};
    private static final int[] PROFILE_WATCHER_COOLDOWN_SECONDS = {780, 600, 450, 300, 180};
    private static final int[] PROFILE_WATCHER_INTERVAL_SECONDS = {15, 12, 10, 8, 6};
    private static final double[] DANGER_WATCHER_CHANCE_MULTIPLIER = {1.55D, 1.35D, 1.15D, 1.00D, 0.85D, 0.70D};
    private static final double[] DANGER_WATCHER_COOLDOWN_MULTIPLIER = {0.68D, 0.80D, 0.92D, 1.00D, 1.12D, 1.28D};

    private UncannyWatcherSystem() {
    }

    private static void debugLog(String message, Object... args) {
        if (UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info("[UncannyDebug/Watcher] " + message, args);
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (shouldBlockWatcherSpawn(player)) {
            return;
        }

        UncannyWorldState state = UncannyWorldState.get(server);
        if (!state.isSystemEnabled()) {
            return;
        }
        if (state.getPhase().index() < UncannyPhase.PHASE_2.index()) {
            return;
        }

        int profile = getIntensityProfile();
        int danger = getDangerLevel();
        int intervalSeconds = Math.min(
                Math.max(1, UncannyConfig.WATCHER_CHECK_INTERVAL_SECONDS.get()),
                PROFILE_WATCHER_INTERVAL_SECONDS[profile - 1]);
        int intervalTicks = Math.max(1, intervalSeconds * 20);
        if (server.getTickCount() % intervalTicks != 0) {
            return;
        }

        if (!isNightOrTwilight(level) || !level.canSeeSky(player.blockPosition())) {
            return;
        }

        UUID playerId = player.getUUID();
        long now = server.getTickCount();

        long cooldownTicks = getWatcherCooldownTicks(state.getPhase(), profile, danger);
        Long lastWatcherTick = state.getLastWatcherTick(playerId);
        if (isCooldownActive(lastWatcherTick, now, cooldownTicks)) {
            return;
        }

        long globalCooldownTicks = UncannyParanoiaEventSystem.getEffectiveGlobalCooldownTicks(state.getPhase());
        if (isCooldownActive(state.getLastGlobalEventTick(), now, globalCooldownTicks)) {
            return;
        }

        if (hasActiveWatcher(player, playerId)) {
            return;
        }

        if (level.random.nextDouble() > getWatcherTriggerChance(state.getPhase(), profile, danger)) {
            return;
        }

        if (spawnWatcherFor(player)) {
            state.setLastWatcherTick(playerId, now);
            state.setLastGlobalEventTick(now);
        }
    }

    public static boolean forceSpawnWatcher(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        if (shouldBlockWatcherSpawn(player)) {
            debugLog("WATCHER force spawn blocked water-or-boat player={}", player.getGameProfile().getName());
            return false;
        }

        if (!UncannyWorldState.get(server).isSystemEnabled()) {
            return false;
        }

        boolean spawned = spawnWatcherFor(player);
        if (spawned) {
            UncannyWorldState state = UncannyWorldState.get(server);
            long now = server.getTickCount();
            state.setLastWatcherTick(player.getUUID(), now);
            state.setLastGlobalEventTick(now);
        }
        return spawned;
    }

    public static boolean spawnWatcherFromEvents(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            debugLog("WATCHER spawn failed: server null");
            return false;
        }
        if (shouldBlockWatcherSpawn(player)) {
            debugLog("WATCHER spawn blocked water-or-boat player={}", player.getGameProfile().getName());
            return false;
        }

        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD || !isNightOrTwilight(level) || !level.canSeeSky(player.blockPosition())) {
            debugLog("WATCHER spawn blocked by dimension/time/sky player={} dim={} nightOrTwilight={} canSeeSky={}",
                    player.getGameProfile().getName(),
                    level.dimension().location(),
                    isNightOrTwilight(level),
                    level.canSeeSky(player.blockPosition()));
            return false;
        }

        UncannyWorldState state = UncannyWorldState.get(server);
        if (!state.isSystemEnabled()) {
            debugLog("WATCHER spawn blocked: system disabled player={}", player.getGameProfile().getName());
            return false;
        }
        if (state.getPhase().index() < UncannyPhase.PHASE_2.index()) {
            debugLog("WATCHER spawn blocked: low phase={} player={}", state.getPhase().index(), player.getGameProfile().getName());
            return false;
        }

        long now = server.getTickCount();
        int profile = getIntensityProfile();
        int danger = getDangerLevel();
        long watcherCooldownTicks = getWatcherCooldownTicks(state.getPhase(), profile, danger);
        if (isCooldownActive(state.getLastWatcherTick(player.getUUID()), now, watcherCooldownTicks)) {
            debugLog("WATCHER spawn blocked: cooldown player={} remaining={}t",
                    player.getGameProfile().getName(),
                    watcherCooldownTicks - Math.max(0L, now - state.getLastWatcherTick(player.getUUID())));
            return false;
        }

        if (hasActiveWatcher(player, player.getUUID())) {
            debugLog("WATCHER spawn blocked: active watcher already present player={}", player.getGameProfile().getName());
            return false;
        }

        boolean spawned = spawnWatcherFor(player);
        if (spawned) {
            state.setLastWatcherTick(player.getUUID(), now);
            debugLog("WATCHER spawn success player={} at~{}", player.getGameProfile().getName(), player.blockPosition());
        } else {
            debugLog("WATCHER spawn failed: no valid position/entity player={}", player.getGameProfile().getName());
        }
        return spawned;
    }

    private static boolean spawnWatcherFor(ServerPlayer player) {
        if (shouldBlockWatcherSpawn(player)) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        BlockPos spawnPos = findWatcherSpawnPos(level, player);
        if (spawnPos == null) {
            spawnPos = findWatcherFallbackPos(level, player);
            debugLog("WATCHER spawn position fallback used player={} fallbackPos={}", player.getGameProfile().getName(), spawnPos);
        }
        if (spawnPos == null) {
            debugLog("WATCHER spawn aborted player={} reason=no-valid-out-of-view-position", player.getGameProfile().getName());
            return false;
        }

        UncannyWatcherEntity watcher = UncannyEntityRegistry.UNCANNY_WATCHER.get().create(level);
        if (watcher == null) {
            debugLog("WATCHER entity create null player={}", player.getGameProfile().getName());
            return false;
        }

        watcher.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
        watcher.setWatchedPlayer(player);
        level.addFreshEntity(watcher);
        return true;
    }

    private static BlockPos findWatcherSpawnPos(ServerLevel level, ServerPlayer player) {
        int configuredMinDistance = UncannyConfig.WATCHER_MIN_DISTANCE.get();
        int configuredMaxDistance = Math.max(configuredMinDistance, UncannyConfig.WATCHER_MAX_DISTANCE.get());
        int runtimeMaxDistance = resolveRuntimeMaxSpawnDistance(player, configuredMaxDistance);

        int maxDistance = Math.max(32, Math.min(configuredMaxDistance, runtimeMaxDistance));
        int minDistance = Math.max(28, Math.min(configuredMinDistance, maxDistance - 6));
        minDistance = Math.max(minDistance, (int) Math.floor(maxDistance * 0.72D));
        if (minDistance >= maxDistance - 2) {
            minDistance = Math.max(24, maxDistance - 4);
        }

        int baseY = (int) Math.floor(player.getY());
        for (int attempt = 0; attempt < 52; attempt++) {
            double angle = level.random.nextDouble() * (Math.PI * 2.0D);
            Vec3 direction = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            double distance = minDistance + level.random.nextDouble() * (maxDistance - minDistance);

            int x = (int) Math.floor(player.getX() + direction.x * distance);
            int z = (int) Math.floor(player.getZ() + direction.z * distance);
            BlockPos pos = findWatcherSpawnAtOrAbovePlayerY(level, x, z, baseY);
            if (pos != null && level.canSeeSky(pos) && isOutsidePlayerView(level, player, pos)) {
                return pos;
            }
        }
        return null;
    }

    private static BlockPos findWatcherFallbackPos(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 backward = new Vec3(-look.x, 0.0D, -look.z);
        if (backward.lengthSqr() < 0.0001D) {
            backward = new Vec3(-1.0D, 0.0D, 0.0D);
        } else {
            backward = backward.normalize();
        }

        int baseY = (int) Math.floor(player.getY());
        int[] fallbackDistances = {64, 58, 52, 46, 40, 34};
        for (int distance : fallbackDistances) {
            for (int side = -1; side <= 1; side++) {
                float yaw = side * (25.0F + level.random.nextFloat() * 20.0F);
                Vec3 direction = backward.yRot(yaw * ((float) Math.PI / 180.0F)).normalize();
                int x = (int) Math.floor(player.getX() + direction.x * distance);
                int z = (int) Math.floor(player.getZ() + direction.z * distance);
                BlockPos pos = findWatcherSpawnAtOrAbovePlayerY(level, x, z, baseY);
                if (pos != null && level.canSeeSky(pos) && isOutsidePlayerView(level, player, pos)) {
                    return pos;
                }
            }
        }
        return null;
    }

    private static BlockPos findWatcherSpawnAtOrAbovePlayerY(ServerLevel level, int x, int z, int baseY) {
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - 2;
        int startY = Math.max(minY, Math.min(maxY, baseY));

        BlockPos candidate = new BlockPos(x, startY, z);
        if (isWatcherStandable(level, candidate)) {
            return candidate.immutable();
        }

        int upperLimit = Math.min(maxY, startY + 28);
        for (int y = startY + 1; y <= upperLimit; y++) {
            candidate = new BlockPos(x, y, z);
            if (isWatcherStandable(level, candidate)) {
                return candidate.immutable();
            }
        }

        int surfaceY = Math.max(minY, Math.min(maxY, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)));
        if (surfaceY >= startY) {
            candidate = new BlockPos(x, surfaceY, z);
            if (isWatcherStandable(level, candidate)) {
                return candidate.immutable();
            }
        }
        return null;
    }

    private static boolean isWatcherStandable(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }
        return level.getBlockState(pos.below()).isSolidRender(level, pos.below());
    }

    private static boolean hasClearSightToPlayer(ServerLevel level, ServerPlayer player, BlockPos watcherPos) {
        Vec3 from = Vec3.atCenterOf(watcherPos).add(0.0D, 1.6D, 0.0D);
        Vec3 to = player.getEyePosition();
        HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS;
    }

    private static boolean isOutsidePlayerView(ServerLevel level, ServerPlayer player, BlockPos watcherPos) {
        Vec3 eye = player.getEyePosition();
        Vec3 toWatcher = Vec3.atCenterOf(watcherPos).add(0.0D, 1.6D, 0.0D).subtract(eye);
        if (toWatcher.lengthSqr() < 0.0001D) {
            return false;
        }

        Vec3 look = player.getViewVector(1.0F).normalize();
        double dot = look.dot(toWatcher.normalize());

        // Strongly behind the player is always valid.
        if (dot <= -0.10D) {
            return true;
        }

        // If there is no direct line from player eyes to spawn point, it is considered out of vision.
        Vec3 to = Vec3.atCenterOf(watcherPos).add(0.0D, 1.6D, 0.0D);
        HitResult hit = level.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() != HitResult.Type.MISS;
    }

    private static int resolveRuntimeMaxSpawnDistance(ServerPlayer player, int fallbackMaxDistance) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return fallbackMaxDistance;
        }

        int viewDistanceChunks = Math.max(2, server.getPlayerList().getViewDistance());
        int simulationDistanceChunks = Math.max(2, server.getPlayerList().getSimulationDistance());
        int effectiveChunks = Math.max(2, Math.min(viewDistanceChunks, simulationDistanceChunks));

        int chunkLimitedMax = Math.max(32, effectiveChunks * 16 - 20);
        return Math.max(32, Math.min(224, chunkLimitedMax));
    }

    private static boolean hasActiveWatcher(ServerPlayer player, UUID playerId) {
        return !player.serverLevel().getEntitiesOfClass(
                UncannyWatcherEntity.class,
                player.getBoundingBox().inflate(256.0D),
                watcher -> watcher.isAlive() && watcher.getWatchedPlayerUuid().map(playerId::equals).orElse(false)).isEmpty();
    }

    private static boolean isNightOrTwilight(ServerLevel level) {
        long dayTime = level.getDayTime() % 24000L;
        return dayTime >= 12000L || dayTime <= 1300L;
    }

    private static int getIntensityProfile() {
        return Math.max(1, Math.min(5, UncannyConfig.EVENT_INTENSITY_PROFILE.get()));
    }

    private static double getWatcherTriggerChance(UncannyPhase phase, int profile, int danger) {
        double baseChance;
        if (phase == UncannyPhase.PHASE_1) {
            baseChance = 0.0D;
        } else if (phase == UncannyPhase.PHASE_2) {
            baseChance = 0.04D;
        } else if (phase == UncannyPhase.PHASE_3) {
            baseChance = 0.08D;
        } else {
            baseChance = 0.13D;
        }
        double configured = Math.max(0.0D, Math.min(1.0D, UncannyConfig.WATCHER_TRIGGER_CHANCE.get()));
        double phaseAdjusted = Math.max(configured, baseChance);
        return Math.max(0.0D, Math.min(0.45D, phaseAdjusted * PROFILE_WATCHER_CHANCE_MULTIPLIER[profile - 1] * DANGER_WATCHER_CHANCE_MULTIPLIER[danger]));
    }

    private static long getWatcherCooldownTicks(UncannyPhase phase, int profile, int danger) {
        int configuredSeconds = Math.max(10, UncannyConfig.WATCHER_COOLDOWN_SECONDS.get());
        int profileSeconds = PROFILE_WATCHER_COOLDOWN_SECONDS[profile - 1];
        int chosenSeconds = Math.min(configuredSeconds, profileSeconds);

        double phaseMultiplier;
        if (phase == UncannyPhase.PHASE_1) {
            phaseMultiplier = 1.25D;
        } else if (phase == UncannyPhase.PHASE_2) {
            phaseMultiplier = 1.10D;
        } else if (phase == UncannyPhase.PHASE_3) {
            phaseMultiplier = 1.00D;
        } else {
            phaseMultiplier = 0.75D;
        }

        int finalSeconds = Math.max(20, (int) Math.round(chosenSeconds * phaseMultiplier * DANGER_WATCHER_COOLDOWN_MULTIPLIER[danger]));
        return finalSeconds * 20L;
    }

    private static int getDangerLevel() {
        return Math.max(0, Math.min(5, UncannyConfig.EVENT_DANGER_LEVEL.get()));
    }

    private static boolean isCooldownActive(Long lastTick, long now, long cooldownTicks) {
        return cooldownTicks > 0 && lastTick != null && now >= lastTick && (now - lastTick) < cooldownTicks;
    }

    private static boolean isCooldownActive(long lastTick, long now, long cooldownTicks) {
        return cooldownTicks > 0 && lastTick != Long.MIN_VALUE && now >= lastTick && (now - lastTick) < cooldownTicks;
    }

    private static boolean shouldBlockWatcherSpawn(ServerPlayer player) {
        return player.isInWaterOrBubble() || player.getVehicle() instanceof Boat;
    }
}
