package com.eotv.echoofthevoid.event.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eotv.echoofthevoid.dev.UncannyDevCatalog;
import com.eotv.echoofthevoid.dev.UncannyDevMetadataCatalog;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class LocalizedWeatherSurfaceTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid");
    private static final String[] IDS = {
            "rain_front",
            "suspended_rain",
            "dry_eye",
            "clear_downpour",
            "wrong_snowline",
            "light_avoiding_rain",
            "converging_rain",
            "leaking_sky"
    };

    @Test
    void everyApprovedWeatherHasOneInspectableDevTrigger() {
        Map<String, Long> triggerCounts = UncannyDevCatalog.entries().stream()
                .filter(entry -> entry.category() == UncannyDevCatalog.Category.WEATHER)
                .filter(entry -> entry.actionKind() == UncannyDevCatalog.ActionKind.TRIGGER_WEATHER)
                .map(UncannyDevCatalog.Entry::actionArg)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        for (String id : IDS) {
            assertEquals(1L, triggerCounts.getOrDefault(id, 0L), id);
            UncannyDevCatalog.Entry entry = UncannyDevCatalog.byId("weather_" + id);
            assertNotNull(entry, id);
            UncannyDevMetadataCatalog.Info info = UncannyDevMetadataCatalog.describe(entry);
            assertEquals(UncannyDevMetadataCatalog.ImplementationStatus.WORKING_BUILD, info.implementation(), id);
            assertEquals(UncannyDevMetadataCatalog.Authority.SHARED, info.authority(), id);
            assertEquals(UncannyDevMetadataCatalog.Danger.NONE, info.danger(), id);
        }
    }

    @Test
    void localizedWeatherIsOneServerSelectedSharedPresentation() throws IOException {
        String weather = read(JAVA_ROOT.resolve(Path.of("event", "UncannyWeatherSystem.java")));
        String network = read(JAVA_ROOT.resolve(Path.of("network", "UncannyNetwork.java")));
        String payload = read(JAVA_ROOT.resolve(Path.of("network", "UncannyLocalizedWeatherPayload.java")));

        for (String constant : new String[]{
                "RAIN_FRONT", "SUSPENDED_RAIN", "DRY_EYE", "CLEAR_DOWNPOUR",
                "WRONG_SNOWLINE", "LIGHT_AVOIDING_RAIN", "CONVERGING_RAIN", "LEAKING_SKY"}) {
            assertTrue(weather.contains(constant), constant);
        }
        assertTrue(weather.contains("if (player.serverLevel() == server.overworld())"));
        assertTrue(weather.contains("PacketDistributor.sendToPlayer(player, payload)"));
        assertTrue(network.contains("UncannyLocalizedWeatherPayload.TYPE"));
        assertTrue(payload.contains("centerX"));
        assertTrue(payload.contains("remainingTicks"));
    }

    @Test
    void presentationNeverMutatesWeatherBlocksOrGameplay() throws IOException {
        String client = read(JAVA_ROOT.resolve(Path.of(
                "client", "UncannyLocalizedWeatherClientEffects.java")));
        String configure = section(
                read(JAVA_ROOT.resolve(Path.of("event", "UncannyWeatherSystem.java"))),
                "private static boolean configureLocalizedWeather(",
                "private static BlockPos findOutdoorWeatherAnchor(");

        assertTrue(client.contains("filterPrecipitation("));
        assertTrue(client.contains("RAIN_TEXTURE"));
        assertTrue(client.contains("ParticleTypes.RAIN"));
        assertFalse(client.contains("setWeatherParameters("));
        assertFalse(client.contains("setBlock("));
        assertFalse(client.contains("setBlockAndUpdate("));
        assertFalse(configure.contains("setWeather("));
        assertFalse(configure.contains("setBlock("));
        assertFalse(configure.contains("addFreshEntity("));
    }

    @Test
    void precipitationInterceptionIsClientOnlyAndFailsFastIfMappingsDrift() throws IOException {
        String mixin = read(JAVA_ROOT.resolve(Path.of(
                "client", "weather_mixin", "UncannyLocalizedPrecipitationMixin.java")));
        String config = read(Path.of("src", "main", "resources", "echoofthevoid.weather.mixins.json"));
        String mods = read(Path.of("src", "main", "templates", "META-INF", "neoforge.mods.toml"));

        assertTrue(mixin.contains("@Mixin(LevelRenderer.class)"));
        assertTrue(mixin.contains("method = {\"renderSnowAndRain\", \"tickRain\"}"));
        assertTrue(mixin.contains("Biome;getPrecipitationAt"));
        assertTrue(config.contains("\"required\": true"));
        assertTrue(config.contains("\"package\": \"com.eotv.echoofthevoid.client.weather_mixin\""));
        assertTrue(config.contains("\"UncannyLocalizedPrecipitationMixin\""));
        assertTrue(mods.contains("config=\"${mod_id}.weather.mixins.json\""));
    }

    @Test
    void lightAvoidanceOnlyUsesBoundedRememberedPlayerLights() throws IOException {
        String state = read(JAVA_ROOT.resolve(Path.of("state", "UncannyWorldState.java")));
        String weather = read(JAVA_ROOT.resolve(Path.of("event", "UncannyWeatherSystem.java")));

        assertTrue(state.contains("playerPlacedLights"));
        assertTrue(state.contains("playerPlacedLights.size() > 256"));
        assertTrue(state.contains("tag.putLongArray(\"playerPlacedLights\""));
        assertTrue(state.contains("tag.getLongArray(\"playerPlacedLights\""));
        assertTrue(weather.contains("BlockEvent.EntityPlaceEvent"));
        assertTrue(weather.contains("BlockEvent.BreakEvent"));
        assertTrue(weather.contains("rememberPlayerPlacedLight"));
        assertTrue(weather.contains("forgetPlayerPlacedLight"));
        assertTrue(weather.contains("isTrackedPlayerLight"));
    }

    @Test
    void clearDownpourRequiresClearWeatherAndOtherLocalizedRainRequiresRain() throws IOException {
        String weather = read(JAVA_ROOT.resolve(Path.of("event", "UncannyWeatherSystem.java")));
        String availability = section(
                weather,
                "private static boolean isWeatherContextAvailable(",
                "private static boolean configureLocalizedWeather(");

        assertTrue(availability.contains("case CLEAR_DOWNPOUR -> !level.isRaining()"));
        assertTrue(availability.contains("case LIGHT_AVOIDING_RAIN -> level.isRaining()"));
        assertTrue(availability.contains("default -> level.isRaining()"));
        assertTrue(weather.contains("findLeakAnchor"));
        assertTrue(weather.contains("level.canSeeSky(roof.above())"));
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String section(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());
        assertTrue(start >= 0, "Missing start marker: " + startMarker);
        assertTrue(end > start, "Missing end marker: " + endMarker);
        return source.substring(start, end);
    }
}
