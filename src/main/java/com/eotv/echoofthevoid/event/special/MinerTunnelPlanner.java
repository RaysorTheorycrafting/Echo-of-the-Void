package com.eotv.echoofthevoid.event.special;

import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import com.eotv.echoofthevoid.event.paranoia.GhostMinerRules;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/** Loaded-chunk-only path planning for two-block-high Miner? tunnel sections. */
public final class MinerTunnelPlanner {
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    private static final int[] WALKABLE_VERTICAL_STEPS = {0, 1, -1};

    private MinerTunnelPlanner() {
    }

    public static TunnelPlan findInitial(
            ServerLevel level, ServerPlayer target, RandomSource random, boolean debug) {
        List<StartCandidate> candidates = collectCandidates(level, target);
        if (debug) {
            return findDebugInitial(level, target, candidates);
        }
        return findInitial(level, target, random, candidates);
    }

    /**
     * Developer retries must be deterministic and must not inherit the scheduler's randomized
     * global search budget. A difficult first candidate used to consume all 1,024 nodes, making
     * the same devmenu action succeed once and then report {@code no_safe_tunnel}. Natural
     * encounters retain their original randomized, globally bounded search below.
     */
    private static TunnelPlan findDebugInitial(
            ServerLevel level, ServerPlayer target, List<StartCandidate> candidates) {
        candidates.sort(Comparator
                .comparingInt((StartCandidate candidate) -> candidate.floorApproach() ? 1 : 0)
                .thenComparingInt(candidate -> heuristic(candidate.pos(), target.blockPosition()))
                .thenComparingLong(candidate -> candidate.pos().asLong()));
        int limit = Math.min(candidates.size(), MinerRules.MAX_DEBUG_START_CANDIDATES);
        for (int index = 0; index < limit; index++) {
            StartCandidate candidate = candidates.get(index);
            int[] candidateSearchBudget = {MinerRules.MAX_SEARCHED_NODES};
            TunnelPlan plan = search(
                    level,
                    candidate.pos(),
                    target,
                    candidate.floorApproach(),
                    GhostMinerRules.MIN_PLANNED_SECTIONS,
                    MinerRules.MAX_TUNNEL_SECTIONS,
                    0,
                    candidateSearchBudget,
                    true,
                    null);
            if (plan != null) {
                return plan;
            }
        }
        return null;
    }

    /** Deterministic orientation seam used by the headless wall/floor characterization tests. */
    public static TunnelPlan findInitialForGameTest(
            ServerLevel level, ServerPlayer target, RandomSource random, boolean floorApproach) {
        List<StartCandidate> candidates = collectCandidates(level, target).stream()
                .filter(candidate -> candidate.floorApproach() == floorApproach)
                .toList();
        return findInitial(level, target, random, new ArrayList<>(candidates));
    }

    private static TunnelPlan findInitial(
            ServerLevel level,
            ServerPlayer target,
            RandomSource random,
            List<StartCandidate> candidates) {
        shuffle(candidates, random);
        int[] remainingSearchBudget = {MinerRules.MAX_SEARCHED_NODES};
        int limit = Math.min(candidates.size(), GhostMinerRules.MAX_START_CANDIDATES_TO_EVALUATE);
        for (int index = 0; index < limit && remainingSearchBudget[0] > 0; index++) {
            StartCandidate candidate = candidates.get(index);
            TunnelPlan plan = search(
                    level,
                    candidate.pos(),
                    target,
                    candidate.floorApproach(),
                    GhostMinerRules.MIN_PLANNED_SECTIONS,
                    MinerRules.MAX_TUNNEL_SECTIONS,
                    0,
                    remainingSearchBudget,
                    true,
                    null);
            if (plan != null) {
                return plan;
            }
        }
        return null;
    }

    public static TunnelPlan replan(
            ServerLevel level,
            BlockPos currentColumn,
            ServerPlayer target,
            int remainingSections,
            int constructionSectionsUsed) {
        return replanAvoiding(
                level,
                currentColumn,
                target,
                remainingSections,
                constructionSectionsUsed,
                null);
    }

