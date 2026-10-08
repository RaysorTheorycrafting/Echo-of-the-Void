package com.eotv.echoofthevoid.event.special;

import com.eotv.echoofthevoid.diagnostics.DiagnosticSeverity;
import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Applies {@link CombatParityRules} to a live creature from the player's real equipment: best
 * carried weapon (enchantments and Strength included) for toughness, worn armour, Protection and
 * Resistance for lethality. Called when a hunt starts and every couple of seconds afterwards, so
 * switching to a better weapon or armour mid-fight is answered in kind.
 */
public final class CombatParity {
    private static final double PLAYER_BASE_ATTACK_SPEED = 4.0D;

    private CombatParity() {
    }

    /** What the player can deal: one full-charge hit and how many of them per second. */
    public record Offense(double hitDamage, double attacksPerSecond, ItemStack weapon) {
    }

    /** Values written to the creature, kept for diagnostics and tests. */
    public record Applied(double maxHealth, double rawDamage, double effectiveDamage, Offense offense) {
    }

    public static Applied apply(Mob creature, ServerPlayer player, CombatParityRules.Profile profile, boolean refill) {
        if (creature == null || player == null || profile == null
                || !(creature.level() instanceof ServerLevel level)) {
            return null;
        }
        Offense offense = bestOffense(level, player, creature);
        double maxHealth = CombatParityRules.maxHealth(profile, offense.hitDamage(), offense.attacksPerSecond());
        double effective = CombatParityRules.effectiveDamagePerHit(profile, player.getMaxHealth());
        DamageSource source = level.damageSources().mobAttack(creature);
        double raw = CombatParityRules.rawDamageFor(effective, damage -> reducedDamage(level, player, source, damage));

        double previousMaximum = creature.getMaxHealth();
        setBase(creature, Attributes.MAX_HEALTH, maxHealth);
        setBase(creature, Attributes.ATTACK_DAMAGE, raw);
        // Toughness lives entirely in health: armour would make the weapon arithmetic lie.
        setBase(creature, Attributes.ARMOR, 0.0D);
        setBase(creature, Attributes.ARMOR_TOUGHNESS, 0.0D);
        creature.setHealth((float) (refill
                ? maxHealth
                : CombatParityRules.rescaledHealth(creature.getHealth(), previousMaximum, maxHealth)));
        if (refill) {
            UncannyDiagnostics.recordForPlayer(
                    player,
                    DiagnosticSeverity.INFO,
                    "special",
                    "combat_parity_applied",
                    UncannyDiagnostics.fields(
                            "profile", profile.id(),
                            "weapon", offense.weapon().isEmpty() ? "hand" : offense.weapon().getItem().toString(),
                            "player_hit", offense.hitDamage(),
                            "player_attacks_per_second", offense.attacksPerSecond(),
                            "max_health", maxHealth,
                            "raw_damage", raw,
                            "effective_damage", effective,
                            "hits_to_kill_player", CombatParityRules.hitsToKillPlayer(profile)));
        }
        return new Applied(maxHealth, raw, effective, offense);
    }

    /** Health only, for a creature that never strikes (Devourer?); keeps its share of life on refresh. */
    public static double applyToughness(Mob creature, ServerPlayer player, double seconds, double minimum, boolean refill) {
        if (creature == null || player == null || !(creature.level() instanceof ServerLevel level)) {
            return 0.0D;
        }
        Offense offense = bestOffense(level, player, creature);
        double maxHealth = CombatParityRules.toughnessOnly(offense.hitDamage(), offense.attacksPerSecond(), seconds, minimum);
        double previousMaximum = creature.getMaxHealth();
        setBase(creature, Attributes.MAX_HEALTH, maxHealth);
        creature.setHealth((float) (refill
                ? maxHealth
                : CombatParityRules.rescaledHealth(creature.getHealth(), previousMaximum, maxHealth)));
        return maxHealth;
    }

    /** The carried weapon (or bare hand) with the highest sustained damage against this creature. */
    public static Offense bestOffense(ServerLevel level, ServerPlayer player, Mob creature) {
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        double base = attack == null ? 1.0D : attack.getBaseValue();
        MobEffectInstance strength = player.getEffect(MobEffects.DAMAGE_BOOST);
        if (strength != null) {
            base += 3.0D * (strength.getAmplifier() + 1);
        }
        DamageSource source = level.damageSources().playerAttack(player);
        List<ItemStack> stacks = new ArrayList<>(player.getInventory().items);
        stacks.addAll(player.getInventory().offhand);
        Offense best = new Offense(base, PLAYER_BASE_ATTACK_SPEED, ItemStack.EMPTY);
        double bestDps = base * Math.min(PLAYER_BASE_ATTACK_SPEED, CombatParityRules.MAX_SUSTAINED_ATTACKS_PER_SECOND);
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            double damage = AdaptiveSpecialEquipment.attributeValue(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE, base);
            damage = EnchantmentHelper.modifyDamage(level, stack, creature, source, (float) damage);
            double speed = AdaptiveSpecialEquipment.attributeValue(
                    stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_SPEED, PLAYER_BASE_ATTACK_SPEED);
            double dps = damage * Math.min(Math.max(0.25D, speed), CombatParityRules.MAX_SUSTAINED_ATTACKS_PER_SECOND);
            if (dps > bestDps) {
                bestDps = dps;
                best = new Offense(damage, speed, stack);
            }
        }
        return best;
    }

    /** Damage left after the player's armour, Protection-style enchantments and Resistance. */
    public static double reducedDamage(ServerLevel level, ServerPlayer player, DamageSource source, double raw) {
        float damage = CombatRules.getDamageAfterAbsorb(
                player,
                (float) raw,
                source,
                (float) player.getArmorValue(),
                (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        MobEffectInstance resistance = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (resistance != null) {
            damage *= Math.max(0.0F, 25 - (resistance.getAmplifier() + 1) * 5) / 25.0F;
        }
        float protection = EnchantmentHelper.getDamageProtection(level, player, source);
        if (protection > 0.0F) {
            damage = CombatRules.getDamageAfterMagicAbsorb(damage, protection);
        }
        return damage;
    }

    private static void setBase(Mob creature, Holder<Attribute> attribute, double value) {
        AttributeInstance instance = creature.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }
}
