package com.eotv.echoofthevoid.event.paranoia.nativeevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AnimalFormationRulesTest {
    @Test
    void circlePointsAreEvenlySpacedOnTheRadius() {
        List<double[]> points = AnimalFormationRules.circlePoints(10.0D, -4.0D, 3.0D, 6, 0.3D);
        assertEquals(6, points.size());
        for (double[] point : points) {
            assertEquals(3.0D, Math.hypot(point[0] - 10.0D, point[1] + 4.0D), 1.0E-9);
        }
        double first = Math.hypot(points.get(0)[0] - points.get(1)[0], points.get(0)[1] - points.get(1)[1]);
        double last = Math.hypot(points.get(5)[0] - points.get(0)[0], points.get(5)[1] - points.get(0)[1]);
        assertEquals(first, last, 1.0E-9);
    }

    @Test
    void circleLeavesRoomForEveryAnimalAndNeverShrinksBelowTheMinimum() {
        assertEquals(2.5D, AnimalFormationRules.circleRadius(4));
        double radius = AnimalFormationRules.circleRadius(AnimalFormationRules.MAX_FORMATION_ANIMALS);
        assertTrue(2.0D * Math.PI * radius / AnimalFormationRules.MAX_FORMATION_ANIMALS >= 1.0D);
    }

    @Test
    void yawFollowsMinecraftConvention() {
        // Minecraft: yaw 0 looks toward +Z (south), 90 toward -X (west), -90 toward +X (east), 180 toward -Z.
        assertEquals(0.0F, AnimalFormationRules.yawToward(0, 0, 0, 5), 1.0E-4);
        assertEquals(-90.0F, AnimalFormationRules.yawToward(0, 0, 5, 0), 1.0E-4);
        assertEquals(90.0F, Math.floorMod((int) AnimalFormationRules.yawToward(0, 0, -5, 0), 360), 1.0E-4);
        assertEquals(180.0F, Math.floorMod((int) AnimalFormationRules.yawToward(0, 0, 0, -5), 360), 1.0E-4);
    }

    @Test
    void gridKeepsOneGapBetweenAnimalsAndAwayFromTheFence() {
        Set<Long> pen = new HashSet<>();
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 5; z++) {
                pen.add(AnimalFormationRules.pack(x, z));
            }
        }
        List<int[]> cells = AnimalFormationRules.gridCells(pen);
        assertEquals(6, cells.size(), "7x5 pen: columns 1,3,5 and rows 1,3");
        for (int[] cell : cells) {
            assertTrue(cell[0] > 0 && cell[0] < 6 && cell[1] > 0 && cell[1] < 4, "never on the border");
            for (int[] other : cells) {
                if (other != cell) {
                    assertTrue(Math.abs(other[0] - cell[0]) + Math.abs(other[1] - cell[1]) >= 2, "never adjacent");
                }
            }
        }
    }

    @Test
    void packingRoundTripsNegativeCoordinates() {
        long packed = AnimalFormationRules.pack(-123456, 789);
        assertEquals(-123456, AnimalFormationRules.unpackX(packed));
        assertEquals(789, AnimalFormationRules.unpackZ(packed));
        long negative = AnimalFormationRules.pack(42, -9);
        assertEquals(42, AnimalFormationRules.unpackX(negative));
        assertEquals(-9, AnimalFormationRules.unpackZ(negative));
    }

    @Test
    void nightAndMorningWindows() {
        assertTrue(AnimalFormationRules.isNight(18000L));
        assertTrue(AnimalFormationRules.isNight(24000L * 3 + 13500L));
        assertFalse(AnimalFormationRules.isNight(6000L));
        assertTrue(AnimalFormationRules.isMorningAfterSleep(24000L * 5));
        assertFalse(AnimalFormationRules.isMorningAfterSleep(24000L * 5 + 4000L));
    }
}
