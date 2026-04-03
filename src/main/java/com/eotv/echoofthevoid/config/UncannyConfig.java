package com.eotv.echoofthevoid.config;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class UncannyConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG_LOGS = BUILDER
            .comment("Enable verbose debug logs for uncanny systems.")
            .define("uncanny.debugLogs", false);

    public static final ModConfigSpec.BooleanValue DARKNESS_ENABLED = BUILDER
            .comment("Enable progressive darkness rendering.")
            .define("uncanny.darkness.enabled", true);

    public static final ModConfigSpec.BooleanValue DARKNESS_BLOCK_LIGHT_ONLY = BUILDER
            .comment("Only block light contributes to brightness. Sky light contribution is disabled.")
            .define("uncanny.darkness.blockLightOnly", false);

    public static final ModConfigSpec.BooleanValue DARKNESS_IGNORE_MOON_PHASE = BUILDER
            .comment("Ignore moon phase influence on night brightness.")
            .define("uncanny.darkness.ignoreMoonPhase", false);

    public static final ModConfigSpec.BooleanValue DARKNESS_ENABLE_OVERWORLD = BUILDER
            .comment("Enable progressive darkness in the Overworld.")
            .define("uncanny.darkness.enableOverworld", true);

    public static final ModConfigSpec.BooleanValue DARKNESS_ENABLE_NETHER = BUILDER
            .comment("Enable progressive darkness in the Nether.")
            .define("uncanny.darkness.enableNether", true);

    public static final ModConfigSpec.BooleanValue DARKNESS_ENABLE_END = BUILDER
            .comment("Enable progressive darkness in the End.")
            .define("uncanny.darkness.enableEnd", true);

    public static final ModConfigSpec.BooleanValue DARKNESS_ENABLE_DEFAULT_DIMENSIONS = BUILDER
            .comment("Enable progressive darkness in modded dimensions with sky.")
            .define("uncanny.darkness.enableDefault", true);

    public static final ModConfigSpec.BooleanValue DARKNESS_ENABLE_SKYLESS_DIMENSIONS = BUILDER
            .comment("Enable progressive darkness in modded dimensions without sky.")
            .define("uncanny.darkness.enableSkyless", true);

    public static final ModConfigSpec.DoubleValue DARKNESS_PHASE1_STRENGTH = BUILDER
            .comment("Progressive darkness strength multiplier for phase 1.")
            .defineInRange("uncanny.darkness.phase1Strength", 0.28D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue DARKNESS_PHASE2_STRENGTH = BUILDER
            .comment("Progressive darkness strength multiplier for phase 2.")
            .defineInRange("uncanny.darkness.phase2Strength", 0.50D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue DARKNESS_PHASE3_STRENGTH = BUILDER
            .comment("Progressive darkness strength multiplier for phase 3.")
            .defineInRange("uncanny.darkness.phase3Strength", 0.72D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue DARKNESS_PHASE4_STRENGTH = BUILDER
            .comment("Progressive darkness strength multiplier for phase 4.")
            .defineInRange("uncanny.darkness.phase4Strength", 0.88D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue DARKNESS_CURVE_EXPONENT = BUILDER
            .comment("Exponent applied after smoothing to shape perceived darkness.")
            .defineInRange("uncanny.darkness.curveExponent", 1.35D, 0.25D, 4D);

    public static final ModConfigSpec.DoubleValue DARKNESS_LIGHT_FLOOR = BUILDER
            .comment("Luminance value treated as fully dark start (0.0 to 1.0).")
            .defineInRange("uncanny.darkness.lightFloor", 0.02D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue DARKNESS_LIGHT_CEILING = BUILDER
            .comment("Luminance value treated as fully bright stop (0.0 to 1.0).")
            .defineInRange("uncanny.darkness.lightCeiling", 0.92D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue DARKNESS_DARKEN_SPEED = BUILDER
            .comment("Smoothing speed when darkness increases (per tick blend factor).")
            .defineInRange("uncanny.darkness.darkenSpeed", 0.14D, 0.01D, 1D);

    public static final ModConfigSpec.DoubleValue DARKNESS_BRIGHTEN_SPEED = BUILDER
            .comment("Smoothing speed when darkness decreases (per tick blend factor).")
            .defineInRange("uncanny.darkness.brightenSpeed", 0.06D, 0.01D, 1D);

    public static final ModConfigSpec.DoubleValue DARKNESS_OVERLAY_MAX_ALPHA = BUILDER
            .comment("Maximum opacity used by the lightweight darkness overlay.")
            .defineInRange("uncanny.darkness.overlayMaxAlpha", 0.42D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue DARKNESS_LIGHTMAP_STRENGTH = BUILDER
            .comment("Global strength multiplier applied to lightmap-based darkness.")
            .defineInRange("uncanny.darkness.lightmapStrength", 1.0D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue DARKNESS_MAX_PIXEL_ATTENUATION = BUILDER
            .comment("Lower bound for per-pixel luminance after darkness attenuation.")
            .defineInRange("uncanny.darkness.maxPixelAttenuation", 0.08D, 0D, 1D);

    public static final ModConfigSpec.IntValue PHASE_P1_TO_P2_MINUTES = BUILDER
            .comment("Minutes required to progress from phase 1 to phase 2.")
            .defineInRange("uncanny.phase.p1ToP2Minutes", 30, 1, 10000);

    public static final ModConfigSpec.IntValue PHASE_P2_TO_P3_MINUTES = BUILDER
            .comment("Minutes required to progress from phase 2 to phase 3.")
            .defineInRange("uncanny.phase.p2ToP3Minutes", 45, 1, 10000);

    public static final ModConfigSpec.IntValue PHASE_P3_TO_P4_MINUTES = BUILDER
            .comment("Minutes required to progress from phase 3 to phase 4.")
            .defineInRange("uncanny.phase.p3ToP4Minutes", 60, 1, 10000);

    public static final ModConfigSpec.DoubleValue PHASE1_REPLACEMENT_CHANCE = BUILDER
            .comment("Natural spawn replacement chance in phase 1.")
            .defineInRange("uncanny.spawn.phase1ReplacementChance", 0.01D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue PHASE2_REPLACEMENT_CHANCE = BUILDER
            .comment("Natural spawn replacement chance in phase 2.")
            .defineInRange("uncanny.spawn.phase2ReplacementChance", 0.20D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue PHASE3_REPLACEMENT_CHANCE = BUILDER
            .comment("Natural spawn replacement chance in phase 3.")
            .defineInRange("uncanny.spawn.phase3ReplacementChance", 0.60D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue PHASE4_REPLACEMENT_CHANCE = BUILDER
            .comment("Natural spawn replacement chance in phase 4.")
            .defineInRange("uncanny.spawn.phase4ReplacementChance", 1.00D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue DEATH_PROGRESS_BOOST = BUILDER
            .comment("Progress added toward next phase after a valid player death.")
            .defineInRange("uncanny.death.progressBoost", 0.15D, 0D, 1D);

    public static final ModConfigSpec.IntValue DEATH_PROGRESS_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown between death-based progress boosts.")
            .defineInRange("uncanny.death.progressCooldownSeconds", 300, 0, 86400);

    public static final ModConfigSpec.IntValue SPAWN_MINIMUM_DISTANCE = BUILDER
            .comment("Minimum distance from nearest player for uncanny natural replacement.")
            .defineInRange("uncanny.spawn.minimumDistance", 12, 0, 256);

    public static final ModConfigSpec.IntValue EVENT_GLOBAL_COOLDOWN_SECONDS = BUILDER
            .comment("Global cooldown between panic events.")
            .defineInRange("uncanny.events.globalCooldownSeconds", 480, 0, 86400);

    public static final ModConfigSpec.IntValue EVENT_INTENSITY_PROFILE = BUILDER
            .comment("Auto-event intensity profile: 1=soft, 2=low, 3=normal, 4=hard, 5=extreme.")
            .defineInRange("uncanny.events.intensityProfile", 3, 1, 5);

    public static final ModConfigSpec.IntValue EVENT_DANGER_LEVEL = BUILDER
            .comment("Danger level: 0=safest special behavior, 3=balanced default, 5=most dangerous.")
            .defineInRange("uncanny.events.dangerLevel", 3, 0, 5);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CORRUPT_MESSAGE_TEXTS = BUILDER
            .comment("Pool of corrupt chat messages (English defaults).")
            .defineList("uncanny.events.corruptMessage.texts",
                    List.of("You should not have done that.", "Do not turn around.", "It is already too late.", "I can see you."),
                    value -> value instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CORRUPT_MESSAGE_COLORS = BUILDER
            .comment("Pool of color names for corrupt messages (e.g. dark_red, red, gray, dark_purple).")
            .defineList("uncanny.events.corruptMessage.colors",
                    List.of("dark_red", "red", "gray", "dark_purple"),
                    value -> value instanceof String);

    public static final ModConfigSpec.DoubleValue CORRUPT_MESSAGE_GLITCH_CHANCE = BUILDER
            .comment("Chance to apply glitch obfuscation formatting on corrupt messages.")
            .defineInRange("uncanny.events.corruptMessage.glitchChance", 0.002D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue RESPAWN_GRACE_SECONDS = BUILDER
            .comment("Grace period after respawn where no panic event can trigger.")
            .defineInRange("uncanny.events.respawnGraceSeconds", 20, 0, 600);

    public static final ModConfigSpec.IntValue BASE_RADIUS_BLOCKS = BUILDER
            .comment("Base radius around player bed.")
            .defineInRange("uncanny.doubleDormant.baseRadiusBlocks", 24, 1, 256);

    public static final ModConfigSpec.IntValue ABSENCE_SECONDS = BUILDER
            .comment("Required out-of-base duration before return can trigger Double Dormant.")
            .defineInRange("uncanny.doubleDormant.absenceSeconds", 300, 1, 86400);

    public static final ModConfigSpec.IntValue DOUBLE_DORMANT_COOLDOWN_SECONDS = BUILDER
            .comment("Per-player cooldown for Double Dormant.")
            .defineInRange("uncanny.doubleDormant.cooldownSeconds", 3600, 0, 86400);

    public static final ModConfigSpec.DoubleValue DOUBLE_DORMANT_TRIGGER_CHANCE = BUILDER
            .comment("Trigger chance when Double Dormant conditions are met.")
            .defineInRange("uncanny.doubleDormant.triggerChance", 0.25D, 0D, 1D);

    public static final ModConfigSpec.IntValue DOUBLE_DORMANT_ESCAPE_DISTANCE_BLOCKS = BUILDER
            .comment("Distance from base center where target can force despawn.")
            .defineInRange("uncanny.doubleDormant.escapeDistanceBlocks", 96, 8, 1024);

    public static final ModConfigSpec.IntValue BLOCK_ON_RECENT_DEATH_SECONDS = BUILDER
            .comment("Double Dormant is blocked if player's death was within this interval.")
            .defineInRange("uncanny.doubleDormant.blockOnRecentDeathSeconds", 300, 0, 86400);

    public static final ModConfigSpec.DoubleValue ENDERMAN_AUTO_AGGRO_CHANCE = BUILDER
            .comment("Periodic chance for Enderman Tracker to auto-aggro nearest player.")
            .defineInRange("uncanny.enderman.autoAggroChance", 0.03D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue ENDERMAN_BEHIND_TELEPORT_CHANCE = BUILDER
            .comment("Chance per check for behind-player teleport during tracker pursuit.")
            .defineInRange("uncanny.enderman.behindTeleportChance", 0.01D, 0D, 1D);

    public static final ModConfigSpec.IntValue EVOKER_CAST_DELAY_MIN_TICKS = BUILDER
            .comment("Minimum extra cast warmup for Uncanny Evoker, in ticks.")
            .defineInRange("uncanny.evoker.castDelayMinTicks", 16, 0, 200);

    public static final ModConfigSpec.IntValue EVOKER_CAST_DELAY_MAX_TICKS = BUILDER
            .comment("Maximum extra cast warmup for Uncanny Evoker, in ticks.")
            .defineInRange("uncanny.evoker.castDelayMaxTicks", 30, 0, 400);

    public static final ModConfigSpec.IntValue WATCHER_COOLDOWN_SECONDS = BUILDER
            .comment("Per-player cooldown for Watcher spawn attempts.")
            .defineInRange("uncanny.watcher.cooldownSeconds", 900, 0, 86400);

    public static final ModConfigSpec.IntValue WATCHER_CHECK_INTERVAL_SECONDS = BUILDER
            .comment("How often Watcher spawn checks run per player.")
            .defineInRange("uncanny.watcher.checkIntervalSeconds", 15, 1, 600);

    public static final ModConfigSpec.DoubleValue WATCHER_TRIGGER_CHANCE = BUILDER
            .comment("Chance for a Watcher to spawn when all conditions are met.")
            .defineInRange("uncanny.watcher.triggerChance", 0.08D, 0D, 1D);

    public static final ModConfigSpec.IntValue WATCHER_MIN_DISTANCE = BUILDER
            .comment("Minimum spawn distance for the Watcher.")
            .defineInRange("uncanny.watcher.minDistance", 52, 16, 256);

    public static final ModConfigSpec.IntValue WATCHER_MAX_DISTANCE = BUILDER
            .comment("Maximum spawn distance for the Watcher.")
            .defineInRange("uncanny.watcher.maxDistance", 70, 16, 256);

    public static final ModConfigSpec.IntValue AUDIO_INTENSITY_PERCENT = BUILDER
            .comment("Audio intensity multiplier, in percent.")
            .defineInRange("uncanny.audio.intensityPercent", 100, 0, 100);

    public static final ModConfigSpec.BooleanValue ENABLE_STINGERS = BUILDER
            .comment("Enable stinger sounds.")
            .define("uncanny.audio.enableStingers", true);

    public static final ModConfigSpec.BooleanValue SOFT_MODE = BUILDER
            .comment("Soft mode can re-enable critical telegraph sounds.")
            .define("uncanny.audio.softMode", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private UncannyConfig() {
    }
}


