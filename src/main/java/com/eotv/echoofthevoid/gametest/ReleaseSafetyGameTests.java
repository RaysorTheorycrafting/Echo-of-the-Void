package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import com.eotv.echoofthevoid.event.UncannyStructureFeatureSystem;
import com.eotv.echoofthevoid.event.UncannyParanoiaEventSystem;
import com.eotv.echoofthevoid.event.UncannyWeatherSystem;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyApprovedSpecialEntity;
import com.eotv.echoofthevoid.event.UncannyClientStateSync;
import com.eotv.echoofthevoid.event.UncannyEventController;
import com.eotv.echoofthevoid.event.special.ApprovedSpecialCatalog;
import com.eotv.echoofthevoid.event.weather.UncannyWeatherPacingRules;
import com.eotv.echoofthevoid.network.UncannyMentalSoundPayload;
import com.eotv.echoofthevoid.network.UncannyWeatherSyncPayload;
import com.eotv.echoofthevoid.sound.UncannySoundDelivery;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import com.eotv.echoofthevoid.world.UncannyBlockMutationSafety;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Broad headless checks for release-only invariants that need a bootstrapped Minecraft registry. */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class ReleaseSafetyGameTests {
    private static final String TEMPLATE = "special_test_room";

    private ReleaseSafetyGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 12)
    public static void everyRegisteredEntityCanBeCreatedTickedAndRemoved(GameTestHelper helper) {
        List<EntityType<?>> types = BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(type -> {
                    ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                    return id != null && EchoOfTheVoid.MODID.equals(id.getNamespace());
                })
                .sorted(Comparator.comparing(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()))
                .toList();
        helper.assertTrue(types.size() >= 30,
                "The release entity lifecycle audit unexpectedly found only " + types.size() + " types");

        List<UUID> created = new ArrayList<>();
        for (int index = 0; index < types.size(); index++) {
            EntityType<?> type = types.get(index);
            Entity entity = type.create(helper.getLevel());
            helper.assertTrue(entity != null,
                    "EntityType.create returned null for " + BuiltInRegistries.ENTITY_TYPE.getKey(type));
            entity.setPos(8.5D, 3.0D + (index % 3), 8.5D);
            entity.setNoGravity(true);
            entity.setSilent(true);
            if (entity instanceof Mob mob) {
                mob.setNoAi(true);
            }
            helper.assertTrue(helper.getLevel().addFreshEntity(entity),
                    "The entity could not enter a ServerLevel: " + BuiltInRegistries.ENTITY_TYPE.getKey(type));
            created.add(entity.getUUID());
        }

        helper.runAtTickTime(3, () -> {
            for (UUID id : created) {
                Entity entity = helper.getLevel().getEntity(id);
                if (entity != null) {
                    entity.discard();
                }
            }
        });
        helper.runAtTickTime(6, () -> {
            for (UUID id : created) {
                helper.assertTrue(helper.getLevel().getEntity(id) == null,
                        "A release-audited entity remained registered after discard: " + id);
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void blockMutationGuardProtectsAltarBlockEntitiesAndDoubleBlocks(GameTestHelper helper) {
        BlockPos chestPos = helper.absolutePos(new BlockPos(3, 1, 3));
        BlockPos doorPos = helper.absolutePos(new BlockPos(5, 1, 3));
        BlockPos altarPos = helper.absolutePos(new BlockPos(7, 1, 3));
        BlockPos altarPartPos = helper.absolutePos(new BlockPos(9, 1, 3));
        BlockPos stonePos = helper.absolutePos(new BlockPos(11, 1, 3));

        helper.getLevel().setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        BlockState door = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        helper.getLevel().setBlock(doorPos, door, 3);
        helper.getLevel().setBlock(altarPos, UncannyBlockRegistry.UNCANNY_ALTAR.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(
                altarPartPos,
                UncannyBlockRegistry.UNCANNY_ALTAR_PART.get().defaultBlockState(),
                3);
        helper.getLevel().setBlock(stonePos, Blocks.STONE.defaultBlockState(), 3);

        helper.assertTrue(UncannyBlockMutationSafety.isProtected(
                        helper.getLevel(), chestPos, helper.getLevel().getBlockState(chestPos)),
                "Block entities must be protected from single-block anomaly mutation");
        helper.assertTrue(UncannyBlockMutationSafety.isProtected(
                        helper.getLevel(), doorPos, helper.getLevel().getBlockState(doorPos)),
                "Both halves of a double block must be treated as a coupled structure");
        helper.assertTrue(UncannyBlockMutationSafety.isProtected(
                        helper.getLevel(), altarPos, helper.getLevel().getBlockState(altarPos)),
                "The Uncanny Altar center must be immutable to generic anomaly effects");
        helper.assertTrue(UncannyBlockMutationSafety.isProtected(
                        helper.getLevel(), altarPartPos, helper.getLevel().getBlockState(altarPartPos)),
                "Every Uncanny Altar part must be immutable to generic anomaly effects");
        helper.assertTrue(!UncannyBlockMutationSafety.isProtected(
                        helper.getLevel(), stonePos, helper.getLevel().getBlockState(stonePos)),
                "An ordinary standalone block must not be over-protected");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void everyWrongVillageHouseVariantGeneratesThroughTheProductionBuilder(GameTestHelper helper) {
        List<String> expected = List.of(
                "too_narrow",
                "too_tall",
                "too_wide",
                "long",
                "flat",
                "offset",
                "bent",
                "split",
                "gigantic",
                "tiny");
        helper.assertTrue(UncannyStructureFeatureSystem.wrongVillageHouseVariantIds().equals(expected),
                "The release catalog must expose all ten approved house variants in stable order");

        BlockPos origin = helper.absolutePos(new BlockPos(8, 1, 8));
        for (int dx = -32; dx <= 32; dx += 16) {
            for (int dz = -32; dz <= 32; dz += 16) {
                helper.getLevel().getChunkAt(origin.offset(dx, 0, dz));
            }
        }
        for (String variant : expected) {
            helper.assertTrue(
                    UncannyStructureFeatureSystem.generateWrongVillageHouseVariantForGameTest(
                            helper.getLevel(), origin, variant),
                    "The forced release generation route failed for wrong village house variant " + variant);
        }
        helper.assertTrue(!UncannyStructureFeatureSystem.generateWrongVillageHouseVariantForGameTest(
                        helper.getLevel(), origin, "missing_variant"),
                "An unknown house variant must never silently fall back to random generation");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 50)
    public static void repeatedWeatherSpecialAndEventStartsRemainBounded(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        player.setInvulnerable(true);
        List<UUID> spawnedSpecials = new ArrayList<>();
        BlockPos specialSpawn = helper.absolutePos(new BlockPos(8, 3, 8));

        int weatherStarts = 0;
        for (int cycle = 0; cycle < 2; cycle++) {
            for (UncannyWeatherPacingRules.Event event : UncannyWeatherPacingRules.Event.values()) {
                if (UncannyWeatherSystem.forceTrigger(player.getServer(), event.id)) {
                    weatherStarts++;
                }
                UncannyWeatherSystem.forceStop(player.getServer());
            }
        }
        helper.assertTrue(weatherStarts >= UncannyWeatherPacingRules.Event.values().length,
                "Repeated weather QA could not start one full catalog-equivalent cycle");

        for (int cycle = 0; cycle < 4; cycle++) {
            for (ApprovedSpecialCatalog.Definition definition : ApprovedSpecialCatalog.definitions()) {
                EntityType<UncannyApprovedSpecialEntity> type =
                        UncannyEntityRegistry.approvedSpecialById(definition.id());
                helper.assertTrue(type != null, "Missing registered Special type for " + definition.id());
                UncannyApprovedSpecialEntity entity = type.create(helper.getLevel());
                helper.assertTrue(entity != null, "Special creation failed during soak for " + definition.id());
                entity.moveTo(
                        specialSpawn.getX() + 0.5D + cycle * 0.1D,
                        specialSpawn.getY(),
                        specialSpawn.getZ() + 0.5D,
                        0.0F,
                        0.0F);
                entity.setup(player, player.blockPosition().offset(cycle, 0, 0));
                helper.assertTrue(helper.getLevel().addFreshEntity(entity),
                        "Special insertion failed during soak for " + definition.id());
                spawnedSpecials.add(entity.getUUID());
            }
        }

        helper.assertTrue(UncannyParanoiaEventSystem.triggerFlashError(player),
                "Flash Error must accept the first soak trigger");
        helper.assertTrue(UncannyParanoiaEventSystem.triggerFalseFall(player),
                "False Fall must accept the first soak trigger");
        helper.assertTrue(UncannyParanoiaEventSystem.triggerFalseInjury(player),
                "False Injury must accept the first soak trigger");
        UncannyParanoiaEventSystem.triggerFlashError(player);
        UncannyParanoiaEventSystem.triggerFalseFall(player);
        UncannyParanoiaEventSystem.triggerFalseInjury(player);

        helper.runAtTickTime(12, () -> {
            for (UUID id : spawnedSpecials) {
                Entity entity = helper.getLevel().getEntity(id);
                if (entity != null) {
                    entity.discard();
                }
            }
            UncannyWeatherSystem.forceStop(player.getServer());
        });
        helper.runAtTickTime(16, () -> {
            for (UUID id : spawnedSpecials) {
                helper.assertTrue(helper.getLevel().getEntity(id) == null,
                        "A Special leaked after the repeated-start soak cleanup: " + id);
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void twoPlayerDeliveryAndLifecycleRemainIsolated(GameTestHelper helper) {
        ServerPlayer first = helper.makeMockServerPlayerInLevel();
        ServerPlayer second = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(first.connection.getConnection());
        NetworkRegistry.configureMockConnection(second.connection.getConnection());
        drainOutbound(first);
        drainOutbound(second);
        UncannyClientStateSync.clearPlayerCache(first);
        UncannyClientStateSync.clearPlayerCache(second);

        UncannyClientStateSync.syncWeather(first, "rain_sobbing");
        UncannyClientStateSync.syncWeather(second, "rain_sobbing");
        helper.assertTrue(countPayload(first, UncannyWeatherSyncPayload.class) == 1,
                "The first player did not receive the shared weather state exactly once");
        helper.assertTrue(countPayload(second, UncannyWeatherSyncPayload.class) == 1,
                "The second player did not receive the shared weather state exactly once");

        UncannySoundDelivery.playMental(
                first,
                UncannySoundRegistry.UNCANNY_WHISPER.get(),
                SoundSource.AMBIENT,
                0.5F,
                1.0F,
                40);
        helper.assertTrue(countPayload(first, UncannyMentalSoundPayload.class) == 1,
                "The targeted player did not receive the private mental sound");
        helper.assertTrue(countPayload(second, UncannyMentalSoundPayload.class) == 0,
                "A private mental sound leaked to the other player");

        UncannyClientStateSync.syncWeather(first, "rain_sobbing");
        helper.assertTrue(countPayload(first, UncannyWeatherSyncPayload.class) == 0,
                "An unchanged state should be suppressed before reconnect");
        UncannyEventController.onPlayerLogout(new PlayerEvent.PlayerLoggedOutEvent(first));
        UncannyClientStateSync.syncWeather(first, "rain_sobbing");
        helper.assertTrue(countPayload(first, UncannyWeatherSyncPayload.class) == 1,
                "Logout cleanup did not permit a full state sync on reconnect");

        UncannyClientStateSync.syncWeather(second, "rain_sobbing");
        helper.assertTrue(countPayload(second, UncannyWeatherSyncPayload.class) == 0,
                "An unchanged state should be suppressed before dimension travel");
        UncannyEventController.onPlayerChangedDimension(new PlayerEvent.PlayerChangedDimensionEvent(
                second, Level.OVERWORLD, Level.NETHER));
        UncannyClientStateSync.clearWeather(second);
        helper.assertTrue(countPayload(second, UncannyWeatherSyncPayload.class) == 1,
                "Dimension cleanup did not send the Overworld weather clear state");
        helper.succeed();
    }

    private static void drainOutbound(ServerPlayer player) {
        EmbeddedChannel channel = (EmbeddedChannel) player.connection.getConnection().channel();
        while (channel.readOutbound() != null) {
            // The login packets are irrelevant to the delivery assertions below.
        }
    }

    private static int countPayload(ServerPlayer player, Class<? extends CustomPacketPayload> payloadClass) {
        EmbeddedChannel channel = (EmbeddedChannel) player.connection.getConnection().channel();
        int count = 0;
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof ClientboundCustomPayloadPacket packet
                    && payloadClass.isInstance(packet.payload())) {
                count++;
            }
        }
        return count;
    }
}
