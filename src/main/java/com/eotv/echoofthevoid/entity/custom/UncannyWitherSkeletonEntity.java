package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public class UncannyWitherSkeletonEntity extends WitherSkeleton implements UncannyEntityMarker {
    private static final EntityDataAccessor<Boolean> ARCHER_VARIANT =
            SynchedEntityData.defineId(UncannyWitherSkeletonEntity.class, EntityDataSerializers.BOOLEAN);

    private int retreatTicks;
    private boolean variantInitialized;

    public UncannyWitherSkeletonEntity(EntityType<? extends WitherSkeleton> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Wither Skeleton?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ARCHER_VARIANT, false);
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor levelAccessor,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(levelAccessor, difficulty, spawnType, spawnGroupData);
        setArcherVariant(this.random.nextBoolean());
        return data;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);

        if (!variantInitialized) {
            setArcherVariant(this.random.nextBoolean());
        }

        if (this.level().isClientSide() || isArcherVariant()) {
            return;
        }

        if (!(this.getTarget() instanceof net.minecraft.world.entity.LivingEntity target)) {
            this.retreatTicks = 0;
            return;
        }

        if (this.retreatTicks > 0) {
            this.retreatTicks--;
            moveAwayFrom(target);
            return;
        }

        this.getNavigation().moveTo(target, 1.55D);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && !isArcherVariant()) {
            this.retreatTicks = 60;
        }
        return hurt;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("ArcherVariant", isArcherVariant());
        tag.putInt("RetreatTicks", retreatTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setArcherVariant(tag.getBoolean("ArcherVariant"));
        retreatTicks = Math.max(0, tag.getInt("RetreatTicks"));
    }

    private void setArcherVariant(boolean archerVariant) {
        this.entityData.set(ARCHER_VARIANT, archerVariant);
        this.variantInitialized = true;

        if (archerVariant) {
            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        } else {
            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        }
        this.reassessWeaponGoal();
    }

    private boolean isArcherVariant() {
        return this.entityData.get(ARCHER_VARIANT);
    }

    private void moveAwayFrom(net.minecraft.world.entity.LivingEntity target) {
        Vec3 away = this.position().subtract(target.position());
        if (away.lengthSqr() < 0.0001D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            away = away.normalize();
        }
        Vec3 destination = this.position().add(away.scale(7.0D));
        this.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.65D);
    }
}
