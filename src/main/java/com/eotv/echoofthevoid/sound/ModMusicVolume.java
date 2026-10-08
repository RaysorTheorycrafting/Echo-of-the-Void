package com.eotv.echoofthevoid.sound;

import java.util.Set;

/**
 * Loudness rule for the mod's own scores (Elsewhere, Blackout and any later one), user decision of
 * 2026-10-08: they follow the average of every sound category instead of the Music slider alone, so
 * a player who turned Vanilla music down or off still hears them, never louder or quieter than the
 * rest of the game. The Master slider still applies on top, through the listener gain. Pure
 * arithmetic: the client passes the slider values of every category except Master.
 */
public final class ModMusicVolume {
    public static final String NAMESPACE = "echoofthevoid";
    /** Scores routed through this rule; everything else keeps its own category. */
    public static final Set<String> SCORE_PATHS = Set.of("music.elsewhere_trial", "event.blackout");

    private ModMusicVolume() {
    }

    public static boolean isScore(String namespace, String path) {
        return NAMESPACE.equals(namespace) && SCORE_PATHS.contains(path);
    }

    /** Mean of the given non-Master category volumes, each clamped to {@code [0, 1]}. */
    public static float averageOf(double... categoryVolumes) {
        if (categoryVolumes == null || categoryVolumes.length == 0) {
            return 1.0F;
        }
        double total = 0.0D;
        for (double volume : categoryVolumes) {
            total += Math.max(0.0D, Math.min(1.0D, volume));
        }
        return (float) (total / categoryVolumes.length);
    }
}
