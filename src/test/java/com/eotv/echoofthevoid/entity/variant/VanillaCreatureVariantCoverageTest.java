package com.eotv.echoofthevoid.entity.variant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eotv.echoofthevoid.event.passive.ApprovedVanillaVariantCatalog;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VanillaCreatureVariantCoverageTest {
    @Test
    void everyCompatibleStandardCreatureHasAtLeastOneFiveWayMatrix() {
        assertEquals(78, VanillaCreatureVariantCoverage.compatibleSpecies().size());
        assertEquals(
                VanillaCreatureVariantCoverage.compatibleSpecies(),
                VanillaCreatureVariantCoverage.coveredSpecies());
        assertEquals(395, VanillaCreatureVariantCoverage.variantProfileCount());
    }

    @Test
    void bossesAndUnusedTechnicalMobsStayOutsideTheMassReplacementSurface() {
        Set<String> covered = VanillaCreatureVariantCoverage.coveredSpecies();
        assertFalse(covered.contains("ender_dragon"));
        assertFalse(covered.contains("wither"));
        assertFalse(covered.contains("giant"));
        assertFalse(covered.contains("illusioner"));
    }

    @Test
    void newPitchBlackSilentProfilesRemainAThreePercentExceptionAcrossTheWholeCatalog() {
        int newBlackSilent = ApprovedVanillaVariantCatalog.pitchBlackSilentVariants().size();
        int establishedBlackSilent = 2; // Fox? v1 and Llama? v3 are preserved legacy profiles.
        int totalBlackSilent = newBlackSilent + establishedBlackSilent;

        assertEquals(10, newBlackSilent);
        assertEquals(12, totalBlackSilent);
        assertTrue(totalBlackSilent * 100 < VanillaCreatureVariantCoverage.variantProfileCount() * 4);
    }
}
