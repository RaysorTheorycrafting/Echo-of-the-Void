package com.eotv.echoofthevoid.event.paranoia.simulation;

import com.eotv.echoofthevoid.campaign.CampaignAct;
import com.eotv.echoofthevoid.campaign.CampaignBeat;
import com.eotv.echoofthevoid.campaign.CampaignDirectorRules;
import com.eotv.echoofthevoid.campaign.CampaignEventFamily;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventCatalog;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventDescriptor;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventIds;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventLane;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventSeverity;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaPacingRules;
import com.eotv.echoofthevoid.event.paranoia.WeightedChoice;
import com.eotv.echoofthevoid.event.paranoia.WeightedSelector;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;

/**
 * Deterministic, headless scheduler model backed by the same catalog and arithmetic as runtime.
 *
 * <p>This first foundation assumes every non-numeric world/context probe succeeds. It therefore
 * measures pacing and weighted selection, not placement failures or gameplay effect execution.</p>
 */
public final class ParanoiaSchedulerSimulator {
    public static final int DEFAULT_GLOBAL_COOLDOWN_SECONDS = 480;
    public static final int INITIAL_EVENT_JOIN_GRACE_TICKS = 18 * ParanoiaPacingRules.TICKS_PER_SECOND;
    public static final int INITIAL_SPECIAL_JOIN_GRACE_TICKS = 24 * ParanoiaPacingRules.TICKS_PER_SECOND;
    public static final int BURST_THRESHOLD_TICKS = 10 * ParanoiaPacingRules.TICKS_PER_SECOND;

    private ParanoiaSchedulerSimulator() {
    }

    public static SimulationReport simulate(Scenario scenario) {
        long durationTicks = Math.max(1L, Math.round(
                scenario.hours() * 60.0D * 60.0D * ParanoiaPacingRules.TICKS_PER_SECOND));
        return simulateInternal(
                scenario,
                durationTicks,
                ignored -> scenario.phase(),
                LongUnaryOperator.identity());
    }

    static SimulationReport simulateCampaignEvents(
            Scenario scenario,
            long durationTicks,
            LongToIntFunction phaseAt,
            LongUnaryOperator resumeTickAt) {
        return simulateInternal(scenario, durationTicks, phaseAt, resumeTickAt);
    }

