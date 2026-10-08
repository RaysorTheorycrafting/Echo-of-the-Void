package com.eotv.echoofthevoid.diagnostics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DiagnosticInstrumentationSurfaceTest {
    private static final Path SOURCE_ROOT = Path.of("src", "main", "java", "com", "eotv", "echoofthevoid");

    @Test
    void lifecycleAndOperatorCommandsRemainWired() throws Exception {
        String entrypoint = read(SOURCE_ROOT.resolve("EchoOfTheVoid.java"));
        String commands = read(SOURCE_ROOT.resolve("command").resolve("UncannyCommandRegistry.java"));

        assertTrue(entrypoint.contains("UncannyDiagnosticEvents::onServerStarted"));
        assertTrue(entrypoint.contains("UncannyDiagnosticEvents::onServerStopping"));
        assertTrue(entrypoint.contains("UncannyDiagnosticEvents::onServerStopped"));
        assertTrue(entrypoint.contains("UncannyDiagnosticEvents::onServerTickPre"));
        assertTrue(entrypoint.contains("UncannyDiagnosticEvents::onServerTickPost"));
        assertTrue(entrypoint.contains("UncannyDiagnosticEvents::onLevelSave"));
        assertTrue(entrypoint.contains("UncannyDiagnosticEvents::onPlayerStartTracking"));
        assertTrue(entrypoint.contains("UncannyDiagnosticEvents::onPlayerStopTracking"));
        assertTrue(commands.contains("Commands.literal(\"diagnostics\")"));
        assertTrue(commands.contains("Commands.literal(\"checkpoint\")"));
        assertTrue(commands.contains("Commands.literal(\"bundle\")"));
        assertTrue(commands.contains("Commands.literal(\"purge\")"));
        assertTrue(commands.contains("Commands.literal(\"confirm\")"));
    }

    @Test
    void allSchedulersExposeOutcomesToTheRecorder() throws Exception {
        String paranoia = read(SOURCE_ROOT.resolve("event").resolve("UncannyParanoiaEventSystem.java"));
        String weather = read(SOURCE_ROOT.resolve("event").resolve("UncannyWeatherSystem.java"));

        assertTrue(paranoia.contains("recordEventOutcome(player, \"primary\""));
        assertTrue(paranoia.contains("recordEventOutcome(player, \"ambient\""));
        assertTrue(paranoia.contains("recordEventOutcome(player, \"special\""));
        assertTrue(paranoia.contains("\"tension_builder_started\""));
        assertTrue(paranoia.contains("\"grand_warden_started\""));
        assertTrue(paranoia.contains("\"grand_warden_ended\""));
        assertTrue(weather.contains("\"weather_roll_miss\""));
        assertTrue(weather.contains("\"weather_started\""));
        assertTrue(weather.contains("\"weather_stopped\""));
    }

    @Test
    void recorderDoesNotCollectChatInventorySeedOrRawPlayerIdentity() throws Exception {
        String recorder = read(SOURCE_ROOT.resolve("diagnostics").resolve("UncannyDiagnostics.java"));

        assertFalse(recorder.contains("getChat"));
        assertFalse(recorder.contains("getInventory"));
        assertFalse(recorder.contains("getSeed()"));
        assertFalse(recorder.contains("getStringUUID()"));
        assertTrue(recorder.contains("PLAYER_ALIASES"));
        assertTrue(recorder.contains("sanitizeCapturedText"));
        assertTrue(read(SOURCE_ROOT.resolve("diagnostics").resolve("UncannyLogCaptureAppender.java"))
                .contains("UncannyDiagnostics.sanitizeCapturedText"));
    }

    @Test
    void clientFailuresAndSoundDeliveryAreObservableAndBounded() throws Exception {
        String network = read(SOURCE_ROOT.resolve("network").resolve("UncannyNetwork.java"));
        String payload = read(SOURCE_ROOT.resolve("network").resolve("UncannyClientDiagnosticPayload.java"));
        String client = read(SOURCE_ROOT.resolve("client").resolve("UncannyClientDiagnostics.java"));
        String mental = read(SOURCE_ROOT.resolve("sound").resolve("UncannySoundDelivery.java"));
        String physical = read(SOURCE_ROOT.resolve("sound").resolve("UncannyPhysicalSoundDelivery.java"));

        assertTrue(network.contains("UncannyClientDiagnosticPayload.TYPE"));
        assertTrue(network.contains("UncannyDiagnostics.clientReport"));
        assertTrue(payload.contains("ByteBufCodecs.stringUtf8(8192)"));
        assertTrue(client.contains("MAX_QUEUED_REPORTS = 64"));
        assertTrue(client.contains("redactLocalPaths"));
        assertTrue(mental.contains("UncannyDiagnostics.mentalSoundSent"));
        assertTrue(physical.contains("UncannyDiagnostics.physicalSoundPlayed"));
    }

    @Test
    void specialPresentationIsTracedFromServerTrackingThroughClientRendering() throws Exception {
        String entrypoint = read(SOURCE_ROOT.resolve("EchoOfTheVoidClient.java"));
        String client = read(SOURCE_ROOT.resolve("client").resolve("UncannyClientDiagnostics.java"));
        String diagnostics = read(SOURCE_ROOT.resolve("diagnostics").resolve("UncannyDiagnostics.java"));
        String follower = read(SOURCE_ROOT.resolve("entity").resolve("custom").resolve("UncannyFollowerEntity.java"));
        String paranoia = read(SOURCE_ROOT.resolve("event").resolve("UncannyParanoiaEventSystem.java"));
        String analyzer = Files.readString(
                Path.of("tools", "Analyze-EotvDiagnostics.ps1"), StandardCharsets.UTF_8);

        assertTrue(entrypoint.contains("UncannyClientDiagnostics::onRenderLivingPost"));
        assertTrue(client.contains("client_special_rendered"));
        assertTrue(client.contains("client_special_directly_observable"));
        assertTrue(client.contains("runtime_entity_id="));
        assertTrue(diagnostics.contains("special_client_tracking_started"));
        assertTrue(diagnostics.contains("special_client_tracking_stopped"));
        assertTrue(diagnostics.contains("\"runtime_entity_id\", entity.getId()"));
        assertTrue(diagnostics.contains("previous.entityTickCount()"));
        assertTrue(follower.contains("this.setPersistenceRequired()"));
        assertTrue(follower.contains("discardWithReason"));
        assertTrue(paranoia.contains("case \"follower\" -> spawnFollower(player, true) || spawnFollower(player, false);"));
        assertTrue(analyzer.contains("Cycle de vie et suivi reseau des Specials"));
        assertTrue(analyzer.contains("Preuves de rendu client des Specials"));
        assertTrue(analyzer.contains("Parcours detailles des Specials"));
        assertTrue(analyzer.contains("Parcours partiels des anciennes sessions"));
        assertTrue(analyzer.contains("rendu_client=non mesurable avec cette ancienne instrumentation"));
        assertTrue(analyzer.contains("Get-BlockDistance"));
        assertTrue(analyzer.contains("[System.Collections.IDictionary]$Trace"));
    }

    @Test
    void everySpecialSpawnSurfaceChecksServerAcceptance() throws Exception {
        String diagnostics = read(SOURCE_ROOT.resolve("diagnostics").resolve("UncannyDiagnostics.java"));
        String paranoia = read(SOURCE_ROOT.resolve("event").resolve("UncannyParanoiaEventSystem.java"));
        String watcher = read(SOURCE_ROOT.resolve("event").resolve("UncannyWatcherSystem.java"));
        String dormant = read(SOURCE_ROOT.resolve("event").resolve("UncannyDoubleDormantSystem.java"));
        String structures = read(SOURCE_ROOT.resolve("event").resolve("UncannyStructureFeatureSystem.java"));
        String approved = read(SOURCE_ROOT.resolve("event").resolve("special").resolve("ApprovedSpecialSystem.java"));
        String commands = read(SOURCE_ROOT.resolve("command").resolve("UncannyCommandRegistry.java"));
        String devMenu = read(SOURCE_ROOT.resolve("dev").resolve("UncannyDevActionExecutor.java"));

        assertTrue(diagnostics.contains("level_accepted"));
        assertTrue(diagnostics.contains("level_rejected"));
        assertTrue(paranoia.contains("specialSpawnResult(player, shadow, added"));
        assertTrue(paranoia.contains("specialSpawnResult(player, hurler, added"));
        assertTrue(paranoia.contains("specialSpawnResult(player, manual, added"));
        assertTrue(paranoia.contains("specialSpawnResult(player, knocker, added"));
        assertTrue(paranoia.contains("specialSpawnResult(player, pulse, added"));
        assertTrue(paranoia.contains("specialSpawnResult(player, usher, added"));
        assertTrue(paranoia.contains("specialSpawnResult(player, keeper, added"));
        assertTrue(paranoia.contains("specialSpawnResult(player, tenant, added"));
        assertTrue(paranoia.contains("return added ? stalker : null"));
        assertTrue(watcher.contains("return added;"));
        assertTrue(watcher.contains("specialSpawnResult(player, watcher, added"));
        assertTrue(dormant.contains("specialSpawnResult(player, doubleDormant, added"));
        assertTrue(dormant.contains("if (!spawnMimic(player, baseContext.baseCenter, level))"));
        assertTrue(dormant.contains("return added;"));
        assertTrue(dormant.contains("public static void forceMimic(ServerPlayer player)"));
        assertTrue(dormant.contains("public static boolean forceMimicChecked(ServerPlayer player)"));
        assertTrue(commands.contains("UncannyDoubleDormantSystem.forceMimicChecked(target)"));
        assertTrue(devMenu.contains("UncannyDoubleDormantSystem.forceMimicChecked(target)"));
        assertTrue(structures.contains("specialSpawnResult(player, terror, added"));
        assertTrue(approved.contains("specialSpawnResult(player, entity, added"));
        assertTrue(commands.contains("specialSpawnResult(target, entity, added"));
        assertTrue(devMenu.contains("specialSpawnResult(target, entity, added"));
    }

    @Test
    void followerEvasionRemainsDiagnosableAndReloadSafe() throws Exception {
        String follower = read(SOURCE_ROOT.resolve("entity").resolve("custom").resolve("UncannyFollowerEntity.java"));

        assertTrue(follower.contains("source.is(DamageTypeTags.IS_PROJECTILE)"));
        assertTrue(follower.contains("public boolean canBeHitByProjectile()"));
        assertTrue(follower.contains("projectile_ignored"));
        assertTrue(follower.contains("melee_hit_evaded"));
        assertTrue(follower.contains("offscreen_repositioned"));
        assertTrue(follower.contains("tag.putInt(\"EvasiveRepositionsRemaining\""));
        assertTrue(follower.contains("tag.getInt(\"EvasiveRepositionsRemaining\""));
        assertTrue(follower.contains("tag.putBoolean(\"EvasiveRepositionArmed\""));
        assertFalse(follower.contains("tag.getBoolean(\"EvasiveRepositionArmed\""));
        assertTrue(follower.contains("this.evasiveRepositionArmed = false;"));
        assertTrue(follower.contains("tag.putLong(\"EvasiveBurstEndTick\""));
        assertFalse(follower.contains("tag.getLong(\"EvasiveBurstEndTick\""));
        for (String key : new String[] {
                "NextEvasiveRepositionTick",
                "AttackSuppressedUntilTick"
        }) {
            assertTrue(follower.contains("tag.putLong(\"" + key + "\""), key);
            assertTrue(follower.contains("tag.getLong(\"" + key + "\""), key);
        }
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
