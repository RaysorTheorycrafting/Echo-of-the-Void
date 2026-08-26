package com.eotv.echoofthevoid.event.paranoia.simulation;

import com.eotv.echoofthevoid.campaign.CampaignDirectorRules;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventCatalog;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventDescriptor;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventSeverity;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaPacingRules;
import com.eotv.echoofthevoid.event.paranoia.TensionPacingRules;
import com.eotv.echoofthevoid.event.special.GrandWardenRules;
import com.eotv.echoofthevoid.event.weather.UncannyWeatherPacingRules;
import com.eotv.echoofthevoid.event.weather.UncannyWeatherPacingRules.Event;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Whole-campaign deterministic release audit.
 *
 * <p>The scheduler, director, weather and Tension/Grand arithmetic come from the same pure rule
 * classes as runtime. Placement/context probes are deliberately treated as available; the report
 * is therefore a numerical upper envelope, not a promise that every selected effect spawns.</p>
 */
public final class CampaignReleaseSimulator {
    private static final long MAJOR_RANDOM_SALT = 0x454F54564D414A4FL;
    private static final long WEATHER_RANDOM_SALT = 0x454F545657454154L;
    private static final int DEFAULT_PHASE_1_MINUTES = 30;
    private static final int DEFAULT_PHASE_2_MINUTES = 45;
    private static final int DEFAULT_PHASE_3_MINUTES = 60;

    private CampaignReleaseSimulator() {
    }

    public static CampaignReport simulate(CampaignScenario scenario) {
        long durationTicks = scenario.campaignDays() * CampaignDirectorRules.TICKS_PER_DAY;
        long phaseTwoStart = minutesToTicks(scenario.phaseOneMinutes());
        long phaseThreeStart = phaseTwoStart + minutesToTicks(scenario.phaseTwoMinutes());
        long phaseFourStart = phaseThreeStart + minutesToTicks(scenario.phaseThreeMinutes());

        List<MajorWindow> majors = generateNaturalMajors(scenario, durationTicks, phaseTwoStart);
        Culmination culmination = resolveCulmination(scenario, durationTicks, majors);
        if (culmination.forced()) {
            majors = rebuildAfterForcedCulmination(scenario, durationTicks, majors, culmination.tick());
        }
        majors.sort(Comparator.comparingLong(MajorWindow::startTick));
        PauseSchedule pauses = PauseSchedule.from(majors);

        double hours = durationTicks
                / (double) (60L * 60L * ParanoiaPacingRules.TICKS_PER_SECOND);
        ParanoiaSchedulerSimulator.Scenario eventScenario =
                ParanoiaSchedulerSimulator.Scenario.reference(
                        4,
                        scenario.profile(),
                        scenario.danger(),
                        hours,
                        scenario.seed(),
                        scenario.campaignDays(),
                        true);
        ParanoiaSchedulerSimulator.SimulationReport events =
                ParanoiaSchedulerSimulator.simulateCampaignEvents(
                        eventScenario,
                        durationTicks,
                        tick -> phaseAt(tick, phaseTwoStart, phaseThreeStart, phaseFourStart),
                        pauses::resumeTickAt);

        WeatherReport weather = simulateWeather(
                scenario,
                durationTicks,
                phaseTwoStart,
                phaseThreeStart,
                phaseFourStart,
                pauses);

        long tensionCount = majors.stream().filter(window -> window.kind() == MajorKind.TENSION).count();
        long grandWardenCount = majors.stream().filter(window -> window.kind() == MajorKind.GRAND_WARDEN).count();
        long unjustifiedMajorBursts = countUnjustifiedMajorBursts(majors);
        long unjustifiedStrongEventBursts = countStrongEventBursts(events.eventSamples());

        return new CampaignReport(
                scenario,
                durationTicks,
                phaseTwoStart,
                phaseThreeStart,
                phaseFourStart,
                events,
                weather,
                tensionCount,
                grandWardenCount,
                culmination.tick(),
                culmination.forced(),
                culmination.expired(),
                culmination.stateTransitions(),
                Math.max(0L, culmination.stateTransitions() - 1L),
                unjustifiedMajorBursts,
                unjustifiedStrongEventBursts,
                permanentlyIneligibleEventIds(scenario.danger()),
                Collections.unmodifiableList(new ArrayList<>(majors)));
    }

