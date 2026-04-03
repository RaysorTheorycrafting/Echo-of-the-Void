package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.event.UncannyParanoiaEventSystem;
import com.eotv.echoofthevoid.phase.UncannyPhase;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.UUID;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class UncannySkeletonEntity extends Skeleton implements UncannyEntityMarker {
    private static final EntityDataAccessor<Integer> SKELETON_VARIANT =
            SynchedEntityData.defineId(UncannySkeletonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STATUE_AWAKENED =
            SynchedEntityData.defineId(UncannySkeletonEntity.class, EntityDataSerializers.BOOLEAN);

    private boolean frozenByGaze;
    private int nextLoadoutCheckTick;
    private int senseThiefShotCooldown;
    private boolean statueNoAiActive;

    public UncannySkeletonEntity(EntityType<? extends Skeleton> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Skeleton?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKELETON_VARIANT, SkeletonVariant.UNASSIGNED.id());
        builder.define(STATUE_AWAKENED, false);
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor levelAccessor,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(levelAccessor, difficulty, spawnType, spawnGroupData);
        ensureCombatLoadout();

        if (levelAccessor instanceof ServerLevel serverLevel) {
            ensureVariantAssigned(serverLevel);
        }
        return data;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!this.level().isClientSide()) {
            ensureCombatLoadout();
            this.nextLoadoutCheckTick = this.tickCount + 20;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        SkeletonVariant variant = getSkeletonVariant();
        if (variant == SkeletonVariant.UNASSIGNED && this.level() instanceof ServerLevel serverLevel) {
            ensureVariantAssigned(serverLevel);
            variant = getSkeletonVariant();
        }

        if (variant == SkeletonVariant.INVERTED) {
            applyInvertedPose();
        }

        if (!this.level().isClientSide() && this.tickCount >= this.nextLoadoutCheckTick) {
            ensureCombatLoadout();
            this.nextLoadoutCheckTick = this.tickCount + 20 + this.random.nextInt(20);
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            if (variant == SkeletonVariant.STATUE_MACABRE) {
                tickStatueState(serverLevel);
            } else {
                this.frozenByGaze = false;
                if (isStatueAwakened()) {
                    setStatueAwakened(false);
                }
                if (this.statueNoAiActive) {
                    this.setNoAi(false);
                    this.statueNoAiActive = false;
                }
            }
        }

        if (this.senseThiefShotCooldown > 0) {
            this.senseThiefShotCooldown--;
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        SkeletonVariant variant = getSkeletonVariant();
        if (variant == SkeletonVariant.UNASSIGNED && this.level() instanceof ServerLevel serverLevel) {
            ensureVariantAssigned(serverLevel);
            variant = getSkeletonVariant();
        }

        if (variant == SkeletonVariant.STATUE_MACABRE && this.frozenByGaze && !isStatueAwakened()) {
            return;
        }

        if (variant == SkeletonVariant.SENSE_THIEF && this.senseThiefShotCooldown > 0) {
            return;
        }

        ItemStack weapon = this.getMainHandItem();
        if (weapon.isEmpty() || !(weapon.getItem() instanceof BowItem)) {
            weapon = new ItemStack(Items.BOW);
        }

        switch (variant) {
            case BLIND_ARTILLERY -> fireBlindArtilleryArrow(target, weapon);
            case SILENT_SNIPER -> fireDirectArrow(target, weapon, ProjectileMode.SILENT_SNIPER);
            case SENSE_THIEF -> {
                fireDirectArrow(target, weapon, ProjectileMode.SENSE_THIEF);
                this.senseThiefShotCooldown = 70;
            }
            default -> fireDirectArrow(target, weapon, ProjectileMode.NORMAL);
        }

        if (variant != SkeletonVariant.SILENT_SNIPER) {
            this.level().playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    SoundEvents.SKELETON_SHOOT,
                    this.getSoundSource(),
                    1.0F,
                    1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (getSkeletonVariant() == SkeletonVariant.STATUE_MACABRE && this.frozenByGaze && !isStatueAwakened()) {
            return false;
        }
        return super.doHurtTarget(entity);
    }

    @Override
    protected boolean isImmobile() {
        if (getSkeletonVariant() == SkeletonVariant.STATUE_MACABRE && this.frozenByGaze && !isStatueAwakened()) {
            return true;
        }
        return super.isImmobile();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (!result) {
            return false;
        }

        if (getSkeletonVariant() == SkeletonVariant.STATUE_MACABRE
                && this.frozenByGaze
                && !isStatueAwakened()
                && source.getEntity() instanceof Player) {
            setStatueAwakened(true);
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 20, 1, false, false, true));
        }
        return true;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        super.playStepSound(pos, blockState);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("UncannySkeletonVariant", getSkeletonVariant().id());
        tag.putBoolean("UncannyStatueAwakened", isStatueAwakened());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSkeletonVariant(SkeletonVariant.fromId(tag.getInt("UncannySkeletonVariant")));
        setStatueAwakened(tag.getBoolean("UncannyStatueAwakened"));
    }

    public SkeletonVariant getSkeletonVariant() {
        return SkeletonVariant.fromId(this.entityData.get(SKELETON_VARIANT));
    }

    private boolean isStatueAwakened() {
        return this.entityData.get(STATUE_AWAKENED);
    }

    private void setSkeletonVariant(SkeletonVariant variant) {
        this.entityData.set(SKELETON_VARIANT, variant.id());
    }

    private void setStatueAwakened(boolean awakened) {
        this.entityData.set(STATUE_AWAKENED, awakened);
    }

    private void ensureVariantAssigned(ServerLevel serverLevel) {
        if (getSkeletonVariant() != SkeletonVariant.UNASSIGNED) {
            return;
        }
        UncannyPhase phase = UncannyWorldState.get(serverLevel.getServer()).getPhase();
        setSkeletonVariant(rollVariantForPhase(phase));
    }

    private void ensureCombatLoadout() {
        ItemStack mainhand = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!(mainhand.getItem() instanceof BowItem)) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            this.reassessWeaponGoal();
            return;
        }

        // Re-check periodically in case an external interaction changed the held item.
        if ((this.tickCount % 200) == 0) {
            this.reassessWeaponGoal();
        }
    }

    private SkeletonVariant rollVariantForPhase(UncannyPhase phase) {
        int roll = this.random.nextInt(100);
        return switch (phase) {
            case PHASE_1 -> SkeletonVariant.INVERTED;
            case PHASE_2 -> roll < 68 ? SkeletonVariant.SILENT_SNIPER : SkeletonVariant.INVERTED;
            case PHASE_3 -> {
                if (roll < 58) {
                    yield SkeletonVariant.BLIND_ARTILLERY;
                }
                if (roll < 86) {
                    yield SkeletonVariant.SILENT_SNIPER;
                }
                yield SkeletonVariant.INVERTED;
            }
            case PHASE_4 -> {
                if (roll < 38) {
                    yield SkeletonVariant.STATUE_MACABRE;
                }
                if (roll < 68) {
                    yield SkeletonVariant.BLIND_ARTILLERY;
                }
                if (roll < 90) {
                    yield SkeletonVariant.SENSE_THIEF;
                }
                yield SkeletonVariant.SILENT_SNIPER;
            }
        };
    }

    private void applyInvertedPose() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        float toTargetYaw = (float) (Mth.atan2(target.getZ() - this.getZ(), target.getX() - this.getX()) * (180.0D / Math.PI)) - 90.0F;
        float bodyYaw = toTargetYaw + 180.0F;
        this.setYRot(bodyYaw);
        this.yRotO = bodyYaw;
        this.setYBodyRot(bodyYaw);
        this.yBodyRotO = bodyYaw;
        this.setYHeadRot(toTargetYaw);
        this.yHeadRotO = toTargetYaw;
    }

    private void tickStatueState(ServerLevel serverLevel) {
        if (isStatueAwakened()) {
            this.frozenByGaze = false;
            if (this.statueNoAiActive) {
                this.setNoAi(false);
                this.statueNoAiActive = false;
            }
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
            return;
        }

        ServerPlayer watcher = resolveWatcher(serverLevel);
        this.frozenByGaze = watcher != null && isDirectlyWatchedBy(watcher);
        if (!this.frozenByGaze) {
            if (this.statueNoAiActive) {
                this.setNoAi(false);
                this.statueNoAiActive = false;
            }
            return;
        }

        this.getNavigation().stop();
        this.setTarget(null);
        this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
        if (!this.statueNoAiActive) {
            this.setNoAi(true);
            this.statueNoAiActive = true;
        }
    }

    @Nullable
    private ServerPlayer resolveWatcher(ServerLevel serverLevel) {
        LivingEntity target = this.getTarget();
        if (target instanceof ServerPlayer serverPlayer && serverPlayer.isAlive()) {
            return serverPlayer;
        }
        Player nearest = serverLevel.getNearestPlayer(this, 28.0D);
        return nearest instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    private boolean isDirectlyWatchedBy(ServerPlayer player) {
        if (!this.hasLineOfSight(player) || !player.hasLineOfSight(this)) {
            return false;
        }
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 toSkeleton = this.position().subtract(player.getEyePosition()).normalize();
        return look.dot(toSkeleton) > 0.90D;
    }

    private void fireDirectArrow(LivingEntity target, ItemStack weapon, ProjectileMode mode) {
        UncannySkeletonArrow arrow = new UncannySkeletonArrow(this.level(), this, mode, null, weapon);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(
                dx,
                dy + horizontal * 0.2D,
                dz,
                1.6F,
                (float) (14 - this.level().getDifficulty().getId() * 4));
        this.level().addFreshEntity(arrow);
    }

    private void fireBlindArtilleryArrow(LivingEntity target, ItemStack weapon) {
        UUID targetUuid = target instanceof ServerPlayer serverPlayer ? serverPlayer.getUUID() : null;
        UncannySkeletonArrow arrow = new UncannySkeletonArrow(this.level(), this, ProjectileMode.BLIND_ARTILLERY, targetUuid, weapon);
        arrow.setNoGravity(true);
        arrow.shoot(0.0D, 1.0D, 0.0D, 1.55F, 0.0F);
        this.level().addFreshEntity(arrow);
    }

    public enum SkeletonVariant {
        UNASSIGNED(0),
        INVERTED(1),
        SILENT_SNIPER(2),
        BLIND_ARTILLERY(3),
        STATUE_MACABRE(4),
        SENSE_THIEF(5);

        private final int id;

        SkeletonVariant(int id) {
            this.id = id;
        }

        public int id() {
            return this.id;
        }

        public static SkeletonVariant fromId(int id) {
            for (SkeletonVariant variant : values()) {
                if (variant.id == id) {
                    return variant;
                }
            }
            return UNASSIGNED;
        }
    }

    private enum ProjectileMode {
        NORMAL,
        SILENT_SNIPER,
        BLIND_ARTILLERY,
        SENSE_THIEF
    }

    private static final class UncannySkeletonArrow extends Arrow {
        private final ProjectileMode mode;
        @Nullable
        private final UUID artilleryTargetUuid;
        private boolean artilleryTeleported;

        private UncannySkeletonArrow(
                Level level,
                LivingEntity shooter,
                ProjectileMode mode,
                @Nullable UUID artilleryTargetUuid,
                ItemStack firedFromWeapon) {
            super(level, shooter, Items.ARROW.getDefaultInstance(), firedFromWeapon);
            this.mode = mode;
            this.artilleryTargetUuid = artilleryTargetUuid;
            if (mode == ProjectileMode.SILENT_SNIPER) {
                this.setSilent(true);
            }
        }

        @Override
        public void tick() {
            super.tick();
            if (this.level().isClientSide() || this.mode != ProjectileMode.BLIND_ARTILLERY || this.artilleryTeleported || this.tickCount < 20) {
                return;
            }

            this.artilleryTeleported = true;
            ServerPlayer target = resolveArtilleryTarget();
            if (target != null && target.isAlive()) {
                this.setPos(target.getX(), target.getY() + 15.0D, target.getZ());
            }
            this.setNoGravity(false);
            this.setDeltaMovement(0.0D, -1.2D, 0.0D);
        }

        @Override
        protected void onHitBlock(BlockHitResult result) {
            if (!this.level().isClientSide() && this.mode == ProjectileMode.SENSE_THIEF) {
                BlockPos pos = result.getBlockPos();
                BlockState state = this.level().getBlockState(pos);
                if (isTorchLike(state.getBlock())) {
                    this.level().destroyBlock(pos, true, this);
                }
            }
            super.onHitBlock(result);
        }

        @Override
        protected void onHitEntity(EntityHitResult result) {
            if (this.mode == ProjectileMode.SENSE_THIEF && !this.level().isClientSide() && result.getEntity() instanceof ServerPlayer player) {
                DamageSource source = this.damageSources().arrow(this, this.getOwner() instanceof LivingEntity living ? living : this);
                player.hurt(source, 1.0F);
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false, true));
                UncannyParanoiaEventSystem.applyTemporaryDeafness(player, 30);
                this.discard();
                return;
            }
            super.onHitEntity(result);
        }

        @Nullable
        private ServerPlayer resolveArtilleryTarget() {
            if (!(this.level() instanceof ServerLevel serverLevel)) {
                return null;
            }
            if (this.artilleryTargetUuid != null) {
                return serverLevel.getServer().getPlayerList().getPlayer(this.artilleryTargetUuid);
            }
            Entity owner = this.getOwner();
            if (owner instanceof LivingEntity livingOwner) {
                Player nearest = serverLevel.getNearestPlayer(livingOwner, 48.0D);
                if (nearest instanceof ServerPlayer serverPlayer) {
                    return serverPlayer;
                }
            }
            return null;
        }

        private static boolean isTorchLike(Block block) {
            return block == Blocks.TORCH
                    || block == Blocks.WALL_TORCH
                    || block == Blocks.SOUL_TORCH
                    || block == Blocks.SOUL_WALL_TORCH
                    || block == Blocks.REDSTONE_TORCH
                    || block == Blocks.REDSTONE_WALL_TORCH;
        }
    }
}
