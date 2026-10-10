package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

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
            Player nearest = UncannyEntityUtil.nearestHuntablePlayer(this, 48.0D);
            if (nearest != null) {
                this.setTarget(nearest);
            }
            return;
        }

        // Vanilla flight and fireball acquisition remain authoritative. The former unbounded
        // per-tick acceleration toward the player was another instance of the unfair homing
        // effect removed from Magma Cube?, Drowned?, Endermite? and Phantom?.
        if (this.tickCount % 200 == 0) {
            if (this.level() instanceof net.minecraft.server.level.ServerLevel screamLevel) com.eotv.echoofthevoid.sound.UncannyPhysicalSoundDelivery.playFromEntity(screamLevel, this, SoundEvents.GHAST_SCREAM, this.getSoundSource(), 1.15F, 0.9F);
        }
    }
}

