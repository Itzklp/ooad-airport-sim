package airport;

import java.util.List;

import sim.Distribution;
import sim.Simulator;

public class SecurityCheckpoint {

    private final List<WaitingLine> lines;
    private final LineChoice lineChoice;
    private final double stopShare;
    private final Distribution stopTime;

    public SecurityCheckpoint(List<WaitingLine> lines, LineChoice lineChoice,
                              double stopShare, Distribution stopTime) {
        this.lines = List.copyOf(lines);
        this.lineChoice = lineChoice;
        this.stopShare = stopShare;
        this.stopTime = stopTime;
    }

    public void admit(Passenger passenger, Simulator sim) {
        WaitingLine chosen = lineChoice.choose(lines, sim.rng());
        passenger.changeState(PassengerState.WAITING_FOR_SCREENING);
        chosen.join(passenger, sim);
    }

    // A stopped passenger is checked aside, so the screening lane is already free for the next one.
    public void afterScreening(Passenger passenger, Simulator sim) {
        if (sim.rng().nextDouble() < stopShare) {
            passenger.changeState(PassengerState.STOPPED);
            double duration = Math.max(0.0, stopTime.sample(sim.rng()));
            sim.schedule(new StopEndEvent(sim.now() + duration, passenger));
        } else {
            passenger.changeState(PassengerState.DEPARTED);
        }
    }
}
