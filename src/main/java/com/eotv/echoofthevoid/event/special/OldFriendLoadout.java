package com.eotv.echoofthevoid.event.special;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.state.BlockState;

/** What the old friend carries: copies of its target's best sword, a shield and the armour worn. */
public final class OldFriendLoadout {
    private OldFriendLoadout() {
    }

    public record Loadout(ItemStack weapon, ItemStack shield, ItemStack head, ItemStack chest, ItemStack legs,
            ItemStack feet) {
    }

    public static Loadout of(ServerPlayer target) {
        Inventory inventory = target.getInventory();
        ItemStack bestSword = ItemStack.EMPTY;
        float bestBonus = Float.NEGATIVE_INFINITY;
        ItemStack shield = target.getOffhandItem().getItem() instanceof ShieldItem ? target.getOffhandItem() : ItemStack.EMPTY;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof SwordItem sword) {
                float bonus = sword.getTier().getAttackDamageBonus() + (stack.isEnchanted() ? 0.01F : 0.0F);
                if (bonus > bestBonus) {
                    bestBonus = bonus;
                    bestSword = stack;
                }
            } else if (shield.isEmpty() && stack.getItem() instanceof ShieldItem) {
                shield = stack;
            }
        }
        return new Loadout(
                copyOne(bestSword),
                copyOne(shield),
                target.getItemBySlot(EquipmentSlot.HEAD).copy(),
                target.getItemBySlot(EquipmentSlot.CHEST).copy(),
                target.getItemBySlot(EquipmentSlot.LEGS).copy(),
                target.getItemBySlot(EquipmentSlot.FEET).copy());
    }

    /**
     * What it eats: a copy of the most filling plain food the target carries (nothing with an effect,
     * so never rotten flesh or a golden apple), cooked beef otherwise.
     */
    public static ItemStack mealOf(ServerPlayer target) {
        Inventory inventory = target.getInventory();
        ItemStack best = ItemStack.EMPTY;
        int bestNutrition = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            net.minecraft.world.food.FoodProperties food = stack.getFoodProperties(target);
            if (food != null && food.effects().isEmpty() && food.nutrition() > bestNutrition) {
                bestNutrition = food.nutrition();
                best = stack;
            }
        }
        return best.isEmpty() ? new ItemStack(Items.COOKED_BEEF) : best.copyWithCount(1);
    }

    private static ItemStack copyOne(ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    /** Axe, pickaxe or shovel of the sword's material (wood without a sword), or a bare hand. */
    public static ItemStack toolFor(BlockState state, ItemStack weapon) {
        Tier tier = weapon.getItem() instanceof SwordItem sword ? sword.getTier() : Tiers.WOOD;
        if (state.is(BlockTags.LEAVES)) {
            // A player slashes through leaves with the sword already in hand.
            return weapon.copy();
        }
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return new ItemStack(pickaxe(tier));
        }
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return new ItemStack(shovel(tier));
        }
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return new ItemStack(axe(tier));
        }
        return ItemStack.EMPTY;
    }

    private static Item pickaxe(Tier tier) {
        return switch (tierOf(tier)) {
            case STONE -> Items.STONE_PICKAXE;
            case IRON -> Items.IRON_PICKAXE;
            case GOLD -> Items.GOLDEN_PICKAXE;
            case DIAMOND -> Items.DIAMOND_PICKAXE;
            case NETHERITE -> Items.NETHERITE_PICKAXE;
            default -> Items.WOODEN_PICKAXE;
        };
    }

    private static Item shovel(Tier tier) {
        return switch (tierOf(tier)) {
            case STONE -> Items.STONE_SHOVEL;
            case IRON -> Items.IRON_SHOVEL;
            case GOLD -> Items.GOLDEN_SHOVEL;
            case DIAMOND -> Items.DIAMOND_SHOVEL;
            case NETHERITE -> Items.NETHERITE_SHOVEL;
            default -> Items.WOODEN_SHOVEL;
        };
    }

    private static Item axe(Tier tier) {
        return switch (tierOf(tier)) {
            case STONE -> Items.STONE_AXE;
            case IRON -> Items.IRON_AXE;
            case GOLD -> Items.GOLDEN_AXE;
            case DIAMOND -> Items.DIAMOND_AXE;
            case NETHERITE -> Items.NETHERITE_AXE;
            default -> Items.WOODEN_AXE;
        };
    }

    private static Tiers tierOf(Tier tier) {
        return tier instanceof Tiers vanilla ? vanilla : Tiers.WOOD;
    }
}
