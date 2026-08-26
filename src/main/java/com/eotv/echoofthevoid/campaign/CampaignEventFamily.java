package com.eotv.echoofthevoid.campaign;

import java.util.Set;

/** Coarse memory groups used to discourage obvious thematic repetition. */
public enum CampaignEventFamily {
    SOUND_TRAIL,
    FALSE_FEEDBACK,
    WORLD_CONTRADICTION,
    OBSERVATION,
    BODY_DOUBT,
    ENVIRONMENTAL_THREAT,
    PRESENCE,
    UNKNOWN;

    private static final Set<String> SOUND_TRAILS = Set.of(
            "footsteps", "ghost_breaking", "empty_teleport", "base_replay", "ghost_miner",
            "cave_collapse", "void_silence", "aquatic_steps", "ghost_cart", "lava_wake",
            "false_container_open", "bucket_drip", "furnace_breath", "campfire_cough",
            "leaf_reply", "tool_answer", "false_animal_hurt");
    private static final Set<String> FALSE_FEEDBACKS = Set.of(
            "bell", "silent_bell", "lever_answer", "pressure_plate_reply", "orphan_signal",
            "cauldron_echo", "false_lid", "misdirected_enchantment", "workbench_reject",
            "cold_furnace");
    private static final Set<String> WORLD_CONTRADICTIONS = Set.of(
            "empty_lead", "empty_wake", "borrowed_painting", "returned_drop",
            "countercurrent_column", "false_sculk_vibration", "watching_arrow",
            "suspended_fall", "beacon_fragment", "stray_experience", "extra_in_the_herd",
            "living_ore", "door_inversion", "misplaced_light", "phantom_harvest");
    private static final Set<String> OBSERVATIONS = Set.of(
            "flash_red", "orphan_shadow", "stolen_pose", "map_intruder", "projected_shadow",
            "empty_congregation", "animal_stare_lock", "pet_refusal", "compass_liar");
    private static final Set<String> BODY_DOUBTS = Set.of(
            "false_fall", "flash", "false_injury", "hotbar_wrong_count", "corrupt_toast",
            "fishing_tug", "corrupt_message");
    private static final Set<String> ENVIRONMENTAL_THREATS = Set.of(
            "blackout", "asphyxia", "hunter_fog", "forced_fallback");
    private static final Set<String> PRESENCES = Set.of(
            "watcher", "pulse", "follower", "knocker", "hurler", "usher", "keeper",
            "tenant", "stalker", "shadow", "surveyor", "mourner", "doubler", "ferryman",
            "listener", "bystander", "double_dormant");

    public static CampaignEventFamily forEvent(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return UNKNOWN;
        }
        if (SOUND_TRAILS.contains(eventId)) {
            return SOUND_TRAIL;
        }
        if (FALSE_FEEDBACKS.contains(eventId)) {
            return FALSE_FEEDBACK;
        }
        if (WORLD_CONTRADICTIONS.contains(eventId)) {
            return WORLD_CONTRADICTION;
        }
        if (OBSERVATIONS.contains(eventId)) {
            return OBSERVATION;
        }
        if (BODY_DOUBTS.contains(eventId)) {
            return BODY_DOUBT;
        }
        if (ENVIRONMENTAL_THREATS.contains(eventId)) {
            return ENVIRONMENTAL_THREAT;
        }
        return PRESENCES.contains(eventId) ? PRESENCE : UNKNOWN;
    }
}
