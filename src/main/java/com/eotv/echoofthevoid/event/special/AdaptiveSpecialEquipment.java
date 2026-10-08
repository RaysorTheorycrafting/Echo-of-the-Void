package com.eotv.echoofthevoid.event.special;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/**
 * Selects the strongest loadout that an adaptive Special could actually use.
 *
 * <p>The scan includes inventory, equipped armor and offhand stacks. Attribute evaluation uses
 * the modifiers exposed by each stack, so compatible modded equipment does not need to extend a
 * hard-coded Vanilla item class. Conditional enchantment damage is deliberately not guessed: it
 * depends on the eventual target and is not a single comparable attack value.</p>
 */
public final class AdaptiveSpecialEquipment {
    private static final double EPSILON = 1.0E-6D;
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private AdaptiveSpecialEquipment() {
    }

    public static Snapshot select(ServerPlayer player) {
        double baseAttackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE) == null
                ? 1.0D
                : player.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
        List<ItemStack> stacks = allCarriedStacks(player);

        ItemStack bestWeapon = ItemStack.EMPTY;
        double bestAttackDamage = baseAttackDamage;
        ItemStack bestShield = ItemStack.EMPTY;
        Map<EquipmentSlot, ItemStack> bestArmor = new EnumMap<>(EquipmentSlot.class);
        Map<EquipmentSlot, ArmorScore> bestArmorScores = new EnumMap<>(EquipmentSlot.class);

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }

            double attackDamage = attributeValue(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE, baseAttackDamage);
            if (attackDamage > bestAttackDamage + EPSILON
                    || (Math.abs(attackDamage - bestAttackDamage) <= EPSILON
                    && isMoreDurable(stack, bestWeapon))) {
                bestWeapon = stack.copy();
                bestAttackDamage = attackDamage;
            }

            if (stack.getItem() instanceof ShieldItem && isMoreDurable(stack, bestShield)) {
                bestShield = stack.copy();
            }

            EquipmentSlot candidateSlot = player.getEquipmentSlotForItem(stack);
            if (!isArmorSlot(candidateSlot)) {
                continue;
            }
            ArmorScore score = new ArmorScore(
                    attributeValue(stack, candidateSlot, Attributes.ARMOR, 0.0D),
                    attributeValue(stack, candidateSlot, Attributes.ARMOR_TOUGHNESS, 0.0D),
                    remainingDurability(stack));
            ArmorScore previous = bestArmorScores.get(candidateSlot);
            if (previous == null || score.compareTo(previous) > 0) {
                bestArmorScores.put(candidateSlot, score);
                bestArmor.put(candidateSlot, stack.copy());
            }
        }

        double armor = 0.0D;
        double toughness = 0.0D;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ArmorScore score = bestArmorScores.get(slot);
            if (score != null) {
                armor += score.armor();
                toughness += score.toughness();
            }
        }
        return new Snapshot(bestWeapon, bestShield, bestArmor, bestAttackDamage, armor, toughness);
    }

    public static double attributeValue(
            ItemStack stack,
            EquipmentSlot slot,
            Holder<Attribute> target,
            double baseValue) {
        AttributeTotals totals = new AttributeTotals();
        stack.forEachModifier(slot, (attribute, modifier) -> {
            if (!attribute.equals(target)) {
                return;
            }
            switch (modifier.operation()) {
                case ADD_VALUE -> totals.addValue += modifier.amount();
                case ADD_MULTIPLIED_BASE -> totals.addBase += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> totals.totalMultipliers.add(modifier.amount());
            }
        });
        double value = baseValue + totals.addValue + baseValue * totals.addBase;
        for (double multiplier : totals.totalMultipliers) {
            value *= 1.0D + multiplier;
        }
        return value;
    }

    private static List<ItemStack> allCarriedStacks(ServerPlayer player) {
        List<ItemStack> stacks = new ArrayList<>(
                player.getInventory().items.size()
                        + player.getInventory().armor.size()
                        + player.getInventory().offhand.size());
        stacks.addAll(player.getInventory().items);
        stacks.addAll(player.getInventory().armor);
        stacks.addAll(player.getInventory().offhand);
        return stacks;
    }

    private static boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD
                || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS
                || slot == EquipmentSlot.FEET;
    }

    private static boolean isMoreDurable(ItemStack candidate, ItemStack current) {
        return current.isEmpty() || remainingDurability(candidate) > remainingDurability(current);
    }

    private static int remainingDurability(ItemStack stack) {
        return stack.isDamageableItem() ? stack.getMaxDamage() - stack.getDamageValue() : Integer.MAX_VALUE;
    }

    public record Snapshot(
            ItemStack weapon,
            ItemStack shield,
            Map<EquipmentSlot, ItemStack> armor,
            double attackDamage,
            double armorValue,
            double armorToughness) {
        public Snapshot {
            weapon = weapon == null ? ItemStack.EMPTY : weapon.copy();
            shield = shield == null ? ItemStack.EMPTY : shield.copy();
            EnumMap<EquipmentSlot, ItemStack> armorCopy = new EnumMap<>(EquipmentSlot.class);
            if (armor != null) {
                armor.forEach((slot, stack) -> armorCopy.put(slot, stack.copy()));
            }
            armor = Map.copyOf(armorCopy);
        }

        public ItemStack armor(EquipmentSlot slot) {
            ItemStack stack = armor.get(slot);
            return stack == null ? ItemStack.EMPTY : stack.copy();
        }
    }

    private record ArmorScore(double armor, double toughness, int durability) implements Comparable<ArmorScore> {
        @Override
        public int compareTo(ArmorScore other) {
            int armorComparison = Double.compare(armor, other.armor);
            if (armorComparison != 0) {
                return armorComparison;
            }
            int toughnessComparison = Double.compare(toughness, other.toughness);
            return toughnessComparison != 0 ? toughnessComparison : Integer.compare(durability, other.durability);
        }
    }

    private static final class AttributeTotals {
        private double addValue;
        private double addBase;
        private final List<Double> totalMultipliers = new ArrayList<>();
    }
}
