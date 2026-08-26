package com.eotv.echoofthevoid.event.paranoia.simulation;

import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventLane;
import java.util.Locale;
import java.util.Map;

/** Command-line entry point used by the Gradle {@code simulateParanoia} task. */
public final class ParanoiaSimulationCli {
    private ParanoiaSimulationCli() {
    }

    public static void main(String[] args) {
        int phase = intArgument(args, "phase", 4);
        int profile = intArgument(args, "profile", 3);
        int danger = intArgument(args, "danger", 3);
        double hours = doubleArgument(args, "hours", 200.0D);
        long seed = longArgument(args, "seed", 0xE07F0111L);
        boolean baseline111 = booleanArgument(args, "baseline111", false);
        boolean director = booleanArgument(args, "director", true);
        int campaignDays = intArgument(args, "campaignDays", 50);
        ParanoiaSchedulerSimulator.Scenario scenario = baseline111
                ? ParanoiaSchedulerSimulator.Scenario.reference111(phase, profile, danger, hours, seed)
                : ParanoiaSchedulerSimulator.Scenario.reference(
                        phase, profile, danger, hours, seed, campaignDays, director);
        ParanoiaSchedulerSimulator.SimulationReport report = ParanoiaSchedulerSimulator.simulate(scenario);

        System.out.printf(Locale.ROOT,
                "scenario phase=%d profile=%d danger=%d hours=%.2f seed=%d baseline111=%s director=%s campaignDays=%d%n",
                phase, profile, danger, hours, seed, baseline111, director, campaignDays);
        System.out.printf(Locale.ROOT,
                "events=%d eventsPerHour=%.4f strongPerHour=%.4f avgPrimarySilence=%.2fs maxPrimarySilence=%.2fs%n",
                report.totalEvents(),
                report.eventsPerHour(),
                report.strongEventsPerHour(),
                report.averagePrimarySilenceSeconds(),
                report.maximumPrimarySilenceSeconds());
        System.out.printf(Locale.ROOT,
                "bursts=%d longEmptyPeriods=%d ineligible=%d effectiveWeights=%d%n",
                report.burstCount(),
                report.longEmptyPeriodCount(),
                report.ineligibleEvents().size(),
                report.effectiveWeights().size());
        for (ParanoiaEventLane lane : ParanoiaEventLane.values()) {
            Long count = report.countsByLane().get(lane);
            if (count != null) {
                System.out.printf(Locale.ROOT, "lane.%s=%d%n", lane.name().toLowerCase(Locale.ROOT), count);
            }
        }
        report.countsByEvent().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .forEach(entry -> System.out.printf(Locale.ROOT, "event.%s=%d%n", entry.getKey(), entry.getValue()));
    }

    private static int intArgument(String[] args, String name, int fallback) {
        return Integer.parseInt(argument(args, name, Integer.toString(fallback)));
    }

    private static long longArgument(String[] args, String name, long fallback) {
        return Long.decode(argument(args, name, Long.toString(fallback)));
    }

    private static double doubleArgument(String[] args, String name, double fallback) {
        return Double.parseDouble(argument(args, name, Double.toString(fallback)));
    }

    private static boolean booleanArgument(String[] args, String name, boolean fallback) {
        return Boolean.parseBoolean(argument(args, name, Boolean.toString(fallback)));
    }

    private static String argument(String[] args, String name, String fallback) {
        String prefix = "--" + name + "=";
        for (String argument : args) {
            if (argument.startsWith(prefix)) {
                return argument.substring(prefix.length());
            }
        }
        return fallback;
    }
}
