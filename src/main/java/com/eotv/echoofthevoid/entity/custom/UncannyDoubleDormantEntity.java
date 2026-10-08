package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.diagnostics.DiagnosticSeverity;
import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import com.eotv.echoofthevoid.event.special.AdaptiveSpecialEquipment;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class UncannyDoubleDormantEntity extends Zombie implements UncannyEntityMarker {
    private static final EntityDataAccessor<Optional<UUID>> COPIED_TARGET_UUID =
            SynchedEntityData.defineId(UncannyDoubleDormantEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final int EYE_CONTACT_ATTACK_DELAY_TICKS = 20 * 10;

    private BlockPos baseCenter = BlockPos.ZERO;
    private BlockPos bedPos = BlockPos.ZERO;
    private long wakeUpAtGameTime = -1L;
    private boolean awakened;

    public UncannyDoubleDormantEntity(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 18;
        UncannyEntityUtil.applyDisplayName(this, "Mimic");
        disableEquipmentDrops();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COPIED_TARGET_UUID, Optional.empty());
    }

    public void copyTarget(ServerPlayer player, BlockPos baseCenter, BlockPos bedPos) {
        this.entityData.set(COPIED_TARGET_UUID, Optional.of(player.getUUID()));
        this.baseCenter = baseCenter.immutable();
        this.bedPos = bedPos.immutable();
        this.awakened = false;
        this.wakeUpAtGameTime = -1L;
        this.setTarget(null);
        this.copyInventoryLoadout(player);
        this.startSleeping(this.bedPos);
    }

    public Optional<UUID> getCopiedTargetUuid() {
        return this.entityData.get(COPIED_TARGET_UUID);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.enableDoorNavigation(this);

        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ServerPlayer copiedPlayer = resolveCopiedPlayer(serverLevel);
        if (copiedPlayer == null || !copiedPlayer.isAlive()) {
            this.discard();
            return;
        }

        double allowedDistance = UncannyConfig.DOUBLE_DORMANT_ESCAPE_DISTANCE_BLOCKS.get();
        if (copiedPlayer.blockPosition().distSqr(baseCenter) > allowedDistance * allowedDistance) {
            this.discard();
            return;
        }

        if (!awakened) {
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);

            if (!this.isSleeping()) {
                this.startSleeping(this.bedPos);
            }

            if (hasDirectEyeContact(copiedPlayer) && this.wakeUpAtGameTime < 0L) {
                this.wakeUpAtGameTime = serverLevel.getGameTime() + EYE_CONTACT_ATTACK_DELAY_TICKS;
            }

            if (this.wakeUpAtGameTime >= 0L && serverLevel.getGameTime() >= this.wakeUpAtGameTime) {
                this.level().playSound(null, this.blockPosition(), SoundEvents.GHAST_HURT, this.getSoundSource(), 1.6F, 0.9F);
                this.stopSleeping();
                this.awakened = true;
                this.setTarget(copiedPlayer);
            }
            return;
        }

        this.setTarget(copiedPlayer);
    }

    @Override
    public boolean isAggressive() {
        return this.awakened;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.getCopiedTargetUuid().ifPresent(uuid -> tag.putUUID("CopiedTarget", uuid));
        tag.putLong("WakeUpAt", wakeUpAtGameTime);
        tag.putBoolean("Awakened", awakened);
        tag.putLong("BaseX", baseCenter.getX());
        tag.putLong("BaseY", baseCenter.getY());
        tag.putLong("BaseZ", baseCenter.getZ());
        tag.putLong("BedX", bedPos.getX());
        tag.putLong("BedY", bedPos.getY());
        tag.putLong("BedZ", bedPos.getZ());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("CopiedTarget")) {
            this.entityData.set(COPIED_TARGET_UUID, Optional.of(tag.getUUID("CopiedTarget")));
        }
        this.wakeUpAtGameTime = tag.getLong("WakeUpAt");
        this.awakened = tag.getBoolean("Awakened");
        this.baseCenter = new BlockPos((int) tag.getLong("BaseX"), (int) tag.getLong("BaseY"), (int) tag.getLong("BaseZ"));
        this.bedPos = new BlockPos((int) tag.getLong("BedX"), (int) tag.getLong("BedY"), (int) tag.getLong("BedZ"));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GHAST_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        UncannyEntityUtil.suppressStepSound(this, pos, blockState);
    }

    private ServerPlayer resolveCopiedPlayer(ServerLevel level) {
        Optional<UUID> targetUuid = this.getCopiedTargetUuid();
        ServerPlayer copied = targetUuid
                .map(uuid -> level.getServer().getPlayerList().getPlayer(uuid))
                .orElse(null);
        return copied != null
                        && copied.isAlive()
                        && !copied.isSpectator()
                        && copied.serverLevel() == level
                ? copied
                : null;
    }

    private boolean hasDirectEyeContact(ServerPlayer player) {
        if (!this.hasLineOfSight(player) || !player.hasLineOfSight(this)) {
            return false;
        }

        Vec3 playerLook = player.getViewVector(1.0F).normalize();
        Vec3 fromPlayerToMimic = this.position().subtract(player.getEyePosition()).normalize();
        return playerLook.dot(fromPlayerToMimic) > 0.93D;
    }

    private void copyInventoryLoadout(ServerPlayer player) {
        AdaptiveSpecialEquipment.Snapshot loadout = AdaptiveSpecialEquipment.select(player);
        this.setItemSlot(EquipmentSlot.MAINHAND, loadout.weapon());
        this.setItemSlot(EquipmentSlot.OFFHAND, loadout.shield());
        this.setItemSlot(EquipmentSlot.HEAD, loadout.armor(EquipmentSlot.HEAD));
        this.setItemSlot(EquipmentSlot.CHEST, loadout.armor(EquipmentSlot.CHEST));
        this.setItemSlot(EquipmentSlot.LEGS, loadout.armor(EquipmentSlot.LEGS));
        this.setItemSlot(EquipmentSlot.FEET, loadout.armor(EquipmentSlot.FEET));
        disableEquipmentDrops();
        UncannyDiagnostics.recordForPlayer(
                player,
                DiagnosticSeverity.INFO,
                "special",
                "adaptive_loadout_selected",
                UncannyDiagnostics.fields(
                        "special", "mimic",
                        "weapon", loadout.weapon().isEmpty() ? "none" : loadout.weapon().getItem().toString(),
                        "attack_damage", loadout.attackDamage(),
                        "armor", loadout.armorValue(),
                        "armor_toughness", loadout.armorToughness()));
    }

    private void disableEquipmentDrops() {
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        this.setDropChance(EquipmentSlot.LEGS, 0.0F);
        this.setDropChance(EquipmentSlot.FEET, 0.0F);
    }
}
