package com.eotv.echoofthevoid.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class UncannyClientAudioEffects {
    private UncannyClientAudioEffects() {
    }

    public static void playZombieRaleInHead(float volume, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        float safeVolume = Math.max(0.0F, Math.min(3.0F, volume));
        float safePitch = Math.max(0.2F, Math.min(2.0F, pitch));
        player.playNotifySound(SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, safeVolume, safePitch);
    }
}
