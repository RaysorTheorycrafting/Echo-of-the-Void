package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyApprovedSpecialEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyStalkerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyTenantEntity;
import com.eotv.echoofthevoid.event.UncannyParanoiaEventSystem;
import com.eotv.echoofthevoid.event.UncannyWatcherSystem;
import com.eotv.echoofthevoid.event.paranoia.GhostMinerBlockPolicy;
import com.eotv.echoofthevoid.event.paranoia.nativeevent.MinecraftNativeAnomalySystem;
import com.eotv.echoofthevoid.event.paranoia.nativeevent.MinecraftNativeAnomalyRules;
import com.eotv.echoofthevoid.event.special.ApprovedSpecialSystem;
import com.eotv.echoofthevoid.event.special.GrandWardenRules;
import com.eotv.echoofthevoid.event.special.UncannySpecialRewardRules;
import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
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

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void attackerAnimationStudiesUseDistinctSyncedStyles(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 playerPosition = helper.absoluteVec(new Vec3(8.5D, 1.0D, 8.5D));
        player.moveTo(playerPosition.x, playerPosition.y, playerPosition.z, 90.0F, 0.0F);

        helper.assertTrue(UncannyParanoiaEventSystem.spawnStalkerForCommand(
                        player, UncannyStalkerEntity.AnimationStyle.CRAWL),
                "The all-fours Attacker? study must be spawnable through its QA route");
        UncannyStalkerEntity crawl = findSingleAttacker(helper, player);
        helper.assertTrue(crawl.getAnimationStyle() == UncannyStalkerEntity.AnimationStyle.CRAWL,
                "The all-fours style must be stored in synced entity data");
        crawl.discard();

        helper.assertTrue(UncannyParanoiaEventSystem.spawnStalkerForCommand(
                        player, UncannyStalkerEntity.AnimationStyle.OUTSTRETCHED),
                "The arms-forward Attacker? study must be spawnable through its QA route");
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
        UncannyStalkerEntity ordinary = findSingleAttacker(helper, player);
        helper.assertTrue(
                ordinary.getAnimationStyle() == UncannyStalkerEntity.AnimationStyle.CRAWL
                        || ordinary.getAnimationStyle() == UncannyStalkerEntity.AnimationStyle.OUTSTRETCHED,
                "Ordinary Attacker? spawns must never use a third visual form");
        helper.succeed();
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
                entity -> entity.isAlive());
        helper.assertTrue(matches.size() == 1,
                "Expected one living Attacker? animation study, found " + matches.size());
        return matches.getFirst();
    }

    private static void fillFloor(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
    }
}
