package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class UncannyStrayEntity extends Stray implements UncannyEntityMarker {
    private static final int WATCH_DURATION_TICKS = 40;
    private int watchTicks;

    public UncannyStrayEntity(EntityType<? extends Stray> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Stray?");
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);

        if (level().isClientSide()) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (!(target instanceof Player player)) {
            watchTicks = 0;
            return;
        }

        double distanceSq = this.distanceToSqr(player);
        if (distanceSq > 64.0D && watchTicks < WATCH_DURATION_TICKS) {
            watchTicks++;
            this.getNavigation().stop();
            this.lookAt(player, 30.0F, 30.0F);
        } else {
            watchTicks = 0;
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
        }
        return hurt;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        UncannyEntityUtil.suppressStepSound(this, pos, blockState);
    }
}

