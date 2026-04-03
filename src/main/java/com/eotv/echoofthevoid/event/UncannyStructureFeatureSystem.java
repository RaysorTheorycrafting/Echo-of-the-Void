package com.eotv.echoofthevoid.event;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyStructureVillagerEntity;
import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import com.eotv.echoofthevoid.lore.UncannyLoreBookLibrary;
import com.eotv.echoofthevoid.phase.UncannyPhase;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

public final class UncannyStructureFeatureSystem {
    // Legacy constants kept for backwards compatibility with debug cooldown state.
    private static final double BASE_TRIGGER_CHANCE = 0.09D;
    private static final double[] PROFILE_TRIGGER_MULTIPLIER = {0.55D, 0.80D, 1.00D, 1.35D, 1.80D};
    private static final double[] PHASE_TRIGGER_MULTIPLIER = {0.35D, 0.75D, 1.00D, 1.25D};

    private static final int[] PROFILE_CHECK_MIN_SECONDS = {120, 90, 75, 60, 45};
    private static final int[] PROFILE_CHECK_MAX_SECONDS = {190, 145, 115, 90, 70};
    private static final int[] PROFILE_COOLDOWN_SECONDS = {3600, 3000, 2400, 1800, 1300};
    private static final String SECRET_HOUSE_MARKER = "secret_house";
    private static final String FALSE_ASCENT_HOUSE_MARKER = "false_ascent_house";
    private static final String FALSE_DESCENT_HOUSE_MARKER = "false_descent_house";
    private static final String SECRET_CHEST_FLAG = "eotv_secret_history_chest";
    private static final String SECRET_CHEST_LORE_GRANTED = "eotv_secret_lore_granted";
    private static final String SECRET_CHEST_BEHIND_TRIGGERED = "eotv_secret_behind_triggered";
    private static final int HISTORY_TOME_COUNT = UncannyLoreBookLibrary.volumeCount();
    private static final double HISTORY_BOOK_CHANCE = 0.65D;
    private static final double HISTORY_BEHIND_YOU_CHANCE_WHEN_COMPLETE = 0.01D;
    private static final double FALSE_SPIRAL_SECRET_HOUSE_CHANCE = 0.65D;
    private static final int WORLDGEN_MIN_FEATURE_DISTANCE = 18;
    private static final int WORLDGEN_MIN_SAME_FEATURE_DISTANCE = 52;
    private static final Map<UUID, Long> LAST_SECRET_CHEST_ROLL_TICK = new HashMap<>();
    private static final ThreadLocal<Boolean> WORLDGEN_PASS = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<String> FORCED_STRUCTURE_VARIANT = new ThreadLocal<>();

    private UncannyStructureFeatureSystem() {
    }

    public static List<String> featureIds() {
        return List.of(
                "anechoic_cube",
                "mimic_shelter",
                "glitched_shelter",
                "patterned_grove",
                "barren_grid",
                "false_descent",
                "false_descent_with_house",
                "false_ascent",
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
                "storage_shed");
    }

    public static boolean forceGenerateSecretHouseForDebug(ServerPlayer anchor) {
        if (anchor == null || anchor.getServer() == null) {
            return false;
        }

        ServerLevel level = anchor.serverLevel();
        if (level.dimension() != Level.OVERWORLD) {
            return false;
        }

        UncannyWorldState state = UncannyWorldState.get(anchor.getServer());
        if (!state.isSystemEnabled()) {
            debugLog("FEATURE secret_house force rejected: system disabled player={}", anchor.getGameProfile().getName());
            return false;
        }

        Direction entranceFacing = anchor.getDirection().getOpposite();
        BlockPos probe = anchor.blockPosition().relative(anchor.getDirection(), 8);
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe.getX(), probe.getZ());
        BlockPos houseCenter = new BlockPos(probe.getX(), surfaceY + 1, probe.getZ());

        if (!hasLoadedArea(level, houseCenter, 8)) {
            debugLog("FEATURE secret_house force failed center={} reason=unloaded-area player={}", houseCenter, anchor.getGameProfile().getName());
            return false;
        }

        prepareSecretHouseSite(level, houseCenter, true);
        if (!generateSecretHouse(level, houseCenter, entranceFacing, true)) {
            debugLog("FEATURE secret_house force failed center={} reason=generate-failed player={}", houseCenter, anchor.getGameProfile().getName());
            return false;
        }

