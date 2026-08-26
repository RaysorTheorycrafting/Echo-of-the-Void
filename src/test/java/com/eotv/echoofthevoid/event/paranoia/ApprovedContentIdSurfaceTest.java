package com.eotv.echoofthevoid.event.paranoia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ApprovedContentIdSurfaceTest {
    @Test
    void approvedEventIdsKeepTheirValidatedSpellingAndOrder() {
        assertEquals(List.of(
                "empty_lead",
                "borrowed_painting",
                "returned_drop",
                "ghost_cart",
                "misdirected_enchantment",
                "false_piston",
                "orphan_signal",
                "cauldron_echo",
                "map_intruder",
                "empty_wake",
                "countercurrent_column",
                "false_sculk_vibration",
                "watching_arrow",
                "suspended_fall",
                "extra_in_the_herd",
                "lava_wake",
                "stray_experience",
                "false_lid",
                "jukebox_afterbeat",
                "beacon_fragment"), List.of(
                ParanoiaEventIds.EMPTY_LEAD,
                ParanoiaEventIds.BORROWED_PAINTING,
                ParanoiaEventIds.RETURNED_DROP,
                ParanoiaEventIds.GHOST_CART,
                ParanoiaEventIds.MISDIRECTED_ENCHANTMENT,
                ParanoiaEventIds.FALSE_PISTON,
                ParanoiaEventIds.ORPHAN_SIGNAL,
                ParanoiaEventIds.CAULDRON_ECHO,
                ParanoiaEventIds.MAP_INTRUDER,
                ParanoiaEventIds.EMPTY_WAKE,
                ParanoiaEventIds.COUNTERCURRENT_COLUMN,
                ParanoiaEventIds.FALSE_SCULK_VIBRATION,
                ParanoiaEventIds.WATCHING_ARROW,
                ParanoiaEventIds.SUSPENDED_FALL,
                ParanoiaEventIds.EXTRA_IN_THE_HERD,
                ParanoiaEventIds.LAVA_WAKE,
                ParanoiaEventIds.STRAY_EXPERIENCE,
                ParanoiaEventIds.FALSE_LID,
                ParanoiaEventIds.JUKEBOX_AFTERBEAT,
                ParanoiaEventIds.BEACON_FRAGMENT));
    }
}
