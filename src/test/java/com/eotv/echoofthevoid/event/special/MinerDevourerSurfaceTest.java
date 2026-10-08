package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eotv.echoofthevoid.campaign.CampaignEventFamily;
import com.eotv.echoofthevoid.dev.UncannyDevCatalog;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventCatalog;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventIds;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaPacingRules;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MinerDevourerSurfaceTest {
    private static final Path ROOT = Path.of("src", "main");
    private static final Path JAVA = ROOT.resolve(Path.of("java", "com", "eotv", "echoofthevoid"));

    @Test
    void idsCatalogSchedulerCooldownAndQaRoutesAgree() {
        var miner = ParanoiaEventCatalog.require(ParanoiaEventIds.MINER);
        var devourer = ParanoiaEventCatalog.require(ParanoiaEventIds.DEVOURER);
        assertTrue(miner.minimumPhase() == 3 && miner.specialWeight() == 2);
        assertTrue(devourer.minimumPhase() == 4 && devourer.specialWeight() == 1);
        assertTrue(ParanoiaPacingRules.specialPerKeyCooldownTicks("miner", 3, 1, 2) == 2_400L * 20L);
        assertTrue(ParanoiaPacingRules.specialPerKeyCooldownTicks("devourer", 4, 1, 2) == 7_200L * 20L);
        assertTrue(CampaignEventFamily.forEvent("miner") == CampaignEventFamily.PRESENCE);
        assertTrue(CampaignEventFamily.forEvent("devourer") == CampaignEventFamily.PRESENCE);

        for (String id : List.of(
                "entity_miner_spawn", "entity_miner_emerged", "entity_devourer_spawn",
                "entity_devourer_arena", "tool_devourer_arena_abandon")) {
            assertNotNull(UncannyDevCatalog.byId(id), id);
        }
    }

    @Test
    void tunnelMutationsAreBoundedPersistentAndProtectionAware() throws IOException {
        String miner = read("entity/custom/UncannyMinerEntity.java");
        String planner = read("event/special/MinerTunnelPlanner.java");
        String policy = read("event/special/UncannyMinerBlockPolicy.java");
        String placeholder = read("block/entity/custom/UncannyRestorationPlaceholderBlockEntity.java");
        assertTrue(miner.contains("CommonHooks.canEntityDestroy"));
        assertTrue(miner.contains("RULE_MOBGRIEFING"));
        assertTrue(miner.contains("MinerRules.MAX_TUNNEL_SECTIONS"));
        assertTrue(miner.contains("SoundEvents.ITEM_PICKUP"));
        assertTrue(miner.contains("pendingPickupTick = now + 5L"));
        assertFalse(miner.contains("popResource("));
        assertFalse(miner.contains("setInvisible(true)"));
        assertFalse(miner.contains("setAnimationStyle("));
        assertFalse(miner.contains("MoverType.SELF"));
        assertFalse(miner.contains("setNoAi(true)"));
        assertTrue(miner.contains("getMoveControl().setWantedPosition"));
        assertTrue(miner.contains("getJumpControl().jump()"));
        assertTrue(miner.contains("confirmedTargetHits"));
        assertTrue(miner.contains("MinerCurrentSectionTargets"));
        assertTrue(miner.contains("MinerExitTunnelColumn"));
        assertTrue(miner.contains("MinerEmergencePositionReached"));
        assertTrue(miner.contains("MinerHuntPathValidationTicks"));
        assertTrue(miner.contains("ensureFinalEmergenceCorridor"));
        assertTrue(miner.contains("path == null || !path.canReach()"));
        assertTrue(miner.contains("advanceAlongRetainedPlan"));
        assertTrue(miner.contains("retained_emergence"));
        assertTrue(planner.contains("MinerRules.MAX_SEARCHED_NODES"));
        assertTrue(planner.contains("GhostMinerRules.isValidStartOffset"));
        assertTrue(planner.contains("distanceSquared >= 9.0D"));
        assertTrue(planner.contains("distanceSquared <= 25.0D"));
        assertTrue(planner.contains("findStaircaseEmergence"));
        assertTrue(planner.contains("replanAvoiding"));
        assertTrue(planner.contains("UNCANNY_RESTORATION_PLACEHOLDER"));
        assertTrue(planner.contains("hasOpenWalkingConnection"));
        assertTrue(policy.contains("classifyAscendingColumn"));
        assertTrue(policy.contains("classifyAscendingTransition"));
        assertTrue(policy.contains("fromBase.above(2)"));
        assertTrue(policy.contains("state.hasBlockEntity()"));
        assertTrue(policy.contains("state.getBlock() instanceof FallingBlock"));
        assertTrue(policy.contains("UNCANNY_ALTAR"));
        assertTrue(placeholder.contains("OriginalState"));
        assertTrue(placeholder.contains("isUnsafeToRestore"));
        assertTrue(placeholder.contains("RESTORATION_OWNER_CLEARANCE"));
        assertTrue(placeholder.contains("RESTORATION_ENTITY_MARGIN"));
        assertTrue(placeholder.contains("Block.UPDATE_ALL"));
    }

    @Test
    void minerRendererUsesTheNormalHumanoidCycleInsteadOfEitherAttackerPose() throws IOException {
        String renderer = read("client/UncannyMinerRenderer.java");
        assertTrue(renderer.contains("super(context);"));
        assertFalse(renderer.contains("UncannyAttackerModel"));
        assertFalse(renderer.contains("AnimationStyle"));
    }

    @Test
    void devourerRendersARealPortalAndArenaIsServerAuthoritative() throws IOException {
        String renderer = read("client/UncannyDevourerRenderer.java");
        String entity = read("entity/custom/UncannyDevourerEntity.java");
        String arena = read("event/special/DevourerArenaSystem.java");
        String pursuer = read("entity/custom/UncannyArenaPursuerEntity.java");
        String pursuerRenderer = read("client/UncannyArenaPursuerRenderer.java");
        // The End Portal shader reads green: the portal is the mod's own Elsewhere-fog texture.
        assertFalse(renderer.contains("RenderType.endPortal()"));
        assertTrue(renderer.contains("RenderType.energySwirl(PORTAL_TEXTURE"));
        assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                "src/main/resources/assets/echoofthevoid/textures/entity/devourer_portal.png")));
        assertTrue(renderer.contains("part.skipDraw = true"));
        assertFalse(renderer.toLowerCase().contains("rectangle"));
        assertTrue(entity.contains("CAPTURE_HORIZONTAL_REACH = 1.10D"));
        // The victim is seized for a visible moment before the trial begins.
        assertTrue(entity.contains("tickSeize(level)") && entity.contains("SEIZE_TICKS"));
        assertTrue(entity.contains("inflate(CAPTURE_HORIZONTAL_REACH, CAPTURE_VERTICAL_REACH"));
        assertTrue(entity.contains("MAX_LIFETIME_TICKS = 20 * 45"));
        assertTrue(entity.contains("UncannySoundRegistry.DEVOURER_RATTLE"));
        assertTrue(entity.contains("UncannySoundRegistry.DEVOURER_EMERGE"));
        assertTrue(entity.contains("broadcastEntityEvent(this, CAPTURE_EVENT)"));
        assertTrue(entity.contains("isEmerging(level)"));
        assertFalse(entity.contains("setBlock("));
        assertTrue(entity.contains("solo_capture_complete"));
        assertTrue(entity.contains("resolveReachableTarget"));
        assertTrue(entity.contains("isObservedByAnyPlayer"));
        assertTrue(entity.contains("DevourerArenaRules.canBeginOriginRetreat"));
        assertTrue(entity.contains("DevourerArenaRules.originForcedRetreatRequired"));
        assertTrue(entity.contains("DevourerForcedRetreatDeadline"));
        assertTrue(entity.contains("unobserved_sink"));
        assertFalse(entity.contains("visible_sink"));
        assertTrue(entity.contains("sinkDeadlineGameTime"));
        String spawner = read("event/special/UncannyDevourerSystem.java");
        assertTrue(spawner.contains("findReachableInColumn"));
        assertTrue(spawner.contains("createPath(target, 1)"));
        assertFalse(spawner.contains("createPath(target, 0)"));
        assertTrue(spawner.contains("devourer.setOnGround(true)"));
        assertTrue(spawner.contains("path != null && path.canReach()"));
        assertTrue(spawner.contains("for (int distance = 4; distance <= 13; distance++)"));
        assertFalse(spawner.contains("reason, \"path_not_reachable\""));
        assertTrue(arena.contains("state.addDevourerArenaSession(session)"));
        assertTrue(arena.indexOf("state.addDevourerArenaSession(session)")
                < arena.indexOf("player.teleportTo("));
        assertTrue(arena.contains("event.getEntity() instanceof UncannyArenaPursuerEntity"));
        assertTrue(arena.contains("event.getDrops().clear()"));
        assertTrue(arena.contains("FORBIDDEN_PLACEMENTS"));
        assertTrue(arena.contains("onFluidPlaced(BlockEvent.FluidPlaceBlockEvent"));
        assertTrue(arena.contains("onPortalSpawn(BlockEvent.PortalSpawnEvent"));
        assertTrue(arena.contains("item instanceof BucketItem"));
        assertTrue(arena.contains("item instanceof FlintAndSteelItem"));
        assertTrue(arena.contains("for (int attempt = 0; attempt < missing; attempt++)"));
        assertFalse(arena.contains("while (session.spawnedPursuers() < expected)"));
        assertTrue(pursuer.contains("extends Spider"));
        assertTrue(pursuer.contains("SCRATCH_TELEGRAPH_TICKS"));
        assertTrue(pursuer.contains("setSilent(true)"));
        assertFalse(pursuer.contains("setGlowingTag(true)"));
        assertTrue(pursuer.contains("WallClimberNavigation") && pursuer.contains("GroundPathNavigation"));
        assertFalse(pursuer.contains("SoundEvents.SPIDER"));
        assertFalse(pursuer.contains("teleportTo("));
        assertTrue(pursuerRenderer.contains("ModelLayers.ZOMBIE"));
        assertTrue(pursuerRenderer.contains("ModelLayers.SKELETON"));
        assertTrue(pursuerRenderer.contains("ModelLayers.VILLAGER"));
        assertTrue(pursuerRenderer.contains("ModelLayers.IRON_GOLEM"));
        assertTrue(pursuerRenderer.contains("SpiderModel"));
        assertTrue(pursuerRenderer.contains("ModelLayers.SHEEP_FUR"));
        assertTrue(pursuerRenderer.contains("ModelLayers.COW"));
        assertTrue(pursuerRenderer.contains("ModelLayers.PIG"));
        assertFalse(pursuerRenderer.contains("EyesLayer"));
    }

    @Test
    void elsewhereDataContainsNoNaturalGenerationOrPortalRoute() throws IOException {
        String eventFacade = read("event/UncannyParanoiaEventSystem.java");
        String dimension = Files.readString(
                ROOT.resolve(Path.of("resources", "data", "echoofthevoid", "dimension", "elsewhere.json")),
                StandardCharsets.UTF_8);
        String type = Files.readString(
                ROOT.resolve(Path.of("resources", "data", "echoofthevoid", "dimension_type", "elsewhere.json")),
                StandardCharsets.UTF_8);
        assertTrue(dimension.contains("\"minecraft:flat\""));
        assertTrue(dimension.contains("\"features\": false"));
        assertTrue(dimension.contains("\"structure_overrides\": []"));
        assertTrue(dimension.contains("\"echoofthevoid:uncanny_block\""));
        assertTrue(type.contains("\"fixed_time\": 18000"));
        assertTrue(type.contains("\"bed_works\": false"));
        assertTrue(type.contains("\"respawn_anchor_works\": false"));
        int arenaGuard = eventFacade.indexOf("if (UncannyDimensions.isElsewhere(player.level()))");
        int nextTickBody = eventFacade.indexOf("long now = server.getTickCount();", arenaGuard);
        String guardBody = eventFacade.substring(arenaGuard, nextTickBody);
        assertTrue(guardBody.contains("return;"));
        assertFalse(guardBody.contains("clearPlayerEventState(player)"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(JAVA.resolve(Path.of(relative)), StandardCharsets.UTF_8);
    }
    @Test
    void devourerModelAnimationsAndSoundsComeFromTheBlockbenchSource() throws IOException {
        String model = read("client/UncannyDevourerModel.java");
        String animations = read("client/UncannyDevourerAnimations.java");
        String sound = read("client/UncannyDevourerPortalSound.java");
        for (String name : java.util.List.of("IDLE", "WALK", "EMERGE", "SINK", "CAPTURE")) {
            assertTrue(animations.contains("AnimationDefinition " + name + " ="), name);
            assertTrue(model.contains("UncannyDevourerAnimations." + name), name);
        }
        assertTrue(model.contains("getChild(\"portal\")") && model.contains("getChild(\"face_rift\")"));
        // A black plate travels with the chest portal so it is never visible from behind.
        assertTrue(model.contains("portal.addOrReplaceChild(\"portal_backing\""));
        assertTrue(model.contains("Math.min(portal.xScale, MAX_PORTAL_X_SCALE)"));
        assertTrue(model.contains("smoothedMouthOpen"));
        assertTrue(sound.contains("this.looping = true") && sound.contains("SoundSource.HOSTILE"));
        assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of("Ressources", "Modeles", "devourer.bbmodel")),
                "The editable Blockbench source must stay next to the generated code");
    }
}
