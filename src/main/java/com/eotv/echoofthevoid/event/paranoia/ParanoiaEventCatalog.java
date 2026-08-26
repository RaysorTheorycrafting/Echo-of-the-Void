package com.eotv.echoofthevoid.event.paranoia;

import static com.eotv.echoofthevoid.event.paranoia.ParanoiaEventIds.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable description of shipped and post-1.1.1 event metadata.
 *
 * <p>Context probes, active tasks, effects, timers and persistence deliberately stay in
 * {@code UncannyParanoiaEventSystem}. This catalog only owns stable value data. Explicitly
 * retired events retain their historical descriptors and identifiers, but are omitted from
 * every active scheduling view.</p>
 */
public final class ParanoiaEventCatalog {
    private static final Set<String> RETIRED_EVENT_IDS = Set.of(ARMOR_BREAK, FORCED_DROP, GIANT_SUN);
    private static final Set<String> MANUAL_ONLY_EVENT_IDS = Set.of(CORRUPT_MESSAGE);
    private static final Set<String> POST_111_EVENT_IDS = Set.of(
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
            BYSTANDER);
    private static final Set<String> VALIDATED_NATIVE_EVENT_IDS = Set.of(
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
            FALSE_LID);
    private static final List<String> REFERENCE_PRIMARY_EVENT_IDS_111 = List.of(
            FOOTSTEPS,
            CORRUPT_MESSAGE,
            FLASH_RED,
            FALSE_FALL,
            ARMOR_BREAK,
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
            FORCED_DROP,
            DOOR_INVERSION,
            LIVING_ORE,
            GIANT_SUN,
            COMPASS_LIAR,
            MISPLACED_LIGHT,
            PET_REFUSAL,
            HOTBAR_WRONG_COUNT,
            CORRUPT_TOAST,
            ASPHYXIA,
            PHANTOM_HARVEST,
            PROJECTED_SHADOW,
            HUNTER_FOG);
    private static final List<String> REFERENCE_AMBIENT_EVENT_IDS_111 = List.of(
            FALSE_CONTAINER_OPEN,
            BUCKET_DRIP,
            FURNACE_BREATH,
            LEVER_ANSWER,
            PRESSURE_PLATE_REPLY,
            CAMPFIRE_COUGH,
            TOOL_ANSWER);
    private static final List<String> REFERENCE_SPECIAL_EVENT_IDS_111 = List.of(
            WATCHER,
            PULSE,
            FOLLOWER,
            KNOCKER,
            HURLER,
            USHER,
            KEEPER,
            TENANT,
            STALKER,
            SHADOW);
    private static final Map<String, ParanoiaEventDescriptor> BY_ID;
    private static final Map<String, ParanoiaEventDescriptor> REFERENCE_BY_ID_111;
    private static final List<ParanoiaEventDescriptor> PRIMARY_EVENTS;
    private static final List<ParanoiaEventDescriptor> REFERENCE_PRIMARY_EVENTS_111;
    private static final List<ParanoiaEventDescriptor> AMBIENT_EVENTS;
    private static final List<ParanoiaEventDescriptor> REFERENCE_AMBIENT_EVENTS_111;
    private static final List<ParanoiaEventDescriptor> SPECIAL_EVENTS;
    private static final List<ParanoiaEventDescriptor> REFERENCE_SPECIAL_EVENTS_111;

