package com.eotv.echoofthevoid.item.custom;

import com.eotv.echoofthevoid.event.UncannyParanoiaEventSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class UncannyCompassItem extends CompassItem {
    public UncannyCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            UncannyParanoiaEventSystem.activateUncannyCompassGuide(serverPlayer, true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
