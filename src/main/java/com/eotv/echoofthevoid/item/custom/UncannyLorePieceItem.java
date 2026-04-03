package com.eotv.echoofthevoid.item.custom;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.lore.UncannyLoreBookLibrary;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

public final class UncannyLorePieceItem extends WrittenBookItem {
    public UncannyLorePieceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.has(DataComponents.WRITTEN_BOOK_CONTENT)) {
            // Safety net: always restore readable content even if stack data was stripped.
            stack.set(DataComponents.WRITTEN_BOOK_CONTENT, defaultBookContent());
        }

        // Vanilla only auto-opens if stack is exactly Items.WRITTEN_BOOK.
        // This custom item must send the open-book packet manually.
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (WrittenBookItem.resolveBookComponents(stack, serverPlayer.createCommandSourceStack(), serverPlayer)) {
                serverPlayer.containerMenu.broadcastChanges();
            }
            serverPlayer.connection.send(new ClientboundOpenBookPacket(hand));
            if (UncannyConfig.DEBUG_LOGS.get()) {
                EchoOfTheVoid.LOGGER.info("[UncannyDebug/Item] LORE_BOOK open packet sent player={} hand={} hasContent={}",
                        serverPlayer.getGameProfile().getName(),
                        hand,
                        stack.has(DataComponents.WRITTEN_BOOK_CONTENT));
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static WrittenBookContent defaultBookContent() {
        return UncannyLoreBookLibrary.defaultContent();
    }
}
