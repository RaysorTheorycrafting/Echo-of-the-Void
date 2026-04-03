package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class UncannyStructureVillagerEntity extends Villager implements UncannyEntityMarker {
    private static final Component VILLAGER_DISPLAY_NAME = Component.literal("Villager?");
    private static final EntityDataAccessor<Integer> SOUND_PROFILE =
            SynchedEntityData.defineId(UncannyStructureVillagerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BEHAVIOR_MODE =
            SynchedEntityData.defineId(UncannyStructureVillagerEntity.class, EntityDataSerializers.INT);
    private static final String TAG_PROFILE = "UncannyStructureVillagerProfile";
    private static final String TAG_MODE = "UncannyStructureVillagerMode";

    private int meleeCooldownTicks = 0;
    private long nextFollowRepathTick = 0L;

    public UncannyStructureVillagerEntity(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Villager?");
    }

    @Override
    public Component getName() {
        return VILLAGER_DISPLAY_NAME;
    }

    @Override
    public Component getDisplayName() {
        return VILLAGER_DISPLAY_NAME;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SOUND_PROFILE, SoundProfile.FLAT.id);
        builder.define(BEHAVIOR_MODE, BehaviorMode.NORMAL.id);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_PROFILE, getSoundProfile().id);
        tag.putInt(TAG_MODE, getBehaviorMode().id);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_PROFILE)) {
            setSoundProfile(SoundProfile.byId(tag.getInt(TAG_PROFILE)));
        }
        if (tag.contains(TAG_MODE)) {
            setBehaviorMode(BehaviorMode.byId(tag.getInt(TAG_MODE)));
        }
    }

    public void setSoundProfile(SoundProfile profile) {
        this.entityData.set(SOUND_PROFILE, profile.id);
    }

    public SoundProfile getSoundProfile() {
        return SoundProfile.byId(this.entityData.get(SOUND_PROFILE));
    }

    public void setBehaviorMode(BehaviorMode mode) {
        this.entityData.set(BEHAVIOR_MODE, mode.id);
    }

    public BehaviorMode getBehaviorMode() {
        return BehaviorMode.byId(this.entityData.get(BEHAVIOR_MODE));
    }

    public void assignRandomBehaviorMode() {
        BehaviorMode[] values = BehaviorMode.values();
        setBehaviorMode(values[this.random.nextInt(values.length)]);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        applyProfileBoundingBox();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.meleeCooldownTicks > 0) {
            this.meleeCooldownTicks--;
        }

        BehaviorMode mode = getBehaviorMode();
        boolean noAi = mode == BehaviorMode.NO_AI;
        if (this.isNoAi() != noAi) {
            this.setNoAi(noAi);
        }
        if (noAi) {
            this.getNavigation().stop();
            this.setTarget(null);
            return;
        }

        if (mode == BehaviorMode.FOLLOW) {
            tickFollowMode();
            return;
        }

        if (mode == BehaviorMode.AGGRESSIVE) {
            if (!(this.getTarget() instanceof LivingEntity target) || !isValidAttackTarget(target)) {
                this.setTarget(findAggressiveTarget());
            }
            tickMeleeMode();
            return;
        }

        if (mode == BehaviorMode.NEUTRAL) {
            if (this.getTarget() != null && !isValidAttackTarget(this.getTarget())) {
                this.setTarget(null);
            }
            tickMeleeMode();
        }
    }

    private void applyProfileBoundingBox() {
        ProfileStretch stretch = profileStretch(getSoundProfile());
        double baseWidth = this.getType().getDimensions().width();
        double baseHeight = this.getType().getDimensions().height();
        double widthScale = Math.max(stretch.xScale, stretch.zScale);
        double expectedWidth = baseWidth * widthScale;
        double expectedHeight = baseHeight * stretch.yScale;
        AABB current = this.getBoundingBox();
        if (Math.abs(current.getXsize() - expectedWidth) < 0.02D
                && Math.abs(current.getYsize() - expectedHeight) < 0.02D) {
            return;
        }

        double half = expectedWidth * 0.5D;
        this.setBoundingBox(new AABB(
                this.getX() - half,
                this.getY(),
                this.getZ() - half,
                this.getX() + half,
                this.getY() + expectedHeight,
                this.getZ() + half));
    }

    public float visualScaleX() {
        return profileStretch(getSoundProfile()).xScale;
    }

    public float visualScaleY() {
        return profileStretch(getSoundProfile()).yScale;
    }

    public float visualScaleZ() {
        return profileStretch(getSoundProfile()).zScale;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (!result || this.level().isClientSide()) {
            return result;
        }
        if (source.getEntity() instanceof LivingEntity attacker
                && (getBehaviorMode() == BehaviorMode.NEUTRAL || getBehaviorMode() == BehaviorMode.AGGRESSIVE)) {
            this.setTarget(attacker);
        }
        return result;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult result = super.mobInteract(player, hand);
        if (!this.level().isClientSide() && result.consumesAction()) {
            this.level().playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    tradeSoundForProfile(getSoundProfile()),
                    SoundSource.NEUTRAL,
                    1.0F,
                    0.95F + this.random.nextFloat() * 0.12F);
        }
        return result;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ambientSoundForProfile(getSoundProfile());
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return hurtSoundForProfile(getSoundProfile());
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return deathSoundForProfile(getSoundProfile());
    }

    private void tickFollowMode() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.getGameTime() < this.nextFollowRepathTick) {
            return;
        }
        this.nextFollowRepathTick = serverLevel.getGameTime() + 10L;
        Player nearest = serverLevel.getNearestPlayer(this, 28.0D);
        if (nearest == null) {
            this.getNavigation().stop();
            return;
        }
        this.getNavigation().moveTo(nearest, 1.05D);
        this.lookAt(nearest, 30.0F, 20.0F);
    }

    private void tickMeleeMode() {
        LivingEntity target = this.getTarget();
        if (!isValidAttackTarget(target)) {
            return;
        }
        this.getNavigation().moveTo(target, 1.14D);
        this.lookAt(target, 30.0F, 20.0F);
        double reach = this.getBbWidth() * 2.2D + target.getBbWidth() * 0.9D;
        double distSq = this.distanceToSqr(target);
        if (distSq <= reach * reach && this.meleeCooldownTicks <= 0) {
            double attackDamage = this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            if (attackDamage <= 0.0D) {
                attackDamage = 3.0D;
            }
            target.hurt(this.damageSources().mobAttack(this), (float) attackDamage);
            this.swing(InteractionHand.MAIN_HAND);
            this.meleeCooldownTicks = 18;
        }
    }

    private @Nullable LivingEntity findAggressiveTarget() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        AABB search = this.getBoundingBox().inflate(16.0D, 6.0D, 16.0D);
        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, search, this::isValidAttackTarget);
        LivingEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (LivingEntity candidate : nearby) {
            double distSq = this.distanceToSqr(candidate);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private boolean isValidAttackTarget(@Nullable LivingEntity candidate) {
        if (candidate == null || candidate == this || !candidate.isAlive()) {
            return false;
        }
        if (candidate instanceof Player player && (player.isSpectator() || player.isCreative())) {
            return false;
        }
        if (!this.hasLineOfSight(candidate)) {
            return false;
        }
        Vec3 delta = candidate.position().subtract(this.position());
        return delta.lengthSqr() <= (20.0D * 20.0D);
    }

    private static SoundEvent ambientSoundForProfile(SoundProfile profile) {
        return switch (profile) {
            case FLAT -> UncannySoundRegistry.UNCANNY_VILLAGER_FLAT_AMBIENT.get();
            case HUGE_LONG_WIDE -> UncannySoundRegistry.UNCANNY_VILLAGER_HUGE_LONG_WIDE_AMBIENT.get();
            case HUGE_THIN -> UncannySoundRegistry.UNCANNY_VILLAGER_HUGE_THIN_AMBIENT.get();
            case VERY_WIDE -> UncannySoundRegistry.UNCANNY_VILLAGER_VERY_WIDE_AMBIENT.get();
            case VERY_LONG -> UncannySoundRegistry.UNCANNY_VILLAGER_VERY_LONG_AMBIENT.get();
        };
    }

    private static SoundEvent hurtSoundForProfile(SoundProfile profile) {
        return switch (profile) {
            case FLAT -> UncannySoundRegistry.UNCANNY_VILLAGER_FLAT_HURT.get();
            case HUGE_LONG_WIDE -> UncannySoundRegistry.UNCANNY_VILLAGER_HUGE_LONG_WIDE_HURT.get();
            case HUGE_THIN -> UncannySoundRegistry.UNCANNY_VILLAGER_HUGE_THIN_HURT.get();
            case VERY_WIDE -> UncannySoundRegistry.UNCANNY_VILLAGER_VERY_WIDE_HURT.get();
            case VERY_LONG -> UncannySoundRegistry.UNCANNY_VILLAGER_VERY_LONG_HURT.get();
        };
    }

    private static SoundEvent deathSoundForProfile(SoundProfile profile) {
        return switch (profile) {
            case FLAT -> UncannySoundRegistry.UNCANNY_VILLAGER_FLAT_DEATH.get();
            case HUGE_LONG_WIDE -> UncannySoundRegistry.UNCANNY_VILLAGER_HUGE_LONG_WIDE_DEATH.get();
            case HUGE_THIN -> UncannySoundRegistry.UNCANNY_VILLAGER_HUGE_THIN_DEATH.get();
            case VERY_WIDE -> UncannySoundRegistry.UNCANNY_VILLAGER_VERY_WIDE_DEATH.get();
            case VERY_LONG -> UncannySoundRegistry.UNCANNY_VILLAGER_VERY_LONG_DEATH.get();
        };
    }

    private static SoundEvent tradeSoundForProfile(SoundProfile profile) {
        return switch (profile) {
            case FLAT -> UncannySoundRegistry.UNCANNY_VILLAGER_FLAT_TRADE.get();
            case HUGE_LONG_WIDE -> UncannySoundRegistry.UNCANNY_VILLAGER_HUGE_LONG_WIDE_TRADE.get();
            case HUGE_THIN -> UncannySoundRegistry.UNCANNY_VILLAGER_HUGE_THIN_TRADE.get();
            case VERY_WIDE -> UncannySoundRegistry.UNCANNY_VILLAGER_VERY_WIDE_TRADE.get();
            case VERY_LONG -> UncannySoundRegistry.UNCANNY_VILLAGER_VERY_LONG_TRADE.get();
        };
    }

    private static ProfileStretch profileStretch(SoundProfile profile) {
        return switch (profile) {
            case FLAT -> new ProfileStretch(1.75F, 0.42F, 1.75F);
            case HUGE_LONG_WIDE -> new ProfileStretch(2.45F, 3.05F, 2.45F);
            case HUGE_THIN -> new ProfileStretch(0.58F, 3.15F, 0.58F);
            case VERY_WIDE -> new ProfileStretch(2.60F, 1.02F, 2.60F);
            case VERY_LONG -> new ProfileStretch(2.95F, 1.18F, 0.72F);
        };
    }

    public enum SoundProfile {
        FLAT(0),
        HUGE_LONG_WIDE(1),
        HUGE_THIN(2),
        VERY_WIDE(3),
        VERY_LONG(4);

        private final int id;

        SoundProfile(int id) {
            this.id = id;
        }

        public static SoundProfile byId(int id) {
            for (SoundProfile profile : values()) {
                if (profile.id == id) {
                    return profile;
                }
            }
            return FLAT;
        }
    }

    public enum BehaviorMode {
        NORMAL(0),
        AGGRESSIVE(1),
        NEUTRAL(2),
        NO_AI(3),
        FOLLOW(4);

        private final int id;

        BehaviorMode(int id) {
            this.id = id;
        }

        public static BehaviorMode byId(int id) {
            for (BehaviorMode mode : values()) {
                if (mode.id == id) {
                    return mode;
                }
            }
            return NORMAL;
        }
    }

    private record ProfileStretch(float xScale, float yScale, float zScale) {
    }
}
