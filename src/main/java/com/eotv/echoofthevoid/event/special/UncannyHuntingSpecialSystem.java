package com.eotv.echoofthevoid.event.special;

import com.eotv.echoofthevoid.diagnostics.DiagnosticSeverity;
import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.AbstractUncannyHuntingSpecialEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyAshwalkerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyDredgerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyDrifterEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyEchoerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyFlankerEntity;
import com.eotv.echoofthevoid.event.paranoia.UncannyDimensionPolicy;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Loaded-chunk-only context finding and transactional spawning for the five hunting Specials. */
public final class UncannyHuntingSpecialSystem {
    private static final Direction[] HORIZONTAL_DIRECTIONS = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private UncannyHuntingSpecialSystem() {
    }

    public static boolean spawnNatural(ServerPlayer player, String id) {
        return spawn(player, id, false, "normal");
    }

    public static boolean spawnForDebug(ServerPlayer player, String id, String state) {
        return spawn(player, id, true, state == null ? "normal" : state);
    }

    public static boolean spawn(ServerPlayer player, String id, boolean debug, String requestedState) {
        if (player == null || !player.isAlive() || player.isSpectator() || player.getServer() == null) {
            return false;
        }
        String normalized = id == null ? "" : id.trim().toLowerCase(java.util.Locale.ROOT);
        ServerLevel level = player.serverLevel();
        if (!debug && !UncannyDimensionPolicy.allowsNaturalSpecial(level, normalized)) {
            return false;
        }
        if (hasActiveForTarget(level, player, normalized)) {
            traceSpawnFailure(player, normalized, "already_active_for_target");
            return false;
        }
        return switch (normalized) {
            case "echoer" -> spawnEchoer(level, player, debug, requestedState);
            case "drifter" -> spawnDrifter(level, player, debug, requestedState);
            case "ashwalker" -> spawnAshwalker(level, player, debug, requestedState);
            case "dredger" -> spawnDredger(level, player, debug, requestedState);
            case "flanker" -> spawnFlankerPair(level, player, debug);
            default -> false;
        };
    }

    private static boolean spawnEchoer(
            ServerLevel level,
            ServerPlayer player,
            boolean debug,
            String requestedState) {
        if (!debug && HuntingSpecialSoundMemory.newestBefore(
                level, player.position(), Long.MAX_VALUE, 42.0D) == null) {
            traceSpawnFailure(player, "echoer", "no_safe_sound_memory");
            return false;
        }
        Vec3 position = findGroundRing(level, player.position(), 14, 18, 24);
        if (position == null) {
            traceSpawnFailure(player, "echoer", "no_loaded_ground_position");
            return false;
        }
        UncannyEchoerEntity entity = UncannyEntityRegistry.UNCANNY_ECHOER.get().create(level);
        if (entity == null) {
            return false;
        }
        entity.moveTo(position.x, position.y, position.z, player.getYRot(), 0.0F);
        entity.setupTarget(player, 20 * 300);
        if ("aggro".equalsIgnoreCase(requestedState)) {
            entity.forceAggroForDebug();
        }
        return add(level, player, entity, debug, "echoer");
    }

    private static boolean spawnDrifter(
            ServerLevel level,
            ServerPlayer player,
            boolean debug,
            String requestedState) {
        boolean dryQa = debug && "dry".equalsIgnoreCase(requestedState);
        if (!debug && !player.isInWaterOrBubble()) {
            traceSpawnFailure(player, "drifter", "target_not_in_deep_water");
            return false;
        }
        Vec3 position = dryQa
                ? findGroundRing(level, player.position(), 8, 12, 18)
                : findDeepWaterPosition(level, player, 14, 24, false);
        if (position == null) {
            traceSpawnFailure(player, "drifter", dryQa ? "no_dry_qa_ground" : "water_too_shallow");
            return false;
        }
        UncannyDrifterEntity entity = UncannyEntityRegistry.UNCANNY_DRIFTER.get().create(level);
        if (entity == null) {
            return false;
        }
        entity.moveTo(position.x, position.y, position.z, player.getYRot(), 0.0F);
        int lifetime = HuntingSpecialRules.DRIFTER_MIN_LIFETIME_TICKS
                + level.random.nextInt(HuntingSpecialRules.DRIFTER_MAX_LIFETIME_TICKS
                        - HuntingSpecialRules.DRIFTER_MIN_LIFETIME_TICKS + 1);
        entity.setupTarget(player, lifetime);
        if (dryQa) {
            entity.forceDryForDebug();
        }
        return add(level, player, entity, debug, "drifter");
    }

