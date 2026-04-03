package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.phase.UncannyPhase;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class UncannyEndermanEntity extends EnderMan implements UncannyEntityMarker {
    private static final EntityDataAccessor<Integer> ENDERMAN_VARIANT =
            SynchedEntityData.defineId(UncannyEndermanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> LATENT_TARGET =
            SynchedEntityData.defineId(UncannyEndermanEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private int lightThiefCooldown;
    private int erraticTeleportCooldown;
    private long erraticStartTick;
    private boolean erraticAggroUnlocked;
    private boolean latentHidden;
    private long latentReturnEarliestTick;
    private long latentReturnLatestTick;
    private boolean suppressRandomTeleport;
    private int soundOffsetTeleportCooldown;

    public UncannyEndermanEntity(EntityType<? extends EnderMan> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Enderman?");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ENDERMAN_VARIANT, EndermanVariant.UNASSIGNED.id());
        builder.define(LATENT_TARGET, Optional.empty());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        EndermanVariant variant = getEndermanVariant();
        if (variant == EndermanVariant.UNASSIGNED) {
            ensureVariantAssigned(serverLevel);
            variant = getEndermanVariant();
        }

        switch (variant) {
            case SOUND_OFFSET -> tickSoundOffset(serverLevel);
            case LIGHT_THIEF -> tickLightThief(serverLevel);
            case ERRATIC_STRAY -> tickErraticStray(serverLevel);
            case LATENT_THREAT -> tickLatentThreat(serverLevel);
            case SPATIAL_ANOMALY -> tickSpatialAnomaly();
            case UNASSIGNED -> {
            }
        }
    }

    @Override
    protected boolean teleport() {
        EndermanVariant variant = getEndermanVariant();
        if (variant == EndermanVariant.SPATIAL_ANOMALY && this.suppressRandomTeleport) {
            return false;
        }

        if (variant != EndermanVariant.SOUND_OFFSET) {
            return super.teleport();
        }

        Vec3 before = this.position();
        boolean wasSilent = this.isSilent();
        this.setSilent(true);
        boolean success = super.teleport();
        this.setSilent(wasSilent);
        if (success && this.level() instanceof ServerLevel serverLevel) {
            playOffsetTeleportSound(serverLevel, before, this.position());
        }
        return success;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        EndermanVariant variant = getEndermanVariant();

        if (variant == EndermanVariant.LATENT_THREAT && this.latentHidden) {
            return false;
        }

        if (variant == EndermanVariant.SPATIAL_ANOMALY && source.getEntity() instanceof ServerPlayer player) {
            this.suppressRandomTeleport = true;
            boolean hurt = super.hurt(source, amount);
            this.suppressRandomTeleport = false;
            if (hurt && this.isAlive()) {
                swapPositionWith(player);
            }
            return hurt;
        }

        boolean hurt = super.hurt(source, amount);
        if (!hurt) {
            return false;
        }

        if (variant == EndermanVariant.ERRATIC_STRAY && !this.erraticAggroUnlocked) {
            this.erraticAggroUnlocked = true;
            if (source.getEntity() instanceof LivingEntity attacker) {
                this.setTarget(attacker);
            }
        }
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (this.latentHidden) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("UncannyEndermanVariant", getEndermanVariant().id());
        this.entityData.get(LATENT_TARGET).ifPresent(uuid -> tag.putUUID("UncannyLatentTarget", uuid));
        tag.putInt("UncannyLightThiefCooldown", this.lightThiefCooldown);
        tag.putInt("UncannyErraticTeleportCooldown", this.erraticTeleportCooldown);
        tag.putLong("UncannyErraticStartTick", this.erraticStartTick);
        tag.putBoolean("UncannyErraticAggroUnlocked", this.erraticAggroUnlocked);
        tag.putBoolean("UncannyLatentHidden", this.latentHidden);
        tag.putLong("UncannyLatentReturnEarliest", this.latentReturnEarliestTick);
        tag.putLong("UncannyLatentReturnLatest", this.latentReturnLatestTick);
        tag.putInt("UncannySoundOffsetTpCooldown", this.soundOffsetTeleportCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setEndermanVariant(EndermanVariant.fromId(tag.getInt("UncannyEndermanVariant")));
        if (tag.hasUUID("UncannyLatentTarget")) {
            this.entityData.set(LATENT_TARGET, Optional.of(tag.getUUID("UncannyLatentTarget")));
        } else {
            this.entityData.set(LATENT_TARGET, Optional.empty());
        }
        this.lightThiefCooldown = Math.max(0, tag.getInt("UncannyLightThiefCooldown"));
        this.erraticTeleportCooldown = Math.max(0, tag.getInt("UncannyErraticTeleportCooldown"));
        this.erraticStartTick = tag.getLong("UncannyErraticStartTick");
        this.erraticAggroUnlocked = tag.getBoolean("UncannyErraticAggroUnlocked");
        this.latentHidden = tag.getBoolean("UncannyLatentHidden");
        this.latentReturnEarliestTick = tag.getLong("UncannyLatentReturnEarliest");
        this.latentReturnLatestTick = tag.getLong("UncannyLatentReturnLatest");
        this.soundOffsetTeleportCooldown = Math.max(0, tag.getInt("UncannySoundOffsetTpCooldown"));
    }

    public void forceTrackerSequence(ServerPlayer player) {
        setEndermanVariant(EndermanVariant.ERRATIC_STRAY);
        this.erraticAggroUnlocked = true;
        this.setTarget(player);
    }

    public EndermanVariant getEndermanVariant() {
        return EndermanVariant.fromId(this.entityData.get(ENDERMAN_VARIANT));
    }

    private void setEndermanVariant(EndermanVariant variant) {
        this.entityData.set(ENDERMAN_VARIANT, variant.id());
    }

    private void ensureVariantAssigned(ServerLevel serverLevel) {
        if (getEndermanVariant() != EndermanVariant.UNASSIGNED) {
            return;
        }
        UncannyPhase phase = UncannyWorldState.get(serverLevel.getServer()).getPhase();
        setEndermanVariant(rollVariantForPhase(phase));
    }

    private EndermanVariant rollVariantForPhase(UncannyPhase phase) {
        int roll = this.random.nextInt(100);
        return switch (phase) {
            case PHASE_1 -> EndermanVariant.SOUND_OFFSET;
            case PHASE_2 -> roll < 70 ? EndermanVariant.LIGHT_THIEF : EndermanVariant.SOUND_OFFSET;
            case PHASE_3 -> {
                if (roll < 62) {
                    yield EndermanVariant.ERRATIC_STRAY;
                }
                if (roll < 90) {
                    yield EndermanVariant.LIGHT_THIEF;
                }
                yield EndermanVariant.SOUND_OFFSET;
            }
            case PHASE_4 -> {
                if (roll < 38) {
                    yield EndermanVariant.LATENT_THREAT;
                }
                if (roll < 66) {
                    yield EndermanVariant.SPATIAL_ANOMALY;
                }
                if (roll < 88) {
                    yield EndermanVariant.ERRATIC_STRAY;
                }
                yield EndermanVariant.LIGHT_THIEF;
            }
        };
    }

    private void tickSoundOffset(ServerLevel serverLevel) {
        this.latentHidden = false;
        this.erraticAggroUnlocked = false;
        this.erraticStartTick = 0L;
        this.setInvisible(false);
        this.setNoGravity(false);

        if (this.soundOffsetTeleportCooldown > 0) {
            this.soundOffsetTeleportCooldown--;
            return;
        }

        Player nearest = serverLevel.getNearestPlayer(this, 22.0D);
        if (nearest == null) {
            return;
        }

        Vec3 before = this.position();
        if (teleportToward(nearest)) {
            playOffsetTeleportSound(serverLevel, before, this.position());
            this.soundOffsetTeleportCooldown = 30 + this.random.nextInt(21);
        } else {
            Vec3 pseudoFrom = nearest.position();
            Vec3 pseudoTo = nearest.position().add(nearest.getLookAngle().normalize().scale(2.0D));
            playOffsetTeleportSound(serverLevel, pseudoFrom, pseudoTo);
            this.soundOffsetTeleportCooldown = 20;
        }
    }

    private void tickLightThief(ServerLevel serverLevel) {
        this.latentHidden = false;
        this.erraticAggroUnlocked = false;
        this.erraticStartTick = 0L;
        this.setInvisible(false);
        this.setNoGravity(false);

        if (this.lightThiefCooldown > 0) {
            this.lightThiefCooldown--;
            return;
        }
        this.lightThiefCooldown = 30 + this.random.nextInt(21);

        BlockPos lightPos = findStealableLight(serverLevel, this.blockPosition(), 18);
        if (lightPos == null) {
            return;
        }

        teleportNear(serverLevel, lightPos);
        BlockState state = serverLevel.getBlockState(lightPos);
        if (!state.isAir()) {
            serverLevel.destroyBlock(lightPos, true, this);
        }
    }

    private void tickErraticStray(ServerLevel serverLevel) {
        this.latentHidden = false;
        this.setInvisible(false);
        this.setNoGravity(false);

        if (this.erraticAggroUnlocked) {
            if (this.getTarget() == null) {
                Player nearest = serverLevel.getNearestPlayer(this, 28.0D);
                if (nearest instanceof LivingEntity living) {
                    this.setTarget(living);
                }
            }
            return;
        }

        long now = serverLevel.getServer().getTickCount();
        if (this.erraticStartTick == 0L) {
            this.erraticStartTick = now;
        }

        Player nearest = serverLevel.getNearestPlayer(this, 28.0D);
        if (nearest != null) {
            this.setTarget(null);
            this.getNavigation().stop();

            if (this.erraticTeleportCooldown > 0) {
                this.erraticTeleportCooldown--;
            } else {
                this.erraticTeleportCooldown = 20 + this.random.nextInt(21);
                franticTeleportAround(serverLevel, nearest, now - this.erraticStartTick);
            }
        }

        if (now - this.erraticStartTick >= 20L * 30L) {
            this.erraticAggroUnlocked = true;
            if (nearest instanceof LivingEntity living) {
                this.setTarget(living);
            }
        }
    }

    private void tickLatentThreat(ServerLevel serverLevel) {
        long now = serverLevel.getServer().getTickCount();
        ServerPlayer targetPlayer = resolveLatentTarget(serverLevel);

        if (!this.latentHidden) {
            ServerPlayer watcher = targetPlayer != null ? targetPlayer : resolveDirectWatcher(serverLevel);
            if (watcher != null && isDirectlyWatchedBy(watcher)) {
                this.latentHidden = true;
                this.entityData.set(LATENT_TARGET, Optional.of(watcher.getUUID()));
                this.latentReturnEarliestTick = now + (30L * 20L);
                this.latentReturnLatestTick = now + (60L * 20L);
                this.setInvisible(true);
                this.setNoGravity(true);
                this.getNavigation().stop();
                this.setTarget(null);
            }
            return;
        }

        this.setInvisible(true);
        this.setNoGravity(true);
        this.getNavigation().stop();
        this.setTarget(null);

        if (targetPlayer == null || !targetPlayer.isAlive()) {
            return;
        }

        if (now < this.latentReturnEarliestTick) {
            return;
        }

        boolean guiOpened = targetPlayer.containerMenu != targetPlayer.inventoryMenu;
        if (!guiOpened && now < this.latentReturnLatestTick) {
            return;
        }

        this.latentHidden = false;
        this.setInvisible(false);
        this.setNoGravity(false);
        teleportInFrontOf(targetPlayer);
        targetPlayer.closeContainer();
        serverLevel.playSound(null, targetPlayer.blockPosition(), SoundEvents.GLASS_BREAK, this.getSoundSource(), 3.4F, 0.65F);
        this.setTarget(targetPlayer);
    }

    private void tickSpatialAnomaly() {
        this.latentHidden = false;
        this.setInvisible(false);
        this.setNoGravity(false);
    }

    private void playOffsetTeleportSound(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 direction = to.subtract(from);
        if (direction.lengthSqr() < 0.0001D) {
            direction = new Vec3(this.random.nextDouble() - 0.5D, 0.0D, this.random.nextDouble() - 0.5D);
        }
        direction = direction.normalize();
        Vec3 soundPos = to.subtract(direction.scale(15.0D));
        level.playSound(null, soundPos.x, soundPos.y, soundPos.z, SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 2.1F, 0.86F);
    }

    private boolean teleportToward(Player player) {
        Vec3 toPlayer = player.position().subtract(this.position());
        Vec3 horizontal = new Vec3(toPlayer.x, 0.0D, toPlayer.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            return false;
        }
        horizontal = horizontal.normalize();
        Vec3 target = player.position().subtract(horizontal.scale(4.0D + this.random.nextDouble() * 2.0D));
        return this.randomTeleport(target.x, player.getY(), target.z, false);
    }

    private void franticTeleportAround(ServerLevel level, Player player, long elapsedTicks) {
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = this.random.nextDouble() * (Math.PI * 2.0D);
            double distance = 4.0D + this.random.nextDouble() * 7.0D;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;
            double y = player.getY() + this.random.nextInt(5) - 2;
            if (!this.randomTeleport(x, y, z, true)) {
                continue;
            }

            float progress = Mth.clamp(elapsedTicks / (20.0F * 30.0F), 0.0F, 1.0F);
            float pitch = 1.35F - progress * 0.85F;
            level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 1.2F, pitch);
            return;
        }
    }

    private void swapPositionWith(ServerPlayer player) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        double ex = this.getX();
        double ey = this.getY();
        double ez = this.getZ();

        player.teleportTo(ex, ey, ez);
        this.teleportTo(px, py, pz);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, this.getSoundSource(), 3.1F, 0.75F);
        }
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20, 0, false, false, true));
    }

    private void teleportNear(ServerLevel level, BlockPos blockPos) {
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = blockPos.getX() + this.random.nextInt(5) - 2;
            int z = blockPos.getZ() + this.random.nextInt(5) - 2;
            int y = blockPos.getY() + this.random.nextInt(3) - 1;
            if (this.randomTeleport(x + 0.5D, y, z + 0.5D, true)) {
                return;
            }
        }
    }

    private void teleportInFrontOf(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontal = horizontal.normalize();
        }
        Vec3 targetPos = player.position().add(horizontal.scale(1.7D));
        this.teleportTo(targetPos.x, player.getY(), targetPos.z);
    }

    @Nullable
    private BlockPos findStealableLight(ServerLevel level, BlockPos center, int radius) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -6; dy <= 6; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (!isStealableLight(state)) {
                        continue;
                    }
                    double dist = pos.distSqr(center);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = pos.immutable();
                    }
                }
            }
        }
        return best;
    }

    private boolean isStealableLight(BlockState state) {
        return state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH)
                || state.is(Blocks.SOUL_WALL_TORCH)
                || state.is(Blocks.LANTERN)
                || state.is(Blocks.SOUL_LANTERN);
    }

    @Nullable
    private ServerPlayer resolveDirectWatcher(ServerLevel level) {
        Player nearest = level.getNearestPlayer(this, 32.0D);
        return nearest instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    @Nullable
    private ServerPlayer resolveLatentTarget(ServerLevel level) {
        return this.entityData.get(LATENT_TARGET)
                .map(uuid -> level.getServer().getPlayerList().getPlayer(uuid))
                .orElse(null);
    }

    private boolean isDirectlyWatchedBy(ServerPlayer player) {
        if (!this.hasLineOfSight(player) || !player.hasLineOfSight(this)) {
            return false;
        }
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 toEnderman = this.position().subtract(player.getEyePosition()).normalize();
        return look.dot(toEnderman) > 0.955D;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        UncannyEntityUtil.suppressStepSound(this, pos, blockState);
    }

    public enum EndermanVariant {
        UNASSIGNED(0),
        SOUND_OFFSET(1),
        LIGHT_THIEF(2),
        ERRATIC_STRAY(3),
        LATENT_THREAT(4),
        SPATIAL_ANOMALY(5);

        private final int id;

        EndermanVariant(int id) {
            this.id = id;
        }

        public int id() {
            return this.id;
        }

        public static EndermanVariant fromId(int id) {
            for (EndermanVariant variant : values()) {
                if (variant.id == id) {
                    return variant;
                }
            }
            return UNASSIGNED;
        }
    }
}
