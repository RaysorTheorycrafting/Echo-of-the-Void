package com.eotv.echoofthevoid.campaign;

import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventCatalog;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventDescriptor;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventLane;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventSeverity;
import com.eotv.echoofthevoid.phase.UncannyPhase;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.MinecraftServer;

/** Persistent world-level pacing layered over, and independent from, the four phases. */
public final class UncannyCampaignDirector {
    private static final long TRANSITION_SALT = 0x43414D504149474EL;
    private static final long DURATION_SALT = 0x4449524543544F52L;

    private UncannyCampaignDirector() {
    }

    public static void tick(MinecraftServer server, UncannyWorldState state, boolean hasActivePlayers) {
        long dayTime = server.overworld().getDayTime();
        if (!state.isCampaignDirectorInitialized()) {
            initialize(server, state, dayTime);
        }
        ensureCulminationScheduled(state);
        if (!hasActivePlayers || !state.isSystemEnabled()) {
            state.observeCampaignDayTime(dayTime);
            return;
        }

        long delta = CampaignDirectorRules.elapsedDelta(state.getCampaignLastObservedDayTime(), dayTime);
        state.advanceCampaignDirector(delta, dayTime);
        advanceExpiredBeats(state);
        ensureCulminationScheduled(state);
        if (culminationAction(state) == CampaignCulminationAction.EXPIRE) {
            state.markCampaignCulminationExpired();
        }
    }

    public static int campaignLengthDays() {
        return UncannyConfig.CAMPAIGN_EXTRA_LONG_100_DAYS.get()
                ? CampaignDirectorRules.EXTRA_LONG_LENGTH_DAYS
                : CampaignDirectorRules.STANDARD_LENGTH_DAYS;
    }

    public static double adjustedTriggerChance(
            UncannyWorldState state, ParanoiaEventLane lane, double baseChance) {
        CampaignAct act = CampaignDirectorRules.actAt(state.getCampaignElapsedTicks(), campaignLengthDays());
        CampaignBeat beat = CampaignBeat.fromSavedName(state.getCampaignBeat());
        return CampaignDirectorRules.triggerChance(baseChance, lane, act, beat);
    }

    public static int adjustedWeight(
            UncannyWorldState state, String eventId, ParanoiaEventLane lane, int baseWeight) {
        ParanoiaEventDescriptor descriptor = ParanoiaEventCatalog.byId().get(eventId);
        ParanoiaEventSeverity severity = descriptor == null
                ? ParanoiaEventSeverity.MEDIUM : descriptor.severity();
        CampaignAct act = CampaignDirectorRules.actAt(state.getCampaignElapsedTicks(), campaignLengthDays());
        CampaignBeat beat = CampaignBeat.fromSavedName(state.getCampaignBeat());
        return CampaignDirectorRules.adjustedWeight(
                baseWeight,
                lane,
                severity,
                CampaignEventFamily.forEvent(eventId),
                state.getCampaignRecentFamilies(),
                state.getCampaignElapsedTicks(),
                state.getCampaignLastStrongEventTick(),
                act,
                beat);
    }

    public static void recordEvent(UncannyWorldState state, String eventId) {
        state.rememberCampaignFamily(
                CampaignEventFamily.forEvent(eventId).name(), CampaignDirectorRules.RECENT_FAMILY_LIMIT);
        ParanoiaEventDescriptor descriptor = ParanoiaEventCatalog.byId().get(eventId);
        if (descriptor != null && (descriptor.severity() == ParanoiaEventSeverity.HIGH
                || descriptor.severity() == ParanoiaEventSeverity.EXTREME)) {
            state.setCampaignLastStrongEventTick(state.getCampaignElapsedTicks());
        }
    }

    public static void recordNaturalMajorEventStarted(UncannyWorldState state) {
        state.setCampaignLastStrongEventTick(state.getCampaignElapsedTicks());
        if (state.getCampaignCulminationState() == CampaignCulminationState.PENDING
                && CampaignDirectorRules.naturalMajorCanSatisfy(
                        state.getCampaignElapsedTicks(), campaignLengthDays())) {
            state.markCampaignCulminationSatisfied();
        }
    }

    public static CampaignCulminationAction culminationAction(UncannyWorldState state) {
        ensureCulminationScheduled(state);
        return CampaignDirectorRules.culminationAction(
                state.getCampaignElapsedTicks(),
                campaignLengthDays(),
                state.getCampaignCulminationState(),
                state.getCampaignCulminationScheduledTick(),
                state.getCampaignCulminationRetryTick());
    }

    public static boolean hasRequiredStrongEventGap(UncannyWorldState state) {
        return CampaignDirectorRules.hasStrongEventGap(
                state.getCampaignElapsedTicks(), state.getCampaignLastStrongEventTick());
    }

    public static void postponeCulmination(UncannyWorldState state) {
        state.postponeCampaignCulmination(
                state.getCampaignElapsedTicks() + CampaignDirectorRules.CULMINATION_RETRY_TICKS);
    }

