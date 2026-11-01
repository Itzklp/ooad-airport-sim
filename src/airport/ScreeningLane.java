package airport;

import sim.Distribution;
import sim.Simulator;

/** A carry-on bag screening lane. After screening the checkpoint decides whether to stop the passenger. */
public class ScreeningLane extends ServicePoint {

    private final SecurityCheckpoint checkpoint;

    public ScreeningLane(String name, Distribution serviceTime, WaitingLine line, SecurityCheckpoint checkpoint) {
        super(name, ResourceType.SECURITY, serviceTime, line);
        this.checkpoint = checkpoint;
    }

    @Override
    protected PassengerState servingState() {
        return PassengerState.BEING_SCREENED;
    }

    @Override
    protected void sendOn(Passenger p, Simulator sim) {
        checkpoint.afterScreening(p, sim);
    }
}
