package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import com.eotv.echoofthevoid.block.entity.custom.UncannyRestorationPlaceholderBlockEntity;
import com.eotv.echoofthevoid.diagnostics.DiagnosticSeverity;
import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.event.special.MinerRules;
import com.eotv.echoofthevoid.event.special.MinerTunnelPlanner;
import com.eotv.echoofthevoid.event.special.UncannyMinerBlockPolicy;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;

/**
 * A physical continuation of Ghost Miner. The entity remains a non-interactive tunnelling
 * controller until it has opened a safe exit, then becomes an ordinary adaptive Attacker?.
 */
public final class UncannyMinerEntity extends UncannyStalkerEntity {
    private static final EntityDataAccessor<Byte> MINER_STAGE =
            SynchedEntityData.defineId(UncannyMinerEntity.class, EntityDataSerializers.BYTE);

    private final List<BlockPos> plannedColumns = new ArrayList<>();
    private final List<BlockPos> currentSectionTargets = new ArrayList<>(4);
    private final List<BlockPos> minedBlocks = new ArrayList<>();
    private BlockPos currentColumn;
    private BlockPos occupiedColumn;
    private BlockPos exitTunnelColumn;
    private BlockPos emergencePosition;
    private boolean floorApproach;
    private int soundsAtCurrentSection;
    private int completedSections;
    private int constructionSectionsUsed;
    private long nextHitTick;
    private long pendingPickupTick = -1L;
    private BlockPos pendingPickupPosition;
    private int emergenceTicks;
    private Vec3 emergenceStart;
    private boolean emergencePositionReached;
    private int huntPathValidationTicks;
    private boolean movingToClearedColumn;
    private boolean initialColumnCleared;
    private int physicalMovementFailureTicks;
    private int physicalRecoveryAttempts;
    private BlockPos physicalDestination;
    private Vec3 previousPhysicalPosition;
    private double previousPhysicalDestinationDistance = Double.POSITIVE_INFINITY;
    private int tunnelGeometryFailureTicks;
    private long attackGraceUntilTick = Long.MIN_VALUE;
    private double maximumPhysicalDisplacement;
    private int continuousFleeTicks;
    private double fleeStartDistance;
    private double previousTargetDistance;
    private boolean accelerated;
    private int confirmedTargetHits;
    private boolean initialized;

