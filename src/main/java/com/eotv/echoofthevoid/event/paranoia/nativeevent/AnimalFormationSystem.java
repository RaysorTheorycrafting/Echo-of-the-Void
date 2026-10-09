package com.eotv.echoofthevoid.event.paranoia.nativeevent;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.phase.UncannyPhase;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import com.eotv.echoofthevoid.world.ObserverSight;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

/**
 * Animal formations: real farm animals found standing in an impossible arrangement, silent and
 * motionless, until someone stares at them for a few seconds; then they simply graze again.
 *
 * <ul>
 *   <li>{@link Variant#CIRCLE}: at night, a herd in a perfect circle facing an empty centre;</li>
 *   <li>{@link Variant#GRID}: the animals of a fenced pen on a perfect lattice, all facing the house;</li>
 *   <li>{@link Variant#DEATH_SITE}: a ring around the spot where the player last died;</li>
 *   <li>{@link Variant#WAKE}: on waking up, a ring around the house, every animal facing the bed.</li>
 * </ul>
 *
 * <p>Vanilla + delta: only the AI and the sounds are suspended. The original flags are stored on the
 * entity itself, so an animal saved mid-formation is restored the next time it loads.</p>
 */
public final class AnimalFormationSystem {
    public static final String FORMATION_TAG = "eotv_formation";
    private static final String PREVIOUS_NO_AI = "eotv_formation_no_ai";
    private static final String PREVIOUS_SILENT = "eotv_formation_silent";
    private static final int TICK_INTERVAL = 5;
    private static final double STARE_DOT = 0.9D;
    private static final double STARE_DISTANCE = 64.0D;
    private static final double FORMING_CLEARANCE = 12.0D;
    private static final double WAKE_CHANCE = 0.12D;
    private static final long WAKE_COOLDOWN_TICKS = 20L * 60L * 60L;
    private static final List<Formation> ACTIVE = new ArrayList<>();
    private static final Map<UUID, Long> NEXT_WAKE_FORMATION = new HashMap<>();

    private AnimalFormationSystem() {
    }

    public enum Variant {
        CIRCLE,
        GRID,
        DEATH_SITE,
        WAKE
    }

    // ------------------------------------------------------------------ triggers

