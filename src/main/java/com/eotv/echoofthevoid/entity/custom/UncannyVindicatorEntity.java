package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class UncannyVindicatorEntity extends Vindicator implements UncannyEntityMarker {
    private int glitchTicks;
    private int nextGlitchTick;

    public UncannyVindicatorEntity(EntityType<? extends Vindicator> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Vindicator?");
        this.nextGlitchTick = 90 + this.random.nextInt(141);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);

        if (this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        }

        if (this.glitchTicks > 0) {
            this.getNavigation().stop();
            this.setTarget(null);
            if ((this.glitchTicks & 1) == 0 || this.random.nextFloat() < 0.40F) {
                float snappedYaw = this.getYRot() + (this.random.nextFloat() - 0.5F) * 240.0F;
                float snappedPitch = Mth.clamp((this.random.nextFloat() - 0.5F) * 120.0F, -80.0F, 80.0F);
                this.setYRot(snappedYaw);
                this.yRotO = snappedYaw;
                this.setYHeadRot(snappedYaw);
                this.setYBodyRot(snappedYaw);
                this.setXRot(snappedPitch);
            }
            Vec3 jitter = new Vec3(
                    (this.random.nextDouble() - 0.5D) * 0.35D,
                    this.getDeltaMovement().y,
                    (this.random.nextDouble() - 0.5D) * 0.35D);
            this.setDeltaMovement(jitter);
            if (this.random.nextFloat() < 0.16F) {
                double tx = this.getX() + (this.random.nextDouble() - 0.5D) * 1.8D;
                double tz = this.getZ() + (this.random.nextDouble() - 0.5D) * 1.8D;
                this.teleportTo(tx, this.getY(), tz);
            }
            this.glitchTicks--;
            return;
        }

        if (!this.level().isClientSide() && this.tickCount >= this.nextGlitchTick) {
            this.glitchTicks = 24 + this.random.nextInt(17);
            this.nextGlitchTick = this.tickCount + 90 + this.random.nextInt(141);
        }
    }
}
