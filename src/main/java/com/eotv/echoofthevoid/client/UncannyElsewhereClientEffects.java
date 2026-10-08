package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.world.ElsewhereFogRules;
import com.eotv.echoofthevoid.world.UncannyDimensions;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.sounds.Music;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * No-sky, close grey fog for Elsewhere (see {@link ElsewhereFogRules}); gameplay remains entirely
 * server-authoritative. Applied to both terrain and sky fog, after Vanilla adjustments such as
 * night vision or Darkness, so every client sees black silhouettes surface at the same distance.
 */
public final class UncannyElsewhereClientEffects {

    private UncannyElsewhereClientEffects() {
    }

    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(UncannyDimensions.ELSEWHERE_ID, new ElsewhereEffects());
    }

    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!isInElsewhere()) {
            return;
        }
        event.setNearPlaneDistance(ElsewhereFogRules.FOG_START_BLOCKS);
        event.setFarPlaneDistance(ElsewhereFogRules.FOG_END_BLOCKS);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!isInElsewhere()) {
            return;
        }
        event.setRed(ElsewhereFogRules.FOG_RED);
        event.setGreen(ElsewhereFogRules.FOG_GREEN);
        event.setBlue(ElsewhereFogRules.FOG_BLUE);
    }

    /**
     * No Vanilla track may start in Elsewhere: its own score is looped by {@link UncannyModMusic}
     * at the average loudness of every sound category, independently of the Music slider.
     */
    public static void onSelectMusic(SelectMusicEvent event) {
        if (isInElsewhere()) {
            event.setMusic(null);
        }
    }

    private static boolean isInElsewhere() {
        var level = Minecraft.getInstance().level;
        return level != null && UncannyDimensions.isElsewhere(level);
    }

    private static final class ElsewhereEffects extends DimensionSpecialEffects {
        private ElsewhereEffects() {
            super(Float.NaN, true, SkyType.NONE, false, true);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
            return new Vec3(ElsewhereFogRules.FOG_RED, ElsewhereFogRules.FOG_GREEN, ElsewhereFogRules.FOG_BLUE);
        }

        @Override
        public boolean isFoggyAt(int x, int y) {
            return true;
        }

        @Override
        public float[] getSunriseColor(float timeOfDay, float partialTicks) {
            return null;
        }
    }
}
