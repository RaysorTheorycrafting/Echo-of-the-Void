package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.event.UncannyParanoiaEventSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class UncannyClientEventEffects {
    private UncannyClientEventEffects() {
    }

    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isAlive()) {
            return;
        }

        boolean taggedOverlay = player.getTags().contains(UncannyParanoiaEventSystem.getFlashRedOverlayTag());
        MobEffectInstance marker = player.getEffect(MobEffects.LUCK);
        boolean effectOverlay = marker != null
                && marker.getAmplifier() == UncannyParanoiaEventSystem.getFlashRedMarkerAmplifier()
                && marker.getDuration() <= 8;

        if (!taggedOverlay && !effectOverlay) {
            return;
        }

        int width = event.getGuiGraphics().guiWidth();
        int height = event.getGuiGraphics().guiHeight();
        event.getGuiGraphics().fill(0, 0, width, height, 0x66FF0000);
    }
}
