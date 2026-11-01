package airport;

/**
 * Anything that wants to hear what happens in the airport model. (Observer pattern)
 * The interface belongs to the model, so the model never depends on the statistics code.
 */
public interface SimListener {

    void passengerArrived(Passenger p);

    void waitEnded(Passenger p, WaitingLine line, double wait);
}
