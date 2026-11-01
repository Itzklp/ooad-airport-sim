package airport;

/** A departing passenger moving through check-in and security. */
public class Passenger {

    private final int id;
    private final CarrierGroup group;
    private final double arrivalTime;
    private final boolean usesKiosk;
    private double joinedLineAt;
    private PassengerState state = PassengerState.ARRIVED;

    public Passenger(int id, CarrierGroup group, double arrivalTime, boolean usesKiosk) {
        this.id = id;
        this.group = group;
        this.arrivalTime = arrivalTime;
        this.usesKiosk = usesKiosk;
    }

    /** Moves to the next state; refuses any move that the state diagram does not allow. */
    public void changeState(PassengerState next) {
        if (!state.canMoveTo(next)) {
            throw new IllegalStateException("Passenger " + id + " cannot go from " + state + " to " + next);
        }
        state = next;
    }

    /** Remembers when the passenger joined a line, so the wait can be measured later. */
    public void startWaiting(double time) {
        joinedLineAt = time;
    }

    /** How long the passenger has waited in the current line. */
    public double waitTime(double now) {
        return now - joinedLineAt;
    }

    public int id() {
        return id;
    }

    public CarrierGroup group() {
        return group;
    }

    public double arrivalTime() {
        return arrivalTime;
    }

    public boolean usesKiosk() {
        return usesKiosk;
    }

    public PassengerState state() {
        return state;
    }
}