    private static SimulationReport simulateInternal(
            Scenario scenario,
            long durationTicks,
            LongToIntFunction phaseAt,
            LongUnaryOperator resumeTickAt) {
        Random random = new Random(scenario.seed());
        int initialPhase = phaseAt.applyAsInt(0L);

        long nextAutoCheck = INITIAL_EVENT_JOIN_GRACE_TICKS + sample(
                random, ParanoiaPacingRules.autoCheckIntervalTicksRange(initialPhase, scenario.profile()));
        ParanoiaPacingRules.IntRange specialInitialRange =
                ParanoiaPacingRules.specialCheckIntervalSecondsRange(initialPhase, scenario.profile());
        long nextSpecialCheck = INITIAL_SPECIAL_JOIN_GRACE_TICKS
                + sample(random, specialInitialRange) * (long) ParanoiaPacingRules.TICKS_PER_SECOND;

        long lastGlobalEvent = Long.MIN_VALUE;
        long lastAmbientEvent = Long.MIN_VALUE;
        long lastSpecialEvent = Long.MIN_VALUE;
        Map<String, Long> eventCooldownUntil = new LinkedHashMap<>();
        Map<String, Long> ambientCooldownUntil = new LinkedHashMap<>();
        Map<String, Long> specialCooldownUntil = new LinkedHashMap<>();
        List<EventSample> samples = new ArrayList<>();
        DirectorSimulationState director = scenario.includeCampaignDirector()
                ? new DirectorSimulationState(scenario.seed(), scenario.campaignLengthDays())
                : null;

        while (true) {
            long now = Math.min(nextAutoCheck, scenario.includeSpecials() ? nextSpecialCheck : Long.MAX_VALUE);
            if (now > durationTicks) {
                break;
            }
            long resumeTick = resumeTickAt.applyAsLong(now);
            if (resumeTick > now) {
                if (nextAutoCheck <= now) {
                    nextAutoCheck = resumeTick;
                }
                if (nextSpecialCheck <= now) {
                    nextSpecialCheck = resumeTick;
                }
                if (director != null) {
                    director.advanceTo(resumeTick);
                }
                continue;
            }
            if (director != null) {
                director.advanceTo(now);
            }
            int phase = phaseAt.applyAsInt(now);
            boolean specialTriggeredThisTick = false;

            // Runtime checks the special lane before the shared primary/ambient check on player tick.
            if (scenario.includeSpecials() && nextSpecialCheck == now) {
                ParanoiaPacingRules.IntRange nextRange =
                        ParanoiaPacingRules.specialCheckIntervalSecondsRange(phase, scenario.profile());
                nextSpecialCheck = now + sample(random, nextRange) * (long) ParanoiaPacingRules.TICKS_PER_SECOND;
                if (phase >= 2) {
                    long globalCooldown = specialGlobalCooldownTicks(scenario, phase);
                    if (!cooldownActive(lastSpecialEvent, now, globalCooldown)
                            && (scenario.includeRetired111Events()
                                    || !cooldownActive(
                                            lastGlobalEvent,
                                            now,
                                            ParanoiaPacingRules.CROSS_LANE_BURST_GUARD_TICKS))
                            && random.nextDouble() <= adjustedChance(
                                    director,
                                    ParanoiaEventLane.SPECIAL,
                                    ParanoiaPacingRules.specialTriggerChance(
                                            phase, scenario.profile(), scenario.danger()))) {
                        List<WeightedChoice<String>> choices = choices(
                                specialEvents(scenario),
                                ParanoiaEventLane.SPECIAL,
                                scenario,
                                specialCooldownUntil,
                                now,
                                director,
                                phase);
                        String selected = select(random, choices);
                        if (selected != null) {
                            samples.add(sample(now, selected, ParanoiaEventLane.SPECIAL));
                            if (director != null) director.record(selected);
                            specialTriggeredThisTick = true;
                            lastSpecialEvent = now;
                            long perKey = specialPerKeyCooldownTicks(selected, scenario, phase);
                            specialCooldownUntil.put(selected, now + Math.max(globalCooldown / 2L, perKey));
                        }
                    }
                }
            }

            if (nextAutoCheck == now) {
                nextAutoCheck = now + sample(
                        random,
                        ParanoiaPacingRules.autoCheckIntervalTicksRange(phase, scenario.profile()));
                if (!scenario.includeRetired111Events()
                        && (specialTriggeredThisTick
                                || cooldownActive(
                                        lastSpecialEvent,
                                        now,
                                        ParanoiaPacingRules.CROSS_LANE_BURST_GUARD_TICKS))) {
                    continue;
                }

                long globalCooldown = ParanoiaPacingRules.effectiveGlobalCooldownTicks(
                        phase,
                        scenario.profile(),
                        scenario.danger(),
                        scenario.configuredGlobalCooldownSeconds());
                if (!cooldownActive(lastGlobalEvent, now, globalCooldown)) {
                    long referenceTick = lastGlobalEvent == Long.MIN_VALUE || now < lastGlobalEvent ? 0L : lastGlobalEvent;
                    boolean forcedBySilence = now - referenceTick >= ParanoiaPacingRules.maxSilenceTicks(
                            phase, scenario.profile(), scenario.danger());
                    double triggerRoll = random.nextDouble();
                    if (forcedBySilence || triggerRoll <= adjustedChance(
                            director,
                            ParanoiaEventLane.PRIMARY,
                            ParanoiaPacingRules.autoTriggerChance(
                                    phase, scenario.profile(), scenario.danger()))) {
                        List<WeightedChoice<String>> choices = choices(
                                primaryEvents(scenario),
                                ParanoiaEventLane.PRIMARY,
                                scenario,
                                eventCooldownUntil,
                                now,
                                director,
                                phase);
                        String selected = select(random, choices);
                        if (selected != null) {
                            samples.add(sample(now, selected, ParanoiaEventLane.PRIMARY));
                            if (director != null) director.record(selected);
                            lastGlobalEvent = now;
                            eventCooldownUntil.put(selected, now + rollEventCooldown(random, scenario, selected, phase));
                        }
                    }
                }

                if (scenario.includeAmbient()) {
                    long ambientGlobalCooldown = ParanoiaPacingRules.ambientGlobalCooldownTicks(
                            phase, scenario.profile(), scenario.danger());
                    if (!cooldownActive(lastAmbientEvent, now, ambientGlobalCooldown)
                            && random.nextDouble() <= adjustedChance(
                                    director,
                                    ParanoiaEventLane.AMBIENT,
                                    ParanoiaPacingRules.ambientTriggerChance(
                                    phase, scenario.profile(), scenario.danger()))) {
                        List<WeightedChoice<String>> choices = choices(
                                ambientEvents(scenario),
                                ParanoiaEventLane.AMBIENT,
                                scenario,
                                ambientCooldownUntil,
                                now,
                                director,
                                phase);
                        String selected = select(random, choices);
                        if (selected != null) {
                            samples.add(sample(now, selected, ParanoiaEventLane.AMBIENT));
                            if (director != null) director.record(selected);
                            lastAmbientEvent = now;
                            int seconds = ParanoiaEventCatalog.require(selected).ambientCooldownSeconds();
                            if (seconds > 0) {
                                ambientCooldownUntil.put(
                                        selected,
                                        now + seconds * (long) ParanoiaPacingRules.TICKS_PER_SECOND);
                            }
                        }
                    }
                }
            }
        }

        return report(scenario, durationTicks, samples, phaseAt);
    }

