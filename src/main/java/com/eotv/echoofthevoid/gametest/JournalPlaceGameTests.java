package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.event.UncannyJournalPlaces;
import com.eotv.echoofthevoid.lore.UncannyJournalProgressRules;
import com.eotv.echoofthevoid.state.JournalProgressRecord;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * Journal place terrain checks and construction (D-100). Places outgrew the test room: they are built on a stone
 * platform in a fresh, distant area of the (structure-free, flat) test world.
 */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class JournalPlaceGameTests {
    private static final String TEMPLATE = "special_test_room";
    private static final int PLATFORM_Y = 70;
    private static final int PLATFORM_RADIUS = 16;
    private static final int AREA_SPACING_CHUNKS = 4;
    private static final int AREA_TICKET_RADIUS = 2;
    private static final int FAR_AREA_TIMEOUT = 20_000;
    /** Far areas generate hundreds of chunks: kept apart from the timing-sensitive creature scenarios. */
    private static final String FAR_AREA_BATCH = "journal_places_far";
    private static final TicketType<ChunkPos> TEST_AREA =
            TicketType.create("echoofthevoid_test_area", Comparator.comparingLong(ChunkPos::toLong), 2_400);

    private JournalPlaceGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void placeIsRefusedOnProtectedBlocksOrWater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        clearRoom(helper);
        BlockPos origin = helper.absolutePos(new BlockPos(8, 1, 8));
        helper.assertTrue(origin.equals(UncannyJournalPlaces.surfaceOrigin(level, origin.getX(), origin.getZ())),
                "The surface origin is the first free block above the floor: "
                        + UncannyJournalPlaces.surfaceOrigin(level, origin.getX(), origin.getZ()) + " vs " + origin);
        helper.assertTrue(UncannyJournalPlaces.rejectionReason(level, 1, origin) == null,
                "A flat dry floor must accept a place: " + UncannyJournalPlaces.rejectionReason(level, 1, origin));

        helper.setBlock(new BlockPos(9, 1, 9), Blocks.TALL_GRASS.defaultBlockState()
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(new BlockPos(9, 2, 9), Blocks.TALL_GRASS.defaultBlockState()
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
        helper.assertTrue(UncannyJournalPlaces.rejectionReason(level, 1, origin) == null,
                "Tall grass is vegetation, not a protected two-block structure");
        helper.setBlock(new BlockPos(9, 2, 9), Blocks.AIR);
        for (int y = 1; y <= 6; y++) {
            helper.setBlock(new BlockPos(9, y, 9), Blocks.OAK_LOG);
        }
        helper.assertTrue(UncannyJournalPlaces.rejectionReason(level, 1, origin) == null,
                "A tree trunk is not uneven ground: " + UncannyJournalPlaces.rejectionReason(level, 1, origin));
        for (int y = 2; y <= 6; y++) {
            helper.setBlock(new BlockPos(9, y, 9), Blocks.AIR);
        }
        helper.setBlock(new BlockPos(9, 1, 9), Blocks.BARREL);
        helper.assertTrue("protected-block".equals(UncannyJournalPlaces.rejectionReason(level, 1, origin)),
                "A block entity in the footprint must refuse the place");
        helper.setBlock(new BlockPos(9, 1, 9), Blocks.AIR);

        helper.setBlock(new BlockPos(7, 0, 8), Blocks.WATER);
        helper.assertTrue("ground".equals(UncannyJournalPlaces.rejectionReason(level, 1, origin)),
                "Water at ground level must refuse the place");
        helper.setBlock(new BlockPos(7, 0, 8), Blocks.STONE);

        helper.assertTrue("depth".equals(UncannyJournalPlaces.rejectionReason(level, 5, origin)),
                "Morgan's mine needs depth above the world floor");
        for (int y = 1; y <= 4; y++) {
            helper.setBlock(new BlockPos(6, y, 6), Blocks.STONE);
        }
        helper.assertTrue("uneven".equals(UncannyJournalPlaces.rejectionReason(level, 1, origin)),
                "A step higher than three blocks must refuse a small place");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = FAR_AREA_TIMEOUT, batch = FAR_AREA_BATCH)
    public static void eachSurfacePlaceIsSoundAndHoldsExactlyItsOwnJournal(GameTestHelper helper) {
        List<Integer> volumes = List.of(1, 2, 3, 6, 4);
        List<Direction> facings = List.of(Direction.SOUTH, Direction.EAST);
        inFarAreas(helper, volumes.size() * facings.size(), origins -> {
            ServerLevel level = helper.getLevel();
            int area = 0;
            for (int volume : volumes) {
                for (Direction facing : facings) {
                    BlockPos origin = origins.get(area++);
                    helper.assertTrue(UncannyJournalPlaces.rejectionReason(level, volume, origin) == null, "A flat platform "
                            + "accepts place " + volume + ": " + UncannyJournalPlaces.rejectionReason(level, volume, origin));
                    BlockPos holder = UncannyJournalPlaces.build(level, volume, origin, facing);
                    assertSingleJournal(helper, level, origin, volume, holder, facing);
                    assertSound(helper, level, origin, volume);
                }
            }
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = FAR_AREA_TIMEOUT, batch = FAR_AREA_BATCH)
    public static void julesLivesInABigHouseWithAWallOfFullChests(GameTestHelper helper) {
        inFarAreas(helper, 1, origins -> julesHouse(helper, origins.get(0)));
    }

    private static void julesHouse(GameTestHelper helper, BlockPos origin) {
        ServerLevel level = helper.getLevel();
        BlockPos lectern = UncannyJournalPlaces.build(level, 4, origin, Direction.WEST);
        int doubleHalves = 0;
        int fullest = 0;
        for (BlockEntity blockEntity : placeBlockEntities(level, origin)) {
            if (blockEntity instanceof ChestBlockEntity chest
                    && chest.getBlockState().getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                doubleHalves++;
                int stacks = 0;
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    stacks += chest.getItem(slot).isEmpty() ? 0 : 1;
                }
                fullest = Math.max(fullest, stacks);
            }
        }
        helper.assertTrue(doubleHalves >= 12, "Jules' storage holds at least six double chests: " + doubleHalves / 2);
        helper.assertTrue(fullest >= 22, "One chest is nearly full of blocks, a hole here and there (" + fullest + ")");
        helper.assertTrue(level.getBlockEntity(lectern) instanceof LecternBlockEntity desk && desk.hasBook(),
                "A lectern holding a journal has a book, like a vanilla written book");
        helper.assertTrue(lectern.equals(script(origin, Direction.WEST, -3, 1, -1)),
                "The template lands where it was drawn (lectern facing the door): " + lectern);
        // The stairs up the west wall of the hall (script x -7, climbing north from z 2).
        List<BlockPos> steps = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            steps.add(script(origin, Direction.WEST, -7, 1 + i, 2 - i));
        }
        assertClimbable(helper, level, steps, "Jules' stairs");
        assertSound(helper, level, origin, 4);
    }

    /** World position of a template script position (x = -right, y = up + 1, z = forward). */
    private static BlockPos script(BlockPos origin, Direction facing, int x, int y, int z) {
        return origin.relative(facing, z).relative(facing.getClockWise(), -x).above(y - 1);
    }

    /**
     * A staircase a player walks up without jumping or breaking anything: each step a stair rising towards the next
     * one, three non-colliding blocks above every step but the last, and the first step reached from level floor.
     */
    private static void assertClimbable(GameTestHelper helper, ServerLevel level, List<BlockPos> steps, String name) {
        for (int i = 0; i < steps.size(); i++) {
            BlockPos step = steps.get(i);
            BlockState state = level.getBlockState(step);
            helper.assertTrue(state.getBlock() instanceof StairBlock
                            && state.getValue(StairBlock.HALF) == net.minecraft.world.level.block.state.properties.Half.BOTTOM,
                    name + ": step " + i + " at " + step + " is " + state);
            Direction up = state.getValue(StairBlock.FACING);
            if (i + 1 < steps.size()) {
                helper.assertTrue(steps.get(i + 1).equals(step.relative(up).above()),
                        name + ": step " + i + " does not rise towards the next one (" + up + ")");
            }
            int room = i + 1 < steps.size() ? 3 : 2;
            for (int h = 1; h <= room; h++) {
                BlockPos above = step.above(h);
                helper.assertTrue(level.getBlockState(above).getCollisionShape(level, above).isEmpty(),
                        name + ": no headroom over step " + i + " (" + level.getBlockState(above) + " at " + above + ")");
            }
        }
        BlockPos first = steps.get(0);
        BlockPos before = first.relative(level.getBlockState(first).getValue(StairBlock.FACING).getOpposite());
        helper.assertTrue(level.getBlockState(before).getCollisionShape(level, before).isEmpty()
                        && level.getBlockState(before.below()).isFaceSturdy(level, before.below(), Direction.UP),
                name + ": the first step is not reached from the floor");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = FAR_AREA_TIMEOUT, batch = FAR_AREA_BATCH)
    public static void morganMineGoesDeepAndStaysWalkable(GameTestHelper helper) {
        inFarAreas(helper, 1, origins -> morganMine(helper, origins.get(0)));
    }

    private static void morganMine(GameTestHelper helper, BlockPos origin) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(UncannyJournalPlaces.rejectionReason(level, 5, origin) == null,
                "Open ground over deep rock accepts Morgan's base: " + UncannyJournalPlaces.rejectionReason(level, 5, origin));
        List<BlockPos> barrels = new ArrayList<>();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos barrel = UncannyJournalPlaces.morganShaftCentre(origin, facing).below(30);
            level.setBlock(barrel, Blocks.BARREL.defaultBlockState(), 2);
            barrels.add(barrel);
        }
        helper.assertTrue("protected-block".equals(UncannyJournalPlaces.rejectionReason(level, 5, origin)),
                "A container in the way of every possible shaft refuses the base");
        barrels.forEach(barrel -> level.setBlock(barrel, Blocks.AIR.defaultBlockState(), 2));

        Direction facing = UncannyJournalPlaces.buildFacing(level, 5, origin, level.random);
        helper.assertTrue(facing != null, "Some door side fits the mine");
        BlockPos holder = UncannyJournalPlaces.build(level, 5, origin, facing);
        assertSingleJournal(helper, level, origin, 5, holder, facing);
        assertSound(helper, level, origin, 5);
        helper.assertTrue(holder.getY() <= origin.getY() - 40, "The journal waits deep down: " + holder.getY());

        // Walk the spiral from the tower floor: each stair one block lower, with headroom, ending on a flat landing.
        BlockPos centre = UncannyJournalPlaces.morganShaftCentre(origin, facing);
        Direction right = facing.getClockWise();
        int[][] ring = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};
        helper.assertTrue(level.getBlockState(centre.relative(facing, 2).below()).isSolidRender(level, centre),
                "The tower floor leads to the first stair");
        int steps = origin.getY() - holder.getY();
        for (int i = 0; ; i++) {
            BlockPos cell = centre.relative(facing, ring[i % 8][0]).relative(right, ring[i % 8][1]);
            BlockPos step = cell.atY(origin.getY() - 1 - i);
            BlockState state = level.getBlockState(step);
            if (!(state.getBlock() instanceof StairBlock)) {
                helper.assertTrue(i > 0 && i % 8 == 0 && state.isSolidRender(level, step),
                        "The spiral ends on a full landing under its first stair, step " + i + ": " + state);
                helper.assertTrue(step.getY() + 1 == holder.getY(), "The landing is level with the hub floor");
                break;
            }
            // Torches may hang in the headroom: they do not collide.
            helper.assertTrue(level.getBlockState(step.above()).getCollisionShape(level, step.above()).isEmpty()
                            && level.getBlockState(step.above(2)).getCollisionShape(level, step.above(2)).isEmpty(),
                    "Headroom over stair " + i);
            helper.assertTrue(i <= steps, "The spiral never runs below the hub");
        }
        helper.assertTrue(level.getBlockState(centre.relative(facing, 2).atY(holder.getY() - 1)).isSolidRender(level, centre)
                        && level.getBlockState(centre.relative(facing, 2).atY(holder.getY())).isAir(),
                "A corridor opens from the landing at the same level");

        boolean shrieker = false;
        boolean redstone = false;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-24, holder.getY() - centre.getY() - 2, -24),
                centre.offset(24, holder.getY() - centre.getY() + 3, 24))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.SCULK_SHRIEKER)) {
                shrieker = true;
                helper.assertTrue(!state.getValue(SculkShriekerBlock.CAN_SUMMON), "The shrieker behind the wall cannot summon");
            }
            redstone |= state.is(Blocks.REDSTONE_WIRE);
        }
        helper.assertTrue(shrieker && redstone, "The walled tunnel keeps its redstone mark and its sculk");

        // One tunnel only (no water passage, no side branch) and a bare room: its single chest is the kit chest.
        int water = 0;
        int chests = 0;
        int stone = 0;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-24, holder.getY() - centre.getY() - 4, -24),
                centre.offset(24, holder.getY() - centre.getY() + 4, 24))) {
            BlockState state = level.getBlockState(pos);
            water += level.getFluidState(pos).isEmpty() ? 0 : 1;
            chests += state.is(Blocks.CHEST) ? 1 : 0;
            stone += state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.STONE_BRICK_STAIRS) ? 1 : 0;
        }
        helper.assertTrue(water == 0 && chests == 1, "Morgan's mine: no water tunnel, one chest (" + water + ", " + chests + ")");
        helper.assertTrue(stone == 0, "Deep down, the mine is deepslate only (" + stone + " stone blocks)");

        // The hall's stairs (script x 0, climbing north from z -4) and the tower's stairs up to its roof.
        List<BlockPos> hall = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            hall.add(script(origin, facing, 0, 1 + i, -4 - i));
        }
        assertClimbable(helper, level, hall, "Morgan's hall stairs");
        List<BlockPos> tower = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            tower.add(script(origin, facing, -10, 1 + i, -5 - i));
        }
        for (int i = 0; i < 6; i++) {
            tower.add(script(origin, facing, -9 + i, 7 + i, -10));
        }
        assertClimbable(helper, level, tower, "Morgan's tower stairs");
        BlockPos roof = script(origin, facing, -4, 13, -9);
        helper.assertTrue(level.getBlockState(roof.below()).isFaceSturdy(level, roof.below(), Direction.UP)
                        && level.getBlockState(roof).isAir(),
                "The tower stairs come out on its roof");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void readingJulesJournalOnItsLecternKeepsTheLecternOpen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        JournalProgressRecord saved = state.getJournalProgress();
        clearRoom(helper);
        try {
            state.setJournalProgress(JournalProgressRecord.empty());
            // Jules' lectern as the place lays it, in the room (the whole house is checked in the far area).
            BlockPos lectern = helper.absolutePos(new BlockPos(8, 1, 8));
            level.setBlock(lectern, Blocks.LECTERN.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LecternBlock.HAS_BOOK, true), 2);
            ((LecternBlockEntity) level.getBlockEntity(lectern)).setBook(UncannyJournalPlaces.createJournal(4));
            ServerPlayer reader = helper.makeMockServerPlayerInLevel();
            NetworkRegistry.configureMockConnection(reader.connection.getConnection());
            reader.setGameMode(GameType.SURVIVAL);
            reader.teleportTo(lectern.getX() + 0.5D, lectern.getY(), lectern.getZ() + 2.5D);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(lectern), Direction.UP, lectern, false);
            reader.gameMode.useItemOn(reader, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(state.getJournalProgress().isFound(4),
                    "Reading the lectern through the real interaction marks The Recipe Isn't There found");
            helper.assertTrue(reader.containerMenu instanceof LecternMenu menu && menu.stillValid(reader),
                    "The lectern screen stays open on a journal: " + reader.containerMenu);
            helper.assertTrue(!((LecternBlockEntity) level.getBlockEntity(lectern)).getBook().isEmpty(),
                    "Reading leaves the book on the lectern");
            reader.closeContainer();
        } finally {
            state.setJournalProgress(saved);
            clearRoom(helper);
        }
        helper.succeed();
    }

    private static void assertSingleJournal(
            GameTestHelper helper, ServerLevel level, BlockPos origin, int volume, BlockPos holder, Direction facing) {
        helper.assertTrue(holder != null, "Place " + volume + " facing " + facing + " must be built");
        List<String> titles = new ArrayList<>();
        for (BlockEntity blockEntity : placeBlockEntities(level, origin)) {
            if (blockEntity instanceof Container container) {
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                    collectTitle(container.getItem(slot), titles);
                }
            } else if (blockEntity instanceof LecternBlockEntity lectern) {
                collectTitle(lectern.getBook(), titles);
            }
        }
        helper.assertTrue(titles.size() == 1, "Place " + volume + " must hold exactly one journal: " + titles);
        helper.assertTrue(UncannyJournalProgressRules.volumeForTitle(titles.get(0)) == volume,
                "Place " + volume + " must hold its own journal, found " + titles);
        helper.assertTrue(level.getBlockEntity(holder) != null && level.getBlockEntity(holder).getPersistentData()
                        .getInt(UncannyJournalPlaces.JOURNAL_PLACE_CHEST_FLAG) == volume,
                "The journal holder of place " + volume + " must remember its volume");
    }

    /**
     * What a player would notice at once: a chest that cannot open, a pillow away from the wall, a roof topped with
     * full planks, cobblestone patches in Eli's plank walls.
     */
    private static void assertSound(GameTestHelper helper, ServerLevel level, BlockPos origin, int volume) {
        for (BlockEntity blockEntity : placeBlockEntities(level, origin)) {
            if (blockEntity instanceof ChestBlockEntity) {
                BlockPos above = blockEntity.getBlockPos().above();
                helper.assertTrue(!level.getBlockState(above).isRedstoneConductor(level, above),
                        "Place " + volume + ": the chest at " + blockEntity.getBlockPos() + " is blocked by "
                                + level.getBlockState(above));
            }
            // Filled like a player: a container that holds anything starts at its first slot (the top half of a
            // double chest), never items scattered slot by slot.
            if ((blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity)
                    && blockEntity instanceof Container container && !container.isEmpty()
                    && !(blockEntity instanceof ChestBlockEntity chest
                            && chest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.LEFT)) {
                helper.assertTrue(!container.getItem(0).isEmpty(),
                        "Place " + volume + ": the container at " + blockEntity.getBlockPos() + " does not start at its first slot");
            }
            if (blockEntity instanceof ChestBlockEntity chest && chest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.LEFT
                    && !chest.isEmpty()) {
                BlockPos top = chest.getBlockPos().relative(ChestBlock.getConnectedDirection(chest.getBlockState()));
                helper.assertTrue(level.getBlockEntity(top) instanceof Container upper && !upper.getItem(0).isEmpty(),
                        "Place " + volume + ": a double chest at " + top + " is filled from its bottom half");
            }
        }
        AABB area = new AABB(origin).inflate(UncannyJournalPlaces.MAX_REACH + 2, 0, UncannyJournalPlaces.MAX_REACH + 2)
                .expandTowards(0, UncannyJournalPlaces.MAX_HEIGHT_ABOVE, 0).expandTowards(0, -UncannyJournalPlaces.MAX_DEPTH_BELOW, 0);
        helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, area).isEmpty(),
                "Place " + volume + ": items lie around the place: "
                        + level.getEntitiesOfClass(ItemEntity.class, area).stream().map(e -> e.getItem().toString()).toList());
        // Every block holds where it is: no torch on a pane, no hanging lantern in the air, no plant on stone.
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-UncannyJournalPlaces.MAX_REACH, -2, -UncannyJournalPlaces.MAX_REACH),
                origin.offset(UncannyJournalPlaces.MAX_REACH, UncannyJournalPlaces.MAX_HEIGHT_ABOVE, UncannyJournalPlaces.MAX_REACH))) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.canSurvive(level, pos)) {
                helper.fail("Place " + volume + ": " + state + " at " + pos + " cannot stay there");
            }
            // A window sits in a wall, under a lintel: a pane with open sky over it pokes out of a roof.
            if (state.is(Blocks.GLASS_PANE) && level.getBlockState(pos.above()).isAir()) {
                helper.fail("Place " + volume + ": a glass pane sticks out at " + pos);
            }
        }
        int reach = UncannyJournalPlaces.MAX_FOOTPRINT_RADIUS + UncannyJournalPlaces.GROUND_MARGIN;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
                helper.assertTrue(!level.getBlockState(top).is(BlockTags.PLANKS),
                        "Place " + volume + ": a full plank block tops the roof at " + top);
                for (int y = origin.getY() - 2; y <= origin.getY() + 14; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD && volume != 2) {
                        BlockPos wall = pos.relative(state.getValue(BedBlock.FACING));
                        helper.assertTrue(level.getBlockState(wall).isSolidRender(level, wall),
                                "Place " + volume + ": the pillow at " + pos + " is not against a wall");
                    }
                    if (volume == 1 && y >= origin.getY() && y <= origin.getY() + 3) {
                        helper.assertTrue(!state.is(Blocks.COBBLESTONE), "Eli's walls are planks only: " + pos);
                    }
                }
            }
        }
        // The top course of a pitched roof is never a full block (Rowan's ridge used to be planks).
        if (volume == 3 || volume == 6) {
            int highest = Integer.MIN_VALUE;
            BlockState crest = Blocks.AIR.defaultBlockState();
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                    BlockState state = level.getBlockState(new BlockPos(x, y, z));
                    if (y > highest && !state.is(Blocks.BIRCH_LEAVES)) {
                        highest = y;
                        crest = state;
                    }
                }
            }
            helper.assertTrue(crest.getBlock() instanceof SlabBlock, "Place " + volume + " has a slab ridge: " + crest);
        }
    }

    /** Block entities of a place: its whole reach and Morgan's depth, like the recovery scan. */
    private static List<BlockEntity> placeBlockEntities(ServerLevel level, BlockPos origin) {
        int reach = UncannyJournalPlaces.MAX_REACH;
        List<BlockEntity> found = new ArrayList<>();
        for (int cx = (origin.getX() - reach) >> 4; cx <= (origin.getX() + reach) >> 4; cx++) {
            for (int cz = (origin.getZ() - reach) >> 4; cz <= (origin.getZ() + reach) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos pos = blockEntity.getBlockPos();
                    if (Math.abs(pos.getX() - origin.getX()) <= reach && Math.abs(pos.getZ() - origin.getZ()) <= reach
                            && pos.getY() >= origin.getY() - UncannyJournalPlaces.MAX_DEPTH_BELOW
                            && pos.getY() <= origin.getY() + UncannyJournalPlaces.MAX_HEIGHT_ABOVE) {
                        found.add(blockEntity);
                    }
                }
            }
        }
        return found;
    }

    /**
     * Runs {@code body} once {@code count} fresh, never-built areas of the flat test world are loaded (through a
     * ticket, like the journal place search, never by a blocking load), each with a stone platform in the air and the
     * origin standing on its centre; then the test succeeds. Each run picks new areas so earlier runs never get in
     * the way.
     */
    private static void inFarAreas(GameTestHelper helper, int count, Consumer<List<BlockPos>> body) {
        ServerLevel level = helper.getLevel();
        int baseX = 2_000 + level.random.nextInt(20_000);
        int baseZ = 2_000 + level.random.nextInt(20_000);
        List<ChunkPos> chunks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ChunkPos chunk = new ChunkPos(baseX + i * AREA_SPACING_CHUNKS, baseZ);
            level.getChunkSource().addRegionTicket(TEST_AREA, chunk, AREA_TICKET_RADIUS, chunk);
            chunks.add(chunk);
        }
        boolean[] done = {false};
        helper.onEachTick(() -> {
            if (done[0]) {
                return;
            }
            if (!chunks.stream().allMatch(chunk -> isAreaLoaded(level, chunk))) {
                // The test server runs ticks back to back while chunks generate on worker threads: let real time
                // pass, or the tick timeout runs out in a few seconds.
                try {
                    Thread.sleep(2L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
            done[0] = true;
            try {
                List<BlockPos> origins = new ArrayList<>();
                for (ChunkPos chunk : chunks) {
                    BlockPos centre = new BlockPos(chunk.getMiddleBlockX(), PLATFORM_Y, chunk.getMiddleBlockZ());
                    for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
                        for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
                            level.setBlock(centre.offset(dx, 0, dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                        }
                    }
                    origins.add(centre.above());
                }
                body.accept(origins);
            } finally {
                chunks.forEach(chunk -> level.getChunkSource().removeRegionTicket(TEST_AREA, chunk, AREA_TICKET_RADIUS, chunk));
            }
            helper.succeed();
        });
    }

    private static boolean isAreaLoaded(ServerLevel level, ChunkPos chunk) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (level.getChunkSource().getChunkNow(chunk.x + dx, chunk.z + dz) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void collectTitle(ItemStack stack, List<String> titles) {
        WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content != null) {
            titles.add(content.title().raw());
        }
    }

    private static void clearRoom(GameTestHelper helper) {
        AABB room = new AABB(helper.absolutePos(new BlockPos(-2, -2, -2)).getCenter(),
                helper.absolutePos(new BlockPos(17, 12, 17)).getCenter());
        for (Entity entity : helper.getLevel().getEntities((Entity) null, room, entity -> !(entity instanceof Player))) {
            entity.discard();
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, -1, z, Blocks.STONE);
                helper.setBlock(x, 0, z, Blocks.STONE);
                // Up to and including the framework's barrier ceiling, which the height map would see.
                for (int y = 1; y <= 10; y++) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }
    }
}
