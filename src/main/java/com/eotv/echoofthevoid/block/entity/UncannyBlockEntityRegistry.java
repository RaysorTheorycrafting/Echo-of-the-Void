package com.eotv.echoofthevoid.block.entity;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import com.eotv.echoofthevoid.block.entity.custom.UncannyAltarBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class UncannyBlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EchoOfTheVoid.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UncannyAltarBlockEntity>> UNCANNY_ALTAR =
            BLOCK_ENTITY_TYPES.register(
                    "uncanny_altar",
                    () -> BlockEntityType.Builder.of(
                                    UncannyAltarBlockEntity::new,
                                    UncannyBlockRegistry.UNCANNY_ALTAR.get())
                            .build(null));

    private UncannyBlockEntityRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}

