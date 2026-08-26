package com.eotv.echoofthevoid.state;

import com.eotv.echoofthevoid.campaign.CampaignCulminationState;
import com.eotv.echoofthevoid.lore.UncannyJournalCatalog;
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
    private String weatherTargetPlayerUuid = "";
    private long weatherSavedDayTime = Long.MIN_VALUE;
    private String lastWeatherEventId = "";
    private int lastHeavyVisualWeatherDurationTicks;
    private int localizedWeatherX;
    private int localizedWeatherY;
    private int localizedWeatherZ;
    private double localizedWeatherDirectionX;
    private double localizedWeatherDirectionZ;
    private int localizedWeatherRadius;
    private long localizedWeatherSeed;
    private long localizedWeatherStartTick = Long.MIN_VALUE;
    private String localizedWeatherData = "";
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
    private boolean beaconFragmentOccurred;
    private boolean mournerOccurred;
    private boolean campaignDirectorInitialized;
    private long campaignElapsedTicks;
    private long campaignLastObservedDayTime = Long.MIN_VALUE;
    private long campaignDirectorSeed;
    private String campaignBeat = "UNEASE";
    private long campaignBeatRemainingTicks;
    private int campaignBeatSequence;
    private long campaignLastStrongEventTick = Long.MIN_VALUE;
    private String campaignCulminationState = CampaignCulminationState.UNINITIALIZED.name();
    private long campaignCulminationScheduledTick = Long.MIN_VALUE;
    private long campaignCulminationRetryTick = Long.MIN_VALUE;

    private final Map<UUID, Long> lastDeathBoostTick = new HashMap<>();
    private final Map<UUID, Long> lastDeathTick = new HashMap<>();
    private final Map<UUID, Long> lastRespawnTick = new HashMap<>();
    private final Map<UUID, Long> leftBaseSinceTick = new HashMap<>();
    private final Map<UUID, Long> lastDoubleDormantTick = new HashMap<>();
    private final Map<UUID, Long> lastWatcherTick = new HashMap<>();
    private final Map<UUID, Integer> firstNightWatcherTriggered = new HashMap<>();
    private final Map<UUID, Long> restartConfirmUntilTick = new HashMap<>();
    private final Map<UUID, Integer> historyTomeMask = new HashMap<>();
    private final Map<UUID, DeathSite> deathSites = new HashMap<>();
    private final List<StructureMarker> structureMarkers = new ArrayList<>();
    private final List<Long> playerPlacedLights = new ArrayList<>();
    private final List<String> campaignRecentFamilies = new ArrayList<>();

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
        data.weatherTargetPlayerUuid = tag.getString("weatherTargetPlayerUuid");
        data.weatherSavedDayTime = tag.getLong("weatherSavedDayTime");
        data.lastWeatherEventId = tag.getString("lastWeatherEventId");
        data.lastHeavyVisualWeatherDurationTicks = tag.getInt("lastHeavyVisualWeatherDurationTicks");
        data.localizedWeatherX = tag.getInt("localizedWeatherX");
        data.localizedWeatherY = tag.getInt("localizedWeatherY");
        data.localizedWeatherZ = tag.getInt("localizedWeatherZ");
        data.localizedWeatherDirectionX = tag.getDouble("localizedWeatherDirectionX");
        data.localizedWeatherDirectionZ = tag.getDouble("localizedWeatherDirectionZ");
        data.localizedWeatherRadius = tag.getInt("localizedWeatherRadius");
        data.localizedWeatherSeed = tag.getLong("localizedWeatherSeed");
        data.localizedWeatherStartTick = tag.contains("localizedWeatherStartTick")
                ? tag.getLong("localizedWeatherStartTick") : Long.MIN_VALUE;
        data.localizedWeatherData = tag.getString("localizedWeatherData");
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
        data.beaconFragmentOccurred = tag.getBoolean("beaconFragmentOccurred");
        data.mournerOccurred = tag.getBoolean("mournerOccurred");
        data.campaignDirectorInitialized = tag.getBoolean("campaignDirectorInitialized");
        data.campaignElapsedTicks = Math.max(0L, tag.getLong("campaignElapsedTicks"));
        data.campaignLastObservedDayTime = tag.contains("campaignLastObservedDayTime")
                ? tag.getLong("campaignLastObservedDayTime") : Long.MIN_VALUE;
        data.campaignDirectorSeed = tag.getLong("campaignDirectorSeed");
        data.campaignBeat = tag.getString("campaignBeat");
        data.campaignBeatRemainingTicks = tag.getLong("campaignBeatRemainingTicks");
        data.campaignBeatSequence = Math.max(0, tag.getInt("campaignBeatSequence"));
        data.campaignLastStrongEventTick = tag.contains("campaignLastStrongEventTick")
                ? tag.getLong("campaignLastStrongEventTick") : Long.MIN_VALUE;
        data.campaignCulminationState = CampaignCulminationState.fromSavedName(
                tag.getString("campaignCulminationState")).name();
        data.campaignCulminationScheduledTick = tag.contains("campaignCulminationScheduledTick")
                ? tag.getLong("campaignCulminationScheduledTick") : Long.MIN_VALUE;
        data.campaignCulminationRetryTick = tag.contains("campaignCulminationRetryTick")
                ? tag.getLong("campaignCulminationRetryTick") : Long.MIN_VALUE;

        readLongMap(tag, "lastDeathBoostTick", data.lastDeathBoostTick);
        readLongMap(tag, "lastDeathTick", data.lastDeathTick);
        readLongMap(tag, "lastRespawnTick", data.lastRespawnTick);
        readLongMap(tag, "leftBaseSinceTick", data.leftBaseSinceTick);
        readLongMap(tag, "lastDoubleDormantTick", data.lastDoubleDormantTick);
        readLongMap(tag, "lastWatcherTick", data.lastWatcherTick);
        readIntMap(tag, "firstNightWatcherTriggered", data.firstNightWatcherTriggered);
        readLongMap(tag, "restartConfirmUntilTick", data.restartConfirmUntilTick);
        readIntMap(tag, "historyTomeMask", data.historyTomeMask);
        readDeathSites(tag, data.deathSites);
        readStructureMarkers(tag, data.structureMarkers);
        readStringList(tag, "campaignRecentFamilies", data.campaignRecentFamilies, 6);
        for (long packedPos : tag.getLongArray("playerPlacedLights")) {
            if (!data.playerPlacedLights.contains(packedPos) && data.playerPlacedLights.size() < 256) {
                data.playerPlacedLights.add(packedPos);
            }
        }

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
        tag.putString("weatherTargetPlayerUuid", weatherTargetPlayerUuid == null ? "" : weatherTargetPlayerUuid);
        tag.putLong("weatherSavedDayTime", weatherSavedDayTime);
        tag.putString("lastWeatherEventId", lastWeatherEventId == null ? "" : lastWeatherEventId);
        tag.putInt("lastHeavyVisualWeatherDurationTicks", lastHeavyVisualWeatherDurationTicks);
        tag.putInt("localizedWeatherX", localizedWeatherX);
        tag.putInt("localizedWeatherY", localizedWeatherY);
        tag.putInt("localizedWeatherZ", localizedWeatherZ);
        tag.putDouble("localizedWeatherDirectionX", localizedWeatherDirectionX);
        tag.putDouble("localizedWeatherDirectionZ", localizedWeatherDirectionZ);
        tag.putInt("localizedWeatherRadius", localizedWeatherRadius);
        tag.putLong("localizedWeatherSeed", localizedWeatherSeed);
        tag.putLong("localizedWeatherStartTick", localizedWeatherStartTick);
        tag.putString("localizedWeatherData", localizedWeatherData == null ? "" : localizedWeatherData);
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
        tag.putBoolean("beaconFragmentOccurred", beaconFragmentOccurred);
        tag.putBoolean("mournerOccurred", mournerOccurred);
        tag.putBoolean("campaignDirectorInitialized", campaignDirectorInitialized);
        tag.putLong("campaignElapsedTicks", campaignElapsedTicks);
        tag.putLong("campaignLastObservedDayTime", campaignLastObservedDayTime);
        tag.putLong("campaignDirectorSeed", campaignDirectorSeed);
        tag.putString("campaignBeat", campaignBeat == null ? "UNEASE" : campaignBeat);
        tag.putLong("campaignBeatRemainingTicks", campaignBeatRemainingTicks);
        tag.putInt("campaignBeatSequence", campaignBeatSequence);
        tag.putLong("campaignLastStrongEventTick", campaignLastStrongEventTick);
        tag.putString("campaignCulminationState", CampaignCulminationState.fromSavedName(
                campaignCulminationState).name());
        tag.putLong("campaignCulminationScheduledTick", campaignCulminationScheduledTick);
        tag.putLong("campaignCulminationRetryTick", campaignCulminationRetryTick);

        writeLongMap(tag, "lastDeathBoostTick", lastDeathBoostTick);
        writeLongMap(tag, "lastDeathTick", lastDeathTick);
        writeLongMap(tag, "lastRespawnTick", lastRespawnTick);
        writeLongMap(tag, "leftBaseSinceTick", leftBaseSinceTick);
        writeLongMap(tag, "lastDoubleDormantTick", lastDoubleDormantTick);
        writeLongMap(tag, "lastWatcherTick", lastWatcherTick);
        writeIntMap(tag, "firstNightWatcherTriggered", firstNightWatcherTriggered);
        writeLongMap(tag, "restartConfirmUntilTick", restartConfirmUntilTick);
        writeIntMap(tag, "historyTomeMask", historyTomeMask);
        writeDeathSites(tag, deathSites);
        writeStructureMarkers(tag, structureMarkers);
        writeStringList(tag, "campaignRecentFamilies", campaignRecentFamilies);
        tag.putLongArray("playerPlacedLights", playerPlacedLights);
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

    public boolean hasBeaconFragmentOccurred() {
        return beaconFragmentOccurred;
    }

    public void markBeaconFragmentOccurred() {
        if (!beaconFragmentOccurred) {
            beaconFragmentOccurred = true;
            this.setDirty();
        }
    }

    public boolean hasMournerOccurred() {
        return mournerOccurred;
    }

    public boolean isCampaignDirectorInitialized() {
        return campaignDirectorInitialized;
    }

    public void initializeCampaignDirector(
            long elapsedTicks,
            long observedDayTime,
            long seed,
            String beat,
            long beatRemainingTicks) {
        campaignDirectorInitialized = true;
        campaignElapsedTicks = Math.max(0L, elapsedTicks);
        campaignLastObservedDayTime = observedDayTime;
        campaignDirectorSeed = seed;
        campaignBeat = beat == null || beat.isBlank() ? "UNEASE" : beat;
        campaignBeatRemainingTicks = Math.max(1L, beatRemainingTicks);
        campaignBeatSequence = 0;
        campaignLastStrongEventTick = Long.MIN_VALUE;
        campaignCulminationState = CampaignCulminationState.UNINITIALIZED.name();
        campaignCulminationScheduledTick = Long.MIN_VALUE;
        campaignCulminationRetryTick = Long.MIN_VALUE;
        campaignRecentFamilies.clear();
        this.setDirty();
    }

    public void resetCampaignDirector() {
        campaignDirectorInitialized = false;
        campaignElapsedTicks = 0L;
        campaignLastObservedDayTime = Long.MIN_VALUE;
        campaignDirectorSeed = 0L;
        campaignBeat = "UNEASE";
        campaignBeatRemainingTicks = 0L;
        campaignBeatSequence = 0;
        campaignLastStrongEventTick = Long.MIN_VALUE;
        campaignCulminationState = CampaignCulminationState.UNINITIALIZED.name();
        campaignCulminationScheduledTick = Long.MIN_VALUE;
        campaignCulminationRetryTick = Long.MIN_VALUE;
        campaignRecentFamilies.clear();
        this.setDirty();
    }

    public long getCampaignElapsedTicks() {
        return campaignElapsedTicks;
    }

    public long getCampaignLastObservedDayTime() {
        return campaignLastObservedDayTime;
    }

    public void observeCampaignDayTime(long dayTime) {
        if (campaignLastObservedDayTime != dayTime) {
            campaignLastObservedDayTime = dayTime;
            this.setDirty();
        }
    }

    public void advanceCampaignDirector(long elapsedDelta, long observedDayTime) {
        long clampedDelta = Math.max(0L, elapsedDelta);
        campaignElapsedTicks = saturatingAdd(campaignElapsedTicks, clampedDelta);
        campaignBeatRemainingTicks -= clampedDelta;
        campaignLastObservedDayTime = observedDayTime;
        this.setDirty();
    }

    public long getCampaignDirectorSeed() {
        return campaignDirectorSeed;
    }

    public String getCampaignBeat() {
        return campaignBeat;
    }

    public long getCampaignBeatRemainingTicks() {
        return campaignBeatRemainingTicks;
    }

    public int getCampaignBeatSequence() {
        return campaignBeatSequence;
    }

    public void startCampaignBeat(String beat, long durationTicks, int sequence) {
        long overdueTicks = Math.min(0L, campaignBeatRemainingTicks);
        campaignBeat = beat == null || beat.isBlank() ? "UNEASE" : beat;
        campaignBeatRemainingTicks = Math.max(Long.MIN_VALUE + 1L, durationTicks + overdueTicks);
        campaignBeatSequence = Math.max(0, sequence);
        this.setDirty();
    }

    public long getCampaignLastStrongEventTick() {
        return campaignLastStrongEventTick;
    }

    public void setCampaignLastStrongEventTick(long tick) {
        campaignLastStrongEventTick = tick;
        this.setDirty();
    }

    public CampaignCulminationState getCampaignCulminationState() {
        return CampaignCulminationState.fromSavedName(campaignCulminationState);
    }

    public long getCampaignCulminationScheduledTick() {
        return campaignCulminationScheduledTick;
    }

    public long getCampaignCulminationRetryTick() {
        return campaignCulminationRetryTick;
    }

    public void scheduleCampaignCulmination(long scheduledTick) {
        campaignCulminationState = CampaignCulminationState.PENDING.name();
        campaignCulminationScheduledTick = Math.max(0L, scheduledTick);
        campaignCulminationRetryTick = campaignCulminationScheduledTick;
        this.setDirty();
    }

    public void postponeCampaignCulmination(long retryTick) {
        if (getCampaignCulminationState() != CampaignCulminationState.PENDING) {
            return;
        }
        campaignCulminationRetryTick = Math.max(campaignCulminationScheduledTick, retryTick);
        this.setDirty();
    }

    public void markCampaignCulminationSatisfied() {
        campaignCulminationState = CampaignCulminationState.SATISFIED.name();
        campaignCulminationRetryTick = Long.MIN_VALUE;
        this.setDirty();
    }

    public void markCampaignCulminationExpired() {
        campaignCulminationState = CampaignCulminationState.EXPIRED.name();
        campaignCulminationRetryTick = Long.MIN_VALUE;
        this.setDirty();
    }

    public List<String> getCampaignRecentFamilies() {
        return List.copyOf(campaignRecentFamilies);
    }

    public void rememberCampaignFamily(String family, int maximumEntries) {
        if (family == null || family.isBlank() || maximumEntries <= 0) {
            return;
        }
        campaignRecentFamilies.add(0, family);
        while (campaignRecentFamilies.size() > maximumEntries) {
            campaignRecentFamilies.remove(campaignRecentFamilies.size() - 1);
        }
        this.setDirty();
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

    public String getWeatherTargetPlayerUuid() {
        return weatherTargetPlayerUuid;
    }

    public void setWeatherTargetPlayerUuid(String weatherTargetPlayerUuid) {
        this.weatherTargetPlayerUuid = weatherTargetPlayerUuid == null ? "" : weatherTargetPlayerUuid;
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

    public void configureLocalizedWeather(
            BlockPos center,
            double directionX,
            double directionZ,
            int radius,
            long seed,
            long startTick,
            String data) {
        localizedWeatherX = center.getX();
        localizedWeatherY = center.getY();
        localizedWeatherZ = center.getZ();
        localizedWeatherDirectionX = directionX;
        localizedWeatherDirectionZ = directionZ;
        localizedWeatherRadius = Math.max(0, radius);
        localizedWeatherSeed = seed;
        localizedWeatherStartTick = startTick;
        localizedWeatherData = data == null ? "" : data;
        this.setDirty();
    }

    public void clearLocalizedWeather() {
        configureLocalizedWeather(BlockPos.ZERO, 0.0D, 0.0D, 0, 0L, Long.MIN_VALUE, "");
    }

    public BlockPos getLocalizedWeatherCenter() {
        return new BlockPos(localizedWeatherX, localizedWeatherY, localizedWeatherZ);
    }

    public double getLocalizedWeatherDirectionX() {
        return localizedWeatherDirectionX;
    }

    public double getLocalizedWeatherDirectionZ() {
        return localizedWeatherDirectionZ;
    }

    public int getLocalizedWeatherRadius() {
        return localizedWeatherRadius;
    }

    public long getLocalizedWeatherSeed() {
        return localizedWeatherSeed;
    }

    public long getLocalizedWeatherStartTick() {
        return localizedWeatherStartTick;
    }

    public String getLocalizedWeatherData() {
        return localizedWeatherData;
    }

    public void rememberPlayerPlacedLight(BlockPos pos) {
        long packed = pos.asLong();
        playerPlacedLights.remove(packed);
        playerPlacedLights.add(packed);
        while (playerPlacedLights.size() > 256) {
            playerPlacedLights.remove(0);
        }
        this.setDirty();
    }

    public void forgetPlayerPlacedLight(BlockPos pos) {
        if (playerPlacedLights.remove(pos.asLong())) {
            this.setDirty();
        }
    }

    public List<BlockPos> getPlayerPlacedLights() {
        return playerPlacedLights.stream().map(BlockPos::of).toList();
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

    public void recordDeathSite(UUID playerId, ResourceKey<Level> dimension, BlockPos position, long tick) {
        String dimensionId = normalizeDimension(dimension);
        if (playerId == null || dimensionId == null || position == null) {
            return;
        }
        deathSites.put(playerId, new DeathSite(dimensionId, position.asLong(), tick, false));
        this.setDirty();
    }

    public DeathSite getDeathSite(UUID playerId) {
        return deathSites.get(playerId);
    }

    public void markMournerUsed(UUID playerId) {
        boolean changed = false;
        if (!mournerOccurred) {
            mournerOccurred = true;
            changed = true;
        }
        DeathSite current = deathSites.get(playerId);
        if (current != null && !current.mournerUsed()) {
            deathSites.put(playerId, new DeathSite(current.dimension(), current.posLong(), current.tick(), true));
            changed = true;
        }
        if (changed) {
            this.setDirty();
        }
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

    public int findWeightedMissingHistoryTome(
            UUID playerId, int maxTomes, double logicalStoryDay, double roll) {
        if (playerId == null || maxTomes <= 0) {
            return -1;
        }
        return UncannyJournalCatalog.selectMissing(
                historyTomeMask.getOrDefault(playerId, 0), maxTomes, logicalStoryDay, roll);
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

    private static long saturatingAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
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

    private static void writeStringList(CompoundTag parent, String key, List<String> values) {
        ListTag list = new ListTag();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            CompoundTag item = new CompoundTag();
            item.putString("value", value);
            list.add(item);
        }
        parent.put(key, list);
    }

    private static void readStringList(
            CompoundTag parent,
            String key,
            List<String> values,
            int maximumEntries) {
        values.clear();
        ListTag list = parent.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && values.size() < maximumEntries; i++) {
            String value = list.getCompound(i).getString("value");
            if (!value.isBlank()) {
                values.add(value);
            }
        }
    }

    private static void writeDeathSites(CompoundTag parent, Map<UUID, DeathSite> sites) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, DeathSite> entry : sites.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putUUID("player", entry.getKey());
            item.putString("dimension", entry.getValue().dimension());
            item.putLong("pos", entry.getValue().posLong());
            item.putLong("tick", entry.getValue().tick());
            item.putBoolean("mournerUsed", entry.getValue().mournerUsed());
            list.add(item);
        }
        parent.put("deathSites", list);
    }

    private static void readDeathSites(CompoundTag parent, Map<UUID, DeathSite> sites) {
        sites.clear();
        ListTag list = parent.getList("deathSites", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            if (!item.hasUUID("player") || item.getString("dimension").isBlank()) {
                continue;
            }
            sites.put(item.getUUID("player"), new DeathSite(
                    item.getString("dimension"), item.getLong("pos"), item.getLong("tick"),
                    item.getBoolean("mournerUsed")));
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

    public record DeathSite(String dimension, long posLong, long tick, boolean mournerUsed) {
        public BlockPos position() {
            return BlockPos.of(posLong);
        }
    }
}

