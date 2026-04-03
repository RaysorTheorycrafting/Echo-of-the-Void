package com.eotv.echoofthevoid.menu;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class UncannyMenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, EchoOfTheVoid.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<UncannyAltarMenu>> UNCANNY_ALTAR =
            MENUS.register(
                    "uncanny_altar",
                    () -> IMenuTypeExtension.create(UncannyAltarMenu::new));

    private UncannyMenuRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
