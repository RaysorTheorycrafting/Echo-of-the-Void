package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
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
        UncannyEntityUtil.forceSilent(this);
        this.setNoGravity(false);

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
            tickLanternEaterBehavior();
            return;
        }

        Vec3 horizontal = player.position().subtract(this.position());
        horizontal = new Vec3(horizontal.x, 0.0D, horizontal.z);
        if (horizontal.lengthSqr() > 0.01D) {
            double desiredY = this.onGround() ? 0.0D : Math.max(-0.35D, this.getDeltaMovement().y - 0.12D);
            this.setDeltaMovement(horizontal.normalize().scale(0.55D).add(0.0D, desiredY, 0.0D));
        }

        if (this.onGround() && this.horizontalCollision) {
            this.setDeltaMovement(this.getDeltaMovement().x, 0.24D, this.getDeltaMovement().z);
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
            this.setDeltaMovement(normalized.scale(0.62D));
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
        UncannyEntityUtil.suppressStepSound(this, pos, blockState);
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

