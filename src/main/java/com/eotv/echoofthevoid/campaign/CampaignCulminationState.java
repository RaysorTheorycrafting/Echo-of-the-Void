package com.eotv.echoofthevoid.campaign;

import java.util.Locale;

public enum CampaignCulminationState {
    UNINITIALIZED,
    PENDING,
    SATISFIED,
    EXPIRED;

    public static CampaignCulminationState fromSavedName(String name) {
        if (name == null || name.isBlank()) {
            return UNINITIALIZED;
        }
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return UNINITIALIZED;
        }
    }

    public boolean isTerminal() {
        return this == SATISFIED || this == EXPIRED;
    }
}
