package com.eotv.echoofthevoid.event.paranoia.nativeevent;

import static com.eotv.echoofthevoid.event.paranoia.ParanoiaEventIds.*;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventCatalog;
import com.eotv.echoofthevoid.network.UncannyArmorStandPosePayload;
import com.eotv.echoofthevoid.network.UncannyEmptyLeadPayload;
import com.eotv.echoofthevoid.network.UncannyFishingTugPayload;
import com.eotv.echoofthevoid.network.UncannyMapIntruderPayload;
import com.eotv.echoofthevoid.network.UncannyOrphanShadowPayload;
import com.eotv.echoofthevoid.network.UncannyPaintingVariantPayload;
import com.eotv.echoofthevoid.network.UncannyReturnedItemPayload;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Runtime for subtle anomalies built only from Minecraft-native visual, block and entity grammar.
 *
 * <p>All important visuals are sent to nearby observers as one shared occurrence. Client-only
 * state is used solely where it protects authoritative gameplay data (block contents, armor stand
 * poses and fishing loot). Every such illusion has an explicit server-driven restoration.</p>
 */
public final class MinecraftNativeAnomalySystem {
    private static final int OBSERVER_RADIUS = 32;
    private static final int EMPTY_WAKE_SPLASH_PARTICLES = 14;
    private static final int EMPTY_WAKE_BUBBLE_PARTICLES = 8;
    private static final long LEAF_CONTEXT_MAX_AGE_TICKS = 45L * 20L;
    private static final long PICKUP_CONTEXT_MAX_AGE_TICKS = 3L * 60L * 20L;
    private static final List<GhostBreakingTask> GHOST_BREAKING_TASKS = new ArrayList<>();
    private static final List<OrphanShadowTask> ORPHAN_SHADOW_TASKS = new ArrayList<>();
    private static final List<ColdFurnaceTask> COLD_FURNACE_TASKS = new ArrayList<>();
    private static final List<ArmorPoseTask> ARMOR_POSE_TASKS = new ArrayList<>();
    private static final List<LeafReplyTask> LEAF_REPLY_TASKS = new ArrayList<>();
    private static final List<SilentBellTask> SILENT_BELL_TASKS = new ArrayList<>();
    private static final List<AnimalAttentionTask> ANIMAL_ATTENTION_TASKS = new ArrayList<>();
    private static final List<VillagerMeetingTask> VILLAGER_MEETING_TASKS = new ArrayList<>();
    private static final List<EmptyLeadTask> EMPTY_LEAD_TASKS = new ArrayList<>();
    private static final List<PaintingVariantTask> PAINTING_VARIANT_TASKS = new ArrayList<>();
    private static final List<ReturnedDropTask> RETURNED_DROP_TASKS = new ArrayList<>();
    private static final List<MisdirectedEnchantmentTask> MISDIRECTED_ENCHANTMENT_TASKS = new ArrayList<>();
    private static final List<CauldronEchoTask> CAULDRON_ECHO_TASKS = new ArrayList<>();
    private static final List<EmptyWakeTask> EMPTY_WAKE_TASKS = new ArrayList<>();
    private static final Map<UUID, LeafContext> LAST_LEAF_CONTEXTS = new HashMap<>();
    private static final Map<UUID, PickupContext> LAST_PICKUP_CONTEXTS = new HashMap<>();
    private static final Map<UUID, Long> ANIMAL_REUSE_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> ARMOR_STAND_REUSE_COOLDOWNS = new HashMap<>();
    private static MinecraftServer trackedServer;
    private static int nextVisualId = 1_500_000_000;

