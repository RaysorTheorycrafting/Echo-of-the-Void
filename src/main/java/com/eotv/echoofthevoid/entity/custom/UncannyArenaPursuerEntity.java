package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.event.special.AdaptiveSpecialCombatProfile;
import com.eotv.echoofthevoid.event.special.ArenaPursuerAppearance;
import com.eotv.echoofthevoid.event.special.DevourerArenaRules;
import com.eotv.echoofthevoid.event.special.DevourerArenaSystem;
import com.eotv.echoofthevoid.world.UncannyDimensions;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Internal, reward-free wall-climbing threat used only by an active Elsewhere session. */
public final class UncannyArenaPursuerEntity extends Spider {
    private static final EntityDataAccessor<Optional<UUID>> SESSION_PLAYER =
            SynchedEntityData.defineId(UncannyArenaPursuerEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Byte> APPEARANCE =
            SynchedEntityData.defineId(UncannyArenaPursuerEntity.class, EntityDataSerializers.BYTE);

    private long cellIndex = -1L;
    private BlockPos scratchPosition;
    private int scratchTicks;
    private int emergenceTicks;
    private double emergenceTargetY;

    public UncannyArenaPursuerEntity(EntityType<? extends Spider> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        setSilent(true);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Only the Spider silhouette may pounce; every other form keeps the gait of its model.
        this.goalSelector.removeAllGoals(goal -> goal instanceof LeapAtTargetGoal);
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F) {
            @Override
            public boolean canUse() {
                return appearance().climbs() && super.canUse();
            }
        });
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SESSION_PLAYER, Optional.empty());
        builder.define(APPEARANCE, (byte) ArenaPursuerAppearance.HUMANOID.id());
    }

    public void initializeFor(ServerPlayer player, long cellIndex) {
        this.entityData.set(SESSION_PLAYER, Optional.of(player.getUUID()));
        this.cellIndex = cellIndex;
        DevourerArenaRules.pursuerProfile(AdaptiveSpecialCombatProfile.attacker(player)).applyTo(this, true);
        setSilent(true);
        setTarget(player);
        setPersistenceRequired();
    }

    public void beginEmergence(double surfaceY) {
        emergenceTicks = DevourerArenaRules.PURSUER_EMERGENCE_TICKS;
        emergenceTargetY = surfaceY;
        setPos(getX(), surfaceY - 1.6D, getZ());
        setNoAi(true);
        setInvulnerable(true);
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
    }

    public boolean isEmerging() {
        return emergenceTicks > 0;
    }

    public ArenaPursuerAppearance appearance() {
        return ArenaPursuerAppearance.byId(this.entityData.get(APPEARANCE));
    }

    public void setAppearance(ArenaPursuerAppearance appearance) {
        ArenaPursuerAppearance safe = appearance == null
                ? ArenaPursuerAppearance.HUMANOID
                : appearance;
        this.entityData.set(APPEARANCE, (byte) safe.id());
        if (!level().isClientSide()) {
            // A walking silhouette must not inherit the Spider's wall-climbing path planner.
            this.navigation = safe.climbs()
                    ? new WallClimberNavigation(this, level())
                    : new GroundPathNavigation(this, level());
        }
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (APPEARANCE.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        ArenaPursuerAppearance appearance = appearance();
        return EntityDimensions.scalable(appearance.width(), appearance.height());
    }

    @Override
    public boolean onClimbable() {
        return appearance().climbs() && super.onClimbable();
    }

    public Optional<UUID> sessionPlayerId() {
        return this.entityData.get(SESSION_PLAYER);
    }

    public long cellIndex() {
        return cellIndex;
    }

    @Override
    public void aiStep() {
        // These internal threats are silent and never outlined: Elsewhere's close fog alone lets them
        // surface a moment before reaching the player. Reassert both flags so legacy NBT (older
        // builds saved Glowing) or an external command cannot reintroduce audio or an outline.
        setSilent(true);
        setGlowingTag(false);
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (!UncannyDimensions.isElsewhere(level)) {
            discard();
            return;
        }
        if (emergenceTicks > 0) {
            tickEmergence(level);
            return;
        }
        ServerPlayer player = sessionPlayerId()
                .map(id -> level.getServer().getPlayerList().getPlayer(id))
                .orElse(null);
        if (player == null || !player.isAlive() || player.level() != level
                || !DevourerArenaSystem.isActiveSession(level, player.getUUID(), cellIndex)) {
            resetScratch();
            if (player != null && !DevourerArenaSystem.hasSession(level, player.getUUID())) {
                discard();
            }
            return;
        }
        if (getTarget() != player) {
            setTarget(player);
        }
        if (tickCount % 5 == 0) {
            tickBlockedPlacement(level, player);
        }
    }

    private void tickEmergence(ServerLevel level) {
        int before = emergenceTicks;
        emergenceTicks--;
        double progress = 1.0D - emergenceTicks / (double) DevourerArenaRules.PURSUER_EMERGENCE_TICKS;
        setPos(getX(), emergenceTargetY - 1.6D + 1.6D * progress, getZ());
        if (before == DevourerArenaRules.PURSUER_EMERGENCE_TICKS) {
            BlockState floor = level.getBlockState(blockPosition().below());
            level.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, floor),
                    getX(), emergenceTargetY, getZ(), 12, 0.5D, 0.05D, 0.5D, 0.01D);
        }
        if (emergenceTicks <= 0) {
            setPos(getX(), emergenceTargetY, getZ());
            setNoAi(false);
            setInvulnerable(false);
            noPhysics = false;
        }
    }

    @Override
    public boolean isPickable() {
        return !isEmerging() && super.isPickable();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return !isEmerging() && super.doHurtTarget(target);
    }

    private void tickBlockedPlacement(ServerLevel level, ServerPlayer player) {
        if (distanceToSqr(player) <= 2.5D * 2.5D
                || !DevourerArenaRules.mayScratchTowards(getY(), player.getY())) {
            // A target up on a pillar is the Spiders' job: walkers never dig the pillar away.
            resetScratch();
            return;
        }
        // Reuse the path the navigation already follows; a fresh A* per pursuer every few ticks was
        // the single heaviest cost of the trial. Only a pursuer with no path at all asks again, staggered.
        var path = getNavigation().getPath();
        if (path == null && !horizontalCollision) {
            if ((tickCount + getId()) % 20 != 0) {
                return;
            }
            path = getNavigation().createPath(player, 0);
        }
        boolean blocked = horizontalCollision || path == null || !path.canReach();
        if (!blocked) {
            resetScratch();
            return;
        }
        BlockPos candidate = findBlockingPlacement(player);
        if (candidate == null) {
            resetScratch();
            return;
        }
        if (!candidate.equals(scratchPosition)) {
            scratchPosition = candidate.immutable();
            scratchTicks = 0;
        }
        scratchTicks += 5;
        if (scratchTicks == 5 || scratchTicks == 20 || scratchTicks == 40) {
            BlockState state = level.getBlockState(scratchPosition);
            level.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, state),
                    scratchPosition.getX() + 0.5D,
                    scratchPosition.getY() + 0.5D,
                    scratchPosition.getZ() + 0.5D,
                    4,
                    0.22D,
                    0.22D,
                    0.22D,
                    0.015D);
        }
        if (scratchTicks >= DevourerArenaRules.SCRATCH_TELEGRAPH_TICKS
                && DevourerArenaSystem.removeTrackedPlacement(
                        player.getUUID(), cellIndex, scratchPosition, this)) {
            resetScratch();
            getNavigation().moveTo(player, 1.0D);
        }
    }

    private BlockPos findBlockingPlacement(ServerPlayer player) {
        Vec3 toward = player.position().subtract(position());
        if (toward.horizontalDistanceSqr() < 0.01D) {
            return null;
        }
        Vec3 direction = new Vec3(toward.x, 0.0D, toward.z).normalize();
        BlockPos feet = BlockPos.containing(
                getX() + direction.x * 0.75D,
                getY(),
                getZ() + direction.z * 0.75D);
        UUID playerId = sessionPlayerId().orElse(null);
        if (playerId == null) {
            return null;
        }
        if (DevourerArenaSystem.isTrackedPlacement((ServerLevel) level(), playerId, cellIndex, feet)) {
            return feet;
        }
        BlockPos head = feet.above();
        return DevourerArenaSystem.isTrackedPlacement((ServerLevel) level(), playerId, cellIndex, head) ? head : null;
    }

    private void resetScratch() {
        scratchPosition = null;
        scratchTicks = 0;
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
    protected void playStepSound(BlockPos pos, BlockState state) {
        // Silent on purpose: Elsewhere's close fog is the only telegraph of an approaching pursuer.
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        sessionPlayerId().ifPresent(id -> tag.putUUID("ArenaPlayer", id));
        tag.putLong("ArenaCell", cellIndex);
        tag.putByte("ArenaAppearance", (byte) appearance().id());
        tag.putInt("ScratchTicks", scratchTicks);
        tag.putInt("ArenaEmergenceTicks", emergenceTicks);
        tag.putDouble("ArenaEmergenceTargetY", emergenceTargetY);
        if (scratchPosition != null) {
            tag.putLong("ScratchPosition", scratchPosition.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("ArenaPlayer")) {
            this.entityData.set(SESSION_PLAYER, Optional.of(tag.getUUID("ArenaPlayer")));
        }
        cellIndex = tag.contains("ArenaCell") ? tag.getLong("ArenaCell") : -1L;
        setAppearance(tag.contains("ArenaAppearance")
                ? ArenaPursuerAppearance.byId(tag.getByte("ArenaAppearance"))
                : ArenaPursuerAppearance.byId(random.nextInt(ArenaPursuerAppearance.count())));
        scratchTicks = Math.max(0, tag.getInt("ScratchTicks"));
        scratchPosition = tag.contains("ScratchPosition")
                ? BlockPos.of(tag.getLong("ScratchPosition"))
                : null;
        emergenceTicks = Math.max(0, Math.min(
                DevourerArenaRules.PURSUER_EMERGENCE_TICKS,
                tag.getInt("ArenaEmergenceTicks")));
        emergenceTargetY = tag.contains("ArenaEmergenceTargetY")
                ? tag.getDouble("ArenaEmergenceTargetY")
                : getY();
        setNoAi(emergenceTicks > 0);
        setInvulnerable(emergenceTicks > 0);
        noPhysics = emergenceTicks > 0;
        setSilent(true);
        setGlowingTag(false);
        setPersistenceRequired();
    }
}
