package com.eotv.echoofthevoid.command;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.dev.UncannyDevQaStateService;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyDoubleDormantEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyEndermanEntity;
import com.eotv.echoofthevoid.event.UncannyDoubleDormantSystem;
import com.eotv.echoofthevoid.event.UncannyPassiveVariantSystem;
import com.eotv.echoofthevoid.event.UncannyParanoiaEventSystem;
import com.eotv.echoofthevoid.event.UncannyStructureFeatureSystem;
import com.eotv.echoofthevoid.event.UncannyWatcherSystem;
import com.eotv.echoofthevoid.event.UncannyWeatherSystem;
import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import com.eotv.echoofthevoid.lore.UncannyLoreBookLibrary;
import com.eotv.echoofthevoid.phase.UncannyPhaseManager;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class UncannyCommandRegistry {
    private static final SimpleCommandExceptionType PLAYER_REQUIRED_EXCEPTION =
            new SimpleCommandExceptionType(Component.literal("This command requires a player target when run from console."));
    private static final List<String> LOCATE_STRUCTURE_IDS = List.of(
            "anechoic_cube",
            "mimic_shelter",
            "glitched_shelter",
            "patterned_grove",
            "barren_grid",
            "false_descent",
            "false_descent_house",
            "false_descent_with_house",
            "false_ascent",
            "false_ascent_house",
            "false_ascent_with_house",
            "isolation_cube",
            "bell_shrine",
            "watching_tower",
            "false_camp",
            "wrong_village_house",
            "wrong_village_utility",
            "sinkhole",
            "observation_platform",
            "wrong_road_segment",
            "false_entrance",
            "storage_shed",
            "secret_house");
    private static final Map<String, String> LOCATE_STRUCTURE_CANONICAL_MARKER = Map.ofEntries(
            Map.entry("anechoic_cube", "anechoic_cube"),
            Map.entry("mimic_shelter", "mimic_shelter"),
            Map.entry("glitched_shelter", "glitched_shelter"),
            Map.entry("patterned_grove", "patterned_grove"),
            Map.entry("barren_grid", "barren_grid"),
            Map.entry("false_descent", "false_descent"),
            Map.entry("false_descent_house", "false_descent_house"),
            Map.entry("false_descent_with_house", "false_descent"),
            Map.entry("false_ascent", "false_ascent"),
            Map.entry("false_ascent_house", "false_ascent_house"),
            Map.entry("false_ascent_with_house", "false_ascent"),
            Map.entry("isolation_cube", "isolation_cube"),
            Map.entry("bell_shrine", "bell_shrine"),
            Map.entry("watching_tower", "watching_tower"),
            Map.entry("false_camp", "false_camp"),
            Map.entry("wrong_village_house", "wrong_village_house"),
            Map.entry("wrong_village_utility", "wrong_village_utility"),
            Map.entry("sinkhole", "sinkhole"),
            Map.entry("observation_platform", "observation_platform"),
            Map.entry("wrong_road_segment", "wrong_road_segment"),
            Map.entry("false_entrance", "false_entrance"),
            Map.entry("storage_shed", "storage_shed"),
            Map.entry("secret_house", "secret_house"));
    private static final List<String> EVENT_VARIANT_EVENTS = List.of(
            "footsteps",
            "asphyxia",
            "armor_break",
            "aquatic_steps",
            "door_inversion",
            "phantom_harvest",
            "phantom_mode",
            "living_ore",
            "projected_shadow");
    private static final Map<String, List<String>> EVENT_VARIANTS = Map.ofEntries(
            Map.entry("footsteps", List.of("basic", "echo", "sprint", "heavy", "ladder_steps")),
            Map.entry("asphyxia", List.of("false_alert", "terrain_drowning", "heavy_lungs")),
            Map.entry("armor_break", List.of("ghost_sound", "drop_gear", "cracked_defense")),
            Map.entry("aquatic_steps", List.of("follower", "slippery_ambush", "invisible_bite")),
            Map.entry("door_inversion", List.of("poltergeist", "lockdown", "intrusion", "door_trap_cascade")),
            Map.entry("phantom_harvest", List.of("black_harvest", "rotten_soil", "infestation")),
            Map.entry("phantom_mode", List.of("lantern_eater")),
            Map.entry("living_ore", List.of("bleeding", "toxic_blood", "vicious_fall", "vein_retreat", "inside_knock")),
            Map.entry("projected_shadow", List.of("mime", "shadow_assault", "ghost_shot")));

    private UncannyCommandRegistry() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("uncanny")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("setPhase")
                        .then(Commands.argument("phase", IntegerArgumentType.integer(0, 4))
                                .executes(UncannyCommandRegistry::setPhase)))
                .then(Commands.literal("addPhaseProgress")
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D, 1.0D))
                                .executes(UncannyCommandRegistry::addPhaseProgress)))
                .then(Commands.literal("setEventProfile")
                        .then(Commands.argument("profile", IntegerArgumentType.integer(1, 5))
                                .executes(UncannyCommandRegistry::setEventProfile)))
                .then(Commands.literal("setDangerLevel")
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 5))
                                .executes(UncannyCommandRegistry::setDangerLevel)))
                .then(Commands.literal("setDebugLogs")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(UncannyCommandRegistry::setDebugLogs)))
                .then(Commands.literal("tensionBuilder")
                        .then(Commands.literal("start")
                                .executes(context -> tensionBuilderStart(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> tensionBuilderStart(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("stop")
                                .executes(context -> tensionBuilderStop(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> tensionBuilderStop(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("status")
                                .executes(context -> tensionBuilderStatus(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> tensionBuilderStatus(context, EntityArgument.getPlayer(context, "target"))))))
                .then(Commands.literal("grandEvent")
                        .then(Commands.literal("start")
                                .executes(context -> triggerGrandEvent(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerGrandEvent(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("stop")
                                .executes(context -> stopGrandEvent(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> stopGrandEvent(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("status")
                                .executes(context -> grandEventStatus(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> grandEventStatus(context, EntityArgument.getPlayer(context, "target"))))))
                .then(Commands.literal("devmenu")
                        .executes(context -> openDevMenu(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> openDevMenu(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("weather")
                        .then(Commands.literal("stop")
                                .executes(UncannyCommandRegistry::stopWeather))
                        .then(Commands.literal("trigger")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .executes(UncannyCommandRegistry::triggerWeather))))
                .then(Commands.literal("spawnUncanny")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .executes(context -> spawnUncanny(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> spawnUncanny(context, EntityArgument.getPlayer(context, "target"))))))
                .then(Commands.literal("forcePassive")
                        .then(Commands.argument("variant", IntegerArgumentType.integer(1, 5))
                                .executes(context -> forcePassive(context, getCallerPlayer(context), 24))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> forcePassive(context, EntityArgument.getPlayer(context, "target"), 24))
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                                                .executes(context -> forcePassive(
                                                        context,
                                                        EntityArgument.getPlayer(context, "target"),
                                                        IntegerArgumentType.getInteger(context, "radius")))))))
                .then(Commands.literal("forceMimic")
                        .executes(context -> forceMimic(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> forceMimic(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("debugMimic")
                        .executes(context -> debugMimic(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> debugMimic(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("debugEvents")
                        .executes(context -> debugEvents(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> debugEvents(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("debugSpecialRoll")
                        .executes(context -> debugSpecialRoll(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> debugSpecialRoll(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("spawnWatcher")
                        .executes(context -> spawnWatcher(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> spawnWatcher(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("spawnShadow")
                        .executes(context -> spawnShadow(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> spawnShadow(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("spawnHurler")
                        .executes(context -> spawnHurler(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> spawnHurler(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("spawnStalker")
                        .executes(context -> spawnStalker(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> spawnStalker(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("spawnAttacker")
                        .executes(context -> spawnStalker(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> spawnStalker(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("spawnKnocker")
                        .executes(context -> spawnKnocker(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> spawnKnocker(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("spawnPulse")
                        .executes(context -> spawnPulse(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> spawnPulse(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("spawnUsher")
                        .executes(context -> spawnUsher(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> spawnUsher(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("spawnKeeper")
                        .executes(context -> spawnKeeper(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> spawnKeeper(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("spawnTenant")
                        .executes(context -> spawnTenant(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> spawnTenant(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("spawnFollower")
                        .executes(context -> spawnFollower(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> spawnFollower(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("spawnPhantomLanternEater")
                        .executes(context -> spawnPhantomLanternEater(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> spawnPhantomLanternEater(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("forceFoxCry")
                        .executes(context -> forceFoxCry(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> forceFoxCry(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("event")
                        .then(Commands.literal("blackout")
                                .executes(context -> triggerBlackout(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerBlackout(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("footsteps")
                                .executes(context -> triggerFootsteps(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerFootsteps(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("flash")
                                .executes(context -> triggerFlash(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerFlash(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("baseReplay")
                                .executes(context -> triggerBaseReplay(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerBaseReplay(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("bell")
                                .executes(context -> triggerBell(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerBell(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("flashRed")
                                .executes(context -> triggerFlashRed(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerFlashRed(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("voidSilence")
                                .executes(context -> triggerVoidSilence(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerVoidSilence(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("falseFall")
                                .executes(context -> triggerFalseFall(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerFalseFall(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("ghostMiner")
                                .executes(context -> triggerGhostMiner(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerGhostMiner(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("caveCollapse")
                                .executes(context -> triggerCaveCollapse(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerCaveCollapse(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("falseInjury")
                                .executes(context -> triggerFalseInjury(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerFalseInjury(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("forceDrop")
                                .executes(context -> triggerForceDrop(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerForceDrop(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("corruptMessage")
                                .executes(context -> triggerCorruptMessage(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerCorruptMessage(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("bed")
                                .executes(context -> triggerBed(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerBed(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("asphyxia")
                                .executes(context -> triggerAsphyxia(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerAsphyxia(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("armorBreak")
                                .executes(context -> triggerArmorBreak(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerArmorBreak(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("aquaticSteps")
                                .executes(context -> triggerAquaticSteps(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerAquaticSteps(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("doorInversion")
                                .executes(context -> triggerDoorInversion(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerDoorInversion(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("phantomHarvest")
                                .executes(context -> triggerPhantomHarvest(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerPhantomHarvest(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("livingOre")
                                .executes(context -> triggerLivingOre(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerLivingOre(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("projectedShadow")
                                .executes(context -> triggerProjectedShadow(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerProjectedShadow(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("giantSun")
                                .executes(context -> triggerGiantSun(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerGiantSun(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("hunterFog")
                                .executes(context -> triggerHunterFog(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerHunterFog(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("grandEvent")
                                .executes(context -> triggerGrandEvent(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerGrandEvent(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("grandEventStop")
                                .executes(context -> stopGrandEvent(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> stopGrandEvent(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("tensionBuilderStart")
                                .executes(context -> tensionBuilderStart(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> tensionBuilderStart(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("tensionBuilderStop")
                                .executes(context -> tensionBuilderStop(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> tensionBuilderStop(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("animalStareLock")
                                .executes(context -> triggerAnimalStareLock(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerAnimalStareLock(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("bedsideOpen")
                                .executes(context -> triggerBedsideOpen(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerBedsideOpen(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("compassLiar")
                                .executes(context -> triggerCompassLiar(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerCompassLiar(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("furnaceBreath")
                                .executes(context -> triggerFurnaceBreath(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerFurnaceBreath(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("misplacedLight")
                                .executes(context -> triggerMisplacedLight(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerMisplacedLight(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("petRefusal")
                                .executes(context -> triggerPetRefusal(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerPetRefusal(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("workbenchReject")
                                .executes(context -> triggerWorkbenchReject(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerWorkbenchReject(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("falseContainerOpen")
                                .executes(context -> triggerFalseContainerOpen(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerFalseContainerOpen(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("leverAnswer")
                                .executes(context -> triggerLeverAnswer(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerLeverAnswer(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("pressurePlateReply")
                                .executes(context -> triggerPressurePlateReply(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerPressurePlateReply(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("campfireCough")
                                .executes(context -> triggerCampfireCough(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerCampfireCough(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("bucketDrip")
                                .executes(context -> triggerBucketDrip(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerBucketDrip(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("hotbarWrongCount")
                                .executes(context -> triggerHotbarWrongCount(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerHotbarWrongCount(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("falseRecipeToast")
                                .executes(context -> triggerFalseRecipeToast(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerFalseRecipeToast(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("corruptToast")
                                .executes(context -> triggerFalseRecipeToast(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerFalseRecipeToast(context, EntityArgument.getPlayer(context, "target")))))
                        .then(Commands.literal("toolAnswer")
                                .executes(context -> triggerToolAnswer(context, getCallerPlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> triggerToolAnswer(context, EntityArgument.getPlayer(context, "target"))))))
                .then(Commands.literal("eventVariant")
                        .then(Commands.argument("event", StringArgumentType.word())
                                .suggests(UncannyCommandRegistry::suggestEventVariantEvents)
                                .then(Commands.argument("variant", StringArgumentType.word())
                                        .suggests(UncannyCommandRegistry::suggestEventVariantVariants)
                                        .executes(context -> triggerEventVariant(context, getCallerPlayer(context)))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(context -> triggerEventVariant(context, EntityArgument.getPlayer(context, "target")))))))
                .then(Commands.literal("grantObserved")
                        .executes(context -> grantObserved(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> grantObserved(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("giveAllTomes")
                        .executes(context -> giveAllTomes(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> giveAllTomes(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("forceStructureVariant")
                        .then(Commands.argument("feature", StringArgumentType.word())
                                .then(Commands.argument("variant", StringArgumentType.word())
                                        .executes(context -> forceStructureVariant(context, getCallerPlayer(context)))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(context -> forceStructureVariant(
                                                        context,
                                                        EntityArgument.getPlayer(context, "target")))))))
                .then(Commands.literal("locate")
                        .then(Commands.literal("structure")
                                .then(Commands.argument("feature", StringArgumentType.word())
                                        .suggests(UncannyCommandRegistry::suggestUncannyStructureIds)
                                        .executes(UncannyCommandRegistry::locateUncannyStructure))))
                .then(Commands.literal("triggerEnderman")
                        .executes(context -> triggerEnderman(context, getCallerPlayer(context)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> triggerEnderman(context, EntityArgument.getPlayer(context, "target"))))));

        // Vanilla /locate structure is intentionally left untouched.
        // Use /uncanny locate structure <id> for custom sub-variant queries.
    }

    private static int setPhase(CommandContext<CommandSourceStack> context) {
        int phase = IntegerArgumentType.getInteger(context, "phase");
        UncannyPhaseManager.setPhase(context.getSource().getServer(), phase);

        UncannyWorldState state = UncannyWorldState.get(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.literal("UncannyPhase set to " + state.getCurrentPhaseIndex()), true);
        return 1;
    }

    private static int addPhaseProgress(CommandContext<CommandSourceStack> context) {
        double amount = DoubleArgumentType.getDouble(context, "amount");
        UncannyPhaseManager.addPhaseProgress(context.getSource().getServer(), amount);

        UncannyWorldState state = UncannyWorldState.get(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.literal("UncannyPhase progress is now " + String.format("%.3f", state.getProgressToNextPhase())), true);
        return 1;
    }

    private static int setEventProfile(CommandContext<CommandSourceStack> context) {
        int profile = IntegerArgumentType.getInteger(context, "profile");
        UncannyConfig.EVENT_INTENSITY_PROFILE.set(profile);
        UncannyConfig.SPEC.save();
        context.getSource().sendSuccess(
                () -> Component.literal("Uncanny event intensity profile set to " + profile + " (1=soft, 5=extreme)."),
                true);
        return 1;
    }

    private static int setDangerLevel(CommandContext<CommandSourceStack> context) {
        int level = IntegerArgumentType.getInteger(context, "level");
        UncannyConfig.EVENT_DANGER_LEVEL.set(level);
        UncannyConfig.SPEC.save();
        context.getSource().sendSuccess(
                () -> Component.literal("Uncanny danger level set to " + level + " (0=safe special behavior, 5=maximum danger)."),
                true);
        return 1;
    }

    private static int setDebugLogs(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        UncannyConfig.DEBUG_LOGS.set(enabled);
        UncannyConfig.SPEC.save();
        UncannyWorldState state = UncannyWorldState.get(context.getSource().getServer());
        state.setDebugLogsEnabled(enabled);
        context.getSource().sendSuccess(
                () -> Component.literal("Uncanny debug logs " + (enabled ? "enabled" : "disabled")
                        + " (world-persistent + config)."),
                true);
        return 1;
    }

    private static int tensionBuilderStart(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean started = UncannyParanoiaEventSystem.triggerTensionBuilderStart(target);
        if (!started) {
            context.getSource().sendFailure(Component.literal("Failed to start TensionBuilder for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Started TensionBuilder for " + target.getName().getString()), true);
        return 1;
    }

    private static int tensionBuilderStop(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean stopped = UncannyParanoiaEventSystem.triggerTensionBuilderStop(target);
        if (!stopped) {
            context.getSource().sendFailure(Component.literal("Failed to stop TensionBuilder for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Stopped TensionBuilder for " + target.getName().getString()), true);
        return 1;
    }

    private static int tensionBuilderStatus(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        context.getSource().sendSuccess(() -> Component.literal(UncannyParanoiaEventSystem.getTensionBuilderStatus(target)), false);
        return 1;
    }

    private static int openDevMenu(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        UncannyDevQaStateService.openMenu(target);
        context.getSource().sendSuccess(() -> Component.literal("Opened Uncanny Dev Debug Menu for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerWeather(CommandContext<CommandSourceStack> context) {
        String type = StringArgumentType.getString(context, "type");
        boolean started = UncannyWeatherSystem.forceTrigger(context.getSource().getServer(), type);
        if (!started) {
            context.getSource().sendFailure(Component.literal(
                    "Unknown or unavailable weather type: " + type
                            + " (valid: rain_silent, rain_dry_storm, rain_ash, rain_sobbing, thunder_silent, thunder_artificial, thunder_target_strike, thunder_stroboscopic, fog_breathing, fog_black, fog_static_wall, sky_fake_morning, sky_empty, sky_pressure)"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered weather event: " + type), true);
        return 1;
    }

    private static int stopWeather(CommandContext<CommandSourceStack> context) {
        UncannyWeatherSystem.forceStop(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.literal("Stopped active weather corruption event."), true);
        return 1;
    }

    private static int spawnUncanny(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        String type = StringArgumentType.getString(context, "type");
        String normalizedType = type.toLowerCase(Locale.ROOT);
        EntityType<? extends Mob> entityType = UncannyEntityRegistry.byCommandType(type);
        if (entityType == null) {
            context.getSource().sendFailure(Component.literal("Unknown uncanny type: " + type));
            return 0;
        }

        if (entityType == UncannyEntityRegistry.UNCANNY_WATCHER.get()) {
            boolean spawned = UncannyWatcherSystem.forceSpawnWatcher(target);
            return completeSpecialSpawn(context, spawned, "Watcher?", target);
        }
        if (entityType == UncannyEntityRegistry.UNCANNY_SHADOW.get()) {
            boolean spawned = UncannyParanoiaEventSystem.spawnShadowForCommand(target);
            return completeSpecialSpawn(context, spawned, "Shadow?", target);
        }
        if (entityType == UncannyEntityRegistry.UNCANNY_HURLER.get()) {
            boolean spawned = UncannyParanoiaEventSystem.spawnHurlerForCommand(target);
            return completeSpecialSpawn(context, spawned, "Hurler?", target);
        }
        if (entityType == UncannyEntityRegistry.UNCANNY_STALKER.get()) {
            boolean spawned = UncannyParanoiaEventSystem.spawnStalkerForCommand(target);
            return completeSpecialSpawn(context, spawned, "Attacker?", target);
        }
        if (entityType == UncannyEntityRegistry.UNCANNY_KNOCKER.get()) {
            boolean spawned = UncannyParanoiaEventSystem.spawnKnockerForCommand(target);
            return completeSpecialSpawn(context, spawned, "Knocker?", target);
        }
        if (entityType == UncannyEntityRegistry.UNCANNY_PULSE.get()) {
            boolean spawned = UncannyParanoiaEventSystem.spawnPulseForCommand(target);
            return completeSpecialSpawn(context, spawned, "Presence?", target);
        }

        Mob entity = entityType.create(target.serverLevel());
        if (entity == null) {
            context.getSource().sendFailure(Component.literal("Failed to create entity for type: " + type));
            return 0;
        }

        entity.moveTo(target.getX(), target.getY() + 0.5D, target.getZ(), target.getYRot(), 0.0F);
        if (entity instanceof UncannyDoubleDormantEntity doubleDormant) {
            doubleDormant.copyTarget(target, target.blockPosition(), target.blockPosition());
        }
        target.serverLevel().addFreshEntity(entity);
        context.getSource().sendSuccess(() -> Component.literal("Spawned " + entityType.getDescription().getString()), true);
        return 1;
    }

    private static int forcePassive(CommandContext<CommandSourceStack> context, ServerPlayer target, int radius) {
        int variant = IntegerArgumentType.getInteger(context, "variant");
        int count = UncannyPassiveVariantSystem.forcePassiveVariantsAround(target, radius, variant);
        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Forced passive variant " + variant + " on " + count + " mobs around "
                                + target.getName().getString() + " within " + radius + " blocks."),
                true);
        return count;
    }

    private static int completeSpecialSpawn(
            CommandContext<CommandSourceStack> context,
            boolean spawned,
            String label,
            ServerPlayer target) {
        if (!spawned) {
            context.getSource().sendFailure(Component.literal("Failed to spawn " + label + " for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Spawned " + label + " for " + target.getName().getString()), true);
        return 1;
    }

    private static int forceMimic(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        UncannyDoubleDormantSystem.forceMimic(target);
        context.getSource().sendSuccess(() -> Component.literal("Forced Mimic event for " + target.getName().getString()), true);
        return 1;
    }

    private static int debugMimic(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        String report = UncannyDoubleDormantSystem.getMimicDebugReport(target);
        context.getSource().sendSuccess(() -> Component.literal(report), false);
        return 1;
    }

    private static int debugEvents(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        String report = UncannyParanoiaEventSystem.getAutoEventDebugReport(target);
        context.getSource().sendSuccess(() -> Component.literal(report), false);
        return 1;
    }

    private static int debugSpecialRoll(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        String report = UncannyParanoiaEventSystem.debugForceRandomSpecialRoll(target);
        context.getSource().sendSuccess(() -> Component.literal(report), false);
        return 1;
    }

    private static int spawnWatcher(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean spawned = UncannyWatcherSystem.forceSpawnWatcher(target);
        if (!spawned) {
            context.getSource().sendFailure(Component.literal("Failed to spawn Watcher for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Spawned Watcher for " + target.getName().getString()), true);
        return 1;
    }

    private static int spawnShadow(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean spawned = UncannyParanoiaEventSystem.spawnShadowForCommand(target);
        if (!spawned) {
            context.getSource().sendFailure(Component.literal("Failed to spawn Shadow? for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Spawned Shadow? for " + target.getName().getString()), true);
        return 1;
    }

    private static int spawnHurler(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean spawned = UncannyParanoiaEventSystem.spawnHurlerForCommand(target);
        if (!spawned) {
            context.getSource().sendFailure(Component.literal("Failed to spawn Hurler? for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Spawned Hurler? for " + target.getName().getString()), true);
        return 1;
    }

    private static int spawnStalker(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean spawned = UncannyParanoiaEventSystem.spawnStalkerForCommand(target);
        if (!spawned) {
            context.getSource().sendFailure(Component.literal("Failed to spawn Attacker? for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Spawned Attacker? for " + target.getName().getString()), true);
        return 1;
    }

    private static int spawnKnocker(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean spawned = UncannyParanoiaEventSystem.spawnKnockerForCommand(target);
        if (!spawned) {
            context.getSource().sendFailure(Component.literal("Failed to spawn Knocker? for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Spawned Knocker? for " + target.getName().getString()), true);
        return 1;
    }

    private static int spawnPulse(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean spawned = UncannyParanoiaEventSystem.spawnPulseForCommand(target);
        if (!spawned) {
            context.getSource().sendFailure(Component.literal("Failed to spawn Presence? for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(
                () -> Component.literal("Spawned Presence? for " + target.getName().getString()
                        + " (invisible; listen for heartbeat)."),
                true);
        return 1;
    }

    private static int spawnUsher(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean spawned = UncannyParanoiaEventSystem.spawnUsherForCommand(target);
        if (!spawned) {
            context.getSource().sendFailure(Component.literal("Failed to spawn Usher? for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Spawned Usher? for " + target.getName().getString()), true);
        return 1;
    }

    private static int spawnKeeper(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean spawned = UncannyParanoiaEventSystem.spawnKeeperForCommand(target);
        if (!spawned) {
            context.getSource().sendFailure(Component.literal("Failed to spawn Keeper? for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Spawned Keeper? for " + target.getName().getString()), true);
        return 1;
    }

    private static int spawnTenant(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean spawned = UncannyParanoiaEventSystem.spawnTenantForCommand(target);
        if (!spawned) {
            context.getSource().sendFailure(Component.literal("Failed to spawn Tenant? for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Spawned Tenant? for " + target.getName().getString()), true);
        return 1;
    }

    private static int spawnFollower(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean spawned = UncannyParanoiaEventSystem.spawnFollowerForCommand(target);
        if (!spawned) {
            context.getSource().sendFailure(Component.literal("Failed to spawn Follower? for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Spawned Follower? for " + target.getName().getString()), true);
        return 1;
    }

    private static int spawnPhantomLanternEater(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean spawned = UncannyParanoiaEventSystem.spawnPhantomLanternEaterForCommand(target);
        if (!spawned) {
            context.getSource().sendFailure(Component.literal("Failed to spawn Phantom? lantern_eater mode for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Spawned Phantom? (lantern_eater mode) for " + target.getName().getString()), true);
        return 1;
    }

    private static int forceFoxCry(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyPassiveVariantSystem.forceFoxCry(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("No Fox found near " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Forced Fox? scream near " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerBlackout(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerTotalBlackout(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Total Blackout for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Total Blackout for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerFootsteps(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerFootstepsBehind(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Footsteps Behind for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Footsteps Behind for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerFlash(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerFlashError(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Flash Error for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Flash Error for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerBaseReplay(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerBaseReplay(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Base Replay for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Base Replay for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerBell(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerBell(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Bell event for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Bell event for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerFlashRed(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerFlashRed(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Flash Red for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Flash Red for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerVoidSilence(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerVoidSilence(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Void Silence for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Void Silence for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerFalseFall(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerFalseFall(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger False Fall for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered False Fall for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerGhostMiner(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerGhostMiner(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Ghost Miner for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Ghost Miner for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerCaveCollapse(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerCaveCollapse(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Cave Collapse for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Cave Collapse for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerFalseInjury(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerFalseInjury(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger False Injury for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered False Injury for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerForceDrop(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerForcedDrop(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Forced Drop for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Forced Drop for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerCorruptMessage(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerCorruptedMessage(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Corrupt Message for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Corrupt Message for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerBed(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerBedDisturbance(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to arm Bed Disturbance for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(
                () -> Component.literal("Armed Bed Disturbance for " + target.getName().getString()
                        + " (click a bed 3 times to force Presence?)."),
                true);
        return 1;
    }

    private static int triggerAsphyxia(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerAsphyxia(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Asphyxia for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Asphyxia for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerArmorBreak(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerArmorBreak(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Armor Break for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Armor Break for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerAquaticSteps(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerAquaticSteps(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Aquatic Steps for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Aquatic Steps for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerDoorInversion(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerDoorInversion(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Door Inversion for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Door Inversion for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerPhantomHarvest(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerPhantomHarvest(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Phantom Harvest for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Phantom Harvest for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerLivingOre(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerLivingOre(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Living Ore for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Living Ore for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerProjectedShadow(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerProjectedShadow(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Projected Shadow for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Projected Shadow for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerGiantSun(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerGiantSun(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Giant Sun for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Giant Sun for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerHunterFog(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerHunterFog(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Hunter Fog for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Hunter Fog for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerGrandEvent(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerGrandEventWarden(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Grand Event (Warden) for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Grand Event (Warden) for " + target.getName().getString()), true);
        return 1;
    }

    private static int stopGrandEvent(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean stopped = UncannyParanoiaEventSystem.triggerGrandEventStop(target);
        if (!stopped) {
            context.getSource().sendFailure(Component.literal("No active Grand Event to stop for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Stopped Grand Event for " + target.getName().getString()), true);
        return 1;
    }

    private static int grandEventStatus(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        context.getSource().sendSuccess(() -> Component.literal(UncannyParanoiaEventSystem.getGrandEventStatus(target)), false);
        return 1;
    }

    private static int triggerAnimalStareLock(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerAnimalStareLock(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Animal Stare Lock for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Animal Stare Lock for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerBedsideOpen(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerBedsideOpen(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Bedside Open for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Bedside Open for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerCompassLiar(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerCompassLiar(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Compass Liar for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Compass Liar for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerFurnaceBreath(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerFurnaceBreath(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Furnace Breath for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Furnace Breath for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerMisplacedLight(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerMisplacedLight(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Misplaced Light for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Misplaced Light for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerPetRefusal(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerPetRefusal(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Pet Refusal for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Pet Refusal for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerWorkbenchReject(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerWorkbenchReject(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Workbench Reject for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Workbench Reject for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerFalseContainerOpen(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerFalseContainerOpen(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger False Container Open for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered False Container Open for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerLeverAnswer(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerLeverAnswer(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Lever Answer for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Lever Answer for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerPressurePlateReply(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerPressurePlateReply(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Pressure Plate Reply for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Pressure Plate Reply for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerCampfireCough(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerCampfireCough(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Campfire Cough for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Campfire Cough for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerBucketDrip(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerBucketDrip(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Bucket Drip for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Bucket Drip for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerHotbarWrongCount(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerHotbarWrongCount(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Hotbar Wrong Count for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Hotbar Wrong Count for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerFalseRecipeToast(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerFalseRecipeToast(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Corrupt Toast for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Corrupt Toast for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerToolAnswer(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        boolean triggered = UncannyParanoiaEventSystem.triggerToolAnswer(target);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal("Failed to trigger Tool Answer for " + target.getName().getString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Triggered Tool Answer for " + target.getName().getString()), true);
        return 1;
    }

    private static int triggerEventVariant(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        String event = StringArgumentType.getString(context, "event");
        String variant = StringArgumentType.getString(context, "variant");
        boolean triggered = UncannyParanoiaEventSystem.triggerEventVariant(target, event, variant);
        if (!triggered) {
            context.getSource().sendFailure(Component.literal(
                    "Failed to trigger event variant '" + event + " " + variant + "' for " + target.getName().getString()
                            + "."));
            return 0;
        }
        context.getSource().sendSuccess(
                () -> Component.literal("Triggered event variant '" + event + " " + variant + "' for " + target.getName().getString()),
                true);
        return 1;
    }

    private static int grantObserved(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        if (target.getServer() == null) {
            context.getSource().sendFailure(Component.literal("Server unavailable for advancement grant."));
            return 0;
        }

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny/observed");
        AdvancementHolder advancement = target.getServer().getAdvancements().get(id);
        if (advancement == null) {
            context.getSource().sendFailure(Component.literal("Advancement not found: " + id));
            return 0;
        }

        AdvancementProgress progress = target.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            target.getAdvancements().award(advancement, criterion);
        }

        boolean done = target.getAdvancements().getOrStartProgress(advancement).isDone();
        if (!done) {
            context.getSource().sendFailure(Component.literal("Advancement grant did not complete for " + target.getName().getString()));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("Granted Observed advancement to " + target.getName().getString()), true);
        return 1;
    }

    private static int giveAllTomes(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        int given = 0;
        for (int volume = 1; volume <= UncannyLoreBookLibrary.volumeCount(); volume++) {
            ItemStack tome = new ItemStack(UncannyItemRegistry.UNCANNY_LORE_PIECE.get());
            tome.set(DataComponents.WRITTEN_BOOK_CONTENT, UncannyLoreBookLibrary.contentForVolume(volume));
            if (!target.getInventory().add(tome)) {
                target.drop(tome, false);
            }
            given++;
        }
        int total = given;
        context.getSource().sendSuccess(
                () -> Component.literal("Gave " + total + " Uncanny Lore tomes to " + target.getName().getString()),
                true);
        return total;
    }

    private static int triggerEnderman(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        AABB searchBox = target.getBoundingBox().inflate(48.0D);
        UncannyEndermanEntity uncannyEnderman = target.serverLevel()
                .getEntitiesOfClass(UncannyEndermanEntity.class, searchBox, entity -> entity.isAlive())
                .stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(target), b.distanceToSqr(target)))
                .orElse(null);

        if (uncannyEnderman == null) {
            uncannyEnderman = UncannyEntityRegistry.UNCANNY_ENDERMAN.get().create(target.serverLevel());
            if (uncannyEnderman == null) {
                context.getSource().sendFailure(Component.literal("Failed to create Enderman?."));
                return 0;
            }
            uncannyEnderman.moveTo(target.getX() + 2.0D, target.getY(), target.getZ() + 2.0D, target.getYRot(), 0.0F);
            target.serverLevel().addFreshEntity(uncannyEnderman);
        }

        uncannyEnderman.forceTrackerSequence(target);
        context.getSource().sendSuccess(() -> Component.literal("Triggered Enderman? tracker on " + target.getName().getString()), true);
        return 1;
    }

    private static int forceStructureVariant(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        String feature = StringArgumentType.getString(context, "feature");
        String variant = StringArgumentType.getString(context, "variant");
        boolean generated = UncannyStructureFeatureSystem.forceGenerateFeatureVariant(target, feature, variant);
        if (!generated) {
            context.getSource().sendFailure(Component.literal(
                    "Failed to generate structure '" + feature + "' with variant '" + variant + "' near "
                            + target.getName().getString() + "."));
            return 0;
        }
        context.getSource().sendSuccess(
                () -> Component.literal("Generated structure '" + feature + "' variant '" + variant + "' near " + target.getName().getString()),
                true);
        return 1;
    }

    private static int locateUncannyStructure(CommandContext<CommandSourceStack> context) {
        return locateUncannyStructure(context, StringArgumentType.getString(context, "feature"));
    }

    private static int locateUncannyStructure(CommandContext<CommandSourceStack> context, String rawFeature) {
        String feature = normalizeUncannyStructureId(rawFeature);
        String markerType = LOCATE_STRUCTURE_CANONICAL_MARKER.get(feature);
        if (markerType == null) {
            context.getSource().sendFailure(Component.literal(
                    "Unknown uncanny structure '" + rawFeature + "'. Use: " + String.join(", ", LOCATE_STRUCTURE_IDS)));
            return 0;
        }

        ServerPlayer player;
        try {
            player = getCallerPlayer(context);
        } catch (CommandSyntaxException exception) {
            context.getSource().sendFailure(Component.literal("This locate variant requires a player executor."));
            return 0;
        }

        UncannyWorldState state = UncannyWorldState.get(context.getSource().getServer());
        BlockPos nearest = state.findNearestStructureMarker(markerType, player.serverLevel().dimension(), player.blockPosition());
        boolean houseVariantOnly = "false_ascent_house".equals(feature) || "false_descent_house".equals(feature);
        if (nearest == null) {
            if (houseVariantOnly) {
                context.getSource().sendFailure(Component.literal(
                        "No generated uncanny structure '" + feature + "' found in this dimension yet."));
                return 0;
            }
            nearest = UncannyStructureFeatureSystem.findNearestPlannedStructure(
                    player.serverLevel(),
                    feature,
                    player.blockPosition());
            if (nearest == null) {
                context.getSource().sendFailure(Component.literal(
                        "No generated or planned uncanny structure '" + feature + "' found in this dimension."));
                return 0;
            }
            double plannedDistance = Math.sqrt(nearest.distSqr(player.blockPosition()));
            BlockPos plannedPos = nearest;
            context.getSource().sendSuccess(() -> Component.literal(
                    "Nearest uncanny structure '" + feature + "' is planned near ["
                            + plannedPos.getX() + " " + plannedPos.getY() + " " + plannedPos.getZ()
                            + "] (" + String.format(java.util.Locale.ROOT, "%.1f", plannedDistance)
                            + " blocks). It will appear when that area generates."),
                    false);
            return 1;
        }

        double distance = Math.sqrt(nearest.distSqr(player.blockPosition()));
        BlockPos foundPos = nearest;
        context.getSource().sendSuccess(() -> Component.literal(
                "Nearest uncanny structure '" + feature + "' is at ["
                        + foundPos.getX() + " " + foundPos.getY() + " " + foundPos.getZ()
                        + "] (" + String.format(java.util.Locale.ROOT, "%.1f", distance) + " blocks)."), false);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestUncannyStructureIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        List<String> ids = LOCATE_STRUCTURE_IDS.stream()
                .flatMap(id -> java.util.stream.Stream.of(id, EchoOfTheVoid.MODID + ":" + id))
                .toList();
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    private static CompletableFuture<Suggestions> suggestEventVariantEvents(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        java.util.LinkedHashSet<String> variants = new java.util.LinkedHashSet<>(EVENT_VARIANT_EVENTS);
        for (String key : EVENT_VARIANT_EVENTS) {
            variants.add(key.replace("_", ""));
            variants.add(toCamelCase(key, false));
            variants.add(toCamelCase(key, true));
        }
        return SharedSuggestionProvider.suggest(variants, builder);
    }

    private static CompletableFuture<Suggestions> suggestEventVariantVariants(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        String event = normalizeEventVariantEventKey(StringArgumentType.getString(context, "event"));
        List<String> variants = EVENT_VARIANTS.getOrDefault(event, List.of());
        java.util.LinkedHashSet<String> all = new java.util.LinkedHashSet<>(variants);
        for (String key : variants) {
            all.add(key.replace("_", ""));
            all.add(toCamelCase(key, false));
            all.add(toCamelCase(key, true));
        }
        return SharedSuggestionProvider.suggest(all, builder);
    }

    private static String normalizeEventVariantEventKey(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if (EVENT_VARIANTS.containsKey(normalized)) {
            return normalized;
        }
        String compact = normalized.replace("_", "");
        for (String candidate : EVENT_VARIANTS.keySet()) {
            if (candidate.replace("_", "").equals(compact)) {
                return candidate;
            }
        }
        return normalized;
    }

    private static String toCamelCase(String value, boolean capitalizeFirst) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        boolean upper = capitalizeFirst;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '_' || c == '-' || c == ' ') {
                upper = true;
                continue;
            }
            if (builder.isEmpty() && !capitalizeFirst) {
                builder.append(Character.toLowerCase(c));
                upper = false;
                continue;
            }
            builder.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return builder.toString();
    }

    private static String normalizeUncannyStructureId(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().toLowerCase(java.util.Locale.ROOT);
        String prefix = EchoOfTheVoid.MODID + ":";
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private static ServerPlayer getCallerPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            return player;
        }
        throw PLAYER_REQUIRED_EXCEPTION.create();
    }
}