    private static List<WeightedChoice<String>> choices(
            List<ParanoiaEventDescriptor> descriptors,
            ParanoiaEventLane lane,
            Scenario scenario,
            Map<String, Long> cooldownUntil,
            long now,
            DirectorSimulationState director,
            int phase) {
        List<WeightedChoice<String>> choices = new ArrayList<>();
        for (ParanoiaEventDescriptor event : descriptors) {
            if (!event.isAvailable(phase, scenario.danger())) {
                continue;
            }
            if (!scenario.includeRetired111Events()
                    && ParanoiaEventIds.HURLER.equals(event.id())
                    && !ParanoiaPacingRules.allowsHurler(phase, true, true)) {
                continue;
            }
            Long until = cooldownUntil.get(event.id());
            if (until != null && now < until) {
                continue;
            }
            int weight = effectiveWeight(event, lane, scenario, phase);
            if (director != null) {
                weight = director.adjustedWeight(event, lane, weight);
            }
            if (weight > 0) {
                choices.add(new WeightedChoice<>(event.id(), weight));
            }
        }
        return choices;
    }

    private static double adjustedChance(
            DirectorSimulationState director,
            ParanoiaEventLane lane,
            double baseChance) {
        return director == null ? baseChance : director.adjustedChance(lane, baseChance);
    }

    private static String select(Random random, List<WeightedChoice<String>> choices) {
        if (choices.isEmpty()) {
            return null;
        }
        int totalWeight = WeightedSelector.totalWeight(choices);
        return WeightedSelector.pick(choices, random.nextInt(totalWeight));
    }

