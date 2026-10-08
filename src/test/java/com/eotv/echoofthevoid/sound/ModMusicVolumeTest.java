package com.eotv.echoofthevoid.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ModMusicVolumeTest {
    // Minecraft 1.21.1 has nine categories besides Master; music is the first of them here.
    private static double[] sliders(double music, double others) {
        double[] values = new double[9];
        java.util.Arrays.fill(values, others);
        values[0] = music;
        return values;
    }

    @Test
    void aLowMusicSliderNoLongerBuriesTheScores() {
        // The user's settings: Music at 20 %, every other category at 100 %.
        float volume = ModMusicVolume.averageOf(sliders(0.2D, 1.0D));
        assertTrue(volume > 0.9F, "scores must sit with the rest of the game: " + volume);
    }

    @Test
    void musicDisabledStillLeavesTheScoresAudible() {
        float volume = ModMusicVolume.averageOf(sliders(0.0D, 0.6D));
        assertTrue(volume > 0.5F && volume < 0.6F, "" + volume);
    }

    @Test
    void valuesAreClamped() {
        assertEquals(1.0F, ModMusicVolume.averageOf(3.0D, 2.0D), 1.0E-6F);
        assertEquals(0.0F, ModMusicVolume.averageOf(-1.0D), 1.0E-6F);
        assertEquals(1.0F, ModMusicVolume.averageOf(), 1.0E-6F);
    }

    @Test
    void onlyTheModsScoresAreRouted() {
        assertTrue(ModMusicVolume.isScore("echoofthevoid", "event.blackout"));
        assertTrue(ModMusicVolume.isScore("echoofthevoid", "music.elsewhere_trial"));
        assertFalse(ModMusicVolume.isScore("minecraft", "event.blackout"));
        assertFalse(ModMusicVolume.isScore("echoofthevoid", "uncanny_whisper"));
    }
}
