package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class UncannyBlazeEntity extends Blaze implements UncannyEntityMarker {
    private int nextVerticalImpulseTick;

    public UncannyBlazeEntity(EntityType<? extends Blaze> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Blaze?");
        this.nextVerticalImpulseTick = 40 + this.random.nextInt(81);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);

        if (this.level().isClientSide()) {
            return;
        }

        if (this.tickCount < this.nextVerticalImpulseTick) {
            return;
        }

        Vec3 delta = this.getDeltaMovement();
        double impulse = (this.random.nextBoolean() ? 1.0D : -1.0D) * (0.55D + this.random.nextDouble() * 0.65D);
        double y = Mth.clamp(delta.y + impulse, -1.6D, 1.6D);
        this.setDeltaMovement(delta.x * 0.35D, y, delta.z * 0.35D);
        this.nextVerticalImpulseTick = this.tickCount + 40 + this.random.nextInt(81);
    }
}
