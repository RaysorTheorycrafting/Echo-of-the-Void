package com.eotv.echoofthevoid.event.special;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Persisted, additive state for one isolated Elsewhere trial. */
public final class DevourerArenaSession {
    public static final long TRIAL_TICKS = 20L * 60L;

    private final UUID playerId;
    private final UUID sourceDevourerId;
    private final String originDimension;
    private final double originX;
    private final double originY;
    private final double originZ;
    private final float originYaw;
    private final float originPitch;
    private final long cellIndex;
    private final CompoundTag entryInventory;
    private final Map<Long, PlacedBlock> placedBlocks = new LinkedHashMap<>();
    private final Set<Long> brokenArenaBlocks = new LinkedHashSet<>();
    private long remainingTicks;
    private long lastOnlineEpochMillis;
    private int spawnedPursuers;
    private int lastKnownAlivePursuers;
    private int replacementsUsed;
    private long lossDetectedElapsedTick = -1L;
    // Session-only: a reload simply restarts the short respite before a missing climber returns.
    private long climberLossElapsedTick = -1L;
    private long nextCoverageElapsedTick;
    private Status status;

    public DevourerArenaSession(
            UUID playerId,
            UUID sourceDevourerId,
            String originDimension,
            double originX,
            double originY,
            double originZ,
            float originYaw,
            float originPitch,
            long cellIndex,
            CompoundTag entryInventory,
            long nowEpochMillis) {
        this(
                playerId,
                sourceDevourerId,
                originDimension,
                originX,
                originY,
                originZ,
                originYaw,
                originPitch,
                cellIndex,
                entryInventory,
                TRIAL_TICKS,
                nowEpochMillis,
                0,
                Status.ACTIVE);
    }

    private DevourerArenaSession(
            UUID playerId,
            UUID sourceDevourerId,
            String originDimension,
            double originX,
            double originY,
            double originZ,
            float originYaw,
            float originPitch,
            long cellIndex,
            CompoundTag entryInventory,
            long remainingTicks,
            long lastOnlineEpochMillis,
            int spawnedPursuers,
            Status status) {
        this.playerId = playerId;
        this.sourceDevourerId = sourceDevourerId;
        this.originDimension = originDimension == null ? "minecraft:overworld" : originDimension;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.originYaw = originYaw;
        this.originPitch = originPitch;
        this.cellIndex = Math.max(0L, cellIndex);
        this.entryInventory = entryInventory == null ? new CompoundTag() : entryInventory.copy();
        this.remainingTicks = Math.max(0L, Math.min(TRIAL_TICKS, remainingTicks));
        this.lastOnlineEpochMillis = Math.max(0L, lastOnlineEpochMillis);
        this.spawnedPursuers = DevourerArenaRules.clampPersistedPursuerCount(spawnedPursuers);
        this.status = status == null ? Status.ACTIVE : status;
    }

    public UUID playerId() {
        return playerId;
    }

    public UUID sourceDevourerId() {
        return sourceDevourerId;
    }

    public String originDimension() {
        return originDimension;
    }

    public double originX() {
        return originX;
    }

    public double originY() {
        return originY;
    }

    public double originZ() {
        return originZ;
    }

    public float originYaw() {
        return originYaw;
    }

    public float originPitch() {
        return originPitch;
    }

    public long cellIndex() {
        return cellIndex;
    }

    public CompoundTag entryInventory() {
        return entryInventory.copy();
    }

    public long remainingTicks() {
        return remainingTicks;
    }

    public void tickRemaining() {
        if (status == Status.ACTIVE && remainingTicks > 0L) {
            remainingTicks--;
        }
    }

    public void setRemainingTicks(long remainingTicks) {
        this.remainingTicks = Math.max(0L, Math.min(TRIAL_TICKS, remainingTicks));
    }

    public long elapsedTicks() {
        return TRIAL_TICKS - remainingTicks;
    }

    public long lastOnlineEpochMillis() {
        return lastOnlineEpochMillis;
    }

    public void markOnline(long nowEpochMillis) {
        this.lastOnlineEpochMillis = Math.max(0L, nowEpochMillis);
    }

    public int spawnedPursuers() {
        return spawnedPursuers;
    }

    public void setSpawnedPursuers(int spawnedPursuers) {
        this.spawnedPursuers = DevourerArenaRules.clampPersistedPursuerCount(spawnedPursuers);
    }

    public int lastKnownAlivePursuers() {
        return lastKnownAlivePursuers;
    }

    public void setLastKnownAlivePursuers(int count) {
        this.lastKnownAlivePursuers = DevourerArenaRules.clampPersistedPursuerCount(count);
    }

    public int replacementsUsed() {
        return replacementsUsed;
    }

    public void recordReplacement() {
        replacementsUsed = DevourerArenaRules.clampReplacementCount(replacementsUsed + 1);
    }

    public long climberLossElapsedTick() {
        return climberLossElapsedTick;
    }

    public void markClimberLoss(long elapsedTick) {
        climberLossElapsedTick = Math.max(0L, elapsedTick);
    }

    public void clearClimberLoss() {
        climberLossElapsedTick = -1L;
    }

    public long lossDetectedElapsedTick() {
        return lossDetectedElapsedTick;
    }

    public void markLossDetected(long elapsedTick) {
        if (lossDetectedElapsedTick < 0L) {
            lossDetectedElapsedTick = Math.max(0L, elapsedTick);
        }
    }

    public void clearLossDetected() {
        lossDetectedElapsedTick = -1L;
    }

    public long nextCoverageElapsedTick() {
        return nextCoverageElapsedTick;
    }

    public void scheduleNextCoverage(long elapsedTick) {
        nextCoverageElapsedTick = Math.max(0L, elapsedTick) + DevourerArenaRules.COVERAGE_REEVALUATION_TICKS;
    }