    private static List<MajorWindow> generateNaturalMajors(
            CampaignScenario scenario, long durationTicks, long phaseTwoStart) {
        Random random = new Random(scenario.seed() ^ MAJOR_RANDOM_SALT);
        List<MajorWindow> result = new ArrayList<>();
        long firstStart = phaseTwoStart + secondsToTicks(sampleInclusive(
                random, TensionPacingRules.BREAK_MIN_SECONDS, TensionPacingRules.BREAK_MAX_SECONDS));
        generateMajorCycles(firstStart, durationTicks, random, result, 1);
        return result;
    }

    private static void generateMajorCycles(
            long firstStart,
            long durationTicks,
            Random random,
            List<MajorWindow> target,
            int firstSequence) {
        long nextTension = firstStart;
        long lastGrandStart = Long.MIN_VALUE;
        int sequence = firstSequence;
        while (nextTension < durationTicks) {
            int durationSeconds = sampleInclusive(
                    random, TensionPacingRules.TENSION_MIN_SECONDS, TensionPacingRules.TENSION_MAX_SECONDS);
            long tensionEnd = Math.min(durationTicks, nextTension + secondsToTicks(durationSeconds));
            target.add(new MajorWindow(MajorKind.TENSION, nextTension, tensionEnd, sequence, false));

            long boostEnd = tensionEnd + secondsToTicks(sampleInclusive(
                    random,
                    TensionPacingRules.GRAND_BOOST_MIN_SECONDS,
                    TensionPacingRules.GRAND_BOOST_MAX_SECONDS));
            long nextRoll = tensionEnd + secondsToTicks(sampleInclusive(
                    random,
                    TensionPacingRules.GRAND_ROLL_MIN_SECONDS,
                    TensionPacingRules.GRAND_ROLL_MAX_SECONDS));
            while (nextRoll <= boostEnd && nextRoll < durationTicks) {
                boolean cooldownReady = lastGrandStart == Long.MIN_VALUE
                        || nextRoll - lastGrandStart >= secondsToTicks(TensionPacingRules.GRAND_COOLDOWN_SECONDS);
                double roll = random.nextDouble();
                if (cooldownReady && roll <= TensionPacingRules.GRAND_POST_TENSION_CHANCE) {
                    int delaySize = GrandWardenRules.PRESPAWN_DELAY_MAX_SECONDS
                            - GrandWardenRules.PRESPAWN_DELAY_MIN_SECONDS + 1;
                    long grandStart = nextRoll + secondsToTicks(
                            GrandWardenRules.preSpawnDelaySeconds(random.nextInt(delaySize)));
                    if (grandStart < durationTicks) {
                        long grandEnd = Math.min(
                                durationTicks,
                                grandStart + secondsToTicks(GrandWardenRules.MAX_RUNTIME_SECONDS));
                        target.add(new MajorWindow(
                                MajorKind.GRAND_WARDEN, grandStart, grandEnd, sequence, true));
                        lastGrandStart = grandStart;
                    }
                    break;
                }
                nextRoll += secondsToTicks(sampleInclusive(
                        random,
                        TensionPacingRules.GRAND_ROLL_MIN_SECONDS,
                        TensionPacingRules.GRAND_ROLL_MAX_SECONDS));
            }

            int breakSeconds = sampleInclusive(
                    random, TensionPacingRules.BREAK_MIN_SECONDS, TensionPacingRules.BREAK_MAX_SECONDS);
            nextTension = tensionEnd + secondsToTicks(breakSeconds);
            sequence++;
        }
    }

