package com.eotv.echoofthevoid.client.weather_mixin;

import com.eotv.echoofthevoid.client.UncannyLocalizedWeatherClientEffects;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Alters only precipitation chosen for rendered columns; world weather remains authoritative. */
@Mixin(LevelRenderer.class)
public abstract class UncannyLocalizedPrecipitationMixin {
    @Redirect(
            method = {"renderSnowAndRain", "tickRain"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
    private Biome.Precipitation eotv$localizedPrecipitation(Biome biome, BlockPos pos) {
        return UncannyLocalizedWeatherClientEffects.filterPrecipitation(pos, biome.getPrecipitationAt(pos));
    }
}