    public Status status() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status == null ? Status.ACTIVE : status;
    }

    public Map<Long, PlacedBlock> placedBlocks() {
        return Map.copyOf(placedBlocks);
    }

    public void rememberPlacedBlock(long packedPos, String blockId) {
        if (placedBlocks.size() < 2048 && blockId != null && !blockId.isBlank()) {
            placedBlocks.put(packedPos, new PlacedBlock(blockId, false));
        }
    }

    public PlacedBlock forgetPlayerBrokenPlacement(long packedPos) {
        return placedBlocks.remove(packedPos);
    }

    public boolean markPlacementRemovedByPursuer(long packedPos) {
        PlacedBlock placed = placedBlocks.get(packedPos);
        if (placed == null) {
            return false;
        }
        placedBlocks.put(packedPos, new PlacedBlock(placed.blockId(), true));
        return true;
    }

    public boolean containsPlacement(long packedPos) {
        return placedBlocks.containsKey(packedPos);
    }

    public void rememberBrokenArenaBlock(long packedPos) {
        if (brokenArenaBlocks.size() < 4096) {
            brokenArenaBlocks.add(packedPos);
        }
    }

    public Set<Long> brokenArenaBlocks() {
        return Set.copyOf(brokenArenaBlocks);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Player", playerId);
        if (sourceDevourerId != null) {
            tag.putUUID("SourceDevourer", sourceDevourerId);
        }
        tag.putString("OriginDimension", originDimension);
        tag.putDouble("OriginX", originX);
        tag.putDouble("OriginY", originY);
        tag.putDouble("OriginZ", originZ);
        tag.putFloat("OriginYaw", originYaw);
        tag.putFloat("OriginPitch", originPitch);
        tag.putLong("CellIndex", cellIndex);
        tag.put("EntryInventory", entryInventory.copy());
        tag.putLong("RemainingTicks", remainingTicks);
        tag.putLong("LastOnlineEpochMillis", lastOnlineEpochMillis);
        tag.putInt("SpawnedPursuers", spawnedPursuers);
        tag.putInt("LastKnownAlivePursuers", lastKnownAlivePursuers);
        tag.putInt("ArenaReplacementsUsed", replacementsUsed);
        tag.putLong("ArenaLossDetectedElapsedTick", lossDetectedElapsedTick);
        tag.putLong("ArenaNextCoverageElapsedTick", nextCoverageElapsedTick);
        tag.putString("Status", status.name());

        ListTag placements = new ListTag();
        for (Map.Entry<Long, PlacedBlock> entry : placedBlocks.entrySet()) {
            CompoundTag placement = new CompoundTag();
            placement.putLong("Pos", entry.getKey());
            placement.putString("Block", entry.getValue().blockId());
            placement.putBoolean("RemovedByPursuer", entry.getValue().removedByPursuer());
            placements.add(placement);
        }
        tag.put("PlacedBlocks", placements);
        tag.putLongArray("BrokenArenaBlocks", brokenArenaBlocks.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }

    public static DevourerArenaSession load(CompoundTag tag) {
        if (tag == null || !tag.hasUUID("Player")) {
            return null;
        }
        DevourerArenaSession session = new DevourerArenaSession(
                tag.getUUID("Player"),
                tag.hasUUID("SourceDevourer") ? tag.getUUID("SourceDevourer") : null,
                tag.getString("OriginDimension"),
                tag.getDouble("OriginX"),
                tag.getDouble("OriginY"),
                tag.getDouble("OriginZ"),
                tag.getFloat("OriginYaw"),
                tag.getFloat("OriginPitch"),
                tag.getLong("CellIndex"),
                tag.getCompound("EntryInventory"),
                tag.contains("RemainingTicks") ? tag.getLong("RemainingTicks") : TRIAL_TICKS,
                tag.getLong("LastOnlineEpochMillis"),
                tag.getInt("SpawnedPursuers"),
                Status.fromName(tag.getString("Status")));
        session.lastKnownAlivePursuers = DevourerArenaRules.clampPersistedPursuerCount(
                tag.getInt("LastKnownAlivePursuers"));
        session.replacementsUsed = DevourerArenaRules.clampReplacementCount(
                tag.getInt("ArenaReplacementsUsed"));
        session.lossDetectedElapsedTick = tag.contains("ArenaLossDetectedElapsedTick")
                ? Math.max(-1L, tag.getLong("ArenaLossDetectedElapsedTick"))
                : -1L;
        session.nextCoverageElapsedTick = tag.contains("ArenaNextCoverageElapsedTick")
                ? Math.max(0L, tag.getLong("ArenaNextCoverageElapsedTick"))
                : 0L;
        ListTag placements = tag.getList("PlacedBlocks", Tag.TAG_COMPOUND);
        for (int index = 0; index < placements.size() && session.placedBlocks.size() < 2048; index++) {
            CompoundTag placement = placements.getCompound(index);
            String blockId = placement.getString("Block");
            if (!blockId.isBlank()) {
                session.placedBlocks.put(
                        placement.getLong("Pos"),
                        new PlacedBlock(blockId, placement.getBoolean("RemovedByPursuer")));
            }
        }
        for (long packed : tag.getLongArray("BrokenArenaBlocks")) {
            if (session.brokenArenaBlocks.size() >= 4096) {
                break;
            }
            session.brokenArenaBlocks.add(packed);
        }
        return session;
    }

    public record PlacedBlock(String blockId, boolean removedByPursuer) {
    }

    public enum Status {
        ACTIVE,
        AWAITING_RESPAWN,
        RETURN_PENDING;

        static Status fromName(String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException | NullPointerException ignored) {
                return ACTIVE;
            }
        }
    }
}
