package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.entity.custom.UncannySleeperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * While a Sleeper? comes for the local sleeper, the sleep darkness is not drawn: the point of the
 * attack is to see it climb onto the bed and open its mouth, not a black screen.
 */
public final class SleeperClientEffects {
    private SleeperClientEffects() {
    }

    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!VanillaGuiLayers.SLEEP_OVERLAY.equals(event.getName())) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isSleeping()) {
            return;
        }
        for (UncannySleeperEntity sleeper : player.level().getEntitiesOfClass(UncannySleeperEntity.class,
                player.getBoundingBox().inflate(32.0D))) {
            UncannySleeperEntity.State state = sleeper.state();
            if (state.ordinal() >= UncannySleeperEntity.State.RUN.ordinal()
                    && state.ordinal() <= UncannySleeperEntity.State.BITE.ordinal()) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
