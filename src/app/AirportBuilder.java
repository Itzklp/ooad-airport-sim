package app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import airport.Airport;
import airport.CarrierGroup;
import airport.CheckInArea;
import airport.CheckInPoint;
import airport.LineChoice;
import airport.PassengerSource;
import airport.RandomSplit;
import airport.ResourceType;
import airport.ScreeningLane;
import airport.SecurityCheckpoint;
import airport.ServicePoint;
import airport.ShortestLine;
import airport.SimListener;
import airport.WaitingLine;
import sim.DistributionFactory;

public class AirportBuilder {

    // An empty list means the scenario can be run; build() relies on this having been checked.
    public List<String> check(Scenario scenario) {
        List<String> problems = new ArrayList<>();
        positiveWholeNumber(scenario, "runs", problems);
        positiveNumber(scenario, "hours", problems);
        if (scenario.get("seed") == null) {
            problems.add("seed is missing");
        } else {
            try {
                scenario.seed();
            } catch (NumberFormatException e) {
                problems.add("seed must be a whole number");
            }
        }
        distribution(scenario, "arrival", problems);
        lineRule(scenario, "checkin.lineRule", problems);

        String groupList = scenario.get("groups");
        if (groupList == null || groupList.isBlank()) {
            problems.add("groups is missing");
        } else {
            double shareTotal = 0.0;
            for (String key : groupKeys(scenario)) {
                String prefix = "group." + key + ".";
                if (scenario.get(prefix + "name") == null) problems.add(prefix + "name is missing");
                Double share = fraction(scenario, prefix + "share", problems);
                Double kioskShare = fraction(scenario, prefix + "kioskShare", problems);
                if (share != null) shareTotal += share;
                Integer kiosks = wholeNumber(scenario, prefix + "kiosks", problems);
                Integer kioskLines = wholeNumber(scenario, prefix + "kioskLines", problems);
                Integer counters = wholeNumber(scenario, prefix + "counters", problems);
                Integer counterLines = wholeNumber(scenario, prefix + "counterLines", problems);
                if (kioskShare != null && kiosks != null && kioskLines != null) {
                    pointsAndLines(prefix + "kiosk", kioskShare > 0, kiosks, kioskLines, problems);
                    if (kiosks > 0) distribution(scenario, prefix + "kioskTime", problems);
                }
                if (kioskShare != null && counters != null && counterLines != null) {
                    pointsAndLines(prefix + "counter", kioskShare < 1, counters, counterLines, problems);
                    if (counters > 0) distribution(scenario, prefix + "counterTime", problems);
                }
            }
            if (Math.abs(shareTotal - 1.0) > 0.001) {
                problems.add("the group shares add up to " + shareTotal + ", not 1");
            }
        }

        Integer lanes = wholeNumber(scenario, "security.lanes", problems);
        Integer lines = wholeNumber(scenario, "security.lines", problems);
        if (lanes != null && lines != null) pointsAndLines("security.", true, lanes, lines, problems);
        lineRule(scenario, "security.lineRule", problems);
        distribution(scenario, "security.time", problems);
        fraction(scenario, "security.stopShare", problems);
        distribution(scenario, "security.stopTime", problems);
        return problems;
    }