    private static boolean spawnAshwalker(
            ServerLevel level,
            ServerPlayer player,
            boolean debug,
            String requestedState) {
        Vec3 position = findLavaSurfacePosition(level, player, 14, 24);
        if (position == null) {
            traceSpawnFailure(player, "ashwalker", "no_connected_loaded_lava_surface");
            return false;
        }
        UncannyAshwalkerEntity entity = UncannyEntityRegistry.UNCANNY_ASHWALKER.get().create(level);
        if (entity == null) {
            return false;
        }
        entity.moveTo(position.x, position.y, position.z, player.getYRot(), 0.0F);
        entity.setupTarget(player, 20 * 240);
        if ("submerged".equalsIgnoreCase(requestedState)) {
            entity.forceSubmergedForDebug();
        }
        return add(level, player, entity, debug, "ashwalker");
    }

    private static boolean spawnDredger(
            ServerLevel level,
            ServerPlayer player,
            boolean debug,
            String requestedState) {
        if (!debug && !player.isInWaterOrBubble()) {
            traceSpawnFailure(player, "dredger", "target_not_in_deep_water");
            return false;
        }
        Vec3 position = findDeepWaterPosition(level, player, 16, 28, true);
        if (position == null) {
            traceSpawnFailure(player, "dredger", "no_loaded_deep_ocean_floor");
            return false;
        }
        UncannyDredgerEntity entity = UncannyEntityRegistry.UNCANNY_DREDGER.get().create(level);
        if (entity == null) {
            return false;
        }
        entity.moveTo(position.x, position.y, position.z, player.getYRot(), 0.0F);
        entity.setupTarget(player, 20 * 300);
        if ("grab".equalsIgnoreCase(requestedState)) {
            Vec3 grabPosition = findDredgerGrabPosition(level, player, entity);
            if (grabPosition == null) {
                traceSpawnFailure(player, "dredger", "qa_grab_requires_clear_deep_water_near_player");
                return false;
            }
            entity.moveTo(grabPosition.x, grabPosition.y, grabPosition.z, player.getYRot(), 0.0F);
            if (!entity.forceGrabForDebug(level, player)) {
                traceSpawnFailure(player, "dredger", "qa_grab_initialization_rejected");
                return false;
            }
        }
        return add(level, player, entity, debug, "dredger");
    }

    /**
     * The QA grab must exercise the production grapple, not ask a Dredger 16-28 blocks away to
     * create a physically impossible tether. Probe only loaded water beside the player and keep
     * the same collision and water-route rules used by the live state machine.
     */
    private static Vec3 findDredgerGrabPosition(
            ServerLevel level,
            ServerPlayer player,
            UncannyDredgerEntity entity) {
        BlockPos origin = player.blockPosition();
        for (int radius = 3; radius <= 5; radius++) {
            for (Direction direction : HORIZONTAL_DIRECTIONS) {
                for (int dy = -2; dy <= 1; dy++) {
                    BlockPos candidate = origin.relative(direction, radius).offset(0, dy, 0);
                    if (!level.hasChunkAt(candidate)
                            || !level.getFluidState(candidate).is(FluidTags.WATER)
                            || !level.getFluidState(candidate.above()).is(FluidTags.WATER)) {
                        continue;
                    }
                    Vec3 point = Vec3.atBottomCenterOf(candidate);
                    if (!isWaterRouteClear(level, player.position(), point)) {
                        continue;
                    }
                    entity.moveTo(point.x, point.y, point.z, player.getYRot(), 0.0F);
                    if (level.noCollision(entity)
                            && findSeabedPullAnchor(level, candidate, 10) != null) {
                        return point;
                    }
                }
            }
        }
        return null;
    }

