package airport;

import java.util.List;
import java.util.Map;

public class Airport {

    private final String name;
    private final List<CarrierGroup> groups;
    private final PassengerSource source;
    private final Map<CarrierGroup, CheckInArea> areas;
    private final SecurityCheckpoint security;
    private final List<WaitingLine> lines;
    private final List<ServicePoint> points;

    public Airport(String name, List<CarrierGroup> groups, PassengerSource source,
                   Map<CarrierGroup, CheckInArea> areas, SecurityCheckpoint security,
                   List<WaitingLine> lines, List<ServicePoint> points) {
        this.name = name;
        this.groups = List.copyOf(groups);
        this.source = source;
        this.areas = Map.copyOf(areas);
        this.security = security;
        this.lines = List.copyOf(lines);
        this.points = List.copyOf(points);
    }

    public CheckInArea checkInFor(CarrierGroup group) {
        return areas.get(group);
    }

    public PassengerSource source() {
        return source;
    }

    public SecurityCheckpoint security() {
        return security;
    }

    public List<CarrierGroup> groups() {
        return groups;
    }

    public List<WaitingLine> lines() {
        return lines;
    }

    public List<ServicePoint> servicePoints() {
        return points;
    }

    public String name() {
        return name;
    }
}
