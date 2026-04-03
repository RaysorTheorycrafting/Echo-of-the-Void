package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.phase.UncannyPhase;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class UncannySpiderEntity extends Spider implements UncannyEntityMarker {
    private static final EntityDataAccessor<Integer> SPIDER_VARIANT =
            SynchedEntityData.defineId(UncannySpiderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> SENSORIAL_HOST =
            SynchedEntityData.defineId(UncannySpiderEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final List<TemporaryWebTask> TEMPORARY_WEBS = new ArrayList<>();
    private static final List<EggHeartbeatTask> EGG_HEARTBEATS = new ArrayList<>();

    private int webPlaceCooldown;
    private boolean fakeDeathActive;
    private boolean fakeDeathTriggered;
    private long fakeDeathStartTick;
    private int scratchLoopCooldown;
    private float lastSensorialHostHealth = -1.0F;
    private int sensorialAgeTicks;
    private int sensorialBrightTicks;

    public UncannySpiderEntity(EntityType<? extends Spider> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Spider?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SPIDER_VARIANT, SpiderVariant.UNASSIGNED.id());
        builder.define(SENSORIAL_HOST, Optional.empty());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);

        SpiderVariant variant = getSpiderVariant();
        if (variant == SpiderVariant.UNASSIGNED && this.level() instanceof ServerLevel serverLevel) {
            ensureVariantAssigned(serverLevel);
            variant = getSpiderVariant();
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        long now = serverLevel.getServer().getTickCount();
        tickTemporaryWebs(serverLevel, now);
        tickEggHeartbeats(serverLevel, now);

        switch (variant) {
            case CREEPING_SHADOW -> tickCreepingShadow();
            case GHOST_WEAVER -> tickGhostWeaver(serverLevel, now);
            case WALKING_NEST -> tickWalkingNest();
            case FALSE_DEATH -> tickFalseDeath(serverLevel, now);
            case SENSORIAL_PHOBIA -> tickSensorialPhobia(serverLevel);
            case UNASSIGNED -> {
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (getSpiderVariant() == SpiderVariant.SENSORIAL_PHOBIA) {
            return false;
        }
        if (this.fakeDeathActive) {
            return false;
        }
        return super.doHurtTarget(entity);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (getSpiderVariant() == SpiderVariant.FALSE_DEATH) {
            if (this.fakeDeathActive) {
                return false;
            }

            if (!this.fakeDeathTriggered) {
                float threshold = Math.max(1.0F, this.getMaxHealth() * 0.5F);
                if (this.getHealth() - amount <= threshold) {
                    this.fakeDeathTriggered = true;
                    this.fakeDeathActive = true;
                    if (this.level() instanceof ServerLevel serverLevel) {
                        this.fakeDeathStartTick = serverLevel.getServer().getTickCount();
                        serverLevel.playSound(
                                null,
                                this.getX(),
                                this.getY(),
                                this.getZ(),
                                SoundEvents.SPIDER_DEATH,
                                this.getSoundSource(),
                                1.0F,
                                1.0F);
                    }
                    this.setHealth(threshold);
                    return true;
                }
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (getSpiderVariant() == SpiderVariant.SENSORIAL_PHOBIA && source.is(DamageTypes.IN_WALL)) {
            return true;
        }
        if (this.fakeDeathActive) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public void die(DamageSource damageSource) {
        if (this.level() instanceof ServerLevel serverLevel && getSpiderVariant() == SpiderVariant.WALKING_NEST) {
            this.xpReward = 0;
            spawnNestEggWave(serverLevel);
        }
        super.die(damageSource);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        if (getSpiderVariant() == SpiderVariant.WALKING_NEST) {
            return;
        }
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        UncannyEntityUtil.suppressStepSound(this, pos, blockState);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("UncannySpiderVariant", getSpiderVariant().id());
        this.entityData.get(SENSORIAL_HOST).ifPresent(uuid -> tag.putUUID("UncannySensorialHost", uuid));
        tag.putInt("UncannyWebPlaceCooldown", this.webPlaceCooldown);
        tag.putBoolean("UncannyFakeDeathActive", this.fakeDeathActive);
        tag.putBoolean("UncannyFakeDeathTriggered", this.fakeDeathTriggered);
        tag.putLong("UncannyFakeDeathStartTick", this.fakeDeathStartTick);
        tag.putInt("UncannyScratchLoopCooldown", this.scratchLoopCooldown);
        tag.putFloat("UncannyLastHostHealth", this.lastSensorialHostHealth);
        tag.putInt("UncannySensorialAgeTicks", this.sensorialAgeTicks);
        tag.putInt("UncannySensorialBrightTicks", this.sensorialBrightTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSpiderVariant(SpiderVariant.fromId(tag.getInt("UncannySpiderVariant")));
        if (tag.hasUUID("UncannySensorialHost")) {
            this.entityData.set(SENSORIAL_HOST, Optional.of(tag.getUUID("UncannySensorialHost")));
        } else {
            this.entityData.set(SENSORIAL_HOST, Optional.empty());
        }
        this.webPlaceCooldown = Math.max(0, tag.getInt("UncannyWebPlaceCooldown"));
        this.fakeDeathActive = tag.getBoolean("UncannyFakeDeathActive");
        this.fakeDeathTriggered = tag.getBoolean("UncannyFakeDeathTriggered");
        this.fakeDeathStartTick = tag.getLong("UncannyFakeDeathStartTick");
        this.scratchLoopCooldown = Math.max(0, tag.getInt("UncannyScratchLoopCooldown"));
        this.lastSensorialHostHealth = tag.getFloat("UncannyLastHostHealth");
        this.sensorialAgeTicks = Math.max(0, tag.getInt("UncannySensorialAgeTicks"));
        this.sensorialBrightTicks = Math.max(0, tag.getInt("UncannySensorialBrightTicks"));
    }

    public SpiderVariant getSpiderVariant() {
        return SpiderVariant.fromId(this.entityData.get(SPIDER_VARIANT));
    }

    private void setSpiderVariant(SpiderVariant variant) {
        this.entityData.set(SPIDER_VARIANT, variant.id());
    }

    private void ensureVariantAssigned(ServerLevel serverLevel) {
        if (getSpiderVariant() != SpiderVariant.UNASSIGNED) {
            return;
        }
        UncannyPhase phase = UncannyWorldState.get(serverLevel.getServer()).getPhase();
        setSpiderVariant(rollVariantForPhase(phase));
    }

    private SpiderVariant rollVariantForPhase(UncannyPhase phase) {
        int roll = this.random.nextInt(100);
        return switch (phase) {
            case PHASE_1 -> SpiderVariant.CREEPING_SHADOW;
            case PHASE_2 -> roll < 68 ? SpiderVariant.GHOST_WEAVER : SpiderVariant.CREEPING_SHADOW;
            case PHASE_3 -> {
                if (roll < 62) {
                    yield SpiderVariant.WALKING_NEST;
                }
                if (roll < 90) {
                    yield SpiderVariant.GHOST_WEAVER;
                }
                yield SpiderVariant.CREEPING_SHADOW;
            }
            case PHASE_4 -> {
                if (roll < 35) {
                    yield SpiderVariant.FALSE_DEATH;
                }
                if (roll < 62) {
                    yield SpiderVariant.SENSORIAL_PHOBIA;
                }
                if (roll < 85) {
                    yield SpiderVariant.WALKING_NEST;
                }
                yield SpiderVariant.GHOST_WEAVER;
            }
        };
    }

    private void tickCreepingShadow() {
        LivingEntity target = this.getTarget();
        boolean jumpAttacking = target != null && !this.onGround() && this.distanceToSqr(target) <= 10.0D;
        this.setSilent(!jumpAttacking);
        this.setInvisible(false);
    }

    private void tickGhostWeaver(ServerLevel level, long now) {
        this.setSilent(true);
        this.setInvisible(false);

        Player target = resolveNearbyPlayer(level, 24.0D);
        if (target == null) {
            return;
        }

        this.setTarget(target);
        this.getNavigation().moveTo(target, 1.05D);

        if (this.webPlaceCooldown > 0) {
            this.webPlaceCooldown--;
            return;
        }
        this.webPlaceCooldown = 20;

        BlockPos candidate = findWebPlacementPos(target.blockPosition());
        if (candidate == null) {
            return;
        }

        level.setBlockAndUpdate(candidate, UncannyBlockRegistry.UNCANNY_BLACK_WEB.get().defaultBlockState());
        TEMPORARY_WEBS.add(new TemporaryWebTask(level.dimension(), candidate.immutable(), now + 20L * 30L));
    }

    private void tickWalkingNest() {
        this.setSilent(false);
        this.setInvisible(false);
    }

    private void tickFalseDeath(ServerLevel level, long now) {
        this.setInvisible(false);

        if (!this.fakeDeathTriggered && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            this.fakeDeathTriggered = true;
            this.fakeDeathActive = true;
            this.fakeDeathStartTick = now;
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SPIDER_DEATH, this.getSoundSource(), 1.0F, 1.0F);
        }

        if (!this.fakeDeathActive) {
            return;
        }

        this.setSilent(true);
        this.setTarget(null);
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
        this.zza = 0.0F;
        this.xxa = 0.0F;

        Player nearest = level.getNearestPlayer(this, 2.0D);
        if (nearest != null || now - this.fakeDeathStartTick >= 60L) {
            this.fakeDeathActive = false;
            this.setSilent(false);
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 10, 1, false, false, true));
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SPIDER_AMBIENT, this.getSoundSource(), 2.5F, 0.55F);
            if (nearest != null) {
                this.setTarget(nearest);
            }
        }
    }

    private void tickSensorialPhobia(ServerLevel level) {
        this.setInvisible(true);
        this.setSilent(true);
        this.setNoGravity(true);
        this.getNavigation().stop();
        this.sensorialAgeTicks++;

        ServerPlayer host = resolveSensorialHost(level);
        if (host == null || !host.isAlive()) {
            this.discard();
            return;
        }

        if (host.isInWater()) {
            this.discard();
            return;
        }

        if (level.getBrightness(LightLayer.BLOCK, host.blockPosition()) >= 15) {
            this.sensorialBrightTicks++;
        } else {
            this.sensorialBrightTicks = 0;
        }
        if (this.sensorialAgeTicks > 30 && this.sensorialBrightTicks >= 20) {
            this.discard();
            return;
        }
        this.lastSensorialHostHealth = host.getHealth();

        Vec3 eye = host.getEyePosition();
        Vec3 offset = host.getLookAngle().normalize().scale(0.15D);
        this.teleportTo(eye.x - offset.x, eye.y - 0.18D, eye.z - offset.z);
        this.setDeltaMovement(Vec3.ZERO);

        if (this.scratchLoopCooldown > 0) {
            this.scratchLoopCooldown--;
        } else {
            this.scratchLoopCooldown = 45 + this.random.nextInt(35);
            level.playSound(null, host, SoundEvents.SPIDER_STEP, this.getSoundSource(), 0.7F, 0.7F + this.random.nextFloat() * 0.4F);
        }
    }

    private void spawnNestEggWave(ServerLevel level) {
        BlockPos center = this.blockPosition();
        long now = level.getServer().getTickCount();
        int eggCount = 3 + this.random.nextInt(3);

        for (int i = 0; i < eggCount; i++) {
            BlockPos eggPos = findEggPlacementPos(level, center);
            if (eggPos == null) {
                continue;
            }

            level.setBlockAndUpdate(eggPos, UncannyBlockRegistry.UNCANNY_EGG.get().defaultBlockState());
            level.scheduleTick(eggPos, UncannyBlockRegistry.UNCANNY_EGG.get(), 60);
            EGG_HEARTBEATS.add(new EggHeartbeatTask(level.dimension(), eggPos.immutable(), now + 60L, now + 8L));
        }
    }

    @Nullable
    private BlockPos findEggPlacementPos(ServerLevel level, BlockPos center) {
        for (int attempt = 0; attempt < 24; attempt++) {
            int x = center.getX() + this.random.nextInt(5) - 2;
            int z = center.getZ() + this.random.nextInt(5) - 2;
            int y = center.getY() + this.random.nextInt(3) - 1;
            BlockPos candidate = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(candidate);
            if (!state.canBeReplaced()) {
                continue;
            }
            if (!level.getBlockState(candidate.below()).isSolidRender(level, candidate.below())) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    @Nullable
    private BlockPos findWebPlacementPos(BlockPos targetPos) {
        for (int attempt = 0; attempt < 8; attempt++) {
            BlockPos candidate = targetPos.offset(this.random.nextInt(3) - 1, 0, this.random.nextInt(3) - 1);
            if (!this.level().getBlockState(candidate).canBeReplaced()) {
                continue;
            }
            if (!this.level().getBlockState(candidate.below()).isSolidRender(this.level(), candidate.below())) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    @Nullable
    private Player resolveNearbyPlayer(ServerLevel level, double radius) {
        LivingEntity target = this.getTarget();
        if (target instanceof Player player && player.isAlive()) {
            return player;
        }
        return level.getNearestPlayer(this, radius);
    }

    @Nullable
    private ServerPlayer resolveSensorialHost(ServerLevel level) {
        Optional<UUID> hostUuid = this.entityData.get(SENSORIAL_HOST);
        if (hostUuid.isPresent()) {
            ServerPlayer existing = level.getServer().getPlayerList().getPlayer(hostUuid.get());
            if (existing != null && existing.isAlive()) {
                return existing;
            }
        }

        ServerPlayer nearest = level.getNearestPlayer(this, 22.0D) instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        if (nearest != null) {
            this.entityData.set(SENSORIAL_HOST, Optional.of(nearest.getUUID()));
        }
        return nearest;
    }

    private static void tickTemporaryWebs(ServerLevel level, long now) {
        Iterator<TemporaryWebTask> iterator = TEMPORARY_WEBS.iterator();
        while (iterator.hasNext()) {
            TemporaryWebTask task = iterator.next();
            if (task.dimension() != level.dimension()) {
                continue;
            }
            if (task.expireTick() > now) {
                continue;
            }

            if (level.getBlockState(task.pos()).is(UncannyBlockRegistry.UNCANNY_BLACK_WEB.get())) {
                level.removeBlock(task.pos(), false);
            }
            iterator.remove();
        }
    }

    private static void tickEggHeartbeats(ServerLevel level, long now) {
        Iterator<EggHeartbeatTask> iterator = EGG_HEARTBEATS.iterator();
        while (iterator.hasNext()) {
            EggHeartbeatTask task = iterator.next();
            if (task.dimension() != level.dimension()) {
                continue;
            }

            BlockState state = level.getBlockState(task.pos());
            if (!state.is(UncannyBlockRegistry.UNCANNY_EGG.get())) {
                iterator.remove();
                continue;
            }

            if (now >= task.hatchTick()) {
                iterator.remove();
                continue;
            }

            if (now >= task.nextBeatTick()) {
                level.playSound(null, task.pos(), SoundEvents.WARDEN_HEARTBEAT, net.minecraft.sounds.SoundSource.HOSTILE, 0.85F, 0.85F);
                task.setNextBeatTick(now + 10L);
            }
        }
    }

    public enum SpiderVariant {
        UNASSIGNED(0),
        CREEPING_SHADOW(1),
        GHOST_WEAVER(2),
        WALKING_NEST(3),
        FALSE_DEATH(4),
        SENSORIAL_PHOBIA(5);

        private final int id;

        SpiderVariant(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static SpiderVariant fromId(int id) {
            for (SpiderVariant variant : values()) {
                if (variant.id == id) {
                    return variant;
                }
            }
            return UNASSIGNED;
        }
    }

    private record TemporaryWebTask(ResourceKey<Level> dimension, BlockPos pos, long expireTick) {
    }

    private static final class EggHeartbeatTask {
        private final ResourceKey<Level> dimension;
        private final BlockPos pos;
        private final long hatchTick;
        private long nextBeatTick;

        private EggHeartbeatTask(ResourceKey<Level> dimension, BlockPos pos, long hatchTick, long nextBeatTick) {
            this.dimension = dimension;
            this.pos = pos;
            this.hatchTick = hatchTick;
            this.nextBeatTick = nextBeatTick;
        }

        private ResourceKey<Level> dimension() {
            return this.dimension;
        }

        private BlockPos pos() {
            return this.pos;
        }

        private long hatchTick() {
            return this.hatchTick;
        }

        private long nextBeatTick() {
            return this.nextBeatTick;
        }

        private void setNextBeatTick(long nextBeatTick) {
            this.nextBeatTick = nextBeatTick;
        }
    }
}