    /** Replans without immediately selecting a physical step that has already proved unusable. */
    public static TunnelPlan replanAvoiding(
            ServerLevel level,
            BlockPos currentColumn,
            ServerPlayer target,
            int remainingSections,
            int constructionSectionsUsed,
            BlockPos excludedFirstStep) {
        if (remainingSections <= 0) {
            return null;
        }
        boolean floorApproach = currentColumn.getY() < target.blockPosition().getY();
        int[] budget = {MinerRules.MAX_SEARCHED_NODES};
        return search(
                level,
                currentColumn,
                target,
                floorApproach,
                0,
                remainingSections,
                constructionSectionsUsed,
                budget,
                false,
                excludedFirstStep);
    }

    /**
     * Revalidates the exit retained from the last successful plan. A one-block target movement may
     * make a fresh exact 3-5 block search fail even though the already planned opening remains safe
     * and close enough to present the encounter correctly.
     */
    public static boolean canUseRetainedEmergence(
            ServerLevel level, BlockPos emergence, ServerPlayer target) {
        if (emergence == null || target == null || target.level() != level || !isStandable(level, emergence)) {
            return false;
        }
        double distanceSquared = horizontalDistanceSquared(emergence, target.blockPosition());
        return distanceSquared >= 4.0D
                && distanceSquared <= 49.0D
                && Math.abs(emergence.getY() - target.blockPosition().getY()) <= 3
                && hasOpenWalkingConnection(level, emergence, target.blockPosition());
    }

    public static boolean canUseRetainedEmergence(
            ServerLevel level, BlockPos currentColumn, BlockPos emergence, ServerPlayer target) {
        if (!canUseRetainedEmergence(level, emergence, target) || currentColumn == null) {
            return false;
        }
        if (emergence.getY() == currentColumn.getY()) {
            return true;
        }
        return UncannyMinerBlockPolicy.classifyAscendingExitTransition(level, currentColumn, emergence)
                != UncannyMinerBlockPolicy.MaterialKind.BLOCKED;
    }

    private static List<StartCandidate> collectCandidates(
            ServerLevel level, ServerPlayer target) {
        List<StartCandidate> candidates = new ArrayList<>();
        BlockPos origin = target.blockPosition();
        for (int dx = -GhostMinerRules.MAX_START_DISTANCE; dx <= GhostMinerRules.MAX_START_DISTANCE; dx++) {
            for (int dz = -GhostMinerRules.MAX_START_DISTANCE; dz <= GhostMinerRules.MAX_START_DISTANCE; dz++) {
                if (!GhostMinerRules.isValidStartOffset(dx, dz)) {
                    continue;
                }
                addNaturalCandidate(level, candidates, origin.offset(dx, 0, dz), false);
                addNaturalCandidate(level, candidates, origin.offset(dx, -2, dz), true);
            }
        }
        return candidates;
    }

    private static void addNaturalCandidate(
            ServerLevel level, List<StartCandidate> candidates, BlockPos pos, boolean floorApproach) {
        if (UncannyMinerBlockPolicy.classifyColumn(level, pos)
                        == UncannyMinerBlockPolicy.MaterialKind.NATURAL
                && hasSturdyDrySupport(level, pos)) {
            candidates.add(new StartCandidate(pos.immutable(), floorApproach));
        }
    }

    private static TunnelPlan search(
            ServerLevel level,
            BlockPos start,
            ServerPlayer target,
            boolean floorApproach,
            int minimumSections,
            int maximumSections,
            int constructionSectionsUsed,
            int[] remainingSearchBudget,
            boolean includeStart,
            BlockPos excludedFirstStep) {
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(Node::score));
        Map<NodeKey, Integer> bestDepth = new HashMap<>();
        Node root = new Node(start.immutable(), null, 0, constructionSectionsUsed,
                heuristic(start, target.blockPosition()));
        open.add(root);
        bestDepth.put(new NodeKey(start.asLong(), constructionSectionsUsed), 0);