    public static boolean trigger(ServerPlayer player, Variant variant, boolean debugImmediate) {
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD
                || ACTIVE.stream().anyMatch(formation -> formation.owner.equals(player.getUUID()))) {
            return false;
        }
        Plan plan = switch (variant) {
            case CIRCLE -> planCircle(level, player, debugImmediate);
            case GRID -> planGrid(level, player, debugImmediate);
            case DEATH_SITE -> planDeathSite(level, player);
            case WAKE -> planWake(level, player);
        };
        if (plan == null) {
            debug("{} refused for {}: no suitable animals or place", variant, player.getGameProfile().getName());
            return false;
        }
        double clearance = variant == Variant.WAKE ? 4.0D : FORMING_CLEARANCE;
        if (!debugImmediate && isSeen(level, plan, level.players(), clearance)) {
            debug("{} refused for {}: would be seen forming", variant, player.getGameProfile().getName());
            return false;
        }
        form(level, player.getUUID(), variant, plan, level.getServer().getTickCount());
        return true;
    }

    /** Morning hook: a night actually slept through may end with the animals waiting outside. */
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        UncannyWorldState state = UncannyWorldState.get(player.getServer());
        long now = player.getServer().getTickCount();
        if (!state.isSystemEnabled() || state.getPhase().index() < UncannyPhase.PHASE_2.index()
                || !AnimalFormationRules.isMorningAfterSleep(level.getDayTime())
                || now < NEXT_WAKE_FORMATION.getOrDefault(player.getUUID(), Long.MIN_VALUE)
                || player.getRandom().nextDouble() >= WAKE_CHANCE) {
            return;
        }
        if (trigger(player, Variant.WAKE, false)) {
            NEXT_WAKE_FORMATION.put(player.getUUID(), now + WAKE_COOLDOWN_TICKS);
        }
    }

    // ------------------------------------------------------------------ planning

    private record Plan(Vec3 centre, List<Animal> animals, List<Vec3> slots, List<Float> yaws) {
    }

    private static Plan planCircle(ServerLevel level, ServerPlayer player, boolean debugImmediate) {
        if (!debugImmediate && !AnimalFormationRules.isNight(level.getDayTime())) {
            return null;
        }
        List<Animal> eligible = eligibleAnimals(level, player.position(), 48.0D);
        List<Animal> herd = largestHerd(eligible, AnimalFormationRules.MIN_CIRCLE_ANIMALS);
        if (herd == null) {
            debug("CIRCLE: no herd of {} among {} eligible animals", AnimalFormationRules.MIN_CIRCLE_ANIMALS, eligible.size());
            return null;
        }
        Vec3 centroid = centroid(herd);
        for (int attempt = 0; attempt < 8; attempt++) {
            double ox = attempt == 0 ? 0.0D : level.random.nextInt(13) - 6;
            double oz = attempt == 0 ? 0.0D : level.random.nextInt(13) - 6;
            int x = (int) Math.floor(centroid.x + ox);
            int z = (int) Math.floor(centroid.z + oz);
            BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!level.canSeeSky(ground) || ground.distSqr(player.blockPosition()) < 16 * 16) {
                debug("CIRCLE: centre {} rejected (sky={}, distance={})", ground, level.canSeeSky(ground),
                        Math.sqrt(ground.distSqr(player.blockPosition())));
                continue;
            }
            Vec3 centre = Vec3.atBottomCenterOf(ground);
            Plan plan = ring(level, centre, herd, AnimalFormationRules.circleRadius(herd.size()), false);
            if (plan != null) {
                return plan;
            }
            debug("CIRCLE: no standing ring around {}", ground);
        }
        return null;
    }

    private static Plan planDeathSite(ServerLevel level, ServerPlayer player) {
        UncannyWorldState.DeathSite site = UncannyWorldState.get(level.getServer()).getDeathSite(player.getUUID());
        if (site == null || !site.dimension().equals(level.dimension().location().toString())) {
            return null;
        }
        BlockPos pos = site.position();
        if (!level.isLoaded(pos) || pos.distSqr(player.blockPosition()) > 96 * 96) {
            return null;
        }
        List<Animal> animals = nearest(eligibleAnimals(level, Vec3.atCenterOf(pos), 48.0D), Vec3.atCenterOf(pos), 8);
        if (animals.size() < AnimalFormationRules.MIN_SMALL_FORMATION_ANIMALS) {
            return null;
        }
        return ring(level, Vec3.atBottomCenterOf(pos), animals, AnimalFormationRules.DEATH_SITE_RADIUS, false);
    }

    private static Plan planWake(ServerLevel level, ServerPlayer player) {
        BlockPos bed = player.getSleepingPos().orElse(player.getRespawnPosition());
        if (bed == null || !level.isLoaded(bed)) {
            return null;
        }
        Vec3 centre = Vec3.atBottomCenterOf(bed);
        List<Animal> animals = nearest(eligibleAnimals(level, centre, 48.0D), centre, 8);
        if (animals.size() < AnimalFormationRules.MIN_SMALL_FORMATION_ANIMALS) {
            return null;
        }
        double phase = level.random.nextDouble() * Math.PI * 2.0D;
        List<Vec3> slots = new ArrayList<>();
        for (double[] point : AnimalFormationRules.circlePoints(centre.x, centre.z, 1.0D, animals.size(), phase)) {
            double dx = point[0] - centre.x;
            double dz = point[1] - centre.z;
            for (double radius = AnimalFormationRules.MIN_WAKE_RADIUS; radius <= AnimalFormationRules.MAX_WAKE_RADIUS; radius += 1.0D) {
                Vec3 slot = standingSpot(level, centre.x + dx * radius, centre.z + dz * radius, bed.getY(), 3);
                if (slot != null && level.canSeeSky(BlockPos.containing(slot))) {
                    slots.add(slot);
                    break;
                }
            }
        }
        if (slots.size() < AnimalFormationRules.MIN_SMALL_FORMATION_ANIMALS) {
            return null;
        }
        List<Animal> used = animals.subList(0, slots.size());
        return new Plan(centre, List.copyOf(used), slots, facing(slots, centre));
    }

    private static Plan planGrid(ServerLevel level, ServerPlayer player, boolean debugImmediate) {
        BlockPos base = WanderingTreeSystem.resolveBaseCenter(player);
        Vec3 searchCentre = debugImmediate ? player.position() : Vec3.atCenterOf(base);
        if (!debugImmediate && player.position().distanceToSqr(searchCentre) > 96.0D * 96.0D) {
            return null;
        }
        List<Animal> candidates = eligibleAnimals(level, searchCentre, 40.0D);
        Set<Long> tried = new HashSet<>();
        for (Animal start : candidates) {
            BlockPos feet = start.blockPosition();
            if (!tried.add(AnimalFormationRules.pack(feet.getX(), feet.getZ()))) {
                continue;
            }
            Map<Long, Integer> pen = floodPen(level, feet);
            if (pen == null) {
                continue;
            }
            tried.addAll(pen.keySet());
            List<Animal> inside = candidates.stream()
                    .filter(animal -> pen.containsKey(AnimalFormationRules.pack(
                            animal.blockPosition().getX(), animal.blockPosition().getZ())))
                    .toList();
            List<int[]> lattice = AnimalFormationRules.gridCells(pen.keySet());
            if (inside.size() < AnimalFormationRules.MIN_SMALL_FORMATION_ANIMALS
                    || inside.size() > lattice.size()
                    || inside.size() < countAllAnimalsIn(level, pen)) {
                continue;
            }
            List<Vec3> slots = new ArrayList<>();
            for (int i = 0; i < inside.size(); i++) {
                int[] cell = lattice.get(i);
                int y = pen.get(AnimalFormationRules.pack(cell[0], cell[1]));
                slots.add(new Vec3(cell[0] + 0.5D, y, cell[1] + 0.5D));
            }
            Vec3 penCentre = centroidOf(slots);
            float yaw = AnimalFormationRules.yawToward(penCentre.x, penCentre.z, base.getX() + 0.5D, base.getZ() + 0.5D);
            List<Float> yaws = new ArrayList<>();
            for (int i = 0; i < slots.size(); i++) {
                yaws.add(yaw);
            }
            return new Plan(penCentre, List.copyOf(inside), slots, yaws);
        }
        return null;
    }

    /** A pen's walkable floor (packed x/z to feet y), or null if the area is open or too large. */
    private static Map<Long, Integer> floodPen(ServerLevel level, BlockPos start) {
        Map<Long, Integer> cells = new LinkedHashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        if (!isStandable(level, start)) {
            return null;
        }
        cells.put(AnimalFormationRules.pack(start.getX(), start.getZ()), start.getY());
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos side = current.relative(direction);
                long key = AnimalFormationRules.pack(side.getX(), side.getZ());
                if (cells.containsKey(key) || !level.isLoaded(side)) {
                    continue;
                }
                BlockPos next = null;
                for (int dy : new int[]{0, 1, -1}) {
                    BlockPos candidate = side.above(dy);
                    // A fence or wall stops the flood even though it is one block taller than the ground.
                    if (isStandable(level, candidate) && !isBarrier(level, current.above(Math.max(dy, 0)).relative(direction))) {
                        next = candidate;
                        break;
                    }
                }
                if (next == null) {
                    continue;
                }
                cells.put(key, next.getY());
                if (cells.size() > AnimalFormationRules.MAX_PEN_CELLS) {
                    return null;
                }
                queue.add(next);
            }
        }
        return cells.size() >= 9 ? cells : null;
    }

    private static boolean isBarrier(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.getCollisionShape(level, pos).isEmpty()
                && state.getCollisionShape(level, pos).max(Direction.Axis.Y) > 1.0D;
    }

    private static int countAllAnimalsIn(ServerLevel level, Map<Long, Integer> pen) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int y = 0;
        for (Map.Entry<Long, Integer> entry : pen.entrySet()) {
            minX = Math.min(minX, AnimalFormationRules.unpackX(entry.getKey()));
            maxX = Math.max(maxX, AnimalFormationRules.unpackX(entry.getKey()));
            minZ = Math.min(minZ, AnimalFormationRules.unpackZ(entry.getKey()));
            maxZ = Math.max(maxZ, AnimalFormationRules.unpackZ(entry.getKey()));
            y = entry.getValue();
        }
        AABB box = new AABB(minX, y - 3, minZ, maxX + 1, y + 4, maxZ + 1);
        return (int) level.getEntitiesOfClass(Animal.class, box, animal -> animal.isAlive()
                && pen.containsKey(AnimalFormationRules.pack(animal.blockPosition().getX(), animal.blockPosition().getZ())))
                .size();
    }

    private static Plan ring(ServerLevel level, Vec3 centre, List<Animal> animals, double radius, boolean requireSky) {
        double phase = level.random.nextDouble() * Math.PI * 2.0D;
        List<Vec3> slots = new ArrayList<>();
        for (double[] point : AnimalFormationRules.circlePoints(centre.x, centre.z, radius, animals.size(), phase)) {
            Vec3 slot = standingSpot(level, point[0], point[1], (int) Math.floor(centre.y), 2);
            if (slot == null || (requireSky && !level.canSeeSky(BlockPos.containing(slot)))) {
                return null;
            }
            slots.add(slot);
        }
        return new Plan(centre, List.copyOf(animals), slots, facing(slots, centre));
    }

    private static List<Float> facing(List<Vec3> slots, Vec3 target) {
        List<Float> yaws = new ArrayList<>();
        for (Vec3 slot : slots) {
            yaws.add(AnimalFormationRules.yawToward(slot.x, slot.z, target.x, target.z));
        }
        return yaws;
    }

    /** Feet position on solid ground within {@code range} blocks of {@code nearY}, free of fluids. */
    private static Vec3 standingSpot(ServerLevel level, double x, double z, int nearY, int range) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        for (int dy = range; dy >= -range; dy--) {
            BlockPos feet = new BlockPos(bx, nearY + dy, bz);
            if (level.isLoaded(feet) && isStandable(level, feet)) {
                return new Vec3(x, feet.getY(), z);
            }
        }
        return null;
    }

    private static boolean isStandable(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        BlockState ground = level.getBlockState(below);
        return ground.isFaceSturdy(level, below, Direction.UP)
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && level.getFluidState(feet).isEmpty()
                && level.getFluidState(below).isEmpty();
    }

    // ------------------------------------------------------------------ animals

    private static List<Animal> eligibleAnimals(ServerLevel level, Vec3 centre, double radius) {
        return level.getEntitiesOfClass(Animal.class, new AABB(centre, centre).inflate(radius, 16.0D, radius),
                AnimalFormationSystem::isEligible);
    }

    /** Ordinary, ownerless farm animals of Vanilla; pets, named, tied, ridden or modded ones are left alone. */
    static boolean isEligible(Animal animal) {
        if (!animal.isAlive() || animal.isNoAi() || animal.isLeashed() || animal.isPassenger() || animal.isVehicle()
                || animal.hasCustomName() || animal.getTags().contains(FORMATION_TAG) || animal.isInLove()
                || animal.isInWater() || !"minecraft".equals(BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType()).getNamespace())) {
            return false;
        }
        if (animal instanceof AbstractHorse horse) {
            return !horse.isTamed();
        }
        return animal instanceof Cow || animal instanceof Sheep || animal instanceof Pig
                || animal instanceof Chicken || animal instanceof Goat || animal instanceof Rabbit;
    }

    private static List<Animal> largestHerd(List<Animal> animals, int minimum) {
        Map<EntityType<?>, List<Animal>> byType = new HashMap<>();
        for (Animal animal : animals) {
            byType.computeIfAbsent(animal.getType(), ignored -> new ArrayList<>()).add(animal);
        }
        List<Animal> best = null;
        for (List<Animal> herd : byType.values()) {
            if (herd.size() >= minimum && (best == null || herd.size() > best.size())) {
                best = herd;
            }
        }
        if (best == null) {
            return null;
        }
        return nearest(best, centroid(best), AnimalFormationRules.MAX_FORMATION_ANIMALS);
    }

    private static List<Animal> nearest(List<Animal> animals, Vec3 point, int limit) {
        return animals.stream()
                .sorted(Comparator.comparingDouble(animal -> animal.position().distanceToSqr(point)))
                .limit(limit)
                .toList();
    }

    private static Vec3 centroid(List<Animal> animals) {
        return centroidOf(animals.stream().map(Entity::position).toList());
    }

    private static Vec3 centroidOf(List<Vec3> points) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (Vec3 point : points) {
            x += point.x;
            y += point.y;
            z += point.z;
        }
        return new Vec3(x / points.size(), y / points.size(), z / points.size());
    }

    // ------------------------------------------------------------------ forming and releasing

    private static boolean isSeen(ServerLevel level, Plan plan, List<? extends Player> observers, double clearance) {
        List<Vec3> points = new ArrayList<>();
        for (Vec3 slot : plan.slots()) {
            points.add(slot.add(0.0D, 0.6D, 0.0D));
        }
        for (Animal animal : plan.animals()) {
            points.add(animal.position().add(0.0D, animal.getBbHeight() * 0.6D, 0.0D));
        }
        return ObserverSight.isSeenByAny(level, points, observers, clearance, pos -> false);
    }

    private static void form(ServerLevel level, UUID owner, Variant variant, Plan plan, long now) {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < plan.animals().size(); i++) {
            Animal animal = plan.animals().get(i);
            Vec3 slot = plan.slots().get(i);
            float yaw = plan.yaws().get(i);
            freeze(animal);
            animal.moveTo(slot.x, slot.y, slot.z, yaw, 0.0F);
            animal.setYHeadRot(yaw);
            animal.setYBodyRot(yaw);
            animal.setDeltaMovement(Vec3.ZERO);
            ids.add(animal.getUUID());
        }
        ACTIVE.add(new Formation(UUID.randomUUID(), owner, level.dimension(), variant, plan.centre(), ids,
                now + AnimalFormationRules.MAX_FORMATION_TICKS));
        debug("{} formed with {} animals around {}", variant, ids.size(), BlockPos.containing(plan.centre()));
    }

    private static void freeze(Animal animal) {
        CompoundTag data = animal.getPersistentData();
        data.putBoolean(PREVIOUS_NO_AI, animal.isNoAi());
        data.putBoolean(PREVIOUS_SILENT, animal.isSilent());
        animal.addTag(FORMATION_TAG);
        animal.getNavigation().stop();
        animal.setNoAi(true);
        animal.setSilent(true);
    }

    static void restore(Entity entity) {
        if (!(entity instanceof Animal animal) || !animal.getTags().contains(FORMATION_TAG)) {
            return;
        }
        CompoundTag data = animal.getPersistentData();
        animal.setNoAi(data.getBoolean(PREVIOUS_NO_AI));
        animal.setSilent(data.getBoolean(PREVIOUS_SILENT));
        data.remove(PREVIOUS_NO_AI);
        data.remove(PREVIOUS_SILENT);
        animal.removeTag(FORMATION_TAG);
    }

    /** An animal saved mid-formation (crash, unload, restart) wakes up as soon as it loads again. */
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !event.getEntity().getTags().contains(FORMATION_TAG)) {
            return;
        }
        UUID id = event.getEntity().getUUID();
        if (ACTIVE.stream().noneMatch(formation -> formation.animals.contains(id))) {
            restore(event.getEntity());
        }
    }

    public static void tick(MinecraftServer server, long now) {
        if (ACTIVE.isEmpty() || now % TICK_INTERVAL != 0L) {
            return;
        }
        Iterator<Formation> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Formation formation = iterator.next();
            ServerLevel level = server.getLevel(formation.dimension);
            if (level == null) {
                iterator.remove();
                continue;
            }
            if (formation.releaseAt.isEmpty()) {
                String reason = breakReason(level, formation, now);
                if (reason != null) {
                    debug("{} released ({})", formation.variant, reason);
                    scheduleRelease(level, formation, now);
                }
            }
            if (!formation.releaseAt.isEmpty()) {
                formation.releaseAt.entrySet().removeIf(entry -> {
                    if (now < entry.getValue()) {
                        return false;
                    }
                    Entity entity = level.getEntity(entry.getKey());
                    if (entity != null) {
                        restore(entity);
                    }
                    return true;
                });
                if (formation.releaseAt.isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

    private static String breakReason(ServerLevel level, Formation formation, long now) {
        if (now >= formation.expireTick) {
            return "unseen";
        }
        for (UUID id : formation.animals) {
            Entity entity = level.getEntity(id);
            if (!(entity instanceof Animal animal) || !animal.isAlive()) {
                return "missing";
            }
            if (animal.hurtTime > 0 || animal.isLeashed() || animal.isPassenger() || animal.isVehicle()) {
                return "disturbed";
            }
        }
        Vec3 focus = formation.centre.add(0.0D, 0.8D, 0.0D);
        for (ServerPlayer player : level.players()) {
            if (stares(player, level, formation, focus)) {
                formation.staredTicks += TICK_INTERVAL;
                break;
            }
        }
        return formation.staredTicks >= AnimalFormationRules.STARE_TICKS_TO_RELEASE ? "stared" : null;
    }

    private static boolean stares(ServerPlayer player, ServerLevel level, Formation formation, Vec3 focus) {
        if (ObserverSight.isStaredAt(player, focus, STARE_DOT, STARE_DISTANCE)) {
            return true;
        }
        for (UUID id : formation.animals) {
            Entity entity = level.getEntity(id);
            if (entity != null && ObserverSight.isStaredAt(player, entity.getEyePosition(), 0.97D, STARE_DISTANCE)) {
                return true;
            }
        }
        return false;
    }

    private static void scheduleRelease(ServerLevel level, Formation formation, long now) {
        for (UUID id : formation.animals) {
            formation.releaseAt.put(id, now + level.random.nextInt(AnimalFormationRules.MAX_RELEASE_STAGGER_TICKS + 1));
        }
    }

    public static void clearForOwner(MinecraftServer server, UUID owner) {
        releaseMatching(server, formation -> formation.owner.equals(owner));
    }

    public static void clear(MinecraftServer server) {
        releaseMatching(server, formation -> true);
        NEXT_WAKE_FORMATION.clear();
    }

    private static void releaseMatching(MinecraftServer server, java.util.function.Predicate<Formation> filter) {
        Iterator<Formation> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Formation formation = iterator.next();
            if (!filter.test(formation)) {
                continue;
            }
            ServerLevel level = server == null ? null : server.getLevel(formation.dimension);
            if (level != null) {
                for (UUID id : formation.animals) {
                    Entity entity = level.getEntity(id);
                    if (entity != null) {
                        restore(entity);
                    }
                }
            }
            iterator.remove();
        }
    }

    /** For GameTests and diagnostics. */
    public static int activeFormationCount() {
        return ACTIVE.size();
    }

    /** For GameTests: forms the given animals on the given slots, bypassing selection and sight. */
    public static void formForTest(ServerLevel level, Variant variant, Vec3 centre, List<Animal> animals,
            List<Vec3> slots, List<Float> yaws) {
        form(level, UUID.randomUUID(), variant, new Plan(centre, List.copyOf(animals), slots, yaws),
                level.getServer().getTickCount());
    }

    /** For GameTests: the planned grid of a pen around {@code feet}, as slot positions. */
    public static List<Vec3> planGridSlotsForTest(ServerLevel level, BlockPos feet) {
        Map<Long, Integer> pen = floodPen(level, feet);
        if (pen == null) {
            return List.of();
        }
        List<Vec3> slots = new ArrayList<>();
        for (int[] cell : AnimalFormationRules.gridCells(pen.keySet())) {
            slots.add(new Vec3(cell[0] + 0.5D, pen.get(AnimalFormationRules.pack(cell[0], cell[1])), cell[1] + 0.5D));
        }
        return slots;
    }

    private static void debug(String message, Object... args) {
        if (UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info("[UncannyDebug/AnimalFormation] " + message, args);
        }
    }

    private static final class Formation {
        private final UUID id;
        private final UUID owner;
        private final ResourceKey<Level> dimension;
        private final Variant variant;
        private final Vec3 centre;
        private final List<UUID> animals;
        private final long expireTick;
        private final Map<UUID, Long> releaseAt = new HashMap<>();
        private int staredTicks;

        private Formation(UUID id, UUID owner, ResourceKey<Level> dimension, Variant variant, Vec3 centre,
                List<UUID> animals, long expireTick) {
            this.id = id;
            this.owner = owner;
            this.dimension = dimension;
            this.variant = variant;
            this.centre = centre;
            this.animals = List.copyOf(animals);
            this.expireTick = expireTick;
        }
    }
}
