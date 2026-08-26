package com.eotv.echoofthevoid.event.paranoia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eotv.echoofthevoid.dev.UncannyDevCatalog;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MinecraftNativeAnomalySurfaceTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid");

    @Test
    void everyValidatedNativeEventHasExactlyOneDevMenuTrigger() {
        Map<String, Long> triggerCounts = UncannyDevCatalog.entries().stream()
                .filter(entry -> entry.category() == UncannyDevCatalog.Category.EVENTS)
                .filter(entry -> entry.actionKind() == UncannyDevCatalog.ActionKind.TRIGGER_EVENT)
                .map(UncannyDevCatalog.Entry::actionArg)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        for (String eventId : ParanoiaEventCatalog.validatedNativeEventIds()) {
            assertEquals(1L, triggerCounts.getOrDefault(eventId, 0L), eventId);
        }
    }

    @Test
    void validatedNativeEventsOnlyUseExistingSchedulerLanesAndPositiveCooldowns() {
        for (String eventId : ParanoiaEventCatalog.validatedNativeEventIds()) {
            ParanoiaEventDescriptor descriptor = ParanoiaEventCatalog.require(eventId);
            assertTrue(descriptor.lanes().contains(ParanoiaEventLane.PRIMARY)
                    || descriptor.lanes().contains(ParanoiaEventLane.AMBIENT), eventId);
            assertTrue(descriptor.primaryWeight() > 0 || descriptor.ambientWeight() > 0, eventId);
            assertTrue(descriptor.eventCooldownSeconds() > 0 || descriptor.ambientCooldownSeconds() > 0, eventId);
        }
    }

    @Test
    void secondNativeBatchUsesSharedIllusionsWithoutAuthoritativeMutations() throws IOException {
        String runtime = read(JAVA_ROOT.resolve(Path.of(
                "event", "paranoia", "nativeevent", "MinecraftNativeAnomalySystem.java")));
        String network = read(JAVA_ROOT.resolve(Path.of("network", "UncannyNetwork.java")));

        for (String id : new String[]{
                "EMPTY_LEAD", "BORROWED_PAINTING", "RETURNED_DROP",
                "MISDIRECTED_ENCHANTMENT", "CAULDRON_ECHO", "EMPTY_WAKE"}) {
            assertTrue(runtime.contains("case " + id + " ->"), id);
        }
        assertTrue(runtime.contains("new UncannyEmptyLeadPayload("));
        assertTrue(runtime.contains("new UncannyPaintingVariantPayload("));
        assertTrue(runtime.contains("new UncannyReturnedItemPayload("));
        assertTrue(runtime.contains("ParticleTypes.ENCHANT"));
        assertTrue(runtime.contains("ParticleTypes.SPLASH"));
        assertTrue(runtime.contains("case EMPTY_LEAD -> triggerEmptyLead(player, debugImmediate)"));
        assertTrue(runtime.contains("(!debugImmediate"));
        assertTrue(runtime.contains("isPointObservedByAny(level, Vec3.atCenterOf(pos)"));
        assertTrue(runtime.contains("EMPTY_WAKE_SPLASH_PARTICLES = 14"));
        assertTrue(runtime.contains("EMPTY_WAKE_BUBBLE_PARTICLES = 8"));
        assertTrue(runtime.contains("ParticleTypes.BUBBLE_POP"));
        assertTrue(runtime.contains("sendBlockStateToObservers"));
        assertFalse(runtime.contains("painting.setVariant("));
        assertFalse(runtime.contains("LayeredCauldronBlock.lowerFillLevel("));

        assertTrue(network.contains("UncannyEmptyLeadPayload.TYPE"));
        assertTrue(network.contains("UncannyPaintingVariantPayload.TYPE"));
        assertTrue(network.contains("UncannyReturnedItemPayload.TYPE"));
    }

    @Test
    void leadAndWakeUseLongBoundedPresentationPaths() throws IOException {
        String runtime = read(JAVA_ROOT.resolve(Path.of(
                "event", "paranoia", "nativeevent", "MinecraftNativeAnomalySystem.java")));

        assertTrue(runtime.contains("EMPTY_LEAD_MIN_VISIBLE_TICKS"));
        assertTrue(runtime.contains("emptyLeadDurationTicks("));
        assertTrue(runtime.contains("EMPTY_WAKE_MIN_POINTS"));
        assertTrue(runtime.contains("EMPTY_WAKE_PULSE_INTERVAL_TICKS"));
        assertTrue(runtime.contains("findWaterSurfacePathTowardPlayer"));
        assertTrue(runtime.contains("ArrayDeque<BlockPos>"));
        assertTrue(runtime.contains("parent.put(next, current)"));
        assertTrue(runtime.contains("level.getBlockState(pos).is(Blocks.WATER)"));
        assertTrue(runtime.contains("level.getBlockState(pos.above()).isAir()"));
        assertFalse(runtime.contains("int length = 4 + level.random.nextInt(5)"));
    }

    @Test
    void nativeVisualStateHasDisconnectDimensionAndDisableCleanup() throws IOException {
        String runtime = read(JAVA_ROOT.resolve(Path.of(
                "event", "paranoia", "nativeevent", "MinecraftNativeAnomalySystem.java")));
        assertTrue(runtime.contains("cleanupOwnerTasks(player.getServer(), ownerId)"));
        assertTrue(runtime.contains("cleanupOwnerTasks(player.getServer(), player.getUUID())"));
        assertTrue(runtime.contains("clearRuntimeState(server)"));
        assertTrue(runtime.contains("restorePainting(server, task)"));
        assertTrue(runtime.contains("restoreCauldronEcho(server"));
        assertTrue(runtime.contains("ItemStack.EMPTY"));
    }

    @Test
    void railAndSignalAnomaliesRemainLoadedSharedAndPresentationOnly() throws IOException {
        String source = read(JAVA_ROOT.resolve(Path.of(
                "event", "paranoia", "nativeevent", "RailAndSignalAnomalySystem.java")));
        assertTrue(source.contains("level.hasChunkAt"));
        assertTrue(source.contains("AbstractMinecart.class"));
        assertTrue(source.contains("SoundEvents.MINECART_RIDING"));
        assertTrue(source.contains("getBestNeighborSignal(pos)"));
        assertTrue(source.contains("ClientboundBlockUpdatePacket"));
        assertTrue(source.contains("restoreBlock(server"));
        assertFalse(source.contains("level.setBlock("));
        assertFalse(source.contains("level.setBlockAndUpdate("));
    }

    @Test
    void mapIntruderIsPrivateTemporaryAndNeverWritesServerMapData() throws IOException {
        String runtime = read(JAVA_ROOT.resolve(Path.of(
                "event", "paranoia", "nativeevent", "MinecraftNativeAnomalySystem.java")));
        String client = read(JAVA_ROOT.resolve(Path.of(
                "client", "UncannyNativeAnomalyClientEffects.java")));
        String network = read(JAVA_ROOT.resolve(Path.of("network", "UncannyNetwork.java")));

        String trigger = runtime.substring(
                runtime.indexOf("public static boolean triggerMapIntruder"),
                runtime.indexOf("private static void tickGhostBreaking"));
        assertTrue(trigger.contains("PacketDistributor.sendToPlayer(player"));
        assertTrue(trigger.contains("data.colors"));
        assertFalse(trigger.contains("data.addDecoration"));
        assertFalse(trigger.contains("data.setDirty"));
        assertTrue(client.contains("MAP_INTRUDER_DECORATION_ID"));
        assertTrue(client.contains("removeMapIntruderDecoration(level)"));
        assertTrue(client.contains("!isHoldingMap"));
        assertTrue(network.contains("UncannyMapIntruderPayload.TYPE"));
    }

    @Test
    void fluidSculkAndLidBatchNeverMutatesAuthoritativeBlocksOrSignals() throws IOException {
        String source = read(JAVA_ROOT.resolve(Path.of(
                "event", "paranoia", "nativeevent", "ContextualWorldAnomalySystem.java")));
        assertTrue(source.contains("ParticleTypes.BUBBLE_COLUMN_UP"));
        assertTrue(source.contains("ParticleTypes.CURRENT_DOWN"));
        assertTrue(source.contains("VibrationParticleOption"));
        assertTrue(source.contains("ClientboundBlockEventPacket"));
        assertTrue(source.contains("ClientboundBlockUpdatePacket"));
        assertTrue(source.contains("SoundEvents.STRIDER_STEP_LAVA"));
        assertTrue(source.contains("level.hasChunkAt"));
        assertFalse(source.contains("level.setBlock("));
        assertFalse(source.contains("level.setBlockAndUpdate("));
        assertFalse(source.contains("level.gameEvent("));
    }

    @Test
    void objectPresentationBatchIsSharedBoundedAndNeverMutatesAuthoritativeObjects() throws IOException {
        String source = read(JAVA_ROOT.resolve(Path.of(
                "event", "paranoia", "nativeevent", "ObjectPresentationAnomalySystem.java")));
        String client = read(JAVA_ROOT.resolve(Path.of(
                "client", "UncannyNativeAnomalyClientEffects.java")));
        String network = read(JAVA_ROOT.resolve(Path.of("network", "UncannyNetwork.java")));

        assertTrue(source.contains("arrow.tickCount >= 100"));
        assertTrue(source.contains("Monster.class"));
        assertTrue(source.contains("distanceToSqr(arrow.position()) <= 2.0D * 2.0D"));
        assertTrue(source.contains("ClientboundBlockUpdatePacket"));
        assertTrue(source.contains("observerIds(level"));
        assertTrue(source.contains("level.hasChunkAt"));
        assertTrue(source.contains("Blocks.AIR.defaultBlockState()"));
        assertFalse(source.contains("level.setBlock("));
        assertFalse(source.contains("level.setBlockAndUpdate("));
        assertFalse(source.contains("FallingBlockEntity"));

        assertTrue(client.contains("ARROW_GAZES"));
        assertTrue(client.contains("SUSPENDED_FALLS"));
        assertTrue(client.contains("getBlockRenderer().renderSingleBlock"));
        assertTrue(network.contains("UncannyArrowGazePayload.TYPE"));
        assertTrue(network.contains("UncannySuspendedFallPayload.TYPE"));
    }

    @Test
    void beaconFragmentUsesTheVanillaBeamAndPersistsItsNaturalOneShotGuard() throws IOException {
        String source = read(JAVA_ROOT.resolve(Path.of(
                "event", "paranoia", "nativeevent", "ObjectPresentationAnomalySystem.java")));
        String client = read(JAVA_ROOT.resolve(Path.of(
                "client", "UncannyNativeAnomalyClientEffects.java")));
        String state = read(JAVA_ROOT.resolve(Path.of("state", "UncannyWorldState.java")));

        assertTrue(source.contains("hasBeaconFragmentOccurred()"));
        assertTrue(source.contains("markBeaconFragmentOccurred()"));
        assertTrue(source.contains("!debugImmediate"));
        assertTrue(source.contains("containsBeaconNearby"));
        assertTrue(source.contains("level.hasChunkAt"));
        assertTrue(source.contains("isClearlyObservedByAny"));
        assertFalse(source.contains("level.setBlock("));
        assertTrue(client.contains("BeaconRenderer.renderBeaconBeam("));
        assertTrue(state.contains("tag.putBoolean(\"beaconFragmentOccurred\""));
        assertTrue(state.contains("tag.getBoolean(\"beaconFragmentOccurred\""));
    }

    @Test
    void strayExperienceUsesRecentCombatButCannotGrantOrRepairExperience() throws IOException {
        String source = read(JAVA_ROOT.resolve(Path.of(
                "event", "paranoia", "nativeevent", "ObjectPresentationAnomalySystem.java")));
        String client = read(JAVA_ROOT.resolve(Path.of(
                "client", "UncannyNativeAnomalyClientEffects.java")));

        assertTrue(source.contains("LivingDeathEvent"));
        assertTrue(source.contains("event.getEntity() instanceof Monster"));
        assertTrue(source.contains("event.getSource().getEntity() instanceof ServerPlayer"));
        assertTrue(source.contains("RECENT_COMBAT_ENDS"));
        assertTrue(source.contains("ExperienceOrb.class"));
        assertTrue(source.contains("UncannyStrayExperiencePayload"));
        assertFalse(source.contains("ExperienceOrb.award("));
        assertFalse(source.contains("addFreshEntity(new ExperienceOrb"));
        assertTrue(client.contains("new ExperienceOrb(level"));
        assertTrue(client.contains("getEntityRenderDispatcher().render("));
        assertFalse(client.contains("level.addEntity"));
        assertFalse(client.contains("level.addFreshEntity"));
    }

    @Test
    void extraHerdAnimalIsASharedRendererOnlyCopyWithStrictAnimalExclusions() throws IOException {
        String source = read(JAVA_ROOT.resolve(Path.of(
                "event", "paranoia", "nativeevent", "ObjectPresentationAnomalySystem.java")));
        String client = read(JAVA_ROOT.resolve(Path.of(
                "client", "UncannyNativeAnomalyClientEffects.java")));

        assertTrue(source.contains("type == EntityType.COW"));
        assertTrue(source.contains("type == EntityType.PIG"));
        assertTrue(source.contains("type == EntityType.SHEEP"));
        assertTrue(source.contains("type == EntityType.CHICKEN"));
        assertTrue(source.contains("!animal.isBaby()"));
        assertTrue(source.contains("!animal.hasCustomName()"));
        assertTrue(source.contains("!animal.isInLove()"));
        assertTrue(source.contains("!animal.isLeashed()"));
        assertTrue(source.contains("together < 3"));
        assertTrue(client.contains("EntityType.byString"));
        assertTrue(client.contains("animal.setBaby(false)"));
        assertTrue(client.contains("getEntityRenderDispatcher().render("));
        assertFalse(client.contains("level.addEntity"));
        assertFalse(source.contains("level.addFreshEntity"));
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