        while (!open.isEmpty() && remainingSearchBudget[0]-- > 0) {
            Node node = open.poll();
            int visibleDepth = includeStart ? node.depth() + 1 : node.depth();
            if (visibleDepth >= minimumSections) {
                EmergenceCandidate emergence = findEmergence(
                        level,
                        node.pos(),
                        target,
                        floorApproach,
                        node.constructionSections(),
                        maximumSections - visibleDepth,
                        node.depth() == 0 ? excludedFirstStep : null);
                if (emergence != null) {
                    List<BlockPos> path = reconstruct(node, includeStart);
                    path.addAll(emergence.excavationColumns());
                    if ((includeStart || !path.isEmpty() || minimumSections == 0)
                            && path.size() <= maximumSections) {
                        return new TunnelPlan(
                                List.copyOf(path),
                                emergence.position().immutable(),
                                floorApproach,
                                emergence.constructionSections());
                    }
                }
            }
            if (visibleDepth >= maximumSections) {
                continue;
            }

            for (Direction direction : HORIZONTAL) {
                BlockPos next = node.pos().relative(direction);
                if (node.depth() == 0 && excludedFirstStep != null && next.equals(excludedFirstStep)) {
                    continue;
                }
                UncannyMinerBlockPolicy.MaterialKind kind =
                        UncannyMinerBlockPolicy.classifyColumn(level, next);
                if (kind == UncannyMinerBlockPolicy.MaterialKind.BLOCKED
                        || !hasSturdyDrySupport(level, next)) {
                    continue;
                }
                int construction = node.constructionSections()
                        + (kind == UncannyMinerBlockPolicy.MaterialKind.CONSTRUCTION ? 1 : 0);
                if (construction > MinerRules.MAX_CONSTRUCTION_SECTIONS
                        || (kind == UncannyMinerBlockPolicy.MaterialKind.CONSTRUCTION
                                && horizontalDistanceSquared(next, target.blockPosition()) > 36.0D)) {
                    continue;
                }
                int nextDepth = node.depth() + 1;
                NodeKey key = new NodeKey(next.asLong(), construction);
                Integer previousDepth = bestDepth.get(key);
                if (previousDepth != null && previousDepth <= nextDepth) {
                    continue;
                }
                bestDepth.put(key, nextDepth);
                open.add(new Node(
                        next.immutable(),
                        node,
                        nextDepth,
                        construction,
                        nextDepth + heuristic(next, target.blockPosition())));
            }
        }
        return null;
    }

    private static EmergenceCandidate findEmergence(
            ServerLevel level,
            BlockPos tunnelColumn,
            ServerPlayer target,
            boolean floorApproach,
            int constructionSections,
            int remainingSections,
            BlockPos excludedFirstStep) {
        BlockPos targetPos = target.blockPosition();
        if (floorApproach && tunnelColumn.getY() < targetPos.getY()) {
            return findStaircaseEmergence(
                    level,
                    tunnelColumn,
                    targetPos,
                    constructionSections,
                    remainingSections,
                    excludedFirstStep);
        }
        for (Direction direction : directionsToward(tunnelColumn, targetPos)) {
            BlockPos candidate = tunnelColumn.relative(direction);
            if (candidate.equals(excludedFirstStep)) {
                continue;
            }
            if (!isStandable(level, candidate)) {
                continue;
            }
            double distanceSquared = horizontalDistanceSquared(candidate, targetPos);
            if (distanceSquared >= 9.0D
                    && distanceSquared <= 25.0D
                    && hasOpenWalkingConnection(level, candidate, targetPos)) {
                return new EmergenceCandidate(candidate, List.of(), constructionSections);
            }
        }
        return null;
    }

    /**
     * Builds a real one-block-at-a-time staircase instead of moving the entity vertically through
     * the final two-block tunnel column. The last move is into already open, standable space, so a
     * floor approach can be observed as successive diagonal steps rather than a sudden emergence.
     */
    private static EmergenceCandidate findStaircaseEmergence(
            ServerLevel level,
            BlockPos tunnelColumn,
            BlockPos targetPos,
            int constructionSections,
            int remainingSections,
            BlockPos excludedFirstStep) {
        int rise = targetPos.getY() - tunnelColumn.getY();
        int excavationSteps = Math.max(0, rise - 1);
        if (excavationSteps > remainingSections) {
            return null;
        }

        for (Direction direction : directionsToward(tunnelColumn, targetPos)) {
            List<BlockPos> staircase = new ArrayList<>(excavationSteps);
            BlockPos cursor = tunnelColumn;
            int totalConstruction = constructionSections;
            boolean safe = true;
            for (int step = 0; step < excavationSteps; step++) {
                BlockPos next = cursor.relative(direction).above();
                if (step == 0 && next.equals(excludedFirstStep)) {
                    safe = false;
                    break;
                }
                UncannyMinerBlockPolicy.MaterialKind kind =
                        UncannyMinerBlockPolicy.classifyAscendingTransition(level, cursor, next);
                if (kind == UncannyMinerBlockPolicy.MaterialKind.BLOCKED
                        || !hasSturdyDrySupport(level, next)) {
                    safe = false;
                    break;
                }
                if (kind == UncannyMinerBlockPolicy.MaterialKind.CONSTRUCTION
                        && ++totalConstruction > MinerRules.MAX_CONSTRUCTION_SECTIONS) {
                    safe = false;
                    break;
                }
                cursor = next;
                staircase.add(cursor.immutable());
            }
            if (!safe) {
                continue;
            }

            BlockPos exit = cursor.relative(direction).above();
            double distanceSquared = horizontalDistanceSquared(exit, targetPos);
            UncannyMinerBlockPolicy.MaterialKind exitTransition =
                    UncannyMinerBlockPolicy.classifyAscendingExitTransition(level, cursor, exit);
            int finalConstruction = totalConstruction
                    + (exitTransition == UncannyMinerBlockPolicy.MaterialKind.CONSTRUCTION ? 1 : 0);
            if (!exit.equals(excludedFirstStep)
                    && exitTransition != UncannyMinerBlockPolicy.MaterialKind.BLOCKED
                    && finalConstruction <= MinerRules.MAX_CONSTRUCTION_SECTIONS
                    && isStandable(level, exit)
                    && distanceSquared >= 9.0D
                    && distanceSquared <= 25.0D
                    && hasOpenWalkingConnection(level, exit, targetPos)) {
                return new EmergenceCandidate(exit, List.copyOf(staircase), finalConstruction);
            }
        }
        return null;
    }

    private static List<Direction> directionsToward(BlockPos from, BlockPos target) {
        List<Direction> directions = new ArrayList<>(List.of(HORIZONTAL));
        directions.sort(Comparator.comparingDouble(
                direction -> horizontalDistanceSquared(from.relative(direction), target)));
        return directions;
    }

    private static boolean hasSturdyDrySupport(ServerLevel level, BlockPos feet) {
        BlockPos supportPos = feet.below();
        BlockState support = level.getBlockState(supportPos);
        return support.isFaceSturdy(level, supportPos, Direction.UP)
                && support.getFluidState().isEmpty();
    }

    public static boolean hasStableWalkingSupport(ServerLevel level, BlockPos feet) {
        return level != null && feet != null && level.hasChunkAt(feet) && hasSturdyDrySupport(level, feet);
    }

    public static boolean isGenuineOpenEmergence(ServerLevel level, BlockPos feet) {
        return level != null && feet != null && isStandable(level, feet);
    }

    private static boolean isStandable(ServerLevel level, BlockPos feet) {
        if (!level.hasChunkAt(feet)
                || !level.getWorldBorder().isWithinBounds(feet)
                || !isGenuineOpenCell(level, feet)
                || !isGenuineOpenCell(level, feet.above())) {
            return false;
        }
        BlockPos below = feet.below();
        BlockState support = level.getBlockState(below);
        return support.isFaceSturdy(level, below, Direction.UP) && support.getFluidState().isEmpty();
    }

    /**
     * A restoration marker is traversable, but it is still part of Miner?'s tunnel. Treating it as
     * exterior air made replans select the previous tunnel column as the final wall exit.
     */
    private static boolean isGenuineOpenCell(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return !state.is(UncannyBlockRegistry.UNCANNY_RESTORATION_PLACEHOLDER.get())
                && state.getFluidState().isEmpty()
                && !state.hasBlockEntity()
                && level.getBlockEntity(pos) == null
                && state.getCollisionShape(level, pos).isEmpty();
    }

    /**
     * Confirms that the chosen opening belongs to the same small, loaded walkable space as the
     * target. This keeps a visually open pocket on the far side of a wall from becoming a combat
     * hand-off that Vanilla navigation cannot complete.
     */
    private static boolean hasOpenWalkingConnection(
            ServerLevel level, BlockPos emergence, BlockPos targetPos) {
        if (!isStandable(level, emergence)) {
            return false;
        }
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        open.add(emergence.immutable());
        visited.add(emergence.asLong());
        int examined = 0;
        while (!open.isEmpty() && examined++ < MinerRules.MAX_OPEN_CONNECTION_NODES) {
            BlockPos current = open.removeFirst();
            if (horizontalDistanceSquared(current, targetPos) <= 2.25D
                    && Math.abs(current.getY() - targetPos.getY()) <= 1) {
                return true;
            }
            for (Direction direction : HORIZONTAL) {
                BlockPos horizontal = current.relative(direction);
                for (int dy : WALKABLE_VERTICAL_STEPS) {
                    BlockPos next = horizontal.offset(0, dy, 0);
                    if (Math.abs(next.getX() - emergence.getX())
                                    > MinerRules.OPEN_CONNECTION_HORIZONTAL_RADIUS
                            || Math.abs(next.getZ() - emergence.getZ())
                                    > MinerRules.OPEN_CONNECTION_HORIZONTAL_RADIUS
                            || Math.abs(next.getY() - emergence.getY())
                                    > MinerRules.OPEN_CONNECTION_VERTICAL_RADIUS
                            || !visited.add(next.asLong())
                            || !isStandable(level, next)) {
                        continue;
                    }
                    open.addLast(next.immutable());
                }
            }
        }
        return false;
    }

    private static List<BlockPos> reconstruct(Node node, boolean includeStart) {
        List<BlockPos> reverse = new ArrayList<>();
        Node cursor = node;
        while (cursor != null) {
            reverse.add(cursor.pos());
            cursor = cursor.parent();
        }
        List<BlockPos> result = new ArrayList<>(reverse.size());
        for (int index = reverse.size() - 1; index >= 0; index--) {
            if (!includeStart && index == reverse.size() - 1) {
                continue;
            }
            result.add(reverse.get(index));
        }
        return result;
    }

    private static int heuristic(BlockPos from, BlockPos to) {
        return Math.abs(from.getX() - to.getX()) + Math.abs(from.getZ() - to.getZ());
    }

    private static double horizontalDistanceSquared(BlockPos first, BlockPos second) {
        double dx = first.getX() + 0.5D - (second.getX() + 0.5D);
        double dz = first.getZ() + 0.5D - (second.getZ() + 0.5D);
        return dx * dx + dz * dz;
    }

    private static <T> void shuffle(List<T> values, RandomSource random) {
        for (int index = values.size() - 1; index > 0; index--) {
            int swap = random.nextInt(index + 1);
            T value = values.get(index);
            values.set(index, values.get(swap));
            values.set(swap, value);
        }
    }

    public record TunnelPlan(
            List<BlockPos> columns,
            BlockPos emergencePosition,
            boolean floorApproach,
            int constructionSections) {
    }

    private record StartCandidate(BlockPos pos, boolean floorApproach) {
    }

    private record EmergenceCandidate(
            BlockPos position,
            List<BlockPos> excavationColumns,
            int constructionSections) {
    }

    private record Node(
            BlockPos pos,
            Node parent,
            int depth,
            int constructionSections,
            int score) {
    }

    private record NodeKey(long pos, int constructionSections) {
    }
}
