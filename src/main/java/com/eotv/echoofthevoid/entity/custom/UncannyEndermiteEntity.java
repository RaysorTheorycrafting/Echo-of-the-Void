package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.entity.variant.ReplacementVariantExpansionSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

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
        this.setSilent(false);

        if (level().isClientSide()) {
            return;
        }

        if (!ReplacementVariantExpansionSystem.usesHistoricalSpecializedBehavior(this)) {
            return;
        }
        cycleTicks++;
        int phase = cycleTicks % (STASIS_TICKS + BURST_TICKS);

        if (phase < STASIS_TICKS) {
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            return;
        }

        LivingEntity target = this.getTarget();
        if (target != null) {
            // Resume ordinary pathfinding after the conspicuous pause. Directly replacing
            // horizontal velocity produced the same unavoidable homing glide as Magma Cube?.
            this.getNavigation().moveTo(target, 1.12D);
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        super.playStepSound(pos, blockState);
    }
}

