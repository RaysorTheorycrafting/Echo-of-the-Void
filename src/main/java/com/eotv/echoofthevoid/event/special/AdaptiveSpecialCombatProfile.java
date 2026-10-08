package com.eotv.echoofthevoid.event.special;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Immutable combat values shared by the few Specials whose concept deliberately adapts to a
 * player's equipment. The arithmetic lives here so Attacker?, Miner? and arena pursuers cannot
 * silently drift apart.
 */
public record AdaptiveSpecialCombatProfile(
        double maxHealth,
        double attackDamage,
        double movementSpeed,
        double armor,
        double armorToughness) {
    public static AdaptiveSpecialCombatProfile attacker(ServerPlayer player) {
        AdaptiveSpecialEquipment.Snapshot loadout = AdaptiveSpecialEquipment.select(player);
        return attacker(
                player.getMaxHealth(),
                player.getAttributeValue(Attributes.MOVEMENT_SPEED),
                loadout.attackDamage(),
                loadout.armorValue(),
                loadout.armorToughness());
    }

    public static AdaptiveSpecialCombatProfile attacker(
            double playerMaxHealth,
            double playerMovementSpeed,
            double strongestAttackDamage,
            double bestArmor,
            double bestArmorToughness) {
        return new AdaptiveSpecialCombatProfile(
                Math.max(20.0D, playerMaxHealth * 0.9D),
                Math.min(12.0D, Math.max(4.0D, strongestAttackDamage)),
                Math.min(0.50D, Math.max(0.34D, playerMovementSpeed * 1.35D)),
                Math.min(20.0D, Math.max(0.0D, bestArmor)),
                Math.min(12.0D, Math.max(0.0D, bestArmorToughness)));
    }

    public AdaptiveSpecialCombatProfile withMovementSpeed(double speed) {
        return new AdaptiveSpecialCombatProfile(
                maxHealth, attackDamage, Math.max(0.0D, speed), armor, armorToughness);
    }

    public void applyTo(LivingEntity entity, boolean refillHealth) {
        setBaseValue(entity, Attributes.MAX_HEALTH, maxHealth);
        setBaseValue(entity, Attributes.ATTACK_DAMAGE, attackDamage);
        setBaseValue(entity, Attributes.MOVEMENT_SPEED, movementSpeed);
        setBaseValue(entity, Attributes.ARMOR, armor);
        setBaseValue(entity, Attributes.ARMOR_TOUGHNESS, armorToughness);
        if (refillHealth) {
            entity.setHealth((float) maxHealth);
        } else {
            entity.setHealth(Math.min(entity.getHealth(), (float) maxHealth));
        }
    }

    private static void setBaseValue(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }
}
