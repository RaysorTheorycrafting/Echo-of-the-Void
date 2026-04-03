package com.eotv.echoofthevoid.dev;

import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyDoubleDormantEntity;
import com.eotv.echoofthevoid.event.UncannyDoubleDormantSystem;
import com.eotv.echoofthevoid.event.UncannyParanoiaEventSystem;
import com.eotv.echoofthevoid.event.UncannyPassiveVariantSystem;
import com.eotv.echoofthevoid.event.UncannyStructureFeatureSystem;
import com.eotv.echoofthevoid.event.UncannyWatcherSystem;
import com.eotv.echoofthevoid.event.UncannyWeatherSystem;
import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class UncannyDevActionExecutor {
    private UncannyDevActionExecutor() {
    }

    public static boolean execute(ServerPlayer target, UncannyDevCatalog.Entry entry) {
        if (target == null || entry == null || target.getServer() == null) {
            return false;
        }

        return switch (entry.actionKind()) {
            case SPAWN_UNCANNY -> spawnUncanny(target, entry.actionArg());
            case SPAWN_UNCANNY_FORCED -> spawnUncannyForcedVariant(target, entry.actionArg());
            case SPAWN_PASSIVE_FORCED -> spawnPassiveForcedVariant(target, entry.actionArg());
            case SPAWN_SPECIAL -> spawnSpecial(target, entry.actionArg());
            case FORCE_MIMIC -> {
                UncannyDoubleDormantSystem.forceMimic(target);
                yield true;
            }
            case TRIGGER_EVENT -> triggerEvent(target, entry.actionArg());
            case TRIGGER_VARIANT -> triggerVariant(target, entry.actionArg());
            case GIVE_ITEM -> giveItem(target, entry.actionArg());
            case TRIGGER_WEATHER -> UncannyWeatherSystem.forceTrigger(target.getServer(), entry.actionArg());
            case STOP_WEATHER -> {
                UncannyWeatherSystem.forceStop(target.getServer());
                yield true;
            }
            case TRIGGER_STRUCTURE -> triggerStructure(target, entry.actionArg());
            case TRIGGER_SECRET_HOUSE -> UncannyStructureFeatureSystem.forceGenerateSecretHouseForDebug(target);
        };
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
        return switch (specialId) {
            case "watcher" -> UncannyWatcherSystem.forceSpawnWatcher(target);
            case "shadow" -> UncannyParanoiaEventSystem.spawnShadowForCommand(target);
            case "hurler" -> UncannyParanoiaEventSystem.spawnHurlerForCommand(target);
            case "attacker" -> UncannyParanoiaEventSystem.spawnStalkerForCommand(target);
            case "knocker" -> UncannyParanoiaEventSystem.spawnKnockerForCommand(target);
            case "pulse" -> UncannyParanoiaEventSystem.spawnPulseForCommand(target);
            case "usher" -> UncannyParanoiaEventSystem.spawnUsherForCommand(target);
            case "keeper" -> UncannyParanoiaEventSystem.spawnKeeperForCommand(target);
            case "tenant" -> UncannyParanoiaEventSystem.spawnTenantForCommand(target);
            case "follower" -> UncannyParanoiaEventSystem.spawnFollowerForCommand(target);
            case "terror" -> spawnUncanny(target, "uncanny_terror");
            default -> false;
        };
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
            case "ghost_miner" -> UncannyParanoiaEventSystem.triggerGhostMiner(target);
            case "cave_collapse" -> UncannyParanoiaEventSystem.triggerCaveCollapse(target);
            case "false_injury" -> UncannyParanoiaEventSystem.triggerFalseInjury(target);
            case "forced_drop" -> UncannyParanoiaEventSystem.triggerForcedDrop(target);
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
            case "armor_break" -> UncannyParanoiaEventSystem.triggerArmorBreak(target);
            case "aquatic_steps" -> UncannyParanoiaEventSystem.triggerAquaticSteps(target);
            case "door_inversion" -> UncannyParanoiaEventSystem.triggerDoorInversion(target);
            case "phantom_harvest" -> UncannyParanoiaEventSystem.triggerPhantomHarvest(target);
            case "living_ore" -> UncannyParanoiaEventSystem.triggerLivingOre(target);
            case "projected_shadow" -> UncannyParanoiaEventSystem.triggerProjectedShadow(target);
            case "giant_sun" -> UncannyParanoiaEventSystem.triggerGiantSun(target);
            case "hunter_fog" -> UncannyParanoiaEventSystem.triggerHunterFog(target);
            case "grand_event", "grand_event_warden" -> UncannyParanoiaEventSystem.triggerGrandEventWarden(target);
            case "grand_event_stop" -> UncannyParanoiaEventSystem.triggerGrandEventStop(target);
            case "tension_builder_start" -> UncannyParanoiaEventSystem.triggerTensionBuilderStart(target);
            case "tension_builder_stop" -> UncannyParanoiaEventSystem.triggerTensionBuilderStop(target);
            default -> false;
        };
    }

    private static boolean triggerVariant(ServerPlayer target, String variantArg) {
        String[] parts = variantArg.split("\\|", 2);
        if (parts.length != 2) {
            return false;
        }
        return UncannyParanoiaEventSystem.triggerEventVariant(target, parts[0], parts[1]);
    }

    private static boolean spawnUncanny(ServerPlayer target, String typeId) {
        return spawnUncannyInternal(target, typeId, null);
    }

    private static boolean spawnUncannyForcedVariant(ServerPlayer target, String actionArg) {
        String[] parts = actionArg.split("\\|");
        if (parts.length < 3) {
            return false;
        }

        String typeId = parts[0];
        String tagKey = parts[1];
        String valueRaw = parts[2];
        String valueType = parts.length >= 4 ? parts[3] : "int";

        return spawnUncannyInternal(target, typeId, entity -> {
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
        });
    }

    private static boolean spawnPassiveForcedVariant(ServerPlayer target, String actionArg) {
        String[] parts = actionArg.split("\\|", 2);
        if (parts.length != 2) {
            return false;
        }

        int variant;
        try {
            variant = Integer.parseInt(parts[1]);
        } catch (NumberFormatException exception) {
            return false;
        }
        return UncannyPassiveVariantSystem.spawnPassiveVariantForCommand(target, parts[0], variant);
    }

    private static boolean spawnUncannyInternal(ServerPlayer target, String typeId, java.util.function.Consumer<Mob> preSpawnCustomizer) {
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
        Vec3 spawnPos = target.position().add(horizontal.scale(2.2D)).add(0.0D, 0.8D, 0.0D);
        entity.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, target.getYRot(), 0.0F);
        if (entity instanceof UncannyDoubleDormantEntity mimic) {
            mimic.copyTarget(target, target.blockPosition(), target.blockPosition());
        }
        if (entity instanceof Monster monster) {
            monster.setTarget(target);
        }
        target.serverLevel().addFreshEntity(entity);
        return true;
    }
}
