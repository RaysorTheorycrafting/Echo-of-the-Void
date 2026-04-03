package com.eotv.echoofthevoid.block.entity.custom;

import com.eotv.echoofthevoid.block.entity.UncannyBlockEntityRegistry;
import com.eotv.echoofthevoid.menu.UncannyAltarMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class UncannyAltarBlockEntity extends BlockEntity implements MenuProvider {
    public UncannyAltarBlockEntity(BlockPos pos, BlockState blockState) {
        super(UncannyBlockEntityRegistry.UNCANNY_ALTAR.get(), pos, blockState);
    }

    public Component getDisplayName() {
        return Component.translatable("container.echoofthevoid.uncanny_altar");
    }

    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new UncannyAltarMenu(containerId, inventory, getBlockPos());
    }
}
