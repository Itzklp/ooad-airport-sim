package airport;

import sim.Distribution;
import sim.Simulator;

/** A self check-in kiosk or a manned counter. After check-in the passenger goes to security. */
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
    protected void sendOn(Passenger p, Simulator sim) {
        security.admit(p, sim);
    }
}
