package com.eotv.echoofthevoid.event.special;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyApprovedSpecialEntity;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.TintedGlassBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Context memory and safe spawning for the approved Special family. */
public final class ApprovedSpecialSystem {
    private static final Map<ResourceKey<Level>, ArrayDeque<SoundMemory>> SOUNDS = new HashMap<>();
    private static final Map<ResourceKey<Level>, ArrayDeque<CombatMemory>> COMBATS = new HashMap<>();
    private static final int MAX_MEMORIES = 64;

    private ApprovedSpecialSystem() {
    }

    public static void onSoundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        recordSound(level, event.getPosition(), event.getSound());
    }

    public static void onSoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        recordSound(level, event.getEntity().position(), event.getSound());
    }

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || event.getEntity() instanceof UncannyEntityMarker
                || event.getSource().getEntity() instanceof UncannyEntityMarker) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker == null || attacker == event.getEntity()) {
            return;
        }
        ArrayDeque<CombatMemory> memories = COMBATS.computeIfAbsent(level.dimension(), ignored -> new ArrayDeque<>());
        memories.addLast(new CombatMemory(
                event.getEntity().getId(), attacker.getId(), event.getEntity().position(), level.getGameTime()));
        trim(memories);
    }

    public static boolean spawn(ServerPlayer player, String rawId, boolean debug) {
        ApprovedSpecialCatalog.Definition definition = ApprovedSpecialCatalog.byId(rawId);
        if (player == null || definition == null) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        SpawnContext context = findContext(level, player, definition.id(), debug);
        if (context == null) {
            return false;
        }

        EntityType<UncannyApprovedSpecialEntity> type = UncannyEntityRegistry.approvedSpecialById(definition.id());
        if (type == null) {
            return false;
        }
        UncannyApprovedSpecialEntity entity = type.create(level);
        if (entity == null) {
            return false;
        }
        entity.moveTo(context.position().x, context.position().y, context.position().z, player.getYRot(), 0.0F);
        entity.setup(player, context.anchor(), context.relatedEntityId());
        if (debug) {
            entity.addTag("eotv_dev_spawned");
        }
        boolean added = level.addFreshEntity(entity);

        if (added && !debug && "mourner".equals(definition.id())) {
            UncannyWorldState.get(level.getServer()).markMournerUsed(player.getUUID());
        }
        return added;
    }

    public static boolean spawnForDebug(ServerPlayer player, String id) {
        return spawn(player, id, true);
    }

    public static DebugSpawnResult spawnForDebugDetailed(ServerPlayer player, String id) {
        boolean success = spawnForDebug(player, id);
        if (success) {
            return new DebugSpawnResult(true, "Special spawned with its required gameplay context.");
        }
        return new DebugSpawnResult(false, debugRequirement(id));
    }

    public static String debugRequirement(String rawId) {
        String id = rawId == null ? "" : rawId.trim().toLowerCase(Locale.ROOT);
        return switch (id) {
            case "ferryman" -> "Ferryman? requires the target player to be riding a boat over at least four blocks of water.";
            case "listener" -> "Listener? requires a recent physical door, chest, barrel, piston, lever, bell, furnace or anvil sound nearby.";
            case "bystander" -> "Bystander? requires a real nearby fight that dealt or received damage during the last five seconds.";
            case "doubler" -> "Doubler? requires a real glass, pane, fence or iron-bar separation near the target player.";
            case "surveyor" -> "Surveyor? requires a real door or glass window with a collision-safe outside position.";
            default -> "The Special's required terrain or safe placement was not found near the target player.";
        };
    }

    public static SoundMemory latestPhysicalSound(
            ServerLevel level,
            Vec3 from,
            long afterTick,
            double radius) {
        ArrayDeque<SoundMemory> memories = SOUNDS.get(level.dimension());
        if (memories == null) {
            return null;
        }
        pruneSounds(level, memories);
        double maximumDistance = radius * radius;
        Iterator<SoundMemory> iterator = memories.descendingIterator();
        while (iterator.hasNext()) {
            SoundMemory memory = iterator.next();
            if (memory.tick() > afterTick && memory.position().distanceToSqr(from) <= maximumDistance) {
                return memory;
            }
        }
        return null;
    }

    public static CombatMemory latestCombat(ServerLevel level, Vec3 from, double radius) {
        ArrayDeque<CombatMemory> memories = COMBATS.get(level.dimension());
        if (memories == null) {
            return null;
        }
        while (!memories.isEmpty() && level.getGameTime() - memories.peekFirst().tick() > 100L) {
            memories.removeFirst();
        }
        double maximumDistance = radius * radius;
        Iterator<CombatMemory> iterator = memories.descendingIterator();
        while (iterator.hasNext()) {
            CombatMemory memory = iterator.next();
            if (memory.position().distanceToSqr(from) <= maximumDistance) {
                return memory;
            }
        }
        return null;
    }

    public static boolean hasRecentPhysicalSound(ServerLevel level, Vec3 from, double radius) {
        return latestPhysicalSound(level, from, Long.MIN_VALUE, radius) != null;
    }

    public static boolean hasRecentCombat(ServerLevel level, Vec3 from, double radius) {
        return latestCombat(level, from, radius) != null;
    }

    private static void recordSound(ServerLevel level, Vec3 position, Holder<SoundEvent> sound) {
        if (sound == null || !isTrackedPhysicalSound(sound)) {
            return;
        }
        ArrayDeque<SoundMemory> memories = SOUNDS.computeIfAbsent(level.dimension(), ignored -> new ArrayDeque<>());
        memories.addLast(new SoundMemory(position, level.getGameTime()));
        trim(memories);
    }

    private static boolean isTrackedPhysicalSound(Holder<SoundEvent> sound) {
        String path = sound.unwrapKey()
                .map(key -> key.location().getPath().toLowerCase(Locale.ROOT))
                .orElse("");
        return path.contains("door")
                || path.contains("chest")
                || path.contains("barrel")
                || path.contains("piston")
                || path.contains("lever")
                || path.contains("button")
                || path.contains("pressure_plate")
                || path.contains("bell")
                || path.contains("furnace")
                || path.contains("anvil")
                || path.contains("trapdoor");
    }

    private static void pruneSounds(ServerLevel level, ArrayDeque<SoundMemory> memories) {
        while (!memories.isEmpty() && level.getGameTime() - memories.peekFirst().tick() > 220L) {
            memories.removeFirst();
        }
    }

    private static <T> void trim(ArrayDeque<T> memories) {
        while (memories.size() > MAX_MEMORIES) {
            memories.removeFirst();
        }
    }

    private static SpawnContext findContext(
            ServerLevel level,
            ServerPlayer player,
            String id,
            boolean debug) {
        return switch (id) {
            case "surveyor" -> findSurveyorContext(level, player, debug);
            case "mourner" -> findMournerContext(level, player, debug);
            case "doubler" -> findDoublerContext(level, player, debug);
            case "ferryman" -> findFerrymanContext(level, player, debug);
            case "listener" -> findListenerContext(level, player, debug);
            case "bystander" -> findBystanderContext(level, player, debug);
            default -> null;
        };
    }

    private static SpawnContext findSurveyorContext(ServerLevel level, ServerPlayer player, boolean debug) {
        BlockPos baseAnchor = player.getRespawnPosition();
        if (baseAnchor == null || player.getRespawnDimension() != level.dimension()) {
            baseAnchor = level.getSharedSpawnPos();
        }
        if (!debug && player.blockPosition().distSqr(baseAnchor) > 48L * 48L) {
            return null;
        }
        BlockPos searchCenter = debug ? player.blockPosition() : baseAnchor;
        SurveyorPlacement placement = findSurveyorPlacement(level, searchCenter, player.position());
        if (placement == null && debug && !searchCenter.equals(baseAnchor)) {
            placement = findSurveyorPlacement(level, baseAnchor, player.position());
        }
        return placement == null
                ? null
                : new SpawnContext(placement.spawnPosition(), placement.featurePos());
    }

    private static SurveyorPlacement findSurveyorPlacement(
            ServerLevel level,
            BlockPos searchCenter,
            Vec3 baseInteriorReference) {
        List<BlockPos> features = new ArrayList<>();
        for (BlockPos mutable : BlockPos.betweenClosed(
                searchCenter.offset(-18, -8, -18),
                searchCenter.offset(18, 8, 18))) {
            if (!level.hasChunkAt(mutable)) {
                continue;
            }
            BlockState state = level.getBlockState(mutable);
            if (isSurveyorFeature(state)) {
                features.add(mutable.immutable());
            }
        }
        features.sort(Comparator.comparingDouble(searchCenter::distSqr));

        for (BlockPos feature : features) {
            Vec3 outsideStand = findSurveyorStandPosition(
                    level, feature, baseInteriorReference, false);
            if (outsideStand == null) {
                continue;
            }
            Vec3 outward = outsideStand.subtract(Vec3.atCenterOf(feature));
            outward = new Vec3(outward.x, 0.0D, outward.z);
            if (outward.lengthSqr() < 0.25D) {
                continue;
            }
            outward = outward.normalize();
            // Start close enough for the approach to remain readable and for Vanilla pathfinding
            // to keep the selected door/window inside its local path. The previous eight-block
            // offset could place Surveyor? beyond a small base platform and leave it immobile.
            for (int distance = 5; distance >= 3; distance--) {
                BlockPos probe = BlockPos.containing(outsideStand.add(outward.scale(distance)));
                BlockPos spawn = findGroundNear(level, probe, 4);
                if (spawn != null) {
                    return new SurveyorPlacement(Vec3.atBottomCenterOf(spawn), feature);
                }
            }
            return new SurveyorPlacement(outsideStand, feature);
        }
        return null;
    }

    /** Returns a collision-safe position directly beside the selected door or window. */
    public static Vec3 findSurveyorStandPosition(
            ServerLevel level,
            BlockPos feature,
            Vec3 sideReference) {
        return findSurveyorStandPosition(level, feature, sideReference, true);
    }

    private static Vec3 findSurveyorStandPosition(
            ServerLevel level,
            BlockPos feature,
            Vec3 sideReference,
            boolean preferNearestSide) {
        if (level == null || feature == null || sideReference == null
                || !level.hasChunkAt(feature)
                || !isSurveyorFeature(level.getBlockState(feature))) {
            return null;
        }
        Vec3 best = null;
        double bestScore = preferNearestSide ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int yOffset : new int[] {0, -1, 1}) {
                BlockPos feet = feature.relative(direction).offset(0, yOffset, 0);
                if (!isSafeSurveyorStand(level, feet)) {
                    continue;
                }
                Vec3 candidate = Vec3.atBottomCenterOf(feet);
                double score = candidate.distanceToSqr(sideReference);
                if ((preferNearestSide && score < bestScore)
                        || (!preferNearestSide && score > bestScore)) {
                    best = candidate;
                    bestScore = score;
                }
            }
        }
        return best;
    }

    private static boolean isSurveyorFeature(BlockState state) {
        if (state.getBlock() instanceof DoorBlock) {
            return !state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    || state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
        }
        return state.is(Blocks.GLASS)
                || state.getBlock() instanceof StainedGlassBlock
                || state.getBlock() instanceof TintedGlassBlock
                || state.getBlock() instanceof StainedGlassPaneBlock
                || state.is(Blocks.GLASS_PANE);
    }

    private static boolean isSafeSurveyorStand(ServerLevel level, BlockPos feet) {
        if (!level.hasChunkAt(feet)
                || !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
            return false;
        }
        BlockPos support = feet.below();
        return level.getBlockState(support).isCollisionShapeFullBlock(level, support);
    }

    private static SpawnContext findMournerContext(ServerLevel level, ServerPlayer player, boolean debug) {
        UncannyWorldState worldState = UncannyWorldState.get(level.getServer());
        if (!debug && worldState.hasMournerOccurred()) {
            return null;
        }
        UncannyWorldState.DeathSite site = worldState.getDeathSite(player.getUUID());
        if (site == null) {
            if (!debug) {
                return null;
            }
            BlockPos fallback = findGroundOnRing(level, player.blockPosition(), player, 5, 9, false);
            return fallback == null ? null : new SpawnContext(Vec3.atBottomCenterOf(fallback), fallback);
        }
        if (site.mournerUsed() && !debug) {
            return null;
        }
        if (!site.dimension().equals(level.dimension().location().toString())) {
            return null;
        }
        BlockPos death = site.position();
        if (!debug && player.blockPosition().distSqr(death) > 96L * 96L) {
            return null;
        }
        BlockPos safe = findGroundNear(level, death, 3);
        return safe == null ? null : new SpawnContext(Vec3.atBottomCenterOf(safe), death);
    }

    private static SpawnContext findDoublerContext(ServerLevel level, ServerPlayer player, boolean debug) {
        BlockPos origin = player.blockPosition();
        for (BlockPos barrier : BlockPos.betweenClosed(origin.offset(-8, -3, -8), origin.offset(8, 4, 8))) {
            BlockState state = level.getBlockState(barrier);
            if (!(state.getBlock() instanceof FenceBlock)
                    && !(state.getBlock() instanceof IronBarsBlock)
                    && !(state.getBlock() instanceof StainedGlassBlock)
                    && !(state.getBlock() instanceof StainedGlassPaneBlock)
                    && !(state.getBlock() instanceof TintedGlassBlock)
                    && !state.is(Blocks.GLASS)
                    && !state.is(Blocks.GLASS_PANE)) {
                continue;
            }
            Vec3 outward = Vec3.atCenterOf(barrier).subtract(player.position());
            if (outward.horizontalDistanceSqr() < 0.5D) {
                continue;
            }
            outward = new Vec3(outward.x, 0.0D, outward.z).normalize();
            BlockPos candidate = BlockPos.containing(Vec3.atCenterOf(barrier).add(outward.scale(2.2D)));
            BlockPos safe = findGroundNear(level, candidate, 2);
            if (safe != null && player.blockPosition().distSqr(safe) >= 5L * 5L) {
                return new SpawnContext(Vec3.atBottomCenterOf(safe), barrier);
            }
        }
        return null;
    }

    private static SpawnContext findFerrymanContext(ServerLevel level, ServerPlayer player, boolean debug) {
        Boat boat = player.getVehicle() instanceof Boat current ? current : null;
        if (boat == null || !isEligibleFerrymanBoat(level, boat)) {
            return null;
        }
        Vec3 position = boat.position().add(0.0D, ApprovedSpecialBehaviorRules.FERRYMAN_VERTICAL_OFFSET, 0.0D);
        return new SpawnContext(position, boat.blockPosition(), boat.getUUID());
    }

    private static SpawnContext findListenerContext(ServerLevel level, ServerPlayer player, boolean debug) {
        SoundMemory memory = latestPhysicalSound(level, player.position(), Long.MIN_VALUE, 40.0D);
        if (memory == null) {
            return null;
        }
        BlockPos anchor = BlockPos.containing(memory.position());
        BlockPos position = findGroundOnRing(level, anchor, player, 8, 16, true);
        return position == null ? null : new SpawnContext(Vec3.atBottomCenterOf(position), anchor);
    }

    private static SpawnContext findBystanderContext(ServerLevel level, ServerPlayer player, boolean debug) {
        CombatMemory combat = latestCombat(level, player.position(), 42.0D);
        if (combat == null) {
            return null;
        }
        BlockPos anchor = BlockPos.containing(combat.position());
        BlockPos position = findGroundOnRing(level, anchor, player, 10, 18, false);
        return position == null ? null : new SpawnContext(Vec3.atBottomCenterOf(position), anchor);
    }

    private static BlockPos findGroundOnRing(
            ServerLevel level,
            BlockPos center,
            ServerPlayer observer,
            int minimum,
            int maximum,
            boolean preferUnseen) {
        for (int attempt = 0; attempt < 48; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            int distance = minimum + level.random.nextInt(Math.max(1, maximum - minimum + 1));
            BlockPos probe = center.offset(
                    Mth.floor(Math.cos(angle) * distance), 0,
                    Mth.floor(Math.sin(angle) * distance));
            BlockPos safe = findGroundNear(level, probe, 5);
            if (safe == null) {
                continue;
            }
            if (preferUnseen && isClearlyObserved(observer, safe)) {
                continue;
            }
            return safe;
        }
        return null;
    }

    private static BlockPos findGroundNear(ServerLevel level, BlockPos probe, int verticalRange) {
        int minY = Math.max(level.getMinBuildHeight() + 1, probe.getY() - verticalRange);
        int maxY = Math.min(level.getMaxBuildHeight() - 3, probe.getY() + verticalRange);
        for (int y = maxY; y >= minY; y--) {
            BlockPos feet = new BlockPos(probe.getX(), y, probe.getZ());
            if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                continue;
            }
            BlockPos below = feet.below();
            if (level.getBlockState(below).isCollisionShapeFullBlock(level, below)) {
                return feet;
            }
        }
        return null;
    }

    public static boolean isEligibleFerrymanBoat(ServerLevel level, Boat boat) {
        if (level == null || boat == null || !boat.isAlive()) {
            return false;
        }
        BlockPos waterline = BlockPos.containing(
                boat.getX(), boat.getBoundingBox().minY - 0.05D, boat.getZ());
        if (!level.getFluidState(waterline).is(FluidTags.WATER)) {
            waterline = waterline.below();
        }
        for (int depth = 0; depth < ApprovedSpecialBehaviorRules.FERRYMAN_MIN_WATER_DEPTH; depth++) {
            BlockPos water = waterline.below(depth);
            if (!level.getFluidState(water).is(FluidTags.WATER)
                    || !level.getBlockState(water).getCollisionShape(level, water).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isSafeFerrymanFollowPosition(ServerLevel level, Vec3 position) {
        if (level == null || position == null) {
            return false;
        }
        double radius = ApprovedSpecialBehaviorRules.FERRYMAN_WATER_SAMPLE_RADIUS;
        for (double xOffset : new double[] {-radius, 0.0D, radius}) {
            for (double zOffset : new double[] {-radius, 0.0D, radius}) {
                for (double height : new double[] {0.10D, 0.95D, 1.75D}) {
                    BlockPos sample = BlockPos.containing(
                            position.x + xOffset,
                            position.y + height,
                            position.z + zOffset);
                    if (!level.hasChunkAt(sample)
                            || !level.getFluidState(sample).is(FluidTags.WATER)
                            || !level.getBlockState(sample).getCollisionShape(level, sample).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Keeps the reveal at the waterline: the Ferryman?'s feet remain submerged while its upper
     * body may enter air. All sampled blocks must stay collision-free and already loaded.
     */
    public static boolean isSafeFerrymanRevealPosition(ServerLevel level, Vec3 position) {
        if (level == null || position == null) {
            return false;
        }
        double radius = ApprovedSpecialBehaviorRules.FERRYMAN_WATER_SAMPLE_RADIUS;
        for (double xOffset : new double[] {-radius, 0.0D, radius}) {
            for (double zOffset : new double[] {-radius, 0.0D, radius}) {
                BlockPos feet = BlockPos.containing(
                        position.x + xOffset, position.y + 0.10D, position.z + zOffset);
                if (!level.hasChunkAt(feet) || !level.getFluidState(feet).is(FluidTags.WATER)) {
                    return false;
                }
                for (double height : new double[] {0.10D, 0.95D, 1.75D}) {
                    BlockPos sample = BlockPos.containing(
                            position.x + xOffset,
                            position.y + height,
                            position.z + zOffset);
                    if (!level.hasChunkAt(sample)
                            || !level.getBlockState(sample).getCollisionShape(level, sample).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isClearlyObserved(ServerPlayer player, BlockPos position) {
        Vec3 to = Vec3.atCenterOf(position).subtract(player.getEyePosition());
        return to.lengthSqr() > 0.01D
                && player.getViewVector(1.0F).normalize().dot(to.normalize()) > 0.86D;
    }

    public record SoundMemory(Vec3 position, long tick) {
    }

    public record CombatMemory(int observedEntityId, int attackerEntityId, Vec3 position, long tick) {
    }

    public record DebugSpawnResult(boolean success, String message) {
    }

    private record SpawnContext(Vec3 position, BlockPos anchor, UUID relatedEntityId) {
        private SpawnContext(Vec3 position, BlockPos anchor) {
            this(position, anchor, null);
        }
    }

    private record SurveyorPlacement(Vec3 spawnPosition, BlockPos featurePos) {
    }
}