    private static long rollEventCooldown(Random random, Scenario scenario, String eventId, int phase) {
        ParanoiaEventDescriptor descriptor = descriptor(scenario, eventId);
        double jitter = random.nextDouble();
        ParanoiaPacingRules.IntRange range = ParanoiaPacingRules.eventCooldownSecondsRange(descriptor.severity());
        int baseSeconds = sample(random, range);
        long cooldown = ParanoiaPacingRules.eventCooldownTicks(
                phase,
                scenario.profile(),
                scenario.danger(),
                descriptor.severity(),
                baseSeconds,
                jitter);
        if (descriptor.eventCooldownSeconds() > 0) {
            cooldown = descriptor.eventCooldownSeconds() * (long) ParanoiaPacingRules.TICKS_PER_SECOND;
        }
        if (ParanoiaEventIds.BLACKOUT.equals(eventId)) {
            cooldown = Math.round(cooldown * 2.35D);
        } else if (ParanoiaEventIds.FOOTSTEPS.equals(eventId)) {
            cooldown = Math.round(cooldown * 1.95D);
        }
        if (!scenario.includeRetired111Events()) {
            cooldown = ParanoiaPacingRules.activeEventCooldownTicks(eventId, phase, cooldown);
        }
        return cooldown;
    }

    private static EventSample sample(long tick, String eventId, ParanoiaEventLane lane) {
        return new EventSample(tick, eventId, lane, ParanoiaEventCatalog.require(eventId).severity());
    }

    private static int sample(Random random, ParanoiaPacingRules.IntRange range) {
        return range.minInclusive() + random.nextInt(range.size());
    }

    private static boolean cooldownActive(long lastTick, long now, long cooldownTicks) {
        return cooldownTicks > 0L
                && lastTick != Long.MIN_VALUE
                && now >= lastTick
                && now - lastTick < cooldownTicks;
    }

