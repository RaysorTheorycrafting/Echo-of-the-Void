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
