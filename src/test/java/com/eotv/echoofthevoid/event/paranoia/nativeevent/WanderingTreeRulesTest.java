package com.eotv.echoofthevoid.event.paranoia.nativeevent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WanderingTreeRulesTest {
    @Test
    void trapArmsOnlyFromTheThirdMove() {
        assertFalse(WanderingTreeRules.isArmed(0));
        assertFalse(WanderingTreeRules.isArmed(2));
        assertTrue(WanderingTreeRules.isArmed(3));
        assertTrue(WanderingTreeRules.isArmed(7));
    }

    @Test
    void firstCandidateHeadsStraightForTheBase() {
        List<int[]> steps = WanderingTreeRules.candidateSteps(0, 0, 30, 0, 4);
        assertFalse(steps.isEmpty());
        assertArrayEquals(new int[]{4, 0}, steps.get(0));
    }

    @Test
    void everyCandidateGetsCloserWithoutEnteringTheStopRadius() {
        int[][] starts = {{0, 0}, {-25, 13}, {40, -40}, {9, 9}, {0, 17}};
        for (int[] start : starts) {
            double before = WanderingTreeRules.horizontalDistance(start[0], start[1], 0, 40);
            for (int[] step : WanderingTreeRules.candidateSteps(start[0], start[1], 0, 40, 4)) {
                double after = WanderingTreeRules.horizontalDistance(start[0] + step[0], start[1] + step[1], 0, 40);
                assertTrue(after < before, "step must approach the base");
                assertTrue(after >= WanderingTreeRules.STOP_DISTANCE_BLOCKS, "step must not cross the stop radius");
                assertTrue(Math.abs(step[0]) <= 4 && Math.abs(step[1]) <= 4, "step must stay short");
            }
        }
    }

    @Test
    void lastStepShrinksInsteadOfOvershootingAndThenTheTreeSettles() {
        List<int[]> steps = WanderingTreeRules.candidateSteps(9, 0, 0, 0, 4);
        assertFalse(steps.isEmpty());
        assertArrayEquals(new int[]{-2, 0}, steps.get(0));
        assertTrue(WanderingTreeRules.hasArrived(7, 0, 0, 0));
        assertTrue(WanderingTreeRules.candidateSteps(7, 0, 0, 0, 4).isEmpty());
        assertFalse(WanderingTreeRules.hasArrived(18, 0, 0, 0));
    }

    @Test
    void adoptionRingLeavesRoomForAtLeastThreeMoves() {
        int x = WanderingTreeRules.MIN_ADOPT_DISTANCE_BLOCKS;
        for (int move = 0; move < WanderingTreeRules.TRAP_MOVE_THRESHOLD; move++) {
            assertFalse(WanderingTreeRules.hasArrived(x, 0, 0, 0), "arrived after only " + move + " moves");
            int[] step = WanderingTreeRules.candidateSteps(x, 0, 0, 0, WanderingTreeRules.MAX_STEP_BLOCKS).get(0);
            x += step[0];
        }
    }

    @Test
    void pathVisitsEveryColumnUpToTheDestination() {
        List<int[]> diagonal = WanderingTreeRules.pathColumns(3, 3);
        assertEquals(3, diagonal.size());
        assertArrayEquals(new int[]{3, 3}, diagonal.get(2));
        List<int[]> straight = WanderingTreeRules.pathColumns(-4, 0);
        assertEquals(4, straight.size());
        assertArrayEquals(new int[]{-1, 0}, straight.get(0));
        assertArrayEquals(new int[]{-4, 0}, straight.get(3));
        List<int[]> skewed = WanderingTreeRules.pathColumns(4, 1);
        for (int i = 1; i < skewed.size(); i++) {
            int[] previous = skewed.get(i - 1);
            int[] current = skewed.get(i);
            assertTrue(Math.abs(current[0] - previous[0]) <= 1 && Math.abs(current[1] - previous[1]) <= 1,
                    "consecutive path columns must touch");
        }
    }

    @Test
    void viewConeCoversPeripheralVisionButNotTheBack() {
        assertTrue(WanderingTreeRules.isInViewCone(0, 0, 1, 0, 0, 10));
        assertTrue(WanderingTreeRules.isInViewCone(0, 0, 1, 10, 0, 4), "about 68 degrees off: still seen");
        assertFalse(WanderingTreeRules.isInViewCone(0, 0, 1, 10, 0, 0), "90 degrees off: unseen");
        assertFalse(WanderingTreeRules.isInViewCone(0, 0, 1, 0, 0, -10), "behind: unseen");
    }

    @Test
    void moveDelayStaysWithinTwoToFiveMinutes() {
        assertEquals(2_400L, WanderingTreeRules.nextMoveDelayTicks(0.0D));
        assertEquals(6_000L, WanderingTreeRules.nextMoveDelayTicks(1.0D));
        long middle = WanderingTreeRules.nextMoveDelayTicks(0.5D);
        assertTrue(middle > 2_400L && middle < 6_000L);
    }
}
