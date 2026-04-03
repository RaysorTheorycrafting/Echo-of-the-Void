package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.level.Level;

public class UncannyEvokerEntity extends Evoker implements UncannyEntityMarker {
    private int delayedFangCueTicks = -1;

    public UncannyEvokerEntity(EntityType<? extends Evoker> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Evoker?");
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);

        if (this.level().isClientSide() || this.delayedFangCueTicks < 0) {
            return;
        }

        if (this.delayedFangCueTicks-- == 0) {
            this.level().playSound(
                    null,
                    this.blockPosition(),
                    SoundEvents.EVOKER_PREPARE_ATTACK,
                    this.getSoundSource(),
                    1.65F,
                    0.55F + this.random.nextFloat() * 0.15F);
            this.delayedFangCueTicks = -1;
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(this::isVanillaAttackSpellGoal);
        this.goalSelector.addGoal(5, new UncannyAttackSpellGoal());
    }

    private boolean isVanillaAttackSpellGoal(Goal goal) {
        return goal.getClass().getName().contains("Evoker$EvokerAttackSpellGoal");
    }

    private class UncannyAttackSpellGoal extends SpellcasterIllager.SpellcasterUseSpellGoal {
        @Override
        protected int getCastWarmupTime() {
            int min = Math.max(0, UncannyConfig.EVOKER_CAST_DELAY_MIN_TICKS.get());
            int max = Math.max(min, UncannyConfig.EVOKER_CAST_DELAY_MAX_TICKS.get());
            int warmup = min + UncannyEvokerEntity.this.random.nextInt(max - min + 1);
            UncannyEvokerEntity.this.delayedFangCueTicks = Math.max(2, warmup / 2);
            return warmup;
        }

        @Override
        protected int getCastingTime() {
            return 20;
        }

        @Override
        protected int getCastingInterval() {
            return 80;
        }

        @Override
        protected SoundEvent getSpellPrepareSound() {
            return null;
        }

        @Override
        protected SpellcasterIllager.IllagerSpell getSpell() {
            return SpellcasterIllager.IllagerSpell.FANGS;
        }

        @Override
        protected void performSpellCasting() {
            LivingEntity target = UncannyEvokerEntity.this.getTarget();
            if (target == null) {
                return;
            }

            double dx = target.getX() - UncannyEvokerEntity.this.getX();
            double dz = target.getZ() - UncannyEvokerEntity.this.getZ();
            float yaw = (float) Mth.atan2(dz, dx);

            double minY = Math.min(target.getY(), UncannyEvokerEntity.this.getY());
            if (UncannyEvokerEntity.this.distanceToSqr(target) < 9.0D) {
                for (int i = 0; i < 5; i++) {
                    float ringYaw = yaw + i * ((float) Math.PI * 0.4F);
                    spawnFangsAt(
                            UncannyEvokerEntity.this.getX() + Mth.cos(ringYaw) * 1.5D,
                            UncannyEvokerEntity.this.getZ() + Mth.sin(ringYaw) * 1.5D,
                            minY,
                            ringYaw,
                            0);
                }
            } else {
                for (int i = 0; i < 6; i++) {
                    double distance = 1.5D + i * 1.2D;
                    spawnFangsAt(
                            UncannyEvokerEntity.this.getX() + Mth.cos(yaw) * distance,
                            UncannyEvokerEntity.this.getZ() + Mth.sin(yaw) * distance,
                            minY,
                            yaw,
                            i);
                }
            }
        }

        private void spawnFangsAt(double x, double z, double y, float yaw, int warmup) {
            EvokerFangs fangs = new EvokerFangs(UncannyEvokerEntity.this.level(), x, y, z, yaw, warmup, UncannyEvokerEntity.this);
            UncannyEvokerEntity.this.level().addFreshEntity(fangs);
        }
    }
}
