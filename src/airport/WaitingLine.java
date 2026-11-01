package airport;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import sim.Simulator;

/**
 * A first-come, first-served line that feeds one or more service points.
 * The category groups lines for reports, for example "Southwest manned check-in",
 * so a scenario with two lines can be compared with a scenario with one.
 */
public class WaitingLine {

    private final String name;
    private final String category;
    private final Deque<Passenger> queue = new ArrayDeque<>();
    private final List<ServicePoint> points = new ArrayList<>();

    public WaitingLine(String name, String category) {
        this.name = name;
        this.category = category;
    }

    /** Called by a ServicePoint when it is created, to link it to the line that feeds it. */
    void addPoint(ServicePoint point) {
        points.add(point);
    }

    /** Puts the passenger at the back of the line and starts service if a point is free. */
    public void join(Passenger p, Simulator sim) {
        p.startWaiting(sim.now());
        queue.addLast(p);
        dispatch(sim);
    }

    /** Gives waiting passengers to any free points fed by this line. */
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
