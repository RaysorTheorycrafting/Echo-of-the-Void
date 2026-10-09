package com.eotv.echoofthevoid.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The edge rule works on normalized device coordinates. The FOV-independence claim is checked by
 * projecting the same screen-edge direction with real perspective matrices at several FOVs.
 */
class BlurEdgeRulesTest {
    @Test
    void invisibleAtTheCentreFullAtTheEdge() {
        assertEquals(0.0F, BlurEdgeRules.alpha(0.0F, 0.0F, 5.0F));
        assertEquals(0.0F, BlurEdgeRules.alpha(0.5F, -0.4F, 5.0F), "the inner part of the screen shows nothing");
        assertEquals(1.0F, BlurEdgeRules.alpha(0.9F, 0.1F, 5.0F), 1.0E-6F);
        assertEquals(1.0F, BlurEdgeRules.alpha(-0.2F, -0.95F, 5.0F), 1.0E-6F, "top and bottom edges count too");
        float halfway = BlurEdgeRules.alpha(0.72F, 0.0F, 5.0F);
        assertTrue(halfway > 0.3F && halfway < 0.7F, "smooth fade between the bands: " + halfway);
    }

    @Test
    void neverDrawnBehindTheCameraOrFarOffScreen() {
        assertEquals(0.0F, BlurEdgeRules.alpha(0.9F, 0.0F, -1.0F));
        assertEquals(0.0F, BlurEdgeRules.alpha(1.6F, 0.0F, 5.0F));
        assertEquals(0.0F, BlurEdgeRules.alpha(Float.NaN, 0.0F, 5.0F));
    }

    @Test
    void theWholeFigureMustStayInTheEdgeBand() {
        // Standing tall at the left edge: its height crosses the horizon line, its width stays outside.
        assertEquals(1.0F, BlurEdgeRules.alphaForExtent(-0.98F, -0.86F, -0.45F, 0.35F), 1.0E-6F);
        // Up close: its body centre falls low on screen, but its head covers the crosshair.
        assertEquals(0.0F, BlurEdgeRules.alphaForExtent(-0.4F, 0.5F, -1.6F, 0.3F));
        // Reaching into the fade with one arm only weakens it.
        float reaching = BlurEdgeRules.alphaForExtent(-0.95F, -0.7F, -0.3F, 0.3F);
        assertTrue(reaching > 0.0F && reaching < 1.0F, "partly inside the fade: " + reaching);
        // Wholly beyond the border it is clipped.
        assertEquals(0.0F, BlurEdgeRules.alphaForExtent(1.3F, 1.5F, -0.2F, 0.2F));
    }

    @Test
    void theSameScreenPositionGivesTheSameOpacityAtAnyFieldOfView() {
        // A point placed on screen at 90 % of the half-width, for vertical FOVs from 30 to 110 degrees
        // on a 16:9 screen: its NDC is the same, so its opacity is the same.
        float aspect = 16.0F / 9.0F;
        Float reference = null;
        for (float fov = 30.0F; fov <= 110.0F; fov += 10.0F) {
            double f = 1.0D / Math.tan(Math.toRadians(fov) / 2.0D);
            double depth = 10.0D;
            double worldX = 0.9D * depth * aspect / f;
            // Perspective projection of (worldX, 0, -depth): clipX = f/aspect * x, clipW = depth.
            float ndcX = (float) ((f / aspect) * worldX / depth);
            float alpha = BlurEdgeRules.alpha(ndcX, 0.0F, (float) depth);
            if (reference == null) {
                reference = alpha;
            }
            assertEquals(reference, alpha, 1.0E-5F, "FOV " + fov);
        }
        assertEquals(1.0F, reference, 1.0E-5F);
    }
}
