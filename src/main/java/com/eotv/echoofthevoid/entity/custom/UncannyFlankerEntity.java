package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.event.special.AdaptiveSpecialCombatProfile;
import com.eotv.echoofthevoid.event.special.HuntingSpecialRules;
import com.eotv.echoofthevoid.event.special.UncannyHuntingSpecialSystem;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** One half of a transactional pair that exchanges pressure across the target's field of view. */
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
    private int advancingMemberIndex;
    private boolean firstEncirclementTriggered;
    private boolean terminalReported;
    private boolean closingOnFocus;
    // Both members chase directly because no encirclement exists but the target stays reachable.
    private boolean pincerChase;

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
        this.replanTicks = 0;
        this.attackGraceTicks = 0;
        this.responseDelayTicks = -1;
        this.survivorChaseTicks = 0;
        this.stableFormationTicks = 0;
        this.advancingMemberIndex = 0;
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
        UncannyFlankerEntity partner = resolvePartner(level);
        if (this.survivorChaseTicks > 0) {
            tickSurvivorChase(focus);
            return;
        }
        if (partner == null || !partner.isAlive() || partner.isSinking()) {
            // A loaded death notification sets the chase immediately. A missing partner after
            // reload receives the same finite fallback rather than becoming a permanent solo mob.
            onPartnerTerminal(false);
            tickSurvivorChase(focus);
            return;
        }

        if (this.responseDelayTicks >= 0 && --this.responseDelayTicks == 0) {
            playPhysicalCue(level, UncannySoundRegistry.UNCANNY_FLANKER_RESPONSE.get(), 1.05F, 0.95F);
            this.responseDelayTicks = -1;
        }
        if (this.attackGraceTicks > 0) {
            this.attackGraceTicks--;
        }
        if (this.closingOnFocus
                && HuntingSpecialRules.isFlankerHoldingStagedStrike(this.distanceTo(focus))) {
            this.getNavigation().stop();
            this.closingOnFocus = false;
        }

        boolean observed = isDirectlyObservedByAnyPlayer(level, 36.0D);
        if (this.memberIndex == 0 && --this.replanTicks <= 0) {
            this.replanTicks = HuntingSpecialRules.FLANKER_REPLAN_INTERVAL_TICKS;
            if (!coordinatePair(level, focus, partner)) {
                // Hold position and wait for the target to return to workable terrain.
                this.getNavigation().stop();
                partner.getNavigation().stop();
                this.closingOnFocus = false;
                partner.closingOnFocus = false;
                this.noGeometryTicks += HuntingSpecialRules.FLANKER_REPLAN_INTERVAL_TICKS;
                partner.noGeometryTicks = this.noGeometryTicks;
            } else {
                this.noGeometryTicks = 0;
                partner.noGeometryTicks = 0;
            }
            if (this.noGeometryTicks >= HuntingSpecialRules.FLANKER_STRANDED_CHECK_TICKS
                    && resolveStrandedMember(level, focus, partner)) {
                return;
            }
            if (this.noGeometryTicks >= HuntingSpecialRules.FLANKER_NO_GEOMETRY_TIMEOUT_TICKS) {
                if (!startPincerChase(level, focus, partner)) {
                    abandonPair(level, focus, "two_sided_geometry_unavailable");
                }
                return;
            }
        }

        if (this.memberIndex == 0) {
            maybeTriggerEncirclement(level, focus, partner);
        }
        if (observed
                && this.distanceToSqr(focus) <= HuntingSpecialRules.FLANKER_EVASION_TRIGGER_DISTANCE
                        * HuntingSpecialRules.FLANKER_EVASION_TRIGGER_DISTANCE) {
            tickObservedEvasion(focus);
        }
        if (this.firstEncirclementTriggered
                && (flankerRole() == Role.ADVANCING || flankerRole() == Role.ATTACKING)
                && !observed
                && this.attackGraceTicks <= 0
                && isBehind(focus, -0.25D)
                && this.distanceToSqr(focus) <= HuntingSpecialRules.FLANKER_ENCIRCLEMENT_ATTACK_DISTANCE
                        * HuntingSpecialRules.FLANKER_ENCIRCLEMENT_ATTACK_DISTANCE
                && this.hasLineOfSight(focus)
                && this.meleeCooldownTicks <= 0) {
            this.setTarget(focus);
            this.doHurtTarget(focus);
            this.meleeCooldownTicks = HuntingSpecialRules.FLANKER_ATTACK_INTERVAL_TICKS;
            setRole(Role.ATTACKING);
        } else {
            this.setTarget(null);
        }
    }

    private boolean closeOnFocus(ServerPlayer focus) {
        this.closingOnFocus = this.getNavigation().moveTo(focus, HuntingSpecialRules.FLANKER_ADVANCING_NAVIGATION_SPEED);
        return this.closingOnFocus;
    }

    private void tickObservedEvasion(ServerPlayer focus) {
        this.closingOnFocus = false;
        setRole(Role.WATCHED);
        this.setTarget(null);
        if ((this.tickCount + this.memberIndex * 2) % 4 != 0) {
            return;
        }
        Vec3 away = this.position().subtract(focus.position()).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 1.0E-4D) {
            double side = this.memberIndex == 0 ? 1.0D : -1.0D;
            away = new Vec3(side, 0.0D, -side);
        }
        Vec3 destination = focus.position().add(
                away.normalize().scale(HuntingSpecialRules.FLANKER_EVASION_DESTINATION_RADIUS));
        this.getNavigation().moveTo(
                destination.x,
                destination.y,
                destination.z,
                HuntingSpecialRules.FLANKER_EVASION_NAVIGATION_SPEED);
    }

    private boolean coordinatePair(
            ServerLevel level,
            ServerPlayer focus,
            UncannyFlankerEntity partner) {
        boolean thisObserved = this.isDirectlyObservedByAnyPlayer(level, 36.0D);
        boolean partnerObserved = partner.isDirectlyObservedByAnyPlayer(level, 36.0D);
        if (thisObserved && !partnerObserved) {
            this.advancingMemberIndex = partner.memberIndex;
        } else if (partnerObserved && !thisObserved) {
            this.advancingMemberIndex = this.memberIndex;
        }
        partner.advancingMemberIndex = this.advancingMemberIndex;

        boolean bothObserved = thisObserved && partnerObserved;
        UncannyFlankerEntity advancing = this.advancingMemberIndex == this.memberIndex ? this : partner;
        UncannyFlankerEntity watched = advancing == this ? partner : this;
        advancing.setRole(bothObserved ? Role.WATCHED : Role.ADVANCING);
        watched.setRole(Role.WATCHED);

        Vec3 view = focus.getViewVector(1.0F);
        double rearAngle = Math.atan2(view.z, view.x) + Math.PI;
        double advancingRadius = HuntingSpecialRules.FLANKER_ADVANCING_MAX_RADIUS;
        // A member that settled on its ring stops up to one arrival tolerance short of it.
        boolean staging = !bothObserved
                && advancing.isBehind(focus, -0.25D)
                && HuntingSpecialRules.canAdvancingFlankerStage(advancing.distanceTo(focus))
                && advancing.hasLineOfSight(focus);
        if (staging) {
            advancingRadius = HuntingSpecialRules.FLANKER_ATTACK_STAGING_RADIUS;
        }
        Vec3 advancingIdeal = focus.position().add(
                Math.cos(rearAngle) * advancingRadius,
                0.0D,
                Math.sin(rearAngle) * advancingRadius);
        Vec3 watchedIdeal = focus.position().add(
                Math.cos(rearAngle + Math.PI) * HuntingSpecialRules.FLANKER_WATCHED_MAX_RADIUS,
                0.0D,
                Math.sin(rearAngle + Math.PI) * HuntingSpecialRules.FLANKER_WATCHED_MAX_RADIUS);
        Vec3 advancingDestination = UncannyHuntingSpecialSystem.findReachableGroundNear(
                level,
                advancing,
                advancingIdeal,
                focus.position(),
                advancingRadius == HuntingSpecialRules.FLANKER_ATTACK_STAGING_RADIUS
                        ? 2.2D
                        : HuntingSpecialRules.FLANKER_ADVANCING_MIN_RADIUS,
                advancingRadius == HuntingSpecialRules.FLANKER_ATTACK_STAGING_RADIUS
                        ? HuntingSpecialRules.FLANKER_ENCIRCLEMENT_ATTACK_DISTANCE
                        : HuntingSpecialRules.FLANKER_ADVANCING_MAX_RADIUS,
                HuntingSpecialRules.FLANKER_MAX_PATH_PROBES / 2);
        Vec3 watchedDestination = UncannyHuntingSpecialSystem.findReachableGroundNear(
                level,
                watched,
                watchedIdeal,
                focus.position(),
                HuntingSpecialRules.FLANKER_WATCHED_MIN_RADIUS,
                HuntingSpecialRules.FLANKER_WATCHED_MAX_RADIUS,
                HuntingSpecialRules.FLANKER_MAX_PATH_PROBES / 2);
        if (advancingDestination == null || watchedDestination == null) {
            return false;
        }
        Vec3 firstOffset = advancingDestination.subtract(focus.position());
        Vec3 secondOffset = watchedDestination.subtract(focus.position());
        if (!HuntingSpecialRules.hasValidFlankerSeparation(
                        firstOffset.x, firstOffset.z, secondOffset.x, secondOffset.z)
                || advancingDestination.distanceToSqr(watchedDestination) < 8.0D * 8.0D) {
            return false;
        }
        double arrival = HuntingSpecialRules.FLANKER_ARRIVAL_TOLERANCE;
        advancing.closingOnFocus = false;
        watched.closingOnFocus = false;
        boolean advancingMoving;
        if (staging) {
            // No arrival slack while staging: a node "reached" 0.9 block short left the member parked
            // just beyond melee reach for good, notably after its first hit knocked the target back.
            // Vanilla navigation refuses a node that close, so the last step closes on the target and
            // tickSpecial halts it at striking distance, still behind.
            advancingMoving = HuntingSpecialRules.isFlankerHoldingStagedStrike(advancing.distanceTo(focus))
                    || advancing.getNavigation().moveTo(
                            advancingDestination.x,
                            advancingDestination.y,
                            advancingDestination.z,
                            HuntingSpecialRules.FLANKER_ADVANCING_NAVIGATION_SPEED)
                    || advancing.closeOnFocus(focus);
        } else {
            advancingMoving = advancing.position().distanceToSqr(advancingDestination) <= arrival * arrival
                    || advancing.getNavigation().moveTo(
                            advancingDestination.x,
                            advancingDestination.y,
                            advancingDestination.z,
                            HuntingSpecialRules.FLANKER_ADVANCING_NAVIGATION_SPEED);
        }
        boolean watchedMoving = watched.position().distanceToSqr(watchedDestination) <= arrival * arrival
                || watched.getNavigation().moveTo(
                        watchedDestination.x,
                        watchedDestination.y,
                        watchedDestination.z,
                        HuntingSpecialRules.FLANKER_WATCHED_NAVIGATION_SPEED);
        if (this.tickCount % 100 == 0) {
            trace(focus, "formation", "coordinated",
                    "advancing_member", advancing.memberIndex,
                    "advancing_radius", advancing.distanceTo(focus),
                    "watched_radius", watched.distanceTo(focus),
                    "separation_degrees", HuntingSpecialRules.angularSeparationDegrees(
                            this.getX() - focus.getX(), this.getZ() - focus.getZ(),
                            partner.getX() - focus.getX(), partner.getZ() - focus.getZ()));
        }
        return advancingMoving && watchedMoving;
    }

    private void maybeTriggerEncirclement(
            ServerLevel level,
            ServerPlayer focus,
            UncannyFlankerEntity partner) {
        if (this.firstEncirclementTriggered || partner.firstEncirclementTriggered) {
            return;
        }
        UncannyFlankerEntity attacker = this.advancingMemberIndex == this.memberIndex ? this : partner;
        Vec3 first = this.position().subtract(focus.position());
        Vec3 second = partner.position().subtract(focus.position());
        boolean validFormation = attacker.flankerRole() == Role.ADVANCING
                && attacker.isBehind(focus, -0.25D)
                && attacker.hasLineOfSight(focus)
                && attacker.distanceTo(focus) <= HuntingSpecialRules.FLANKER_ENCIRCLEMENT_ATTACK_DISTANCE
                && HuntingSpecialRules.hasValidFlankerSeparation(first.x, first.z, second.x, second.z);
        this.stableFormationTicks = validFormation ? this.stableFormationTicks + 1 : 0;
        partner.stableFormationTicks = this.stableFormationTicks;
        if (!HuntingSpecialRules.isStableFlankerEncirclement(
                this.tickCount,
                this.stableFormationTicks,
                attacker.distanceTo(focus),
                attacker.isBehind(focus, -0.25D),
                first.x,
                first.z,
                second.x,
                second.z)) {
            return;
        }
        this.firstEncirclementTriggered = true;
        partner.firstEncirclementTriggered = true;
        this.attackGraceTicks = HuntingSpecialRules.FLANKER_FIRST_ENCIRCLEMENT_GRACE_TICKS;
        partner.attackGraceTicks = HuntingSpecialRules.FLANKER_FIRST_ENCIRCLEMENT_GRACE_TICKS;
        UncannyFlankerEntity caller = this.memberIndex == 0 ? this : partner;
        UncannyFlankerEntity responder = this.memberIndex == 0 ? partner : this;
        caller.playPhysicalCue(level, UncannySoundRegistry.UNCANNY_FLANKER_CALL.get(), 1.08F, 0.96F);
        responder.responseDelayTicks = HuntingSpecialRules.FLANKER_RESPONSE_DELAY_TICKS;
        trace(focus, "transition", "first_encirclement",
                "separation_degrees", HuntingSpecialRules.angularSeparationDegrees(
                        first.x, first.z, second.x, second.z));
    }

    private void tickSurvivorChase(ServerPlayer focus) {
        this.closingOnFocus = false;
        setRole(Role.SURVIVOR);
        this.setTarget(focus);
        this.getNavigation().moveTo(focus, HuntingSpecialRules.FLANKER_ADVANCING_NAVIGATION_SPEED);
        if (this.distanceToSqr(focus) <= HuntingSpecialRules.FLANKER_ENCIRCLEMENT_ATTACK_DISTANCE
                        * HuntingSpecialRules.FLANKER_ENCIRCLEMENT_ATTACK_DISTANCE
                && this.hasLineOfSight(focus)
                && this.meleeCooldownTicks <= 0) {
            this.doHurtTarget(focus);
            this.meleeCooldownTicks = HuntingSpecialRules.FLANKER_ATTACK_INTERVAL_TICKS;
        }
        if (--this.survivorChaseTicks <= 0) {
            beginSinking(focus, "survivor_chase_complete");
        }
    }

    public void onPartnerTerminal(boolean killedByPlayer) {
        if (this.isSinking() || (this.survivorChaseTicks > 0 && !this.pincerChase)) {
            return;
        }
        this.pincerChase = false;
        this.survivorChaseTicks = HuntingSpecialRules.FLANKER_SURVIVOR_CHASE_TICKS;
        if (this.level() instanceof ServerLevel parityLevel) {
            ServerPlayer focus = resolveFocus(parityLevel);
            if (focus != null) {
                // Alone, it carries a whole hunter's toughness: losing the partner is no escape.
                refreshCombatParity(focus, false);
            }
        }
        if (this.level() instanceof ServerLevel level) {
            playPhysicalCue(level, UncannySoundRegistry.UNCANNY_FLANKER_RESPONSE.get(), 1.16F, 0.78F);
            trace(resolveFocus(level), "transition", "partner_lost_direct_chase",
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

    /** No encirclement exists, but the target is reachable: close in from both sides at once. */
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
        var path = member.getNavigation().createPath(focus, 1);
        return path != null && path.canReach();
    }

    @Override
    public int getMaxFallDistance() {
        // A land hunter that walked off a cliff left its partner without a ring: never plan drops.
        return HuntingSpecialRules.FLANKER_MAX_PLANNED_DROP;
    }

    @Override
    protected com.eotv.echoofthevoid.event.special.CombatParityRules.Profile combatParity() {
        return this.survivorChaseTicks > 0 && !this.pincerChase
                ? com.eotv.echoofthevoid.event.special.CombatParityRules.FLANKER_SURVIVOR
                : com.eotv.echoofthevoid.event.special.CombatParityRules.FLANKER_PAIR_MEMBER;
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
    }

    @Override
    protected String specialId() {
        return "flanker";
    }

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
