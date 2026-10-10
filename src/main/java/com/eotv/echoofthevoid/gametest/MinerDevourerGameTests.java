package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import com.eotv.echoofthevoid.block.entity.custom.UncannyRestorationPlaceholderBlockEntity;
import com.eotv.echoofthevoid.dev.UncannyDevActionExecutor;
import com.eotv.echoofthevoid.dev.UncannyDevCatalog;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyArenaPursuerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyDevourerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyMinerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyStalkerEntity;
import com.eotv.echoofthevoid.event.special.DevourerArenaSession;
import com.eotv.echoofthevoid.event.special.DevourerArenaRules;
import com.eotv.echoofthevoid.event.special.DevourerArenaSystem;
import com.eotv.echoofthevoid.event.special.ArenaPursuerAppearance;
import com.eotv.echoofthevoid.event.special.MinerTunnelPlanner;
import com.eotv.echoofthevoid.event.special.UncannyMinerBlockPolicy;
import com.eotv.echoofthevoid.event.special.UncannyMinerSystem;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import com.eotv.echoofthevoid.world.UncannyDimensions;
import java.util.ArrayList;
import java.util.List;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Headless safety checks for Miner?, Devourer? and the isolated Elsewhere trial. */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class MinerDevourerGameTests {
    private static final String TEMPLATE = "special_test_room";

    private MinerDevourerGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void minerPolicyRejectsValuablesDataGravityFluidsAndModBlocks(GameTestHelper helper) {
        assertMaterial(helper, new BlockPos(2, 2, 2), Blocks.STONE,
                UncannyMinerBlockPolicy.MaterialKind.NATURAL);
        assertMaterial(helper, new BlockPos(4, 2, 2), Blocks.OAK_PLANKS,
                UncannyMinerBlockPolicy.MaterialKind.CONSTRUCTION);
        BlockPos mixedColumn = new BlockPos(4, 2, 4);
        helper.setBlock(mixedColumn, Blocks.STONE);
        helper.setBlock(mixedColumn.above(), Blocks.OAK_PLANKS);
        helper.assertTrue(
                UncannyMinerBlockPolicy.classifyColumn(helper.getLevel(), helper.absolutePos(mixedColumn))
                        == UncannyMinerBlockPolicy.MaterialKind.CONSTRUCTION,
                "A construction block in either half of a tunnel column must consume its allowance");
        assertMaterial(helper, new BlockPos(6, 2, 2), Blocks.DIAMOND_ORE,
                UncannyMinerBlockPolicy.MaterialKind.BLOCKED);
        assertMaterial(helper, new BlockPos(8, 2, 2), Blocks.WATER,
                UncannyMinerBlockPolicy.MaterialKind.BLOCKED);
        assertMaterial(helper, new BlockPos(10, 2, 2), Blocks.SAND,
                UncannyMinerBlockPolicy.MaterialKind.BLOCKED);
        assertMaterial(helper, new BlockPos(12, 2, 2), Blocks.CHEST,
                UncannyMinerBlockPolicy.MaterialKind.BLOCKED);
        assertMaterial(helper, new BlockPos(14, 2, 2), UncannyBlockRegistry.UNCANNY_ALTAR.get(),
                UncannyMinerBlockPolicy.MaterialKind.BLOCKED);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void restorationSectionStaysPairedAndDefersAroundItsOwner(GameTestHelper helper) {
        BlockPos lower = new BlockPos(8, 1, 8);
        BlockPos upper = lower.above();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 occupied = Vec3.atBottomCenterOf(helper.absolutePos(lower));
        player.moveTo(occupied.x, occupied.y, occupied.z, 0.0F, 0.0F);
        long sharedDeadline = helper.getLevel().getGameTime() + 1L;
        for (BlockPos relative : List.of(lower, upper)) {
            BlockPos absolute = helper.absolutePos(relative);
            helper.setBlock(relative, UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get());
            var blockEntity = helper.getLevel().getBlockEntity(absolute);
            helper.assertTrue(blockEntity instanceof UncannyRestorationPlaceholderBlockEntity,
                    "Each half of a restored tunnel section must own persistent restoration state");
            ((UncannyRestorationPlaceholderBlockEntity) blockEntity).initialize(
                    Blocks.STONE.defaultBlockState(), sharedDeadline, player.getUUID());
        }

        helper.runAtTickTime(4, () -> {
            helper.assertBlockPresent(UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get(), lower);
            helper.assertBlockPresent(UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get(), upper);
        });
        helper.runAtTickTime(6, () -> player.moveTo(occupied.x + 3.0D, occupied.y, occupied.z, 0.0F, 0.0F));
        helper.runAtTickTime(12, () -> {
            helper.assertBlockPresent(UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get(), lower);
            helper.assertBlockPresent(UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get(), upper);
        });
        helper.runAtTickTime(14, () -> player.moveTo(occupied.x + 6.0D, occupied.y, occupied.z, 0.0F, 0.0F));
        helper.runAtTickTime(30, () -> {
            helper.assertBlockPresent(Blocks.STONE, lower);
            helper.assertBlockPresent(Blocks.STONE, upper);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void minerPlannerSupportsWallAndFloorRoutesWithoutChangingItsBounds(GameTestHelper helper) {
        fillFloor(helper);
        for (int x = 5; x <= 15; x++) {
            helper.setBlock(x, 1, 8, Blocks.STONE);
            helper.setBlock(x, 2, 8, Blocks.STONE);
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 wallHeight = helper.absoluteVec(new Vec3(1.5D, 1.0D, 8.5D));
        player.moveTo(wallHeight.x, wallHeight.y, wallHeight.z, 0.0F, 0.0F);
        BlockPos expectedStart = helper.absolutePos(new BlockPos(15, 1, 8));
        helper.assertTrue(UncannyMinerBlockPolicy.classify(helper.getLevel(), expectedStart)
                        == UncannyMinerBlockPolicy.MaterialKind.NATURAL,
                "The exact-distance stone start column must remain eligible: "
                        + helper.getLevel().getBlockState(expectedStart));
        MinerTunnelPlanner.TunnelPlan wall = MinerTunnelPlanner.findInitialForGameTest(
                helper.getLevel(), player, RandomSource.create(19L), false);
        helper.assertTrue(wall != null && !wall.floorApproach(),
                "A two-high stone wall must yield a bounded wall route");
        helper.assertTrue(wall.columns().size() <= 20,
                "Wall route must respect the twenty-section maximum");

        // Reproduce a real underground ceiling. The first upward step needs to excavate the
        // headroom over the column it leaves, not just the destination's two visible blocks.
        for (int x = 5; x <= 15; x++) {
            helper.setBlock(x, 3, 8, Blocks.STONE);
        }
        helper.setBlock(5, 4, 8, Blocks.STONE);
        for (int x = 0; x <= 4; x++) {
            for (int z = 6; z <= 10; z++) {
                helper.setBlock(x, 2, z, Blocks.STONE);
            }
        }
        Vec3 floorHeight = helper.absoluteVec(new Vec3(1.5D, 3.0D, 8.5D));
        player.moveTo(floorHeight.x, floorHeight.y, floorHeight.z, 0.0F, 0.0F);
        MinerTunnelPlanner.TunnelPlan floor = MinerTunnelPlanner.findInitialForGameTest(
                helper.getLevel(), player, RandomSource.create(23L), true);
        helper.assertTrue(floor != null && floor.floorApproach(),
                "The same stone volume below the player must yield a bounded floor route");
        helper.assertTrue(floor.columns().size() <= 20,
                "Floor route must respect the twenty-section maximum");
        BlockPos staircase = floor.columns().getLast();
        BlockPos beforeStaircase = floor.columns().get(floor.columns().size() - 2);
        helper.assertTrue(staircase.getY() == beforeStaircase.getY() + 1
                        && Math.abs(staircase.getX() - beforeStaircase.getX())
                                + Math.abs(staircase.getZ() - beforeStaircase.getZ()) == 1,
                "A floor route must finish with a diagonal one-block staircase section");
        helper.assertTrue(floor.emergencePosition().getY() == staircase.getY() + 1
                        && Math.abs(floor.emergencePosition().getX() - staircase.getX())
                                + Math.abs(floor.emergencePosition().getZ() - staircase.getZ()) == 1,
                "The final emergence must be a second diagonal step into standable open space");
        helper.assertTrue(UncannyMinerBlockPolicy.classifyAscendingTransition(
                                helper.getLevel(), beforeStaircase, staircase)
                        != UncannyMinerBlockPolicy.MaterialKind.BLOCKED,
                "The first underground stair must include safe swept headroom");
        helper.assertTrue(UncannyMinerBlockPolicy.classifyAscendingExitTransition(
                                helper.getLevel(), staircase, floor.emergencePosition())
                        != UncannyMinerBlockPolicy.MaterialKind.BLOCKED,
                "The final open exit must include safe swept headroom");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void minerPlannerNeverUsesItsRestorationTunnelAsAnExit(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 targetPosition = helper.absoluteVec(new Vec3(4.5D, 1.0D, 8.5D));
        player.moveTo(targetPosition.x, targetPosition.y, targetPosition.z, 0.0F, 0.0F);

        BlockPos current = helper.absolutePos(new BlockPos(8, 1, 8));
        BlockPos previousTunnelColumn = helper.absolutePos(new BlockPos(7, 1, 8));
        for (BlockPos cell : List.of(previousTunnelColumn, previousTunnelColumn.above())) {
            helper.getLevel().setBlock(
                    cell,
                    UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get().defaultBlockState(),
                    3);
            helper.assertTrue(
                    helper.getLevel().getBlockEntity(cell)
                            instanceof UncannyRestorationPlaceholderBlockEntity placeholder,
                    "The reproduced previous tunnel column must own restoration state");
            ((UncannyRestorationPlaceholderBlockEntity) helper.getLevel().getBlockEntity(cell))
                    .initialize(Blocks.STONE.defaultBlockState(), helper.getLevel().getGameTime() + 160L, null);
        }

        helper.assertTrue(
                !MinerTunnelPlanner.canUseRetainedEmergence(
                        helper.getLevel(), current, previousTunnelColumn, player),
                "A restoration placeholder column is the tunnel behind Miner?, never an exterior exit");
        MinerTunnelPlanner.TunnelPlan continuation = MinerTunnelPlanner.replan(
                helper.getLevel(), current, player, 6, 0);
        helper.assertTrue(continuation != null,
                "The planner must still find a genuine open exit beside the reproduced tunnel");
        helper.assertTrue(!continuation.emergencePosition().equals(previousTunnelColumn),
                "A replan must not reverse into the previous restoration column");
        helper.assertTrue(
                !helper.getLevel().getBlockState(continuation.emergencePosition())
                                .is(UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get())
                        && !helper.getLevel().getBlockState(continuation.emergencePosition().above())
                                .is(UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get()),
                "A wall emergence must be two genuine open blocks, not a temporary tunnel marker");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 1220)
    public static void minerRunsAFullUndergroundTunnelAndActuallyHitsItsTarget(GameTestHelper helper) {
        fillFloor(helper);
        for (int x = 5; x <= 15; x++) {
            helper.setBlock(x, 1, 8, Blocks.STONE);
            helper.setBlock(x, 2, 8, Blocks.STONE);
            helper.setBlock(x, 3, 8, Blocks.STONE);
        }
        helper.setBlock(5, 4, 8, Blocks.STONE);
        for (int x = 0; x <= 4; x++) {
            for (int z = 6; z <= 10; z++) {
                helper.setBlock(x, 2, z, Blocks.STONE);
            }
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 position = helper.absoluteVec(new Vec3(1.5D, 3.0D, 8.5D));
        player.moveTo(position.x, position.y, position.z, 0.0F, 0.0F);
        player.setInvulnerable(false);
        player.getAbilities().invulnerable = false;
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(512.0D);
        player.setHealth(512.0F);
        float startingHealth = player.getHealth();
        boolean previousMobGriefing = helper.getLevel().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, helper.getLevel().getServer());
        helper.assertTrue(UncannyMinerSystem.spawnDebugTunnel(player),
                "Miner? must accept the exact 14-18 block production tunnel in QA");

        UUID[] minerId = new UUID[1];
        boolean[] reloadedMidTunnel = new boolean[1];
        helper.runAtTickTime(2, () -> {
            List<UncannyMinerEntity> miners = helper.getLevel().getEntitiesOfClass(
                    UncannyMinerEntity.class, helper.getBounds().inflate(2.0D), Entity::isAlive);
            helper.assertTrue(miners.size() == 1 && !miners.getFirst().isInvisible(),
                    "The physical Miner? must be visible even while it is still tunnelling");
            minerId[0] = miners.getFirst().getUUID();
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                            UncannyStalkerEntity.class,
                            helper.getBounds().inflate(2.0D),
                            entity -> entity.isAlive()
                                    && entity.getType() == UncannyEntityRegistry.UNCANNY_STALKER.get())
                            .isEmpty(),
                    "The tunnel route must not create a second Attacker? entity");
        });
        helper.runAtTickTime(110, () -> {
            List<UncannyMinerEntity> active = helper.getLevel().getEntitiesOfClass(
                    UncannyMinerEntity.class,
                    helper.getBounds().inflate(2.0D),
                    miner -> miner.isAlive() && miner.getUUID().equals(minerId[0]));
            helper.assertTrue(active.size() == 1
                            && active.getFirst().stage() == UncannyMinerEntity.Stage.BURROWING,
                    "The reload boundary must occur during the real tunnelling state");
            UncannyMinerEntity original = active.getFirst();
            CompoundTag saved = new CompoundTag();
            original.saveWithoutId(saved);
            original.discard();

            UncannyMinerEntity reloaded = UncannyEntityRegistry.UNCANNY_MINER.get().create(helper.getLevel());
            helper.assertTrue(reloaded != null, "Miner? must be constructible for tunnel reload");
            reloaded.load(saved);
            helper.assertTrue(reloaded.getUUID().equals(minerId[0])
                            && reloaded.stage() == UncannyMinerEntity.Stage.BURROWING,
                    "Miner? must preserve its UUID and tunnelling stage across reload");
            helper.assertTrue(helper.getLevel().addFreshEntity(reloaded),
                    "The reloaded Miner? must re-enter the same server level");
            reloadedMidTunnel[0] = true;
        });
        helper.runAtTickTime(170, () -> {
            Vec3 moved = helper.absoluteVec(new Vec3(2.5D, 3.0D, 7.5D));
            player.moveTo(moved.x, moved.y, moved.z, 0.0F, 0.0F);
        });
        helper.runAtTickTime(900, () -> {
            Vec3 awayFromTunnel = helper.absoluteVec(new Vec3(1.5D, 3.0D, 6.5D));
            player.moveTo(awayFromTunnel.x, awayFromTunnel.y, awayFromTunnel.z, 0.0F, 0.0F);
        });
        helper.runAtTickTime(1180, () -> {
            List<UncannyMinerEntity> miners = helper.getLevel().getEntitiesOfClass(
                    UncannyMinerEntity.class, player.getBoundingBox().inflate(40.0D), Entity::isAlive);
            helper.assertTrue(miners.size() == 1
                            && miners.getFirst().getUUID().equals(minerId[0])
                            && miners.getFirst().stage() == UncannyMinerEntity.Stage.HUNTING
                            && !miners.getFirst().isInvisible(),
                    "The same visible Miner? must physically climb out and remain in its hunting stage");
            helper.assertTrue(reloadedMidTunnel[0]
                            && miners.getFirst().confirmedTargetHits() >= 2
                            && player.getHealth() < startingHealth,
                    "The reloaded end-to-end sequence must continue through multiple real Miner? melee hits");
            for (int x = 5; x <= 15; x++) {
                helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 1, 8));
                helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 2, 8));
                helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 3, 8));
            }
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(5, 4, 8));
            AABB tunnelVolume = new AABB(
                    helper.absoluteVec(new Vec3(4.0D, 0.5D, 7.0D)),
                    helper.absoluteVec(new Vec3(16.0D, 6.0D, 10.0D)));
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(
                    ItemEntity.class, tunnelVolume);
            helper.assertTrue(drops.isEmpty(), "Miner?'s artificial pickup cue must never create a real drop");
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING)
                    .set(previousMobGriefing, helper.getLevel().getServer());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 1120, batch = "miner_wall_sequence")
    public static void minerWalksOutOfAHorizontalTunnelAndAttacks(GameTestHelper helper) {
        fillFloor(helper);
        for (int x = 5; x <= 15; x++) {
            helper.setBlock(x, 1, 8, Blocks.STONE);
            helper.setBlock(x, 2, 8, Blocks.STONE);
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 position = helper.absoluteVec(new Vec3(1.5D, 1.0D, 8.5D));
        player.moveTo(position.x, position.y, position.z, 0.0F, 0.0F);
        player.setInvulnerable(false);
        player.getAbilities().invulnerable = false;
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(512.0D);
        player.setHealth(512.0F);
        float startingHealth = player.getHealth();
        boolean previousMobGriefing = helper.getLevel().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING)
                .set(true, helper.getLevel().getServer());
        List<BlockPos> exactColumns = new ArrayList<>();
        for (int x = 15; x >= 5; x--) {
            exactColumns.add(helper.absolutePos(new BlockPos(x, 1, 8)));
        }
        BlockPos exactEmergence = helper.absolutePos(new BlockPos(4, 1, 8));
        MinerTunnelPlanner.TunnelPlan exactPlan = new MinerTunnelPlanner.TunnelPlan(
                List.copyOf(exactColumns), exactEmergence, false, 0);
        UncannyMinerEntity miner = UncannyEntityRegistry.UNCANNY_MINER.get().create(helper.getLevel());
        helper.assertTrue(miner != null && miner.initializeTunnel(player, exactPlan, true),
                "The deterministic horizontal wall route must initialize Miner?");
        helper.assertTrue(helper.getLevel().addFreshEntity(miner),
                "The deterministic wall route must contain one physical Miner?");

        UUID minerId = miner.getUUID();
        UncannyMinerEntity[] activeMiner = {miner};
        boolean[] emergenceReloaded = {false};
        boolean[] huntingObserved = {false};
        boolean[] knockbackApplied = {false};
        boolean[] targetMovedClear = {false};
        BlockPos[] finalTunnelColumn = {null};
        for (int tick = 2; tick < 1080; tick++) {
            helper.runAtTickTime(tick, () -> {
                UncannyMinerEntity currentMiner = activeMiner[0];
                if (!currentMiner.isAlive()) {
                    helper.fail("The same Miner? must survive its wall tunnel, emergence and combat hand-off");
                    return;
                }
                if (!emergenceReloaded[0]
                        && currentMiner.stage() == UncannyMinerEntity.Stage.EMERGING) {
                    CompoundTag saved = new CompoundTag();
                    currentMiner.saveWithoutId(saved);
                    helper.assertTrue(saved.contains("MinerExitTunnelColumn")
                                    && saved.contains("MinerEmergencePositionReached")
                                    && saved.contains("MinerHuntPathValidationTicks"),
                            "The wall exit and combat hand-off state must be persistent");
                    currentMiner.discard();
                    UncannyMinerEntity reloaded = UncannyEntityRegistry.UNCANNY_MINER.get()
                            .create(helper.getLevel());
                    helper.assertTrue(reloaded != null, "Miner? must be constructible during emergence reload");
                    reloaded.load(saved);
                    helper.assertTrue(reloaded.getUUID().equals(minerId)
                                    && reloaded.stage() == UncannyMinerEntity.Stage.EMERGING,
                            "Emergence reload must preserve the same Miner? and stage");
                    helper.assertTrue(helper.getLevel().addFreshEntity(reloaded),
                            "The emerging Miner? must resume after reload");
                    activeMiner[0] = reloaded;
                    emergenceReloaded[0] = true;
                    return;
                }
                if (currentMiner.stage() != UncannyMinerEntity.Stage.HUNTING) {
                    return;
                }
                if (!huntingObserved[0]) {
                    huntingObserved[0] = true;
                    CompoundTag state = new CompoundTag();
                    currentMiner.saveWithoutId(state);
                    helper.assertTrue(state.contains("MinerExitTunnelColumn")
                                    && state.contains("MinerEmergencePosition"),
                            "A hunting Miner? must retain its physical exit geometry");
                    finalTunnelColumn[0] = BlockPos.of(state.getLong("MinerExitTunnelColumn"));
                    BlockPos emergence = BlockPos.of(state.getLong("MinerEmergencePosition"));
                    helper.assertTrue(MinerTunnelPlanner.isGenuineOpenEmergence(helper.getLevel(), emergence),
                            "Combat may begin only after Miner? reaches genuine two-block exterior air");
                    helper.assertTrue(isCollisionFree(helper, finalTunnelColumn[0])
                                    && isCollisionFree(helper, finalTunnelColumn[0].above()),
                            "The final wall opening must be two blocks high when combat begins");
                    helper.assertTrue(helper.getLevel().noCollision(currentMiner),
                            "Miner? must not begin combat intersecting its restored wall");
                }
                if (!knockbackApplied[0]) {
                    currentMiner.knockback(1.6D, -1.0D, 0.0D);
                    knockbackApplied[0] = true;
                }
                helper.assertTrue(helper.getLevel().noCollision(currentMiner),
                        "Restoration must not entomb or suffocate Miner? after combat knockback");
                if (!targetMovedClear[0] && currentMiner.confirmedTargetHits() >= 2) {
                    Vec3 awayFromTunnel = helper.absoluteVec(new Vec3(1.5D, 1.0D, 2.0D));
                    player.moveTo(awayFromTunnel.x, awayFromTunnel.y, awayFromTunnel.z, 0.0F, 0.0F);
                    targetMovedClear[0] = true;
                }
            });
        }
        helper.runAtTickTime(1080, () -> {
            List<UncannyMinerEntity> miners = helper.getLevel().getEntitiesOfClass(
                    UncannyMinerEntity.class, player.getBoundingBox().inflate(40.0D), Entity::isAlive);
            helper.assertTrue(miners.size() == 1
                            && miners.getFirst().getUUID().equals(minerId)
                            && miners.getFirst().stage() == UncannyMinerEntity.Stage.HUNTING,
                    "The same Miner? must physically leave the horizontal tunnel and hunt");
            helper.assertTrue(huntingObserved[0]
                            && emergenceReloaded[0]
                            && knockbackApplied[0]
                            && targetMovedClear[0]
                            && miners.getFirst().confirmedTargetHits() >= 2
                            && player.getHealth() < startingHealth,
                    "Wall emergence must survive knockback and end in repeated real melee attacks");
            for (int x = 5; x <= 15; x++) {
                helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 1, 8));
                helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 2, 8));
            }
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING)
                    .set(previousMobGriefing, helper.getLevel().getServer());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "miner_debug_retest")
    public static void minerDeveloperTunnelCanReplaceAnActiveAttempt(GameTestHelper helper) {
        fillFloor(helper);
        for (int x = 5; x <= 15; x++) {
            helper.setBlock(x, 1, 8, Blocks.STONE);
            helper.setBlock(x, 2, 8, Blocks.STONE);
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 position = helper.absoluteVec(new Vec3(1.5D, 1.0D, 8.5D));
        player.moveTo(position.x, position.y, position.z, 0.0F, 0.0F);
        player.setInvulnerable(true);
        boolean previousMobGriefing = helper.getLevel().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING)
                .set(true, helper.getLevel().getServer());
        helper.assertTrue(UncannyMinerSystem.spawnDebugTunnel(player),
                "The first developer Miner? tunnel must start");
        UUID[] firstId = new UUID[1];
        helper.runAtTickTime(4, () -> {
            List<UncannyMinerEntity> active = helper.getLevel().getEntitiesOfClass(
                    UncannyMinerEntity.class,
                    player.getBoundingBox().inflate(40.0D),
                    miner -> miner.isAlive() && miner.targets(player));
            helper.assertTrue(active.size() == 1, "The first QA attempt must own one Miner?");
            firstId[0] = active.getFirst().getUUID();
        });
        helper.runAtTickTime(45, () -> helper.assertTrue(
                UncannyMinerSystem.spawnDebugTunnel(player),
                "Re-running the dev action must clean and replace the active tunnel immediately"));
        helper.runAtTickTime(52, () -> {
            List<UncannyMinerEntity> active = helper.getLevel().getEntitiesOfClass(
                    UncannyMinerEntity.class,
                    player.getBoundingBox().inflate(40.0D),
                    miner -> miner.isAlive() && miner.targets(player));
            helper.assertTrue(active.size() == 1 && !active.getFirst().getUUID().equals(firstId[0]),
                    "The second QA attempt must be a single fresh Miner?, never a refused or duplicate spawn");
            UncannyMinerSystem.abortForTarget(player);
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING)
                    .set(previousMobGriefing, helper.getLevel().getServer());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void minerRefusesToStartWhenMobGriefingIsDisabled(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        boolean previous = helper.getLevel().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING)
                .set(false, helper.getLevel().getServer());
        helper.assertTrue(!UncannyMinerSystem.spawnDebugTunnel(player),
                "Miner? must refuse all world mutation while mobGriefing is false");
        helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING)
                .set(previous, helper.getLevel().getServer());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void devourerKeepsItsFixedStatsAndRemainsProjectileVulnerable(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 playerPos = helper.absoluteVec(new Vec3(8.5D, 1.0D, 8.5D));
        player.moveTo(playerPos.x, playerPos.y, playerPos.z, 0.0F, 0.0F);
        UncannyDevourerEntity devourer = UncannyEntityRegistry.UNCANNY_DEVOURER.get().create(helper.getLevel());
        helper.assertTrue(devourer != null, "Devourer? must be constructible on a dedicated server");
        Vec3 devourerPos = helper.absoluteVec(new Vec3(8.5D, 1.0D, 8.5D));
        devourer.moveTo(devourerPos.x, devourerPos.y, devourerPos.z, 0.0F, 0.0F);
        devourer.initializeFor(player);
        player.moveTo(devourerPos.x + 1.9D, devourerPos.y, devourerPos.z, 0.0F, 0.0F);
        helper.assertTrue(devourer.isWithinCaptureReach(player),
                "Devourer?'s arms must reach about a block beyond its body");
        player.moveTo(devourerPos.x + 2.3D, devourerPos.y, devourerPos.z, 0.0F, 0.0F);
        helper.assertTrue(!devourer.isWithinCaptureReach(player),
                "Devourer?'s reach must stay an arm's length rather than becoming an aura");
        player.moveTo(playerPos.x, playerPos.y, playerPos.z, 0.0F, 0.0F);
        helper.assertTrue(helper.getLevel().addFreshEntity(devourer), "Devourer? must enter the level");
        // User, 2026-10-08: far harder to put down, scaled to the best weapon the player carries.
        helper.assertTrue(devourer.getMaxHealth() >= com.eotv.echoofthevoid.event.special.CombatParityRules.DEVOURER_MIN_HEALTH,
                "Devourer? must be hard to put down: " + devourer.getMaxHealth());
        helper.assertTrue(devourer.getArmorValue() == 4, "Devourer? must have exactly four armor points");
        float before = devourer.getHealth();
        Arrow arrow = new Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        helper.assertTrue(devourer.hurt(helper.getLevel().damageSources().arrow(arrow, player), 2.0F),
                "Devourer? must remain vulnerable to projectiles");
        helper.assertTrue(devourer.getHealth() < before, "Projectile damage must reduce Devourer?'s health");
        devourer.discard();
        if (!elsewhereUnavailableOnVanillaGameTestServer(helper)) {
            UncannyDevCatalog.Entry entry = UncannyDevCatalog.byId("entity_devourer_spawn");
            helper.assertTrue(entry != null, "The visible Devourer? menu action must exist");
            UncannyDevActionExecutor.ExecutionResult result =
                    UncannyDevActionExecutor.executeDetailed(player, entry, 4);
            helper.assertTrue(result.success(),
                    "The Devourer? menu action must keep trying checked candidates: " + result.message());
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                            UncannyDevourerEntity.class,
                            helper.getBounds().inflate(32.0D),
                            Entity::isAlive).size() == 1,
                    "A successful developer action must add exactly one Devourer?");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void devourerRetreatWaitsForUnreachabilityAndCompleteLossOfSight(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer first = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(first.connection.getConnection());
        Vec3 firstPos = helper.absoluteVec(new Vec3(8.5D, 1.0D, 8.5D));
        first.moveTo(firstPos.x, firstPos.y, firstPos.z, 0.0F, 0.0F);
        ServerPlayer second = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(second.connection.getConnection());
        Vec3 secondPos = helper.absoluteVec(new Vec3(9.5D, 1.0D, 8.5D));
        second.moveTo(secondPos.x, secondPos.y, secondPos.z, 0.0F, 0.0F);
        UncannyDevourerEntity devourer = UncannyEntityRegistry.UNCANNY_DEVOURER.get().create(helper.getLevel());
        helper.assertTrue(devourer != null, "Devourer? must be constructible");
        Vec3 start = helper.absoluteVec(new Vec3(8.5D, 1.0D, 11.5D));
        devourer.moveTo(start.x, start.y, start.z, 180.0F, 0.0F);
        devourer.initializeFor(first);
        for (int y = 1; y <= 3; y++) {
            for (int x = 7; x <= 10; x++) {
                helper.setBlock(x, y, 10, Blocks.GLASS);
                helper.setBlock(x, y, 13, Blocks.GLASS);
            }
            for (int z = 11; z <= 12; z++) {
                helper.setBlock(7, y, z, Blocks.GLASS);
                helper.setBlock(10, y, z, Blocks.GLASS);
            }
        }
        CompoundTag state = new CompoundTag();
        devourer.saveWithoutId(state);
        state.putInt("DevourerLifetime", UncannyDevourerEntity.MAX_LIFETIME_TICKS - 1);
        markAlreadyEmerged(helper, state);
        state.putInt("DevourerNextPulse", UncannyDevourerEntity.MAX_LIFETIME_TICKS + 100);
        state.putBoolean("DevourerArrivalSound", true);
        devourer.load(state);
        helper.assertTrue(helper.getLevel().addFreshEntity(devourer),
                "The expiring witnessed Devourer? must enter the ServerLevel");

        helper.runAtTickTime(108, () -> {
            helper.assertTrue(devourer.isAlive() && !devourer.isSinking(),
                    "An unreachable Devourer? must remain while players still observe it through glass");
            first.setYRot(180.0F);
            first.setYHeadRot(180.0F);
        });
        helper.runAtTickTime(114, () -> {
            helper.assertTrue(devourer.isAlive() && !devourer.isSinking(),
                    "One remaining observer must keep the multiplayer Devourer? present");
            second.setYRot(180.0F);
            second.setYHeadRot(180.0F);
        });
        helper.runAtTickTime(120, () -> {
            helper.assertTrue(devourer.isAlive() && devourer.isSinking() && devourer.getY() < start.y,
                    "Devourer? may sink only after no player is reachable and every observer looks away");
            CompoundTag saved = new CompoundTag();
            devourer.saveWithoutId(saved);
            helper.assertTrue(saved.getBoolean("DevourerSinking")
                            && saved.getLong("DevourerSinkDeadline") > helper.getLevel().getGameTime(),
                    "The absolute disappearance deadline must survive chunk unload or restart");
        });
        helper.runAtTickTime(162, () -> {
            helper.assertTrue(devourer.isRemoved(),
                    "The unobserved sink must finish after its bounded forty-tick animation");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void devourerSeizesAndHoldsItsVictimBeforeTheTrialBegins(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 start = helper.absoluteVec(new Vec3(8.5D, 1.0D, 8.5D));
        player.moveTo(start.x + 1.8D, start.y, start.z, 90.0F, 0.0F);
        // Mock players report creative abilities; the Devourer? only takes survival players.
        player.getAbilities().invulnerable = false;
        UncannyDevourerEntity devourer = UncannyEntityRegistry.UNCANNY_DEVOURER.get().create(helper.getLevel());
        helper.assertTrue(devourer != null, "Devourer? must be constructible");
        devourer.moveTo(start.x, start.y, start.z, 270.0F, 0.0F);
        devourer.initializeFor(player);
        CompoundTag emerged = new CompoundTag();
        devourer.saveWithoutId(emerged);
        markAlreadyEmerged(helper, emerged);
        devourer.load(emerged);
        helper.assertTrue(helper.getLevel().addFreshEntity(devourer), "Devourer? must enter the level");
        double before = player.distanceTo(devourer);

        helper.runAtTickTime(6, () -> {
            helper.assertTrue(devourer.isSeizing(),
                    "A victim within arm's reach must be seized rather than vanish on the same tick");
            helper.assertTrue(devourer.captureCount() == 0,
                    "The trial must not begin before the capture animation has played");
            helper.assertTrue(devourer.mouthOpenAmount() >= 0.99F, "The chest portal must be wide open");
            helper.assertTrue(player.distanceTo(devourer) <= before + 0.05D,
                    "A seized victim is drawn towards the portal, never thrown away from it");
            devourer.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void devourerStaysHarmlessWhileItsBodyEmergesFromThePortal(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 playerPos = helper.absoluteVec(new Vec3(8.5D, 1.0D, 4.5D));
        player.moveTo(playerPos.x, playerPos.y, playerPos.z, 0.0F, 0.0F);
        // Mock players report creative abilities; the Devourer? only takes survival players.
        player.getAbilities().invulnerable = false;
        UncannyDevourerEntity devourer = UncannyEntityRegistry.UNCANNY_DEVOURER.get().create(helper.getLevel());
        helper.assertTrue(devourer != null, "Devourer? must be constructible");
        Vec3 start = helper.absoluteVec(new Vec3(8.5D, 1.0D, 12.5D));
        devourer.moveTo(start.x, start.y, start.z, 180.0F, 0.0F);
        devourer.initializeFor(player);
        helper.assertTrue(helper.getLevel().addFreshEntity(devourer), "Devourer? must enter the level");

        helper.runAtTickTime(UncannyDevourerEntity.EMERGE_TICKS - 8, () -> {
            helper.assertTrue(devourer.getTarget() == null && devourer.getNavigation().isDone(),
                    "While the emerge animation plays the Devourer? must neither target nor walk");
            helper.assertTrue(devourer.distanceToSqr(start) < 0.01D,
                    "An emerging Devourer? must stay inside its portal");
            CompoundTag saved = new CompoundTag();
            devourer.saveWithoutId(saved);
            helper.assertTrue(saved.contains("DevourerSpawnGameTime"),
                    "The emerge clock must persist so a reload cannot replay or skip it");
        });
        helper.runAtTickTime(UncannyDevourerEntity.EMERGE_TICKS + 8, () -> {
            helper.assertTrue(devourer.getTarget() == player,
                    "Once fully out of its portal the Devourer? must pursue its reachable target");
            devourer.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void aDevourerKilledMidSeizeReleasesItsVictim(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 start = helper.absoluteVec(new Vec3(8.5D, 1.0D, 8.5D));
        player.moveTo(start.x + 1.8D, start.y, start.z, 90.0F, 0.0F);
        // Mock players report creative abilities; the Devourer? only takes survival players.
        player.getAbilities().invulnerable = false;
        UncannyDevourerEntity devourer = UncannyEntityRegistry.UNCANNY_DEVOURER.get().create(helper.getLevel());
        helper.assertTrue(devourer != null, "Devourer? must be constructible");
        devourer.moveTo(start.x, start.y, start.z, 270.0F, 0.0F);
        devourer.initializeFor(player);
        CompoundTag emerged = new CompoundTag();
        devourer.saveWithoutId(emerged);
        markAlreadyEmerged(helper, emerged);
        devourer.load(emerged);
        helper.assertTrue(helper.getLevel().addFreshEntity(devourer), "Devourer? must enter the level");
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(devourer.isSeizing(), "The setup must start a seize");
            devourer.hurt(helper.getLevel().damageSources().playerAttack(player), 10_000.0F);
        });
        // Two ticks into the death animation: well before the seize could have finished on its own.
        helper.runAtTickTime(6, () -> {
            // Regression (2026-10-08): aiStep runs through the death animation and finished the seize.
            helper.assertTrue(devourer.isDeadOrDying() && !devourer.isSeizing() && devourer.captureCount() == 0,
                    "A dying Devourer? must let go at once instead of capturing");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void devourerIgnoresACreativePlayer(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 start = helper.absoluteVec(new Vec3(8.5D, 1.0D, 8.5D));
        player.moveTo(start.x + 1.8D, start.y, start.z, 90.0F, 0.0F);
        player.getAbilities().invulnerable = true;
        UncannyDevourerEntity devourer = UncannyEntityRegistry.UNCANNY_DEVOURER.get().create(helper.getLevel());
        helper.assertTrue(devourer != null, "Devourer? must be constructible");
        devourer.moveTo(start.x, start.y, start.z, 270.0F, 0.0F);
        devourer.initializeFor(player);
        CompoundTag emerged = new CompoundTag();
        devourer.saveWithoutId(emerged);
        markAlreadyEmerged(helper, emerged);
        devourer.load(emerged);
        helper.assertTrue(helper.getLevel().addFreshEntity(devourer), "Devourer? must enter the level");
        helper.runAtTickTime(10, () -> {
            // User, 2026-10-09: a creative player was pulled into the trial.
            helper.assertTrue(!devourer.isSeizing() && devourer.captureCount() == 0,
                    "A creative player within arm's reach must never be seized");
            devourer.discard();
            helper.succeed();
        });
    }

    /** Old saves and long-running scenarios describe a Devourer whose emergence ended long ago. */
    private static void markAlreadyEmerged(GameTestHelper helper, CompoundTag state) {
        state.putLong("DevourerSpawnGameTime",
                helper.getLevel().getGameTime() - UncannyDevourerEntity.EMERGE_TICKS - 1L);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 90)
    public static void devourerForcesItsRetreatAfterThreeMinutesWithoutAnEarlierOpportunity(
            GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 playerPos = helper.absoluteVec(new Vec3(8.5D, 1.0D, 4.5D));
        player.moveTo(playerPos.x, playerPos.y, playerPos.z, 0.0F, 0.0F);
        UncannyDevourerEntity devourer = UncannyEntityRegistry.UNCANNY_DEVOURER.get().create(helper.getLevel());
        helper.assertTrue(devourer != null, "Devourer? must be constructible");
        Vec3 start = helper.absoluteVec(new Vec3(8.5D, 1.0D, 12.5D));
        devourer.moveTo(start.x, start.y, start.z, 180.0F, 0.0F);
        devourer.initializeFor(player);
        CompoundTag state = new CompoundTag();
        devourer.saveWithoutId(state);
        state.putLong("DevourerForcedRetreatDeadline", helper.getLevel().getGameTime() + 20L);
        markAlreadyEmerged(helper, state);
        state.putBoolean("DevourerArrivalSound", true);
        devourer.load(state);
        helper.assertTrue(helper.getLevel().addFreshEntity(devourer),
                "The deadline-controlled Devourer? must enter the ServerLevel");

        helper.runAtTickTime(10, () -> {
            var path = devourer.getNavigation().createPath(player, 1);
            helper.assertTrue(devourer.isAlive() && !devourer.isSinking(),
                    "Devourer? must remain before the exact three-minute deadline");
            helper.assertTrue(path != null && path.canReach(),
                    "The setup must prove the Devourer had a genuine opportunity before it was lost");
            CompoundTag snapshot = new CompoundTag();
            devourer.saveWithoutId(snapshot);
            long persistedDeadline = snapshot.getLong("DevourerForcedRetreatDeadline");
            UncannyDevourerEntity reloaded = UncannyEntityRegistry.UNCANNY_DEVOURER.get()
                    .create(helper.getLevel());
            helper.assertTrue(reloaded != null, "Devourer? must remain constructible for an NBT reload");
            reloaded.load(snapshot);
            CompoundTag roundTrip = new CompoundTag();
            reloaded.saveWithoutId(roundTrip);
            helper.assertTrue(roundTrip.getLong("DevourerForcedRetreatDeadline") == persistedDeadline,
                    "The absolute forced-retreat deadline must survive an entity NBT reload");
            reloaded.discard();
            player.moveTo(start.x + 40.0D, start.y, start.z, 180.0F, 0.0F);
        });
        helper.runAtTickTime(25, () -> {
            helper.assertTrue(devourer.isAlive() && devourer.isSinking() && devourer.getY() < start.y,
                    "The three-minute cap must sink a Devourer that no longer has any reachable target");
            CompoundTag saved = new CompoundTag();
            devourer.saveWithoutId(saved);
            helper.assertTrue(saved.getLong("DevourerForcedRetreatDeadline")
                            <= helper.getLevel().getGameTime(),
                    "The forced-retreat deadline must remain absolute and persisted");
        });
        helper.runAtTickTime(66, () -> {
            helper.assertTrue(devourer.isRemoved(),
                    "The forced retreat must finish through the normal forty-tick sink");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void arenaSessionSerializationPreservesRecoveryCriticalState(GameTestHelper helper) {
        UUID player = UUID.randomUUID();
        UUID source = UUID.randomUUID();
        CompoundTag inventory = new CompoundTag();
        inventory.putInt("Sentinel", 73);
        DevourerArenaSession original = new DevourerArenaSession(
                player, source, "minecraft:the_nether", 1.25D, 64.0D, -8.5D,
                35.0F, -12.0F, 7L, inventory, 50_000L);
        original.setRemainingTicks(417L);
        original.setSpawnedPursuers(DevourerArenaRules.MAX_PURSUERS);
        original.setLastKnownAlivePursuers(14);
        original.recordReplacement();
        original.recordReplacement();
        original.markLossDetected(123L);
        original.scheduleNextCoverage(160L);
        original.rememberPlacedBlock(123L, "minecraft:cobblestone");
        original.rememberPlacedBlock(456L, "minecraft:oak_planks");
        original.markPlacementRemovedByPursuer(456L);
        original.rememberBrokenArenaBlock(789L);
        original.setStatus(DevourerArenaSession.Status.AWAITING_RESPAWN);

        DevourerArenaSession restored = DevourerArenaSession.load(original.save());
        helper.assertTrue(restored != null, "A valid arena session must deserialize");
        helper.assertTrue(player.equals(restored.playerId()) && source.equals(restored.sourceDevourerId()),
                "Session ownership and Devourer source must survive reload");
        helper.assertTrue(restored.remainingTicks() == 417L
                        && restored.spawnedPursuers() == DevourerArenaRules.MAX_PURSUERS,
                "Countdown and the complete bounded wave progress must survive reload");
        helper.assertTrue(restored.lastKnownAlivePursuers() == 14
                        && restored.replacementsUsed() == 2
                        && restored.lossDetectedElapsedTick() == 123L
                        && restored.nextCoverageElapsedTick()
                                == 160L + DevourerArenaRules.COVERAGE_REEVALUATION_TICKS,
                "Sector rebalance budgets and delays must survive reload");
        helper.assertTrue(restored.entryInventory().getInt("Sentinel") == 73,
                "The exact entry inventory snapshot must survive reload");
        helper.assertTrue(restored.placedBlocks().size() == 2
                        && restored.placedBlocks().get(456L).removedByPursuer(),
                "Placed and pursuer-removed blocks must survive reload");
        helper.assertTrue(restored.brokenArenaBlocks().contains(789L)
                        && restored.status() == DevourerArenaSession.Status.AWAITING_RESPAWN,
                "Terrain recovery and death state must survive reload");

        CompoundTag minimal = new CompoundTag();
        minimal.putUUID("Player", UUID.randomUUID());
        DevourerArenaSession legacy = DevourerArenaSession.load(minimal);
        helper.assertTrue(legacy != null
                        && legacy.remainingTicks() == DevourerArenaSession.TRIAL_TICKS
                        && legacy.status() == DevourerArenaSession.Status.ACTIVE,
                "Missing additive fields in an old world must load with safe defaults");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void devourerContactStartsAndCountdownCompletesARealArenaSession(GameTestHelper helper) {
        if (elsewhereUnavailableOnVanillaGameTestServer(helper)) {
            helper.succeed();
            return;
        }
        helper.assertTrue(helper.getLevel().getServer().getLevel(UncannyDimensions.ELSEWHERE) != null,
                "The elsewhere dimension must load on a normal dedicated server");
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 origin = helper.absoluteVec(new Vec3(8.5D, 1.0D, 8.5D));
        player.moveTo(origin.x, origin.y, origin.z, 0.0F, 0.0F);
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));

        UncannyDevourerEntity devourer = UncannyEntityRegistry.UNCANNY_DEVOURER.get().create(helper.getLevel());
        helper.assertTrue(devourer != null, "Devourer? must be constructible for contact capture");
        devourer.moveTo(origin.x, origin.y, origin.z, 0.0F, 0.0F);
        devourer.initializeFor(player);
        CompoundTag emerged = new CompoundTag();
        devourer.saveWithoutId(emerged);
        markAlreadyEmerged(helper, emerged);
        devourer.load(emerged);
        helper.assertTrue(helper.getLevel().addFreshEntity(devourer),
                "Devourer? must enter the origin level before contact");

        helper.runAtTickTime(UncannyDevourerEntity.SEIZE_TICKS + 8, () -> {
            UncannyWorldState state = UncannyWorldState.get(helper.getLevel().getServer());
            DevourerArenaSession session = state.getDevourerArenaSession(player.getUUID());
            helper.assertTrue(session != null && UncannyDimensions.isElsewhere(player.level()),
                    "Intersecting hitboxes must start and persist a real Elsewhere session");
            helper.assertTrue(devourer.captureCount() == 1,
                    "One Devourer? must capture the same player at most once");
            helper.assertTrue(session.remainingTicks() == DevourerArenaSession.TRIAL_TICKS,
                    "The countdown must wait for the physically loaded initial pursuer group");
        });
        // Cross-dimension player tickets become entity-ticking asynchronously. The arena must
        // wait for that real state, create at most two pursuers per tick, and only then begin the
        // countdown. A persisted counter alone is deliberately not accepted by this assertion.
        helper.runAtTickTime(100, () -> {
            UncannyWorldState state = UncannyWorldState.get(helper.getLevel().getServer());
            DevourerArenaSession session = state.getDevourerArenaSession(player.getUUID());
            helper.assertTrue(session != null, "The arena session must remain active during inspection");
            List<UncannyArenaPursuerEntity> initial = new ArrayList<>();
            for (Entity entity : player.serverLevel().getAllEntities()) {
                if (entity instanceof UncannyArenaPursuerEntity pursuer
                        && pursuer.cellIndex() == session.cellIndex()) {
                    initial.add(pursuer);
                }
            }
            helper.assertTrue(initial.size() == DevourerArenaRules.INITIAL_PURSUERS,
                    "All initial arena pursuers must be physically present; found " + initial.size());
            helper.assertTrue(initial.stream().allMatch(Entity::isSilent),
                    "Every arena pursuer must remain completely silent");
            helper.assertTrue(initial.stream().noneMatch(Entity::isCurrentlyGlowing),
                    "Arena pursuers must surface from the fog, never through a Glowing outline");
            helper.assertTrue(initial.stream().filter(pursuer -> pursuer.appearance().climbs()).count()
                            >= DevourerArenaRules.MIN_CLIMBING_PURSUERS,
                    "At least two climbing Spiders must answer a pillar from the first second");
            helper.assertTrue(EnumSet.copyOf(initial.stream()
                            .map(UncannyArenaPursuerEntity::appearance)
                            .toList()).size() == DevourerArenaRules.INITIAL_PURSUERS
                                    - DevourerArenaRules.MIN_CLIMBING_PURSUERS + 1,
                    "Apart from the two Spiders the initial group must show distinct familiar silhouettes");
            helper.assertTrue(initial.stream().allMatch(pursuer ->
                            Math.abs(pursuer.getBbWidth() - pursuer.appearance().width()) < 1.0E-4F
                                    && Math.abs(pursuer.getBbHeight() - pursuer.appearance().height()) < 1.0E-4F),
                    "Each pursuer must use the hitbox of the creature it shows");
            helper.assertTrue(initial.stream().allMatch(pursuer -> pursuer.appearance().climbs()
                            == (pursuer.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.WallClimberNavigation)),
                    "Only the Spider silhouette may plan wall-climbing paths");
            session.setRemainingTicks(2L);
            state.markDevourerArenaSessionsDirty();
        });
        helper.runAtTickTime(112, () -> {
            UncannyWorldState state = UncannyWorldState.get(helper.getLevel().getServer());
            helper.assertTrue(state.getDevourerArenaSession(player.getUUID()) == null,
                    "A completed countdown must remove its persistent session");
            helper.assertTrue(!UncannyDimensions.isElsewhere(player.level()),
                    "A surviving player must return to the origin dimension");
            helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 3,
                    "A successful trial must preserve the player's ordinary inventory");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void arenaDeathRestoresExactInventoryWithBothKeepInventoryModes(GameTestHelper helper) {
        if (elsewhereUnavailableOnVanillaGameTestServer(helper)) {
            helper.succeed();
            return;
        }
        fillFloor(helper);
        boolean previousKeepInventory = helper.getLevel().getGameRules()
                .getBoolean(GameRules.RULE_KEEPINVENTORY);
        helper.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY)
                .set(false, helper.getLevel().getServer());
        ServerPlayer[] current = {helper.makeMockServerPlayerInLevel()};
        NetworkRegistry.configureMockConnection(current[0].connection.getConnection());
        Vec3 origin = helper.absoluteVec(new Vec3(8.5D, 1.0D, 8.5D));
        current[0].moveTo(origin.x, origin.y, origin.z, 0.0F, 0.0F);
        current[0].getInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
        helper.assertTrue(DevourerArenaSystem.enterDebugArena(current[0]),
                "The non-keepInventory death trial must start");

        helper.runAtTickTime(5, () -> {
            current[0].getInventory().setItem(0, new ItemStack(Items.GOLD_INGOT, 7));
            killArenaPlayer(current[0]);
            helper.assertTrue(!current[0].isAlive(),
                    "The arena must preserve a genuine, uncancelled player death");
        });
        helper.runAtTickTime(7, () -> {
            assertAwaitingRespawnWithoutDrops(helper, current[0]);
            helper.assertTrue(DevourerArenaSystem.abandonSession(current[0]),
                    "An administrative cleanup requested on the death screen must be accepted");
            DevourerArenaSession pending = UncannyWorldState.get(helper.getLevel().getServer())
                    .getDevourerArenaSession(current[0].getUUID());
            helper.assertTrue(pending != null
                            && pending.status() == DevourerArenaSession.Status.RETURN_PENDING
                            && !current[0].isAlive(),
                    "Cleanup must wait for a real respawn instead of teleporting or restoring a dead player");
            current[0] = helper.getLevel().getServer().getPlayerList().respawn(
                    current[0], false, Entity.RemovalReason.KILLED);
            helper.assertTrue(current[0] != null && !UncannyDimensions.isElsewhere(current[0].level()),
                    "Respawn after a failed trial must return the replacement player safely");
            helper.assertTrue(current[0].getInventory().countItem(Items.DIAMOND) == 3
                            && current[0].getInventory().countItem(Items.GOLD_INGOT) == 0,
                    "keepInventory=false must restore exactly the entry inventory, not the death inventory");
        });
        helper.runAtTickTime(12, () -> {
            helper.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY)
                    .set(true, helper.getLevel().getServer());
            current[0].getInventory().clearContent();
            current[0].getInventory().setItem(0, new ItemStack(Items.EMERALD, 4));
            helper.assertTrue(DevourerArenaSystem.enterDebugArena(current[0]),
                    "The keepInventory death trial must start independently");
        });
        helper.runAtTickTime(17, () -> {
            current[0].getInventory().setItem(0, new ItemStack(Items.LAPIS_LAZULI, 9));
            killArenaPlayer(current[0]);
            helper.assertTrue(!current[0].isAlive(),
                    "keepInventory must not prevent the genuine arena death");
        });
        helper.runAtTickTime(19, () -> {
            assertAwaitingRespawnWithoutDrops(helper, current[0]);
            current[0] = helper.getLevel().getServer().getPlayerList().respawn(
                    current[0], false, Entity.RemovalReason.KILLED);
            helper.assertTrue(current[0] != null && !UncannyDimensions.isElsewhere(current[0].level()),
                    "The second failed trial must also return safely");
            helper.assertTrue(current[0].getInventory().countItem(Items.EMERALD) == 4
                            && current[0].getInventory().countItem(Items.LAPIS_LAZULI) == 0,
                    "keepInventory=true must still restore exactly the entry snapshot without duplication");
            helper.assertTrue(UncannyWorldState.get(helper.getLevel().getServer())
                            .getDevourerArenaSession(current[0].getUUID()) == null,
                    "A respawned player must leave no arena session behind");
            helper.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY)
                    .set(previousKeepInventory, helper.getLevel().getServer());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void pursuerScratchesOnlySessionPlacementsAndCleanupRefundsThem(GameTestHelper helper) {
        if (elsewhereUnavailableOnVanillaGameTestServer(helper)) {
            helper.succeed();
            return;
        }
        fillFloor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        player.setInvulnerable(true);
        Vec3 origin = helper.absoluteVec(new Vec3(8.5D, 1.0D, 8.5D));
        player.moveTo(origin.x, origin.y, origin.z, 0.0F, 0.0F);
        helper.assertTrue(DevourerArenaSystem.enterDebugArena(player),
                "A placement-scratch trial must start in Elsewhere");
        UncannyWorldState state = UncannyWorldState.get(helper.getLevel().getServer());
        DevourerArenaSession session = state.getDevourerArenaSession(player.getUUID());
        helper.assertTrue(session != null, "The scratch trial must own a persisted session");
        var elsewhere = helper.getLevel().getServer().getLevel(UncannyDimensions.ELSEWHERE);
        helper.assertTrue(elsewhere != null, "Elsewhere must remain available during the scratch trial");
        int centerX = com.eotv.echoofthevoid.event.special.DevourerArenaRules.cellCenterX(session.cellIndex());

        for (UncannyArenaPursuerEntity initial : elsewhere.getEntitiesOfClass(
                UncannyArenaPursuerEntity.class,
                new AABB(centerX - 64.0D, 0.0D, -64.0D, centerX + 64.0D, 64.0D, 64.0D))) {
            initial.discard();
        }
        session.setSpawnedPursuers(DevourerArenaRules.INITIAL_PURSUERS - 2);
        player.moveTo(centerX + 5.5D, 13.0D, 0.5D, 90.0F, 0.0F);

        BlockPos lowerPlacement = new BlockPos(centerX + 1, 13, 0);
        BlockPos upperPlacement = lowerPlacement.above();
        elsewhere.setBlock(lowerPlacement, Blocks.COBBLESTONE.defaultBlockState(), 3);
        elsewhere.setBlock(upperPlacement, Blocks.COBBLESTONE.defaultBlockState(), 3);
        session.rememberPlacedBlock(lowerPlacement.asLong(), "minecraft:cobblestone");
        session.rememberPlacedBlock(upperPlacement.asLong(), "minecraft:cobblestone");
        for (int x = centerX; x <= centerX + 6; x++) {
            elsewhere.setBlock(new BlockPos(x, 15, 0), Blocks.BEDROCK.defaultBlockState(), 3);
            for (int y = 13; y <= 14; y++) {
                elsewhere.setBlock(new BlockPos(x, y, -1), Blocks.BEDROCK.defaultBlockState(), 3);
                elsewhere.setBlock(new BlockPos(x, y, 1), Blocks.BEDROCK.defaultBlockState(), 3);
            }
        }
        BlockPos untrackedControl = new BlockPos(centerX, 15, 0);

        UncannyArenaPursuerEntity pursuer = UncannyEntityRegistry.UNCANNY_ARENA_PURSUER.get().create(elsewhere);
        helper.assertTrue(pursuer != null, "An arena pursuer must be constructible");
        pursuer.moveTo(centerX + 0.5D, 13.0D, 0.5D, 0.0F, 0.0F);
        pursuer.initializeFor(player, session.cellIndex());
        helper.assertTrue(elsewhere.addFreshEntity(pursuer), "The controlled pursuer must enter its arena cell");
        state.markDevourerArenaSessionsDirty();

        helper.runAtTickTime(35, () -> helper.assertTrue(
                elsewhere.getBlockState(lowerPlacement).is(Blocks.COBBLESTONE),
                "The three-second scratch telegraph must not remove a placement early"));
        helper.runAtTickTime(100, () -> {
            boolean lowerRemoved = elsewhere.getBlockState(lowerPlacement).isAir();
            boolean upperRemoved = elsewhere.getBlockState(upperPlacement).isAir();
            helper.assertTrue(lowerRemoved || upperRemoved,
                    "A fully blocking tracked placement must be removable after its telegraph");
            helper.assertTrue(elsewhere.getBlockState(untrackedControl).is(Blocks.BEDROCK),
                    "A pursuer must never remove an untracked arena or control block");
            helper.assertTrue(session.placedBlocks().values().stream()
                            .anyMatch(DevourerArenaSession.PlacedBlock::removedByPursuer),
                    "The removed placement must be marked for one-time restitution");
            helper.assertTrue(DevourerArenaSystem.abandonSession(player),
                    "The controlled cleanup route must finish the scratch trial");
            helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 2,
                    "Cleanup must refund both the remaining and pursuer-removed placements exactly once");
            player.setInvulnerable(false);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 50)
    public static void twoArenaSessionsAreIsolatedPersistedAndCleanlyAbandoned(GameTestHelper helper) {
        if (elsewhereUnavailableOnVanillaGameTestServer(helper)) {
            // Vanilla's GameTestServer deliberately replaces the datapack LEVEL_STEM registry
            // with the flat preset. This same test is also run through /test on a normal server,
            // where a missing Elsewhere dimension remains a hard failure.
            helper.succeed();
            return;
        }
        helper.assertTrue(helper.getLevel().getServer().getLevel(UncannyDimensions.ELSEWHERE) != null,
                "The elsewhere dimension must load on a normal dedicated server");
        fillFloor(helper);
        ServerPlayer first = helper.makeMockServerPlayerInLevel();
        ServerPlayer second = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(first.connection.getConnection());
        NetworkRegistry.configureMockConnection(second.connection.getConnection());
        Vec3 firstOrigin = helper.absoluteVec(new Vec3(5.5D, 1.0D, 8.5D));
        Vec3 secondOrigin = helper.absoluteVec(new Vec3(10.5D, 1.0D, 8.5D));
        first.moveTo(firstOrigin.x, firstOrigin.y, firstOrigin.z, 0.0F, 0.0F);
        second.moveTo(secondOrigin.x, secondOrigin.y, secondOrigin.z, 0.0F, 0.0F);
        first.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
        second.getInventory().setItem(0, new ItemStack(Items.EMERALD, 4));

        helper.assertTrue(DevourerArenaSystem.enterDebugArena(first), "First arena session must start");
        helper.assertTrue(DevourerArenaSystem.enterDebugArena(second), "Second arena session must start");
        UncannyWorldState state = UncannyWorldState.get(helper.getLevel().getServer());
        DevourerArenaSession firstSession = state.getDevourerArenaSession(first.getUUID());
        DevourerArenaSession secondSession = state.getDevourerArenaSession(second.getUUID());
        helper.assertTrue(firstSession != null && secondSession != null,
                "Both sessions must be persisted before their teleports");
        helper.assertTrue(firstSession.cellIndex() != secondSession.cellIndex(),
                "Concurrent players must never share an arena cell");
        helper.assertTrue(Math.abs(first.getX() - second.getX()) >= 2048.0D,
                "Concurrent arena cells must remain at least 2048 blocks apart");
        helper.assertTrue(UncannyDimensions.isElsewhere(first.level()) && UncannyDimensions.isElsewhere(second.level()),
                "Both players must actually enter elsewhere");

        helper.assertTrue(DevourerArenaSystem.abandonSession(first), "First QA cleanup must succeed");
        helper.assertTrue(DevourerArenaSystem.abandonSession(second), "Second QA cleanup must succeed");
        helper.assertTrue(!UncannyDimensions.isElsewhere(first.level()) && !UncannyDimensions.isElsewhere(second.level()),
                "QA cleanup must return both players to their original dimension");
        helper.assertTrue(first.getInventory().countItem(Items.DIAMOND) == 3,
                "Successful cleanup must preserve the first inventory exactly");
        helper.assertTrue(second.getInventory().countItem(Items.EMERALD) == 4,
                "Successful cleanup must preserve the second inventory exactly");
        helper.assertTrue(state.getDevourerArenaSession(first.getUUID()) == null
                        && state.getDevourerArenaSession(second.getUUID()) == null,
                "Cleaned arena sessions must leave no persistent state");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void elsewhereKeepsAStandardEotvWorldStable(GameTestHelper helper) {
        if (elsewhereUnavailableOnVanillaGameTestServer(helper)) {
            helper.succeed();
            return;
        }
        var server = helper.getLevel().getServer();
        helper.assertTrue(server.getLevel(UncannyDimensions.ELSEWHERE) != null,
                "The stable lifecycle correction must not remove the Elsewhere dimension");
        helper.assertTrue(
                server.getWorldData().worldGenSettingsLifecycle()
                        == com.mojang.serialization.Lifecycle.stable(),
                "A standard EOTV world must not be classified as experimental");
        helper.succeed();
    }

    private static boolean elsewhereUnavailableOnVanillaGameTestServer(GameTestHelper helper) {
        return helper.getLevel().getServer() instanceof GameTestServer
                && helper.getLevel().getServer().getLevel(UncannyDimensions.ELSEWHERE) == null;
    }

    private static void assertAwaitingRespawnWithoutDrops(GameTestHelper helper, ServerPlayer player) {
        DevourerArenaSession session = UncannyWorldState.get(helper.getLevel().getServer())
                .getDevourerArenaSession(player.getUUID());
        helper.assertTrue(session != null && session.status() == DevourerArenaSession.Status.AWAITING_RESPAWN,
                "A genuine death must persist an awaiting-respawn session");
        var elsewhere = helper.getLevel().getServer().getLevel(UncannyDimensions.ELSEWHERE);
        helper.assertTrue(elsewhere != null && elsewhere.getEntitiesOfClass(
                        ItemEntity.class,
                        new AABB(player.blockPosition()).inflate(64.0D)).isEmpty(),
                "A failed arena trial must not leave recoverable item drops in Elsewhere");
    }

    private static void killArenaPlayer(ServerPlayer player) {
        player.setInvulnerable(false);
        player.getAbilities().invulnerable = false;
        player.setHealth(0.0F);
        player.die(player.damageSources().genericKill());
    }

    private static void assertMaterial(
            GameTestHelper helper,
            BlockPos relative,
            net.minecraft.world.level.block.Block block,
            UncannyMinerBlockPolicy.MaterialKind expected) {
        helper.setBlock(relative, block);
        BlockPos absolute = helper.absolutePos(relative);
        helper.assertTrue(UncannyMinerBlockPolicy.classify(helper.getLevel(), absolute) == expected,
                block + " must classify as " + expected);
    }

    private static boolean isCollisionFree(GameTestHelper helper, BlockPos absolute) {
        return helper.getLevel().getBlockState(absolute)
                .getCollisionShape(helper.getLevel(), absolute)
                .isEmpty();
    }

    private static void fillFloor(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
    }
}