    private static SimulationReport report(
            Scenario scenario,
            long durationTicks,
            List<EventSample> samples,
            LongToIntFunction phaseAt) {
        Map<String, Long> counts = new LinkedHashMap<>();
        Map<String, Long> repeatedFamilies = new LinkedHashMap<>();
        EnumMap<ParanoiaEventLane, Long> laneCounts = new EnumMap<>(ParanoiaEventLane.class);
        EnumMap<ParanoiaEventLane, String> previousByLane = new EnumMap<>(ParanoiaEventLane.class);
        List<Long> primaryTicks = new ArrayList<>();
        long strongEvents = 0L;
        long burstCount = 0L;
        long previousTick = Long.MIN_VALUE;

        for (EventSample event : samples) {
            counts.merge(event.eventId(), 1L, Long::sum);
            laneCounts.merge(event.lane(), 1L, Long::sum);
            if (event.severity() == ParanoiaEventSeverity.HIGH
                    || event.severity() == ParanoiaEventSeverity.EXTREME) {
                strongEvents++;
            }
            CampaignEventFamily classifiedFamily = CampaignEventFamily.forEvent(event.eventId());
            String family = classifiedFamily == CampaignEventFamily.UNKNOWN
                    ? event.eventId() : classifiedFamily.name();
            String previousFamily = previousByLane.put(event.lane(), family);
            if (family.equals(previousFamily)) {
                repeatedFamilies.merge(family, 1L, Long::sum);
            }
            if (previousTick != Long.MIN_VALUE && event.tick() - previousTick <= BURST_THRESHOLD_TICKS) {
                burstCount++;
            }
            previousTick = event.tick();
            if (event.lane() == ParanoiaEventLane.PRIMARY) {
                primaryTicks.add(event.tick());
            }
        }

        List<Long> silenceGaps = new ArrayList<>();
        long previousPrimary = 0L;
        for (Long tick : primaryTicks) {
            silenceGaps.add(tick - previousPrimary);
            previousPrimary = tick;
        }
        silenceGaps.add(Math.max(0L, durationTicks - previousPrimary));
        double averageSilenceTicks = silenceGaps.stream().mapToLong(Long::longValue).average().orElse(durationTicks);
        long maximumSilenceTicks = silenceGaps.stream().mapToLong(Long::longValue).max().orElse(durationTicks);
        long longEmptyPeriods = 0L;
        long gapStartTick = 0L;
        for (Long gap : silenceGaps) {
            int phase = phaseAt.applyAsInt(gapStartTick);
            long maximumAllowedSilence = ParanoiaPacingRules.maxSilenceTicks(
                    phase, scenario.profile(), scenario.danger());
            if (gap > maximumAllowedSilence) {
                longEmptyPeriods++;
            }
            gapStartTick += gap;
        }

        Map<String, Integer> effectiveWeights = new LinkedHashMap<>();
        Set<String> ineligible = new LinkedHashSet<>();
        int finalPhase = phaseAt.applyAsInt(durationTicks);
        for (ParanoiaEventLane lane : List.of(
                ParanoiaEventLane.PRIMARY, ParanoiaEventLane.AMBIENT, ParanoiaEventLane.SPECIAL)) {
            List<ParanoiaEventDescriptor> descriptors = switch (lane) {
                case PRIMARY -> primaryEvents(scenario);
                case AMBIENT -> ambientEvents(scenario);
                case SPECIAL -> specialEvents(scenario);
                default -> List.of();
            };
            for (ParanoiaEventDescriptor event : descriptors) {
                int weight = event.isAvailable(finalPhase, scenario.danger())
                        ? effectiveWeight(event, lane, scenario, finalPhase)
                        : 0;
                String key = lane.name().toLowerCase() + ":" + event.id();
                if (weight > 0) {
                    effectiveWeights.put(key, weight);
                } else {
                    ineligible.add(key);
                }
            }
        }

        double hours = durationTicks / (double) (60 * 60 * ParanoiaPacingRules.TICKS_PER_SECOND);
        return new SimulationReport(
                scenario,
                durationTicks,
                samples.size(),
                samples.size() / hours,
                strongEvents / hours,
                ticksToSeconds(averageSilenceTicks),
                ticksToSeconds(maximumSilenceTicks),
                burstCount,
                longEmptyPeriods,
                immutable(counts),
                immutable(repeatedFamilies),
                Collections.unmodifiableMap(new EnumMap<>(laneCounts)),
                immutableInt(effectiveWeights),
                Collections.unmodifiableSet(new LinkedHashSet<>(ineligible)),
                Collections.unmodifiableList(new ArrayList<>(samples)));
    }

    private static double ticksToSeconds(double ticks) {
        return ticks / ParanoiaPacingRules.TICKS_PER_SECOND;
    }

    private static List<ParanoiaEventDescriptor> primaryEvents(Scenario scenario) {
        return scenario.includeRetired111Events()
                ? ParanoiaEventCatalog.referencePrimaryEvents111()
                : ParanoiaEventCatalog.primaryEvents();
    }

    private static List<ParanoiaEventDescriptor> ambientEvents(Scenario scenario) {
        return scenario.includeRetired111Events()
                ? ParanoiaEventCatalog.referenceAmbientEvents111()
                : ParanoiaEventCatalog.ambientEvents();
    }

    private static List<ParanoiaEventDescriptor> specialEvents(Scenario scenario) {
        return scenario.includeRetired111Events()
                ? ParanoiaEventCatalog.referenceSpecialEvents111()
                : ParanoiaEventCatalog.specialEvents();
    }

    private static ParanoiaEventDescriptor descriptor(Scenario scenario, String eventId) {
        return scenario.includeRetired111Events()
                ? ParanoiaEventCatalog.referenceRequire111(eventId)
                : ParanoiaEventCatalog.require(eventId);
    }

    private static int effectiveWeight(
            ParanoiaEventDescriptor event,
            ParanoiaEventLane lane,
            Scenario scenario,
            int phase) {
        if (scenario.includeRetired111Events()) {
            return ParanoiaPacingRules.effectiveWeight(
                    event.id(), event.baseWeight(lane), scenario.profile(), scenario.danger());
        }
        return ParanoiaPacingRules.activeEffectiveWeight(
                event.id(), event.baseWeight(lane), phase, scenario.profile(), scenario.danger());
    }

