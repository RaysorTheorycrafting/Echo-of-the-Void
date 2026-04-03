package com.eotv.echoofthevoid.worldgen.structure;

import com.eotv.echoofthevoid.worldgen.UncannyStructureWorldgenRegistry;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public final class UncannyFeatureStructure extends Structure {
    public static final MapCodec<UncannyFeatureStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            com.mojang.serialization.Codec.STRING.fieldOf("feature").forGetter(structure -> structure.featureId))
            .apply(instance, UncannyFeatureStructure::new));

    private final String featureId;
    private static final double DISTANCE_SPAWN_START_BLOCKS = 600.0D;
    private static final double DISTANCE_FULL_RATE_BLOCKS = 12_000.0D;
    private static final double DISTANCE_NEAR_CHANCE = 0.40D;
    private static final double DISTANCE_FAR_CHANCE = 0.95D;

    public UncannyFeatureStructure(StructureSettings settings, String featureId) {
        super(settings);
        this.featureId = featureId;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        if (isSurfaceFeature(featureId) && !hasLikelyDrySurface(context, chunkPos)) {
            return Optional.empty();
        }

        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();
        int centerSurfaceY = context.chunkGenerator()
                .getBaseHeight(centerX, centerZ, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        if ("false_descent".equals(featureId) && centerSurfaceY - 34 <= context.heightAccessor().getMinBuildHeight()) {
            return Optional.empty();
        }
        if ("false_ascent".equals(featureId) && centerSurfaceY + 47 >= context.heightAccessor().getMaxBuildHeight()) {
            return Optional.empty();
        }
        if (!passesDistanceSpawnChance(context, chunkPos, centerSurfaceY)) {
            return Optional.empty();
        }

        BlockPos pivot = new BlockPos(chunkPos.getMiddleBlockX(), 0, chunkPos.getMiddleBlockZ());
        return Optional.of(new GenerationStub(
                pivot,
                builder -> builder.addPiece(new UncannyFeatureStructurePiece(featureId, chunkPos))));
    }

    private static boolean isSurfaceFeature(String featureId) {
        return !"anechoic_cube".equals(featureId) && !"isolation_cube".equals(featureId);
    }

    private static boolean hasLikelyDrySurface(GenerationContext context, ChunkPos chunkPos) {
        int centerX = chunkPos.getMinBlockX() + 8;
        int centerZ = chunkPos.getMinBlockZ() + 8;
        int minBuildHeight = context.heightAccessor().getMinBuildHeight();
        int seaLevel = context.chunkGenerator().getSeaLevel();
        int[] offsets = {0, -3, 3, -6, 6};
        int drySamples = 0;

        for (int dx : offsets) {
            for (int dz : offsets) {
                int x = centerX + dx;
                int z = centerZ + dz;
                int worldSurfaceY = context.chunkGenerator()
                        .getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                int oceanFloorY = context.chunkGenerator()
                        .getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
                if (worldSurfaceY <= minBuildHeight + 4) {
                    continue;
                }
                if (worldSurfaceY - oceanFloorY > 1) {
                    continue;
                }
                if (worldSurfaceY < seaLevel - 2) {
                    continue;
                }
                drySamples++;
                if (drySamples >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean passesDistanceSpawnChance(GenerationContext context, ChunkPos chunkPos, int centerSurfaceY) {
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();
        double distance = Math.sqrt((double) centerX * centerX + (double) centerZ * centerZ);
        double t = (distance - DISTANCE_SPAWN_START_BLOCKS) / (DISTANCE_FULL_RATE_BLOCKS - DISTANCE_SPAWN_START_BLOCKS);
        if (t < 0.0D) {
            t = 0.0D;
        } else if (t > 1.0D) {
            t = 1.0D;
        }
        double chance = DISTANCE_NEAR_CHANCE + (DISTANCE_FAR_CHANCE - DISTANCE_NEAR_CHANCE) * t;

        long hash = mix64((long) chunkPos.x * 341873128712L
                ^ (long) chunkPos.z * 132897987541L
                ^ (long) featureId.hashCode() * 42317861L
                ^ (long) centerSurfaceY * 1000003L);
        double roll = ((hash >>> 11) & ((1L << 53) - 1)) * 0x1.0p-53;
        return roll < chance;
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    @Override
    public StructureType<?> type() {
        return UncannyStructureWorldgenRegistry.UNCANNY_FEATURE_STRUCTURE.get();
    }
}
