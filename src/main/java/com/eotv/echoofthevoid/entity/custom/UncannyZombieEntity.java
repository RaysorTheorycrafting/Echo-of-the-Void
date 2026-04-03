package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.network.UncannyZombieRalePayload;
import com.eotv.echoofthevoid.phase.UncannyPhase;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import com.eotv.echoofthevoid.state.UncannyWorldState;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.DifficultyInstance;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class UncannyZombieEntity extends Zombie implements UncannyEntityMarker {
    private static final EntityDataAccessor<Integer> ZOMBIE_VARIANT =
            SynchedEntityData.defineId(UncannyZombieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BAIT_TRIGGERED =
            SynchedEntityData.defineId(UncannyZombieEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int RALE_GLOBAL_COOLDOWN_TICKS = 20 * 4;
    private static long lastGlobalRaleTick = Long.MIN_VALUE;
    private static final float TALL_GLITCH_TOTAL_HEIGHT = 6.0F;
    private static final float BASE_ZOMBIE_HEIGHT = 1.95F;
    private static final float TALL_GLITCH_Y_SCALE = TALL_GLITCH_TOTAL_HEIGHT / BASE_ZOMBIE_HEIGHT;

    private double defaultMovementSpeed = Double.NaN;
    private int variantAmbientDelayTicks = 12;
    private int variantLureDelayTicks = 45;
    private int variantTeleportDelayTicks = 80;

    public UncannyZombieEntity(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Zombie?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ZOMBIE_VARIANT, ZombieVariant.UNASSIGNED.id());
        builder.define(BAIT_TRIGGERED, false);
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor levelAccessor,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(levelAccessor, difficulty, spawnType, spawnGroupData);
        if (levelAccessor instanceof ServerLevel serverLevel) {
            ensureVariantAssigned(serverLevel);
        }
        return data;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        ZombieVariant variant = getZombieVariant();
        if (variant == ZombieVariant.UNASSIGNED && this.level() instanceof ServerLevel serverLevel) {
            ensureVariantAssigned(serverLevel);
            variant = getZombieVariant();
        }

        applyVariantPose(variant);
        applyTallGlitchBoundingBox(variant);

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        cacheDefaultMovementSpeed();
        applyVariantMovement(variant);
        tickVariantBehavior(serverLevel, variant);
    }

    private void applyTallGlitchBoundingBox(ZombieVariant variant) {
        double width = this.getType().getDimensions().width();
        double halfWidth = width * 0.5D;
        double expectedHeight = variant == ZombieVariant.TALL_GLITCH_TEST ? TALL_GLITCH_TOTAL_HEIGHT : this.getType().getDimensions().height();
        AABB current = this.getBoundingBox();
        double currentHeight = current.getYsize();
        if (Math.abs(currentHeight - expectedHeight) < 0.02D) {
            return;
        }

        AABB targetBox = new AABB(
                this.getX() - halfWidth,
                this.getY(),
                this.getZ() - halfWidth,
                this.getX() + halfWidth,
                this.getY() + expectedHeight,
                this.getZ() + halfWidth);
        this.setBoundingBox(targetBox);
    }

    @Override
    public boolean isAggressive() {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        if (getZombieVariant() == ZombieVariant.TALL_GLITCH_TEST && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    UncannySoundRegistry.UNCANNY_ZOMBIE_TALL_STEP.get(),
                    this.getSoundSource(),
                    0.95F,
                    0.92F + this.random.nextFloat() * 0.20F);
            return;
        }
        if (getZombieVariant() == ZombieVariant.BAIT && isBaitTriggered()) {
            UncannyEntityUtil.suppressStepSound(this, pos, blockState);
            return;
        }
        super.playStepSound(pos, blockState);
    }

    @Override
    public void playAmbientSound() {
        ZombieVariant variant = getZombieVariant();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            super.playAmbientSound();
            return;
        }

        if (variant == ZombieVariant.RALE_FICTIF) {
            // Handled explicitly by tickRaleFictifAmbient to keep the effect always perceptible.
            return;
        }

        if (variant == ZombieVariant.DESYNC) {
            super.playAmbientSound();
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        ZombieVariant variant = getZombieVariant();
        if (variant == ZombieVariant.RALE_FICTIF
                || variant == ZombieVariant.BROKEN_NECK
                || variant == ZombieVariant.RENDER_GLITCH
                || variant == ZombieVariant.TALL_GLITCH_TEST) {
            return null;
        }
        if (variant == ZombieVariant.BAIT) {
            return null;
        }
        return super.getAmbientSound();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        if (getZombieVariant() == ZombieVariant.TALL_GLITCH_TEST) {
            return null;
        }
        if (getZombieVariant() == ZombieVariant.BAIT && isBaitTriggered()) {
            return null;
        }
        return super.getHurtSound(damageSource);
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        if (getZombieVariant() == ZombieVariant.TALL_GLITCH_TEST) {
            return null;
        }
        if (getZombieVariant() == ZombieVariant.BAIT && isBaitTriggered()) {
            return null;
        }
        return super.getDeathSound();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && getZombieVariant() == ZombieVariant.TALL_GLITCH_TEST && this.level() instanceof ServerLevel serverLevel) {
            playTallGlitchBurst(serverLevel, false);
        }
        if (result && getZombieVariant() == ZombieVariant.BAIT && !isBaitTriggered()) {
            setBaitTriggered(true);
            this.variantLureDelayTicks = 200;
            this.variantAmbientDelayTicks = 80;
        }
        return result;
    }

    @Override
    public void die(DamageSource damageSource) {
        if (getZombieVariant() == ZombieVariant.TALL_GLITCH_TEST && this.level() instanceof ServerLevel serverLevel) {
            playTallGlitchBurst(serverLevel, true);
        }
        super.die(damageSource);
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean result = super.doHurtTarget(entity);
        if (result && getZombieVariant() == ZombieVariant.RENDER_GLITCH && entity instanceof Player player) {
            this.level().playSound(
                    null,
                    player.getX(),
                    player.getEyeY(),
                    player.getZ(),
                    SoundEvents.ELDER_GUARDIAN_CURSE,
                    SoundSource.HOSTILE,
                    2.2F,
                    0.58F + this.random.nextFloat() * 0.2F);
        }
        return result;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("UncannyZombieVariant", getZombieVariant().id());
        tag.putBoolean("UncannyBaitTriggered", isBaitTriggered());
        tag.putInt("UncannyAmbientDelay", this.variantAmbientDelayTicks);
        tag.putInt("UncannyLureDelay", this.variantLureDelayTicks);
        tag.putInt("UncannyTeleportDelay", this.variantTeleportDelayTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setZombieVariant(ZombieVariant.fromId(tag.getInt("UncannyZombieVariant")));
        setBaitTriggered(tag.getBoolean("UncannyBaitTriggered"));
        this.variantAmbientDelayTicks = Math.max(10, tag.getInt("UncannyAmbientDelay"));
        this.variantLureDelayTicks = Math.max(10, tag.getInt("UncannyLureDelay"));
        this.variantTeleportDelayTicks = Math.max(10, tag.getInt("UncannyTeleportDelay"));
    }

    public ZombieVariant getZombieVariant() {
        return ZombieVariant.fromId(this.entityData.get(ZOMBIE_VARIANT));
    }

    public boolean isBaitTriggered() {
        return this.entityData.get(BAIT_TRIGGERED);
    }

    private void setZombieVariant(ZombieVariant variant) {
        ZombieVariant previous = getZombieVariant();
        this.entityData.set(ZOMBIE_VARIANT, variant.id());
        if (previous != variant) {
            this.refreshDimensions();
        }
    }

    public void forceTallGlitchTestVariant() {
        setZombieVariant(ZombieVariant.TALL_GLITCH_TEST);
        this.variantAmbientDelayTicks = 4;
        this.setBaitTriggered(false);
    }

    public boolean isTallGlitchTestVariant() {
        return getZombieVariant() == ZombieVariant.TALL_GLITCH_TEST;
    }

    public static float tallGlitchYScale() {
        return TALL_GLITCH_Y_SCALE;
    }

    private void setBaitTriggered(boolean triggered) {
        this.entityData.set(BAIT_TRIGGERED, triggered);
    }

    private void ensureVariantAssigned(ServerLevel serverLevel) {
        if (getZombieVariant() != ZombieVariant.UNASSIGNED) {
            return;
        }
        UncannyPhase phase = UncannyWorldState.get(serverLevel.getServer()).getPhase();
        ZombieVariant variant = rollVariantForPhase(phase);
        setZombieVariant(variant);
        if (variant == ZombieVariant.RALE_FICTIF) {
            this.variantAmbientDelayTicks = 4;
        }
    }

    private ZombieVariant rollVariantForPhase(UncannyPhase phase) {
        int roll = this.random.nextInt(100);
        return switch (phase) {
            case PHASE_1 -> ZombieVariant.RALE_FICTIF;
            case PHASE_2 -> roll < 65 ? ZombieVariant.BROKEN_NECK : ZombieVariant.RALE_FICTIF;
            case PHASE_3 -> {
                if (roll < 55) {
                    yield ZombieVariant.BAIT;
                }
                if (roll < 85) {
                    yield ZombieVariant.BROKEN_NECK;
                }
                yield ZombieVariant.RALE_FICTIF;
            }
            case PHASE_4 -> {
                if (roll < 40) {
                    yield ZombieVariant.DESYNC;
                }
                if (roll < 65) {
                    yield ZombieVariant.BAIT;
                }
                if (roll < 85) {
                    yield ZombieVariant.RENDER_GLITCH;
                }
                yield ZombieVariant.BROKEN_NECK;
            }
        };
    }

    private void cacheDefaultMovementSpeed() {
        if (!Double.isNaN(this.defaultMovementSpeed)) {
            return;
        }
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            this.defaultMovementSpeed = speed.getBaseValue();
        }
    }

    private void applyVariantMovement(ZombieVariant variant) {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }

        double base = Double.isNaN(this.defaultMovementSpeed) ? speed.getBaseValue() : this.defaultMovementSpeed;
        double desired = switch (variant) {
            case BROKEN_NECK -> (this.tickCount % 12) < 5 ? base * 0.28D : base * 1.15D;
            case BAIT -> isBaitTriggered() ? Math.max(0.37D, base * 1.65D) : base * 0.72D;
            case DESYNC -> base * 0.50D;
            case RENDER_GLITCH -> base * 1.04D;
            case TALL_GLITCH_TEST -> base * 0.86D;
            case RALE_FICTIF, UNASSIGNED -> base;
        };

        desired = Math.max(0.05D, Math.min(0.75D, desired));
        if (Math.abs(speed.getBaseValue() - desired) > 0.0005D) {
            speed.setBaseValue(desired);
        }
    }

    private void tickVariantBehavior(ServerLevel serverLevel, ZombieVariant variant) {
        if (variant == ZombieVariant.RALE_FICTIF) {
            this.setSilent(true);
        } else if (!(variant == ZombieVariant.BAIT && isBaitTriggered())) {
            this.setSilent(false);
        }

        switch (variant) {
            case BROKEN_NECK -> tickBrokenNeckAmbient(serverLevel);
            case BAIT -> tickBaitMode(serverLevel);
            case RENDER_GLITCH -> tickRenderGlitch(serverLevel);
            case TALL_GLITCH_TEST -> tickTallGlitchAmbient(serverLevel);
            case RALE_FICTIF -> {
                this.setSprinting(false);
                if (isBaitTriggered()) {
                    this.setBaitTriggered(false);
                }
                tickRaleFictifAmbient(serverLevel);
            }
            case DESYNC, UNASSIGNED -> {
                this.setSprinting(false);
                if (variant != ZombieVariant.BAIT && isBaitTriggered()) {
                    this.setBaitTriggered(false);
                }
            }
        }
    }

    private void tickTallGlitchAmbient(ServerLevel serverLevel) {
        if (--this.variantAmbientDelayTicks > 0) {
            return;
        }

        serverLevel.playSound(
                null,
                this.getX(),
                this.getY() + this.getBbHeight() * 0.85D,
                this.getZ(),
                UncannySoundRegistry.UNCANNY_ZOMBIE_TALL_AMBIENT.get(),
                this.getSoundSource(),
                1.08F + this.random.nextFloat() * 0.20F,
                0.95F + this.random.nextFloat() * 0.10F);

        this.variantAmbientDelayTicks = 32 + this.random.nextInt(34);
    }

    private void playTallGlitchBurst(ServerLevel serverLevel, boolean death) {
        serverLevel.playSound(
                null,
                this.getX(),
                this.getY() + this.getBbHeight() * 0.85D,
                this.getZ(),
                death ? UncannySoundRegistry.UNCANNY_ZOMBIE_TALL_DEATH.get() : UncannySoundRegistry.UNCANNY_ZOMBIE_TALL_HURT.get(),
                this.getSoundSource(),
                death ? 1.35F : 1.15F,
                0.94F + this.random.nextFloat() * 0.12F);
    }

    private void tickRaleFictifAmbient(ServerLevel serverLevel) {
        if (--this.variantAmbientDelayTicks > 0) {
            return;
        }

        long now = serverLevel.getServer().getTickCount();
        long last = lastGlobalRaleTick;
        if (last != Long.MIN_VALUE && now < last) {
            // World tick reset between worlds: clear stale cooldown marker.
            lastGlobalRaleTick = Long.MIN_VALUE;
            last = Long.MIN_VALUE;
            debugRale(serverLevel, "RALE cooldown-reset worldTick={} zombie={}", now, this.getUUID());
        }

        boolean cooldownReady = last == Long.MIN_VALUE || (now - last) >= RALE_GLOBAL_COOLDOWN_TICKS;
        if (!cooldownReady) {
            long delta = now - last;
            debugRale(serverLevel,
                    "RALE blocked-cooldown zombie={} now={} last={} remaining={}t",
                    this.getUUID(),
                    now,
                    last,
                    RALE_GLOBAL_COOLDOWN_TICKS - delta);
            this.variantAmbientDelayTicks = 16 + this.random.nextInt(25);
            return;
        }

        ServerPlayer receiver = resolveRaleReceiver(serverLevel);
        if (receiver == null) {
            debugRale(serverLevel,
                    "RALE no-receiver zombie={} pos={} now={}",
                    this.getUUID(),
                    this.blockPosition(),
                    now);
            this.variantAmbientDelayTicks = 16 + this.random.nextInt(25);
            return;
        }

        float pitch = 0.66F + this.random.nextFloat() * 0.22F;
        playRaleAtReceiverHead(serverLevel, receiver, pitch);
        lastGlobalRaleTick = now;
        this.variantAmbientDelayTicks = 16 + this.random.nextInt(25);
    }

    private void tickBrokenNeckAmbient(ServerLevel serverLevel) {
        if (--this.variantAmbientDelayTicks > 0) {
            return;
        }

        serverLevel.playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                SoundEvents.ZOMBIE_AMBIENT,
                this.getSoundSource(),
                0.20F,
                0.38F + this.random.nextFloat() * 0.12F);
        this.variantAmbientDelayTicks = 70 + this.random.nextInt(50);
    }

    private void tickBaitMode(ServerLevel serverLevel) {
        if (!isBaitTriggered()) {
            this.setSprinting(false);
            if (this.getTarget() != null) {
                this.setTarget(null);
                this.getNavigation().stop();
            }

            if (isInDarkness(serverLevel) && --this.variantLureDelayTicks <= 0) {
                playBaitLureSound(serverLevel);
                this.variantLureDelayTicks = 70 + this.random.nextInt(90);
            }
            return;
        }

        this.setSilent(true);
        this.setSprinting(true);

        if (!(this.getTarget() instanceof Player player) || !player.isAlive()) {
            Player nearestPlayer = serverLevel.getNearestPlayer(this, 28.0D);
            if (nearestPlayer != null) {
                this.setTarget(nearestPlayer);
            }
        }

        if (this.getTarget() != null) {
            this.getNavigation().moveTo(this.getTarget(), 1.35D);
        }
    }

    private void tickRenderGlitch(ServerLevel serverLevel) {
        if (--this.variantTeleportDelayTicks <= 0) {
            attemptGlitchTeleport();
            this.variantTeleportDelayTicks = 60 + this.random.nextInt(41);
        }

        if (--this.variantAmbientDelayTicks <= 0) {
            serverLevel.playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    SoundEvents.ZOMBIE_AMBIENT,
                    this.getSoundSource(),
                    1.28F,
                    0.24F + this.random.nextFloat() * 1.7F);
            this.variantAmbientDelayTicks = 60 + this.random.nextInt(60);
        }
    }

    private void attemptGlitchTeleport() {
        for (int attempt = 0; attempt < 8; attempt++) {
            double dx = (this.random.nextBoolean() ? 1 : -1) * (1 + this.random.nextInt(2));
            double dz = (this.random.nextBoolean() ? 1 : -1) * (1 + this.random.nextInt(2));
            double x = this.getX() + dx + (this.random.nextDouble() - 0.5D) * 0.25D;
            double y = this.getY() + this.random.nextInt(3) - 1;
            double z = this.getZ() + dz + (this.random.nextDouble() - 0.5D) * 0.25D;
            if (this.randomTeleport(x, y, z, true)) {
                return;
            }
        }
    }

    private void applyVariantPose(ZombieVariant variant) {
        if (variant != ZombieVariant.BROKEN_NECK) {
            return;
        }
        float forcedHeadYaw = this.getYRot() + 90.0F;
        this.setYHeadRot(forcedHeadYaw);
        this.yHeadRotO = forcedHeadYaw;
    }

    private void playRaleAtReceiverHead(ServerLevel serverLevel, ServerPlayer receiver, float pitch) {
        PacketDistributor.sendToPlayer(receiver, new UncannyZombieRalePayload(1.85F, pitch));
        debugRale(serverLevel,
                "RALE sent zombie={} -> player={} pos={} volume={} pitch={}",
                this.getUUID(),
                receiver.getGameProfile().getName(),
                receiver.blockPosition(),
                1.85F,
                pitch);
    }

    @Nullable
    private ServerPlayer resolveRaleReceiver(ServerLevel serverLevel) {
        if (this.getTarget() instanceof ServerPlayer targetPlayer
                && targetPlayer.isAlive()
                && !targetPlayer.isSpectator()
                && this.distanceToSqr(targetPlayer) <= 20.0D * 20.0D) {
            return targetPlayer;
        }
        Player nearest = serverLevel.getNearestPlayer(this, 28.0D);
        if (nearest instanceof ServerPlayer serverPlayer && serverPlayer.isAlive() && !serverPlayer.isSpectator()) {
            return serverPlayer;
        }
        return null;
    }

    private void debugRale(ServerLevel serverLevel, String message, Object... args) {
        if (serverLevel == null || serverLevel.getServer() == null) {
            return;
        }
        boolean enabled = UncannyConfig.DEBUG_LOGS.get()
                || UncannyWorldState.get(serverLevel.getServer()).isDebugLogsEnabled();
        if (!enabled) {
            return;
        }
        EchoOfTheVoid.LOGGER.info("[UncannyDebug/Rale] " + message, args);
    }

    private boolean isInDarkness(ServerLevel level) {
        return level.getRawBrightness(this.blockPosition(), 0) <= 7;
    }

    private void playBaitLureSound(ServerLevel level) {
        SoundEvent chosen = switch (this.random.nextInt(3)) {
            case 0 -> SoundEvents.CAT_AMBIENT;
            case 1 -> SoundEvents.WOLF_AMBIENT;
            default -> SoundEvents.VILLAGER_AMBIENT;
        };

        level.playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                chosen,
                SoundSource.HOSTILE,
                0.8F,
                0.88F + this.random.nextFloat() * 0.28F);
    }

    public enum ZombieVariant {
        UNASSIGNED(0),
        RALE_FICTIF(1),
        BROKEN_NECK(2),
        BAIT(3),
        DESYNC(4),
        RENDER_GLITCH(5),
        TALL_GLITCH_TEST(99);

        private final int id;

        ZombieVariant(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static ZombieVariant fromId(int id) {
            for (ZombieVariant variant : values()) {
                if (variant.id == id) {
                    return variant;
                }
            }
            return UNASSIGNED;
        }
    }
}

