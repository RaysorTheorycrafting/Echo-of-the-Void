package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.diagnostics.DiagnosticSeverity;
import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.event.special.DevourerArenaRules;
import com.eotv.echoofthevoid.event.special.DevourerArenaSystem;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Slow, audible contact threat that opens isolated, persistent Elsewhere trials. */
public final class UncannyDevourerEntity extends Monster implements UncannyEntityMarker {
    public static final int MAX_LIFETIME_TICKS = 20 * 45;
    public static final int MAX_CAPTURES = 8;
    /** Its arms reach about a block beyond its body: brushing past it is no longer safe. */
    public static final double CAPTURE_HORIZONTAL_REACH = 1.10D;
    public static final double CAPTURE_VERTICAL_REACH = 0.40D;
    /** The victim is held and drawn into the chest portal this long before the trial begins. */
    public static final int SEIZE_TICKS = 16;
    private static final EntityDataAccessor<Optional<UUID>> PRIMARY_TARGET =
            SynchedEntityData.defineId(UncannyDevourerEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> MOUTH_OPEN =
            SynchedEntityData.defineId(UncannyDevourerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SINKING =
            SynchedEntityData.defineId(UncannyDevourerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> SPAWN_GAME_TIME =
            SynchedEntityData.defineId(UncannyDevourerEntity.class, EntityDataSerializers.LONG);
    /** Length of the client emerge animation; the server holds still and harmless meanwhile. */
    public static final int EMERGE_TICKS = 32;
    private static final byte CAPTURE_EVENT = 71;
    // Session-only: a seize interrupted by a reload simply has to be won again.
    private UUID seizedPlayerId;
    private int seizeTicks;

    // Client-side animation clocks, driven by synchronized state (spawn time, sinking, capture event).
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState emergeAnimationState = new AnimationState();
    public final AnimationState captureAnimationState = new AnimationState();
    public final AnimationState sinkAnimationState = new AnimationState();
    private float clientMouthOpen = 0.25F;
    private float previousClientMouthOpen = 0.25F;

    /** Players taken and not yet back from their trial (a returned player can be taken again). */
    private final Set<UUID> capturedPlayers = new HashSet<>();
    /** First game time each taken player was seen back in the world, for the regrab grace. */
    private final java.util.Map<UUID, Long> returnSeenAt = new java.util.HashMap<>();
    private int totalCaptures;
    /** Grace after a victim's return (death or survival) before the same Devourer? may take them again. */
    public static final int REGRAB_GRACE_TICKS = 20 * 5;
    private int lifetimeTicks;
    private int noReachableTargetTicks;
    private int nextPulseTick;
    private int nextReachabilityCheckTick;
    private UUID cachedReachableTargetId;
    private boolean arrivalSoundPlayed;
    private long forcedRetreatDeadlineGameTime = Long.MIN_VALUE;
    private long sinkDeadlineGameTime = Long.MIN_VALUE;
    private String sinkReason = "none";

    public UncannyDevourerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        UncannyEntityUtil.applyDisplayName(this, "Devourer?");
        this.nextPulseTick = 100 + random.nextInt(61);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PRIMARY_TARGET, Optional.empty());
        builder.define(MOUTH_OPEN, 0.25F);
        builder.define(SINKING, false);
        builder.define(SPAWN_GAME_TIME, Long.MIN_VALUE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 32.0F));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void initializeFor(ServerPlayer target) {
        this.entityData.set(PRIMARY_TARGET, Optional.of(target.getUUID()));
        this.entityData.set(SPAWN_GAME_TIME, target.level().getGameTime());
        refreshToughness(target, true);
        setTarget(target);
        setPersistenceRequired();
    }

    public int captureCount() {
        return totalCaptures;
    }

    public float mouthOpenAmount() {
        return this.entityData.get(MOUTH_OPEN);
    }

    /** Client mouth value eased between ticks so the portal opens smoothly. */
    public float smoothedMouthOpen(float partialTick) {
        float t = Math.max(0.0F, Math.min(1.0F, partialTick));
        return previousClientMouthOpen + (clientMouthOpen - previousClientMouthOpen) * t;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            return;
        }
        previousClientMouthOpen = clientMouthOpen;
        clientMouthOpen += (mouthOpenAmount() - clientMouthOpen) * 0.25F;
        if (isSinking()) {
            idleAnimationState.stop();
            sinkAnimationState.startIfStopped(tickCount);
            return;
        }
        idleAnimationState.startIfStopped(tickCount);
        if (tickCount == 1 && !emergeAnimationState.isStarted()) {
            startEmergeFromSpawnTime();
        }
    }

