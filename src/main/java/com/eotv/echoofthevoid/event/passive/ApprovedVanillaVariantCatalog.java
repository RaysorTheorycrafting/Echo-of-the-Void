package com.eotv.echoofthevoid.event.passive;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Immutable design/runtime catalog for the twenty player-approved Vanilla-derived variants. */
public final class ApprovedVanillaVariantCatalog {
    private static final List<Variant> VARIANTS = List.of(
            variant("bee_false_hive", "bee", "Bee?", "False Hive", 1, Rarity.RARE, 0),
            variant("bat_wrong_roost", "bat", "Bat?", "Wrong Roost", 1, Rarity.UNCOMMON, 0),
            variant("rabbit_return_to_cover", "rabbit", "Rabbit?", "Return to Cover", 1, Rarity.RARE, 0),
            variant("goat_echo_ram", "goat", "Goat?", "Echo Ram", 2, Rarity.RARE, 1),
            variant("horse_empty_rider", "horse", "Horse?", "Empty Rider", 2, Rarity.RARE, 0),
            variant("allay_wrong_recipient", "allay", "Allay?", "Wrong Recipient", 2, Rarity.VERY_RARE, 0),
            variant("axolotl_healthy_feign", "axolotl", "Axolotl?", "Healthy Feign", 2, Rarity.RARE, 0),
            variant("dolphin_blindside_escort", "dolphin", "Dolphin?", "Blindside Escort", 2, Rarity.RARE, 0),
            variant("frog_empty_tongue", "frog", "Frog?", "Empty Tongue", 1, Rarity.RARE, 0),
            variant("turtle_false_nest", "turtle", "Turtle?", "False Nest", 2, Rarity.RARE, 0),
            variant("sniffer_second_dig", "sniffer", "Sniffer?", "Second Dig", 2, Rarity.VERY_RARE, 0),
            variant("armadillo_empty_threat", "armadillo", "Armadillo?", "Empty Threat", 1, Rarity.RARE, 0),
            variant("glow_squid_light_lag", "glow_squid", "Glow Squid?", "Light Lag", 2, Rarity.RARE, 0),
            variant("breeze_returned_wind", "breeze", "Breeze?", "Returned Wind", 3, Rarity.RARE, 2),
            variant("cave_spider_ceiling_wait", "cave_spider", "Cave Spider?", "Ceiling Wait", 3, Rarity.RARE, 3),
            variant("shulker_empty_aim", "shulker", "Shulker?", "Empty Aim", 3, Rarity.RARE, 2),
            variant("guardian_false_beam", "guardian", "Guardian?", "False Beam", 3, Rarity.RARE, 2),
            variant("vex_caught_between", "vex", "Vex?", "Caught Between", 3, Rarity.VERY_RARE, 3),
            variant("silverfish_wrong_stone", "silverfish", "Silverfish?", "Wrong Stone", 2, Rarity.RARE, 1),
            variant("zombified_piglin_procession", "zombified_piglin", "Zombified Piglin?", "Procession", 2, Rarity.VERY_RARE, 1));

    private static final Map<String, Variant> BY_ID;
    private static final Map<String, Variant> BY_TYPE;

    static {
        Map<String, Variant> byId = new LinkedHashMap<>();
        Map<String, Variant> byType = new LinkedHashMap<>();
        for (Variant variant : VARIANTS) {
            if (byId.put(variant.id(), variant) != null) {
                throw new IllegalStateException("Duplicate approved variant id: " + variant.id());
            }
            if (byType.put(variant.typeKey(), variant) != null) {
                throw new IllegalStateException("Duplicate approved variant type: " + variant.typeKey());
            }
        }
        BY_ID = Map.copyOf(byId);
        BY_TYPE = Map.copyOf(byType);
    }

    private ApprovedVanillaVariantCatalog() {
    }

    public static List<Variant> variants() {
        return VARIANTS;
    }

    public static Variant byId(String id) {
        return id == null ? null : BY_ID.get(id.trim().toLowerCase(Locale.ROOT));
    }

    public static Variant byTypeKey(String typeKey) {
        return typeKey == null ? null : BY_TYPE.get(typeKey.trim().toLowerCase(Locale.ROOT));
    }

    /** Per-natural-spawn chance. It is deliberately independent from the old phase-4 replacement flood. */
    public static double naturalChance(Variant variant, int phase) {
        int clampedPhase = Math.max(1, Math.min(4, phase));
        if (variant == null || clampedPhase < variant.minimumPhase()) {
            return 0.0D;
        }
        return switch (variant.rarity()) {
            case UNCOMMON -> switch (clampedPhase) {
                case 1 -> 0.006D;
                case 2 -> 0.014D;
                case 3 -> 0.028D;
                default -> 0.045D;
            };
            case RARE -> switch (clampedPhase) {
                case 1 -> 0.0025D;
                case 2 -> 0.008D;
                case 3 -> 0.018D;
                default -> 0.035D;
            };
            case VERY_RARE -> switch (clampedPhase) {
                case 1 -> 0.0006D;
                case 2 -> 0.0025D;
                case 3 -> 0.006D;
                default -> 0.012D;
            };
        };
    }

    private static Variant variant(
            String id,
            String typeKey,
            String displayName,
            String behavior,
            int minimumPhase,
            Rarity rarity,
            int danger) {
        return new Variant(id, typeKey, displayName, behavior, minimumPhase, rarity, danger);
    }

    public enum Rarity {
        UNCOMMON,
        RARE,
        VERY_RARE
    }

    public record Variant(
            String id,
            String typeKey,
            String displayName,
            String behavior,
            int minimumPhase,
            Rarity rarity,
            int danger) {
    }
}
