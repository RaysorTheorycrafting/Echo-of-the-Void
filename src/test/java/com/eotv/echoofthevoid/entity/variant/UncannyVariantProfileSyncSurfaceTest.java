package com.eotv.echoofthevoid.entity.variant;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class UncannyVariantProfileSyncSurfaceTest {
    private static final Path ROOT = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid");

    @Test
    void profileIsResentOnTrackingAndBufferedAcrossPacketOrdering() throws IOException {
        String mod = read(ROOT.resolve("EchoOfTheVoid.java"));
        String clientMod = read(ROOT.resolve("EchoOfTheVoidClient.java"));
        String network = read(ROOT.resolve(Path.of("network", "UncannyNetwork.java")));
        String server = read(ROOT.resolve(Path.of(
                "entity", "variant", "UncannyVariantProfileSyncSystem.java")));
        String client = read(ROOT.resolve(Path.of(
                "client", "UncannyVariantProfileClientState.java")));

        assertTrue(mod.contains("UncannyVariantProfileSyncSystem::onPlayerStartTracking"));
        assertTrue(clientMod.contains("UncannyVariantProfileClientState::onClientTick"));
        assertTrue(network.contains("UncannyVariantProfilePayload.TYPE"));
        assertTrue(network.contains("UncannyVariantProfileClientState.apply(payload)"));
        assertTrue(server.contains("PacketDistributor.sendToPlayer"));
        assertTrue(server.contains("UncannyPassiveEnabled"));
        assertTrue(client.contains("PENDING_LIFETIME_TICKS = 200L"));
        assertTrue(client.contains("level != trackedLevel"));
    }

    @Test
    void detachedDevPreviewUsesTheSameCatalogPresentationProfile() throws IOException {
        String preview = read(ROOT.resolve(Path.of("client", "UncannyDevEntityPreview.java")));
        assertTrue(preview.contains("ReplacementVariantExpansionCatalog.variant"));
        assertTrue(preview.contains("ApprovedVanillaVariantCatalog.byId"));
        assertTrue(preview.contains("ApprovedVanillaVariantCatalog.speciesByTypeKey"));
        assertTrue(preview.contains("\"approved_random\".equals(parts[0])"));
        assertTrue(preview.contains("VanillaVariantBehaviorRuntime.visualTag"));
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
