package airport;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sim.Distribution;
import sim.Simulator;

/**
 * Creates passengers at random times, the "Entrance" of the paper's Arena model,
 * and gives each one an airline and a check-in choice. (Factory)
 */
public class PassengerSource {

    private final Distribution interarrival;
    private final List<CarrierGroup> groups;
    private final List<SimListener> listeners = new ArrayList<>();
    private int nextId = 0;

    public PassengerSource(Distribution interarrival, List<CarrierGroup> groups) {
        this.interarrival = interarrival;
        this.groups = List.copyOf(groups);
    }

    public void addListener(SimListener l) {
        listeners.add(l);
    }

    /** Creates the next passenger and tells the listeners about it. */
    public Passenger next(Simulator sim) {
        Random rng = sim.rng();
        CarrierGroup group = pickGroup(rng.nextDouble());
        boolean kiosk = rng.nextDouble() < group.kioskShare();
        Passenger p = new Passenger(++nextId, group, sim.now(), kiosk);
        for (SimListener l : listeners) l.passengerArrived(p);
        return p;
    }

    /** Time of the next arrival. A gap below zero (the paper's formula starts at -0.001) is taken as zero. */
    public double nextArrivalTime(Simulator sim) {
        return sim.now() + Math.max(0.0, interarrival.sample(sim.rng()));
    }

    private CarrierGroup pickGroup(double u) {
        double cumulative = 0.0;
        for (CarrierGroup g : groups) {
            cumulative += g.share();
            if (u < cumulative) return g;
        }
        return groups.get(groups.size() - 1);
    }
}
