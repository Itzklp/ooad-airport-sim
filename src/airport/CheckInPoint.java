package airport;

import sim.Distribution;
import sim.Simulator;

public class CheckInPoint extends ServicePoint {

    private final SecurityCheckpoint security;

    public CheckInPoint(String name, ResourceType type, Distribution serviceTime,
                        WaitingLine line, SecurityCheckpoint security) {
        super(name, type, serviceTime, line);
        this.security = security;
    }

    @Override
    protected PassengerState servingState() {
        return PassengerState.CHECKING_IN;
    }

    @Override
    protected void sendOn(Passenger passenger, Simulator sim) {
        security.admit(passenger, sim);
    }
}
