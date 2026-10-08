package com.eotv.echoofthevoid.event.special;

import com.eotv.echoofthevoid.diagnostics.DiagnosticSeverity;
import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyMinerEntity;
import com.eotv.echoofthevoid.event.UncannyParanoiaEventSystem;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Natural, command and developer entry points for Miner?. */
public final class UncannyMinerSystem {
    private static final double ACTIVE_SEARCH_RADIUS = MinerRules.MAX_TARGET_DISTANCE + 8.0D;

    private UncannyMinerSystem() {
    }

    public static boolean spawnNatural(ServerPlayer target) {
        return spawnTunnel(target, false, "natural_scheduler");
    }

    public static boolean spawnDebugTunnel(ServerPlayer target) {
        prepareDebugRetest(target);
        return spawnTunnel(target, true, "developer_tunnel");
    }

    public static boolean spawnDebugEmerged(ServerPlayer target) {
        prepareDebugRetest(target);
        if (!canSpawnFor(target, true)) {
            return false;
        }
        ServerLevel level = target.serverLevel();
        Vec3 spawn = findSafeEmergedPosition(target);
        if (spawn == null) {
            traceFailure(target, "no_safe_emerged_position", "developer_emerged");
            return false;
        }
        UncannyMinerEntity miner = UncannyEntityRegistry.UNCANNY_MINER.get().create(level);
        if (miner == null) {
            traceFailure(target, "entity_creation_failed", "developer_emerged");
            return false;
        }
        miner.moveTo(spawn.x, spawn.y, spawn.z, target.getYRot() + 180.0F, 0.0F);
        miner.initializeEmerged(target);
        miner.addTag("eotv_dev_spawned");
        boolean added = level.addFreshEntity(miner);
        UncannyDiagnostics.specialSpawnResult(target, miner, added, "developer_emerged");
        return added;
    }

    public static boolean hasActiveForTarget(ServerPlayer target) {
        if (target == null) {
            return false;
        }
        AABB bounds = target.getBoundingBox().inflate(ACTIVE_SEARCH_RADIUS);
        return !target.serverLevel().getEntitiesOfClass(
                        UncannyMinerEntity.class,
                        bounds,
                        miner -> miner.isAlive() && miner.targets(target))
                .isEmpty();
    }

    public static int abortForTarget(ServerPlayer target) {
        if (target == null) {
            return 0;
        }
        ServerLevel level = target.serverLevel();
        List<UncannyMinerEntity> miners = level.getEntitiesOfClass(
                UncannyMinerEntity.class,
                target.getBoundingBox().inflate(96.0D),
                miner -> miner.targets(target));
        int restoredNow = 0;
        for (UncannyMinerEntity miner : miners) {
            miner.discard();
            restoredNow += miner.restoreTunnelNowForDebug(level);
        }
        if (!miners.isEmpty()) {
            UncannyDiagnostics.recordForPlayer(
                    target,
                    DiagnosticSeverity.INFO,
                    "special",
                    "miner_debug_active_attempt_removed",
                    UncannyDiagnostics.fields(
                            "miners_removed", miners.size(),
                            "blocks_restored_immediately", restoredNow));
        }
        return miners.size();
    }

    private static void prepareDebugRetest(ServerPlayer target) {
        if (target == null) {
            return;
        }
        int cleared = abortForTarget(target);
        boolean ghostMinerCleared = UncannyParanoiaEventSystem.hasActiveGhostMiner(target.getUUID());
        UncannyParanoiaEventSystem.cancelGhostMiner(target.getUUID());
        if (cleared > 0 || ghostMinerCleared) {
            UncannyDiagnostics.recordForPlayer(
                    target,
                    DiagnosticSeverity.INFO,
                    "special",
                    "miner_debug_retest_cleanup",
                    UncannyDiagnostics.fields(
                            "miners_removed", cleared,
                            "ghost_miner_removed", ghostMinerCleared));
        }
    }

    private static boolean spawnTunnel(ServerPlayer target, boolean debug, String route) {
        if (!canSpawnFor(target, debug)) {
            return false;
        }
        ServerLevel level = target.serverLevel();
        MinerTunnelPlanner.TunnelPlan plan = MinerTunnelPlanner.findInitial(
                level, target, target.getRandom(), debug);
        if (plan == null) {
            traceFailure(target, "no_safe_tunnel", route);
            return false;
        }
        UncannyMinerEntity miner = UncannyEntityRegistry.UNCANNY_MINER.get().create(level);
        if (miner == null || !miner.initializeTunnel(target, plan, debug)) {
            traceFailure(target, "entity_initialization_failed", route);
            return false;
        }
        if (debug) {
            miner.addTag("eotv_dev_spawned");
        }
        boolean added = level.addFreshEntity(miner);
        UncannyDiagnostics.specialSpawnResult(target, miner, added, route);
        if (added) {
            // Mutual exclusion is established atomically only after the real entity exists.
            UncannyParanoiaEventSystem.cancelGhostMiner(target.getUUID());
        }
        return added;
    }

    private static boolean canSpawnFor(ServerPlayer target, boolean debug) {
        if (target == null
                || target.getServer() == null
                || !target.isAlive()
                || target.isSpectator()) {
            return false;
        }
        if (!UncannyWorldState.get(target.getServer()).isSystemEnabled()) {
            return false;
        }
        if (target.level().dimension() != Level.OVERWORLD
                && target.level().dimension() != Level.NETHER) {
            traceFailure(target, "dimension_not_allowed", debug ? "developer_tunnel" : "natural_scheduler");
            return false;
        }
        if (!target.serverLevel().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            traceFailure(target, "mob_griefing_disabled", debug ? "developer_tunnel" : "natural_scheduler");
            return false;
        }
        if (hasActiveForTarget(target)
                || UncannyParanoiaEventSystem.hasActiveGhostMiner(target.getUUID())) {
            traceFailure(target, "miner_conflict", debug ? "developer_tunnel" : "natural_scheduler");
            return false;
        }
        return true;
    }

    private static Vec3 findSafeEmergedPosition(ServerPlayer target) {
        ServerLevel level = target.serverLevel();
        for (int distance = 4; distance <= 8; distance++) {
            for (int quarter = 0; quarter < 4; quarter++) {
                double angle = quarter * (Math.PI / 2.0D) + target.getRandom().nextDouble() * 0.35D;
                int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
                int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
                for (int dy = 3; dy >= -4; dy--) {
                    var feet = new net.minecraft.core.BlockPos(x, target.blockPosition().getY() + dy, z);
                    double centerX = feet.getX() + 0.5D;
                    double centerZ = feet.getZ() + 0.5D;
                    AABB entityBounds = new AABB(
                            centerX - 0.30D, feet.getY(), centerZ - 0.30D,
                            centerX + 0.30D, feet.getY() + 1.95D, centerZ + 0.30D);
                    if (!level.hasChunkAt(feet) || !level.noCollision(entityBounds)) {
                        continue;
                    }
                    var below = feet.below();
                    if (level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
                        return Vec3.atBottomCenterOf(feet);
                    }
                }
            }
        }
        return null;
    }

    private static void traceFailure(ServerPlayer target, String reason, String route) {
        UncannyDiagnostics.recordForPlayer(
                target,
                DiagnosticSeverity.INFO,
                "special",
                "miner_spawn_refused",
                UncannyDiagnostics.fields("reason", reason, "spawn_route", route));
    }
}
