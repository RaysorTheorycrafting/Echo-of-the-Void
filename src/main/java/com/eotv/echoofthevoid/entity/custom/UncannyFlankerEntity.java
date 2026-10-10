package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.event.special.AdaptiveSpecialCombatProfile;
import com.eotv.echoofthevoid.event.special.CombatParityRules;
import com.eotv.echoofthevoid.event.special.FlankerRules;
import com.eotv.echoofthevoid.event.special.HuntingSpecialRules;
import com.eotv.echoofthevoid.event.special.UncannyHuntingSpecialSystem;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * One half of a Flanker? pair: a pincer (see {@link FlankerRules}). The member the target looks at
 * is the bait, holding in front and backing off when charged; the other is the blade, circling out
 * of sight, dashing in from behind for a single blow and breaking off. Looking at the blade swaps
 * the roles. Together the pair is as strong as one Attacker?: half its toughness each, its blow,
 * and one shared strike rhythm.
 */
public class UncannyFlankerEntity extends AbstractUncannyHuntingSpecialEntity {
    private static final EntityDataAccessor<Optional<UUID>> PARTNER_ID = SynchedEntityData.defineId(
            UncannyFlankerEntity.class,
            EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Byte> ROLE = SynchedEntityData.defineId(
            UncannyFlankerEntity.class,
            EntityDataSerializers.BYTE);

    private UUID pairId;
    private int memberIndex;
    private int noGeometryTicks;
    private int replanTicks;
    private int attackGraceTicks;
    private int responseDelayTicks;
    private int survivorChaseTicks;
    private int stableFormationTicks;
    /** Which member (index) is the bait; kept in step on both members by member 0. */
    private int advancingMemberIndex;
    private boolean firstEncirclementTriggered;
    private boolean terminalReported;
    // Both members chase directly because no way round the target exists but it stays reachable.
    private boolean pincerChase;

    private int pairStrikeCooldown;
    private int pincerWindowTicks;
    private int bladeRecoverTicks;
    private boolean dashing;
    private boolean lunging;
    private int strafeSide = 1;
    private int strafeTicks;

    public UncannyFlankerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "Flanker?");
        // A land hunter: never route through water, where it sank instead of holding its ring.
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, -1.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PARTNER_ID, Optional.empty());
        builder.define(ROLE, (byte) Role.ADVANCING.id());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    public void setupPair(
            ServerPlayer player,
            UUID pairId,
            UUID partnerId,
            int memberIndex,
            AdaptiveSpecialCombatProfile attackerProfile) {
        setupTarget(player, 20 * 240);
        this.pairId = pairId;
        this.entityData.set(PARTNER_ID, Optional.of(partnerId));
        this.memberIndex = memberIndex == 0 ? 0 : 1;
        HuntingSpecialRules.flankerProfile(attackerProfile).applyTo(this, true);
        refreshCombatParity(player, true);
        var followRange = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.setBaseValue(HuntingSpecialRules.FLANKER_FOLLOW_RANGE);
        }
        this.noGeometryTicks = 0;
        this.replanTicks = this.memberIndex * 3;
        this.attackGraceTicks = 0;
        this.responseDelayTicks = -1;
        this.survivorChaseTicks = 0;
        this.stableFormationTicks = 0;
        // Member 1 starts as the bait, ahead of the target; member 0 as the blade, behind.
        this.advancingMemberIndex = 1;
        this.firstEncirclementTriggered = false;
        this.terminalReported = false;
        this.pincerChase = false;
        setRole(this.memberIndex == 0 ? Role.ADVANCING : Role.WATCHED);
        trace(player, "spawn", "pair_member_created", "pair_id", pairId, "member", this.memberIndex);
    }

    public UUID pairId() {
        return this.pairId;
    }

    public Optional<UUID> partnerId() {
        return this.entityData.get(PARTNER_ID);
    }

    public int memberIndex() {
        return this.memberIndex;
    }

    public Role flankerRole() {
        return Role.byId(this.entityData.get(ROLE));
    }

    public boolean isBait() {
        return this.advancingMemberIndex == this.memberIndex;
    }

    // ------------------------------------------------------------------ main loop

    @Override
    protected void tickSpecial(ServerLevel level, ServerPlayer focus) {
        if (this.pairId == null) {
            beginSinking(focus, "missing_pair_identity");
            return;
        }
        if (this.distanceToSqr(focus) > HuntingSpecialRules.FLANKER_MAX_FOCUS_DISTANCE
                * HuntingSpecialRules.FLANKER_MAX_FOCUS_DISTANCE) {
            abandonPair(level, focus, "focus_beyond_range");
            return;
        }
        if (this.pairStrikeCooldown > 0) {
            this.pairStrikeCooldown--;
        }
        if (this.pincerWindowTicks > 0) {
            this.pincerWindowTicks--;
        }
        if (this.attackGraceTicks > 0) {
            this.attackGraceTicks--;
        }
        this.getLookControl().setLookAt(focus, 40.0F, 40.0F);
        UncannyFlankerEntity partner = resolvePartner(level);
        if (this.survivorChaseTicks > 0) {
            if (this.pincerChase && partner != null && partner.isAlive() && !partner.isSinking()) {
                tickDirectChase(focus, partner);
            } else {
                tickSurvivor(level, focus);
            }
            return;
        }
        if (partner == null || !partner.isAlive() || partner.isSinking()) {
            // A loaded death notification sets the solo hunt immediately. A missing partner after
            // reload receives the same finite fallback rather than becoming a permanent solo mob.
            onPartnerTerminal(false);
            tickSurvivor(level, focus);
            return;
        }
        if (this.responseDelayTicks >= 0 && --this.responseDelayTicks == 0) {
            playPhysicalCue(level, UncannySoundRegistry.UNCANNY_FLANKER_RESPONSE.get(), 1.05F, 0.95F);
            this.responseDelayTicks = -1;
        }
        if (this.memberIndex == 0) {
            updateRoles(focus, partner);
            if (geometryFallback(level, focus, partner)) {
                return;
            }
        }
        if (!this.firstEncirclementTriggered) {
            tickApproach(level, focus);
            if (this.memberIndex == 0) {
                maybeStartHunt(level, focus, partner);
            }
            return;
        }
        if (isBait()) {
            tickBait(level, focus, partner);
        } else {
            tickBlade(level, focus, partner);
        }
    }

    /** The member looked at is the bait; turning clearly toward the other swaps them. */
    private void updateRoles(ServerPlayer focus, UncannyFlankerEntity partner) {
        double mine = viewCosine(focus, this);
        double theirs = viewCosine(focus, partner);
        double cosine0 = this.memberIndex == 0 ? mine : theirs;
        double cosine1 = this.memberIndex == 0 ? theirs : mine;
        int bait = FlankerRules.chooseBait(this.advancingMemberIndex, cosine0, cosine1);
        if (bait != this.advancingMemberIndex) {
            // The old bait is now behind the target: it becomes the blade, fresh.
            for (UncannyFlankerEntity member : new UncannyFlankerEntity[] {this, partner}) {
                member.dashing = false;
                member.lunging = false;
                member.bladeRecoverTicks = 0;
                member.replanTicks = 0;
            }
        }
        this.advancingMemberIndex = bait;
        partner.advancingMemberIndex = bait;
        for (UncannyFlankerEntity member : new UncannyFlankerEntity[] {this, partner}) {
            member.setRole(member.isBait() ? Role.WATCHED
                    : member.bladeRecoverTicks > 0 ? Role.ATTACKING : Role.ADVANCING);
        }
    }

    /**
     * No way round the target for too long: a member stranded below hunts on alone, otherwise the
     * pair comes straight in from both sides, or gives up if the target cannot be reached at all.
     */
    private boolean geometryFallback(ServerLevel level, ServerPlayer focus, UncannyFlankerEntity partner) {
        int stuck = Math.max(this.noGeometryTicks, partner.noGeometryTicks);
        if (stuck >= HuntingSpecialRules.FLANKER_STRANDED_CHECK_TICKS && this.tickCount % 20 == 0
                && resolveStrandedMember(level, focus, partner)) {
            return true;
        }
        if (stuck >= HuntingSpecialRules.FLANKER_NO_GEOMETRY_TIMEOUT_TICKS) {
            if (!startPincerChase(level, focus, partner)) {
                abandonPair(level, focus, "two_sided_geometry_unavailable");
            }
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ before the hunt

    /** Takes position: the bait ahead of the target, the blade behind it, out of sight. */
    private void tickApproach(ServerLevel level, ServerPlayer focus) {
        if (isBait() && this.distanceTo(focus) < FlankerRules.BAIT_BACKOFF_DISTANCE) {
            backOff(focus);
            return;
        }
        if (--this.replanTicks > 0 && !this.getNavigation().isDone()) {
            return;
        }
        this.replanTicks = 10;
        double[] view = flatView(focus);
        double angle = isBait() ? 0.0D : (signedAngle(focus, this) >= 0.0D ? 150.0D : -150.0D);
        moveOnRing(level, focus, view, angle, 10.0D, 8.0D, 12.0D,
                isBait() ? FlankerRules.BAIT_SPRINT_RATIO : FlankerRules.ORBIT_SPRINT_RATIO);
    }

    private void maybeStartHunt(ServerLevel level, ServerPlayer focus, UncannyFlankerEntity partner) {
        UncannyFlankerEntity blade = isBait() ? partner : this;
        UncannyFlankerEntity bait = isBait() ? this : partner;
        boolean formed = FlankerRules.isFormation(signedAngle(focus, blade), signedAngle(focus, bait),
                blade.distanceTo(focus), bait.distanceTo(focus));
        this.stableFormationTicks = formed
                ? Math.min(FlankerRules.FORMATION_STABLE_TICKS, this.stableFormationTicks + 1) : 0;
        partner.stableFormationTicks = this.stableFormationTicks;
        boolean ready = this.tickCount >= FlankerRules.MIN_AGE_BEFORE_HUNT_TICKS
                && (this.stableFormationTicks >= FlankerRules.FORMATION_STABLE_TICKS
                        || this.tickCount >= FlankerRules.APPROACH_TIMEOUT_TICKS);
        if (!ready) {
            return;
        }
        this.firstEncirclementTriggered = true;
        partner.firstEncirclementTriggered = true;
        this.attackGraceTicks = HuntingSpecialRules.FLANKER_FIRST_ENCIRCLEMENT_GRACE_TICKS;
        partner.attackGraceTicks = HuntingSpecialRules.FLANKER_FIRST_ENCIRCLEMENT_GRACE_TICKS;
        // The call comes from behind, the answer from ahead: the target learns it is surrounded.
        blade.playPhysicalCue(level, UncannySoundRegistry.UNCANNY_FLANKER_CALL.get(), 1.08F, 0.96F);
        bait.responseDelayTicks = HuntingSpecialRules.FLANKER_RESPONSE_DELAY_TICKS;
        trace(focus, "transition", "first_encirclement",
                "formed", formed,
                "separation_degrees", HuntingSpecialRules.angularSeparationDegrees(
                        this.getX() - focus.getX(), this.getZ() - focus.getZ(),
                        partner.getX() - focus.getX(), partner.getZ() - focus.getZ()));
    }

    // ------------------------------------------------------------------ the hunt

    /** In front, strafing, out of reach; lunges in when the blade dashes. */
    private void tickBait(ServerLevel level, ServerPlayer focus, UncannyFlankerEntity partner) {
        this.setTarget(null);
        double distance = this.distanceTo(focus);
        if (this.pincerWindowTicks > 0 && distance <= FlankerRules.STRIKE_REACH && canHit(focus)) {
            strike(level, focus, partner, true);
            this.lunging = false;
            return;
        }
        if (partner.dashing && this.attackGraceTicks <= 0 && distance <= 10.0D) {
            // The pincer: both close at once, from the front and from behind.
            this.lunging = true;
            this.getNavigation().moveTo(focus, speed(FlankerRules.DASH_SPRINT_RATIO));
            return;
        }
        this.lunging = false;
        if (distance < FlankerRules.BAIT_BACKOFF_DISTANCE) {
            backOff(focus);
            return;
        }
        if (--this.strafeTicks <= 0) {
            this.strafeSide = this.random.nextBoolean() ? 1 : -1;
            this.strafeTicks = 30 + this.random.nextInt(30);
            this.replanTicks = 0;
        }
        if (--this.replanTicks > 0 && !this.getNavigation().isDone()) {
            return;
        }
        this.replanTicks = 8;
        moveOnRing(level, focus, flatView(focus), this.strafeSide * FlankerRules.BAIT_STRAFE_DEGREES,
                FlankerRules.BAIT_DISTANCE, FlankerRules.BAIT_BACKOFF_DISTANCE, FlankerRules.BAIT_DISTANCE + 2.5D,
                FlankerRules.BAIT_SPRINT_RATIO);
    }

    /** Circles out of sight to the target's back, dashes, strikes once, breaks off to the side. */
    private void tickBlade(ServerLevel level, ServerPlayer focus, UncannyFlankerEntity partner) {
        double[] view = flatView(focus);
        double angle = signedAngle(focus, this);
        double distance = this.distanceTo(focus);
        if (this.bladeRecoverTicks > 0) {
            this.bladeRecoverTicks--;
            this.dashing = false;
            this.setTarget(null);
            if (this.bladeRecoverTicks % 8 == 7) {
                moveOnRing(level, focus, view, (angle >= 0.0D ? 1.0D : -1.0D) * 115.0D, 7.5D, 5.0D, 9.5D,
                        FlankerRules.ORBIT_SPRINT_RATIO);
            }
            return;
        }
        if (this.dashing) {
            if (FlankerRules.isInView(angle) && distance > 3.0D) {
                // Seen coming: it veers off instead of running into a raised sword.
                this.dashing = false;
                this.replanTicks = 0;
            } else {
                this.setTarget(focus);
                this.getNavigation().moveTo(focus, speed(FlankerRules.DASH_SPRINT_RATIO));
                if (distance <= FlankerRules.STRIKE_REACH && !FlankerRules.isInView(angle) && canHit(focus)) {
                    strike(level, focus, partner, false);
                }
                return;
            }
        }
        if (this.attackGraceTicks <= 0 && FlankerRules.canStrikeFrom(angle)
                && distance <= FlankerRules.DASH_DISTANCE && this.pairStrikeCooldown <= 6) {
            this.dashing = true;
            // Quick steps right behind: the one warning a good player can turn around on.
            BlockPos below = this.blockPosition().below();
            playPhysicalCue(level, level.getBlockState(below).getSoundType().getStepSound(), 1.0F, 1.25F);
            this.getNavigation().moveTo(focus, speed(FlankerRules.DASH_SPRINT_RATIO));
            return;
        }
        this.setTarget(null);
        if (--this.replanTicks > 0 && !this.getNavigation().isDone()) {
            return;
        }
        this.replanTicks = 6;
        double radius = FlankerRules.orbitRadius(distance, angle);
        boolean moving = moveOnRing(level, focus, view, FlankerRules.orbitAngle(angle), radius,
                Math.max(2.5D, radius - 2.0D), radius + 2.0D, FlankerRules.ORBIT_SPRINT_RATIO);
        this.noGeometryTicks = moving ? 0 : this.noGeometryTicks + 6;
    }

    /** Alone: the same hit-and-run from behind, at half an Attacker?'s toughness, then it is gone. */
    private void tickSurvivor(ServerLevel level, ServerPlayer focus) {
        setRole(Role.SURVIVOR);
        tickBlade(level, focus, null);
        if (--this.survivorChaseTicks <= 0) {
            beginSinking(focus, "survivor_chase_complete");
        }
    }

    /** No way round the target: both come straight in, still sharing one strike rhythm. */
    private void tickDirectChase(ServerPlayer focus, UncannyFlankerEntity partner) {
        setRole(Role.SURVIVOR);
        this.setTarget(focus);
        this.getNavigation().moveTo(focus, speed(FlankerRules.ORBIT_SPRINT_RATIO));
        if (this.distanceTo(focus) <= HuntingSpecialRules.FLANKER_SURVIVOR_STRIKE_DISTANCE && canHit(focus)
                && this.level() instanceof ServerLevel level) {
            strike(level, focus, partner, false);
            this.bladeRecoverTicks = 0;
        }
        if (--this.survivorChaseTicks <= 0) {
            beginSinking(focus, "pincer_chase_complete");
        }
    }

    private boolean canHit(ServerPlayer focus) {
        return this.meleeCooldownTicks <= 0 && this.pairStrikeCooldown <= 0 && this.hasLineOfSight(focus);
    }

    /** One blow; the pair then waits a whole Attacker? cadence (two after a pincer). */
    private void strike(ServerLevel level, ServerPlayer focus, UncannyFlankerEntity partner, boolean pincer) {
        this.setTarget(focus);
        this.swing(InteractionHand.MAIN_HAND);
        this.doHurtTarget(focus);
        int gap = FlankerRules.pairStrikeGapTicks();
        this.meleeCooldownTicks = gap;
        this.pairStrikeCooldown = pincer ? gap * 2 : gap;
        if (partner != null) {
            partner.pairStrikeCooldown = this.pairStrikeCooldown;
            if (pincer) {
                partner.pairStrikeCooldown = gap * 2;
            } else if (partner.lunging) {
                // The bait is closing in from the front: its blow may follow at once.
                partner.pincerWindowTicks = FlankerRules.PINCER_WINDOW_TICKS;
                partner.pairStrikeCooldown = 0;
            }
        }
        if (!isBait() || partner == null) {
            this.dashing = false;
            this.bladeRecoverTicks = FlankerRules.BLADE_RECOVER_TICKS;
            setRole(partner == null ? Role.SURVIVOR : Role.ATTACKING);
        }
        trace(focus, "attack", pincer ? "pincer_blow" : "blow", "angle", signedAngle(focus, this));
    }

    /** Charged by the target: back away out of its reach, keeping it in view. */
    private void backOff(ServerPlayer focus) {
        Vec3 away = this.position().subtract(focus.position()).multiply(1.0D, 0.0D, 1.0D);
        away = away.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : away.normalize();
        Vec3 back = focus.position().add(away.scale(FlankerRules.BAIT_DISTANCE + 1.5D));
        if (!this.getNavigation().moveTo(back.x, back.y, back.z, speed(FlankerRules.BACKOFF_SPRINT_RATIO))) {
            // Its back to a wall: slide along it instead.
            Vec3 aside = focus.position().add(new Vec3(-away.z, 0.0D, away.x).scale(this.strafeSide * FlankerRules.BAIT_DISTANCE));
            this.getNavigation().moveTo(aside.x, aside.y, aside.z, speed(FlankerRules.BACKOFF_SPRINT_RATIO));
        }
    }

    // ------------------------------------------------------------------ geometry

    /** Somewhere on a ring round the target at the given angle; tighter rings where the wide one is blocked. */
    private boolean moveOnRing(ServerLevel level, ServerPlayer focus, double[] view, double angle, double radius,
            double minimumRadius, double maximumRadius, double sprintRatio) {
        double[] direction = FlankerRules.rotate(view[0], view[1], angle);
        for (double ring : new double[] {radius, (radius + minimumRadius) * 0.5D, minimumRadius}) {
            Vec3 ideal = focus.position().add(direction[0] * ring, 0.0D, direction[1] * ring);
            Vec3 destination = UncannyHuntingSpecialSystem.findReachableGroundNear(
                    level, this, ideal, focus.position(), Math.min(ring, minimumRadius), Math.max(ring, maximumRadius), 4);
            if (destination != null
                    && this.getNavigation().moveTo(destination.x, destination.y, destination.z, speed(sprintRatio))) {
                return true;
            }
        }
        return false;
    }

    private double speed(double sprintRatio) {
        return FlankerRules.navigationModifier(this.getAttributeValue(Attributes.MOVEMENT_SPEED), sprintRatio);
    }

    private static double[] flatView(ServerPlayer focus) {
        Vec3 view = focus.getViewVector(1.0F).multiply(1.0D, 0.0D, 1.0D);
        if (view.lengthSqr() < 1.0E-6D) {
            float yaw = focus.getYRot() * ((float) Math.PI / 180.0F);
            view = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        }
        view = view.normalize();
        return new double[] {view.x, view.z};
    }

    private static double signedAngle(ServerPlayer focus, Entity member) {
        double[] view = flatView(focus);
        return FlankerRules.signedAngleFromView(view[0], view[1],
                member.getX() - focus.getX(), member.getZ() - focus.getZ());
    }

    /** How directly the target looks at a member: the cosine of the horizontal angle. */
    private static double viewCosine(ServerPlayer focus, Entity member) {
        return Math.cos(Math.toRadians(signedAngle(focus, member)));
    }

    // ------------------------------------------------------------------ losing the pair

    public void onPartnerTerminal(boolean killedByPlayer) {
        if (this.isSinking() || (this.survivorChaseTicks > 0 && !this.pincerChase)) {
            return;
        }
        this.pincerChase = false;
        this.dashing = false;
        this.lunging = false;
        this.survivorChaseTicks = HuntingSpecialRules.FLANKER_SURVIVOR_CHASE_TICKS;
        // It keeps its own half of the pair's toughness: alone it is half the danger.
        if (this.level() instanceof ServerLevel level) {
            playPhysicalCue(level, UncannySoundRegistry.UNCANNY_FLANKER_RESPONSE.get(), 1.16F, 0.78F);
            trace(resolveFocus(level), "transition", "partner_lost_solo_hunt",
                    "partner_player_kill", killedByPlayer,
                    "chase_ticks", this.survivorChaseTicks);
        }
    }

    /**
     * One member fell off a cliff or into a ravine and can no longer reach the target: it sinks
     * away and the other one hunts on alone instead of the whole pair giving up.
     */
    private boolean resolveStrandedMember(ServerLevel level, ServerPlayer focus, UncannyFlankerEntity partner) {
        boolean thisReaches = canReach(this, focus);
        boolean partnerReaches = canReach(partner, focus);
        if (thisReaches == partnerReaches) {
            return false;
        }
        UncannyFlankerEntity stranded = thisReaches ? partner : this;
        UncannyFlankerEntity hunter = thisReaches ? this : partner;
        trace(focus, "transition", "partner_stranded_solo_hunt",
                "stranded_member", stranded.memberIndex,
                "stranded_y", stranded.getY(),
                "target_y", focus.getY());
        stranded.beginSinking(focus, "stranded_without_route");
        hunter.onPartnerTerminal(false);
        return true;
    }

    /** No way round the target, but it is reachable: close in from both sides at once. */
    private boolean startPincerChase(ServerLevel level, ServerPlayer focus, UncannyFlankerEntity partner) {
        if (!canReach(this, focus) && !canReach(partner, focus)) {
            return false;
        }
        for (UncannyFlankerEntity member : new UncannyFlankerEntity[] {this, partner}) {
            member.pincerChase = true;
            member.survivorChaseTicks = HuntingSpecialRules.FLANKER_PINCER_CHASE_TICKS;
            member.noGeometryTicks = 0;
        }
        trace(focus, "transition", "pincer_chase_without_ring");
        return true;
    }

    private static boolean canReach(UncannyFlankerEntity member, ServerPlayer focus) {
        if (focus.isInWater() || member.isInWater()) {
            // No ground path crosses water, but it swims after its prey (UncannySwimming).
            return true;
        }
        var path = member.getNavigation().createPath(focus, 1);
        return path != null && path.canReach();
    }

    @Override
    public int getMaxFallDistance() {
        // A land hunter that walked off a cliff left its partner without a ring: never plan drops.
        return HuntingSpecialRules.FLANKER_MAX_PLANNED_DROP;
    }

    @Override
    protected CombatParityRules.Profile combatParity() {
        return CombatParityRules.FLANKER_PAIR_MEMBER;
    }

    private void abandonPair(ServerLevel level, ServerPlayer focus, String reason) {
        UncannyFlankerEntity partner = resolvePartner(level);
        beginSinking(focus, reason);
        if (partner != null && !partner.isSinking()) {
            partner.beginSinking(focus, reason);
        }
    }

    private UncannyFlankerEntity resolvePartner(ServerLevel level) {
        Entity entity = partnerId().map(level::getEntity).orElse(null);
        return entity instanceof UncannyFlankerEntity flanker ? flanker : null;
    }

    @Override
    protected void onBeginSinking(ServerPlayer focus, String reason) {
        reportTerminal(false, "abandon:" + reason);
    }

    @Override
    public void die(DamageSource source) {
        boolean killedByPlayer = source.getEntity() instanceof Player;
        reportTerminal(killedByPlayer, "death");
        if (this.level() instanceof ServerLevel level) {
            UncannyFlankerEntity partner = resolvePartner(level);
            if (partner != null && partner.isAlive()) {
                partner.onPartnerTerminal(killedByPlayer);
            }
        }
        super.die(source);
    }

    private void reportTerminal(boolean killedByPlayer, String reason) {
        if (this.terminalReported || this.pairId == null || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.terminalReported = true;
        UncannyHuntingSpecialSystem.recordFlankerTerminal(
                level, this.pairId, this.getUUID(), killedByPlayer, reason);
    }

    private void setRole(Role role) {
        this.entityData.set(ROLE, (byte) role.id());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.pairId != null) {
            tag.putUUID("FlankerPairId", this.pairId);
        }
        partnerId().ifPresent(uuid -> tag.putUUID("FlankerPartnerId", uuid));
        tag.putInt("FlankerMemberIndex", this.memberIndex);
        tag.putByte("FlankerRole", (byte) flankerRole().id());
        tag.putInt("FlankerNoGeometryTicks", this.noGeometryTicks);
        tag.putInt("FlankerReplanTicks", this.replanTicks);
        tag.putInt("FlankerAttackGrace", this.attackGraceTicks);
        tag.putInt("FlankerResponseDelay", this.responseDelayTicks);
        tag.putInt("FlankerSurvivorChase", this.survivorChaseTicks);
        tag.putBoolean("FlankerPincerChase", this.pincerChase);
        tag.putInt("FlankerStableFormation", this.stableFormationTicks);
        tag.putInt("FlankerAdvancingMember", this.advancingMemberIndex);
        tag.putBoolean("FlankerEncirclement", this.firstEncirclementTriggered);
        tag.putBoolean("FlankerTerminalReported", this.terminalReported);
        tag.putInt("FlankerPairStrikeCooldown", this.pairStrikeCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.pairId = tag.hasUUID("FlankerPairId") ? tag.getUUID("FlankerPairId") : null;
        if (tag.hasUUID("FlankerPartnerId")) {
            this.entityData.set(PARTNER_ID, Optional.of(tag.getUUID("FlankerPartnerId")));
        }
        this.memberIndex = tag.getInt("FlankerMemberIndex") == 0 ? 0 : 1;
        this.entityData.set(ROLE, tag.getByte("FlankerRole"));
        this.noGeometryTicks = Math.max(0, tag.getInt("FlankerNoGeometryTicks"));
        this.replanTicks = Math.max(0, tag.getInt("FlankerReplanTicks"));
        this.attackGraceTicks = Math.max(0, tag.getInt("FlankerAttackGrace"));
        this.responseDelayTicks = tag.contains("FlankerResponseDelay")
                ? tag.getInt("FlankerResponseDelay") : -1;
        this.survivorChaseTicks = Math.max(0, tag.getInt("FlankerSurvivorChase"));
        this.pincerChase = tag.getBoolean("FlankerPincerChase");
        this.stableFormationTicks = Math.max(0, tag.getInt("FlankerStableFormation"));
        this.advancingMemberIndex = tag.getInt("FlankerAdvancingMember") == 0 ? 0 : 1;
        this.firstEncirclementTriggered = tag.getBoolean("FlankerEncirclement");
        this.terminalReported = tag.getBoolean("FlankerTerminalReported");
        this.pairStrikeCooldown = Math.max(0, tag.getInt("FlankerPairStrikeCooldown"));
    }

    @Override
    protected String specialId() {
        return "flanker";
    }

    /** Synced role; ids are persistent ({@code FlankerRole}): WATCHED is the bait, ADVANCING the blade. */
    public enum Role {
        ADVANCING(0), WATCHED(1), ATTACKING(2), SURVIVOR(3);

        private final int id;

        Role(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static Role byId(int id) {
            return switch (id) {
                case 1 -> WATCHED;
                case 2 -> ATTACKING;
                case 3 -> SURVIVOR;
                default -> ADVANCING;
            };
        }
    }
}
