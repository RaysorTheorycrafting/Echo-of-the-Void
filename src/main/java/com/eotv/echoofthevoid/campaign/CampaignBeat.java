package com.eotv.echoofthevoid.campaign;

/** Short hidden pressure states which keep the campaign from becoming a monotonic frequency ramp. */
public enum CampaignBeat {
    REST,
    UNEASE,
    PRESSURE,
    RELEASE,
    AFTERSHOCK;

    public static CampaignBeat fromSavedName(String name) {
        if (name == null || name.isBlank()) {
            return UNEASE;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return UNEASE;
        }
    }
}
