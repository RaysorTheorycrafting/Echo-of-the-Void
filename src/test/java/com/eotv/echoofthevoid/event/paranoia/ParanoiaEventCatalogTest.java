package com.eotv.echoofthevoid.event.paranoia;

import static com.eotv.echoofthevoid.event.paranoia.ParanoiaEventIds.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ParanoiaEventCatalogTest {
    @Test
    void activePrimaryIdentifiersKeep111OrderAfterExplicitRetirements() {
        assertEquals(List.of(
                        FOOTSTEPS,
                        FLASH_RED,
                        FALSE_FALL,
                        BASE_REPLAY,
                        GHOST_MINER,
                        CAVE_COLLAPSE,
                        BELL,
                        VOID_SILENCE,
                        AQUATIC_STEPS,
                        ANIMAL_STARE_LOCK,
                        WORKBENCH_REJECT,
                        BLACKOUT,
                        FLASH,
                        FALSE_INJURY,
                        DOOR_INVERSION,
                        LIVING_ORE,
                        COMPASS_LIAR,
                        MISPLACED_LIGHT,
                        PET_REFUSAL,
                        HOTBAR_WRONG_COUNT,
                        CORRUPT_TOAST,
                        ASPHYXIA,
                        PHANTOM_HARVEST,
                        PROJECTED_SHADOW,
                        HUNTER_FOG,
                        GHOST_BREAKING,
                        EMPTY_TELEPORT,
                        ORPHAN_SHADOW,
                        FALSE_ANIMAL_HURT,
                        SILENT_BELL,
                        EMPTY_CONGREGATION,
                        EMPTY_LEAD,
                        EMPTY_WAKE,
                        GHOST_CART,
                        LAVA_WAKE,
                        FALSE_SCULK_VIBRATION,
                        WATCHING_ARROW,
                        SUSPENDED_FALL,
                        BEACON_FRAGMENT,
                        STRAY_EXPERIENCE,
                        EXTRA_IN_THE_HERD),
                ids(ParanoiaEventCatalog.primaryEvents()));
    }

    @Test
    void immutable111ReferenceViewsExcludeEveryPost111Event() {
        assertEquals(29, ParanoiaEventCatalog.referencePrimaryEvents111().size());
        assertEquals(7, ParanoiaEventCatalog.referenceAmbientEvents111().size());
        assertEquals(10, ParanoiaEventCatalog.referenceSpecialEvents111().size());
        assertTrue(ids(ParanoiaEventCatalog.referencePrimaryEvents111()).containsAll(
                List.of(ARMOR_BREAK, FORCED_DROP, GIANT_SUN)));
        assertTrue(ParanoiaEventCatalog.post111EventIds().stream()
                .noneMatch(ids(ParanoiaEventCatalog.referencePrimaryEvents111())::contains));
        assertTrue(ParanoiaEventCatalog.post111EventIds().stream()
                .noneMatch(ids(ParanoiaEventCatalog.referenceAmbientEvents111())::contains));
        assertEquals(List.of(
                        WATCHER, PULSE, FOLLOWER, KNOCKER, HURLER,
                        USHER, KEEPER, TENANT, STALKER, SHADOW),
                ids(ParanoiaEventCatalog.referenceSpecialEvents111()));
        assertEquals(3, ParanoiaEventCatalog.referenceRequire111(HOTBAR_WRONG_COUNT).minimumPhase());
        assertEquals(3, ParanoiaEventCatalog.referenceRequire111(CORRUPT_TOAST).minimumPhase());
        assertEquals(3, ParanoiaEventCatalog.referenceRequire111(HURLER).minimumPhase());
    }

    @Test
    void retiredIdentifiersRemainReservedWithTheir111MetadataButCannotBeScheduled() {
        assertEquals(Set.of(ARMOR_BREAK, FORCED_DROP, GIANT_SUN), ParanoiaEventCatalog.retiredEventIds());
        List<String> activePrimaryIds = ids(ParanoiaEventCatalog.primaryEvents());

        for (String retiredId : ParanoiaEventCatalog.retiredEventIds()) {
            assertTrue(ParanoiaEventCatalog.byId().containsKey(retiredId));
            assertTrue(ParanoiaEventCatalog.isRetired(retiredId));
            assertFalse(activePrimaryIds.contains(retiredId));
            assertTrue(ParanoiaEventCatalog.require(retiredId).primaryWeight() > 0);
        }
        assertFalse(ParanoiaEventCatalog.isRetired(FOOTSTEPS));
        assertEquals(Set.of(CORRUPT_MESSAGE), ParanoiaEventCatalog.manualOnlyEventIds());
        assertFalse(activePrimaryIds.contains(CORRUPT_MESSAGE));
    }

    @Test
    void activeAmbientAndSpecialIdentifiersKeepTheirDocumentedOrder() {
        assertEquals(List.of(
                        FALSE_CONTAINER_OPEN,
                        BUCKET_DRIP,
                        FURNACE_BREATH,
                        LEVER_ANSWER,
                        PRESSURE_PLATE_REPLY,
                        CAMPFIRE_COUGH,
                        TOOL_ANSWER,
                        LEAF_REPLY,
                        COLD_FURNACE,
                        STOLEN_POSE,
                        FISHING_TUG,
                        BORROWED_PAINTING,
                        CAULDRON_ECHO,
                        RETURNED_DROP,
                        MISDIRECTED_ENCHANTMENT,
                        ORPHAN_SIGNAL,
                        MAP_INTRUDER,
                        COUNTERCURRENT_COLUMN,
                        FALSE_LID),
                ids(ParanoiaEventCatalog.ambientEvents()));
        assertEquals(List.of(
                        WATCHER,
                        PULSE,
                        FOLLOWER,
                        KNOCKER,
                        HURLER,
                        USHER,
                        KEEPER,
                        TENANT,
                        STALKER,
                        SHADOW,
                        SURVEYOR,
                        MOURNER,
                        DOUBLER,
                        FERRYMAN,
                        LISTENER,
                        BYSTANDER),
                ids(ParanoiaEventCatalog.specialEvents()));
    }

    @Test
    void catalogContainsEveryCanonicalTriggerIdentifierExactlyOnce() {
        assertEquals(87, ParanoiaEventCatalog.byId().size());
        assertEquals(87, Set.copyOf(ParanoiaEventCatalog.byId().keySet()).size());
        assertTrue(ParanoiaEventCatalog.byId().keySet().stream()
                .allMatch(id -> id.matches("[a-z][a-z0-9_]*")));
        assertThrows(UnsupportedOperationException.class,
                () -> ParanoiaEventCatalog.byId().put("new_id", ParanoiaEventCatalog.require(FOOTSTEPS)));
        assertThrows(IllegalArgumentException.class, () -> ParanoiaEventCatalog.require("Footsteps"));
    }

    @Test
    void phaseDangerSeverityAndCooldownMetadataMatch111() {
        ParanoiaEventDescriptor footsteps = ParanoiaEventCatalog.require(FOOTSTEPS);
        assertEquals(1, footsteps.minimumPhase());
        assertEquals(16, footsteps.primaryWeight());
        assertEquals(ParanoiaEventSeverity.LIGHT, footsteps.severity());

        ParanoiaEventDescriptor blackout = ParanoiaEventCatalog.require(BLACKOUT);
        assertEquals(3, blackout.minimumPhase());
        assertEquals(7, blackout.primaryWeight());
        assertEquals(ParanoiaEventSeverity.EXTREME, blackout.severity());

        ParanoiaEventDescriptor usher = ParanoiaEventCatalog.require(USHER);
        assertEquals(3, usher.minimumPhase());
        assertEquals(1, usher.specialWeight());

        ParanoiaEventDescriptor stalker = ParanoiaEventCatalog.require(STALKER);
        assertEquals(2, stalker.minimumDanger());
        assertFalse(stalker.isAvailable(3, 1));
        assertTrue(stalker.isAvailable(3, 2));

        assertEquals(900, ParanoiaEventCatalog.require(ANIMAL_STARE_LOCK).eventCooldownSeconds());
        assertEquals(1200, ParanoiaEventCatalog.require(COMPASS_LIAR).eventCooldownSeconds());
        assertEquals(1800, ParanoiaEventCatalog.require(WORKBENCH_REJECT).eventCooldownSeconds());
        assertEquals(190, ParanoiaEventCatalog.require(FALSE_CONTAINER_OPEN).ambientCooldownSeconds());
        assertEquals(420, ParanoiaEventCatalog.require(FURNACE_BREATH).ambientCooldownSeconds());
        assertEquals(760, ParanoiaEventCatalog.require(TOOL_ANSWER).ambientCooldownSeconds());

        assertEquals(Set.of(
                        ORPHAN_SHADOW,
                        GHOST_BREAKING,
                        COLD_FURNACE,
                        EMPTY_TELEPORT,
                        FALSE_ANIMAL_HURT,
                        STOLEN_POSE,
                        FISHING_TUG,
                        LEAF_REPLY,
                        SILENT_BELL,
                        EMPTY_CONGREGATION,
                        EMPTY_LEAD,
                        BORROWED_PAINTING,
                        RETURNED_DROP,
                        GHOST_CART,
                        MISDIRECTED_ENCHANTMENT,
                        ORPHAN_SIGNAL,
                        CAULDRON_ECHO,
                        MAP_INTRUDER,
                        EMPTY_WAKE,
                        COUNTERCURRENT_COLUMN,
                        FALSE_SCULK_VIBRATION,
                        WATCHING_ARROW,
                        SUSPENDED_FALL,
                        BEACON_FRAGMENT,
                        STRAY_EXPERIENCE,
                        EXTRA_IN_THE_HERD,
                        LAVA_WAKE,
                        FALSE_LID,
                        SURVEYOR,
                        MOURNER,
                        DOUBLER,
                        FERRYMAN,
                        LISTENER,
                        BYSTANDER),
                ParanoiaEventCatalog.post111EventIds());
        assertEquals(Set.of(
                        ORPHAN_SHADOW, GHOST_BREAKING, COLD_FURNACE, EMPTY_TELEPORT,
                        FALSE_ANIMAL_HURT, STOLEN_POSE, FISHING_TUG, LEAF_REPLY,
                        SILENT_BELL, EMPTY_CONGREGATION, EMPTY_LEAD, BORROWED_PAINTING,
                        RETURNED_DROP, GHOST_CART, MISDIRECTED_ENCHANTMENT, ORPHAN_SIGNAL,
                        CAULDRON_ECHO, MAP_INTRUDER, EMPTY_WAKE, COUNTERCURRENT_COLUMN,
                        FALSE_SCULK_VIBRATION, WATCHING_ARROW, SUSPENDED_FALL, BEACON_FRAGMENT,
                        STRAY_EXPERIENCE, EXTRA_IN_THE_HERD, LAVA_WAKE, FALSE_LID),
                ParanoiaEventCatalog.validatedNativeEventIds());
        assertEquals(2, ParanoiaEventCatalog.require(ORPHAN_SHADOW).minimumPhase());
        assertEquals(5, ParanoiaEventCatalog.require(ORPHAN_SHADOW).primaryWeight());
        assertEquals(900, ParanoiaEventCatalog.require(ORPHAN_SHADOW).eventCooldownSeconds());
        assertEquals(4, ParanoiaEventCatalog.require(LEAF_REPLY).ambientWeight());
        assertEquals(1800, ParanoiaEventCatalog.require(SILENT_BELL).eventCooldownSeconds());
        assertEquals(ParanoiaEventSeverity.MEDIUM,
                ParanoiaEventCatalog.require(EMPTY_CONGREGATION).severity());
        assertEquals(1, ParanoiaEventCatalog.require(EMPTY_LEAD).minimumPhase());
        assertEquals(1200, ParanoiaEventCatalog.require(EMPTY_LEAD).eventCooldownSeconds());
        assertEquals(900, ParanoiaEventCatalog.require(EMPTY_WAKE).eventCooldownSeconds());
        assertEquals(1800, ParanoiaEventCatalog.require(BORROWED_PAINTING).ambientCooldownSeconds());
        assertEquals(900, ParanoiaEventCatalog.require(CAULDRON_ECHO).ambientCooldownSeconds());
        assertEquals(1200, ParanoiaEventCatalog.require(RETURNED_DROP).ambientCooldownSeconds());
        assertEquals(1800, ParanoiaEventCatalog.require(MISDIRECTED_ENCHANTMENT).ambientCooldownSeconds());
        assertEquals(2, ParanoiaEventCatalog.require(GHOST_CART).minimumPhase());
        assertEquals(1200, ParanoiaEventCatalog.require(GHOST_CART).eventCooldownSeconds());
        assertEquals(1200, ParanoiaEventCatalog.require(ORPHAN_SIGNAL).ambientCooldownSeconds());
        assertEquals(3, ParanoiaEventCatalog.require(MAP_INTRUDER).minimumPhase());
        assertEquals(1, ParanoiaEventCatalog.require(MAP_INTRUDER).ambientWeight());
        assertEquals(2400, ParanoiaEventCatalog.require(MAP_INTRUDER).ambientCooldownSeconds());
        assertEquals(1200, ParanoiaEventCatalog.require(COUNTERCURRENT_COLUMN).ambientCooldownSeconds());
        assertEquals(2400, ParanoiaEventCatalog.require(FALSE_SCULK_VIBRATION).eventCooldownSeconds());
        assertEquals(1500, ParanoiaEventCatalog.require(WATCHING_ARROW).eventCooldownSeconds());
        assertEquals(2400, ParanoiaEventCatalog.require(SUSPENDED_FALL).eventCooldownSeconds());
        assertEquals(7200, ParanoiaEventCatalog.require(BEACON_FRAGMENT).eventCooldownSeconds());
        assertEquals(1200, ParanoiaEventCatalog.require(STRAY_EXPERIENCE).eventCooldownSeconds());
        assertEquals(2400, ParanoiaEventCatalog.require(EXTRA_IN_THE_HERD).eventCooldownSeconds());
        assertEquals(1200, ParanoiaEventCatalog.require(LAVA_WAKE).eventCooldownSeconds());
        assertEquals(1800, ParanoiaEventCatalog.require(FALSE_LID).ambientCooldownSeconds());
    }

    @Test
    void phaseAvailabilityCountsAreStable() {
        assertEquals(10, availablePrimary(1));
        assertEquals(24, availablePrimary(2));
        assertEquals(37, availablePrimary(3));
        assertEquals(41, availablePrimary(4));

        assertEquals(8, availableSpecial(2, 0));
        assertEquals(15, availableSpecial(3, 0));
        assertEquals(15, availableSpecial(3, 1));
        assertEquals(16, availableSpecial(3, 2));
    }

    @Test
    void activePhaseTwoMetadataExposesTheApprovedAdditionalVariety() {
        assertEquals(2, ParanoiaEventCatalog.require(HOTBAR_WRONG_COUNT).minimumPhase());
        assertEquals(2, ParanoiaEventCatalog.require(CORRUPT_TOAST).minimumPhase());
        assertEquals(2, ParanoiaEventCatalog.require(HURLER).minimumPhase());
    }

    private static List<String> ids(List<ParanoiaEventDescriptor> descriptors) {
        return descriptors.stream().map(ParanoiaEventDescriptor::id).toList();
    }

    private static long availablePrimary(int phase) {
        return ParanoiaEventCatalog.primaryEvents().stream()
                .filter(event -> event.isAvailable(phase, 5))
                .count();
    }

    private static long availableSpecial(int phase, int danger) {
        return ParanoiaEventCatalog.specialEvents().stream()
                .filter(event -> event.isAvailable(phase, danger))
                .count();
    }
}
