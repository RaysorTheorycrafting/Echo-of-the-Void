package com.eotv.echoofthevoid.worldgen.structure;

import com.eotv.echoofthevoid.event.UncannyStructureFeatureSystem;
import com.eotv.echoofthevoid.worldgen.UncannyStructureWorldgenRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class UncannyFeatureStructurePiece extends StructurePiece {
    private static final String TAG_FEATURE_ID = "FeatureId";
    private static final String TAG_ANCHOR_CHUNK_X = "AnchorChunkX";
    private static final String TAG_ANCHOR_CHUNK_Z = "AnchorChunkZ";

    private final String featureId;
    private final int anchorChunkX;
    private final int anchorChunkZ;

    public UncannyFeatureStructurePiece(String featureId, ChunkPos anchorChunk) {
        super(UncannyStructureWorldgenRegistry.UNCANNY_FEATURE_PIECE.get(), 0, makeBoundingBox(anchorChunk));
        this.featureId = featureId;
        this.anchorChunkX = anchorChunk.x;
        this.anchorChunkZ = anchorChunk.z;
    }

    public UncannyFeatureStructurePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(UncannyStructureWorldgenRegistry.UNCANNY_FEATURE_PIECE.get(), tag);
        this.featureId = tag.getString(TAG_FEATURE_ID);
        this.anchorChunkX = tag.getInt(TAG_ANCHOR_CHUNK_X);
        this.anchorChunkZ = tag.getInt(TAG_ANCHOR_CHUNK_Z);
    }

    private static BoundingBox makeBoundingBox(ChunkPos anchorChunk) {
        int centerX = anchorChunk.getMiddleBlockX();
        int centerZ = anchorChunk.getMiddleBlockZ();
        int radius = 24;
        int centerY = 80;
        return new BoundingBox(centerX - radius, centerY - 20, centerZ - radius, centerX + radius, centerY + 24, centerZ + radius);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString(TAG_FEATURE_ID, featureId);
        tag.putInt(TAG_ANCHOR_CHUNK_X, anchorChunkX);
        tag.putInt(TAG_ANCHOR_CHUNK_Z, anchorChunkZ);
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            BlockPos pivot) {
        if (chunkPos.x != anchorChunkX || chunkPos.z != anchorChunkZ) {
            return;
        }
        if (!(level instanceof ServerLevelAccessor accessor)) {
            return;
        }
        if (accessor.getLevel().dimension() != Level.OVERWORLD) {
            return;
        }
        ServerLevel serverLevel = accessor.getLevel();
        MinecraftServer server = serverLevel.getServer();
        if (server == null) {
            return;
        }
        ChunkPos anchor = new ChunkPos(anchorChunkX, anchorChunkZ);
        server.execute(() -> UncannyStructureFeatureSystem.generateWorldgenFeatureAtChunk(serverLevel, featureId, anchor));
    }
}