    private void startEmergeFromSpawnTime() {
        long spawn = this.entityData.get(SPAWN_GAME_TIME);
        long age = spawn == Long.MIN_VALUE ? EMERGE_TICKS : level().getGameTime() - spawn;
        if (age < EMERGE_TICKS) {
            // Start from the real age so a late-tracking client stays in step with the server.
            emergeAnimationState.start(tickCount - (int) Math.max(0L, age));
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SINKING.equals(key) && level().isClientSide() && isSinking()) {
            idleAnimationState.stop();
            sinkAnimationState.start(tickCount);
        }
        if (SPAWN_GAME_TIME.equals(key) && level().isClientSide() && tickCount == 0) {
            // The spawn time arrives with the entity itself: start emerging before the first frame
            // so the full body never flashes above ground.
            startEmergeFromSpawnTime();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == CAPTURE_EVENT) {
            captureAnimationState.start(tickCount);
            return;
        }
        super.handleEntityEvent(id);
    }

    private boolean isEmerging(ServerLevel level) {
        long spawn = this.entityData.get(SPAWN_GAME_TIME);
        return spawn != Long.MIN_VALUE && level.getGameTime() - spawn < EMERGE_TICKS;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (isDeadOrDying()) {
            // LivingEntity keeps calling aiStep through the death animation: a dying Devourer?
            // must release its victim instead of finishing the seize and capturing anyway.
            seizedPlayerId = null;
            seizeTicks = 0;
            getNavigation().stop();
            return;
        }
        if (isSinking()) {
            tickSinking(level);
            return;
        }
        ensureForcedRetreatDeadline(level);
        lifetimeTicks++;
        if (this.entityData.get(SPAWN_GAME_TIME) == Long.MIN_VALUE) {
            // Older saves and command spawns have no spawn time: treat them as fully emerged.
            this.entityData.set(SPAWN_GAME_TIME, level.getGameTime() - EMERGE_TICKS);
        }
        if (!arrivalSoundPlayed) {
            arrivalSoundPlayed = true;
            playPhysicalLayer(level, true);
        }
        if (lifetimeTicks >= nextPulseTick) {
            playPhysicalLayer(level, false);
            nextPulseTick = lifetimeTicks + 60 + random.nextInt(61);
        }
        if (isEmerging(level)) {
            // The body is still rising out of its portal on every client: no pursuit, no capture.
            getNavigation().stop();
            setTarget(null);
            return;
        }
        if (seizedPlayerId != null) {
            tickSeize(level);
            return;
        }
        boolean captureLimitReached = totalCaptures >= MAX_CAPTURES;
        ServerPlayer target = captureLimitReached ? null : resolveReachableTarget(level);
        if (target != null && this.tickCount % com.eotv.echoofthevoid.event.special.CombatParityRules.REFRESH_INTERVAL_TICKS == 0) {
            refreshToughness(target, false);
        }
        if (target == null) {
            setTarget(null);
            this.entityData.set(MOUTH_OPEN, 0.25F);
            noReachableTargetTicks = Math.min(Integer.MAX_VALUE, noReachableTargetTicks + 1);
            if (DevourerArenaRules.originForcedRetreatRequired(
                    level.getGameTime(),
                    forcedRetreatDeadlineGameTime,
                    false)) {
                beginSinking(level, "three_minute_unreachable_timeout");
                return;
            }
            boolean observed = isObservedByAnyPlayer(level);
            if (DevourerArenaRules.canBeginOriginRetreat(
                    noReachableTargetTicks,
                    false,
                    observed)) {
                String reason = captureLimitReached
                        ? "capture_limit_unreachable"
                        : lifetimeTicks >= MAX_LIFETIME_TICKS
                                ? "expired_unreachable"
                                : "no_reachable_target";
                beginSinking(level, reason);
            }
            return;
        }
        noReachableTargetTicks = 0;
        float distance = (float) Math.sqrt(distanceToSqr(target));
        this.entityData.set(MOUTH_OPEN,
                Math.max(0.25F, Math.min(1.0F, 1.0F - (distance - 1.0F) / 12.0F)));
        if (getTarget() != target) {
            setTarget(target);
        }
        if (isWithinCaptureReach(target) && isEligible(target)) {
            beginSeize(level, target);
        }
    }

