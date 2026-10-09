package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.event.special.BlurRules;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Blur?: something that runs. It is only ever drawn at the edge of the player's screen (see
 * {@code UncannyBlurRenderer}); looked at directly there is nothing but trampled grass and running
 * steps. It keeps to the player's periphery, runs alongside when they sprint, sometimes rushes them
 * and veers off at the last moment, and whispers "psss" close by. Once per appearance it lands three
 * or four light blows (never fatal) and runs far away without a sound. Nothing can hit it; water,
 * rain, lava and fire make it sink into the ground.
 */
public class UncannyBlurEntity extends Monster implements UncannyEntityMarker {
    private static final EntityDataAccessor<Optional<UUID>> TARGET =
            SynchedEntityData.defineId(UncannyBlurEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final double RUN_SPEED = 1.55D;
    private static final double CHARGE_SPEED = 2.1D;

    /** Its few blows: light, never scaled by difficulty, never fatal (see {@link BlurRules}). */
    public static final ResourceKey<DamageType> STRIKE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("echoofthevoid", "blur_strike"));

    private enum Mode {
        PERIPHERY,
        PACE,
        CHARGE,
        PASS,
        /** Once per appearance: a short flurry of light blows from nowhere. */
        STRIKE,
        /** Then it runs far away without a sound and is gone. */
        FLEE
    }

    private Mode mode = Mode.PERIPHERY;
    private int lifetimeTicks = 20 * 90;
    private int strikeDelayTicks = 20 * 40;
    private int strikesLeft;
    private int strikesDealt;
    private int strikeCooldown;
    private int dodgeTicks;
    private int modeTicks;
    private int decisionTicks;
    private int nextChargeTicks = 20 * 30;
    private int nextWhisperTicks = 20 * 15;
    private int sinkTicks = -1;
    private Vec3 destination;
    private int side = 1;

    public UncannyBlurEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        UncannyEntityUtil.applyDisplayName(this, "Blur?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET, Optional.empty());
    }

    public void setup(ServerPlayer target, int lifetime) {
        this.entityData.set(TARGET, Optional.of(target.getUUID()));
        this.lifetimeTicks = lifetime;
        this.side = this.random.nextBoolean() ? 1 : -1;
        this.strikeDelayTicks = BlurRules.strikeDelayTicks(lifetime, this.random.nextDouble());
    }

    /** QA and GameTests: the flurry now, if the target is close enough. */
    public void strikeNow() {
        this.strikeDelayTicks = 0;
    }

    public int strikesDealt() {
        return this.strikesDealt;
    }

    public boolean isFleeing() {
        return this.mode == Mode.FLEE;
    }

    /** QA traces: current mode and strike state. */
    public String describeState() {
        return this.mode + " strikeDelay=" + this.strikeDelayTicks + " left=" + this.strikesLeft + " modeTicks=" + this.modeTicks;
    }

    public Optional<UUID> targetId() {
        return this.entityData.get(TARGET);
    }