    private static boolean spawnFlankerPair(
            ServerLevel level,
            ServerPlayer player,
            boolean debug) {
        if (!HuntingSpecialRules.isFlankerTerrainDryEnough(
                countFlankerWaterSurface(level, player, true), countFlankerWaterSurface(level, player, false))) {
            // Rivers and coasts cut the two-sided rings; such encounters only ended in a sink.
            traceSpawnFailure(player, "flanker", "water_cuts_encirclement_area");
            return false;
        }
        FlankerSpawnPair spawnPair = findOpposedFlankerPositions(
                level, player, debug ? 9 : 8, 11);
        if (spawnPair == null) {
            traceSpawnFailure(player, "flanker", "opposed_loaded_positions_unavailable");
            return false;
        }

        UncannyFlankerEntity first = UncannyEntityRegistry.UNCANNY_FLANKER.get().create(level);
        UncannyFlankerEntity second = UncannyEntityRegistry.UNCANNY_FLANKER.get().create(level);
        if (first == null || second == null) {
            return false;
        }
        first.moveTo(spawnPair.first().x, spawnPair.first().y, spawnPair.first().z, player.getYRot(), 0.0F);
        second.moveTo(spawnPair.second().x, spawnPair.second().y, spawnPair.second().z, player.getYRot(), 0.0F);
        first.setOnGround(true);
        second.setOnGround(true);
        if (!level.noCollision(first) || !level.noCollision(second)) {
            traceSpawnFailure(player, "flanker", "one_of_two_positions_collides");
            return false;
        }
        UUID pairId = UUID.randomUUID();
        AdaptiveSpecialCombatProfile profile = AdaptiveSpecialCombatProfile.attacker(player);
        first.setupPair(player, pairId, second.getUUID(), 0, profile);
        second.setupPair(player, pairId, first.getUUID(), 1, profile);
        if (debug) {
            first.addTag("eotv_dev_spawned");
            second.addTag("eotv_dev_spawned");
        }
        if (!level.addFreshEntity(first)) {
            traceSpawnFailure(player, "flanker", "first_add_failed");
            return false;
        }
        if (!level.addFreshEntity(second)) {
            first.discard();
            traceSpawnFailure(player, "flanker", "second_add_failed_first_removed");
            return false;
        }
        // Navigation cannot reliably build a path for an entity that has not entered the level.
        // Add both members provisionally, then keep or roll back the complete transaction.
        if (!positionFlankerPairOnReachableOpposites(
                level,
                player,
                first,
                second,
                debug ? 9 : 8,
                11)) {
            first.discard();
            second.discard();
            traceSpawnFailure(player, "flanker", "one_of_two_paths_unreachable_pair_removed");
            return false;
        }
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        if (!state.initializeFlankerPair(pairId, first.getUUID(), second.getUUID())) {
            first.discard();
            second.discard();
            traceSpawnFailure(player, "flanker", "pair_persistence_initialization_failed");
            return false;
        }
        UncannyDiagnostics.specialSpawnResult(player, first, true, debug ? "dev_flanker_pair" : "flanker_pair");
        UncannyDiagnostics.specialSpawnResult(player, second, true, debug ? "dev_flanker_pair" : "flanker_pair");
        return true;
    }

    private static boolean add(
            ServerLevel level,
            ServerPlayer player,
            Mob entity,
            boolean debug,
            String id) {
        if (debug) {
            entity.addTag("eotv_dev_spawned");
        }
        boolean added = level.addFreshEntity(entity);
        UncannyDiagnostics.specialSpawnResult(
                player, entity, added, debug ? "dev_hunting_special:" + id : "hunting_special:" + id);
        return added;
    }

