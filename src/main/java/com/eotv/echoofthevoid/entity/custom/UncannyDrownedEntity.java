package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.entity.variant.ReplacementVariantExpansionSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class UncannyDrownedEntity extends Drowned implements UncannyEntityMarker {
    private int surgeTelegraphTicks;
    private int surgeTicks;
    private int surgeCooldownTicks;

    public UncannyDrownedEntity(EntityType<? extends Drowned> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Drowned?");
    }

    @Override
    public void aiStep() {
        // LivingEntity.canBreatheUnderwater() is final in 1.21.1 and is driven by the
        // minecraft:can_breathe_under_water entity-type tag. Refill before Vanilla's tick as an
        // additional migration guard for entities loaded before the tag was added.
        this.setAirSupply(this.getMaxAirSupply());
        super.aiStep();
        this.setSilent(false);
        this.setAirSupply(this.getMaxAirSupply());

        if (this.level().isClientSide()
                || !ReplacementVariantExpansionSystem.usesHistoricalSpecializedBehavior(this)) {
            return;
        }
        if (this.surgeCooldownTicks > 0) {
            this.surgeCooldownTicks--;
        }
        LivingEntity target = this.getTarget();
        if (!this.isInWater() || target == null || !target.isAlive()) {
            this.surgeTelegraphTicks = 0;
            this.surgeTicks = 0;
            return;
        }
        if (this.surgeTelegraphTicks > 0) {
            this.surgeTelegraphTicks--;
            var current = this.getDeltaMovement();
            this.setDeltaMovement(current.x * 0.70D, current.y, current.z * 0.70D);
            if (this.tickCount % 4 == 0 && this.level() instanceof net.minecraft.server.level.ServerLevel level) {
                level.sendParticles(ParticleTypes.BUBBLE,
                        this.getX(), this.getEyeY(), this.getZ(), 4, 0.28D, 0.25D, 0.28D, 0.02D);
            }
            if (this.surgeTelegraphTicks == 0) {
                this.surgeTicks = 24;
            }
            return;
        }
        if (this.surgeTicks > 0) {
            this.surgeTicks--;
            // The former surge wrote a target-derived velocity every tick. On fast or large
            // Drowned-derived mobs that became an unavoidable homing slide. Keep the bubbles as
            // a readable cue, then let Vanilla navigation perform a short, bounded pursuit.
            this.getNavigation().moveTo(target, 1.08D);
            if (this.surgeTicks == 0) {
                this.surgeCooldownTicks = 120;
            }
            return;
        }
        double distance = this.distanceTo(target);
        if (this.surgeCooldownTicks <= 0 && distance >= 4.0D && distance <= 12.0D) {
            this.surgeTelegraphTicks = 18;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("UncannyDrownedSurgeTelegraph", this.surgeTelegraphTicks);
        tag.putInt("UncannyDrownedSurgeTicks", this.surgeTicks);
        tag.putInt("UncannyDrownedSurgeCooldown", this.surgeCooldownTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.surgeTelegraphTicks = Math.max(0, tag.getInt("UncannyDrownedSurgeTelegraph"));
        this.surgeTicks = Math.max(0, tag.getInt("UncannyDrownedSurgeTicks"));
        this.surgeCooldownTicks = Math.max(0, tag.getInt("UncannyDrownedSurgeCooldown"));
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        super.playStepSound(pos, blockState);
    }
}

