package results;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import airport.ResourceType;

// Combines the runs the way the paper reports them: average of the run averages,
// smallest minimum and largest maximum, all in minutes.
public class ExperimentResult {

    private final String scenarioName;
    private final String note;
    private final List<RunResult> runs = new ArrayList<>();

    public ExperimentResult(String scenarioName, String note) {
        this.scenarioName = scenarioName;
        this.note = note;
    }

    public void add(RunResult run) {
        runs.add(run);
    }

    public int runCount() {
        return runs.size();
    }

    public String scenarioName() {
        return scenarioName;
    }

    public Set<String> categories() {
        Set<String> names = new LinkedHashSet<>();
        for (RunResult run : runs) names.addAll(run.waits().keySet());
        return names;
    }

    // Runs in which nobody used the line are left out of the average.
    public double averageWait(String category) {
        double sum = 0.0;
        int usedRuns = 0;
        for (RunResult run : runs) {
            WaitStats waits = run.waits().get(category);
            if (waits != null && waits.count() > 0) {
                sum += waits.average();
                usedRuns++;
            }
        }
        return usedRuns == 0 ? 0.0 : sum / usedRuns / 60.0;
    }

    // Standard deviation between the run averages, which is what a t-test between scenarios needs.
    public double waitStdDev(String category) {
        double mean = averageWait(category) * 60.0;
        double sumOfSquares = 0.0;
        int usedRuns = 0;
        for (RunResult run : runs) {
            WaitStats waits = run.waits().get(category);
            if (waits != null && waits.count() > 0) {
                double difference = waits.average() - mean;
                sumOfSquares += difference * difference;
                usedRuns++;
            }
        }
        return usedRuns < 2 ? 0.0 : Math.sqrt(sumOfSquares / (usedRuns - 1)) / 60.0;
    }

    public double minWait(String category) {
        double smallest = Double.POSITIVE_INFINITY;
        for (RunResult run : runs) {
            WaitStats waits = run.waits().get(category);
            if (waits != null && waits.count() > 0) smallest = Math.min(smallest, waits.min());
        }
        return smallest == Double.POSITIVE_INFINITY ? 0.0 : smallest / 60.0;
    }

    public double maxWait(String category) {
        double largest = 0.0;
        for (RunResult run : runs) {
            WaitStats waits = run.waits().get(category);
            if (waits != null) largest = Math.max(largest, waits.max());
        }
        return largest / 60.0;
    }

    public double averageUtilization(ResourceType type) {
        double sum = 0.0;
        int runsWithType = 0;
        for (RunResult run : runs) {
            Double utilization = run.utilization().get(type);
            if (utilization != null) {
                sum += utilization;
                runsWithType++;
            }
        }
        return runsWithType == 0 ? 0.0 : 100.0 * sum / runsWithType;
    }

    public double averageArrivals(String group) {
        double sum = 0.0;
        for (RunResult run : runs) sum += run.arrivals().getOrDefault(group, 0);
        return runs.isEmpty() ? 0.0 : sum / runs.size();
    }

    private Set<String> groups() {
        Set<String> names = new LinkedHashSet<>();
        for (RunResult run : runs) names.addAll(run.arrivals().keySet());
        return names;
    }

    public String toText() {
        StringBuilder text = new StringBuilder();
        text.append("Scenario: ").append(scenarioName).append("  (").append(runs.size()).append(" runs)\n");
        if (note != null && !note.isBlank()) text.append("Note: ").append(note).append('\n');
        text.append(String.format(Locale.ROOT, "%n%-38s %10s %10s %10s %10s%n", "Waiting time (minutes)", "Average", "Std dev", "Minimum", "Maximum"));
        for (String category : categories()) {
            text.append(String.format(Locale.ROOT, "%-38s %10.2f %10.2f %10.2f %10.2f%n",
                    category, averageWait(category), waitStdDev(category), minWait(category), maxWait(category)));
        }
        text.append("(Std dev is between the run averages.)\n");
        text.append(String.format(Locale.ROOT, "%n%-38s %10s%n", "Utilization", "Average"));
        for (ResourceType type : ResourceType.values()) {
            text.append(String.format(Locale.ROOT, "%-38s %9.2f%%%n", type.label(), averageUtilization(type)));
        }
        text.append(String.format(Locale.ROOT, "%n%-38s %10s%n", "Passengers per day", "Average"));
        for (String group : groups()) {
            text.append(String.format(Locale.ROOT, "%-38s %10.2f%n", group, averageArrivals(group)));
        }
        return text.toString();
    }

    public String toCsv() {
        StringBuilder csv = new StringBuilder("section,name,average,std_dev,minimum,maximum\n");
        for (String category : categories()) {
            csv.append(String.format(Locale.ROOT, "wait_minutes,%s,%.4f,%.4f,%.4f,%.4f%n",
                    category, averageWait(category), waitStdDev(category), minWait(category), maxWait(category)));
        }
        for (ResourceType type : ResourceType.values()) {
            csv.append(String.format(Locale.ROOT, "utilization_percent,%s,%.4f,,,%n", type.label(), averageUtilization(type)));
        }
        for (String group : groups()) {
            csv.append(String.format(Locale.ROOT, "passengers_per_day,%s,%.4f,,,%n", group, averageArrivals(group)));
        }
        return csv.toString();
    }

    public static String compare(List<ExperimentResult> results) {
        Set<String> rows = new LinkedHashSet<>();
        for (ExperimentResult result : results) rows.addAll(result.categories());
        StringBuilder table = new StringBuilder(String.format(Locale.ROOT, "%-38s", "Average wait (minutes)"));
        for (ExperimentResult result : results) table.append(String.format(Locale.ROOT, " %16.16s", result.scenarioName()));
        table.append('\n');
        for (String row : rows) {
            table.append(String.format(Locale.ROOT, "%-38s", row));
            for (ExperimentResult result : results) {
                table.append(result.categories().contains(row)
                        ? String.format(Locale.ROOT, " %16.2f", result.averageWait(row))
                        : String.format(Locale.ROOT, " %16s", "-"));
            }
            table.append('\n');
        }
        table.append(String.format(Locale.ROOT, "%n%-38s%n", "Utilization (%)"));
        for (ResourceType type : ResourceType.values()) {
            table.append(String.format(Locale.ROOT, "%-38s", type.label()));
            for (ExperimentResult result : results) table.append(String.format(Locale.ROOT, " %16.2f", result.averageUtilization(type)));
            table.append('\n');
        }
        return table.toString();
    }
}
