package com.eotv.echoofthevoid.event.paranoia.nativeevent;

import com.eotv.echoofthevoid.network.UncannyArrowGazePayload;
import com.eotv.echoofthevoid.network.UncannyBeaconFragmentPayload;
import com.eotv.echoofthevoid.network.UncannySuspendedFallPayload;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import com.eotv.echoofthevoid.network.UncannyStrayExperiencePayload;
import com.eotv.echoofthevoid.network.UncannyExtraHerdAnimalPayload;

/** Presentation-only transformations of existing arrows and gravity blocks. */
public final class ObjectPresentationAnomalySystem {
    private static final int OBSERVER_RADIUS = 34;
    private static final List<ArrowTask> ARROW_TASKS = new ArrayList<>();
    private static final List<SuspendedFallTask> SUSPENDED_FALL_TASKS = new ArrayList<>();
    private static final List<BeaconFragmentTask> BEACON_FRAGMENT_TASKS = new ArrayList<>();
    private static final List<StrayExperienceTask> STRAY_EXPERIENCE_TASKS = new ArrayList<>();
    private static final List<ExtraHerdAnimalTask> EXTRA_HERD_ANIMAL_TASKS = new ArrayList<>();
    private static final Map<UUID, CombatEndContext> RECENT_COMBAT_ENDS = new HashMap<>();
    private static int nextVisualId = 1_700_000_000;

    private ObjectPresentationAnomalySystem() {
    }

