package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.event.special.ApprovedSpecialCatalog;
import com.eotv.echoofthevoid.event.special.ApprovedSpecialBehaviorRules;
import com.eotv.echoofthevoid.event.special.ApprovedSpecialSystem;
import com.eotv.echoofthevoid.sound.UncannyPhysicalSoundDelivery;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared server entity for Surveyor?, Mourner?, Doubler?, Ferryman?, Listener? and Bystander?. */
public class UncannyApprovedSpecialEntity extends Monster implements UncannyEntityMarker {
    private static final EntityDataAccessor<Optional<UUID>> FOCUS_PLAYER =
            SynchedEntityData.defineId(UncannyApprovedSpecialEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> FOCUS_BOAT =
            SynchedEntityData.defineId(UncannyApprovedSpecialEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final int MAX_LIFETIME = 20 * 180;
    private static final int MOURNER_TEAR_INTERVAL_TICKS = 16;
    private static final double MOURNER_TEAR_HEIGHT = 1.02D;
    private static final double MOURNER_TEAR_FORWARD_OFFSET = 0.205D;
    private static final double MOURNER_TEAR_EYE_OFFSET = 0.087D;

    private BlockPos anchor;
    private int lifetime;
    private int state;
    private int stateTicks;
    private int quietTicks;
    private int stuckTicks;
    private int copiedActions;
    private int failedActions;
    private int mirrorErrorTick;
    private int attacksDelivered;
    private int nextSoundTick;
    private int dedicatedSoundCuesPlayed;
    private boolean mournerAudibleCuePlayed;
    private boolean ferrymanRevealStarted;
    private Vec3 ferrymanSceneTarget;
    private Vec3 ferrymanDepartureDirection = Vec3.ZERO;
    private Vec3 lastFerrymanBoatPosition;
    private long lastSoundMemoryTick;
    private Vec3 lastPosition = Vec3.ZERO;
    private Vec3 surveyorInspectionTarget;
    private Vec3 doublerPlaneNormal = Vec3.ZERO;
    private Vec3 lastDoublerFocusPosition;
    private boolean surveyorFleeing;
    private final ArrayDeque<MirrorSample> mirrorSamples = new ArrayDeque<>();

    public UncannyApprovedSpecialEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        UncannyEntityUtil.applyDisplayName(this, kind().displayName());
        this.xpReward = 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FOCUS_PLAYER, Optional.empty());
        builder.define(FOCUS_BOAT, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this) {
            @Override
            public boolean canUse() {
                return !UncannyApprovedSpecialEntity.this.isFerryman() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !UncannyApprovedSpecialEntity.this.isFerryman() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public void setup(ServerPlayer focus, BlockPos anchor) {
        setup(focus, anchor, null);
    }

    public void setup(ServerPlayer focus, BlockPos anchor, UUID relatedEntityId) {
        this.entityData.set(FOCUS_PLAYER, Optional.of(focus.getUUID()));
        this.entityData.set(FOCUS_BOAT, Optional.ofNullable(relatedEntityId));
        this.anchor = anchor == null ? focus.blockPosition() : anchor.immutable();
        this.lifetime = 0;
        this.state = 0;
        this.stateTicks = 0;
        this.quietTicks = 0;
        this.stuckTicks = 0;
        this.copiedActions = 0;
        this.failedActions = 0;
        this.mirrorErrorTick = 180 + this.random.nextInt(181);
        this.attacksDelivered = 0;
        this.nextSoundTick = switch (kind().id()) {
            case "mourner" -> 8 + this.random.nextInt(18);
            case "ferryman" -> 24 + this.random.nextInt(28);
            default -> 55 + this.random.nextInt(80);
        };
        this.dedicatedSoundCuesPlayed = 0;
        this.mournerAudibleCuePlayed = false;
        this.ferrymanRevealStarted = false;
        this.ferrymanSceneTarget = null;
        this.ferrymanDepartureDirection = Vec3.ZERO;
        this.lastFerrymanBoatPosition = null;
        this.lastSoundMemoryTick = Long.MIN_VALUE;
        this.lastPosition = this.position();
        this.surveyorInspectionTarget = null;
        this.surveyorFleeing = false;
        this.doublerPlaneNormal = Vec3.ZERO;
        this.lastDoublerFocusPosition = "doubler".equals(kind().id()) ? focus.position() : null;
        if ("surveyor".equals(kind().id()) && this.level() instanceof ServerLevel level) {
            this.surveyorInspectionTarget = ApprovedSpecialSystem.findSurveyorStandPosition(
                    level, this.anchor, this.position());
        }
        if ("doubler".equals(kind().id())) {
            ensureDoublerPlaneNormal(focus);
        }
        this.setSilent(true);
        this.setNoGravity(this.isFerryman());
        this.setPersistenceRequired();
    }

    public String specialId() {
        return kind().id();
    }

    public int copiedActions() {
        return copiedActions;
    }

    public int failedActions() {
        return failedActions;
    }

    public Optional<UUID> focusedBoatId() {
        return this.entityData.get(FOCUS_BOAT);
    }

    public boolean hasPlayedMournerCueInRange() {
        return this.mournerAudibleCuePlayed;
    }

    public boolean hasStartedFerrymanReveal() {
        return this.ferrymanRevealStarted;
    }

    public int dedicatedSoundCuesPlayed() {
        return this.dedicatedSoundCuesPlayed;
    }

    public boolean isSurveyorFleeing() {
        return this.surveyorFleeing;
    }

    public Vec3 surveyorInspectionTarget() {
        return this.surveyorInspectionTarget;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.setSilent(true);
        if (this.level().isClientSide()) {
            tickMournerTears();
            return;
        }
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer focus = resolveFocus(level);
        if (focus == null || ++this.lifetime > MAX_LIFETIME) {
            beginSinking();
        }
        if (this.state == 99) {
            tickSinking();
            return;
        }
        if (focus == null) {
            return;
        }
        if (this.isPassenger()) {
            this.stopRiding();
        }

        switch (kind().id()) {
            case "surveyor" -> tickSurveyor(focus);
            case "mourner" -> tickMourner(focus);
            case "doubler" -> tickDoubler(focus);
            case "ferryman" -> tickFerryman(focus);
            case "listener" -> tickListener(level, focus);
            case "bystander" -> tickBystander(level, focus);
            default -> beginSinking();
        }
        detectPermanentObstruction();
    }

    private void tickMournerTears() {
        if (!"mourner".equals(kind().id())
                || this.isInvisible()
                || this.tickCount % MOURNER_TEAR_INTERVAL_TICKS != 0) {
            return;
        }

        float yawRadians = (float) Math.toRadians(this.getYHeadRot());
        Vec3 forward = new Vec3(-Mth.sin(yawRadians), 0.0D, Mth.cos(yawRadians));
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        Vec3 eyeCenter = this.position()
                .add(0.0D, MOURNER_TEAR_HEIGHT, 0.0D)
                .add(forward.scale(MOURNER_TEAR_FORWARD_OFFSET));

        spawnMournerTear(eyeCenter.add(right.scale(MOURNER_TEAR_EYE_OFFSET)));
        spawnMournerTear(eyeCenter.add(right.scale(-MOURNER_TEAR_EYE_OFFSET)));
    }

    private void spawnMournerTear(Vec3 origin) {
        this.level().addParticle(
                ParticleTypes.FALLING_WATER,
                origin.x,
                origin.y,
                origin.z,
                0.0D,
                -0.012D,
                0.0D);
    }

    private void tickSurveyor(ServerPlayer focus) {
        if (this.state == 3) {
            tickSurveyorFlee(focus);
            return;
        }

        if (this.distanceToSqr(focus) <= 8.0D * 8.0D
                && isFocusLookingAt(focus, 0.91D)
                && hasCompletelyOpenLineFrom(focus)) {
            this.state = 3;
            this.stateTicks = 0;
            this.surveyorFleeing = true;
            tickSurveyorFlee(focus);
            return;
        }

        if (this.surveyorInspectionTarget == null && this.level() instanceof ServerLevel level) {
            this.surveyorInspectionTarget = ApprovedSpecialSystem.findSurveyorStandPosition(
                    level, this.anchor, this.position());
        }
        if (this.surveyorInspectionTarget == null || this.anchor == null) {
            beginSinking();
            return;
        }

        Vec3 feature = Vec3.atCenterOf(this.anchor);
        this.getLookControl().setLookAt(feature.x, feature.y, feature.z, 55.0F, 40.0F);
        double targetDistanceSqr = this.position().distanceToSqr(this.surveyorInspectionTarget);
        if (targetDistanceSqr > 2.0D * 2.0D) {
            this.getNavigation().moveTo(
                    this.surveyorInspectionTarget.x,
                    this.surveyorInspectionTarget.y,
                    this.surveyorInspectionTarget.z,
                    0.96D);
        } else if (targetDistanceSqr > 0.30D * 0.30D) {
            // Ground navigation considers a path complete before the mob visually hugs a wall.
            // Finish only the last short, collision-checked segment so Surveyor? reaches the
            // selected opening without gaining phasing or bypassing an obstructed route.
            this.getNavigation().stop();
            Vec3 offset = this.surveyorInspectionTarget.subtract(this.position());
            Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
            if (horizontal.lengthSqr() > 0.0001D) {
                this.move(MoverType.SELF, horizontal.normalize().scale(Math.min(0.11D, horizontal.length())));
                this.hasImpulse = true;
            }
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        } else {
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        }
    }

    private void tickSurveyorFlee(ServerPlayer focus) {
        this.stateTicks++;
        Vec3 away = this.position().subtract(focus.position());
        away = new Vec3(away.x, 0.0D, away.z);
        if (away.lengthSqr() < 0.01D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 destination = this.position().add(away.normalize().scale(14.0D));
        this.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.22D);
        this.lookAt(focus, 70.0F, 45.0F);
        if (this.distanceToSqr(focus) >= 18.0D * 18.0D || this.stateTicks >= 80) {
            beginSinking(38);
        }
    }

    private void tickMourner(ServerPlayer focus) {
        this.getNavigation().stop();
        this.setShiftKeyDown(true);
        if (this.state == 0) {
            this.getLookControl().setLookAt(this.getX(), this.getY() - 1.0D, this.getZ(), 20.0F, 20.0F);
            boolean focusInAudibleRange = focus.distanceToSqr(this)
                    <= ApprovedSpecialBehaviorRules.MOURNER_AUDIBLE_RANGE
                            * ApprovedSpecialBehaviorRules.MOURNER_AUDIBLE_RANGE;
            boolean cueDue = this.lifetime >= this.nextSoundTick
                    || (focusInAudibleRange && !this.mournerAudibleCuePlayed);
            if (cueDue && this.level() instanceof ServerLevel level) {
                UncannyPhysicalSoundDelivery.playFromEntity(
                        level,
                        this,
                        UncannySoundRegistry.UNCANNY_MOURNER_SOB.get(),
                        SoundSource.HOSTILE,
                        ApprovedSpecialBehaviorRules.MOURNER_SOB_VOLUME,
                        0.70F + this.random.nextFloat() * 0.10F);
                this.dedicatedSoundCuesPlayed++;
                if (focusInAudibleRange) {
                    this.mournerAudibleCuePlayed = true;
                }
                this.nextSoundTick = this.lifetime
                        + ApprovedSpecialBehaviorRules.mournerSobIntervalTicks(this.random.nextInt(111));
            }
            boolean observed = this.lifetime >= ApprovedSpecialBehaviorRules.MOURNER_MIN_OBSERVATION_TICKS
                    && this.mournerAudibleCuePlayed
                    && focus.distanceToSqr(this) <= 14.0D * 14.0D
                    && focus.hasLineOfSight(this)
                    && isFocusLookingAt(focus, 0.94D);
            this.quietTicks = observed ? this.quietTicks + 1 : 0;
            if (this.quietTicks >= ApprovedSpecialBehaviorRules.MOURNER_REQUIRED_GAZE_TICKS) {
                this.state = 1;
                this.stateTicks = 0;
                this.quietTicks = 0;
            }
            return;
        }
        this.stateTicks++;
        this.lookAt(focus, Math.min(55.0F, 4.0F + this.stateTicks * 0.8F), 22.0F);
        if (this.stateTicks >= ApprovedSpecialBehaviorRules.MOURNER_ACKNOWLEDGEMENT_TICKS) {
            beginSinking(ApprovedSpecialBehaviorRules.MOURNER_SINK_TICKS);
        }
    }

    private void tickDoubler(ServerPlayer focus) {
        if (this.state == 2) {
            tickDoublerAttack(focus);
            return;
        }
        ensureDoublerPlaneNormal(focus);
        if (hasCrossedDoublerSeparation(focus)) {
            armDoublerAttack(focus);
            return;
        }
        this.getNavigation().stop();
        Vec3 currentFocusPosition = focus.position();
        Vec3 motion = this.lastDoublerFocusPosition == null
                ? Vec3.ZERO
                : currentFocusPosition.subtract(this.lastDoublerFocusPosition);
        this.lastDoublerFocusPosition = currentFocusPosition;
        mirrorSamples.addLast(new MirrorSample(
                motion, focus.isShiftKeyDown(), focus.isSprinting(), focus.swinging, focus.getYRot()));
        while (mirrorSamples.size() > 4) {
            MirrorSample sample = mirrorSamples.removeFirst();
            boolean deliberateError = this.lifetime == this.mirrorErrorTick;
            this.setShiftKeyDown(deliberateError ? !sample.crouching() : sample.crouching());
            this.setSprinting(sample.sprinting());
            Vec3 playerFacing = Vec3.directionFromRotation(0.0F, sample.yaw());
            Vec3 mirroredFacing = mirrorDoublerVector(playerFacing);
            float mirroredYaw = (float) (Mth.atan2(-mirroredFacing.x, mirroredFacing.z)
                    * (180.0D / Math.PI));
            this.setYRot(mirroredYaw);
            this.setYHeadRot(this.getYRot());
            if (sample.swinging()) {
                this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            if (sample.motion().horizontalDistanceSqr() > 0.0002D) {
                Vec3 mirrored = mirrorDoublerVector(sample.motion());
                if (deliberateError) {
                    mirrored = mirrored.yRot(0.45F);
                    failedActions++;
                } else {
                    copiedActions++;
                }
                Vec3 copiedMotion = new Vec3(
                        mirrored.x * 0.92D,
                        Mth.clamp(mirrored.y, -0.42D, 0.42D),
                        mirrored.z * 0.92D);
                this.move(MoverType.SELF, copiedMotion);
                this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
                this.hasImpulse = true;
            }
        }
        if (focus.distanceToSqr(this) > 30.0D * 30.0D) {
            beginSinking();
        }
    }

    private void armDoublerAttack(ServerPlayer focus) {
        if (this.state == 2) {
            return;
        }
        this.state = 2;
        this.stateTicks = 0;
        this.getNavigation().stop();
        this.lookAt(focus, 90.0F, 70.0F);
        if (this.level() instanceof ServerLevel level) {
            UncannyPhysicalSoundDelivery.playFromEntity(
                    level, this, SoundEvents.ARMOR_STAND_HIT, SoundSource.HOSTILE, 0.42F, 0.55F);
        }
    }

    private void ensureDoublerPlaneNormal(ServerPlayer focus) {
        if (this.doublerPlaneNormal.horizontalDistanceSqr() >= 0.5D || this.anchor == null) {
            return;
        }
        Vec3 normal = this.position().subtract(Vec3.atCenterOf(this.anchor));
        normal = new Vec3(normal.x, 0.0D, normal.z);
        if (normal.lengthSqr() < 0.01D) {
            normal = Vec3.atCenterOf(this.anchor).subtract(focus.position());
            normal = new Vec3(normal.x, 0.0D, normal.z);
        }
        this.doublerPlaneNormal = normal.lengthSqr() < 0.01D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : normal.normalize();
    }

    private Vec3 mirrorDoublerVector(Vec3 vector) {
        ApprovedSpecialBehaviorRules.MirroredMotion mirrored =
                ApprovedSpecialBehaviorRules.mirrorAcrossHorizontalPlane(
                        vector.x,
                        vector.y,
                        vector.z,
                        this.doublerPlaneNormal.x,
                        this.doublerPlaneNormal.z);
        return new Vec3(mirrored.x(), mirrored.y(), mirrored.z());
    }

    private boolean hasCrossedDoublerSeparation(ServerPlayer focus) {
        if (this.anchor == null || this.doublerPlaneNormal.horizontalDistanceSqr() < 0.5D
                || focus.distanceToSqr(this) > 12.0D * 12.0D) {
            return false;
        }
        Vec3 fromPlane = focus.position().subtract(Vec3.atCenterOf(this.anchor));
        return fromPlane.dot(this.doublerPlaneNormal) >= 0.45D;
    }

    private void tickDoublerAttack(ServerPlayer focus) {
        this.stateTicks++;
        this.lookAt(focus, 100.0F, 80.0F);
        if (this.stateTicks < 22) {
            this.getNavigation().stop();
            return;
        }
        if (this.distanceToSqr(focus) > 15.0D * 15.0D) {
            beginSinking();
            return;
        }
        this.getNavigation().moveTo(focus, 1.08D);
        if (this.distanceToSqr(focus) <= 2.1D * 2.1D && this.stateTicks % 24 == 0) {
            if (this.doHurtTarget(focus)) {
                attacksDelivered++;
            }
            if (attacksDelivered >= 3) {
                beginSinking();
            }
        }
    }

    private void tickFerryman(ServerPlayer focus) {
        Boat boat = resolveFerrymanBoat(focus);
        this.getNavigation().stop();
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
        if (!(this.level() instanceof ServerLevel level) || boat == null) {
            this.setPos(this.getX(), this.getY() - 0.025D, this.getZ());
            if (++this.quietTicks > ApprovedSpecialBehaviorRules.FERRYMAN_MISSING_BOAT_RETIRE_TICKS) {
                beginSinking();
            }
            return;
        }
        if (!ApprovedSpecialSystem.isEligibleFerrymanBoat(level, boat)) {
            beginSinking(42);
            return;
        }

        Vec3 boatMotion = boat.getDeltaMovement();
        Vec3 boatPositionDelta = this.lastFerrymanBoatPosition == null
                ? Vec3.ZERO
                : boat.position().subtract(this.lastFerrymanBoatPosition);
        this.lastFerrymanBoatPosition = boat.position();
        // The boat velocity can retain small horizontal impulses while the hull is visibly
        // stationary (notably with a passenger and water drag).  The scene is about what the
        // player can observe, so use the actual inter-tick displacement as the source of truth.
        boolean boatIsMoving = ApprovedSpecialBehaviorRules.ferrymanBoatIsMoving(
                boatPositionDelta.x, boatPositionDelta.z);
        if (this.state == 1) {
            tickFerrymanReveal(level, focus, boat);
            return;
        }
        if (this.state == 2) {
            tickFerrymanDeparture(level, focus, boat);
            return;
        }
        if (boatIsMoving) {
            this.quietTicks = 0;
            Vec3 horizontalMotion = new Vec3(boatPositionDelta.x, 0.0D, boatPositionDelta.z);
            Vec3 desired = boat.position()
                    .subtract(horizontalMotion.normalize().scale(ApprovedSpecialBehaviorRules.FERRYMAN_TRAILING_DISTANCE))
                    .add(0.0D, ApprovedSpecialBehaviorRules.FERRYMAN_VERTICAL_OFFSET, 0.0D);
            if (!ApprovedSpecialSystem.isSafeFerrymanFollowPosition(level, desired)) {
                desired = boat.position().add(0.0D, ApprovedSpecialBehaviorRules.FERRYMAN_VERTICAL_OFFSET, 0.0D);
            }
            moveFerrymanToward(level, boat, desired, boatMotion);
            this.lookAt(focus, 30.0F, 30.0F);
            if (this.lifetime % 18 == 0) {
                level.sendParticles(
                        ParticleTypes.BUBBLE,
                        this.getX(), this.getEyeY(), this.getZ(),
                        4, 0.28D, 0.24D, 0.28D, 0.025D);
            }
            if (this.lifetime >= this.nextSoundTick) {
                UncannyPhysicalSoundDelivery.playFromEntity(
                        level,
                        this,
                        UncannySoundRegistry.UNCANNY_FERRYMAN_WAKE.get(),
                        SoundSource.HOSTILE,
                        ApprovedSpecialBehaviorRules.FERRYMAN_WAKE_VOLUME,
                        0.80F + this.random.nextFloat() * 0.09F);
                this.dedicatedSoundCuesPlayed++;
                this.nextSoundTick = this.lifetime
                        + ApprovedSpecialBehaviorRules.ferrymanWakeIntervalTicks(this.random.nextInt(181));
            }
        } else if (++this.quietTicks >= ApprovedSpecialBehaviorRules.FERRYMAN_IDLE_RISE_DELAY_TICKS) {
            beginFerrymanReveal(level, boat);
        }
    }

    private void beginFerrymanReveal(ServerLevel level, Boat boat) {
        Vec3 away = this.position().subtract(boat.position());
        away = new Vec3(away.x, 0.0D, away.z);
        if (away.lengthSqr() < 0.01D) {
            away = Vec3.directionFromRotation(0.0F, boat.getYRot()).scale(-1.0D);
        }
        away = away.normalize();
        Vec3 target = findFerrymanRevealPosition(level, boat, away);
        if (target == null) {
            beginSinking(42);
            return;
        }

        this.state = 1;
        this.stateTicks = 0;
        this.quietTicks = 0;
        this.ferrymanRevealStarted = true;
        this.ferrymanSceneTarget = target;
        this.ferrymanDepartureDirection = new Vec3(
                target.x - boat.getX(), 0.0D, target.z - boat.getZ()).normalize();
        UncannyPhysicalSoundDelivery.playFromEntity(
                level,
                this,
                UncannySoundRegistry.UNCANNY_FERRYMAN_WAKE.get(),
                SoundSource.HOSTILE,
                ApprovedSpecialBehaviorRules.FERRYMAN_WAKE_VOLUME,
                0.76F + this.random.nextFloat() * 0.08F);
        this.dedicatedSoundCuesPlayed++;
        level.sendParticles(
                ParticleTypes.BUBBLE,
                target.x, target.y + 0.6D, target.z,
                10, 0.42D, 0.35D, 0.42D, 0.035D);
    }

    private void tickFerrymanReveal(ServerLevel level, ServerPlayer focus, Boat boat) {
        this.stateTicks++;
        if (this.ferrymanSceneTarget == null) {
            Vec3 away = this.position().subtract(boat.position());
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.lengthSqr() < 0.01D) {
                away = Vec3.directionFromRotation(0.0F, boat.getYRot()).scale(-1.0D);
            }
            this.ferrymanSceneTarget = findFerrymanRevealPosition(level, boat, away.normalize());
            if (this.ferrymanSceneTarget == null) {
                beginSinking(42);
                return;
            }
        }

        Vec3 offset = this.ferrymanSceneTarget.subtract(this.position());
        if (offset.lengthSqr() <= 0.14D) {
            this.setPos(this.ferrymanSceneTarget.x, this.ferrymanSceneTarget.y, this.ferrymanSceneTarget.z);
            this.state = 2;
            this.stateTicks = 0;
            this.lookAt(focus, 55.0F, 35.0F);
            level.sendParticles(
                    ParticleTypes.SPLASH,
                    this.getX(), this.getY() + 1.0D, this.getZ(),
                    12, 0.55D, 0.20D, 0.55D, 0.08D);
            return;
        }

        Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
        if (horizontal.horizontalDistance() > ApprovedSpecialBehaviorRules.FERRYMAN_REVEAL_MAX_HORIZONTAL_STEP) {
            horizontal = horizontal.normalize().scale(
                    ApprovedSpecialBehaviorRules.FERRYMAN_REVEAL_MAX_HORIZONTAL_STEP);
        }
        double vertical = Mth.clamp(
                offset.y,
                -ApprovedSpecialBehaviorRules.FERRYMAN_REVEAL_MAX_VERTICAL_STEP,
                ApprovedSpecialBehaviorRules.FERRYMAN_REVEAL_MAX_VERTICAL_STEP);
        Vec3 candidate = this.position().add(horizontal.x, vertical, horizontal.z);
        if (ApprovedSpecialSystem.isSafeFerrymanRevealPosition(level, candidate)
                && isCollisionFreeAt(level, candidate)) {
            this.setPos(candidate.x, candidate.y, candidate.z);
            this.hasImpulse = true;
        } else if (this.stateTicks >= ApprovedSpecialBehaviorRules.FERRYMAN_REVEAL_TIMEOUT_TICKS) {
            beginSinking(42);
            return;
        }
        if (this.stateTicks % 7 == 0) {
            level.sendParticles(
                    ParticleTypes.BUBBLE,
                    this.getX(), this.getEyeY(), this.getZ(),
                    5, 0.30D, 0.32D, 0.30D, 0.030D);
        }
    }

    private void tickFerrymanDeparture(ServerLevel level, ServerPlayer focus, Boat boat) {
        this.stateTicks++;
        this.lookAt(focus, 42.0F, 30.0F);
        if (this.stateTicks <= ApprovedSpecialBehaviorRules.FERRYMAN_REVEAL_HOLD_TICKS) {
            // Water movement is applied by the Vanilla mob tick before this scene tick.
            // Re-anchor the hold so the entity cannot visibly sink while watching the boat.
            if (this.ferrymanSceneTarget != null) {
                this.setPos(
                        this.ferrymanSceneTarget.x,
                        this.ferrymanSceneTarget.y,
                        this.ferrymanSceneTarget.z);
            }
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        Vec3 direction = this.ferrymanDepartureDirection;
        if (direction.horizontalDistanceSqr() < 0.01D) {
            direction = this.position().subtract(boat.position());
            direction = new Vec3(direction.x, 0.0D, direction.z);
            direction = direction.lengthSqr() < 0.01D ? new Vec3(1.0D, 0.0D, 0.0D) : direction.normalize();
            this.ferrymanDepartureDirection = direction;
        }
        int departureAge = this.stateTicks - ApprovedSpecialBehaviorRules.FERRYMAN_REVEAL_HOLD_TICKS;
        double downward = departureAge > 24 ? -0.018D : 0.0D;
        Vec3 candidate = this.position()
                .add(direction.scale(ApprovedSpecialBehaviorRules.FERRYMAN_DEPARTURE_STEP))
                .add(0.0D, downward, 0.0D);
        if ((ApprovedSpecialSystem.isSafeFerrymanRevealPosition(level, candidate)
                        || ApprovedSpecialSystem.isSafeFerrymanFollowPosition(level, candidate))
                && isCollisionFreeAt(level, candidate)) {
            this.setPos(candidate.x, candidate.y, candidate.z);
            this.hasImpulse = true;
        } else {
            beginSinking(42);
            return;
        }
        if (departureAge % 10 == 0) {
            level.sendParticles(
                    ParticleTypes.BUBBLE,
                    this.getX(), this.getEyeY(), this.getZ(),
                    4, 0.25D, 0.22D, 0.25D, 0.025D);
        }
        if (departureAge >= ApprovedSpecialBehaviorRules.FERRYMAN_DEPARTURE_TICKS) {
            beginSinking(42);
        }
    }

    private Vec3 findFerrymanRevealPosition(ServerLevel level, Boat boat, Vec3 preferredDirection) {
        double[] radii = {ApprovedSpecialBehaviorRules.FERRYMAN_REVEAL_DISTANCE, 4.5D, 3.5D, 2.8D};
        float[] rotations = {0.0F, 0.52F, -0.52F, 1.05F, -1.05F, (float) Math.PI};
        for (double radius : radii) {
            for (float rotation : rotations) {
                Vec3 direction = preferredDirection.yRot(rotation);
                Vec3 candidate = new Vec3(
                        boat.getX() + direction.x * radius,
                        boat.getY() + ApprovedSpecialBehaviorRules.FERRYMAN_REVEAL_FEET_Y_OFFSET,
                        boat.getZ() + direction.z * radius);
                if (ApprovedSpecialSystem.isSafeFerrymanRevealPosition(level, candidate)
                        && isCollisionFreeAt(level, candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean isCollisionFreeAt(ServerLevel level, Vec3 candidate) {
        return level.noCollision(this, this.getBoundingBox().move(candidate.subtract(this.position())));
    }

    private void moveFerrymanToward(ServerLevel level, Boat boat, Vec3 desired, Vec3 boatMotion) {
        Vec3 offset = desired.subtract(this.position());
        Vec3 horizontalStep = new Vec3(
                offset.x * 0.36D + boatMotion.x * 0.52D,
                0.0D,
                offset.z * 0.36D + boatMotion.z * 0.52D);
        if (horizontalStep.horizontalDistance() > ApprovedSpecialBehaviorRules.FERRYMAN_MAX_HORIZONTAL_STEP) {
            horizontalStep = horizontalStep.normalize().scale(ApprovedSpecialBehaviorRules.FERRYMAN_MAX_HORIZONTAL_STEP);
        }
        double nextY = this.getY() + Mth.clamp(
                offset.y * 0.55D,
                -ApprovedSpecialBehaviorRules.FERRYMAN_MAX_VERTICAL_STEP,
                ApprovedSpecialBehaviorRules.FERRYMAN_MAX_VERTICAL_STEP);
        nextY = Math.min(nextY, boat.getY() + ApprovedSpecialBehaviorRules.FERRYMAN_MAX_FEET_Y_OFFSET);
        Vec3 candidate = new Vec3(
                this.getX() + horizontalStep.x,
                nextY,
                this.getZ() + horizontalStep.z);
        if (!ApprovedSpecialSystem.isSafeFerrymanFollowPosition(level, candidate)) {
            candidate = desired;
        }
        if (ApprovedSpecialSystem.isSafeFerrymanFollowPosition(level, candidate)) {
            this.setPos(candidate.x, candidate.y, candidate.z);
            this.setDeltaMovement(Vec3.ZERO);
            this.hasImpulse = true;
        }
    }

    private Boat resolveFerrymanBoat(ServerPlayer focus) {
        if (this.level() instanceof ServerLevel level) {
            Boat stored = this.entityData.get(FOCUS_BOAT)
                    .map(level::getEntity)
                    .filter(Boat.class::isInstance)
                    .map(Boat.class::cast)
                    .filter(Entity::isAlive)
                    .orElse(null);
            if (stored != null) {
                return stored;
            }
        }
        Entity vehicle = focus.getVehicle();
        if (vehicle instanceof Boat boat && boat.isAlive()) {
            this.entityData.set(FOCUS_BOAT, Optional.of(boat.getUUID()));
            return boat;
        }
        return null;
    }

    private void tickListener(ServerLevel level, ServerPlayer focus) {
        ApprovedSpecialSystem.SoundMemory memory =
                ApprovedSpecialSystem.latestPhysicalSound(level, this.position(), this.lastSoundMemoryTick, 34.0D);
        if (memory != null) {
            this.lastSoundMemoryTick = memory.tick();
            this.quietTicks = 0;
            this.getNavigation().moveTo(memory.position().x, memory.position().y, memory.position().z, 0.88D);
            this.getLookControl().setLookAt(memory.position().x, memory.position().y, memory.position().z, 55.0F, 40.0F);
        } else if (++this.quietTicks > 160 + Math.floorMod(this.getId(), 81)) {
            beginSinking();
        }
        if (this.distanceToSqr(focus) < 4.0D * 4.0D) {
            Vec3 away = this.position().subtract(focus.position()).normalize();
            Vec3 destination = this.position().add(away.scale(7.0D));
            this.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.0D);
        }
    }

    private void tickBystander(ServerLevel level, ServerPlayer focus) {
        ApprovedSpecialSystem.CombatMemory combat =
                ApprovedSpecialSystem.latestCombat(level, this.position(), 36.0D);
        if (combat == null || level.getGameTime() - combat.tick() > 80L) {
            if (++this.quietTicks > 70) {
                beginSinking();
            }
            return;
        }
        this.quietTicks = 0;
        // Bystander? watches whoever delivered the latest real blow, not its victim.
        Entity observed = level.getEntity(combat.attackerEntityId());
        if (observed != null) {
            this.getLookControl().setLookAt(observed, 90.0F, 75.0F);
        }
        if (this.distanceToSqr(focus) < 6.0D * 6.0D) {
            Vec3 away = this.position().subtract(focus.position());
            away = away.lengthSqr() < 0.01D ? new Vec3(1.0D, 0.0D, 0.0D) : away.normalize();
            Vec3 destination = this.position().add(away.scale(10.0D));
            this.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.14D);
        } else {
            this.getNavigation().stop();
        }
    }

    private void detectPermanentObstruction() {
        if (this.state == 99
                || "mourner".equals(kind().id())
                || "doubler".equals(kind().id())
                || "ferryman".equals(kind().id())) {
            return;
        }
        if (this.position().distanceToSqr(this.lastPosition) < 0.0004D && !this.getNavigation().isDone()) {
            stuckTicks++;
            if (stuckTicks > 100) {
                beginSinking();
            }
        } else {
            stuckTicks = 0;
            lastPosition = this.position();
        }
    }

    private void beginSinking() {
        beginSinking(34);
    }

    private void beginSinking(int durationTicks) {
        if (this.state == 99) {
            return;
        }
        this.state = 99;
        this.stateTicks = Math.max(1, durationTicks);
        this.getNavigation().stop();
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setTarget(null);
    }

    private void tickSinking() {
        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(this.getX(), this.getY() - 0.065D, this.getZ());
        if (--this.stateTicks <= 0) {
            this.discard();
        }
    }

    private ApprovedSpecialCatalog.Definition kind() {
        String path = this.getType().builtInRegistryHolder().key().location().getPath();
        String id = path.startsWith("uncanny_") ? path.substring("uncanny_".length()) : path;
        ApprovedSpecialCatalog.Definition definition = ApprovedSpecialCatalog.byId(id);
        return definition == null ? ApprovedSpecialCatalog.byId("surveyor") : definition;
    }

    private boolean isFerryman() {
        String path = this.getType().builtInRegistryHolder().key().location().getPath();
        return "uncanny_ferryman".equals(path);
    }

    private ServerPlayer resolveFocus(ServerLevel level) {
        Optional<UUID> uuid = this.entityData.get(FOCUS_PLAYER);
        if (uuid.isPresent()) {
            ServerPlayer bound = level.getServer().getPlayerList().getPlayer(uuid.get());
            return bound != null
                            && bound.isAlive()
                            && !bound.isSpectator()
                            && bound.serverLevel() == level
                    ? bound
                    : null;
        }
        ServerPlayer nearest = level.getNearestPlayer(this, 96.0D) instanceof ServerPlayer candidate ? candidate : null;
        if (nearest != null && nearest.isAlive() && !nearest.isSpectator()) {
            this.entityData.set(FOCUS_PLAYER, Optional.of(nearest.getUUID()));
            return nearest;
        }
        return null;
    }

    private boolean isFocusLookingAt(ServerPlayer focus, double threshold) {
        Vec3 toEntity = this.getEyePosition().subtract(focus.getEyePosition());
        if (toEntity.lengthSqr() < 0.0001D) {
            return true;
        }
        return focus.getViewVector(1.0F).normalize().dot(toEntity.normalize()) >= threshold;
    }

    private boolean hasCompletelyOpenLineFrom(ServerPlayer focus) {
        BlockHitResult hit = focus.serverLevel().clip(new ClipContext(
                focus.getEyePosition(),
                this.getEyePosition(),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                focus));
        return hit.getType() == HitResult.Type.MISS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if ("doubler".equals(kind().id())) {
            if (!this.level().isClientSide() && source.getEntity() instanceof ServerPlayer player) {
                armDoublerAttack(player);
            }
            return super.hurt(source, amount);
        }
        if (!this.level().isClientSide()) {
            beginSinking();
        }
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !"doubler".equals(kind().id()) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean canBeHitByProjectile() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(FOCUS_PLAYER).ifPresent(uuid -> tag.putUUID("FocusPlayer", uuid));
        this.entityData.get(FOCUS_BOAT).ifPresent(uuid -> tag.putUUID("FocusBoat", uuid));
        if (anchor != null) {
            tag.putLong("Anchor", anchor.asLong());
        }
        tag.putInt("Lifetime", lifetime);
        tag.putInt("State", state);
        tag.putInt("StateTicks", stateTicks);
        tag.putInt("QuietTicks", quietTicks);
        tag.putInt("MirrorErrorTick", mirrorErrorTick);
        tag.putInt("CopiedActions", copiedActions);
        tag.putInt("FailedActions", failedActions);
        tag.putInt("AttacksDelivered", attacksDelivered);
        tag.putInt("NextSoundTick", nextSoundTick);
        tag.putInt("DedicatedSoundCuesPlayed", dedicatedSoundCuesPlayed);
        tag.putBoolean("MournerAudibleCuePlayed", mournerAudibleCuePlayed);
        tag.putBoolean("FerrymanRevealStarted", ferrymanRevealStarted);
        if (ferrymanSceneTarget != null) {
            tag.putDouble("FerrymanSceneTargetX", ferrymanSceneTarget.x);
            tag.putDouble("FerrymanSceneTargetY", ferrymanSceneTarget.y);
            tag.putDouble("FerrymanSceneTargetZ", ferrymanSceneTarget.z);
        }
        tag.putDouble("FerrymanDepartureX", ferrymanDepartureDirection.x);
        tag.putDouble("FerrymanDepartureZ", ferrymanDepartureDirection.z);
        tag.putLong("LastSoundMemoryTick", lastSoundMemoryTick);
        tag.putBoolean("SurveyorFleeing", surveyorFleeing);
        if (surveyorInspectionTarget != null) {
            tag.putDouble("SurveyorTargetX", surveyorInspectionTarget.x);
            tag.putDouble("SurveyorTargetY", surveyorInspectionTarget.y);
            tag.putDouble("SurveyorTargetZ", surveyorInspectionTarget.z);
        }
        tag.putDouble("DoublerPlaneNormalX", doublerPlaneNormal.x);
        tag.putDouble("DoublerPlaneNormalZ", doublerPlaneNormal.z);
        if (lastDoublerFocusPosition != null) {
            tag.putDouble("LastDoublerFocusX", lastDoublerFocusPosition.x);
            tag.putDouble("LastDoublerFocusY", lastDoublerFocusPosition.y);
            tag.putDouble("LastDoublerFocusZ", lastDoublerFocusPosition.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("FocusPlayer")) {
            this.entityData.set(FOCUS_PLAYER, Optional.of(tag.getUUID("FocusPlayer")));
        }
        if (tag.hasUUID("FocusBoat")) {
            this.entityData.set(FOCUS_BOAT, Optional.of(tag.getUUID("FocusBoat")));
        }
        this.anchor = tag.contains("Anchor") ? BlockPos.of(tag.getLong("Anchor")) : null;
        this.lifetime = tag.getInt("Lifetime");
        this.state = tag.getInt("State");
        this.stateTicks = tag.getInt("StateTicks");
        this.quietTicks = tag.getInt("QuietTicks");
        this.mirrorErrorTick = Math.max(180, tag.getInt("MirrorErrorTick"));
        this.copiedActions = tag.getInt("CopiedActions");
        this.failedActions = tag.getInt("FailedActions");
        this.attacksDelivered = tag.getInt("AttacksDelivered");
        this.nextSoundTick = Math.max(this.lifetime + 1, tag.getInt("NextSoundTick"));
        this.dedicatedSoundCuesPlayed = Math.max(0, tag.getInt("DedicatedSoundCuesPlayed"));
        this.mournerAudibleCuePlayed = tag.getBoolean("MournerAudibleCuePlayed");
        this.ferrymanRevealStarted = tag.getBoolean("FerrymanRevealStarted") || this.state == 1 || this.state == 2;
        if (tag.contains("FerrymanSceneTargetX")
                && tag.contains("FerrymanSceneTargetY")
                && tag.contains("FerrymanSceneTargetZ")) {
            this.ferrymanSceneTarget = new Vec3(
                    tag.getDouble("FerrymanSceneTargetX"),
                    tag.getDouble("FerrymanSceneTargetY"),
                    tag.getDouble("FerrymanSceneTargetZ"));
        }
        this.ferrymanDepartureDirection = new Vec3(
                tag.getDouble("FerrymanDepartureX"), 0.0D, tag.getDouble("FerrymanDepartureZ"));
        this.lastSoundMemoryTick = tag.contains("LastSoundMemoryTick")
                ? tag.getLong("LastSoundMemoryTick")
                : Long.MIN_VALUE;
        this.surveyorFleeing = tag.getBoolean("SurveyorFleeing");
        if (tag.contains("SurveyorTargetX")
                && tag.contains("SurveyorTargetY")
                && tag.contains("SurveyorTargetZ")) {
            this.surveyorInspectionTarget = new Vec3(
                    tag.getDouble("SurveyorTargetX"),
                    tag.getDouble("SurveyorTargetY"),
                    tag.getDouble("SurveyorTargetZ"));
        }
        this.doublerPlaneNormal = new Vec3(
                tag.getDouble("DoublerPlaneNormalX"), 0.0D, tag.getDouble("DoublerPlaneNormalZ"));
        if (tag.contains("LastDoublerFocusX")
                && tag.contains("LastDoublerFocusY")
                && tag.contains("LastDoublerFocusZ")) {
            this.lastDoublerFocusPosition = new Vec3(
                    tag.getDouble("LastDoublerFocusX"),
                    tag.getDouble("LastDoublerFocusY"),
                    tag.getDouble("LastDoublerFocusZ"));
        }
    }

    private record MirrorSample(Vec3 motion, boolean crouching, boolean sprinting, boolean swinging, float yaw) {
    }
}
