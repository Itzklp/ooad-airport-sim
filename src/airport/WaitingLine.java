package airport;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import sim.Simulator;

public class WaitingLine {

    private final String name;
    private final String category;   // lines with the same category are reported together, e.g. "Southwest manned check-in"
    private final Deque<Passenger> queue = new ArrayDeque<>();
    private final List<ServicePoint> points = new ArrayList<>();

    public WaitingLine(String name, String category) {
        this.name = name;
        this.category = category;
    }

    void addPoint(ServicePoint point) {
        points.add(point);
    }

    public void join(Passenger passenger, Simulator sim) {
        passenger.startWaiting(sim.now());
        queue.addLast(passenger);
        dispatch(sim);
    }

    public void dispatch(Simulator sim) {
        for (ServicePoint point : points) {
            if (queue.isEmpty()) return;
            if (!point.isBusy()) point.startNext(sim);
        }
    }

    public Passenger takeNext() {
        return queue.pollFirst();
    }

    public int length() {
        return queue.size();
    }

    public String name() {
        return name;
    }

    public String category() {
        return category;
    }
}