    private static Culmination resolveCulmination(
            CampaignScenario scenario, long durationTicks, List<MajorWindow> majors) {
        long naturalThreshold = Math.round(
                durationTicks * CampaignDirectorRules.CULMINATION_NATURAL_SATISFACTION_PROGRESS);
        long scheduled = CampaignDirectorRules.culminationScheduledTick(
                scenario.seed() ^ 0x454F545643414D50L, scenario.campaignDays());
        long cutoff = Math.round(
                durationTicks * CampaignDirectorRules.CULMINATION_FORCED_ATTEMPT_CUTOFF_PROGRESS);
        long natural = majors.stream()
                .mapToLong(MajorWindow::startTick)
                .filter(tick -> tick >= naturalThreshold && tick < durationTicks)
                .min()
                .orElse(Long.MAX_VALUE);
        if (natural <= scheduled) {
            return new Culmination(natural, false, false, 1L);
        }

        PauseSchedule pauses = PauseSchedule.from(majors);
        long attempt = scheduled;
        while (attempt < cutoff) {
            long resumed = pauses.resumeTickAt(attempt);
            if (resumed > attempt) {
                attempt = Math.max(
                        resumed,
                        attempt + CampaignDirectorRules.CULMINATION_RETRY_TICKS);
                continue;
            }
            long previousMajor = Long.MIN_VALUE;
            for (MajorWindow major : majors) {
                if (major.startTick() <= attempt) {
                    previousMajor = Math.max(previousMajor, major.startTick());
                }
            }
            boolean strongGap = previousMajor == Long.MIN_VALUE
                    || attempt - previousMajor >= CampaignDirectorRules.CULMINATION_STRONG_EVENT_GAP_TICKS;
            if (strongGap) {
                if (attempt < natural) {
                    return new Culmination(attempt, true, false, 1L);
                }
                return new Culmination(natural, false, false, 1L);
            }
            attempt += CampaignDirectorRules.CULMINATION_RETRY_TICKS;
        }
        if (natural < durationTicks) {
            return new Culmination(natural, false, false, 1L);
        }
        return new Culmination(Long.MIN_VALUE, false, true, 0L);
    }

    private static List<MajorWindow> rebuildAfterForcedCulmination(
            CampaignScenario scenario,
            long durationTicks,
            List<MajorWindow> original,
            long forcedTick) {
        List<MajorWindow> rebuilt = new ArrayList<>();
        for (MajorWindow window : original) {
            if (window.startTick() < forcedTick) {
                rebuilt.add(window);
            }
        }
        int nextSequence = rebuilt.stream().mapToInt(MajorWindow::sequence).max().orElse(0) + 1;
        Random random = new Random(scenario.seed() ^ MAJOR_RANDOM_SALT ^ forcedTick);
        long forcedEnd = Math.min(
                durationTicks,
                forcedTick + secondsToTicks(sampleInclusive(
                        random,
                        TensionPacingRules.TENSION_MIN_SECONDS,
                        TensionPacingRules.TENSION_MAX_SECONDS)));
        rebuilt.add(new MajorWindow(MajorKind.TENSION, forcedTick, forcedEnd, nextSequence, false));

        long suffixStart = forcedEnd + secondsToTicks(sampleInclusive(
                random, TensionPacingRules.BREAK_MIN_SECONDS, TensionPacingRules.BREAK_MAX_SECONDS));
        generateMajorCycles(suffixStart, durationTicks, random, rebuilt, nextSequence + 1);
        return rebuilt;
    }

