package com.eotv.echoofthevoid.entity.variant;

import com.eotv.echoofthevoid.event.passive.ApprovedVanillaVariantCatalog;
import java.util.LinkedHashSet;
import java.util.Set;

/** Auditable coverage manifest for every standard, non-boss Vanilla creature in 1.21.1. */
public final class VanillaCreatureVariantCoverage {
    private static final Set<String> COMPATIBLE_SPECIES = Set.of(
            "allay", "armadillo", "axolotl", "bat", "bee", "blaze", "bogged", "breeze",
            "camel", "cat", "cave_spider", "chicken", "cod", "cow", "creeper", "dolphin",
            "donkey", "drowned", "elder_guardian", "enderman", "endermite", "evoker", "fox",
            "frog", "ghast", "glow_squid", "goat", "guardian", "hoglin", "horse", "husk",
            "iron_golem", "llama", "magma_cube", "mooshroom", "mule", "ocelot", "panda",
            "parrot", "phantom", "pig", "piglin", "piglin_brute", "pillager", "polar_bear",
            "pufferfish", "rabbit", "ravager", "salmon", "sheep", "shulker", "silverfish",
            "skeleton", "skeleton_horse", "slime", "sniffer", "snow_golem", "spider", "squid",
            "stray", "strider", "tadpole", "trader_llama", "tropical_fish", "turtle", "vex",
            "villager", "vindicator", "wandering_trader", "warden", "witch", "wither_skeleton",
            "wolf", "zoglin", "zombie", "zombie_horse", "zombie_villager", "zombified_piglin");

    private static final Set<String> LEGACY_PASSIVE_SPECIES = Set.of(
            "chicken", "pig", "cow", "sheep", "wolf", "cat", "fox", "squid", "cod", "salmon",
            "parrot", "llama", "villager", "wandering_trader", "rabbit");

    private static final Set<String> ESTABLISHED_FIVE_WAY_REPLACEMENTS = Set.of(
            "zombie", "skeleton", "creeper", "spider", "enderman", "iron_golem");

    private static final Set<String> COVERED_SPECIES;
    private static final int VARIANT_PROFILE_COUNT;

    static {
        LinkedHashSet<String> covered = new LinkedHashSet<>();
        covered.addAll(LEGACY_PASSIVE_SPECIES);
        covered.addAll(ESTABLISHED_FIVE_WAY_REPLACEMENTS);
        ReplacementVariantExpansionCatalog.species().stream()
                .map(ReplacementVariantExpansionCatalog.Species::vanillaTypeKey)
                .forEach(covered::add);
        ApprovedVanillaVariantCatalog.species().stream()
                .map(ApprovedVanillaVariantCatalog.Species::typeKey)
                .forEach(covered::add);
        COVERED_SPECIES = Set.copyOf(covered);

        VARIANT_PROFILE_COUNT = LEGACY_PASSIVE_SPECIES.size() * 5
                + ESTABLISHED_FIVE_WAY_REPLACEMENTS.size() * 5
                + ReplacementVariantExpansionCatalog.variants().size()
                + ApprovedVanillaVariantCatalog.variants().size();

        if (!COVERED_SPECIES.equals(COMPATIBLE_SPECIES)) {
            LinkedHashSet<String> missing = new LinkedHashSet<>(COMPATIBLE_SPECIES);
            missing.removeAll(COVERED_SPECIES);
            LinkedHashSet<String> unexpected = new LinkedHashSet<>(COVERED_SPECIES);
            unexpected.removeAll(COMPATIBLE_SPECIES);
            throw new IllegalStateException(
                    "Vanilla creature variant coverage drifted; missing=" + missing + ", unexpected=" + unexpected);
        }
    }

    private VanillaCreatureVariantCoverage() {
    }

    public static Set<String> compatibleSpecies() {
        return COMPATIBLE_SPECIES;
    }

    public static Set<String> coveredSpecies() {
        return COVERED_SPECIES;
    }

    public static Set<String> legacyPassiveSpecies() {
        return LEGACY_PASSIVE_SPECIES;
    }

    public static Set<String> establishedFiveWayReplacements() {
        return ESTABLISHED_FIVE_WAY_REPLACEMENTS;
    }

    public static int variantProfileCount() {
        return VARIANT_PROFILE_COUNT;
    }
}