    public static void tick(MinecraftServer server, long now) {
        tickArrows(server, now);
        tickSuspendedFalls(server, now);
        tickBeaconFragments(server, now);
        tickStrayExperience(server, now);
        tickExtraHerdAnimals(server, now);
        RECENT_COMBAT_ENDS.entrySet().removeIf(entry -> now - entry.getValue().tick() > 1_200L);
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || !(event.getEntity() instanceof Monster)
                || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null || !UncannyWorldState.get(server).isSystemEnabled()) {
            return;
        }
        RECENT_COMBAT_ENDS.put(player.getUUID(), new CombatEndContext(
                level.dimension(), event.getEntity().position(), server.getTickCount()));
    }

    public static boolean triggerWatchingArrow(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.getEntitiesOfClass(
                Monster.class, player.getBoundingBox().inflate(24.0D), Entity::isAlive).isEmpty()) {
            return false;
        }
        List<AbstractArrow> arrows = level.getEntitiesOfClass(
                AbstractArrow.class,
                player.getBoundingBox().inflate(20.0D, 10.0D, 20.0D),
                arrow -> arrow.isAlive()
                        && (arrow.getType() == EntityType.ARROW || arrow.getType() == EntityType.SPECTRAL_ARROW)
                        && arrow.tickCount >= 100
                        && arrow.getDeltaMovement().lengthSqr() < 1.0E-6D
                        && !arrow.isPickable()
                        && !isEntityObservedByAny(level, arrow, 28.0D)
                        && ARROW_TASKS.stream().noneMatch(task -> task.arrowUuid().equals(arrow.getUUID())));
        if (arrows.isEmpty()) {
            return false;
        }
        AbstractArrow arrow = arrows.get(level.random.nextInt(arrows.size()));
        Set<UUID> observers = observerIds(level, arrow.position(), OBSERVER_RADIUS);
        if (observers.isEmpty()) {
            return false;
        }
        Vec3 direction = player.getEyePosition().subtract(arrow.position());
        float yaw = (float) (Mth.atan2(direction.x, direction.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (-(Mth.atan2(direction.y,
                Math.sqrt(direction.x * direction.x + direction.z * direction.z)) * Mth.RAD_TO_DEG));
        int duration = 100 + level.random.nextInt(101);
        sendToObservers(player.getServer(), observers,
                new UncannyArrowGazePayload(arrow.getId(), true, yaw, pitch, duration));
        ARROW_TASKS.add(new ArrowTask(
                player.getUUID(), level.dimension(), arrow.getUUID(), arrow.getId(), arrow.position(),
                observers, player.getServer().getTickCount() + 12L,
                player.getServer().getTickCount() + duration));
        return true;
    }

    public static boolean triggerSuspendedFall(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<BlockPos> blocks = findGravityBlocks(level, player.blockPosition(), 18, 36);
        blocks.removeIf(pos -> isObservedByAny(level, Vec3.atCenterOf(pos), 28.0D, 0.94D)
                || SUSPENDED_FALL_TASKS.stream().anyMatch(task ->
                task.dimension().equals(level.dimension()) && task.pos().equals(pos))
                || level.players().stream().anyMatch(observer ->
                observer.getBoundingBox().inflate(0.5D).intersects(new AABB(
                        pos.getX(), pos.getY() - 2.0D, pos.getZ(),
                        pos.getX() + 1.0D, pos.getY() + 2.0D, pos.getZ() + 1.0D))));
        if (blocks.isEmpty()) {
            return false;
        }
        BlockPos pos = blocks.get(level.random.nextInt(blocks.size()));
        BlockState state = level.getBlockState(pos);
        Set<UUID> observers = observerIds(level, Vec3.atCenterOf(pos), OBSERVER_RADIUS);
        if (observers.isEmpty()) {
            return false;
        }
        int visualId = nextVisualId();
        int duration = 44;
        sendBlockState(player.getServer(), observers, pos, Blocks.AIR.defaultBlockState());
        sendToObservers(player.getServer(), observers, new UncannySuspendedFallPayload(
                visualId, true, Block.getId(state), pos.getX(), pos.getY(), pos.getZ(), duration));
        SUSPENDED_FALL_TASKS.add(new SuspendedFallTask(
                player.getUUID(), level.dimension(), visualId, pos.immutable(), state,
                observers, player.getServer().getTickCount() + duration));
        return true;
    }

    public static boolean triggerBeaconFragment(ServerPlayer player, boolean debugImmediate) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        UncannyWorldState worldState = UncannyWorldState.get(server);
        if (!debugImmediate && worldState.hasBeaconFragmentOccurred()) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        BlockPos origin = findBeaconFragmentOrigin(level, player);
        if (origin == null) {
            return false;
        }
        Vec3 center = Vec3.atBottomCenterOf(origin);
        Set<UUID> observers = observerIds(level, center, 96);
        if (observers.isEmpty()) {
            return false;
        }
        int visualId = nextVisualId();
        int height = 12 + level.random.nextInt(13);
        int duration = 100 + level.random.nextInt(81);
        int color = 0xFFD9E6FF;
        sendToObservers(server, observers, new UncannyBeaconFragmentPayload(
                visualId, true, center.x, center.y, center.z, height, color, duration));
        long now = server.getTickCount();
        BEACON_FRAGMENT_TASKS.add(new BeaconFragmentTask(
                player.getUUID(), level.dimension(), visualId, center, height, color,
                observers, now + 20L, now + duration));
        if (!debugImmediate) {
            worldState.markBeaconFragmentOccurred();
        }
        return true;
    }

    public static boolean triggerStrayExperience(ServerPlayer player, boolean debugImmediate) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        long now = server.getTickCount();
        CombatEndContext context = RECENT_COMBAT_ENDS.get(player.getUUID());
        Vec3 start;
        if (debugImmediate && (context == null || !context.dimension().equals(level.dimension()))) {
            start = player.position().add(player.getLookAngle().scale(3.5D)).add(0.0D, 0.35D, 0.0D);
        } else {
            if (context == null || !context.dimension().equals(level.dimension())
                    || now - context.tick() < 40L || now - context.tick() > 1_200L
                    || player.position().distanceToSqr(context.position()) > 32.0D * 32.0D) {
                return false;
            }
            start = context.position().add(0.0D, 0.35D, 0.0D);
        }
        if (!level.getEntitiesOfClass(
                Monster.class, player.getBoundingBox().inflate(16.0D), Entity::isAlive).isEmpty()
                || !level.getEntitiesOfClass(
                ExperienceOrb.class, AABB.ofSize(start, 8.0D, 5.0D, 8.0D), Entity::isAlive).isEmpty()) {
            return false;
        }
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        Vec3 target = start.add(Math.cos(angle) * 2.4D, 0.7D, Math.sin(angle) * 2.4D);
        if (!level.noCollision(AABB.ofSize(target, 0.45D, 0.45D, 0.45D))
                || !level.getEntities(null, AABB.ofSize(target, 1.0D, 1.0D, 1.0D)).isEmpty()) {
            return false;
        }
        Set<UUID> observers = observerIds(level, start, OBSERVER_RADIUS);
        if (observers.isEmpty()) {
            return false;
        }
        int visualId = nextVisualId();
        int count = 2 + level.random.nextInt(2);
        int duration = 50 + level.random.nextInt(21);
        sendToObservers(server, observers, new UncannyStrayExperiencePayload(
                visualId, true, start.x, start.y, start.z,
                target.x, target.y, target.z, count, duration));
        STRAY_EXPERIENCE_TASKS.add(new StrayExperienceTask(
                player.getUUID(), level.dimension(), visualId, start, target,
                count, observers, now + duration));
        if (!debugImmediate) {
            RECENT_COMBAT_ENDS.remove(player.getUUID());
        }
        return true;
    }

    public static boolean triggerExtraHerdAnimal(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<Animal> nearby = level.getEntitiesOfClass(
                Animal.class,
                player.getBoundingBox().inflate(32.0D, 12.0D, 32.0D),
                ObjectPresentationAnomalySystem::isEligibleHerdAnimal);
        Map<EntityType<?>, List<Animal>> groups = new HashMap<>();
        for (Animal animal : nearby) {
            groups.computeIfAbsent(animal.getType(), ignored -> new ArrayList<>()).add(animal);
        }
        List<List<Animal>> herds = groups.values().stream()
                .filter(group -> group.size() >= 3)
                .filter(group -> group.stream().anyMatch(anchor ->
                        group.stream().filter(other -> other.distanceToSqr(anchor) <= 10.0D * 10.0D).count() >= 3))
                .toList();
        if (herds.isEmpty()) {
            return false;
        }
        List<Animal> herd = herds.get(level.random.nextInt(herds.size()));
        Animal anchor = herd.get(level.random.nextInt(herd.size()));
        Vec3 fakePosition = findHerdCompanionPosition(level, anchor);
        if (fakePosition == null || isObservedByAny(level, fakePosition, 36.0D, 0.94D)) {
            return false;
        }
        Set<UUID> observers = observerIds(level, fakePosition, 48);
        if (observers.isEmpty()) {
            return false;
        }
        int visualId = nextVisualId();
        int duration = 160 + level.random.nextInt(141);
        Vec3 offset = fakePosition.subtract(anchor.position());
        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(anchor.getType()).toString();
        sendToObservers(player.getServer(), observers, new UncannyExtraHerdAnimalPayload(
                visualId, true, anchor.getId(), typeId,
                offset.x, offset.y, offset.z, duration));
        List<UUID> herdIds = herd.stream().map(Entity::getUUID).toList();
        EXTRA_HERD_ANIMAL_TASKS.add(new ExtraHerdAnimalTask(
                player.getUUID(), level.dimension(), visualId, anchor.getUUID(), anchor.getId(),
                typeId, offset, herdIds, observers, player.getServer().getTickCount() + duration));
        return true;
    }

    public static void clearForOwner(MinecraftServer server, UUID ownerId) {
        Iterator<ArrowTask> arrowIterator = ARROW_TASKS.iterator();
        while (arrowIterator.hasNext()) {
            ArrowTask task = arrowIterator.next();
            if (task.ownerId().equals(ownerId)) {
                clearArrow(server, task);
                arrowIterator.remove();
            }
        }
        Iterator<SuspendedFallTask> fallIterator = SUSPENDED_FALL_TASKS.iterator();
        while (fallIterator.hasNext()) {
            SuspendedFallTask task = fallIterator.next();
            if (task.ownerId().equals(ownerId)) {
                clearSuspendedFall(server, task);
                fallIterator.remove();
            }
        }
        Iterator<BeaconFragmentTask> beaconIterator = BEACON_FRAGMENT_TASKS.iterator();
        while (beaconIterator.hasNext()) {
            BeaconFragmentTask task = beaconIterator.next();
            if (task.ownerId().equals(ownerId)) {
                clearBeaconFragment(server, task);
                beaconIterator.remove();
            }
        }
        Iterator<StrayExperienceTask> experienceIterator = STRAY_EXPERIENCE_TASKS.iterator();
        while (experienceIterator.hasNext()) {
            StrayExperienceTask task = experienceIterator.next();
            if (task.ownerId().equals(ownerId)) {
                clearStrayExperience(server, task);
                experienceIterator.remove();
            }
        }
        RECENT_COMBAT_ENDS.remove(ownerId);
        Iterator<ExtraHerdAnimalTask> herdIterator = EXTRA_HERD_ANIMAL_TASKS.iterator();
        while (herdIterator.hasNext()) {
            ExtraHerdAnimalTask task = herdIterator.next();
            if (task.ownerId().equals(ownerId)) {
                clearExtraHerdAnimal(server, task);
                herdIterator.remove();
            }
        }
    }

    public static void clear(MinecraftServer server) {
        if (server != null) {
            ARROW_TASKS.forEach(task -> clearArrow(server, task));
            SUSPENDED_FALL_TASKS.forEach(task -> clearSuspendedFall(server, task));
            BEACON_FRAGMENT_TASKS.forEach(task -> clearBeaconFragment(server, task));
            STRAY_EXPERIENCE_TASKS.forEach(task -> clearStrayExperience(server, task));
            EXTRA_HERD_ANIMAL_TASKS.forEach(task -> clearExtraHerdAnimal(server, task));
        }
        ARROW_TASKS.clear();
        SUSPENDED_FALL_TASKS.clear();
        BEACON_FRAGMENT_TASKS.clear();
        STRAY_EXPERIENCE_TASKS.clear();
        RECENT_COMBAT_ENDS.clear();
        EXTRA_HERD_ANIMAL_TASKS.clear();
    }

    private static void tickArrows(MinecraftServer server, long now) {
        Iterator<ArrowTask> iterator = ARROW_TASKS.iterator();
        while (iterator.hasNext()) {
            ArrowTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            Entity raw = level == null ? null : level.getEntity(task.arrowUuid());
            AbstractArrow arrow = raw instanceof AbstractArrow value ? value : null;
            boolean clear = arrow == null || owner == null || owner.serverLevel() != level
                    || now >= task.endTick()
                    || arrow.position().distanceToSqr(task.originalPosition()) > 0.04D;
            if (!clear && now >= task.minimumVisibleUntil()) {
                clear = level.players().stream().anyMatch(observer -> observer.isAlive()
                        && observer.position().distanceToSqr(arrow.position()) <= 2.0D * 2.0D);
            }
            if (clear) {
                clearArrow(server, task);
                iterator.remove();
            }
        }
    }

    private static void tickSuspendedFalls(MinecraftServer server, long now) {
        Iterator<SuspendedFallTask> iterator = SUSPENDED_FALL_TASKS.iterator();
        while (iterator.hasNext()) {
            SuspendedFallTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            if (level == null || !level.hasChunkAt(task.pos()) || now >= task.endTick()
                    || !level.getBlockState(task.pos()).equals(task.originalState())) {
                clearSuspendedFall(server, task);
                iterator.remove();
            }
        }
    }

    private static void tickBeaconFragments(MinecraftServer server, long now) {
        Iterator<BeaconFragmentTask> iterator = BEACON_FRAGMENT_TASKS.iterator();
        while (iterator.hasNext()) {
            BeaconFragmentTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId());
            boolean clear = level == null || owner == null || owner.serverLevel() != level
                    || now >= task.endTick();
            if (!clear && now >= task.minimumVisibleUntil()) {
                clear = isClearlyObservedByAny(level, task.position(), 96.0D);
            }
            if (clear) {
                clearBeaconFragment(server, task);
                iterator.remove();
            }
        }
    }

    private static void tickStrayExperience(MinecraftServer server, long now) {
        Iterator<StrayExperienceTask> iterator = STRAY_EXPERIENCE_TASKS.iterator();
        while (iterator.hasNext()) {
            StrayExperienceTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            if (level == null || now >= task.endTick()) {
                clearStrayExperience(server, task);
                iterator.remove();
            }
        }
    }

    private static void tickExtraHerdAnimals(MinecraftServer server, long now) {
        Iterator<ExtraHerdAnimalTask> iterator = EXTRA_HERD_ANIMAL_TASKS.iterator();
        while (iterator.hasNext()) {
            ExtraHerdAnimalTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            Entity rawAnchor = level == null ? null : level.getEntity(task.anchorUuid());
            Animal anchor = rawAnchor instanceof Animal animal ? animal : null;
            boolean clear = level == null || anchor == null || !isEligibleHerdAnimal(anchor)
                    || now >= task.endTick();
            if (!clear) {
                long together = task.herdIds().stream()
                        .map(level::getEntity)
                        .filter(entity -> entity instanceof Animal animal
                                && isEligibleHerdAnimal(animal)
                                && entity.getType() == anchor.getType()
                                && entity.distanceToSqr(anchor) <= 10.0D * 10.0D)
                        .count();
                Vec3 fakePosition = anchor.position().add(task.offset());
                clear = together < 3
                        || level.players().stream().anyMatch(observer -> observer.isAlive()
                        && observer.position().distanceToSqr(fakePosition) <= 5.0D * 5.0D)
                        || !level.noCollision(AABB.ofSize(
                        fakePosition, anchor.getBbWidth(), anchor.getBbHeight(), anchor.getBbWidth()));
            }
            if (clear) {
                clearExtraHerdAnimal(server, task);
                iterator.remove();
            }
        }
    }

    private static void clearArrow(MinecraftServer server, ArrowTask task) {
        sendToObservers(server, task.observers(),
                new UncannyArrowGazePayload(task.entityId(), false, 0.0F, 0.0F, 0));
    }

    private static void clearSuspendedFall(MinecraftServer server, SuspendedFallTask task) {
        ServerLevel level = server == null ? null : server.getLevel(task.dimension());
        if (level != null && level.hasChunkAt(task.pos())) {
            sendBlockState(server, task.observers(), task.pos(), level.getBlockState(task.pos()));
        }
        sendToObservers(server, task.observers(), new UncannySuspendedFallPayload(
                task.visualId(), false, 0,
                task.pos().getX(), task.pos().getY(), task.pos().getZ(), 0));
    }

    private static void clearBeaconFragment(MinecraftServer server, BeaconFragmentTask task) {
        sendToObservers(server, task.observers(), new UncannyBeaconFragmentPayload(
                task.visualId(), false,
                task.position().x, task.position().y, task.position().z,
                task.height(), task.color(), 0));
    }

    private static void clearStrayExperience(MinecraftServer server, StrayExperienceTask task) {
        sendToObservers(server, task.observers(), new UncannyStrayExperiencePayload(
                task.visualId(), false,
                task.start().x, task.start().y, task.start().z,
                task.target().x, task.target().y, task.target().z,
                task.orbCount(), 0));
    }

    private static void clearExtraHerdAnimal(MinecraftServer server, ExtraHerdAnimalTask task) {
        sendToObservers(server, task.observers(), new UncannyExtraHerdAnimalPayload(
                task.visualId(), false, task.anchorEntityId(), task.entityTypeId(),
                task.offset().x, task.offset().y, task.offset().z, 0));
    }

    private static int nextVisualId() {
        if (nextVisualId == Integer.MAX_VALUE) {
            nextVisualId = 1_700_000_000;
        }
        return nextVisualId++;
    }

    private static List<BlockPos> findGravityBlocks(
            ServerLevel level, BlockPos center, int radius, int limit) {
        List<BlockPos> result = new ArrayList<>();
        for (int y = -7; y <= 7 && result.size() < limit; y++) {
            for (int x = -radius; x <= radius && result.size() < limit; x++) {
                for (int z = -radius; z <= radius && result.size() < limit; z++) {
                    if (x * x + z * z < 36 || x * x + z * z > radius * radius) {
                        continue;
                    }
                    BlockPos pos = center.offset(x, y, z);
                    if (!level.hasChunkAt(pos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    BlockState support = level.getBlockState(pos.below());
                    VoxelShape supportShape = support.getCollisionShape(level, pos.below());
                    if ((state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL))
                            && !FallingBlock.isFree(support)
                            && !supportShape.isEmpty()
                            && supportShape.max(Direction.Axis.Y) <= 0.55D) {
                        result.add(pos.immutable());
                    }
                }
            }
        }
        return result;
    }

    private static BlockPos findBeaconFragmentOrigin(ServerLevel level, ServerPlayer player) {
        BlockPos center = player.blockPosition();
        for (int attempt = 0; attempt < 64; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            int distance = 28 + level.random.nextInt(37);
            int x = center.getX() + Mth.floor(Math.cos(angle) * distance);
            int z = center.getZ() + Mth.floor(Math.sin(angle) * distance);
            BlockPos chunkProbe = new BlockPos(x, level.getSeaLevel(), z);
            if (!level.hasChunkAt(chunkProbe)) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos origin = new BlockPos(x, y, z);
            if (y <= level.getMinBuildHeight() + 4 || y + 26 >= level.getMaxBuildHeight()
                    || !level.canSeeSky(origin)
                    || !level.getBlockState(origin).isAir()
                    || level.getBlockState(origin.below()).isAir()
                    || isObservedByAny(level, Vec3.atBottomCenterOf(origin), 96.0D, 0.94D)
                    || containsBeaconNearby(level, origin, 16, 12)) {
                continue;
            }
            boolean clearColumn = true;
            for (int dy = 0; dy <= 26; dy++) {
                BlockPos beamPos = origin.above(dy);
                if (!level.hasChunkAt(beamPos) || !level.getBlockState(beamPos).isAir()) {
                    clearColumn = false;
                    break;
                }
            }
            if (clearColumn) {
                return origin;
            }
        }
        return null;
    }

    private static boolean containsBeaconNearby(ServerLevel level, BlockPos origin, int radius, int verticalRadius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -verticalRadius; y <= verticalRadius; y++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.BEACON)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isEligibleHerdAnimal(Animal animal) {
        EntityType<?> type = animal.getType();
        return (type == EntityType.COW || type == EntityType.PIG
                || type == EntityType.SHEEP || type == EntityType.CHICKEN)
                && animal.isAlive() && !animal.isBaby() && !animal.hasCustomName()
                && !animal.isInLove() && !animal.isLeashed()
                && !animal.isPassenger() && !animal.isVehicle();
    }

    private static Vec3 findHerdCompanionPosition(ServerLevel level, Animal anchor) {
        double startAngle = level.random.nextDouble() * Math.PI * 2.0D;
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = startAngle + attempt * Math.PI / 4.0D;
            double distance = 1.35D + (attempt % 2) * 0.35D;
            double x = anchor.getX() + Math.cos(angle) * distance;
            double z = anchor.getZ() + Math.sin(angle) * distance;
            for (int dy = -2; dy <= 2; dy++) {
                BlockPos feet = BlockPos.containing(x, anchor.getY() + dy, z);
                if (!level.hasChunkAt(feet)
                        || !level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) {
                    continue;
                }
                Vec3 position = new Vec3(x, feet.getY(), z);
                AABB bounds = AABB.ofSize(
                        position.add(0.0D, anchor.getBbHeight() * 0.5D, 0.0D),
                        anchor.getBbWidth(), anchor.getBbHeight(), anchor.getBbWidth());
                if (level.noCollision(bounds) && level.getEntities(null, bounds.inflate(0.15D)).isEmpty()) {
                    return position;
                }
            }
        }
        return null;
    }

    private static boolean isEntityObservedByAny(ServerLevel level, Entity entity, double radius) {
        return isObservedByAny(level, entity.getBoundingBox().getCenter(), radius, 0.94D);
    }

    private static boolean isObservedByAny(ServerLevel level, Vec3 point, double radius, double threshold) {
        for (ServerPlayer observer : level.players()) {
            if (!observer.isAlive() || observer.isSpectator()) {
                continue;
            }
            Vec3 delta = point.subtract(observer.getEyePosition());
            if (delta.lengthSqr() <= radius * radius
                    && observer.getLookAngle().normalize().dot(delta.normalize()) >= threshold) {
                return true;
            }
        }
        return false;
    }

    private static boolean isClearlyObservedByAny(ServerLevel level, Vec3 point, double radius) {
        for (ServerPlayer observer : level.players()) {
            if (!observer.isAlive() || observer.isSpectator()) {
                continue;
            }
            Vec3 delta = point.subtract(observer.getEyePosition());
            if (delta.lengthSqr() > radius * radius
                    || observer.getLookAngle().normalize().dot(delta.normalize()) < 0.975D) {
                continue;
            }
            HitResult hit = level.clip(new ClipContext(
                    observer.getEyePosition(), point,
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, observer));
            if (hit.getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    private static Set<UUID> observerIds(ServerLevel level, Vec3 center, double radius) {
        Set<UUID> result = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator()
                    && player.position().distanceToSqr(center) <= radius * radius) {
                result.add(player.getUUID());
            }
        }
        return Set.copyOf(result);
    }

    private static void sendToObservers(
            MinecraftServer server, Set<UUID> observers, CustomPacketPayload payload) {
        if (server == null) {
            return;
        }
        for (UUID observerId : observers) {
            ServerPlayer observer = server.getPlayerList().getPlayer(observerId);
            if (observer != null) {
                PacketDistributor.sendToPlayer(observer, payload);
            }
        }
    }

    private static void sendBlockState(
            MinecraftServer server, Set<UUID> observers, BlockPos pos, BlockState state) {
        if (server == null) {
            return;
        }
        for (UUID observerId : observers) {
            ServerPlayer observer = server.getPlayerList().getPlayer(observerId);
            if (observer != null) {
                observer.connection.send(new ClientboundBlockUpdatePacket(pos, state));
            }
        }
    }

    private record ArrowTask(UUID ownerId, ResourceKey<Level> dimension, UUID arrowUuid,
                             int entityId, Vec3 originalPosition, Set<UUID> observers,
                             long minimumVisibleUntil, long endTick) {
    }

    private record SuspendedFallTask(UUID ownerId, ResourceKey<Level> dimension, int visualId,
                                     BlockPos pos, BlockState originalState, Set<UUID> observers,
                                     long endTick) {
    }

    private record BeaconFragmentTask(UUID ownerId, ResourceKey<Level> dimension, int visualId,
                                      Vec3 position, int height, int color, Set<UUID> observers,
                                      long minimumVisibleUntil, long endTick) {
    }

    private record StrayExperienceTask(UUID ownerId, ResourceKey<Level> dimension, int visualId,
                                       Vec3 start, Vec3 target, int orbCount, Set<UUID> observers,
                                       long endTick) {
    }

    private record CombatEndContext(ResourceKey<Level> dimension, Vec3 position, long tick) {
    }

    private record ExtraHerdAnimalTask(UUID ownerId, ResourceKey<Level> dimension, int visualId,
                                       UUID anchorUuid, int anchorEntityId, String entityTypeId,
                                       Vec3 offset, List<UUID> herdIds, Set<UUID> observers,
                                       long endTick) {
    }
}
