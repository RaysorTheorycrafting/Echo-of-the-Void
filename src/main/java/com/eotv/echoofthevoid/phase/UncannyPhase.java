package com.eotv.echoofthevoid.phase;

import com.eotv.echoofthevoid.config.UncannyConfig;

public enum UncannyPhase {
    PHASE_1(1, "message.echoofthevoid.phase1"),
    PHASE_2(2, "message.echoofthevoid.phase2"),
    PHASE_3(3, "message.echoofthevoid.phase3"),
    PHASE_4(4, "message.echoofthevoid.phase4");

    private final int index;
    private final String messageKey;

    UncannyPhase(int index, String messageKey) {
        this.index = index;
        this.messageKey = messageKey;
    }

    public int index() {
        return index;
    }

    public String messageKey() {
        return messageKey;
    }

    public double replacementChance() {
        return switch (this) {
            case PHASE_1 -> UncannyConfig.PHASE1_REPLACEMENT_CHANCE.get();
            case PHASE_2 -> UncannyConfig.PHASE2_REPLACEMENT_CHANCE.get();
            case PHASE_3 -> UncannyConfig.PHASE3_REPLACEMENT_CHANCE.get();
            case PHASE_4 -> UncannyConfig.PHASE4_REPLACEMENT_CHANCE.get();
        };
    }

    public long durationTicks() {
        int minutes = switch (this) {
            case PHASE_1 -> UncannyConfig.PHASE_P1_TO_P2_MINUTES.get();
            case PHASE_2 -> UncannyConfig.PHASE_P2_TO_P3_MINUTES.get();
            case PHASE_3 -> UncannyConfig.PHASE_P3_TO_P4_MINUTES.get();
            case PHASE_4 -> 0;
        };

        if (this == PHASE_4) {
            return Long.MAX_VALUE;
        }
        return minutes * 60L * 20L;
    }

    public boolean isFinal() {
        return this == PHASE_4;
    }

    public UncannyPhase next() {
        return switch (this) {
            case PHASE_1 -> PHASE_2;
            case PHASE_2 -> PHASE_3;
            case PHASE_3, PHASE_4 -> PHASE_4;
        };
    }

    public static UncannyPhase fromIndex(int index) {
        return switch (index) {
            case 1 -> PHASE_1;
            case 2 -> PHASE_2;
            case 3 -> PHASE_3;
            case 4 -> PHASE_4;
            default -> PHASE_1;
        };
    }
}

