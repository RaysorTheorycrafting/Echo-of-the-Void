package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class UncannyPulseEntity extends Monster implements UncannyEntityMarker {
    private static final DustParticleOptions SHADOW_PARTICLE =
            new DustParticleOptions(new Vector3f(0.02F, 0.02F, 0.02F), 1.0F);
    private long nextHeartbeatTick = Long.MIN_VALUE;
    private long nextWitherTick = Long.MIN_VALUE;

    public UncannyPulseEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Presence?");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.38D));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.isAlive() || this.isRemoved() || this.deathTime > 0) {
            this.nextHeartbeatTick = Long.MAX_VALUE;
            return;
        }
        this.setInvisible(true);
        UncannyEntityUtil.forceSilent(this);
        UncannyEntityUtil.enableDoorNavigation(this);
        this.setGlowingTag(false);

        if (this.level().isClientSide()) {
            return;
        }

        if (!(this.level().getNearestPlayer(this, 24.0D) instanceof ServerPlayer target) || !target.isAlive()) {
            return;
        }

        this.setTarget(null);
        this.getNavigation().moveTo(target, 0.68D);
        tickHeartbeat(target);
        applyWitherAura();
        emitGroundShadow();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        UncannyEntityUtil.suppressStepSound(this, pos, state);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    private void tickHeartbeat(ServerPlayer target) {
        if (!this.isAlive() || this.isRemoved() || this.deathTime > 0) {
            this.nextHeartbeatTick = Long.MAX_VALUE;
            return;
        }
        long now = this.level().getGameTime();
        if (now < this.nextHeartbeatTick) {
            return;
        }

        double distance = Math.sqrt(this.distanceToSqr(target));
        double proximity = 1.0D - Mth.clamp(distance / 20.0D, 0.0D, 1.0D);
        float volume = (float) (0.22D + proximity * 0.50D);
        float pitch = (float) (0.72D + proximity * 0.55D);
        target.serverLevel().playSound(
                null,
                this.getX(),
                this.getY() + 1.0D,
                this.getZ(),
                UncannySoundRegistry.UNCANNY_HEARTBEAT.get(),
                SoundSource.HOSTILE,
                volume,
                pitch);

        this.nextHeartbeatTick = now + 200L + this.random.nextInt(81);
    }

    @Override
    public void die(DamageSource source) {
        this.nextHeartbeatTick = Long.MAX_VALUE;
        super.die(source);
    }

    private void applyWitherAura() {
        long now = this.level().getGameTime();
        if (now < this.nextWitherTick) {
            return;
        }

        boolean applied = false;
        for (LivingEntity entity : this.level().getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(2.2D),
                living -> living.isAlive()
                        && living != this
                        && (living instanceof Player || living instanceof Enemy))) {
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0, false, true, true));
            applied = true;
        }

        if (applied) {
            this.nextWitherTick = now + 10L;
        }
    }

    private void emitGroundShadow() {
        if ((this.tickCount & 1) != 0 || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
                SHADOW_PARTICLE,
                this.getX(),
                this.getY() + 0.02D,
                this.getZ(),
                1,
                0.18D,
                0.0D,
                0.18D,
                0.0D);
    }
}
