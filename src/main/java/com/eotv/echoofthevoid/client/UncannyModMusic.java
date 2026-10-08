package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.sound.ModMusicVolume;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import com.eotv.echoofthevoid.world.UncannyDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Plays the mod's scores at the average loudness of all sound categories ({@link ModMusicVolume}).
 * They go through the Master channel, so the Music slider alone cannot mute them, and the volume is
 * re-read every tick so moving any slider takes effect at once.
 */
public final class UncannyModMusic {
    private static ScoreInstance elsewhereLoop;

    private UncannyModMusic() {
    }

    public static SoundInstance play(ResourceLocation id, boolean looping) {
        ScoreInstance instance = new ScoreInstance(SoundEvent.createVariableRangeEvent(id), looping);
        Minecraft.getInstance().getSoundManager().play(instance);
        return instance;
    }

    public static float currentVolume() {
        var options = Minecraft.getInstance().options;
        double[] sliders = java.util.Arrays.stream(SoundSource.values())
                .filter(source -> source != SoundSource.MASTER)
                .mapToDouble(options::getSoundSourceVolume)
                .toArray();
        return ModMusicVolume.averageOf(sliders);
    }

    /** Keeps the Elsewhere score looping exactly while the player is in the dimension. */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean inElsewhere = minecraft.level != null && minecraft.player != null
                && UncannyDimensions.isElsewhere(minecraft.level);
        var sounds = minecraft.getSoundManager();
        if (inElsewhere) {
            if (elsewhereLoop == null || !sounds.isActive(elsewhereLoop)) {
                // Silence any Vanilla track that was already playing when the player arrived.
                minecraft.getMusicManager().stopPlaying();
                elsewhereLoop = (ScoreInstance) play(UncannySoundRegistry.ELSEWHERE_TRIAL_MUSIC.getId(), true);
                UncannyClientDiagnostics.enqueue(
                        "INFO",
                        "mod_score_started",
                        "Elsewhere score started at the averaged sound volume",
                        "sound_id=" + UncannySoundRegistry.ELSEWHERE_TRIAL_MUSIC.getId() + ";volume=" + currentVolume());
            }
        } else if (elsewhereLoop != null) {
            sounds.stop(elsewhereLoop);
            elsewhereLoop = null;
        }
    }

    public static boolean isElsewhereLoopPlaying() {
        return elsewhereLoop != null && Minecraft.getInstance().getSoundManager().isActive(elsewhereLoop);
    }

    private static final class ScoreInstance extends AbstractTickableSoundInstance {
        private ScoreInstance(SoundEvent sound, boolean looping) {
            super(sound, SoundSource.MASTER, SoundInstance.createUnseededRandom());
            this.looping = looping;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = currentVolume();
        }

        @Override
        public void tick() {
            this.volume = currentVolume();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
