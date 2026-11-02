package results;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import airport.ResourceType;

class ReportTest {

    private static WaitStats waitsOf(double... seconds) {
        WaitStats stats = new WaitStats();
        for (double wait : seconds) stats.add(wait);
        return stats;
    }

    private static RunResult run(WaitStats counterWaits, double mannedUse, int passengers) {
        Map<String, WaitStats> waits = new LinkedHashMap<>();
        waits.put("Counter line", counterWaits);
        waits.put("Security 1", waitsOf(0, 6));
        Map<ResourceType, Double> utilization = new EnumMap<>(ResourceType.class);
        utilization.put(ResourceType.MANNED_CHECK_IN, mannedUse);
        utilization.put(ResourceType.SECURITY, 0.5);
        return new RunResult(waits, utilization, Map.of("Airline", passengers));
    }

    // Run 1: counter waits 60 and 120 s. Run 2: 0 and 240 s. Run 3: nobody used the counter.
    private static ExperimentResult threeRuns(String name) {
        ExperimentResult result = new ExperimentResult(name, "Counts are assumptions.");
        result.add(run(waitsOf(60, 120), 0.70, 100));
        result.add(run(waitsOf(0, 240), 0.80, 110));
        result.add(run(waitsOf(), 0.90, 120));
        return result;
    }

    @Test
    void averageWaitIsTheAverageOfRunAveragesInMinutes() {
        // run averages are 90 s and 120 s; the empty run is left out
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
        ExperimentResult result = threeRuns("A");
        assertEquals(0.0, result.minWait("Counter line"), 1e-9);
        assertEquals(4.0, result.maxWait("Counter line"), 1e-9);
    }

    @Test
    void utilizationAndArrivalsAreAveragedOverRuns() {
        ExperimentResult result = threeRuns("A");
        assertEquals(80.0, result.averageUtilization(ResourceType.MANNED_CHECK_IN), 1e-9);
        assertEquals(0.0, result.averageUtilization(ResourceType.SELF_CHECK_IN), "no kiosks at all");
        assertEquals(110.0, result.averageArrivals("Airline"), 1e-9);
        assertEquals(3, result.runCount());
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
        Map<String, WaitStats> waits = new LinkedHashMap<>();
        waits.put("Only in B", waitsOf(120));
        other.add(new RunResult(waits, new EnumMap<>(ResourceType.class), Map.of()));

        List<String> rows = ExperimentResult.compare(List.of(threeRuns("A"), other)).lines().toList();
        assertTrue(rows.get(0).matches("Average wait \\(minutes\\)\\s+A\\s+B"), rows.get(0));
        String counterRow = rows.stream().filter(row -> row.startsWith("Counter line")).findFirst().orElseThrow();
        assertTrue(counterRow.matches("Counter line\\s+1\\.75\\s+-"), "a line missing from B shows -: " + counterRow);
        String onlyInB = rows.stream().filter(row -> row.startsWith("Only in B")).findFirst().orElseThrow();
        assertTrue(onlyInB.matches("Only in B\\s+-\\s+2\\.00"), onlyInB);
    }
}
