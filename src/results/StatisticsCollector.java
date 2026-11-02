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

public class StatisticsCollector implements SimListener {

    private final Map<String, WaitStats> waits = new LinkedHashMap<>();
    private final Map<String, Integer> arrivals = new LinkedHashMap<>();

    @Override
    public void passengerArrived(Passenger passenger) {
        arrivals.merge(passenger.group().name(), 1, Integer::sum);
    }

    @Override
    public void waitEnded(Passenger passenger, WaitingLine line, double wait) {
        waits.computeIfAbsent(line.category(), category -> new WaitStats()).add(wait);
    }

    public RunResult summary(Airport airport, double endTime) {
        Map<String, WaitStats> waitsByLine = new LinkedHashMap<>();
        for (WaitingLine line : airport.lines()) {
            waitsByLine.put(line.category(), waits.getOrDefault(line.category(), new WaitStats()));
        }

        Map<ResourceType, Double> busySeconds = new EnumMap<>(ResourceType.class);
        Map<ResourceType, Integer> pointCount = new EnumMap<>(ResourceType.class);
        for (ServicePoint point : airport.servicePoints()) {
            busySeconds.merge(point.type(), point.busyTime(endTime), Double::sum);
            pointCount.merge(point.type(), 1, Integer::sum);
        }
        Map<ResourceType, Double> utilization = new EnumMap<>(ResourceType.class);
        for (ResourceType type : busySeconds.keySet()) {
            utilization.put(type, busySeconds.get(type) / (pointCount.get(type) * endTime));
        }

        Map<String, Integer> arrivalsByGroup = new LinkedHashMap<>();
        for (CarrierGroup group : airport.groups()) {
            arrivalsByGroup.put(group.name(), arrivals.getOrDefault(group.name(), 0));
        }
        return new RunResult(waitsByLine, utilization, arrivalsByGroup);
    }
}
