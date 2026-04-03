package com.eotv.echoofthevoid.sound;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.resources.ResourceLocation;

public final class UncannySoundRefs {
    public static final ResourceLocation ENDERMAN_BEHIND_STINGER = id("uncanny/enderman_behind_stinger");
    public static final ResourceLocation GHAST_HOWLER_STINGER = id("uncanny/ghast_howler_stinger");
    public static final ResourceLocation CREEPER_BACKCHARGE = id("uncanny/creeper_backcharge");
    public static final ResourceLocation TORCH_CLICK_MISLEAD = id("uncanny/torch_click_mislead");
    public static final ResourceLocation EVENT_TOTAL_BLACKOUT = id("uncanny/events/total_blackout");
    public static final ResourceLocation EVENT_FLASH_ERROR = id("uncanny/events/flash_error");
    public static final ResourceLocation EVENT_FOOTSTEPS_BEHIND = id("uncanny/events/footsteps_behind");
    public static final ResourceLocation EVENT_BASE_REPLAY = id("uncanny/events/base_replay");
    public static final ResourceLocation EVENT_BELL = id("uncanny/events/bell");
    public static final ResourceLocation WATCHER_CAVE_CUE = id("uncanny/watcher_cave_cue");

    private UncannySoundRefs() {
    }

    public static void logRegisteredPlaceholders() {
        // Method name kept stable to avoid touching existing call sites/debug workflows.
        EchoOfTheVoid.LOGGER.info("Uncanny sound refs: {}, {}, {}, {}, {}, {}, {}, {}, {}, {}",
                ENDERMAN_BEHIND_STINGER,
                GHAST_HOWLER_STINGER,
                CREEPER_BACKCHARGE,
                TORCH_CLICK_MISLEAD,
                EVENT_TOTAL_BLACKOUT,
                EVENT_FLASH_ERROR,
                EVENT_FOOTSTEPS_BEHIND,
                EVENT_BASE_REPLAY,
                EVENT_BELL,
                WATCHER_CAVE_CUE);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, path);
    }
}