    private static WeatherReport simulateWeather(
            CampaignScenario scenario,
            long durationTicks,
            long phaseTwoStart,
            long phaseThreeStart,
            long phaseFourStart,
            PauseSchedule pauses) {
        Random random = new Random(scenario.seed() ^ WEATHER_RANDOM_SALT);
        Map<String, Long> counts = new LinkedHashMap<>();
        Set<String> eligible = new LinkedHashSet<>();
        long now = 0L;
        long nextCheck = 0L;
        String lastEventId = "";
        int lastHeavyDuration = 0;
        long interruptedByMajor = 0L;

        while (nextCheck < durationTicks) {
            now = Math.max(now, nextCheck);
            long resume = pauses.resumeTickAt(now);
            if (resume > now) {
                now = resume;
                nextCheck = resume;
                continue;
            }
            int phase = phaseAt(now, phaseTwoStart, phaseThreeStart, phaseFourStart);
            for (Event event : Event.values()) {
                if (phase >= event.minPhase
                        && UncannyWeatherPacingRules.effectiveWeight(
                                event, phase, scenario.profile(), scenario.danger(), true) > 0) {
                    eligible.add(event.id);
                }
            }

            nextCheck = now + sampleWeatherNextCheck(random, phase, scenario.profile());
            if (random.nextDouble() > UncannyWeatherPacingRules.triggerChance(phase, scenario.profile())) {
                continue;
            }
            Event selected = selectWeather(
                    random, phase, scenario.profile(), scenario.danger(), lastEventId);
            if (selected == null) {
                continue;
            }
            int duration = selected.minDurationTicks
                    + random.nextInt(selected.maxDurationTicks - selected.minDurationTicks + 1);
            if (selected.heavyVisual()) {
                UncannyWeatherPacingRules.IntRange visualRange =
                        UncannyWeatherPacingRules.visualDurationRange(lastHeavyDuration);
                duration = visualRange.minInclusive() + random.nextInt(visualRange.size());
                lastHeavyDuration = duration;
            }
            long plannedEnd = Math.min(durationTicks, now + duration);
            long pauseStart = pauses.firstStartBetween(now, plannedEnd);
            if (pauseStart != Long.MAX_VALUE) {
                plannedEnd = pauseStart;
                interruptedByMajor++;
            }
            counts.merge(selected.id, 1L, Long::sum);
            lastEventId = selected.id;
            long cooldown = sampleWeatherCooldown(
                    random, phase, scenario.profile(), selected.severityMultiplier);
            now = Math.max(now + duration + cooldown, plannedEnd);
            nextCheck = Math.max(nextCheck, now);
        }

        Set<String> ineligible = new LinkedHashSet<>();
        for (Event event : Event.values()) {
            if (!eligible.contains(event.id)) {
                ineligible.add(event.id);
            }
        }
        return new WeatherReport(
                counts.values().stream().mapToLong(Long::longValue).sum(),
                Collections.unmodifiableMap(counts),
                Collections.unmodifiableSet(eligible),
                Collections.unmodifiableSet(ineligible),
                interruptedByMajor);
    }

    private static long sampleWeatherNextCheck(Random random, int phase, int profile) {
        UncannyWeatherPacingRules.IntRange range =
                UncannyWeatherPacingRules.nextCheckSecondsRange(phase, profile);
        int baseSeconds = range.minInclusive() + random.nextInt(range.size());
        boolean longDelay = random.nextFloat() < 0.15F;
        int longSeconds = longDelay ? 8 + random.nextInt(28) : 0;
        boolean shortDelay = random.nextFloat() < 0.20F;
        int shortSeconds = shortDelay ? 1 + random.nextInt(6) : 0;
        return UncannyWeatherPacingRules.nextCheckDelayTicks(
                baseSeconds, longDelay, longSeconds, shortDelay, shortSeconds);
    }

    private static long sampleWeatherCooldown(
            Random random, int phase, int profile, float severityScale) {
        UncannyWeatherPacingRules.IntRange range =
                UncannyWeatherPacingRules.cooldownSecondsRange(phase, profile, severityScale);
        int baseSeconds = range.minInclusive() + random.nextInt(range.size());
        boolean longDelay = random.nextFloat() < 0.20F;
        int longSeconds = longDelay ? 12 + random.nextInt(34) : 0;
        return UncannyWeatherPacingRules.cooldownTicks(baseSeconds, longDelay, longSeconds);
    }

    private static Event selectWeather(
            Random random, int phase, int profile, int danger, String lastEventId) {
        List<WeightedWeather> choices = weatherChoices(phase, profile, danger, lastEventId, true);
        if (choices.isEmpty() && lastEventId != null && !lastEventId.isBlank()) {
            choices = weatherChoices(phase, profile, danger, "", false);
        }
        if (choices.isEmpty()) {
            return null;
        }
        int total = choices.stream().mapToInt(WeightedWeather::weight).sum();
        int roll = random.nextInt(total);
        int cursor = 0;
        for (WeightedWeather choice : choices) {
            cursor += choice.weight();
            if (roll < cursor) {
                return choice.event();
            }
        }
        return choices.getLast().event();
    }

