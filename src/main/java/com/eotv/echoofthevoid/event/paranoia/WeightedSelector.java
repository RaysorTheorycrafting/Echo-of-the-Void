package com.eotv.echoofthevoid.event.paranoia;

import java.util.List;

/** Pure weighted selection using a caller-provided roll for reproducible runtime parity tests. */
public final class WeightedSelector {
    private WeightedSelector() {
    }

    public static int totalWeight(List<? extends WeightedChoice<?>> choices) {
        int total = 0;
        for (WeightedChoice<?> choice : choices) {
            total = Math.addExact(total, choice.weight());
        }
        return total;
    }

    /**
     * Returns the selected value, or {@code null} when the roll is outside the pool.
     * Runtime callers provide {@code nextInt(totalWeight)}, matching the 1.1.1 behavior.
     */
    public static <T> T pick(List<WeightedChoice<T>> choices, int roll) {
        int running = roll;
        for (WeightedChoice<T> choice : choices) {
            running -= choice.weight();
            if (running < 0) {
                return choice.value();
            }
        }
        return null;
    }

    public static int pickIndex(int[] weights, int roll) {
        int running = roll;
        for (int index = 0; index < weights.length; index++) {
            running -= weights[index];
            if (running < 0) {
                return index;
            }
        }
        return -1;
    }
}