    private static long specialGlobalCooldownTicks(Scenario scenario, int phase) {
        if (scenario.includeRetired111Events()) {
            return ParanoiaPacingRules.specialGlobalCooldownTicks(
                    phase, scenario.profile(), scenario.danger());
        }
        return ParanoiaPacingRules.activeSpecialGlobalCooldownTicks(
                phase, scenario.profile(), scenario.danger());
    }

    private static long specialPerKeyCooldownTicks(String eventId, Scenario scenario, int phase) {
        if (scenario.includeRetired111Events()) {
            return ParanoiaPacingRules.specialPerKeyCooldownTicks(
                    eventId, phase, scenario.profile(), scenario.danger());
        }
        return ParanoiaPacingRules.activeSpecialPerKeyCooldownTicks(
                eventId, phase, scenario.profile(), scenario.danger());
    }

    private static <K> Map<K, Long> immutable(Map<K, Long> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static Map<String, Integer> immutableInt(Map<String, Integer> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static final class DirectorSimulationState {
        private static final long TRANSITION_SALT = 0x43414D504149474EL;
        private static final long DURATION_SALT = 0x4449524543544F52L;

        private final long seed;
        private final int campaignLengthDays;
        private final ArrayDeque<String> recentFamilies = new ArrayDeque<>();
        private CampaignBeat beat = CampaignBeat.UNEASE;
        private long beatRemainingTicks;
        private long elapsedTicks;
        private long lastStrongEventTick = Long.MIN_VALUE;
        private int sequence;

        private DirectorSimulationState(long seed, int campaignLengthDays) {
            this.seed = seed ^ 0x454F545643414D50L;
            this.campaignLengthDays = campaignLengthDays;
            this.beatRemainingTicks = CampaignDirectorRules.beatDurationTicks(
                    beat,
                    CampaignDirectorRules.deterministicUnit(this.seed, 0, DURATION_SALT));
        }

        private void advanceTo(long targetTick) {
            long delta = Math.max(0L, targetTick - elapsedTicks);
            elapsedTicks = targetTick;
            beatRemainingTicks -= delta;
            int guard = 0;
            while (beatRemainingTicks <= 0L && guard++ < 128) {
                long overdue = Math.min(0L, beatRemainingTicks);
                sequence++;
                beat = CampaignDirectorRules.nextBeat(
                        beat,
                        CampaignDirectorRules.deterministicUnit(seed, sequence, TRANSITION_SALT));
                beatRemainingTicks = CampaignDirectorRules.beatDurationTicks(
                        beat,
                        CampaignDirectorRules.deterministicUnit(seed, sequence, DURATION_SALT)) + overdue;
            }
        }

        private double adjustedChance(ParanoiaEventLane lane, double baseChance) {
            return CampaignDirectorRules.triggerChance(
                    baseChance,
                    lane,
                    CampaignDirectorRules.actAt(elapsedTicks, campaignLengthDays),
                    beat);
        }

        private int adjustedWeight(
                ParanoiaEventDescriptor descriptor,
                ParanoiaEventLane lane,
                int baseWeight) {
            return CampaignDirectorRules.adjustedWeight(
                    baseWeight,
                    lane,
                    descriptor.severity(),
                    CampaignEventFamily.forEvent(descriptor.id()),
                    List.copyOf(recentFamilies),
                    elapsedTicks,
                    lastStrongEventTick,
                    CampaignDirectorRules.actAt(elapsedTicks, campaignLengthDays),
                    beat);
        }

        private void record(String eventId) {
            CampaignEventFamily family = CampaignEventFamily.forEvent(eventId);
            recentFamilies.addFirst(family.name());
            while (recentFamilies.size() > CampaignDirectorRules.RECENT_FAMILY_LIMIT) {
                recentFamilies.removeLast();
            }
            ParanoiaEventDescriptor descriptor = ParanoiaEventCatalog.require(eventId);
            if (descriptor.severity() == ParanoiaEventSeverity.HIGH
                    || descriptor.severity() == ParanoiaEventSeverity.EXTREME) {
                lastStrongEventTick = elapsedTicks;
            }
        }
    }

    public record Scenario(
            int phase,
            int profile,
            int danger,
            double hours,
            long seed,
            int configuredGlobalCooldownSeconds,
            boolean includeAmbient,
            boolean includeSpecials,
            boolean includeRetired111Events,
            boolean includeCampaignDirector,
            int campaignLengthDays) {
        public Scenario {
            if (phase < 1 || phase > 4) {
                throw new IllegalArgumentException("phase must be in [1, 4]");
            }
            if (profile < 1 || profile > 5) {
                throw new IllegalArgumentException("profile must be in [1, 5]");
            }
            if (danger < 0 || danger > 5) {
                throw new IllegalArgumentException("danger must be in [0, 5]");
            }
            if (!Double.isFinite(hours) || hours <= 0.0D) {
                throw new IllegalArgumentException("hours must be finite and positive");
            }
            if (campaignLengthDays != CampaignDirectorRules.STANDARD_LENGTH_DAYS
                    && campaignLengthDays != CampaignDirectorRules.EXTRA_LONG_LENGTH_DAYS) {
                throw new IllegalArgumentException("campaignLengthDays must be 50 or 100");
            }
        }

        public static Scenario reference(int phase, int profile, int danger, double hours, long seed) {
            return new Scenario(
                    phase,
                    profile,
                    danger,
                    hours,
                    seed,
                    DEFAULT_GLOBAL_COOLDOWN_SECONDS,
                    true,
                    true,
                    false,
                    true,
                    CampaignDirectorRules.STANDARD_LENGTH_DAYS);
        }

        public static Scenario reference(
                int phase, int profile, int danger, double hours, long seed, int campaignLengthDays) {
            return reference(phase, profile, danger, hours, seed, campaignLengthDays, true);
        }

        public static Scenario reference(
                int phase,
                int profile,
                int danger,
                double hours,
                long seed,
                int campaignLengthDays,
                boolean includeCampaignDirector) {
            return new Scenario(
                    phase,
                    profile,
                    danger,
                    hours,
                    seed,
                    DEFAULT_GLOBAL_COOLDOWN_SECONDS,
                    true,
                    true,
                    false,
                    includeCampaignDirector,
                    campaignLengthDays);
        }

        /** Historical comparison mode; never used by the live scheduler. */
        public static Scenario reference111(int phase, int profile, int danger, double hours, long seed) {
            return new Scenario(
                    phase,
                    profile,
                    danger,
                    hours,
                    seed,
                    DEFAULT_GLOBAL_COOLDOWN_SECONDS,
                    true,
                    true,
                    true,
                    false,
                    CampaignDirectorRules.STANDARD_LENGTH_DAYS);
        }
    }

    public record EventSample(
            long tick,
            String eventId,
            ParanoiaEventLane lane,
            ParanoiaEventSeverity severity) {
    }

    public record SimulationReport(
            Scenario scenario,
            long durationTicks,
            long totalEvents,
            double eventsPerHour,
            double strongEventsPerHour,
            double averagePrimarySilenceSeconds,
            double maximumPrimarySilenceSeconds,
            long burstCount,
            long longEmptyPeriodCount,
            Map<String, Long> countsByEvent,
            Map<String, Long> consecutiveRepeatsByFamily,
            Map<ParanoiaEventLane, Long> countsByLane,
            Map<String, Integer> effectiveWeights,
            Set<String> ineligibleEvents,
            List<EventSample> eventSamples) {
    }
}
