package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class UncannyEndermiteEntity extends Endermite implements UncannyEntityMarker {
    private static final int STASIS_TICKS = 40;
    private static final int BURST_TICKS = 20;
    private int cycleTicks;

    public UncannyEndermiteEntity(EntityType<? extends Endermite> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Endermite?");
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);

        if (level().isClientSide()) {
            return;
        }

        cycleTicks++;
        int phase = cycleTicks % (STASIS_TICKS + BURST_TICKS);

        if (phase < STASIS_TICKS) {
            this.getNavigation().stop();
            this.setDeltaMovement(new Vec3(0.0D, this.getDeltaMovement().y, 0.0D));
            return;
        }

        LivingEntity target = this.getTarget();
        if (target != null) {
            Vec3 direction = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(direction.scale(0.45D).add(0.0D, this.getDeltaMovement().y, 0.0D));
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        UncannyEntityUtil.suppressStepSound(this, pos, blockState);
    }
}

