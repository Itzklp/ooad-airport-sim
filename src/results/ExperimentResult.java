package results;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import airport.ResourceType;

/**
 * The results of all runs of one scenario, summarised the way the paper reports them:
 * average of the run averages, smallest minimum and largest maximum, in minutes.
 */
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
        for (RunResult r : runs) names.addAll(r.waits().keySet());
        return names;
    }

    /** Average waiting time in minutes, over the runs in which someone used the line. */
    public double averageWait(String category) {
        double sum = 0.0;
        int n = 0;
        for (RunResult r : runs) {
            WaitStats w = r.waits().get(category);
            if (w != null && w.count() > 0) {
                sum += w.average();
                n++;
            }
        }
        return n == 0 ? 0.0 : sum / n / 60.0;
    }

    /** Standard deviation of the run averages in minutes, so two scenarios can be compared as in the paper's t-tests. */
    public double waitStdDev(String category) {
        double mean = averageWait(category) * 60.0;
        double sumSq = 0.0;
        int n = 0;
        for (RunResult r : runs) {
            WaitStats w = r.waits().get(category);
            if (w != null && w.count() > 0) {
                sumSq += (w.average() - mean) * (w.average() - mean);
                n++;
            }
        }
        return n < 2 ? 0.0 : Math.sqrt(sumSq / (n - 1)) / 60.0;
    }

    public double minWait(String category) {
        double min = Double.POSITIVE_INFINITY;
        for (RunResult r : runs) {
            WaitStats w = r.waits().get(category);
            if (w != null && w.count() > 0) min = Math.min(min, w.min());
        }
        return min == Double.POSITIVE_INFINITY ? 0.0 : min / 60.0;
    }

    public double maxWait(String category) {
        double max = 0.0;
        for (RunResult r : runs) {
            WaitStats w = r.waits().get(category);
            if (w != null) max = Math.max(max, w.max());
        }
        return max / 60.0;
    }

    /** Average utilization of one resource type, as a percentage. */
    public double averageUtilization(ResourceType type) {
        double sum = 0.0;
        int n = 0;
        for (RunResult r : runs) {
            Double u = r.utilization().get(type);
            if (u != null) {
                sum += u;
                n++;
            }
        }
        return n == 0 ? 0.0 : 100.0 * sum / n;
    }

    public double averageArrivals(String group) {
        double sum = 0.0;
        for (RunResult r : runs) sum += r.arrivals().getOrDefault(group, 0);
        return runs.isEmpty() ? 0.0 : sum / runs.size();
    }

    private Set<String> groups() {
        Set<String> names = new LinkedHashSet<>();
        for (RunResult r : runs) names.addAll(r.arrivals().keySet());
        return names;
    }

    /** A plain-text report for the console. */
    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Scenario: ").append(scenarioName).append("  (").append(runs.size()).append(" runs)\n");
        if (note != null && !note.isBlank()) sb.append("Note: ").append(note).append('\n');
        sb.append(String.format(Locale.ROOT, "%n%-38s %10s %10s %10s %10s%n", "Waiting time (minutes)", "Average", "Std dev", "Minimum", "Maximum"));
        for (String c : categories()) {
            sb.append(String.format(Locale.ROOT, "%-38s %10.2f %10.2f %10.2f %10.2f%n",
                    c, averageWait(c), waitStdDev(c), minWait(c), maxWait(c)));
        }
        sb.append("(Std dev is between the run averages.)\n");
        sb.append(String.format(Locale.ROOT, "%n%-38s %10s%n", "Utilization", "Average"));
        for (ResourceType t : ResourceType.values()) {
            sb.append(String.format(Locale.ROOT, "%-38s %9.2f%%%n", t.label(), averageUtilization(t)));
        }
        sb.append(String.format(Locale.ROOT, "%n%-38s %10s%n", "Passengers per day", "Average"));
        for (String g : groups()) {
            sb.append(String.format(Locale.ROOT, "%-38s %10.2f%n", g, averageArrivals(g)));
        }
        return sb.toString();
    }

    /** The same figures as comma-separated values, for a spreadsheet. */
    public String toCsv() {
        StringBuilder sb = new StringBuilder("section,name,average,std_dev,minimum,maximum\n");
        for (String c : categories()) {
            sb.append(String.format(Locale.ROOT, "wait_minutes,%s,%.4f,%.4f,%.4f,%.4f%n",
                    c, averageWait(c), waitStdDev(c), minWait(c), maxWait(c)));
        }
        for (ResourceType t : ResourceType.values()) {
            sb.append(String.format(Locale.ROOT, "utilization_percent,%s,%.4f,,,%n", t.label(), averageUtilization(t)));
        }
        for (String g : groups()) {
            sb.append(String.format(Locale.ROOT, "passengers_per_day,%s,%.4f,,,%n", g, averageArrivals(g)));
        }
        return sb.toString();
    }

    /** Average waits and utilization of several scenarios side by side. */
    public static String compare(List<ExperimentResult> results) {
        Set<String> rows = new LinkedHashSet<>();
        for (ExperimentResult r : results) rows.addAll(r.categories());
        StringBuilder sb = new StringBuilder(String.format(Locale.ROOT, "%-38s", "Average wait (minutes)"));
        for (ExperimentResult r : results) sb.append(String.format(Locale.ROOT, " %16.16s", r.scenarioName()));
        sb.append('\n');
        for (String row : rows) {
            sb.append(String.format(Locale.ROOT, "%-38s", row));
            for (ExperimentResult r : results) {
                sb.append(r.categories().contains(row)
                        ? String.format(Locale.ROOT, " %16.2f", r.averageWait(row))
                        : String.format(Locale.ROOT, " %16s", "-"));
            }
            sb.append('\n');
        }
        sb.append(String.format(Locale.ROOT, "%n%-38s%n", "Utilization (%)"));
        for (ResourceType t : ResourceType.values()) {
            sb.append(String.format(Locale.ROOT, "%-38s", t.label()));
            for (ExperimentResult r : results) sb.append(String.format(Locale.ROOT, " %16.2f", r.averageUtilization(t)));
            sb.append('\n');
        }
        return sb.toString();
    }
}
