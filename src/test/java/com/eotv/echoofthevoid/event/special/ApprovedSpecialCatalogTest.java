package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eotv.echoofthevoid.dev.UncannyDevCatalog;
import com.eotv.echoofthevoid.dev.UncannyDevMetadataCatalog;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventCatalog;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventLane;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ApprovedSpecialCatalogTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid");
    private static final List<String> IDS = List.of(
            "surveyor", "mourner", "doubler", "ferryman", "listener", "bystander");

    @Test
    void catalogContainsOnlyTheSixActiveApprovedSpecials() {
        assertEquals(IDS, ApprovedSpecialCatalog.definitions().stream()
                .map(ApprovedSpecialCatalog.Definition::id)
                .toList());
        assertEquals(6, new HashSet<>(IDS).size());
        assertNull(ApprovedSpecialCatalog.byId("impostor"));
        assertNull(ApprovedSpecialCatalog.byId("pilgrim"));
        assertNull(ApprovedSpecialCatalog.byId("climber"));
        assertTrue(ApprovedSpecialCatalog.definitions().stream()
                .allMatch(definition -> definition.displayName().matches("[A-Za-z]+\\?")));
    }

    @Test
    void phaseDangerWeightAndWorkingStatusAreFrozen() {
        assertDefinition("surveyor", 2, 0, 4, ApprovedSpecialCatalog.Status.WORKING);
        assertDefinition("mourner", 3, 0, 1, ApprovedSpecialCatalog.Status.WORKING);
        assertDefinition("doubler", 3, 2, 2, ApprovedSpecialCatalog.Status.WORKING);
        assertDefinition("ferryman", 3, 1, 3, ApprovedSpecialCatalog.Status.WORKING);
        assertDefinition("listener", 2, 0, 3, ApprovedSpecialCatalog.Status.WORKING);
        assertDefinition("bystander", 2, 0, 4, ApprovedSpecialCatalog.Status.WORKING);

        for (ApprovedSpecialCatalog.Definition definition : ApprovedSpecialCatalog.definitions()) {
            var descriptor = ParanoiaEventCatalog.require(definition.id());
            assertEquals(definition.minimumPhase(), descriptor.minimumPhase(), definition.id());
            assertEquals(definition.weight(), descriptor.specialWeight(), definition.id());
            assertEquals(Set.of(ParanoiaEventLane.SPECIAL), descriptor.lanes(), definition.id());
        }
        assertNull(ParanoiaEventCatalog.byId().get("climber"));
    }

    @Test
    void everyApprovedSpecialHasOneInspectableDevAction() {
        Set<String> arguments = new HashSet<>();
        for (UncannyDevCatalog.Entry entry : UncannyDevCatalog.entries()) {
            if (entry.actionKind() == UncannyDevCatalog.ActionKind.SPAWN_SPECIAL && IDS.contains(entry.actionArg())) {
                assertTrue(arguments.add(entry.actionArg()), entry.actionArg());
            }
        }
        assertEquals(Set.copyOf(IDS), arguments);
        for (String id : IDS) {
            UncannyDevCatalog.Entry entry = UncannyDevCatalog.byId("entity_" + id + "_spawn");
            assertNotNull(entry, id);
            UncannyDevMetadataCatalog.Info info = UncannyDevMetadataCatalog.describe(entry);
            assertEquals(UncannyDevMetadataCatalog.Authority.SHARED, info.authority(), id);
            assertEquals(UncannyDevMetadataCatalog.ValidationNeed.MANUAL_REQUIRED, info.validation(), id);
            assertEquals(UncannyDevMetadataCatalog.ImplementationStatus.WORKING_BUILD,
                    info.implementation(), id);
        }
    }

    @Test
    void contextualSpawnsAndBoundedResponsesRemainServerAuthoritative() throws IOException {
        String system = read(JAVA_ROOT.resolve(Path.of("event", "special", "ApprovedSpecialSystem.java")));
        String entity = read(JAVA_ROOT.resolve(Path.of("entity", "custom", "UncannyApprovedSpecialEntity.java")));

        for (String context : List.of(
                "findSurveyorContext", "findMournerContext", "findDoublerContext", "findFerrymanContext",
                "findListenerContext", "findBystanderContext")) {
            assertTrue(system.contains(context), context);
        }
        assertFalse(system.contains("findClimberContext"));
        assertTrue(system.contains("memory.tick() > afterTick"));
        assertTrue(system.contains("memories.descendingIterator()"));
        assertTrue(system.contains("if (added && !debug && \"mourner\".equals(definition.id()))"));
        assertTrue(entity.contains("if (attacksDelivered >= 3)"));
        assertTrue(entity.contains("this.stateTicks < 22"));
        assertTrue(entity.contains("failedActions++"));
        assertTrue(entity.contains("copiedActions++"));
        assertTrue(entity.contains("LastSoundMemoryTick"));
        assertTrue(entity.contains("FocusBoat"));
        assertTrue(entity.contains("isEligibleFerrymanBoat"));
        assertTrue(entity.contains("isSafeFerrymanFollowPosition"));
        assertTrue(entity.contains("isSafeFerrymanRevealPosition"));
        assertTrue(entity.contains("MournerAudibleCuePlayed"));
        assertTrue(entity.contains("FerrymanRevealStarted"));
        assertTrue(entity.contains("UNCANNY_MOURNER_SOB"));
        assertTrue(entity.contains("UNCANNY_FERRYMAN_WAKE"));
        assertTrue(entity.contains("level.getEntity(combat.attackerEntityId())"));
        assertFalse(entity.contains("level.getEntity(combat.observedEntityId())"));
        assertFalse(system.contains("findWater("));
        assertFalse(entity.contains("boat.hurt("));
        assertFalse(entity.contains("ejectPassengers("));
        assertFalse(entity.contains("setBlock("));
        assertFalse(entity.contains("enableDoorNavigation"));
    }

    @Test
    void climberPrototypeIsCompletelyRemoved() throws IOException {
        String registry = read(JAVA_ROOT.resolve(Path.of("entity", "UncannyEntityRegistry.java")));
        String scheduler = read(JAVA_ROOT.resolve(Path.of("event", "UncannyParanoiaEventSystem.java")));
        String sounds = Files.readString(Path.of(
                "src", "main", "resources", "assets", "echoofthevoid", "sounds.json"), StandardCharsets.UTF_8);

        assertNull(ApprovedSpecialCatalog.byId("climber"));
        assertNull(UncannyDevCatalog.byId("entity_climber_spawn"));
        assertFalse(Files.exists(JAVA_ROOT.resolve(Path.of("entity", "custom", "UncannyClimberEntity.java"))));
        assertFalse(Files.exists(JAVA_ROOT.resolve(Path.of("client", "UncannyClimberRenderer.java"))));
        assertFalse(Files.exists(Path.of(
                "src", "main", "resources", "assets", "echoofthevoid", "sounds", "uncanny", "climber")));
        assertFalse(registry.contains("uncanny_climber"));
        assertFalse(sounds.contains("uncanny_climber"));
        assertFalse(scheduler.contains("addSpecialEntityChoiceIfReady(specialChoices, player, \"climber\""));
    }

    @Test
    void newCreatureSoundsRemainPhysicalAndInspectable() throws IOException {
        String shared = read(JAVA_ROOT.resolve(Path.of("entity", "custom", "UncannyApprovedSpecialEntity.java")))
                .replace("\r\n", "\n");
        String physicalDelivery = read(JAVA_ROOT.resolve(Path.of("sound", "UncannyPhysicalSoundDelivery.java")))
                .replace("\r\n", "\n");
        String attacker = read(JAVA_ROOT.resolve(Path.of("entity", "custom", "UncannyStalkerEntity.java")))
                .replace("\r\n", "\n");
        String sounds = Files.readString(Path.of(
                "src", "main", "resources", "assets", "echoofthevoid", "sounds.json"), StandardCharsets.UTF_8);

        for (String id : List.of(
                "uncanny_mourner_sob", "uncanny_ferryman_wake",
                "uncanny_attacker_rush", "uncanny_attacker_scream",
                "uncanny_attacker_hurt", "uncanny_attacker_death")) {
            assertTrue(sounds.contains("\"" + id + "\""), id);
            assertNotNull(UncannyDevCatalog.byId("audio_physical_" + id), id);
        }
        assertNull(UncannyDevCatalog.byId("audio_physical_uncanny_climber_scrape"));
        assertTrue(shared.contains("UncannyPhysicalSoundDelivery.playFromEntity("));
        assertTrue(physicalDelivery.contains("sourceEntity.getX()"));
        assertTrue(physicalDelivery.contains("sourceEntity.getY()"));
        assertTrue(physicalDelivery.contains("sourceEntity.getZ()"));
        assertFalse(physicalDelivery.contains("null,\n                sourceEntity,"));
        assertTrue(attacker.contains("level.playSound(\n                null,\n                this,"));
        assertFalse(shared.contains("UncannySoundDelivery"));
        assertFalse(attacker.contains("UncannySoundDelivery"));
        assertTrue(attacker.contains("this.random.nextInt(4)"));
        assertTrue(attacker.contains("UNCANNY_ATTACKER_HURT"));
        assertTrue(attacker.contains("UNCANNY_ATTACKER_DEATH"));
        assertTrue(attacker.contains("UNCANNY_ATTACKER_SCREAM"));
        assertTrue(attacker.contains("ATTACKER_BODY_SOUND_VOLUME = 1.00F"));
        assertTrue(attacker.contains("ATTACKER_RUSH_VOLUME = 1.20F"));
        assertTrue(attacker.contains("ATTACKER_SCREAM_VOLUME = 1.45F"));
        assertTrue(attacker.contains("if (!this.discovered)"));
        for (String file : List.of("attacker_scream_1.ogg", "attacker_scream_2.ogg")) {
            Path asset = Path.of(
                    "src", "main", "resources", "assets", "echoofthevoid", "sounds",
                    "uncanny", "attacker", file);
            assertTrue(Files.isRegularFile(asset), file);
            assertTrue(Files.size(asset) > 15_000L, file + " must contain an audible scream, not an empty cue");
        }
        assertTrue(sounds.contains("\"uncanny_terror_lock\""));
        assertNotNull(UncannyDevCatalog.byId("audio_mental_uncanny_terror_lock"));
    }

    @Test
    void everyDeclaredSoundSubtitleHasAnEnglishTranslation() throws IOException {
        String sounds = Files.readString(Path.of(
                "src", "main", "resources", "assets", "echoofthevoid", "sounds.json"), StandardCharsets.UTF_8);
        String english = Files.readString(Path.of(
                "src", "main", "resources", "assets", "echoofthevoid", "lang", "en_us.json"), StandardCharsets.UTF_8);
        var subtitles = Pattern.compile("\\\"subtitle\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(sounds);
        int declared = 0;
        while (subtitles.find()) {
            String key = subtitles.group(1);
            declared++;
            assertTrue(english.contains("\"" + key + "\""), () -> "Missing en_us subtitle: " + key);
        }
        assertTrue(declared > 0, "sounds.json should declare subtitles");
    }

    @Test
    void terrorUsesOneContinuousPrivateSoundForTheWholeCameraLock() throws IOException {
        String terror = read(JAVA_ROOT.resolve(Path.of("entity", "custom", "UncannyTerrorEntity.java")));
        Path asset = Path.of(
                "src", "main", "resources", "assets", "echoofthevoid", "sounds",
                "uncanny", "terror", "terror_lock_1.ogg");

        assertTrue(terror.contains("ENGAGED_DURATION_TICKS = 20 * 5"));
        assertTrue(terror.contains("UNCANNY_TERROR_LOCK"));
        assertTrue(terror.contains("1.10F, 1.0F, ENGAGED_DURATION_TICKS"));
        assertFalse(terror.contains("playProximitySound"));
        assertFalse(terror.contains("UNCANNY_TINNITUS"));
        assertTrue(Files.isRegularFile(asset));
        assertTrue(Files.size(asset) > 20_000L, "The continuous Terror asset must not be an empty cue");
    }

    @Test
    void mournerUsesADedicatedGroundedClientPose() throws IOException {
        String client = read(JAVA_ROOT.resolve(Path.of("EchoOfTheVoidClient.java")));
        String renderer = read(JAVA_ROOT.resolve(Path.of("client", "UncannyMournerRenderer.java")));
        String model = read(JAVA_ROOT.resolve(Path.of("client", "UncannyMournerModel.java")));
        String entity = read(JAVA_ROOT.resolve(Path.of("entity", "custom", "UncannyApprovedSpecialEntity.java")));

        assertTrue(client.contains(
                "UNCANNY_MOURNER.get(), UncannyMournerRenderer::new"));
        assertTrue(renderer.contains("new UncannyMournerModel("));
        assertTrue(model.contains("this.body.y = 11.0F"));
        assertTrue(model.contains("this.rightArm.y = 13.2F"));
        assertTrue(model.contains("this.leftArm.y = 13.2F"));
        assertTrue(model.contains("this.rightLeg.xRot = 1.47F"));
        assertTrue(model.contains("this.leftLeg.xRot = 1.47F"));
        assertTrue(model.contains("slowBreath"));
        assertTrue(model.contains("unevenSob"));
        assertTrue(model.contains("this.rightSleeve.copyFrom(this.rightArm)"));
        assertTrue(model.contains("this.leftPants.copyFrom(this.leftLeg)"));
        assertTrue(entity.contains("MOURNER_TEAR_INTERVAL_TICKS = 16"));
        assertTrue(entity.contains("ParticleTypes.FALLING_WATER"));
        assertTrue(entity.contains("spawnMournerTear(eyeCenter.add(right.scale(MOURNER_TEAR_EYE_OFFSET)))"));
        assertTrue(entity.contains("spawnMournerTear(eyeCenter.add(right.scale(-MOURNER_TEAR_EYE_OFFSET)))"));
        assertTrue(entity.contains("-0.012D"));
    }

    @Test
    void attackerAnimationFormsAreSyncedPersistentAndSeparatelyTestable() throws IOException {
        String client = read(JAVA_ROOT.resolve(Path.of("EchoOfTheVoidClient.java")));
        String renderer = read(JAVA_ROOT.resolve(Path.of("client", "UncannyAttackerRenderer.java")));
        String model = read(JAVA_ROOT.resolve(Path.of("client", "UncannyAttackerModel.java")));
        String entity = read(JAVA_ROOT.resolve(Path.of("entity", "custom", "UncannyStalkerEntity.java")));
        String executor = read(JAVA_ROOT.resolve(Path.of("dev", "UncannyDevActionExecutor.java")));

        assertTrue(client.contains("UNCANNY_STALKER.get(), UncannyAttackerRenderer::new"));
        assertTrue(renderer.contains("new UncannyAttackerModel("));
        assertTrue(model.contains("case CRAWL -> setupCrawl"));
        assertTrue(model.contains("case OUTSTRETCHED -> setupOutstretched"));
        assertTrue(model.contains("this.body.z = -3.2F"));
        assertTrue(model.contains("this.body.xRot = 1.50F"));
        assertTrue(model.contains("this.rightArm.xRot = -1.48F"));
        assertTrue(model.contains("this.rightLeg.z = 0.9F"));
        assertTrue(model.contains("this.leftLeg.z = 0.9F"));
        assertTrue(model.contains("irregularPulse"));
        assertTrue(entity.contains("EntityDataSerializers.BYTE"));
        assertTrue(entity.contains("tag.putByte(\"AttackerAnimationStyle\""));
        assertTrue(entity.contains("CRAWL(1)"));
        assertTrue(entity.contains("OUTSTRETCHED(2)"));
        assertTrue(entity.contains("AnimationStyle random(RandomSource random)"));
        assertFalse(entity.contains("STANDARD("));

        UncannyDevCatalog.Entry crawl = UncannyDevCatalog.byId("entity_attacker_crawl");
        UncannyDevCatalog.Entry outstretched = UncannyDevCatalog.byId("entity_attacker_outstretched");
        assertNotNull(crawl);
        assertNotNull(outstretched);
        assertEquals("attacker", crawl.groupKey());
        assertEquals("attacker_crawl", crawl.actionArg());
        assertEquals("attacker_outstretched", outstretched.actionArg());
        assertTrue(executor.contains("AnimationStyle.CRAWL"));
        assertTrue(executor.contains("AnimationStyle.OUTSTRETCHED"));
    }

    @Test
    void everyRegisteredSpecialSharesOneReducedCombatRewardContract() throws IOException {
        String bootstrap = read(JAVA_ROOT.resolve(Path.of("EchoOfTheVoid.java")));
        String registry = read(JAVA_ROOT.resolve(Path.of("entity", "UncannyEntityRegistry.java")));
        String rewards = read(JAVA_ROOT.resolve(Path.of(
                "event", "special", "UncannySpecialRewardSystem.java")));
        String utility = read(JAVA_ROOT.resolve(Path.of("entity", "UncannyEntityUtil.java")));

        assertTrue(bootstrap.contains("UncannySpecialRewardSystem::onLivingDrops"));
        assertTrue(rewards.contains("UncannyEntityRegistry.isSpecialEntity(entity.getType())"));
        assertTrue(rewards.contains("event.isRecentlyHit()"));
        assertTrue(rewards.contains("GameRules.RULE_DOMOBLOOT"));
        assertTrue(rewards.contains("UNCANNY_REALITY_SHARD.get()"));
        assertTrue(rewards.contains("UNCANNY_REALITY_SHARD_PIECE.get()"));
        assertFalse(utility.contains("dropPulseStyleRewards"));

        for (String holder : List.of(
                "UNCANNY_DOUBLE_DORMANT", "UNCANNY_WATCHER", "UNCANNY_STALKER",
                "UNCANNY_HURLER", "UNCANNY_SHADOW", "UNCANNY_KNOCKER",
                "UNCANNY_PULSE", "UNCANNY_TERROR", "UNCANNY_USHER",
                "UNCANNY_KEEPER", "UNCANNY_TENANT", "UNCANNY_FOLLOWER",
                "UNCANNY_SURVEYOR", "UNCANNY_MOURNER", "UNCANNY_DOUBLER",
                "UNCANNY_FERRYMAN", "UNCANNY_LISTENER", "UNCANNY_BYSTANDER")) {
            assertTrue(registry.substring(registry.indexOf("public static boolean isSpecialEntity"))
                    .contains(holder + ".get()"), holder);
        }

        for (String source : List.of(
                "UncannyPulseEntity.java", "UncannyKnockerEntity.java",
                "UncannyShadowEntity.java", "UncannyDoubleDormantEntity.java",
                "UncannyFollowerEntity.java", "UncannyKeeperEntity.java",
                "UncannyTenantEntity.java", "UncannyUsherEntity.java")) {
            assertFalse(read(JAVA_ROOT.resolve(Path.of("entity", "custom", source)))
                    .contains("dropCustomDeathLoot"), source);
        }
    }

    @Test
    void mournerDeathMemoryIsAdditiveBackwardCompatibleSavedData() throws IOException {
        String state = read(JAVA_ROOT.resolve(Path.of("state", "UncannyWorldState.java")));
        String controller = read(JAVA_ROOT.resolve(Path.of("event", "UncannyEventController.java")));

        assertTrue(state.contains("parent.put(\"deathSites\", list)"));
        assertTrue(state.contains("parent.getList(\"deathSites\", Tag.TAG_COMPOUND)"));
        assertTrue(state.contains("recordDeathSite(UUID playerId"));
        assertTrue(state.contains("hasMournerOccurred()"));
        assertTrue(state.contains("tag.putBoolean(\"mournerOccurred\", mournerOccurred)"));
        assertTrue(state.contains("markMournerUsed(UUID playerId)"));
        assertTrue(controller.contains("state.recordDeathSite("));
        assertTrue(state.contains("DATA_NAME = \"echoofthevoid_uncanny_world\""));
    }

    private static void assertDefinition(
            String id, int phase, int danger, int weight, ApprovedSpecialCatalog.Status status) {
        ApprovedSpecialCatalog.Definition definition = ApprovedSpecialCatalog.byId(id);
        assertNotNull(definition, id);
        assertEquals(phase, definition.minimumPhase(), id);
        assertEquals(danger, definition.danger(), id);
        assertEquals(weight, definition.weight(), id);
        assertEquals(status, definition.status(), id);
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
