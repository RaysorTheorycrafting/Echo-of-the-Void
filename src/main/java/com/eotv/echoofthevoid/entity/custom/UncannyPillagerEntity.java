package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class UncannyPillagerEntity extends Pillager implements UncannyEntityMarker {
    private enum CombatState {
        STARE,
        BURST
    }

    private CombatState combatState = CombatState.STARE;
    private int stateTicks = 60;
    private int burstShots;
    private int shotCooldown;
    private int burstRecoveryTicks;

    public UncannyPillagerEntity(EntityType<? extends Pillager> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Pillager?");
    }

    @Override
    public void aiStep() {
        super.aiStep();
        UncannyEntityUtil.forceSilent(this);

        if (this.level().isClientSide()) {
            return;
        }

        if (this.burstRecoveryTicks > 0) {
            this.burstRecoveryTicks--;
        }

        Player player = this.level().getNearestPlayer(this, 32.0D);
        if (player == null || !player.isAlive()) {
            this.setTarget(null);
            this.combatState = CombatState.STARE;
            this.stateTicks = randomStareTicks();
            return;
        }

        this.lookAt(player, 60.0F, 30.0F);

        if (combatState == CombatState.STARE) {
            this.setTarget(null);
            holdDistance(player);
            if (this.burstRecoveryTicks <= 0 && (this.stateTicks-- <= 0 || this.distanceToSqr(player) < 144.0D)) {
                this.combatState = CombatState.BURST;
                this.burstShots = 1 + this.random.nextInt(2);
                this.shotCooldown = 10 + this.random.nextInt(7);
                this.stateTicks = 48 + this.random.nextInt(33);
            }
            return;
        }

        this.setTarget(player);
        retreatFrom(player);

        if (this.shotCooldown > 0) {
            this.shotCooldown--;
        }

        if (this.burstShots > 0 && this.shotCooldown <= 0 && this.hasLineOfSight(player)) {
            fireBoltAt(player);
            this.burstShots--;
            this.shotCooldown = 16 + this.random.nextInt(9);
        }

        if (this.stateTicks-- <= 0 && this.burstShots <= 0) {
            this.combatState = CombatState.STARE;
            this.stateTicks = randomStareTicks();
            this.burstRecoveryTicks = 70 + this.random.nextInt(61);
            this.setTarget(null);
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        UncannyEntityUtil.suppressStepSound(this, pos, blockState);
    }

    private int randomStareTicks() {
        return 70 + this.random.nextInt(71);
    }

    private void holdDistance(Player player) {
        double distanceSq = this.distanceToSqr(player);
        if (distanceSq > 25.0D * 25.0D) {
            this.getNavigation().moveTo(player, 1.0D);
            return;
        }

        if (distanceSq < 15.0D * 15.0D) {
            retreatFrom(player);
            return;
        }

        this.getNavigation().stop();
    }

    private void retreatFrom(Player player) {
        Vec3 away = this.position().subtract(player.position());
        if (away.lengthSqr() < 0.001D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            away = away.normalize();
        }

        Vec3 destination = this.position().add(away.scale(8.0D));
        this.getNavigation().moveTo(destination.x, destination.y, destination.z, 0.95D);
    }

    private void fireBoltAt(Player player) {
        ItemStack weapon = this.getMainHandItem().isEmpty() ? new ItemStack(Items.CROSSBOW) : this.getMainHandItem().copy();
        Arrow arrow = new Arrow(this.level(), this, new ItemStack(Items.ARROW), weapon);
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double dy = player.getY(0.3333333333333333D) - arrow.getY() + horizontal * 0.18D;
        arrow.shoot(dx, dy, dz, 2.0F, 10.0F);
        arrow.setBaseDamage(4.0D);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        this.level().addFreshEntity(arrow);
    }
}
