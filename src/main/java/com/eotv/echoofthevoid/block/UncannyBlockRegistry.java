package com.eotv.echoofthevoid.block;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.block.custom.UncannyBlackWebBlock;
import com.eotv.echoofthevoid.block.custom.UncannyAltarBlock;
import com.eotv.echoofthevoid.block.custom.UncannyAltarPartBlock;
import com.eotv.echoofthevoid.block.custom.UncannyEggBlock;
import com.eotv.echoofthevoid.block.custom.UncannyVoidDoorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class UncannyBlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EchoOfTheVoid.MODID);

    public static final DeferredBlock<Block> UNCANNY_BLOCK = BLOCKS.register("uncanny_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(1.8F, 6.0F)
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<Block> UNCANNY_EGG = BLOCKS.register("uncanny_egg",
            () -> new UncannyEggBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.1F)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion()));

    public static final DeferredBlock<Block> UNCANNY_BLACK_WEB = BLOCKS.register("uncanny_black_web",
            () -> new UncannyBlackWebBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.2F)
                    .sound(SoundType.WOOL)
                    .noCollission()
                    .noOcclusion()
                    .replaceable()));

    public static final DeferredBlock<Block> UNCANNY_ALTAR = BLOCKS.register("uncanny_altar",
            () -> new UncannyAltarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(4.0F, 1200.0F)
                    .sound(SoundType.DEEPSLATE_TILES)
                    .lightLevel(state -> state.getValue(UncannyAltarBlock.HAS_CUBE) ? 12 : 10)
                    .noOcclusion()));

    public static final DeferredBlock<Block> UNCANNY_ALTAR_PART = BLOCKS.register("uncanny_altar_part",
            () -> new UncannyAltarPartBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .noOcclusion()));

    public static final DeferredBlock<Block> UNCANNY_VOID_DOOR = BLOCKS.register("uncanny_void_door",
            () -> new UncannyVoidDoorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5F, 1200.0F)
                    .sound(SoundType.EMPTY)
                    .noOcclusion()));

    private UncannyBlockRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
