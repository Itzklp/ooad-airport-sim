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

/**
 * Checks a Scenario and builds a fresh Airport from it. (Builder)
 * check() lists every problem; build() assumes check() found none.
 */
public class AirportBuilder {

    /** Returns a list of readable problems; an empty list means the scenario can be run. */
    public List<String> check(Scenario s) {
        List<String> errors = new ArrayList<>();
        positiveInt(s, "runs", errors);
        positiveNumber(s, "hours", errors);
        if (s.get("seed") == null) {
            errors.add("seed is missing");
        } else {
            try {
                s.seed();
            } catch (NumberFormatException e) {
                errors.add("seed must be a whole number");
            }
        }
        distribution(s, "arrival", errors);
        lineRule(s, "checkin.lineRule", errors);

        String groupList = s.get("groups");
        if (groupList == null || groupList.isBlank()) {
            errors.add("groups is missing");
        } else {
            double shareSum = 0.0;
            for (String key : groupKeys(s)) {
                String p = "group." + key + ".";
                if (s.get(p + "name") == null) errors.add(p + "name is missing");
                Double share = fraction(s, p + "share", errors);
                Double kioskShare = fraction(s, p + "kioskShare", errors);
                if (share != null) shareSum += share;
                Integer kiosks = wholeNumber(s, p + "kiosks", errors);
                Integer kioskLines = wholeNumber(s, p + "kioskLines", errors);
                Integer counters = wholeNumber(s, p + "counters", errors);
                Integer counterLines = wholeNumber(s, p + "counterLines", errors);
                if (kioskShare != null && kiosks != null && kioskLines != null) {
                    pointsAndLines(p + "kiosk", kioskShare > 0, kiosks, kioskLines, errors);
                    if (kiosks > 0) distribution(s, p + "kioskTime", errors);
                }
                if (kioskShare != null && counters != null && counterLines != null) {
                    pointsAndLines(p + "counter", kioskShare < 1, counters, counterLines, errors);
                    if (counters > 0) distribution(s, p + "counterTime", errors);
                }
            }
            if (Math.abs(shareSum - 1.0) > 0.001) {
                errors.add("the group shares add up to " + shareSum + ", not 1");
            }
        }

        Integer lanes = wholeNumber(s, "security.lanes", errors);
        Integer lines = wholeNumber(s, "security.lines", errors);
        if (lanes != null && lines != null) pointsAndLines("security.", true, lanes, lines, errors);
        lineRule(s, "security.lineRule", errors);
        distribution(s, "security.time", errors);
        fraction(s, "security.stopShare", errors);
        distribution(s, "security.stopTime", errors);
        return errors;
    }

    /** Builds a new, empty airport for one run and connects the listener to it. */
    public Airport build(Scenario s, SimListener listener) {
        List<CarrierGroup> groups = new ArrayList<>();
        Map<String, CarrierGroup> byKey = new LinkedHashMap<>();
        for (String key : groupKeys(s)) {
            String p = "group." + key + ".";
            CarrierGroup g = new CarrierGroup(s.get(p + "name"), s.getDouble(p + "share"), s.getDouble(p + "kioskShare"));
            groups.add(g);
            byKey.put(key, g);
        }

        PassengerSource source = new PassengerSource(DistributionFactory.parse(s.get("arrival")), groups);
        source.addListener(listener);

        List<WaitingLine> allLines = new ArrayList<>();
        List<ServicePoint> allPoints = new ArrayList<>();

        // security first, because check-in points need to know where to send passengers
        int laneCount = s.getInt("security.lanes");
        List<WaitingLine> securityLines = makeLines("Security checkpoint", s.getInt("security.lines"), true);
        SecurityCheckpoint security = new SecurityCheckpoint(securityLines, lineChoice(s.get("security.lineRule")),
                s.getDouble("security.stopShare"), DistributionFactory.parse(s.get("security.stopTime")));
        for (int i = 0; i < laneCount; i++) {
            ScreeningLane lane = new ScreeningLane("Screening lane " + (i + 1),
                    DistributionFactory.parse(s.get("security.time")),
                    securityLines.get(i % securityLines.size()), security);
            lane.addListener(listener);
            allPoints.add(lane);
        }

        LineChoice checkInChoice = lineChoice(s.get("checkin.lineRule"));
        Map<CarrierGroup, CheckInArea> areas = new LinkedHashMap<>();
        for (Map.Entry<String, CarrierGroup> e : byKey.entrySet()) {
            String p = "group." + e.getKey() + ".";
            CarrierGroup g = e.getValue();
            List<WaitingLine> kioskLines = makeLines(g.name() + " self check-in", s.getInt(p + "kioskLines"), false);
            List<WaitingLine> counterLines = makeLines(g.name() + " manned check-in", s.getInt(p + "counterLines"), false);
            int kiosks = s.getInt(p + "kiosks");
            for (int i = 0; i < kiosks; i++) {
                CheckInPoint k = new CheckInPoint(g.name() + " kiosk " + (i + 1), ResourceType.SELF_CHECK_IN,
                        DistributionFactory.parse(s.get(p + "kioskTime")), kioskLines.get(i % kioskLines.size()), security);
                k.addListener(listener);
                allPoints.add(k);
            }
            int counters = s.getInt(p + "counters");
            for (int i = 0; i < counters; i++) {
                CheckInPoint c = new CheckInPoint(g.name() + " counter " + (i + 1), ResourceType.MANNED_CHECK_IN,
                        DistributionFactory.parse(s.get(p + "counterTime")), counterLines.get(i % counterLines.size()), security);
                c.addListener(listener);
                allPoints.add(c);
            }
            areas.put(g, new CheckInArea(g, kioskLines, counterLines, checkInChoice));
            allLines.addAll(kioskLines);
            allLines.addAll(counterLines);
        }
        allLines.addAll(securityLines);
        return new Airport(s.name(), groups, source, areas, security, allLines, allPoints);
    }