    public UncannyMinerEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Miner?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        // A directly summoned entity is an emerged Miner? instead of an inert invisible controller.
        builder.define(MINER_STAGE, (byte) Stage.HUNTING.id());
    }

    public boolean initializeTunnel(
            ServerPlayer target,
            MinerTunnelPlanner.TunnelPlan plan,
            boolean debug) {
        if (target == null || plan == null || plan.columns().isEmpty()) {
            return false;
        }
        setHuntTarget(target);
        this.plannedColumns.clear();
        this.plannedColumns.addAll(plan.columns());
        this.currentColumn = this.plannedColumns.removeFirst();
        this.occupiedColumn = null;
        this.exitTunnelColumn = null;
        this.emergencePosition = plan.emergencePosition();
        this.floorApproach = plan.floorApproach();
        this.soundsAtCurrentSection = 0;
        this.completedSections = 0;
        this.constructionSectionsUsed = isConstructionColumn(target.serverLevel(), currentColumn) ? 1 : 0;
        this.nextHitTick = target.serverLevel().getGameTime() + 6L;
        this.previousTargetDistance = distanceTo(target);
        this.fleeStartDistance = this.previousTargetDistance;
        this.pendingPickupTick = -1L;
        this.pendingPickupPosition = null;
        this.emergenceTicks = 0;
        this.emergenceStart = null;
        this.emergencePositionReached = false;
        this.huntPathValidationTicks = 0;
        this.movingToClearedColumn = false;
        this.initialColumnCleared = false;
        this.physicalMovementFailureTicks = 0;
        this.physicalRecoveryAttempts = 0;
        this.physicalDestination = null;
        this.previousPhysicalPosition = null;
        this.previousPhysicalDestinationDistance = Double.POSITIVE_INFINITY;
        this.tunnelGeometryFailureTicks = 0;
        this.attackGraceUntilTick = Long.MIN_VALUE;
        this.maximumPhysicalDisplacement = 0.0D;
        this.confirmedTargetHits = 0;
        this.initialized = true;
        setStage(Stage.BURROWING);
        configureTunnelState();
        setPos(currentColumn.getX() + 0.5D, currentColumn.getY(), currentColumn.getZ() + 0.5D);
        resetPhysicalMovement(currentColumn);
        if (!prepareCurrentSection(target.serverLevel())) {
            return false;
        }
        setPersistenceRequired();
        UncannyDiagnostics.recordSpecialLifecycle(
                target,
                this,
                "miner",
                "tunnel_planned",
                debug ? "debug" : "natural",
                DiagnosticSeverity.INFO,
                UncannyDiagnostics.fields(
                        "sections", plan.columns().size(),
                        "floor_approach", floorApproach,
                        "construction_sections_planned", plan.constructionSections()));
        return true;
    }

    public void initializeEmerged(ServerPlayer target) {
        setHuntTarget(target);
        this.occupiedColumn = blockPosition().immutable();
        this.exitTunnelColumn = null;
        this.initialized = true;
        setStage(Stage.HUNTING);
        configureHuntingState();
        setPersistenceRequired();
    }

    public Stage stage() {
        return Stage.byId(this.entityData.get(MINER_STAGE));
    }

    public boolean targets(ServerPlayer player) {
        return player != null
                && level() instanceof ServerLevel serverLevel
                && player.equals(resolveTargetPlayer(serverLevel));
    }

    @Override
    protected boolean usesHiddenPathRecovery() {
        return false;
    }

    @Override
    protected com.eotv.echoofthevoid.event.special.CombatParityRules.Profile combatParity() {
        return com.eotv.echoofthevoid.event.special.CombatParityRules.MINER;
    }

    @Override
    protected boolean runsStalkerHuntLogic() {
        return stage() == Stage.HUNTING;
    }

    @Override
    protected void onVisiblePathFailure(ServerPlayer targetPlayer) {
        super.onVisiblePathFailure(targetPlayer);
        UncannyDiagnostics.recordSpecialLifecycle(
                targetPlayer,
                this,
                "miner",
                "visible_hunt_repath",
                "attacker_hidden_recovery_suppressed",
                DiagnosticSeverity.WARNING,
                UncannyDiagnostics.fields("distance", distanceTo(targetPlayer)));
    }

    @Override
    public void aiStep() {
        // Even while tunnelling, use LivingEntity's real travel, collision and jump pipeline. The
        // Stalker hook above suppresses only Attacker?'s hunt logic until emergence.
        super.aiStep();
        Stage stage = stage();
        if (stage == Stage.HUNTING) {
            return;
        }

        // Client movement is driven by normal entity synchronization. Only the server mutates
        // blocks or advances the tunnelling state machine.
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer target = resolveTargetPlayer(level);
        if (!isValidTunnelTarget(target)) {
            abortTunnel(target, "target_unavailable");
            return;
        }
        if (distanceToSqr(target) > MinerRules.MAX_TARGET_DISTANCE * MinerRules.MAX_TARGET_DISTANCE) {
            abortTunnel(target, "target_too_far");
            return;
        }
        if (stage == Stage.EMERGING) {
            tickEmergence(level, target);
        } else {
            tickTunnel(level, target);
        }
    }

    private void tickTunnel(ServerLevel level, ServerPlayer target) {
        configureTunnelState();
        if (!validatePhysicalTunnelPosition(target)) {
            return;
        }
        updateFleeState(target);
        long now = level.getGameTime();
        if (pendingPickupTick >= 0L && now >= pendingPickupTick) {
            float pitch = ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 2.0F;
            level.playSound(
                    null,
                    pendingPickupPosition,
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS,
                    0.20F,
                    pitch);
            pendingPickupTick = -1L;
            pendingPickupPosition = null;
        }
        if (movingToClearedColumn) {
            if (tickPhysicalMovement(level, target, currentColumn, "cleared_column")) {
                movingToClearedColumn = false;
                physicalMovementFailureTicks = 0;
                physicalRecoveryAttempts = 0;
                occupiedColumn = currentColumn.immutable();
                resetPhysicalMovement(null);
                if (!advanceOrEmerge(level, target)) {
                    return;
                }
            }
            return;
        }
        if (now < nextHitTick || currentColumn == null) {
            return;
        }
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            abortTunnel(target, "mob_griefing_disabled");
            return;
        }

        if (soundsAtCurrentSection >= currentSectionTargets.size()) {
            abortTunnel(target, "empty_section_state");
            return;
        }
        BlockPos strikePos = currentSectionTargets.get(soundsAtCurrentSection);
        MineResult result = replaceWithPlaceholder(level, strikePos);
        if (!result.success()) {
            abortTunnel(target, result.reason());
            return;
        }
        playMiningSound(level, strikePos, result.originalState());
        pendingPickupPosition = strikePos.immutable();
        pendingPickupTick = now + 5L;
        soundsAtCurrentSection++;

        boolean completedSection = soundsAtCurrentSection >= currentSectionTargets.size();
        if (completedSection) {
            synchronizeRestorationDeadline(
                    level,
                    currentSectionTargets,
                    now + MinerRules.RESTORE_DELAY_TICKS);
            completedSections++;
            soundsAtCurrentSection = 0;
            initialColumnCleared = true;
            movingToClearedColumn = true;
            resetPhysicalMovement(currentColumn);
            configureTunnelState();
        }
        int delay = MinerRules.nextHitDelayTicks(
                random.nextInt(8), completedSection, completedSections, accelerated);
        nextHitTick = now + delay;
    }

    private MineResult replaceWithPlaceholder(ServerLevel level, BlockPos pos) {
        BlockState original = level.getBlockState(pos);
        if (original.is(UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get())
                && level.getBlockEntity(pos) instanceof UncannyRestorationPlaceholderBlockEntity placeholder
                && getUUID().equals(placeholder.ownerEntityId())) {
            return new MineResult(true, placeholder.originalState(), "already_owned");
        }
        UncannyMinerBlockPolicy.MaterialKind kind = UncannyMinerBlockPolicy.classify(level, pos);
        if (kind == UncannyMinerBlockPolicy.MaterialKind.BLOCKED) {
            traceBlockRefusal(pos, original, "policy");
            return new MineResult(false, original, "unsafe_block");
        }
        if (!CommonHooks.canEntityDestroy(level, pos, this)) {
            traceBlockRefusal(pos, original, "protection_hook");
            return new MineResult(false, original, "protected_block");
        }
        if (original.getPistonPushReaction() == PushReaction.DESTROY) {
            traceBlockRefusal(pos, original, "secondary_destruction");
            return new MineResult(false, original, "unsafe_reaction");
        }
        boolean changed = level.setBlock(
                pos,
                UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get().defaultBlockState(),
                Block.UPDATE_ALL);
        if (!changed
                || !(level.getBlockEntity(pos) instanceof UncannyRestorationPlaceholderBlockEntity placeholder)) {
            if (changed) {
                level.setBlock(pos, original, Block.UPDATE_ALL);
            }
            return new MineResult(false, original, "placeholder_failed");
        }
        placeholder.initialize(original, level.getGameTime() + MinerRules.RESTORE_DELAY_TICKS, getUUID());
        BlockPos immutablePos = pos.immutable();
        if (!minedBlocks.contains(immutablePos)) {
            minedBlocks.add(immutablePos);
        }
        return new MineResult(true, original, "replaced");
    }

    private void synchronizeRestorationDeadline(
            ServerLevel level, Iterable<BlockPos> positions, long restoreAtGameTime) {
        for (BlockPos pos : positions) {
            if (level.hasChunkAt(pos)
                    && level.getBlockEntity(pos) instanceof UncannyRestorationPlaceholderBlockEntity placeholder
                    && getUUID().equals(placeholder.ownerEntityId())) {
                placeholder.deferRestoreUntil(restoreAtGameTime);
            }
        }
    }

    private void playMiningSound(ServerLevel level, BlockPos pos, BlockState original) {
        var soundType = original.getSoundType();
        level.playSound(
                null,
                pos,
                soundType.getBreakSound(),
                SoundSource.BLOCKS,
                Math.min(1.18F, 0.78F + soundType.getVolume() * 0.37F),
                0.84F + random.nextFloat() * 0.26F);
    }

    private boolean advanceOrEmerge(ServerLevel level, ServerPlayer target) {
        if (completedSections >= MinerRules.MAX_TUNNEL_SECTIONS) {
            abortTunnel(target, "section_limit");
            return false;
        }

        BlockPos routeOrigin = occupiedColumn == null ? currentColumn : occupiedColumn;

        // Once the last planned excavation is complete, the retained exit is the authoritative
        // next step. Re-running A* here used to reject a safe opening after a small player move,
        // discard Miner?, and restore the visible hole before the entity could emerge.
        if (plannedColumns.isEmpty() && canEnterRetainedEmergence(level, target)) {
            traceRetainedPlan(target, "retained_emergence", 0);
            beginEmergence(level, target);
            return false;
        }

        MinerTunnelPlanner.TunnelPlan continuation = MinerTunnelPlanner.replan(
                level,
                routeOrigin,
                target,
                MinerRules.MAX_TUNNEL_SECTIONS - completedSections,
                constructionSectionsUsed);
        if (continuation == null) {
            if (advanceAlongRetainedPlan(level, target)) {
                return true;
            }
            if (canEnterRetainedEmergence(level, target)) {
                traceRetainedPlan(target, "retained_emergence_after_replan_failure", 0);
                beginEmergence(level, target);
                return false;
            }
            abortTunnel(target, "no_safe_replan");
            return false;
        }
        this.emergencePosition = continuation.emergencePosition();
        this.floorApproach = continuation.floorApproach();
        this.plannedColumns.clear();
        this.plannedColumns.addAll(continuation.columns());
        if (plannedColumns.isEmpty()) {
            beginEmergence(level, target);
            return false;
        }
        this.currentColumn = plannedColumns.removeFirst();
        if (isConstructionStep(level, routeOrigin, currentColumn)) {
            constructionSectionsUsed++;
        }
        if (!prepareCurrentSection(level)) {
            abortTunnel(target, "unsafe_planned_section");
            return false;
        }
        return true;
    }

    private boolean advanceAlongRetainedPlan(ServerLevel level, ServerPlayer target) {
        if (plannedColumns.isEmpty()) {
            return false;
        }
        BlockPos next = plannedColumns.getFirst();
        BlockPos routeOrigin = occupiedColumn == null ? currentColumn : occupiedColumn;
        if (!isAdjacentTunnelStep(routeOrigin, next) || !isSafePlannedStep(level, routeOrigin, next)) {
            return false;
        }
        plannedColumns.removeFirst();
        currentColumn = next;
        if (isConstructionStep(level, routeOrigin, currentColumn)) {
            constructionSectionsUsed++;
        }
        if (!prepareCurrentSection(level)) {
            return false;
        }
        traceRetainedPlan(target, "retained_route", plannedColumns.size());
        return true;
    }

    private boolean canEnterRetainedEmergence(ServerLevel level, ServerPlayer target) {
        if (currentColumn == null || emergencePosition == null) {
            return false;
        }
        int horizontal = Math.abs(currentColumn.getX() - emergencePosition.getX())
                + Math.abs(currentColumn.getZ() - emergencePosition.getZ());
        int vertical = Math.abs(currentColumn.getY() - emergencePosition.getY());
        if (horizontal != 1
                || vertical > 1
                || !MinerTunnelPlanner.canUseRetainedEmergence(level, emergencePosition, target)) {
            return false;
        }
        // The final headroom has already been excavated by this Miner? at this point. The global
        // planner correctly classifies restoration placeholders as blocked for every *new* route,
        // but retained-route validation must recognize its own collision-free placeholder rather
        // than treating successful excavation as a newly unsafe block.
        return emergencePosition.getY() <= currentColumn.getY()
                || isOwnedClearedCell(level, currentColumn.above(2));
    }

    private boolean isOwnedClearedCell(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getCollisionShape(level, pos).isEmpty()
                && !state.is(UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get())) {
            return true;
        }
        return state.is(UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get())
                && level.getBlockEntity(pos) instanceof UncannyRestorationPlaceholderBlockEntity placeholder
                && getUUID().equals(placeholder.ownerEntityId());
    }

    private static boolean isAdjacentTunnelStep(BlockPos current, BlockPos next) {
        if (current == null || next == null) {
            return false;
        }
        int horizontal = Math.abs(current.getX() - next.getX())
                + Math.abs(current.getZ() - next.getZ());
        int vertical = next.getY() - current.getY();
        return horizontal == 1 && (vertical == 0 || vertical == 1);
    }

    private static boolean isSafePlannedStep(ServerLevel level, BlockPos origin, BlockPos destination) {
        if (!MinerTunnelPlanner.hasStableWalkingSupport(level, destination)) {
            return false;
        }
        if (origin != null && destination.getY() == origin.getY() + 1) {
            return UncannyMinerBlockPolicy.classifyAscendingTransition(level, origin, destination)
                    != UncannyMinerBlockPolicy.MaterialKind.BLOCKED;
        }
        return UncannyMinerBlockPolicy.classifyColumn(level, destination)
                != UncannyMinerBlockPolicy.MaterialKind.BLOCKED;
    }

    private void traceRetainedPlan(ServerPlayer target, String reason, int remainingSections) {
        UncannyDiagnostics.recordSpecialLifecycle(
                target,
                this,
                "miner",
                "replan_fallback",
                reason,
                DiagnosticSeverity.INFO,
                UncannyDiagnostics.fields(
                        "sections_mined", completedSections,
                        "retained_sections", remainingSections,
                        "current_column", currentColumn == null ? "none" : currentColumn.toShortString(),
                        "emergence_position", emergencePosition == null
                                ? "none"
                                : emergencePosition.toShortString()));
    }

    private void beginEmergence(ServerLevel level, ServerPlayer target) {
        this.exitTunnelColumn = currentColumn == null ? null : currentColumn.immutable();
        if (!ensureFinalEmergenceCorridor(level, target)) {
            return;
        }
        this.emergenceStart = position();
        this.emergenceTicks = 0;
        this.emergencePositionReached = false;
        this.huntPathValidationTicks = 0;
        this.physicalMovementFailureTicks = 0;
        this.physicalRecoveryAttempts = 0;
        this.attackGraceUntilTick = level.getGameTime() + MinerRules.EMERGENCE_GRACE_TICKS;
        setStage(Stage.EMERGING);
        setInvisible(false);
        resetPhysicalMovement(emergencePosition);
        playForcedThreatCue(level, true);
        UncannyDiagnostics.recordSpecialLifecycle(
                target,
                this,
                "miner",
                "emerging",
                floorApproach ? "floor" : "wall",
                DiagnosticSeverity.INFO,
                UncannyDiagnostics.fields(
                        "sections_mined", completedSections,
                        "exit_tunnel_column", exitTunnelColumn == null
                                ? "none"
                                : exitTunnelColumn.toShortString(),
                        "emergence_position", emergencePosition == null
                                ? "none"
                                : emergencePosition.toShortString()));
    }

    private void tickEmergence(ServerLevel level, ServerPlayer target) {
        configureTunnelState();
        setInvisible(false);
        emergenceTicks++;
        if (!ensureFinalEmergenceCorridor(level, target)) {
            return;
        }
        if (!emergencePositionReached) {
            if (!tickPhysicalMovement(level, target, emergencePosition, "emergence")) {
                return;
            }
            occupiedColumn = emergencePosition.immutable();
            emergencePositionReached = true;
            resetPhysicalMovement(null);
        }
        if (!transitionToHunting(level, target, "attacker_profile_enabled")) {
            huntPathValidationTicks++;
            if (huntPathValidationTicks == 1 || huntPathValidationTicks == 20) {
                UncannyDiagnostics.recordSpecialLifecycle(
                        target,
                        this,
                        "miner",
                        "hunt_path_wait",
                        "emergence_not_yet_connected",
                        DiagnosticSeverity.WARNING,
                        UncannyDiagnostics.fields(
                                "wait_ticks", huntPathValidationTicks,
                                "distance", distanceTo(target)));
            }
            if (huntPathValidationTicks >= MinerRules.HUNT_PATH_VALIDATION_TICKS) {
                abortTunnel(target, "emergence_hunt_path_unreachable");
            }
            return;
        }
        UncannyDiagnostics.recordSpecialLifecycle(
                target,
                this,
                "miner",
                "emerged",
                "attacker_profile_enabled",
                DiagnosticSeverity.INFO,
                UncannyDiagnostics.fields(
                        "grace_ticks", MinerRules.EMERGENCE_GRACE_TICKS,
                        "movement_ticks", emergenceTicks,
                        "max_tick_displacement", maximumPhysicalDisplacement));
    }

    private boolean tickPhysicalMovement(
            ServerLevel level,
            ServerPlayer target,
            BlockPos destinationBlock,
            String movementPhase) {
        if (destinationBlock == null || !level.hasChunkAt(destinationBlock)) {
            abortTunnel(target, movementPhase + "_destination_unavailable");
            return false;
        }
        if (!destinationBlock.equals(physicalDestination)) {
            resetPhysicalMovement(destinationBlock);
        }

        Vec3 destination = Vec3.atBottomCenterOf(destinationBlock);
        Vec3 offset = destination.subtract(position());
        double horizontalDistance = Math.hypot(offset.x, offset.z);
        double totalDistance = offset.length();
        boolean stableSupport = MinerTunnelPlanner.hasStableWalkingSupport(level, destinationBlock);
        boolean collisionFree = level.noCollision(
                this,
                getBoundingBox().move(destination.subtract(position())));
        if (MinerRules.hasReachedPhysicalDestination(
                horizontalDistance,
                offset.y,
                stableSupport,
                collisionFree)) {
            this.getNavigation().stop();
            this.setZza(0.0F);
            tracePhysicalMovement(target, movementPhase + "_reached", horizontalDistance);
            return true;
        }

        Vec3 actual = previousPhysicalPosition == null
                ? Vec3.ZERO
                : position().subtract(previousPhysicalPosition);
        this.maximumPhysicalDisplacement = Math.max(this.maximumPhysicalDisplacement, actual.length());
        if (actual.length() > MinerRules.MAX_PHYSICAL_DISPLACEMENT_PER_TICK) {
            UncannyDiagnostics.recordSpecialLifecycle(
                    target,
                    this,
                    "miner",
                    "physical_displacement_violation",
                    movementPhase,
                    DiagnosticSeverity.ERROR,
                    UncannyDiagnostics.fields("distance", actual.length()));
        }

        boolean moved = actual.lengthSqr() >= 1.0E-4D;
        boolean closedDistance = previousPhysicalDestinationDistance - totalDistance >= 0.01D;
        if (moved || closedDistance) {
            physicalMovementFailureTicks = 0;
        } else {
            physicalMovementFailureTicks++;
        }
        previousPhysicalPosition = position();
        previousPhysicalDestinationDistance = totalDistance;

        // MoveControl and JumpControl feed Minecraft's normal travel/collision pipeline on the
        // following entity tick. This is intentionally not a direct move or position rewrite.
        if (getNavigation().isDone() || tickCount % 10 == 0) {
            getNavigation().moveTo(
                    destination.x,
                    destination.y,
                    destination.z,
                    MinerRules.TUNNEL_MOVEMENT_SPEED_MODIFIER);
        }
        getMoveControl().setWantedPosition(
                destination.x,
                destination.y,
                destination.z,
                MinerRules.TUNNEL_MOVEMENT_SPEED_MODIFIER);
        if (offset.y > 0.35D
                && horizontalDistance <= 1.35D
                && (onGround() || horizontalCollision)) {
            getJumpControl().jump();
        }

        if (MinerRules.shouldAttemptPhysicalRecovery(
                physicalMovementFailureTicks,
                physicalRecoveryAttempts)) {
            physicalRecoveryAttempts++;
            physicalMovementFailureTicks = 0;
            if (recoverPhysicalMovement(level, target, destinationBlock, movementPhase)) {
                return false;
            }
            resetPhysicalMovement(destinationBlock, false);
        } else if (physicalMovementFailureTicks >= MinerRules.PHYSICAL_REPATH_TICKS
                && physicalRecoveryAttempts >= MinerRules.MAX_PHYSICAL_RECOVERY_ATTEMPTS) {
            if (!tryTransitionToVisibleHunt(level, target, movementPhase + "_recovery_exhausted")) {
                abortTunnel(target, movementPhase + "_physical_recovery_exhausted");
            }
        }
        return false;
    }

    private boolean recoverPhysicalMovement(
            ServerLevel level,
            ServerPlayer target,
            BlockPos failedDestination,
            String movementPhase) {
        UncannyDiagnostics.recordSpecialLifecycle(
                target,
                this,
                "miner",
                "physical_recovery",
                movementPhase,
                DiagnosticSeverity.WARNING,
                UncannyDiagnostics.fields(
                        "attempt", physicalRecoveryAttempts,
                        "failed_destination", failedDestination.toShortString(),
                        "occupied_column", occupiedColumn == null ? "none" : occupiedColumn.toShortString(),
                        "distance", distanceTo(target)));

        if (tryTransitionToVisibleHunt(level, target, movementPhase + "_direct_hunt")) {
            return true;
        }

        BlockPos routeOrigin = occupiedColumn;
        int remainingSections = MinerRules.MAX_TUNNEL_SECTIONS - completedSections;
        if (routeOrigin == null || remainingSections <= 0) {
            return false;
        }
        MinerTunnelPlanner.TunnelPlan recovery = MinerTunnelPlanner.replanAvoiding(
                level,
                routeOrigin,
                target,
                remainingSections,
                constructionSectionsUsed,
                failedDestination);
        if (recovery == null) {
            return false;
        }

        this.emergencePosition = recovery.emergencePosition();
        this.floorApproach = recovery.floorApproach();
        this.plannedColumns.clear();
        this.plannedColumns.addAll(recovery.columns());
        setStage(Stage.BURROWING);
        if (plannedColumns.isEmpty()) {
            currentColumn = routeOrigin;
            beginEmergence(level, target);
            return true;
        }

        currentColumn = plannedColumns.removeFirst();
        if (isConstructionStep(level, routeOrigin, currentColumn)) {
            constructionSectionsUsed++;
        }
        movingToClearedColumn = false;
        soundsAtCurrentSection = 0;
        if (!prepareCurrentSection(level)) {
            // The world may change between A* validation and state installation. Do not leave a
            // half-installed recovery route behind: that corrupts currentColumn/plannedColumns
            // and used to make the next tick look like an unexplained mining stop.
            abortTunnel(target, movementPhase + "_recovery_route_invalidated");
            return true;
        }
        resetPhysicalMovement(currentColumn, false);
        traceRetainedPlan(target, "physical_recovery_route", plannedColumns.size());
        return true;
    }

    private boolean tryTransitionToVisibleHunt(
            ServerLevel level,
            ServerPlayer target,
            String reason) {
        if (distanceToSqr(target) > 6.5D * 6.5D
                || Math.abs(getY() - target.getY()) > 1.5D
                || !hasLineOfSight(target)
                || !target.hasLineOfSight(this)) {
            return false;
        }
        var path = getNavigation().createPath(target, 0);
        if (path == null || !path.canReach()) {
            return false;
        }
        attackGraceUntilTick = Math.max(
                attackGraceUntilTick,
                level.getGameTime() + MinerRules.EMERGENCE_GRACE_TICKS);
        playForcedThreatCue(level, true);
        return transitionToHunting(level, target, reason, path);
    }

    private boolean transitionToHunting(ServerLevel level, ServerPlayer target, String reason) {
        Path path = getNavigation().createPath(target, 0);
        if (path == null || !path.canReach()) {
            return false;
        }
        return transitionToHunting(level, target, reason, path);
    }

    private boolean transitionToHunting(
            ServerLevel level, ServerPlayer target, String reason, Path path) {
        if (path == null || !path.canReach()) {
            return false;
        }
        setStage(Stage.HUNTING);
        configureHuntingState();
        setHuntTarget(target);
        getNavigation().moveTo(path, 1.28D);
        UncannyDiagnostics.recordSpecialLifecycle(
                target,
                this,
                "miner",
                "hunt_started",
                reason,
                DiagnosticSeverity.INFO,
                UncannyDiagnostics.fields(
                        "distance", distanceTo(target),
                        "grace_remaining", Math.max(0L, attackGraceUntilTick - level.getGameTime())));
        return true;
    }

    private boolean ensureFinalEmergenceCorridor(ServerLevel level, ServerPlayer target) {
        if (exitTunnelColumn == null || emergencePosition == null) {
            abortTunnel(target, "missing_final_emergence_geometry");
            return false;
        }
        List<BlockPos> protectedExitCells = new ArrayList<>(3);
        protectedExitCells.add(exitTunnelColumn);
        protectedExitCells.add(exitTunnelColumn.above());
        if (emergencePosition.getY() > exitTunnelColumn.getY()) {
            protectedExitCells.add(exitTunnelColumn.above(2));
        }
        for (BlockPos cell : protectedExitCells) {
            if (!ensureOwnedExitCellOpen(level, target, cell)) {
                return false;
            }
        }
        if (!MinerTunnelPlanner.isGenuineOpenEmergence(level, emergencePosition)) {
            abortTunnel(target, "emergence_no_longer_two_blocks_open");
            return false;
        }
        Vec3 destination = Vec3.atBottomCenterOf(emergencePosition);
        if (!level.noCollision(this, getBoundingBox().move(destination.subtract(position())))) {
            abortTunnel(target, "emergence_hitbox_obstructed");
            return false;
        }
        synchronizeRestorationDeadline(
                level,
                protectedExitCells,
                level.getGameTime() + MinerRules.RESTORE_DELAY_TICKS);
        return true;
    }

    private boolean ensureOwnedExitCellOpen(
            ServerLevel level, ServerPlayer target, BlockPos cell) {
        BlockState state = level.getBlockState(cell);
        if (state.is(UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get())) {
            if (level.getBlockEntity(cell) instanceof UncannyRestorationPlaceholderBlockEntity placeholder
                    && getUUID().equals(placeholder.ownerEntityId())) {
                return true;
            }
            abortTunnel(target, "foreign_restoration_marker_at_exit");
            return false;
        }
        if (state.isAir()) {
            return true;
        }
        // A fluid, a block entity or another collision-free special block can enter the opening
        // after it was planned. It is not safe exterior air and must never be silently accepted as
        // part of the final two-block corridor.
        if (!state.getFluidState().isEmpty()
                || state.hasBlockEntity()
                || level.getBlockEntity(cell) != null
                || state.getCollisionShape(level, cell).isEmpty()) {
            abortTunnel(target, "unsafe_non_air_final_exit_cell");
            return false;
        }
        MineResult result = replaceWithPlaceholder(level, cell);
        if (!result.success()) {
            abortTunnel(target, "unsafe_final_exit_cell_" + result.reason());
            return false;
        }
        playMiningSound(level, cell, result.originalState());
        return true;
    }

    private void resetPhysicalMovement(BlockPos destination) {
        resetPhysicalMovement(destination, true);
    }

    private void resetPhysicalMovement(BlockPos destination, boolean resetRecoveryAttempts) {
        getNavigation().stop();
        physicalDestination = destination == null ? null : destination.immutable();
        previousPhysicalPosition = position();
        previousPhysicalDestinationDistance = destination == null
                ? Double.POSITIVE_INFINITY
                : position().distanceTo(Vec3.atBottomCenterOf(destination));
        physicalMovementFailureTicks = 0;
        if (resetRecoveryAttempts) {
            physicalRecoveryAttempts = 0;
        }
    }

    private void tracePhysicalMovement(ServerPlayer target, String outcome, double remainingDistance) {
        UncannyDiagnostics.recordSpecialLifecycle(
                target,
                this,
                "miner",
                "physical_movement",
                outcome,
                DiagnosticSeverity.INFO,
                UncannyDiagnostics.fields(
                        "remaining_distance", remainingDistance,
                        "max_tick_displacement", maximumPhysicalDisplacement,
                        "current_column", currentColumn == null ? "none" : currentColumn.toShortString()));
    }

    private void updateFleeState(ServerPlayer target) {
        double currentDistance = distanceTo(target);
        Vec3 away = target.position().subtract(position());
        Vec3 targetMotion = target.getDeltaMovement();
        boolean movingAway = away.lengthSqr() > 0.01D
                && targetMotion.horizontalDistanceSqr() > 0.0025D
                && targetMotion.normalize().dot(away.normalize()) > 0.25D
                && currentDistance >= previousTargetDistance - 0.05D;
        if (movingAway) {
            if (continuousFleeTicks == 0) {
                fleeStartDistance = currentDistance;
            }
            continuousFleeTicks++;
        } else {
            continuousFleeTicks = 0;
            fleeStartDistance = currentDistance;
            if (accelerated) {
                accelerated = false;
                traceAcceleration(target, false);
            }
        }
        if (!accelerated && MinerRules.confirmsFleeing(
                continuousFleeTicks, fleeStartDistance, currentDistance)) {
            accelerated = true;
            traceAcceleration(target, true);
        }
        previousTargetDistance = currentDistance;
    }

    private boolean validatePhysicalTunnelPosition(ServerPlayer target) {
        if (!initialColumnCleared || currentColumn == null) {
            this.tunnelGeometryFailureTicks = 0;
            return true;
        }
        // While the next column is excavated the Miner stands in the immediately preceding
        // two-block corridor. A vertical divergence larger than one stair proves that it has
        // fallen out of the planned tunnel; leaving it alive there made every later QA spawn fail
        // with miner_conflict even though the player could no longer see or reach it.
        boolean withinVerticalCorridor = this.getY() >= currentColumn.getY() - 1.25D
                && this.getY() <= currentColumn.getY() + 1.75D;
        boolean withinHorizontalCorridor = Math.abs(this.getX() - (currentColumn.getX() + 0.5D)) <= 1.75D
                && Math.abs(this.getZ() - (currentColumn.getZ() + 0.5D)) <= 1.75D;
        if (withinVerticalCorridor && withinHorizontalCorridor) {
            this.tunnelGeometryFailureTicks = 0;
            return true;
        }
        if (++this.tunnelGeometryFailureTicks < 20) {
            return true;
        }
        abortTunnel(target, "left_physical_tunnel_corridor");
        return false;
    }

    private boolean isValidTunnelTarget(ServerPlayer target) {
        return target != null
                && target.isAlive()
                && !target.isSpectator()
                && target.level() == level();
    }

    private void configureTunnelState() {
        // Keep Mob/ LivingEntity movement controls alive while preventing inherited combat and
        // wander goals from competing with the one-column physical tunnel controller.
        setNoAi(false);
        goalSelector.disableControlFlag(Goal.Flag.MOVE);
        goalSelector.disableControlFlag(Goal.Flag.LOOK);
        goalSelector.disableControlFlag(Goal.Flag.JUMP);
        goalSelector.disableControlFlag(Goal.Flag.TARGET);
        targetSelector.disableControlFlag(Goal.Flag.TARGET);
        setTarget(null);
        setNoGravity(!initialColumnCleared);
        setInvulnerable(true);
        noPhysics = !initialColumnCleared;
        setInvisible(false);
    }

    private void configureHuntingState() {
        setNoAi(false);
        goalSelector.enableControlFlag(Goal.Flag.MOVE);
        goalSelector.enableControlFlag(Goal.Flag.LOOK);
        goalSelector.enableControlFlag(Goal.Flag.JUMP);
        goalSelector.enableControlFlag(Goal.Flag.TARGET);
        // Miner? has one explicit encounter target. Letting NearestAttackableTargetGoal compete
        // with that UUID can redirect it toward a bystander between two server ticks, which is
        // especially visible in multiplayer and can pull it back into the open tunnel.
        targetSelector.disableControlFlag(Goal.Flag.TARGET);
        setNoGravity(false);
        setInvulnerable(false);
        noPhysics = false;
        setInvisible(false);
    }

    private void abortTunnel(ServerPlayer target, String reason) {
        if (!(level() instanceof ServerLevel level)) {
            discard();
            return;
        }
        requestTunnelRestoration(level);
        UncannyDiagnostics.recordSpecialLifecycle(
                target,
                this,
                "miner",
                "aborted",
                reason,
                DiagnosticSeverity.WARNING,
                UncannyDiagnostics.fields(
                        "sections_mined", completedSections,
                        "blocks_pending_restoration", minedBlocks.size()));
        discard();
    }

    public void requestTunnelRestoration(ServerLevel level) {
        for (BlockPos pos : minedBlocks) {
            if (level.hasChunkAt(pos)
                    && level.getBlockEntity(pos) instanceof UncannyRestorationPlaceholderBlockEntity placeholder
                    && getUUID().equals(placeholder.ownerEntityId())) {
                placeholder.requestImmediateRestore();
            }
        }
    }

    public int restoreTunnelNowForDebug(ServerLevel level) {
        int restored = 0;
        for (BlockPos pos : minedBlocks) {
            if (level.hasChunkAt(pos)
                    && level.getBlockEntity(pos) instanceof UncannyRestorationPlaceholderBlockEntity placeholder
                    && getUUID().equals(placeholder.ownerEntityId())) {
                placeholder.requestImmediateRestore();
                if (placeholder.restoreNowIfPossible(level)) {
                    restored++;
                }
            }
        }
        return restored;
    }

    private void traceBlockRefusal(BlockPos pos, BlockState state, String reason) {
        ServerPlayer target = level() instanceof ServerLevel serverLevel
                ? resolveTargetPlayer(serverLevel)
                : null;
        UncannyDiagnostics.recordSpecialLifecycle(
                target,
                this,
                "miner",
                "block_refused",
                reason,
                DiagnosticSeverity.INFO,
                UncannyDiagnostics.fields("block", state.getBlock(), "block_position", pos.toShortString()));
    }

    private void traceAcceleration(ServerPlayer target, boolean enabled) {
        UncannyDiagnostics.recordSpecialLifecycle(
                target,
                this,
                "miner",
                enabled ? "accelerated" : "normal_cadence_restored",
                enabled ? "target_fleeing" : "flee_ended",
                DiagnosticSeverity.INFO,
                UncannyDiagnostics.fields("distance", distanceTo(target)));
    }

    @Override
    public boolean isPickable() {
        return stage() == Stage.HUNTING && super.isPickable();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return stage() != Stage.HUNTING || super.isInvulnerableTo(source);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (level() instanceof ServerLevel level && level.getGameTime() < attackGraceUntilTick) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof ServerPlayer player) {
            confirmedTargetHits++;
            if (confirmedTargetHits == 1) {
                UncannyDiagnostics.recordSpecialLifecycle(
                        player,
                        this,
                        "miner",
                        "target_hit",
                        "end_to_end_sequence_complete",
                        DiagnosticSeverity.INFO,
                        UncannyDiagnostics.fields(
                                "sections_mined", completedSections,
                                "distance", distanceTo(player)));
            }
        }
        return hit;
    }

    public int confirmedTargetHits() {
        return confirmedTargetHits;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("MinerStage", (byte) stage().id());
        tag.putBoolean("MinerInitialized", initialized);
        tag.putBoolean("MinerFloorApproach", floorApproach);
        tag.putInt("MinerSoundsAtSection", soundsAtCurrentSection);
        tag.putInt("MinerCompletedSections", completedSections);
        tag.putInt("MinerConstructionSections", constructionSectionsUsed);
        tag.putLong("MinerNextHitTick", nextHitTick);
        tag.putLong("MinerPendingPickupTick", pendingPickupTick);
        tag.putInt("MinerEmergenceTicks", emergenceTicks);
        tag.putBoolean("MinerEmergencePositionReached", emergencePositionReached);
        tag.putInt("MinerHuntPathValidationTicks", huntPathValidationTicks);
        tag.putBoolean("MinerMovingToClearedColumn", movingToClearedColumn);
        tag.putBoolean("MinerInitialColumnCleared", initialColumnCleared);
        tag.putInt("MinerPhysicalMovementFailures", physicalMovementFailureTicks);
        tag.putInt("MinerPhysicalRecoveryAttempts", physicalRecoveryAttempts);
        tag.putInt("MinerTunnelGeometryFailures", tunnelGeometryFailureTicks);
        tag.putLong("MinerAttackGraceUntil", attackGraceUntilTick);
        tag.putDouble("MinerMaximumPhysicalDisplacement", maximumPhysicalDisplacement);
        tag.putInt("MinerFleeTicks", continuousFleeTicks);
        tag.putDouble("MinerFleeStartDistance", fleeStartDistance);
        tag.putDouble("MinerPreviousTargetDistance", previousTargetDistance);
        tag.putBoolean("MinerAccelerated", accelerated);
        tag.putInt("MinerConfirmedTargetHits", confirmedTargetHits);
        if (currentColumn != null) {
            tag.putLong("MinerCurrentColumn", currentColumn.asLong());
        }
        if (occupiedColumn != null) {
            tag.putLong("MinerOccupiedColumn", occupiedColumn.asLong());
        }
        if (exitTunnelColumn != null) {
            tag.putLong("MinerExitTunnelColumn", exitTunnelColumn.asLong());
        }
        if (physicalDestination != null) {
            tag.putLong("MinerPhysicalDestination", physicalDestination.asLong());
        }
        if (emergencePosition != null) {
            tag.putLong("MinerEmergencePosition", emergencePosition.asLong());
        }
        if (pendingPickupPosition != null) {
            tag.putLong("MinerPendingPickupPosition", pendingPickupPosition.asLong());
        }
        if (emergenceStart != null) {
            tag.putDouble("MinerEmergenceStartX", emergenceStart.x);
            tag.putDouble("MinerEmergenceStartY", emergenceStart.y);
            tag.putDouble("MinerEmergenceStartZ", emergenceStart.z);
        }
        tag.putLongArray("MinerPlannedColumns", plannedColumns.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLongArray(
                "MinerCurrentSectionTargets",
                currentSectionTargets.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLongArray("MinerMinedBlocks", minedBlocks.stream().mapToLong(BlockPos::asLong).toArray());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setStage(tag.contains("MinerStage") ? Stage.byId(tag.getByte("MinerStage")) : Stage.HUNTING);
        initialized = tag.getBoolean("MinerInitialized");
        floorApproach = tag.getBoolean("MinerFloorApproach");
        soundsAtCurrentSection = Math.max(0, tag.getInt("MinerSoundsAtSection"));
        completedSections = Math.max(0, tag.getInt("MinerCompletedSections"));
        constructionSectionsUsed = Math.max(0, tag.getInt("MinerConstructionSections"));
        nextHitTick = tag.getLong("MinerNextHitTick");
        pendingPickupTick = tag.contains("MinerPendingPickupTick") ? tag.getLong("MinerPendingPickupTick") : -1L;
        emergenceTicks = Math.max(0, tag.getInt("MinerEmergenceTicks"));
        emergencePositionReached = tag.getBoolean("MinerEmergencePositionReached");
        huntPathValidationTicks = Math.max(0, tag.getInt("MinerHuntPathValidationTicks"));
        movingToClearedColumn = tag.getBoolean("MinerMovingToClearedColumn");
        initialColumnCleared = tag.contains("MinerInitialColumnCleared")
                ? tag.getBoolean("MinerInitialColumnCleared")
                : stage() == Stage.EMERGING;
        physicalMovementFailureTicks = Math.max(0, tag.getInt("MinerPhysicalMovementFailures"));
        physicalRecoveryAttempts = Math.max(0, tag.getInt("MinerPhysicalRecoveryAttempts"));
        tunnelGeometryFailureTicks = Math.max(0, tag.getInt("MinerTunnelGeometryFailures"));
        attackGraceUntilTick = tag.contains("MinerAttackGraceUntil")
                ? tag.getLong("MinerAttackGraceUntil")
                : Long.MIN_VALUE;
        maximumPhysicalDisplacement = Math.max(0.0D, tag.getDouble("MinerMaximumPhysicalDisplacement"));
        continuousFleeTicks = Math.max(0, tag.getInt("MinerFleeTicks"));
        fleeStartDistance = tag.getDouble("MinerFleeStartDistance");
        previousTargetDistance = tag.getDouble("MinerPreviousTargetDistance");
        accelerated = tag.getBoolean("MinerAccelerated");
        confirmedTargetHits = Math.max(0, tag.getInt("MinerConfirmedTargetHits"));
        currentColumn = tag.contains("MinerCurrentColumn") ? BlockPos.of(tag.getLong("MinerCurrentColumn")) : null;
        occupiedColumn = tag.contains("MinerOccupiedColumn")
                ? BlockPos.of(tag.getLong("MinerOccupiedColumn"))
                : blockPosition().immutable();
        exitTunnelColumn = tag.contains("MinerExitTunnelColumn")
                ? BlockPos.of(tag.getLong("MinerExitTunnelColumn"))
                : stage() == Stage.EMERGING && currentColumn != null ? currentColumn.immutable() : null;
        physicalDestination = tag.contains("MinerPhysicalDestination")
                ? BlockPos.of(tag.getLong("MinerPhysicalDestination"))
                : null;
        emergencePosition = tag.contains("MinerEmergencePosition")
                ? BlockPos.of(tag.getLong("MinerEmergencePosition"))
                : null;
        pendingPickupPosition = tag.contains("MinerPendingPickupPosition")
                ? BlockPos.of(tag.getLong("MinerPendingPickupPosition"))
                : null;
        if (tag.contains("MinerEmergenceStartX")) {
            emergenceStart = new Vec3(
                    tag.getDouble("MinerEmergenceStartX"),
                    tag.getDouble("MinerEmergenceStartY"),
                    tag.getDouble("MinerEmergenceStartZ"));
        }
        plannedColumns.clear();
        for (long packed : tag.getLongArray("MinerPlannedColumns")) {
            plannedColumns.add(BlockPos.of(packed));
        }
        currentSectionTargets.clear();
        if (tag.contains("MinerCurrentSectionTargets")) {
            for (long packed : tag.getLongArray("MinerCurrentSectionTargets")) {
                currentSectionTargets.add(BlockPos.of(packed));
            }
        } else if (currentColumn != null && stage() == Stage.BURROWING) {
            // Compatibility with saves made before variable-height staircase sections existed.
            currentSectionTargets.add(currentColumn.immutable());
            currentSectionTargets.add(currentColumn.above().immutable());
        }
        minedBlocks.clear();
        for (long packed : tag.getLongArray("MinerMinedBlocks")) {
            minedBlocks.add(BlockPos.of(packed));
        }
        if (stage() == Stage.HUNTING) {
            configureHuntingState();
        } else {
            configureTunnelState();
            repairLoadedTransitionTargets();
            resetPhysicalMovement(
                    stage() == Stage.EMERGING
                            ? emergencePosition
                            : movingToClearedColumn ? currentColumn : physicalDestination,
                    false);
        }
    }

    private boolean prepareCurrentSection(ServerLevel level) {
        currentSectionTargets.clear();
        soundsAtCurrentSection = 0;
        if (currentColumn == null) {
            return false;
        }
        boolean ascending = occupiedColumn != null
                && currentColumn.getY() == occupiedColumn.getY() + 1
                && isAdjacentTunnelStep(occupiedColumn, currentColumn);
        if (ascending) {
            if (UncannyMinerBlockPolicy.classifyAscendingTransition(level, occupiedColumn, currentColumn)
                    == UncannyMinerBlockPolicy.MaterialKind.BLOCKED) {
                return false;
            }
            // The first visible break is above and in front of Miner?. The ceiling over the
            // column it leaves is then opened before its feet block, giving its 1.95-block model
            // enough swept clearance to perform a genuine diagonal jump.
            addPresentTarget(level, currentColumn.above());
            addPresentTarget(level, occupiedColumn.above(2));
            addPresentTarget(level, currentColumn);
        } else {
            addPresentTarget(level, currentColumn);
            addPresentTarget(level, currentColumn.above());
        }

        if (plannedColumns.isEmpty()
                && emergencePosition != null
                && emergencePosition.getY() == currentColumn.getY() + 1
                && isAdjacentTunnelStep(currentColumn, emergencePosition)) {
            if (UncannyMinerBlockPolicy.classifyAscendingExitTransition(
                            level, currentColumn, emergencePosition)
                    == UncannyMinerBlockPolicy.MaterialKind.BLOCKED) {
                return false;
            }
            addPresentTarget(level, currentColumn.above(2));
        }

        if (currentSectionTargets.isEmpty()) {
            Vec3 destination = Vec3.atBottomCenterOf(currentColumn);
            boolean openAndSupported = MinerTunnelPlanner.hasStableWalkingSupport(level, currentColumn)
                    && level.noCollision(this, getBoundingBox().move(destination.subtract(position())));
            if (!openAndSupported) {
                return false;
            }
            initialColumnCleared = true;
            movingToClearedColumn = true;
            resetPhysicalMovement(currentColumn);
        }
        return true;
    }

    private void addPresentTarget(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir() && !currentSectionTargets.contains(pos)) {
            currentSectionTargets.add(pos.immutable());
        }
    }

    private void repairLoadedTransitionTargets() {
        if (!(level() instanceof ServerLevel level)
                || stage() != Stage.BURROWING
                || movingToClearedColumn
                || currentColumn == null) {
            return;
        }
        if (occupiedColumn != null
                && currentColumn.getY() == occupiedColumn.getY() + 1
                && isAdjacentTunnelStep(occupiedColumn, currentColumn)) {
            addPresentTarget(level, occupiedColumn.above(2));
        }
        if (plannedColumns.isEmpty()
                && emergencePosition != null
                && emergencePosition.getY() == currentColumn.getY() + 1
                && isAdjacentTunnelStep(currentColumn, emergencePosition)) {
            addPresentTarget(level, currentColumn.above(2));
        }
        soundsAtCurrentSection = Math.min(soundsAtCurrentSection, currentSectionTargets.size());
    }

    private static boolean isConstructionColumn(ServerLevel level, BlockPos column) {
        UncannyMinerBlockPolicy.MaterialKind ordinary =
                UncannyMinerBlockPolicy.classifyColumn(level, column);
        if (ordinary == UncannyMinerBlockPolicy.MaterialKind.CONSTRUCTION) {
            return true;
        }
        return ordinary == UncannyMinerBlockPolicy.MaterialKind.BLOCKED
                && UncannyMinerBlockPolicy.classifyAscendingColumn(level, column)
                        == UncannyMinerBlockPolicy.MaterialKind.CONSTRUCTION;
    }

    private static boolean isConstructionStep(ServerLevel level, BlockPos origin, BlockPos destination) {
        if (origin != null && destination != null && destination.getY() == origin.getY() + 1) {
            return UncannyMinerBlockPolicy.classifyAscendingTransition(level, origin, destination)
                    == UncannyMinerBlockPolicy.MaterialKind.CONSTRUCTION;
        }
        return isConstructionColumn(level, destination);
    }

    private void setStage(Stage stage) {
        this.entityData.set(MINER_STAGE, (byte) stage.id());
    }

    private record MineResult(boolean success, BlockState originalState, String reason) {
    }

    public enum Stage {
        BURROWING(0),
        EMERGING(1),
        HUNTING(2);

        private final int id;

        Stage(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static Stage byId(int id) {
            return switch (id) {
                case 0 -> BURROWING;
                case 1 -> EMERGING;
                default -> HUNTING;
            };
        }
    }
}