    private static boolean hasActiveForTarget(ServerLevel level, ServerPlayer player, String id) {
        AABB bounds = player.getBoundingBox().inflate(96.0D);
        return switch (id) {
            case "echoer" -> !level.getEntitiesOfClass(UncannyEchoerEntity.class, bounds,
                    entity -> entity.isAlive() && entity.focusPlayerId().filter(player.getUUID()::equals).isPresent()).isEmpty();
            case "drifter" -> !level.getEntitiesOfClass(UncannyDrifterEntity.class, bounds,
                    entity -> entity.isAlive() && entity.focusPlayerId().filter(player.getUUID()::equals).isPresent()).isEmpty();
            case "ashwalker" -> !level.getEntitiesOfClass(UncannyAshwalkerEntity.class, bounds,
                    entity -> entity.isAlive() && entity.focusPlayerId().filter(player.getUUID()::equals).isPresent()).isEmpty();
            case "dredger" -> !level.getEntitiesOfClass(UncannyDredgerEntity.class, bounds,
                    entity -> entity.isAlive() && entity.focusPlayerId().filter(player.getUUID()::equals).isPresent()).isEmpty();
            case "flanker" -> !level.getEntitiesOfClass(UncannyFlankerEntity.class, bounds,
                    entity -> entity.isAlive() && entity.focusPlayerId().filter(player.getUUID()::equals).isPresent()).isEmpty();
            default -> false;
        };
    }

    public static Vec3 findGroundCover(
            ServerLevel level,
            ServerPlayer focus,
            Vec3 origin,
            int minimumDistance,
            int maximumDistance) {
        Vec3 away = origin.subtract(focus.position());
        Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        double baseAngle = Math.atan2(horizontal.z, horizontal.x);
        for (int attempt = 0; attempt < 18; attempt++) {
            double angle = baseAngle + (level.random.nextDouble() - 0.5D) * 1.6D;
            double distance = minimumDistance + level.random.nextDouble() * (maximumDistance - minimumDistance);
            Vec3 ideal = focus.position().add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
            Vec3 stand = findStandableNear(level, ideal, 3);
            if (stand != null
                    && !AbstractUncannyHuntingSpecialEntity.isDirectlyObserved(focus,
                            stand.add(0.0D, 1.62D, 0.0D), 0.02D)) {
                return stand;
            }
        }
        return null;
    }

