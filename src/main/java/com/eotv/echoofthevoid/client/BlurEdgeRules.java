package com.eotv.echoofthevoid.client;

/**
 * Where Blur? may be drawn, in normalized device coordinates: -1..1 across the screen on both axes,
 * whatever the field of view, aspect ratio or dynamic FOV effects, since those are all inside the
 * projection matrix that produced the coordinates. It only exists in the outer band of the screen.
 */
public final class BlurEdgeRules {
    /** Below this distance from the centre (fraction of the half-screen) it is never drawn. */
    public static final float FADE_START = 0.62F;
    /** From this distance on it is drawn at full strength. */
    public static final float FADE_END = 0.82F;
    /** Slightly beyond the border it is clipped anyway; the margin covers its body reaching inside. */
    public static final float OFF_SCREEN = 1.25F;

    private BlurEdgeRules() {
    }

    /**
     * @param ndcX horizontal normalized device coordinate of the body centre
     * @param ndcY vertical normalized device coordinate of the body centre
     * @param clipW homogeneous w after projection; not positive means behind the camera
     * @return opacity factor 0..1
     */
    public static float alpha(float ndcX, float ndcY, float clipW) {
        if (clipW <= 0.0F) {
            return 0.0F;
        }
        return alphaForExtent(ndcX, ndcX, ndcY, ndcY);
    }

    /**
     * Opacity for the whole figure as it lies on screen. What counts is its part nearest the centre:
     * close up, one point of the body can sit in the edge band while the head covers the crosshair.
     *
     * @return opacity factor 0..1, zero as soon as any part of the figure reaches inside the band
     */
    public static float alphaForExtent(float minX, float maxX, float minY, float maxY) {
        if (Float.isNaN(minX) || Float.isNaN(maxX) || Float.isNaN(minY) || Float.isNaN(maxY)) {
            return 0.0F;
        }
        float edge = Math.max(gapFromCentre(minX, maxX), gapFromCentre(minY, maxY));
        if (edge >= OFF_SCREEN) {
            return 0.0F;
        }
        float t = (edge - FADE_START) / (FADE_END - FADE_START);
        t = Math.max(0.0F, Math.min(1.0F, t));
        return t * t * (3.0F - 2.0F * t);
    }

    private static float gapFromCentre(float min, float max) {
        if (min <= 0.0F && max >= 0.0F) {
            return 0.0F;
        }
        return Math.min(Math.abs(min), Math.abs(max));
    }
}
