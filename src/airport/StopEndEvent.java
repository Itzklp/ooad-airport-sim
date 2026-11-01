package airport;

import sim.Event;
import sim.Simulator;

/** A passenger who was stopped at security finishes the extra check and leaves. */
public class StopEndEvent extends Event {

    private final Passenger passenger;

    public StopEndEvent(double time, Passenger passenger) {
        super(time);
        this.passenger = passenger;
    }

    @Override
    public void execute(Simulator sim) {
        passenger.changeState(PassengerState.DEPARTED);
    }
}
