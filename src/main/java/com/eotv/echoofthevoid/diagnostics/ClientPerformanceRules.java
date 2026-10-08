package com.eotv.echoofthevoid.diagnostics;

/** Pure thresholds for bounded client FPS diagnostics. */
public final class ClientPerformanceRules {
    public static final int SAMPLE_INTERVAL_TICKS = 20;
    public static final int REQUIRED_CONSECUTIVE_SAMPLES = 3;
    public static final int REPORT_COOLDOWN_TICKS = 20 * 60;
    public static final int ABSOLUTE_LOW_FPS = 45;
    public static final int RELATIVE_BASELINE_MINIMUM = 60;
    public static final double RELATIVE_LOW_RATIO = 0.55D;

    private ClientPerformanceRules() {
    }

    public static boolean isDegraded(int framesPerSecond, int baselineFramesPerSecond) {
        if (framesPerSecond <= 0) {
            return false;
        }
        return framesPerSecond <= ABSOLUTE_LOW_FPS
                || (baselineFramesPerSecond >= RELATIVE_BASELINE_MINIMUM
                        && framesPerSecond <= Math.floor(baselineFramesPerSecond * RELATIVE_LOW_RATIO));
    }

    /** Lets a recovered client raise its baseline quickly while a transient dip lowers it slowly. */
    public static int updateBaseline(int previousBaseline, int framesPerSecond) {
        if (framesPerSecond <= 0) {
            return Math.max(0, previousBaseline);
        }
        if (previousBaseline <= 0) {
            return framesPerSecond;
        }
        if (framesPerSecond > previousBaseline) {
            return previousBaseline + Math.max(1, (framesPerSecond - previousBaseline) / 4);
        }
        return Math.max(framesPerSecond, previousBaseline - 1);
    }
}
