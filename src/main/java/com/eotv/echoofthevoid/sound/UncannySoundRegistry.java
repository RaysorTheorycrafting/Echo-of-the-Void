package com.eotv.echoofthevoid.sound;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class UncannySoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, EchoOfTheVoid.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_HURLER_SCREAM = SOUND_EVENTS.register(
            "uncanny_hurler_scream",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_hurler_scream")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_KNOCKER_KNOCK = SOUND_EVENTS.register(
            "uncanny_knocker_knock",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_knocker_knock")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_FOX_SCREAM = SOUND_EVENTS.register(
            "uncanny_fox_scream",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_fox_scream")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_HEARTBEAT = SOUND_EVENTS.register(
            "uncanny_heartbeat",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_heartbeat")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_MONSTER_BREATH = SOUND_EVENTS.register(
            "uncanny_monster_breath",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_monster_breath")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_SCARY_LAUGH = SOUND_EVENTS.register(
            "uncanny_scary_laugh",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_scary_laugh")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_WHISPER = SOUND_EVENTS.register(
            "uncanny_whisper",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_whisper")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_TINNITUS = SOUND_EVENTS.register(
            "uncanny_tinnitus",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_tinnitus")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_PSSS = SOUND_EVENTS.register(
            "uncanny_psss",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_psss")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_FOLLOW_ME_CREATURE_GLITCH = SOUND_EVENTS.register(
            "uncanny_follow_me_creature_glitch",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_follow_me_creature_glitch")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_GRANDEVENT_DONT_MOVE = SOUND_EVENTS.register(
            "uncanny_grandevent_dont_move",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_grandevent_dont_move")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_GRANDEVENT_DONT_MAKE_A_SOUND = SOUND_EVENTS.register(
            "uncanny_grandevent_dont_make_a_sound",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_grandevent_dont_make_a_sound")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_GRANDEVENT_IT_IS_HERE = SOUND_EVENTS.register(
            "uncanny_grandevent_it_is_here",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_grandevent_it_is_here")));

    public static final DeferredHolder<SoundEvent, SoundEvent> ORE_INSIDE_KNOCK = SOUND_EVENTS.register(
            "ore_inside_knock",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "ore_inside_knock")));

    public static final DeferredHolder<SoundEvent, SoundEvent> CAMPFIRE_COUGH_CREEPY = SOUND_EVENTS.register(
            "campfire_cough_creepy",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "campfire_cough_creepy")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_ZOMBIE_TALL_AMBIENT = SOUND_EVENTS.register(
            "uncanny_zombie_tall_ambient",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_zombie_tall_ambient")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_ZOMBIE_TALL_HURT = SOUND_EVENTS.register(
            "uncanny_zombie_tall_hurt",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_zombie_tall_hurt")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_ZOMBIE_TALL_DEATH = SOUND_EVENTS.register(
            "uncanny_zombie_tall_death",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_zombie_tall_death")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_ZOMBIE_TALL_STEP = SOUND_EVENTS.register(
            "uncanny_zombie_tall_step",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_zombie_tall_step")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_FLAT_AMBIENT = SOUND_EVENTS.register(
            "uncanny_villager_flat_ambient",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_flat_ambient")));
    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_FLAT_HURT = SOUND_EVENTS.register(
            "uncanny_villager_flat_hurt",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_flat_hurt")));
    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_FLAT_DEATH = SOUND_EVENTS.register(
            "uncanny_villager_flat_death",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_flat_death")));
    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_FLAT_TRADE = SOUND_EVENTS.register(
            "uncanny_villager_flat_trade",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_flat_trade")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_HUGE_LONG_WIDE_AMBIENT = SOUND_EVENTS.register(
            "uncanny_villager_huge_long_wide_ambient",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_huge_long_wide_ambient")));
    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_HUGE_LONG_WIDE_HURT = SOUND_EVENTS.register(
            "uncanny_villager_huge_long_wide_hurt",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_huge_long_wide_hurt")));
    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_HUGE_LONG_WIDE_DEATH = SOUND_EVENTS.register(
            "uncanny_villager_huge_long_wide_death",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_huge_long_wide_death")));
    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_HUGE_LONG_WIDE_TRADE = SOUND_EVENTS.register(
            "uncanny_villager_huge_long_wide_trade",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_huge_long_wide_trade")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_HUGE_THIN_AMBIENT = SOUND_EVENTS.register(
            "uncanny_villager_huge_thin_ambient",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_huge_thin_ambient")));
    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_HUGE_THIN_HURT = SOUND_EVENTS.register(
            "uncanny_villager_huge_thin_hurt",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_huge_thin_hurt")));
    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_HUGE_THIN_DEATH = SOUND_EVENTS.register(
            "uncanny_villager_huge_thin_death",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_huge_thin_death")));
    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_HUGE_THIN_TRADE = SOUND_EVENTS.register(
            "uncanny_villager_huge_thin_trade",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_huge_thin_trade")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_VERY_WIDE_AMBIENT = SOUND_EVENTS.register(
            "uncanny_villager_very_wide_ambient",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_very_wide_ambient")));
    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_VERY_WIDE_HURT = SOUND_EVENTS.register(
            "uncanny_villager_very_wide_hurt",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_very_wide_hurt")));
    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_VERY_WIDE_DEATH = SOUND_EVENTS.register(
            "uncanny_villager_very_wide_death",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_very_wide_death")));
    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_VERY_WIDE_TRADE = SOUND_EVENTS.register(
            "uncanny_villager_very_wide_trade",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_very_wide_trade")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_VERY_LONG_AMBIENT = SOUND_EVENTS.register(
            "uncanny_villager_very_long_ambient",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_very_long_ambient")));
    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_VERY_LONG_HURT = SOUND_EVENTS.register(
            "uncanny_villager_very_long_hurt",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_very_long_hurt")));
    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_VERY_LONG_DEATH = SOUND_EVENTS.register(
            "uncanny_villager_very_long_death",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_very_long_death")));
    public static final DeferredHolder<SoundEvent, SoundEvent> UNCANNY_VILLAGER_VERY_LONG_TRADE = SOUND_EVENTS.register(
            "uncanny_villager_very_long_trade",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_villager_very_long_trade")));

    private UncannySoundRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
