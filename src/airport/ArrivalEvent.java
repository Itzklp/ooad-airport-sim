package airport;

import sim.Event;
import sim.Simulator;

/** A passenger arrives: send them to check-in and schedule the next arrival. */
public class ArrivalEvent extends Event {

    private final Airport airport;

    public ArrivalEvent(double time, Airport airport) {
        super(time);
        this.airport = airport;
    }

    @Override
    public void execute(Simulator sim) {
        Passenger p = airport.source().next(sim);
        airport.checkInFor(p.group()).admit(p, sim);
        sim.schedule(new ArrivalEvent(airport.source().nextArrivalTime(sim), airport));
    }
}
