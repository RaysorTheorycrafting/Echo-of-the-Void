package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.event.UncannyParanoiaEventSystem;
import com.eotv.echoofthevoid.phase.UncannyPhase;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class UncannyCreeperEntity extends Creeper implements UncannyEntityMarker {
    private static final EntityDataAccessor<Integer> CREEPER_VARIANT =
            SynchedEntityData.defineId(UncannyCreeperEntity.class, EntityDataSerializers.INT);

    private static final int FUSE_DURATION_TICKS = 35;
    private static final int FALSE_ALERT_AUDIO_TICKS = 30;

    private boolean fuseActive;
    private int fuseTicks;
    private int fakeHissCooldown = 35;
    private boolean silhouetteDirectionInitialized;
    private boolean silhouetteStopped;
    private Vec3 silhouetteDirection = Vec3.ZERO;
    private boolean falseAlertHissPlayed;
    private int falseAlertDespawnTicks;

    public UncannyCreeperEntity(EntityType<? extends Creeper> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Creeper?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CREEPER_VARIANT, CreeperVariant.UNASSIGNED.id());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(this::isVanillaSwellGoal);
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor levelAccessor,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(levelAccessor, difficulty, spawnType, spawnGroupData);
        if (levelAccessor instanceof ServerLevel serverLevel) {
            ensureVariantAssigned(serverLevel);
        }
        return data;
    }

    @Override
    public void tick() {
        super.tick();

        CreeperVariant variant = getCreeperVariant();
        if (variant == CreeperVariant.UNASSIGNED && this.level() instanceof ServerLevel serverLevel) {
            ensureVariantAssigned(serverLevel);
            variant = getCreeperVariant();
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.setSilent(variant == CreeperVariant.SILHOUETTE);

        switch (variant) {
            case FALSE_ALERT -> tickFalseAlert(serverLevel);
            case SILHOUETTE -> tickSilhouette(serverLevel);
            case DEFECT, VENTRILOQUIST, ABSORBER -> tickPredatoryFuse(serverLevel, variant);
            case UNASSIGNED -> {
            }
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        super.playStepSound(pos, blockState);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        CreeperVariant variant = getCreeperVariant();
        if (!this.level().isClientSide() && variant == CreeperVariant.FALSE_ALERT) {
            return false;
        }
        if (!this.level().isClientSide()
                && variant == CreeperVariant.SILHOUETTE
                && source.getEntity() instanceof Player
                && this.level() instanceof ServerLevel serverLevel) {
            PrimedTnt tnt = EntityType.TNT.create(serverLevel);
            if (tnt != null) {
                tnt.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                tnt.setFuse(50);
                serverLevel.addFreshEntity(tnt);
            }
            this.discard();
            return true;
        }

        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("UncannyCreeperVariant", getCreeperVariant().id());
        tag.putBoolean("UncannyFuseActive", this.fuseActive);
        tag.putInt("UncannyFuseTicks", this.fuseTicks);
        tag.putInt("UncannyFakeHissCooldown", this.fakeHissCooldown);
        tag.putBoolean("UncannySilhouetteDirectionInitialized", this.silhouetteDirectionInitialized);
        tag.putBoolean("UncannySilhouetteStopped", this.silhouetteStopped);
        tag.putDouble("UncannySilhouetteDirX", this.silhouetteDirection.x);
        tag.putDouble("UncannySilhouetteDirY", this.silhouetteDirection.y);
        tag.putDouble("UncannySilhouetteDirZ", this.silhouetteDirection.z);
        tag.putBoolean("UncannyFalseAlertHissPlayed", this.falseAlertHissPlayed);
        tag.putInt("UncannyFalseAlertDespawnTicks", this.falseAlertDespawnTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setCreeperVariant(CreeperVariant.fromId(tag.getInt("UncannyCreeperVariant")));
        this.fuseActive = tag.getBoolean("UncannyFuseActive");
        this.fuseTicks = Math.max(0, tag.getInt("UncannyFuseTicks"));
        this.fakeHissCooldown = Math.max(1, tag.getInt("UncannyFakeHissCooldown"));
        this.silhouetteDirectionInitialized = tag.getBoolean("UncannySilhouetteDirectionInitialized");
        this.silhouetteStopped = tag.getBoolean("UncannySilhouetteStopped");
        this.silhouetteDirection = new Vec3(
                tag.getDouble("UncannySilhouetteDirX"),
                tag.getDouble("UncannySilhouetteDirY"),
                tag.getDouble("UncannySilhouetteDirZ"));
        this.falseAlertHissPlayed = tag.getBoolean("UncannyFalseAlertHissPlayed");
        this.falseAlertDespawnTicks = Math.max(0, tag.getInt("UncannyFalseAlertDespawnTicks"));
    }

    public CreeperVariant getCreeperVariant() {
        return CreeperVariant.fromId(this.entityData.get(CREEPER_VARIANT));
    }

    private void setCreeperVariant(CreeperVariant variant) {
        this.entityData.set(CREEPER_VARIANT, variant.id());
    }

    private void ensureVariantAssigned(ServerLevel serverLevel) {
        if (getCreeperVariant() != CreeperVariant.UNASSIGNED) {
            return;
        }
        UncannyPhase phase = UncannyWorldState.get(serverLevel.getServer()).getPhase();
        setCreeperVariant(rollVariantForPhase(phase));
    }

    private CreeperVariant rollVariantForPhase(UncannyPhase phase) {
        int roll = this.random.nextInt(100);
        return switch (phase) {
            case PHASE_1 -> CreeperVariant.DEFECT;
            case PHASE_2 -> roll < 70 ? CreeperVariant.FALSE_ALERT : CreeperVariant.DEFECT;
            case PHASE_3 -> {
                if (roll < 62) {
                    yield CreeperVariant.SILHOUETTE;
                }
                if (roll < 90) {
                    yield CreeperVariant.FALSE_ALERT;
                }
                yield CreeperVariant.DEFECT;
            }
            case PHASE_4 -> {
                if (roll < 45) {
                    yield CreeperVariant.VENTRILOQUIST;
                }
                if (roll < 50) {
                    yield CreeperVariant.ABSORBER;
                }
                if (roll < 78) {
                    yield CreeperVariant.SILHOUETTE;
                }
                yield CreeperVariant.FALSE_ALERT;
            }
        };
    }

    private void tickFalseAlert(ServerLevel serverLevel) {
        this.fuseActive = false;
        this.fuseTicks = 0;
        this.setInvisible(false);
        this.setInvulnerable(false);
        this.setNoAi(false);
        this.setNoGravity(false);
        this.noPhysics = false;

        if (this.falseAlertDespawnTicks > 0) {
            this.falseAlertDespawnTicks--;
            if (this.falseAlertDespawnTicks <= 0) {
                this.discard();
            }
            return;
        }

        Player target = resolveClosestThreatPlayer(serverLevel);
        if (target == null || !target.isAlive()) {
            return;
        }

        this.setTarget(target);
        this.getNavigation().moveTo(target, 1.0D);

        if (!this.falseAlertHissPlayed) {
            if (!shouldStartFuse(target)) {
                return;
            }
            startFuse(target, CreeperVariant.FALSE_ALERT, serverLevel);
            this.falseAlertHissPlayed = true;
            this.falseAlertDespawnTicks = FALSE_ALERT_AUDIO_TICKS;
        }
    }

    private void tickSilhouette(ServerLevel serverLevel) {
        this.fuseActive = false;
        this.fuseTicks = 0;
        this.setInvisible(false);
        this.noPhysics = false;
        this.setTarget(null);
        this.getNavigation().stop();

        if (!this.silhouetteDirectionInitialized || this.silhouetteDirection.lengthSqr() < 0.0001D) {
            float yaw = this.random.nextFloat() * (float) (Math.PI * 2.0D);
            this.silhouetteDirection = new Vec3(Mth.cos(yaw), 0.0D, Mth.sin(yaw)).normalize();
            this.silhouetteDirectionInitialized = true;
        }

        if (this.horizontalCollision) {
            this.silhouetteStopped = true;
        }

        if (this.silhouetteStopped) {
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            return;
        }

        Vec3 movement = new Vec3(this.silhouetteDirection.x * 0.24D, this.getDeltaMovement().y, this.silhouetteDirection.z * 0.24D);
        this.setDeltaMovement(movement);
        this.setYRot((float) (Mth.atan2(this.silhouetteDirection.z, this.silhouetteDirection.x) * (180.0D / Math.PI)) - 90.0F);
    }

    private void tickPredatoryFuse(ServerLevel serverLevel, CreeperVariant variant) {
        this.setInvulnerable(false);
        this.setInvisible(false);
        this.noPhysics = false;

        Player target = resolveClosestThreatPlayer(serverLevel);
        if (target != null && target.isAlive()) {
            this.setTarget(target);
            this.getNavigation().moveTo(target, 1.03D);
            if (!this.fuseActive && shouldStartFuse(target)) {
                startFuse(target, variant, serverLevel);
            }
        }

        if (!this.fuseActive) {
            return;
        }

        this.fuseTicks++;
        if (this.fuseTicks < FUSE_DURATION_TICKS) {
            return;
        }

        this.fuseActive = false;
        this.fuseTicks = 0;
        executeFuseResult(serverLevel, variant, target);
    }

    private boolean shouldStartFuse(Player player) {
        return this.distanceToSqr(player) <= 3.2D * 3.2D && this.hasLineOfSight(player);
    }

    private void startFuse(Player target, CreeperVariant variant, ServerLevel serverLevel) {
        this.fuseActive = true;
        this.fuseTicks = 0;

        if (variant == CreeperVariant.ABSORBER) {
            return;
        }

        if (variant == CreeperVariant.VENTRILOQUIST) {
            Vec3 behind = getPointBehindPlayer(target, 1.7D);
            serverLevel.playSound(
                    null,
                    behind.x,
                    target.getEyeY() - 0.35D,
                    behind.z,
                    SoundEvents.CREEPER_PRIMED,
                    this.getSoundSource(),
                    1.0F,
                    1.0F);
            return;
        }

        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CREEPER_PRIMED, this.getSoundSource(), 1.0F, 1.0F);
    }

    private void executeFuseResult(ServerLevel serverLevel, CreeperVariant variant, @Nullable Player target) {
        switch (variant) {
            case DEFECT -> {
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CREEPER_DEATH, this.getSoundSource(), 1.0F, 1.0F);
                this.discard();
            }
            case VENTRILOQUIST -> {
                float radius = this.isPowered() ? 6.0F : 3.0F;
                serverLevel.explode(this, this.getX(), this.getY(), this.getZ(), radius, Level.ExplosionInteraction.MOB);
                this.discard();
            }
            case ABSORBER -> {
                triggerAbsorberEffect(serverLevel, target);
                this.discard();
            }
            default -> this.discard();
        }
    }

    private void triggerAbsorberEffect(ServerLevel serverLevel, @Nullable Player directTarget) {
        AABB range = this.getBoundingBox().inflate(20.0D);
        for (ServerPlayer nearby : serverLevel.getEntitiesOfClass(ServerPlayer.class, range, player -> player.isAlive())) {
            UncannyParanoiaEventSystem.triggerTotalBlackout(nearby);
        }
        if (directTarget instanceof ServerPlayer serverPlayer) {
            UncannyParanoiaEventSystem.triggerTotalBlackout(serverPlayer);
        }
        destroyNearbyLightSources(serverLevel, this.blockPosition(), 15);
    }

    private void destroyNearbyLightSources(ServerLevel serverLevel, BlockPos center, int radius) {
        int radiusSqr = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -6; dy <= 6; dy++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSqr) {
                        continue;
                    }

                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = serverLevel.getBlockState(pos);
                    if (state.isAir() || state.is(Blocks.BEDROCK)) {
                        continue;
                    }

                    FluidState fluid = serverLevel.getFluidState(pos);
                    if (!fluid.isEmpty()) {
                        continue;
                    }

                    if (state.getLightEmission(serverLevel, pos) <= 0) {
                        continue;
                    }
                    if (state.getDestroySpeed(serverLevel, pos) < 0.0F) {
                        continue;
                    }

                    serverLevel.destroyBlock(pos, true, this);
                }
            }
        }
    }

    @Nullable
    private Player resolveClosestThreatPlayer(ServerLevel serverLevel) {
        LivingEntity target = this.getTarget();
        if (target instanceof Player player && player.isAlive()) {
            return player;
        }
        double followRange = this.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 16.0D;
        }
        return serverLevel.getNearestPlayer(this, followRange);
    }

    private Vec3 getPointBehindPlayer(Player player, double distance) {
        Vec3 look = player.getViewVector(1.0F);
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontal = horizontal.normalize();
        }
        return player.position().subtract(horizontal.scale(distance));
    }

    private boolean isVanillaSwellGoal(Goal goal) {
        return goal.getClass().getName().equals("net.minecraft.world.entity.ai.goal.SwellGoal");
    }

    public enum CreeperVariant {
        UNASSIGNED(0),
        DEFECT(1),
        FALSE_ALERT(2),
        SILHOUETTE(3),
        VENTRILOQUIST(4),
        ABSORBER(5);

        private final int id;

        CreeperVariant(int id) {
            this.id = id;
        }

        public int id() {
            return this.id;
        }

        public static CreeperVariant fromId(int id) {
            for (CreeperVariant variant : values()) {
                if (variant.id == id) {
                    return variant;
                }
            }
            return UNASSIGNED;
        }
    }
}
