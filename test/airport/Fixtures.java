package airport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import sim.Distribution;
import sim.Event;
import sim.Simulator;

/**
 * Small pieces shared by the airport tests. Service times are constants,
 * so the time at which each thing happens can be worked out by hand.
 */
final class Fixtures {

    static final double COUNTER_TIME = 30;
    static final double KIOSK_TIME = 25;
    static final double SCREEN_TIME = 20;

    private Fixtures() {
    }

    /** A "distribution" that always gives the same value. Used only in tests. */
    static Distribution constant(double value) {
        return new Distribution() {
            @Override
            public double sample(Random rng) {
                return value;
            }

            @Override
            public double mean() {
                return value;
            }
        };
    }

    /** An event that runs the given action, for placing passengers at chosen times. */
    static Event at(double time, Consumer<Simulator> action) {
        return new Event(time) {
            @Override
            public void execute(Simulator sim) {
                action.accept(sim);
            }
        };
    }

    static Passenger passenger(int id) {
        return new Passenger(id, new CarrierGroup("Test", 1, 0), 0, false);
    }

    /** A passenger who has already checked in, ready to join a security line. */
    static Passenger checkedIn(int id) {
        Passenger p = passenger(id);
        p.changeState(PassengerState.WAITING_FOR_CHECK_IN);
        p.changeState(PassengerState.CHECKING_IN);
        return p;
    }

    /** A security checkpoint with one fast lane and no stops, for tests that only look at check-in. */
    static SecurityCheckpoint fastSecurity() {
        WaitingLine line = new WaitingLine("Security 1", "Security 1");
        SecurityCheckpoint security = new SecurityCheckpoint(List.of(line), new RandomSplit(), 0, constant(0));
        new ScreeningLane("Lane 1", constant(1), line, security);
        return security;
    }

    /**
     * A whole airport with one airline, one kiosk, one counter and one screening lane.
     * Counter 30 s, kiosk 25 s, screening 20 s.
     */
    static Airport airport(Distribution arrival, double kioskShare, double stopShare,
                           Distribution stopTime, SimListener listener) {
        CarrierGroup group = new CarrierGroup("Test", 1, kioskShare);
        PassengerSource source = new PassengerSource(arrival, List.of(group));
        source.addListener(listener);

        WaitingLine securityLine = new WaitingLine("Security 1", "Security 1");
        SecurityCheckpoint security = new SecurityCheckpoint(List.of(securityLine), new RandomSplit(), stopShare, stopTime);
        ScreeningLane lane = new ScreeningLane("Lane 1", constant(SCREEN_TIME), securityLine, security);

        WaitingLine kioskLine = new WaitingLine("Test self check-in", "Test self check-in");
        WaitingLine counterLine = new WaitingLine("Test manned check-in", "Test manned check-in");
        CheckInPoint kiosk = new CheckInPoint("Kiosk 1", ResourceType.SELF_CHECK_IN, constant(KIOSK_TIME), kioskLine, security);
        CheckInPoint counter = new CheckInPoint("Counter 1", ResourceType.MANNED_CHECK_IN, constant(COUNTER_TIME), counterLine, security);
        for (ServicePoint sp : List.of(lane, kiosk, counter)) sp.addListener(listener);

        CheckInArea area = new CheckInArea(group, List.of(kioskLine), List.of(counterLine), new ShortestLine());
        return new Airport("Tiny airport", List.of(group), source, Map.of(group, area), security,
                List.of(kioskLine, counterLine, securityLine), List.of(lane, kiosk, counter));
    }

    /** One finished wait, as reported to a listener. */
    record Wait(int passengerId, String category, double seconds, PassengerState stateAfter) {
    }

    /** A listener that writes down everything it hears. */
    static class Recorder implements SimListener {
        final List<Passenger> arrived = new ArrayList<>();
        final List<Wait> waits = new ArrayList<>();

        @Override
        public void passengerArrived(Passenger p) {
            arrived.add(p);
        }

        @Override
        public void waitEnded(Passenger p, WaitingLine line, double wait) {
            waits.add(new Wait(p.id(), line.category(), wait, p.state()));
        }

        List<Integer> idsServedAt(String category) {
            return waits.stream().filter(w -> w.category().equals(category)).map(Wait::passengerId).toList();
        }
    }
}
