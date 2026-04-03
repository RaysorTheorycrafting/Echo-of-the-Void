package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class UncannyPiglinBruteEntity extends PiglinBrute implements UncannyEntityMarker {
    private int forcedAggroTicks;

    public UncannyPiglinBruteEntity(EntityType<? extends PiglinBrute> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Piglin Brute?");
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);

        if (this.level().isClientSide()) {
            return;
        }

        if (forcedAggroTicks > 0) {
            forcedAggroTicks--;
        }

        if (this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
        }

        Player nearest = this.level().getNearestPlayer(this, 20.0D);
        if (nearest == null || !nearest.isAlive()) {
            return;
        }

        boolean playerLookingAtBrute = isPlayerLookingAtMe(nearest);
        if (forcedAggroTicks <= 0 && playerLookingAtBrute) {
            this.getNavigation().stop();
            this.setTarget(null);
            this.setAggressive(false);
            this.setDeltaMovement(new Vec3(0.0D, this.getDeltaMovement().y * 0.1D, 0.0D));
            return;
        }

        this.setAggressive(true);
        this.setTarget(nearest);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (!hurt) {
            return false;
        }

        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity livingEntity) {
            this.setTarget(livingEntity);
            this.forcedAggroTicks = 80;
        }
        return true;
    }

    private boolean isPlayerLookingAtMe(Player player) {
        if (!this.hasLineOfSight(player) || !player.hasLineOfSight(this)) {
            return false;
        }

        Vec3 playerLook = player.getViewVector(1.0F).normalize();
        Vec3 toBrute = this.position().subtract(player.getEyePosition()).normalize();
        return playerLook.dot(toBrute) > 0.90D;
    }
}
