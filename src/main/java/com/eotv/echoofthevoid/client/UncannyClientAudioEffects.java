package com.eotv.echoofthevoid.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class UncannyClientAudioEffects {
    private static final List<TimedMentalSound> TIMED_MENTAL_SOUNDS = new ArrayList<>();
    private static ClientLevel trackedLevel;

    private UncannyClientAudioEffects() {
    }

    public static void playZombieRaleInHead(float volume, float pitch) {
        playInHead(
                SoundEvents.ZOMBIE_AMBIENT.getLocation().toString(),
                SoundSource.HOSTILE.getName(),
                volume,
                pitch,
                0);
    }

    public static void playInHead(
            String rawSoundId,
            String rawSourceName,
            float volume,
            float pitch,
            int maximumDurationTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        if (minecraft.level != trackedLevel) {
            stopAllTimedSounds(minecraft);
            trackedLevel = minecraft.level;
        }
        ResourceLocation soundId = ResourceLocation.tryParse(rawSoundId);
        if (soundId == null) {
            return;
        }
        SoundSource source = parseSource(rawSourceName);
        float safeVolume = Mth.clamp(volume, 0.0F, 2.0F);
        float safePitch = Mth.clamp(pitch, 0.2F, 2.0F);
        SimpleSoundInstance sound = new SimpleSoundInstance(
                soundId,
                source,
                safeVolume,
                safePitch,
                SoundInstance.createUnseededRandom(),
                false,
                0,
                SoundInstance.Attenuation.NONE,
                0.0D,
                0.0D,
                0.0D,
                true);
        minecraft.getSoundManager().play(sound);
        if (maximumDurationTicks > 0) {
            TIMED_MENTAL_SOUNDS.add(new TimedMentalSound(
                    sound,
                    player.level().getGameTime() + maximumDurationTicks));
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level != trackedLevel) {
            stopAllTimedSounds(minecraft);
            trackedLevel = minecraft.level;
        }
        if (player == null) {
            stopAllTimedSounds(minecraft);
            return;
        }
        long now = player.level().getGameTime();
        Iterator<TimedMentalSound> iterator = TIMED_MENTAL_SOUNDS.iterator();
        while (iterator.hasNext()) {
            TimedMentalSound timed = iterator.next();
            if (now >= timed.endTick()) {
                minecraft.getSoundManager().stop(timed.sound());
                iterator.remove();
            }
        }
    }

    private static SoundSource parseSource(String rawName) {
        for (SoundSource source : SoundSource.values()) {
            if (source.getName().equalsIgnoreCase(rawName)) {
                return source;
            }
        }
        return SoundSource.AMBIENT;
    }

    private static void stopAllTimedSounds(Minecraft minecraft) {
        for (TimedMentalSound timed : TIMED_MENTAL_SOUNDS) {
            minecraft.getSoundManager().stop(timed.sound());
        }
        TIMED_MENTAL_SOUNDS.clear();
    }

    private record TimedMentalSound(SimpleSoundInstance sound, long endTick) {
    }
}
