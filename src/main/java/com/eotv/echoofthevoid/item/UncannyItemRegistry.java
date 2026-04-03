package com.eotv.echoofthevoid.item;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import com.eotv.echoofthevoid.item.custom.UncannyCompassItem;
import com.eotv.echoofthevoid.item.custom.UncannyLorePieceItem;
import com.eotv.echoofthevoid.lore.UncannyLoreBookLibrary;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class UncannyItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EchoOfTheVoid.MODID);

    public static final DeferredItem<Item> UNCANNY_REALITY_SHARD = ITEMS.register(
            "uncanny_reality_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> UNCANNY_REALITY_SHARD_PIECE = ITEMS.register(
            "uncanny_reality_shard_piece",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> REALITY_CUBE = ITEMS.register(
            "reality_cube",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));
    public static final DeferredItem<Item> UNCANNY_COMPASS = ITEMS.register(
            "uncanny_compass",
            () -> new UncannyCompassItem(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1)));
    public static final DeferredItem<Item> UNCANNY_LORE_PIECE = ITEMS.register(
            "uncanny_lore_piece",
            () -> new UncannyLorePieceItem(new Item.Properties()
                    .rarity(Rarity.UNCOMMON)
                    .stacksTo(1)
                    .component(DataComponents.WRITTEN_BOOK_CONTENT, defaultLoreBookContent())));
    public static final DeferredItem<BlockItem> UNCANNY_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("uncanny_block", UncannyBlockRegistry.UNCANNY_BLOCK);
    public static final DeferredItem<BlockItem> UNCANNY_ALTAR_ITEM =
            ITEMS.registerSimpleBlockItem("uncanny_altar", UncannyBlockRegistry.UNCANNY_ALTAR);

    private UncannyItemRegistry() {
    }

    private static WrittenBookContent defaultLoreBookContent() {
        return UncannyLoreBookLibrary.defaultContent();
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}

