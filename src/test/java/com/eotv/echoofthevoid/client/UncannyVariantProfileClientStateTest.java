package com.eotv.echoofthevoid.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UncannyVariantProfileClientStateTest {
    @Test
    void onlyEmptyOrEotvOwnedPresentationTagsCanCrossThePayloadBoundary() {
        assertTrue(UncannyVariantProfileClientState.isAllowedTag(""));
        assertTrue(UncannyVariantProfileClientState.isAllowedTag("eotv_variant_visual_pitch_black"));
        assertTrue(UncannyVariantProfileClientState.isAllowedTag("eotv_passive_fox_v1"));

        assertFalse(UncannyVariantProfileClientState.isAllowedTag(null));
        assertFalse(UncannyVariantProfileClientState.isAllowedTag("pitch_black"));
        assertFalse(UncannyVariantProfileClientState.isAllowedTag("eotv_dev_spawned"));
        assertFalse(UncannyVariantProfileClientState.isAllowedTag("minecraft:arbitrary"));
    }
}