    // ---------- helpers ----------

    private static List<String> groupKeys(Scenario s) {
        List<String> keys = new ArrayList<>();
        for (String k : s.get("groups").split(",")) {
            if (!k.isBlank()) keys.add(k.trim());
        }
        return keys;
    }

    /** Check-in lines of one kind share a category, so one line and two lines report the same way.
     *  Security lines are reported one by one, as in the paper. */
    private static List<WaitingLine> makeLines(String base, int count, boolean eachOwnCategory) {
        List<WaitingLine> lines = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String name = count == 1 && !eachOwnCategory ? base : base + " " + i;
            lines.add(new WaitingLine(name, eachOwnCategory ? name : base));
        }
        return lines;
    }

    private static LineChoice lineChoice(String rule) {
        return "shortest".equals(rule) ? new ShortestLine() : new RandomSplit();
    }

    private static void pointsAndLines(String prefix, boolean needed, int points, int lines, List<String> errors) {
        if (needed && points < 1) errors.add(prefix + "s: at least one is needed");
        if (points > 0 && lines < 1) errors.add(prefix + "Lines: at least one line is needed");
        if (lines > points) errors.add(prefix + "Lines: more lines than service points");
    }

    private static void distribution(Scenario s, String key, List<String> errors) {
        try {
            DistributionFactory.parse(s.get(key));
        } catch (IllegalArgumentException e) {
            errors.add(key + ": " + e.getMessage());
        }
    }

    private static void lineRule(Scenario s, String key, List<String> errors) {
        String v = s.get(key);
        if (!"random".equals(v) && !"shortest".equals(v)) errors.add(key + " must be random or shortest");
    }

    private static Double fraction(Scenario s, String key, List<String> errors) {
        try {
            double v = s.getDouble(key);
            if (v < 0 || v > 1) {
                errors.add(key + " must be between 0 and 1");
                return null;
            }
            return v;
        } catch (NullPointerException | NumberFormatException e) {
            errors.add(key + " is missing or not a number");
            return null;
        }
    }

    private static Integer wholeNumber(Scenario s, String key, List<String> errors) {
        try {
            int v = s.getInt(key);
            if (v < 0) {
                errors.add(key + " cannot be negative");
                return null;
            }
            return v;
        } catch (NumberFormatException e) {
            errors.add(key + " is missing or not a whole number");
            return null;
        }
    }

    private static void positiveInt(Scenario s, String key, List<String> errors) {
        Integer v = wholeNumber(s, key, errors);
        if (v != null && v < 1) errors.add(key + " must be at least 1");
    }

    private static void positiveNumber(Scenario s, String key, List<String> errors) {
        try {
            if (s.getDouble(key) <= 0) errors.add(key + " must be greater than 0");
        } catch (NullPointerException | NumberFormatException e) {
            errors.add(key + " is missing or not a number");
        }
    }
}
