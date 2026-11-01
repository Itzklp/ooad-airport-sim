package airport;

import java.util.List;

import sim.Distribution;
import sim.Simulator;

/** The screening lanes and their lines, shared by all airlines, plus the random stops. */
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

    /** Puts a passenger who has finished check-in in one of the screening lines. */
    public void admit(Passenger p, Simulator sim) {
        WaitingLine line = lineChoice.choose(lines, sim.rng());
        p.changeState(PassengerState.WAITING_FOR_SCREENING);
        line.join(p, sim);
    }

    /** After screening, stops a set share of passengers for an extra check; the rest leave. */
    public void afterScreening(Passenger p, Simulator sim) {
        if (sim.rng().nextDouble() < stopShare) {
            p.changeState(PassengerState.STOPPED);
            double duration = Math.max(0.0, stopTime.sample(sim.rng()));
            sim.schedule(new StopEndEvent(sim.now() + duration, p));
        } else {
            p.changeState(PassengerState.DEPARTED);
        }
    }
}