    public Airport build(Scenario scenario, SimListener listener) {
        List<CarrierGroup> groups = new ArrayList<>();
        Map<String, CarrierGroup> groupsByKey = new LinkedHashMap<>();
        for (String key : groupKeys(scenario)) {
            String prefix = "group." + key + ".";
            CarrierGroup group = new CarrierGroup(scenario.get(prefix + "name"),
                    scenario.getDouble(prefix + "share"), scenario.getDouble(prefix + "kioskShare"));
            groups.add(group);
            groupsByKey.put(key, group);
        }

        PassengerSource source = new PassengerSource(DistributionFactory.parse(scenario.get("arrival")), groups);
        source.addListener(listener);

        List<WaitingLine> allLines = new ArrayList<>();
        List<ServicePoint> allPoints = new ArrayList<>();

        // security is built first, because every check-in point needs to know where to send passengers
        int laneCount = scenario.getInt("security.lanes");
        List<WaitingLine> securityLines = makeLines("Security checkpoint", scenario.getInt("security.lines"), true);
        SecurityCheckpoint security = new SecurityCheckpoint(securityLines, lineChoice(scenario.get("security.lineRule")),
                scenario.getDouble("security.stopShare"), DistributionFactory.parse(scenario.get("security.stopTime")));
        for (int i = 0; i < laneCount; i++) {
            ScreeningLane lane = new ScreeningLane("Screening lane " + (i + 1),
                    DistributionFactory.parse(scenario.get("security.time")),
                    securityLines.get(i % securityLines.size()), security);
            lane.addListener(listener);
            allPoints.add(lane);
        }

        LineChoice checkInChoice = lineChoice(scenario.get("checkin.lineRule"));
        Map<CarrierGroup, CheckInArea> areas = new LinkedHashMap<>();
        for (Map.Entry<String, CarrierGroup> entry : groupsByKey.entrySet()) {
            String prefix = "group." + entry.getKey() + ".";
            CarrierGroup group = entry.getValue();
            List<WaitingLine> kioskLines = makeLines(group.name() + " self check-in", scenario.getInt(prefix + "kioskLines"), false);
            List<WaitingLine> counterLines = makeLines(group.name() + " manned check-in", scenario.getInt(prefix + "counterLines"), false);
            int kioskCount = scenario.getInt(prefix + "kiosks");
            for (int i = 0; i < kioskCount; i++) {
                CheckInPoint kiosk = new CheckInPoint(group.name() + " kiosk " + (i + 1), ResourceType.SELF_CHECK_IN,
                        DistributionFactory.parse(scenario.get(prefix + "kioskTime")), kioskLines.get(i % kioskLines.size()), security);
                kiosk.addListener(listener);
                allPoints.add(kiosk);
            }
            int counterCount = scenario.getInt(prefix + "counters");
            for (int i = 0; i < counterCount; i++) {
                CheckInPoint counter = new CheckInPoint(group.name() + " counter " + (i + 1), ResourceType.MANNED_CHECK_IN,
                        DistributionFactory.parse(scenario.get(prefix + "counterTime")), counterLines.get(i % counterLines.size()), security);
                counter.addListener(listener);
                allPoints.add(counter);
            }
            areas.put(group, new CheckInArea(group, kioskLines, counterLines, checkInChoice));
            allLines.addAll(kioskLines);
            allLines.addAll(counterLines);
        }
        allLines.addAll(securityLines);
        return new Airport(scenario.name(), groups, source, areas, security, allLines, allPoints);
    }

    private static List<String> groupKeys(Scenario scenario) {
        List<String> keys = new ArrayList<>();
        for (String key : scenario.get("groups").split(",")) {
            if (!key.isBlank()) keys.add(key.trim());
        }
        return keys;
    }

    // Check-in lines of one kind share a report category, so one line and two lines are reported the same way.
    // Security lines are reported one by one, as in the paper.
    private static List<WaitingLine> makeLines(String baseName, int count, boolean eachOwnCategory) {
        List<WaitingLine> lines = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String name = count == 1 && !eachOwnCategory ? baseName : baseName + " " + i;
            lines.add(new WaitingLine(name, eachOwnCategory ? name : baseName));
        }
        return lines;
    }

    private static LineChoice lineChoice(String rule) {
        return "shortest".equals(rule) ? new ShortestLine() : new RandomSplit();
    }

    private static void pointsAndLines(String prefix, boolean needed, int points, int lines, List<String> problems) {
        if (needed && points < 1) problems.add(prefix + "s: at least one is needed");
        if (points > 0 && lines < 1) problems.add(prefix + "Lines: at least one line is needed");
        if (lines > points) problems.add(prefix + "Lines: more lines than service points");
    }

    private static void distribution(Scenario scenario, String key, List<String> problems) {
        try {
            DistributionFactory.parse(scenario.get(key));
        } catch (IllegalArgumentException e) {
            problems.add(key + ": " + e.getMessage());
        }
    }

    private static void lineRule(Scenario scenario, String key, List<String> problems) {
        String rule = scenario.get(key);
        if (!"random".equals(rule) && !"shortest".equals(rule)) problems.add(key + " must be random or shortest");
    }

    private static Double fraction(Scenario scenario, String key, List<String> problems) {
        try {
            double value = scenario.getDouble(key);
            if (value < 0 || value > 1) {
                problems.add(key + " must be between 0 and 1");
                return null;
            }
            return value;
        } catch (NullPointerException | NumberFormatException e) {
            problems.add(key + " is missing or not a number");
            return null;
        }
    }

    private static Integer wholeNumber(Scenario scenario, String key, List<String> problems) {
        try {
            int value = scenario.getInt(key);
            if (value < 0) {
                problems.add(key + " cannot be negative");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            problems.add(key + " is missing or not a whole number");
            return null;
        }
    }

    private static void positiveWholeNumber(Scenario scenario, String key, List<String> problems) {
        Integer value = wholeNumber(scenario, key, problems);
        if (value != null && value < 1) problems.add(key + " must be at least 1");
    }

    private static void positiveNumber(Scenario scenario, String key, List<String> problems) {
        try {
            if (scenario.getDouble(key) <= 0) problems.add(key + " must be greater than 0");
        } catch (NullPointerException | NumberFormatException e) {
            problems.add(key + " is missing or not a number");
        }
    }
}
