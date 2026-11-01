package airport;

import java.util.List;

import sim.Simulator;

/** The kiosks, counters and their lines for one airline group. */
public class CheckInArea {

    private final CarrierGroup group;
    private final List<WaitingLine> kioskLines;
    private final List<WaitingLine> counterLines;
    private final LineChoice lineChoice;

    public CheckInArea(CarrierGroup group, List<WaitingLine> kioskLines,
                       List<WaitingLine> counterLines, LineChoice lineChoice) {
        this.group = group;
        this.kioskLines = List.copyOf(kioskLines);
        this.counterLines = List.copyOf(counterLines);
        this.lineChoice = lineChoice;
    }

    /** Puts a newly arrived passenger in a kiosk line or a counter line. */
    public void admit(Passenger p, Simulator sim) {
        List<WaitingLine> lines = p.usesKiosk() ? kioskLines : counterLines;
        WaitingLine line = lineChoice.choose(lines, sim.rng());
        p.changeState(PassengerState.WAITING_FOR_CHECK_IN);
        line.join(p, sim);
    }

    public CarrierGroup group() {
        return group;
    }
}
