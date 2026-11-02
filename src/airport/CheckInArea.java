package airport;

import java.util.List;

import sim.Simulator;

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

    public void admit(Passenger passenger, Simulator sim) {
        List<WaitingLine> choices = passenger.usesKiosk() ? kioskLines : counterLines;
        WaitingLine chosen = lineChoice.choose(choices, sim.rng());
        passenger.changeState(PassengerState.WAITING_FOR_CHECK_IN);
        chosen.join(passenger, sim);
    }

    public CarrierGroup group() {
        return group;
    }
}
