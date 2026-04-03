package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class UncannyGhastEntity extends Ghast implements UncannyEntityMarker {
    public UncannyGhastEntity(EntityType<? extends Ghast> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Ghast?");
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            return;
        }

        if (!(this.getTarget() instanceof Player player)) {
            Player nearest = level().getNearestPlayer(this, 48.0D);
            if (nearest != null) {
                this.setTarget(nearest);
            }
            return;
        }

        Vec3 toPlayer = player.position().subtract(this.position()).normalize();
        this.setDeltaMovement(this.getDeltaMovement().add(toPlayer.scale(0.04D)));

        if (this.tickCount % 200 == 0) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.GHAST_SCREAM, this.getSoundSource(), 2.0F, 0.9F);
        }
    }
}

