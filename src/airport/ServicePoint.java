package airport;

import java.util.ArrayList;
import java.util.List;

import sim.Distribution;
import sim.Simulator;

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

    public void addListener(SimListener listener) {
        listeners.add(listener);
    }

    public final void startNext(Simulator sim) {
        if (current != null) return;
        Passenger passenger = line.takeNext();
        if (passenger == null) return;
        current = passenger;
        busySince = sim.now();
        passenger.changeState(servingState());
        double wait = passenger.waitTime(sim.now());
        for (SimListener listener : listeners) listener.waitEnded(passenger, line, wait);
        double duration = Math.max(0.0, serviceTime.sample(sim.rng()));
        sim.schedule(new ServiceEndEvent(sim.now() + duration, this));
    }

    public final void finishService(Simulator sim) {
        Passenger finished = current;
        current = null;
        busyTime += sim.now() - busySince;
        sendOn(finished, sim);
        line.dispatch(sim);
    }

    protected abstract PassengerState servingState();

    protected abstract void sendOn(Passenger passenger, Simulator sim);

    public boolean isBusy() {
        return current != null;
    }

    // includes a service still in progress
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