    private static List<WeightedWeather> weatherChoices(
            int phase, int profile, int danger, String lastEventId, boolean presentationLimits) {
        List<WeightedWeather> result = new ArrayList<>();
        for (Event event : Event.values()) {
            if (phase < event.minPhase || event.id.equals(lastEventId)) {
                continue;
            }
            int weight = UncannyWeatherPacingRules.effectiveWeight(
                    event, phase, profile, danger, presentationLimits);
            if (weight > 0) {
                result.add(new WeightedWeather(event, weight));
            }
        }
        return result;
    }

    private static Set<String> permanentlyIneligibleEventIds(int danger) {
        Set<String> result = new LinkedHashSet<>();
        for (ParanoiaEventDescriptor descriptor : allEvents()) {
            boolean eligible = false;
            for (int phase = 1; phase <= 4; phase++) {
                if (descriptor.isAvailable(phase, danger)) {
                    eligible = true;
                    break;
                }
            }
            if (!eligible) {
                result.add(descriptor.id());
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static List<ParanoiaEventDescriptor> allEvents() {
        List<ParanoiaEventDescriptor> result = new ArrayList<>();
        result.addAll(ParanoiaEventCatalog.primaryEvents());
        result.addAll(ParanoiaEventCatalog.ambientEvents());
        result.addAll(ParanoiaEventCatalog.specialEvents());
        return result;
    }

    private static long countUnjustifiedMajorBursts(List<MajorWindow> majors) {
        List<MajorWindow> sorted = majors.stream()
                .sorted(Comparator.comparingLong(MajorWindow::startTick))
                .toList();
        long bursts = 0L;
        for (int i = 1; i < sorted.size(); i++) {
            MajorWindow previous = sorted.get(i - 1);
            MajorWindow current = sorted.get(i);
            if (current.startTick() - previous.startTick()
                    >= CampaignDirectorRules.CULMINATION_STRONG_EVENT_GAP_TICKS) {
                continue;
            }
            boolean linkedGrand = current.kind() == MajorKind.GRAND_WARDEN
                    && current.linkedToTension()
                    && current.sequence() == previous.sequence();
            if (!linkedGrand) {
                bursts++;
            }
        }
        return bursts;
    }

    private static long countStrongEventBursts(
            List<ParanoiaSchedulerSimulator.EventSample> samples) {
        long previousStrong = Long.MIN_VALUE;
        long bursts = 0L;
        for (ParanoiaSchedulerSimulator.EventSample sample : samples) {
            if (sample.severity() != ParanoiaEventSeverity.HIGH
                    && sample.severity() != ParanoiaEventSeverity.EXTREME) {
                continue;
            }
            if (previousStrong != Long.MIN_VALUE
                    && sample.tick() - previousStrong <= ParanoiaSchedulerSimulator.BURST_THRESHOLD_TICKS) {
                bursts++;
            }
            previousStrong = sample.tick();
        }
        return bursts;
    }

    private static int phaseAt(
            long tick, long phaseTwoStart, long phaseThreeStart, long phaseFourStart) {
        if (tick < phaseTwoStart) return 1;
        if (tick < phaseThreeStart) return 2;
        if (tick < phaseFourStart) return 3;
        return 4;
    }

    private static int sampleInclusive(Random random, int minimum, int maximum) {
        return minimum + random.nextInt(maximum - minimum + 1);
    }

    private static long minutesToTicks(int minutes) {
        return minutes * 60L * ParanoiaPacingRules.TICKS_PER_SECOND;
    }

    private static long secondsToTicks(int seconds) {
        return TensionPacingRules.secondsToTicks(seconds);
    }

    public record CampaignScenario(
            int profile,
            int danger,
            int campaignDays,
            long seed,
            int phaseOneMinutes,
            int phaseTwoMinutes,
            int phaseThreeMinutes) {
        public CampaignScenario {
            if (profile < 1 || profile > 5) {
                throw new IllegalArgumentException("profile must be in [1, 5]");
            }
            if (danger < 0 || danger > 5) {
                throw new IllegalArgumentException("danger must be in [0, 5]");
            }
            if (campaignDays != CampaignDirectorRules.STANDARD_LENGTH_DAYS
                    && campaignDays != CampaignDirectorRules.EXTRA_LONG_LENGTH_DAYS) {
                throw new IllegalArgumentException("campaignDays must be 50 or 100");
            }
            if (phaseOneMinutes <= 0 || phaseTwoMinutes <= 0 || phaseThreeMinutes <= 0) {
                throw new IllegalArgumentException("phase durations must be positive");
            }
        }

        public static CampaignScenario standard(int profile, int danger, long seed) {
            return new CampaignScenario(
                    profile,
                    danger,
                    CampaignDirectorRules.STANDARD_LENGTH_DAYS,
                    seed,
                    DEFAULT_PHASE_1_MINUTES,
                    DEFAULT_PHASE_2_MINUTES,
                    DEFAULT_PHASE_3_MINUTES);
        }

        public static CampaignScenario extraLong(int profile, int danger, long seed) {
            return new CampaignScenario(
                    profile,
                    danger,
                    CampaignDirectorRules.EXTRA_LONG_LENGTH_DAYS,
                    seed,
                    DEFAULT_PHASE_1_MINUTES,
                    DEFAULT_PHASE_2_MINUTES,
                    DEFAULT_PHASE_3_MINUTES);
        }
    }

    public record CampaignReport(
            CampaignScenario scenario,
            long durationTicks,
            long phaseTwoStartTick,
            long phaseThreeStartTick,
            long phaseFourStartTick,
            ParanoiaSchedulerSimulator.SimulationReport events,
            WeatherReport weather,
            long tensionBuilderCount,
            long grandWardenCount,
            long culminationTick,
            boolean forcedCulmination,
            boolean culminationExpired,
            long culminationStateTransitions,
            long doubleCulminationCount,
            long unjustifiedMajorBurstCount,
            long strongEventBurstCount,
            Set<String> permanentlyIneligibleEventIds,
            List<MajorWindow> majorWindows) {
    }

    public record WeatherReport(
            long totalEvents,
            Map<String, Long> countsByEvent,
            Set<String> eligibleEventIds,
            Set<String> ineligibleEventIds,
            long interruptedByMajorCount) {
    }

    public record MajorWindow(
            MajorKind kind,
            long startTick,
            long endTick,
            int sequence,
            boolean linkedToTension) {
    }

    public enum MajorKind {
        TENSION,
        GRAND_WARDEN
    }

    private record Culmination(long tick, boolean forced, boolean expired, long stateTransitions) {
    }

    private record WeightedWeather(Event event, int weight) {
    }

    private record PauseWindow(long startTick, long endTick) {
    }

    private static final class PauseSchedule {
        private final List<PauseWindow> windows;

        private PauseSchedule(List<PauseWindow> windows) {
            this.windows = windows;
        }

        private static PauseSchedule from(List<MajorWindow> majors) {
            List<PauseWindow> sorted = majors.stream()
                    .filter(window -> window.endTick() > window.startTick())
                    .map(window -> new PauseWindow(window.startTick(), window.endTick()))
                    .sorted(Comparator.comparingLong(PauseWindow::startTick))
                    .toList();
            List<PauseWindow> merged = new ArrayList<>();
            for (PauseWindow window : sorted) {
                if (merged.isEmpty()) {
                    merged.add(window);
                    continue;
                }
                PauseWindow previous = merged.getLast();
                if (window.startTick() <= previous.endTick()) {
                    merged.set(
                            merged.size() - 1,
                            new PauseWindow(previous.startTick(), Math.max(previous.endTick(), window.endTick())));
                } else {
                    merged.add(window);
                }
            }
            return new PauseSchedule(List.copyOf(merged));
        }

        private long resumeTickAt(long tick) {
            for (PauseWindow window : windows) {
                if (tick < window.startTick()) {
                    return tick;
                }
                if (tick >= window.startTick() && tick < window.endTick()) {
                    return window.endTick();
                }
            }
            return tick;
        }

        private long firstStartBetween(long startExclusive, long endExclusive) {
            for (PauseWindow window : windows) {
                if (window.startTick() > startExclusive && window.startTick() < endExclusive) {
                    return window.startTick();
                }
            }
            return Long.MAX_VALUE;
        }
    }
}
