package airport;

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

    public void changeState(PassengerState next) {
        if (!state.canMoveTo(next)) {
            throw new IllegalStateException("Passenger " + id + " cannot go from " + state + " to " + next);
        }
        state = next;
    }

    public void startWaiting(double time) {
        joinedLineAt = time;
    }

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
