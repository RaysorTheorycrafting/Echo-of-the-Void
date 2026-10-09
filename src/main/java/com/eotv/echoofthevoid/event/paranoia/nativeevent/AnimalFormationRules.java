package com.eotv.echoofthevoid.event.paranoia.nativeevent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Minecraft-free geometry and pacing of the animal formations (circle, grid, death site, wake). */
public final class AnimalFormationRules {
    public static final int MIN_CIRCLE_ANIMALS = 5;
    public static final int MIN_SMALL_FORMATION_ANIMALS = 4;
    public static final int MAX_FORMATION_ANIMALS = 12;
    /** A deliberate look this long (summed while staring) breaks the formation. */
    public static final int STARE_TICKS_TO_RELEASE = 60;
    /** Nobody found it: the animals quietly go back to grazing. */
    public static final long MAX_FORMATION_TICKS = 20L * 60L * 5L;
    public static final int MAX_RELEASE_STAGGER_TICKS = 14;
    public static final double DEATH_SITE_RADIUS = 3.0D;
    public static final double MIN_WAKE_RADIUS = 7.0D;
    public static final double MAX_WAKE_RADIUS = 11.0D;
    public static final int MAX_PEN_CELLS = 144;

    private AnimalFormationRules() {
    }

    /** Radius giving each animal roughly 1.1 blocks of arc, never tighter than 2.5 blocks. */
    public static double circleRadius(int animals) {
        return Math.max(2.5D, animals * 1.1D / (2.0D * Math.PI));
    }

    /** Evenly spaced points on a circle, starting at {@code phase} radians. */
    public static List<double[]> circlePoints(double centreX, double centreZ, double radius, int count, double phase) {
        List<double[]> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double angle = phase + 2.0D * Math.PI * i / count;
            points.add(new double[]{centreX + Math.cos(angle) * radius, centreZ + Math.sin(angle) * radius});
        }
        return points;
    }

    /** Minecraft yaw (degrees) for a mob at (fromX, fromZ) looking toward (toX, toZ). */
    public static float yawToward(double fromX, double fromZ, double toX, double toZ) {
        return (float) (Math.toDegrees(Math.atan2(toZ - fromZ, toX - fromX)) - 90.0D);
    }

    /**
     * Lattice cells of a pen: every other cell on both axes, never touching the pen's border, so each
     * animal stands alone with a clear one-block gap. {@code cells} are packed as {@link #pack}.
     */
    public static List<int[]> gridCells(Set<Long> cells) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (long packed : cells) {
            minX = Math.min(minX, unpackX(packed));
            minZ = Math.min(minZ, unpackZ(packed));
        }
        List<int[]> lattice = new ArrayList<>();
        for (long packed : cells) {
            int x = unpackX(packed);
            int z = unpackZ(packed);
            if (Math.floorMod(x - minX, 2) != 1 || Math.floorMod(z - minZ, 2) != 1) {
                continue;
            }
            if (cells.contains(pack(x + 1, z)) && cells.contains(pack(x - 1, z))
                    && cells.contains(pack(x, z + 1)) && cells.contains(pack(x, z - 1))) {
                lattice.add(new int[]{x, z});
            }
        }
        lattice.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
        return lattice;
    }

    public static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    public static int unpackZ(long packed) {
        return (int) packed;
    }

    /** True at night, when circles in open fields are allowed (Minecraft day time modulo 24000). */
    public static boolean isNight(long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        return time >= 13000L && time <= 23000L;
    }

    /** True in the first part of the morning, i.e. right after a night actually slept through. */
    public static boolean isMorningAfterSleep(long dayTime) {
        return Math.floorMod(dayTime, 24000L) < 1000L;
    }
}
