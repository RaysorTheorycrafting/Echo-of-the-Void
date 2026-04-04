package com.eotv.echoofthevoid.state;

import com.eotv.echoofthevoid.phase.UncannyPhase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class UncannyWorldState extends SavedData {
    private static final String DATA_NAME = "echoofthevoid_uncanny_world";

    private UncannyPhase phase = UncannyPhase.PHASE_1;
    private double progressToNextPhase;
    private long lastGlobalEventTick = Long.MIN_VALUE;
    private boolean purgeActive;
    private boolean phaseLockActive;
    private int lockedPhaseIndex = 1;
    private long weatherCooldownUntilTick = Long.MIN_VALUE;
    private long weatherNextCheckTick = Long.MIN_VALUE;
    private String activeWeatherEventId = "";
    private long weatherEventEndTick = Long.MIN_VALUE;
    private long weatherAuxTick = Long.MIN_VALUE;
    private int weatherAuxValue;
    private long weatherSavedDayTime = Long.MIN_VALUE;
    private String lastWeatherEventId = "";
    private int lastHeavyVisualWeatherDurationTicks;
    private boolean debugLogsEnabled;
    private long structureCooldownUntilTick = Long.MIN_VALUE;
    private long structureNextCheckTick = Long.MIN_VALUE;
    private long tensionBuilderEndTick = Long.MIN_VALUE;
    private long tensionBuilderNextStartTick = Long.MIN_VALUE;
    private long tensionBuilderGrandEventBoostUntilTick = Long.MIN_VALUE;
    private long tensionBuilderNextGrandEventRollTick = Long.MIN_VALUE;
    private long tensionBuilderLastGrandEventTick = Long.MIN_VALUE;
    private long tensionBuilderLastUpdateTick = Long.MIN_VALUE;
    private long tensionBuilderPendingGrandEventStartTick = Long.MIN_VALUE;
    private String tensionBuilderPendingGrandEventDimension = "";
    private boolean tensionBuilderPendingGrandEventForced;
    private boolean tensionBuilderPendingGrandEventWarningSent;
    private long tensionBuilderPendingGrandEventWarningTick = Long.MIN_VALUE;
    private long tensionBuilderPendingGrandEventDelayTicks = Long.MIN_VALUE;

    private final Map<UUID, Long> lastDeathBoostTick = new HashMap<>();
    private final Map<UUID, Long> lastDeathTick = new HashMap<>();
    private final Map<UUID, Long> lastRespawnTick = new HashMap<>();
    private final Map<UUID, Long> leftBaseSinceTick = new HashMap<>();
    private final Map<UUID, Long> lastDoubleDormantTick = new HashMap<>();
    private final Map<UUID, Long> lastWatcherTick = new HashMap<>();
    private final Map<UUID, Integer> firstNightWatcherTriggered = new HashMap<>();
    private final Map<UUID, Long> restartConfirmUntilTick = new HashMap<>();
    private final Map<UUID, Integer> historyTomeMask = new HashMap<>();
    private final List<StructureMarker> structureMarkers = new ArrayList<>();

    public static UncannyWorldState create() {
        return new UncannyWorldState();
    }

    public static UncannyWorldState load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        UncannyWorldState data = create();
        data.phase = UncannyPhase.fromIndex(tag.getInt("phase"));
        data.progressToNextPhase = tag.getDouble("progressToNextPhase");
        data.lastGlobalEventTick = tag.getLong("lastGlobalEventTick");
        data.purgeActive = tag.getBoolean("purgeActive");
        data.phaseLockActive = tag.getBoolean("phaseLockActive");
        data.lockedPhaseIndex = clampLockPhase(tag.contains("lockedPhaseIndex") ? tag.getInt("lockedPhaseIndex") : data.phase.index());
        data.weatherCooldownUntilTick = tag.getLong("weatherCooldownUntilTick");
        data.weatherNextCheckTick = tag.getLong("weatherNextCheckTick");
        data.activeWeatherEventId = tag.getString("activeWeatherEventId");
        data.weatherEventEndTick = tag.getLong("weatherEventEndTick");
        data.weatherAuxTick = tag.getLong("weatherAuxTick");
        data.weatherAuxValue = tag.getInt("weatherAuxValue");
        data.weatherSavedDayTime = tag.getLong("weatherSavedDayTime");
        data.lastWeatherEventId = tag.getString("lastWeatherEventId");
        data.lastHeavyVisualWeatherDurationTicks = tag.getInt("lastHeavyVisualWeatherDurationTicks");
        data.debugLogsEnabled = tag.getBoolean("debugLogsEnabled");
        data.structureCooldownUntilTick = tag.getLong("structureCooldownUntilTick");
        data.structureNextCheckTick = tag.getLong("structureNextCheckTick");
        data.tensionBuilderEndTick = tag.contains("tensionBuilderEndTick") ? tag.getLong("tensionBuilderEndTick") : Long.MIN_VALUE;
        data.tensionBuilderNextStartTick = tag.contains("tensionBuilderNextStartTick") ? tag.getLong("tensionBuilderNextStartTick") : Long.MIN_VALUE;
        data.tensionBuilderGrandEventBoostUntilTick = tag.contains("tensionBuilderGrandEventBoostUntilTick") ? tag.getLong("tensionBuilderGrandEventBoostUntilTick") : Long.MIN_VALUE;
        data.tensionBuilderNextGrandEventRollTick = tag.contains("tensionBuilderNextGrandEventRollTick") ? tag.getLong("tensionBuilderNextGrandEventRollTick") : Long.MIN_VALUE;
        data.tensionBuilderLastGrandEventTick = tag.contains("tensionBuilderLastGrandEventTick") ? tag.getLong("tensionBuilderLastGrandEventTick") : Long.MIN_VALUE;
        data.tensionBuilderLastUpdateTick = tag.contains("tensionBuilderLastUpdateTick") ? tag.getLong("tensionBuilderLastUpdateTick") : Long.MIN_VALUE;
        data.tensionBuilderPendingGrandEventStartTick = tag.contains("tensionBuilderPendingGrandEventStartTick")
                ? tag.getLong("tensionBuilderPendingGrandEventStartTick")
                : Long.MIN_VALUE;
        data.tensionBuilderPendingGrandEventDimension = tag.getString("tensionBuilderPendingGrandEventDimension");
        data.tensionBuilderPendingGrandEventForced = tag.getBoolean("tensionBuilderPendingGrandEventForced");
        data.tensionBuilderPendingGrandEventWarningSent = tag.getBoolean("tensionBuilderPendingGrandEventWarningSent");
        data.tensionBuilderPendingGrandEventWarningTick = tag.contains("tensionBuilderPendingGrandEventWarningTick")
                ? tag.getLong("tensionBuilderPendingGrandEventWarningTick")
                : Long.MIN_VALUE;
        data.tensionBuilderPendingGrandEventDelayTicks = tag.contains("tensionBuilderPendingGrandEventDelayTicks")
                ? tag.getLong("tensionBuilderPendingGrandEventDelayTicks")
                : Long.MIN_VALUE;

        readLongMap(tag, "lastDeathBoostTick", data.lastDeathBoostTick);
        readLongMap(tag, "lastDeathTick", data.lastDeathTick);
        readLongMap(tag, "lastRespawnTick", data.lastRespawnTick);
        readLongMap(tag, "leftBaseSinceTick", data.leftBaseSinceTick);
        readLongMap(tag, "lastDoubleDormantTick", data.lastDoubleDormantTick);
        readLongMap(tag, "lastWatcherTick", data.lastWatcherTick);
        readIntMap(tag, "firstNightWatcherTriggered", data.firstNightWatcherTriggered);
        readLongMap(tag, "restartConfirmUntilTick", data.restartConfirmUntilTick);
        readIntMap(tag, "historyTomeMask", data.historyTomeMask);
        readStructureMarkers(tag, data.structureMarkers);

        return data;
    }

    public static UncannyWorldState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(
                UncannyWorldState::create,
                UncannyWorldState::load), DATA_NAME);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("phase", phase.index());
        tag.putDouble("progressToNextPhase", progressToNextPhase);
        tag.putLong("lastGlobalEventTick", lastGlobalEventTick);
        tag.putBoolean("purgeActive", purgeActive);
        tag.putBoolean("phaseLockActive", phaseLockActive);
        tag.putInt("lockedPhaseIndex", lockedPhaseIndex);
        tag.putLong("weatherCooldownUntilTick", weatherCooldownUntilTick);
        tag.putLong("weatherNextCheckTick", weatherNextCheckTick);
        tag.putString("activeWeatherEventId", activeWeatherEventId == null ? "" : activeWeatherEventId);
        tag.putLong("weatherEventEndTick", weatherEventEndTick);
        tag.putLong("weatherAuxTick", weatherAuxTick);
        tag.putInt("weatherAuxValue", weatherAuxValue);
        tag.putLong("weatherSavedDayTime", weatherSavedDayTime);
        tag.putString("lastWeatherEventId", lastWeatherEventId == null ? "" : lastWeatherEventId);
        tag.putInt("lastHeavyVisualWeatherDurationTicks", lastHeavyVisualWeatherDurationTicks);
        tag.putBoolean("debugLogsEnabled", debugLogsEnabled);
        tag.putLong("structureCooldownUntilTick", structureCooldownUntilTick);
        tag.putLong("structureNextCheckTick", structureNextCheckTick);
        tag.putLong("tensionBuilderEndTick", tensionBuilderEndTick);
        tag.putLong("tensionBuilderNextStartTick", tensionBuilderNextStartTick);
        tag.putLong("tensionBuilderGrandEventBoostUntilTick", tensionBuilderGrandEventBoostUntilTick);
        tag.putLong("tensionBuilderNextGrandEventRollTick", tensionBuilderNextGrandEventRollTick);
        tag.putLong("tensionBuilderLastGrandEventTick", tensionBuilderLastGrandEventTick);
        tag.putLong("tensionBuilderLastUpdateTick", tensionBuilderLastUpdateTick);
        tag.putLong("tensionBuilderPendingGrandEventStartTick", tensionBuilderPendingGrandEventStartTick);
        tag.putString("tensionBuilderPendingGrandEventDimension", tensionBuilderPendingGrandEventDimension == null ? "" : tensionBuilderPendingGrandEventDimension);
        tag.putBoolean("tensionBuilderPendingGrandEventForced", tensionBuilderPendingGrandEventForced);
        tag.putBoolean("tensionBuilderPendingGrandEventWarningSent", tensionBuilderPendingGrandEventWarningSent);
        tag.putLong("tensionBuilderPendingGrandEventWarningTick", tensionBuilderPendingGrandEventWarningTick);
        tag.putLong("tensionBuilderPendingGrandEventDelayTicks", tensionBuilderPendingGrandEventDelayTicks);

        writeLongMap(tag, "lastDeathBoostTick", lastDeathBoostTick);
        writeLongMap(tag, "lastDeathTick", lastDeathTick);
        writeLongMap(tag, "lastRespawnTick", lastRespawnTick);
        writeLongMap(tag, "leftBaseSinceTick", leftBaseSinceTick);
        writeLongMap(tag, "lastDoubleDormantTick", lastDoubleDormantTick);
        writeLongMap(tag, "lastWatcherTick", lastWatcherTick);
        writeIntMap(tag, "firstNightWatcherTriggered", firstNightWatcherTriggered);
        writeLongMap(tag, "restartConfirmUntilTick", restartConfirmUntilTick);
        writeIntMap(tag, "historyTomeMask", historyTomeMask);
        writeStructureMarkers(tag, structureMarkers);
        return tag;
    }

    public UncannyPhase getPhase() {
        return phase;
    }

    public void setPhase(UncannyPhase phase) {
        this.phase = phase;
        this.setDirty();
    }

    public double getProgressToNextPhase() {
        return progressToNextPhase;
    }

    public void setProgressToNextPhase(double progressToNextPhase) {
        this.progressToNextPhase = clampProgress(progressToNextPhase);
        this.setDirty();
    }

    public long getLastGlobalEventTick() {
        return lastGlobalEventTick;
    }

    public void setLastGlobalEventTick(long tick) {
        this.lastGlobalEventTick = tick;
        this.setDirty();
    }

    public boolean isPurgeActive() {
        return purgeActive;
    }

    public void setPurgeActive(boolean purgeActive) {
        this.purgeActive = purgeActive;
        this.setDirty();
    }

    public boolean isPhaseLockActive() {
        return phaseLockActive;
    }

    public void setPhaseLockActive(boolean phaseLockActive) {
        this.phaseLockActive = phaseLockActive;
        this.setDirty();
    }

    public int getLockedPhaseIndex() {
        return lockedPhaseIndex;
    }

    public void setLockedPhaseIndex(int lockedPhaseIndex) {
        this.lockedPhaseIndex = clampLockPhase(lockedPhaseIndex);
        this.setDirty();
    }

    public boolean isSystemEnabled() {
        return !purgeActive;
    }

    public int getCurrentPhaseIndex() {
        return purgeActive ? 0 : phase.index();
    }

    public long getWeatherCooldownUntilTick() {
        return weatherCooldownUntilTick;
    }

    public void setWeatherCooldownUntilTick(long weatherCooldownUntilTick) {
        this.weatherCooldownUntilTick = weatherCooldownUntilTick;
        this.setDirty();
    }

    public long getWeatherNextCheckTick() {
        return weatherNextCheckTick;
    }

    public void setWeatherNextCheckTick(long weatherNextCheckTick) {
        this.weatherNextCheckTick = weatherNextCheckTick;
        this.setDirty();
    }

    public String getActiveWeatherEventId() {
        return activeWeatherEventId;
    }

    public void setActiveWeatherEventId(String activeWeatherEventId) {
        this.activeWeatherEventId = activeWeatherEventId == null ? "" : activeWeatherEventId;
        this.setDirty();
    }

    public long getWeatherEventEndTick() {
        return weatherEventEndTick;
    }

    public void setWeatherEventEndTick(long weatherEventEndTick) {
        this.weatherEventEndTick = weatherEventEndTick;
        this.setDirty();
    }

    public long getWeatherAuxTick() {
        return weatherAuxTick;
    }

    public void setWeatherAuxTick(long weatherAuxTick) {
        this.weatherAuxTick = weatherAuxTick;
        this.setDirty();
    }

    public int getWeatherAuxValue() {
        return weatherAuxValue;
    }

    public void setWeatherAuxValue(int weatherAuxValue) {
        this.weatherAuxValue = weatherAuxValue;
        this.setDirty();
    }

    public long getWeatherSavedDayTime() {
        return weatherSavedDayTime;
    }

    public void setWeatherSavedDayTime(long weatherSavedDayTime) {
        this.weatherSavedDayTime = weatherSavedDayTime;
        this.setDirty();
    }

    public String getLastWeatherEventId() {
        return lastWeatherEventId;
    }

    public void setLastWeatherEventId(String lastWeatherEventId) {
        this.lastWeatherEventId = lastWeatherEventId == null ? "" : lastWeatherEventId;
        this.setDirty();
    }

    public int getLastHeavyVisualWeatherDurationTicks() {
        return lastHeavyVisualWeatherDurationTicks;
    }

    public void setLastHeavyVisualWeatherDurationTicks(int lastHeavyVisualWeatherDurationTicks) {
        this.lastHeavyVisualWeatherDurationTicks = Math.max(0, lastHeavyVisualWeatherDurationTicks);
        this.setDirty();
    }

    public boolean isDebugLogsEnabled() {
        return debugLogsEnabled;
    }

    public void setDebugLogsEnabled(boolean debugLogsEnabled) {
        this.debugLogsEnabled = debugLogsEnabled;
        this.setDirty();
    }

    public long getStructureCooldownUntilTick() {
        return structureCooldownUntilTick;
    }

    public void setStructureCooldownUntilTick(long structureCooldownUntilTick) {
        this.structureCooldownUntilTick = structureCooldownUntilTick;
        this.setDirty();
    }

    public long getStructureNextCheckTick() {
        return structureNextCheckTick;
    }

    public void setStructureNextCheckTick(long structureNextCheckTick) {
        this.structureNextCheckTick = structureNextCheckTick;
        this.setDirty();
    }

    public long getTensionBuilderEndTick() {
        return tensionBuilderEndTick;
    }

    public void setTensionBuilderEndTick(long tensionBuilderEndTick) {
        this.tensionBuilderEndTick = tensionBuilderEndTick;
        this.setDirty();
    }

    public long getTensionBuilderNextStartTick() {
        return tensionBuilderNextStartTick;
    }

    public void setTensionBuilderNextStartTick(long tensionBuilderNextStartTick) {
        this.tensionBuilderNextStartTick = tensionBuilderNextStartTick;
        this.setDirty();
    }

    public long getTensionBuilderGrandEventBoostUntilTick() {
        return tensionBuilderGrandEventBoostUntilTick;
    }

    public void setTensionBuilderGrandEventBoostUntilTick(long tensionBuilderGrandEventBoostUntilTick) {
        this.tensionBuilderGrandEventBoostUntilTick = tensionBuilderGrandEventBoostUntilTick;
        this.setDirty();
    }

    public long getTensionBuilderNextGrandEventRollTick() {
        return tensionBuilderNextGrandEventRollTick;
    }

    public void setTensionBuilderNextGrandEventRollTick(long tensionBuilderNextGrandEventRollTick) {
        this.tensionBuilderNextGrandEventRollTick = tensionBuilderNextGrandEventRollTick;
        this.setDirty();
    }

    public long getTensionBuilderLastGrandEventTick() {
        return tensionBuilderLastGrandEventTick;
    }

    public void setTensionBuilderLastGrandEventTick(long tensionBuilderLastGrandEventTick) {
        this.tensionBuilderLastGrandEventTick = tensionBuilderLastGrandEventTick;
        this.setDirty();
    }

    public long getTensionBuilderLastUpdateTick() {
        return tensionBuilderLastUpdateTick;
    }

    public void setTensionBuilderLastUpdateTick(long tensionBuilderLastUpdateTick) {
        this.tensionBuilderLastUpdateTick = tensionBuilderLastUpdateTick;
        this.setDirty();
    }

    public long getTensionBuilderPendingGrandEventStartTick() {
        return tensionBuilderPendingGrandEventStartTick;
    }

    public void setTensionBuilderPendingGrandEventStartTick(long tensionBuilderPendingGrandEventStartTick) {
        this.tensionBuilderPendingGrandEventStartTick = tensionBuilderPendingGrandEventStartTick;
        this.setDirty();
    }

    public String getTensionBuilderPendingGrandEventDimension() {
        return tensionBuilderPendingGrandEventDimension;
    }

    public void setTensionBuilderPendingGrandEventDimension(String tensionBuilderPendingGrandEventDimension) {
        this.tensionBuilderPendingGrandEventDimension = tensionBuilderPendingGrandEventDimension == null ? "" : tensionBuilderPendingGrandEventDimension;
        this.setDirty();
    }

    public boolean isTensionBuilderPendingGrandEventWarningSent() {
        return tensionBuilderPendingGrandEventWarningSent;
    }

    public void setTensionBuilderPendingGrandEventWarningSent(boolean tensionBuilderPendingGrandEventWarningSent) {
        this.tensionBuilderPendingGrandEventWarningSent = tensionBuilderPendingGrandEventWarningSent;
        this.setDirty();
    }

    public boolean isTensionBuilderPendingGrandEventForced() {
        return tensionBuilderPendingGrandEventForced;
    }

    public void setTensionBuilderPendingGrandEventForced(boolean tensionBuilderPendingGrandEventForced) {
        this.tensionBuilderPendingGrandEventForced = tensionBuilderPendingGrandEventForced;
        this.setDirty();
    }

    public long getTensionBuilderPendingGrandEventWarningTick() {
        return tensionBuilderPendingGrandEventWarningTick;
    }

    public void setTensionBuilderPendingGrandEventWarningTick(long tensionBuilderPendingGrandEventWarningTick) {
        this.tensionBuilderPendingGrandEventWarningTick = tensionBuilderPendingGrandEventWarningTick;
        this.setDirty();
    }

    public long getTensionBuilderPendingGrandEventDelayTicks() {
        return tensionBuilderPendingGrandEventDelayTicks;
    }

    public void setTensionBuilderPendingGrandEventDelayTicks(long tensionBuilderPendingGrandEventDelayTicks) {
        this.tensionBuilderPendingGrandEventDelayTicks = tensionBuilderPendingGrandEventDelayTicks;
        this.setDirty();
    }

    public List<StructureMarker> getStructureMarkers() {
        return List.copyOf(structureMarkers);
    }

    public void addStructureMarker(String type, ResourceKey<Level> dimension, BlockPos pos) {
        String normalizedType = normalizeType(type);
        String dimensionId = normalizeDimension(dimension);
        if (normalizedType == null || dimensionId == null || pos == null) {
            return;
        }
        structureMarkers.add(new StructureMarker(normalizedType, dimensionId, pos.asLong()));
        pruneStructureMarkers();
        this.setDirty();
    }

    public boolean hasStructureMarkerNearby(String type, ResourceKey<Level> dimension, BlockPos pos, int radius) {
        String normalizedType = normalizeType(type);
        String dimensionId = normalizeDimension(dimension);
        if (normalizedType == null || dimensionId == null || pos == null || radius <= 0) {
            return false;
        }
        long radiusSq = (long) radius * radius;
        for (StructureMarker marker : structureMarkers) {
            if (!normalizedType.equals(marker.type()) || !dimensionId.equals(marker.dimension())) {
                continue;
            }
            if (BlockPos.of(marker.posLong()).distSqr(pos) <= radiusSq) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyStructureMarkerNearby(ResourceKey<Level> dimension, BlockPos pos, int radius) {
        String dimensionId = normalizeDimension(dimension);
        if (dimensionId == null || pos == null || radius <= 0) {
            return false;
        }
        long radiusSq = (long) radius * radius;
        for (StructureMarker marker : structureMarkers) {
            if (!dimensionId.equals(marker.dimension())) {
                continue;
            }
            if (BlockPos.of(marker.posLong()).distSqr(pos) <= radiusSq) {
                return true;
            }
        }
        return false;
    }

    public BlockPos findNearestStructureMarker(String type, ResourceKey<Level> dimension, BlockPos from) {
        String normalizedType = normalizeType(type);
        String dimensionId = normalizeDimension(dimension);
        if (normalizedType == null || dimensionId == null || from == null) {
            return null;
        }

        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (StructureMarker marker : structureMarkers) {
            if (!normalizedType.equals(marker.type()) || !dimensionId.equals(marker.dimension())) {
                continue;
            }
            BlockPos markerPos = BlockPos.of(marker.posLong());
            double distSq = markerPos.distSqr(from);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = markerPos;
            }
        }
        return nearest;
    }

    public Long getRestartConfirmUntilTick(UUID playerId) {
        return restartConfirmUntilTick.get(playerId);
    }

    public void setRestartConfirmUntilTick(UUID playerId, long tick) {
        restartConfirmUntilTick.put(playerId, tick);
        this.setDirty();
    }

    public void clearRestartConfirmUntilTick(UUID playerId) {
        if (restartConfirmUntilTick.remove(playerId) != null) {
            this.setDirty();
        }
    }

    public Long getLastDeathBoostTick(UUID playerId) {
        return lastDeathBoostTick.get(playerId);
    }

    public void setLastDeathBoostTick(UUID playerId, long tick) {
        lastDeathBoostTick.put(playerId, tick);
        this.setDirty();
    }

    public Long getLastDeathTick(UUID playerId) {
        return lastDeathTick.get(playerId);
    }

    public void setLastDeathTick(UUID playerId, long tick) {
        lastDeathTick.put(playerId, tick);
        this.setDirty();
    }

    public Long getLastRespawnTick(UUID playerId) {
        return lastRespawnTick.get(playerId);
    }

    public void setLastRespawnTick(UUID playerId, long tick) {
        lastRespawnTick.put(playerId, tick);
        this.setDirty();
    }

    public Long getLeftBaseSinceTick(UUID playerId) {
        return leftBaseSinceTick.get(playerId);
    }

    public void setLeftBaseSinceTick(UUID playerId, long tick) {
        leftBaseSinceTick.put(playerId, tick);
        this.setDirty();
    }

    public void clearLeftBaseSinceTick(UUID playerId) {
        leftBaseSinceTick.remove(playerId);
        this.setDirty();
    }

    public Long getLastDoubleDormantTick(UUID playerId) {
        return lastDoubleDormantTick.get(playerId);
    }

    public void setLastDoubleDormantTick(UUID playerId, long tick) {
        lastDoubleDormantTick.put(playerId, tick);
        this.setDirty();
    }

    public Long getLastWatcherTick(UUID playerId) {
        return lastWatcherTick.get(playerId);
    }

    public void setLastWatcherTick(UUID playerId, long tick) {
        lastWatcherTick.put(playerId, tick);
        this.setDirty();
    }

    public boolean isFirstNightWatcherTriggered(UUID playerId) {
        return firstNightWatcherTriggered.getOrDefault(playerId, 0) != 0;
    }

    public void markFirstNightWatcherTriggered(UUID playerId) {
        firstNightWatcherTriggered.put(playerId, 1);
        this.setDirty();
    }

    public int findFirstMissingHistoryTome(UUID playerId, int maxTomes) {
        if (playerId == null || maxTomes <= 0) {
            return -1;
        }
        int mask = historyTomeMask.getOrDefault(playerId, 0);
        for (int tome = 1; tome <= Math.min(30, maxTomes); tome++) {
            int bit = 1 << (tome - 1);
            if ((mask & bit) == 0) {
                return tome;
            }
        }
        return -1;
    }

    public void markHistoryTomeFound(UUID playerId, int tomeIndex) {
        if (playerId == null || tomeIndex <= 0 || tomeIndex > 30) {
            return;
        }
        int bit = 1 << (tomeIndex - 1);
        int currentMask = historyTomeMask.getOrDefault(playerId, 0);
        int nextMask = currentMask | bit;
        if (nextMask != currentMask) {
            historyTomeMask.put(playerId, nextMask);
            this.setDirty();
        }
    }

    public void addProgress(double progressDelta) {
        if (purgeActive || phaseLockActive) {
            return;
        }

        if (phase.isFinal()) {
            progressToNextPhase = 1.0D;
            this.setDirty();
            return;
        }

        progressToNextPhase = clampProgress(progressToNextPhase + progressDelta);
        this.setDirty();
    }

    public boolean tryAdvancePhaseOneStep() {
        if (purgeActive || phaseLockActive || phase.isFinal() || progressToNextPhase < 1.0D) {
            return false;
        }

        phase = phase.next();
        if (phase.isFinal()) {
            progressToNextPhase = 1.0D;
        } else {
            progressToNextPhase = clampProgress(progressToNextPhase - 1.0D);
        }

        this.setDirty();
        return true;
    }

    private static double clampProgress(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static int clampLockPhase(int phaseIndex) {
        return Math.max(1, Math.min(4, phaseIndex));
    }

    private void pruneStructureMarkers() {
        int maxMarkers = 512;
        if (structureMarkers.size() <= maxMarkers) {
            return;
        }
        int removeCount = structureMarkers.size() - maxMarkers;
        for (int i = 0; i < removeCount; i++) {
            structureMarkers.remove(0);
        }
    }

    private static String normalizeType(String type) {
        if (type == null) {
            return null;
        }
        String trimmed = type.trim().toLowerCase(java.util.Locale.ROOT);
        return trimmed.isBlank() ? null : trimmed;
    }

    private static String normalizeDimension(ResourceKey<Level> dimension) {
        return dimension == null ? null : dimension.location().toString();
    }

    private static void writeLongMap(CompoundTag parent, String key, Map<UUID, Long> map) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Long> entry : map.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putUUID("player", entry.getKey());
            item.putLong("tick", entry.getValue());
            list.add(item);
        }
        parent.put(key, list);
    }

    private static void readLongMap(CompoundTag parent, String key, Map<UUID, Long> map) {
        map.clear();
        ListTag list = parent.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            if (item.hasUUID("player")) {
                map.put(item.getUUID("player"), item.getLong("tick"));
            }
        }
    }

    private static void writeIntMap(CompoundTag parent, String key, Map<UUID, Integer> map) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Integer> entry : map.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putUUID("player", entry.getKey());
            item.putInt("value", entry.getValue());
            list.add(item);
        }
        parent.put(key, list);
    }

    private static void readIntMap(CompoundTag parent, String key, Map<UUID, Integer> map) {
        map.clear();
        ListTag list = parent.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            if (item.hasUUID("player")) {
                map.put(item.getUUID("player"), item.getInt("value"));
            }
        }
    }

    private static void writeStructureMarkers(CompoundTag parent, List<StructureMarker> markers) {
        ListTag list = new ListTag();
        for (StructureMarker marker : markers) {
            CompoundTag item = new CompoundTag();
            item.putString("type", marker.type());
            item.putString("dimension", marker.dimension());
            item.putLong("pos", marker.posLong());
            list.add(item);
        }
        parent.put("structureMarkers", list);
    }

    private static void readStructureMarkers(CompoundTag parent, List<StructureMarker> markers) {
        markers.clear();
        ListTag list = parent.getList("structureMarkers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            String type = normalizeType(item.getString("type"));
            String dimension = item.getString("dimension");
            if (type == null || dimension == null || dimension.isBlank()) {
                continue;
            }
            markers.add(new StructureMarker(type, dimension, item.getLong("pos")));
        }
    }

    public record StructureMarker(String type, String dimension, long posLong) {
    }
}

