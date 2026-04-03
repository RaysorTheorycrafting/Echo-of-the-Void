package com.eotv.echoofthevoid.menu;

import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import com.eotv.echoofthevoid.block.custom.UncannyAltarBlock;
import com.eotv.echoofthevoid.phase.UncannyPhaseManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class UncannyAltarMenu extends AbstractContainerMenu {
    public static final int BUTTON_PURGE = 0;
    public static final int BUTTON_PURGE_CONFIRM = 5;
    public static final int BUTTON_PHASE_1 = 1;
    public static final int BUTTON_PHASE_2 = 2;
    public static final int BUTTON_PHASE_3 = 3;
    public static final int BUTTON_PHASE_4 = 4;

    private final Level level;
    private final BlockPos altarPos;

    public UncannyAltarMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos());
    }

    public UncannyAltarMenu(int containerId, Inventory inventory, BlockPos altarPos) {
        super(UncannyMenuRegistry.UNCANNY_ALTAR.get(), containerId);
        this.level = inventory.player.level();
        this.altarPos = altarPos.immutable();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, altarPos), player, UncannyBlockRegistry.UNCANNY_ALTAR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return false;
        }

        if (!level.isLoaded(altarPos)) {
            return false;
        }

        if (!level.getBlockState(altarPos).is(UncannyBlockRegistry.UNCANNY_ALTAR.get())) {
            return false;
        }

        switch (id) {
            case BUTTON_PURGE -> {
                return true;
            }
            case BUTTON_PURGE_CONFIRM -> {
                UncannyPhaseManager.purgeWorld(serverPlayer.getServer());
                level.setBlock(altarPos, level.getBlockState(altarPos).setValue(UncannyAltarBlock.HAS_CUBE, false), 3);
                serverPlayer.closeContainer();
                return true;
            }
            case BUTTON_PHASE_1, BUTTON_PHASE_2, BUTTON_PHASE_3, BUTTON_PHASE_4 -> {
                UncannyPhaseManager.lockPhase(serverPlayer.getServer(), id);
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