    public static Snapshot snapshot(UncannyWorldState state) {
        int lengthDays = campaignLengthDays();
        return new Snapshot(
                CampaignDirectorRules.campaignDay(state.getCampaignElapsedTicks()),
                lengthDays,
                CampaignDirectorRules.actAt(state.getCampaignElapsedTicks(), lengthDays),
                CampaignBeat.fromSavedName(state.getCampaignBeat()),
                state.getCampaignBeatRemainingTicks(),
                state.getCampaignRecentFamilies(),
                state.getCampaignCulminationState(),
                state.getCampaignCulminationScheduledTick(),
                state.getCampaignCulminationRetryTick());
    }

    public static String debugSummary(UncannyWorldState state) {
        Snapshot snapshot = snapshot(state);
        return "day=" + String.format(Locale.ROOT, "%.2f", snapshot.day()) + "/" + snapshot.lengthDays()
                + ",act=" + snapshot.act().name().toLowerCase(Locale.ROOT)
                + ",beat=" + snapshot.beat().name().toLowerCase(Locale.ROOT)
                + ",beatRemaining=" + snapshot.beatRemainingTicks() + "t"
                + ",recentFamilies=" + snapshot.recentFamilies()
                + ",culmination=" + snapshot.culminationState().name().toLowerCase(Locale.ROOT)
                + ",culminationAt=" + snapshot.culminationScheduledTick() + "t"
                + ",culminationRetry=" + snapshot.culminationRetryTick() + "t";
    }

    private static void initialize(MinecraftServer server, UncannyWorldState state, long dayTime) {
        long elapsed = inferredElapsedTicks(state.getPhase(), state.getProgressToNextPhase(), dayTime);
        long seed = server.overworld().getSeed() ^ 0x454F545643414D50L;
        CampaignBeat initialBeat = CampaignBeat.UNEASE;
        long duration = CampaignDirectorRules.beatDurationTicks(
                initialBeat, CampaignDirectorRules.deterministicUnit(seed, 0, DURATION_SALT));
        state.initializeCampaignDirector(elapsed, dayTime, seed, initialBeat.name(), duration);
    }

    static long inferredElapsedTicks(UncannyPhase phase, double phaseProgress, long dayTime) {
        long p1 = UncannyConfig.PHASE_P1_TO_P2_MINUTES.get() * 60L * 20L;
        long p2 = UncannyConfig.PHASE_P2_TO_P3_MINUTES.get() * 60L * 20L;
        long p3 = UncannyConfig.PHASE_P3_TO_P4_MINUTES.get() * 60L * 20L;
        double progress = Math.max(0.0D, Math.min(1.0D, phaseProgress));
        long phaseElapsed = switch (phase) {
            case PHASE_1 -> Math.round(p1 * progress);
            case PHASE_2 -> p1 + Math.round(p2 * progress);
            case PHASE_3 -> p1 + p2 + Math.round(p3 * progress);
            case PHASE_4 -> p1 + p2 + p3;
        };
        long campaignCap = campaignLengthDays() * CampaignDirectorRules.TICKS_PER_DAY;
        return Math.min(campaignCap, Math.max(phaseElapsed, Math.max(0L, dayTime)));
    }

    private static void advanceExpiredBeats(UncannyWorldState state) {
        int guard = 0;
        while (state.getCampaignBeatRemainingTicks() <= 0L && guard++ < 32) {
            int sequence = state.getCampaignBeatSequence() + 1;
            CampaignBeat next = CampaignDirectorRules.nextBeat(
                    CampaignBeat.fromSavedName(state.getCampaignBeat()),
                    CampaignDirectorRules.deterministicUnit(
                            state.getCampaignDirectorSeed(), sequence, TRANSITION_SALT));
            long duration = CampaignDirectorRules.beatDurationTicks(
                    next,
                    CampaignDirectorRules.deterministicUnit(
                            state.getCampaignDirectorSeed(), sequence, DURATION_SALT));
            state.startCampaignBeat(next.name(), duration, sequence);
        }
    }

    private static void ensureCulminationScheduled(UncannyWorldState state) {
        if (!state.isCampaignDirectorInitialized()
                || state.getCampaignCulminationState() != CampaignCulminationState.UNINITIALIZED) {
            return;
        }
        int lengthDays = campaignLengthDays();
        long elapsed = state.getCampaignElapsedTicks();
        if (CampaignDirectorRules.campaignProgress(elapsed, lengthDays) >= 1.0D) {
            state.markCampaignCulminationExpired();
            return;
        }
        long scheduled = CampaignDirectorRules.culminationScheduledTick(
                state.getCampaignDirectorSeed(), lengthDays);
        if (elapsed > scheduled) {
            scheduled = elapsed + CampaignDirectorRules.CULMINATION_RETRY_TICKS;
        }
        state.scheduleCampaignCulmination(scheduled);
    }

    public record Snapshot(
            double day,
            int lengthDays,
            CampaignAct act,
            CampaignBeat beat,
            long beatRemainingTicks,
            List<String> recentFamilies,
            CampaignCulminationState culminationState,
            long culminationScheduledTick,
            long culminationRetryTick) {
    }
}
