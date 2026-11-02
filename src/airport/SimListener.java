package airport;

public interface SimListener {

    void passengerArrived(Passenger passenger);

    void waitEnded(Passenger passenger, WaitingLine line, double wait);
}
