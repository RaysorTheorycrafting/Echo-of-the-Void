package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class UncannyUsherEntity extends Monster implements UncannyEntityMarker {
    private static final EntityDataAccessor<Optional<UUID>> TARGET_PLAYER =
            SynchedEntityData.defineId(UncannyUsherEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private BlockPos guideTarget;
    private int nextCueTick;
    private int ignoredTicks;
    private int stuckTicks;
    private int attackRoll;
    private boolean aggressive;
    private Vec3 lastPos = Vec3.ZERO;
    private int cueIntervalTicks = 120;
    private int stuckCheckGrace = 0;
    private boolean guiding;
    private int lookedTicks;
    private boolean followCuePlayed;
    private int guideStartTick;
    private boolean vanishing;
    private int vanishEndTick;
    private int nearTargetIdleTicks;
    private int farNavDoneTicks;

    public UncannyUsherEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Usher?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET_PLAYER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.35D, false));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    public void setupUsher(ServerPlayer player, BlockPos target) {
        this.entityData.set(TARGET_PLAYER, Optional.of(player.getUUID()));
        this.guideTarget = target == null ? null : target.immutable();
        this.attackRoll = this.random.nextInt(100);
        this.ignoredTicks = 0;
        this.stuckTicks = 0;
        this.stuckCheckGrace = 12;
        this.lastPos = this.position();
        this.nextCueTick = this.tickCount + 30;
        this.cueIntervalTicks = 120;
        this.aggressive = false;
        this.guiding = false;
        this.lookedTicks = 0;
        this.followCuePlayed = false;
        this.guideStartTick = -1;
        this.vanishing = false;
        this.vanishEndTick = 0;
        this.nearTargetIdleTicks = 0;
        this.farNavDoneTicks = 0;
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.33D);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel level)) {
            return;
        }

        UncannyEntityUtil.enableDoorNavigation(this);
        ServerPlayer player = resolveTargetPlayer(level);
        if (player == null || !player.isAlive()) {
            this.discard();
            return;
        }
        if (this.vanishing) {
            tickVanishing(level);
            return;
        }

        if (this.aggressive) {
            this.setSilent(true);
            this.setTarget(player);
            this.getNavigation().moveTo(player, 1.45D);
            return;
        }

        this.setTarget(null);
        this.setSilent(true);
        this.jumping = false;
        this.lookAt(player, 70.0F, 70.0F);
        this.getLookControl().setLookAt(player.getX(), player.getEyeY(), player.getZ(), 70.0F, 70.0F);
        if (!this.guiding) {
            this.getNavigation().stop();
        }

        boolean looked = isPlayerLookingAtMe(player);
        if (looked) {
            this.lookedTicks = Math.min(200, this.lookedTicks + 1);
        } else {
            this.lookedTicks = Math.max(0, this.lookedTicks - 2);
        }
        boolean playerFollowing = player.distanceToSqr(this) <= 14.0D * 14.0D;
        if (!this.followCuePlayed && this.lookedTicks >= 20) {
            playCue(level, player, true);
            this.followCuePlayed = true;
            this.ignoredTicks = 0;
            this.nextCueTick = this.tickCount + 200;
        } else if (looked
                && this.followCuePlayed
                && this.tickCount >= this.nextCueTick
                && (!this.guiding || !playerFollowing)
                && player.distanceToSqr(this) > 9.0D * 9.0D) {
            playCue(level, player, true);
            this.ignoredTicks = 0;
            this.nextCueTick = this.tickCount + 200;
        } else if (this.tickCount >= this.nextCueTick
                && !looked
                && (!this.guiding || !playerFollowing)
                && !(this.followCuePlayed && player.distanceToSqr(this) <= 9.0D * 9.0D)) {
            playCue(level, player, false);
            this.ignoredTicks += 20;
            this.cueIntervalTicks = this.followCuePlayed ? 150 + this.random.nextInt(91) : 95 + this.random.nextInt(61);
            this.nextCueTick = this.tickCount + this.cueIntervalTicks;
        }

        if (!this.guiding
                && this.followCuePlayed
                && this.guideTarget != null
                && player.distanceToSqr(this) <= 7.0D * 7.0D) {
            this.guiding = true;
            this.guideStartTick = this.tickCount;
            this.stuckTicks = 0;
            this.stuckCheckGrace = 12;
            this.lastPos = this.position();
        }

        if (this.guiding && this.guideTarget != null) {
            if (playerFollowing) {
                this.getNavigation().moveTo(
                        this.guideTarget.getX() + 0.5D,
                        this.guideTarget.getY(),
                        this.guideTarget.getZ() + 0.5D,
                        1.14D);
                clearLeavesInFront(level);
                tryStepUpObstacle(level);
            } else {
                this.getNavigation().stop();
            }

            double targetX = this.guideTarget.getX() + 0.5D;
            double targetY = this.guideTarget.getY() + 0.5D;
            double targetZ = this.guideTarget.getZ() + 0.5D;
            double dx = this.getX() - targetX;
            double dz = this.getZ() - targetZ;
            double horizontalDistSqr = dx * dx + dz * dz;
            double verticalAbs = Math.abs(this.getY() - targetY);
            double toTarget = dx * dx + dz * dz + (this.getY() - targetY) * (this.getY() - targetY);

            if ((this.tickCount % 40) == 0 && com.eotv.echoofthevoid.config.UncannyConfig.DEBUG_LOGS.get()) {
                EchoOfTheVoid.LOGGER.info(
                        "[UncannyDebug/Usher] guideTick player={} pos={} target={} horizDistSqr={} vertical={} navDone={} following={} stuckTicks={}",
                        player.getGameProfile().getName(),
                        this.blockPosition(),
                        this.guideTarget,
                        horizontalDistSqr,
                        verticalAbs,
                        this.getNavigation().isDone(),
                        playerFollowing,
                        this.stuckTicks);
            }

            if (horizontalDistSqr <= 12.0D * 12.0D) {
                debugLogVanish(level, player, "close_radius", horizontalDistSqr, verticalAbs);
                startVanishing(level);
                return;
            }
            if (horizontalDistSqr <= 30.0D * 30.0D && this.getNavigation().isDone()) {
                debugLogVanish(level, player, "nav_done_close", horizontalDistSqr, verticalAbs);
                startVanishing(level);
                return;
            }
            if (verticalAbs >= 16.0D && horizontalDistSqr <= 35.0D * 35.0D) {
                debugLogVanish(level, player, "vertical_unreachable", horizontalDistSqr, verticalAbs);
                startVanishing(level);
                return;
            }
            if (this.guideStartTick >= 0
                    && (this.tickCount - this.guideStartTick) >= 20 * 45
                    && horizontalDistSqr <= 36.0D * 36.0D) {
                debugLogVanish(level, player, "time_close", horizontalDistSqr, verticalAbs);
                startVanishing(level);
                return;
            }
            if (this.guideStartTick >= 0
                    && (this.tickCount - this.guideStartTick) >= 20 * 90
                    && (horizontalDistSqr <= 72.0D * 72.0D || toTarget <= 90.0D * 90.0D || verticalAbs <= 24.0D)) {
                debugLogVanish(level, player, "time_failsafe", horizontalDistSqr, verticalAbs);
                startVanishing(level);
                return;
            }
            if (this.getNavigation().isDone() && horizontalDistSqr <= 40.0D * 40.0D) {
                this.nearTargetIdleTicks++;
                if (this.nearTargetIdleTicks >= 40) {
                    debugLogVanish(level, player, "idle_near_target", horizontalDistSqr, verticalAbs);
                    startVanishing(level);
                    return;
                }
            } else {
                this.nearTargetIdleTicks = 0;
            }

            if (playerFollowing && this.getNavigation().isDone() && horizontalDistSqr >= 60.0D * 60.0D) {
                this.farNavDoneTicks++;
                if (this.farNavDoneTicks >= 20 * 12) {
                    debugLogCompassDrop(level, player, horizontalDistSqr, this.farNavDoneTicks / 20);
                    level.addFreshEntity(new ItemEntity(
                            level,
                            this.getX(),
                            this.getY() + 0.2D,
                            this.getZ(),
                            new ItemStack(UncannyItemRegistry.UNCANNY_COMPASS.get())));
                    level.playSound(null, this.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 0.9F, 0.86F);
                    startVanishing(level);
                    return;
                }
            } else {
                this.farNavDoneTicks = 0;
            }
        }

        if (this.guiding && playerFollowing && this.tickCount % 20 == 0) {
            if (this.stuckCheckGrace > 0) {
                this.stuckCheckGrace--;
                this.stuckTicks = 0;
            } else {
                if (this.lastPos.distanceToSqr(this.position()) < 0.0225D) {
                    this.stuckTicks++;
                } else {
                    this.stuckTicks = 0;
                }
            }
            this.lastPos = this.position();

            if (this.stuckTicks >= 50
                    && this.guideTarget != null
                    && this.guideStartTick >= 0
                    && (this.tickCount - this.guideStartTick) >= 220) {
                double targetDistSqr = this.distanceToSqr(
                        this.guideTarget.getX() + 0.5D,
                        this.guideTarget.getY() + 0.5D,
                        this.guideTarget.getZ() + 0.5D);
                if (targetDistSqr <= 36.0D) {
                    this.stuckTicks = 0;
                    this.stuckCheckGrace = 10;
                } else if (targetDistSqr <= 24.0D * 24.0D) {
                    debugLogVanish(level, player, "stuck_near_target", targetDistSqr, Math.abs(this.getY() - (this.guideTarget.getY() + 0.5D)));
                    startVanishing(level);
                    return;
                } else if (this.guideTarget != null) {
                    double hdx = this.getX() - (this.guideTarget.getX() + 0.5D);
                    double hdz = this.getZ() - (this.guideTarget.getZ() + 0.5D);
                    if ((hdx * hdx + hdz * hdz) <= 56.0D * 56.0D) {
                        debugLogVanish(level, player, "stuck_horizontal_near", hdx * hdx + hdz * hdz, Math.abs(this.getY() - (this.guideTarget.getY() + 0.5D)));
                        startVanishing(level);
                        return;
                    }
                } else {
                    debugLogCompassDrop(level, player, targetDistSqr, this.stuckTicks);
                    level.addFreshEntity(new ItemEntity(
                            level,
                            this.getX(),
                            this.getY() + 0.2D,
                            this.getZ(),
                            new ItemStack(UncannyItemRegistry.UNCANNY_COMPASS.get())));
                    level.playSound(null, this.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 0.9F, 0.86F);
                    this.discard();
                    return;
                }
            } else if (this.stuckTicks >= 50
                    && this.guideStartTick >= 0
                    && (this.tickCount - this.guideStartTick) >= 220) {
                debugLogCompassDrop(level, player, -1.0D, this.stuckTicks);
                level.addFreshEntity(new ItemEntity(
                        level,
                        this.getX(),
                        this.getY() + 0.2D,
                        this.getZ(),
                        new ItemStack(UncannyItemRegistry.UNCANNY_COMPASS.get())));
                level.playSound(null, this.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 0.9F, 0.86F);
                this.discard();
                return;
            } else if (this.stuckTicks >= 24) {
                this.getNavigation().stop();
                if (this.guideTarget != null) {
                    this.getNavigation().moveTo(
                            this.guideTarget.getX() + 0.5D,
                            this.guideTarget.getY(),
                            this.guideTarget.getZ() + 0.5D,
                    1.18D);
                }
            }
        } else if (!this.guiding) {
            this.stuckTicks = 0;
            this.guideStartTick = -1;
            this.lastPos = this.position();
            this.nearTargetIdleTicks = 0;
            this.farNavDoneTicks = 0;
        }

        if (this.ignoredTicks >= 180) {
            if (this.attackRoll >= 10) {
                level.playSound(null, this.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1.0F, 0.8F);
                this.discard();
                return;
            }
            this.aggressive = true;
            if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.40D);
            }
        }
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

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        UncannyEntityUtil.suppressStepSound(this, pos, blockState);
    }

    @Override
    public void jumpFromGround() {
        // Usher never jumps: only step-up and pathing adjustments are used.
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        UncannyEntityUtil.dropPulseStyleRewards(level, this, this.random);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(TARGET_PLAYER).ifPresent(uuid -> tag.putUUID("TargetPlayer", uuid));
        if (this.guideTarget != null) {
            tag.putInt("GuideTargetX", this.guideTarget.getX());
            tag.putInt("GuideTargetY", this.guideTarget.getY());
            tag.putInt("GuideTargetZ", this.guideTarget.getZ());
        }
        tag.putInt("NextCueTick", this.nextCueTick);
        tag.putInt("IgnoredTicks", this.ignoredTicks);
        tag.putInt("StuckTicks", this.stuckTicks);
        tag.putInt("AttackRoll", this.attackRoll);
        tag.putBoolean("Aggressive", this.aggressive);
        tag.putInt("CueIntervalTicks", this.cueIntervalTicks);
        tag.putInt("StuckCheckGrace", this.stuckCheckGrace);
        tag.putBoolean("Guiding", this.guiding);
        tag.putInt("LookedTicks", this.lookedTicks);
        tag.putBoolean("FollowCuePlayed", this.followCuePlayed);
        tag.putInt("GuideStartTick", this.guideStartTick);
        tag.putBoolean("Vanishing", this.vanishing);
        tag.putInt("VanishEndTick", this.vanishEndTick);
        tag.putInt("NearTargetIdleTicks", this.nearTargetIdleTicks);
        tag.putInt("FarNavDoneTicks", this.farNavDoneTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("TargetPlayer")) {
            this.entityData.set(TARGET_PLAYER, Optional.of(tag.getUUID("TargetPlayer")));
        }
        if (tag.contains("GuideTargetX") && tag.contains("GuideTargetY") && tag.contains("GuideTargetZ")) {
            this.guideTarget = new BlockPos(tag.getInt("GuideTargetX"), tag.getInt("GuideTargetY"), tag.getInt("GuideTargetZ"));
        }
        this.nextCueTick = tag.getInt("NextCueTick");
        this.ignoredTicks = Math.max(0, tag.getInt("IgnoredTicks"));
        this.stuckTicks = Math.max(0, tag.getInt("StuckTicks"));
        this.attackRoll = Math.max(0, tag.getInt("AttackRoll"));
        this.aggressive = tag.getBoolean("Aggressive");
        this.cueIntervalTicks = Math.max(40, tag.contains("CueIntervalTicks") ? tag.getInt("CueIntervalTicks") : 120);
        this.stuckCheckGrace = Math.max(0, tag.getInt("StuckCheckGrace"));
        this.guiding = tag.getBoolean("Guiding");
        this.lookedTicks = Math.max(0, tag.getInt("LookedTicks"));
        this.followCuePlayed = tag.getBoolean("FollowCuePlayed");
        this.guideStartTick = tag.contains("GuideStartTick") ? tag.getInt("GuideStartTick") : -1;
        this.vanishing = tag.getBoolean("Vanishing");
        this.vanishEndTick = tag.getInt("VanishEndTick");
        this.nearTargetIdleTicks = Math.max(0, tag.getInt("NearTargetIdleTicks"));
        this.farNavDoneTicks = Math.max(0, tag.getInt("FarNavDoneTicks"));
    }

    private ServerPlayer resolveTargetPlayer(ServerLevel level) {
        Optional<UUID> target = this.entityData.get(TARGET_PLAYER);
        if (target.isPresent()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(target.get());
            if (player != null) {
                return player;
            }
        }
        Player nearest = level.getNearestPlayer(this, 36.0D);
        if (nearest instanceof ServerPlayer serverPlayer) {
            this.entityData.set(TARGET_PLAYER, Optional.of(serverPlayer.getUUID()));
            return serverPlayer;
        }
        return null;
    }

    private boolean isPlayerLookingAtMe(ServerPlayer player) {
        if (!player.hasLineOfSight(this)) {
            return false;
        }
        Vec3 toEntity = this.position().add(0.0D, this.getEyeHeight(), 0.0D).subtract(player.getEyePosition()).normalize();
        Vec3 look = player.getViewVector(1.0F).normalize();
        return look.dot(toEntity) > 0.955D;
    }

    private void playCue(ServerLevel level, ServerPlayer player, boolean looked) {
        SoundEvent event;
        float volume;
        float pitch;
        if (looked) {
            event = UncannySoundRegistry.UNCANNY_FOLLOW_ME_CREATURE_GLITCH.get();
            volume = 0.95F;
            pitch = 1.0F;
            if (event == null) {
                event = UncannySoundRegistry.UNCANNY_WHISPER.get();
                volume = 0.65F;
                pitch = 0.8F;
            }
        } else {
            event = UncannySoundRegistry.UNCANNY_PSSS.get();
            volume = 0.5F;
            pitch = 0.92F + level.random.nextFloat() * 0.12F;
            if (event == null) {
                event = UncannySoundRegistry.UNCANNY_MONSTER_BREATH.get();
            }
        }
        if (event != null) {
            player.playNotifySound(event, SoundSource.HOSTILE, volume, pitch);
        }
    }

    private void debugLogCompassDrop(ServerLevel level, ServerPlayer player, double targetDistSqr, int stuckSeconds) {
        if (!com.eotv.echoofthevoid.config.UncannyConfig.DEBUG_LOGS.get()) {
            return;
        }
        String targetInfo = this.guideTarget == null
                ? "none"
                : this.guideTarget.getX() + "," + this.guideTarget.getY() + "," + this.guideTarget.getZ();
        EchoOfTheVoid.LOGGER.info(
                "[UncannyDebug/Usher] Compass fallback drop player={} stuck={}s target={} distSqr={}",
                player.getGameProfile().getName(),
                stuckSeconds,
                targetInfo,
                targetDistSqr);
    }

    private void tryStepUpObstacle(ServerLevel level) {
        Vec3 forward = this.getDeltaMovement();
        if (forward.horizontalDistanceSqr() < 0.0009D && this.guideTarget != null) {
            Vec3 towardTarget = Vec3.atCenterOf(this.guideTarget).subtract(this.position());
            forward = new Vec3(towardTarget.x, 0.0D, towardTarget.z);
        }
        if (forward.horizontalDistanceSqr() < 0.0009D) {
            return;
        }
        forward = new Vec3(forward.x, 0.0D, forward.z).normalize();
        BlockPos aheadBase = BlockPos.containing(
                this.getX() + forward.x * 0.9D,
                this.getY(),
                this.getZ() + forward.z * 0.9D);
        if (isPassableForStep(level, aheadBase) && isPassableForStep(level, aheadBase.above())) {
            return;
        }
        for (int step = 1; step <= 2; step++) {
            BlockPos feet = aheadBase.above(step);
            BlockPos head = feet.above();
            BlockPos floor = feet.below();
            if (!isPassableForStep(level, feet) || !isPassableForStep(level, head)) {
                continue;
            }
            if (!level.getBlockState(floor).isSolidRender(level, floor)) {
                continue;
            }
            this.setPos(
                    aheadBase.getX() + 0.5D - forward.x * 0.15D,
                    feet.getY(),
                    aheadBase.getZ() + 0.5D - forward.z * 0.15D);
            return;
        }
    }

    private boolean isPassableForStep(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.getCollisionShape(level, pos).isEmpty();
    }

    private void clearLeavesInFront(ServerLevel level) {
        if ((this.tickCount % 4) != 0) {
            return;
        }
        Vec3 forward = this.getDeltaMovement().horizontalDistanceSqr() > 0.0001D
                ? this.getDeltaMovement().normalize()
                : this.getLookAngle().normalize();
        if (forward.lengthSqr() < 0.0001D) {
            return;
        }
        BlockPos origin = BlockPos.containing(this.getX() + forward.x * 0.9D, this.getY() + 0.9D, this.getZ() + forward.z * 0.9D);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos target = origin.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(target);
                    if (state.getBlock() instanceof LeavesBlock) {
                        level.destroyBlock(target, false, this);
                    } else if (!state.isAir() && state.canBeReplaced()) {
                        level.destroyBlock(target, false, this);
                    }
                }
            }
        }
    }

    private void startVanishing(ServerLevel level) {
        if (this.vanishing) {
            return;
        }
        this.vanishing = true;
        this.vanishEndTick = this.tickCount + 26;
        this.setNoGravity(true);
        this.getNavigation().stop();
        level.playSound(null, this.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 0.85F, 0.9F);
    }

    private void tickVanishing(ServerLevel level) {
        this.setDeltaMovement(0.0D, -0.065D, 0.0D);
        this.setPos(this.getX(), this.getY() - 0.065D, this.getZ());
        if (this.tickCount >= this.vanishEndTick) {
            this.setNoGravity(false);
            this.discard();
        }
    }

    private void debugLogVanish(ServerLevel level, ServerPlayer player, String reason, double horizontalDistSqr, double verticalAbs) {
        if (!com.eotv.echoofthevoid.config.UncannyConfig.DEBUG_LOGS.get()) {
            return;
        }
        EchoOfTheVoid.LOGGER.info(
                "[UncannyDebug/Usher] vanish reason={} player={} pos={} target={} horizDistSqr={} vertical={} navDone={} guideForTicks={}",
                reason,
                player.getGameProfile().getName(),
                this.blockPosition(),
                this.guideTarget,
                horizontalDistSqr,
                verticalAbs,
                this.getNavigation().isDone(),
                this.guideStartTick < 0 ? -1 : (this.tickCount - this.guideStartTick));
    }
}
