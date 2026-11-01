package results;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import airport.Airport;
import airport.CarrierGroup;
import airport.Passenger;
import airport.ResourceType;
import airport.ServicePoint;
import airport.SimListener;
import airport.WaitingLine;

/** Listens to one run and collects waiting times and passenger counts. (Observer) */
public class StatisticsCollector implements SimListener {

    private final Map<String, WaitStats> waits = new LinkedHashMap<>();
    private final Map<String, Integer> arrivals = new LinkedHashMap<>();

    @Override
    public void passengerArrived(Passenger p) {
        arrivals.merge(p.group().name(), 1, Integer::sum);
    }

    @Override
    public void waitEnded(Passenger p, WaitingLine line, double wait) {
        waits.computeIfAbsent(line.category(), k -> new WaitStats()).add(wait);
    }

    /** Builds the result of the run. Utilization comes from the service points' own busy times. */
    public RunResult summary(Airport airport, double endTime) {
        Map<String, WaitStats> allWaits = new LinkedHashMap<>();
        for (WaitingLine line : airport.lines()) {
            allWaits.put(line.category(), waits.getOrDefault(line.category(), new WaitStats()));
        }

        Map<ResourceType, Double> busy = new EnumMap<>(ResourceType.class);
        Map<ResourceType, Integer> count = new EnumMap<>(ResourceType.class);
        for (ServicePoint sp : airport.servicePoints()) {
            busy.merge(sp.type(), sp.busyTime(endTime), Double::sum);
            count.merge(sp.type(), 1, Integer::sum);
        }
        Map<ResourceType, Double> utilization = new EnumMap<>(ResourceType.class);
        for (ResourceType type : busy.keySet()) {
            utilization.put(type, busy.get(type) / (count.get(type) * endTime));
        }

        Map<String, Integer> allArrivals = new LinkedHashMap<>();
        for (CarrierGroup g : airport.groups()) {
            allArrivals.put(g.name(), arrivals.getOrDefault(g.name(), 0));
        }
        return new RunResult(allWaits, utilization, allArrivals);
    }
}
