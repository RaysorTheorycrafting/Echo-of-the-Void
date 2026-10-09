package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.event.paranoia.nativeevent.WanderingTreeRules;
import com.eotv.echoofthevoid.event.paranoia.nativeevent.WanderingTreeSystem;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import com.eotv.echoofthevoid.state.WanderingTreeRecord;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * The wandering tree on real blocks: a small oak on a grass floor walks diagonally across the room
 * (away from the barrier walls, which would make it non-isolated), then the trap is checked.
 */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class WanderingTreeGameTests {
    private static final String TEMPLATE = "special_test_room";
    private static final BlockPos TRUNK = new BlockPos(2, 1, 2);
    private static final BlockPos FAR_BASE = new BlockPos(40, 1, 40);

    private WanderingTreeGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void treeWalksThreeTimesWholeAndOnlyOverGround(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper, Blocks.GRASS_BLOCK);
        plantOak(helper, TRUNK);
        WanderingTreeRecord record = WanderingTreeSystem.adopt(
                level, UUID.randomUUID(), helper.absolutePos(TRUNK.above(2)), Long.MAX_VALUE);
        helper.assertTrue(record != null, "An isolated oak on grass must be adoptable");
        helper.assertTrue(record.base().equals(helper.absolutePos(TRUNK)), "Adoption must resolve the trunk base");

        BlockPos baseCentre = helper.absolutePos(FAR_BASE);
        BlockPos previous = record.base();
        for (int move = 1; move <= WanderingTreeRules.TRAP_MOVE_THRESHOLD; move++) {
            WanderingTreeSystem.MoveOutcome outcome = WanderingTreeSystem.attemptMove(
                    level, current(level, record.id()), baseCentre, List.of(), true);
            helper.assertTrue(outcome == WanderingTreeSystem.MoveOutcome.MOVED, "Move " + move + " failed: " + outcome);
            WanderingTreeRecord now = current(level, record.id());
            BlockPos base = now.base();
            helper.assertTrue(now.moves() == move, "The record must count every move");
            helper.assertTrue(base.distSqr(baseCentre) < previous.distSqr(baseCentre), "Each move must approach the base");
            int step = Math.max(Math.abs(base.getX() - previous.getX()), Math.abs(base.getZ() - previous.getZ()));
            helper.assertTrue(step >= 2 && step <= WanderingTreeRules.MAX_STEP_BLOCKS, "Unexpected step " + step);
            helper.assertTrue(level.getBlockState(previous).isAir(), "The old trunk must be gone");
            helper.assertTrue(level.getBlockState(base.below()).is(Blocks.DIRT), "The new trunk must stand on dirt");
            assertOakAt(helper, level, base);
            previous = base;
        }
        helper.assertTrue(WanderingTreeRules.isArmed(current(level, record.id()).moves()), "Three moves must arm the tree");

        // The walk and the armed trap survive a save and reload of the world SavedData.
        WanderingTreeRecord saved = current(level, record.id());
        UncannyWorldState reloaded = UncannyWorldState.load(
                UncannyWorldState.get(level.getServer()).save(new CompoundTag(), level.registryAccess()),
                level.registryAccess());
        helper.assertTrue(reloaded.getWanderingTrees().contains(saved), "The tree record must persist unchanged");
        forget(level, record.id());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void cuttingAnArmedTreeMakesTheWholeTreeVanishWithoutDrops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper, Blocks.GRASS_BLOCK);
        plantOak(helper, TRUNK);
        WanderingTreeRecord record = WanderingTreeSystem.adopt(level, UUID.randomUUID(), helper.absolutePos(TRUNK), Long.MAX_VALUE);
        helper.assertTrue(record != null, "The oak must be adoptable");
        UncannyWorldState.get(level.getServer()).putWanderingTree(record.withMoves(WanderingTreeRules.TRAP_MOVE_THRESHOLD));

        ServerPlayer player = mockPlayer(helper);
        BlockPos cut = helper.absolutePos(TRUNK.above(1));
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, cut, level.getBlockState(cut), player);
        WanderingTreeSystem.onBlockBreak(event);

        helper.assertTrue(event.isCanceled(), "The cut must be replaced by the vanishing");
        helper.assertTrue(WanderingTreeSystem.isWarningPending(player.getUUID()), "The cutter must be told after the shriek");
        AABB area = new AABB(helper.absolutePos(TRUNK)).inflate(3.0D, 6.0D, 3.0D);
        for (BlockPos pos : BlockPos.betweenClosed(
                helper.absolutePos(TRUNK).offset(-2, 0, -2), helper.absolutePos(TRUNK).offset(2, 5, 2))) {
            BlockState state = level.getBlockState(pos);
            helper.assertTrue(!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES),
                    "Every log and leaf must disappear at once, found " + state + " at " + pos);
        }
        helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, area).isEmpty(), "The vanishing must drop nothing");
        helper.assertTrue(find(level, record.id()) == null, "A vanished tree is no longer tracked");
        player.setGameMode(GameType.SPECTATOR);
        helper.succeed();
    }

    /**
     * Regression (user playtest 2026-10-09): after five moves down a slope the armed tree touched other
     * blocks, the strict read refused it at the cut and the trap was silently forgotten.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void armedTreeThatIsNoLongerIsolatedStillVanishesAndWarnsTheCutter(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper, Blocks.GRASS_BLOCK);
        plantOak(helper, TRUNK);
        WanderingTreeRecord record = WanderingTreeSystem.adopt(level, UUID.randomUUID(), helper.absolutePos(TRUNK), Long.MAX_VALUE);
        helper.assertTrue(record != null, "The oak must be adoptable");
        UncannyWorldState.get(level.getServer()).putWanderingTree(record.withMoves(WanderingTreeRules.TRAP_MOVE_THRESHOLD + 2));
        // Something built against the canopy after it settled.
        BlockPos wall = TRUNK.offset(2, 3, 0);
        helper.setBlock(wall, Blocks.COBBLESTONE);
        helper.assertTrue(WanderingTreeSystem.adopt(level, UUID.randomUUID(), helper.absolutePos(TRUNK), Long.MAX_VALUE) == null,
                "Precondition: the tree is no longer strictly isolated");

        ServerPlayer player = mockPlayer(helper);
        BlockPos cut = helper.absolutePos(TRUNK.above(2));
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, cut, level.getBlockState(cut), player);
        WanderingTreeSystem.onBlockBreak(event);

        helper.assertTrue(event.isCanceled(), "An armed tree must vanish even when it is no longer isolated");
        for (BlockPos pos : BlockPos.betweenClosed(
                helper.absolutePos(TRUNK).offset(-1, 0, -1), helper.absolutePos(TRUNK).offset(1, 4, 1))) {
            BlockState state = level.getBlockState(pos);
            helper.assertTrue(!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES), "Left behind: " + state + " at " + pos);
        }
        helper.assertTrue(level.getBlockState(helper.absolutePos(wall)).is(Blocks.COBBLESTONE), "Foreign blocks stay untouched");
        helper.assertTrue(WanderingTreeSystem.isWarningPending(player.getUUID()), "The cutter must be told after the shriek");
        helper.assertTrue(find(level, record.id()) == null, "A vanished tree is no longer tracked");
        player.setGameMode(GameType.SPECTATOR);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void cuttingATreeBeforeItsThirdMoveIsAnOrdinaryCut(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper, Blocks.GRASS_BLOCK);
        plantOak(helper, TRUNK);
        WanderingTreeRecord record = WanderingTreeSystem.adopt(level, UUID.randomUUID(), helper.absolutePos(TRUNK), Long.MAX_VALUE);
        helper.assertTrue(record != null, "The oak must be adoptable");
        UncannyWorldState.get(level.getServer()).putWanderingTree(record.withMoves(WanderingTreeRules.TRAP_MOVE_THRESHOLD - 1));

        ServerPlayer player = mockPlayer(helper);
        BlockPos cut = helper.absolutePos(TRUNK);
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, cut, level.getBlockState(cut), player);
        WanderingTreeSystem.onBlockBreak(event);

        helper.assertTrue(!event.isCanceled(), "An unarmed tree must break normally");
        assertOakAt(helper, level, cut);
        helper.assertTrue(find(level, record.id()) == null, "A tree being cut stops wandering");
        player.setGameMode(GameType.SPECTATOR);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void treeNeverCrossesGroundThatIsNotDirtOrGrass(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper, Blocks.STONE);
        helper.setBlock(TRUNK.below(), Blocks.GRASS_BLOCK);
        plantOak(helper, TRUNK);
        WanderingTreeRecord record = WanderingTreeSystem.adopt(level, UUID.randomUUID(), helper.absolutePos(TRUNK), Long.MAX_VALUE);
        helper.assertTrue(record != null, "The oak must be adoptable on its grass block");

        WanderingTreeSystem.MoveOutcome outcome = WanderingTreeSystem.attemptMove(
                level, record, helper.absolutePos(FAR_BASE), List.of(), true);
        helper.assertTrue(outcome == WanderingTreeSystem.MoveOutcome.BLOCKED, "Stone ahead must block: " + outcome);
        assertOakAt(helper, level, helper.absolutePos(TRUNK));
        helper.assertTrue(current(level, record.id()).moves() == 0, "A blocked attempt is not a move");
        forget(level, record.id());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void treeIsSeenInFrontButNotBehindThePlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper, Blocks.GRASS_BLOCK);
        plantOak(helper, TRUNK);
        BlockPos base = helper.absolutePos(TRUNK);
        ServerPlayer player = mockPlayer(helper);
        Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(13, 1, 13)));
        Vec3 crown = Vec3.atCenterOf(base.above(3));
        Vec3 toTree = crown.subtract(stand.add(0.0D, player.getEyeHeight(), 0.0D));
        float yaw = (float) (Math.toDegrees(Math.atan2(toTree.z, toTree.x)) - 90.0D);
        float pitch = (float) -Math.toDegrees(Math.atan2(toTree.y, Math.hypot(toTree.x, toTree.z)));

        player.moveTo(stand.x, stand.y, stand.z, yaw, pitch);
        player.setYHeadRot(yaw);
        helper.assertTrue(WanderingTreeSystem.isObserved(level, base, List.of(player)),
                "A player facing the tree in open air must see it");

        player.moveTo(stand.x, stand.y, stand.z, yaw + 180.0F, -pitch);
        player.setYHeadRot(yaw + 180.0F);
        helper.assertTrue(!WanderingTreeSystem.isObserved(level, base, List.of(player)),
                "A player facing away must not see it");
        player.setGameMode(GameType.SPECTATOR);
        helper.succeed();
    }

    /** Mock players must accept the mod payloads, or the next phase sync crashes the test server. */
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        return player;
    }

    private static void floor(GameTestHelper helper, net.minecraft.world.level.block.Block block) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 0, z, block);
            }
        }
    }

    /** Four logs, two leaf rings and a cap, with Vanilla leaf distances so nothing decays. */
    private static void plantOak(GameTestHelper helper, BlockPos trunk) {
        for (int y = 0; y < 4; y++) {
            helper.setBlock(trunk.above(y), Blocks.OAK_LOG);
        }
        for (int y = 2; y <= 3; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        helper.setBlock(trunk.offset(dx, y, dz), leaves(Math.abs(dx) + Math.abs(dz)));
                    }
                }
            }
        }
        helper.setBlock(trunk.above(4), leaves(1));
        for (int[] side : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            helper.setBlock(trunk.offset(side[0], 4, side[1]), leaves(2));
        }
    }

    private static BlockState leaves(int distance) {
        return Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, distance);
    }

    private static void assertOakAt(GameTestHelper helper, ServerLevel level, BlockPos base) {
        for (int y = 0; y < 4; y++) {
            helper.assertTrue(level.getBlockState(base.above(y)).is(Blocks.OAK_LOG), "Missing log " + y + " at " + base);
        }
        int leaves = 0;
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-1, 2, -1), base.offset(1, 4, 1))) {
            if (level.getBlockState(pos).is(Blocks.OAK_LEAVES)) {
                leaves++;
            }
        }
        helper.assertTrue(leaves == 21, "The crown must travel whole, found " + leaves + " leaves at " + base);
    }

    private static WanderingTreeRecord find(ServerLevel level, UUID id) {
        return UncannyWorldState.get(level.getServer()).getWanderingTrees().stream()
                .filter(record -> record.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    private static WanderingTreeRecord current(ServerLevel level, UUID id) {
        WanderingTreeRecord record = find(level, id);
        if (record == null) {
            throw new IllegalStateException("Wandering tree record " + id + " disappeared");
        }
        return record;
    }

    private static void forget(ServerLevel level, UUID id) {
        UncannyWorldState.get(level.getServer()).removeWanderingTree(id);
    }
}
