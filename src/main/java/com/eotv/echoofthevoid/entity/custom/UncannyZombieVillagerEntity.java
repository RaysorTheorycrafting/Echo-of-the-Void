package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class UncannyZombieVillagerEntity extends ZombieVillager implements UncannyEntityMarker {
    private static final SoundEvent[] VILLAGER_SOUNDS = new SoundEvent[] {
            SoundEvents.VILLAGER_AMBIENT,
            SoundEvents.VILLAGER_TRADE,
            SoundEvents.VILLAGER_YES,
            SoundEvents.VILLAGER_NO,
            SoundEvents.VILLAGER_CELEBRATE
    };

    public UncannyZombieVillagerEntity(EntityType<? extends ZombieVillager> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Zombie Villager?");
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && this.tickCount % 120 == 0 && random.nextFloat() < 0.2F) {
            this.playSound(randomVillagerSound(), 0.9F, 0.85F + random.nextFloat() * 0.3F);
        }
    }

    @Override
    public SoundEvent getAmbientSound() {
        return randomVillagerSound();
    }

    @Override
    public SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public boolean isAggressive() {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        UncannyEntityUtil.suppressStepSound(this, pos, blockState);
    }

    private SoundEvent randomVillagerSound() {
        return VILLAGER_SOUNDS[random.nextInt(VILLAGER_SOUNDS.length)];
    }
}