    public static Vec3 findOppositeSoundPosition(
            ServerLevel level,
            ServerPlayer focus,
            Vec3 hunterPosition,
            int minimumDistance,
            int maximumDistance) {
        Vec3 hunterOffset = hunterPosition.subtract(focus.position());
        Vec3 horizontal = new Vec3(hunterOffset.x, 0.0D, hunterOffset.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        horizontal = horizontal.normalize().scale(-1.0D);
        double distance = minimumDistance + level.random.nextDouble() * (maximumDistance - minimumDistance);
        return findStandableNear(level, focus.position().add(horizontal.scale(distance)), 4);
    }

    public static Vec3 findReachableGroundNear(
            ServerLevel level,
            Mob mob,
            Vec3 ideal,
            int maximumProbes) {
        return findReachableGroundNear(
                level,
                mob,
                ideal,
                ideal,
                0.0D,
                Double.POSITIVE_INFINITY,
                maximumProbes);
    }

    public static Vec3 findReachableGroundNear(
            ServerLevel level,
            Mob mob,
            Vec3 ideal,
            Vec3 radiusCenter,
            double minimumRadius,
            double maximumRadius,
            int maximumProbes) {
        int probes = Math.max(1, Math.min(16, maximumProbes));
        double minimumRadiusSquared = Math.max(0.0D, minimumRadius) * Math.max(0.0D, minimumRadius);
        double maximumRadiusSquared = Double.isFinite(maximumRadius)
                ? Math.max(minimumRadius, maximumRadius) * Math.max(minimumRadius, maximumRadius)
                : Double.POSITIVE_INFINITY;
        for (int attempt = 0; attempt < probes; attempt++) {
            Vec3 offset = attempt == 0
                    ? ideal
                    : ideal.add(level.random.nextInt(7) - 3, 0.0D, level.random.nextInt(7) - 3);
            Vec3 stand = findStandableNear(level, offset, 2);
            if (stand == null) {
                continue;
            }
            double horizontalX = stand.x - radiusCenter.x;
            double horizontalZ = stand.z - radiusCenter.z;
            double radiusSquared = horizontalX * horizontalX + horizontalZ * horizontalZ;
            if (radiusSquared < minimumRadiusSquared || radiusSquared > maximumRadiusSquared) {
                continue;
            }
            Path path = mob.getNavigation().createPath(BlockPos.containing(stand), 0);
            if (path != null && path.canReach()) {
                return stand;
            }
        }
        return null;
    }

    public static Vec3 findNearestWater(ServerLevel level, BlockPos origin, int radius) {
        Vec3 best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                for (int dy = -5; dy <= 3; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!level.hasChunkAt(pos) || !level.getFluidState(pos).is(FluidTags.WATER)) {
                        continue;
                    }
                    double distance = pos.distSqr(origin);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = Vec3.atCenterOf(pos);
                    }
                }
            }
        }
        return best;
    }

    /** Samples the loaded ground surface of the encirclement area every two blocks. */
    private static int countFlankerWaterSurface(ServerLevel level, ServerPlayer player, boolean waterOnly) {
        // waterOnly counts water columns; otherwise only dry solid ground columns.
        int radius = (int) Math.ceil(HuntingSpecialRules.FLANKER_WATCHED_MAX_RADIUS);
        BlockPos center = player.blockPosition();
        int count = 0;
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                BlockPos column = center.offset(dx, 0, dz);
                if (!level.hasChunkAt(column)) {
                    continue;
                }
                for (int dy = 4; dy >= -6; dy--) {
                    BlockPos probe = column.above(dy);
                    if (level.getFluidState(probe).is(FluidTags.WATER)) {
                        if (waterOnly) {
                            count++;
                        }
                        break;
                    }
                    if (!level.getBlockState(probe).getCollisionShape(level, probe).isEmpty()) {
                        if (!waterOnly) {
                            count++;
                        }
                        break;
                    }
                }
            }
        }
        return count;
    }

    public static boolean isNearWater(ServerLevel level, BlockPos origin, int radius) {
        return findNearestWater(level, origin, radius) != null;
    }

    public static Vec3 findSeabedPullAnchor(ServerLevel level, BlockPos origin, int horizontalRadius) {
        for (int attempt = 0; attempt < 24; attempt++) {
            int dx = attempt == 0 ? 0 : level.random.nextInt(horizontalRadius * 2 + 1) - horizontalRadius;
            int dz = attempt == 0 ? 0 : level.random.nextInt(horizontalRadius * 2 + 1) - horizontalRadius;
            BlockPos probe = origin.offset(dx, 0, dz);
            if (!level.hasChunkAt(probe)) {
                continue;
            }
            if (!level.getFluidState(probe).is(FluidTags.WATER)) {
                continue;
            }
            BlockPos cursor = probe;
            int upwardScans = 0;
            while (upwardScans++ < 32
                    && level.hasChunkAt(cursor.above())
                    && level.getFluidState(cursor.above()).is(FluidTags.WATER)) {
                cursor = cursor.above();
            }
            int waterBlocks = 0;
            while (cursor.getY() > level.getMinBuildHeight() + 1 && waterBlocks <= 64) {
                if (!level.getFluidState(cursor).is(FluidTags.WATER)) {
                    if (waterBlocks >= 8 && level.getBlockState(cursor).isCollisionShapeFullBlock(level, cursor)) {
                        return Vec3.atBottomCenterOf(cursor.above());
                    }
                    break;
                }
                waterBlocks++;
                cursor = cursor.below();
            }
        }
        return null;
    }

    public static boolean isWaterRouteClear(ServerLevel level, Vec3 from, Vec3 to) {
        if (level.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty())).getType() != HitResult.Type.MISS) {
            return false;
        }
        for (int step = 0; step <= 8; step++) {
            Vec3 sample = from.lerp(to, step / 8.0D);
            BlockPos pos = BlockPos.containing(sample);
            if (!level.hasChunkAt(pos) || !level.getFluidState(pos).is(FluidTags.WATER)) {
                return false;
            }
        }
        return true;
    }

    public static List<BlockPos> findConnectedLavaRoute(
            ServerLevel level,
            BlockPos start,
            BlockPos target,
            int nodeLimit) {
        BlockPos surfaceStart = findNearbyLavaSurface(level, start, 2);
        if (surfaceStart == null) {
            return null;
        }
        int limit = Math.max(1, Math.min(HuntingSpecialRules.ASHWALKER_PATH_NODE_LIMIT, nodeLimit));
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        Map<Long, Long> parent = new HashMap<>();
        open.add(surfaceStart);
        visited.add(surfaceStart.asLong());
        parent.put(surfaceStart.asLong(), Long.MIN_VALUE);
        BlockPos best = surfaceStart;
        double bestDistance = horizontalDistanceSqr(surfaceStart, target);
        // Lava under a bridge is a tunnel, never a destination: it would keep the hunter
        // submerged directly below a player standing on a suspended bridge.
        BlockPos bestOpen = hasOpenAshwalkerHeadroom(level, surfaceStart) ? surfaceStart : null;
        double bestOpenDistance = bestOpen == null ? Double.MAX_VALUE : bestDistance;

        while (!open.isEmpty() && visited.size() < limit) {
            BlockPos current = open.removeFirst();
            double distance = horizontalDistanceSqr(current, target);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = current;
            }
            if (distance < bestOpenDistance && hasOpenAshwalkerHeadroom(level, current)) {
                bestOpenDistance = distance;
                bestOpen = current;
                if (distance <= 4.0D) {
                    break;
                }
            }
            for (Direction direction : HORIZONTAL_DIRECTIONS) {
                BlockPos horizontal = current.relative(direction);
                BlockPos next = null;
                for (int dy = 1; dy >= -1; dy--) {
                    BlockPos candidate = horizontal.offset(0, dy, 0);
                    if (isLoadedLavaSurface(level, candidate)) {
                        next = candidate;
                        break;
                    }
                }
                if (next == null || !visited.add(next.asLong())) {
                    continue;
                }
                parent.put(next.asLong(), current.asLong());
                open.addLast(next);
            }
        }
        if (bestOpen != null) {
            best = bestOpen;
            bestDistance = bestOpenDistance;
        }
        if (best.equals(surfaceStart) && bestDistance > 4.0D) {
            return null;
        }
        List<BlockPos> reversed = new ArrayList<>();
        long cursor = best.asLong();
        while (cursor != Long.MIN_VALUE) {
            reversed.add(BlockPos.of(cursor));
            cursor = parent.getOrDefault(cursor, Long.MIN_VALUE);
        }
        Collections.reverse(reversed);
        return reversed;
    }

    public static void recordFlankerTerminal(
            ServerLevel level,
            UUID pairId,
            UUID memberId,
            boolean killedByPlayer,
            String reason) {
        UncannyWorldState.FlankerPairRewardState state = UncannyWorldState.get(level.getServer())
                .recordFlankerTerminal(pairId, memberId, killedByPlayer);
        UncannyDiagnostics.record(
                state != null && state.resolved() && !state.rewardEligible()
                        ? DiagnosticSeverity.WARNING : DiagnosticSeverity.INFO,
                "special",
                "flanker_pair_terminal",
                UncannyDiagnostics.fields(
                        "pair_id", pairId,
                        "member_id", memberId,
                        "player_kill", killedByPlayer,
                        "reason", reason,
                        "resolved", state != null && state.resolved(),
                        "reward_eligible", state != null && state.rewardEligible()));
    }

    public static boolean claimFlankerReward(ServerLevel level, UUID pairId) {
        return level != null
                && pairId != null
                && UncannyWorldState.get(level.getServer()).claimFlankerPairReward(pairId);
    }

    private static Vec3 findGroundRing(
            ServerLevel level,
            Vec3 center,
            int minimumDistance,
            int maximumDistance,
            int attempts) {
        for (int attempt = 0; attempt < attempts; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = minimumDistance + level.random.nextDouble() * (maximumDistance - minimumDistance);
            Vec3 ideal = center.add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
            Vec3 stand = findStandableNear(level, ideal, 5);
            if (stand != null) {
                return stand;
            }
        }
        return null;
    }

    private static Vec3 findStandableNear(ServerLevel level, Vec3 ideal, int verticalRadius) {
        BlockPos origin = BlockPos.containing(ideal);
        if (!level.hasChunkAt(origin)) {
            return null;
        }
        for (int dy = verticalRadius; dy >= -verticalRadius; dy--) {
            BlockPos feet = origin.offset(0, dy, 0);
            if (!level.hasChunkAt(feet)) {
                continue;
            }
            BlockState feetState = level.getBlockState(feet);
            BlockState headState = level.getBlockState(feet.above());
            BlockState floorState = level.getBlockState(feet.below());
            if (feetState.getCollisionShape(level, feet).isEmpty()
                    && headState.getCollisionShape(level, feet.above()).isEmpty()
                    && floorState.isCollisionShapeFullBlock(level, feet.below())) {
                return Vec3.atBottomCenterOf(feet);
            }
        }
        return null;
    }

    private static Vec3 findDeepWaterPosition(
            ServerLevel level,
            ServerPlayer player,
            int minimumDistance,
            int maximumDistance,
            boolean floorPosition) {
        for (int attempt = 0; attempt < 36; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = minimumDistance + level.random.nextDouble() * (maximumDistance - minimumDistance);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            BlockPos chunkProbe = new BlockPos(x, player.blockPosition().getY(), z);
            if (!level.hasChunkAt(chunkProbe)) {
                continue;
            }
            int startY = Math.max(player.blockPosition().getY() + 8, level.getSeaLevel() + 2);
            int waterDepth = 0;
            BlockPos topWater = null;
            BlockPos floor = null;
            for (int y = Math.min(level.getMaxBuildHeight() - 2, startY);
                    y > level.getMinBuildHeight() + 1 && y >= startY - 48;
                    y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (level.getFluidState(pos).is(FluidTags.WATER)) {
                    if (topWater == null) {
                        topWater = pos;
                    }
                    waterDepth++;
                } else if (waterDepth > 0) {
                    floor = pos;
                    break;
                }
            }
            if (waterDepth < 8 || topWater == null || floor == null) {
                continue;
            }
            BlockPos chosen = floorPosition
                    ? floor.above()
                    : topWater.below(Math.min(3, waterDepth - 2));
            return Vec3.atCenterOf(chosen);
        }
        return null;
    }

    private static Vec3 findLavaSurfacePosition(
            ServerLevel level,
            ServerPlayer player,
            int minimumDistance,
            int maximumDistance) {
        for (int attempt = 0; attempt < 48; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = minimumDistance + level.random.nextDouble() * (maximumDistance - minimumDistance);
            BlockPos column = BlockPos.containing(
                    player.getX() + Math.cos(angle) * distance,
                    player.getY(),
                    player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunkAt(column)) {
                continue;
            }
            BlockPos surface = findNearbyLavaSurface(level, column, 14);
            if (surface == null || !hasOpenAshwalkerHeadroom(level, surface)) {
                continue;
            }
            List<BlockPos> route = findConnectedLavaRoute(
                    level, surface, player.blockPosition(), HuntingSpecialRules.ASHWALKER_PATH_NODE_LIMIT);
            if (route != null && route.size() >= 3) {
                return Vec3.atCenterOf(surface).add(0.0D, 0.12D, 0.0D);
            }
        }
        return null;
    }

    private static BlockPos findNearbyLavaSurface(ServerLevel level, BlockPos origin, int verticalRadius) {
        for (int dy = verticalRadius; dy >= -verticalRadius; dy--) {
            BlockPos candidate = origin.offset(0, dy, 0);
            if (isLoadedLavaSurface(level, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isLoadedLavaSurface(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos)
                && level.getFluidState(pos).is(FluidTags.LAVA)
                && !level.getFluidState(pos.above()).is(FluidTags.LAVA);
    }

    private static boolean hasOpenAshwalkerHeadroom(ServerLevel level, BlockPos lavaSurface) {
        BlockPos head = lavaSurface.above();
        return level.hasChunkAt(head)
                && level.getBlockState(head).getCollisionShape(level, head).isEmpty();
    }

    private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private static boolean canReachPlayer(Mob entity, ServerPlayer player) {
        Path path = entity.getNavigation().createPath(player, 1);
        return path != null && path.canReach();
    }

    private static FlankerSpawnPair findOpposedFlankerPositions(
            ServerLevel level,
            ServerPlayer player,
            int minimumDistance,
            int maximumDistance) {
        double radius = minimumDistance + (maximumDistance - minimumDistance) * 0.25D;
        double baseAngle = Math.toRadians(player.getYRot());
        for (int attempt = 0; attempt < HuntingSpecialRules.FLANKER_MAX_PATH_PROBES; attempt++) {
            double angle = baseAngle + attempt * (Math.PI * 2.0D
                    / HuntingSpecialRules.FLANKER_MAX_PATH_PROBES);
            Vec3 offset = new Vec3(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            Vec3 first = findStandableNear(level, player.position().add(offset), 5);
            Vec3 second = findStandableNear(level, player.position().subtract(offset), 5);
            if (first == null || second == null) {
                continue;
            }
            Vec3 firstOffset = first.subtract(player.position());
            Vec3 secondOffset = second.subtract(player.position());
            if (HuntingSpecialRules.hasValidFlankerSeparation(
                    firstOffset.x,
                    firstOffset.z,
                    secondOffset.x,
                    secondOffset.z)) {
                return new FlankerSpawnPair(first, second);
            }
        }
        return null;
    }

    /**
     * Checks a linked pair only after both navigation objects belong to the live level. The first
     * candidate is retained when possible; bounded, evenly distributed alternatives prevent one
     * unlucky random angle from rejecting an otherwise valid open encounter.
     */
    private static boolean positionFlankerPairOnReachableOpposites(
            ServerLevel level,
            ServerPlayer player,
            UncannyFlankerEntity first,
            UncannyFlankerEntity second,
            int minimumDistance,
            int maximumDistance) {
        if (canReachPlayer(first, player) && canReachPlayer(second, player)) {
            return true;
        }
        double radius = minimumDistance + (maximumDistance - minimumDistance) * 0.25D;
        double baseAngle = Math.toRadians(player.getYRot());
        for (int attempt = 0; attempt < HuntingSpecialRules.FLANKER_MAX_PATH_PROBES; attempt++) {
            double angle = baseAngle + attempt * (Math.PI * 2.0D
                    / HuntingSpecialRules.FLANKER_MAX_PATH_PROBES);
            Vec3 offset = new Vec3(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            Vec3 firstPosition = findStandableNear(level, player.position().add(offset), 5);
            Vec3 secondPosition = findStandableNear(level, player.position().subtract(offset), 5);
            if (firstPosition == null || secondPosition == null) {
                continue;
            }
            Vec3 firstOffset = firstPosition.subtract(player.position());
            Vec3 secondOffset = secondPosition.subtract(player.position());
            if (!HuntingSpecialRules.hasValidFlankerSeparation(
                    firstOffset.x,
                    firstOffset.z,
                    secondOffset.x,
                    secondOffset.z)) {
                continue;
            }
            first.moveTo(firstPosition.x, firstPosition.y, firstPosition.z, player.getYRot(), 0.0F);
            second.moveTo(secondPosition.x, secondPosition.y, secondPosition.z, player.getYRot(), 0.0F);
            first.setOnGround(true);
            second.setOnGround(true);
            first.getNavigation().stop();
            second.getNavigation().stop();
            if (level.noCollision(first)
                    && level.noCollision(second)
                    && canReachPlayer(first, player)
                    && canReachPlayer(second, player)) {
                return true;
            }
        }
        return false;
    }

    private static void traceSpawnFailure(ServerPlayer player, String id, String reason) {
        UncannyDiagnostics.recordEventOutcome(
                player,
                "special",
                id,
                "context_failed",
                UncannyDiagnostics.fields("reason", reason, "loaded_chunks_only", true));
    }

    private record FlankerSpawnPair(Vec3 first, Vec3 second) {
    }
}
