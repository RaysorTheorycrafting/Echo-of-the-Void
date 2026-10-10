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
            UncannyClientDiagnostics.enqueue(
                    "WARNING",
                    "mental_sound_not_played",
                    "Mental sound was received without a living local player",
                    "sound_id=" + rawSoundId);
            return;
        }
        if (minecraft.level != trackedLevel) {
            stopAllTimedSounds(minecraft);
            trackedLevel = minecraft.level;
        }
        ResourceLocation soundId = ResourceLocation.tryParse(rawSoundId);
        if (soundId == null) {
            UncannyClientDiagnostics.enqueue(
                    "ERROR",
                    "mental_sound_invalid_id",
                    "Mental sound payload contained an invalid resource location",
                    "sound_id=" + rawSoundId);
            return;
        }
        SoundSource source = parseSource(rawSourceName);
        float safeVolume = Mth.clamp(volume, 0.0F, 2.0F);
        float safePitch = Mth.clamp(pitch, 0.2F, 2.0F);
        if (com.eotv.echoofthevoid.sound.ModMusicVolume.isScore(soundId.getNamespace(), soundId.getPath())) {
            // A score (Blackout) follows the average of all sound sliders, not its own category.
            SoundInstance score = UncannyModMusic.play(soundId, false);
            if (maximumDurationTicks > 0) {
                TIMED_MENTAL_SOUNDS.add(new TimedMentalSound(score, player.level().getGameTime() + maximumDurationTicks));
            }
            UncannyClientDiagnostics.enqueue(
                    "INFO",
                    "mental_sound_played",
                    "Client started a mod score at the averaged sound volume",
                    "sound_id=" + soundId + ";volume=" + UncannyModMusic.currentVolume()
                            + ";maximum_duration_ticks=" + maximumDurationTicks);
            return;
        }
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
        UncannyClientDiagnostics.enqueue(
                "INFO",
                "mental_sound_played",
                "Client accepted a non-positional mental sound",
                "sound_id=" + soundId
                        + ";source=" + source.getName()
                        + ";volume=" + safeVolume
                        + ";pitch=" + safePitch
                        + ";maximum_duration_ticks=" + maximumDurationTicks);
        if (maximumDurationTicks > 0) {
            TIMED_MENTAL_SOUNDS.add(new TimedMentalSound(
                    sound,
                    player.level().getGameTime() + maximumDurationTicks));
        }
    }

    /** A physical cry that follows its (silent) entity through the world. */
    public static void playFollowingEntity(com.eotv.echoofthevoid.network.UncannyEntitySoundPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        ResourceLocation soundId = ResourceLocation.tryParse(payload.soundId());
        if (level == null || soundId == null) {
            return;
        }
        net.minecraft.world.entity.Entity entity = level.getEntity(payload.entityId());
        minecraft.getSoundManager().play(new UncannyEntityFollowingSound(
                net.minecraft.sounds.SoundEvent.createVariableRangeEvent(soundId),
                parseSource(payload.sourceName()),
                Mth.clamp(payload.volume(), 0.0F, 8.0F),
                Mth.clamp(payload.pitch(), 0.2F, 2.0F),
                payload.seed(),
                entity,
                payload.x(),
                payload.y(),
                payload.z()));
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

    public static String diagnosticState() {
        return "timed_mental_sounds=" + TIMED_MENTAL_SOUNDS.size();
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

    private record TimedMentalSound(SoundInstance sound, long endTick) {
    }
}