    public boolean isSinking() {
        return this.sinkTicks >= 0;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel level) || this.isDeadOrDying()) {
            return;
        }
        if (isSinking()) {
            tickSink();
            return;
        }
        if (this.mode == Mode.FLEE) {
            tickFlee(level.getServer().getPlayerList().getPlayer(targetId().orElse(new UUID(0L, 0L))));
            return;
        }
        if (this.isInWaterRainOrBubble() || this.isInLava() || this.isOnFire()
                || this.level().getBlockStatesIfLoaded(this.getBoundingBox()).anyMatch(state -> state.is(BlockTags.FIRE))) {
            beginSink();
            return;
        }
        ServerPlayer target = targetId().map(id -> level.getServer().getPlayerList().getPlayer(id)).orElse(null);
        if (target == null || !target.isAlive() || target.level() != level || target.isSpectator()
                || --this.lifetimeTicks <= 0 || this.distanceToSqr(target) > 72.0D * 72.0D) {
            beginSink();
            return;
        }
        tickMovement(level, target);
        tickTraces(level);
        tickWhisper(target);
    }

    private void tickMovement(ServerLevel level, ServerPlayer target) {
        this.decisionTicks--;
        this.nextChargeTicks--;
        this.modeTicks++;
        double distance = Math.sqrt(this.distanceToSqr(target));
        if (this.mode == Mode.STRIKE) {
            tickStrike(level, target, distance);
            return;
        }
        if (--this.strikeDelayTicks <= 0 && distance <= BlurRules.STRIKE_START_DISTANCE
                && target.onGround() && !target.isInWater() && !target.getAbilities().invulnerable) {
            this.mode = Mode.STRIKE;
            this.modeTicks = 0;
            this.strikesLeft = BlurRules.strikeCount(this.random.nextDouble());
            this.strikeCooldown = 0;
            this.dodgeTicks = 0;
            return;
        }
        if (this.mode == Mode.CHARGE) {
            if (distance < 3.0D) {
                // Veers off at the last moment and runs past, never touching.
                Vec3 through = target.position().subtract(this.position()).normalize();
                Vec3 sideways = new Vec3(-through.z, 0.0D, through.x).scale(this.side * 4.0D);
                this.destination = target.position().add(through.scale(10.0D)).add(sideways);
                this.mode = Mode.PASS;
                this.decisionTicks = 30;
            } else {
                this.getNavigation().moveTo(target, CHARGE_SPEED);
            }
            return;
        }
        if (this.nextChargeTicks <= 0 && distance < 18.0D) {
            this.mode = Mode.CHARGE;
            this.nextChargeTicks = 20 * (25 + this.random.nextInt(25));
            return;
        }
        if (target.isSprinting()) {
            this.mode = Mode.PACE;
            Vec3 heading = target.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
            heading = heading.lengthSqr() < 1.0E-4D ? target.getLookAngle().multiply(1.0D, 0.0D, 1.0D) : heading;
            heading = heading.normalize();
            Vec3 sideways = new Vec3(-heading.z, 0.0D, heading.x).scale(this.side * Math.max(5.0D, distance * 0.7D));
            this.destination = target.position().add(heading.scale(6.0D)).add(sideways);
            this.getNavigation().moveTo(this.destination.x, this.destination.y, this.destination.z, CHARGE_SPEED);
            this.setSprinting(true);
            return;
        }
        if (this.mode == Mode.PACE) {
            this.mode = Mode.PERIPHERY;
            this.decisionTicks = 0;
        }
        if (this.decisionTicks <= 0 || this.destination == null
                || this.position().distanceToSqr(this.destination) < 2.0D) {
            this.mode = Mode.PERIPHERY;
            this.decisionTicks = 30 + this.random.nextInt(60);
            if (this.random.nextInt(4) == 0) {
                this.side = -this.side;
            }
            // Somewhere around the edge of the player's sight, at a restless distance.
            // Minecraft looks along (-sin yaw, cos yaw); turn that 55-105 degrees to one side.
            double angle = Math.toRadians(target.getYRot() + this.side * (55.0D + this.random.nextInt(50)));
            double radius = 8.0D + this.random.nextDouble() * 12.0D;
            this.destination = target.position().add(-Math.sin(angle) * radius, 0.0D, Math.cos(angle) * radius);
            if (this.random.nextInt(3) == 0) {
                this.getNavigation().stop();
                this.destination = this.position();
                return;
            }
        }
        this.setSprinting(true);
        this.getNavigation().moveTo(this.destination.x, this.destination.y, this.destination.z, RUN_SPEED);
    }

    /**
     * A few quick, light blows from something the player cannot quite see: it darts in, strikes, darts
     * aside, comes back. Never fatal, never more than a few hearts in all.
     */
    private void tickStrike(ServerLevel level, ServerPlayer target, double distance) {
        if (target.isInWater() || target.getAbilities().invulnerable || this.modeTicks > BlurRules.MAX_STRIKE_TICKS) {
            startFlee(target);
            return;
        }
        this.setSprinting(true);
        if (this.strikeCooldown > 0) {
            this.strikeCooldown--;
        }
        if (this.dodgeTicks > 0) {
            this.dodgeTicks--;
            this.getNavigation().moveTo(this.destination.x, this.destination.y, this.destination.z, CHARGE_SPEED);
            return;
        }
        this.getNavigation().moveTo(target, CHARGE_SPEED);
        if (distance > BlurRules.STRIKE_REACH || this.strikeCooldown > 0) {
            return;
        }
        float amount = BlurRules.strikeDamage(target.getHealth());
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (amount > 0.0F) {
            target.hurt(new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(STRIKE), this), amount);
        }
        this.strikesDealt++;
        if (--this.strikesLeft <= 0) {
            startFlee(target);
            return;
        }
        this.strikeCooldown = BlurRules.MIN_STRIKE_GAP_TICKS + this.random.nextInt(8);
        this.dodgeTicks = 8 + this.random.nextInt(6);
        Vec3 away = this.position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
        away = away.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : away.normalize();
        if (this.random.nextBoolean()) {
            this.side = -this.side;
        }
        Vec3 sideways = new Vec3(-away.z, 0.0D, away.x).scale(this.side * 3.5D);
        this.destination = target.position().add(away.scale(2.5D)).add(sideways);
    }

    /** Off at full speed, straight away from the player, without a sound; gone once far enough. */
    private void startFlee(ServerPlayer target) {
        this.mode = Mode.FLEE;
        this.modeTicks = 0;
        Vec3 away = this.position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
        away = away.lengthSqr() < 1.0E-4D ? target.getLookAngle().multiply(-1.0D, 0.0D, -1.0D) : away;
        this.destination = this.position().add(away.normalize().scale(BlurRules.FLEE_DISTANCE + 16.0D));
        this.getNavigation().stop();
    }

    private void tickFlee(ServerPlayer target) {
        this.modeTicks++;
        if (target == null || target.level() != this.level() || this.isInWater()
                || this.distanceToSqr(target) > BlurRules.FLEE_DISTANCE * BlurRules.FLEE_DISTANCE
                || this.modeTicks > BlurRules.MAX_FLEE_TICKS) {
            // Simply gone: no sinking, no smoke, no sound.
            this.discard();
            return;
        }
        this.setSprinting(true);
        if (this.getNavigation().isDone() || this.modeTicks % 20 == 0) {
            this.getNavigation().moveTo(this.destination.x, this.destination.y, this.destination.z, CHARGE_SPEED);
        }
    }

    /** What the player can see head-on: grass and flowers kicked up, running steps. */
    private void tickTraces(ServerLevel level) {
        if (!this.onGround() || this.getDeltaMovement().horizontalDistanceSqr() < 0.004D || this.tickCount % 3 != 0) {
            return;
        }
        BlockPos feet = this.blockPosition();
        BlockState plant = level.getBlockState(feet);
        BlockState ground = level.getBlockState(feet.below());
        BlockState kicked = plant.is(BlockTags.REPLACEABLE_BY_TREES) || plant.is(BlockTags.FLOWERS) ? plant : ground;
        if (!kicked.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, kicked),
                    this.getX(), this.getY() + 0.15D, this.getZ(), 5, 0.25D, 0.08D, 0.25D, 0.08D);
        }
        if (this.tickCount % 6 == 0 && !ground.isAir()) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ground.getSoundType().getStepSound(),
                    SoundSource.HOSTILE, 0.45F, 1.15F + this.random.nextFloat() * 0.2F);
        }
    }

    private void tickWhisper(ServerPlayer target) {
        if (--this.nextWhisperTicks > 0) {
            return;
        }
        double distance = Math.sqrt(this.distanceToSqr(target));
        Vec3 look = target.getLookAngle();
        Vec3 to = this.position().subtract(target.position()).normalize();
        if (distance > 3.0D && distance < 10.0D && look.dot(to) < 0.3D) {
            target.connection.send(new ClientboundSoundPacket(Holder.direct(UncannySoundRegistry.UNCANNY_PSSS.get()),
                    SoundSource.HOSTILE, this.getX(), this.getEyeY(), this.getZ(), 0.55F,
                    0.92F + this.random.nextFloat() * 0.15F, this.random.nextLong()));
            this.nextWhisperTicks = 20 * (18 + this.random.nextInt(25));
        } else {
            this.nextWhisperTicks = 20;
        }
    }

    private void beginSink() {
        if (isSinking()) {
            return;
        }
        this.sinkTicks = 0;
        this.getNavigation().stop();
    }

    private void tickSink() {
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(this.getX(), this.getY() - UncannySinkTransition.step(this, 0.09D, 30), this.getZ());
        if (++this.sinkTicks >= 30 || UncannySinkTransition.breaksIntoOpenSpace(this)) {
            UncannySinkTransition.vanish(this);
        }
    }

    // ------------------------------------------------------------------ untouchable

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_DROWNING)) {
            beginSink();
        }
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** It runs past and never touches: brushing a player must not shove them either. */
    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
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

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        targetId().ifPresent(id -> tag.putUUID("BlurTarget", id));
        tag.putInt("Lifetime", this.lifetimeTicks);
        tag.putInt("SinkTicks", this.sinkTicks);
        tag.putInt("StrikeDelay", this.strikeDelayTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("BlurTarget")) {
            this.entityData.set(TARGET, Optional.of(tag.getUUID("BlurTarget")));
        }
        this.lifetimeTicks = tag.contains("Lifetime") ? tag.getInt("Lifetime") : this.lifetimeTicks;
        this.sinkTicks = tag.contains("SinkTicks") ? tag.getInt("SinkTicks") : -1;
        this.strikeDelayTicks = tag.contains("StrikeDelay") ? tag.getInt("StrikeDelay") : this.strikeDelayTicks;
    }
}
