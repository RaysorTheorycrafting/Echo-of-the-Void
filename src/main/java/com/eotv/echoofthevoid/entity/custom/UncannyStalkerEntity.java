package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.event.special.ApprovedSpecialBehaviorRules;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class UncannyStalkerEntity extends Monster implements UncannyEntityMarker {
    private static final float ATTACKER_BODY_SOUND_VOLUME = 1.00F;
    private static final float ATTACKER_RUSH_VOLUME = 1.20F;
    private static final float ATTACKER_SCREAM_VOLUME = 1.45F;
    private static final EntityDataAccessor<Optional<UUID>> TARGET_PLAYER =
            SynchedEntityData.defineId(UncannyStalkerEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Byte> ANIMATION_STYLE =
            SynchedEntityData.defineId(UncannyStalkerEntity.class, EntityDataSerializers.BYTE);

    private int noPathTicks;
    private int hiddenTicks;
    private BlockPos hiddenSpot;
    private int threatCueMode = -1;
    private boolean threatCuePlayed;
    private boolean discovered;

    public UncannyStalkerEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Attacker?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET_PLAYER, Optional.empty());
        builder.define(ANIMATION_STYLE, (byte) AnimationStyle.CRAWL.id());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.28D, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void setHuntTarget(ServerPlayer player) {
        UUID previous = this.entityData.get(TARGET_PLAYER).orElse(null);
        this.entityData.set(TARGET_PLAYER, Optional.of(player.getUUID()));
        this.setTarget(player);
        if (!player.getUUID().equals(previous)) {
            this.threatCueMode = this.random.nextInt(4);
            this.threatCuePlayed = false;
            this.discovered = false;
        }
        this.syncStatsFromPlayer(player);
    }

    public AnimationStyle getAnimationStyle() {
        return AnimationStyle.byId(this.entityData.get(ANIMATION_STYLE));
    }

    public void setAnimationStyle(AnimationStyle style) {
        this.entityData.set(ANIMATION_STYLE, (byte) (style == null ? AnimationStyle.CRAWL.id() : style.id()));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Old saves may contain the former blanket Silent flag. Only explicit sounds are muted now.
        this.setSilent(false);
        UncannyEntityUtil.enableDoorNavigation(this);

        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ServerPlayer targetPlayer = resolveTargetPlayer(serverLevel);
        if (targetPlayer != null && targetPlayer.isAlive()) {
            if (this.hiddenTicks > 0) {
                tickHiddenState(targetPlayer);
                return;
            }

            if (this.getTarget() != targetPlayer) {
                this.setTarget(targetPlayer);
            }
            if (this.distanceToSqr(targetPlayer) <= 6.0D * 6.0D
                    || (this.distanceToSqr(targetPlayer) <= 18.0D * 18.0D
                            && this.hasLineOfSight(targetPlayer)
                            && targetPlayer.hasLineOfSight(this))) {
                this.discovered = true;
            }
            maybePlayThreatCue(serverLevel, targetPlayer);

            if (this.tickCount % 20 == 0) {
                if (!canPathTo(targetPlayer)) {
                    this.noPathTicks += 20;
                    if (this.noPathTicks >= 80) {
                        enterHiddenState(targetPlayer);
                        return;
                    }
                } else {
                    this.noPathTicks = 0;
                }
            }
            return;
        }

        if (this.tickCount % 40 == 0) {
            Player nearest = this.level().getNearestPlayer(this, 24.0D);
            if (nearest instanceof ServerPlayer serverPlayer && serverPlayer.isAlive()) {
                setHuntTarget(serverPlayer);
            }
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!this.discovered) {
            UncannyEntityUtil.suppressStepSound(this, pos, state);
            return;
        }
        super.playStepSound(pos, state);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return UncannySoundRegistry.UNCANNY_ATTACKER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return UncannySoundRegistry.UNCANNY_ATTACKER_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return ATTACKER_BODY_SOUND_VOLUME;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.discovered = true;
        }
        if (hit
                && !this.threatCuePlayed
                && ApprovedSpecialBehaviorRules.shouldPlayAttackerCue(
                        this.threatCueMode, this.distanceToSqr(target), this.hasLineOfSight(target), true)
                && this.level() instanceof ServerLevel level) {
            playThreatCue(level);
        }
        return hit;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(TARGET_PLAYER).ifPresent(uuid -> tag.putUUID("TargetPlayer", uuid));
        tag.putInt("NoPathTicks", this.noPathTicks);
        tag.putInt("HiddenTicks", this.hiddenTicks);
        tag.putInt("ThreatCueMode", this.threatCueMode);
        tag.putBoolean("ThreatCuePlayed", this.threatCuePlayed);
        tag.putBoolean("Discovered", this.discovered);
        tag.putByte("AttackerAnimationStyle", (byte) getAnimationStyle().id());
        if (this.hiddenSpot != null) {
            tag.putInt("HiddenSpotX", this.hiddenSpot.getX());
            tag.putInt("HiddenSpotY", this.hiddenSpot.getY());
            tag.putInt("HiddenSpotZ", this.hiddenSpot.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("TargetPlayer")) {
            this.entityData.set(TARGET_PLAYER, Optional.of(tag.getUUID("TargetPlayer")));
        }
        this.noPathTicks = Math.max(0, tag.getInt("NoPathTicks"));
        this.hiddenTicks = Math.max(0, tag.getInt("HiddenTicks"));
        this.threatCueMode = tag.contains("ThreatCueMode")
                ? Mth.clamp(tag.getInt("ThreatCueMode"), 0, 3)
                : this.random.nextInt(4);
        this.threatCuePlayed = tag.getBoolean("ThreatCuePlayed");
        this.discovered = tag.getBoolean("Discovered");
        this.setAnimationStyle(tag.contains("AttackerAnimationStyle")
                ? AnimationStyle.byId(tag.getByte("AttackerAnimationStyle"))
                : AnimationStyle.CRAWL);
        if (tag.contains("HiddenSpotX") && tag.contains("HiddenSpotY") && tag.contains("HiddenSpotZ")) {
            this.hiddenSpot = new BlockPos(tag.getInt("HiddenSpotX"), tag.getInt("HiddenSpotY"), tag.getInt("HiddenSpotZ"));
        }
    }

    private ServerPlayer resolveTargetPlayer(ServerLevel level) {
        Optional<UUID> targetUuid = this.entityData.get(TARGET_PLAYER);
        return targetUuid.map(uuid -> level.getServer().getPlayerList().getPlayer(uuid)).orElse(null);
    }

    private void syncStatsFromPlayer(ServerPlayer player) {
        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            double maxHealth = Math.max(20.0D, player.getMaxHealth() * 0.9D);
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
            this.setHealth((float) maxHealth);
        }

        if (this.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            double attack = Math.max(4.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.min(12.0D, attack));
        }

        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            double speed = Math.max(0.34D, player.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.35D);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(Math.min(0.50D, speed));
        }

        if (this.getAttribute(Attributes.ARMOR) != null) {
            this.getAttribute(Attributes.ARMOR).setBaseValue(Math.min(20.0D, player.getAttributeValue(Attributes.ARMOR)));
        }

        if (this.getAttribute(Attributes.ARMOR_TOUGHNESS) != null) {
            this.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(Math.min(12.0D, player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)));
        }
    }

    private boolean canPathTo(ServerPlayer player) {
        var path = this.getNavigation().createPath(player, 0);
        return path != null && path.canReach();
    }

    private void maybePlayThreatCue(ServerLevel level, ServerPlayer targetPlayer) {
        if (this.threatCuePlayed) {
            return;
        }
        if (ApprovedSpecialBehaviorRules.shouldPlayAttackerCue(
                this.threatCueMode,
                this.distanceToSqr(targetPlayer),
                this.hasLineOfSight(targetPlayer),
                false)) {
            playThreatCue(level);
        }
    }

    private void playThreatCue(ServerLevel level) {
        this.threatCuePlayed = true;
        boolean aggressiveScream = this.threatCueMode >= 2;
        level.playSound(
                null,
                this,
                aggressiveScream
                        ? UncannySoundRegistry.UNCANNY_ATTACKER_SCREAM.get()
                        : UncannySoundRegistry.UNCANNY_ATTACKER_RUSH.get(),
                SoundSource.HOSTILE,
                aggressiveScream ? ATTACKER_SCREAM_VOLUME : ATTACKER_RUSH_VOLUME,
                aggressiveScream
                        ? 0.92F + this.random.nextFloat() * 0.08F
                        : 0.88F + this.random.nextFloat() * 0.10F);
    }

    private void enterHiddenState(ServerPlayer player) {
        this.hiddenTicks = 20 * 30;
        this.noPathTicks = 0;
        this.setTarget(null);
        this.hiddenSpot = findHiddenSpot(player);
        this.getNavigation().stop();
    }

    private void tickHiddenState(ServerPlayer targetPlayer) {
        this.setTarget(null);

        if (this.hiddenSpot == null || this.blockPosition().distSqr(this.hiddenSpot) < 4 || this.tickCount % 40 == 0) {
            this.hiddenSpot = findHiddenSpot(targetPlayer);
        }

        if (this.hiddenSpot != null) {
            this.getNavigation().moveTo(this.hiddenSpot.getX() + 0.5D, this.hiddenSpot.getY(), this.hiddenSpot.getZ() + 0.5D, 1.1D);
        } else {
            this.getNavigation().stop();
        }

        this.hiddenTicks--;
        if (this.hiddenTicks > 0) {
            return;
        }

        if (canPathTo(targetPlayer)) {
            this.setTarget(targetPlayer);
            return;
        }

        // Stay hidden until pathing becomes possible.
        this.hiddenTicks = 20;
    }

    private BlockPos findHiddenSpot(ServerPlayer targetPlayer) {
        BlockPos origin = targetPlayer.blockPosition();

        for (int attempt = 0; attempt < 32; attempt++) {
            double angle = this.random.nextDouble() * (Math.PI * 2.0D);
            double distance = 5.0D + this.random.nextDouble() * 8.0D;
            int x = Mth.floor(origin.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(origin.getZ() + Math.sin(angle) * distance);

            for (int dy = 4; dy >= -6; dy--) {
                BlockPos candidate = new BlockPos(x, origin.getY() + dy, z);
                if (!isStandable(candidate)) {
                    continue;
                }
                if (isOccludedFromTarget(candidate, targetPlayer)) {
                    return candidate.immutable();
                }
            }
        }
        return null;
    }

    private boolean isStandable(BlockPos pos) {
        BlockState feet = this.level().getBlockState(pos);
        BlockState head = this.level().getBlockState(pos.above());
        BlockState below = this.level().getBlockState(pos.below());
        return feet.isAir() && head.isAir() && below.isSolidRender(this.level(), pos.below());
    }

    private boolean isOccludedFromTarget(BlockPos candidate, ServerPlayer targetPlayer) {
        Vec3 from = Vec3.atCenterOf(candidate).add(0.0D, this.getEyeHeight(), 0.0D);
        Vec3 to = targetPlayer.getEyePosition();
        HitResult hitResult = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hitResult.getType() == HitResult.Type.BLOCK;
    }

    public enum AnimationStyle {
        CRAWL(1),
        OUTSTRETCHED(2);

        private final int id;

        AnimationStyle(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static AnimationStyle byId(int id) {
            return id == OUTSTRETCHED.id ? OUTSTRETCHED : CRAWL;
        }

        public static AnimationStyle random(RandomSource random) {
            return random.nextBoolean() ? CRAWL : OUTSTRETCHED;
        }
    }
}


