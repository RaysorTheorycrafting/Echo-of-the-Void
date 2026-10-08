package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyApprovedSpecialEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyAmbusherEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyFollowerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyStalkerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyTenantEntity;
import com.eotv.echoofthevoid.event.UncannyParanoiaEventSystem;
import com.eotv.echoofthevoid.event.UncannyWatcherSystem;
import com.eotv.echoofthevoid.event.paranoia.GhostMinerBlockPolicy;
import com.eotv.echoofthevoid.event.paranoia.nativeevent.MinecraftNativeAnomalySystem;
import com.eotv.echoofthevoid.event.paranoia.nativeevent.MinecraftNativeAnomalyRules;
import com.eotv.echoofthevoid.event.special.ApprovedSpecialSystem;
import com.eotv.echoofthevoid.event.special.AdaptiveSpecialEquipment;
import com.eotv.echoofthevoid.event.special.GrandWardenRules;
import com.eotv.echoofthevoid.event.special.UncannySpecialRewardRules;
import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Headless server integration checks for targeted Special movement, audio and Vanilla contracts. */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class ApprovedSpecialGameTests {
    private static final String TEMPLATE = "special_test_room";

    private ApprovedSpecialGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void playerKilledGrandWardenDropsImportantShardStack(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Warden warden = helper.spawn(EntityType.WARDEN, new Vec3(8.5D, 1.0D, 8.5D));
        warden.addTag(GrandWardenRules.ENTITY_TAG);
        warden.setHealth(1.0F);

        helper.assertTrue(warden.hurt(helper.getLevel().damageSources().playerAttack(player), 100.0F),
                "The tagged Grand Warden must accept a real player kill");
        helper.runAtTickTime(4, () -> {
            List<ItemEntity> shards = helper.getLevel().getEntitiesOfClass(
                    ItemEntity.class,
                    helper.getBounds().inflate(4.0D),
                    item -> item.getItem().is(UncannyItemRegistry.UNCANNY_REALITY_SHARD.get()));
            int count = shards.stream().mapToInt(item -> item.getItem().getCount()).sum();
            helper.assertTrue(count >= UncannySpecialRewardRules.GRAND_WARDEN_MIN_SHARDS
                            && count <= UncannySpecialRewardRules.GRAND_WARDEN_MAX_SHARDS,
                    "A player-killed Grand Warden must drop 6-10 Reality Shards, found " + count);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void watcherEncounterIsBlockedWhileThePlayerSleeps(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        BlockPos sleepingPos = helper.absolutePos(new BlockPos(8, 1, 8));
        player.startSleeping(sleepingPos);

        helper.assertTrue(player.isSleeping(), "The mock player must expose the real sleeping state");
        helper.assertTrue(!UncannyWatcherSystem.forceSpawnWatcher(player),
                "Watcher? must not start an encounter while its target is sleeping");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                        com.eotv.echoofthevoid.entity.custom.UncannyWatcherEntity.class,
                        helper.getBounds()).isEmpty(),
                "No Watcher? may be created by the blocked sleeping route");
        player.stopSleeping();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void followerEncounterIsProtectedFromVanillaDistanceDespawn(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 playerPosition = helper.absoluteVec(new Vec3(8.5D, 1.0D, 8.5D));
        player.moveTo(playerPosition.x, playerPosition.y, playerPosition.z, 0.0F, 0.0F);
        UncannyFollowerEntity follower = UncannyEntityRegistry.UNCANNY_FOLLOWER.get().create(helper.getLevel());
        helper.assertTrue(follower != null, "Follower? must be creatable");
        follower.moveTo(helper.absoluteVec(new Vec3(4.5D, 1.0D, 4.5D)));
        follower.setupFollower(player, 6_000L);

        helper.assertTrue(follower.isPersistenceRequired(),
                "A multi-minute Follower? encounter must not be eligible for Vanilla distance despawn");
        helper.assertTrue(helper.getLevel().addFreshEntity(follower),
                "The persistent Follower? must enter the real ServerLevel");
        helper.runAtTickTime(8, () -> {
            helper.assertTrue(follower.isAlive() && !follower.isRemoved(),
                    "Follower? must remain alive after its configured encounter starts");
            follower.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 70)
    public static void followerRejectsProjectilesAndEvadesInsteadOfDyingToOneMeleeHit(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 playerPosition = helper.absoluteVec(new Vec3(8.5D, 1.0D, 8.5D));
        player.moveTo(playerPosition.x, playerPosition.y, playerPosition.z, 0.0F, 0.0F);
        UncannyFollowerEntity follower = UncannyEntityRegistry.UNCANNY_FOLLOWER.get().create(helper.getLevel());
        helper.assertTrue(follower != null, "Follower? must be creatable");
        follower.moveTo(helper.absoluteVec(new Vec3(6.5D, 1.0D, 8.5D)));
        follower.setupFollower(player, 6_000L);
        helper.assertTrue(helper.getLevel().addFreshEntity(follower),
                "Follower? must enter the real ServerLevel for damage checks");

        float initialHealth = follower.getHealth();
        Arrow arrow = new Arrow(
                helper.getLevel(), player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        helper.assertTrue(!follower.canBeHitByProjectile(),
                "Follower? must not offer a projectile collision target");
        helper.assertTrue(!follower.hurt(helper.getLevel().damageSources().arrow(arrow, player), 100.0F),
                "Projectile-tagged damage must be rejected before damage or knockback");
        helper.assertTrue(follower.getHealth() == initialHealth,
                "An ignored projectile must leave Follower?'s health unchanged");

        helper.assertTrue(follower.hurt(helper.getLevel().damageSources().playerAttack(player), 100.0F),
                "Direct melee must remain a valid way to fight Follower?");
        helper.assertTrue(follower.isAlive() && initialHealth - follower.getHealth() <= 4.001F,
                "One melee hit must start evasion rather than instantly removing Follower?");
        CompoundTag evasionState = new CompoundTag();
        follower.saveWithoutId(evasionState);
        helper.assertTrue(evasionState.getBoolean("EvasiveRepositionArmed"),
                "A melee hit must persistently arm Follower?'s evasive state");
        helper.assertTrue(evasionState.getInt("EvasiveRepositionsRemaining") == 2,
                "A melee hit must not consume an offscreen reposition before one succeeds");
        helper.assertTrue(evasionState.getLong("AttackSuppressedUntilTick") > helper.getLevel().getGameTime(),
                "Follower? must persist a grace window instead of counterattacking immediately");
        UncannyFollowerEntity reloaded = UncannyEntityRegistry.UNCANNY_FOLLOWER.get().create(helper.getLevel());
        helper.assertTrue(reloaded != null, "Follower? must be creatable for the reload check");
        reloaded.load(evasionState);
        CompoundTag stateAfterReload = new CompoundTag();
        reloaded.saveWithoutId(stateAfterReload);
        helper.assertTrue(!stateAfterReload.getBoolean("EvasiveRepositionArmed"),
                "A reload must require a fresh pursuit before any offscreen reposition");
        helper.assertTrue(!reloaded.isInvisible(),
                "An interrupted reposition cloak must never survive a reload");
        helper.runAtTickTime(45, () -> {
            helper.assertTrue(follower.isAlive() && !follower.isRemoved(),
                    "Follower? must remain in the encounter after its former 38-tick sink window");
            follower.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 50)
    public static void followerTreatsGlassAsDirectPlayerVision(GameTestHelper helper) {
        fillFloor(helper);
        for (int z = 0; z < 16; z++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(8, y, z, Blocks.GLASS);
            }
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 playerPosition = helper.absoluteVec(new Vec3(1.5D, 1.0D, 8.5D));
        player.moveTo(playerPosition.x, playerPosition.y, playerPosition.z, -90.0F, 0.0F);

        UncannyFollowerEntity follower = UncannyEntityRegistry.UNCANNY_FOLLOWER.get().create(helper.getLevel());
        helper.assertTrue(follower != null, "Follower? must be creatable");
        Vec3 initial = helper.absoluteVec(new Vec3(15.5D, 1.0D, 8.5D));
        follower.moveTo(initial.x, initial.y, initial.z, 90.0F, 0.0F);
        follower.setupFollower(player, 6_000L);
        helper.assertTrue(helper.getLevel().addFreshEntity(follower),
                "Follower? must enter the glass visibility test");
        player.lookAt(EntityAnchorArgument.Anchor.EYES, follower.getEyePosition());

        helper.runAtTickTime(20, () -> {
            player.lookAt(EntityAnchorArgument.Anchor.EYES, follower.getEyePosition());
            helper.assertTrue(follower.position().distanceToSqr(initial) < 0.20D * 0.20D,
                    "Follower? must remain still when directly watched through transparent glass");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void ferrymanFollowsThenRevealsBesideAStoppedBoat(GameTestHelper helper) {
        fillFloor(helper);
        for (int x = 1; x <= 14; x++) {
            for (int z = 1; z <= 14; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(x, y, z, Blocks.WATER);
                }
            }
        }

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 boatPosition = helper.absoluteVec(new Vec3(5.5D, 5.0D, 8.5D));
        player.moveTo(boatPosition.x, boatPosition.y, boatPosition.z, 0.0F, 0.0F);
        Boat boat = helper.spawn(EntityType.BOAT, new Vec3(5.5D, 5.0D, 8.5D));
        helper.assertTrue(player.startRiding(boat, true), "The mock player must occupy the Ferryman test boat");
        helper.assertTrue(ApprovedSpecialSystem.spawnForDebug(player, "ferryman"),
                "Ferryman? must accept a real occupied boat over deep water");

        for (int tick = 1; tick <= 60; tick++) {
            helper.runAtTickTime(tick, () -> {
                boat.setPos(boat.getX() + 0.035D, boat.getY(), boat.getZ());
                boat.setDeltaMovement(0.035D, 0.0D, 0.0D);
            });
        }
        for (int tick = 10; tick <= 50; tick += 10) {
            int sampleTick = tick;
            helper.runAtTickTime(sampleTick, () -> assertFerrymanBoundAndSubmerged(helper, player, boat, sampleTick));
        }
        Vec3[] stoppedBoatPosition = new Vec3[1];
        for (int tick = 61; tick <= 125; tick++) {
            helper.runAtTickTime(tick, () -> {
                if (stoppedBoatPosition[0] == null) {
                    stoppedBoatPosition[0] = boat.position();
                }
                Vec3 stopped = stoppedBoatPosition[0];
                boat.setPos(stopped.x, stopped.y, stopped.z);
                boat.setDeltaMovement(Vec3.ZERO);
            });
        }
        helper.runAtTickTime(118, () -> {
            List<UncannyApprovedSpecialEntity> ferrymen = findFerrymen(helper, boat);
            helper.assertTrue(ferrymen.size() == 1, "Ferryman? must remain present for its reveal");
            UncannyApprovedSpecialEntity ferryman = ferrymen.getFirst();
            helper.assertTrue(ferryman.hasStartedFerrymanReveal(),
                    "Stopping the boat must start Ferryman?'s visible rise instead of making it sink");
            helper.assertTrue(ferryman.getY() >= boat.getY() - 1.45D,
                    "Ferryman? must rise near the waterline after the boat stops; ferrymanY="
                            + ferryman.getY() + ", boatY=" + boat.getY() + ", position=" + ferryman.position());
            helper.assertTrue(ferryman.position().subtract(boat.position()).horizontalDistance() >= 2.4D,
                    "Ferryman? must reveal itself at a readable distance from the hull");
            helper.assertTrue(boat.isAlive(), "Ferryman? must not damage the boat during the reveal");
            helper.assertTrue(player.getVehicle() == boat, "Ferryman? must not eject the passenger during the reveal");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 250)
    public static void ferrymanWaitsForTenSecondsOfContinuousDeepWaterNavigation(GameTestHelper helper) {
        fillFloor(helper);
        for (int x = 1; x <= 14; x++) {
            for (int z = 1; z <= 14; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(x, y, z, Blocks.WATER);
                }
            }
        }

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 boatPosition = helper.absoluteVec(new Vec3(3.5D, 5.0D, 8.5D));
        player.moveTo(boatPosition.x, boatPosition.y, boatPosition.z, 0.0F, 0.0F);
        Boat boat = helper.spawn(EntityType.BOAT, new Vec3(3.5D, 5.0D, 8.5D));
        helper.assertTrue(player.startRiding(boat, true), "The mock player must occupy the deferred Ferryman test boat");

        UncannyWorldState state = UncannyWorldState.get(player.getServer());
        state.clearPendingFerrymanEncounter(player.getUUID());
        helper.assertTrue(state.armPendingFerrymanEncounter(player.getUUID(), 200),
                "The Ferryman reservation must accept the exact ten-second lower bound");
        for (int tick = 1; tick <= 215; tick++) {
            helper.runAtTickTime(tick, () -> {
                boat.setPos(boat.getX() + 0.035D, boat.getY(), boat.getZ());
                boat.setDeltaMovement(0.035D, 0.0D, 0.0D);
                // Mock players do not consistently publish NeoForge PlayerTickEvent hooks in
                // the GameTest runner. Exercise the same public production tick explicitly;
                // a surface test separately protects its registration in onPlayerTick.
                ApprovedSpecialSystem.tickPendingFerrymanEncounter(player);
            });
        }
        helper.runAtTickTime(190, () -> helper.assertTrue(findFerrymen(helper, boat).isEmpty(),
                "Ferryman? must not appear before ten seconds of measured navigation"));
        helper.runAtTickTime(215, () -> {
            helper.assertTrue(findFerrymen(helper, boat).size() == 1,
                    "Ferryman? must materialize after the persisted ten-second reservation is satisfied");
            helper.assertTrue(state.getPendingFerrymanEncounter(player.getUUID()) == null,
                    "The reservation must clear only after successful entity insertion");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void ferrymanReservationRoundTripsThroughSavedData(GameTestHelper helper) {
        UUID playerId = UUID.randomUUID();
        UncannyWorldState original = UncannyWorldState.create();
        helper.assertTrue(original.armPendingFerrymanEncounter(playerId, 260),
                "A valid thirteen-second Ferryman reservation must be accepted");
        original.setPendingFerrymanProgress(playerId, 87);

        CompoundTag saved = original.save(new CompoundTag(), helper.getLevel().registryAccess());
        UncannyWorldState restored = UncannyWorldState.load(saved, helper.getLevel().registryAccess());
        UncannyWorldState.PendingFerrymanEncounter pending = restored.getPendingFerrymanEncounter(playerId);
        helper.assertTrue(pending != null, "A selected Ferryman encounter must survive save and reload");
        helper.assertTrue(pending.requiredNavigationTicks() == 260,
                "The selected 10-15 second navigation threshold must survive save and reload");
        helper.assertTrue(pending.progressTicks() == 87,
                "Continuous deep-water progress must survive save and reload");
        helper.assertTrue(UncannyWorldState.load(new CompoundTag(), helper.getLevel().registryAccess())
                        .getPendingFerrymanEncounterCount() == 0,
                "Older worlds without the additive reservation list must load with no pending encounter");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void ambusherHasARealDebugRouteAndRemainsKillable(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 playerPosition = helper.absoluteVec(new Vec3(8.5D, 2.0D, 8.5D));
        player.moveTo(playerPosition.x, playerPosition.y, playerPosition.z, 0.0F, 0.0F);

        helper.assertTrue(ApprovedSpecialSystem.spawnAmbusher(player, true),
                "Ambusher? must have a direct QA spawn route on safe open ground");
        List<UncannyAmbusherEntity> ambushers = helper.getLevel().getEntitiesOfClass(
                UncannyAmbusherEntity.class,
                player.getBoundingBox().inflate(16.0D),
                entity -> entity.isAlive());
        helper.assertTrue(ambushers.size() == 1, "The direct QA route must insert exactly one Ambusher?");
        UncannyAmbusherEntity ambusher = ambushers.getFirst();
        float health = ambusher.getHealth();
        helper.assertTrue(ambusher.hurt(helper.getLevel().damageSources().playerAttack(player), 2.0F),
                "Ambusher? must accept ordinary player melee damage");
        helper.assertTrue(ambusher.getHealth() < health, "Ambusher? melee damage must not be swallowed");
        ambusher.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 130)
    public static void ambusherTelegraphsOneAttackThenSinks(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 playerPosition = helper.absoluteVec(new Vec3(8.5D, 2.0D, 8.5D));
        player.moveTo(playerPosition.x, playerPosition.y, playerPosition.z, 0.0F, 0.0F);

        helper.assertTrue(ApprovedSpecialSystem.spawnAmbusher(player, false),
                "Ambusher? must find a fair direct route behind a grounded target");
        helper.runAtTickTime(60, () -> {
            List<UncannyAmbusherEntity> ambushers = helper.getLevel().getEntitiesOfClass(
                    UncannyAmbusherEntity.class,
                    player.getBoundingBox().inflate(16.0D),
                    entity -> entity.isAlive());
            helper.assertTrue(ambushers.size() == 1,
                    "Ambusher? must still be visible while completing its bounded sink");
            CompoundTag state = new CompoundTag();
            ambushers.getFirst().saveWithoutId(state);
            helper.assertTrue(state.getInt("State") == 2,
                    "Ambusher? must enter its sinking state immediately after its only attack attempt");
            helper.assertTrue(state.getBoolean("AttackAttempted"),
                    "The stable-ground follow-up must execute its single ordinary melee attempt");
        });
        helper.runAtTickTime(110, () -> {
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                            UncannyAmbusherEntity.class,
                            player.getBoundingBox().inflate(32.0D),
                            entity -> entity.isAlive()).isEmpty(),
                    "Ambusher? must leave after sinking instead of attacking a second time");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 50)
    public static void mournerPlaysACueAfterThePlayerEntersAudibleRange(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 playerPosition = helper.absoluteVec(new Vec3(8.5D, 1.0D, 8.5D));
        player.moveTo(playerPosition.x, playerPosition.y, playerPosition.z, 90.0F, 0.0F);
        helper.assertTrue(ApprovedSpecialSystem.spawnForDebug(player, "mourner"),
                "Mourner? must have a safe debug spawn near the player");

        helper.runAtTickTime(8, () -> {
            List<UncannyApprovedSpecialEntity> mourners = helper.getLevel().getEntitiesOfClass(
                    UncannyApprovedSpecialEntity.class,
                    helper.getBounds(),
                    entity -> entity.isAlive() && "mourner".equals(entity.specialId()));
            helper.assertTrue(mourners.size() == 1, "Exactly one Mourner? must remain active");
            UncannyApprovedSpecialEntity mourner = mourners.getFirst();
            helper.assertTrue(mourner.hasPlayedMournerCueInRange(),
                    "Mourner? must replay its sob once the focused player is actually in audible range");
            helper.assertTrue(mourner.dedicatedSoundCuesPlayed() >= 1,
                    "Mourner? must emit at least one physical sound cue");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30, batch = "attacker_animation")
    public static void attackerAnimationStudiesUseDistinctSyncedStyles(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 playerPosition = helper.absoluteVec(new Vec3(8.5D, 1.0D, 8.5D));
        player.moveTo(playerPosition.x, playerPosition.y, playerPosition.z, 90.0F, 0.0F);

        helper.assertTrue(UncannyParanoiaEventSystem.spawnStalkerForCommand(
                        player, UncannyStalkerEntity.AnimationStyle.CRAWL),
                "The all-fours Attacker? study must be spawnable through its QA route");
        // addFreshEntity may defer visibility to level queries until the next server tick while
        // parallel GameTests are ticking. Observe each QA spawn only after it has joined the level.
        helper.runAtTickTime(5, () -> {
            UncannyStalkerEntity crawl = findSingleAttacker(helper, player);
            helper.assertTrue(crawl.getAnimationStyle() == UncannyStalkerEntity.AnimationStyle.CRAWL,
                    "The all-fours style must be stored in synced entity data");
            crawl.discard();

            helper.assertTrue(UncannyParanoiaEventSystem.spawnStalkerForCommand(
                            player, UncannyStalkerEntity.AnimationStyle.OUTSTRETCHED),
                    "The arms-forward Attacker? study must be spawnable through its QA route");
        });
        helper.runAtTickTime(10, () -> {
            UncannyStalkerEntity outstretched = findSingleAttacker(helper, player);
            helper.assertTrue(outstretched.getAnimationStyle() == UncannyStalkerEntity.AnimationStyle.OUTSTRETCHED,
                    "The arms-forward style must be stored in synced entity data");
            outstretched.discard();

            helper.assertTrue(UncannyStalkerEntity.AnimationStyle.values().length == 2,
                    "Attacker? must no longer expose its former standard model");
            helper.assertTrue(UncannyStalkerEntity.AnimationStyle.byId(0) == UncannyStalkerEntity.AnimationStyle.CRAWL,
                    "A legacy or missing style id must migrate to one of the two retained forms");
            helper.assertTrue(UncannyParanoiaEventSystem.spawnStalkerForCommand(player),
                    "The ordinary QA spawn must choose one of the retained Attacker? forms");
        });
        helper.runAtTickTime(15, () -> {
            UncannyStalkerEntity ordinary = findSingleAttacker(helper, player);
            helper.assertTrue(
                    ordinary.getAnimationStyle() == UncannyStalkerEntity.AnimationStyle.CRAWL
                            || ordinary.getAnimationStyle() == UncannyStalkerEntity.AnimationStyle.OUTSTRETCHED,
                    "Ordinary Attacker? spawns must never use a third visual form");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void ghostMinerUsesNaturalUndergroundSoundsOnly(GameTestHelper helper) {
        helper.assertTrue(GhostMinerBlockPolicy.isNaturalUnderground(Blocks.STONE.defaultBlockState()),
                "Ghost Miner must accept natural stone");
        helper.assertTrue(GhostMinerBlockPolicy.isNaturalUnderground(Blocks.DEEPSLATE.defaultBlockState()),
                "Ghost Miner must accept natural deepslate");
        helper.assertTrue(GhostMinerBlockPolicy.isNaturalUnderground(Blocks.DIRT.defaultBlockState()),
                "Ghost Miner must accept natural dirt");
        helper.assertTrue(!GhostMinerBlockPolicy.isNaturalUnderground(Blocks.OAK_PLANKS.defaultBlockState()),
                "Ghost Miner must never reproduce player-house plank sounds");
        helper.assertTrue(!GhostMinerBlockPolicy.isNaturalUnderground(Blocks.COBBLESTONE.defaultBlockState()),
                "Ghost Miner must not treat a generic built stone block as a natural tunnel material");
        helper.assertTrue(!GhostMinerBlockPolicy.isNaturalUnderground(Blocks.OAK_LOG.defaultBlockState()),
                "Ghost Miner must reject constructed timber and mineshaft supports");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void ghostMinerDebugRouteFindsARealNaturalTunnel(GameTestHelper helper) {
        fillFloor(helper);
        for (int x = 6; x <= 10; x++) {
            helper.setBlock(x, 1, 8, Blocks.STONE);
            helper.setBlock(x, 2, 8, Blocks.STONE);
        }

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 playerPosition = helper.absoluteVec(new Vec3(2.5D, 1.0D, 8.5D));
        player.moveTo(playerPosition.x, playerPosition.y, playerPosition.z, 90.0F, 0.0F);

        helper.assertTrue(UncannyParanoiaEventSystem.triggerGhostMinerForDebug(player),
                "Ghost Miner's QA route must exhaustively find a valid four-section natural tunnel");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 130)
    public static void surveyorHugsAWindowThenFleesOnlyFromAnOpenSightLine(GameTestHelper helper) {
        fillFloor(helper);
        for (int z = 5; z <= 11; z++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(8, y, z, Blocks.STONE);
            }
        }
        helper.setBlock(8, 1, 8, Blocks.GLASS);
        helper.setBlock(8, 2, 8, Blocks.GLASS);

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 inside = helper.absoluteVec(new Vec3(4.5D, 1.0D, 8.5D));
        player.moveTo(inside.x, inside.y, inside.z, -90.0F, 0.0F);
        helper.assertTrue(ApprovedSpecialSystem.spawnForDebug(player, "surveyor"),
                "Surveyor? must use the real test window as an inspection target");

        helper.runAtTickTime(78, () -> {
            UncannyApprovedSpecialEntity surveyor = findSingleSpecial(helper, "surveyor");
            Vec3 window = Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 1, 8)));
            helper.assertTrue(surveyor.surveyorInspectionTarget() != null,
                    "Surveyor? must retain a collision-safe position beside its chosen window");
            helper.assertTrue(surveyor.position().distanceToSqr(window) <= 2.2D * 2.2D,
                    "Surveyor? must actively reach the window instead of orbiting far from the house; position="
                            + surveyor.position());
            helper.assertTrue(!surveyor.isSurveyorFleeing(),
                    "Glass between the player and Surveyor? must count as a block and prevent flight");
        });

        for (int tick = 80; tick <= 92; tick++) {
            helper.runAtTickTime(tick, () -> {
                UncannyApprovedSpecialEntity surveyor = findSingleSpecial(helper, "surveyor");
                Vec3 outside = helper.absoluteVec(new Vec3(10.5D, 1.0D, 8.5D));
                player.moveTo(outside.x, outside.y, outside.z, 90.0F, 0.0F);
                player.lookAt(EntityAnchorArgument.Anchor.EYES, surveyor.getEyePosition());
            });
        }
        helper.runAtTickTime(96, () -> {
            UncannyApprovedSpecialEntity surveyor = findSingleSpecial(helper, "surveyor");
            helper.assertTrue(surveyor.isSurveyorFleeing(),
                    "A direct block-free look from within eight blocks must make Surveyor? flee");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void doublerMirrorsParallelMovementAndCanBeKilled(GameTestHelper helper) {
        fillFloor(helper);
        helper.setBlock(8, 1, 8, Blocks.GLASS);
        helper.setBlock(8, 2, 8, Blocks.GLASS);

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 playerStart = helper.absoluteVec(new Vec3(3.5D, 1.0D, 8.5D));
        player.moveTo(playerStart.x, playerStart.y, playerStart.z, -90.0F, 0.0F);
        helper.assertTrue(ApprovedSpecialSystem.spawnForDebug(player, "doubler"),
                "Doubler? must accept a real glass separation");

        Vec3[] doublerStart = new Vec3[1];
        helper.runAtTickTime(2, () -> doublerStart[0] = findSingleSpecial(helper, "doubler").position());
        for (int tick = 4; tick <= 18; tick++) {
            helper.runAtTickTime(tick, () -> player.setPos(
                    player.getX(), player.getY(), player.getZ() + 0.16D));
        }
        helper.runAtTickTime(27, () -> {
            UncannyApprovedSpecialEntity doubler = findSingleSpecial(helper, "doubler");
            helper.assertTrue(doubler.copiedActions() >= 6,
                    "Doubler? must consume and apply most delayed player movement samples");
            helper.assertTrue(doublerStart[0] != null
                            && doubler.getZ() >= doublerStart[0].z + 0.70D,
                    "Movement parallel to the glass must be copied in the same direction; start="
                            + doublerStart[0] + ", current=" + doubler.position());
            doubler.setHealth(4.0F);
            helper.assertTrue(doubler.hurt(helper.getLevel().damageSources().playerAttack(player), 10.0F),
                    "Doubler? must accept real player damage");
        });
        helper.runAtTickTime(31, () -> {
            List<UncannyApprovedSpecialEntity> doublers = helper.getLevel().getEntitiesOfClass(
                    UncannyApprovedSpecialEntity.class,
                    helper.getBounds(),
                    entity -> "doubler".equals(entity.specialId()) && entity.isAlive());
            helper.assertTrue(doublers.isEmpty(), "Doubler? must be killable instead of remaining invulnerable");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 210)
    public static void emptyLeadDebugRouteAcceptsTheFenceBeingInspected(GameTestHelper helper) {
        fillFloor(helper);
        helper.setBlock(8, 1, 8, Blocks.OAK_FENCE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 playerPosition = helper.absoluteVec(new Vec3(4.5D, 1.0D, 8.5D));
        player.moveTo(playerPosition.x, playerPosition.y, playerPosition.z, -90.0F, 0.0F);
        player.lookAt(
                EntityAnchorArgument.Anchor.EYES,
                Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 1, 8))));

        helper.runAtTickTime(2, () -> {
            helper.assertTrue(MinecraftNativeAnomalySystem.triggerForDebug(player, "empty_lead"),
                    "The dev route must accept the pointed fence even while the tester observes it");
            MinecraftNativeAnomalySystem.EmptyLeadDebugSnapshot snapshot =
                    MinecraftNativeAnomalySystem.emptyLeadSnapshotForTesting(player.getUUID())
                            .orElseThrow(() -> new AssertionError("Empty Lead must create one active task"));
            long now = player.getServer().getTickCount();
            helper.assertTrue(snapshot.minimumVisibleUntil() - now
                            == MinecraftNativeAnomalyRules.EMPTY_LEAD_MIN_VISIBLE_TICKS,
                    "Empty Lead must remain visible for nine and a half seconds before observation can end it");
            helper.assertTrue(snapshot.endTick() - now
                            >= MinecraftNativeAnomalyRules.EMPTY_LEAD_MIN_DURATION_TICKS
                            && snapshot.endTick() - now
                            <= MinecraftNativeAnomalyRules.EMPTY_LEAD_MAX_DURATION_TICKS,
                    "Empty Lead must last about ten seconds");
        });
        helper.runAtTickTime(170, () -> {
            helper.assertTrue(
                    MinecraftNativeAnomalySystem.emptyLeadSnapshotForTesting(player.getUUID()).isPresent(),
                    "Looking at Empty Lead must not erase it before its readable minimum duration");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 150)
    public static void emptyWakeBuildsALongWaterOnlyPathTowardThePlayer(GameTestHelper helper) {
        fillFloor(helper);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 1, z, Blocks.WATER);
            }
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 playerPosition = helper.absoluteVec(new Vec3(1.5D, 2.0D, 1.5D));
        player.moveTo(playerPosition.x, playerPosition.y, playerPosition.z, 0.0F, 0.0F);

        helper.runAtTickTime(2, () -> {
            helper.assertTrue(MinecraftNativeAnomalySystem.triggerForDebug(player, "empty_wake"),
                    "Empty Wake must accept a sufficiently long open water surface");
            MinecraftNativeAnomalySystem.EmptyWakeDebugSnapshot snapshot =
                    MinecraftNativeAnomalySystem.emptyWakeSnapshotForTesting(player.getUUID())
                            .orElseThrow(() -> new AssertionError("Empty Wake must create one active task"));
            List<Vec3> points = snapshot.points();
            helper.assertTrue(points.size() >= MinecraftNativeAnomalyRules.EMPTY_WAKE_MIN_POINTS,
                    "Empty Wake must contain enough points to remain visible for several seconds");
            helper.assertTrue(points.size() <= MinecraftNativeAnomalyRules.EMPTY_WAKE_TARGET_POINTS,
                    "Empty Wake pathfinding must remain bounded");
            helper.assertTrue(points.getFirst().distanceTo(player.position())
                            >= points.getLast().distanceTo(player.position()) + 6.0D,
                    "Empty Wake must travel from distant water toward the player");

            for (int index = 0; index < points.size(); index++) {
                Vec3 point = points.get(index);
                BlockPos water = BlockPos.containing(point.x, point.y - 0.15D, point.z);
                helper.assertTrue(helper.getLevel().getBlockState(water).is(Blocks.WATER),
                        "Every wake point must remain on a pure water block: " + water);
                helper.assertTrue(helper.getLevel().getBlockState(water.above()).isAir(),
                        "Every wake point must have clear air above it: " + water);
                if (index > 0) {
                    BlockPos previous = BlockPos.containing(
                            points.get(index - 1).x,
                            points.get(index - 1).y - 0.15D,
                            points.get(index - 1).z);
                    int horizontalStep = Math.abs(water.getX() - previous.getX())
                            + Math.abs(water.getZ() - previous.getZ());
                    helper.assertTrue(horizontalStep == 1 && water.getY() == previous.getY(),
                            "Wake path steps must be contiguous water surface cells");
                }
            }
        });
        helper.runAtTickTime(90, () -> {
            MinecraftNativeAnomalySystem.EmptyWakeDebugSnapshot snapshot =
                    MinecraftNativeAnomalySystem.emptyWakeSnapshotForTesting(player.getUUID())
                            .orElseThrow(() -> new AssertionError(
                                    "Empty Wake must still be running after four seconds"));
            helper.assertTrue(snapshot.index() > 0 && snapshot.index() < snapshot.points().size(),
                    "Empty Wake must advance progressively instead of appearing in one burst");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 150)
    public static void tenantCrossesItsDoorAndReachesAnInteriorPosition(GameTestHelper helper) {
        fillFloor(helper);
        for (int z = 0; z < 16; z++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(8, y, z, Blocks.STONE);
            }
        }
        helper.setBlock(
                8, 1, 8,
                Blocks.OAK_DOOR.defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                        .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
                        .setValue(BlockStateProperties.OPEN, false));
        helper.setBlock(
                8, 2, 8,
                Blocks.OAK_DOOR.defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                        .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)
                        .setValue(BlockStateProperties.OPEN, false));

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 playerPosition = helper.absoluteVec(new Vec3(3.5D, 1.0D, 8.5D));
        player.moveTo(playerPosition.x, playerPosition.y, playerPosition.z, 90.0F, 0.0F);

        UncannyTenantEntity tenant = UncannyEntityRegistry.UNCANNY_TENANT.get().create(helper.getLevel());
        helper.assertTrue(tenant != null, "Tenant? must be creatable");
        Vec3 outside = helper.absoluteVec(new Vec3(10.5D, 1.0D, 8.5D));
        BlockPos door = helper.absolutePos(new BlockPos(8, 1, 8));
        BlockPos interior = helper.absolutePos(new BlockPos(7, 1, 8));
        tenant.moveTo(outside.x, outside.y, outside.z, 90.0F, 0.0F);
        tenant.setupTenant(player, door, interior);
        tenant.setPersistenceRequired();
        helper.assertTrue(helper.getLevel().addFreshEntity(tenant), "Tenant? must enter the test level");

        helper.runAtTickTime(110, () -> {
            helper.assertTrue(tenant.isAlive(), "Tenant? must remain present before being observed indoors");
            helper.assertTrue(tenant.hasReachedHome(),
                    "Tenant? must reach the actual interior target instead of stopping outside the door");
            helper.assertTrue(tenant.position().distanceToSqr(Vec3.atBottomCenterOf(interior)) <= 1.5D * 1.5D,
                    "Tenant? must physically stand on the interior side of the wall; tenant="
                            + tenant.position() + ", interior=" + Vec3.atBottomCenterOf(interior));
            helper.assertTrue(!helper.getLevel().getBlockState(door).getValue(BlockStateProperties.OPEN),
                    "A door opened by Tenant? must return to its initially closed state");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void vanillaDerivedReplacementsRestoreTheirPhysicalSounds(GameTestHelper helper) {
        fillFloor(helper);
        List<EntityType<? extends Mob>> types = List.of(
                UncannyEntityRegistry.UNCANNY_BLAZE.get(),
                UncannyEntityRegistry.UNCANNY_DROWNED.get(),
                UncannyEntityRegistry.UNCANNY_ENDERMITE.get(),
                UncannyEntityRegistry.UNCANNY_EVOKER.get(),
                UncannyEntityRegistry.UNCANNY_HOGLIN.get(),
                UncannyEntityRegistry.UNCANNY_HUSK.get(),
                UncannyEntityRegistry.UNCANNY_MAGMA_CUBE.get(),
                UncannyEntityRegistry.UNCANNY_PHANTOM.get(),
                UncannyEntityRegistry.UNCANNY_PIGLIN_BRUTE.get(),
                UncannyEntityRegistry.UNCANNY_PILLAGER.get(),
                UncannyEntityRegistry.UNCANNY_RAVAGER.get(),
                UncannyEntityRegistry.UNCANNY_SLIME.get(),
                UncannyEntityRegistry.UNCANNY_SPIDERLING.get(),
                UncannyEntityRegistry.UNCANNY_STRAY.get(),
                UncannyEntityRegistry.UNCANNY_VINDICATOR.get(),
                UncannyEntityRegistry.UNCANNY_WITHER_SKELETON.get());

        for (int index = 0; index < types.size(); index++) {
            Mob entity = types.get(index).create(helper.getLevel());
            helper.assertTrue(entity != null, "Every audited replacement type must be creatable");
            int x = 2 + (index % 4) * 3;
            int z = 2 + (index / 4) * 3;
            Vec3 position = helper.absoluteVec(new Vec3(x + 0.5D, 1.0D, z + 0.5D));
            entity.moveTo(position.x, position.y, position.z, 0.0F, 0.0F);
            entity.setSilent(true);
            entity.setInvulnerable(true);
            entity.setPersistenceRequired();
            helper.assertTrue(helper.getLevel().addFreshEntity(entity),
                    "The audited replacement must enter the test level: " + types.get(index));
        }

        helper.runAtTickTime(5, () -> {
            List<Mob> replacements = helper.getLevel().getEntitiesOfClass(
                    Mob.class,
                    helper.getBounds(),
                    entity -> types.contains(entity.getType()));
            helper.assertTrue(replacements.size() == types.size(),
                    "Every audited replacement must remain present for the sound-policy check");
            for (Mob replacement : replacements) {
                helper.assertTrue(!replacement.isSilent(),
                        "Vanilla-derived replacement must clear the legacy blanket Silent flag: "
                                + replacement.getType());
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void adaptiveSpecialsUseTheStrongestCarriedLoadout(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        player.getInventory().clearContent();
        player.getInventory().items.set(0, new ItemStack(Items.WOODEN_SWORD));
        player.getInventory().items.set(1, new ItemStack(Items.DIAMOND_SWORD));
        player.getInventory().items.set(2, new ItemStack(Items.NETHERITE_AXE));
        player.getInventory().items.set(3, new ItemStack(Items.IRON_CHESTPLATE));
        player.getInventory().items.set(4, new ItemStack(Items.NETHERITE_CHESTPLATE));
        player.getInventory().items.set(5, new ItemStack(Items.NETHERITE_HELMET));
        player.getInventory().items.set(6, new ItemStack(Items.NETHERITE_LEGGINGS));
        player.getInventory().items.set(7, new ItemStack(Items.NETHERITE_BOOTS));
        player.getInventory().offhand.set(0, new ItemStack(Items.SHIELD));

        AdaptiveSpecialEquipment.Snapshot snapshot = AdaptiveSpecialEquipment.select(player);
        helper.assertTrue(snapshot.weapon().is(Items.NETHERITE_AXE),
                "The adaptive scan must choose highest effective damage, not the first sword");
        helper.assertTrue(snapshot.armor(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE),
                "The adaptive scan must choose the strongest chest armor from inventory");
        helper.assertTrue(snapshot.armorValue() >= 20.0D && snapshot.armorToughness() >= 12.0D,
                "The full netherite set must contribute its effective armor and toughness");

        var mimic = UncannyEntityRegistry.UNCANNY_DOUBLE_DORMANT.get().create(helper.getLevel());
        helper.assertTrue(mimic != null, "Mimic must be creatable for its loadout contract");
        mimic.copyTarget(player, player.blockPosition(), player.blockPosition());
        helper.assertTrue(mimic.getMainHandItem().is(Items.NETHERITE_AXE),
                "Mimic must equip the selected highest-damage weapon");
        helper.assertTrue(mimic.getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE),
                "Mimic must equip the selected strongest chestplate");

        UncannyStalkerEntity attacker = UncannyEntityRegistry.UNCANNY_STALKER.get().create(helper.getLevel());
        helper.assertTrue(attacker != null, "Attacker? must be creatable for its adaptive stat contract");
        attacker.setHuntTarget(player);
        // Duel parity (2026-10-08): toughness follows the best sustained weapon, lethality the
        // armour actually worn. The diamond sword out-damages the slower netherite axe per second.
        var offense = com.eotv.echoofthevoid.event.special.CombatParity.bestOffense(helper.getLevel(), player, attacker);
        helper.assertTrue(offense.weapon().is(Items.DIAMOND_SWORD),
                "Parity must measure the weapon with the highest sustained damage: " + offense.weapon());
        double expectedHealth = com.eotv.echoofthevoid.event.special.CombatParityRules.maxHealth(
                com.eotv.echoofthevoid.event.special.CombatParityRules.ATTACKER,
                offense.hitDamage(), offense.attacksPerSecond());
        helper.assertTrue(Math.abs(attacker.getMaxHealth() - expectedHealth) < 0.01D,
                "Attacker? must be exactly as tough as the strongest carried weapon: " + attacker.getMaxHealth());
        helper.assertTrue(attacker.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) == 0.0D,
                "Toughness lives in health, never in armour that would distort the weapon arithmetic");
        double effective = com.eotv.echoofthevoid.event.special.CombatParity.reducedDamage(
                helper.getLevel(),
                player,
                helper.getLevel().damageSources().mobAttack(attacker),
                attacker.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
        double expectedEffective = com.eotv.echoofthevoid.event.special.CombatParityRules.effectiveDamagePerHit(
                com.eotv.echoofthevoid.event.special.CombatParityRules.ATTACKER, player.getMaxHealth());
        helper.assertTrue(Math.abs(effective - expectedEffective) < 0.05D,
                "Each landed hit must take the planned share of the player's health: " + effective);

        // Wearing the netherite set is answered by harder raw hits, not by a safer fight.
        player.getInventory().armor.set(2, new ItemStack(Items.NETHERITE_CHESTPLATE));
        player.getInventory().armor.set(3, new ItemStack(Items.NETHERITE_HELMET));
        player.getInventory().armor.set(1, new ItemStack(Items.NETHERITE_LEGGINGS));
        player.getInventory().armor.set(0, new ItemStack(Items.NETHERITE_BOOTS));
        attacker.setHuntTarget(player);
        double armouredRaw = attacker.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        double armouredEffective = com.eotv.echoofthevoid.event.special.CombatParity.reducedDamage(
                helper.getLevel(), player, helper.getLevel().damageSources().mobAttack(attacker), armouredRaw);
        helper.assertTrue(Math.abs(armouredEffective - expectedEffective) < 0.05D,
                "Armour must not change the share of health a landed hit takes: " + armouredEffective);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void scenicSpecialsCanBeStruckAndSinkInsteadOfDying(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        UncannyApprovedSpecialEntity surveyor = UncannyEntityRegistry.UNCANNY_SURVEYOR.get().create(helper.getLevel());
        helper.assertTrue(surveyor != null, "Surveyor? must be creatable");
        surveyor.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(8.5D, 1.0D, 8.5D)));
        surveyor.setup(player, helper.absolutePos(new BlockPos(8, 1, 12)));
        helper.assertTrue(helper.getLevel().addFreshEntity(surveyor), "Surveyor? must enter the level");
        // User decision 2026-10-08: scenes are reachable; sinking away is their only protection.
        helper.assertTrue(surveyor.isAttackable() && surveyor.canBeHitByProjectile(),
                "A player's sword or arrow must reach a scenic Special");
        helper.assertTrue(surveyor.isInvulnerableTo(helper.getLevel().damageSources().fall()),
                "The world itself (falls, lava, walls) must never end a scene");
        float before = surveyor.getHealth();
        helper.assertTrue(surveyor.hurt(helper.getLevel().damageSources().playerAttack(player), 1000.0F),
                "A player's blow must land");
        helper.assertTrue(surveyor.isAlive() && surveyor.getHealth() >= 1.0F && surveyor.getHealth() < before,
                "The blow is felt but never kills");
        helper.assertTrue(surveyor.isSinkingAway() && !surveyor.isAttackable(),
                "Once struck it sinks away and cannot be struck again");
        surveyor.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void aSinkingSceneDissolvesInsteadOfPokingThroughACeiling(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        // A one-block floor with an open room right under it: someone below would see the body.
        for (int x = 6; x <= 10; x++) {
            for (int z = 6; z <= 10; z++) {
                helper.setBlock(x, 4, z, net.minecraft.world.level.block.Blocks.STONE);
                helper.setBlock(x, 3, z, net.minecraft.world.level.block.Blocks.AIR);
                helper.setBlock(x, 2, z, net.minecraft.world.level.block.Blocks.AIR);
            }
        }
        UncannyApprovedSpecialEntity surveyor = UncannyEntityRegistry.UNCANNY_SURVEYOR.get().create(helper.getLevel());
        helper.assertTrue(surveyor != null, "Surveyor? must be creatable");
        surveyor.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(8.5D, 5.0D, 8.5D)));
        surveyor.setup(player, helper.absolutePos(new BlockPos(8, 5, 12)));
        helper.assertTrue(helper.getLevel().addFreshEntity(surveyor), "Surveyor? must enter the level");
        surveyor.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.assertTrue(surveyor.isSinkingAway(), "The blow must start the sink");
        // The full sink lasts twenty ticks; crossing a one-block floor takes about nine.
        helper.runAtTickTime(14, () -> {
            helper.assertTrue(surveyor.isRemoved(),
                    "Once its feet reach open air below, it must dissolve rather than slide through the ceiling");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void scenicSpecialsAreInvisibleToVanillaGolemTargeting(GameTestHelper helper) {
        fillFloor(helper);
        IronGolem ironGolem = EntityType.IRON_GOLEM.create(helper.getLevel());
        SnowGolem snowGolem = EntityType.SNOW_GOLEM.create(helper.getLevel());
        UncannyApprovedSpecialEntity surveyor = UncannyEntityRegistry.UNCANNY_SURVEYOR.get().create(helper.getLevel());
        UncannyApprovedSpecialEntity doubler = UncannyEntityRegistry.UNCANNY_DOUBLER.get().create(helper.getLevel());
        var watcher = UncannyEntityRegistry.UNCANNY_WATCHER.get().create(helper.getLevel());
        var terror = UncannyEntityRegistry.UNCANNY_TERROR.get().create(helper.getLevel());
        helper.assertTrue(ironGolem != null && snowGolem != null && surveyor != null
                        && doubler != null && watcher != null && terror != null,
                "All golem-targeting audit entities must be creatable");

        helper.assertTrue(!surveyor.canBeSeenAsEnemy()
                        && !ironGolem.canAttack(surveyor)
                        && !snowGolem.canAttack(surveyor),
                "A scenic Approved Special must never enter Iron/Snow Golem targeting");
        helper.assertTrue(!watcher.canBeSeenAsEnemy() && !terror.canBeSeenAsEnemy(),
                "Watcher? and Terror? must be protected presentation entities");
        helper.assertTrue(doubler.canBeSeenAsEnemy()
                        && ironGolem.canAttack(doubler)
                        && snowGolem.canAttack(doubler),
                "Doubler? is deliberately killable and must retain normal hostile targeting");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void uncannyBlockMatchesObsidianResistanceAndHasNoSound(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(8, 1, 8));
        var uncanny = UncannyBlockRegistry.UNCANNY_BLOCK.get().defaultBlockState();
        var obsidian = Blocks.OBSIDIAN.defaultBlockState();
        helper.setBlock(8, 1, 8, uncanny);
        helper.assertTrue(uncanny.getDestroySpeed(helper.getLevel(), pos)
                        == obsidian.getDestroySpeed(helper.getLevel(), pos),
                "Uncanny Block hardness must match Obsidian");
        helper.assertTrue(UncannyBlockRegistry.UNCANNY_BLOCK.get().getExplosionResistance()
                        == Blocks.OBSIDIAN.getExplosionResistance(),
                "Uncanny Block blast resistance must match Obsidian");
        helper.assertTrue(uncanny.getSoundType() == SoundType.EMPTY,
                "Uncanny Block must have no place, break, hit, fall or step sound");
        helper.assertTrue(uncanny.is(BlockTags.MINEABLE_WITH_PICKAXE)
                        && uncanny.is(BlockTags.NEEDS_DIAMOND_TOOL),
                "Uncanny Block must use the Obsidian-level pickaxe tool contract");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 420)
    public static void followerWalksAcrossLoadedWaterSurfaceWithoutAbandoning(GameTestHelper helper) {
        fillFloor(helper);
        for (int x = 3; x <= 11; x++) {
            for (int z = 6; z <= 10; z++) {
                helper.setBlock(x, 1, z, Blocks.WATER);
            }
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 ownerPosition = helper.absoluteVec(new Vec3(14.5D, 1.0D, 8.5D));
        player.moveTo(ownerPosition.x, ownerPosition.y, ownerPosition.z, -90.0F, 0.0F);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, ownerPosition.add(10.0D, 0.0D, 0.0D));
        player.setInvulnerable(true);
        UncannyFollowerEntity follower = UncannyEntityRegistry.UNCANNY_FOLLOWER.get().create(helper.getLevel());
        helper.assertTrue(follower != null, "Follower? must be creatable");
        Vec3 start = helper.absoluteVec(new Vec3(1.5D, 1.0D, 8.5D));
        follower.moveTo(start.x, start.y, start.z, -90.0F, 0.0F);
        follower.setupFollower(player, 6_000L);
        helper.assertTrue(helper.getLevel().addFreshEntity(follower),
                "Follower? must enter the loaded water-crossing fixture");

        helper.runAtTickTime(360, () -> {
            helper.assertTrue(follower.isAlive() && !follower.isRemoved(),
                    "Entering water must never make Follower? abandon its encounter");
            Vec3 towardOwner = ownerPosition.subtract(start).normalize();
            double progress = follower.position().subtract(start).dot(towardOwner);
            helper.assertTrue(progress >= 8.0D,
                    "Follower? must physically traverse at least eight loaded water cells");
            helper.assertTrue(!follower.isUnderWater(),
                    "Follower? must walk on the surface instead of diving through the route");
            follower.discard();
            helper.succeed();
        });
    }

    private static void assertFerrymanBoundAndSubmerged(
            GameTestHelper helper,
            ServerPlayer player,
            Boat boat,
            int sampleTick) {
        List<UncannyApprovedSpecialEntity> ferrymen = findFerrymen(helper, boat);
        helper.assertTrue(ferrymen.size() == 1,
                "Exactly one living Ferryman? must remain at sample tick " + sampleTick);
        UncannyApprovedSpecialEntity ferryman = ferrymen.getFirst();
        helper.assertTrue(ferryman.focusedBoatId().filter(boat.getUUID()::equals).isPresent(),
                "Ferryman? must stay bound to the boat selected at spawn");
        helper.assertTrue(ferryman.getY() <= boat.getY() - 1.90D,
                "Ferryman? must remain fully below the boat at sample tick " + sampleTick);
        helper.assertTrue(ferryman.position().subtract(boat.position()).horizontalDistance() <= 4.5D,
                "Ferryman? must follow the boat without teleporting away");
        BlockPos eye = BlockPos.containing(ferryman.getX(), ferryman.getEyeY(), ferryman.getZ());
        helper.assertTrue(helper.getLevel().getFluidState(eye).is(FluidTags.WATER),
                "Ferryman?'s eyes must remain submerged at sample tick " + sampleTick);
        helper.assertTrue(boat.isAlive(), "Ferryman? must not damage the boat");
        helper.assertTrue(player.getVehicle() == boat, "Ferryman? must not eject the passenger");
    }

    private static List<UncannyApprovedSpecialEntity> findFerrymen(GameTestHelper helper, Boat boat) {
        return helper.getLevel().getEntitiesOfClass(
                UncannyApprovedSpecialEntity.class,
                new AABB(boat.position().add(-16.0D, -8.0D, -16.0D), boat.position().add(16.0D, 8.0D, 16.0D)),
                entity -> entity.isAlive() && "ferryman".equals(entity.specialId()));
    }

    private static UncannyApprovedSpecialEntity findSingleSpecial(GameTestHelper helper, String id) {
        List<UncannyApprovedSpecialEntity> matches = helper.getLevel().getEntitiesOfClass(
                UncannyApprovedSpecialEntity.class,
                helper.getBounds().inflate(12.0D),
                entity -> entity.isAlive() && id.equals(entity.specialId()));
        helper.assertTrue(matches.size() == 1,
                "Expected one living " + id + " Special, found " + matches.size());
        return matches.getFirst();
    }

    private static UncannyStalkerEntity findSingleAttacker(GameTestHelper helper, ServerPlayer player) {
        List<UncannyStalkerEntity> matches = helper.getLevel().getEntitiesOfClass(
                UncannyStalkerEntity.class,
                player.getBoundingBox().inflate(32.0D),
                entity -> entity.isAlive() && targetsPlayer(entity, player));
        helper.assertTrue(matches.size() == 1,
                "Expected one living Attacker? animation study, found " + matches.size());
        return matches.getFirst();
    }

    private static boolean targetsPlayer(UncannyStalkerEntity entity, ServerPlayer player) {
        CompoundTag state = new CompoundTag();
        entity.saveWithoutId(state);
        return state.hasUUID("TargetPlayer") && player.getUUID().equals(state.getUUID("TargetPlayer"));
    }

    private static void fillFloor(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
    }
}
