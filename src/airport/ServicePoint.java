package airport;

import java.util.ArrayList;
import java.util.List;

import sim.Distribution;
import sim.Simulator;

/**
 * Anything that serves one passenger at a time from one waiting line:
 * a kiosk, a counter or a screening lane.
 * startNext() and finishService() fix the steps of a service; subclasses fill in
 * servingState() and sendOn(). (Template Method pattern)
 */
public abstract class ServicePoint {

    private final String name;
    private final ResourceType type;
    private final Distribution serviceTime;
    private final WaitingLine line;
    private final List<SimListener> listeners = new ArrayList<>();
    private Passenger current;
    private double busySince;
    private double busyTime;

    @SuppressWarnings("this-escape")   // the line only stores the reference; it does not call the point yet
    protected ServicePoint(String name, ResourceType type, Distribution serviceTime, WaitingLine line) {
        this.name = name;
        this.type = type;
        this.serviceTime = serviceTime;
        this.line = line;
        line.addPoint(this);
    }

    public void addListener(SimListener l) {
        listeners.add(l);
    }

    /** Takes the next passenger from the line and schedules the end of the service. */
    public final void startNext(Simulator sim) {
        if (current != null) return;
        Passenger p = line.takeNext();
        if (p == null) return;
        current = p;
        busySince = sim.now();
        p.changeState(servingState());
        double wait = p.waitTime(sim.now());
        for (SimListener l : listeners) l.waitEnded(p, line, wait);
        double duration = Math.max(0.0, serviceTime.sample(sim.rng()));
        sim.schedule(new ServiceEndEvent(sim.now() + duration, this));
    }

    /** Ends the current service, sends the passenger on, and serves the next one in the line. */
    public final void finishService(Simulator sim) {
        Passenger p = current;
        current = null;
        busyTime += sim.now() - busySince;
        sendOn(p, sim);
        line.dispatch(sim);
    }

    /** State the passenger is in while being served here. */
    protected abstract PassengerState servingState();

    /** Where the passenger goes after the service. */
    protected abstract void sendOn(Passenger p, Simulator sim);

    public boolean isBusy() {
        return current != null;
    }

    /** Total busy time up to now, including a service still in progress. */
    public double busyTime(double now) {
        return busyTime + (current != null ? now - busySince : 0.0);
    }

    public String name() {
        return name;
    }

    public ResourceType type() {
        return type;
    }
}
