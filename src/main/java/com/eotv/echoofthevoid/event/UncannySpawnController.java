package com.eotv.echoofthevoid.event;

import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyPhantomEntity;
import com.eotv.echoofthevoid.entity.custom.UncannySkeletonEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyStrayEntity;
import com.eotv.echoofthevoid.phase.UncannyPhase;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

public final class UncannySpawnController {
    private static final Set<UUID> PENDING_REPLACEMENT = new HashSet<>();

    private UncannySpawnController() {
    }

    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (mob instanceof UncannyEntityMarker || !(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!UncannyWorldState.get(serverLevel.getServer()).isSystemEnabled()) {
            return;
        }

        if (mob instanceof IronGolem ironGolem && ironGolem.isPlayerCreated()) {
            return;
        }

        if (!isReplacementEligibleSpawnType(event.getSpawnType())) {
            return;
        }

        EntityType<? extends Mob> replacementType = UncannyEntityRegistry.getReplacement(mob.getType());
        if (replacementType == null) {
            return;
        }

        UncannyWorldState state = UncannyWorldState.get(serverLevel.getServer());
        UncannyPhase phase = state.getPhase();

        // FinalizeSpawn can run on worldgen worker threads; never touch serverLevel.random here.
        long seed = mob.getUUID().getMostSignificantBits()
                ^ mob.getUUID().getLeastSignificantBits()
                ^ serverLevel.getGameTime()
                ^ ((long) event.getSpawnType().ordinal() << 16);
        RandomSource localRandom = RandomSource.create(seed);
        if (localRandom.nextDouble() > phase.replacementChance()) {
            return;
        }

        Player nearestPlayer = serverLevel.getNearestPlayer(mob, UncannyConfig.SPAWN_MINIMUM_DISTANCE.get());
        if (nearestPlayer != null) {
            return;
        }

        PENDING_REPLACEMENT.add(mob.getUUID());
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Vex vex) {
            vex.setSilent(true);
        }
        if (event.getEntity() instanceof Zoglin zoglin) {
            zoglin.setSilent(true);
        }

        if (!(event.getEntity() instanceof Mob originalMob)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!UncannyWorldState.get(serverLevel.getServer()).isSystemEnabled()) {
            PENDING_REPLACEMENT.remove(originalMob.getUUID());
            return;
        }

        if (!PENDING_REPLACEMENT.remove(originalMob.getUUID())) {
            return;
        }

        EntityType<? extends Mob> replacementType = UncannyEntityRegistry.getReplacement(originalMob.getType());
        if (replacementType == null) {
            return;
        }

        Mob replacementMob = replacementType.create(serverLevel);
        if (replacementMob == null) {
            return;
        }

        replacementMob.moveTo(
                originalMob.getX(),
                originalMob.getY(),
                originalMob.getZ(),
                originalMob.getYRot(),
                originalMob.getXRot());

        SpawnGroupData spawnData = replacementMob.finalizeSpawn(
                serverLevel,
                serverLevel.getCurrentDifficultyAt(replacementMob.blockPosition()),
                MobSpawnType.NATURAL,
                null);
        if (spawnData != null) {
            // noop: finalizeSpawn has been applied to restore vanilla-equivalent loadouts/flags.
        }

        applyPhaseSpeedBoost(replacementMob, UncannyWorldState.get(serverLevel.getServer()).getPhase());

        event.setCanceled(true);
        serverLevel.addFreshEntity(replacementMob);
    }

    private static void applyPhaseSpeedBoost(Mob mob, UncannyPhase phase) {
        if (!(mob instanceof UncannySkeletonEntity || mob instanceof UncannyStrayEntity || mob instanceof UncannyPhantomEntity)) {
            return;
        }

        if (mob.getAttribute(Attributes.MOVEMENT_SPEED) == null) {
            return;
        }

        double multiplier = switch (phase) {
            case PHASE_2 -> 1.10D;
            case PHASE_3, PHASE_4 -> 1.20D;
            default -> 1.00D;
        };

        double capped = Math.min(mob.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * multiplier, mob.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * 1.25D);
        mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(capped);
    }

    private static boolean isReplacementEligibleSpawnType(MobSpawnType spawnType) {
        return switch (spawnType) {
            case SPAWNER, SPAWN_EGG, COMMAND, DISPENSER, TRIAL_SPAWNER, BUCKET, BREEDING, MOB_SUMMONED, TRIGGERED -> false;
            default -> true;
        };
    }
}

