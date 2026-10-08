package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.entity.variant.ReplacementVariantExpansionSystem;
import com.eotv.echoofthevoid.phase.UncannyPhase;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

public class UncannyPhantomEntity extends Phantom implements UncannyEntityMarker {
    private boolean lanternEaterMode;
    private boolean modeInitialized;
    private int nextLanternScanTick;
    private int nextLanternEatTick;
    private BlockPos lanternTarget;

    public UncannyPhantomEntity(EntityType<? extends Phantom> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Phantom?");
    }

    public void setLanternEaterMode(boolean lanternEaterMode) {
        this.lanternEaterMode = lanternEaterMode;
        this.modeInitialized = true;
        this.nextLanternScanTick = this.tickCount;
        this.lanternTarget = null;
    }

    public boolean isLanternEaterMode() {
        return this.lanternEaterMode;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.setSilent(false);

        if (level().isClientSide()) {
            return;
        }

        if (!this.modeInitialized) {
            this.modeInitialized = true;
            if (this.level().getServer() != null) {
                UncannyPhase phase = UncannyWorldState.get(this.level().getServer()).getPhase();
                if (phase.index() >= UncannyPhase.PHASE_2.index() && this.random.nextFloat() < 0.36F) {
                    this.lanternEaterMode = true;
                }
            }
        }

        Player player = this.getTarget() instanceof Player targetPlayer ? targetPlayer : null;
        if (player == null) {
            Player nearest = level().getNearestPlayer(this, 24.0D);
            if (nearest != null) {
                this.setTarget(nearest);
                player = nearest;
            } else {
                return;
            }
        }

        if (this.lanternEaterMode) {
            this.setNoGravity(false);
            tickLanternEaterBehavior();
            return;
        }

        if (!ReplacementVariantExpansionSystem.usesHistoricalSpecializedBehavior(this)) {
            // The remaining expansion variants are Vanilla Phantoms plus their advertised,
            // bounded catalog cue. They must not inherit Grounded Hunter's forced fall/chase.
            return;
        }
        // Keep the historical ID for saved entities, but not its forced ground-homing motion.
        // Sparse ash beneath the real Vanilla flight path preserves the uncanny presentation
        // without changing a Phantom's readable swoop, speed or counter-play.
        if (this.tickCount % 12 == 0 && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.ASH,
                    this.getX(),
                    this.getY() - 0.35D,
                    this.getZ(),
                    2,
                    0.15D,
                    0.05D,
                    0.15D,
                    0.002D);
        }
    }

    private boolean tickLanternEaterBehavior() {
        if (this.lanternTarget != null && !isValidLightTarget(this.lanternTarget)) {
            this.lanternTarget = null;
        }

        if (this.lanternTarget == null && this.tickCount >= this.nextLanternEatTick && this.tickCount >= this.nextLanternScanTick) {
            this.lanternTarget = findNearestLightTarget(14);
            this.nextLanternScanTick = this.tickCount + 25 + this.random.nextInt(20);
        }

        if (this.lanternTarget == null) {
            return false;
        }

        Vec3 target = Vec3.atCenterOf(this.lanternTarget);
        Vec3 delta = target.subtract(this.position());
        if (delta.lengthSqr() > 0.0001D) {
            Vec3 normalized = delta.normalize();
            Vec3 desired = normalized.scale(0.28D);
            Vec3 current = this.getDeltaMovement();
            this.setDeltaMovement(current.add(desired.subtract(current).scale(0.32D)));
        }

        if (this.distanceToSqr(target.x, target.y, target.z) <= 3.0D && this.tickCount >= this.nextLanternEatTick) {
            if (removeLightTarget(this.lanternTarget)) {
                this.nextLanternEatTick = this.tickCount + 80 + this.random.nextInt(40);
            } else {
                this.nextLanternEatTick = this.tickCount + 30;
            }
            this.lanternTarget = null;
            this.nextLanternScanTick = this.nextLanternEatTick;
        }
        return true;
    }

    private boolean isValidLightTarget(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.LANTERN);
    }

    private BlockPos findNearestLightTarget(int radius) {
        BlockPos origin = this.blockPosition();
        BlockPos nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -6; dy <= 6; dy++) {
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    if (!isValidLightTarget(candidate)) {
                        continue;
                    }
                    double dist = candidate.distSqr(origin);
                    if (dist < bestDist) {
                        bestDist = dist;
                        nearest = candidate.immutable();
                    }
                }
            }
        }
        return nearest;
    }

    private boolean removeLightTarget(BlockPos pos) {
        if (!isValidLightTarget(pos)) {
            return false;
        }
        return this.level().destroyBlock(pos, true, this);
    }

    @Override
    public float maxUpStep() {
        return 1.05F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        super.playStepSound(pos, blockState);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("LanternEater", this.lanternEaterMode);
        tag.putBoolean("LanternModeInitialized", this.modeInitialized);
        tag.putInt("LanternNextScanTick", this.nextLanternScanTick);
        tag.putInt("LanternNextEatTick", this.nextLanternEatTick);
        if (this.lanternTarget != null) {
            tag.putInt("LanternTargetX", this.lanternTarget.getX());
            tag.putInt("LanternTargetY", this.lanternTarget.getY());
            tag.putInt("LanternTargetZ", this.lanternTarget.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.lanternEaterMode = tag.getBoolean("LanternEater");
        this.modeInitialized = tag.getBoolean("LanternModeInitialized");
        this.nextLanternScanTick = tag.getInt("LanternNextScanTick");
        this.nextLanternEatTick = tag.getInt("LanternNextEatTick");
        if (tag.contains("LanternTargetX") && tag.contains("LanternTargetY") && tag.contains("LanternTargetZ")) {
            this.lanternTarget = new BlockPos(tag.getInt("LanternTargetX"), tag.getInt("LanternTargetY"), tag.getInt("LanternTargetZ"));
        } else {
            this.lanternTarget = null;
        }
    }
}

