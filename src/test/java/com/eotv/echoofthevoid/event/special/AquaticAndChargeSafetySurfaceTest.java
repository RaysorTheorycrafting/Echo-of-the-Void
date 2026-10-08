package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AquaticAndChargeSafetySurfaceTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid");

    @Test
    void everyDrownedDerivedTypeUsesTheVanillaUnderwaterBreathingTag() throws IOException {
        String tag = Files.readString(Path.of(
                "src", "main", "resources", "data", "minecraft", "tags", "entity_type",
                "can_breathe_under_water.json"), StandardCharsets.UTF_8);
        assertTrue(tag.contains("echoofthevoid:uncanny_drowned"));
        assertTrue(tag.contains("echoofthevoid:uncanny_drifter"));
        assertTrue(tag.contains("echoofthevoid:uncanny_dredger"));
        assertTrue(tag.contains("\"replace\": false"));

        String base = read("entity/custom/AbstractUncannyAquaticSpecialEntity.java");
        String drowned = read("entity/custom/UncannyDrownedEntity.java");
        assertFalse(base.contains("boolean canBreatheUnderwater()"));
        assertFalse(drowned.contains("boolean canBreatheUnderwater()"));
        assertTrue(base.contains("setAirSupply(this.getMaxAirSupply())"));
    }

    @Test
    void formerInstantChargesKeepReadableVanillaLocomotion() throws IOException {
        String drowned = read("entity/custom/UncannyDrownedEntity.java");
        String endermite = read("entity/custom/UncannyEndermiteEntity.java");
        String magma = read("entity/custom/UncannyMagmaCubeEntity.java");
        String slime = read("entity/custom/UncannySlimeEntity.java");
        String phantom = read("entity/custom/UncannyPhantomEntity.java");
        String ghast = read("entity/custom/UncannyGhastEntity.java");
        for (String source : java.util.List.of(drowned, endermite, magma, slime, phantom)) {
            assertTrue(source.contains("usesHistoricalSpecializedBehavior(this)"));
        }
        assertFalse(drowned.contains("target.position().subtract(this.position())"));
        assertFalse(endermite.contains("target.position().subtract(this.position())"));
        assertFalse(magma.contains("glideSpeed"));
        assertFalse(slime.contains("glideSpeed"));
        assertFalse(phantom.contains("player.position().subtract(this.position())"));
        assertFalse(ghast.contains("toPlayer.scale"));
        assertTrue(drowned.contains("getNavigation().moveTo(target, 1.08D)"));
        assertTrue(endermite.contains("getNavigation().moveTo(target, 1.12D)"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(JAVA_ROOT.resolve(Path.of(relative)), StandardCharsets.UTF_8);
    }
}