    /** Hard to put down (user, 2026-10-08): about 14 s of the player's best sustained damage, 80 HP at least. */
    private void refreshToughness(ServerPlayer player, boolean refill) {
        com.eotv.echoofthevoid.event.special.CombatParity.applyToughness(
                this,
                player,
                com.eotv.echoofthevoid.event.special.CombatParityRules.DEVOURER_PLAYER_SECONDS,
                com.eotv.echoofthevoid.event.special.CombatParityRules.DEVOURER_MIN_HEALTH,
                refill);
        var knockback = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.setBaseValue(0.9D);
        }
    }

    public boolean isSeizing() {
        return seizedPlayerId != null;
    }

    /** Arms close on the victim and the chest portal flares: the capture is seen, then happens. */
    private void beginSeize(ServerLevel level, ServerPlayer target) {
        seizedPlayerId = target.getUUID();
        seizeTicks = 0;
        getNavigation().stop();
        this.entityData.set(MOUTH_OPEN, 1.0F);
        level.broadcastEntityEvent(this, CAPTURE_EVENT);
        level.playSound(null, this, UncannySoundRegistry.DEVOURER_EMERGE.get(), SoundSource.HOSTILE, 1.4F, 0.78F);
        UncannyDiagnostics.recordSpecialLifecycle(
                target,
                this,
                "devourer",
                "capture",
                "seize_started",
                DiagnosticSeverity.INFO,
                UncannyDiagnostics.fields("seize_ticks", SEIZE_TICKS));
    }

    private void tickSeize(ServerLevel level) {
        ServerPlayer target = level.getServer().getPlayerList().getPlayer(seizedPlayerId);
        if (target == null || !isEligible(target)) {
            seizedPlayerId = null;
            seizeTicks = 0;
            return;
        }
        getNavigation().stop();
        getLookControl().setLookAt(target, 90.0F, 90.0F);
        this.entityData.set(MOUTH_OPEN, 1.0F);
        // Draw the victim towards the portal in the chest, gently enough never to fling it.
        Vec3 portal = position().add(0.0D, getBbHeight() * 0.55D, 0.0D)
                .add(Vec3.directionFromRotation(0.0F, getYRot()).scale(0.6D));
        Vec3 pull = portal.subtract(target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
        if (pull.lengthSqr() > 1.0E-4D) {
            target.setDeltaMovement(pull.normalize().scale(Math.min(0.22D, pull.length() * 0.35D)));
            target.hurtMarked = true;
        }
        if (++seizeTicks < SEIZE_TICKS) {
            return;
        }
        seizedPlayerId = null;
        seizeTicks = 0;
        if (DevourerArenaSystem.capturePlayer(this, target)) {
            capturedPlayers.add(target.getUUID());
            returnSeenAt.remove(target.getUUID());
            totalCaptures++;
            invalidateReachabilityCache();
            UncannyDiagnostics.recordSpecialLifecycle(
                    target,
                    this,
                    "devourer",
                    "capture",
                        "hitboxes_contacted",
                        DiagnosticSeverity.INFO,
                        UncannyDiagnostics.fields("capture_count", totalCaptures));
            if (!hasOtherActivePlayer(level, target.getUUID())) {
                // The origin chunk normally unloads during a solo trial. Removing the source now
                // prevents its lifetime counter from freezing and resuming after the return.
                traceDeparture(level, "solo_capture_complete", "immediate_unwitnessed");
                discard();
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // MeleeAttackGoal is retained purely for pathing. Capture is checked against the real
        // intersecting hitboxes above and never delegates to Vanilla melee damage.
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isSinking() && super.isPickable();
    }

    public boolean isWithinCaptureReach(Entity target) {
        return target != null
                && getBoundingBox()
                        .inflate(CAPTURE_HORIZONTAL_REACH, CAPTURE_VERTICAL_REACH, CAPTURE_HORIZONTAL_REACH)
                        .intersects(target.getBoundingBox());
    }

    private ServerPlayer resolveReachableTarget(ServerLevel level) {
        if (lifetimeTicks < nextReachabilityCheckTick) {
            if (cachedReachableTargetId == null) {
                return null;
            }
            ServerPlayer cached = level.getServer().getPlayerList().getPlayer(cachedReachableTargetId);
            if (isEligible(cached)) {
                return cached;
            }
        }
        nextReachabilityCheckTick = lifetimeTicks + DevourerArenaRules.ORIGIN_REACHABILITY_REFRESH_TICKS;
        cachedReachableTargetId = null;
        UUID preferred = this.entityData.get(PRIMARY_TARGET).orElse(null);
        if (preferred != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(preferred);
            if (isReachable(player)) {
                cachedReachableTargetId = player.getUUID();
                return player;
            }
        }
        ServerPlayer nearest = null;
        double bestDistance = 32.0D * 32.0D;
        for (ServerPlayer player : level.players()) {
            double distance = distanceToSqr(player);
            if (distance < bestDistance && isReachable(player)) {
                nearest = player;
                bestDistance = distance;
            }
        }
        if (nearest != null) {
            cachedReachableTargetId = nearest.getUUID();
        }
        return nearest;
    }

    private boolean isReachable(ServerPlayer player) {
        if (!isEligible(player)) {
            return false;
        }
        if (isWithinCaptureReach(player)) {
            return true;
        }
        if (player.isInWater() || this.isInWater()) {
            // No ground path crosses water, but it swims after its prey (UncannySwimming).
            return true;
        }
        var path = this.getNavigation().createPath(player, 1);
        return path != null && path.canReach();
    }

    private boolean isEligible(ServerPlayer player) {
        return player != null
                && player.level() == level()
                && player.isAlive()
                && !player.isSpectator()
                && !player.getAbilities().invulnerable
                && distanceToSqr(player) <= 32.0D * 32.0D
                && !DevourerArenaSystem.hasSession(player.getUUID())
                && isBackFromTrial(player.getUUID());
    }

    /**
     * A player already taken comes back (dead or alive) and becomes prey again after a short grace,
     * so a Devourer? still standing at the origin keeps hunting the one who returns.
     */
    private boolean isBackFromTrial(UUID playerId) {
        if (!capturedPlayers.contains(playerId)) {
            return true;
        }
        long now = level().getGameTime();
        Long seen = returnSeenAt.putIfAbsent(playerId, now);
        if (seen == null || now - seen < REGRAB_GRACE_TICKS) {
            return false;
        }
        capturedPlayers.remove(playerId);
        returnSeenAt.remove(playerId);
        return true;
    }

    public boolean isSinking() {
        return this.entityData.get(SINKING);
    }

    private boolean hasOtherActivePlayer(ServerLevel level, UUID capturedPlayerId) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (!player.getUUID().equals(capturedPlayerId)
                    && player.isAlive()
                    && !player.isSpectator()) {
                return true;
            }
        }
        return false;
    }

    private void beginSinking(ServerLevel level, String reason) {
        this.sinkReason = reason;
        this.sinkDeadlineGameTime = level.getGameTime() + 40L;
        this.entityData.set(SINKING, true);
        this.entityData.set(MOUTH_OPEN, 0.0F);
        this.setTarget(null);
        this.getNavigation().stop();
        this.setNoAi(true);
        this.setNoGravity(true);
        this.noPhysics = true;
        String departureMode = "three_minute_unreachable_timeout".equals(reason)
                ? "forced_timeout_sink"
                : "unobserved_sink";
        traceDeparture(level, reason, departureMode);
    }

    private boolean isObservedByAnyPlayer(ServerLevel level) {
        Vec3 target = this.getEyePosition();
        double rangeSqr = DevourerArenaRules.ORIGIN_OBSERVER_RANGE
                * DevourerArenaRules.ORIGIN_OBSERVER_RANGE;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            Vec3 origin = player.getEyePosition();
            Vec3 toDevourer = target.subtract(origin);
            if (toDevourer.lengthSqr() > rangeSqr) {
                continue;
            }
            if (toDevourer.lengthSqr() < 1.0E-6D) {
                return true;
            }
            Vec3 look = player.getViewVector(1.0F).normalize();
            if (look.dot(toDevourer.normalize()) <= 0.05D) {
                continue;
            }
            if (player.level().clip(new ClipContext(
                            origin,
                            target,
                            ClipContext.Block.VISUAL,
                            ClipContext.Fluid.NONE,
                            player))
                    .getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    private void invalidateReachabilityCache() {
        cachedReachableTargetId = null;
        nextReachabilityCheckTick = 0;
    }

    private void ensureForcedRetreatDeadline(ServerLevel level) {
        if (forcedRetreatDeadlineGameTime == Long.MIN_VALUE) {
            forcedRetreatDeadlineGameTime = DevourerArenaRules.originForcedRetreatDeadline(
                    level.getGameTime(),
                    lifetimeTicks);
        }
    }

    private void tickSinking(ServerLevel level) {
        this.setTarget(null);
        this.getNavigation().stop();
        this.setNoAi(true);
        this.setNoGravity(true);
        this.noPhysics = true;
        if (sinkDeadlineGameTime == Long.MIN_VALUE) {
            sinkDeadlineGameTime = level.getGameTime() + 40L;
        }
        if (level.getGameTime() >= sinkDeadlineGameTime) {
            UncannySinkTransition.vanish(this);
            return;
        }
        double step = UncannySinkTransition.step(this, 0.045D, 40);
        this.setDeltaMovement(0.0D, -step, 0.0D);
        this.setPos(this.getX(), this.getY() - step, this.getZ());
        if (UncannySinkTransition.breaksIntoOpenSpace(this)) {
            UncannySinkTransition.vanish(this);
        }
    }

    private void playPhysicalLayer(ServerLevel level, boolean arrival) {
        // Physical, spatial and shared. The distorted portal hum itself is a client loop bound to
        // the entity (UncannyDevourerPortalSound); the server only adds the tearing arrival and
        // the creature's short rattles.
        level.playSound(
                null,
                this,
                arrival
                        ? UncannySoundRegistry.DEVOURER_EMERGE.get()
                        : UncannySoundRegistry.DEVOURER_RATTLE.get(),
                SoundSource.HOSTILE,
                arrival ? 1.6F : 0.9F,
                arrival ? 1.0F : 0.9F + random.nextFloat() * 0.2F);
    }

    private void traceDeparture(ServerLevel level, String reason, String mode) {
        ServerPlayer player = getTarget() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        UncannyDiagnostics.recordSpecialLifecycle(
                player,
                this,
                "devourer",
                "departed",
                reason,
                DiagnosticSeverity.INFO,
                UncannyDiagnostics.fields(
                        "captures", capturedPlayers.size(),
                        "lifetime_ticks", lifetimeTicks,
                        "forced_retreat_deadline", forcedRetreatDeadlineGameTime,
                        "departure_mode", mode,
                        "sink_deadline", sinkDeadlineGameTime));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(PRIMARY_TARGET).ifPresent(id -> tag.putUUID("PrimaryTarget", id));
        tag.putInt("DevourerLifetime", lifetimeTicks);
        tag.putInt("DevourerNoTargetTicks", noReachableTargetTicks);
        tag.putInt("DevourerNextPulse", nextPulseTick);
        tag.putBoolean("DevourerArrivalSound", arrivalSoundPlayed);
        tag.putLong("DevourerForcedRetreatDeadline", forcedRetreatDeadlineGameTime);
        tag.putBoolean("DevourerSinking", isSinking());
        tag.putLong("DevourerSinkDeadline", sinkDeadlineGameTime);
        tag.putString("DevourerSinkReason", sinkReason);
        tag.putLong("DevourerSpawnGameTime", this.entityData.get(SPAWN_GAME_TIME));
        ListTag captured = new ListTag();
        for (UUID id : capturedPlayers) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", id);
            captured.add(entry);
        }
        tag.put("CapturedPlayers", captured);
        tag.putInt("DevourerTotalCaptures", totalCaptures);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("PrimaryTarget")) {
            this.entityData.set(PRIMARY_TARGET, Optional.of(tag.getUUID("PrimaryTarget")));
        }
        lifetimeTicks = Math.max(0, tag.getInt("DevourerLifetime"));
        noReachableTargetTicks = Math.max(0, tag.getInt("DevourerNoTargetTicks"));
        invalidateReachabilityCache();
        nextPulseTick = Math.max(lifetimeTicks + 1, tag.getInt("DevourerNextPulse"));
        arrivalSoundPlayed = tag.getBoolean("DevourerArrivalSound");
        forcedRetreatDeadlineGameTime = tag.contains("DevourerForcedRetreatDeadline")
                ? tag.getLong("DevourerForcedRetreatDeadline")
                : Long.MIN_VALUE;
        this.entityData.set(SINKING, tag.getBoolean("DevourerSinking"));
        sinkDeadlineGameTime = tag.contains("DevourerSinkDeadline")
                ? tag.getLong("DevourerSinkDeadline")
                : Long.MIN_VALUE;
        sinkReason = tag.contains("DevourerSinkReason") ? tag.getString("DevourerSinkReason") : "legacy";
        if (tag.contains("DevourerSpawnGameTime")) {
            this.entityData.set(SPAWN_GAME_TIME, tag.getLong("DevourerSpawnGameTime"));
        }
        capturedPlayers.clear();
        ListTag captured = tag.getList("CapturedPlayers", Tag.TAG_COMPOUND);
        for (int index = 0; index < captured.size() && capturedPlayers.size() < MAX_CAPTURES; index++) {
            CompoundTag entry = captured.getCompound(index);
            if (entry.hasUUID("Player")) {
                capturedPlayers.add(entry.getUUID("Player"));
            }
        }
        totalCaptures = tag.contains("DevourerTotalCaptures")
                ? Math.max(0, tag.getInt("DevourerTotalCaptures"))
                : capturedPlayers.size();
        if (isSinking()) {
            this.setNoAi(true);
            this.setNoGravity(true);
            this.noPhysics = true;
        }
        setPersistenceRequired();
    }
}
