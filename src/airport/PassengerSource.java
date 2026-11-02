package airport;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sim.Distribution;
import sim.Simulator;

public class PassengerSource {

    private final Distribution interarrival;
    private final List<CarrierGroup> groups;
    private final List<SimListener> listeners = new ArrayList<>();
    private int nextId = 0;

    public PassengerSource(Distribution interarrival, List<CarrierGroup> groups) {
        this.interarrival = interarrival;
        this.groups = List.copyOf(groups);
    }

    public void addListener(SimListener listener) {
        listeners.add(listener);
    }

    public Passenger next(Simulator sim) {
        Random random = sim.rng();
        CarrierGroup group = pickGroup(random.nextDouble());
        boolean usesKiosk = random.nextDouble() < group.kioskShare();
        Passenger passenger = new Passenger(++nextId, group, sim.now(), usesKiosk);
        for (SimListener listener : listeners) listener.passengerArrived(passenger);
        return passenger;
    }

    // The paper's arrival formula starts at -0.001, so a gap below zero is taken as zero.
    public double nextArrivalTime(Simulator sim) {
        return sim.now() + Math.max(0.0, interarrival.sample(sim.rng()));
    }

    private CarrierGroup pickGroup(double draw) {
        double runningShare = 0.0;
        for (CarrierGroup group : groups) {
            runningShare += group.share();
            if (draw < runningShare) return group;
        }
        return groups.get(groups.size() - 1);
    }
}
