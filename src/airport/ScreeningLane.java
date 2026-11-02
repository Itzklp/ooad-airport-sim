package airport;

import sim.Distribution;
import sim.Simulator;

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
    protected void sendOn(Passenger passenger, Simulator sim) {
        checkpoint.afterScreening(passenger, sim);
    }
}
