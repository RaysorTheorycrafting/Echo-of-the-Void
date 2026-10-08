package com.eotv.echoofthevoid.sound;

import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import com.eotv.echoofthevoid.network.UncannyMentalSoundPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

/** Explicit delivery policy for sounds which are meant to exist inside one player's perception. */
public final class UncannySoundDelivery {
    private UncannySoundDelivery() {
    }

    public static void playMental(
            ServerPlayer player,
            SoundEvent sound,
            SoundSource source,
            float volume,
            float pitch,
            int maximumDurationTicks) {
        if (player == null || sound == null || source == null || !player.isAlive()) {
            return;
        }
        int boundedDuration = Math.max(0, Math.min(maximumDurationTicks, 20 * 30));
        PacketDistributor.sendToPlayer(player, new UncannyMentalSoundPayload(
                sound.getLocation().toString(),
                source.getName(),
                volume,
                pitch,
                boundedDuration));
        UncannyDiagnostics.mentalSoundSent(
                player,
                sound.getLocation().toString(),
                source.getName(),
                volume,
                pitch,
                boundedDuration);
    }
}
