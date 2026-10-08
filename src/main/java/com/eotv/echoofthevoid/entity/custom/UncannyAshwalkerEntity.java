package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.event.special.HuntingSpecialRules;
import com.eotv.echoofthevoid.event.special.UncannyHuntingSpecialSystem;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Nether lava hunter whose traversal limitation is real rather than an arbitrary leash. */
public class UncannyAshwalkerEntity extends AbstractUncannyHuntingSpecialEntity {
    private static final EntityDataAccessor<Boolean> SUBMERGED = SynchedEntityData.defineId(
            UncannyAshwalkerEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> MOTION_STATE = SynchedEntityData.defineId(
            UncannyAshwalkerEntity.class,
            EntityDataSerializers.BYTE);

    private final ArrayDeque<BlockPos> route = new ArrayDeque<>();
    private int submergedTicks;
    private int biteGraceTicks;
    private int pathFailureTicks;
    private int routeRefreshTicks;
    private boolean firstBiteCuePlayed;
    private BlockPos lavaHome;
    private Vec3 lungeTarget;
    private int excursionTicks;
    private int strandedTicks;
    private boolean expandedHitbox;
    // Leap state is short-lived: after a reload the excursion simply walks home.
    private double leapHorizontalSpeed = 0.26D;
    private int landBites;
    private int leapTelegraphTicks;
    private Vec3 pendingLanding;
    private int returnStallTicks;
    private double lastReturnDistanceSqr = Double.MAX_VALUE;
    // Client clock of the last motion change, so the body can rise out of the lava smoothly.
    private int clientMotionChangeTick;

    public UncannyAshwalkerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "Ashwalker?");
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SUBMERGED, false);
        builder.define(MOTION_STATE, (byte) MotionState.SURFACE.id());
    }

    @Override
    protected void registerGoals() {
        // Movement is constrained to the explicit loaded lava graph below.
    }

    @Override
    protected void onTargetConfigured(ServerPlayer player) {
        this.setNoGravity(true);
        this.submergedTicks = 0;
        this.biteGraceTicks = 0;
        this.pathFailureTicks = 0;
        this.routeRefreshTicks = 0;
        this.firstBiteCuePlayed = false;
        this.lavaHome = null;
        this.lungeTarget = null;
        this.excursionTicks = 0;
        this.strandedTicks = 0;
        this.entityData.set(SUBMERGED, false);
        setMotionState(MotionState.SURFACE);
        this.route.clear();
        trace(player, "spawn", "connected_lava_hunt_started");
    }

    public boolean isSubmerged() {
        return this.entityData.get(SUBMERGED);
    }

    public MotionState motionState() {
        return MotionState.byId(this.entityData.get(MOTION_STATE));
    }

    public void forceSubmergedForDebug() {
        this.submergedTicks = HuntingSpecialRules.ASHWALKER_SUBMERGE_MAX_TICKS;
        this.entityData.set(SUBMERGED, true);
        setMotionState(MotionState.SUBMERGED);
    }

    @Override
    protected void tickSpecial(ServerLevel level, ServerPlayer focus) {
        if (this.distanceToSqr(focus) > 52.0D * 52.0D) {
            beginSinking(focus, "focus_beyond_range");
            return;
        }
        if (motionState() == MotionState.LUNGING || motionState() == MotionState.RETURNING) {
            tickExcursion(level, focus);
            return;
        }

        this.setNoGravity(true);
        this.noPhysics = true;
        if (!level.getFluidState(this.blockPosition()).is(FluidTags.LAVA)
                && !level.getFluidState(this.blockPosition().below()).is(FluidTags.LAVA)) {
            beginSinking(focus, "left_connected_lava");
            return;
        }

        boolean coveredBySolidBlock = !level.getBlockState(this.blockPosition().above())
                .getCollisionShape(level, this.blockPosition().above()).isEmpty();
        if (this.submergedTicks > 0) {
            this.submergedTicks--;
            this.entityData.set(SUBMERGED, true);
            setMotionState(MotionState.SUBMERGED);
        } else if (coveredBySolidBlock) {
            // Connected lava may pass below a bridge. Keep following through it, but hide the
            // complete model instead of rendering the head through the solid platform.
            this.entityData.set(SUBMERGED, true);
            setMotionState(MotionState.SUBMERGED);
        } else {
            this.entityData.set(SUBMERGED, false);
            setMotionState(MotionState.SURFACE);
        }
        if (this.biteGraceTicks > 0) {
            this.biteGraceTicks--;
        }

        if (--this.routeRefreshTicks <= 0 || this.route.isEmpty()) {
            this.routeRefreshTicks = 12;
            List<BlockPos> planned = UncannyHuntingSpecialSystem.findConnectedLavaRoute(
                    level,
                    this.blockPosition(),
                    focus.blockPosition(),
                    HuntingSpecialRules.ASHWALKER_PATH_NODE_LIMIT);
            boolean alreadyClosest = planned != null && planned.size() < 2
                    && Math.hypot(focus.getX() - getX(), focus.getZ() - getZ()) <= 3.0D;
            if (alreadyClosest) {
                // Already at the open lava nearest its prey: holding here is the hunt, not a failure.
                this.pathFailureTicks = 0;
                this.route.clear();
            } else if (planned == null || planned.size() < 2) {
                if (++this.pathFailureTicks >= 120) {
                    beginSinking(focus, "loaded_lava_route_unavailable");
                }
            } else {
                this.pathFailureTicks = 0;
                this.route.clear();
                planned.stream().skip(1).forEach(this.route::addLast);
            }
        }
        moveAlongRoute();

        Vec3 landing = !isSubmerged() && isPotentialShoreTarget(focus)
                ? findLungeLanding(level, focus)
                : null;
        if (landing != null) {
            if (!this.firstBiteCuePlayed) {
                this.firstBiteCuePlayed = true;
                this.biteGraceTicks = HuntingSpecialRules.ASHWALKER_FIRST_BITE_GRACE_MIN_TICKS
                        + this.random.nextInt(HuntingSpecialRules.ASHWALKER_FIRST_BITE_GRACE_MAX_TICKS
                                - HuntingSpecialRules.ASHWALKER_FIRST_BITE_GRACE_MIN_TICKS + 1);
                playPhysicalCue(level, UncannySoundRegistry.UNCANNY_ASHWALKER_CRY.get(), 1.16F, 0.88F);
                boilLava(level, 10);
                trace(focus, "transition", "first_bite_telegraphed", "reaction_ticks", this.biteGraceTicks);
            } else if (this.biteGraceTicks <= 0) {
                if (this.leapTelegraphTicks <= 0) {
                    // Every later leap is still announced by the lava boiling where it will burst.
                    this.leapTelegraphTicks = HuntingSpecialRules.ASHWALKER_LEAP_TELEGRAPH_TICKS;
                    this.pendingLanding = landing;
                    boilLava(level, 6);
                } else if (--this.leapTelegraphTicks <= 0) {
                    beginLunge(level, focus, landing);
                    this.pendingLanding = null;
                }
            }
        } else if (this.leapTelegraphTicks > 0) {
            // The target stepped out of range while the lava boiled: no leap this time.
            this.leapTelegraphTicks = 0;
            this.pendingLanding = null;
        } else if (!isSubmerged()
                && this.distanceToSqr(focus) <= 2.3D * 2.3D
                && this.hasLineOfSight(focus)
                && this.meleeCooldownTicks <= 0) {
            this.doHurtTarget(focus);
            this.meleeCooldownTicks = HuntingSpecialRules.ASHWALKER_BITE_INTERVAL_TICKS;
        }
    }

    private boolean isPotentialShoreTarget(ServerPlayer focus) {
        BlockPos lava = level().getFluidState(blockPosition()).is(FluidTags.LAVA)
                ? blockPosition()
                : blockPosition().below();
        double horizontal = Math.hypot(focus.getX() - this.getX(), focus.getZ() - this.getZ());
        double rise = focus.getY() - (lava.getY() + 1.0D);
        return horizontal <= HuntingSpecialRules.ASHWALKER_MAX_LUNGE_DISTANCE + 1.2D
                && rise >= -0.5D
                && rise <= HuntingSpecialRules.ASHWALKER_MAX_LUNGE_RISE + 0.75D;
    }

    private void beginLunge(ServerLevel level, ServerPlayer focus, Vec3 landing) {
        BlockPos current = level.getFluidState(blockPosition()).is(FluidTags.LAVA)
                ? blockPosition()
                : blockPosition().below();
        this.lavaHome = current.immutable();
        this.lungeTarget = landing;
        this.excursionTicks = 0;
        this.strandedTicks = 0;
        this.route.clear();
        this.setNoGravity(false);
        this.noPhysics = false;
        this.entityData.set(SUBMERGED, false);
        setMotionState(MotionState.LUNGING);
        Vec3 horizontal = landing.subtract(position()).multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = focus.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
        }
        // Burst clear of the lava first: lava drag would halve the leap on its very first tick.
        this.setPos(getX(), current.getY() + 1.05D, getZ());
        double distance = Math.max(0.5D, horizontal.length());
        int flightTicks = HuntingSpecialRules.ashwalkerLeapTicks(distance);
        double rise = Math.max(0.0D, landing.y - getY());
        this.leapHorizontalSpeed = distance / flightTicks;
        this.landBites = 0;
        this.returnStallTicks = 0;
        this.lastReturnDistanceSqr = Double.MAX_VALUE;
        horizontal = horizontal.lengthSqr() < 1.0E-4D
                ? new Vec3(0.0D, 0.0D, this.leapHorizontalSpeed)
                : horizontal.normalize().scale(this.leapHorizontalSpeed);
        this.setDeltaMovement(horizontal.x,
                HuntingSpecialRules.ashwalkerLeapVerticalVelocity(rise, flightTicks), horizontal.z);
        this.hasImpulse = true;
        boilLava(level, 18);
        level.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.LAVA_POP,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.4F, 0.6F);
        trace(focus, "transition", "shore_lunge_started",
                "home", lavaHome.toShortString(),
                "landing", BlockPos.containing(landing).toShortString());
    }

    private void tickExcursion(ServerLevel level, ServerPlayer focus) {
        this.setNoGravity(false);
        this.noPhysics = false;
        this.excursionTicks++;
        if (this.excursionTicks % 20 == 0) {
            trace(focus, "excursion_path", motionState().name().toLowerCase(java.util.Locale.ROOT),
                    "distance_from_lava_home", lavaHome == null
                            ? -1.0D
                            : Math.sqrt(this.distanceToSqr(Vec3.atBottomCenterOf(lavaHome))),
                    "distance_to_target", this.distanceTo(focus),
                    "velocity", this.getDeltaMovement().toString(),
                    "excursion_ticks", this.excursionTicks);
        }
        boolean inLava = level.getFluidState(this.blockPosition()).is(FluidTags.LAVA)
                || level.getFluidState(this.blockPosition().below()).is(FluidTags.LAVA);
        if (motionState() == MotionState.RETURNING && inLava && this.excursionTicks > 4) {
            finishReturn(focus);
            return;
        }
        if (!inLava) {
            this.strandedTicks++;
            if (this.strandedTicks >= HuntingSpecialRules.ASHWALKER_STRANDED_DAMAGE_START_TICKS
                    && (this.strandedTicks - HuntingSpecialRules.ASHWALKER_STRANDED_DAMAGE_START_TICKS) % 20 == 0) {
                this.hurt(level.damageSources().dryOut(), HuntingSpecialRules.ASHWALKER_STRANDED_DAMAGE);
                trace(focus, "limitation", "stranded_damage", "stranded_ticks", strandedTicks);
            }
        } else {
            this.strandedTicks = 0;
        }

        if (motionState() == MotionState.LUNGING) {
            boolean airborne = !this.onGround() && this.excursionTicks < 20;
            if (airborne) {
                steerExcursionToward(lungeTarget, this.leapHorizontalSpeed, true);
            } else if (hasGroundAhead(level, focus.position())) {
                // Landed: run the player down for a moment instead of hopping straight back.
                steerExcursionToward(focus.position(), HuntingSpecialRules.ASHWALKER_LAND_SPEED, true);
            } else {
                // A lava creature never follows its prey off a ledge.
                this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            }
            if (this.excursionTicks >= 4
                    && this.distanceToSqr(focus) <= 2.8D * 2.8D
                    && this.hasLineOfSight(focus)
                    && this.meleeCooldownTicks <= 0) {
                this.doHurtTarget(focus);
                this.meleeCooldownTicks = HuntingSpecialRules.ASHWALKER_BITE_INTERVAL_TICKS;
                this.landBites++;
            }
            boolean lostTarget = !airborne && this.distanceToSqr(focus) > 7.0D * 7.0D;
            if (this.landBites >= HuntingSpecialRules.ASHWALKER_LAND_BITES
                    || lostTarget
                    || this.excursionTicks >= HuntingSpecialRules.ASHWALKER_MAX_LAND_TICKS) {
                setMotionState(MotionState.RETURNING);
                this.excursionTicks = 0;
                trace(focus, "transition", "returning_to_lava", "land_bites", this.landBites);
            }
            return;
        }

        Vec3 home = lavaHome == null ? null : Vec3.atCenterOf(lavaHome).add(0.0D, 0.10D, 0.0D);
        if (home == null || !level.hasChunkAt(lavaHome)) {
            return;
        }
        double homeDistanceSqr = this.position().distanceToSqr(home);
        this.returnStallTicks = homeDistanceSqr < this.lastReturnDistanceSqr - 0.01D ? 0 : this.returnStallTicks + 1;
        this.lastReturnDistanceSqr = Math.min(this.lastReturnDistanceSqr, homeDistanceSqr);
        if (this.returnStallTicks >= HuntingSpecialRules.ASHWALKER_RETURN_STALL_TICKS) {
            // Wedged against a bridge end or a ledge right beside its lava: slip back in rather
            // than drying out on the shore.
            this.noPhysics = true;
            Vec3 slip = home.subtract(position());
            this.setDeltaMovement(slip.length() > 0.3D ? slip.normalize().scale(0.3D) : slip);
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
            return;
        }
        steerExcursionToward(home, 0.25D, false);
    }

    private void steerExcursionToward(Vec3 destination, double horizontalSpeed, boolean allowSingleStepClimb) {
        if (destination == null) {
            return;
        }
        Vec3 horizontal = destination.subtract(position()).multiply(1.0D, 0.0D, 1.0D);
        Vec3 current = getDeltaMovement();
        if (horizontal.lengthSqr() > 0.01D) {
            horizontal = horizontal.normalize().scale(horizontalSpeed);
            double y = current.y;
            if (allowSingleStepClimb
                    && this.horizontalCollision
                    && destination.y - this.getY() <= HuntingSpecialRules.ASHWALKER_MAX_LUNGE_RISE + 0.5D) {
                y = Math.max(y, 0.38D);
            } else if (!allowSingleStepClimb && this.onGround()) {
                y = Math.max(y, 0.34D);
            }
            this.setDeltaMovement(horizontal.x, y, horizontal.z);
            this.setYRot((float) (Math.toDegrees(Math.atan2(horizontal.z, horizontal.x)) - 90.0D));
            this.yBodyRot = this.getYRot();
            this.hasImpulse = true;
        }
    }

    private void finishReturn(ServerPlayer focus) {
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.lungeTarget = null;
        this.excursionTicks = 0;
        this.strandedTicks = 0;
        setMotionState(MotionState.SURFACE);
        trace(focus, "transition", "lava_return_completed");
    }

    private Vec3 findLungeLanding(ServerLevel level, ServerPlayer focus) {
        BlockPos home = level.getFluidState(blockPosition()).is(FluidTags.LAVA)
                ? blockPosition()
                : blockPosition().below();
        for (int radius = 0; radius <= 2; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos feet = focus.blockPosition().offset(dx, 0, dz);
                    int riseAboveLavaSurface = feet.getY() - (home.getY() + 1);
                    if (!level.hasChunkAt(feet)
                            || riseAboveLavaSurface < 0
                            || riseAboveLavaSurface > HuntingSpecialRules.ASHWALKER_MAX_LUNGE_RISE
                            || Math.hypot(
                                            feet.getX() - home.getX(),
                                            feet.getZ() - home.getZ())
                                    > HuntingSpecialRules.ASHWALKER_MAX_LUNGE_DISTANCE) {
                        continue;
                    }
                    BlockPos support = feet.below();
                    if (!level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)
                            || !level.getFluidState(feet).isEmpty()
                            || !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                            || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                        continue;
                    }
                    Vec3 candidate = Vec3.atBottomCenterOf(feet);
                    if (hasClearLungeArc(level, candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private boolean hasClearLungeArc(ServerLevel level, Vec3 destination) {
        Vec3 start = position();
        for (int step = 1; step <= 8; step++) {
            double progress = step / 8.0D;
            double arcHeight = 0.72D + 0.18D * Math.sqrt(start.distanceToSqr(destination));
            Vec3 sample = start.lerp(destination, progress)
                    .add(0.0D, Math.sin(Math.PI * progress) * arcHeight, 0.0D);
            BlockPos samplePos = BlockPos.containing(sample);
            if (!level.hasChunkAt(samplePos)) {
                return false;
            }
            AABB fullBody = new AABB(
                    sample.x - 0.55D,
                    sample.y,
                    sample.z - 0.55D,
                    sample.x + 0.55D,
                    sample.y + 2.20D,
                    sample.z + 0.55D);
            if (!level.noCollision(this, fullBody)) {
                return false;
            }
        }
        return true;
    }

    private void moveAlongRoute() {
        BlockPos next = this.route.peekFirst();
        if (next == null) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 destination = Vec3.atCenterOf(next).add(0.0D, 0.12D, 0.0D);
        Vec3 offset = destination.subtract(this.position());
        if (offset.lengthSqr() <= 0.20D) {
            this.route.removeFirst();
            return;
        }
        double maximumStep = this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.25D;
        Vec3 step = offset.normalize().scale(Math.min(maximumStep, offset.length()));
        this.setDeltaMovement(step);
        this.move(net.minecraft.world.entity.MoverType.SELF, step);
        this.setYRot((float) (Math.toDegrees(Math.atan2(step.z, step.x)) - 90.0D));
        this.yBodyRot = this.getYRot();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean projectile = source.is(DamageTypeTags.IS_PROJECTILE);
        boolean hurt = super.hurt(source, amount);
        if (hurt && projectile && !this.isDeadOrDying()) {
            if (motionState() == MotionState.LUNGING || motionState() == MotionState.RETURNING) {
                setMotionState(MotionState.RETURNING);
                this.excursionTicks = 0;
                if (this.level() instanceof ServerLevel level) {
                    trace(resolveFocus(level), "transition", "projectile_forced_return");
                }
                return true;
            }
            int rolled = HuntingSpecialRules.ASHWALKER_SUBMERGE_MIN_TICKS
                    + this.random.nextInt(HuntingSpecialRules.ASHWALKER_SUBMERGE_MAX_TICKS
                            - HuntingSpecialRules.ASHWALKER_SUBMERGE_MIN_TICKS + 1);
            this.submergedTicks = HuntingSpecialRules.extendAshwalkerSubmergeTicks(rolled, this.submergedTicks);
            this.entityData.set(SUBMERGED, true);
            if (this.level() instanceof ServerLevel level) {
                trace(resolveFocus(level), "transition", "projectile_submerge",
                        "remaining_ticks", this.submergedTicks);
            }
        }
        return hurt;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE) || super.isInvulnerableTo(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AshwalkerSubmergedTicks", this.submergedTicks);
        tag.putInt("AshwalkerBiteGrace", this.biteGraceTicks);
        tag.putInt("AshwalkerPathFailures", this.pathFailureTicks);
        tag.putInt("AshwalkerRouteRefresh", this.routeRefreshTicks);
        tag.putBoolean("AshwalkerFirstBiteCue", this.firstBiteCuePlayed);
        tag.putByte("AshwalkerMotionState", (byte) motionState().id());
        tag.putInt("AshwalkerExcursionTicks", this.excursionTicks);
        tag.putInt("AshwalkerStrandedTicks", this.strandedTicks);
        if (lavaHome != null) {
            tag.putLong("AshwalkerLavaHome", lavaHome.asLong());
        }
        if (lungeTarget != null) {
            tag.putDouble("AshwalkerLungeTargetX", lungeTarget.x);
            tag.putDouble("AshwalkerLungeTargetY", lungeTarget.y);
            tag.putDouble("AshwalkerLungeTargetZ", lungeTarget.z);
        }
        tag.putLongArray("AshwalkerRoute", this.route.stream().mapToLong(BlockPos::asLong).toArray());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.submergedTicks = Math.max(0, tag.getInt("AshwalkerSubmergedTicks"));
        this.biteGraceTicks = Math.max(0, tag.getInt("AshwalkerBiteGrace"));
        this.pathFailureTicks = Math.max(0, tag.getInt("AshwalkerPathFailures"));
        this.routeRefreshTicks = Math.max(0, tag.getInt("AshwalkerRouteRefresh"));
        this.firstBiteCuePlayed = tag.getBoolean("AshwalkerFirstBiteCue");
        setMotionState(tag.contains("AshwalkerMotionState")
                ? MotionState.byId(tag.getByte("AshwalkerMotionState"))
                : this.submergedTicks > 0 ? MotionState.SUBMERGED : MotionState.SURFACE);
        this.entityData.set(SUBMERGED, motionState() == MotionState.SUBMERGED || this.submergedTicks > 0);
        this.excursionTicks = Math.max(0, tag.getInt("AshwalkerExcursionTicks"));
        this.strandedTicks = Math.max(0, tag.getInt("AshwalkerStrandedTicks"));
        this.lavaHome = tag.contains("AshwalkerLavaHome")
                ? BlockPos.of(tag.getLong("AshwalkerLavaHome"))
                : null;
        this.lungeTarget = tag.contains("AshwalkerLungeTargetX")
                ? new Vec3(
                        tag.getDouble("AshwalkerLungeTargetX"),
                        tag.getDouble("AshwalkerLungeTargetY"),
                        tag.getDouble("AshwalkerLungeTargetZ"))
                : null;
        this.route.clear();
        for (long packed : tag.getLongArray("AshwalkerRoute")) {
            this.route.addLast(BlockPos.of(packed));
        }
        boolean excursion = motionState() == MotionState.LUNGING || motionState() == MotionState.RETURNING;
        this.setNoGravity(!excursion);
        this.noPhysics = !excursion;
    }

    private void setMotionState(MotionState state) {
        boolean shouldExpand = state == MotionState.LUNGING || state == MotionState.RETURNING;
        if (this.expandedHitbox != shouldExpand) {
            this.expandedHitbox = shouldExpand;
            this.refreshDimensions();
        }
        this.entityData.set(MOTION_STATE, (byte) state.id());
    }

    /** True when one step towards {@code destination} still has ground at most one block down. */
    private boolean hasGroundAhead(ServerLevel level, Vec3 destination) {
        Vec3 horizontal = destination.subtract(position()).multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            return true;
        }
        Vec3 ahead = position().add(horizontal.normalize().scale(0.9D));
        BlockPos feet = BlockPos.containing(ahead.x, getY() - 0.2D, ahead.z);
        for (int depth = 0; depth <= 1; depth++) {
            BlockPos below = feet.below(depth);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                    || level.getFluidState(below).is(FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
    }

    /** Ticks since the motion state last changed on this client (render smoothing only). */
    public float clientMotionAge(float partialTick) {
        return this.tickCount - this.clientMotionChangeTick + partialTick;
    }

    private void boilLava(ServerLevel level, int count) {
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,
                getX(), getY() + 0.4D, getZ(), count, 0.45D, 0.1D, 0.45D, 0.0D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                getX(), getY() + 0.6D, getZ(), count / 2, 0.4D, 0.2D, 0.4D, 0.01D);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (MOTION_STATE.equals(key)) {
            this.clientMotionChangeTick = this.tickCount;
            // The server already refreshes when it changes state. The client must perform the
            // same refresh when synced data arrives, otherwise F3+B and local collision previews
            // retain the small lava-surface box while the complete body is visibly lunging.
            boolean shouldExpand = motionState() == MotionState.LUNGING
                    || motionState() == MotionState.RETURNING;
            if (this.expandedHitbox != shouldExpand) {
                this.expandedHitbox = shouldExpand;
                this.refreshDimensions();
            }
        }
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.expandedHitbox
                ? EntityDimensions.scalable(1.10F, 2.20F)
                : super.getDefaultDimensions(pose);
    }

    @Override
    protected com.eotv.echoofthevoid.event.special.CombatParityRules.Profile combatParity() {
        return com.eotv.echoofthevoid.event.special.CombatParityRules.ASHWALKER;
    }

    @Override
    protected String specialId() {
        return "ashwalker";
    }

    public enum MotionState {
        SURFACE(0), SUBMERGED(1), LUNGING(2), RETURNING(3);

        private final int id;

        MotionState(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static MotionState byId(int id) {
            return switch (id) {
                case 1 -> SUBMERGED;
                case 2 -> LUNGING;
                case 3 -> RETURNING;
                default -> SURFACE;
            };
        }
    }
}
