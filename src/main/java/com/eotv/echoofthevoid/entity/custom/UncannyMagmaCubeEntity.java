package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class UncannyMagmaCubeEntity extends MagmaCube implements UncannyEntityMarker {
    public UncannyMagmaCubeEntity(EntityType<? extends MagmaCube> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Magma Cube?");
        this.setSilent(true);
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor levelAccessor,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(levelAccessor, difficulty, spawnType, spawnGroupData);
        this.setSize(1, true);
        return data;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);

        if (this.level().isClientSide()) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            Player nearestPlayer = this.level().getNearestPlayer(this, 28.0D);
            if (nearestPlayer != null && nearestPlayer.isAlive()) {
                target = nearestPlayer;
                this.setTarget(nearestPlayer);
            }
        }

        Vec3 delta = this.getDeltaMovement();
        if (target != null) {
            Vec3 toTarget = target.position().subtract(this.position());
            Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
            if (horizontal.lengthSqr() > 0.0001D) {
                Vec3 dir = horizontal.normalize();
                double glideSpeed = 0.28D + Math.min(0.10D, this.getSize() * 0.015D);
                double vx = Mth.lerp(0.80D, delta.x, dir.x * glideSpeed);
                double vz = Mth.lerp(0.80D, delta.z, dir.z * glideSpeed);
                double vy = this.onGround() ? 0.0D : Math.max(-0.26D, delta.y - 0.08D);
                this.setDeltaMovement(vx, vy, vz);
                float bodyYaw = (float) (Mth.atan2(vz, vx) * (180.0D / Math.PI)) - 90.0F;
                this.setYRot(bodyYaw);
                this.setYBodyRot(bodyYaw);
                this.setYHeadRot(bodyYaw);
            }
        } else if (this.onGround()) {
            this.setDeltaMovement(delta.x * 0.86D, 0.0D, delta.z * 0.86D);
        } else {
            this.setDeltaMovement(delta.x, Math.max(-0.28D, delta.y - 0.08D), delta.z);
        }
    }

    @Override
    public void jumpFromGround() {
        Vec3 delta = this.getDeltaMovement();
        this.setDeltaMovement(delta.x, 0.0D, delta.z);
        this.hasImpulse = true;
    }

    @Override
    public float maxUpStep() {
        return 2.75F;
    }
}