    private MinecraftNativeAnomalySystem() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }
        if (trackedServer != server) {
            clearRuntimeState(null);
            trackedServer = server;
        }
        long now = server.getTickCount();
        if (!UncannyWorldState.get(server).isSystemEnabled()) {
            clearRuntimeState(server);
            return;
        }

        tickGhostBreaking(server, now);
        tickOrphanShadows(server, now);
        tickColdFurnaces(server, now);
        tickArmorPoses(server, now);
        tickLeafReplies(server, now);
        tickSilentBells(server, now);
        tickAnimalAttention(server, now);
        tickVillagerMeetings(server, now);
        tickEmptyLeads(server, now);
        tickPaintingVariants(server, now);
        tickReturnedDrops(server, now);
        tickMisdirectedEnchantments(server, now);
        tickCauldronEchoes(server, now);
        tickEmptyWakes(server, now);
        RailAndSignalAnomalySystem.tick(server, now);
        ContextualWorldAnomalySystem.tick(server, now);
        ObjectPresentationAnomalySystem.tick(server, now);
        LAST_LEAF_CONTEXTS.entrySet().removeIf(entry -> now - entry.getValue().tick() > LEAF_CONTEXT_MAX_AGE_TICKS);
        LAST_PICKUP_CONTEXTS.entrySet().removeIf(entry -> now - entry.getValue().tick() > PICKUP_CONTEXT_MAX_AGE_TICKS);
        ANIMAL_REUSE_COOLDOWNS.entrySet().removeIf(entry -> now >= entry.getValue());
        ARMOR_STAND_REUSE_COOLDOWNS.entrySet().removeIf(entry -> now >= entry.getValue());
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)
                || player.getServer() == null) {
            return;
        }
        BlockState state = event.getState();
        if (state.is(BlockTags.LEAVES)) {
            LAST_LEAF_CONTEXTS.put(player.getUUID(), new LeafContext(
                    player.serverLevel().dimension(),
                    event.getPos().immutable(),
                    player.getServer().getTickCount()));
        }
    }

    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        RailAndSignalAnomalySystem.onNeighborNotify(event);
    }

    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        ItemStack original = event.getOriginalStack();
        ItemEntity itemEntity = event.getItemEntity();
        if (!isOrdinaryReturnedDropSource(original)) {
            return;
        }
        ItemStack visual = new ItemStack(original.getItem());
        LAST_PICKUP_CONTEXTS.put(player.getUUID(), new PickupContext(
                player.serverLevel().dimension(), itemEntity.position(), visual,
                player.getServer().getTickCount()));
    }

    /**
     * Returned Drop is a memory of an ordinary placed material, never a copy of valuable or
     * player-authored item data. Count is deliberately irrelevant because the visual is always one
     * freshly-created item type.
     */
    private static boolean isOrdinaryReturnedDropSource(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)
                || !stack.isComponentsPatchEmpty()
                || stack.getRarity() != net.minecraft.world.item.Rarity.COMMON) {
            return false;
        }
        return !blockItem.getBlock().defaultBlockState().hasBlockEntity();
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof AbstractFurnaceBlock) {
            cancelColdFurnace(level, pos);
        }
        if (state.getBlock() instanceof BellBlock) {
            SILENT_BELL_TASKS.removeIf(task -> task.dimension().equals(level.dimension()) && task.bellPos().equals(pos));
            VILLAGER_MEETING_TASKS.removeIf(task -> task.dimension().equals(level.dimension())
                    && task.target().distanceToSqr(Vec3.atCenterOf(pos)) <= 48.0D * 48.0D);
        }
        if (state.getBlock() instanceof LayeredCauldronBlock) {
            cancelCauldronEcho(level, pos);
        }
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        restoreArmorPoseForInteraction(event.getEntity(), event.getTarget());
        restorePaintingForInteraction(event.getEntity(), event.getTarget());
    }

    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        restoreArmorPoseForInteraction(event.getEntity(), event.getTarget());
        restorePaintingForInteraction(event.getEntity(), event.getTarget());
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        UUID ownerId = player.getUUID();
        LAST_LEAF_CONTEXTS.remove(ownerId);
        LAST_PICKUP_CONTEXTS.remove(ownerId);
        cleanupOwnerTasks(player.getServer(), ownerId);
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LAST_LEAF_CONTEXTS.remove(player.getUUID());
        LAST_PICKUP_CONTEXTS.remove(player.getUUID());
        cleanupOwnerTasks(player.getServer(), player.getUUID());
    }

    public static boolean trigger(ServerPlayer player, String eventId) {
        return trigger(player, eventId, false);
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        ObjectPresentationAnomalySystem.onLivingDeath(event);
    }

    /** Debug surfaces use immediate contextual delays but the exact same effect implementation. */
    public static boolean triggerForDebug(ServerPlayer player, String eventId) {
        return trigger(player, eventId, true);
    }

    private static boolean trigger(ServerPlayer player, String eventId, boolean debugImmediate) {
        if (!canTrigger(player, eventId)) {
            return false;
        }
        return switch (eventId) {
            case ORPHAN_SHADOW -> triggerOrphanShadow(player);
            case GHOST_BREAKING -> triggerGhostBreaking(player);
            case COLD_FURNACE -> triggerColdFurnace(player);
            case EMPTY_TELEPORT -> triggerEmptyTeleport(player);
            case FALSE_ANIMAL_HURT -> triggerFalseAnimalHurt(player);
            case STOLEN_POSE -> triggerStolenPose(player);
            case FISHING_TUG -> triggerFishingTug(player);
            case LEAF_REPLY -> triggerLeafReply(player, debugImmediate);
            case SILENT_BELL -> triggerSilentBell(player, debugImmediate);
            case EMPTY_CONGREGATION -> triggerEmptyCongregation(player);
            case EMPTY_LEAD -> triggerEmptyLead(player, debugImmediate);
            case BORROWED_PAINTING -> triggerBorrowedPainting(player);
            case RETURNED_DROP -> triggerReturnedDrop(player);
            case GHOST_CART -> RailAndSignalAnomalySystem.triggerGhostCart(player);
            case MISDIRECTED_ENCHANTMENT -> triggerMisdirectedEnchantment(player);
            case ORPHAN_SIGNAL -> RailAndSignalAnomalySystem.triggerOrphanSignal(player);
            case CAULDRON_ECHO -> triggerCauldronEcho(player);
            case MAP_INTRUDER -> triggerMapIntruder(player);
            case EMPTY_WAKE -> triggerEmptyWake(player);
            case COUNTERCURRENT_COLUMN -> ContextualWorldAnomalySystem.triggerCountercurrentColumn(player);
            case FALSE_SCULK_VIBRATION -> ContextualWorldAnomalySystem.triggerFalseSculkVibration(player);
            case LAVA_WAKE -> ContextualWorldAnomalySystem.triggerLavaWake(player);
            case FALSE_LID -> ContextualWorldAnomalySystem.triggerFalseLid(player);
            case WATCHING_ARROW -> ObjectPresentationAnomalySystem.triggerWatchingArrow(player);
            case SUSPENDED_FALL -> ObjectPresentationAnomalySystem.triggerSuspendedFall(player);
            case BEACON_FRAGMENT -> ObjectPresentationAnomalySystem.triggerBeaconFragment(player, debugImmediate);
            case STRAY_EXPERIENCE -> ObjectPresentationAnomalySystem.triggerStrayExperience(player, debugImmediate);
            case EXTRA_IN_THE_HERD -> ObjectPresentationAnomalySystem.triggerExtraHerdAnimal(player);
            default -> false;
        };
    }

    public static boolean triggerOrphanShadow(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos feet = findEdgeGroundPosition(level, player, 5, 11, true);
        if (feet == null) {
            return false;
        }
        Vec3 point = new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
        Set<UUID> observers = observerIds(level, point, OBSERVER_RADIUS);
        if (observers.isEmpty()) {
            return false;
        }
        int shadowId = nextVisualId();
        int durationTicks = 80 + level.random.nextInt(81);
        float radius = 0.43F + level.random.nextFloat() * 0.22F;
        sendToObservers(player.getServer(), observers, new UncannyOrphanShadowPayload(
                shadowId, true, point.x, point.y, point.z, radius, durationTicks));
        ORPHAN_SHADOW_TASKS.add(new OrphanShadowTask(
                player.getUUID(), level.dimension(), shadowId, point, radius,
                player.getServer().getTickCount() + durationTicks, observers));
        debug("orphan_shadow shared={} pos={} duration={}t", observers.size(), feet, durationTicks);
        return true;
    }

    public static boolean triggerGhostBreaking(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = findGhostBreakingBlock(level, player);
        if (pos == null) {
            return false;
        }
        long now = player.getServer().getTickCount();
        int breakerId = nextVisualId();
        int[][] variants = {
                {0, 1, 2, 3, 2, -1},
                {0, 1, 1, 2, -1},
                {0, 2, 3, 3, 1, -1}
        };
        int[] stages = variants[level.random.nextInt(variants.length)];
        level.destroyBlockProgress(breakerId, pos, stages[0]);
        playSharedBlockHit(level, pos, level.getBlockState(pos), 0.24F);
        GHOST_BREAKING_TASKS.add(new GhostBreakingTask(
                player.getUUID(), level.dimension(), breakerId, pos.immutable(), stages,
                1, now + 7L + level.random.nextInt(8)));
        return true;
    }

    public static boolean triggerColdFurnace(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = findNearbyBlock(level, player.blockPosition(), 14, state ->
                state.getBlock() instanceof AbstractFurnaceBlock
                        && state.hasProperty(BlockStateProperties.LIT)
                        && !state.getValue(BlockStateProperties.LIT));
        if (pos == null
                || isPointObservedByAny(level, Vec3.atCenterOf(pos), 24.0D, 0.90D)
                || COLD_FURNACE_TASKS.stream().anyMatch(task ->
                task.dimension().equals(level.dimension()) && task.pos().equals(pos))) {
            return false;
        }
        BlockState realState = level.getBlockState(pos);
        BlockState fakeState = realState.setValue(BlockStateProperties.LIT, true);
        Set<UUID> observers = observerIds(level, Vec3.atCenterOf(pos), 24);
        if (observers.isEmpty()) {
            return false;
        }
        sendBlockStateToObservers(player.getServer(), observers, pos, fakeState);
        long now = player.getServer().getTickCount();
        long endTick = now + 25L + level.random.nextInt(21);
        long cueTick = level.random.nextFloat() < 0.45F ? now + 4L + level.random.nextInt(14) : Long.MAX_VALUE;
        COLD_FURNACE_TASKS.add(new ColdFurnaceTask(
                player.getUUID(), level.dimension(), pos.immutable(), endTick, cueTick, observers, false));
        return true;
    }

    public static boolean triggerEmptyTeleport(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos first = findEdgeGroundPosition(level, player, 4, 10, false);
        if (first == null) {
            return false;
        }
        int variant = level.random.nextInt(4);
        spawnSparsePortalParticles(level, first, 3 + level.random.nextInt(7));
        if (variant == 2 || variant == 3) {
            BlockPos second = findEdgeGroundPosition(level, player, 7, 14, false);
            if (second != null && !second.closerThan(first, 4.0D)) {
                spawnSparsePortalParticles(level, second, 3 + level.random.nextInt(5));
            }
        }
        if (variant == 3 || level.random.nextFloat() < 0.16F) {
            level.playSound(null, first, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE,
                    0.18F, 0.88F + level.random.nextFloat() * 0.18F);
        }
        return true;
    }

    public static boolean triggerFalseAnimalHurt(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = player.getServer().getTickCount();
        List<Animal> candidates = level.getEntitiesOfClass(
                Animal.class,
                player.getBoundingBox().inflate(14.0D, 6.0D, 14.0D),
                animal -> animal.isAlive()
                        && !animal.isBaby()
                        && !animal.isInLove()
                        && (!(animal instanceof TamableAnimal tamable) || !tamable.isTame())
                        && (!(animal instanceof AbstractHorse horse) || !horse.isTamed())
                        && animal.getHealth() >= animal.getMaxHealth() * 0.75F
                        && animal.getLastHurtByMob() == null
                        && !ANIMAL_REUSE_COOLDOWNS.containsKey(animal.getUUID()));
        if (candidates.isEmpty() || !level.getEntitiesOfClass(
                Monster.class, player.getBoundingBox().inflate(18.0D), Entity::isAlive).isEmpty()) {
            return false;
        }
        Animal animal = candidates.get(level.random.nextInt(candidates.size()));
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        Vec3 emptyPoint = animal.position().add(Math.cos(angle) * 3.0D, 0.7D, Math.sin(angle) * 3.0D);
        Set<UUID> observers = observerIds(level, animal.position(), OBSERVER_RADIUS);
        for (UUID observerId : observers) {
            ServerPlayer observer = player.getServer().getPlayerList().getPlayer(observerId);
            if (observer != null) {
                observer.connection.send(new ClientboundHurtAnimationPacket(animal));
            }
        }
        if (level.random.nextFloat() < 0.28F) {
            level.playSound(null, BlockPos.containing(emptyPoint), SoundEvents.GRASS_STEP,
                    SoundSource.NEUTRAL, 0.20F, 0.8F + level.random.nextFloat() * 0.25F);
        }
        ANIMAL_ATTENTION_TASKS.add(new AnimalAttentionTask(
                player.getUUID(), level.dimension(), animal.getUUID(), emptyPoint, now + 24L));
        ANIMAL_REUSE_COOLDOWNS.put(animal.getUUID(), now + 30L * 60L * 20L);
        return true;
    }

    public static boolean triggerStolenPose(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = player.getServer().getTickCount();
        List<ArmorStand> candidates = level.getEntitiesOfClass(
                ArmorStand.class,
                player.getBoundingBox().inflate(16.0D, 8.0D, 16.0D),
                stand -> stand.isAlive()
                        && !stand.isInvisible()
                        && !stand.isMarker()
                        && !stand.isNoGravity()
                        && !ARMOR_STAND_REUSE_COOLDOWNS.containsKey(stand.getUUID())
                        && !isEntityObservedByAny(level, stand, 24.0D));
        if (candidates.isEmpty()) {
            return false;
        }
        ArmorStand stand = candidates.get(level.random.nextInt(candidates.size()));
        Set<UUID> observers = observerIds(level, stand.position(), 24);
        if (observers.isEmpty()) {
            return false;
        }
        int variant = level.random.nextInt(stand.isShowArms() ? 4 : 2);
        int durationTicks = 80 + level.random.nextInt(81);
        sendToObservers(player.getServer(), observers,
                new UncannyArmorStandPosePayload(stand.getId(), variant, durationTicks));
        ARMOR_POSE_TASKS.add(new ArmorPoseTask(
                player.getUUID(), level.dimension(), stand.getUUID(), stand.getId(),
                now + durationTicks, observers));
        ARMOR_STAND_REUSE_COOLDOWNS.put(stand.getUUID(), now + 45L * 60L * 20L);
        return true;
    }

    public static boolean triggerFishingTug(ServerPlayer player) {
        FishingHook hook = player.fishing;
        if (hook == null || !hook.isAlive() || !hook.isInWater() || hook.getHookedIn() != null) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        Vec3 radial = hook.position().subtract(player.position());
        Vec3 lateral = new Vec3(-radial.z, 0.0D, radial.x);
        if (lateral.lengthSqr() < 0.01D) {
            lateral = new Vec3(1.0D, 0.0D, 0.0D);
        }
        lateral = lateral.normalize().scale(level.random.nextBoolean() ? 0.15D : -0.15D);
        Set<UUID> observers = observerIds(level, hook.position(), OBSERVER_RADIUS);
        sendToObservers(player.getServer(), observers, new UncannyFishingTugPayload(
                hook.getId(), (float) lateral.x, (float) lateral.z));
        Vec3 splash = hook.position().add(lateral.scale(2.2D));
        level.sendParticles(ParticleTypes.SPLASH, splash.x, splash.y + 0.1D, splash.z,
                2, 0.08D, 0.02D, 0.08D, 0.0D);
        if (level.random.nextFloat() < 0.35F) {
            level.playSound(null, BlockPos.containing(splash), SoundEvents.FISHING_BOBBER_SPLASH,
                    SoundSource.NEUTRAL, 0.22F, 0.88F + level.random.nextFloat() * 0.18F);
        }
        return true;
    }

    public static boolean triggerLeafReply(ServerPlayer player, boolean debugImmediate) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        long now = server.getTickCount();
        LeafContext context = LAST_LEAF_CONTEXTS.get(player.getUUID());
        if (!debugImmediate && (context == null
                || !context.dimension().equals(player.serverLevel().dimension())
                || now - context.tick() > LEAF_CONTEXT_MAX_AGE_TICKS)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        BlockPos center = context == null ? player.blockPosition() : context.pos();
        List<BlockPos> candidates = findNearbyBlocks(level, center, 14, state -> state.is(BlockTags.LEAVES), 24);
        if (candidates.size() < 3 || (UncannyWorldState.get(server).getActiveWeatherEventId() != null
                && !UncannyWorldState.get(server).getActiveWeatherEventId().isBlank())) {
            return false;
        }
        List<BlockPos> leaves = new ArrayList<>(3);
        while (leaves.size() < 3) {
            leaves.add(candidates.remove(level.random.nextInt(candidates.size())));
        }
        long firstTick = now + (debugImmediate ? 10L : 100L + level.random.nextInt(701));
        int middleGap = 7 + level.random.nextInt(8);
        int finalGap = level.random.nextFloat() < 0.35F ? middleGap * 2 : middleGap;
        LEAF_REPLY_TASKS.add(new LeafReplyTask(
                player.getUUID(), level.dimension(), List.copyOf(leaves.subList(0, 3)),
                new long[]{firstTick, firstTick + middleGap, firstTick + middleGap + finalGap}, 0));
        LAST_LEAF_CONTEXTS.remove(player.getUUID());
        return true;
    }

    public static boolean triggerSilentBell(ServerPlayer player, boolean debugImmediate) {
        ServerLevel level = player.serverLevel();
        BlockPos bellPos = findNearbyBlock(level, player.blockPosition(), 18,
                state -> state.getBlock() instanceof BellBlock);
        if (bellPos == null || SILENT_BELL_TASKS.stream().anyMatch(task ->
                task.dimension().equals(level.dimension()) && task.bellPos().equals(bellPos))) {
            return false;
        }
        BlockState bellState = level.getBlockState(bellPos);
        Direction direction = bellState.hasProperty(BellBlock.FACING)
                ? bellState.getValue(BellBlock.FACING)
                : Direction.NORTH;
        Set<UUID> observers = observerIds(level, Vec3.atCenterOf(bellPos), OBSERVER_RADIUS);
        for (UUID observerId : observers) {
            ServerPlayer observer = player.getServer().getPlayerList().getPlayer(observerId);
            if (observer != null) {
                observer.connection.send(new ClientboundBlockEventPacket(
                        bellPos, bellState.getBlock(), 1, direction.get3DDataValue()));
            }
        }
        BlockPos delayedSoundPos = findEdgeGroundPosition(level, player, 7, 16, false);
        if (delayedSoundPos == null) {
            delayedSoundPos = bellPos.offset(8, 0, 0);
        }
        long now = player.getServer().getTickCount();
        long delay = debugImmediate ? 35L : (12L * 20L + level.random.nextInt(24 * 20));
        SILENT_BELL_TASKS.add(new SilentBellTask(
                player.getUUID(), level.dimension(), bellPos.immutable(), delayedSoundPos.immutable(), now + delay));
        return true;
    }

    public static boolean triggerEmptyCongregation(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(24.0D), Entity::isAlive).isEmpty()) {
            return false;
        }
        List<Villager> villagers = level.getEntitiesOfClass(
                Villager.class,
                player.getBoundingBox().inflate(18.0D, 6.0D, 18.0D),
                villager -> villager.isAlive()
                        && !villager.isBaby()
                        && !villager.isSleeping()
                        && !villager.isTrading()
                        && level.getRaidAt(villager.blockPosition()) == null);
        if (villagers.size() < 3) {
            return false;
        }
        List<Villager> selected = new ArrayList<>(3);
        while (selected.size() < 3 && !villagers.isEmpty()) {
            selected.add(villagers.remove(level.random.nextInt(villagers.size())));
        }
        Vec3 average = selected.stream().map(Entity::position).reduce(Vec3.ZERO, Vec3::add).scale(1.0D / selected.size());
        Vec3 target = findEmptyMeetingPoint(level, average);
        if (target == null) {
            return false;
        }
        long now = player.getServer().getTickCount();
        VILLAGER_MEETING_TASKS.add(new VillagerMeetingTask(
                player.getUUID(),
                level.dimension(),
                selected.stream().map(Entity::getUUID).toList(),
                target,
                now,
                now + 70L + level.random.nextInt(51)));
        return true;
    }

    public static boolean triggerEmptyLead(ServerPlayer player) {
        return triggerEmptyLead(player, false);
    }

    private static boolean triggerEmptyLead(ServerPlayer player, boolean debugImmediate) {
        ServerLevel level = player.serverLevel();
        List<BlockPos> fences = findNearbyBlocks(
                level, player.blockPosition(), 14, state -> state.is(BlockTags.FENCES), 32);
        if (debugImmediate) {
            Vec3 eye = player.getEyePosition();
            BlockHitResult pointed = level.clip(new ClipContext(
                    eye,
                    eye.add(player.getLookAngle().normalize().scale(18.0D)),
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player));
            BlockPos pointedPos = pointed.getBlockPos();
            if (pointed.getType() == HitResult.Type.BLOCK
                    && level.getBlockState(pointedPos).is(BlockTags.FENCES)) {
                fences.remove(pointedPos);
                fences.add(0, pointedPos.immutable());
            }
        }
        fences.removeIf(pos -> (!debugImmediate
                        && isPointObservedByAny(level, Vec3.atCenterOf(pos), 24.0D, 0.90D))
                || EMPTY_LEAD_TASKS.stream().anyMatch(task ->
                        task.dimension().equals(level.dimension()) && task.anchorPos().equals(pos)));
        if (fences.isEmpty()) {
            return false;
        }
        BlockPos anchorPos = debugImmediate ? fences.getFirst() : fences.get(level.random.nextInt(fences.size()));
        Vec3 anchor = Vec3.atCenterOf(anchorPos).add(0.0D, 0.42D, 0.0D);
        Vec3 end = findEmptyLeadEnd(level, player, anchor);
        if (end == null) {
            return false;
        }
        Set<UUID> observers = observerIds(level, anchor, OBSERVER_RADIUS);
        if (observers.isEmpty()) {
            return false;
        }
        long now = player.getServer().getTickCount();
        int duration = MinecraftNativeAnomalyRules.emptyLeadDurationTicks(
                level.random.nextInt(
                        MinecraftNativeAnomalyRules.EMPTY_LEAD_MAX_DURATION_TICKS
                                - MinecraftNativeAnomalyRules.EMPTY_LEAD_MIN_DURATION_TICKS
                                + 1));
        int visualId = nextVisualId();
        sendToObservers(player.getServer(), observers, new UncannyEmptyLeadPayload(
                visualId, true, anchor.x, anchor.y, anchor.z, end.x, end.y, end.z, duration));
        EMPTY_LEAD_TASKS.add(new EmptyLeadTask(
                player.getUUID(), level.dimension(), visualId, anchorPos.immutable(), anchor, end,
                now + MinecraftNativeAnomalyRules.EMPTY_LEAD_MIN_VISIBLE_TICKS,
                now + duration,
                observers));
        return true;
    }

    public static boolean triggerBorrowedPainting(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<Painting> paintings = level.getEntitiesOfClass(
                Painting.class,
                player.getBoundingBox().inflate(18.0D, 10.0D, 18.0D),
                painting -> painting.isAlive()
                        && !isEntityObservedByAny(level, painting, 26.0D)
                        && PAINTING_VARIANT_TASKS.stream().noneMatch(task ->
                        task.paintingUuid().equals(painting.getUUID())));
        if (paintings.isEmpty()) {
            return false;
        }
        Painting painting = paintings.get(level.random.nextInt(paintings.size()));
        Holder<PaintingVariant> original = painting.getVariant();
        List<Holder<PaintingVariant>> variants = new ArrayList<>();
        level.registryAccess().registryOrThrow(Registries.PAINTING_VARIANT)
                .getTagOrEmpty(PaintingVariantTags.PLACEABLE)
                .forEach(variant -> {
                    if (!variant.equals(original)
                            && variant.value().width() == original.value().width()
                            && variant.value().height() == original.value().height()) {
                        variants.add(variant);
                    }
                });
        if (variants.isEmpty()) {
            return false;
        }
        Holder<PaintingVariant> borrowed = variants.get(level.random.nextInt(variants.size()));
        Set<UUID> observers = observerIds(level, painting.position(), OBSERVER_RADIUS);
        if (observers.isEmpty()) {
            return false;
        }
        long now = player.getServer().getTickCount();
        int duration = 120 + level.random.nextInt(121);
        sendToObservers(player.getServer(), observers,
                new UncannyPaintingVariantPayload(painting.getId(), borrowed, duration));
        PAINTING_VARIANT_TASKS.add(new PaintingVariantTask(
                player.getUUID(), level.dimension(), painting.getUUID(), painting.getId(), original,
                now + 12L, now + duration, observers));
        return true;
    }

    public static boolean triggerReturnedDrop(ServerPlayer player) {
        PickupContext context = LAST_PICKUP_CONTEXTS.get(player.getUUID());
        if (context == null || !context.dimension().equals(player.serverLevel().dimension())) {
            return false;
        }
        long now = player.getServer().getTickCount();
        if (now - context.tick() < 40L || now - context.tick() > PICKUP_CONTEXT_MAX_AGE_TICKS
                || context.visual().isEmpty()
                || context.position().distanceToSqr(player.position()) > 32.0D * 32.0D) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = BlockPos.containing(context.position());
        if (!level.hasChunkAt(pos) || level.getBlockState(pos).isCollisionShapeFullBlock(level, pos)
                || !level.getEntitiesOfClass(ItemEntity.class,
                new AABB(context.position(), context.position()).inflate(0.85D),
                item -> item.isAlive() && item.getItem().is(context.visual().getItem())).isEmpty()) {
            return false;
        }
        Set<UUID> observers = observerIds(level, context.position(), OBSERVER_RADIUS);
        if (observers.isEmpty()) {
            return false;
        }
        int visualId = nextVisualId();
        int duration = 120 + level.random.nextInt(101);
        sendToObservers(player.getServer(), observers, new UncannyReturnedItemPayload(
                visualId, true, context.visual(), context.position().x, context.position().y,
                context.position().z, duration));
        RETURNED_DROP_TASKS.add(new ReturnedDropTask(
                player.getUUID(), level.dimension(), visualId, context.position(), now + 16L,
                now + duration, observers));
        LAST_PICKUP_CONTEXTS.remove(player.getUUID());
        return true;
    }

    public static boolean triggerMisdirectedEnchantment(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<BlockPos> tables = findNearbyBlocks(
                level, player.blockPosition(), 16, state -> state.is(Blocks.ENCHANTING_TABLE), 16);
        tables.removeIf(pos -> MISDIRECTED_ENCHANTMENT_TASKS.stream().anyMatch(task ->
                task.dimension().equals(level.dimension()) && task.tablePos().equals(pos)));
        if (tables.isEmpty()) {
            return false;
        }
        BlockPos table = tables.get(level.random.nextInt(tables.size()));
        List<BlockPos> shelves = EnchantingTableBlock.BOOKSHELF_OFFSETS.stream()
                .filter(offset -> EnchantingTableBlock.isValidBookShelf(level, table, offset))
                .map(table::offset)
                .toList();
        if (shelves.isEmpty()) {
            return false;
        }
        Vec3 falseTarget = findFalseEnchantTarget(level, table, player);
        if (falseTarget == null) {
            return false;
        }
        long now = player.getServer().getTickCount();
        MISDIRECTED_ENCHANTMENT_TASKS.add(new MisdirectedEnchantmentTask(
                player.getUUID(), level.dimension(), table.immutable(), List.copyOf(shelves), falseTarget,
                6 + level.random.nextInt(6), now + 4L));
        return true;
    }

    public static boolean triggerCauldronEcho(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<BlockPos> cauldrons = findNearbyBlocks(level, player.blockPosition(), 14,
                state -> state.getBlock() instanceof LayeredCauldronBlock
                        && state.getValue(LayeredCauldronBlock.LEVEL) > LayeredCauldronBlock.MIN_FILL_LEVEL,
                20);
        cauldrons.removeIf(pos -> isPointObservedByAny(level, Vec3.atCenterOf(pos), 22.0D, 0.91D)
                || CAULDRON_ECHO_TASKS.stream().anyMatch(task ->
                task.dimension().equals(level.dimension()) && task.pos().equals(pos)));
        if (cauldrons.isEmpty()) {
            return false;
        }
        BlockPos pos = cauldrons.get(level.random.nextInt(cauldrons.size()));
        BlockState real = level.getBlockState(pos);
        BlockState fake = real.setValue(
                LayeredCauldronBlock.LEVEL,
                real.getValue(LayeredCauldronBlock.LEVEL) - 1);
        Set<UUID> observers = observerIds(level, Vec3.atCenterOf(pos), OBSERVER_RADIUS);
        if (observers.isEmpty()) {
            return false;
        }
        sendBlockStateToObservers(player.getServer(), observers, pos, fake);
        level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS,
                0.20F, 0.82F + level.random.nextFloat() * 0.18F);
        long now = player.getServer().getTickCount();
        CAULDRON_ECHO_TASKS.add(new CauldronEchoTask(
                player.getUUID(), level.dimension(), pos.immutable(), now + 10L,
                now + 70L + level.random.nextInt(61), observers));
        return true;
    }

    public static boolean triggerEmptyWake(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<Vec3> wake = findEmptyWake(level, player);
        if (wake.size() < MinecraftNativeAnomalyRules.EMPTY_WAKE_MIN_POINTS) {
            return false;
        }
        long now = player.getServer().getTickCount();
        EMPTY_WAKE_TASKS.add(new EmptyWakeTask(
                player.getUUID(), level.dimension(), List.copyOf(wake), 0, now + 3L));
        return true;
    }

    /** Read-only diagnostics used by the headless regression suite. */
    public static Optional<EmptyLeadDebugSnapshot> emptyLeadSnapshotForTesting(UUID ownerId) {
        return EMPTY_LEAD_TASKS.stream()
                .filter(task -> task.ownerId().equals(ownerId))
                .findFirst()
                .map(task -> new EmptyLeadDebugSnapshot(
                        task.minimumVisibleUntil(),
                        task.endTick()));
    }

    /** Read-only diagnostics used by the headless regression suite. */
    public static Optional<EmptyWakeDebugSnapshot> emptyWakeSnapshotForTesting(UUID ownerId) {
        return EMPTY_WAKE_TASKS.stream()
                .filter(task -> task.ownerId().equals(ownerId))
                .findFirst()
                .map(task -> new EmptyWakeDebugSnapshot(
                        List.copyOf(task.points()),
                        task.index(),
                        task.nextPulseTick()));
    }

    public static boolean triggerMapIntruder(ServerPlayer player) {
        ItemStack mapStack = player.getMainHandItem();
        MapId mapId = mapStack.get(DataComponents.MAP_ID);
        if (mapId == null) {
            mapStack = player.getOffhandItem();
            mapId = mapStack.get(DataComponents.MAP_ID);
        }
        if (mapId == null || !(mapStack.getItem() instanceof MapItem)) {
            return false;
        }
        MapItemSavedData data = MapItem.getSavedData(mapStack, player.serverLevel());
        if (data == null) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        for (int attempt = 0; attempt < 192; attempt++) {
            int pixelX = 8 + level.random.nextInt(112);
            int pixelY = 8 + level.random.nextInt(112);
            if (data.colors[pixelY * 128 + pixelX] == 0) {
                continue;
            }
            boolean horizontal = level.random.nextBoolean();
            byte moveX = (byte) (horizontal ? (level.random.nextBoolean() ? 2 : -2) : 0);
            byte moveY = (byte) (!horizontal ? (level.random.nextBoolean() ? 2 : -2) : 0);
            byte x = (byte) ((pixelX - 64) * 2);
            byte y = (byte) ((pixelY - 64) * 2);
            PacketDistributor.sendToPlayer(player, new UncannyMapIntruderPayload(
                    mapId.id(), x, y, moveX, moveY, 90 + level.random.nextInt(51)));
            return true;
        }
        return false;
    }

    private static void tickGhostBreaking(MinecraftServer server, long now) {
        Iterator<GhostBreakingTask> iterator = GHOST_BREAKING_TASKS.iterator();
        while (iterator.hasNext()) {
            GhostBreakingTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            if (level == null || owner == null || owner.serverLevel() != level
                    || !isSafeGhostBreakingBlock(level, task.pos())
                    || isPointObservedByAny(level, Vec3.atCenterOf(task.pos()), 28.0D, 0.91D)) {
                clearGhostBreaking(level, task);
                iterator.remove();
                continue;
            }
            if (now < task.nextStageTick()) {
                continue;
            }
            int stage = task.stages()[task.stageIndex()];
            level.destroyBlockProgress(task.breakerId(), task.pos(), stage);
            if (stage < 0 || task.stageIndex() + 1 >= task.stages().length) {
                iterator.remove();
                continue;
            }
            if (level.random.nextFloat() < 0.82F) {
                playSharedBlockHit(level, task.pos(), level.getBlockState(task.pos()), 0.18F);
            }
            task.advance(now + 7L + level.random.nextInt(10));
        }
    }

    private static void tickOrphanShadows(MinecraftServer server, long now) {
        Iterator<OrphanShadowTask> iterator = ORPHAN_SHADOW_TASKS.iterator();
        while (iterator.hasNext()) {
            OrphanShadowTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            if (level == null || owner == null || owner.serverLevel() != level || now >= task.endTick()
                    || isPointObservedByAny(level, task.point().add(0.0D, 0.05D, 0.0D), 28.0D, 0.94D)) {
                sendToObservers(server, task.observers(), new UncannyOrphanShadowPayload(
                        task.shadowId(), false, task.point().x, task.point().y, task.point().z, task.radius(), 0));
                iterator.remove();
            }
        }
    }

    private static void tickColdFurnaces(MinecraftServer server, long now) {
        Iterator<ColdFurnaceTask> iterator = COLD_FURNACE_TASKS.iterator();
        while (iterator.hasNext()) {
            ColdFurnaceTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            boolean restore = level == null || owner == null || owner.serverLevel() != level || now >= task.endTick();
            if (!restore && isPointObservedByAny(level, Vec3.atCenterOf(task.pos()), 24.0D, 0.93D)) {
                restore = true;
            }
            if (restore) {
                restoreColdFurnace(server, level, task);
                iterator.remove();
                continue;
            }
            if (!task.cuePlayed() && now >= task.cueTick()) {
                level.playSound(null, task.pos(), SoundEvents.FURNACE_FIRE_CRACKLE,
                        SoundSource.BLOCKS, 0.16F, 0.92F + level.random.nextFloat() * 0.12F);
                task.markCuePlayed();
            }
        }
    }

    private static void tickArmorPoses(MinecraftServer server, long now) {
        Iterator<ArmorPoseTask> iterator = ARMOR_POSE_TASKS.iterator();
        while (iterator.hasNext()) {
            ArmorPoseTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            Entity raw = level == null ? null : level.getEntity(task.standUuid());
            boolean restore = !(raw instanceof ArmorStand stand) || owner == null || owner.serverLevel() != level
                    || now >= task.endTick() || isEntityObservedByAny(level, stand, 24.0D);
            if (restore) {
                sendToObservers(server, task.observers(),
                        new UncannyArmorStandPosePayload(task.entityId(), -1, 0));
                iterator.remove();
            }
        }
    }

    private static void tickLeafReplies(MinecraftServer server, long now) {
        Iterator<LeafReplyTask> iterator = LEAF_REPLY_TASKS.iterator();
        while (iterator.hasNext()) {
            LeafReplyTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            if (level == null || owner == null || owner.serverLevel() != level) {
                iterator.remove();
                continue;
            }
            if (task.index() >= task.positions().size()) {
                iterator.remove();
                continue;
            }
            if (now < task.pulseTicks()[task.index()]) {
                continue;
            }
            BlockPos pos = task.positions().get(task.index());
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LEAVES)) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D,
                        4, 0.28D, 0.22D, 0.28D, 0.015D);
                level.playSound(null, pos, state.getSoundType().getStepSound(), SoundSource.BLOCKS,
                        0.23F, 0.86F + level.random.nextFloat() * 0.20F);
            }
            task.advance();
            if (task.index() >= task.positions().size()) {
                iterator.remove();
            }
        }
    }

    private static void tickSilentBells(MinecraftServer server, long now) {
        Iterator<SilentBellTask> iterator = SILENT_BELL_TASKS.iterator();
        while (iterator.hasNext()) {
            SilentBellTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            if (level == null || owner == null || owner.serverLevel() != level) {
                iterator.remove();
                continue;
            }
            if (now >= task.soundTick()) {
                level.playSound(null, task.delayedSoundPos(), SoundEvents.BELL_BLOCK,
                        SoundSource.BLOCKS, 0.52F, 0.90F + level.random.nextFloat() * 0.10F);
                iterator.remove();
            }
        }
    }

    private static void tickAnimalAttention(MinecraftServer server, long now) {
        Iterator<AnimalAttentionTask> iterator = ANIMAL_ATTENTION_TASKS.iterator();
        while (iterator.hasNext()) {
            AnimalAttentionTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            Entity raw = level == null ? null : level.getEntity(task.animalUuid());
            if (!(raw instanceof Animal animal) || owner == null || owner.serverLevel() != level
                    || now >= task.endTick()) {
                iterator.remove();
                continue;
            }
            animal.getLookControl().setLookAt(task.target().x, task.target().y, task.target().z, 26.0F, 22.0F);
        }
    }

    private static void tickVillagerMeetings(MinecraftServer server, long now) {
        Iterator<VillagerMeetingTask> iterator = VILLAGER_MEETING_TASKS.iterator();
        while (iterator.hasNext()) {
            VillagerMeetingTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            if (level == null || owner == null || owner.serverLevel() != level || now >= task.endTick()
                    || level.players().stream().anyMatch(player -> player.isAlive()
                    && player.position().distanceToSqr(task.target()) < 2.25D)
                    || !level.getEntitiesOfClass(Monster.class, new AABB(task.target(), task.target()).inflate(18.0D), Entity::isAlive).isEmpty()) {
                iterator.remove();
                continue;
            }
            for (int index = 0; index < task.villagerUuids().size(); index++) {
                if (now < task.startTick() + index * 10L) {
                    continue;
                }
                Entity raw = level.getEntity(task.villagerUuids().get(index));
                if (raw instanceof Villager villager && villager.isAlive() && !villager.isTrading()
                        && level.getRaidAt(villager.blockPosition()) == null) {
                    villager.getNavigation().stop();
                    villager.getLookControl().setLookAt(
                            task.target().x, task.target().y, task.target().z, 30.0F, 25.0F);
                }
            }
        }
    }

    private static void tickEmptyLeads(MinecraftServer server, long now) {
        Iterator<EmptyLeadTask> iterator = EMPTY_LEAD_TASKS.iterator();
        while (iterator.hasNext()) {
            EmptyLeadTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            boolean remove = level == null || owner == null || owner.serverLevel() != level
                    || now >= task.endTick();
            if (!remove && now >= task.minimumVisibleUntil()) {
                remove = owner.position().distanceToSqr(task.anchor()) <= 2.6D * 2.6D
                        || isPointObservedByAny(level, task.end(), 26.0D, 0.96D);
            }
            if (remove) {
                sendToObservers(server, task.observers(), new UncannyEmptyLeadPayload(
                        task.visualId(), false,
                        task.anchor().x, task.anchor().y, task.anchor().z,
                        task.end().x, task.end().y, task.end().z, 0));
                iterator.remove();
            }
        }
    }

    private static void tickPaintingVariants(MinecraftServer server, long now) {
        Iterator<PaintingVariantTask> iterator = PAINTING_VARIANT_TASKS.iterator();
        while (iterator.hasNext()) {
            PaintingVariantTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            Entity raw = level == null ? null : level.getEntity(task.paintingUuid());
            Painting painting = raw instanceof Painting value ? value : null;
            boolean restore = painting == null || owner == null || owner.serverLevel() != level
                    || now >= task.endTick();
            if (!restore && now >= task.minimumVisibleUntil()) {
                restore = isEntityObservedByAny(level, painting, 26.0D);
            }
            if (restore) {
                restorePainting(server, task);
                iterator.remove();
            }
        }
    }

    private static void tickReturnedDrops(MinecraftServer server, long now) {
        Iterator<ReturnedDropTask> iterator = RETURNED_DROP_TASKS.iterator();
        while (iterator.hasNext()) {
            ReturnedDropTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            boolean remove = level == null || owner == null || owner.serverLevel() != level
                    || now >= task.endTick();
            if (!remove && now >= task.minimumVisibleUntil()) {
                remove = level.players().stream().anyMatch(observer -> observer.isAlive()
                        && !observer.isSpectator()
                        && observer.position().distanceToSqr(task.position()) <= 2.2D * 2.2D);
            }
            if (remove) {
                sendToObservers(server, task.observers(), new UncannyReturnedItemPayload(
                        task.visualId(), false, ItemStack.EMPTY,
                        task.position().x, task.position().y, task.position().z, 0));
                iterator.remove();
            }
        }
    }

    private static void tickMisdirectedEnchantments(MinecraftServer server, long now) {
        Iterator<MisdirectedEnchantmentTask> iterator = MISDIRECTED_ENCHANTMENT_TASKS.iterator();
        while (iterator.hasNext()) {
            MisdirectedEnchantmentTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            if (level == null || owner == null || owner.serverLevel() != level
                    || !level.getBlockState(task.tablePos()).is(Blocks.ENCHANTING_TABLE)
                    || task.pulsesRemaining() <= 0) {
                iterator.remove();
                continue;
            }
            if (now < task.nextPulseTick()) {
                continue;
            }
            List<BlockPos> validShelves = task.shelves().stream()
                    .filter(pos -> level.getBlockState(pos).getEnchantPowerBonus(level, pos) != 0.0F)
                    .toList();
            if (validShelves.isEmpty()) {
                iterator.remove();
                continue;
            }
            BlockPos shelf = validShelves.get(level.random.nextInt(validShelves.size()));
            Vec3 target = task.falseTarget();
            level.sendParticles(
                    ParticleTypes.ENCHANT,
                    target.x, target.y, target.z,
                    0,
                    shelf.getX() + 0.5D - target.x,
                    shelf.getY() + 0.7D - target.y,
                    shelf.getZ() + 0.5D - target.z,
                    1.0D);
            if (task.pulsesRemaining() == 1 && level.random.nextFloat() < 0.35F) {
                level.playSound(null, BlockPos.containing(target), SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.BLOCKS, 0.16F, 1.35F + level.random.nextFloat() * 0.18F);
            }
            task.advance(now + 3L + level.random.nextInt(5));
        }
    }

    private static void tickCauldronEchoes(MinecraftServer server, long now) {
        Iterator<CauldronEchoTask> iterator = CAULDRON_ECHO_TASKS.iterator();
        while (iterator.hasNext()) {
            CauldronEchoTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            boolean restore = level == null || owner == null || owner.serverLevel() != level
                    || now >= task.endTick();
            if (!restore && now >= task.minimumVisibleUntil()) {
                restore = !(level.getBlockState(task.pos()).getBlock() instanceof LayeredCauldronBlock)
                        || isPointObservedByAny(level, Vec3.atCenterOf(task.pos()), 24.0D, 0.95D);
            }
            if (restore) {
                restoreCauldronEcho(server, level, task);
                iterator.remove();
            }
        }
    }

    private static void tickEmptyWakes(MinecraftServer server, long now) {
        Iterator<EmptyWakeTask> iterator = EMPTY_WAKE_TASKS.iterator();
        while (iterator.hasNext()) {
            EmptyWakeTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            if (level == null || owner == null || owner.serverLevel() != level
                    || task.index() >= task.points().size()) {
                iterator.remove();
                continue;
            }
            if (now < task.nextPulseTick()) {
                continue;
            }
            Vec3 point = task.points().get(task.index());
            BlockPos waterPos = BlockPos.containing(point.x, point.y - 0.15D, point.z);
            if (!isWaterSurface(level, waterPos)) {
                iterator.remove();
                continue;
            }
            level.sendParticles(ParticleTypes.SPLASH,
                    point.x, point.y, point.z, EMPTY_WAKE_SPLASH_PARTICLES,
                    0.42D, 0.05D, 0.42D, 0.07D);
            level.sendParticles(ParticleTypes.BUBBLE_POP,
                    point.x, point.y - 0.04D, point.z, EMPTY_WAKE_BUBBLE_PARTICLES,
                    0.32D, 0.03D, 0.32D, 0.045D);
            if ((task.index() == 0 || task.index() == task.points().size() - 1)
                    && level.random.nextFloat() < 0.30F) {
                level.playSound(null, BlockPos.containing(point), SoundEvents.FISHING_BOBBER_SPLASH,
                        SoundSource.NEUTRAL, 0.16F, 0.92F + level.random.nextFloat() * 0.16F);
            }
            task.advance(now + MinecraftNativeAnomalyRules.EMPTY_WAKE_PULSE_INTERVAL_TICKS);
        }
    }

    private static boolean canTrigger(ServerPlayer player, String eventId) {
        if (player == null || player.getServer() == null || player.isSpectator()) {
            return false;
        }
        UncannyWorldState state = UncannyWorldState.get(player.getServer());
        if (!state.isSystemEnabled() || !ParanoiaEventCatalog.post111EventIds().contains(eventId)) {
            return false;
        }
        return state.getCurrentPhaseIndex() >= ParanoiaEventCatalog.require(eventId).minimumPhase();
    }

    private static BlockPos findGhostBreakingBlock(ServerLevel level, ServerPlayer player) {
        BlockPos center = player.blockPosition();
        for (int attempt = 0; attempt < 90; attempt++) {
            int dx = level.random.nextInt(17) - 8;
            int dy = level.random.nextInt(7) - 3;
            int dz = level.random.nextInt(17) - 8;
            if (dx * dx + dz * dz < 16) {
                continue;
            }
            BlockPos pos = center.offset(dx, dy, dz);
            if (!level.hasChunkAt(pos) || !isSafeGhostBreakingBlock(level, pos)
                    || isPointObservedByAny(level, Vec3.atCenterOf(pos), 28.0D, 0.88D)) {
                continue;
            }
            return pos;
        }
        return null;
    }

    private static boolean isSafeGhostBreakingBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return (state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.PLANKS))
                && level.getBlockEntity(pos) == null
                && state.getDestroySpeed(level, pos) >= 0.0F
                && state.isCollisionShapeFullBlock(level, pos);
    }

    private static BlockPos findEdgeGroundPosition(
            ServerLevel level,
            ServerPlayer player,
            int minDistance,
            int maxDistance,
            boolean requireLight) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        for (int attempt = 0; attempt < 80; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            int distance = minDistance + level.random.nextInt(Math.max(1, maxDistance - minDistance + 1));
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            for (int dy = 3; dy >= -6; dy--) {
                BlockPos feet = new BlockPos(x, player.getBlockY() + dy, z);
                if (!level.hasChunkAt(feet)) {
                    continue;
                }
                BlockPos supportPos = feet.below();
                BlockState support = level.getBlockState(supportPos);
                if (!support.isCollisionShapeFullBlock(level, supportPos)
                        || !level.getBlockState(feet).isAir()
                        || !level.getBlockState(feet.above()).isAir()
                        || (requireLight && level.getMaxLocalRawBrightness(feet) <= 3)
                        || !level.getEntities(null, new AABB(feet).inflate(0.15D)).isEmpty()) {
                    continue;
                }
                Vec3 to = Vec3.atCenterOf(feet).subtract(eye).normalize();
                double dot = look.dot(to);
                if (dot < -0.20D || dot > 0.78D) {
                    continue;
                }
                return feet.immutable();
            }
        }
        return null;
    }

    private static BlockPos findNearbyBlock(
            ServerLevel level,
            BlockPos center,
            int radius,
            java.util.function.Predicate<BlockState> predicate) {
        List<BlockPos> matches = findNearbyBlocks(level, center, radius, predicate, 24);
        return matches.isEmpty() ? null : matches.get(level.random.nextInt(matches.size()));
    }

    private static List<BlockPos> findNearbyBlocks(
            ServerLevel level,
            BlockPos center,
            int radius,
            java.util.function.Predicate<BlockState> predicate,
            int limit) {
        List<BlockPos> matches = new ArrayList<>();
        int vertical = Math.min(7, radius);
        for (int y = -vertical; y <= vertical && matches.size() < limit; y++) {
            for (int x = -radius; x <= radius && matches.size() < limit; x++) {
                for (int z = -radius; z <= radius && matches.size() < limit; z++) {
                    if (x * x + z * z > radius * radius) {
                        continue;
                    }
                    BlockPos pos = center.offset(x, y, z);
                    if (level.hasChunkAt(pos) && predicate.test(level.getBlockState(pos))) {
                        matches.add(pos.immutable());
                    }
                }
            }
        }
        return matches;
    }

    private static Vec3 findEmptyLeadEnd(ServerLevel level, ServerPlayer player, Vec3 anchor) {
        for (int attempt = 0; attempt < 36; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double length = 3.0D + level.random.nextDouble() * 2.5D;
            Vec3 end = anchor.add(
                    Math.cos(angle) * length,
                    -0.25D + level.random.nextDouble() * 0.35D,
                    Math.sin(angle) * length);
            BlockPos endPos = BlockPos.containing(end);
            if (!level.hasChunkAt(endPos)
                    || !level.getBlockState(endPos).getCollisionShape(level, endPos).isEmpty()
                    || !level.getBlockState(endPos.above()).getCollisionShape(level, endPos.above()).isEmpty()) {
                continue;
            }
            Vec3 horizontal = new Vec3(end.x - anchor.x, 0.0D, end.z - anchor.z).normalize();
            Vec3 rayStart = anchor.add(horizontal.scale(0.85D));
            BlockHitResult hit = level.clip(new ClipContext(
                    rayStart, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) {
                return end;
            }
        }
        return null;
    }

    private static Vec3 findFalseEnchantTarget(ServerLevel level, BlockPos table, ServerPlayer player) {
        Vec3 origin = Vec3.atCenterOf(table).add(0.0D, 1.5D, 0.0D);
        for (int attempt = 0; attempt < 36; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = 2.2D + level.random.nextDouble() * 2.4D;
            Vec3 target = origin.add(Math.cos(angle) * distance, -0.25D, Math.sin(angle) * distance);
            BlockPos targetPos = BlockPos.containing(target);
            if (!level.hasChunkAt(targetPos)
                    || !level.getBlockState(targetPos).getCollisionShape(level, targetPos).isEmpty()) {
                continue;
            }
            BlockHitResult hit = level.clip(new ClipContext(
                    origin, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) {
                return target;
            }
        }
        return null;
    }

    private static List<Vec3> findEmptyWake(ServerLevel level, ServerPlayer player) {
        BlockPos center = player.blockPosition();
        List<BlockPos> nearPlayerSurfaces = new ArrayList<>();
        for (int dy = 4; dy >= -7; dy--) {
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    if (dx * dx + dz * dz > 8 * 8) {
                        continue;
                    }
                    BlockPos candidate = center.offset(dx, dy, dz);
                    if (isWaterSurface(level, candidate)) {
                        nearPlayerSurfaces.add(candidate.immutable());
                    }
                }
            }
        }
        nearPlayerSurfaces.sort(Comparator.comparingDouble(pos -> horizontalDistanceSqr(pos, player.position())));

        List<BlockPos> bestPath = List.of();
        double bestEndDistance = Double.MAX_VALUE;
        int seedLimit = Math.min(12, nearPlayerSurfaces.size());
        for (int index = 0; index < seedLimit; index++) {
            BlockPos nearPlayer = nearPlayerSurfaces.get(index);
            List<BlockPos> candidate = findWaterSurfacePathTowardPlayer(level, center, nearPlayer);
            if (candidate.size() < MinecraftNativeAnomalyRules.EMPTY_WAKE_MIN_POINTS) {
                continue;
            }
            double endDistance = horizontalDistanceSqr(candidate.getLast(), player.position());
            if (bestPath.isEmpty()
                    || endDistance < bestEndDistance - 0.01D
                    || (Math.abs(endDistance - bestEndDistance) <= 0.01D
                    && candidate.size() > bestPath.size())) {
                bestPath = candidate;
                bestEndDistance = endDistance;
            }
        }

        if (bestPath.isEmpty()) {
            return List.of();
        }
        return bestPath.stream()
                .map(pos -> new Vec3(pos.getX() + 0.5D, pos.getY() + 1.02D, pos.getZ() + 0.5D))
                .toList();
    }

    /**
     * Breadth-first search from water nearest the player. Reconstructing from the distant node
     * back through its parents gives a guaranteed water-only wake travelling toward the player.
     */
    private static List<BlockPos> findWaterSurfacePathTowardPlayer(
            ServerLevel level,
            BlockPos playerCenter,
            BlockPos nearPlayer) {
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        Map<BlockPos, Integer> depth = new HashMap<>();
        open.add(nearPlayer);
        parent.put(nearPlayer, null);
        depth.put(nearPlayer, 0);

        BlockPos farthest = null;
        int farthestDepth = -1;
        double nearDistance = horizontalDistanceSqr(nearPlayer, Vec3.atCenterOf(playerCenter));
        int targetDepth = MinecraftNativeAnomalyRules.EMPTY_WAKE_TARGET_POINTS - 1;

        while (!open.isEmpty()) {
            BlockPos current = open.removeFirst();
            int currentDepth = depth.get(current);
            double currentDistance = horizontalDistanceSqr(current, Vec3.atCenterOf(playerCenter));
            if (currentDepth >= MinecraftNativeAnomalyRules.EMPTY_WAKE_MIN_POINTS - 1
                    && Math.sqrt(currentDistance) >= Math.sqrt(nearDistance) + 6.0D
                    && currentDepth > farthestDepth) {
                farthest = current;
                farthestDepth = currentDepth;
            }
            if (currentDepth >= targetDepth) {
                continue;
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = current.relative(direction).immutable();
                int dx = next.getX() - playerCenter.getX();
                int dz = next.getZ() - playerCenter.getZ();
                if (dx * dx + dz * dz
                        > MinecraftNativeAnomalyRules.EMPTY_WAKE_MAX_SEARCH_RADIUS
                        * MinecraftNativeAnomalyRules.EMPTY_WAKE_MAX_SEARCH_RADIUS
                        || parent.containsKey(next)
                        || !isWaterSurface(level, next)) {
                    continue;
                }
                parent.put(next, current);
                depth.put(next, currentDepth + 1);
                open.addLast(next);
            }
        }

        if (farthest == null) {
            return List.of();
        }
        List<BlockPos> towardPlayer = new ArrayList<>(farthestDepth + 1);
        for (BlockPos cursor = farthest; cursor != null; cursor = parent.get(cursor)) {
            towardPlayer.add(cursor);
        }
        return List.copyOf(towardPlayer);
    }

    private static boolean isWaterSurface(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos)
                && level.getBlockState(pos).is(Blocks.WATER)
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).isEmpty()
                && level.getBlockState(pos.above()).isAir();
    }

    private static double horizontalDistanceSqr(BlockPos pos, Vec3 target) {
        double dx = pos.getX() + 0.5D - target.x;
        double dz = pos.getZ() + 0.5D - target.z;
        return dx * dx + dz * dz;
    }

    private static Vec3 findEmptyMeetingPoint(ServerLevel level, Vec3 average) {
        for (int attempt = 0; attempt < 30; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = 2.0D + level.random.nextDouble() * 2.5D;
            BlockPos pos = BlockPos.containing(
                    average.x + Math.cos(angle) * distance,
                    average.y,
                    average.z + Math.sin(angle) * distance);
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                    && level.getEntities(null, new AABB(pos).inflate(0.6D)).isEmpty()) {
                return Vec3.atCenterOf(pos);
            }
        }
        return null;
    }

    private static boolean isPointObservedByAny(
            ServerLevel level,
            Vec3 point,
            double radius,
            double dotThreshold) {
        for (ServerPlayer observer : level.players()) {
            if (observer.isAlive() && !observer.isSpectator()
                    && observer.position().distanceToSqr(point) <= radius * radius
                    && isPointObserved(observer, point, dotThreshold)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPointObserved(ServerPlayer observer, Vec3 point, double dotThreshold) {
        Vec3 eye = observer.getEyePosition();
        Vec3 to = point.subtract(eye);
        if (to.lengthSqr() < 0.01D || observer.getLookAngle().normalize().dot(to.normalize()) < dotThreshold) {
            return false;
        }
        BlockHitResult hit = observer.serverLevel().clip(new ClipContext(
                eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, observer));
        return hit.getType() == HitResult.Type.MISS
                || hit.getLocation().distanceToSqr(point) < 1.0D
                || hit.getBlockPos().equals(BlockPos.containing(point));
    }

    private static boolean isEntityObservedByAny(ServerLevel level, Entity entity, double radius) {
        if (level == null || entity == null) {
            return false;
        }
        Vec3 target = entity.getBoundingBox().getCenter();
        for (ServerPlayer observer : level.players()) {
            if (!observer.isAlive() || observer.isSpectator()
                    || observer.distanceToSqr(entity) > radius * radius) {
                continue;
            }
            Vec3 to = target.subtract(observer.getEyePosition()).normalize();
            if (observer.getLookAngle().normalize().dot(to) >= 0.92D && observer.hasLineOfSight(entity)) {
                return true;
            }
        }
        return false;
    }

    private static Set<UUID> observerIds(ServerLevel level, Vec3 center, double radius) {
        Set<UUID> observers = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator()
                    && player.position().distanceToSqr(center) <= radius * radius) {
                observers.add(player.getUUID());
            }
        }
        return Set.copyOf(observers);
    }

    private static void sendToObservers(
            MinecraftServer server,
            Set<UUID> observerIds,
            CustomPacketPayload payload) {
        if (server == null) {
            return;
        }
        for (UUID observerId : observerIds) {
            ServerPlayer observer = server.getPlayerList().getPlayer(observerId);
            if (observer != null) {
                PacketDistributor.sendToPlayer(observer, payload);
            }
        }
    }

    private static void sendBlockStateToObservers(
            MinecraftServer server,
            Set<UUID> observerIds,
            BlockPos pos,
            BlockState state) {
        for (UUID observerId : observerIds) {
            ServerPlayer observer = server.getPlayerList().getPlayer(observerId);
            if (observer != null) {
                observer.connection.send(new ClientboundBlockUpdatePacket(pos, state));
            }
        }
    }

    private static void spawnSparsePortalParticles(ServerLevel level, BlockPos pos, int count) {
        level.sendParticles(ParticleTypes.PORTAL,
                pos.getX() + 0.5D,
                pos.getY() + 0.9D,
                pos.getZ() + 0.5D,
                count,
                0.22D,
                0.65D,
                0.22D,
                0.02D);
    }

    private static void playSharedBlockHit(ServerLevel level, BlockPos pos, BlockState state, float volume) {
        level.playSound(null, pos, state.getSoundType().getHitSound(), SoundSource.BLOCKS,
                volume, 0.78F + level.random.nextFloat() * 0.20F);
    }

    private static void clearGhostBreaking(ServerLevel level, GhostBreakingTask task) {
        if (level != null) {
            level.destroyBlockProgress(task.breakerId(), task.pos(), -1);
        }
    }

    private static void cancelColdFurnace(ServerLevel level, BlockPos pos) {
        Iterator<ColdFurnaceTask> iterator = COLD_FURNACE_TASKS.iterator();
        while (iterator.hasNext()) {
            ColdFurnaceTask task = iterator.next();
            if (task.dimension().equals(level.dimension()) && task.pos().equals(pos)) {
                restoreColdFurnace(level.getServer(), level, task);
                iterator.remove();
            }
        }
    }

    private static void restoreColdFurnace(MinecraftServer server, ServerLevel level, ColdFurnaceTask task) {
        if (server == null || level == null) {
            return;
        }
        sendBlockStateToObservers(server, task.observers(), task.pos(), level.getBlockState(task.pos()));
    }

    private static void restoreArmorPoseForInteraction(Entity player, Entity target) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(target instanceof ArmorStand stand)) {
            return;
        }
        MinecraftServer server = serverPlayer.getServer();
        Iterator<ArmorPoseTask> iterator = ARMOR_POSE_TASKS.iterator();
        while (iterator.hasNext()) {
            ArmorPoseTask task = iterator.next();
            if (task.standUuid().equals(stand.getUUID())) {
                sendToObservers(server, task.observers(),
                        new UncannyArmorStandPosePayload(task.entityId(), -1, 0));
                iterator.remove();
            }
        }
    }

    private static void restorePaintingForInteraction(Entity player, Entity target) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(target instanceof Painting painting)) {
            return;
        }
        Iterator<PaintingVariantTask> iterator = PAINTING_VARIANT_TASKS.iterator();
        while (iterator.hasNext()) {
            PaintingVariantTask task = iterator.next();
            if (task.paintingUuid().equals(painting.getUUID())) {
                restorePainting(serverPlayer.getServer(), task);
                iterator.remove();
            }
        }
    }

    private static void restorePainting(MinecraftServer server, PaintingVariantTask task) {
        sendToObservers(server, task.observers(),
                new UncannyPaintingVariantPayload(task.entityId(), task.original(), 0));
    }

    private static void cancelCauldronEcho(ServerLevel level, BlockPos pos) {
        Iterator<CauldronEchoTask> iterator = CAULDRON_ECHO_TASKS.iterator();
        while (iterator.hasNext()) {
            CauldronEchoTask task = iterator.next();
            if (task.dimension().equals(level.dimension()) && task.pos().equals(pos)) {
                restoreCauldronEcho(level.getServer(), level, task);
                iterator.remove();
            }
        }
    }

    private static void restoreCauldronEcho(
            MinecraftServer server,
            ServerLevel level,
            CauldronEchoTask task) {
        if (server != null && level != null) {
            sendBlockStateToObservers(server, task.observers(), task.pos(), level.getBlockState(task.pos()));
        }
    }

    private static void cleanupOwnerTasks(MinecraftServer server, UUID ownerId) {
        RailAndSignalAnomalySystem.clearForOwner(server, ownerId);
        ContextualWorldAnomalySystem.clearForOwner(server, ownerId);
        ObjectPresentationAnomalySystem.clearForOwner(server, ownerId);
        Iterator<GhostBreakingTask> ghostIterator = GHOST_BREAKING_TASKS.iterator();
        while (ghostIterator.hasNext()) {
            GhostBreakingTask task = ghostIterator.next();
            if (task.ownerId().equals(ownerId)) {
                clearGhostBreaking(server == null ? null : server.getLevel(task.dimension()), task);
                ghostIterator.remove();
            }
        }
        Iterator<OrphanShadowTask> shadowIterator = ORPHAN_SHADOW_TASKS.iterator();
        while (shadowIterator.hasNext()) {
            OrphanShadowTask task = shadowIterator.next();
            if (task.ownerId().equals(ownerId)) {
                sendToObservers(server, task.observers(), new UncannyOrphanShadowPayload(
                        task.shadowId(), false, task.point().x, task.point().y, task.point().z, task.radius(), 0));
                shadowIterator.remove();
            }
        }
        Iterator<ColdFurnaceTask> furnaceIterator = COLD_FURNACE_TASKS.iterator();
        while (furnaceIterator.hasNext()) {
            ColdFurnaceTask task = furnaceIterator.next();
            if (task.ownerId().equals(ownerId)) {
                restoreColdFurnace(server, server == null ? null : server.getLevel(task.dimension()), task);
                furnaceIterator.remove();
            }
        }
        Iterator<ArmorPoseTask> poseIterator = ARMOR_POSE_TASKS.iterator();
        while (poseIterator.hasNext()) {
            ArmorPoseTask task = poseIterator.next();
            if (task.ownerId().equals(ownerId)) {
                sendToObservers(server, task.observers(),
                        new UncannyArmorStandPosePayload(task.entityId(), -1, 0));
                poseIterator.remove();
            }
        }
        Iterator<EmptyLeadTask> leadIterator = EMPTY_LEAD_TASKS.iterator();
        while (leadIterator.hasNext()) {
            EmptyLeadTask task = leadIterator.next();
            if (task.ownerId().equals(ownerId)) {
                sendToObservers(server, task.observers(), new UncannyEmptyLeadPayload(
                        task.visualId(), false,
                        task.anchor().x, task.anchor().y, task.anchor().z,
                        task.end().x, task.end().y, task.end().z, 0));
                leadIterator.remove();
            }
        }
        Iterator<PaintingVariantTask> paintingIterator = PAINTING_VARIANT_TASKS.iterator();
        while (paintingIterator.hasNext()) {
            PaintingVariantTask task = paintingIterator.next();
            if (task.ownerId().equals(ownerId)) {
                restorePainting(server, task);
                paintingIterator.remove();
            }
        }
        Iterator<ReturnedDropTask> returnedIterator = RETURNED_DROP_TASKS.iterator();
        while (returnedIterator.hasNext()) {
            ReturnedDropTask task = returnedIterator.next();
            if (task.ownerId().equals(ownerId)) {
                sendToObservers(server, task.observers(), new UncannyReturnedItemPayload(
                        task.visualId(), false, ItemStack.EMPTY,
                        task.position().x, task.position().y, task.position().z, 0));
                returnedIterator.remove();
            }
        }
        Iterator<CauldronEchoTask> cauldronIterator = CAULDRON_ECHO_TASKS.iterator();
        while (cauldronIterator.hasNext()) {
            CauldronEchoTask task = cauldronIterator.next();
            if (task.ownerId().equals(ownerId)) {
                restoreCauldronEcho(server, server == null ? null : server.getLevel(task.dimension()), task);
                cauldronIterator.remove();
            }
        }
        MISDIRECTED_ENCHANTMENT_TASKS.removeIf(task -> task.ownerId().equals(ownerId));
        EMPTY_WAKE_TASKS.removeIf(task -> task.ownerId().equals(ownerId));
        LEAF_REPLY_TASKS.removeIf(task -> task.ownerId().equals(ownerId));
        SILENT_BELL_TASKS.removeIf(task -> task.ownerId().equals(ownerId));
        ANIMAL_ATTENTION_TASKS.removeIf(task -> task.ownerId().equals(ownerId));
        VILLAGER_MEETING_TASKS.removeIf(task -> task.ownerId().equals(ownerId));
    }

    private static void clearRuntimeState(MinecraftServer server) {
        RailAndSignalAnomalySystem.clear(server);
        ContextualWorldAnomalySystem.clear(server);
        ObjectPresentationAnomalySystem.clear(server);
        if (server != null) {
            Set<UUID> owners = new HashSet<>();
            GHOST_BREAKING_TASKS.forEach(task -> owners.add(task.ownerId()));
            ORPHAN_SHADOW_TASKS.forEach(task -> owners.add(task.ownerId()));
            COLD_FURNACE_TASKS.forEach(task -> owners.add(task.ownerId()));
            ARMOR_POSE_TASKS.forEach(task -> owners.add(task.ownerId()));
            EMPTY_LEAD_TASKS.forEach(task -> owners.add(task.ownerId()));
            PAINTING_VARIANT_TASKS.forEach(task -> owners.add(task.ownerId()));
            RETURNED_DROP_TASKS.forEach(task -> owners.add(task.ownerId()));
            CAULDRON_ECHO_TASKS.forEach(task -> owners.add(task.ownerId()));
            for (UUID owner : owners) {
                cleanupOwnerTasks(server, owner);
            }
        }
        GHOST_BREAKING_TASKS.clear();
        ORPHAN_SHADOW_TASKS.clear();
        COLD_FURNACE_TASKS.clear();
        ARMOR_POSE_TASKS.clear();
        LEAF_REPLY_TASKS.clear();
        SILENT_BELL_TASKS.clear();
        ANIMAL_ATTENTION_TASKS.clear();
        VILLAGER_MEETING_TASKS.clear();
        EMPTY_LEAD_TASKS.clear();
        PAINTING_VARIANT_TASKS.clear();
        RETURNED_DROP_TASKS.clear();
        MISDIRECTED_ENCHANTMENT_TASKS.clear();
        CAULDRON_ECHO_TASKS.clear();
        EMPTY_WAKE_TASKS.clear();
        LAST_LEAF_CONTEXTS.clear();
        LAST_PICKUP_CONTEXTS.clear();
        ANIMAL_REUSE_COOLDOWNS.clear();
        ARMOR_STAND_REUSE_COOLDOWNS.clear();
    }

    private static int nextVisualId() {
        if (nextVisualId == Integer.MAX_VALUE) {
            nextVisualId = 1_500_000_000;
        }
        return nextVisualId++;
    }

    private static void debug(String message, Object... args) {
        if (UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info("[UncannyDebug/NativeAnomaly] " + message, args);
        }
    }

    private static final class GhostBreakingTask {
        private final UUID ownerId;
        private final ResourceKey<Level> dimension;
        private final int breakerId;
        private final BlockPos pos;
        private final int[] stages;
        private int stageIndex;
        private long nextStageTick;

        private GhostBreakingTask(UUID ownerId, ResourceKey<Level> dimension, int breakerId, BlockPos pos,
                                  int[] stages, int stageIndex, long nextStageTick) {
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.breakerId = breakerId;
            this.pos = pos;
            this.stages = stages;
            this.stageIndex = stageIndex;
            this.nextStageTick = nextStageTick;
        }

        private UUID ownerId() { return ownerId; }
        private ResourceKey<Level> dimension() { return dimension; }
        private int breakerId() { return breakerId; }
        private BlockPos pos() { return pos; }
        private int[] stages() { return stages; }
        private int stageIndex() { return stageIndex; }
        private long nextStageTick() { return nextStageTick; }
        private void advance(long nextTick) { stageIndex++; nextStageTick = nextTick; }
    }

    private static final class ColdFurnaceTask {
        private final UUID ownerId;
        private final ResourceKey<Level> dimension;
        private final BlockPos pos;
        private final long endTick;
        private final long cueTick;
        private final Set<UUID> observers;
        private boolean cuePlayed;

        private ColdFurnaceTask(UUID ownerId, ResourceKey<Level> dimension, BlockPos pos, long endTick,
                                long cueTick, Set<UUID> observers, boolean cuePlayed) {
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.pos = pos;
            this.endTick = endTick;
            this.cueTick = cueTick;
            this.observers = observers;
            this.cuePlayed = cuePlayed;
        }

        private UUID ownerId() { return ownerId; }
        private ResourceKey<Level> dimension() { return dimension; }
        private BlockPos pos() { return pos; }
        private long endTick() { return endTick; }
        private long cueTick() { return cueTick; }
        private Set<UUID> observers() { return observers; }
        private boolean cuePlayed() { return cuePlayed; }
        private void markCuePlayed() { cuePlayed = true; }
    }

    private static final class LeafReplyTask {
        private final UUID ownerId;
        private final ResourceKey<Level> dimension;
        private final List<BlockPos> positions;
        private final long[] pulseTicks;
        private int index;

        private LeafReplyTask(UUID ownerId, ResourceKey<Level> dimension, List<BlockPos> positions,
                              long[] pulseTicks, int index) {
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.positions = positions;
            this.pulseTicks = pulseTicks;
            this.index = index;
        }

        private UUID ownerId() { return ownerId; }
        private ResourceKey<Level> dimension() { return dimension; }
        private List<BlockPos> positions() { return positions; }
        private long[] pulseTicks() { return pulseTicks; }
        private int index() { return index; }
        private void advance() { index++; }
    }

    private record LeafContext(ResourceKey<Level> dimension, BlockPos pos, long tick) {
    }

    private record PickupContext(ResourceKey<Level> dimension, Vec3 position, ItemStack visual, long tick) {
    }

    private record OrphanShadowTask(UUID ownerId, ResourceKey<Level> dimension, int shadowId,
                                    Vec3 point, float radius, long endTick, Set<UUID> observers) {
    }

    private record ArmorPoseTask(UUID ownerId, ResourceKey<Level> dimension, UUID standUuid,
                                 int entityId, long endTick, Set<UUID> observers) {
    }

    private record SilentBellTask(UUID ownerId, ResourceKey<Level> dimension, BlockPos bellPos,
                                  BlockPos delayedSoundPos, long soundTick) {
    }

    private record AnimalAttentionTask(UUID ownerId, ResourceKey<Level> dimension, UUID animalUuid,
                                       Vec3 target, long endTick) {
    }

    private record VillagerMeetingTask(UUID ownerId, ResourceKey<Level> dimension, List<UUID> villagerUuids,
                                       Vec3 target, long startTick, long endTick) {
    }

    private record EmptyLeadTask(
            UUID ownerId,
            ResourceKey<Level> dimension,
            int visualId,
            BlockPos anchorPos,
            Vec3 anchor,
            Vec3 end,
            long minimumVisibleUntil,
            long endTick,
            Set<UUID> observers) {
    }

    public record EmptyLeadDebugSnapshot(long minimumVisibleUntil, long endTick) {
    }

    private record PaintingVariantTask(
            UUID ownerId,
            ResourceKey<Level> dimension,
            UUID paintingUuid,
            int entityId,
            Holder<PaintingVariant> original,
            long minimumVisibleUntil,
            long endTick,
            Set<UUID> observers) {
    }

    private record ReturnedDropTask(
            UUID ownerId,
            ResourceKey<Level> dimension,
            int visualId,
            Vec3 position,
            long minimumVisibleUntil,
            long endTick,
            Set<UUID> observers) {
    }

    private static final class MisdirectedEnchantmentTask {
        private final UUID ownerId;
        private final ResourceKey<Level> dimension;
        private final BlockPos tablePos;
        private final List<BlockPos> shelves;
        private final Vec3 falseTarget;
        private int pulsesRemaining;
        private long nextPulseTick;

        private MisdirectedEnchantmentTask(
                UUID ownerId,
                ResourceKey<Level> dimension,
                BlockPos tablePos,
                List<BlockPos> shelves,
                Vec3 falseTarget,
                int pulsesRemaining,
                long nextPulseTick) {
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.tablePos = tablePos;
            this.shelves = shelves;
            this.falseTarget = falseTarget;
            this.pulsesRemaining = pulsesRemaining;
            this.nextPulseTick = nextPulseTick;
        }

        private UUID ownerId() { return ownerId; }
        private ResourceKey<Level> dimension() { return dimension; }
        private BlockPos tablePos() { return tablePos; }
        private List<BlockPos> shelves() { return shelves; }
        private Vec3 falseTarget() { return falseTarget; }
        private int pulsesRemaining() { return pulsesRemaining; }
        private long nextPulseTick() { return nextPulseTick; }
        private void advance(long nextTick) { pulsesRemaining--; nextPulseTick = nextTick; }
    }

    private record CauldronEchoTask(
            UUID ownerId,
            ResourceKey<Level> dimension,
            BlockPos pos,
            long minimumVisibleUntil,
            long endTick,
            Set<UUID> observers) {
    }

    private static final class EmptyWakeTask {
        private final UUID ownerId;
        private final ResourceKey<Level> dimension;
        private final List<Vec3> points;
        private int index;
        private long nextPulseTick;

        private EmptyWakeTask(
                UUID ownerId,
                ResourceKey<Level> dimension,
                List<Vec3> points,
                int index,
                long nextPulseTick) {
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.points = points;
            this.index = index;
            this.nextPulseTick = nextPulseTick;
        }

        private UUID ownerId() { return ownerId; }
        private ResourceKey<Level> dimension() { return dimension; }
        private List<Vec3> points() { return points; }
        private int index() { return index; }
        private long nextPulseTick() { return nextPulseTick; }
        private void advance(long nextTick) { index++; nextPulseTick = nextTick; }
    }

    public record EmptyWakeDebugSnapshot(List<Vec3> points, int index, long nextPulseTick) {
        public EmptyWakeDebugSnapshot {
            points = List.copyOf(points);
        }
    }
}
