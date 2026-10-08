package com.eotv.echoofthevoid.entity.variant;

import com.eotv.echoofthevoid.event.passive.ApprovedVanillaVariantCatalog.BehaviorKind;
import com.eotv.echoofthevoid.event.passive.ApprovedVanillaVariantCatalog.VisualStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Five-way additive diversity for Vanilla-derived replacement classes that historically had one behavior. */
public final class ReplacementVariantExpansionCatalog {
    private static final List<Species> SPECIES = List.of(
            species("uncanny_husk", "husk", "Husk?",
                    b("parched", "Parched Walker", "Keeps the historical Husk? behavior and full Vanilla combat contract.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("late_sand", "Late Sand", "Leaves a delayed sand-like foot trace behind its real pursuit.", BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL),
                    b("sun_stare", "Sun Stare", "Hesitates briefly when watched, without gaining immunity to daylight or damage.", BehaviorKind.WATCHED_STILLNESS, VisualStyle.TALL),
                    b("broken_march", "Broken March", "Advances in an irregular, slower rhythm that never removes its attack cue.", BehaviorKind.STAGGERED_PAUSE, VisualStyle.MIRRORED),
                    b("ash_return", "Ash Return", "Revisits an earlier idle point only when it has no target.", BehaviorKind.RETURN_TO_MEMORY, VisualStyle.ASHEN)),
            species("uncanny_drowned", "drowned", "Drowned?",
                    b("undertow", "Undertow", "Pauses behind a bubble telegraph, then resumes a short Vanilla-path pursuit without forced velocity.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("wake_behind", "Wake Behind", "Leaves delayed bubbles behind its real swim path.", BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL),
                    b("watched_drift", "Watched Drift", "Briefly stalls under direct observation, making its next movement readable.", BehaviorKind.WATCHED_STILLNESS, VisualStyle.WIDE),
                    b("broken_current", "Broken Current", "Moves in short uneven bursts while preserving trident and melee rules.", BehaviorKind.STAGGERED_PAUSE, VisualStyle.MIRRORED),
                    b("shore_memory", "Shore Memory", "Returns to an old idle water point only outside combat.", BehaviorKind.RETURN_TO_MEMORY, VisualStyle.PALE)),
            species("uncanny_zombie_villager", "zombie_villager", "Zombie Villager?",
                    b("borrowed_voice", "Borrowed Voice", "Keeps the historical borrowed Villager voice and cure-compatible Vanilla body.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("late_trade", "Late Trade", "Makes a harmless interaction gesture toward empty air while idle.", BehaviorKind.FALSE_INTERACTION, VisualStyle.NORMAL),
                    b("watched_cure", "Watched Cure", "Stops briefly when examined, but never changes curing or conversion data.", BehaviorKind.WATCHED_STILLNESS, VisualStyle.TALL),
                    b("broken_routine", "Broken Routine", "Hesitates in an uneven pursuit rhythm with no added speed or damage.", BehaviorKind.STAGGERED_PAUSE, VisualStyle.MIRRORED),
                    b("village_memory", "Village Memory", "Returns to an earlier idle position as though remembering a workstation.", BehaviorKind.RETURN_TO_MEMORY, VisualStyle.ASHEN)),
            species("uncanny_stray", "stray", "Stray?",
                    b("distant_watch", "Distant Watch", "Keeps the historical distant observation and Vanilla slowing arrows.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("late_snow", "Late Snow", "Leaves one delayed snow trace behind its real route.", BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL),
                    b("watched_archer", "Watched Archer", "Pauses briefly under a direct gaze, never suppressing its bow sound.", BehaviorKind.WATCHED_STILLNESS, VisualStyle.NARROW),
                    b("fractured_advance", "Fractured Advance", "Approaches in an irregular rhythm without firing faster.", BehaviorKind.STAGGERED_PAUSE, VisualStyle.MIRRORED),
                    b("frozen_memory", "Frozen Memory", "Returns to an older idle firing position only after losing every target.", BehaviorKind.RETURN_TO_MEMORY, VisualStyle.PALE)),
            species("uncanny_endermite", "endermite", "Endermite?",
                    b("stasis_burst", "Stasis Burst", "Freezes conspicuously, then resumes ordinary pathfinding without a homing glide.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("late_scuttle", "Late Scuttle", "Leaves a delayed block trace at a point already crossed.", BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL),
                    b("watched_stasis", "Watched Stasis", "Adds a rare readable pause only while directly observed.", BehaviorKind.WATCHED_STILLNESS, VisualStyle.WIDE),
                    b("fractured_burst", "Fractured Burst", "Breaks idle movement into smaller bursts without increasing attack speed.", BehaviorKind.STAGGERED_PAUSE, VisualStyle.COMPRESSED),
                    b("wrong_end", "Wrong End", "Returns to an earlier idle point after the player has moved away.", BehaviorKind.RETURN_TO_MEMORY, VisualStyle.MIRRORED)),
            species("uncanny_ghast", "ghast", "Ghast?",
                    b("hunter", "Hunter", "Keeps the historical Ghast? pursuit and ordinary audible fireball rules.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("late_cloud", "Late Cloud", "Leaves a delayed trace in the air behind its real flight.", BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL),
                    b("watched_float", "Watched Float", "Becomes briefly still under a direct gaze before resuming its attack.", BehaviorKind.WATCHED_STILLNESS, VisualStyle.WIDE),
                    b("broken_hover", "Broken Hover", "Hesitates in the air without shortening any fireball telegraph.", BehaviorKind.STAGGERED_PAUSE, VisualStyle.COMPRESSED),
                    b("empty_horizon", "Empty Horizon", "Faces a remote empty point only after losing its target.", BehaviorKind.EMPTY_FOCUS, VisualStyle.ASHEN)),
            species("uncanny_phantom", "phantom", "Phantom?",
                    b("grounded", "Ground Trace", "Keeps Vanilla Phantom flight and leaves sparse ash below its real route.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("lantern_eater", "Lantern Eater", "Uses the validated light-seeking mode and drops every removed light normally.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("late_wing", "Late Wing", "Leaves a delayed aerial trace behind its actual dive.", BehaviorKind.DELAYED_TRACE, VisualStyle.WIDE),
                    b("watched_dive", "Watched Dive", "Suspends its approach briefly under direct observation, creating counter-play.", BehaviorKind.WATCHED_STILLNESS, VisualStyle.MIRRORED),
                    b("fractured_sky", "Fractured Sky", "Moves in short uneven beats without becoming faster or silent.", BehaviorKind.STAGGERED_PAUSE, VisualStyle.ASHEN)),
            species("uncanny_pillager", "pillager", "Pillager?",
                    b("patient_volley", "Patient Volley", "Keeps the historical telegraphed one-or-two-shot volley.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("late_boot", "Late Boot", "Leaves a delayed ground trace behind a real reposition.", BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL),
                    b("watched_crossbow", "Watched Crossbow", "Pauses briefly while directly watched and never quickens reload.", BehaviorKind.WATCHED_STILLNESS, VisualStyle.NARROW),
                    b("broken_rank", "Broken Rank", "Advances in an irregular rhythm without adding projectiles.", BehaviorKind.STAGGERED_PAUSE, VisualStyle.MIRRORED),
                    b("empty_captain", "Empty Captain", "Turns toward an absent formation point only outside combat and raids.", BehaviorKind.EMPTY_FOCUS, VisualStyle.ASHEN)),
            species("uncanny_vindicator", "vindicator", "Vindicator?",
                    b("glitch_step", "Glitch Step", "Keeps the historical bounded glitch movement and Vanilla axe threat.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("late_axe", "Late Axe", "Leaves a delayed block trace after a real step, never a second hit.", BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL),
                    b("watched_executioner", "Watched Executioner", "Freezes for a short readable beat under direct observation.", BehaviorKind.WATCHED_STILLNESS, VisualStyle.TALL),
                    b("fractured_charge", "Fractured Charge", "Breaks its advance into uneven pauses without increasing damage.", BehaviorKind.STAGGERED_PAUSE, VisualStyle.MIRRORED),
                    b("empty_door", "Empty Door", "Faces an absent doorway only after every real target is gone.", BehaviorKind.EMPTY_FOCUS, VisualStyle.ASHEN)),
            species("uncanny_evoker", "evoker", "Evoker?",
                    b("delayed_fangs", "Delayed Fangs", "Keeps the historical audible, delayed fang pattern and Vanilla spell priority.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("late_robe", "Late Robe", "Leaves one delayed ground trace behind its real movement.", BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL),
                    b("watched_spell", "Watched Spell", "Pauses briefly while watched, making its next real cast easier to read.", BehaviorKind.WATCHED_STILLNESS, VisualStyle.NARROW),
                    b("broken_cast", "Broken Cast", "Moves in irregular beats but never reduces the real spell cue.", BehaviorKind.STAGGERED_PAUSE, VisualStyle.MIRRORED),
                    b("empty_totem", "Empty Totem", "Makes one harmless gesture toward empty air only while not casting.", BehaviorKind.FALSE_INTERACTION, VisualStyle.ASHEN)),
            species("uncanny_ravager", "ravager", "Ravager?",
                    b("broken_charge", "Broken Charge", "Keeps the historical bounded body jitter and Vanilla Ravager damage.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("late_impact", "Late Impact", "Leaves delayed dust behind a real step without a second collision.", BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL),
                    b("watched_mass", "Watched Mass", "Stops briefly under direct view, giving a clear defensive window.", BehaviorKind.WATCHED_STILLNESS, VisualStyle.WIDE),
                    b("fractured_run", "Fractured Run", "Advances in heavy uneven beats without extra speed or knockback.", BehaviorKind.STAGGERED_PAUSE, VisualStyle.COMPRESSED),
                    b("empty_rider", "Empty Rider", "Looks toward its back as though a missing raider were still mounted.", BehaviorKind.EMPTY_FOCUS, VisualStyle.ASHEN)),
            species("uncanny_blaze", "blaze", "Blaze?",
                    b("vertical_fault", "Vertical Fault", "Keeps the historical vertical impulses and audible Vanilla fire attacks.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("late_ember", "Late Ember", "Leaves a delayed trace in the air behind its actual movement.", BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL),
                    b("watched_rods", "Watched Rods", "Holds still for a brief observed beat without muting a fireball cue.", BehaviorKind.WATCHED_STILLNESS, VisualStyle.TALL),
                    b("fractured_float", "Fractured Float", "Moves in short uneven intervals without increasing volley rate.", BehaviorKind.STAGGERED_PAUSE, VisualStyle.MIRRORED),
                    b("empty_heat", "Empty Heat", "Looks toward a point with no fire only after losing its target.", BehaviorKind.EMPTY_FOCUS, VisualStyle.ASHEN)),
            species("uncanny_wither_skeleton", "wither_skeleton", "Wither Skeleton?",
                    b("archer", "Archer", "Keeps the historical audible bow variant and Vanilla Wither effects.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("retreating_blade", "Retreating Blade", "Keeps the historical melee strike followed by a bounded retreat.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("late_bone", "Late Bone", "Leaves a delayed ground trace behind its real pursuit.", BehaviorKind.DELAYED_TRACE, VisualStyle.TALL),
                    b("watched_wither", "Watched Wither", "Pauses under direct observation without suppressing weapon cues.", BehaviorKind.WATCHED_STILLNESS, VisualStyle.MIRRORED),
                    b("fractured_fortress", "Fractured Fortress", "Advances in irregular beats and never gains additional damage.", BehaviorKind.STAGGERED_PAUSE, VisualStyle.ASHEN)),
            species("uncanny_piglin_brute", "piglin_brute", "Piglin Brute?",
                    b("observed_statue", "Observed Statue", "Keeps the historical gaze-bound statue behavior and Vanilla anger.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("late_gold", "Late Gold", "Leaves one delayed ground trace behind a real step.", BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL),
                    b("watched_guard", "Watched Guard", "Adds a bounded visible pause without muting the axe or hurt sounds.", BehaviorKind.WATCHED_STILLNESS, VisualStyle.WIDE),
                    b("broken_patrol", "Broken Patrol", "Moves in uneven beats while preserving every real target.", BehaviorKind.STAGGERED_PAUSE, VisualStyle.MIRRORED),
                    b("empty_bastion", "Empty Bastion", "Faces an absent guard point only outside aggression.", BehaviorKind.EMPTY_FOCUS, VisualStyle.ASHEN)),
            species("uncanny_hoglin", "hoglin", "Hoglin?",
                    b("nameless", "Nameless", "Keeps the historical hidden-name presentation and Vanilla Hoglin combat.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("late_trot", "Late Trot", "Leaves delayed dust behind its actual movement.", BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL),
                    b("watched_tusk", "Watched Tusk", "Stops briefly under observation, preserving the real charge telegraph.", BehaviorKind.WATCHED_STILLNESS, VisualStyle.WIDE),
                    b("fractured_charge", "Fractured Charge", "Advances in an irregular rhythm without extra knockback.", BehaviorKind.STAGGERED_PAUSE, VisualStyle.COMPRESSED),
                    b("empty_crimson", "Empty Crimson", "Returns to an old idle point only when no target or breeding task exists.", BehaviorKind.RETURN_TO_MEMORY, VisualStyle.ASHEN)),
            species("uncanny_slime", "slime", "Slime?",
                    b("crawler", "Ground Crawler", "Keeps its compact historical silhouette but now uses readable Vanilla jumps and splitting.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("late_slime", "Late Slime", "Leaves a delayed block trace behind its real jump path.", BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL),
                    b("watched_mass", "Watched Mass", "Stops briefly while watched and resumes without a surprise jump.", BehaviorKind.WATCHED_STILLNESS, VisualStyle.WIDE),
                    b("fractured_glide", "Fractured Glide", "Breaks its ordinary advance with readable pauses without gaining contact damage.", BehaviorKind.STAGGERED_PAUSE, VisualStyle.COMPRESSED),
                    b("empty_split", "Empty Split", "Circles an empty point only while it has no target; death splitting stays Vanilla.", BehaviorKind.CIRCLE_EMPTY, VisualStyle.PALE)),
            species("uncanny_magma_cube", "magma_cube", "Magma Cube?",
                    b("lava_crawler", "Lava Crawler", "Keeps its compact historical silhouette but now uses readable Vanilla jumps and splitting.", BehaviorKind.SPECIALIZED, VisualStyle.NORMAL),
                    b("late_magma", "Late Magma", "Leaves a delayed hot block trace behind its real jump path.", BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL),
                    b("watched_core", "Watched Core", "Stops for a short visible beat under observation.", BehaviorKind.WATCHED_STILLNESS, VisualStyle.TALL),
                    b("fractured_glide", "Fractured Glide", "Breaks its ordinary advance with readable pauses and no extra fire or damage.", BehaviorKind.STAGGERED_PAUSE, VisualStyle.COMPRESSED),
                    b("empty_lava", "Empty Lava", "Returns to an older idle position only while targetless.", BehaviorKind.RETURN_TO_MEMORY, VisualStyle.ASHEN)));

    private static final Map<String, Species> BY_ENTITY_TYPE;
    private static final Map<String, Species> BY_VANILLA_TYPE;
    private static final Map<String, Variant> BY_ID;
    private static final List<Variant> VARIANTS;

    static {
        Map<String, Species> byEntity = new LinkedHashMap<>();
        Map<String, Species> byVanilla = new LinkedHashMap<>();
        Map<String, Variant> byId = new LinkedHashMap<>();
        List<Variant> variants = new ArrayList<>();
        for (Species species : SPECIES) {
            if (species.variants().size() != 5
                    || byEntity.put(species.entityTypePath(), species) != null
                    || byVanilla.put(species.vanillaTypeKey(), species) != null) {
                throw new IllegalStateException("Invalid replacement variant species: " + species.entityTypePath());
            }
            for (int index = 0; index < species.variants().size(); index++) {
                Variant variant = species.variants().get(index);
                if (variant.index() != index + 1 || byId.put(variant.id(), variant) != null) {
                    throw new IllegalStateException("Invalid replacement variant: " + variant.id());
                }
                variants.add(variant);
            }
        }
        BY_ENTITY_TYPE = Map.copyOf(byEntity);
        BY_VANILLA_TYPE = Map.copyOf(byVanilla);
        BY_ID = Map.copyOf(byId);
        VARIANTS = List.copyOf(variants);
    }

    private ReplacementVariantExpansionCatalog() {
    }

    public static List<Species> species() {
        return SPECIES;
    }

    public static List<Variant> variants() {
        return VARIANTS;
    }

    public static Species byEntityTypePath(String path) {
        return path == null ? null : BY_ENTITY_TYPE.get(path.toLowerCase(Locale.ROOT));
    }

    public static Species byVanillaTypeKey(String key) {
        return key == null ? null : BY_VANILLA_TYPE.get(key.toLowerCase(Locale.ROOT));
    }

    public static Variant byId(String id) {
        return id == null ? null : BY_ID.get(id.toLowerCase(Locale.ROOT));
    }

    public static Variant variant(String entityTypePath, int index) {
        Species species = byEntityTypePath(entityTypePath);
        return species == null || index < 1 || index > 5 ? null : species.variants().get(index - 1);
    }

    /** Uses the same phase distribution as the established five-way passive matrix. */
    public static Variant selectVariant(String entityTypePath, int phase, long ticket) {
        Species species = byEntityTypePath(entityTypePath);
        if (species == null) {
            return null;
        }
        int roll = (int) Math.floorMod(ticket, 100L);
        int index = switch (Math.max(1, Math.min(4, phase))) {
            case 1 -> 1;
            case 2 -> roll < 68 ? 2 : 1;
            case 3 -> roll < 58 ? 3 : (roll < 86 ? 2 : 1);
            default -> roll < 32 ? 5 : (roll < 64 ? 4 : (roll < 86 ? 3 : 2));
        };
        return species.variants().get(index - 1);
    }

    private static Species species(
            String entityTypePath,
            String vanillaTypeKey,
            String displayName,
            Blueprint... blueprints) {
        List<Variant> variants = new ArrayList<>(blueprints.length);
        for (int index = 0; index < blueprints.length; index++) {
            Blueprint blueprint = blueprints[index];
            variants.add(new Variant(
                    vanillaTypeKey + "_" + blueprint.idSuffix(),
                    entityTypePath,
                    vanillaTypeKey,
                    displayName,
                    index + 1,
                    blueprint.label(),
                    blueprint.description(),
                    index == 0 ? 1 : index == 1 ? 2 : index == 2 ? 3 : 4,
                    blueprint.behaviorKind(),
                    blueprint.visualStyle()));
        }
        return new Species(entityTypePath, vanillaTypeKey, displayName, List.copyOf(variants));
    }

    private static Blueprint b(
            String idSuffix,
            String label,
            String description,
            BehaviorKind behaviorKind,
            VisualStyle visualStyle) {
        return new Blueprint(idSuffix, label, description, behaviorKind, visualStyle);
    }

    public record Species(
            String entityTypePath,
            String vanillaTypeKey,
            String displayName,
            List<Variant> variants) {

        public Species {
            variants = List.copyOf(variants);
        }
    }

    public record Variant(
            String id,
            String entityTypePath,
            String vanillaTypeKey,
            String displayName,
            int index,
            String label,
            String description,
            int minimumPhase,
            BehaviorKind behaviorKind,
            VisualStyle visualStyle) {
    }

    private record Blueprint(
            String idSuffix,
            String label,
            String description,
            BehaviorKind behaviorKind,
            VisualStyle visualStyle) {
    }
}
