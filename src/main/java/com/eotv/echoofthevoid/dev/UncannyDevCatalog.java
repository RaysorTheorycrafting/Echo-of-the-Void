package com.eotv.echoofthevoid.dev;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class UncannyDevCatalog {
    private static final Map<String, Entry> ENTRIES_BY_ID = new LinkedHashMap<>();
    private static final List<Entry> ENTRIES = new ArrayList<>();

    static {
        // Entity: specials and unique threats
        addGrouped(Category.ENTITIES, "watcher", "Watcher?", "entity_watcher_spawn", "Spawn", ActionKind.SPAWN_SPECIAL, "watcher");
        addGrouped(Category.ENTITIES, "shadow", "Shadow?", "entity_shadow_spawn", "Spawn", ActionKind.SPAWN_SPECIAL, "shadow");
        addGrouped(Category.ENTITIES, "hurler", "Hurler?", "entity_hurler_spawn", "Spawn", ActionKind.SPAWN_SPECIAL, "hurler");
        addGrouped(Category.ENTITIES, "attacker", "Attacker?", "entity_attacker_spawn", "Spawn", ActionKind.SPAWN_SPECIAL, "attacker");
        addGrouped(Category.ENTITIES, "knocker", "Knocker?", "entity_knocker_spawn", "Spawn", ActionKind.SPAWN_SPECIAL, "knocker");
        addGrouped(Category.ENTITIES, "presence", "Presence?", "entity_presence_spawn", "Spawn", ActionKind.SPAWN_SPECIAL, "pulse");
        addGrouped(Category.ENTITIES, "terror", "Terror?", "entity_terror_spawn", "Spawn", ActionKind.SPAWN_SPECIAL, "terror");
        addGrouped(Category.ENTITIES, "usher", "Usher?", "entity_usher_spawn", "Spawn", ActionKind.SPAWN_SPECIAL, "usher");
        addGrouped(Category.ENTITIES, "keeper", "Keeper?", "entity_keeper_spawn", "Spawn", ActionKind.SPAWN_SPECIAL, "keeper");
        addGrouped(Category.ENTITIES, "tenant", "Tenant?", "entity_tenant_spawn", "Spawn", ActionKind.SPAWN_SPECIAL, "tenant");
        addGrouped(Category.ENTITIES, "follower", "Follower?", "entity_follower_spawn", "Spawn", ActionKind.SPAWN_SPECIAL, "follower");

        addGrouped(Category.ENTITIES, "mimic", "Mimic", "entity_mimic_force_event", "Force Event", ActionKind.FORCE_MIMIC, "");
        addGrouped(Category.ENTITIES, "mimic", "Mimic", "entity_mimic_spawn", "Spawn Direct", ActionKind.SPAWN_UNCANNY, "uncanny_mimic");

        // Entity: uncanny mobs with variants
        addGrouped(Category.ENTITIES, "zombie", "Zombie?", "entity_zombie_spawn", "Spawn (Random)", ActionKind.SPAWN_UNCANNY, "uncanny_zombie");
        addGrouped(Category.ENTITIES, "zombie", "Zombie?", "entity_zombie_v1", "Variant 1 - Rale Fictif", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_zombie|UncannyZombieVariant|1");
        addGrouped(Category.ENTITIES, "zombie", "Zombie?", "entity_zombie_v2", "Variant 2 - Broken Neck", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_zombie|UncannyZombieVariant|2");
        addGrouped(Category.ENTITIES, "zombie", "Zombie?", "entity_zombie_v3", "Variant 3 - Bait", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_zombie|UncannyZombieVariant|3");
        addGrouped(Category.ENTITIES, "zombie", "Zombie?", "entity_zombie_v4", "Variant 4 - Desync", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_zombie|UncannyZombieVariant|4");
        addGrouped(Category.ENTITIES, "zombie", "Zombie?", "entity_zombie_v5", "Variant 5 - Render Glitch", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_zombie|UncannyZombieVariant|5");

        addGrouped(Category.ENTITIES, "skeleton", "Skeleton?", "entity_skeleton_spawn", "Spawn (Random)", ActionKind.SPAWN_UNCANNY, "uncanny_skeleton");
        addGrouped(Category.ENTITIES, "skeleton", "Skeleton?", "entity_skeleton_v1", "Variant 1 - Inverted", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_skeleton|UncannySkeletonVariant|1");
        addGrouped(Category.ENTITIES, "skeleton", "Skeleton?", "entity_skeleton_v2", "Variant 2 - Silent Sniper", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_skeleton|UncannySkeletonVariant|2");
        addGrouped(Category.ENTITIES, "skeleton", "Skeleton?", "entity_skeleton_v3", "Variant 3 - Blind Artillery", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_skeleton|UncannySkeletonVariant|3");
        addGrouped(Category.ENTITIES, "skeleton", "Skeleton?", "entity_skeleton_v4", "Variant 4 - Statue Macabre", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_skeleton|UncannySkeletonVariant|4");
        addGrouped(Category.ENTITIES, "skeleton", "Skeleton?", "entity_skeleton_v5", "Variant 5 - Sense Thief", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_skeleton|UncannySkeletonVariant|5");

        addGrouped(Category.ENTITIES, "creeper", "Creeper?", "entity_creeper_spawn", "Spawn (Random)", ActionKind.SPAWN_UNCANNY, "uncanny_creeper");
        addGrouped(Category.ENTITIES, "creeper", "Creeper?", "entity_creeper_v1", "Variant 1 - Defect", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_creeper|UncannyCreeperVariant|1");
        addGrouped(Category.ENTITIES, "creeper", "Creeper?", "entity_creeper_v2", "Variant 2 - False Alert", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_creeper|UncannyCreeperVariant|2");
        addGrouped(Category.ENTITIES, "creeper", "Creeper?", "entity_creeper_v3", "Variant 3 - Silhouette", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_creeper|UncannyCreeperVariant|3");
        addGrouped(Category.ENTITIES, "creeper", "Creeper?", "entity_creeper_v4", "Variant 4 - Ventriloquist", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_creeper|UncannyCreeperVariant|4");
        addGrouped(Category.ENTITIES, "creeper", "Creeper?", "entity_creeper_v5", "Variant 5 - Absorber", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_creeper|UncannyCreeperVariant|5");

        addGrouped(Category.ENTITIES, "spider", "Spider?", "entity_spider_spawn", "Spawn (Random)", ActionKind.SPAWN_UNCANNY, "uncanny_spider");
        addGrouped(Category.ENTITIES, "spider", "Spider?", "entity_spider_v1", "Variant 1 - Creeping Shadow", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_spider|UncannySpiderVariant|1");
        addGrouped(Category.ENTITIES, "spider", "Spider?", "entity_spider_v2", "Variant 2 - Ghost Weaver", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_spider|UncannySpiderVariant|2");
        addGrouped(Category.ENTITIES, "spider", "Spider?", "entity_spider_v3", "Variant 3 - Walking Nest", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_spider|UncannySpiderVariant|3");
        addGrouped(Category.ENTITIES, "spider", "Spider?", "entity_spider_v4", "Variant 4 - False Death", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_spider|UncannySpiderVariant|4");
        addGrouped(Category.ENTITIES, "spider", "Spider?", "entity_spider_v5", "Variant 5 - Sensorial Phobia", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_spider|UncannySpiderVariant|5");

        addGrouped(Category.ENTITIES, "enderman", "Enderman?", "entity_enderman_spawn", "Spawn (Random)", ActionKind.SPAWN_UNCANNY, "uncanny_enderman");
        addGrouped(Category.ENTITIES, "enderman", "Enderman?", "entity_enderman_v1", "Variant 1 - Sound Offset", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_enderman|UncannyEndermanVariant|1");
        addGrouped(Category.ENTITIES, "enderman", "Enderman?", "entity_enderman_v2", "Variant 2 - Light Thief", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_enderman|UncannyEndermanVariant|2");
        addGrouped(Category.ENTITIES, "enderman", "Enderman?", "entity_enderman_v3", "Variant 3 - Erratic Stray", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_enderman|UncannyEndermanVariant|3");
        addGrouped(Category.ENTITIES, "enderman", "Enderman?", "entity_enderman_v4", "Variant 4 - Latent Threat", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_enderman|UncannyEndermanVariant|4");
        addGrouped(Category.ENTITIES, "enderman", "Enderman?", "entity_enderman_v5", "Variant 5 - Spatial Anomaly", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_enderman|UncannyEndermanVariant|5");

        addGrouped(Category.ENTITIES, "wither_skeleton", "Wither Skeleton?", "entity_wither_skeleton_spawn", "Spawn (Random)", ActionKind.SPAWN_UNCANNY, "uncanny_wither_skeleton");
        addGrouped(Category.ENTITIES, "wither_skeleton", "Wither Skeleton?", "entity_wither_skeleton_archer", "Variant - Archer", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_wither_skeleton|ArcherVariant|true|bool");
        addGrouped(Category.ENTITIES, "wither_skeleton", "Wither Skeleton?", "entity_wither_skeleton_melee", "Variant - Melee", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_wither_skeleton|ArcherVariant|false|bool");

        addGrouped(Category.ENTITIES, "husk", "Husk?", "entity_husk_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_husk");
        addGrouped(Category.ENTITIES, "drowned", "Drowned?", "entity_drowned_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_drowned");
        addGrouped(Category.ENTITIES, "zombie_villager", "Zombie Villager?", "entity_zombie_villager_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_zombie_villager");
        addGrouped(Category.ENTITIES, "stray", "Stray?", "entity_stray_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_stray");
        addGrouped(Category.ENTITIES, "spiderling", "Spiderling?", "entity_spiderling_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_spiderling");
        addGrouped(Category.ENTITIES, "endermite", "Endermite?", "entity_endermite_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_endermite");
        addGrouped(Category.ENTITIES, "ghast", "Ghast?", "entity_ghast_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_ghast");
        addGrouped(Category.ENTITIES, "phantom", "Phantom?", "entity_phantom_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_phantom");
        addGrouped(Category.ENTITIES, "phantom", "Phantom?", "entity_phantom_lantern_eater", "Mode - Lantern Eater", ActionKind.TRIGGER_VARIANT, "phantom_mode|lantern_eater");
        addGrouped(Category.ENTITIES, "iron_golem", "Iron Golem?", "entity_iron_golem_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_iron_golem");
        addGrouped(Category.ENTITIES, "pillager", "Pillager?", "entity_pillager_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_pillager");
        addGrouped(Category.ENTITIES, "vindicator", "Vindicator?", "entity_vindicator_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_vindicator");
        addGrouped(Category.ENTITIES, "evoker", "Evoker?", "entity_evoker_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_evoker");
        addGrouped(Category.ENTITIES, "ravager", "Ravager?", "entity_ravager_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_ravager");
        addGrouped(Category.ENTITIES, "blaze", "Blaze?", "entity_blaze_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_blaze");
        addGrouped(Category.ENTITIES, "piglin_brute", "Piglin Brute?", "entity_piglin_brute_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_piglin_brute");
        addGrouped(Category.ENTITIES, "hoglin", "Hoglin?", "entity_hoglin_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_hoglin");
        addGrouped(Category.ENTITIES, "slime", "Slime?", "entity_slime_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_slime");
        addGrouped(Category.ENTITIES, "magma_cube", "Magma Cube?", "entity_magma_cube_spawn", "Spawn", ActionKind.SPAWN_UNCANNY, "uncanny_magma_cube");

        // Entity: passive uncanny variants
        addGrouped(Category.ENTITIES, "pig", "Pig?", "entity_pig_spawn", "Spawn (Random Variant)", ActionKind.SPAWN_PASSIVE_FORCED, "pig|0");
        addGrouped(Category.ENTITIES, "pig", "Pig?", "entity_pig_v1", "Variant 1 - Breath", ActionKind.SPAWN_PASSIVE_FORCED, "pig|1");
        addGrouped(Category.ENTITIES, "pig", "Pig?", "entity_pig_v2", "Variant 2 - Retrograde", ActionKind.SPAWN_PASSIVE_FORCED, "pig|2");
        addGrouped(Category.ENTITIES, "pig", "Pig?", "entity_pig_v3", "Variant 3 - Carcass", ActionKind.SPAWN_PASSIVE_FORCED, "pig|3");
        addGrouped(Category.ENTITIES, "pig", "Pig?", "entity_pig_v4", "Variant 4 - Insatiable", ActionKind.SPAWN_PASSIVE_FORCED, "pig|4");
        addGrouped(Category.ENTITIES, "pig", "Pig?", "entity_pig_v5", "Variant 5 - Human Scream", ActionKind.SPAWN_PASSIVE_FORCED, "pig|5");

        addGrouped(Category.ENTITIES, "cow", "Cow?", "entity_cow_spawn", "Spawn (Random Variant)", ActionKind.SPAWN_PASSIVE_FORCED, "cow|0");
        addGrouped(Category.ENTITIES, "cow", "Cow?", "entity_cow_v1", "Variant 1 - Sound Offset", ActionKind.SPAWN_PASSIVE_FORCED, "cow|1");
        addGrouped(Category.ENTITIES, "cow", "Cow?", "entity_cow_v2", "Variant 2 - Observer", ActionKind.SPAWN_PASSIVE_FORCED, "cow|2");
        addGrouped(Category.ENTITIES, "cow", "Cow?", "entity_cow_v3", "Variant 3 - Distorted Moo", ActionKind.SPAWN_PASSIVE_FORCED, "cow|3");
        addGrouped(Category.ENTITIES, "cow", "Cow?", "entity_cow_v4", "Variant 4 - False Death", ActionKind.SPAWN_PASSIVE_FORCED, "cow|4");
        addGrouped(Category.ENTITIES, "cow", "Cow?", "entity_cow_v5", "Variant 5 - Cursed Milking", ActionKind.SPAWN_PASSIVE_FORCED, "cow|5");

        addGrouped(Category.ENTITIES, "sheep", "Sheep?", "entity_sheep_spawn", "Spawn (Random Variant)", ActionKind.SPAWN_PASSIVE_FORCED, "sheep|0");
        addGrouped(Category.ENTITIES, "sheep", "Sheep?", "entity_sheep_v1", "Variant 1 - Glitched Chameleon", ActionKind.SPAWN_PASSIVE_FORCED, "sheep|1");
        addGrouped(Category.ENTITIES, "sheep", "Sheep?", "entity_sheep_v2", "Variant 2 - Jammer", ActionKind.SPAWN_PASSIVE_FORCED, "sheep|2");
        addGrouped(Category.ENTITIES, "sheep", "Sheep?", "entity_sheep_v3", "Variant 3 - Statue", ActionKind.SPAWN_PASSIVE_FORCED, "sheep|3");
        addGrouped(Category.ENTITIES, "sheep", "Sheep?", "entity_sheep_v4", "Variant 4 - Fake Flee", ActionKind.SPAWN_PASSIVE_FORCED, "sheep|4");
        addGrouped(Category.ENTITIES, "sheep", "Sheep?", "entity_sheep_v5", "Variant 5 - Flesh Wool", ActionKind.SPAWN_PASSIVE_FORCED, "sheep|5");

        addGrouped(Category.ENTITIES, "chicken", "Chicken?", "entity_chicken_spawn", "Spawn (Random Variant)", ActionKind.SPAWN_PASSIVE_FORCED, "chicken|0");
        addGrouped(Category.ENTITIES, "chicken", "Chicken?", "entity_chicken_v1", "Variant 1 - Human Steps", ActionKind.SPAWN_PASSIVE_FORCED, "chicken|1");
        addGrouped(Category.ENTITIES, "chicken", "Chicken?", "entity_chicken_v2", "Variant 2 - Broken Neck", ActionKind.SPAWN_PASSIVE_FORCED, "chicken|2");
        addGrouped(Category.ENTITIES, "chicken", "Chicken?", "entity_chicken_v3", "Variant 3 - False Egg", ActionKind.SPAWN_PASSIVE_FORCED, "chicken|3");
        addGrouped(Category.ENTITIES, "chicken", "Chicken?", "entity_chicken_v4", "Variant 4 - Glide", ActionKind.SPAWN_PASSIVE_FORCED, "chicken|4");
        addGrouped(Category.ENTITIES, "chicken", "Chicken?", "entity_chicken_v5", "Variant 5 - Alarm", ActionKind.SPAWN_PASSIVE_FORCED, "chicken|5");

        addGrouped(Category.ENTITIES, "wolf", "Wolf?", "entity_wolf_spawn", "Spawn (Random Variant)", ActionKind.SPAWN_PASSIVE_FORCED, "wolf|0");
        addGrouped(Category.ENTITIES, "wolf", "Wolf?", "entity_wolf_v1", "Variant 1 - Mute", ActionKind.SPAWN_PASSIVE_FORCED, "wolf|1");
        addGrouped(Category.ENTITIES, "wolf", "Wolf?", "entity_wolf_v2", "Variant 2 - Fake Friend", ActionKind.SPAWN_PASSIVE_FORCED, "wolf|2");
        addGrouped(Category.ENTITIES, "wolf", "Wolf?", "entity_wolf_v3", "Variant 3 - Fake Growl", ActionKind.SPAWN_PASSIVE_FORCED, "wolf|3");
        addGrouped(Category.ENTITIES, "wolf", "Wolf?", "entity_wolf_v4", "Variant 4 - Imitator", ActionKind.SPAWN_PASSIVE_FORCED, "wolf|4");
        addGrouped(Category.ENTITIES, "wolf", "Wolf?", "entity_wolf_v5", "Variant 5 - Traitor", ActionKind.SPAWN_PASSIVE_FORCED, "wolf|5");

        addGrouped(Category.ENTITIES, "cat", "Cat?", "entity_cat_spawn", "Spawn (Random Variant)", ActionKind.SPAWN_PASSIVE_FORCED, "cat|0");
        addGrouped(Category.ENTITIES, "cat", "Cat?", "entity_cat_v1", "Variant 1 - Loud Purr", ActionKind.SPAWN_PASSIVE_FORCED, "cat|1");
        addGrouped(Category.ENTITIES, "cat", "Cat?", "entity_cat_v2", "Variant 2 - Exorcist", ActionKind.SPAWN_PASSIVE_FORCED, "cat|2");
        addGrouped(Category.ENTITIES, "cat", "Cat?", "entity_cat_v3", "Variant 3 - Omen Gift", ActionKind.SPAWN_PASSIVE_FORCED, "cat|3");
        addGrouped(Category.ENTITIES, "cat", "Cat?", "entity_cat_v4", "Variant 4 - Untouchable", ActionKind.SPAWN_PASSIVE_FORCED, "cat|4");
        addGrouped(Category.ENTITIES, "cat", "Cat?", "entity_cat_v5", "Variant 5 - Sense Thief", ActionKind.SPAWN_PASSIVE_FORCED, "cat|5");

        addGrouped(Category.ENTITIES, "fox", "Fox?", "entity_fox_spawn", "Spawn (Random Variant)", ActionKind.SPAWN_PASSIVE_FORCED, "fox|0");
        addGrouped(Category.ENTITIES, "fox", "Fox?", "entity_fox_v1", "Variant 1 - Seated Shadow", ActionKind.SPAWN_PASSIVE_FORCED, "fox|1");
        addGrouped(Category.ENTITIES, "fox", "Fox?", "entity_fox_v2", "Variant 2 - Silent Marauder", ActionKind.SPAWN_PASSIVE_FORCED, "fox|2");
        addGrouped(Category.ENTITIES, "fox", "Fox?", "entity_fox_v3", "Variant 3 - Screamer", ActionKind.SPAWN_PASSIVE_FORCED, "fox|3");
        addGrouped(Category.ENTITIES, "fox", "Fox?", "entity_fox_v4", "Variant 4 - False Death", ActionKind.SPAWN_PASSIVE_FORCED, "fox|4");
        addGrouped(Category.ENTITIES, "fox", "Fox?", "entity_fox_v5", "Variant 5 - Spatial Anomaly", ActionKind.SPAWN_PASSIVE_FORCED, "fox|5");

        addGrouped(Category.ENTITIES, "squid", "Squid?", "entity_squid_spawn", "Spawn (Random Variant)", ActionKind.SPAWN_PASSIVE_FORCED, "squid|0");
        addGrouped(Category.ENTITIES, "squid", "Squid?", "entity_squid_v1", "Variant 1 - White Ink", ActionKind.SPAWN_PASSIVE_FORCED, "squid|1");
        addGrouped(Category.ENTITIES, "squid", "Squid?", "entity_squid_v2", "Variant 2 - Macabre Floater", ActionKind.SPAWN_PASSIVE_FORCED, "squid|2");
        addGrouped(Category.ENTITIES, "squid", "Squid?", "entity_squid_v3", "Variant 3 - Aquatic Offset", ActionKind.SPAWN_PASSIVE_FORCED, "squid|3");
        addGrouped(Category.ENTITIES, "squid", "Squid?", "entity_squid_v4", "Variant 4 - Drowner", ActionKind.SPAWN_PASSIVE_FORCED, "squid|4");
        addGrouped(Category.ENTITIES, "squid", "Squid?", "entity_squid_v5", "Variant 5 - Light Absorber", ActionKind.SPAWN_PASSIVE_FORCED, "squid|5");

        addGrouped(Category.ENTITIES, "cod", "Cod?", "entity_cod_spawn", "Spawn (Random Variant)", ActionKind.SPAWN_PASSIVE_FORCED, "cod|0");
        addGrouped(Category.ENTITIES, "cod", "Cod?", "entity_cod_v1", "Variant 1 - Inverted Swimmer", ActionKind.SPAWN_PASSIVE_FORCED, "cod|1");
        addGrouped(Category.ENTITIES, "cod", "Cod?", "entity_cod_v2", "Variant 2 - Static School", ActionKind.SPAWN_PASSIVE_FORCED, "cod|2");
        addGrouped(Category.ENTITIES, "cod", "Cod?", "entity_cod_v3", "Variant 3 - Illusion", ActionKind.SPAWN_PASSIVE_FORCED, "cod|3");
        addGrouped(Category.ENTITIES, "cod", "Cod?", "entity_cod_v4", "Variant 4 - Lure", ActionKind.SPAWN_PASSIVE_FORCED, "cod|4");
        addGrouped(Category.ENTITIES, "cod", "Cod?", "entity_cod_v5", "Variant 5 - Abyssal Gaze", ActionKind.SPAWN_PASSIVE_FORCED, "cod|5");

        addGrouped(Category.ENTITIES, "salmon", "Salmon?", "entity_salmon_spawn", "Spawn (Random Variant)", ActionKind.SPAWN_PASSIVE_FORCED, "salmon|0");
        addGrouped(Category.ENTITIES, "salmon", "Salmon?", "entity_salmon_v1", "Variant 1 - Inverted Swimmer", ActionKind.SPAWN_PASSIVE_FORCED, "salmon|1");
        addGrouped(Category.ENTITIES, "salmon", "Salmon?", "entity_salmon_v2", "Variant 2 - Static School", ActionKind.SPAWN_PASSIVE_FORCED, "salmon|2");
        addGrouped(Category.ENTITIES, "salmon", "Salmon?", "entity_salmon_v3", "Variant 3 - Illusion", ActionKind.SPAWN_PASSIVE_FORCED, "salmon|3");
        addGrouped(Category.ENTITIES, "salmon", "Salmon?", "entity_salmon_v4", "Variant 4 - Lure", ActionKind.SPAWN_PASSIVE_FORCED, "salmon|4");
        addGrouped(Category.ENTITIES, "salmon", "Salmon?", "entity_salmon_v5", "Variant 5 - Abyssal Gaze", ActionKind.SPAWN_PASSIVE_FORCED, "salmon|5");

        addGrouped(Category.ENTITIES, "parrot", "Parrot?", "entity_parrot_spawn", "Spawn (Random Variant)", ActionKind.SPAWN_PASSIVE_FORCED, "parrot|0");
        addGrouped(Category.ENTITIES, "parrot", "Parrot?", "entity_parrot_v1", "Variant 1 - Liar", ActionKind.SPAWN_PASSIVE_FORCED, "parrot|1");
        addGrouped(Category.ENTITIES, "parrot", "Parrot?", "entity_parrot_v2", "Variant 2 - Fake Groan", ActionKind.SPAWN_PASSIVE_FORCED, "parrot|2");
        addGrouped(Category.ENTITIES, "parrot", "Parrot?", "entity_parrot_v3", "Variant 3 - Whisperer", ActionKind.SPAWN_PASSIVE_FORCED, "parrot|3");
        addGrouped(Category.ENTITIES, "parrot", "Parrot?", "entity_parrot_v4", "Variant 4 - Macabre Statue", ActionKind.SPAWN_PASSIVE_FORCED, "parrot|4");
        addGrouped(Category.ENTITIES, "parrot", "Parrot?", "entity_parrot_v5", "Variant 5 - Alarm", ActionKind.SPAWN_PASSIVE_FORCED, "parrot|5");

        addGrouped(Category.ENTITIES, "llama", "Llama?", "entity_llama_spawn", "Spawn (Random Variant)", ActionKind.SPAWN_PASSIVE_FORCED, "llama|0");
        addGrouped(Category.ENTITIES, "llama", "Llama?", "entity_llama_v1", "Variant 1 - Ghost Spit", ActionKind.SPAWN_PASSIVE_FORCED, "llama|1");
        addGrouped(Category.ENTITIES, "llama", "Llama?", "entity_llama_v2", "Variant 2 - Twisted Head", ActionKind.SPAWN_PASSIVE_FORCED, "llama|2");
        addGrouped(Category.ENTITIES, "llama", "Llama?", "entity_llama_v3", "Variant 3 - Black Shadow", ActionKind.SPAWN_PASSIVE_FORCED, "llama|3");
        addGrouped(Category.ENTITIES, "llama", "Llama?", "entity_llama_v4", "Variant 4 - Breath", ActionKind.SPAWN_PASSIVE_FORCED, "llama|4");
        addGrouped(Category.ENTITIES, "llama", "Llama?", "entity_llama_v5", "Variant 5 - Sense Thief", ActionKind.SPAWN_PASSIVE_FORCED, "llama|5");

        addGrouped(Category.ENTITIES, "villager", "Villager?", "entity_villager_spawn", "Spawn (Random Variant)", ActionKind.SPAWN_PASSIVE_FORCED, "villager|0");
        addGrouped(Category.ENTITIES, "villager", "Villager?", "entity_villager_v1", "Variant 1 - Insomniac", ActionKind.SPAWN_PASSIVE_FORCED, "villager|1");
        addGrouped(Category.ENTITIES, "villager", "Villager?", "entity_villager_v2", "Variant 2 - False Calm", ActionKind.SPAWN_PASSIVE_FORCED, "villager|2");
        addGrouped(Category.ENTITIES, "villager", "Villager?", "entity_villager_v3", "Variant 3 - Silent Follower", ActionKind.SPAWN_PASSIVE_FORCED, "villager|3");
        addGrouped(Category.ENTITIES, "villager", "Villager?", "entity_villager_v4", "Variant 4 - Macabre Trade", ActionKind.SPAWN_PASSIVE_FORCED, "villager|4");
        addGrouped(Category.ENTITIES, "villager", "Villager?", "entity_villager_v5", "Variant 5 - Infected", ActionKind.SPAWN_PASSIVE_FORCED, "villager|5");

        addGrouped(Category.ENTITIES, "structure_villager", "Structure Villager?", "entity_structure_villager_spawn", "Spawn (Random Profile)", ActionKind.SPAWN_UNCANNY, "uncanny_structure_villager");
        addGrouped(Category.ENTITIES, "structure_villager", "Structure Villager?", "entity_structure_villager_flat", "Profile - Flat", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_structure_villager|UncannyStructureVillagerProfile|0");
        addGrouped(Category.ENTITIES, "structure_villager", "Structure Villager?", "entity_structure_villager_huge_long_wide", "Profile - Gigantic", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_structure_villager|UncannyStructureVillagerProfile|1");
        addGrouped(Category.ENTITIES, "structure_villager", "Structure Villager?", "entity_structure_villager_huge_thin", "Profile - Tall Thin", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_structure_villager|UncannyStructureVillagerProfile|2");
        addGrouped(Category.ENTITIES, "structure_villager", "Structure Villager?", "entity_structure_villager_very_wide", "Profile - Very Wide", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_structure_villager|UncannyStructureVillagerProfile|3");
        addGrouped(Category.ENTITIES, "structure_villager", "Structure Villager?", "entity_structure_villager_very_long", "Profile - Very Long", ActionKind.SPAWN_UNCANNY_FORCED, "uncanny_structure_villager|UncannyStructureVillagerProfile|4");

        addGrouped(Category.ENTITIES, "wandering_trader", "Wandering Trader?", "entity_trader_spawn", "Spawn (Random Variant)", ActionKind.SPAWN_PASSIVE_FORCED, "wandering_trader|0");
        addGrouped(Category.ENTITIES, "wandering_trader", "Wandering Trader?", "entity_trader_v1", "Variant 1 - Invisible", ActionKind.SPAWN_PASSIVE_FORCED, "wandering_trader|1");
        addGrouped(Category.ENTITIES, "wandering_trader", "Wandering Trader?", "entity_trader_v2", "Variant 2 - Void Seller", ActionKind.SPAWN_PASSIVE_FORCED, "wandering_trader|2");
        addGrouped(Category.ENTITIES, "wandering_trader", "Wandering Trader?", "entity_trader_v3", "Variant 3 - Fake Groan", ActionKind.SPAWN_PASSIVE_FORCED, "wandering_trader|3");
        addGrouped(Category.ENTITIES, "wandering_trader", "Wandering Trader?", "entity_trader_v4", "Variant 4 - Trap", ActionKind.SPAWN_PASSIVE_FORCED, "wandering_trader|4");
        addGrouped(Category.ENTITIES, "wandering_trader", "Wandering Trader?", "entity_trader_v5", "Variant 5 - Messenger", ActionKind.SPAWN_PASSIVE_FORCED, "wandering_trader|5");

        // Events
        addGrouped(Category.EVENTS, "blackout", "Total Blackout", "event_blackout", "Trigger", ActionKind.TRIGGER_EVENT, "blackout");
        addGrouped(Category.EVENTS, "footsteps", "Footsteps", "event_footsteps", "Trigger (Random)", ActionKind.TRIGGER_EVENT, "footsteps");
        addGrouped(Category.EVENTS, "footsteps", "Footsteps", "event_footsteps_basic", "Variant - Basic", ActionKind.TRIGGER_VARIANT, "footsteps|basic");
        addGrouped(Category.EVENTS, "footsteps", "Footsteps", "event_footsteps_echo", "Variant - Echo", ActionKind.TRIGGER_VARIANT, "footsteps|echo");
        addGrouped(Category.EVENTS, "footsteps", "Footsteps", "event_footsteps_sprint", "Variant - Sprint", ActionKind.TRIGGER_VARIANT, "footsteps|sprint");
        addGrouped(Category.EVENTS, "footsteps", "Footsteps", "event_footsteps_heavy", "Variant - Heavy", ActionKind.TRIGGER_VARIANT, "footsteps|heavy");
        addGrouped(Category.EVENTS, "footsteps", "Footsteps", "event_footsteps_ladder_steps", "Variant - Ladder Steps", ActionKind.TRIGGER_VARIANT, "footsteps|ladder_steps");

        addGrouped(Category.EVENTS, "flash_error", "Flash Error", "event_flash_error", "Trigger", ActionKind.TRIGGER_EVENT, "flash");
        addGrouped(Category.EVENTS, "base_replay", "Base Replay", "event_base_replay", "Trigger", ActionKind.TRIGGER_EVENT, "base_replay");
        addGrouped(Category.EVENTS, "bell", "Bell", "event_bell", "Trigger", ActionKind.TRIGGER_EVENT, "bell");
        addGrouped(Category.EVENTS, "flash_red", "Flash Red", "event_flash_red", "Trigger", ActionKind.TRIGGER_EVENT, "flash_red");
        addGrouped(Category.EVENTS, "void_silence", "Void Silence", "event_void_silence", "Trigger", ActionKind.TRIGGER_EVENT, "void_silence");
        addGrouped(Category.EVENTS, "false_fall", "False Fall", "event_false_fall", "Trigger", ActionKind.TRIGGER_EVENT, "false_fall");
        addGrouped(Category.EVENTS, "ghost_miner", "Ghost Miner", "event_ghost_miner", "Trigger", ActionKind.TRIGGER_EVENT, "ghost_miner");
        addGrouped(Category.EVENTS, "cave_collapse", "Cave Collapse", "event_cave_collapse", "Trigger", ActionKind.TRIGGER_EVENT, "cave_collapse");
        addGrouped(Category.EVENTS, "false_injury", "False Injury", "event_false_injury", "Trigger", ActionKind.TRIGGER_EVENT, "false_injury");
        addGrouped(Category.EVENTS, "forced_drop", "Forced Drop", "event_forced_drop", "Trigger", ActionKind.TRIGGER_EVENT, "forced_drop");
        addGrouped(Category.EVENTS, "corrupt_message", "Corrupt Message", "event_corrupt_message", "Trigger", ActionKind.TRIGGER_EVENT, "corrupt_message");
        addGrouped(Category.EVENTS, "bed", "Bed Disturbance", "event_bed_disturbance", "Trigger", ActionKind.TRIGGER_EVENT, "bed");
        addGrouped(Category.EVENTS, "animal_stare_lock", "Animal Stare Lock", "event_animal_stare_lock", "Trigger", ActionKind.TRIGGER_EVENT, "animal_stare_lock");
        addGrouped(Category.EVENTS, "bedside_open", "Bedside Open", "event_bedside_open", "Trigger", ActionKind.TRIGGER_EVENT, "bedside_open");
        addGrouped(Category.EVENTS, "compass_liar", "Compass Liar", "event_compass_liar", "Trigger", ActionKind.TRIGGER_EVENT, "compass_liar");
        addGrouped(Category.EVENTS, "compass_liar", "Compass Liar", "event_compass_liar_give_uncanny_compass", "Give Uncanny Compass", ActionKind.GIVE_ITEM, "uncanny_compass");
        addGrouped(Category.EVENTS, "furnace_breath", "Furnace Breath", "event_furnace_breath", "Trigger", ActionKind.TRIGGER_EVENT, "furnace_breath");
        addGrouped(Category.EVENTS, "misplaced_light", "Misplaced Light", "event_misplaced_light", "Trigger", ActionKind.TRIGGER_EVENT, "misplaced_light");
        addGrouped(Category.EVENTS, "pet_refusal", "Pet Refusal", "event_pet_refusal", "Trigger", ActionKind.TRIGGER_EVENT, "pet_refusal");
        addGrouped(Category.EVENTS, "workbench_reject", "Workbench Reject", "event_workbench_reject", "Trigger", ActionKind.TRIGGER_EVENT, "workbench_reject");
        addGrouped(Category.EVENTS, "false_container_open", "False Container Open", "event_false_container_open", "Trigger", ActionKind.TRIGGER_EVENT, "false_container_open");
        addGrouped(Category.EVENTS, "lever_answer", "Lever Answer", "event_lever_answer", "Trigger", ActionKind.TRIGGER_EVENT, "lever_answer");
        addGrouped(Category.EVENTS, "pressure_plate_reply", "Pressure Plate Reply", "event_pressure_plate_reply", "Trigger", ActionKind.TRIGGER_EVENT, "pressure_plate_reply");
        addGrouped(Category.EVENTS, "campfire_cough", "Campfire Cough", "event_campfire_cough", "Trigger", ActionKind.TRIGGER_EVENT, "campfire_cough");
        addGrouped(Category.EVENTS, "bucket_drip", "Bucket Drip", "event_bucket_drip", "Trigger", ActionKind.TRIGGER_EVENT, "bucket_drip");
        addGrouped(Category.EVENTS, "hotbar_wrong_count", "Hotbar Wrong Count", "event_hotbar_wrong_count", "Trigger", ActionKind.TRIGGER_EVENT, "hotbar_wrong_count");
        addGrouped(Category.EVENTS, "corrupt_toast", "Corrupt Toast", "event_corrupt_toast", "Trigger", ActionKind.TRIGGER_EVENT, "corrupt_toast");
        addGrouped(Category.EVENTS, "tool_answer", "Tool Answer", "event_tool_answer", "Trigger", ActionKind.TRIGGER_EVENT, "tool_answer");

        addGrouped(Category.EVENTS, "asphyxia", "Asphyxia", "event_asphyxia", "Trigger (Random)", ActionKind.TRIGGER_EVENT, "asphyxia");
        addGrouped(Category.EVENTS, "asphyxia", "Asphyxia", "event_asphyxia_false_alert", "Variant - False Alert", ActionKind.TRIGGER_VARIANT, "asphyxia|false_alert");
        addGrouped(Category.EVENTS, "asphyxia", "Asphyxia", "event_asphyxia_terrain_drowning", "Variant - Terrain Drowning", ActionKind.TRIGGER_VARIANT, "asphyxia|terrain_drowning");
        addGrouped(Category.EVENTS, "asphyxia", "Asphyxia", "event_asphyxia_heavy_lungs", "Variant - Heavy Lungs", ActionKind.TRIGGER_VARIANT, "asphyxia|heavy_lungs");

        addGrouped(Category.EVENTS, "armor_break", "Armor Break", "event_armor_break", "Trigger (Random)", ActionKind.TRIGGER_EVENT, "armor_break");
        addGrouped(Category.EVENTS, "armor_break", "Armor Break", "event_armor_break_ghost_sound", "Variant - Ghost Sound", ActionKind.TRIGGER_VARIANT, "armor_break|ghost_sound");
        addGrouped(Category.EVENTS, "armor_break", "Armor Break", "event_armor_break_drop_gear", "Variant - Drop Gear", ActionKind.TRIGGER_VARIANT, "armor_break|drop_gear");
        addGrouped(Category.EVENTS, "armor_break", "Armor Break", "event_armor_break_cracked_defense", "Variant - Cracked Defense", ActionKind.TRIGGER_VARIANT, "armor_break|cracked_defense");

        addGrouped(Category.EVENTS, "aquatic_steps", "Aquatic Steps", "event_aquatic_steps", "Trigger (Random)", ActionKind.TRIGGER_EVENT, "aquatic_steps");
        addGrouped(Category.EVENTS, "aquatic_steps", "Aquatic Steps", "event_aquatic_steps_follower", "Variant - Follower", ActionKind.TRIGGER_VARIANT, "aquatic_steps|follower");
        addGrouped(Category.EVENTS, "aquatic_steps", "Aquatic Steps", "event_aquatic_steps_slippery_ambush", "Variant - Slippery Ambush", ActionKind.TRIGGER_VARIANT, "aquatic_steps|slippery_ambush");
        addGrouped(Category.EVENTS, "aquatic_steps", "Aquatic Steps", "event_aquatic_steps_invisible_bite", "Variant - Invisible Bite", ActionKind.TRIGGER_VARIANT, "aquatic_steps|invisible_bite");

        addGrouped(Category.EVENTS, "door_inversion", "Door Inversion", "event_door_inversion", "Trigger (Random)", ActionKind.TRIGGER_EVENT, "door_inversion");
        addGrouped(Category.EVENTS, "door_inversion", "Door Inversion", "event_door_inversion_poltergeist", "Variant - Poltergeist", ActionKind.TRIGGER_VARIANT, "door_inversion|poltergeist");
        addGrouped(Category.EVENTS, "door_inversion", "Door Inversion", "event_door_inversion_lockdown", "Variant - Lockdown", ActionKind.TRIGGER_VARIANT, "door_inversion|lockdown");
        addGrouped(Category.EVENTS, "door_inversion", "Door Inversion", "event_door_inversion_intrusion", "Variant - Intrusion", ActionKind.TRIGGER_VARIANT, "door_inversion|intrusion");
        addGrouped(Category.EVENTS, "door_inversion", "Door Inversion", "event_door_inversion_door_trap_cascade", "Variant - Door Trap Cascade", ActionKind.TRIGGER_VARIANT, "door_inversion|door_trap_cascade");

        addGrouped(Category.EVENTS, "phantom_harvest", "Phantom Harvest", "event_phantom_harvest", "Trigger (Random)", ActionKind.TRIGGER_EVENT, "phantom_harvest");
        addGrouped(Category.EVENTS, "phantom_harvest", "Phantom Harvest", "event_phantom_harvest_black_harvest", "Variant - Black Harvest", ActionKind.TRIGGER_VARIANT, "phantom_harvest|black_harvest");
        addGrouped(Category.EVENTS, "phantom_harvest", "Phantom Harvest", "event_phantom_harvest_rotten_soil", "Variant - Rotten Soil", ActionKind.TRIGGER_VARIANT, "phantom_harvest|rotten_soil");
        addGrouped(Category.EVENTS, "phantom_harvest", "Phantom Harvest", "event_phantom_harvest_infestation", "Variant - Infestation", ActionKind.TRIGGER_VARIANT, "phantom_harvest|infestation");

        addGrouped(Category.EVENTS, "living_ore", "Living Ore", "event_living_ore", "Trigger (Random)", ActionKind.TRIGGER_EVENT, "living_ore");
        addGrouped(Category.EVENTS, "living_ore", "Living Ore", "event_living_ore_bleeding", "Variant - Bleeding", ActionKind.TRIGGER_VARIANT, "living_ore|bleeding");
        addGrouped(Category.EVENTS, "living_ore", "Living Ore", "event_living_ore_toxic_blood", "Variant - Toxic Blood", ActionKind.TRIGGER_VARIANT, "living_ore|toxic_blood");
        addGrouped(Category.EVENTS, "living_ore", "Living Ore", "event_living_ore_vicious_fall", "Variant - Vicious Fall", ActionKind.TRIGGER_VARIANT, "living_ore|vicious_fall");
        addGrouped(Category.EVENTS, "living_ore", "Living Ore", "event_living_ore_vein_retreat", "Variant - Vein Retreat", ActionKind.TRIGGER_VARIANT, "living_ore|vein_retreat");
        addGrouped(Category.EVENTS, "living_ore", "Living Ore", "event_living_ore_inside_knock", "Variant - Inside Knock", ActionKind.TRIGGER_VARIANT, "living_ore|inside_knock");

        addGrouped(Category.EVENTS, "projected_shadow", "Projected Shadow", "event_projected_shadow", "Trigger (Random)", ActionKind.TRIGGER_EVENT, "projected_shadow");
        addGrouped(Category.EVENTS, "projected_shadow", "Projected Shadow", "event_projected_shadow_mime", "Variant - Mime", ActionKind.TRIGGER_VARIANT, "projected_shadow|mime");
        addGrouped(Category.EVENTS, "projected_shadow", "Projected Shadow", "event_projected_shadow_shadow_assault", "Variant - Shadow Assault", ActionKind.TRIGGER_VARIANT, "projected_shadow|shadow_assault");
        addGrouped(Category.EVENTS, "projected_shadow", "Projected Shadow", "event_projected_shadow_ghost_shot", "Variant - Ghost Shot", ActionKind.TRIGGER_VARIANT, "projected_shadow|ghost_shot");

        addGrouped(Category.EVENTS, "giant_sun", "Giant Sun", "event_giant_sun", "Trigger", ActionKind.TRIGGER_EVENT, "giant_sun");
        addGrouped(Category.EVENTS, "hunter_fog", "Hunter Fog", "event_hunter_fog", "Trigger", ActionKind.TRIGGER_EVENT, "hunter_fog");
        addGrouped(Category.EVENTS, "grand_event", "Grand Event", "event_grand_event_warden", "Trigger - Warden", ActionKind.TRIGGER_EVENT, "grand_event_warden");
        addGrouped(Category.EVENTS, "grand_event", "Grand Event", "event_grand_event_stop", "Force Stop", ActionKind.TRIGGER_EVENT, "grand_event_stop");
        addGrouped(Category.EVENTS, "tension_builder", "Tension Builder", "event_tension_builder_start", "Start Cycle Window", ActionKind.TRIGGER_EVENT, "tension_builder_start");
        addGrouped(Category.EVENTS, "tension_builder", "Tension Builder", "event_tension_builder_stop", "Stop / Break Window", ActionKind.TRIGGER_EVENT, "tension_builder_stop");

        // Weather
        addGrouped(Category.WEATHER, "rain", "Rain", "weather_rain_silent", "Silent Rain", ActionKind.TRIGGER_WEATHER, "rain_silent");
        addGrouped(Category.WEATHER, "rain", "Rain", "weather_rain_dry_storm", "Dry Storm", ActionKind.TRIGGER_WEATHER, "rain_dry_storm");
        addGrouped(Category.WEATHER, "rain", "Rain", "weather_rain_ash", "Ash Rain", ActionKind.TRIGGER_WEATHER, "rain_ash");
        addGrouped(Category.WEATHER, "rain", "Rain", "weather_rain_sobbing", "Sobbing Sky", ActionKind.TRIGGER_WEATHER, "rain_sobbing");

        addGrouped(Category.WEATHER, "thunder", "Thunder", "weather_thunder_silent", "Silent Thunder", ActionKind.TRIGGER_WEATHER, "thunder_silent");
        addGrouped(Category.WEATHER, "thunder", "Thunder", "weather_thunder_artificial", "Artificial Thunder", ActionKind.TRIGGER_WEATHER, "thunder_artificial");
        addGrouped(Category.WEATHER, "thunder", "Thunder", "weather_thunder_target_strike", "Target Strike", ActionKind.TRIGGER_WEATHER, "thunder_target_strike");
        addGrouped(Category.WEATHER, "thunder", "Thunder", "weather_thunder_stroboscopic", "Stroboscopic Storm", ActionKind.TRIGGER_WEATHER, "thunder_stroboscopic");

        addGrouped(Category.WEATHER, "fog", "Fog", "weather_fog_breathing", "Breathing Fog", ActionKind.TRIGGER_WEATHER, "fog_breathing");
        addGrouped(Category.WEATHER, "fog", "Fog", "weather_fog_black", "Black Fog", ActionKind.TRIGGER_WEATHER, "fog_black");
        addGrouped(Category.WEATHER, "fog", "Fog", "weather_fog_static_wall", "Static Wall", ActionKind.TRIGGER_WEATHER, "fog_static_wall");

        addGrouped(Category.WEATHER, "sky", "Sky", "weather_sky_fake_morning", "Fake Morning", ActionKind.TRIGGER_WEATHER, "sky_fake_morning");
        addGrouped(Category.WEATHER, "sky", "Sky", "weather_sky_empty", "Empty Sky", ActionKind.TRIGGER_WEATHER, "sky_empty");
        addGrouped(Category.WEATHER, "sky", "Sky", "weather_sky_pressure", "Atmospheric Pressure", ActionKind.TRIGGER_WEATHER, "sky_pressure");

        addGrouped(Category.WEATHER, "control", "Control", "weather_stop", "Stop Active", ActionKind.STOP_WEATHER, "");

        // Structures / world features (grouped by family with variants)
        addGrouped(Category.STRUCTURES, "anechoic_cube", "Anechoic Cube", "structure_anechoic_cube_random", "Generate (Random)", ActionKind.TRIGGER_STRUCTURE, "anechoic_cube");
        addGrouped(Category.STRUCTURES, "mimic_shelter", "Mimic Shelter", "structure_mimic_shelter_random", "Generate (Random)", ActionKind.TRIGGER_STRUCTURE, "mimic_shelter");
        addGrouped(Category.STRUCTURES, "mimic_shelter", "Mimic Shelter", "structure_mimic_shelter_classic", "Variant - Classic", ActionKind.TRIGGER_STRUCTURE, "mimic_shelter|classic");
        addGrouped(Category.STRUCTURES, "mimic_shelter", "Mimic Shelter", "structure_mimic_shelter_bed_wall", "Variant - Bed Into Wall", ActionKind.TRIGGER_STRUCTURE, "mimic_shelter|bed_wall");
        addGrouped(Category.STRUCTURES, "mimic_shelter", "Mimic Shelter", "structure_mimic_shelter_furnace_blocker", "Variant - Furnace Blocker", ActionKind.TRIGGER_STRUCTURE, "mimic_shelter|furnace_blocker");
        addGrouped(Category.STRUCTURES, "mimic_shelter", "Mimic Shelter", "structure_mimic_shelter_side_door", "Variant - Side Door", ActionKind.TRIGGER_STRUCTURE, "mimic_shelter|side_door");
        addGrouped(Category.STRUCTURES, "mimic_shelter", "Mimic Shelter", "structure_mimic_shelter_lore_corner", "Variant - Lore Corner", ActionKind.TRIGGER_STRUCTURE, "mimic_shelter|lore_corner");
        addGrouped(Category.STRUCTURES, "mimic_shelter", "Mimic Shelter", "structure_mimic_shelter_lore_clutter", "Variant - Lore Clutter", ActionKind.TRIGGER_STRUCTURE, "mimic_shelter|lore_clutter");
        addGrouped(Category.STRUCTURES, "glitched_shelter", "Glitched Shelter", "structure_glitched_shelter_random", "Generate (Random)", ActionKind.TRIGGER_STRUCTURE, "glitched_shelter");
        addGrouped(Category.STRUCTURES, "patterned_grove", "Patterned Grove", "structure_patterned_grove_random", "Generate (Random)", ActionKind.TRIGGER_STRUCTURE, "patterned_grove");
        addGrouped(Category.STRUCTURES, "barren_grid", "Barren Grid", "structure_barren_grid_random", "Generate (Random)", ActionKind.TRIGGER_STRUCTURE, "barren_grid");
        addGrouped(Category.STRUCTURES, "false_descent", "False Descent", "structure_false_descent_random", "Generate (Random)", ActionKind.TRIGGER_STRUCTURE, "false_descent");
        addGrouped(Category.STRUCTURES, "false_descent", "False Descent", "structure_false_descent_with_house", "Generate (With House)", ActionKind.TRIGGER_STRUCTURE, "false_descent_with_house");
        addGrouped(Category.STRUCTURES, "false_ascent", "False Ascent", "structure_false_ascent_random", "Generate (Random)", ActionKind.TRIGGER_STRUCTURE, "false_ascent");
        addGrouped(Category.STRUCTURES, "false_ascent", "False Ascent", "structure_false_ascent_with_house", "Generate (With House)", ActionKind.TRIGGER_STRUCTURE, "false_ascent_with_house");
        addGrouped(Category.STRUCTURES, "isolation_cube", "Isolation Cube", "structure_isolation_cube_random", "Generate (Random)", ActionKind.TRIGGER_STRUCTURE, "isolation_cube");

        addGrouped(Category.STRUCTURES, "bell_shrine", "Bell Shrine", "structure_bell_shrine_random", "Generate (Random Variant)", ActionKind.TRIGGER_STRUCTURE, "bell_shrine");
        addGrouped(Category.STRUCTURES, "bell_shrine", "Bell Shrine", "structure_bell_shrine_open", "Variant - Open", ActionKind.TRIGGER_STRUCTURE, "bell_shrine|open");
        addGrouped(Category.STRUCTURES, "bell_shrine", "Bell Shrine", "structure_bell_shrine_ruined", "Variant - Ruined", ActionKind.TRIGGER_STRUCTURE, "bell_shrine|ruined");
        addGrouped(Category.STRUCTURES, "bell_shrine", "Bell Shrine", "structure_bell_shrine_closed", "Variant - Closed", ActionKind.TRIGGER_STRUCTURE, "bell_shrine|closed");
        addGrouped(Category.STRUCTURES, "bell_shrine", "Bell Shrine", "structure_bell_shrine_hilltop", "Variant - Hilltop", ActionKind.TRIGGER_STRUCTURE, "bell_shrine|hilltop");
        addGrouped(Category.STRUCTURES, "bell_shrine", "Bell Shrine", "structure_bell_shrine_forest", "Variant - Forest", ActionKind.TRIGGER_STRUCTURE, "bell_shrine|forest");
        addGrouped(Category.STRUCTURES, "bell_shrine", "Bell Shrine", "structure_bell_shrine_buried", "Variant - Buried", ActionKind.TRIGGER_STRUCTURE, "bell_shrine|buried");

        addGrouped(Category.STRUCTURES, "watching_tower", "Watching Tower", "structure_watching_tower_random", "Generate (Random Variant)", ActionKind.TRIGGER_STRUCTURE, "watching_tower");
        addGrouped(Category.STRUCTURES, "watching_tower", "Watching Tower", "structure_watching_tower_wooden", "Variant - Wooden", ActionKind.TRIGGER_STRUCTURE, "watching_tower|wooden");
        addGrouped(Category.STRUCTURES, "watching_tower", "Watching Tower", "structure_watching_tower_stone", "Variant - Stone", ActionKind.TRIGGER_STRUCTURE, "watching_tower|stone");
        addGrouped(Category.STRUCTURES, "watching_tower", "Watching Tower", "structure_watching_tower_broken", "Variant - Broken", ActionKind.TRIGGER_STRUCTURE, "watching_tower|broken");
        addGrouped(Category.STRUCTURES, "watching_tower", "Watching Tower", "structure_watching_tower_overbuilt", "Variant - Overbuilt", ActionKind.TRIGGER_STRUCTURE, "watching_tower|overbuilt");
        addGrouped(Category.STRUCTURES, "watching_tower", "Watching Tower", "structure_watching_tower_twin", "Variant - Twin Tower Site", ActionKind.TRIGGER_STRUCTURE, "watching_tower|twin");
        addGrouped(Category.STRUCTURES, "watching_tower", "Watching Tower", "structure_watching_tower_closed", "Variant - Closed", ActionKind.TRIGGER_STRUCTURE, "watching_tower|closed");

        addGrouped(Category.STRUCTURES, "false_camp", "False Camp", "structure_false_camp_random", "Generate (Random Variant)", ActionKind.TRIGGER_STRUCTURE, "false_camp");
        addGrouped(Category.STRUCTURES, "false_camp", "False Camp", "structure_false_camp_fresh", "Variant - Fresh Camp", ActionKind.TRIGGER_STRUCTURE, "false_camp|fresh");
        addGrouped(Category.STRUCTURES, "false_camp", "False Camp", "structure_false_camp_looted", "Variant - Looted Camp", ActionKind.TRIGGER_STRUCTURE, "false_camp|looted");
        addGrouped(Category.STRUCTURES, "false_camp", "False Camp", "structure_false_camp_interrupted", "Variant - Interrupted Camp", ActionKind.TRIGGER_STRUCTURE, "false_camp|interrupted");
        addGrouped(Category.STRUCTURES, "false_camp", "False Camp", "structure_false_camp_minimal", "Variant - Minimal Camp", ActionKind.TRIGGER_STRUCTURE, "false_camp|minimal");
        addGrouped(Category.STRUCTURES, "false_camp", "False Camp", "structure_false_camp_travel", "Variant - Travel Camp", ActionKind.TRIGGER_STRUCTURE, "false_camp|travel");
        addGrouped(Category.STRUCTURES, "false_camp", "False Camp", "structure_false_camp_water", "Variant - Camp by Water", ActionKind.TRIGGER_STRUCTURE, "false_camp|water");
        addGrouped(Category.STRUCTURES, "false_camp", "False Camp", "structure_false_camp_under_cliff", "Variant - Camp Under Cliff", ActionKind.TRIGGER_STRUCTURE, "false_camp|under_cliff");

        addGrouped(Category.STRUCTURES, "wrong_village_house", "Wrong Village House", "structure_wrong_village_house_random", "Generate (Random Variant)", ActionKind.TRIGGER_STRUCTURE, "wrong_village_house");
        addGrouped(Category.STRUCTURES, "wrong_village_house", "Wrong Village House", "structure_wrong_village_house_too_narrow", "Variant - Too Narrow", ActionKind.TRIGGER_STRUCTURE, "wrong_village_house|too_narrow");
        addGrouped(Category.STRUCTURES, "wrong_village_house", "Wrong Village House", "structure_wrong_village_house_too_tall", "Variant - Too Tall", ActionKind.TRIGGER_STRUCTURE, "wrong_village_house|too_tall");
        addGrouped(Category.STRUCTURES, "wrong_village_house", "Wrong Village House", "structure_wrong_village_house_too_wide", "Variant - Too Wide", ActionKind.TRIGGER_STRUCTURE, "wrong_village_house|too_wide");
        addGrouped(Category.STRUCTURES, "wrong_village_house", "Wrong Village House", "structure_wrong_village_house_long", "Variant - Long", ActionKind.TRIGGER_STRUCTURE, "wrong_village_house|long");
        addGrouped(Category.STRUCTURES, "wrong_village_house", "Wrong Village House", "structure_wrong_village_house_flat", "Variant - Flat", ActionKind.TRIGGER_STRUCTURE, "wrong_village_house|flat");
        addGrouped(Category.STRUCTURES, "wrong_village_house", "Wrong Village House", "structure_wrong_village_house_offset", "Variant - Offset", ActionKind.TRIGGER_STRUCTURE, "wrong_village_house|offset");
        addGrouped(Category.STRUCTURES, "wrong_village_house", "Wrong Village House", "structure_wrong_village_house_bent", "Variant - Bent", ActionKind.TRIGGER_STRUCTURE, "wrong_village_house|bent");
        addGrouped(Category.STRUCTURES, "wrong_village_house", "Wrong Village House", "structure_wrong_village_house_split", "Variant - Split", ActionKind.TRIGGER_STRUCTURE, "wrong_village_house|split");
        addGrouped(Category.STRUCTURES, "wrong_village_house", "Wrong Village House", "structure_wrong_village_house_gigantic", "Variant - Gigantic", ActionKind.TRIGGER_STRUCTURE, "wrong_village_house|gigantic");
        addGrouped(Category.STRUCTURES, "wrong_village_house", "Wrong Village House", "structure_wrong_village_house_tiny", "Variant - Tiny", ActionKind.TRIGGER_STRUCTURE, "wrong_village_house|tiny");

        addGrouped(Category.STRUCTURES, "wrong_village_utility", "Wrong Village Utility", "structure_wrong_village_utility_random", "Generate (Random Variant)", ActionKind.TRIGGER_STRUCTURE, "wrong_village_utility");
        addGrouped(Category.STRUCTURES, "wrong_village_utility", "Wrong Village Utility", "structure_wrong_village_utility_smithy", "Variant - Wrong Smithy", ActionKind.TRIGGER_STRUCTURE, "wrong_village_utility|wrong_smithy");
        addGrouped(Category.STRUCTURES, "wrong_village_utility", "Wrong Village Utility", "structure_wrong_village_utility_church", "Variant - Wrong Church", ActionKind.TRIGGER_STRUCTURE, "wrong_village_utility|wrong_church");
        addGrouped(Category.STRUCTURES, "wrong_village_utility", "Wrong Village Utility", "structure_wrong_village_utility_library", "Variant - Wrong Library", ActionKind.TRIGGER_STRUCTURE, "wrong_village_utility|wrong_library");
        addGrouped(Category.STRUCTURES, "wrong_village_utility", "Wrong Village Utility", "structure_wrong_village_utility_butcher", "Variant - Wrong Butcher", ActionKind.TRIGGER_STRUCTURE, "wrong_village_utility|wrong_butcher");
        addGrouped(Category.STRUCTURES, "wrong_village_utility", "Wrong Village Utility", "structure_wrong_village_utility_farm_shed", "Variant - Wrong Farm Shed", ActionKind.TRIGGER_STRUCTURE, "wrong_village_utility|wrong_farm_shed");
        addGrouped(Category.STRUCTURES, "wrong_village_utility", "Wrong Village Utility", "structure_wrong_village_utility_meeting", "Variant - Wrong Meeting Point", ActionKind.TRIGGER_STRUCTURE, "wrong_village_utility|wrong_meeting");

        addGrouped(Category.STRUCTURES, "sinkhole", "Sinkhole / Missing Chunk", "structure_sinkhole_random", "Generate (Random Variant)", ActionKind.TRIGGER_STRUCTURE, "sinkhole");
        addGrouped(Category.STRUCTURES, "sinkhole", "Sinkhole / Missing Chunk", "structure_sinkhole_open_void", "Variant - Open Void Pit", ActionKind.TRIGGER_STRUCTURE, "sinkhole|open_void_pit");
        addGrouped(Category.STRUCTURES, "sinkhole", "Sinkhole / Missing Chunk", "structure_sinkhole_structured", "Variant - Structured Sinkhole", ActionKind.TRIGGER_STRUCTURE, "sinkhole|structured_sinkhole");
        addGrouped(Category.STRUCTURES, "sinkhole", "Sinkhole / Missing Chunk", "structure_sinkhole_replaced", "Variant - Replaced Chunk", ActionKind.TRIGGER_STRUCTURE, "sinkhole|replaced_chunk");
        addGrouped(Category.STRUCTURES, "sinkhole", "Sinkhole / Missing Chunk", "structure_sinkhole_false_bottom", "Variant - False Bottom", ActionKind.TRIGGER_STRUCTURE, "sinkhole|false_bottom");
        addGrouped(Category.STRUCTURES, "sinkhole", "Sinkhole / Missing Chunk", "structure_sinkhole_vertical", "Variant - Vertical Shaft", ActionKind.TRIGGER_STRUCTURE, "sinkhole|vertical_shaft");
        addGrouped(Category.STRUCTURES, "sinkhole", "Sinkhole / Missing Chunk", "structure_sinkhole_broken_edge", "Variant - Broken Chunk Edge", ActionKind.TRIGGER_STRUCTURE, "sinkhole|broken_edge");
        addGrouped(Category.STRUCTURES, "sinkhole", "Sinkhole / Missing Chunk", "structure_sinkhole_inverted", "Variant - Inverted Chunk", ActionKind.TRIGGER_STRUCTURE, "sinkhole|inverted_chunk");

        addGrouped(Category.STRUCTURES, "observation_platform", "Observation Platform", "structure_observation_platform_random", "Generate (Random Variant)", ActionKind.TRIGGER_STRUCTURE, "observation_platform");
        addGrouped(Category.STRUCTURES, "observation_platform", "Observation Platform", "structure_observation_platform_wood", "Variant - Wood Platform", ActionKind.TRIGGER_STRUCTURE, "observation_platform|wood");
        addGrouped(Category.STRUCTURES, "observation_platform", "Observation Platform", "structure_observation_platform_stone", "Variant - Stone Platform", ActionKind.TRIGGER_STRUCTURE, "observation_platform|stone");
        addGrouped(Category.STRUCTURES, "observation_platform", "Observation Platform", "structure_observation_platform_broken", "Variant - Broken Platform", ActionKind.TRIGGER_STRUCTURE, "observation_platform|broken");
        addGrouped(Category.STRUCTURES, "observation_platform", "Observation Platform", "structure_observation_platform_overextended", "Variant - Overextended Platform", ActionKind.TRIGGER_STRUCTURE, "observation_platform|overextended");
        addGrouped(Category.STRUCTURES, "observation_platform", "Observation Platform", "structure_observation_platform_double", "Variant - Double Platform", ActionKind.TRIGGER_STRUCTURE, "observation_platform|double");
        addGrouped(Category.STRUCTURES, "observation_platform", "Observation Platform", "structure_observation_platform_decorated", "Variant - Platform with Chair/Bed/Chest", ActionKind.TRIGGER_STRUCTURE, "observation_platform|decorated");

        addGrouped(Category.STRUCTURES, "wrong_road_segment", "Wrong Road Segment", "structure_wrong_road_segment_random", "Generate (Random Variant)", ActionKind.TRIGGER_STRUCTURE, "wrong_road_segment");
        addGrouped(Category.STRUCTURES, "wrong_road_segment", "Wrong Road Segment", "structure_wrong_road_segment_nowhere", "Variant - Road to Nowhere", ActionKind.TRIGGER_STRUCTURE, "wrong_road_segment|nowhere");
        addGrouped(Category.STRUCTURES, "wrong_road_segment", "Wrong Road Segment", "structure_wrong_road_segment_wall", "Variant - Road Ending at Wall", ActionKind.TRIGGER_STRUCTURE, "wrong_road_segment|wall");
        addGrouped(Category.STRUCTURES, "wrong_road_segment", "Wrong Road Segment", "structure_wrong_road_segment_bell", "Variant - Road to Bell Shrine", ActionKind.TRIGGER_STRUCTURE, "wrong_road_segment|to_bell");
        addGrouped(Category.STRUCTURES, "wrong_road_segment", "Wrong Road Segment", "structure_wrong_road_segment_wrong_light", "Variant - Wrong Lighting", ActionKind.TRIGGER_STRUCTURE, "wrong_road_segment|wrong_light");
        addGrouped(Category.STRUCTURES, "wrong_road_segment", "Wrong Road Segment", "structure_wrong_road_segment_straight", "Variant - Too Straight", ActionKind.TRIGGER_STRUCTURE, "wrong_road_segment|too_straight");
        addGrouped(Category.STRUCTURES, "wrong_road_segment", "Wrong Road Segment", "structure_wrong_road_segment_tiny", "Variant - Tiny Wilderness Road", ActionKind.TRIGGER_STRUCTURE, "wrong_road_segment|tiny_wild");

        addGrouped(Category.STRUCTURES, "false_entrance", "False Entrance", "structure_false_entrance_random", "Generate (Random Variant)", ActionKind.TRIGGER_STRUCTURE, "false_entrance");
        addGrouped(Category.STRUCTURES, "false_entrance", "False Entrance", "structure_false_entrance_mine", "Variant - Mine Entrance", ActionKind.TRIGGER_STRUCTURE, "false_entrance|mine");
        addGrouped(Category.STRUCTURES, "false_entrance", "False Entrance", "structure_false_entrance_cellar", "Variant - Cellar Entrance", ActionKind.TRIGGER_STRUCTURE, "false_entrance|cellar");
        addGrouped(Category.STRUCTURES, "false_entrance", "False Entrance", "structure_false_entrance_stone_stairs", "Variant - Stone Stair Entrance", ActionKind.TRIGGER_STRUCTURE, "false_entrance|stone_stairs");
        addGrouped(Category.STRUCTURES, "false_entrance", "False Entrance", "structure_false_entrance_bricked", "Variant - Bricked Entrance", ActionKind.TRIGGER_STRUCTURE, "false_entrance|bricked");
        addGrouped(Category.STRUCTURES, "false_entrance", "False Entrance", "structure_false_entrance_cliff", "Variant - Cliff Entrance", ActionKind.TRIGGER_STRUCTURE, "false_entrance|cliff");
        addGrouped(Category.STRUCTURES, "false_entrance", "False Entrance", "structure_false_entrance_trapdoor", "Variant - Trapdoor Entrance", ActionKind.TRIGGER_STRUCTURE, "false_entrance|trapdoor");

        addGrouped(Category.STRUCTURES, "storage_shed", "Storage Shed", "structure_storage_shed_random", "Generate (Random Variant)", ActionKind.TRIGGER_STRUCTURE, "storage_shed");
        addGrouped(Category.STRUCTURES, "storage_shed", "Storage Shed", "structure_storage_shed_organized", "Variant - Organized Shed", ActionKind.TRIGGER_STRUCTURE, "storage_shed|organized");
        addGrouped(Category.STRUCTURES, "storage_shed", "Storage Shed", "structure_storage_shed_messy", "Variant - Messy Shed", ActionKind.TRIGGER_STRUCTURE, "storage_shed|messy");
        addGrouped(Category.STRUCTURES, "storage_shed", "Storage Shed", "structure_storage_shed_oversized", "Variant - Oversized Shed", ActionKind.TRIGGER_STRUCTURE, "storage_shed|oversized");
        addGrouped(Category.STRUCTURES, "storage_shed", "Storage Shed", "structure_storage_shed_tiny", "Variant - Tiny Shed", ActionKind.TRIGGER_STRUCTURE, "storage_shed|tiny");
        addGrouped(Category.STRUCTURES, "storage_shed", "Storage Shed", "structure_storage_shed_wrong_interior", "Variant - Wrong Interior", ActionKind.TRIGGER_STRUCTURE, "storage_shed|wrong_interior");
        addGrouped(Category.STRUCTURES, "storage_shed", "Storage Shed", "structure_storage_shed_many_chests", "Variant - Too Many Chests", ActionKind.TRIGGER_STRUCTURE, "storage_shed|too_many_chests");

        addGrouped(Category.STRUCTURES, "secret_house", "Secret House", "structure_secret_house", "Generate - Secret House (Black Door)", ActionKind.TRIGGER_SECRET_HOUSE, "secret_house");

        ENTRIES.sort(Comparator.comparing(Entry::category).thenComparing(Entry::id));
    }

    private UncannyDevCatalog() {
    }

    public static List<Entry> entries() {
        return List.copyOf(ENTRIES);
    }

    public static List<Category> primaryCategories() {
        return List.of(Category.ENTITIES, Category.EVENTS, Category.WEATHER, Category.STRUCTURES);
    }

    public static List<Group> groups(Category category) {
        if (category == null || category == Category.ALL) {
            return List.of();
        }
        Map<String, Group> groups = new LinkedHashMap<>();
        for (Entry entry : ENTRIES) {
            if (entry.category() != category) {
                continue;
            }
            groups.putIfAbsent(entry.groupKey(), new Group(entry.groupKey(), entry.groupLabel(), category));
        }
        return List.copyOf(groups.values());
    }

    public static List<Entry> entries(Category category, String groupKey) {
        if (category == null || category == Category.ALL) {
            return List.of();
        }
        String normalizedGroup = normalize(groupKey);
        return ENTRIES.stream()
                .filter(entry -> entry.category() == category)
                .filter(entry -> normalizedGroup == null || entry.groupKey().equals(normalizedGroup))
                .sorted(Comparator.comparing(Entry::label))
                .toList();
    }

    public static Entry byId(String id) {
        if (id == null) {
            return null;
        }
        return ENTRIES_BY_ID.get(id.toLowerCase(Locale.ROOT));
    }

    private static void addGrouped(
            Category category,
            String groupKey,
            String groupLabel,
            String id,
            String label,
            ActionKind actionKind,
            String actionArg) {
        String normalizedId = normalize(id);
        String normalizedGroup = normalize(groupKey);
        Entry entry = new Entry(
                normalizedId,
                category,
                normalizedGroup == null ? "misc" : normalizedGroup,
                groupLabel,
                label,
                actionKind,
                actionArg);
        ENTRIES_BY_ID.put(normalizedId, entry);
        ENTRIES.add(entry);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.isBlank() ? null : trimmed;
    }

    public enum Category {
        ALL("All"),
        ENTITIES("Entities"),
        EVENTS("Events"),
        WEATHER("Weather"),
        STRUCTURES("Structures");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return this.label;
        }
    }

    public enum ActionKind {
        SPAWN_UNCANNY,
        SPAWN_UNCANNY_FORCED,
        SPAWN_PASSIVE_FORCED,
        SPAWN_SPECIAL,
        FORCE_MIMIC,
        TRIGGER_EVENT,
        TRIGGER_VARIANT,
        GIVE_ITEM,
        TRIGGER_WEATHER,
        STOP_WEATHER,
        TRIGGER_STRUCTURE,
        TRIGGER_SECRET_HOUSE
    }

    public enum QaStatus {
        GRAY,
        ORANGE,
        GREEN
    }

    public record Entry(
            String id,
            Category category,
            String groupKey,
            String groupLabel,
            String label,
            ActionKind actionKind,
            String actionArg) {
    }

    public record Group(String key, String label, Category category) {
    }
}
