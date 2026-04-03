package com.eotv.echoofthevoid.worldgen;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.worldgen.structure.UncannyFeatureStructure;
import com.eotv.echoofthevoid.worldgen.structure.UncannyFeatureStructurePiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class UncannyStructureWorldgenRegistry {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, EchoOfTheVoid.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, EchoOfTheVoid.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<UncannyFeatureStructure>> UNCANNY_FEATURE_STRUCTURE =
            STRUCTURE_TYPES.register("uncanny_feature", () -> () -> UncannyFeatureStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> UNCANNY_FEATURE_PIECE =
            STRUCTURE_PIECE_TYPES.register("uncanny_feature_piece", () -> UncannyFeatureStructurePiece::new);

    private UncannyStructureWorldgenRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECE_TYPES.register(modEventBus);
    }
}
