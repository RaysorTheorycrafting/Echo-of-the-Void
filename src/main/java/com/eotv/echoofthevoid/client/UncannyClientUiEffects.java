package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class UncannyClientUiEffects {
    private static int fakeSlot = -1;
    private static int fakeCount = 0;
    private static long fakeCountEndTick = Long.MIN_VALUE;
    private static boolean fakeLogged = false;

    private UncannyClientUiEffects() {
    }

    public static void showHotbarWrongCount(int slot, int count, int durationTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        fakeSlot = Math.max(0, Math.min(8, slot));
        fakeCount = Math.max(1, Math.min(99, count));
        fakeCountEndTick = player.level().getGameTime() + Math.max(1, durationTicks);
        fakeLogged = false;
        if (UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info(
                    "[UncannyDebug/UI] hotbar payload slot={} fakeCount={} duration={} endTick={}",
                    fakeSlot,
                    fakeCount,
                    durationTicks,
                    fakeCountEndTick);
        }
    }

    public static void showFalseRecipeToast(String title, String subtitle) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getToasts() == null) {
            return;
        }
        Component titleComponent = Component.literal(title == null || title.isBlank() ? "New recipe unlocked" : title);
        Component subtitleComponent = Component.literal(subtitle == null ? "" : subtitle);
        SystemToast.addOrUpdate(
                minecraft.getToasts(),
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                titleComponent,
                subtitleComponent);
    }

    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        // Layer-based renderer handles normal HUD drawing order.
    }

    public static void onRenderGuiLayerPost(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.SAVING_INDICATOR.equals(event.getName())) {
            return;
        }
        renderFakeHotbarCount(event.getGuiGraphics().guiWidth(), event.getGuiGraphics().guiHeight(), event.getGuiGraphics());
    }

    private static void renderFakeHotbarCount(int screenWidth, int screenHeight, net.minecraft.client.gui.GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isAlive() || fakeSlot < 0) {
            return;
        }
        if (player.level().getGameTime() >= fakeCountEndTick) {
            clearFakeHotbarCount();
            return;
        }
        ItemStack stack = player.getInventory().getItem(fakeSlot);
        if (stack.isEmpty()) {
            return;
        }
        if (stack.getCount() <= 1 && fakeCount <= 1) {
            return;
        }
        if (!fakeLogged && UncannyConfig.DEBUG_LOGS.get()) {
            fakeLogged = true;
            EchoOfTheVoid.LOGGER.info(
                    "[UncannyDebug/UI] hotbar render slot={} actual={} fake={} timeLeft={}",
                    fakeSlot,
                    stack.getCount(),
                    fakeCount,
                    fakeCountEndTick - player.level().getGameTime());
        }

        String text = Integer.toString(fakeCount);
        int itemX = screenWidth / 2 - 90 + fakeSlot * 20 + 2;
        int itemY = screenHeight - 19;
        int textX = itemX + 19 - 2 - minecraft.font.width(text);
        int textY = itemY + 9;

        guiGraphics.fill(
                textX - 1,
                textY - 1,
                textX + minecraft.font.width(text) + 1,
                textY + 9,
                0xFF000000);
        guiGraphics.drawString(minecraft.font, text, textX, textY, 0xFFFFFFFF, false);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (fakeSlot < 0) {
            return;
        }
        if (player == null || !player.isAlive() || player.level().getGameTime() >= fakeCountEndTick) {
            clearFakeHotbarCount();
        }
    }

    private static void clearFakeHotbarCount() {
        fakeSlot = -1;
        fakeCount = 0;
        fakeCountEndTick = Long.MIN_VALUE;
        fakeLogged = false;
    }
}