        BlockPos doorLower = houseCenter.relative(entranceFacing, 3).above();
        placeBlackDoor(level, doorLower, entranceFacing);
        state.addStructureMarker(SECRET_HOUSE_MARKER, level.dimension(), houseCenter);
        debugLog("FEATURE secret_house force generated door={} house={} player={}", doorLower, houseCenter, anchor.getGameProfile().getName());
        return true;
    }

    public static boolean forceGenerateFeature(ServerPlayer anchor, String featureId) {
        return forceGenerateFeatureInternal(anchor, featureId, null);
    }

    public static boolean forceGenerateFeatureVariant(ServerPlayer anchor, String featureId, String variantId) {
        String normalizedVariant = variantId == null ? null : variantId.trim().toLowerCase(Locale.ROOT);
        if (normalizedVariant != null && normalizedVariant.isBlank()) {
            normalizedVariant = null;
        }
        return forceGenerateFeatureInternal(anchor, featureId, normalizedVariant);
    }

    private static boolean forceGenerateFeatureInternal(ServerPlayer anchor, String featureId, String forcedVariant) {
        if (anchor == null || anchor.getServer() == null) {
            return false;
        }

        ServerLevel level = anchor.serverLevel();
        if (level.dimension() != Level.OVERWORLD) {
            return false;
        }

        UncannyWorldState state = UncannyWorldState.get(anchor.getServer());
        if (!state.isSystemEnabled()) {
            debugLog("FEATURE force rejected: system disabled player={}", anchor.getGameProfile().getName());
            return false;
        }

        String normalized = featureId == null ? "" : featureId.trim().toLowerCase(Locale.ROOT);
        boolean forceSecretHouseAttach = false;
        FeatureType feature = switch (normalized) {
            case "false_descent_with_house" -> {
                forceSecretHouseAttach = true;
                yield FeatureType.FALSE_DESCENT;
            }
            case "false_ascent_with_house" -> {
                forceSecretHouseAttach = true;
                yield FeatureType.FALSE_ASCENT;
            }
            default -> FeatureType.byId(featureId);
        };
        if (feature == null) {
            debugLog("FEATURE force rejected: unknown id={} player={}", featureId, anchor.getGameProfile().getName());
            return false;
        }

        BlockPos origin = findOrigin(level, anchor, feature, state, true, true, false);
        if (origin == null) {
            debugLog("FEATURE force no-origin type={} anchor={}", feature.id, anchor.getGameProfile().getName());
            return false;
        }

        boolean generated;
        if (forcedVariant == null) {
            generated = generateFeature(level, feature, origin, forceSecretHouseAttach);
        } else {
            FORCED_STRUCTURE_VARIANT.set(forcedVariant);
            try {
                generated = generateFeature(level, feature, origin, forceSecretHouseAttach);
            } finally {
                FORCED_STRUCTURE_VARIANT.remove();
            }
        }

        if (!generated) {
            debugLog("FEATURE force generation-failed type={} origin={}", feature.id, origin);
            return false;
        }

        long now = anchor.getServer().getTickCount();
        int profile = getIntensityProfile();
        UncannyPhase phase = state.getPhase();
        state.addStructureMarker(feature.id, level.dimension(), origin);
        state.setStructureCooldownUntilTick(now + rollCooldownTicks(level, profile, phase));
        state.setStructureNextCheckTick(now + rollNextCheckTicks(level, profile, phase));
        debugLog(
                "FEATURE force generated type={} origin={} phase={} profile={} forcedSecretHouse={}",
                feature.id,
                origin,
                phase.index(),
                profile,
                forceSecretHouseAttach);
        return true;
    }

    public static boolean generateWorldgenFeatureAtChunk(ServerLevel level, String rawFeatureId, ChunkPos chunkPos) {
        if (level == null || chunkPos == null || level.getServer() == null || level.dimension() != Level.OVERWORLD) {
            return false;
        }

        FeatureType feature = resolveLocateFeature(rawFeatureId);
        if (feature == null) {
            debugLog("FEATURE worldgen rejected type={} reason=unknown-feature", rawFeatureId);
            return false;
        }

        UncannyWorldState state = UncannyWorldState.get(level.getServer());
        int attempts = 32;
        for (int attempt = 0; attempt < attempts; attempt++) {
            BlockPos origin = resolveOriginForChunk(level, feature, chunkPos, attempt);
            if (origin == null) {
                continue;
            }
            if (state.hasStructureMarkerNearby(feature.id, level.dimension(), origin, WORLDGEN_MIN_SAME_FEATURE_DISTANCE)) {
                debugLog("FEATURE worldgen skipped type={} origin={} reason=same-type-too-close", feature.id, origin);
                continue;
            }
            if (WORLDGEN_MIN_FEATURE_DISTANCE > 0 && hasAnyFeatureMarkerNearby(state, level, origin, WORLDGEN_MIN_FEATURE_DISTANCE)) {
                debugLog("FEATURE worldgen skipped type={} origin={} reason=too-close-to-feature", feature.id, origin);
                continue;
            }

            boolean generated;
            WORLDGEN_PASS.set(true);
            try {
                generated = generateFeature(level, feature, origin);
            } finally {
                WORLDGEN_PASS.set(false);
            }
            if (!generated) {
                continue;
            }

            state.addStructureMarker(feature.id, level.dimension(), origin);
            debugLog(
                    "FEATURE worldgen generated type={} origin={} chunk={} attempt={}/{}",
                    feature.id,
                    origin,
                    chunkPos,
                    attempt + 1,
                    attempts);
            return true;
        }

        debugLog("FEATURE worldgen failed type={} chunk={} attempts={}", feature.id, chunkPos, attempts);
        return false;
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getServer() == null || player.isSpectator()) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos pos = event.getPos();
        if (!(level.getBlockState(pos).getBlock() instanceof ChestBlock)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) {
            return;
        }

        CompoundTag chestData = chest.getPersistentData();
        debugLog(
                "FEATURE secret_house chest clicked player={} pos={} hand={} isSecretChest={} loreGranted={} behindTriggered={}",
                player.getGameProfile().getName(),
                pos,
                event.getHand(),
                chestData.getBoolean(SECRET_CHEST_FLAG),
                chestData.getBoolean(SECRET_CHEST_LORE_GRANTED),
                chestData.getBoolean(SECRET_CHEST_BEHIND_TRIGGERED));
        if (!chestData.getBoolean(SECRET_CHEST_FLAG)) {
            return;
        }

        long now = player.getServer().getTickCount();
        Long lastRollTick = LAST_SECRET_CHEST_ROLL_TICK.get(player.getUUID());
        if (lastRollTick != null && now - lastRollTick < 8L) {
            debugLog(
                    "FEATURE secret_house chest skip player={} pos={} reason=cooldown now={} last={} delta={}",
                    player.getGameProfile().getName(),
                    pos,
                    now,
                    lastRollTick,
                    now - lastRollTick);
            return;
        }
        LAST_SECRET_CHEST_ROLL_TICK.put(player.getUUID(), now);

        if (chestData.getBoolean(SECRET_CHEST_LORE_GRANTED) || chestData.getBoolean(SECRET_CHEST_BEHIND_TRIGGERED)) {
            debugLog(
                    "FEATURE secret_house chest skip player={} pos={} reason=already-resolved loreGranted={} behindTriggered={}",
                    player.getGameProfile().getName(),
                    pos,
                    chestData.getBoolean(SECRET_CHEST_LORE_GRANTED),
                    chestData.getBoolean(SECRET_CHEST_BEHIND_TRIGGERED));
            return;
        }

        double roll = level.random.nextDouble();
        if (roll > HISTORY_BOOK_CHANCE) {
            debugLog(
                    "FEATURE secret_house chest miss player={} roll={} chance={}",
                    player.getGameProfile().getName(),
                    String.format(java.util.Locale.ROOT, "%.4f", roll),
                    String.format(java.util.Locale.ROOT, "%.4f", HISTORY_BOOK_CHANCE));
            return;
        }

        UncannyWorldState state = UncannyWorldState.get(player.getServer());
        int nextTome = state.findFirstMissingHistoryTome(player.getUUID(), HISTORY_TOME_COUNT);
        debugLog(
                "FEATURE secret_house chest roll-hit player={} pos={} roll={} nextTome={}",
                player.getGameProfile().getName(),
                pos,
                String.format(java.util.Locale.ROOT, "%.4f", roll),
                nextTome);
        if (nextTome > 0) {
            ItemStack historyPiece = createHistoryPiece(nextTome);
            if (!insertIntoChestOrDrop(level, pos, chest, historyPiece)) {
                debugLog("FEATURE secret_house chest roll-hit player={} tome={} result=insert-failed", player.getGameProfile().getName(), nextTome);
                return;
            }
            chestData.putBoolean(SECRET_CHEST_LORE_GRANTED, true);
            chest.setChanged();
            state.markHistoryTomeFound(player.getUUID(), nextTome);
            debugLog(
                    "FEATURE secret_house chest roll-hit player={} roll={} result=lore-piece tome={}",
                    player.getGameProfile().getName(),
                    String.format(java.util.Locale.ROOT, "%.4f", roll),
                    nextTome);
            return;
        }

        double behindRoll = level.random.nextDouble();
        if (behindRoll > HISTORY_BEHIND_YOU_CHANCE_WHEN_COMPLETE) {
            debugLog(
                    "FEATURE secret_house chest complete-set miss player={} behindRoll={} chance={}",
                    player.getGameProfile().getName(),
                    String.format(java.util.Locale.ROOT, "%.4f", behindRoll),
                    String.format(java.util.Locale.ROOT, "%.4f", HISTORY_BEHIND_YOU_CHANCE_WHEN_COMPLETE));
            return;
        }

        ItemStack behindYou = new ItemStack(Items.BOOK);
        behindYou.set(DataComponents.CUSTOM_NAME, Component.literal("Behind you"));
        insertIntoChestOrDrop(level, pos, chest, behindYou);
        chestData.putBoolean(SECRET_CHEST_BEHIND_TRIGGERED, true);
        chest.setChanged();
        debugLog(
                "FEATURE secret_house chest roll-hit player={} roll={} result=behind-you",
                player.getGameProfile().getName(),
                String.format(java.util.Locale.ROOT, "%.4f", roll));
        spawnTerrorBehind(player);
    }

    public static BlockPos findNearestPlannedStructure(ServerLevel level, String featureId, BlockPos from) {
        if (level == null || from == null || featureId == null) {
            return null;
        }
        if (level.dimension() != Level.OVERWORLD) {
            return null;
        }

        FeatureType feature = resolveLocateFeature(featureId);
        if (feature == null) {
            return null;
        }

        ChunkPos fromChunk = new ChunkPos(from);
        int baseRegionX = Math.floorDiv(fromChunk.x, feature.spacing);
        int baseRegionZ = Math.floorDiv(fromChunk.z, feature.spacing);

        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        int maxRegionRadius = 12;
        for (int radius = 0; radius <= maxRegionRadius; radius++) {
            for (int rx = baseRegionX - radius; rx <= baseRegionX + radius; rx++) {
                for (int rz = baseRegionZ - radius; rz <= baseRegionZ + radius; rz++) {
                    if (radius > 0
                            && rx > baseRegionX - radius
                            && rx < baseRegionX + radius
                            && rz > baseRegionZ - radius
                            && rz < baseRegionZ + radius) {
                        continue;
                    }
                    ChunkPos candidateChunk = computeCandidateChunk(level, feature, rx, rz);
                    BlockPos candidate = new BlockPos(
                            candidateChunk.getMinBlockX() + 8,
                            from.getY(),
                            candidateChunk.getMinBlockZ() + 8);
                    double distSq = candidate.distSqr(from);
                    if (distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        nearest = candidate;
                    }
                }
            }
            if (nearest != null && radius >= 2) {
                break;
            }
        }
        return nearest;
    }

    private static ChunkPos computeCandidateChunk(ServerLevel level, FeatureType feature, int regionX, int regionZ) {
        long seed = level.getSeed();
        long mixed = seed
                + feature.salt
                + (long) regionX * 341873128712L
                + (long) regionZ * 132897987541L;
        RandomSource random = RandomSource.create(mixed);
        int range = Math.max(1, feature.spacing - feature.separation);
        int chunkX = regionX * feature.spacing + random.nextInt(range);
        int chunkZ = regionZ * feature.spacing + random.nextInt(range);
        return new ChunkPos(chunkX, chunkZ);
    }

    private static BlockPos resolveOriginForChunk(ServerLevel level, FeatureType feature, ChunkPos chunkPos, int attempt) {
        long seed = level.getSeed();
        long mixed = seed
                + feature.salt * 31L
                + (long) chunkPos.x * 42317861L
                + (long) chunkPos.z * 374761393L
                + (long) (attempt + 1) * 91815541L;
        RandomSource random = RandomSource.create(mixed);
        int x = chunkPos.getMinBlockX() + 8 + random.nextInt(7) - 3;
        int z = chunkPos.getMinBlockZ() + 8 + random.nextInt(7) - 3;
        BlockPos drySurface = resolveDrySurfaceOrigin(level, x, z, 16);
        int surfaceY = drySurface != null ? drySurface.getY() : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int surfaceX = drySurface != null ? drySurface.getX() : x;
        int surfaceZ = drySurface != null ? drySurface.getZ() : z;

        return switch (feature) {
            case ANECHOIC_CUBE -> {
                int minY = level.getMinBuildHeight() + 8;
                int maxY = surfaceY - 8;
                if (maxY <= minY) {
                    yield null;
                }
                int y = minY + random.nextInt(maxY - minY + 1);
                yield new BlockPos(surfaceX, y, surfaceZ);
            }
            case MIMIC_SHELTER,
                    GLITCHED_SHELTER,
                    PATTERNED_GROVE,
                    BARREN_GRID,
                    BELL_SHRINE,
                    WATCHING_TOWER,
                    FALSE_CAMP,
                    WRONG_VILLAGE_HOUSE,
                    WRONG_VILLAGE_UTILITY,
                    SINKHOLE,
                    OBSERVATION_PLATFORM,
                    WRONG_ROAD_SEGMENT,
                    FALSE_ENTRANCE,
                    STORAGE_SHED -> {
                if (drySurface == null) {
                    yield null;
                }
                yield new BlockPos(surfaceX, surfaceY, surfaceZ);
            }
            case FALSE_DESCENT, FALSE_ASCENT -> {
                if (drySurface == null) {
                    yield null;
                }
                yield new BlockPos(surfaceX, surfaceY + 1, surfaceZ);
            }
            case ISOLATION_CUBE -> {
                int minY = level.getMinBuildHeight() + 6;
                int maxY = surfaceY - 4;
                if (maxY <= minY) {
                    yield null;
                }
                int y = minY + random.nextInt(maxY - minY + 1);
                yield new BlockPos(surfaceX, y, surfaceZ);
            }
        };
    }

    private static BlockPos resolveDrySurfaceOrigin(ServerLevel level, int baseX, int baseZ, int maxRadius) {
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    int x = baseX + dx;
                    int z = baseZ + dz;
                    int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    if (y <= level.getMinBuildHeight() + 4 || y < level.getSeaLevel() - 2) {
                        continue;
                    }
                    BlockPos support = findSolidSupportBelow(level, new BlockPos(x, y - 1, z), 6);
                    if (support == null) {
                        continue;
                    }
                    BlockPos feet = support.above();
                    if (level.getFluidState(feet).is(FluidTags.WATER) || level.getFluidState(support).is(FluidTags.WATER)) {
                        continue;
                    }
                    return feet;
                }
            }
        }
        return null;
    }

    private static BlockPos findSolidSupportBelow(ServerLevel level, BlockPos start, int maxDepth) {
        for (int i = 0; i <= maxDepth; i++) {
            BlockPos candidate = start.below(i);
            BlockState state = level.getBlockState(candidate);
            if (state.isSolidRender(level, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static FeatureType resolveLocateFeature(String rawFeatureId) {
        if (rawFeatureId == null) {
            return null;
        }
        String normalized = rawFeatureId.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith(EchoOfTheVoid.MODID + ":")) {
            normalized = normalized.substring((EchoOfTheVoid.MODID + ":").length());
        }
        if ("false_descent_with_house".equals(normalized)) {
            normalized = "false_descent";
        } else if ("false_ascent_with_house".equals(normalized)) {
            normalized = "false_ascent";
        }
        if ("secret_house".equals(normalized)) {
            return null;
        }
        return FeatureType.byId(normalized);
    }

    private static boolean hasAnyFeatureMarkerNearby(UncannyWorldState state, ServerLevel level, BlockPos origin, int radius) {
        for (FeatureType featureType : FeatureType.values()) {
            if (state.hasStructureMarkerNearby(featureType.id, level.dimension(), origin, radius)) {
                return true;
            }
        }
        return false;
    }

    private static boolean generateFeature(ServerLevel level, FeatureType feature, BlockPos origin) {
        return generateFeature(level, feature, origin, false);
    }

    private static boolean generateFeature(
            ServerLevel level,
            FeatureType feature,
            BlockPos origin,
            boolean forceSecretHouseAttachOnSpiral) {
        boolean generated = switch (feature) {
            case ANECHOIC_CUBE -> generateAnechoicCube(level, origin);
            case MIMIC_SHELTER -> generateMimicShelter(level, origin);
            case GLITCHED_SHELTER -> generateGlitchedShelter(level, origin);
            case PATTERNED_GROVE -> generatePatternedGrove(level, origin);
            case BARREN_GRID -> generateBarrenGrid(level, origin);
            case FALSE_DESCENT -> generateFalseDescent(level, origin, forceSecretHouseAttachOnSpiral);
            case FALSE_ASCENT -> generateFalseAscent(level, origin, forceSecretHouseAttachOnSpiral);
            case ISOLATION_CUBE -> generateIsolationCube(level, origin);
            case BELL_SHRINE -> generateBellShrine(level, origin);
            case WATCHING_TOWER -> generateWatchingTower(level, origin);
            case FALSE_CAMP -> generateFalseCamp(level, origin);
            case WRONG_VILLAGE_HOUSE -> generateWrongVillageHouse(level, origin);
            case WRONG_VILLAGE_UTILITY -> generateWrongVillageUtility(level, origin);
            case SINKHOLE -> generateSinkhole(level, origin);
            case OBSERVATION_PLATFORM -> generateObservationPlatform(level, origin);
            case WRONG_ROAD_SEGMENT -> generateWrongRoadSegment(level, origin);
            case FALSE_ENTRANCE -> generateFalseEntrance(level, origin);
            case STORAGE_SHED -> generateStorageShed(level, origin);
        };
        if (!generated) {
            return false;
        }

        if (feature == FeatureType.FALSE_ASCENT || feature == FeatureType.FALSE_DESCENT) {
            return true;
        }
        if (shouldAttachSecretHouse(level, feature) && feature != FeatureType.PATTERNED_GROVE && feature != FeatureType.BARREN_GRID) {
            attachBlackDoorAndSecretHouse(level, feature, origin);
        }
        return true;
    }

    private static boolean shouldAttachSecretHouse(ServerLevel level, FeatureType feature) {
        return switch (feature) {
            case MIMIC_SHELTER, GLITCHED_SHELTER, ISOLATION_CUBE -> true;
            case WATCHING_TOWER -> level.random.nextDouble() < 0.38D;
            case WRONG_VILLAGE_HOUSE -> level.random.nextDouble() < 0.45D;
            case WRONG_VILLAGE_UTILITY -> level.random.nextDouble() < 0.35D;
            case FALSE_ENTRANCE -> level.random.nextDouble() < 0.55D;
            case SINKHOLE -> level.random.nextDouble() < 0.30D;
            case STORAGE_SHED -> level.random.nextDouble() < 0.32D;
            default -> false;
        };
    }

    private static void attachBlackDoorAndSecretHouse(ServerLevel level, FeatureType feature, BlockPos origin) {
        if (level.getServer() == null) {
            return;
        }

        Direction facing = randomHorizontal(level);
        BlockPos doorLower = resolveSecretDoorPos(level, origin, facing);
        if (doorLower == null) {
            debugLog("FEATURE secret_house skip feature={} origin={} reason=no-door-pos", feature.id, origin);
            return;
        }

        if (!attachConnectedSecretHouse(level, feature, doorLower, facing, false)) {
            debugLog("FEATURE secret_house skip feature={} origin={} reason=attach-failed", feature.id, origin);
        }
    }

    private static BlockPos resolveSecretDoorPos(ServerLevel level, BlockPos origin, Direction facing) {
        for (int attempt = 0; attempt < 24; attempt++) {
            int x = origin.getX() + level.random.nextInt(11) - 5;
            int z = origin.getZ() + level.random.nextInt(11) - 5;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos lower = new BlockPos(x, y + 1, z);
            if (canPlaceDoorAt(level, lower, facing)) {
                return lower;
            }
        }

        int fallbackY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ());
        BlockPos fallback = new BlockPos(origin.getX(), fallbackY + 1, origin.getZ());
        return canPlaceDoorAt(level, fallback, facing) ? fallback : null;
    }

    private static boolean canPlaceDoorAt(ServerLevel level, BlockPos lower, Direction facing) {
        if (!level.hasChunkAt(lower) || !level.hasChunkAt(lower.above())) {
            return false;
        }
        BlockState below = level.getBlockState(lower.below());
        if (!below.isSolidRender(level, lower.below())) {
            return false;
        }
        BlockState currentLower = level.getBlockState(lower);
        BlockState currentUpper = level.getBlockState(lower.above());
        if ((!currentLower.isAir() && !currentLower.canBeReplaced()) || (!currentUpper.isAir() && !currentUpper.canBeReplaced())) {
            return false;
        }
        BlockPos forward = lower.relative(facing);
        BlockPos backward = lower.relative(facing.getOpposite());
        return !level.getFluidState(forward).is(FluidTags.WATER)
                && !level.getFluidState(backward).is(FluidTags.WATER);
    }

    private static void placeBlackDoor(ServerLevel level, BlockPos lower, Direction facing) {
        BlockPos upper = lower.above();
        level.setBlock(lower, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(upper, Blocks.AIR.defaultBlockState(), 3);
        if (level.getBlockState(lower.below()).isAir()) {
            level.setBlock(lower.below(), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }
        level.setBlock(
                lower,
                UncannyBlockRegistry.UNCANNY_VOID_DOOR.get().defaultBlockState()
                        .setValue(DoorBlock.FACING, facing)
                        .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                        .setValue(DoorBlock.OPEN, false)
                        .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER),
                3);
        level.setBlock(
                upper,
                UncannyBlockRegistry.UNCANNY_VOID_DOOR.get().defaultBlockState()
                        .setValue(DoorBlock.FACING, facing)
                        .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                        .setValue(DoorBlock.OPEN, false)
                        .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER),
                3);
    }

    private static BlockPos secretHouseCenterFromDoor(BlockPos doorLower, Direction doorFacing) {
        return doorLower.below().relative(doorFacing.getOpposite(), 3);
    }

    private static BlockPos resolveConnectedHouseCenter(ServerLevel level, BlockPos doorLower, Direction doorFacing) {
        BlockPos base = secretHouseCenterFromDoor(doorLower, doorFacing);
        if (canBuildSecretHouseAt(level, base)) {
            return base;
        }

        for (int distance = 3; distance <= 6; distance++) {
            for (int side = -2; side <= 2; side++) {
                BlockPos candidate = doorLower
                        .below()
                        .relative(doorFacing.getOpposite(), distance)
                        .relative(doorFacing.getClockWise(), side);
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos shifted = candidate.offset(0, dy, 0);
                    if (canBuildSecretHouseAt(level, shifted)) {
                        return shifted;
                    }
                }

                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate.getX(), candidate.getZ());
                BlockPos surfaceCandidate = new BlockPos(candidate.getX(), surfaceY + 1, candidate.getZ());
                if (canBuildSecretHouseAt(level, surfaceCandidate)) {
                    return surfaceCandidate;
                }
            }
        }
        return null;
    }

    private static boolean attachConnectedSecretHouse(
            ServerLevel level,
            FeatureType feature,
            BlockPos doorLower,
            Direction doorFacing,
            boolean force) {
        if (!force && !canPlaceDoorAt(level, doorLower, doorFacing)) {
            debugLog("FEATURE secret_house attach rejected feature={} door={} reason=door-not-placeable", feature.id, doorLower);
            return false;
        }

        boolean strictAlignment = feature == FeatureType.FALSE_ASCENT || feature == FeatureType.FALSE_DESCENT;
        BlockPos houseCenter = strictAlignment
                ? secretHouseCenterFromDoor(doorLower, doorFacing)
                : resolveConnectedHouseCenter(level, doorLower, doorFacing);
        if (houseCenter == null) {
            debugLog("FEATURE secret_house attach rejected feature={} door={} reason=no-valid-center", feature.id, doorLower);
            return false;
        }

        if (strictAlignment) {
            prepareSecretHouseSite(level, houseCenter, false);
        } else if (!canBuildSecretHouseAt(level, houseCenter)) {
            debugLog(
                    "FEATURE secret_house attach rejected feature={} door={} house={} strict={} reason=site-invalid",
                    feature.id,
                    doorLower,
                    houseCenter,
                    strictAlignment);
            return false;
        }

        if (!generateSecretHouse(level, houseCenter, doorFacing, strictAlignment)) {
            debugLog(
                    "FEATURE secret_house attach rejected feature={} door={} house={} strict={} reason=generation-failed",
                    feature.id,
                    doorLower,
                    houseCenter,
                    strictAlignment);
            return false;
        }

        placeBlackDoor(level, doorLower, doorFacing);
        level.setBlock(doorLower.relative(doorFacing), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(doorLower.relative(doorFacing).above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(doorLower.relative(doorFacing.getOpposite()), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(doorLower.relative(doorFacing.getOpposite()).above(), Blocks.AIR.defaultBlockState(), 3);
        if (level.getServer() != null) {
            UncannyWorldState.get(level.getServer()).addStructureMarker(SECRET_HOUSE_MARKER, level.dimension(), houseCenter);
        }
        debugLog("FEATURE secret_house attached feature={} door={} house={}", feature.id, doorLower, houseCenter);
        return true;
    }

    // Dedicated alignment for False Ascent/Descent:
    // The black door must be directly in front of the last stair, with the house behind that door.
    private static boolean attachSpiralSecretHouse(
            ServerLevel level,
            FeatureType feature,
            BlockPos doorLower,
            Direction stairExitDirection) {
        // Rotate black door orientation by 180° compared to previous behavior.
        Direction houseEntranceFacing = stairExitDirection.getOpposite();
        BlockPos houseCenter = doorLower.below().relative(stairExitDirection, 3);

        prepareSecretHouseSite(level, houseCenter, false);
        if (!generateSecretHouse(level, houseCenter, houseEntranceFacing, true)) {
            debugLog(
                    "FEATURE secret_house spiral attach rejected feature={} door={} house={} reason=generation-failed",
                    feature.id,
                    doorLower,
                    houseCenter);
            return false;
        }

        placeBlackDoor(level, doorLower, houseEntranceFacing.getOpposite());

        // Ensure direct access from the final stair to the black door.
        BlockPos approachLower = doorLower.relative(stairExitDirection.getOpposite());
        BlockPos approachUpper = approachLower.above();
        level.setBlock(approachLower, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(approachUpper, Blocks.AIR.defaultBlockState(), 3);
        if (level.getBlockState(approachLower.below()).isAir()) {
            level.setBlock(approachLower.below(), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }

        if (level.getServer() != null) {
            UncannyWorldState state = UncannyWorldState.get(level.getServer());
            state.addStructureMarker(SECRET_HOUSE_MARKER, level.dimension(), houseCenter);
            if (feature == FeatureType.FALSE_ASCENT) {
                state.addStructureMarker(FALSE_ASCENT_HOUSE_MARKER, level.dimension(), houseCenter);
            } else if (feature == FeatureType.FALSE_DESCENT) {
                state.addStructureMarker(FALSE_DESCENT_HOUSE_MARKER, level.dimension(), houseCenter);
            }
        }
        debugLog(
                "FEATURE secret_house spiral attached feature={} door={} house={} stairExit={}",
                feature.id,
                doorLower,
                houseCenter,
                stairExitDirection);
        return true;
    }

    private static void prepareSecretHouseSite(ServerLevel level, BlockPos center, boolean wideBuffer) {
        int radius = wideBuffer ? 4 : 3;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos foundation = new BlockPos(center.getX() + dx, center.getY() - 1, center.getZ() + dz);
                level.setBlock(foundation, Blocks.COBBLESTONE.defaultBlockState(), 3);
                for (int dy = 0; dy <= 5; dy++) {
                    BlockPos clear = foundation.above(dy);
                    level.setBlock(clear, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static boolean canBuildSecretHouseAt(ServerLevel level, BlockPos center) {
        if (!hasLoadedArea(level, center, 6)) {
            return false;
        }
        if (center.getY() <= level.getMinBuildHeight() + 2 || center.getY() + 8 >= level.getMaxBuildHeight() - 1) {
            return false;
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                BlockPos ground = new BlockPos(center.getX() + dx, center.getY() - 1, center.getZ() + dz);
                if (level.getFluidState(ground).is(FluidTags.WATER)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean generateSecretHouse(ServerLevel level, BlockPos center, Direction entranceFacing) {
        return generateSecretHouse(level, center, entranceFacing, false);
    }

    private static boolean generateSecretHouse(ServerLevel level, BlockPos center, Direction entranceFacing, boolean skipValidation) {
        if (!skipValidation && !canBuildSecretHouseAt(level, center)) {
            return false;
        }

        BlockPos corner = center.offset(-3, 0, -3);
        for (int dx = 0; dx < 7; dx++) {
            for (int dz = 0; dz < 7; dz++) {
                BlockPos floor = corner.offset(dx, 0, dz);
                BlockPos under = floor.below();
                if (level.getBlockState(under).isAir()) {
                    level.setBlock(under, Blocks.COBBLESTONE.defaultBlockState(), 3);
                }
                level.setBlock(floor, Blocks.OAK_PLANKS.defaultBlockState(), 3);

                for (int dy = 1; dy <= 4; dy++) {
                    BlockPos target = corner.offset(dx, dy, dz);
                    boolean wall = dx == 0 || dx == 6 || dz == 0 || dz == 6;
                    if (!wall) {
                        level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                        continue;
                    }

                    boolean cornerPillar = (dx == 0 || dx == 6) && (dz == 0 || dz == 6);
                    if (dy == 4) {
                        level.setBlock(target, Blocks.OAK_PLANKS.defaultBlockState(), 3);
                    } else if (cornerPillar) {
                        level.setBlock(target, Blocks.OAK_LOG.defaultBlockState(), 3);
                    } else if (dy == 2 && (dx == 3 || dz == 3)) {
                        level.setBlock(target, Blocks.GLASS_PANE.defaultBlockState(), 3);
                    } else {
                        level.setBlock(target, Blocks.COBBLESTONE.defaultBlockState(), 3);
                    }
                }
            }
        }

        BlockPos doorLower = center.relative(entranceFacing, 3).above();
        BlockPos doorUpper = doorLower.above();
        level.setBlock(doorLower, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(doorUpper, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(
                doorLower,
                Blocks.OAK_DOOR.defaultBlockState()
                        .setValue(DoorBlock.FACING, entranceFacing)
                        .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                        .setValue(DoorBlock.OPEN, false)
                        .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER),
                3);
        level.setBlock(
                doorUpper,
                Blocks.OAK_DOOR.defaultBlockState()
                        .setValue(DoorBlock.FACING, entranceFacing)
                        .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                        .setValue(DoorBlock.OPEN, false)
                        .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER),
                3);

        BlockPos chestPos = center.relative(entranceFacing.getOpposite(), 2).above();
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, entranceFacing), 3);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chestBlockEntity) {
            populateSecretHouseChest(level, chestBlockEntity);
        }

        level.setBlock(center.above(2), Blocks.TORCH.defaultBlockState(), 3);
        return true;
    }

    private static void populateSecretHouseChest(ServerLevel level, ChestBlockEntity chest) {
        chest.clearContent();
        chest.getPersistentData().putBoolean(SECRET_CHEST_FLAG, true);

        int fills = 8 + level.random.nextInt(5);
        for (int i = 0; i < fills; i++) {
            ItemStack stack = createRandomJunkLoot(level);
            int slot = level.random.nextInt(chest.getContainerSize());
            if (chest.getItem(slot).isEmpty()) {
                chest.setItem(slot, stack);
            }
        }
        chest.setChanged();
        debugLog("FEATURE secret_house chest prepared pos={} junkSlotsFilled~{}", chest.getBlockPos(), fills);
    }

    private static ItemStack createRandomJunkLoot(ServerLevel level) {
        return switch (level.random.nextInt(16)) {
            case 0 -> new ItemStack(Items.ROTTEN_FLESH, 2 + level.random.nextInt(5));
            case 1 -> new ItemStack(Items.BONE, 1 + level.random.nextInt(4));
            case 2 -> new ItemStack(Items.STRING, 1 + level.random.nextInt(4));
            case 3 -> new ItemStack(Items.GUNPOWDER, 1 + level.random.nextInt(3));
            case 4 -> new ItemStack(Items.DIRT, 8 + level.random.nextInt(17));
            case 5 -> new ItemStack(Items.COBBLESTONE, 8 + level.random.nextInt(21));
            case 6 -> new ItemStack(Items.GRAVEL, 6 + level.random.nextInt(12));
            case 7 -> new ItemStack(Items.ROTTEN_FLESH, 1 + level.random.nextInt(3));
            case 8 -> new ItemStack(Items.POTATO, 1 + level.random.nextInt(3));
            case 9 -> new ItemStack(Items.BREAD, 1 + level.random.nextInt(2));
            case 10 -> createHeavilyDamagedItem(Items.WOODEN_SWORD, level);
            case 11 -> createHeavilyDamagedItem(Items.STONE_PICKAXE, level);
            case 12 -> createHeavilyDamagedItem(Items.LEATHER_HELMET, level);
            case 13 -> createHeavilyDamagedItem(Items.LEATHER_CHESTPLATE, level);
            case 14 -> createHeavilyDamagedItem(Items.LEATHER_BOOTS, level);
            default -> createHeavilyDamagedItem(Items.STONE_AXE, level);
        };
    }

    private static ItemStack createHeavilyDamagedItem(net.minecraft.world.item.Item item, ServerLevel level) {
        ItemStack stack = new ItemStack(item);
        if (stack.isDamageableItem()) {
            int max = stack.getMaxDamage();
            int damage = Math.min(max - 1, (int) (max * (0.86D + level.random.nextDouble() * 0.12D)));
            stack.setDamageValue(damage);
        }
        return stack;
    }

    private static ItemStack createHistoryPiece(int tomeIndex) {
        ItemStack piece = new ItemStack(UncannyItemRegistry.UNCANNY_LORE_PIECE.get());
        piece.set(DataComponents.WRITTEN_BOOK_CONTENT, UncannyLoreBookLibrary.contentForVolume(tomeIndex));
        return piece;
    }

    public static WrittenBookContent defaultHistoryBookContent(int tomeIndex) {
        int resolvedVolume = tomeIndex <= 0 ? 1 : tomeIndex;
        return UncannyLoreBookLibrary.contentForVolume(resolvedVolume);
    }

    private static boolean insertIntoChestOrDrop(ServerLevel level, BlockPos chestPos, ChestBlockEntity chest, ItemStack stack) {
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (chest.getItem(i).isEmpty()) {
                chest.setItem(i, stack);
                chest.setChanged();
                return true;
            }
        }
        BlockPos dropPos = chestPos.above();
        Block.popResource(level, dropPos, stack);
        return true;
    }

    private static void spawnTerrorBehind(ServerPlayer player) {
        if (player.getServer() == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Monster terror = UncannyEntityRegistry.UNCANNY_TERROR.get().create(level);
        if (terror == null) {
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 0.0001D) {
            horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 backwards = horizontalLook.normalize().scale(-1.0D);
        Vec3 right = new Vec3(-backwards.z, 0.0D, backwards.x);

        BlockPos spawnPos = null;
        double[] distances = {3.8D, 4.8D, 2.9D};
        double[] offsets = {0.0D, 1.2D, -1.2D, 2.2D, -2.2D};
        for (double dist : distances) {
            for (double sideOffset : offsets) {
                Vec3 candidateVec = player.position().add(backwards.scale(dist)).add(right.scale(sideOffset));
                BlockPos candidate = BlockPos.containing(candidateVec.x, player.getY(), candidateVec.z);
                if (isValidTerrorSpawnPos(level, candidate, player)) {
                    spawnPos = candidate;
                    break;
                }
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos shifted = candidate.offset(0, dy, 0);
                    if (isValidTerrorSpawnPos(level, shifted, player)) {
                        spawnPos = shifted;
                        break;
                    }
                }
                if (spawnPos != null) {
                    break;
                }
            }
            if (spawnPos != null) {
                break;
            }
        }
        if (spawnPos == null) {
            debugLog("FEATURE secret_house terror spawn skipped player={} reason=no-safe-pos-behind", player.getGameProfile().getName());
            return;
        }

        terror.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot(), 0.0F);
        level.addFreshEntity(terror);
    }

    private static boolean isValidTerrorSpawnPos(ServerLevel level, BlockPos candidate, ServerPlayer targetPlayer) {
        BlockState feet = level.getBlockState(candidate);
        BlockState head = level.getBlockState(candidate.above());
        BlockState below = level.getBlockState(candidate.below());
        if (!feet.isAir() || !head.isAir() || !below.isSolidRender(level, candidate.below())) {
            return false;
        }
        double distSq = targetPlayer.position().distanceToSqr(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
        return distSq >= 4.0D;
    }

    private static FeatureType pickFeature(ServerLevel level, UncannyPhase phase, int profile) {
        List<WeightedFeature> pool = new ArrayList<>();
        pool.add(new WeightedFeature(FeatureType.ANECHOIC_CUBE, 12));
        pool.add(new WeightedFeature(FeatureType.MIMIC_SHELTER, 11));
        pool.add(new WeightedFeature(FeatureType.GLITCHED_SHELTER, 8 + Math.max(0, profile - 2)));
        pool.add(new WeightedFeature(FeatureType.ISOLATION_CUBE, 7 + Math.max(0, phase.index() - 2)));
        pool.add(new WeightedFeature(FeatureType.FALSE_CAMP, 10 + Math.max(0, profile - 2)));
        pool.add(new WeightedFeature(FeatureType.STORAGE_SHED, 8 + Math.max(0, profile - 2)));
        pool.add(new WeightedFeature(FeatureType.WRONG_ROAD_SEGMENT, 9 + Math.max(0, phase.index() - 1)));
        pool.add(new WeightedFeature(FeatureType.BELL_SHRINE, 7 + Math.max(0, phase.index() - 1)));
        pool.add(new WeightedFeature(FeatureType.OBSERVATION_PLATFORM, 7 + Math.max(0, profile - 2)));
        pool.add(new WeightedFeature(FeatureType.FALSE_ENTRANCE, 7 + Math.max(0, phase.index() - 1)));
        if (phase.index() >= 3) {
            pool.add(new WeightedFeature(FeatureType.PATTERNED_GROVE, 8 + Math.max(0, profile - 3)));
            pool.add(new WeightedFeature(FeatureType.BARREN_GRID, 7 + Math.max(0, phase.index() - 3)));
            pool.add(new WeightedFeature(FeatureType.FALSE_DESCENT, 6 + Math.max(0, phase.index() - 3)));
            pool.add(new WeightedFeature(FeatureType.FALSE_ASCENT, 5 + Math.max(0, profile - 3)));
            pool.add(new WeightedFeature(FeatureType.WATCHING_TOWER, 8 + Math.max(0, profile - 3)));
            pool.add(new WeightedFeature(FeatureType.WRONG_VILLAGE_HOUSE, 8 + Math.max(0, phase.index() - 2)));
            pool.add(new WeightedFeature(FeatureType.WRONG_VILLAGE_UTILITY, 7 + Math.max(0, phase.index() - 2)));
            pool.add(new WeightedFeature(FeatureType.SINKHOLE, 6 + Math.max(0, phase.index() - 3)));
        }

        int totalWeight = pool.stream().mapToInt(WeightedFeature::weight).sum();
        if (totalWeight <= 0) {
            return null;
        }

        int target = level.random.nextInt(totalWeight);
        int cursor = 0;
        for (WeightedFeature entry : pool) {
            cursor += entry.weight();
            if (target < cursor) {
                return entry.type();
            }
        }
        return pool.get(pool.size() - 1).type();
    }

    private static BlockPos findOrigin(
            ServerLevel level,
            ServerPlayer anchor,
            FeatureType feature,
            UncannyWorldState state,
            boolean forceNear,
            boolean ignoreMarkers,
            boolean requireFreshChunk) {
        if (forceNear) {
            return findOriginNearAnchor(level, anchor, feature, state, ignoreMarkers, requireFreshChunk);
        }

        MinecraftServer server = level.getServer();
        int viewDistance = Math.max(2, server.getPlayerList().getViewDistance());
        int simulationDistance = Math.max(2, server.getPlayerList().getSimulationDistance());
        int effective = Math.max(4, Math.min(viewDistance, simulationDistance) - 1);
        int minChunks = Math.max(3, effective / 2);
        int maxChunks = Math.max(minChunks + 1, effective);

        int notLoaded = 0;
        int lowSurface = 0;
        int nullOrigin = 0;
        int markerRejected = 0;
        int invalidOrigin = 0;
        int staleChunk = 0;
        int attempts = 88;
        for (int attempt = 0; attempt < attempts; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            int chunkDistance = minChunks + level.random.nextInt(maxChunks - minChunks + 1);
            int x = Mth.floor(anchor.getX() + Math.cos(angle) * chunkDistance * 16.0D) + level.random.nextInt(16) - 8;
            int z = Mth.floor(anchor.getZ() + Math.sin(angle) * chunkDistance * 16.0D) + level.random.nextInt(16) - 8;

            BlockPos probe = new BlockPos(x, anchor.getBlockY(), z);
            if (!level.hasChunkAt(probe)) {
                notLoaded++;
                continue;
            }

            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (surfaceY <= level.getMinBuildHeight() + 6) {
                lowSurface++;
                continue;
            }

            BlockPos origin = switch (feature) {
                case ANECHOIC_CUBE -> {
                    int minY = level.getMinBuildHeight() + 8;
                    int maxY = surfaceY - 8;
                    if (maxY <= minY) {
                        yield null;
                    }
                    int y = minY + level.random.nextInt(maxY - minY + 1);
                    yield new BlockPos(x, y, z);
                }
                case MIMIC_SHELTER,
                        GLITCHED_SHELTER,
                        PATTERNED_GROVE,
                        BARREN_GRID,
                        BELL_SHRINE,
                        WATCHING_TOWER,
                        FALSE_CAMP,
                        WRONG_VILLAGE_HOUSE,
                        WRONG_VILLAGE_UTILITY,
                        SINKHOLE,
                        OBSERVATION_PLATFORM,
                        WRONG_ROAD_SEGMENT,
                        FALSE_ENTRANCE,
                        STORAGE_SHED -> new BlockPos(x, surfaceY, z);
                case FALSE_DESCENT -> new BlockPos(x, surfaceY + 1, z);
                case FALSE_ASCENT -> new BlockPos(x, surfaceY + 1, z);
                case ISOLATION_CUBE -> {
                    int minY = level.getMinBuildHeight() + 6;
                    int maxY = surfaceY - 4;
                    if (maxY <= minY) {
                        yield null;
                    }
                    int y = minY + level.random.nextInt(maxY - minY + 1);
                    yield new BlockPos(x, y, z);
                }
            };
            if (origin == null) {
                nullOrigin++;
                continue;
            }

            if (requireFreshChunk && !isFreshOriginChunk(level, origin)) {
                staleChunk++;
                continue;
            }

            if (!ignoreMarkers) {
                if (state.hasAnyStructureMarkerNearby(level.dimension(), origin, feature.exclusionRadius)) {
                    markerRejected++;
                    continue;
                }
                if (state.hasStructureMarkerNearby(feature.id, level.dimension(), origin, feature.sameTypeExclusionRadius)) {
                    markerRejected++;
                    continue;
                }
            }

            if (!isValidOrigin(level, feature, origin, false)) {
                invalidOrigin++;
                continue;
            }
            return origin;
        }
        debugLog(
                "FEATURE origin-fail type={} near=false ignoreMarkers={} attempts={} notLoaded={} lowSurface={} nullOrigin={} markerRejected={} invalidOrigin={} staleChunk={} anchor={}",
                feature.id,
                ignoreMarkers,
                attempts,
                notLoaded,
                lowSurface,
                nullOrigin,
                markerRejected,
                invalidOrigin,
                staleChunk,
                anchor.blockPosition());
        return null;
    }

    private static BlockPos findOriginNearAnchor(
            ServerLevel level,
            ServerPlayer anchor,
            FeatureType feature,
            UncannyWorldState state,
            boolean ignoreMarkers,
            boolean requireFreshChunk) {
        int minDistance = switch (feature) {
            case ANECHOIC_CUBE -> 16;
            case MIMIC_SHELTER,
                    GLITCHED_SHELTER,
                    FALSE_DESCENT,
                    FALSE_ASCENT,
                    FALSE_CAMP,
                    WRONG_ROAD_SEGMENT,
                    STORAGE_SHED -> 10;
            case PATTERNED_GROVE,
                    BARREN_GRID,
                    BELL_SHRINE,
                    WATCHING_TOWER,
                    WRONG_VILLAGE_HOUSE,
                    WRONG_VILLAGE_UTILITY,
                    OBSERVATION_PLATFORM,
                    FALSE_ENTRANCE -> 18;
            case SINKHOLE, ISOLATION_CUBE -> 14;
        };
        int maxDistance = switch (feature) {
            case ANECHOIC_CUBE -> 42;
            case MIMIC_SHELTER,
                    GLITCHED_SHELTER,
                    FALSE_DESCENT,
                    FALSE_ASCENT,
                    FALSE_CAMP,
                    WRONG_ROAD_SEGMENT,
                    STORAGE_SHED -> 30;
            case PATTERNED_GROVE,
                    BARREN_GRID,
                    BELL_SHRINE,
                    WATCHING_TOWER,
                    WRONG_VILLAGE_HOUSE,
                    WRONG_VILLAGE_UTILITY,
                    OBSERVATION_PLATFORM,
                    FALSE_ENTRANCE -> 44;
            case SINKHOLE, ISOLATION_CUBE -> 36;
        };

        int notLoaded = 0;
        int nullOrigin = 0;
        int markerRejected = 0;
        int invalidOrigin = 0;
        int staleChunk = 0;
        int attempts = 96;
        for (int attempt = 0; attempt < attempts; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            int distance = minDistance + level.random.nextInt(Math.max(1, maxDistance - minDistance + 1));
            int x = Mth.floor(anchor.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(anchor.getZ() + Math.sin(angle) * distance);

            BlockPos probe = new BlockPos(x, anchor.getBlockY(), z);
            if (!level.hasChunkAt(probe)) {
                notLoaded++;
                continue;
            }

            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos origin = switch (feature) {
                case ANECHOIC_CUBE -> {
                    int minY = level.getMinBuildHeight() + 8;
                    int maxY = Math.max(minY + 1, surfaceY - 8);
                    if (maxY <= minY) {
                        yield null;
                    }
                    int y = minY + level.random.nextInt(maxY - minY + 1);
                    yield new BlockPos(x, y, z);
                }
                case MIMIC_SHELTER,
                        GLITCHED_SHELTER,
                        PATTERNED_GROVE,
                        BARREN_GRID,
                        BELL_SHRINE,
                        WATCHING_TOWER,
                        FALSE_CAMP,
                        WRONG_VILLAGE_HOUSE,
                        WRONG_VILLAGE_UTILITY,
                        SINKHOLE,
                        OBSERVATION_PLATFORM,
                        WRONG_ROAD_SEGMENT,
                        FALSE_ENTRANCE,
                        STORAGE_SHED -> new BlockPos(x, surfaceY, z);
                case FALSE_DESCENT -> new BlockPos(x, surfaceY + 1, z);
                case FALSE_ASCENT -> new BlockPos(x, surfaceY + 1, z);
                case ISOLATION_CUBE -> {
                    int minY = level.getMinBuildHeight() + 6;
                    int maxY = Math.max(minY + 1, surfaceY - 4);
                    if (maxY <= minY) {
                        yield null;
                    }
                    int y = minY + level.random.nextInt(maxY - minY + 1);
                    yield new BlockPos(x, y, z);
                }
            };

            if (origin == null) {
                nullOrigin++;
                continue;
            }
            if (requireFreshChunk && !isFreshOriginChunk(level, origin)) {
                staleChunk++;
                continue;
            }
            if (!ignoreMarkers) {
                if (state.hasAnyStructureMarkerNearby(level.dimension(), origin, feature.exclusionRadius)) {
                    markerRejected++;
                    continue;
                }
                if (state.hasStructureMarkerNearby(feature.id, level.dimension(), origin, feature.sameTypeExclusionRadius)) {
                    markerRejected++;
                    continue;
                }
            }
            if (!isValidOrigin(level, feature, origin, ignoreMarkers)) {
                invalidOrigin++;
                continue;
            }
            return origin;
        }

        if (ignoreMarkers) {
            int[][] offsets = {
                    {16, 0}, {-16, 0}, {0, 16}, {0, -16},
                    {22, 10}, {-22, 10}, {22, -10}, {-22, -10}
            };
            for (int[] offset : offsets) {
                int x = anchor.blockPosition().getX() + offset[0];
                int z = anchor.blockPosition().getZ() + offset[1];
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos forced = switch (feature) {
                    case ANECHOIC_CUBE -> {
                        int minY = level.getMinBuildHeight() + 8;
                        int maxY = Math.max(minY + 1, surfaceY - 8);
                        if (maxY <= minY) {
                            yield null;
                        }
                        yield new BlockPos(x, minY + level.random.nextInt(maxY - minY + 1), z);
                    }
                    case MIMIC_SHELTER,
                            GLITCHED_SHELTER,
                            PATTERNED_GROVE,
                            BARREN_GRID,
                            BELL_SHRINE,
                            WATCHING_TOWER,
                            FALSE_CAMP,
                            WRONG_VILLAGE_HOUSE,
                            WRONG_VILLAGE_UTILITY,
                            SINKHOLE,
                            OBSERVATION_PLATFORM,
                            WRONG_ROAD_SEGMENT,
                            FALSE_ENTRANCE,
                            STORAGE_SHED -> new BlockPos(x, surfaceY, z);
                    case FALSE_DESCENT -> new BlockPos(x, surfaceY + 1, z);
                    case FALSE_ASCENT -> new BlockPos(x, surfaceY + 1, z);
                    case ISOLATION_CUBE -> {
                        int minY = level.getMinBuildHeight() + 6;
                        int maxY = Math.max(minY + 1, surfaceY - 4);
                        if (maxY <= minY) {
                            yield null;
                        }
                        yield new BlockPos(x, minY + level.random.nextInt(maxY - minY + 1), z);
                    }
                };
                if (forced != null && isValidOrigin(level, feature, forced, true)) {
                    debugLog("FEATURE origin-fallback type={} origin={} anchor={}", feature.id, forced, anchor.blockPosition());
                    return forced;
                }
            }
        }

        debugLog(
                "FEATURE origin-fail type={} near=true ignoreMarkers={} attempts={} notLoaded={} nullOrigin={} markerRejected={} invalidOrigin={} staleChunk={} anchor={}",
                feature.id,
                ignoreMarkers,
                attempts,
                notLoaded,
                nullOrigin,
                markerRejected,
                invalidOrigin,
                staleChunk,
                anchor.blockPosition());
        return null;
    }

    private static boolean isFreshOriginChunk(ServerLevel level, BlockPos origin) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(origin.getX() >> 4, origin.getZ() >> 4);
        if (chunk == null) {
            return false;
        }
        // Prevent sudden pop-in on long-explored terrain: only allow near-new chunks.
        return chunk.getInhabitedTime() <= 20L * 30L;
    }

    private static boolean isValidOrigin(ServerLevel level, FeatureType feature, BlockPos origin, boolean relaxed) {
        if (!level.hasChunkAt(origin)) {
            return false;
        }

        return switch (feature) {
            case ANECHOIC_CUBE -> canGenerateAnechoic(level, origin, relaxed);
            case MIMIC_SHELTER,
                    GLITCHED_SHELTER,
                    BELL_SHRINE,
                    WATCHING_TOWER,
                    FALSE_CAMP,
                    WRONG_VILLAGE_HOUSE,
                    WRONG_VILLAGE_UTILITY,
                    SINKHOLE,
                    OBSERVATION_PLATFORM,
                    WRONG_ROAD_SEGMENT,
                    FALSE_ENTRANCE,
                    STORAGE_SHED -> {
                if (!relaxed && !hasLoadedArea(level, origin, 4)) {
                    yield false;
                }
                BlockPos support = findSolidSupportBelow(level, origin.below(), 6);
                if (support == null) {
                    yield false;
                }
                BlockState base = level.getBlockState(support);
                yield base.isSolidRender(level, support)
                        && !base.getFluidState().is(FluidTags.WATER)
                        && !level.getFluidState(support.above()).is(FluidTags.WATER);
            }
            case PATTERNED_GROVE, BARREN_GRID -> relaxed || hasLoadedArea(level, origin, 18);
            case FALSE_DESCENT -> origin.getY() - 34 > level.getMinBuildHeight();
            case FALSE_ASCENT -> origin.getY() + 46 < level.getMaxBuildHeight();
            case ISOLATION_CUBE -> {
                if (!relaxed && !hasLoadedArea(level, origin, 4)) {
                    yield false;
                }
                yield origin.getY() - 4 > level.getMinBuildHeight();
            }
        };
    }

    private static boolean canGenerateAnechoic(ServerLevel level, BlockPos center, boolean relaxed) {
        if (!relaxed && !hasLoadedArea(level, center, 5)) {
            return false;
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos target = center.offset(dx, dy, dz);
                    if (target.getY() <= level.getMinBuildHeight() + 1 || target.getY() >= level.getMaxBuildHeight() - 1) {
                        return false;
                    }
                    if (level.getBlockState(target).is(Blocks.BEDROCK)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean hasLoadedArea(ServerLevel level, BlockPos center, int radius) {
        if (Boolean.TRUE.equals(WORLDGEN_PASS.get())) {
            return true;
        }
        return level.hasChunkAt(center.offset(radius, 0, radius))
                && level.hasChunkAt(center.offset(-radius, 0, radius))
                && level.hasChunkAt(center.offset(radius, 0, -radius))
                && level.hasChunkAt(center.offset(-radius, 0, -radius));
    }

    private static boolean generateAnechoicCube(ServerLevel level, BlockPos center) {
        if (!canGenerateAnechoic(level, center, false)) {
            debugLog("FEATURE anechoic rejected center={} reason=invalid-volume", center);
            return false;
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos target = center.offset(dx, dy, dz);
                    boolean shell = Math.abs(dx) == 4 || Math.abs(dy) == 4 || Math.abs(dz) == 4;
                    level.setBlock(target, shell ? UncannyBlockRegistry.UNCANNY_BLOCK.get().defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        return true;
    }

    private static boolean generateMimicShelter(ServerLevel level, BlockPos center) {
        int floorY = center.getY();
        if (floorY <= level.getMinBuildHeight() + 1 || floorY + 5 >= level.getMaxBuildHeight()) {
            debugLog("FEATURE mimic_shelter rejected center={} reason=height-out-of-range", center);
            return false;
        }

        String[] variants = {
                "classic",
                "bed_wall",
                "furnace_blocker",
                "side_door",
                "lore_corner",
                "lore_clutter"
        };
        int variant = pickVariantIndex(level, "mimic_shelter", variants);
        BlockPos corner = center.offset(-2, 0, -2);
        BlockState plank = Blocks.OAK_PLANKS.defaultBlockState();

        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                BlockPos floor = corner.offset(dx, 0, dz);
                BlockPos under = floor.below();
                if (level.getBlockState(under).isAir()) {
                    level.setBlock(under, Blocks.DIRT.defaultBlockState(), 3);
                }
                level.setBlock(floor, plank, 3);

                for (int dy = 1; dy <= 3; dy++) {
                    BlockPos wall = corner.offset(dx, dy, dz);
                    boolean border = dx == 0 || dx == 4 || dz == 0 || dz == 4;
                    level.setBlock(wall, border ? plank : Blocks.AIR.defaultBlockState(), 3);
                }
                level.setBlock(corner.offset(dx, 4, dz), plank, 3);
            }
        }

        // Default door on south wall; side_door variant moves it to east wall.
        if (variant == 3) {
            placeSimpleDoor(level, corner.offset(4, 1, 2), Direction.WEST);
        } else {
            placeSimpleDoor(level, corner.offset(2, 1, 4), Direction.NORTH);
        }

        switch (variant) {
            case 1 -> {
                level.setBlock(corner.offset(1, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
                level.setBlock(corner.offset(3, 1, 1), Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.SOUTH), 3);
                level.setBlock(corner.offset(3, 1, 3), Blocks.TORCH.defaultBlockState(), 3);
                BlockPos bedFoot = corner.offset(2, 1, 2);
                BlockPos bedHead = bedFoot.south();
                level.setBlock(bedFoot, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT), 3);
                level.setBlock(bedHead, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.HEAD), 3);
            }
            case 2 -> {
                level.setBlock(corner.offset(1, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
                level.setBlock(corner.offset(2, 1, 2), Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.WEST), 3);
                level.setBlock(corner.offset(3, 1, 3), Blocks.TORCH.defaultBlockState(), 3);
                BlockPos bedFoot = corner.offset(1, 1, 3);
                BlockPos bedHead = bedFoot.north();
                level.setBlock(bedFoot, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT), 3);
                level.setBlock(bedHead, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD), 3);
            }
            case 3 -> {
                level.setBlock(corner.offset(1, 1, 3), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
                level.setBlock(corner.offset(3, 1, 2), Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH), 3);
                level.setBlock(corner.offset(1, 1, 1), Blocks.TORCH.defaultBlockState(), 3);
                BlockPos bedFoot = corner.offset(2, 1, 1);
                BlockPos bedHead = bedFoot.south();
                level.setBlock(bedFoot, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT), 3);
                level.setBlock(bedHead, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.HEAD), 3);
            }
            case 4 -> {
                level.setBlock(corner.offset(1, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
                level.setBlock(corner.offset(3, 1, 2), Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.EAST), 3);
                level.setBlock(corner.offset(3, 1, 3), Blocks.TORCH.defaultBlockState(), 3);
                BlockPos bedFoot = corner.offset(2, 1, 2);
                BlockPos bedHead = bedFoot.north();
                level.setBlock(bedFoot, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT), 3);
                level.setBlock(bedHead, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD), 3);
                BlockPos chestPos = corner.offset(3, 1, 1);
                level.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), 3);
                if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                    populateExplorationChest(level, chest, 0.55D);
                }
            }
            case 5 -> {
                level.setBlock(corner.offset(1, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
                level.setBlock(corner.offset(2, 1, 1), Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.SOUTH), 3);
                level.setBlock(corner.offset(3, 1, 3), Blocks.TORCH.defaultBlockState(), 3);
                BlockPos bedFoot = corner.offset(1, 1, 3);
                BlockPos bedHead = bedFoot.east();
                level.setBlock(bedFoot, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.FOOT), 3);
                level.setBlock(bedHead, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.HEAD), 3);
                BlockPos loreChest = corner.offset(3, 1, 1);
                level.setBlock(loreChest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST), 3);
                if (level.getBlockEntity(loreChest) instanceof ChestBlockEntity chest) {
                    populateExplorationChest(level, chest, 0.70D);
                }
                BlockPos junkChest = corner.offset(2, 1, 3);
                level.setBlock(junkChest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), 3);
                if (level.getBlockEntity(junkChest) instanceof ChestBlockEntity chest) {
                    populateAmbientChest(level, chest);
                }
            }
            default -> {
                level.setBlock(corner.offset(1, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
                level.setBlock(corner.offset(4, 1, 2), Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.EAST), 3);
                level.setBlock(corner.offset(3, 1, 3), Blocks.TORCH.defaultBlockState(), 3);
                BlockPos bedFoot = corner.offset(2, 1, 2);
                BlockPos bedHead = bedFoot.north();
                level.setBlock(bedFoot, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT), 3);
                level.setBlock(bedHead, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD), 3);
            }
        }

        debugLog("FEATURE mimic_shelter variant={} origin={}", variants[variant], center);
        return true;
    }

    private static boolean generateGlitchedShelter(ServerLevel level, BlockPos center) {
        int floorY = center.getY();
        if (floorY <= level.getMinBuildHeight() + 1 || floorY + 5 >= level.getMaxBuildHeight()) {
            debugLog("FEATURE glitched_shelter rejected center={} reason=height-out-of-range", center);
            return false;
        }
        if (!hasLoadedArea(level, center, 4)) {
            debugLog("FEATURE glitched_shelter rejected center={} reason=unloaded-area", center);
            return false;
        }

        BlockPos corner = center.offset(-2, 0, -2);
        int placed = 0;
        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                BlockPos floor = corner.offset(dx, 0, dz);
                BlockPos under = floor.below();
                if (level.getBlockState(under).isAir()) {
                    level.setBlock(under, Blocks.DIRT.defaultBlockState(), 3);
                }

                for (int dy = 0; dy < 4; dy++) {
                    BlockPos target = corner.offset(dx, dy, dz);
                    boolean shell = dy == 0 || dy == 3 || dx == 0 || dx == 4 || dz == 0 || dz == 4;
                    BlockState state;
                    if (shell) {
                        // Walls/roof/floor are complete (never air).
                        if (level.random.nextFloat() < 0.25F) {
                            state = pickGlitchedShelterShellBlock(level);
                        } else {
                            state = Blocks.OAK_PLANKS.defaultBlockState();
                        }
                    } else {
                        // Interior remains air, except occasional useful blocks on interior floor.
                        if (dy == 1 && level.random.nextFloat() < 0.25F) {
                            state = pickGlitchedShelterUtilityBlock(level);
                        } else {
                            state = Blocks.AIR.defaultBlockState();
                        }
                    }

                    if (!state.is(Blocks.AIR)) {
                        placed++;
                    }
                    level.setBlock(target, state, 3);
                }
            }
        }

        // Add a usable access door on one wall.
        BlockPos doorLower = corner.offset(2, 1, 4);
        BlockPos doorUpper = doorLower.above();
        level.setBlock(doorLower, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(doorUpper, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(
                doorLower,
                Blocks.OAK_DOOR.defaultBlockState()
                        .setValue(DoorBlock.FACING, Direction.NORTH)
                        .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                        .setValue(DoorBlock.OPEN, false)
                        .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER),
                3);
        level.setBlock(
                doorUpper,
                Blocks.OAK_DOOR.defaultBlockState()
                        .setValue(DoorBlock.FACING, Direction.NORTH)
                        .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                        .setValue(DoorBlock.OPEN, false)
                        .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER),
                3);

        if (placed == 0) {
            level.setBlock(center, Blocks.OAK_PLANKS.defaultBlockState(), 3);
        }
        return true;
    }

    private static BlockState pickGlitchedShelterShellBlock(ServerLevel level) {
        return switch (level.random.nextInt(5)) {
            case 0 -> Blocks.OAK_PLANKS.defaultBlockState();
            case 1 -> Blocks.STONE_BRICKS.defaultBlockState();
            case 2 -> Blocks.GLASS.defaultBlockState();
            case 3 -> Blocks.CRAFTING_TABLE.defaultBlockState();
            default -> Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, randomHorizontal(level));
        };
    }

    private static BlockState pickGlitchedShelterUtilityBlock(ServerLevel level) {
        return switch (level.random.nextInt(4)) {
            case 0 -> Blocks.CRAFTING_TABLE.defaultBlockState();
            case 1 -> Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, randomHorizontal(level));
            case 2 -> Blocks.TORCH.defaultBlockState();
            default -> Blocks.RED_BED.defaultBlockState()
                    .setValue(BedBlock.FACING, randomHorizontal(level))
                    .setValue(BedBlock.PART, level.random.nextBoolean() ? BedPart.HEAD : BedPart.FOOT);
        };
    }

    private static Direction randomHorizontal(ServerLevel level) {
        return switch (level.random.nextInt(4)) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }

    private static boolean generatePatternedGrove(ServerLevel level, BlockPos center) {
        int y = center.getY();
        if (y <= level.getMinBuildHeight() + 1 || y + 10 >= level.getMaxBuildHeight()) {
            debugLog("FEATURE patterned_grove rejected center={} reason=height-out-of-range", center);
            return false;
        }
        if (!hasLoadedArea(level, center, 18)) {
            debugLog("FEATURE patterned_grove rejected center={} reason=unloaded-area", center);
            return false;
        }

        for (int dx = -15; dx < 15; dx++) {
            for (int dz = -15; dz < 15; dz++) {
                BlockPos ground = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                BlockPos below = ground.below();
                if (level.getBlockState(below).isAir()) {
                    level.setBlock(below, Blocks.DIRT.defaultBlockState(), 3);
                }
                level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                for (int up = 1; up <= 8; up++) {
                    level.setBlock(ground.above(up), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        for (int dx = -10; dx <= 10; dx += 5) {
            for (int dz = -10; dz <= 10; dz += 5) {
                BlockPos base = new BlockPos(center.getX() + dx, y + 1, center.getZ() + dz);
                placePatternedTree(level, base);
            }
        }
        return true;
    }

    private static boolean generateBarrenGrid(ServerLevel level, BlockPos center) {
        if (!generatePatternedGrove(level, center)) {
            return false;
        }
        int y = center.getY();
        for (int dx = -15; dx < 15; dx++) {
            for (int dz = -15; dz < 15; dz++) {
                BlockPos ground = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                if (level.getBlockState(ground).is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(ground, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                }
                for (int dy = 0; dy <= 24; dy++) {
                    BlockPos target = ground.above(dy);
                    if (level.getBlockState(target).is(Blocks.OAK_LEAVES)) {
                        level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        return true;
    }

    private static void placePatternedTree(ServerLevel level, BlockPos base) {
        if (!placeVanillaOak(level, base)) {
            placeSimpleOak(level, base);
        }
    }

    private static boolean placeVanillaOak(ServerLevel level, BlockPos base) {
        try {
            var configuredLookup = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
            var oakHolder = configuredLookup.get(TreeFeatures.OAK);
            if (oakHolder.isEmpty()) {
                debugLog("FEATURE patterned_grove tree-fallback base={} reason=missing-oak-feature", base);
                return false;
            }
            ConfiguredFeature<?, ?> oak = oakHolder.get().value();
            boolean placed = oak.place(level, level.getChunkSource().getGenerator(), level.random, base);
            if (!placed) {
                debugLog("FEATURE patterned_grove tree-fallback base={} reason=oak-place-failed", base);
            }
            return placed;
        } catch (Exception exception) {
            debugLog("FEATURE patterned_grove tree-fallback base={} reason=oak-exception {}", base, exception.toString());
            return false;
        }
    }

    private static void placeSimpleOak(ServerLevel level, BlockPos base) {
        int trunkHeight = 4;
        for (int i = 0; i < trunkHeight; i++) {
            level.setBlock(base.above(i), Blocks.OAK_LOG.defaultBlockState(), 3);
        }

        int crownY = base.getY() + trunkHeight - 1;
        for (int dy = 0; dy <= 2; dy++) {
            int radius = dy == 2 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && dy < 2) {
                        continue;
                    }
                    BlockPos leaf = new BlockPos(base.getX() + dx, crownY + dy, base.getZ() + dz);
                    if (level.getBlockState(leaf).isAir() || level.getBlockState(leaf).is(Blocks.OAK_LEAVES)) {
                        level.setBlock(leaf, Blocks.OAK_LEAVES.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static boolean generateFalseDescent(ServerLevel level, BlockPos topCenter) {
        return generateFalseDescent(level, topCenter, false);
    }

    private static boolean generateFalseDescent(ServerLevel level, BlockPos topCenter, boolean forceAttachSecretHouse) {
        final int maxDepth = 30;
        int minStartY = level.getMinBuildHeight() + maxDepth + 5;
        if (topCenter.getY() < minStartY) {
            topCenter = new BlockPos(topCenter.getX(), minStartY, topCenter.getZ());
        }
        if (!hasLoadedArea(level, topCenter, 3)) {
            debugLog("FEATURE false_descent rejected center={} reason=unloaded-area", topCenter);
            return false;
        }
        boolean attachSecretHouse = forceAttachSecretHouse || level.random.nextDouble() < FALSE_SPIRAL_SECRET_HOUSE_CHANCE;
        debugLog(
                "FEATURE false_descent attach-roll force={} requested={} chance={}",
                forceAttachSecretHouse,
                attachSecretHouse,
                FALSE_SPIRAL_SECRET_HOUSE_CHANCE);
        return generateFalseSpiral(level, topCenter, maxDepth, -1, true, attachSecretHouse, FeatureType.FALSE_DESCENT);
    }

    private static boolean generateFalseAscent(ServerLevel level, BlockPos bottomCenter) {
        return generateFalseAscent(level, bottomCenter, false);
    }

    private static boolean generateFalseAscent(ServerLevel level, BlockPos bottomCenter, boolean forceAttachSecretHouse) {
        final int maxHeight = 40;
        int maxStartY = level.getMaxBuildHeight() - (maxHeight + 7);
        if (bottomCenter.getY() > maxStartY) {
            bottomCenter = new BlockPos(bottomCenter.getX(), maxStartY, bottomCenter.getZ());
        }
        if (!hasLoadedArea(level, bottomCenter, 3)) {
            debugLog("FEATURE false_ascent rejected center={} reason=unloaded-area", bottomCenter);
            return false;
        }
        boolean attachSecretHouse = forceAttachSecretHouse || level.random.nextDouble() < FALSE_SPIRAL_SECRET_HOUSE_CHANCE;
        debugLog(
                "FEATURE false_ascent attach-roll force={} requested={} chance={}",
                forceAttachSecretHouse,
                attachSecretHouse,
                FALSE_SPIRAL_SECRET_HOUSE_CHANCE);
        return generateFalseSpiral(level, bottomCenter, maxHeight, 1, true, attachSecretHouse, FeatureType.FALSE_ASCENT);
    }

    private static boolean generateFalseSpiral(
            ServerLevel level,
            BlockPos center,
            int levels,
            int verticalStep,
            boolean placeVoidDoorAtEnd,
            boolean attachSecretHouse,
            FeatureType feature) {
        final Direction[] clockwiseCycle = {
                Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
        };

        BlockPos cursor = center.offset(-1, 0, 1);
        int step = 0;
        int stairsPlaced = 0;
        Direction lastDirection = Direction.NORTH;
        List<BlockPos> carvedStairPositions = new ArrayList<>();
        while (step < levels) {
            Direction direction = clockwiseCycle[(step / 2) % clockwiseCycle.length];
            BlockPos stairPos = cursor.offset(0, verticalStep, 0).relative(direction);
            int y = stairPos.getY();
            if (y <= level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 2) {
                debugLog("FEATURE {} aborted center={} step={} y={} reason=out-of-bounds", feature.id, center, step, y);
                return false;
            }

            BlockPos layerCenter = new BlockPos(center.getX(), y, center.getZ());
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos layer = layerCenter.offset(dx, 0, dz);
                    boolean ring = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                    level.setBlock(layer, ring ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }

            BlockPos support = stairPos.below();
            if (level.getBlockState(support).isAir()) {
                level.setBlock(support, Blocks.STONE_BRICKS.defaultBlockState(), 3);
            }

            level.setBlock(
                    stairPos,
                    Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                            .setValue(
                                    HorizontalDirectionalBlock.FACING,
                                    verticalStep > 0 ? direction : direction.getOpposite()),
                    3);
            carvedStairPositions.add(stairPos.immutable());

            stairsPlaced++;
            cursor = stairPos;
            lastDirection = direction;
            step++;
        }

        // Final pass: keep 4 blocks of headroom over each stair even after later layers are built.
        for (BlockPos stairPos : carvedStairPositions) {
            for (int up = 1; up <= 4; up++) {
                BlockPos headroom = stairPos.above(up);
                if (headroom.getY() >= level.getMaxBuildHeight() - 1 || headroom.getY() <= level.getMinBuildHeight() + 1) {
                    break;
                }
                level.setBlock(headroom, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        if (placeVoidDoorAtEnd) {
            BlockPos doorLower = cursor.above().relative(lastDirection);
            BlockPos doorUpper = doorLower.above();
            if (doorUpper.getY() < level.getMaxBuildHeight() - 1) {
                boolean attached = false;
                if (attachSecretHouse) {
                    if (feature == FeatureType.FALSE_ASCENT || feature == FeatureType.FALSE_DESCENT) {
                        attached = attachSpiralSecretHouse(level, feature, doorLower, lastDirection);
                    } else {
                        attached = attachConnectedSecretHouse(level, feature, doorLower, lastDirection, true);
                    }
                }
                if (!attached) {
                    placeBlackDoor(level, doorLower, lastDirection);
                }

                BlockPos approachLower = doorLower.relative(lastDirection);
                BlockPos approachUpper = approachLower.above();
                level.setBlock(approachLower, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(approachUpper, Blocks.AIR.defaultBlockState(), 3);
                if (level.getBlockState(approachLower.below()).isAir()) {
                    level.setBlock(approachLower.below(), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                }
                debugLog(
                        "FEATURE {} end-door={} attachRequested={} attached={}",
                        feature.id,
                        doorLower,
                        attachSecretHouse,
                        attached);
            }
        }

        debugLog(
                "FEATURE {} built center={} levels={} stairsPlaced={} pattern=NN-EE-SS-WW verticalStep={}",
                feature.id,
                center,
                step,
                stairsPlaced,
                verticalStep);
        return true;
    }

    private static boolean generateIsolationCube(ServerLevel level, BlockPos center) {
        if (!hasLoadedArea(level, center, 4)) {
            debugLog("FEATURE isolation_cube rejected center={} reason=unloaded-area", center);
            return false;
        }
        if (center.getY() - 4 <= level.getMinBuildHeight() || center.getY() + 4 >= level.getMaxBuildHeight()) {
            debugLog("FEATURE isolation_cube rejected center={} reason=height-out-of-range", center);
            return false;
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos target = center.offset(dx, dy, dz);
                    boolean shell = Math.abs(dx) == 3 || Math.abs(dy) == 3 || Math.abs(dz) == 3;
                    level.setBlock(target, shell ? Blocks.GLASS.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        level.setBlock(center, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        BlockPos torchPos = center.above();
        if (torchPos.getY() < level.getMaxBuildHeight() - 1) {
            level.setBlock(torchPos, Blocks.TORCH.defaultBlockState(), 3);
        }
        return true;
    }

    private static int pickVariantIndex(ServerLevel level, String featureId, String... variants) {
        if (variants == null || variants.length == 0) {
            return 0;
        }
        String forced = FORCED_STRUCTURE_VARIANT.get();
        if (forced != null && !forced.isBlank()) {
            for (int i = 0; i < variants.length; i++) {
                if (variants[i].equalsIgnoreCase(forced) || Integer.toString(i + 1).equals(forced)) {
                    return i;
                }
            }
            debugLog("FEATURE {} forced-variant '{}' not found in {}", featureId, forced, String.join(",", variants));
        }
        return level.random.nextInt(variants.length);
    }

    private static void fillBox(ServerLevel level, BlockPos from, BlockPos to, BlockState state) {
        int minX = Math.min(from.getX(), to.getX());
        int maxX = Math.max(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int maxY = Math.max(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxZ = Math.max(from.getZ(), to.getZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    level.setBlock(new BlockPos(x, y, z), state, 3);
                }
            }
        }
    }

    private static void clearBox(ServerLevel level, BlockPos from, BlockPos to) {
        fillBox(level, from, to, Blocks.AIR.defaultBlockState());
    }

    private static void placeSimpleDoor(ServerLevel level, BlockPos lower, Direction facing) {
        level.setBlock(lower, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(lower.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(
                lower,
                Blocks.OAK_DOOR.defaultBlockState()
                        .setValue(DoorBlock.FACING, facing)
                        .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                        .setValue(DoorBlock.OPEN, false)
                        .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER),
                3);
        level.setBlock(
                lower.above(),
                Blocks.OAK_DOOR.defaultBlockState()
                        .setValue(DoorBlock.FACING, facing)
                        .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                        .setValue(DoorBlock.OPEN, false)
                        .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER),
                3);
    }

    private static void placeSimpleBed(ServerLevel level, BlockPos footPos, Direction facing, BlockState bedState) {
        BlockPos headPos = footPos.relative(facing);
        level.setBlock(footPos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(headPos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(
                footPos,
                bedState.setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.FOOT),
                3);
        level.setBlock(
                headPos,
                bedState.setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.HEAD),
                3);
    }

    private static void placeFalseDescentStyleStairs(ServerLevel level, BlockPos center, int levels) {
        final Direction[] clockwiseCycle = {
                Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
        };
        BlockPos cursor = center.offset(-1, 0, 1);
        List<BlockPos> carvedStairs = new ArrayList<>();

        for (int step = 0; step < levels; step++) {
            Direction direction = clockwiseCycle[(step / 2) % clockwiseCycle.length];
            BlockPos stairPos = cursor.below().relative(direction);
            int y = stairPos.getY();
            if (y <= level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 2) {
                break;
            }

            BlockPos layerCenter = new BlockPos(center.getX(), y, center.getZ());
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos layer = layerCenter.offset(dx, 0, dz);
                    boolean ring = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                    level.setBlock(layer, ring ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }

            BlockPos support = stairPos.below();
            if (level.getBlockState(support).isAir()) {
                level.setBlock(support, Blocks.STONE_BRICKS.defaultBlockState(), 3);
            }
            level.setBlock(
                    stairPos,
                    Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, direction.getOpposite()),
                    3);
            carvedStairs.add(stairPos.immutable());
            cursor = stairPos;
        }

        for (BlockPos stairPos : carvedStairs) {
            for (int up = 1; up <= 4; up++) {
                BlockPos headroom = stairPos.above(up);
                if (headroom.getY() >= level.getMaxBuildHeight() - 1 || headroom.getY() <= level.getMinBuildHeight() + 1) {
                    break;
                }
                level.setBlock(headroom, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void populateAmbientChest(ServerLevel level, ChestBlockEntity chest) {
        chest.clearContent();
        int fills = 2 + level.random.nextInt(4);
        for (int i = 0; i < fills; i++) {
            int slot = level.random.nextInt(chest.getContainerSize());
            if (chest.getItem(slot).isEmpty()) {
                chest.setItem(slot, createRandomJunkLoot(level));
            }
        }
        chest.setChanged();
    }

    private static void populateExplorationChest(ServerLevel level, ChestBlockEntity chest, double loreChance) {
        if (level.random.nextDouble() < loreChance) {
            populateSecretHouseChest(level, chest);
            return;
        }
        populateAmbientChest(level, chest);
    }

    private static void placeBellCore(ServerLevel level, BlockPos bellPos) {
        BlockPos support = bellPos.below();
        if (level.getBlockState(support).isAir()) {
            level.setBlock(support, Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }
        level.setBlock(bellPos, Blocks.BELL.defaultBlockState(), 3);
    }

    private static boolean generateBellShrine(ServerLevel level, BlockPos origin) {
        if (!hasLoadedArea(level, origin, 8)) {
            return false;
        }
        String[] variants = {"open", "ruined", "closed", "hilltop", "forest", "buried"};
        int variant = pickVariantIndex(level, "bell_shrine", variants);
        BlockPos center = origin;
        if (variant == 3) {
            int bestY = origin.getY();
            BlockPos best = origin;
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX() + dx, origin.getZ() + dz);
                    if (y > bestY) {
                        bestY = y;
                        best = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
                    }
                }
            }
            center = best;
        }
        if (variant == 5) {
            center = center.below(2);
        }

        int y = center.getY();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos floor = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                if (level.getBlockState(floor.below()).isAir()) {
                    level.setBlock(floor.below(), Blocks.COBBLESTONE.defaultBlockState(), 3);
                }
                level.setBlock(floor, Blocks.STONE_BRICKS.defaultBlockState(), 3);
                for (int dy = 1; dy <= 5; dy++) {
                    level.setBlock(floor.above(dy), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        if (variant == 0 || variant == 2 || variant == 3 || variant == 4) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) != 2 && Math.abs(dz) != 2) {
                        continue;
                    }
                    BlockPos post = new BlockPos(center.getX() + dx, y + 1, center.getZ() + dz);
                    BlockState wall = variant == 4 ? Blocks.OAK_LOG.defaultBlockState() : Blocks.COBBLESTONE_WALL.defaultBlockState();
                    level.setBlock(post, wall, 3);
                    if (variant == 2) {
                        level.setBlock(post.above(), wall, 3);
                    }
                }
            }
        }

        if (variant == 2) {
            fillBox(level, center.offset(-2, y + 3 - center.getY(), -2), center.offset(2, y + 3 - center.getY(), 2), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            placeSimpleDoor(level, new BlockPos(center.getX(), y + 1, center.getZ() - 2), Direction.NORTH);
        } else if (variant == 1) {
            for (int i = 0; i < 16; i++) {
                BlockPos breakPos = center.offset(level.random.nextInt(5) - 2, level.random.nextInt(3), level.random.nextInt(5) - 2);
                level.setBlock(breakPos, Blocks.AIR.defaultBlockState(), 3);
            }
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (int dy = 0; dy <= 2; dy++) {
                        BlockPos target = center.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(target);
                        if ((state.is(Blocks.COBBLESTONE) || state.is(Blocks.STONE_BRICKS) || state.is(Blocks.COBBLESTONE_WALL))
                                && level.random.nextDouble() < 0.32D) {
                            level.setBlock(
                                    target,
                                    level.random.nextBoolean()
                                            ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                                            : Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                                    3);
                        }
                    }
                }
            }
        } else if (variant == 4) {
            for (int i = 0; i < 14; i++) {
                BlockPos leaf = center.offset(level.random.nextInt(9) - 4, 1 + level.random.nextInt(3), level.random.nextInt(9) - 4);
                if (level.getBlockState(leaf).isAir()) {
                    level.setBlock(leaf, Blocks.OAK_LEAVES.defaultBlockState(), 3);
                }
            }
        } else if (variant == 5) {
            fillBox(level, center.offset(-3, -1, -3), center.offset(3, 0, 3), Blocks.DEEPSLATE.defaultBlockState());
        }

        placeBellCore(level, center.above(1));
        debugLog("FEATURE bell_shrine variant={} center={}", variants[variant], center);
        return true;
    }

    private static void buildTowerCore(
            ServerLevel level,
            BlockPos center,
            int radius,
            int height,
            BlockState wallBlock,
            boolean broken,
            boolean closedTop) {
        int x0 = center.getX();
        int y0 = center.getY();
        int z0 = center.getZ();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos floor = new BlockPos(x0 + dx, y0, z0 + dz);
                if (level.getBlockState(floor.below()).isAir()) {
                    level.setBlock(floor.below(), wallBlock, 3);
                }
                level.setBlock(floor, wallBlock, 3);
            }
        }

        for (int dy = 1; dy <= height; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos target = new BlockPos(x0 + dx, y0 + dy, z0 + dz);
                    boolean wall = Math.abs(dx) == radius || Math.abs(dz) == radius;
                    if (wall) {
                        level.setBlock(target, wallBlock, 3);
                    } else {
                        level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        int topY = y0 + height + 1;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos top = new BlockPos(x0 + dx, topY, z0 + dz);
                level.setBlock(top, wallBlock, 3);
            }
        }
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                    continue;
                }
                level.setBlock(new BlockPos(x0 + dx, topY + 1, z0 + dz), Blocks.OAK_FENCE.defaultBlockState(), 3);
            }
        }
        if (closedTop) {
            fillBox(level, new BlockPos(x0 - radius, topY + 2, z0 - radius), new BlockPos(x0 + radius, topY + 2, z0 + radius), wallBlock);
        }

        if (broken) {
            for (int i = 0; i < 10; i++) {
                BlockPos breakPos = new BlockPos(
                        x0 + level.random.nextInt(radius * 2 + 1) - radius,
                        y0 + height - 2 + level.random.nextInt(4),
                        z0 + level.random.nextInt(radius * 2 + 1) - radius);
                level.setBlock(breakPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static boolean generateWatchingTower(ServerLevel level, BlockPos origin) {
        if (!hasLoadedArea(level, origin, 12)) {
            return false;
        }
        String[] variants = {"wooden", "stone", "broken", "overbuilt", "twin", "closed"};
        int variant = pickVariantIndex(level, "watching_tower", variants);

        switch (variant) {
            case 0 -> buildTowerCore(level, origin, 2, 8, Blocks.OAK_LOG.defaultBlockState(), false, false);
            case 1 -> buildTowerCore(level, origin, 2, 9, Blocks.STONE_BRICKS.defaultBlockState(), false, false);
            case 2 -> buildTowerCore(level, origin, 2, 9, Blocks.COBBLESTONE.defaultBlockState(), true, false);
            case 3 -> buildTowerCore(level, origin, 1, 17, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), false, false);
            case 4 -> {
                buildTowerCore(level, origin.offset(-5, 0, 0), 2, 8, Blocks.STONE_BRICKS.defaultBlockState(), true, false);
                buildTowerCore(level, origin.offset(5, 0, 0), 2, 10, Blocks.STONE_BRICKS.defaultBlockState(), false, false);
                fillBox(level, origin.offset(-3, 7, -1), origin.offset(3, 7, 1), Blocks.OAK_PLANKS.defaultBlockState());
            }
            case 5 -> buildTowerCore(level, origin, 2, 10, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), false, true);
            default -> buildTowerCore(level, origin, 2, 8, Blocks.STONE_BRICKS.defaultBlockState(), false, false);
        }

        BlockPos chestPos = origin.above(2);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, randomHorizontal(level)), 3);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            populateExplorationChest(level, chest, 0.18D);
        }
        debugLog("FEATURE watching_tower variant={} origin={}", variants[variant], origin);
        return true;
    }

    private static boolean generateFalseCamp(ServerLevel level, BlockPos origin) {
        if (!hasLoadedArea(level, origin, 10)) {
            return false;
        }
        String[] variants = {"fresh", "looted", "interrupted", "minimal", "travel", "water", "under_cliff"};
        int variant = pickVariantIndex(level, "false_camp", variants);

        BlockPos center = origin;
        if (variant == 5) {
            BlockPos water = findNearbyWater(level, origin, 10);
            if (water != null) {
                center = water.above();
            }
        }

        Direction facing = randomHorizontal(level);
        fillBox(level, center.offset(-4, 0, -4), center.offset(4, 0, 4), Blocks.COARSE_DIRT.defaultBlockState());
        BlockPos chestPos = center.offset(2, 1, -1);

        switch (variant) {
            case 0 -> { // fresh
                level.setBlock(center, Blocks.CAMPFIRE.defaultBlockState(), 3);
                fillBox(level, center.offset(-2, 1, -2), center.offset(-1, 2, -1), Blocks.OAK_LOG.defaultBlockState());
                placeSimpleBed(level, center.offset(1, 1, 1), Direction.SOUTH, Blocks.RED_BED.defaultBlockState());
                level.setBlock(center.offset(-2, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
                level.setBlock(center.offset(-2, 1, 2), Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.EAST), 3);
                chestPos = center.offset(2, 1, 0);
            }
            case 1 -> { // looted
                level.setBlock(center, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false), 3);
                fillBox(level, center.offset(-3, 1, -1), center.offset(3, 1, 1), Blocks.OAK_SLAB.defaultBlockState());
                level.setBlock(center.offset(-1, 1, 2), Blocks.BARREL.defaultBlockState(), 3);
                level.setBlock(center.offset(1, 1, 2), Blocks.AIR.defaultBlockState(), 3);
                chestPos = center.offset(-2, 1, 2);
            }
            case 2 -> { // interrupted
                level.setBlock(center, Blocks.CAMPFIRE.defaultBlockState(), 3);
                fillBox(level, center.offset(-2, 1, -2), center.offset(2, 1, -2), Blocks.COBWEB.defaultBlockState());
                fillBox(level, center.offset(-3, 1, 2), center.offset(3, 1, 2), Blocks.COBWEB.defaultBlockState());
                placeSimpleBed(level, center.offset(2, 1, 0), Direction.WEST, Blocks.BROWN_BED.defaultBlockState());
                level.setBlock(center.offset(-2, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
                level.setBlock(center.offset(-3, 1, 1), Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.EAST), 3);
                chestPos = center.offset(0, 1, 3);
            }
            case 3 -> { // minimal
                level.setBlock(center, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false), 3);
                fillBox(level, center.offset(-1, 1, -1), center.offset(1, 1, 1), Blocks.AIR.defaultBlockState());
                placeSimpleBed(level, center.offset(1, 1, -1), Direction.EAST, Blocks.WHITE_BED.defaultBlockState());
                chestPos = center.offset(-1, 1, 1);
            }
            case 4 -> { // travel
                fillBox(level, center.offset(-4, 0, 0), center.offset(4, 0, 0), Blocks.DIRT_PATH.defaultBlockState());
                level.setBlock(center.offset(-1, 1, 0), Blocks.CAMPFIRE.defaultBlockState(), 3);
                placeSimpleBed(level, center.offset(2, 1, -1), Direction.SOUTH, Blocks.GRAY_BED.defaultBlockState());
                level.setBlock(center.offset(-3, 1, -1), Blocks.BARREL.defaultBlockState(), 3);
                level.setBlock(center.offset(-3, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
                chestPos = center.offset(3, 1, 1);
            }
            case 5 -> { // water
                fillBox(level, center.offset(-3, 0, -1), center.offset(3, 0, 1), Blocks.OAK_PLANKS.defaultBlockState());
                fillBox(level, center.offset(-1, 0, 2), center.offset(1, 0, 4), Blocks.OAK_PLANKS.defaultBlockState());
                level.setBlock(center, Blocks.CAMPFIRE.defaultBlockState(), 3);
                placeSimpleBed(level, center.offset(2, 1, -1), Direction.SOUTH, Blocks.BLUE_BED.defaultBlockState());
                level.setBlock(center.offset(-2, 1, -1), Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.EAST), 3);
                chestPos = center.offset(0, 1, 4);
            }
            case 6 -> { // under cliff (only variant with door)
                fillBox(level, center.offset(-3, 0, -3), center.offset(3, 0, 3), Blocks.COBBLESTONE.defaultBlockState());
                fillBox(level, center.offset(-3, 3, -3), center.offset(3, 4, 3), Blocks.STONE.defaultBlockState());
                clearBox(level, center.offset(-2, 1, -2), center.offset(2, 3, 2));
                placeSimpleDoor(level, center.offset(0, 1, -2), facing);
                level.setBlock(center.offset(0, 1, 0), Blocks.CAMPFIRE.defaultBlockState(), 3);
                placeSimpleBed(level, center.offset(1, 1, 1), Direction.WEST, Blocks.RED_BED.defaultBlockState());
                level.setBlock(center.offset(-1, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
                level.setBlock(center.offset(-2, 1, 1), Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.EAST), 3);
                chestPos = center.offset(2, 1, 1);
            }
            default -> {
                level.setBlock(center, Blocks.CAMPFIRE.defaultBlockState(), 3);
                placeSimpleBed(level, center.offset(1, 1, 1), Direction.SOUTH, Blocks.RED_BED.defaultBlockState());
            }
        }

        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), 3);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            populateExplorationChest(level, chest, 0.22D);
        }
        debugLog("FEATURE false_camp variant={} origin={}", variants[variant], center);
        return true;
    }

    private static void buildVillageLikeHouseShell(
            ServerLevel level,
            BlockPos corner,
            int width,
            int length,
            int height,
            Direction entranceFacing) {
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < length; dz++) {
                BlockPos floor = corner.offset(dx, 0, dz);
                if (level.getBlockState(floor.below()).isAir()) {
                    level.setBlock(floor.below(), Blocks.COBBLESTONE.defaultBlockState(), 3);
                }
                level.setBlock(floor, Blocks.OAK_PLANKS.defaultBlockState(), 3);
            }
        }

        for (int dy = 1; dy <= height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < length; dz++) {
                    BlockPos target = corner.offset(dx, dy, dz);
                    boolean wall = dx == 0 || dz == 0 || dx == width - 1 || dz == length - 1;
                    if (!wall) {
                        level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                        continue;
                    }
                    boolean lowerBand = dy <= 2;
                    BlockState wallBlock = lowerBand ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState();
                    if (dy == 2 && ((dx == width / 2 && (dz == 0 || dz == length - 1)) || (dz == length / 2 && (dx == 0 || dx == width - 1)))) {
                        wallBlock = Blocks.GLASS_PANE.defaultBlockState();
                    }
                    level.setBlock(target, wallBlock, 3);
                }
            }
        }

        fillBox(level, corner.offset(0, height + 1, 0), corner.offset(width - 1, height + 1, length - 1), Blocks.OAK_STAIRS.defaultBlockState());
        BlockPos door = switch (entranceFacing) {
            case NORTH -> corner.offset(width / 2, 1, 0);
            case SOUTH -> corner.offset(width / 2, 1, length - 1);
            case EAST -> corner.offset(width - 1, 1, length / 2);
            default -> corner.offset(0, 1, length / 2);
        };
        placeSimpleDoor(level, door, entranceFacing);
    }

    private static void placeWrongHouseInterior(
            ServerLevel level,
            BlockPos corner,
            int width,
            int length,
            int height,
            Direction entranceFacing) {
        BlockPos center = corner.offset(width / 2, 1, length / 2);

        // Deliberately wrong but readable interior logic.
        level.setBlock(center, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, entranceFacing), 3);
        level.setBlock(center.relative(entranceFacing), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        level.setBlock(center.relative(entranceFacing.getClockWise()), Blocks.OAK_STAIRS.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, entranceFacing), 3);

        BlockPos bedFoot = corner.offset(1, 1, 1);
        level.setBlock(bedFoot, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, entranceFacing.getOpposite()).setValue(BedBlock.PART, BedPart.FOOT), 3);
        level.setBlock(bedFoot.relative(entranceFacing.getOpposite()), Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, entranceFacing.getOpposite()).setValue(BedBlock.PART, BedPart.HEAD), 3);

        BlockPos absurdChest = corner.offset(width - 2, Math.min(height, 4), length - 2);
        level.setBlock(absurdChest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, entranceFacing), 3);
        if (level.getBlockEntity(absurdChest) instanceof ChestBlockEntity chest) {
            populateExplorationChest(level, chest, 0.20D);
        }
    }

    private static boolean generateWrongVillageHouse(ServerLevel level, BlockPos origin) {
        if (!hasLoadedArea(level, origin, 18)) {
            return false;
        }
        String[] variants = {
                "too_narrow", "too_tall", "too_wide", "long", "flat", "offset", "bent", "split", "gigantic", "tiny"
        };
        int variant = pickVariantIndex(level, "wrong_village_house", variants);
        Direction entrance = randomHorizontal(level);
        BlockPos corner;
        UncannyStructureVillagerEntity.SoundProfile villagerProfile = null;
        BlockPos villagerAnchor = origin.above();

        switch (variant) {
            case 0 -> {
                corner = origin.offset(-1, 0, -4);
                buildVillageLikeHouseShell(level, corner, 3, 9, 4, entrance);
                placeWrongHouseInterior(level, corner, 3, 9, 4, entrance);
                villagerAnchor = corner.offset(1, 1, 4);
            }
            case 1 -> {
                corner = origin.offset(-2, 0, -3);
                buildVillageLikeHouseShell(level, corner, 5, 7, 8, entrance);
                placeWrongHouseInterior(level, corner, 5, 7, 8, entrance);
                villagerProfile = UncannyStructureVillagerEntity.SoundProfile.HUGE_THIN;
                villagerAnchor = corner.offset(2, 1, 3);
            }
            case 2 -> {
                corner = origin.offset(-6, 0, -3);
                buildVillageLikeHouseShell(level, corner, 13, 7, 4, entrance);
                placeWrongHouseInterior(level, corner, 13, 7, 4, entrance);
                villagerProfile = UncannyStructureVillagerEntity.SoundProfile.VERY_WIDE;
                villagerAnchor = corner.offset(6, 1, 3);
            }
            case 3 -> {
                corner = origin.offset(-2, 0, -12);
                buildVillageLikeHouseShell(level, corner, 5, 24, 4, entrance);
                placeWrongHouseInterior(level, corner, 5, 24, 4, entrance);
                villagerProfile = UncannyStructureVillagerEntity.SoundProfile.VERY_LONG;
                villagerAnchor = corner.offset(2, 1, 12);
            }
            case 4 -> {
                corner = origin.offset(-4, 0, -4);
                buildVillageLikeHouseShell(level, corner, 9, 9, 2, entrance);
                placeWrongHouseInterior(level, corner, 9, 9, 2, entrance);
                villagerProfile = UncannyStructureVillagerEntity.SoundProfile.FLAT;
                villagerAnchor = corner.offset(4, 1, 4);
            }
            case 5 -> {
                corner = origin.offset(-3, 0, -3);
                buildVillageLikeHouseShell(level, corner, 7, 7, 4, entrance);
                buildVillageLikeHouseShell(level, corner.offset(3, 0, 3), 7, 7, 4, entrance.getOpposite());
                placeWrongHouseInterior(level, corner, 7, 7, 4, entrance);
                villagerAnchor = corner.offset(3, 1, 3);
            }
            case 6 -> {
                corner = origin.offset(-5, 0, -5);
                buildVillageLikeHouseShell(level, corner, 7, 11, 4, entrance);
                buildVillageLikeHouseShell(level, corner.offset(5, 0, 5), 7, 7, 4, entrance.getClockWise());
                placeWrongHouseInterior(level, corner, 7, 11, 4, entrance);
                villagerAnchor = corner.offset(3, 1, 5);
            }
            case 7 -> {
                corner = origin.offset(-6, 0, -3);
                buildVillageLikeHouseShell(level, corner, 5, 7, 4, entrance);
                buildVillageLikeHouseShell(level, corner.offset(8, 0, 0), 5, 7, 4, entrance);
                fillBox(level, corner.offset(5, 1, 2), corner.offset(7, 1, 4), Blocks.OAK_STAIRS.defaultBlockState());
                placeWrongHouseInterior(level, corner, 5, 7, 4, entrance);
                villagerAnchor = corner.offset(2, 1, 3);
            }
            case 8 -> {
                corner = origin.offset(-8, 0, -7);
                buildVillageLikeHouseShell(level, corner, 17, 15, 8, entrance);
                placeWrongHouseInterior(level, corner, 17, 15, 8, entrance);
                villagerProfile = UncannyStructureVillagerEntity.SoundProfile.HUGE_LONG_WIDE;
                villagerAnchor = corner.offset(8, 1, 7);
            }
            default -> {
                corner = origin.offset(-1, 0, -1);
                buildVillageLikeHouseShell(level, corner, 3, 3, 3, entrance);
                placeWrongHouseInterior(level, corner, 3, 3, 3, entrance);
                villagerAnchor = corner.offset(1, 1, 1);
            }
        }

        spawnStructureVillager(level, villagerAnchor, villagerProfile, 0.50D);
        debugLog("FEATURE wrong_village_house variant={} origin={}", variants[variant], origin);
        return true;
    }

    private static void spawnStructureVillager(
            ServerLevel level,
            BlockPos anchor,
            @Nullable UncannyStructureVillagerEntity.SoundProfile profile,
            double chance) {
        if (profile == null || level.random.nextDouble() > chance) {
            return;
        }
        UncannyStructureVillagerEntity villager = UncannyEntityRegistry.UNCANNY_STRUCTURE_VILLAGER.get().create(level);
        if (villager == null) {
            return;
        }

        villager.setSoundProfile(profile);
        villager.assignRandomBehaviorMode();
        villager.setPersistenceRequired();
        BlockPos spawnPos = resolveStructureVillagerSpawnPos(level, anchor);
        villager.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(villager);
    }

    private static BlockPos resolveStructureVillagerSpawnPos(ServerLevel level, BlockPos anchor) {
        for (int r = 0; r <= 4; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        BlockPos candidate = anchor.offset(dx, dy, dz);
                        BlockState feet = level.getBlockState(candidate);
                        BlockState head = level.getBlockState(candidate.above());
                        BlockState below = level.getBlockState(candidate.below());
                        if (feet.isAir() && head.isAir() && below.isSolidRender(level, candidate.below())) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return anchor;
    }

    private static boolean generateWrongVillageUtility(ServerLevel level, BlockPos origin) {
        if (!hasLoadedArea(level, origin, 14)) {
            return false;
        }
        String[] variants = {"wrong_smithy", "wrong_church", "wrong_library", "wrong_butcher", "wrong_farm_shed", "wrong_meeting"};
        int variant = pickVariantIndex(level, "wrong_village_utility", variants);
        Direction facing = randomHorizontal(level);

        switch (variant) {
            case 0 -> {
                BlockPos c = origin.offset(-4, 0, -3);
                buildVillageLikeHouseShell(level, c, 9, 7, 4, facing);
                fillBox(level, c.offset(1, 1, 1), c.offset(2, 2, 2), Blocks.LAVA.defaultBlockState());
                level.setBlock(c.offset(4, 1, 3), Blocks.BLAST_FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, facing), 3);
            }
            case 1 -> {
                BlockPos c = origin.offset(-2, 0, -2);
                buildVillageLikeHouseShell(level, c, 5, 5, 8, facing);
                fillBox(level, c.offset(2, 1, 2), c.offset(2, 4, 2), Blocks.COBBLESTONE_WALL.defaultBlockState());
                level.setBlock(c.offset(2, 2, 1), Blocks.BELL.defaultBlockState(), 3);
            }
            case 2 -> {
                BlockPos c = origin.offset(-4, 0, -4);
                buildVillageLikeHouseShell(level, c, 9, 9, 4, facing);
                fillBox(level, c.offset(1, 1, 1), c.offset(7, 3, 1), Blocks.BOOKSHELF.defaultBlockState());
                fillBox(level, c.offset(7, 1, 2), c.offset(7, 6, 2), Blocks.BOOKSHELF.defaultBlockState());
            }
            case 3 -> {
                BlockPos c = origin.offset(-3, 0, -3);
                buildVillageLikeHouseShell(level, c, 7, 7, 4, facing);
                level.setBlock(c.offset(3, 1, 3), Blocks.SMOKER.defaultBlockState().setValue(FurnaceBlock.FACING, facing), 3);
                level.setBlock(c.offset(1, 1, 1), Blocks.CAULDRON.defaultBlockState(), 3);
            }
            case 4 -> {
                BlockPos c = origin.offset(-5, 0, -4);
                buildVillageLikeHouseShell(level, c, 11, 7, 4, facing);
                fillBox(level, c.offset(1, 1, 1), c.offset(9, 1, 2), Blocks.HAY_BLOCK.defaultBlockState());
                fillBox(level, c.offset(2, 0, 4), c.offset(8, 0, 6), Blocks.FARMLAND.defaultBlockState());
            }
            default -> {
                fillBox(level, origin.offset(-3, 0, -3), origin.offset(3, 0, 3), Blocks.COBBLESTONE.defaultBlockState());
                level.setBlock(origin.above(), Blocks.BELL.defaultBlockState(), 3);
                fillBox(level, origin.offset(-2, 1, -2), origin.offset(-2, 3, -2), Blocks.OAK_FENCE.defaultBlockState());
                fillBox(level, origin.offset(2, 1, 2), origin.offset(2, 3, 2), Blocks.OAK_FENCE.defaultBlockState());
            }
        }

        debugLog("FEATURE wrong_village_utility variant={} origin={}", variants[variant], origin);
        return true;
    }

    private static void carveCircularHole(ServerLevel level, BlockPos center, int radius, int depth, BlockState wallBlock, boolean keepBottom) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius + 0.2D) {
                    continue;
                }
                for (int d = 0; d <= depth; d++) {
                    BlockPos target = center.offset(dx, -d, dz);
                    if (d == depth && keepBottom) {
                        level.setBlock(target, wallBlock, 3);
                    } else if (dist >= radius - 0.8D) {
                        level.setBlock(target, wallBlock, 3);
                    } else {
                        level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static boolean generateSinkhole(ServerLevel level, BlockPos origin) {
        if (!hasLoadedArea(level, origin, 18)) {
            return false;
        }
        String[] variants = {"open_void_pit", "structured_sinkhole", "replaced_chunk", "false_bottom", "vertical_shaft", "broken_edge", "inverted_chunk"};
        int variant = pickVariantIndex(level, "sinkhole", variants);

        switch (variant) {
            case 0 -> carveCircularHole(level, origin, 6, 16, UncannyBlockRegistry.UNCANNY_BLOCK.get().defaultBlockState(), true);
            case 1 -> {
                carveCircularHole(level, origin, 5, 12, Blocks.STONE_BRICKS.defaultBlockState(), true);
                for (int i = 0; i < 12; i++) {
                    BlockPos stair = origin.offset((i % 4) - 2, -i / 2, (i % 3) - 1);
                    level.setBlock(stair, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, randomHorizontal(level)), 3);
                }
            }
            case 2 -> {
                for (int dx = -8; dx <= 7; dx++) {
                    for (int dz = -8; dz <= 7; dz++) {
                        BlockPos top = origin.offset(dx, 0, dz);
                        level.setBlock(top, (Math.abs(dx + dz) % 3 == 0 ? UncannyBlockRegistry.UNCANNY_BLOCK.get() : Blocks.COARSE_DIRT).defaultBlockState(), 3);
                        clearBox(level, top.above(), top.above(4));
                    }
                }
            }
            case 3 -> {
                carveCircularHole(level, origin, 5, 5, Blocks.DEEPSLATE.defaultBlockState(), true);
                carveCircularHole(level, origin.below(5), 2, 12, UncannyBlockRegistry.UNCANNY_BLOCK.get().defaultBlockState(), false);
                level.setBlock(origin.below(5), Blocks.STONE.defaultBlockState(), 3);
            }
            case 4 -> {
                for (int dy = 0; dy <= 28; dy++) {
                    clearBox(level, origin.offset(-1, -dy, -1), origin.offset(1, -dy, 1));
                    fillBox(level, origin.offset(-2, -dy, -2), origin.offset(2, -dy, -2), Blocks.STONE_BRICKS.defaultBlockState());
                }
            }
            case 5 -> {
                clearBox(level, origin.offset(-8, -18, -4), origin.offset(8, 2, 4));
                fillBox(level, origin.offset(-8, -18, -4), origin.offset(-8, 2, 4), Blocks.STONE.defaultBlockState());
                fillBox(level, origin.offset(8, -18, -4), origin.offset(8, 2, 4), Blocks.STONE.defaultBlockState());
                fillBox(level, origin.offset(-8, -18, -4), origin.offset(8, -18, 4), UncannyBlockRegistry.UNCANNY_BLOCK.get().defaultBlockState());
            }
            default -> {
                carveCircularHole(level, origin, 5, 11, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), false);
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        for (int dy = 0; dy <= 5; dy++) {
                            BlockPos floating = origin.offset(dx, 8 + dy, dz);
                            if (Math.abs(dx) + Math.abs(dz) + dy < 8) {
                                level.setBlock(floating, dy < 2 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(), 3);
                            }
                        }
                    }
                }
            }
        }

        debugLog("FEATURE sinkhole variant={} origin={}", variants[variant], origin);
        return true;
    }

    private static void buildPlatform(
            ServerLevel level,
            BlockPos center,
            int width,
            int length,
            int elevation,
            BlockState platformBlock,
            boolean broken) {
        BlockPos base = center.above(elevation);
        int hx = width / 2;
        int hz = length / 2;
        for (int dx = -hx; dx <= hx; dx++) {
            for (int dz = -hz; dz <= hz; dz++) {
                BlockPos top = base.offset(dx, 0, dz);
                boolean remove = broken && level.random.nextDouble() < 0.18D;
                level.setBlock(top, remove ? Blocks.AIR.defaultBlockState() : platformBlock, 3);
                if (Math.abs(dx) == hx || Math.abs(dz) == hz) {
                    level.setBlock(top.above(), Blocks.OAK_FENCE.defaultBlockState(), 3);
                }
            }
        }

        int[] px = {-hx, hx, -hx, hx};
        int[] pz = {-hz, -hz, hz, hz};
        for (int i = 0; i < 4; i++) {
            BlockPos postTop = base.offset(px[i], -1, pz[i]);
            for (int d = 0; d <= elevation; d++) {
                level.setBlock(postTop.below(d), Blocks.OAK_LOG.defaultBlockState(), 3);
            }
        }
    }

    private static boolean generateObservationPlatform(ServerLevel level, BlockPos origin) {
        if (!hasLoadedArea(level, origin, 10)) {
            return false;
        }
        String[] variants = {"wood", "stone", "broken", "overextended", "double", "decorated"};
        int variant = pickVariantIndex(level, "observation_platform", variants);

        switch (variant) {
            case 0 -> buildPlatform(level, origin, 7, 7, 4, Blocks.OAK_PLANKS.defaultBlockState(), false);
            case 1 -> buildPlatform(level, origin, 7, 7, 4, Blocks.STONE_BRICKS.defaultBlockState(), false);
            case 2 -> buildPlatform(level, origin, 7, 7, 5, Blocks.OAK_PLANKS.defaultBlockState(), true);
            case 3 -> {
                buildPlatform(level, origin, 11, 5, 5, Blocks.OAK_PLANKS.defaultBlockState(), false);
                fillBox(level, origin.offset(6, 5, -1), origin.offset(9, 5, 1), Blocks.AIR.defaultBlockState());
            }
            case 4 -> {
                buildPlatform(level, origin.offset(-5, 0, 0), 5, 5, 4, Blocks.OAK_PLANKS.defaultBlockState(), false);
                buildPlatform(level, origin.offset(5, 2, 0), 5, 5, 6, Blocks.STONE_BRICKS.defaultBlockState(), false);
                fillBox(level, origin.offset(-2, 5, -1), origin.offset(2, 5, 1), Blocks.OAK_PLANKS.defaultBlockState());
            }
            default -> {
                buildPlatform(level, origin, 7, 7, 4, Blocks.OAK_PLANKS.defaultBlockState(), false);
                level.setBlock(origin.above(5), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), 3);
                level.setBlock(origin.above(5).offset(1, 0, 0), Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.WEST), 3);
                if (level.getBlockEntity(origin.above(5)) instanceof ChestBlockEntity chest) {
                    populateExplorationChest(level, chest, 0.15D);
                }
            }
        }

        debugLog("FEATURE observation_platform variant={} origin={}", variants[variant], origin);
        return true;
    }

    private static void placeRoadLine(ServerLevel level, BlockPos start, Direction direction, int length, boolean lightWrong) {
        for (int i = 0; i < length; i++) {
            BlockPos path = start.relative(direction, i);
            level.setBlock(path, Blocks.DIRT_PATH.defaultBlockState(), 3);
            level.setBlock(path.below(), Blocks.DIRT.defaultBlockState(), 3);
            if (lightWrong && i % 5 == 0) {
                level.setBlock(path.above(2), Blocks.TORCH.defaultBlockState(), 3);
            }
        }
    }

    private static boolean generateWrongRoadSegment(ServerLevel level, BlockPos origin) {
        if (!hasLoadedArea(level, origin, 20)) {
            return false;
        }
        String[] variants = {"nowhere", "wall", "to_bell", "wrong_light", "too_straight", "tiny_wild"};
        int variant = pickVariantIndex(level, "wrong_road_segment", variants);
        Direction dir = randomHorizontal(level);

        switch (variant) {
            case 0 -> placeRoadLine(level, origin, dir, 14, false);
            case 1 -> {
                placeRoadLine(level, origin, dir, 10, false);
                fillBox(level, origin.relative(dir, 11).offset(-1, 1, -1), origin.relative(dir, 11).offset(1, 3, 1), Blocks.COBBLESTONE.defaultBlockState());
            }
            case 2 -> {
                placeRoadLine(level, origin, dir, 16, false);
                BlockPos shrine = origin.relative(dir, 17);
                fillBox(level, shrine.offset(-2, 0, -2), shrine.offset(2, 0, 2), Blocks.STONE_BRICKS.defaultBlockState());
                placeBellCore(level, shrine.above());
            }
            case 3 -> placeRoadLine(level, origin, dir, 18, true);
            case 4 -> placeRoadLine(level, origin, dir, 40, false);
            default -> placeRoadLine(level, origin, dir, 6, false);
        }

        debugLog("FEATURE wrong_road_segment variant={} origin={}", variants[variant], origin);
        return true;
    }

    private static boolean generateFalseEntrance(ServerLevel level, BlockPos origin) {
        if (!hasLoadedArea(level, origin, 12)) {
            return false;
        }
        String[] variants = {"mine", "cellar", "stone_stairs", "bricked", "cliff", "trapdoor"};
        int variant = pickVariantIndex(level, "false_entrance", variants);
        Direction facing = randomHorizontal(level);
        BlockPos front = origin;

        if (variant == 4) {
            fillBox(level, origin.offset(-4, 0, -1), origin.offset(4, 6, 4), Blocks.STONE.defaultBlockState());
        }

        fillBox(level, front.offset(-2, 0, -2), front.offset(2, 4, 2), Blocks.COBBLESTONE.defaultBlockState());
        clearBox(level, front.offset(-1, 1, -1), front.offset(1, 3, 1));

        switch (variant) {
            case 0 -> {
                fillBox(level, front.offset(-2, 1, -2), front.offset(-2, 3, 2), Blocks.OAK_LOG.defaultBlockState());
                fillBox(level, front.offset(2, 1, -2), front.offset(2, 3, 2), Blocks.OAK_LOG.defaultBlockState());
                clearBox(level, front.offset(0, 1, 0), front.offset(0, -6, 0));
            }
            case 1 -> {
                placeSimpleDoor(level, front.offset(0, 1, -1), facing);
                fillBox(level, front.offset(0, 0, 0), front.offset(0, -4, 4), Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing));
            }
            case 2 -> {
                clearBox(level, front.offset(-2, 1, -2), front.offset(2, 4, 6));
                placeFalseDescentStyleStairs(level, front.offset(0, 0, 1), 12);
            }
            case 3 -> fillBox(level, front.offset(-1, 1, -1), front.offset(1, 3, 1), Blocks.BRICKS.defaultBlockState());
            case 4 -> {
                clearBox(level, front.offset(0, 1, 0), front.offset(0, -8, 0));
                fillBox(level, front.offset(-1, -8, -1), front.offset(1, -8, 1), UncannyBlockRegistry.UNCANNY_BLOCK.get().defaultBlockState());
            }
            default -> level.setBlock(front.offset(0, 1, 0), Blocks.OAK_TRAPDOOR.defaultBlockState(), 3);
        }

        debugLog("FEATURE false_entrance variant={} origin={}", variants[variant], origin);
        return true;
    }

    private static boolean generateStorageShed(ServerLevel level, BlockPos origin) {
        if (!hasLoadedArea(level, origin, 10)) {
            return false;
        }
        String[] variants = {"organized", "messy", "oversized", "tiny", "wrong_interior", "too_many_chests"};
        int variant = pickVariantIndex(level, "storage_shed", variants);
        Direction facing = randomHorizontal(level);

        int width = switch (variant) {
            case 2 -> 9;
            case 3 -> 3;
            default -> 5;
        };
        int length = switch (variant) {
            case 2 -> 7;
            case 3 -> 3;
            default -> 5;
        };
        int height = variant == 2 ? 5 : 4;

        BlockPos corner = origin.offset(-(width / 2), 0, -(length / 2));
        buildVillageLikeHouseShell(level, corner, width, length, height, facing);

        int chestCount = switch (variant) {
            case 5 -> 8;
            case 0 -> 2;
            case 1 -> 3;
            default -> 4;
        };
        for (int i = 0; i < chestCount; i++) {
            int x = corner.getX() + 1 + level.random.nextInt(Math.max(1, width - 2));
            int z = corner.getZ() + 1 + level.random.nextInt(Math.max(1, length - 2));
            int y = corner.getY() + 1 + (variant == 4 && level.random.nextBoolean() ? 1 : 0);
            BlockPos chestPos = new BlockPos(x, y, z);
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 3);
            if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                populateExplorationChest(level, chest, 0.18D);
            }
        }

        if (variant == 1) {
            for (int i = 0; i < 8; i++) {
                BlockPos clutter = corner.offset(level.random.nextInt(width), 1, level.random.nextInt(length));
                level.setBlock(clutter, level.random.nextBoolean() ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.BARREL.defaultBlockState(), 3);
            }
        } else if (variant == 4) {
            level.setBlock(corner.offset(width / 2, 1, length / 2), Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, facing), 3);
            level.setBlock(corner.offset(width / 2, 2, length / 2), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        }

        debugLog("FEATURE storage_shed variant={} origin={}", variants[variant], origin);
        return true;
    }

    private static BlockPos findNearbyWater(ServerLevel level, BlockPos center, int radius) {
        for (int r = 1; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }
                    BlockPos pos = center.offset(dx, 0, dz);
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
                    BlockPos probe = new BlockPos(pos.getX(), y - 1, pos.getZ());
                    if (level.getFluidState(probe).is(FluidTags.WATER) || level.getFluidState(probe.above()).is(FluidTags.WATER)) {
                        return probe;
                    }
                }
            }
        }
        return null;
    }

    private static long rollNextCheckTicks(ServerLevel level, int profile, UncannyPhase phase) {
        double phaseScale = switch (phase) {
            case PHASE_1 -> 1.25D;
            case PHASE_2 -> 1.05D;
            case PHASE_3 -> 0.90D;
            case PHASE_4 -> 0.80D;
        };

        int minSeconds = Math.max(20, (int) Math.round(PROFILE_CHECK_MIN_SECONDS[profile - 1] * phaseScale));
        int maxSeconds = Math.max(minSeconds + 1, (int) Math.round(PROFILE_CHECK_MAX_SECONDS[profile - 1] * phaseScale));
        return (minSeconds + level.random.nextInt(maxSeconds - minSeconds + 1)) * 20L;
    }

    private static long rollCooldownTicks(ServerLevel level, int profile, UncannyPhase phase) {
        double phaseScale = switch (phase) {
            case PHASE_1 -> 1.35D;
            case PHASE_2 -> 1.00D;
            case PHASE_3 -> 0.85D;
            case PHASE_4 -> 0.75D;
        };
        int seconds = Math.max(600, (int) Math.round(PROFILE_COOLDOWN_SECONDS[profile - 1] * phaseScale));
        int jitter = Math.max(1, seconds / 5);
        int rolled = Math.max(600, seconds - jitter + level.random.nextInt(jitter * 2 + 1));
        return rolled * 20L;
    }

    private static double getTriggerChance(int profile, UncannyPhase phase) {
        double chance = BASE_TRIGGER_CHANCE
                * PROFILE_TRIGGER_MULTIPLIER[profile - 1]
                * PHASE_TRIGGER_MULTIPLIER[Math.max(0, phase.index() - 1)];
        return Mth.clamp(chance, 0.01D, 0.30D);
    }

    private static int getIntensityProfile() {
        return Mth.clamp(UncannyConfig.EVENT_INTENSITY_PROFILE.get(), 1, 5);
    }

    private static void debugLog(String message, Object... args) {
        if (UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info("[UncannyDebug/Feature] " + message, args);
        }
    }

    private enum FeatureType {
        ANECHOIC_CUBE("anechoic_cube", 100, 120, 60, 22, 17_101L),
        MIMIC_SHELTER("mimic_shelter", 140, 170, 40, 14, 17_102L),
        GLITCHED_SHELTER("glitched_shelter", 140, 170, 44, 15, 17_103L),
        PATTERNED_GROVE("patterned_grove", 190, 230, 73, 27, 17_104L),
        BARREN_GRID("barren_grid", 190, 230, 77, 29, 17_105L),
        FALSE_DESCENT("false_descent", 120, 170, 48, 15, 17_106L),
        FALSE_ASCENT("false_ascent", 120, 170, 48, 15, 17_107L),
        ISOLATION_CUBE("isolation_cube", 120, 150, 46, 14, 17_108L),
        BELL_SHRINE("bell_shrine", 120, 170, 80, 28, 17_201L),
        WATCHING_TOWER("watching_tower", 140, 180, 80, 28, 17_202L),
        FALSE_CAMP("false_camp", 90, 140, 66, 22, 17_203L),
        WRONG_VILLAGE_HOUSE("wrong_village_house", 150, 220, 94, 33, 17_204L),
        WRONG_VILLAGE_UTILITY("wrong_village_utility", 150, 220, 92, 32, 17_205L),
        SINKHOLE("sinkhole", 160, 220, 104, 36, 17_206L),
        OBSERVATION_PLATFORM("observation_platform", 110, 160, 76, 27, 17_207L),
        WRONG_ROAD_SEGMENT("wrong_road_segment", 120, 160, 70, 24, 17_208L),
        FALSE_ENTRANCE("false_entrance", 110, 160, 66, 23, 17_209L),
        STORAGE_SHED("storage_shed", 95, 140, 58, 20, 17_210L);

        private final String id;
        private final int exclusionRadius;
        private final int sameTypeExclusionRadius;
        private final int spacing;
        private final int separation;
        private final long salt;

        FeatureType(
                String id,
                int exclusionRadius,
                int sameTypeExclusionRadius,
                int spacing,
                int separation,
                long salt) {
            this.id = id;
            this.exclusionRadius = exclusionRadius;
            this.sameTypeExclusionRadius = sameTypeExclusionRadius;
            this.spacing = spacing;
            this.separation = separation;
            this.salt = salt;
        }

        private static FeatureType byId(String rawId) {
            if (rawId == null) {
                return null;
            }
            String normalized = rawId.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("echoofthevoid:")) {
                normalized = normalized.substring("echoofthevoid:".length());
            }
            for (FeatureType value : values()) {
                if (value.id.equals(normalized)) {
                    return value;
                }
            }
            return null;
        }
    }

    private record WeightedFeature(FeatureType type, int weight) {
    }
}


