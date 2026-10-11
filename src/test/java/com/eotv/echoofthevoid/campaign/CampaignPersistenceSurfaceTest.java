package com.eotv.echoofthevoid.campaign;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CampaignPersistenceSurfaceTest {
    private static final Path JAVA_ROOT = Path.of("src", "main", "java", "com", "eotv", "echoofthevoid");

    @Test
    void directorUsesAdditiveSavedDataAndTheExistingPhaseTickOwner() throws IOException {
        String state = read(JAVA_ROOT.resolve(Path.of("state", "UncannyWorldState.java")));
        String phaseManager = read(JAVA_ROOT.resolve(Path.of("phase", "UncannyPhaseManager.java")));
        String config = read(JAVA_ROOT.resolve(Path.of("config", "UncannyConfig.java")));

        for (String key : new String[] {
                "campaignDirectorInitialized", "campaignElapsedTicks", "campaignLastObservedDayTime",
                "campaignDirectorSeed", "campaignBeat", "campaignBeatRemainingTicks",
                "campaignBeatSequence", "campaignLastStrongEventTick", "campaignRecentFamilies",
                "campaignCulminationState", "campaignCulminationScheduledTick",
                "campaignCulminationRetryTick"
        }) {
            assertTrue(state.contains("\"" + key + "\""), key);
        }
        assertTrue(phaseManager.contains("UncannyCampaignDirector.tick(server, state, !activePlayers.isEmpty())"));
        assertTrue(phaseManager.contains("state.resetCampaignDirector()"));
        assertTrue(config.contains("uncanny.campaign.extraLong100Days"));
        assertTrue(config.contains("false"));
    }

    @Test
    void journalProgressIsAnAdditiveBlockMigratedFromPerPlayerMasks() throws IOException {
        String state = read(JAVA_ROOT.resolve(Path.of("state", "UncannyWorldState.java")));
        String record = read(JAVA_ROOT.resolve(Path.of("state", "JournalProgressRecord.java")));
        assertTrue(record.contains("TAG = \"journalProgress\""));
        for (String key : new String[] {
                "foundMask", "lastDiscoveryElapsedTicks", "lastDiscoveryAnchor", "starterMapDelivered", "pending", "places", "maps",
                "volume", "dimension", "pos", "status", "reservedElapsedTicks", "mapId",
                "lostMask", "rerolledMask", "holder", "taken"
        }) {
            assertTrue(record.contains("\"" + key + "\""), key);
        }
        assertTrue(state.contains("tag.getLongArray(\"playerPlacedContainers\")")
                        && state.contains("tag.putLongArray(\"playerPlacedContainers\", playerPlacedContainers)"),
                "Player-placed containers are an additive index");
        assertTrue(state.contains("readIntMap(tag, \"historyTomeMask\", data.historyTomeMask);"),
                "Per-player masks stay readable");
        assertTrue(state.contains("JournalProgressRecord.migrated(data.historyTomeMask.values())"),
                "Worlds without the block migrate from per-player masks");
        assertTrue(state.indexOf("readIntMap(tag, \"historyTomeMask\"")
                        < state.indexOf("JournalProgressRecord.migrated("),
                "Migration must run after the per-player masks are loaded");
    }

    @Test
    void runtimeUsesTheExistingTensionBuilderAndKeepsQaOutOfCampaignProgress() throws IOException {
        String runtime = read(JAVA_ROOT.resolve(Path.of("event", "UncannyParanoiaEventSystem.java")));
        String controller = read(JAVA_ROOT.resolve(Path.of("event", "UncannyEventController.java")));
        assertTrue(runtime.contains("TensionStartCause.CAMPAIGN_CULMINATION"));
        assertTrue(runtime.contains("TensionStartCause.NATURAL"));
        assertTrue(runtime.contains("TensionStartCause.QA"));
        assertTrue(runtime.contains("if (cause == TensionStartCause.QA)"));
        assertTrue(runtime.contains("UncannyCampaignDirector.recordNaturalMajorEventStarted(state)"));
        assertTrue(runtime.contains("player.isSleeping()"));
        assertTrue(runtime.contains("player.isRemoved()"));
        assertTrue(runtime.contains("CAMPAIGN_CULMINATION_CONTEXT.isStable"));
        assertTrue(controller.contains("deferCampaignCulminationForPlayer(player)"));
        assertTrue(controller.contains("forgetCampaignCulminationPlayer(player)"));
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
