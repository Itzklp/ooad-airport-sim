package airport;

import sim.Event;
import sim.Simulator;

public class ArrivalEvent extends Event {

    private final Airport airport;

    public ArrivalEvent(double time, Airport airport) {
        super(time);
        this.airport = airport;
    }

    @Override
    public void execute(Simulator sim) {
        Passenger passenger = airport.source().next(sim);
        airport.checkInFor(passenger.group()).admit(passenger, sim);
        sim.schedule(new ArrivalEvent(airport.source().nextArrivalTime(sim), airport));
    }
}
