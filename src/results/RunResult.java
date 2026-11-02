package results;

import java.util.Map;

import airport.ResourceType;

public class RunResult {

    private final Map<String, WaitStats> waits;              // by line category, in seconds
    private final Map<ResourceType, Double> utilization;     // 0.0 to 1.0
    private final Map<String, Integer> arrivals;             // by airline group

    public RunResult(Map<String, WaitStats> waits, Map<ResourceType, Double> utilization,
                     Map<String, Integer> arrivals) {
        this.waits = waits;
        this.utilization = utilization;
        this.arrivals = arrivals;
    }

    public Map<String, WaitStats> waits() {
        return waits;
    }

    public Map<ResourceType, Double> utilization() {
        return utilization;
    }

    public Map<String, Integer> arrivals() {
        return arrivals;
    }
}
