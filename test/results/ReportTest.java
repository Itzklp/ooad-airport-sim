package results;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import airport.ResourceType;

/**
 * Phase 3: combining runs the way the paper reports them, and the text, CSV and comparison reports.
 * Covers requirements 9 and 10. Runs are made by hand so every figure can be checked.
 */
class ReportTest {

    private static WaitStats waits(double... seconds) {
        WaitStats w = new WaitStats();
        for (double s : seconds) w.add(s);
        return w;
    }

    private static RunResult run(WaitStats line, double mannedUse, int passengers) {
        Map<String, WaitStats> w = new LinkedHashMap<>();
        w.put("Counter line", line);
        w.put("Security 1", waits(0, 6));
        Map<ResourceType, Double> u = new EnumMap<>(ResourceType.class);
        u.put(ResourceType.MANNED_CHECK_IN, mannedUse);
        u.put(ResourceType.SECURITY, 0.5);
        return new RunResult(w, u, Map.of("Airline", passengers));
    }

    /** Run 1: waits 60 and 120 s. Run 2: waits 0 and 240 s. Run 3: nobody used the line. */
    private static ExperimentResult threeRuns(String name) {
        ExperimentResult r = new ExperimentResult(name, "Counts are assumptions.");
        r.add(run(waits(60, 120), 0.70, 100));
        r.add(run(waits(0, 240), 0.80, 110));
        r.add(run(waits(), 0.90, 120));
        return r;
    }

    @Test
    void averageWaitIsTheAverageOfRunAveragesInMinutes() {
        // run averages 90 s and 120 s; the empty run is left out
        assertEquals(1.75, threeRuns("A").averageWait("Counter line"), 1e-9);
    }

    @Test
    void standardDeviationIsBetweenRunAverages() {
        // run averages 90 s and 120 s: mean 105 s, sample standard deviation sqrt(450) s
        assertEquals(Math.sqrt(450) / 60, threeRuns("A").waitStdDev("Counter line"), 1e-9);
        assertEquals(0.0, threeRuns("A").waitStdDev("Security 1"), 1e-9, "every run had the same average");
    }

    @Test
    void minimumAndMaximumAreOverAllRuns() {
        ExperimentResult r = threeRuns("A");
        assertEquals(0.0, r.minWait("Counter line"), 1e-9);
        assertEquals(4.0, r.maxWait("Counter line"), 1e-9);
    }

    @Test
    void utilizationAndArrivalsAreAveragedOverRuns() {
        ExperimentResult r = threeRuns("A");
        assertEquals(80.0, r.averageUtilization(ResourceType.MANNED_CHECK_IN), 1e-9);
        assertEquals(0.0, r.averageUtilization(ResourceType.SELF_CHECK_IN), "no kiosks at all");
        assertEquals(110.0, r.averageArrivals("Airline"), 1e-9);
        assertEquals(3, r.runCount());
    }

    @Test
    void csvHasTheHeaderAndOneRowPerLineCategory() {
        List<String> rows = threeRuns("A").toCsv().lines().toList();
        assertEquals("section,name,average,std_dev,minimum,maximum", rows.get(0));
        assertEquals("wait_minutes,Counter line,1.7500,0.3536,0.0000,4.0000", rows.get(1));
        assertEquals("wait_minutes,Security 1,0.0500,0.0000,0.0000,0.1000", rows.get(2));
        assertEquals(1 + 2 + ResourceType.values().length + 1, rows.size(), "header, 2 lines, 3 resources, 1 airline");
        assertTrue(rows.contains("utilization_percent,Manned check-in,80.0000,,,"));
        assertTrue(rows.contains("passengers_per_day,Airline,110.0000,,,"));
    }

    @Test
    void textReportShowsTheNoteOnAssumedValues() {
        String text = threeRuns("A").toText();
        assertTrue(text.contains("Scenario: A  (3 runs)"));
        assertTrue(text.contains("Note: Counts are assumptions."));
        assertTrue(text.contains("Counter line"));
    }

    @Test
    void comparisonShowsEachScenarioAsAColumn() {
        ExperimentResult other = new ExperimentResult("B", "");
        Map<String, WaitStats> w = new LinkedHashMap<>();
        w.put("Only in B", waits(120));
        other.add(new RunResult(w, new EnumMap<>(ResourceType.class), Map.of()));

        List<String> rows = ExperimentResult.compare(List.of(threeRuns("A"), other)).lines().toList();
        assertTrue(rows.get(0).matches("Average wait \\(minutes\\)\\s+A\\s+B"), rows.get(0));
        String counterRow = rows.stream().filter(s -> s.startsWith("Counter line")).findFirst().orElseThrow();
        assertTrue(counterRow.matches("Counter line\\s+1\\.75\\s+-"), "a line missing from B shows -: " + counterRow);
        String onlyB = rows.stream().filter(s -> s.startsWith("Only in B")).findFirst().orElseThrow();
        assertTrue(onlyB.matches("Only in B\\s+-\\s+2\\.00"), onlyB);
    }
}