    static {
        LinkedHashMap<String, ParanoiaEventDescriptor> events = new LinkedHashMap<>();

        // Primary lane, in the exact order in which the 1.1.1 facade builds its weighted pool.
        add(events, event(FOOTSTEPS, "Footsteps", 1, 0, ParanoiaEventSeverity.LIGHT, 16, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(CORRUPT_MESSAGE, "Corrupt Message", 1, 0, ParanoiaEventSeverity.LIGHT, 18, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(FLASH_RED, "Flash Red", 1, 0, ParanoiaEventSeverity.MEDIUM, 8, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(FALSE_FALL, "False Fall", 1, 0, ParanoiaEventSeverity.MEDIUM, 9, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(ARMOR_BREAK, "Armor Break", 1, 0, ParanoiaEventSeverity.MEDIUM, 8, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(BASE_REPLAY, "Base Replay", 1, 0, ParanoiaEventSeverity.LIGHT, 16, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(GHOST_MINER, "Ghost Miner", 1, 0, ParanoiaEventSeverity.MEDIUM, 11, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(CAVE_COLLAPSE, "Cave Collapse", 1, 0, ParanoiaEventSeverity.MEDIUM, 8, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(BELL, "Bell", 2, 0, ParanoiaEventSeverity.MEDIUM, 14, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(VOID_SILENCE, "Void Silence", 2, 0, ParanoiaEventSeverity.HIGH, 7, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(AQUATIC_STEPS, "Aquatic Steps", 2, 0, ParanoiaEventSeverity.MEDIUM, 10, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(ANIMAL_STARE_LOCK, "Animal Stare Lock", 2, 0, ParanoiaEventSeverity.MEDIUM, 3, 0, 0, 900, 0, ParanoiaEventLane.PRIMARY, ParanoiaEventLane.CONTEXTUAL));
        add(events, event(WORKBENCH_REJECT, "Workbench Reject", 2, 0, ParanoiaEventSeverity.HIGH, 1, 0, 0, 1800, 0, ParanoiaEventLane.PRIMARY, ParanoiaEventLane.CONTEXTUAL));
        add(events, event(BLACKOUT, "Blackout", 3, 0, ParanoiaEventSeverity.EXTREME, 7, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(FLASH, "Flash Error", 3, 0, ParanoiaEventSeverity.HIGH, 4, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(FALSE_INJURY, "False Injury", 3, 0, ParanoiaEventSeverity.HIGH, 4, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(FORCED_DROP, "Forced Drop", 3, 0, ParanoiaEventSeverity.EXTREME, 2, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(DOOR_INVERSION, "Door Inversion", 3, 0, ParanoiaEventSeverity.HIGH, 9, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(LIVING_ORE, "Living Ore", 3, 0, ParanoiaEventSeverity.MEDIUM, 8, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY, ParanoiaEventLane.CONTEXTUAL));
        add(events, event(GIANT_SUN, "Giant Sun", 3, 0, ParanoiaEventSeverity.HIGH, 5, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(COMPASS_LIAR, "Compass Liar", 3, 0, ParanoiaEventSeverity.MEDIUM, 2, 0, 0, 1200, 0, ParanoiaEventLane.PRIMARY, ParanoiaEventLane.CONTEXTUAL));
        add(events, event(MISPLACED_LIGHT, "Misplaced Light", 3, 0, ParanoiaEventSeverity.MEDIUM, 3, 0, 0, 900, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(PET_REFUSAL, "Pet Refusal", 3, 0, ParanoiaEventSeverity.MEDIUM, 2, 0, 0, 1500, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(HOTBAR_WRONG_COUNT, "Hotbar Wrong Count", 3, 0, ParanoiaEventSeverity.MEDIUM, 4, 0, 0, 480, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(CORRUPT_TOAST, "Corrupt Toast", 3, 0, ParanoiaEventSeverity.MEDIUM, 2, 0, 0, 1200, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(ASPHYXIA, "Asphyxia", 4, 0, ParanoiaEventSeverity.EXTREME, 6, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(PHANTOM_HARVEST, "Phantom Harvest", 4, 0, ParanoiaEventSeverity.HIGH, 8, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(PROJECTED_SHADOW, "Projected Shadow", 4, 0, ParanoiaEventSeverity.HIGH, 8, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(HUNTER_FOG, "Hunter Fog", 4, 0, ParanoiaEventSeverity.HIGH, 7, 0, 0, 0, 0, ParanoiaEventLane.PRIMARY));

        // Minecraft-native events accepted after 1.1.1. They share the existing scheduler slots;
        // adding candidates changes composition, never the primary/ambient clocks themselves.
        add(events, event(GHOST_BREAKING, "Ghost Breaking", 1, 0, ParanoiaEventSeverity.LIGHT, 5, 0, 0, 720, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(EMPTY_TELEPORT, "Empty Teleport", 1, 0, ParanoiaEventSeverity.LIGHT, 5, 0, 0, 600, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(ORPHAN_SHADOW, "Orphan Shadow", 2, 0, ParanoiaEventSeverity.LIGHT, 5, 0, 0, 900, 0, ParanoiaEventLane.PRIMARY));
        add(events, event(FALSE_ANIMAL_HURT, "False Animal Hurt", 2, 0, ParanoiaEventSeverity.LIGHT, 3, 0, 0, 900, 0, ParanoiaEventLane.PRIMARY, ParanoiaEventLane.CONTEXTUAL));
        add(events, event(SILENT_BELL, "Silent Bell", 3, 0, ParanoiaEventSeverity.MEDIUM, 2, 0, 0, 1800, 0, ParanoiaEventLane.PRIMARY, ParanoiaEventLane.CONTEXTUAL));
        add(events, event(EMPTY_CONGREGATION, "Empty Congregation", 3, 0, ParanoiaEventSeverity.MEDIUM, 2, 0, 0, 1800, 0, ParanoiaEventLane.PRIMARY, ParanoiaEventLane.CONTEXTUAL));
        add(events, event(EMPTY_LEAD, "Empty Lead", 1, 0, ParanoiaEventSeverity.LIGHT, 3, 0, 0, 1200, 0, ParanoiaEventLane.PRIMARY, ParanoiaEventLane.CONTEXTUAL));
        add(events, event(EMPTY_WAKE, "Empty Wake", 1, 0, ParanoiaEventSeverity.LIGHT, 3, 0, 0, 900, 0, ParanoiaEventLane.PRIMARY, ParanoiaEventLane.CONTEXTUAL));
        add(events, event(GHOST_CART, "Ghost Cart", 2, 0, ParanoiaEventSeverity.LIGHT, 2, 0, 0, 1200, 0, ParanoiaEventLane.PRIMARY, ParanoiaEventLane.CONTEXTUAL));
        add(events, event(LAVA_WAKE, "Lava Wake", 2, 0, ParanoiaEventSeverity.LIGHT, 2, 0, 0, 1200, 0, ParanoiaEventLane.PRIMARY, ParanoiaEventLane.CONTEXTUAL));
        add(events, event(FALSE_SCULK_VIBRATION, "False Sculk Vibration", 3, 0, ParanoiaEventSeverity.LIGHT, 1, 0, 0, 2400, 0, ParanoiaEventLane.PRIMARY, ParanoiaEventLane.CONTEXTUAL));
        add(events, event(WATCHING_ARROW, "Watching Arrow", 2, 0, ParanoiaEventSeverity.LIGHT, 2, 0, 0, 1500, 0, ParanoiaEventLane.PRIMARY, ParanoiaEventLane.CONTEXTUAL));
        add(events, event(SUSPENDED_FALL, "Suspended Fall", 3, 0, ParanoiaEventSeverity.LIGHT, 1, 0, 0, 2400, 0, ParanoiaEventLane.PRIMARY, ParanoiaEventLane.CONTEXTUAL));
        add(events, event(BEACON_FRAGMENT, "Beacon Fragment", 3, 0, ParanoiaEventSeverity.LIGHT, 1, 0, 0, 7200, 0, ParanoiaEventLane.PRIMARY, ParanoiaEventLane.CONTEXTUAL));
        add(events, event(STRAY_EXPERIENCE, "Stray Experience", 2, 0, ParanoiaEventSeverity.LIGHT, 2, 0, 0, 1200, 0, ParanoiaEventLane.PRIMARY, ParanoiaEventLane.CONTEXTUAL));
        add(events, event(EXTRA_IN_THE_HERD, "Extra in the Herd", 2, 0, ParanoiaEventSeverity.LIGHT, 1, 0, 0, 2400, 0, ParanoiaEventLane.PRIMARY, ParanoiaEventLane.CONTEXTUAL));

        // Ambient and interaction lanes. Existing primary descriptors are merged without changing order.
        merge(events, event(FALSE_CONTAINER_OPEN, "False Container Open", 1, 0, ParanoiaEventSeverity.LIGHT, 0, 7, 0, 300, 190, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(BUCKET_DRIP, "Bucket Drip", 1, 0, ParanoiaEventSeverity.LIGHT, 0, 6, 0, 360, 210, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(FURNACE_BREATH, "Furnace Breath", 2, 0, ParanoiaEventSeverity.LIGHT, 0, 6, 0, 720, 420, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(LEVER_ANSWER, "Lever Answer", 2, 0, ParanoiaEventSeverity.LIGHT, 0, 6, 0, 300, 190, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(PRESSURE_PLATE_REPLY, "Pressure Plate Reply", 2, 0, ParanoiaEventSeverity.LIGHT, 0, 6, 0, 300, 190, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(CAMPFIRE_COUGH, "Campfire Cough", 2, 0, ParanoiaEventSeverity.LIGHT, 0, 5, 0, 480, 250, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(TOOL_ANSWER, "Tool Answer", 2, 0, ParanoiaEventSeverity.LIGHT, 0, 6, 0, 1200, 760, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(LEAF_REPLY, "Leaf Reply", 1, 0, ParanoiaEventSeverity.LIGHT, 0, 4, 0, 600, 600, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(COLD_FURNACE, "Cold Furnace", 2, 0, ParanoiaEventSeverity.LIGHT, 0, 3, 0, 900, 900, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(STOLEN_POSE, "Stolen Pose", 2, 0, ParanoiaEventSeverity.LIGHT, 0, 2, 0, 1200, 1200, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(FISHING_TUG, "Fishing Tug", 2, 0, ParanoiaEventSeverity.LIGHT, 0, 2, 0, 1200, 1200, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(BORROWED_PAINTING, "Borrowed Painting", 1, 0, ParanoiaEventSeverity.LIGHT, 0, 2, 0, 1800, 1800, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(CAULDRON_ECHO, "Cauldron Echo", 1, 0, ParanoiaEventSeverity.LIGHT, 0, 3, 0, 900, 900, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(RETURNED_DROP, "Returned Drop", 2, 0, ParanoiaEventSeverity.LIGHT, 0, 2, 0, 1200, 1200, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(MISDIRECTED_ENCHANTMENT, "Misdirected Enchantment", 2, 0, ParanoiaEventSeverity.LIGHT, 0, 2, 0, 1800, 1800, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(ORPHAN_SIGNAL, "Orphan Signal", 2, 0, ParanoiaEventSeverity.LIGHT, 0, 2, 0, 1200, 1200, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(MAP_INTRUDER, "Map Intruder", 3, 0, ParanoiaEventSeverity.LIGHT, 0, 1, 0, 2400, 2400, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(COUNTERCURRENT_COLUMN, "Countercurrent Column", 2, 0, ParanoiaEventSeverity.LIGHT, 0, 2, 0, 1200, 1200, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(FALSE_LID, "False Lid", 2, 0, ParanoiaEventSeverity.LIGHT, 0, 2, 0, 1800, 1800, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(BEDSIDE_OPEN, "Bedside Open", 2, 0, ParanoiaEventSeverity.MEDIUM, 0, 0, 0, 600, 0, ParanoiaEventLane.CONTEXTUAL));
        merge(events, event(FALSE_RECIPE_TOAST, "False Recipe Toast", 3, 0, ParanoiaEventSeverity.MEDIUM, 0, 0, 0, 1200, 0, ParanoiaEventLane.CONTEXTUAL));

        // Special lane. Phase gates reflect the actual 1.1.1 buildSpecialEntityChoices implementation.
        merge(events, event(WATCHER, "Watcher", 2, 0, ParanoiaEventSeverity.LIGHT, 0, 0, 18, 0, 0, ParanoiaEventLane.SPECIAL));
        merge(events, event(PULSE, "Pulse", 2, 0, ParanoiaEventSeverity.HIGH, 0, 0, 4, 0, 0, ParanoiaEventLane.SPECIAL));
        merge(events, event(FOLLOWER, "Follower", 2, 0, ParanoiaEventSeverity.MEDIUM, 0, 0, 8, 0, 0, ParanoiaEventLane.SPECIAL));
        merge(events, event(KNOCKER, "Knocker", 2, 0, ParanoiaEventSeverity.MEDIUM, 0, 0, 11, 0, 0, ParanoiaEventLane.SPECIAL));
        merge(events, event(HURLER, "Hurler", 3, 0, ParanoiaEventSeverity.HIGH, 0, 0, 12, 0, 0, ParanoiaEventLane.SPECIAL));
        merge(events, event(USHER, "Usher", 3, 0, ParanoiaEventSeverity.MEDIUM, 0, 0, 1, 0, 0, ParanoiaEventLane.SPECIAL));
        merge(events, event(KEEPER, "Keeper", 3, 0, ParanoiaEventSeverity.MEDIUM, 0, 0, 4, 0, 0, ParanoiaEventLane.SPECIAL));
        merge(events, event(TENANT, "Tenant", 3, 0, ParanoiaEventSeverity.MEDIUM, 0, 0, 5, 0, 0, ParanoiaEventLane.SPECIAL));
        merge(events, event(STALKER, "Stalker", 3, 2, ParanoiaEventSeverity.HIGH, 0, 0, 11, 0, 0, ParanoiaEventLane.SPECIAL));
        merge(events, event(SHADOW, "Shadow", 3, 0, ParanoiaEventSeverity.HIGH, 0, 0, 12, 0, 0, ParanoiaEventLane.SPECIAL));
        merge(events, event(SURVEYOR, "Surveyor", 2, 0, ParanoiaEventSeverity.LIGHT, 0, 0, 4, 0, 0, ParanoiaEventLane.SPECIAL));
        merge(events, event(MOURNER, "Mourner", 3, 0, ParanoiaEventSeverity.LIGHT, 0, 0, 1, 0, 0, ParanoiaEventLane.SPECIAL));
        merge(events, event(DOUBLER, "Doubler", 3, 0, ParanoiaEventSeverity.MEDIUM, 0, 0, 2, 0, 0, ParanoiaEventLane.SPECIAL));
        merge(events, event(FERRYMAN, "Ferryman", 3, 0, ParanoiaEventSeverity.MEDIUM, 0, 0, 3, 0, 0, ParanoiaEventLane.SPECIAL));
        merge(events, event(LISTENER, "Listener", 2, 0, ParanoiaEventSeverity.LIGHT, 0, 0, 3, 0, 0, ParanoiaEventLane.SPECIAL));
        merge(events, event(BYSTANDER, "Bystander", 2, 0, ParanoiaEventSeverity.LIGHT, 0, 0, 4, 0, 0, ParanoiaEventLane.SPECIAL));

        merge(events, event(GRAND_EVENT, "Grand Event", 4, 0, ParanoiaEventSeverity.EXTREME, 0, 0, 0, 0, 0, ParanoiaEventLane.CONTROL));
        merge(events, event(GRAND_EVENT_WARDEN, "Grand Event Warden", 4, 0, ParanoiaEventSeverity.EXTREME, 0, 0, 0, 0, 0, ParanoiaEventLane.CONTROL));
        merge(events, event(GRAND_EVENT_STOP, "Grand Event Stop", 1, 0, ParanoiaEventSeverity.MEDIUM, 0, 0, 0, 0, 0, ParanoiaEventLane.CONTROL));
        merge(events, event(TENSION_BUILDER_START, "Tension Builder Start", 1, 0, ParanoiaEventSeverity.MEDIUM, 0, 0, 0, 0, 0, ParanoiaEventLane.CONTROL));
        merge(events, event(TENSION_BUILDER_STOP, "Tension Builder Stop", 1, 0, ParanoiaEventSeverity.MEDIUM, 0, 0, 0, 0, 0, ParanoiaEventLane.CONTROL));

        // Freeze the shipped descriptors before applying approved work-build progression tuning.
        REFERENCE_PRIMARY_EVENTS_111 = byIds(events, REFERENCE_PRIMARY_EVENT_IDS_111);
        REFERENCE_AMBIENT_EVENTS_111 = byIds(events, REFERENCE_AMBIENT_EVENT_IDS_111);
        REFERENCE_SPECIAL_EVENTS_111 = byIds(events, REFERENCE_SPECIAL_EVENT_IDS_111);
        REFERENCE_BY_ID_111 = referenceById(
                REFERENCE_PRIMARY_EVENTS_111,
                REFERENCE_AMBIENT_EVENTS_111,
                REFERENCE_SPECIAL_EVENTS_111);

        // Phase 2 needs two additional harmless doubts and one rare context-gated release entity.
        setMinimumPhase(events, HOTBAR_WRONG_COUNT, 2);
        setMinimumPhase(events, CORRUPT_TOAST, 2);
        setMinimumPhase(events, HURLER, 2);

        BY_ID = Collections.unmodifiableMap(events);
        PRIMARY_EVENTS = byLane(events, ParanoiaEventLane.PRIMARY, false);
        AMBIENT_EVENTS = byLane(events, ParanoiaEventLane.AMBIENT, false);
        SPECIAL_EVENTS = byLane(events, ParanoiaEventLane.SPECIAL, false);
    }

    private ParanoiaEventCatalog() {
    }

    public static Map<String, ParanoiaEventDescriptor> byId() {
        return BY_ID;
    }

    public static List<ParanoiaEventDescriptor> primaryEvents() {
        return PRIMARY_EVENTS;
    }

    /** Reference-only view used to compare the active scheduler with the shipped 1.1.1 pool. */
    public static List<ParanoiaEventDescriptor> referencePrimaryEvents111() {
        return REFERENCE_PRIMARY_EVENTS_111;
    }

    public static List<ParanoiaEventDescriptor> ambientEvents() {
        return AMBIENT_EVENTS;
    }

    /** Reference-only ambient view used by the immutable 1.1.1 simulation. */
    public static List<ParanoiaEventDescriptor> referenceAmbientEvents111() {
        return REFERENCE_AMBIENT_EVENTS_111;
    }

    public static List<ParanoiaEventDescriptor> specialEvents() {
        return SPECIAL_EVENTS;
    }

    /** Reference-only special view used by the immutable 1.1.1 simulation. */
    public static List<ParanoiaEventDescriptor> referenceSpecialEvents111() {
        return REFERENCE_SPECIAL_EVENTS_111;
    }

    /** Historical identifiers intentionally kept reserved after their gameplay was retired. */
    public static Set<String> retiredEventIds() {
        return RETIRED_EVENT_IDS;
    }

    /** Debug-compatible identifiers intentionally excluded from all natural scheduling views. */
    public static Set<String> manualOnlyEventIds() {
        return MANUAL_ONLY_EVENT_IDS;
    }

    /** Identifiers introduced by the current work build and absent from the shipped 1.1.1 JAR. */
    public static Set<String> post111EventIds() {
        return POST_111_EVENT_IDS;
    }

    /** Approved Minecraft-native anomalies implemented after 1.1.1 (excluding entity Specials). */
    public static Set<String> validatedNativeEventIds() {
        return VALIDATED_NATIVE_EVENT_IDS;
    }

    public static boolean isRetired(String id) {
        return RETIRED_EVENT_IDS.contains(id);
    }

    public static ParanoiaEventDescriptor require(String id) {
        ParanoiaEventDescriptor descriptor = BY_ID.get(id);
        if (descriptor == null) {
            throw new IllegalArgumentException("Unknown paranoia event id: " + id);
        }
        return descriptor;
    }

    /** Descriptor from the immutable shipped 1.1.1 scheduling surface. */
    public static ParanoiaEventDescriptor referenceRequire111(String id) {
        ParanoiaEventDescriptor descriptor = REFERENCE_BY_ID_111.get(id);
        if (descriptor == null) {
            throw new IllegalArgumentException("Unknown 1.1.1 paranoia event id: " + id);
        }
        return descriptor;
    }

    public static ParanoiaEventSeverity severityOrDefault(String id) {
        ParanoiaEventDescriptor descriptor = BY_ID.get(id);
        return descriptor == null ? ParanoiaEventSeverity.MEDIUM : descriptor.severity();
    }

    public static boolean isSpecial(String id) {
        ParanoiaEventDescriptor descriptor = BY_ID.get(id);
        return descriptor != null && descriptor.lanes().contains(ParanoiaEventLane.SPECIAL);
    }

    private static ParanoiaEventDescriptor event(
            String id,
            String displayName,
            int minimumPhase,
            int minimumDanger,
            ParanoiaEventSeverity severity,
            int primaryWeight,
            int ambientWeight,
            int specialWeight,
            int eventCooldownSeconds,
            int ambientCooldownSeconds,
            ParanoiaEventLane... lanes) {
        return new ParanoiaEventDescriptor(
                id,
                displayName,
                minimumPhase,
                minimumDanger,
                Set.of(lanes),
                severity,
                primaryWeight,
                ambientWeight,
                specialWeight,
                eventCooldownSeconds,
                ambientCooldownSeconds);
    }

    private static void add(Map<String, ParanoiaEventDescriptor> events, ParanoiaEventDescriptor descriptor) {
        if (events.putIfAbsent(descriptor.id(), descriptor) != null) {
            throw new IllegalStateException("Duplicate paranoia event id: " + descriptor.id());
        }
    }

    private static void merge(Map<String, ParanoiaEventDescriptor> events, ParanoiaEventDescriptor descriptor) {
        ParanoiaEventDescriptor previous = events.get(descriptor.id());
        if (previous == null) {
            add(events, descriptor);
            return;
        }
        Set<ParanoiaEventLane> mergedLanes = new java.util.HashSet<>(previous.lanes());
        mergedLanes.addAll(descriptor.lanes());
        events.put(descriptor.id(), new ParanoiaEventDescriptor(
                previous.id(),
                previous.displayName(),
                Math.min(previous.minimumPhase(), descriptor.minimumPhase()),
                Math.min(previous.minimumDanger(), descriptor.minimumDanger()),
                mergedLanes,
                previous.severity(),
                Math.max(previous.primaryWeight(), descriptor.primaryWeight()),
                Math.max(previous.ambientWeight(), descriptor.ambientWeight()),
                Math.max(previous.specialWeight(), descriptor.specialWeight()),
                Math.max(previous.eventCooldownSeconds(), descriptor.eventCooldownSeconds()),
                Math.max(previous.ambientCooldownSeconds(), descriptor.ambientCooldownSeconds())));
    }

    private static void setMinimumPhase(
            Map<String, ParanoiaEventDescriptor> events,
            String eventId,
            int minimumPhase) {
        ParanoiaEventDescriptor previous = events.get(eventId);
        if (previous == null) {
            throw new IllegalStateException("Cannot tune missing paranoia event: " + eventId);
        }
        events.put(eventId, new ParanoiaEventDescriptor(
                previous.id(),
                previous.displayName(),
                minimumPhase,
                previous.minimumDanger(),
                previous.lanes(),
                previous.severity(),
                previous.primaryWeight(),
                previous.ambientWeight(),
                previous.specialWeight(),
                previous.eventCooldownSeconds(),
                previous.ambientCooldownSeconds()));
    }

    @SafeVarargs
    private static Map<String, ParanoiaEventDescriptor> referenceById(
            List<ParanoiaEventDescriptor>... descriptorLists) {
        LinkedHashMap<String, ParanoiaEventDescriptor> result = new LinkedHashMap<>();
        for (List<ParanoiaEventDescriptor> descriptors : descriptorLists) {
            for (ParanoiaEventDescriptor descriptor : descriptors) {
                result.put(descriptor.id(), descriptor);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static List<ParanoiaEventDescriptor> byLane(
            Map<String, ParanoiaEventDescriptor> events,
            ParanoiaEventLane lane,
            boolean includeRetired) {
        List<ParanoiaEventDescriptor> result = new ArrayList<>();
        for (ParanoiaEventDescriptor descriptor : events.values()) {
            if (descriptor.lanes().contains(lane)
                    && (includeRetired || !isRetired(descriptor.id()))
                    && (includeRetired || !MANUAL_ONLY_EVENT_IDS.contains(descriptor.id()))) {
                result.add(descriptor);
            }
        }
        return List.copyOf(result);
    }

    private static List<ParanoiaEventDescriptor> byIds(
            Map<String, ParanoiaEventDescriptor> events,
            List<String> ids) {
        List<ParanoiaEventDescriptor> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            ParanoiaEventDescriptor descriptor = events.get(id);
            if (descriptor == null) {
                throw new IllegalStateException("Missing catalog descriptor for reference id: " + id);
            }
            result.add(descriptor);
        }
        return List.copyOf(result);
    }
}
