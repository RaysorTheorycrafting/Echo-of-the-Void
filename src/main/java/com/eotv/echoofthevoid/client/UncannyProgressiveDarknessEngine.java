package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.event.UncannyClientStateSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public final class UncannyProgressiveDarknessEngine {
    public static final UncannyProgressiveDarknessEngine INSTANCE = new UncannyProgressiveDarknessEngine();
    private static final int LIGHTMAP_SIZE = 16;

    private final float[][] targetLuminance = new float[LIGHTMAP_SIZE][LIGHTMAP_SIZE];
    private final float[][] smoothedLuminance = new float[LIGHTMAP_SIZE][LIGHTMAP_SIZE];

    private long currentGameTick = Long.MIN_VALUE;
    private long lastUpdateTick = Long.MIN_VALUE;
    private long lastUploadAppliedTick = Long.MIN_VALUE;
    private long lastDebugTick = Long.MIN_VALUE;
    private int dirtyWithoutUploadTicks;
    private boolean mixinInactiveLogged;
    private boolean hookActiveLogged;
    private boolean active;

    private UncannyProgressiveDarknessEngine() {
        resetLuminanceTables(1.0F);
    }

    public void updateLuminance(float partialTick, Minecraft minecraft, float prevFlicker, boolean lightmapDirty) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            this.active = false;
            this.currentGameTick = Long.MIN_VALUE;
            return;
        }

        int phase = UncannyClientStateSync.getClientPhaseIndex();
        long now = level.getGameTime();
        this.currentGameTick = now;

        boolean enabled = UncannyConfig.DARKNESS_ENABLED.get();
        DimensionProfile profile = resolveDimensionProfile(level);
        boolean profileEnabled = isProfileEnabled(profile);
        if (!enabled || phase <= 0 || !profileEnabled) {
            this.active = false;
            this.dirtyWithoutUploadTicks = 0;
            if (this.lastUpdateTick == Long.MIN_VALUE || now != this.lastUpdateTick) {
                resetLuminanceTables(1.0F);
            }
            this.lastUpdateTick = now;
            return;
        }

        this.active = true;
        long dtTicks = this.lastUpdateTick == Long.MIN_VALUE ? 1L : Math.max(1L, now - this.lastUpdateTick);
        this.lastUpdateTick = now;

        float phaseStrength = resolvePhaseStrength(phase);
        SkyInputs skyInputs = resolveSkyInputs(level);
        float skyFactor = skyInputs.skyFactor();
        float weatherBonus = resolveWeatherBonus();
        float lightmapStrength = (float) UncannyConfig.DARKNESS_LIGHTMAP_STRENGTH.get().doubleValue();
        float maxPixelAttenuation = (float) UncannyConfig.DARKNESS_MAX_PIXEL_ATTENUATION.get().doubleValue();
        float floor = (float) UncannyConfig.DARKNESS_LIGHT_FLOOR.get().doubleValue();
        float ceiling = (float) UncannyConfig.DARKNESS_LIGHT_CEILING.get().doubleValue();
        float exponent = (float) UncannyConfig.DARKNESS_CURVE_EXPONENT.get().doubleValue();
        float darkenSpeed = (float) UncannyConfig.DARKNESS_DARKEN_SPEED.get().doubleValue();
        float brightenSpeed = (float) UncannyConfig.DARKNESS_BRIGHTEN_SPEED.get().doubleValue();

        if (ceiling <= floor + 0.0001F) {
            ceiling = floor + 0.0001F;
        }

        DimensionType dimensionType = level.dimensionType();
        float flickerFactor = Mth.clamp(1.0F + prevFlicker * 0.08F, 0.80F, 1.20F);

        float targetMin = 1.0F;
        float targetMax = 0.0F;
        float targetSum = 0.0F;

        for (int sky = 0; sky < LIGHTMAP_SIZE; sky++) {
            for (int block = 0; block < LIGHTMAP_SIZE; block++) {
                float skyBase = LightTexture.getBrightness(dimensionType, sky);
                float blockBase = LightTexture.getBrightness(dimensionType, block) * flickerFactor;

                float sceneLight = Math.max(
                        blockBase * profile.blockWeight,
                        skyBase * skyFactor * profile.skyWeight);
                sceneLight = Mth.clamp(sceneLight, 0.0F, 1.0F);

                float mapped = Mth.clamp((sceneLight - floor) / (ceiling - floor), 0.0F, 1.0F);
                float darkBase = 1.0F - mapped;
                float smoothCurve = darkBase * darkBase * (3.0F - 2.0F * darkBase);
                float curved = (float) Math.pow(Mth.clamp(smoothCurve, 0.0F, 1.0F), Math.max(0.001F, exponent));

                float targetDarkness = (curved * phaseStrength + weatherBonus) * lightmapStrength;
                targetDarkness = Mth.clamp(targetDarkness, 0.0F, 1.0F);

                float targetLum = Mth.clamp(1.0F - targetDarkness, maxPixelAttenuation, 1.0F);
                this.targetLuminance[sky][block] = targetLum;
                targetMin = Math.min(targetMin, targetLum);
                targetMax = Math.max(targetMax, targetLum);
                targetSum += targetLum;

                float previous = this.smoothedLuminance[sky][block];
                float rate = targetLum < previous ? darkenSpeed : brightenSpeed;
                float blend = 1.0F - (float) Math.pow(1.0F - Mth.clamp(rate, 0.001F, 1.0F), dtTicks);
                this.smoothedLuminance[sky][block] = Mth.lerp(blend, previous, targetLum);
            }
        }

        boolean uploadIntercepted = this.lastUploadAppliedTick == now;
        if (uploadIntercepted && !this.hookActiveLogged && UncannyConfig.DEBUG_LOGS.get()) {
            this.hookActiveLogged = true;
            EchoOfTheVoid.LOGGER.info("DARKNESS_REWORK hook=lightmap_upload active=true");
        }
        if (lightmapDirty && !uploadIntercepted) {
            this.dirtyWithoutUploadTicks++;
        } else {
            this.dirtyWithoutUploadTicks = 0;
        }

        if (uploadIntercepted && this.mixinInactiveLogged && UncannyConfig.DEBUG_LOGS.get()) {
            this.mixinInactiveLogged = false;
            EchoOfTheVoid.LOGGER.info("DARKNESS_REWORK hook=lightmap_upload active=true state=restored");
        }
        if (!this.mixinInactiveLogged && this.dirtyWithoutUploadTicks >= 100 && UncannyConfig.DEBUG_LOGS.get()) {
            this.mixinInactiveLogged = true;
            this.hookActiveLogged = false;
            EchoOfTheVoid.LOGGER.warn("DARKNESS_REWORK mixin_inactive tick={} dirtyWithoutUploadTicks={}", now, this.dirtyWithoutUploadTicks);
        }

        if (UncannyConfig.DEBUG_LOGS.get() && (this.lastDebugTick == Long.MIN_VALUE || now - this.lastDebugTick >= 20L)) {
            this.lastDebugTick = now;
            float avg = targetSum / (LIGHTMAP_SIZE * LIGHTMAP_SIZE);
            EchoOfTheVoid.LOGGER.info(
                    "DARKNESS_REWORK phase={} profile={} day={} moon={} rainPenalty={} skyFactor={} weather={} targetLumAvg={} targetLumMin={} targetLumMax={} lightmapDirty={} uploadIntercepted={}",
                    phase,
                    profile.id,
                    format3(skyInputs.dayVisibility()),
                    format3(skyInputs.moonFactor()),
                    format3(skyInputs.rainPenalty()),
                    format3(skyFactor),
                    format3(weatherBonus),
                    format3(avg),
                    format3(targetMin),
                    format3(targetMax),
                    lightmapDirty,
                    uploadIntercepted);
        }
    }

    public int darkenPixel(int argb, int blockLevel, int skyLevel) {
        if (!this.active) {
            return argb;
        }
        if (blockLevel < 0 || blockLevel >= LIGHTMAP_SIZE || skyLevel < 0 || skyLevel >= LIGHTMAP_SIZE) {
            return argb;
        }

        float scale = Mth.clamp(this.smoothedLuminance[skyLevel][blockLevel], 0.0F, 1.0F);
        if (scale >= 0.999F) {
            return argb;
        }

        int alpha = argb & 0xFF000000;
        int red = argb & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = (argb >> 16) & 0xFF;

        red = Mth.clamp(Math.round(red * scale), 0, 255);
        green = Mth.clamp(Math.round(green * scale), 0, 255);
        blue = Mth.clamp(Math.round(blue * scale), 0, 255);

        return alpha | (blue << 16) | (green << 8) | red;
    }

    public boolean isActive() {
        return this.active;
    }

    public void markUploadIntercepted() {
        if (this.currentGameTick != Long.MIN_VALUE) {
            this.lastUploadAppliedTick = this.currentGameTick;
        }
    }

    private void resetLuminanceTables(float value) {
        for (int y = 0; y < LIGHTMAP_SIZE; y++) {
            for (int x = 0; x < LIGHTMAP_SIZE; x++) {
                this.targetLuminance[y][x] = value;
                this.smoothedLuminance[y][x] = value;
            }
        }
    }

    private boolean isProfileEnabled(DimensionProfile profile) {
        return switch (profile.id) {
            case "overworld" -> UncannyConfig.DARKNESS_ENABLE_OVERWORLD.get();
            case "nether" -> UncannyConfig.DARKNESS_ENABLE_NETHER.get();
            case "end" -> UncannyConfig.DARKNESS_ENABLE_END.get();
            case "default" -> UncannyConfig.DARKNESS_ENABLE_DEFAULT_DIMENSIONS.get();
            case "skyless" -> UncannyConfig.DARKNESS_ENABLE_SKYLESS_DIMENSIONS.get();
            default -> true;
        };
    }

    private DimensionProfile resolveDimensionProfile(Level level) {
        if (level.dimension() == Level.OVERWORLD) {
            return DimensionProfile.OVERWORLD;
        }
        if (level.dimension() == Level.NETHER) {
            return DimensionProfile.NETHER;
        }
        if (level.dimension() == Level.END) {
            return DimensionProfile.END;
        }
        return level.dimensionType().hasSkyLight() ? DimensionProfile.DEFAULT : DimensionProfile.SKYLESS;
    }

    private float resolvePhaseStrength(int phase) {
        return switch (phase) {
            case 1 -> (float) UncannyConfig.DARKNESS_PHASE1_STRENGTH.get().doubleValue();
            case 2 -> (float) UncannyConfig.DARKNESS_PHASE2_STRENGTH.get().doubleValue();
            case 3 -> (float) UncannyConfig.DARKNESS_PHASE3_STRENGTH.get().doubleValue();
            default -> (float) UncannyConfig.DARKNESS_PHASE4_STRENGTH.get().doubleValue();
        };
    }

    private SkyInputs resolveSkyInputs(ClientLevel level) {
        if (UncannyConfig.DARKNESS_BLOCK_LIGHT_ONLY.get() || !level.dimensionType().hasSkyLight()) {
            return new SkyInputs(0.0F, 0.0F, 1.0F, 0.0F);
        }

        float dayVisibility = Mth.clamp(1.0F - (level.getSkyDarken(1.0F) / 15.0F), 0.0F, 1.0F);
        float nightVisibility = 1.0F - dayVisibility;
        float moon = UncannyConfig.DARKNESS_IGNORE_MOON_PHASE.get()
                ? 1.0F
                : Mth.clamp(level.getMoonBrightness(), 0.0F, 1.0F);
        float rainPenalty = Mth.clamp(1.0F - level.getRainLevel(1.0F) * 0.35F - level.getThunderLevel(1.0F) * 0.45F, 0.25F, 1.0F);
        float skyFactor = Mth.clamp((dayVisibility + nightVisibility * moon) * rainPenalty, 0.0F, 1.0F);
        return new SkyInputs(dayVisibility, moon, rainPenalty, skyFactor);
    }

    private float resolveWeatherBonus() {
        float bonus = 0.0F;
        if (UncannyClientStateSync.isClientWeather("fog_black")) {
            bonus = Math.max(bonus, 0.15F);
        }
        if (UncannyClientStateSync.isClientWeather("fog_static_wall")) {
            bonus = Math.max(bonus, 0.11F);
        }
        if (UncannyClientStateSync.isClientWeather("sky_empty")) {
            bonus = Math.max(bonus, 0.18F);
        }
        if (UncannyClientStateSync.isClientWeather("rain_ash")) {
            bonus = Math.max(bonus, 0.06F);
        }
        if (UncannyClientStateSync.isClientWeather("rain_sobbing")) {
            bonus = Math.max(bonus, 0.04F);
        }
        if (UncannyClientStateSync.isClientHunterFogActive()) {
            bonus = Math.max(bonus, 0.07F);
        }
        return Mth.clamp(bonus, 0.0F, 0.35F);
    }

    private static String format3(float value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private static final class DimensionProfile {
        private static final DimensionProfile OVERWORLD = new DimensionProfile("overworld", 1.0F, 1.0F);
        private static final DimensionProfile NETHER = new DimensionProfile("nether", 1.0F, 0.0F);
        private static final DimensionProfile END = new DimensionProfile("end", 1.0F, 0.55F);
        private static final DimensionProfile DEFAULT = new DimensionProfile("default", 1.0F, 0.95F);
        private static final DimensionProfile SKYLESS = new DimensionProfile("skyless", 1.0F, 0.0F);

        private final String id;
        private final float blockWeight;
        private final float skyWeight;

        private DimensionProfile(String id, float blockWeight, float skyWeight) {
            this.id = id;
            this.blockWeight = blockWeight;
            this.skyWeight = skyWeight;
        }
    }

    private record SkyInputs(float dayVisibility, float moonFactor, float rainPenalty, float skyFactor) {
    }
}
