package com.eotv.echoofthevoid.event.special;

import com.eotv.echoofthevoid.diagnostics.DiagnosticSeverity;
import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyDevourerEntity;
import com.eotv.echoofthevoid.world.UncannyDimensions;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Loaded-chunk-only spawn routes for Devourer?. */
public final class UncannyDevourerSystem {
    private UncannyDevourerSystem() {
    }

    public static boolean spawnNatural(ServerPlayer target) {
        return spawn(target, false, "natural_scheduler");
    }

    public static boolean spawnDebug(ServerPlayer target) {
        return spawn(target, true, "developer_spawn");
    }

    private static boolean spawn(ServerPlayer target, boolean debug, String route) {
        String preconditionFailure = preconditionFailure(target);
        if (preconditionFailure != null) {
            traceFailure(target, preconditionFailure, route);
            return false;
        }
        if (!debug && UncannyWorldState.get(target.getServer()).isDevourerGlobalCooldownActive()) {
            traceFailure(target, "global_cooldown", route);
            return false;
        }
        ServerLevel level = target.serverLevel();
        UncannyDevourerEntity devourer = UncannyEntityRegistry.UNCANNY_DEVOURER.get().create(level);
        if (devourer == null) {
            traceFailure(target, "entity_creation_failed", route);
            return false;
        }
        Vec3 position = findReachableSpawn(level, target, devourer, debug);
        if (position == null) {
            traceFailure(target, "no_safe_reachable_position", route);
            return false;
        }
        devourer.moveTo(position.x, position.y, position.z, faceYaw(position, target.position()), 0.0F);
        devourer.initializeFor(target);
        if (debug) {
            devourer.addTag("eotv_dev_spawned");
        }
        boolean added = level.addFreshEntity(devourer);
        UncannyDiagnostics.specialSpawnResult(target, devourer, added, route);
        return added;
    }

    private static Vec3 findReachableSpawn(
            ServerLevel level,
            ServerPlayer target,
            UncannyDevourerEntity devourer,
            boolean debug) {
        Set<Long> checkedColumns = new HashSet<>();

        if (debug) {
            // The QA route must remain practical around bases and uneven terrain. It still checks
            // collision, support and a real navigation path, but may use a nearer 4-13 block ring.
            double angleOffset = Math.toRadians(target.getYRot() + 180.0F);
            for (int distance = 4; distance <= 13; distance++) {
                for (int sector = 0; sector < 24; sector++) {
                    double angle = angleOffset + sector * (Math.PI * 2.0D / 24.0D);
                    int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
                    int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
                    Vec3 reachable = findReachableInColumn(
                            level, target, devourer, x, z, checkedColumns);
                    if (reachable != null) {
                        return reachable;
                    }
                }
            }
        }

        // Natural attempts retain the delivered 14-20 block staging distance. A rejected path no
        // longer aborts the whole event: every sampled safe column gets its own reachability check.
        for (int attempt = 0; attempt < 96; attempt++) {
            double angle = target.getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = 14.0D + target.getRandom().nextDouble() * 6.0D;
            int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
            Vec3 reachable = findReachableInColumn(level, target, devourer, x, z, checkedColumns);
            if (reachable != null) {
                return reachable;
            }
        }

        return null;
    }

    private static Vec3 findReachableInColumn(
            ServerLevel level,
            ServerPlayer target,
            UncannyDevourerEntity devourer,
            int x,
            int z,
            Set<Long> checkedColumns) {
        long columnKey = BlockPos.asLong(x, 0, z);
        if (!checkedColumns.add(columnKey)) {
            return null;
        }
        for (int dy = 5; dy >= -8; dy--) {
            BlockPos feet = new BlockPos(x, target.blockPosition().getY() + dy, z);
            if (!isSafeSpawnVolume(level, feet)) {
                continue;
            }
            Vec3 position = Vec3.atBottomCenterOf(feet);
            devourer.moveTo(position.x, position.y, position.z, 0.0F, 0.0F);
            // GroundPathNavigation deliberately refuses to build a path while a newly created
            // mob reports neither on-ground, swimming nor riding. The support check above proves
            // this candidate is grounded, but no entity tick has run yet to establish that state.
            devourer.setOnGround(true);
            devourer.getNavigation().stop();
            // A 1.2-block-wide mob cannot occupy the player's exact path node. Requiring zero
            // reach therefore rejects even a completely open floor. One node of reach still
            // requires a complete melee route while allowing the final continuous contact step.
            var path = devourer.getNavigation().createPath(target, 1);
            if (path != null && path.canReach()) {
                return position;
            }
        }
        return null;
    }

    private static boolean isSafeSpawnVolume(ServerLevel level, BlockPos feet) {
        if (!level.hasChunkAt(feet)
                || !level.getWorldBorder().isWithinBounds(feet)
                || !level.getFluidState(feet).isEmpty()) {
            return false;
        }
        double centerX = feet.getX() + 0.5D;
        double centerZ = feet.getZ() + 0.5D;
        AABB bounds = new AABB(
                centerX - 0.60D, feet.getY(), centerZ - 0.60D,
                centerX + 0.60D, feet.getY() + 2.70D, centerZ + 0.60D);
        BlockPos support = feet.below();
        return level.noCollision(bounds)
                && level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)
                && level.getFluidState(support).isEmpty();
    }

    private static String preconditionFailure(ServerPlayer target) {
        if (target == null || target.getServer() == null) {
            return "missing_server_context";
        }
        if (!target.isAlive() || target.isSpectator()) {
            return "target_not_eligible";
        }
        if (target.getServer().getLevel(UncannyDimensions.ELSEWHERE) == null) {
            return "elsewhere_unavailable";
        }
        if (!isVanillaDimension(target.serverLevel()) || UncannyDimensions.isElsewhere(target.level())) {
            return "dimension_not_allowed";
        }
        if (DevourerArenaSystem.activeSessionCount(target.getServer())
                >= DevourerArenaRules.MAX_SIMULTANEOUS_SESSIONS) {
            return "arena_capacity_reached";
        }
        return null;
    }

    private static boolean isVanillaDimension(ServerLevel level) {
        return level.dimension() == Level.OVERWORLD
                || level.dimension() == Level.NETHER
                || level.dimension() == Level.END;
    }

    private static float faceYaw(Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        return (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0D);
    }

    private static void traceFailure(ServerPlayer target, String reason, String route) {
        if (target == null) {
            return;
        }
        UncannyDiagnostics.recordForPlayer(
                target,
                DiagnosticSeverity.INFO,
                "special",
                "devourer_spawn_refused",
                UncannyDiagnostics.fields("reason", reason, "route", route));
    }
}
