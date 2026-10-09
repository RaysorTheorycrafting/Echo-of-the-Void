package com.eotv.echoofthevoid.dev;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyDoubleDormantEntity;
import com.eotv.echoofthevoid.entity.variant.ReplacementVariantExpansionCatalog;
import com.eotv.echoofthevoid.entity.variant.ReplacementVariantExpansionSystem;
import com.eotv.echoofthevoid.event.UncannyDoubleDormantSystem;
import com.eotv.echoofthevoid.event.UncannyDebugBoundsEventSystem;
import com.eotv.echoofthevoid.event.UncannyParanoiaEventSystem;
import com.eotv.echoofthevoid.event.paranoia.nativeevent.WanderingTreeSystem;
import com.eotv.echoofthevoid.event.UncannyPassiveVariantSystem;
import com.eotv.echoofthevoid.event.passive.ApprovedVanillaVariantSystem;
import com.eotv.echoofthevoid.event.special.ApprovedSpecialSystem;
import com.eotv.echoofthevoid.event.special.DevourerArenaSystem;
import com.eotv.echoofthevoid.event.special.UncannyDevourerSystem;
import com.eotv.echoofthevoid.event.special.UncannyHuntingSpecialSystem;
import com.eotv.echoofthevoid.event.special.UncannyMinerSystem;
import com.eotv.echoofthevoid.event.UncannyStructureFeatureSystem;
import com.eotv.echoofthevoid.event.UncannyWatcherSystem;
import com.eotv.echoofthevoid.event.UncannyWeatherSystem;
import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import com.eotv.echoofthevoid.phase.UncannyPhaseManager;
import com.eotv.echoofthevoid.sound.UncannySoundDelivery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class UncannyDevActionExecutor {
    public static final String DEV_SPAWNED_TAG = "eotv_dev_spawned";
    private static final int DEFAULT_SPAWN_DISTANCE = 4;
    private static final int MAX_CLEANUP_RADIUS = 256;

    private UncannyDevActionExecutor() {
    }

    public static boolean execute(ServerPlayer target, UncannyDevCatalog.Entry entry) {
        return executeDetailed(target, entry, DEFAULT_SPAWN_DISTANCE).success();
    }

    public static ExecutionResult executeDetailed(
            ServerPlayer target,
            UncannyDevCatalog.Entry entry,
            int requestedSpawnDistance) {
        if (target == null || entry == null || target.getServer() == null) {
            return ExecutionResult.failure("Missing target, catalog entry or server context.");
        }

        int spawnDistance = Math.max(2, Math.min(24, requestedSpawnDistance));
        return switch (entry.actionKind()) {
            case SPAWN_UNCANNY -> result(
                    spawnUncanny(target, entry.actionArg(), spawnDistance),
                    "Entity spawned at a checked position.",
                    "Entity type could not be created or no collision-free position was found.");
            case SPAWN_UNCANNY_FORCED -> result(
                    spawnUncannyForcedVariant(target, entry.actionArg(), spawnDistance),
                    "Forced entity variant spawned.",
                    "Forced variant arguments are invalid or the entity could not be placed.");
            case SPAWN_PASSIVE_FORCED -> result(
                    spawnPassiveForcedVariant(target, entry.actionArg()),
                    "Passive variant spawned.",
                    "Passive type or variant is invalid, or its required environment was not found.");
            case SPAWN_SPECIAL -> spawnSpecialDetailed(target, entry.actionArg());
            case FORCE_MIMIC -> result(
                    UncannyDoubleDormantSystem.forceMimicChecked(target),
                    "Mimic event started.",
                    "Mimic? could not be added to the target level.");
            case TRIGGER_EVENT -> result(
                    triggerEvent(target, entry.actionArg()),
                    "Event accepted by its runtime trigger.",
                    "Event rejected its current context or is already active.");
            case TRIGGER_VARIANT -> result(
                    triggerVariant(target, entry.actionArg()),
                    "Variant action completed.",
                    "Variant action is unavailable in the current context.");
            case GIVE_ITEM -> result(
                    giveItem(target, entry.actionArg()),
                    "Debug item added to the target inventory.",
                    "Item is unknown or the target inventory rejected it.");
            case TRIGGER_WEATHER -> result(
                    UncannyWeatherSystem.forceTrigger(target.getServer(), entry.actionArg()),
                    "Weather event started.",
                    "Weather event is unknown or incompatible with the current state.");
            case STOP_WEATHER -> {
                UncannyWeatherSystem.forceStop(target.getServer());
                yield ExecutionResult.success("Active uncanny weather cleared.");
            }
            case TRIGGER_STRUCTURE -> result(
                    triggerStructure(target, entry.actionArg()),
                    "Structure feature generated.",
                    "No safe and compatible terrain was found in the generator search area.");
            case TRIGGER_SECRET_HOUSE -> result(
                    UncannyStructureFeatureSystem.forceGenerateSecretHouseForDebug(target),
                    "Secret house generated.",
                    "Secret house generation found no compatible terrain.");
            case PLAY_SOUND_PHYSICAL -> playSound(target, entry.actionArg(), false);
            case PLAY_SOUND_MENTAL -> playSound(target, entry.actionArg(), true);
            case CLEAN_TEST_ENTITIES -> cleanupTestEntities(target);
            case RESET_TEST_ENVIRONMENT -> resetTransientTestState(target);
            case SET_PHASE -> setPhase(target, entry.actionArg());
        };
    }

    private static ExecutionResult result(boolean success, String successMessage, String failureMessage) {
        return success ? ExecutionResult.success(successMessage) : ExecutionResult.failure(failureMessage);
    }

    private static ExecutionResult playSound(ServerPlayer target, String soundPath, boolean mental) {
        if (soundPath == null || soundPath.isBlank()) {
            return ExecutionResult.failure("No sound identifier is configured for this entry.");
        }
        ResourceLocation soundId = ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, soundPath);
        if (!BuiltInRegistries.SOUND_EVENT.containsKey(soundId)) {
            return ExecutionResult.failure("Unknown registered sound: " + soundId);
        }
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundId);
        if (mental) {
            UncannySoundDelivery.playMental(target, sound, SoundSource.AMBIENT, 0.65F, 1.0F, 120);
            return ExecutionResult.success("Private mental route sent only to " + target.getGameProfile().getName() + ".");
        }
        target.serverLevel().playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                sound,
                SoundSource.AMBIENT,
                0.8F,
                1.0F);
        return ExecutionResult.success("Spatial sound played at the target position for nearby players.");
    }

    private static ExecutionResult cleanupTestEntities(ServerPlayer target) {
        int removed = 0;
        for (Mob mob : target.serverLevel().getEntitiesOfClass(
                Mob.class,
                target.getBoundingBox().inflate(MAX_CLEANUP_RADIUS),
                mob -> mob.getTags().contains(DEV_SPAWNED_TAG))) {
            mob.discard();
            removed++;
        }
        return ExecutionResult.success("Removed " + removed + " entity/entities created by the dev menu.", removed);
    }

    private static ExecutionResult resetTransientTestState(ServerPlayer target) {
        ExecutionResult cleanup = cleanupTestEntities(target);
        UncannyWeatherSystem.forceStop(target.getServer());
        UncannyParanoiaEventSystem.triggerGrandEventStop(target);
        UncannyParanoiaEventSystem.triggerTensionBuilderStop(target);
        int miners = UncannyMinerSystem.abortForTarget(target);
        boolean arena = DevourerArenaSystem.abandonSession(target);
        return ExecutionResult.success(
                "Transient weather, Grand Warden, Tension Builder, " + miners + " Miner? tunnel(s), "
                        + (arena ? "one Devourer? arena" : "no Devourer? arena") + " and "
                        + cleanup.affectedEntities()
                        + " dev entity/entities were cleared.",
                cleanup.affectedEntities() + miners);
    }

    private static ExecutionResult setPhase(ServerPlayer target, String rawPhase) {
        int phase;
        try {
            phase = Integer.parseInt(rawPhase);
        } catch (NumberFormatException exception) {
            return ExecutionResult.failure("Invalid phase value: " + rawPhase);
        }
        if (phase < 1 || phase > 4) {
            return ExecutionResult.failure("Dev phase must be between 1 and 4.");
        }
        UncannyPhaseManager.setPhase(target.getServer(), phase);
        return ExecutionResult.success("World phase set to " + phase + "; natural progression remains unlocked.");
    }

    private static boolean triggerStructure(ServerPlayer target, String actionArg) {
        if (actionArg == null || actionArg.isBlank()) {
            return false;
        }
        String[] parts = actionArg.split("\\|", 2);
        if (parts.length == 2) {
            return UncannyStructureFeatureSystem.forceGenerateFeatureVariant(target, parts[0], parts[1]);
        }
        return UncannyStructureFeatureSystem.forceGenerateFeature(target, actionArg);
    }

    private static boolean spawnSpecial(ServerPlayer target, String specialId) {
        String[] requested = specialId.split("\\|", 2);
        String requestedId = requested[0];
        String requestedState = requested.length == 2 ? requested[1] : "normal";
        if (switch (requestedId) {
            case "echoer", "drifter", "ashwalker", "dredger", "flanker" -> true;
            default -> false;
        }) {
            return UncannyHuntingSpecialSystem.spawnForDebug(target, requestedId, requestedState);
        }
        return switch (specialId) {
            case "watcher" -> UncannyWatcherSystem.forceSpawnWatcher(target);
            case "sleeper" -> com.eotv.echoofthevoid.event.special.SleeperSystem.spawnForDebug(target);
            case "blur" -> com.eotv.echoofthevoid.event.special.BlurSystem.spawnForDebug(target);
            case "percher" -> com.eotv.echoofthevoid.event.special.PercherSystem.spawnForDebug(target);
            case "shadow" -> UncannyParanoiaEventSystem.spawnShadowForCommand(target);
            case "hurler" -> UncannyParanoiaEventSystem.spawnHurlerForCommand(target);
            case "attacker" -> UncannyParanoiaEventSystem.spawnStalkerForCommand(target);
            case "knocker" -> UncannyParanoiaEventSystem.spawnKnockerForCommand(target);
            case "pulse" -> UncannyParanoiaEventSystem.spawnPulseForCommand(target);
            case "usher" -> UncannyParanoiaEventSystem.spawnUsherForCommand(target);
            case "keeper" -> UncannyParanoiaEventSystem.spawnKeeperForCommand(target);
            case "tenant" -> UncannyParanoiaEventSystem.spawnTenantForCommand(target);
            case "follower" -> UncannyParanoiaEventSystem.spawnFollowerForCommand(target);
            case "ambusher" -> ApprovedSpecialSystem.spawnAmbusher(target, true);
            case "miner" -> UncannyMinerSystem.spawnDebugTunnel(target);
            case "miner_emerged" -> UncannyMinerSystem.spawnDebugEmerged(target);
            case "devourer" -> UncannyDevourerSystem.spawnDebug(target);
            case "devourer_arena" -> DevourerArenaSystem.enterDebugArena(target);
            case "devourer_arena_abandon" -> DevourerArenaSystem.abandonSession(target);
            case "ferryman_deferred" -> ApprovedSpecialSystem.armFerrymanEncounter(target);
            case "surveyor", "mourner", "doubler", "ferryman", "listener", "bystander" ->
                    ApprovedSpecialSystem.spawnForDebug(target, specialId);
            case "terror" -> spawnUncanny(target, "uncanny_terror");
            default -> false;
        };
    }

    private static ExecutionResult spawnSpecialDetailed(ServerPlayer target, String specialId) {
        String[] requested = specialId.split("\\|", 2);
        String requestedId = requested[0];
        String requestedState = requested.length == 2 ? requested[1] : "normal";
        if (switch (requestedId) {
            case "echoer", "drifter", "ashwalker", "dredger", "flanker" -> true;
            default -> false;
        }) {
            return result(
                    UncannyHuntingSpecialSystem.spawnForDebug(target, requestedId, requestedState),
                    "Hunting Special spawned through its production context and requested QA state.",
                    "The requested Special requires compatible loaded terrain and no active copy for this target.");
        }
        if ("miner".equals(specialId)) {
            return result(
                    UncannyMinerSystem.spawnDebugTunnel(target),
                    "Miner? tunnel started through the production planner.",
                    "Miner? requires loaded safe terrain, mobGriefing and no active Ghost Miner for this target.");
        }
        if ("miner_emerged".equals(specialId)) {
            return result(
                    UncannyMinerSystem.spawnDebugEmerged(target),
                    "Miner? spawned after emergence with Attacker? combat behavior.",
                    "No collision-free emerged position was found, or Miner? conflicts with an active tunnel.");
        }
        if ("devourer".equals(specialId)) {
            return result(
                    UncannyDevourerSystem.spawnDebug(target),
                    "Devourer? spawned on a checked, reachable path.",
                    "Devourer? requires a loaded safe path in a Vanilla dimension.");
        }
        if ("devourer_arena".equals(specialId)) {
            return result(
                    DevourerArenaSystem.enterDebugArena(target),
                    "Devourer? arena session created and persisted before teleportation.",
                    "The elsewhere dimension is unavailable or this player already owns an arena session.");
        }
        if ("devourer_arena_abandon".equals(specialId)) {
            return result(
                    DevourerArenaSystem.abandonSession(target),
                    "Devourer? arena abandoned, cleaned and returned safely.",
                    "This player has no Devourer? arena session to abandon.");
        }
        if ("ferryman_deferred".equals(specialId)) {
            return result(
                    ApprovedSpecialSystem.armFerrymanEncounter(target),
                    "Ferryman? encounter reserved; navigate the same boat over deep water for 10-15 seconds.",
                    "Ferryman? already has a pending or active encounter for this player.");
        }
        if (switch (specialId) {
            case "surveyor", "mourner", "doubler", "ferryman", "listener", "bystander" -> true;
            default -> false;
        }) {
            ApprovedSpecialSystem.DebugSpawnResult result =
                    ApprovedSpecialSystem.spawnForDebugDetailed(target, specialId);
            return result.success()
                    ? ExecutionResult.success(result.message())
                    : ExecutionResult.failure(result.message());
        }
        return result(
                spawnSpecial(target, specialId),
                "Special spawn routine completed.",
                "Special spawn conditions or placement failed; check the selected dimension and terrain.");
    }

    private static boolean giveItem(ServerPlayer target, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        ItemStack stack = switch (itemId) {
            case "uncanny_compass" -> new ItemStack(UncannyItemRegistry.UNCANNY_COMPASS.get());
            case "uncanny_reality_shard" -> new ItemStack(UncannyItemRegistry.UNCANNY_REALITY_SHARD.get());
            case "uncanny_reality_shard_piece" -> new ItemStack(UncannyItemRegistry.UNCANNY_REALITY_SHARD_PIECE.get());
            default -> ItemStack.EMPTY;
        };
        if (stack.isEmpty()) {
            return false;
        }
        return target.getInventory().add(stack);
    }

    private static boolean triggerEvent(ServerPlayer target, String eventId) {
        return switch (eventId) {
            case "blackout" -> UncannyParanoiaEventSystem.triggerTotalBlackout(target);
            case "footsteps" -> UncannyParanoiaEventSystem.triggerFootstepsBehind(target);
            case "flash" -> UncannyParanoiaEventSystem.triggerFlashError(target);
            case "base_replay" -> UncannyParanoiaEventSystem.triggerBaseReplay(target);
            case "bell" -> UncannyParanoiaEventSystem.triggerBell(target);
            case "flash_red" -> UncannyParanoiaEventSystem.triggerFlashRed(target);
            case "void_silence" -> UncannyParanoiaEventSystem.triggerVoidSilence(target);
            case "false_fall" -> UncannyParanoiaEventSystem.triggerFalseFall(target);
            case "ghost_miner" -> UncannyParanoiaEventSystem.triggerGhostMinerForDebug(target);
            case "cave_collapse" -> UncannyParanoiaEventSystem.triggerCaveCollapse(target);
            case "false_injury" -> UncannyParanoiaEventSystem.triggerFalseInjury(target);
            case "corrupt_message" -> UncannyParanoiaEventSystem.triggerCorruptedMessage(target);
            case "animal_stare_lock" -> UncannyParanoiaEventSystem.triggerAnimalStareLock(target);
            case "bedside_open" -> UncannyParanoiaEventSystem.triggerBedsideOpen(target);
            case "compass_liar" -> UncannyParanoiaEventSystem.triggerCompassLiar(target);
            case "furnace_breath" -> UncannyParanoiaEventSystem.triggerFurnaceBreath(target);
            case "misplaced_light" -> UncannyParanoiaEventSystem.triggerMisplacedLight(target);
            case "pet_refusal" -> UncannyParanoiaEventSystem.triggerPetRefusal(target);
            case "workbench_reject" -> UncannyParanoiaEventSystem.triggerWorkbenchReject(target);
            case "false_container_open" -> UncannyParanoiaEventSystem.triggerFalseContainerOpen(target);
            case "lever_answer" -> UncannyParanoiaEventSystem.triggerLeverAnswer(target);
            case "pressure_plate_reply" -> UncannyParanoiaEventSystem.triggerPressurePlateReply(target);
            case "campfire_cough" -> UncannyParanoiaEventSystem.triggerCampfireCough(target);
            case "bucket_drip" -> UncannyParanoiaEventSystem.triggerBucketDrip(target);
            case "hotbar_wrong_count" -> UncannyParanoiaEventSystem.triggerHotbarWrongCount(target);
            case "false_recipe_toast", "corrupt_toast" -> UncannyParanoiaEventSystem.triggerFalseRecipeToast(target);
            case "tool_answer" -> UncannyParanoiaEventSystem.triggerToolAnswer(target);
            case "bed" -> UncannyParanoiaEventSystem.triggerBedDisturbance(target);
            case "asphyxia" -> UncannyParanoiaEventSystem.triggerAsphyxia(target);
            case "aquatic_steps" -> UncannyParanoiaEventSystem.triggerAquaticSteps(target);
            case "door_inversion" -> UncannyParanoiaEventSystem.triggerDoorInversion(target);
            case "phantom_harvest" -> UncannyParanoiaEventSystem.triggerPhantomHarvest(target);
            case "living_ore" -> UncannyParanoiaEventSystem.triggerLivingOre(target);
            case "projected_shadow" -> UncannyParanoiaEventSystem.triggerProjectedShadow(target);
            case "hunter_fog" -> UncannyParanoiaEventSystem.triggerHunterFog(target);
            case "debug_bounds" -> UncannyDebugBoundsEventSystem.triggerForDebug(target);
            case "orphan_shadow", "ghost_breaking", "cold_furnace", "empty_teleport",
                    "false_animal_hurt", "stolen_pose", "fishing_tug", "leaf_reply",
                    "silent_bell", "empty_congregation", "empty_lead", "borrowed_painting",
                    "returned_drop", "ghost_cart", "misdirected_enchantment", "orphan_signal",
                    "cauldron_echo", "map_intruder", "empty_wake", "countercurrent_column",
                    "false_sculk_vibration", "watching_arrow", "suspended_fall", "beacon_fragment",
                    "stray_experience", "extra_in_the_herd", "lava_wake", "false_lid",
                    "wandering_tree",
                    "animal_circle", "animal_grid", "animal_death_circle", "animal_wake_circle", "restored_block", "missing_block", "joined_game", "old_friend" ->
                    UncannyParanoiaEventSystem.triggerMinecraftNativeAnomalyForDebug(target, eventId);
            case "wandering_tree_step" -> reportWanderingTree(target,
                    WanderingTreeSystem.forceStep(target) == WanderingTreeSystem.MoveOutcome.MOVED);
            case "wandering_tree_arm" -> reportWanderingTree(target, WanderingTreeSystem.arm(target));
            case "old_friend_attack" -> reportOldFriend(target, com.eotv.echoofthevoid.event.special.OldFriendSystem.forceAttack(target));
            case "old_friend_reset" -> reportOldFriend(target, target.getServer() != null && com.eotv.echoofthevoid.event.special.OldFriendSystem.reset(target.getServer()));
            case "grand_event", "grand_event_warden" -> UncannyParanoiaEventSystem.triggerGrandEventWarden(target);
            case "grand_event_stop" -> UncannyParanoiaEventSystem.triggerGrandEventStop(target);
            case "tension_builder_start" -> UncannyParanoiaEventSystem.triggerTensionBuilderStart(target);
            case "tension_builder_stop" -> UncannyParanoiaEventSystem.triggerTensionBuilderStop(target);
            default -> false;
        };
    }

    /** QA feedback: the move outcome and trap state are otherwise invisible by design. */
    private static boolean reportWanderingTree(ServerPlayer target, boolean success) {
        target.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                WanderingTreeSystem.describeFor(target)));
        return success;
    }

    private static boolean reportOldFriend(ServerPlayer target, boolean success) {
        if (target.getServer() != null) {
            target.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    com.eotv.echoofthevoid.event.special.OldFriendSystem.describe(target.getServer())));
        }
        return success;
    }

    private static boolean triggerVariant(ServerPlayer target, String variantArg) {
        String[] parts = variantArg.split("\\|", 2);
        if (parts.length != 2) {
            return false;
        }
        return UncannyParanoiaEventSystem.triggerEventVariant(target, parts[0], parts[1]);
    }

    private static boolean spawnUncanny(ServerPlayer target, String typeId) {
        return spawnUncanny(target, typeId, DEFAULT_SPAWN_DISTANCE);
    }

    private static boolean spawnUncanny(ServerPlayer target, String typeId, int spawnDistance) {
        return spawnUncannyInternal(target, typeId, null, spawnDistance);
    }

    private static boolean spawnUncannyForcedVariant(ServerPlayer target, String actionArg, int spawnDistance) {
        String[] parts = actionArg.split("\\|");
        if (parts.length < 3) {
            return false;
        }

        String typeId = parts[0];
        String tagKey = parts[1];
        String valueRaw = parts[2];
        String valueType = parts.length >= 4 ? parts[3] : "int";

        if ("replacement".equalsIgnoreCase(valueType)) {
            int variantIndex;
            try {
                variantIndex = Integer.parseInt(valueRaw);
            } catch (NumberFormatException exception) {
                return false;
            }
            if (ReplacementVariantExpansionCatalog.variant(typeId, variantIndex) == null) {
                return false;
            }
        }

        return spawnUncannyInternal(target, typeId, entity -> {
            if ("replacement".equalsIgnoreCase(valueType)) {
                ReplacementVariantExpansionSystem.applyForcedVariant(
                        entity, Integer.parseInt(valueRaw), target.serverLevel().getGameTime());
                return;
            }
            CompoundTag tag = new CompoundTag();
            entity.addAdditionalSaveData(tag);
            if ("bool".equalsIgnoreCase(valueType)) {
                tag.putBoolean(tagKey, Boolean.parseBoolean(valueRaw));
            } else {
                try {
                    tag.putInt(tagKey, Integer.parseInt(valueRaw));
                } catch (NumberFormatException exception) {
                    return;
                }
            }
            entity.readAdditionalSaveData(tag);
        }, spawnDistance);
    }

    private static boolean spawnPassiveForcedVariant(ServerPlayer target, String actionArg) {
        String[] parts = actionArg.split("\\|", 2);
        if (parts.length != 2) {
            return false;
        }

        if ("approved".equals(parts[0])) {
            return ApprovedVanillaVariantSystem.forceSpawn(target, parts[1]);
        }
        if ("approved_random".equals(parts[0])) {
            return ApprovedVanillaVariantSystem.forceSpawnRandom(target, parts[1]);
        }

        int variant;
        try {
            variant = Integer.parseInt(parts[1]);
        } catch (NumberFormatException exception) {
            return false;
        }
        return UncannyPassiveVariantSystem.spawnPassiveVariantForCommand(target, parts[0], variant);
    }

    private static boolean spawnUncannyInternal(
            ServerPlayer target,
            String typeId,
            java.util.function.Consumer<Mob> preSpawnCustomizer,
            int spawnDistance) {
        EntityType<? extends Mob> entityType = UncannyEntityRegistry.byCommandType(typeId);
        if (entityType == null) {
            return false;
        }

        if (entityType == UncannyEntityRegistry.UNCANNY_WATCHER.get()) {
            return UncannyWatcherSystem.forceSpawnWatcher(target);
        }
        if (entityType == UncannyEntityRegistry.UNCANNY_SHADOW.get()) {
            return UncannyParanoiaEventSystem.spawnShadowForCommand(target);
        }
        if (entityType == UncannyEntityRegistry.UNCANNY_HURLER.get()) {
            return UncannyParanoiaEventSystem.spawnHurlerForCommand(target);
        }
        if (entityType == UncannyEntityRegistry.UNCANNY_STALKER.get()) {
            return UncannyParanoiaEventSystem.spawnStalkerForCommand(target);
        }
        if (entityType == UncannyEntityRegistry.UNCANNY_KNOCKER.get()) {
            return UncannyParanoiaEventSystem.spawnKnockerForCommand(target);
        }
        if (entityType == UncannyEntityRegistry.UNCANNY_PULSE.get()) {
            return UncannyParanoiaEventSystem.spawnPulseForCommand(target);
        }

        Mob entity = entityType.create(target.serverLevel());
        if (entity == null) {
            return false;
        }

        if (preSpawnCustomizer != null) {
            preSpawnCustomizer.accept(entity);
        }

        Vec3 look = target.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontal = horizontal.normalize();
        }
        Vec3 spawnPos = findSafeSpawnPosition(target.serverLevel(), target, entity, horizontal, spawnDistance);
        if (spawnPos == null) {
            entity.discard();
            return false;
        }
        entity.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, target.getYRot(), 0.0F);
        if (entity instanceof UncannyDoubleDormantEntity mimic) {
            mimic.copyTarget(target, target.blockPosition(), target.blockPosition());
        }
        if (entity instanceof Monster monster) {
            monster.setTarget(target);
        }
        entity.addTag(DEV_SPAWNED_TAG);
        boolean added = target.serverLevel().addFreshEntity(entity);
        UncannyDiagnostics.specialSpawnResult(target, entity, added, "dev_menu_generic");
        return added;
    }

    private static Vec3 findSafeSpawnPosition(
            ServerLevel level,
            ServerPlayer target,
            Mob entity,
            Vec3 horizontal,
            int requestedDistance) {
        Vec3 side = new Vec3(-horizontal.z, 0.0D, horizontal.x);
        double[] sideOffsets = {0.0D, 1.5D, -1.5D, 3.0D, -3.0D};
        double[] heightOffsets = {0.0D, 1.0D, -1.0D, 2.0D};
        double distance = Math.max(2.0D, Math.min(24.0D, requestedDistance));

        for (double heightOffset : heightOffsets) {
            for (double sideOffset : sideOffsets) {
                Vec3 candidate = target.position()
                        .add(horizontal.scale(distance))
                        .add(side.scale(sideOffset))
                        .add(0.0D, heightOffset, 0.0D);
                BlockPos blockPos = BlockPos.containing(candidate);
                if (!level.hasChunkAt(blockPos) || !level.getWorldBorder().isWithinBounds(blockPos)) {
                    continue;
                }
                entity.moveTo(candidate.x, candidate.y, candidate.z, target.getYRot(), 0.0F);
                if (level.noCollision(entity)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    public record ExecutionResult(boolean success, String message, int affectedEntities) {
        public static ExecutionResult success(String message) {
            return success(message, 0);
        }

        public static ExecutionResult success(String message, int affectedEntities) {
            return new ExecutionResult(true, message, Math.max(0, affectedEntities));
        }

        public static ExecutionResult failure(String message) {
            return new ExecutionResult(false, message, 0);
        }
    }
}
