package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class UncannyRavagerEntity extends Ravager implements UncannyEntityMarker {
    private int jitterTicks;
    private int nextJitterTick;

    public UncannyRavagerEntity(EntityType<? extends Ravager> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Ravager?");
        this.nextJitterTick = 10 + this.random.nextInt(21);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);

        if (!this.level().isClientSide() && this.tickCount >= this.nextJitterTick && this.jitterTicks <= 0) {
            this.jitterTicks = 24 + this.random.nextInt(27);
            this.nextJitterTick = this.tickCount + 10 + this.random.nextInt(26);
        }

        if (this.jitterTicks > 0) {
            float headYawOffset = (this.random.nextFloat() - 0.5F) * 520.0F;
            float bodyYawOffset = (this.random.nextFloat() - 0.5F) * 280.0F;
            float pitchOffset = (this.random.nextFloat() - 0.5F) * 170.0F;
            float snappedBodyYaw = this.getYRot() + bodyYawOffset;

            this.setYRot(snappedBodyYaw);
            this.yRotO = snappedBodyYaw;
            this.setYBodyRot(snappedBodyYaw);
            this.setYHeadRot(snappedBodyYaw + headYawOffset);
            this.setXRot(Mth.clamp(pitchOffset, -88.0F, 88.0F));

            Vec3 delta = this.getDeltaMovement().add(
                    (this.random.nextDouble() - 0.5D) * 0.28D,
                    (this.random.nextDouble() - 0.5D) * 0.12D,
                    (this.random.nextDouble() - 0.5D) * 0.28D);
            this.setDeltaMovement(delta);

            if ((this.jitterTicks % 5) == 0) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.18D, 0.0D));
            }
            this.jitterTicks--;
        }
    }
}
