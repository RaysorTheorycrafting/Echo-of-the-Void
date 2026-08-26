package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import net.minecraft.core.BlockPos;
import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class UncannyHoglinEntity extends Hoglin implements UncannyEntityMarker {
    public UncannyHoglinEntity(EntityType<? extends Hoglin> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Hoglin?");
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.setSilent(false);
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        super.playStepSound(pos, blockState);
    }
}
