package com.eotv.echoofthevoid.event;

import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import com.eotv.echoofthevoid.lore.UncannyLoreBookLibrary;
import com.eotv.echoofthevoid.world.StructureForgeTemplates;
import com.eotv.echoofthevoid.world.UncannyBlockMutationSafety;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.decoration.PaintingVariants;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The six journal places (D-100): terrain checks and construction. Each place is drawn in a local frame
 * (forward = towards its door, right = clockwise of it) so it can face any side. Its surface fits within
 * {@link #MAX_FOOTPRINT_RADIUS} blocks of its origin plus a {@link #GROUND_MARGIN} where the levelled ground blends
 * into the terrain, and nothing (Morgan's mine included) reaches farther than {@link #MAX_REACH} horizontally.
 *
 * <p>Catalog volumes: 1 five torches (Eli), 2 A Longer Way Home (Mara), 3 Room for One (Rowan),
 * 4 The Recipe Isn't There (Jules, the only journal on a lectern), 5 Under the Third Warning (Morgan),
 * 6 only dan heard it (Casey).
 */
public final class UncannyJournalPlaces {
    /** Flag on the chest (or Jules' lectern) holding a place's journal: the catalog volume. */
    public static final String JOURNAL_PLACE_CHEST_FLAG = "eotv_journal_place";
    /** Flag on every chest a place builds: the only chests a lost journal may be put back into. */
    public static final String PLACE_CONTAINER_FLAG = "eotv_journal_place_container";
    public static final int MAX_FOOTPRINT_RADIUS = 12;
    public static final int GROUND_MARGIN = 3;
    /** Horizontal extent of the largest place, its mine and its blended ground; a candidate chunk loads 19. */
    public static final int MAX_REACH = 18;
    /** Vertical extent of the places around their origin (Morgan's mine goes deepest). */
    public static final int MAX_DEPTH_BELOW = 160;
    public static final int MAX_HEIGHT_ABOVE = 24;
    private static final int SMALL_PLACE_RADIUS = 6;
    private static final int CLEAR_HEIGHT = 16;
    private static final int TREE_CLEAR_HEIGHT = 32;
    /** Morgan's mine: the spiral shaft centre in the local frame, and its depth rules. */
    private static final int SHAFT_FW = -7;
    private static final int SHAFT_R = 7;
    private static final int[][] SHAFT_RING = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};
    /**
     * StructureForge templates and the template position of their script origin (the {@code anchor} of their
     * {@code .markers.json}): one block under the place origin.
     */
    static final String JULES_TEMPLATE = "journal_place_jules";
    static final String MORGAN_TEMPLATE = "journal_place_morgan";
    private static final BlockPos JULES_ANCHOR = new BlockPos(10, 0, 8);
    private static final BlockPos MORGAN_ANCHOR = new BlockPos(12, 0, 12);
    /** Persistent data a template puts on a container (or a lectern) for the code to fill, then removed. */
    private static final String TEMPLATE_FILL_TAG = "eotv_fill";
    private static final int MORGAN_TARGET_FEET_Y = -40;
    private static final int MORGAN_MAX_DEPTH = 140;
    /** Under this height the rock is deepslate: the mine is lined and floored with it, never with stone. */
    private static final int DEEPSLATE_LEVEL = 8;
    static final int MORGAN_MIN_DEPTH = 40;

    private UncannyJournalPlaces() {
    }

    /** First free block above the real ground of this column: tree trunks and leaves do not count, water does. */
    public static BlockPos surfaceOrigin(ServerLevel level, int x, int z) {
        return new BlockPos(x, groundHeight(level, x, z), z);
    }

    /** The origin of a place centred on this column: the median ground height of its footprint. */
    public static BlockPos levelledOrigin(ServerLevel level, int volume, int x, int z) {
        int radius = footprintRadius(volume);
        int grid = radius <= SMALL_PLACE_RADIUS ? 1 : 2;
        List<Integer> heights = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx += grid) {
            for (int dz = -radius; dz <= radius; dz += grid) {
                heights.add(groundHeight(level, x + dx, z + dz));
            }
        }
        heights.sort(Integer::compare);
        return new BlockPos(x, heights.get(heights.size() / 2), z);
    }

    static int groundHeight(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y - 1, z);
        while (pos.getY() > level.getMinBuildHeight()) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) {
                break;
            }
            pos.move(Direction.DOWN);
        }
        return pos.getY() + 1;
    }

    /** Oceans, rivers and beaches never hold a place. Works on ungenerated chunks too. */
    public static boolean isWaterBiome(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN)
                || biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_BEACH);
    }

    /** {@code null} when the place of {@code volume} can stand at {@code origin}, else a short reason. */
    public static String rejectionReason(ServerLevel level, int volume, BlockPos origin) {
        if (isWaterBiome(level, origin)) {
            return "biome";
        }
        if (volume == 5 && morganBottomUp(level, origin) == Integer.MIN_VALUE) {
            return "depth";
        }
        int radius = footprintRadius(volume);
        // Origin, corners and edge midpoints: a village or ruin may only overlap one side of the footprint. Real
        // pieces only: a whole start's bounding box (a mineshaft under the hill) would refuse empty surface.
        for (int dx = -radius; dx <= radius; dx += radius) {
            for (int dz = -radius; dz <= radius; dz += radius) {
                for (int dy = 0; dy <= 4; dy += 4) {
                    if (hasStructurePiece(level, origin.offset(dx, dy, dz))) {
                        return "vanilla-structure";
                    }
                }
            }
        }
        int maxStep = maxGroundStep(volume);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int surfaceY = groundHeight(level, x, z);
                if (Math.abs(surfaceY - origin.getY()) > maxStep) {
                    return "uneven";
                }
                BlockPos ground = new BlockPos(x, surfaceY - 1, z);
                if (!level.getFluidState(ground).isEmpty() || !level.getBlockState(ground).isSolidRender(level, ground)) {
                    return "ground";
                }
                for (int y = origin.getY() - maxStep - 1; y <= origin.getY() + 12; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isProtectedTerrain(level, pos, level.getBlockState(pos))) {
                        return "protected-block";
                    }
                }
            }
        }
        if (volume != 5) {
            return null;
        }
        String reason = null;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            reason = morganUndergroundReason(level, origin, facing);
            if (reason == null) {
                return null;
            }
        }
        return reason;
    }

    /** The side the door of the place faces: random, except Morgan's, whose mine must fit under the place. */
    public static Direction buildFacing(ServerLevel level, int volume, BlockPos origin, RandomSource random) {
        int first = random.nextInt(4);
        if (volume != 5) {
            return Direction.from2DDataValue(first);
        }
        for (int i = 0; i < 4; i++) {
            Direction facing = Direction.from2DDataValue((first + i) % 4);
            if (morganUndergroundReason(level, origin, facing) == null) {
                return facing;
            }
        }
        return null;
    }

    /** The shared mutation guard, except tall grass and tall flowers, which are only vegetation here. */
    private static boolean isProtectedTerrain(ServerLevel level, BlockPos pos, BlockState state) {
        return !(state.getBlock() instanceof DoublePlantBlock) && UncannyBlockMutationSafety.isProtected(level, pos, state);
    }

    /** Underground, the deep dark's own sculk blocks are rock like any other. */
    private static boolean isProtectedUnderground(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.is(Blocks.SCULK_SENSOR) && !state.is(Blocks.CALIBRATED_SCULK_SENSOR)
                && !state.is(Blocks.SCULK_SHRIEKER) && !state.is(Blocks.SCULK_CATALYST)
                && isProtectedTerrain(level, pos, state);
    }

    private static boolean hasStructurePiece(ServerLevel level, BlockPos pos) {
        return level.structureManager().getStructureWithPieceAt(pos, any -> true).isValid();
    }

    /**
     * Builds the place of {@code volume} with its door towards {@code facing}.
     *
     * @return the chest (lectern for Jules) holding the journal, or {@code null} if it could not be filled
     */
    public static BlockPos build(ServerLevel level, int volume, BlockPos origin, Direction facing) {
        Frame frame = new Frame(level, origin.immutable(), facing);
        int radius = footprintRadius(volume);
        frame.prepareGround(radius, GROUND_MARGIN);
        BlockPos journal = switch (volume) {
            case 1 -> buildEli(frame);
            case 2 -> buildMara(frame);
            case 3 -> buildRowan(frame);
            case 4 -> buildJules(frame);
            case 5 -> buildMorgan(frame);
            case 6 -> buildCasey(frame);
            default -> null;
        };
        frame.updateShapes();
        frame.dustWithSnow(radius + GROUND_MARGIN);
        clearLooseItems(level, origin);
        return journal;
    }

    /**
     * Removes the items lying around a place (seeds of the cleared grass, anything knocked off while building), so it
     * never looks like it has just appeared. Place journals on the ground are left alone.
     *
     * @return how many item entities were removed
     */
    public static int clearLooseItems(ServerLevel level, BlockPos origin) {
        AABB area = new AABB(origin).inflate(MAX_REACH + 2, 0, MAX_REACH + 2)
                .expandTowards(0.0D, MAX_HEIGHT_ABOVE, 0.0D).expandTowards(0.0D, -MAX_DEPTH_BELOW, 0.0D);
        int removed = 0;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area,
                item -> UncannyJournalRecovery.placeJournalVolume(item.getItem()) <= 0)) {
            item.discard();
            removed++;
        }
        return removed;
    }

    /** Block entities of a place: its whole reach, from {@code below} under the origin to {@code above} over it. */
    public static List<BlockEntity> blockEntitiesAround(ServerLevel level, BlockPos origin, int below, int above) {
        List<BlockEntity> found = new ArrayList<>();
        for (int cx = (origin.getX() - MAX_REACH) >> 4; cx <= (origin.getX() + MAX_REACH) >> 4; cx++) {
            for (int cz = (origin.getZ() - MAX_REACH) >> 4; cz <= (origin.getZ() + MAX_REACH) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos pos = blockEntity.getBlockPos();
                    if (Math.abs(pos.getX() - origin.getX()) <= MAX_REACH && Math.abs(pos.getZ() - origin.getZ()) <= MAX_REACH
                            && pos.getY() >= origin.getY() - below && pos.getY() <= origin.getY() + above) {
                        found.add(blockEntity);
                    }
                }
            }
        }
        return found;
    }

    public static ItemStack createJournal(int volume) {
        ItemStack journal = new ItemStack(UncannyItemRegistry.UNCANNY_LORE_PIECE.get());
        journal.set(DataComponents.WRITTEN_BOOK_CONTENT, UncannyLoreBookLibrary.contentForVolume(volume));
        return UncannyJournalRecovery.tag(journal, volume);
    }

    static int footprintRadius(int volume) {
        return switch (volume) {
            case 1 -> 4;
            case 2 -> 5;
            case 4 -> MAX_FOOTPRINT_RADIUS;
            case 5 -> MAX_FOOTPRINT_RADIUS;
            default -> SMALL_PLACE_RADIUS;
        };
    }

    /** A small place sits on gentle ground; a big base is levelled like a player would, cutting and filling more. */
    static int maxGroundStep(int volume) {
        return footprintRadius(volume) <= SMALL_PLACE_RADIUS ? 3 : 5;
    }

    // ---------------------------------------------------------------- 1. five torches (Eli, beginner, messy)

    /** A cramped first shelter: one torch on every wall but none over the door, dirt under one of them. */
    private static BlockPos buildEli(Frame f) {
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        f.box(-3, 3, -3, 3, 0, 4, (fw, up, r) -> planks, true);
        f.floor(-2, 2, -2, 2, Blocks.DIRT.defaultBlockState());
        f.roof(-3, 3, -3, 3, 4, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        f.door(3, 0, Blocks.OAK_DOOR);
        f.wallTorch(-2, 1, 0, f.forward());
        f.wallTorch(0, 1, -2, f.right());
        f.wallTorch(0, 1, 2, f.left());
        f.wallTorch(2, 1, -2, f.back());
        // The fifth torch, over the door, is missing; the dirt marks the one Eli watched.
        f.set(0, 0, -3, Blocks.DIRT.defaultBlockState());
        f.bed(-1, 0, 2, f.back(), Blocks.RED_BED);
        f.set(1, 0, -2, Blocks.CRAFTING_TABLE.defaultBlockState());
        f.set(1, 0, 2, Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, f.left()));
        BlockPos chest = f.chest(-2, 0, -2, f.forward());
        fillMessy(f.level, chest, List.of(
                new ItemStack(Items.DIRT, 23), new ItemStack(Items.WHEAT_SEEDS, 7), new ItemStack(Items.DANDELION, 3),
                new ItemStack(Items.POPPY, 2), worn(Items.WOODEN_PICKAXE, 0.7F, f), worn(Items.WOODEN_SWORD, 0.4F, f),
                new ItemStack(Items.BREAD, 2), new ItemStack(Items.ROTTEN_FLESH, 4), new ItemStack(Items.COBBLESTONE, 17),
                new ItemStack(Items.OAK_SAPLING, 1), new ItemStack(Items.STICK, 5), new ItemStack(Items.TORCH, 1)));
        return placeJournal(f.level, chest, 1, true);
    }

    // ---------------------------------------------------------------- 2. A Longer Way Home (Mara, explorer)

    /** A camp far from everything: cold campfire, lean-to over a bed, birch logs, a travel pack. */
    private static BlockPos buildMara(Frame f) {
        f.set(0, 0, 0, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        BlockState seat = Blocks.BIRCH_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, f.right().getAxis());
        f.set(2, 0, -1, seat);
        f.set(2, 0, 0, seat);
        BlockState seatAcross = Blocks.BIRCH_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, f.forward().getAxis());
        f.set(0, 0, 2, seatAcross);
        // Lean-to: a sloped wool roof over the bed, open towards the fire.
        f.bed(-3, 0, -1, f.right(), Blocks.LIGHT_GRAY_BED);
        for (int r = -2; r <= 1; r++) {
            f.set(-4, 0, r, Blocks.BROWN_WOOL.defaultBlockState());
            f.set(-4, 1, r, Blocks.BROWN_WOOL.defaultBlockState());
            f.set(-3, 2, r, Blocks.BROWN_WOOL.defaultBlockState());
        }
        f.set(-3, 0, -2, Blocks.OAK_FENCE.defaultBlockState());
        f.set(-3, 1, -2, Blocks.OAK_FENCE.defaultBlockState());
        f.set(-3, 0, 1, Blocks.OAK_FENCE.defaultBlockState());
        f.set(-3, 1, 1, Blocks.OAK_FENCE.defaultBlockState());
        f.set(-1, 0, 3, Blocks.OAK_FENCE.defaultBlockState());
        f.set(-1, 1, 3, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        f.set(-2, 0, 3, Blocks.CRAFTING_TABLE.defaultBlockState());
        f.birch(-4, -4);
        f.birch(-3, 4);
        BlockPos chest = f.chest(-3, 0, 2, f.forward());
        fillTidy(f.level, chest, List.of(
                new ItemStack(Items.COOKED_BEEF, 9), new ItemStack(Items.BIRCH_LOG, 24), worn(Items.IRON_AXE, 0.35F, f),
                new ItemStack(Items.TORCH, 16), new ItemStack(Items.ARROW, 12), new ItemStack(Items.BIRCH_BOAT, 1)));
        return placeJournal(f.level, chest, 2, false);
    }

    // ---------------------------------------------------------------- 3. Room for One (Rowan, animal keeper)

    /** A finished-looking house with an upstairs room nobody uses, and a pen of cows beside it. */
    private static BlockPos buildRowan(Frame f) {
        BlockState logs = Blocks.SPRUCE_LOG.defaultBlockState();
        BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        // House: f -4..2, r -5..1, two storeys.
        f.box(-4, 2, -5, 1, 0, 7, (fw, up, r) -> (fw == -4 || fw == 2) && (r == -5 || r == 1) ? logs : planks, true);
        f.floor(-3, 1, -4, 0, Blocks.OAK_PLANKS.defaultBlockState());
        f.fill(-3, 1, 4, -4, 0, Blocks.OAK_PLANKS.defaultBlockState());
        f.gableRoof(true, -5, 1, -4, 2, 7, Blocks.SPRUCE_STAIRS, planks, slab(Blocks.SPRUCE_SLAB), true, true);
        f.set(-1, 1, 1, Blocks.GLASS_PANE.defaultBlockState());
        f.set(-1, 5, 1, Blocks.GLASS_PANE.defaultBlockState());
        f.set(2, 5, -2, Blocks.GLASS_PANE.defaultBlockState());
        f.door(2, -2, Blocks.SPRUCE_DOOR);
        // Ladder up through a hole in the upper floor.
        for (int up = 0; up <= 4; up++) {
            f.set(-3, up, -4, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, f.right()));
        }
        f.wallTorch(-2, 1, 0, f.left());
        f.wallTorch(-2, 6, 0, f.left());
        // Downstairs: a tidy keeper's chest, crafting table, furnace.
        f.set(1, 0, -4, Blocks.CRAFTING_TABLE.defaultBlockState());
        f.set(0, 0, -4, Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, f.right()));
        BlockPos keeper = f.chest(-3, 0, 0, f.forward());
        fillTidy(f.level, keeper, List.of(
                new ItemStack(Items.WHEAT, 32), new ItemStack(Items.WHEAT, 17), new ItemStack(Items.CARROT, 24),
                new ItemStack(Items.LEAD, 4), new ItemStack(Items.BONE, 12), new ItemStack(Items.NAME_TAG, 1)));
        // Upstairs: the blue bed (pillow against the back wall), the sunset painting, the "empty" chest.
        f.bed(-2, 5, -3, f.back(), Blocks.BLUE_BED);
        f.painting(-3, 6, -1, f.forward(), PaintingVariants.SUNSET);
        BlockPos upstairs = f.chest(1, 5, 0, f.left());
        fillMessy(f.level, upstairs, List.of(
                new ItemStack(Items.BOWL, 1), new ItemStack(Items.WHEAT_SEEDS, 1), new ItemStack(Items.STRING, 1)));
        // Pen: f -4..3, r 3..6, with a gate and two cows.
        for (int fw = -4; fw <= 3; fw++) {
            for (int r = 3; r <= 6; r++) {
                boolean edge = fw == -4 || fw == 3 || r == 3 || r == 6;
                if (edge) {
                    f.set(fw, 0, r, Blocks.OAK_FENCE.defaultBlockState());
                }
            }
        }
        f.set(3, 0, 4, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, f.forward()));
        // Outside the pen: a full block inside lets the cows step onto it and over the fence.
        f.set(-5, 0, 4, Blocks.HAY_BLOCK.defaultBlockState());
        f.cow(0, 4);
        f.cow(1, 5);
        return placeJournal(f.level, upstairs, 3, true);
    }

    // ---------------------------------------------------------------- 4. The Recipe Isn't There (Jules, crafter)

    /**
     * An advanced player's big house, drawn with StructureForge ({@code Ressources/Structures/journal_place_jules.py}):
     * a two-storey hall with a porch and a storage wing holding a wall of double chests. The journal lies open on a
     * lectern facing the door; the story's props stand around it (the chest of exactly six stacks, the page in a
     * barrel, the furnace that finished an item with no flame, the painting, the open door to the storage).
     */
    private static BlockPos buildJules(Frame f) {
        if (!f.template(JULES_TEMPLATE, JULES_ANCHOR)) {
            return null;
        }
        return fillTemplateContainers(f, 4);
    }

    /** What each container tagged {@code eotv_fill} in a template holds; {@code null} for the journal itself. */
    private static List<ItemStack> templateContents(String kind, Frame f) {
        RandomSource random = f.level.random;
        return switch (kind) {
            case "six" -> List.of(
                    new ItemStack(Items.IRON_INGOT, 23), new ItemStack(Items.COPPER_INGOT, 31), new ItemStack(Items.REDSTONE, 17),
                    new ItemStack(Items.GLASS, 32), new ItemStack(Items.PAPER, 9),
                    new ItemStack(UncannyItemRegistry.UNCANNY_REALITY_SHARD_PIECE.get(), 3));
            case "notes" -> List.of(
                    new ItemStack(Items.BOOK, 6), new ItemStack(Items.PAPER, 21), new ItemStack(Items.FEATHER, 7),
                    new ItemStack(Items.INK_SAC, 4), new ItemStack(Items.MAP, 2), new ItemStack(Items.WRITABLE_BOOK, 1));
            case "blocks" -> buildingBlocks(random);
            case "gear" -> randomEquipment(f);
            case "wood" -> List.of(
                    new ItemStack(Items.OAK_LOG, 64), new ItemStack(Items.OAK_LOG, 64), new ItemStack(Items.SPRUCE_LOG, 64),
                    new ItemStack(Items.SPRUCE_LOG, 41), new ItemStack(Items.DARK_OAK_LOG, 37), new ItemStack(Items.BIRCH_LOG, 22),
                    new ItemStack(Items.SPRUCE_PLANKS, 64), new ItemStack(Items.OAK_PLANKS, 50), new ItemStack(Items.STICK, 48),
                    new ItemStack(Items.OAK_SAPLING, 12), new ItemStack(Items.SPRUCE_SAPLING, 9), new ItemStack(Items.CHARCOAL, 30));
            case "drops" -> List.of(
                    new ItemStack(Items.BONE, 47), new ItemStack(Items.BONE_MEAL, 30), new ItemStack(Items.STRING, 38),
                    new ItemStack(Items.GUNPOWDER, 21), new ItemStack(Items.ROTTEN_FLESH, 64), new ItemStack(Items.ROTTEN_FLESH, 19),
                    new ItemStack(Items.ARROW, 44), new ItemStack(Items.SPIDER_EYE, 9), new ItemStack(Items.ENDER_PEARL, 3),
                    new ItemStack(Items.LEATHER, 14), new ItemStack(Items.FEATHER, 25));
            case "minerals" -> List.of(
                    new ItemStack(Items.COAL, 64), new ItemStack(Items.COAL, 64), new ItemStack(Items.COAL, 27),
                    new ItemStack(Items.RAW_IRON, 36), new ItemStack(Items.IRON_INGOT, 64), new ItemStack(Items.IRON_INGOT, 18),
                    new ItemStack(Items.GOLD_INGOT, 14), new ItemStack(Items.RAW_COPPER, 52), new ItemStack(Items.LAPIS_LAZULI, 27),
                    new ItemStack(Items.REDSTONE, 64), new ItemStack(Items.REDSTONE, 23), new ItemStack(Items.DIAMOND, 4),
                    new ItemStack(Items.EMERALD, 2), new ItemStack(Items.FLINT, 11));
            case "redstone" -> List.of(
                    new ItemStack(Items.PISTON, 12), new ItemStack(Items.STICKY_PISTON, 4), new ItemStack(Items.REPEATER, 9),
                    new ItemStack(Items.COMPARATOR, 3), new ItemStack(Items.OBSERVER, 6), new ItemStack(Items.HOPPER, 5),
                    new ItemStack(Items.REDSTONE_TORCH, 20), new ItemStack(Items.LEVER, 7), new ItemStack(Items.STONE_BUTTON, 11),
                    new ItemStack(Items.DISPENSER, 2), new ItemStack(Items.RAIL, 32), new ItemStack(Items.POWERED_RAIL, 6));
            case "food" -> List.of(
                    new ItemStack(Items.WHEAT, 64), new ItemStack(Items.WHEAT, 22), new ItemStack(Items.BREAD, 18),
                    new ItemStack(Items.CARROT, 40), new ItemStack(Items.POTATO, 52), new ItemStack(Items.COOKED_BEEF, 21),
                    new ItemStack(Items.WHEAT_SEEDS, 64), new ItemStack(Items.SUGAR_CANE, 30), new ItemStack(Items.PUMPKIN, 6),
                    new ItemStack(Items.EGG, 12), new ItemStack(Items.APPLE, 5));
            case "farm" -> List.of(
                    new ItemStack(Items.WHITE_WOOL, 40), new ItemStack(Items.LEATHER, 9), new ItemStack(Items.MELON_SEEDS, 14),
                    new ItemStack(Items.PUMPKIN_SEEDS, 8), new ItemStack(Items.BEETROOT_SEEDS, 21), new ItemStack(Items.FLOWER_POT, 4),
                    new ItemStack(Items.CAKE, 1), new ItemStack(Items.COOKIE, 16), new ItemStack(Items.HONEY_BOTTLE, 3));
            case "sand" -> List.of(new ItemStack(Items.SAND, 64), new ItemStack(Items.SAND, 64),
                    new ItemStack(Items.GLASS, 32), new ItemStack(Items.TORCH, 64));
            // Morgan's hall.
            case "stone" -> stoneBlocks(random);
            case "ores" -> List.of(
                    new ItemStack(Items.RAW_IRON, 64), new ItemStack(Items.RAW_IRON, 64), new ItemStack(Items.RAW_IRON, 29),
                    new ItemStack(Items.RAW_GOLD, 41), new ItemStack(Items.RAW_COPPER, 64), new ItemStack(Items.COAL, 64),
                    new ItemStack(Items.COAL, 64), new ItemStack(Items.LAPIS_LAZULI, 64), new ItemStack(Items.REDSTONE, 64),
                    new ItemStack(Items.REDSTONE, 64), new ItemStack(Items.DIAMOND, 9), new ItemStack(Items.EMERALD, 3),
                    new ItemStack(Items.AMETHYST_SHARD, 18), new ItemStack(Items.QUARTZ, 40));
            case "morgan_food" -> List.of(
                    new ItemStack(Items.COOKED_PORKCHOP, 64), new ItemStack(Items.COOKED_BEEF, 48), new ItemStack(Items.BREAD, 32),
                    new ItemStack(Items.GOLDEN_CARROT, 24), new ItemStack(Items.BAKED_POTATO, 40), new ItemStack(Items.WHEAT, 64),
                    new ItemStack(Items.CARROT, 64), new ItemStack(Items.POTATO, 64), new ItemStack(Items.WHEAT_SEEDS, 64));
            case "morgan_drops" -> List.of(
                    new ItemStack(Items.BONE, 64), new ItemStack(Items.STRING, 64), new ItemStack(Items.GUNPOWDER, 40),
                    new ItemStack(Items.ARROW, 64), new ItemStack(Items.ENDER_PEARL, 6), new ItemStack(Items.SLIME_BALL, 13),
                    new ItemStack(Items.ROTTEN_FLESH, 64), new ItemStack(Items.SPIDER_EYE, 22), new ItemStack(Items.PHANTOM_MEMBRANE, 5));
            case "building" -> List.of(
                    new ItemStack(Items.STONE_BRICKS, 64), new ItemStack(Items.DEEPSLATE_BRICKS, 64), new ItemStack(Items.SPRUCE_LOG, 64),
                    new ItemStack(Items.SPRUCE_PLANKS, 64), new ItemStack(Items.GLASS, 64), new ItemStack(Items.LADDER, 32),
                    new ItemStack(Items.RAIL, 40));
            case "tools" -> List.of(
                    worn(Items.IRON_PICKAXE, 0.5F, f), enchanted(Items.IRON_PICKAXE, f), enchanted(Items.BOW, f),
                    worn(Items.CROSSBOW, 0.3F, f), worn(Items.SHIELD, 0.4F, f), worn(Items.SHEARS, 0.2F, f),
                    worn(Items.FLINT_AND_STEEL, 0.5F, f), new ItemStack(Items.BUCKET, 3), new ItemStack(Items.LAVA_BUCKET, 1),
                    enchanted(Items.BOOK, f), enchanted(Items.BOOK, f), new ItemStack(Items.SADDLE, 1),
                    new ItemStack(Items.NAME_TAG, 2), new ItemStack(Items.SPYGLASS, 1), new ItemStack(Items.COMPASS, 1));
            case "torches" -> List.of(new ItemStack(Items.TORCH, 64), new ItemStack(Items.TORCH, 64),
                    new ItemStack(Items.TORCH, 64), new ItemStack(Items.TORCH, 41));
            case "nether" -> List.of(
                    new ItemStack(Items.OBSIDIAN, 5), new ItemStack(Items.BLAZE_ROD, 4), new ItemStack(Items.NETHER_WART, 23),
                    new ItemStack(Items.MAGMA_CREAM, 3), new ItemStack(Items.GLOWSTONE_DUST, 17), new ItemStack(Items.NETHERRACK, 64));
            case "potions" -> List.of(
                    PotionContents.createItemStack(Items.POTION, Potions.NIGHT_VISION),
                    PotionContents.createItemStack(Items.POTION, Potions.NIGHT_VISION),
                    PotionContents.createItemStack(Items.POTION, Potions.FIRE_RESISTANCE),
                    new ItemStack(Items.GLASS_BOTTLE, 9), new ItemStack(Items.NETHER_WART, 12), new ItemStack(Items.BLAZE_POWDER, 6));
            default -> List.of();
        };
    }

    /**
     * Fills the containers a template tagged {@code eotv_fill}: a double chest from its top half, like a player would
     * (in order, a gap here and there), and flags every container of the place. Returns the journal's holder.
     */
    private static BlockPos fillTemplateContainers(Frame f, int volume) {
        BlockPos holder = null;
        for (BlockEntity blockEntity : blockEntitiesAround(f.level, f.origin, 2, MAX_HEIGHT_ABOVE)) {
            if (!blockEntity.getPersistentData().contains(TEMPLATE_FILL_TAG)) {
                continue;
            }
            String kind = blockEntity.getPersistentData().getString(TEMPLATE_FILL_TAG);
            blockEntity.getPersistentData().remove(TEMPLATE_FILL_TAG);
            if ("journal".equals(kind)) {
                holder = holdJournal(f.level, blockEntity, volume);
                continue;
            }
            Container container = topFirst(f.level, blockEntity);
            if (container != null) {
                fillLikeAPlayer(f.level.random, container, templateContents(kind, f), 0.12F);
            }
        }
        return holder;
    }

    /** Puts the journal on a tagged lectern, or in a tagged chest. */
    private static BlockPos holdJournal(ServerLevel level, BlockEntity blockEntity, int volume) {
        BlockPos pos = blockEntity.getBlockPos();
        if (blockEntity instanceof LecternBlockEntity desk) {
            desk.setBook(createJournal(volume));
            desk.getPersistentData().putInt(JOURNAL_PLACE_CHEST_FLAG, volume);
            desk.setChanged();
            level.setBlock(pos, level.getBlockState(pos).setValue(LecternBlock.HAS_BOOK, true), 2);
            return pos;
        }
        return placeJournal(level, pos, volume, false);
    }

    /** A chest as a player sees it: a double chest's top half first; every chest is flagged as the place's. */
    private static Container topFirst(ServerLevel level, BlockEntity blockEntity) {
        if (!(blockEntity instanceof Container container)) {
            return null;
        }
        blockEntity.getPersistentData().putBoolean(PLACE_CONTAINER_FLAG, true);
        BlockState state = blockEntity.getBlockState();
        if (!(blockEntity instanceof ChestBlockEntity) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return container;
        }
        BlockPos partnerPos = blockEntity.getBlockPos().relative(ChestBlock.getConnectedDirection(state));
        if (!(level.getBlockEntity(partnerPos) instanceof ChestBlockEntity partner)) {
            return container;
        }
        partner.getPersistentData().putBoolean(PLACE_CONTAINER_FLAG, true);
        // The RIGHT half is the top of the double chest screen.
        return state.getValue(ChestBlock.TYPE) == ChestType.RIGHT
                ? new CompoundContainer(container, partner) : new CompoundContainer(partner, container);
    }

    /** A chest nearly full of the blocks a builder digs out: cobblestone above all, deepslate and dirt. */
    private static List<ItemStack> buildingBlocks(RandomSource random) {
        List<ItemStack> stacks = new ArrayList<>();
        int cobble = 22 + random.nextInt(6);
        int deepslate = 10 + random.nextInt(5);
        int dirt = 6 + random.nextInt(4);
        for (int i = 0; i < cobble; i++) {
            stacks.add(new ItemStack(Items.COBBLESTONE, 64));
        }
        for (int i = 0; i < deepslate; i++) {
            stacks.add(new ItemStack(Items.COBBLED_DEEPSLATE, 64));
        }
        for (int i = 0; i < dirt; i++) {
            stacks.add(new ItemStack(Items.DIRT, 64));
        }
        stacks.add(new ItemStack(Items.GRAVEL, 37));
        stacks.add(new ItemStack(Items.ANDESITE, 29));
        stacks.add(new ItemStack(Items.DIRT, 1 + random.nextInt(63)));
        return stacks;
    }

    /**
     * Gear a player piles up: fishing treasure, the odd find from a dungeon or a mineshaft, and a full enchanted iron
     * armour set they never wore.
     */
    private static List<ItemStack> randomEquipment(Frame f) {
        List<ItemStack> gear = new ArrayList<>();
        ServerLevel level = f.level;
        Vec3 origin = Vec3.atCenterOf(f.origin);
        LootParams fishing = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD)).create(LootContextParamSets.FISHING);
        for (int i = 0; i < 3; i++) {
            gear.addAll(loot(level, BuiltInLootTables.FISHING_TREASURE).getRandomItems(fishing));
        }
        LootParams chest = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin)
                .create(LootContextParamSets.CHEST);
        for (ResourceKey<LootTable> table : List.of(BuiltInLootTables.SIMPLE_DUNGEON, BuiltInLootTables.ABANDONED_MINESHAFT)) {
            for (ItemStack found : loot(level, table).getRandomItems(chest)) {
                if (found.getMaxStackSize() == 1 || found.is(Items.GOLDEN_APPLE)) {
                    gear.add(found);
                }
            }
        }
        for (Item piece : List.of(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS)) {
            ItemStack armour = EnchantmentHelper.enchantItem(level.random, new ItemStack(piece), 12 + level.random.nextInt(14),
                    level.registryAccess(), Optional.empty());
            armour.setDamageValue(level.random.nextInt(Math.max(1, armour.getMaxDamage() / 5)));
            gear.add(armour);
        }
        gear.add(worn(Items.SHIELD, 0.3F, f));
        gear.add(worn(Items.IRON_SWORD, 0.25F, f));
        return gear;
    }

    private static LootTable loot(ServerLevel level, ResourceKey<LootTable> key) {
        return level.getServer().reloadableRegistries().getLootTable(key);
    }

    // ---------------------------------------------------------------- 5. Under the Third Warning (Morgan, miner)

    /**
     * The largest base by far. The surface is a StructureForge template
     * ({@code Ressources/Structures/journal_place_morgan.py}): a walled compound with a stone hall, a tower over the
     * shaft (stairs up to its roof), a cold nether portal and a farm. The mine depends on the site's depth and is dug
     * here: a spiral down to the deep dark, a bare room with the kit chest and the journal, and the one old tunnel
     * Morgan walled off and marked with redstone, sculk still listening behind it.
     */
    private static BlockPos buildMorgan(Frame f) {
        int ub = morganBottomUp(f.level, f.origin);
        if (ub == Integer.MIN_VALUE || !f.template(MORGAN_TEMPLATE, MORGAN_ANCHOR)) {
            return null;
        }
        fillTemplateContainers(f, 5);
        return buildMorganMine(f, ub);
    }

    /** The spiral down, the room and the walled tunnel. Returns the kit chest holding the journal. */
    private static BlockPos buildMorganMine(Frame f, int ub) {
        int c = SHAFT_FW;
        int d = SHAFT_R;
        // The shaft is lined only where the ground is open; the corridor, the room and the tunnel are dressed in
        // deepslate all round, as dug deep below the stone.
        f.encaseAndCarve(morganInteriors(ub), -1, 1);
        BlockState landing = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        // The first stair lies in the tower floor, flush with it; each next one a block lower round the ring.
        int stepCount = -ub;
        for (int i = 0; i < stepCount; i++) {
            int[] cell = SHAFT_RING[i % SHAFT_RING.length];
            int[] previous = i == 0 ? new int[] {2, 0} : SHAFT_RING[(i - 1) % SHAFT_RING.length];
            Direction ascent = f.localDirection(previous[0] - cell[0], previous[1] - cell[1]);
            int up = -1 - i;
            f.set(c + cell[0], up, d + cell[1], stairs(f.at(0, up, 0).getY() < DEEPSLATE_LEVEL
                    ? Blocks.COBBLED_DEEPSLATE_STAIRS : Blocks.STONE_BRICK_STAIRS, ascent));
            if (i % SHAFT_RING.length == 4) {
                f.wallTorch(c + cell[0], up + 2, d + cell[1], f.back());
            }
        }
        // The last cell is a flat landing at the level of the corridor floor, never a stair beside a block.
        f.set(c + SHAFT_RING[0][0], ub - 1, d + SHAFT_RING[0][1], landing);
        for (int up = ub - 1; up <= -1; up++) {
            f.set(c, up, d, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        }
        f.set(c, 0, d, Blocks.LANTERN.defaultBlockState());

        // Corridor and the bare room: only the kit chest, and light.
        for (int a = 2; a <= 4; a++) {
            f.set(c + a, ub - 1, d, landing);
        }
        for (int a = 5; a <= 13; a++) {
            for (int b = -3; b <= 3; b++) {
                f.set(c + a, ub - 1, d + b, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            }
        }
        f.wallTorch(c + 3, ub + 1, d, f.left());
        f.hangingLantern(c + 7, ub + 3, d);
        f.hangingLantern(c + 11, ub + 3, d);
        BlockPos kit = f.chest(c + 6, ub, d - 3, f.right());
        fillLikeAPlayer(f.level.random, (Container) f.level.getBlockEntity(kit), List.of(
                worn(Items.IRON_PICKAXE, 0.6F, f), new ItemStack(Items.TORCH, 64), new ItemStack(Items.TORCH, 64),
                new ItemStack(Items.WATER_BUCKET, 1), new ItemStack(Items.SNOWBALL, 16), new ItemStack(Items.GRAY_WOOL, 12),
                new ItemStack(Items.REDSTONE, 9), new ItemStack(Items.COOKED_PORKCHOP, 11)), 0.0F);

        // The old tunnel, walled off and marked; behind the wall the sculk still answers footsteps.
        f.wallTorch(c + 16, ub + 1, d, f.left());
        for (int up = ub; up <= ub + 2; up++) {
            f.set(c + 21, up, d, Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        }
        f.set(c + 20, ub, d, Blocks.REDSTONE_WIRE.defaultBlockState());
        for (int a = 22; a <= 23; a++) {
            for (int b = -1; b <= 1; b++) {
                for (int up = ub - 1; up <= ub + 2; up++) {
                    f.set(c + a, up, d + b, Blocks.SCULK.defaultBlockState());
                }
            }
        }
        f.set(c + 22, ub, d, Blocks.SCULK_SENSOR.defaultBlockState());
        f.set(c + 23, ub, d, Blocks.SCULK_SHRIEKER.defaultBlockState());
        return placeJournal(f.level, kit, 5, false);
    }

    private static List<ItemStack> stoneBlocks(RandomSource random) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < 24 + random.nextInt(6); i++) {
            stacks.add(new ItemStack(Items.COBBLED_DEEPSLATE, 64));
        }
        for (int i = 0; i < 12 + random.nextInt(4); i++) {
            stacks.add(new ItemStack(Items.COBBLESTONE, 64));
        }
        stacks.add(new ItemStack(Items.TUFF, 64));
        stacks.add(new ItemStack(Items.TUFF, 51));
        stacks.add(new ItemStack(Items.DIORITE, 33));
        stacks.add(new ItemStack(Items.GRANITE, 40));
        stacks.add(new ItemStack(Items.DIRT, 64));
        stacks.add(new ItemStack(Items.GRAVEL, 22));
        return stacks;
    }

    /** Local up of the feet at the bottom of Morgan's mine, or {@link Integer#MIN_VALUE} where it cannot go deep enough. */
    static int morganBottomUp(ServerLevel level, BlockPos origin) {
        int y0 = origin.getY();
        int target = Math.max(MORGAN_TARGET_FEET_Y, Math.max(y0 - MORGAN_MAX_DEPTH, level.getMinBuildHeight() + 16));
        // One block down per stair, whole turns of the spiral: the landing lies under the first stair, towards the hub.
        int ring = SHAFT_RING.length;
        int steps = Math.max(ring, (y0 - target + ring - 1) / ring * ring);
        if (steps < MORGAN_MIN_DEPTH || y0 - steps - 5 <= level.getMinBuildHeight()) {
            return Integer.MIN_VALUE;
        }
        return -steps;
    }

    /** Morgan's shaft centre in the world for a given door side (tests and diagnostics). */
    public static BlockPos morganShaftCentre(BlockPos origin, Direction facing) {
        return origin.relative(facing, SHAFT_FW).relative(facing.getClockWise(), SHAFT_R);
    }

    /** The air of Morgan's mine in the local frame: shaft, corridor, hub, the three tunnels and the water pit. */
    private static List<Box> morganInteriors(int ub) {
        int c = SHAFT_FW;
        int d = SHAFT_R;
        return List.of(
                new Box(c - 1, c + 1, ub - 1, -1, d - 1, d + 1),
                new Box(c + 2, c + 4, ub, ub + 2, d, d),
                new Box(c + 5, c + 13, ub, ub + 3, d - 3, d + 3),
                new Box(c + 14, c + 20, ub, ub + 2, d, d));
    }

    /** The mine must fit: deep enough above the world floor, no vanilla structure, nothing protected in its rock. */
    private static String morganUndergroundReason(ServerLevel level, BlockPos origin, Direction facing) {
        int ub = morganBottomUp(level, origin);
        if (ub == Integer.MIN_VALUE) {
            return "depth";
        }
        Frame f = new Frame(level, origin, facing);
        List<Box> interiors = new ArrayList<>(morganInteriors(ub));
        // The sculk pocket behind the wall.
        interiors.add(new Box(SHAFT_FW + 21, SHAFT_FW + 23, ub - 1, ub + 2, SHAFT_R - 1, SHAFT_R + 1));
        for (Box box : interiors) {
            for (int up = box.up1(); up >= box.up0(); up -= 6) {
                if (hasStructurePiece(level, f.at((box.fw0() + box.fw1()) / 2, up, (box.r0() + box.r1()) / 2))
                        || hasStructurePiece(level, f.at(box.fw0(), up, box.r0()))
                        || hasStructurePiece(level, f.at(box.fw1(), up, box.r1()))) {
                    return "vanilla-structure";
                }
            }
            for (int fw = box.fw0() - 1; fw <= box.fw1() + 1; fw++) {
                for (int r = box.r0() - 1; r <= box.r1() + 1; r++) {
                    for (int up = box.up0() - 1; up <= Math.min(box.up1() + 1, -2); up++) {
                        BlockPos pos = f.at(fw, up, r);
                        if (isProtectedUnderground(level, pos, level.getBlockState(pos))) {
                            return "protected-block";
                        }
                    }
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- 6. only dan heard it (Casey and friends)

    /** A house built together on day one: three beds, an armor stand in the corner, wheat out front. */
    private static BlockPos buildCasey(Frame f) {
        BlockState logs = Blocks.OAK_LOG.defaultBlockState();
        BlockState planks = Blocks.BIRCH_PLANKS.defaultBlockState();
        f.box(-5, 1, -3, 3, 0, 4, (fw, up, r) -> (fw == -5 || fw == 1) && (r == -3 || r == 3) ? logs : planks, true);
        f.floor(-4, 0, -2, 2, Blocks.OAK_PLANKS.defaultBlockState());
        f.gableRoof(true, -3, 3, -5, 1, 4, Blocks.OAK_STAIRS, planks, slab(Blocks.OAK_SLAB), true, true);
        f.set(-2, 1, -3, pane());
        f.set(-2, 1, 3, pane());
        f.set(1, 2, 2, pane());
        f.door(1, -1, Blocks.OAK_DOOR);
        // Pillows against the back wall.
        f.bed(-3, 0, -2, f.back(), Blocks.RED_BED);
        f.bed(-3, 0, 0, f.back(), Blocks.YELLOW_BED);
        f.bed(-3, 0, 2, f.back(), Blocks.GREEN_BED);
        f.wallTorch(-1, 2, -2, f.right());
        f.wallTorch(-1, 2, 2, f.left());
        f.set(0, 0, -2, Blocks.CRAFTING_TABLE.defaultBlockState());
        f.armorStand(0, 0, 2, List.of(new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE)));
        BlockPos chest = f.chest(-1, 0, -2, f.left().getOpposite());
        fillMessy(f.level, chest, List.of(
                new ItemStack(Items.ARROW, 23), new ItemStack(Items.ARROW, 9), new ItemStack(Items.WHEAT, 19),
                new ItemStack(Items.WHEAT, 64), new ItemStack(Items.LEATHER_HELMET, 1), new ItemStack(Items.LEATHER_BOOTS, 1),
                worn(Items.BOW, 0.5F, f), new ItemStack(Items.BREAD, 5), new ItemStack(Items.BONE, 3),
                new ItemStack(Items.STRING, 6), new ItemStack(Items.COBBLESTONE, 40), new ItemStack(Items.CARROT, 7)));
        // Wheat field in front, with its water line.
        for (int fw = 3; fw <= 6; fw++) {
            for (int r = -4; r <= 4; r++) {
                boolean water = r == 0 && fw <= 5;
                f.set(fw, -1, r, water ? Blocks.WATER.defaultBlockState()
                        : Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
                if (!water) {
                    int age = Math.min(CropBlock.MAX_AGE, 3 + f.level.random.nextInt(5));
                    f.set(fw, 0, r, ((CropBlock) Blocks.WHEAT).getStateForAge(age));
                }
            }
        }
        return placeJournal(f.level, chest, 6, true);
    }

    // ---------------------------------------------------------------- chests

    /** Puts the journal in the chest where its owner left it: after their things, as the last stack put away. */
    private static BlockPos placeJournal(ServerLevel level, BlockPos chestPos, int volume, boolean afterAGap) {
        if (chestPos == null || !(level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest)) {
            return null;
        }
        List<Integer> free = UncannyJournalMaps.freeSlots(chest);
        if (free.isEmpty()) {
            return null;
        }
        int last = -1;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            if (!chest.getItem(slot).isEmpty()) {
                last = slot;
            }
        }
        int wanted = last + (afterAGap ? 2 : 1);
        int slot = free.stream().filter(s -> s >= wanted).findFirst().orElse(free.get(0));
        chest.setItem(slot, createJournal(volume));
        chest.getPersistentData().putInt(JOURNAL_PLACE_CHEST_FLAG, volume);
        chest.setChanged();
        return chestPos;
    }

    /** A careful player: stacks side by side from the first slot. */
    private static void fillTidy(ServerLevel level, BlockPos chestPos, List<ItemStack> items) {
        if (level.getBlockEntity(chestPos) instanceof Container chest) {
            fillLikeAPlayer(level.random, chest, items, 0.0F);
        }
    }

    /** A careless player: the same order, but with holes where something was taken out. */
    private static void fillMessy(ServerLevel level, BlockPos chestPos, List<ItemStack> items) {
        if (level.getBlockEntity(chestPos) instanceof Container chest) {
            fillLikeAPlayer(level.random, chest, items, 0.3F);
        }
    }

    /**
     * Fills a chest the way a player keeps it: stacks in order from the first slot, top row first, with an empty
     * slot left now and then ({@code gapChance} before each stack). Never scattered slot by slot.
     */
    static void fillLikeAPlayer(RandomSource random, Container chest, List<ItemStack> items, float gapChance) {
        if (chest == null) {
            return;
        }
        int slot = 0;
        for (ItemStack item : items) {
            if (slot > 0 && random.nextFloat() < gapChance) {
                slot++;
            }
            if (slot >= chest.getContainerSize()) {
                return;
            }
            chest.setItem(slot++, item);
        }
    }

    private static ItemStack worn(Item item, float usedFraction, Frame frame) {
        ItemStack stack = new ItemStack(item);
        if (stack.isDamageableItem()) {
            float jitter = 0.9F + frame.level.random.nextFloat() * 0.2F;
            stack.setDamageValue(Math.min(stack.getMaxDamage() - 1, Math.round(stack.getMaxDamage() * usedFraction * jitter)));
        }
        return stack;
    }

    private static ItemStack enchanted(Item item, Frame frame) {
        ServerLevel level = frame.level;
        ItemStack stack = EnchantmentHelper.enchantItem(level.random, new ItemStack(item), 10 + level.random.nextInt(16),
                level.registryAccess(), Optional.empty());
        if (stack.isDamageableItem()) {
            stack.setDamageValue(level.random.nextInt(Math.max(1, stack.getMaxDamage() / 3)));
        }
        return stack;
    }

    private static BlockState slab(Block block) {
        return block.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    private static BlockState stairs(Block block, Direction ascent) {
        return block.defaultBlockState().setValue(StairBlock.FACING, ascent).setValue(StairBlock.HALF, Half.BOTTOM);
    }

    private static BlockState pane() {
        return Blocks.GLASS_PANE.defaultBlockState();
    }

    /** A box of the local frame, bounds included. */
    private record Box(int fw0, int fw1, int up0, int up1, int r0, int r1) {
        boolean contains(int fw, int up, int r) {
            return fw >= fw0 && fw <= fw1 && up >= up0 && up <= up1 && r >= r0 && r <= r1;
        }
    }

    // ---------------------------------------------------------------- local frame

    /** Local drawing frame: {@code at(forward, up, right)} relative to the origin, door side = forward. */
    private static final class Frame {
        private final ServerLevel level;
        private final BlockPos origin;
        private final Direction forward;
        private final List<BlockPos> shaped = new ArrayList<>();

        @FunctionalInterface
        private interface Palette {
            BlockState at(int forward, int up, int right);
        }

        private Frame(ServerLevel level, BlockPos origin, Direction forward) {
            this.level = level;
            this.origin = origin;
            this.forward = forward;
        }

        Direction forward() {
            return forward;
        }

        Direction back() {
            return forward.getOpposite();
        }

        Direction right() {
            return forward.getClockWise();
        }

        Direction left() {
            return forward.getCounterClockWise();
        }

        /** World direction of a unit step in the local frame. */
        Direction localDirection(int dfw, int dr) {
            if (dfw > 0) {
                return forward();
            }
            if (dfw < 0) {
                return back();
            }
            return dr > 0 ? right() : left();
        }

        /** Places a StructureForge template on this frame (its script origin one block under the place origin). */
        boolean template(String name, BlockPos anchorLocal) {
            return StructureForgeTemplates.place(level, name, origin.below(), anchorLocal, forward) != null;
        }

        BlockPos at(int fw, int up, int r) {
            return origin.relative(forward, fw).relative(right(), r).above(up);
        }

        void set(int fw, int up, int r, BlockState state) {
            BlockPos pos = at(fw, up, r);
            level.setBlock(pos, state, 2);
            if (needsShape(state)) {
                shaped.add(pos);
            }
        }

        /**
         * Levels the ground like a player would: the footprint is cut or filled to the origin height, then a margin
         * blends it back into the terrain; the local topsoil is kept and what stands above is cleared.
         */
        void prepareGround(int radius, int margin) {
            BlockState topsoil = dominantTopsoil(radius + margin);
            BlockState filler = topsoil.is(Blocks.SAND) ? Blocks.SANDSTONE.defaultBlockState()
                    : topsoil.is(Blocks.RED_SAND) ? Blocks.RED_SANDSTONE.defaultBlockState() : Blocks.DIRT.defaultBlockState();
            int base = origin.getY();
            int reach = radius + margin;
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    int natural = groundHeight(level, x, z);
                    int ring = Math.max(Math.abs(dx), Math.abs(dz)) - radius;
                    int target = base;
                    if (ring > 0) {
                        BlockPos naturalTop = new BlockPos(x, natural - 1, z);
                        BlockPos standing = naturalTop.above();
                        if (!level.getFluidState(naturalTop).isEmpty() || level.getBlockState(standing).is(BlockTags.LOGS)) {
                            continue;
                        }
                        target = base + (int) Math.round((natural - base) * ring / (double) (margin + 1));
                        if (target == natural) {
                            continue;
                        }
                    }
                    int clearTop = ring > 0 ? Math.max(natural, target) + 1 : Math.max(natural, target) + TREE_CLEAR_HEIGHT;
                    for (int y = target; y <= clearTop; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (state.isAir() || isProtectedTerrain(level, pos, state)) {
                            continue;
                        }
                        boolean terrain = y < natural;
                        boolean vegetation = state.canBeReplaced() && level.getFluidState(pos).isEmpty();
                        boolean overFootprint = ring <= 0
                                && (y <= Math.max(natural, target) + CLEAR_HEIGHT || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES));
                        if (terrain || vegetation || overFootprint) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                    BlockPos surface = new BlockPos(x, target - 1, z);
                    if (!isProtectedTerrain(level, surface, level.getBlockState(surface))) {
                        level.setBlock(surface, topsoil, 2);
                    }
                    int depthLimit = ring > 0 ? target - natural + 1 : maxFill(target, natural);
                    for (int depth = 2; depth <= depthLimit; depth++) {
                        BlockPos pos = new BlockPos(x, target - depth, z);
                        if (level.getBlockState(pos).isSolidRender(level, pos) && level.getFluidState(pos).isEmpty()) {
                            break;
                        }
                        level.setBlock(pos, filler, 2);
                    }
                }
            }
        }

        /**
         * The surface block most of the area shows (grass, sand, snow, podzol...): the block under the origin itself
         * may be buried dirt once the origin is levelled to the median height.
         */
        private BlockState dominantTopsoil(int reach) {
            java.util.Map<BlockState, Integer> counts = new java.util.HashMap<>();
            for (int dx = -reach; dx <= reach; dx += 2) {
                for (int dz = -reach; dz <= reach; dz += 2) {
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    BlockState top = level.getBlockState(new BlockPos(x, groundHeight(level, x, z) - 1, z));
                    BlockState soil = naturalTopsoil(top);
                    if (soil == top) {
                        counts.merge(soil, 1, Integer::sum);
                    }
                }
            }
            return counts.entrySet().stream().max(java.util.Map.Entry.comparingByValue())
                    .map(java.util.Map.Entry::getKey).orElse(Blocks.GRASS_BLOCK.defaultBlockState());
        }

        private static int maxFill(int target, int natural) {
            return Math.max(target - natural, 0) + 4;
        }

        /** Hollow walls of the given height, solid floor under them. */
        void box(int f0, int f1, int r0, int r1, int up0, int height, Palette walls, boolean hollow) {
            for (int fw = f0; fw <= f1; fw++) {
                for (int r = r0; r <= r1; r++) {
                    boolean edge = fw == f0 || fw == f1 || r == r0 || r == r1;
                    for (int up = up0; up < up0 + height; up++) {
                        if (edge || !hollow) {
                            set(fw, up, r, walls.at(fw, up, r));
                        }
                    }
                }
            }
        }

        void floor(int f0, int f1, int r0, int r1, BlockState state) {
            fill(f0, f1, -1, r0, r1, state);
        }

        void fill(int f0, int f1, int up, int r0, int r1, BlockState state) {
            for (int fw = f0; fw <= f1; fw++) {
                for (int r = r0; r <= r1; r++) {
                    set(fw, up, r, state);
                }
            }
        }

        void roof(int f0, int f1, int r0, int r1, int up, BlockState slab) {
            for (int fw = f0 - 1; fw <= f1 + 1; fw++) {
                for (int r = r0 - 1; r <= r1 + 1; r++) {
                    set(fw, up, r, slab);
                }
            }
        }

        /**
         * Two-sided roof over walls {@code s0..s1} of the sloped axis and {@code l0..l1} of the ridge axis, starting at
         * height {@code up} just above the walls. The ridge runs along forward ({@code ridgeAlongForward}, slopes towards
         * the left and right walls) or along right (slopes towards the front and back walls). An odd span ends in a
         * slab ridge, an even one in two meeting rows of stairs; the gable ends are closed with {@code wall}. A side
         * without overhang abuts a taller wall.
         */
        void gableRoof(boolean ridgeAlongForward, int s0, int s1, int l0, int l1, int up, Block stairs, BlockState wall,
                BlockState ridge, boolean overhangLow, boolean overhangHigh) {
            Direction upslope = ridgeAlongForward ? right() : forward();
            BlockState rising = UncannyJournalPlaces.stairs(stairs, upslope);
            BlockState falling = UncannyJournalPlaces.stairs(stairs, upslope.getOpposite());
            int lFrom = overhangLow ? l0 - 1 : l0;
            int lTo = overhangHigh ? l1 + 1 : l1;
            for (int k = 0; ; k++) {
                int low = s0 - 1 + k;
                int high = s1 + 1 - k;
                if (low > high) {
                    return;
                }
                for (int l = lFrom; l <= lTo; l++) {
                    if (low == high) {
                        roofSet(ridgeAlongForward, low, up + k, l, ridge);
                        continue;
                    }
                    roofSet(ridgeAlongForward, low, up + k, l, rising);
                    roofSet(ridgeAlongForward, high, up + k, l, falling);
                }
                if (low + 1 <= high - 1) {
                    // Solid row under the next course of stairs, so no gap opens between wall and roof.
                    for (int l = l0; l <= l1; l++) {
                        roofSet(ridgeAlongForward, low + 1, up + k, l, wall);
                        roofSet(ridgeAlongForward, high - 1, up + k, l, wall);
                    }
                }
                for (int s = low + 2; s <= high - 2; s++) {
                    roofSet(ridgeAlongForward, s, up + k, l0, wall);
                    roofSet(ridgeAlongForward, s, up + k, l1, wall);
                }
            }
        }

        private void roofSet(boolean ridgeAlongForward, int s, int up, int l, BlockState state) {
            if (ridgeAlongForward) {
                set(l, up, s, state);
            } else {
                set(s, up, l, state);
            }
        }

        /** A front door in the wall at {@code fw}, opening inwards. */
        void door(int fw, int r, Block door) {
            door(fw, 0, r, door, back(), DoorHingeSide.LEFT, false);
        }

        void door(int fw, int up, int r, Block door, Direction facing, DoorHingeSide hinge, boolean open) {
            BlockState lower = door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HINGE, hinge)
                    .setValue(DoorBlock.OPEN, open).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
            set(fw, up, r, lower);
            set(fw, up + 1, r, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        }

        void wallTorch(int fw, int up, int r, Direction pointing) {
            set(fw, up, r, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, pointing));
        }

        void hangingLantern(int fw, int up, int r) {
            set(fw, up, r, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }

        /** Foot at (fw, up, r); the head lies one block towards {@code headward}. */
        void bed(int fw, int up, int r, Direction headward, Block bed) {
            BlockPos foot = at(fw, up, r);
            BlockState state = bed.defaultBlockState().setValue(BedBlock.FACING, headward);
            level.setBlock(foot, state.setValue(BedBlock.PART, BedPart.FOOT), 2);
            level.setBlock(foot.relative(headward), state.setValue(BedBlock.PART, BedPart.HEAD), 2);
        }

        BlockPos chest(int fw, int up, int r, Direction front) {
            BlockPos pos = at(fw, up, r);
            level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, front), 2);
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                chest.clearContent();
                chest.getPersistentData().putBoolean(PLACE_CONTAINER_FLAG, true);
                chest.setChanged();
            }
            return pos;
        }

        /**
         * Lines the given air spaces with rock where the ground is open, wet or loose (caves, aquifers, gravel), then
         * hollows them: the mine stays dry and sealed whatever it crosses. Nothing above {@code maxShellUp} is lined.
         * The boxes from {@code dressedFrom} on are lined all round with deepslate, whatever the rock was.
         */
        void encaseAndCarve(List<Box> interiors, int maxShellUp, int dressedFrom) {
            for (int index = 0; index < interiors.size(); index++) {
                Box box = interiors.get(index);
                boolean dressed = index >= dressedFrom;
                for (int fw = box.fw0() - 1; fw <= box.fw1() + 1; fw++) {
                    for (int r = box.r0() - 1; r <= box.r1() + 1; r++) {
                        for (int up = box.up0() - 1; up <= Math.min(box.up1() + 1, maxShellUp); up++) {
                            if (inside(interiors, fw, up, r)) {
                                continue;
                            }
                            BlockPos pos = at(fw, up, r);
                            BlockState state = level.getBlockState(pos);
                            if (dressed) {
                                level.setBlock(pos, Blocks.DEEPSLATE.defaultBlockState(), 2);
                            } else if (state.isAir() || !level.getFluidState(pos).isEmpty()
                                    || !state.isSolidRender(level, pos) || state.getBlock() instanceof FallingBlock) {
                                level.setBlock(pos, pos.getY() < DEEPSLATE_LEVEL ? Blocks.COBBLED_DEEPSLATE.defaultBlockState()
                                        : Blocks.COBBLESTONE.defaultBlockState(), 2);
                            }
                        }
                    }
                }
            }
            for (Box box : interiors) {
                for (int fw = box.fw0(); fw <= box.fw1(); fw++) {
                    for (int r = box.r0(); r <= box.r1(); r++) {
                        for (int up = box.up0(); up <= box.up1(); up++) {
                            level.setBlock(at(fw, up, r), Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }

        private static boolean inside(List<Box> boxes, int fw, int up, int r) {
            for (Box box : boxes) {
                if (box.contains(fw, up, r)) {
                    return true;
                }
            }
            return false;
        }

        void birch(int fw, int r) {
            int height = 5 + level.random.nextInt(2);
            for (int up = 0; up < height; up++) {
                set(fw, up, r, Blocks.BIRCH_LOG.defaultBlockState());
            }
            for (int up = height - 3; up <= height; up++) {
                int fromTop = up - height;
                int spread = 1 - fromTop / 2;
                for (int a = -spread; a <= spread; a++) {
                    for (int b = -spread; b <= spread; b++) {
                        boolean corner = Math.abs(a) == spread && Math.abs(b) == spread;
                        if (corner && (fromTop == 0 || level.random.nextBoolean())) {
                            continue;
                        }
                        BlockPos pos = at(fw + a, up, r + b);
                        if (level.getBlockState(pos).isAir()) {
                            level.setBlock(pos, Blocks.BIRCH_LEAVES.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }

        /** A painting hung on the wall behind (fw, up, r), facing {@code facing}. */
        void painting(int fw, int up, int r, Direction facing, ResourceKey<PaintingVariant> variant) {
            Holder<PaintingVariant> holder = level.registryAccess().registryOrThrow(Registries.PAINTING_VARIANT)
                    .getHolderOrThrow(variant);
            Painting painting = new Painting(level, at(fw, up, r), facing, holder);
            if (painting.survives()) {
                level.addFreshEntity(painting);
            }
        }

        void cow(int fw, int r) {
            Cow cow = EntityType.COW.create(level);
            if (cow == null) {
                return;
            }
            BlockPos pos = at(fw, 0, r);
            cow.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            cow.setPersistenceRequired();
            level.addFreshEntity(cow);
        }

        /** An armor stand facing the door, wearing the pieces given from the head down. */
        void armorStand(int fw, int up, int r, List<ItemStack> pieces) {
            ArmorStand stand = EntityType.ARMOR_STAND.create(level);
            if (stand == null) {
                return;
            }
            BlockPos pos = at(fw, up, r);
            stand.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, back().toYRot(), 0.0F);
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (int i = 0; i < pieces.size() && i < slots.length; i++) {
                stand.setItemSlot(slots[i], pieces.get(i));
            }
            level.addFreshEntity(stand);
        }

        /**
         * In a cold biome the levelling removed the snow cover: lay it back on every exposed top (ground and roofs) so
         * the footprint does not show as a bare square.
         */
        void dustWithSnow(int radius) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
                    if (level.getBlockState(top).isAir()
                            && level.getBiome(top).value().coldEnoughToSnow(top)
                            && Blocks.SNOW.defaultBlockState().canSurvive(level, top)) {
                        level.setBlock(top, Blocks.SNOW.defaultBlockState(), 2);
                    }
                }
            }
        }

        /** Fences, panes, gates, walls, wire and stairs take their final shape once their neighbours exist. */
        void updateShapes() {
            for (BlockPos pos : shaped) {
                BlockState state = level.getBlockState(pos);
                level.setBlock(pos, Block.updateFromNeighbourShapes(state, level, pos), 2);
            }
        }

        private static boolean needsShape(BlockState state) {
            return state.is(BlockTags.FENCES) || state.is(Blocks.GLASS_PANE) || state.is(BlockTags.FENCE_GATES)
                    || state.is(Blocks.REDSTONE_WIRE) || state.is(BlockTags.STAIRS) || state.is(BlockTags.WALLS);
        }

        private static BlockState naturalTopsoil(BlockState state) {
            if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)
                    || state.is(Blocks.PODZOL) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.MYCELIUM)
                    || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.DIRT)
                    || state.is(Blocks.STONE) || state.is(Blocks.TERRACOTTA)) {
                return state;
            }
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
    }
}
