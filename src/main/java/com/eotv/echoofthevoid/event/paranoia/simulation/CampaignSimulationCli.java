package com.eotv.echoofthevoid.event.paranoia.simulation;

import java.util.Locale;

/** Command-line entry point for the whole-campaign release audit. */
public final class CampaignSimulationCli {
    private CampaignSimulationCli() {
    }

    public static void main(String[] args) {
        int profile = intArgument(args, "profile", 3);
        int danger = intArgument(args, "danger", 3);
        int days = intArgument(args, "days", 50);
        long seed = longArgument(args, "seed", 0xE07F0200L);
        CampaignReleaseSimulator.CampaignScenario scenario = days == 100
                ? CampaignReleaseSimulator.CampaignScenario.extraLong(profile, danger, seed)
                : CampaignReleaseSimulator.CampaignScenario.standard(profile, danger, seed);
        CampaignReleaseSimulator.CampaignReport report = CampaignReleaseSimulator.simulate(scenario);

        System.out.printf(Locale.ROOT,
                "campaign=%dd profile=%d danger=%d seed=%d events=%d events/h=%.3f strong/h=%.3f "
                        + "weather=%d tension=%d grand=%d culmination=%.2fd forced=%s double=%d "
                        + "majorBursts=%d strongBursts=%d maxSilence=%.2fs%n",
                days,
                profile,
                danger,
                seed,
                report.events().totalEvents(),
                report.events().eventsPerHour(),
                report.events().strongEventsPerHour(),
                report.weather().totalEvents(),
                report.tensionBuilderCount(),
                report.grandWardenCount(),
                report.culminationTick() / 24000.0D,
                report.forcedCulmination(),
                report.doubleCulminationCount(),
                report.unjustifiedMajorBurstCount(),
                report.strongEventBurstCount(),
                report.events().maximumPrimarySilenceSeconds());
        System.out.println("weatherCounts=" + report.weather().countsByEvent());
        System.out.println("eventLaneCounts=" + report.events().countsByLane());
        System.out.println("ineligibleEvents=" + report.permanentlyIneligibleEventIds());
        System.out.println("ineligibleWeather=" + report.weather().ineligibleEventIds());
    }

    private static int intArgument(String[] args, String name, int fallback) {
        String value = argument(args, name);
        return value == null ? fallback : Integer.parseInt(value);
    }

    private static long longArgument(String[] args, String name, long fallback) {
        String value = argument(args, name);
        return value == null ? fallback : Long.decode(value);
    }

    private static String argument(String[] args, String name) {
        String prefix = "--" + name + "=";
        for (String argument : args) {
            if (argument.startsWith(prefix)) {
                return argument.substring(prefix.length());
            }
        }
        return null;
    }
}
