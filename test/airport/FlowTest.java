package airport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import sim.ExponentialDist;
import sim.Simulator;

/**
 * Phase 2: whole passenger journeys through a tiny airport.
 * Covers requirements 3 to 7 and business rule 4 (security only after check-in).
 */
class FlowTest {

    private static final String COUNTER_LINE = "Test manned check-in";
    private static final String KIOSK_LINE = "Test self check-in";
    private static final String SECURITY_LINE = "Security 1";

    /** Runs the tiny airport with an arrival every 60 s on average, for the given number of hours. */
    private static Airport runFor(double hours, double kioskShare, double stopShare, double stopTime,
                                  Fixtures.Recorder recorder, long seed) {
        Simulator sim = new Simulator(seed);
        Airport airport = Fixtures.airport(new ExponentialDist(60), kioskShare, stopShare,
                Fixtures.constant(stopTime), recorder);
        sim.schedule(new ArrivalEvent(airport.source().nextArrivalTime(sim), airport));
        sim.run(hours * 3600);
        return airport;
    }

    @Test
    void onePassengerGoesThroughEveryStateAtTheExpectedTimes() {
        Simulator sim = new Simulator(1);
        Airport airport = Fixtures.airport(Fixtures.constant(1e9), 0, 1.0, Fixtures.constant(50), new Fixtures.Recorder());
        Passenger p = airport.source().next(sim);
        airport.checkInFor(p.group()).admit(p, sim);

        sim.run(10);
        assertEquals(PassengerState.CHECKING_IN, p.state(), "counter from 0 to 30");
        sim.run(40);
        assertEquals(PassengerState.BEING_SCREENED, p.state(), "screening from 30 to 50");
        sim.run(60);
        assertEquals(PassengerState.STOPPED, p.state(), "stop from 50 to 100");
        sim.run(120);
        assertEquals(PassengerState.DEPARTED, p.state());
    }

    @Test
    void securityRefusesAPassengerWhoHasNotCheckedIn() {
        Airport airport = Fixtures.airport(Fixtures.constant(1e9), 0, 0, Fixtures.constant(0), new Fixtures.Recorder());
        Passenger p = Fixtures.passenger(1);
        assertThrows(IllegalStateException.class, () -> airport.security().admit(p, new Simulator(1)));
    }

    @Test
    void everyPassengerReachesSecurityOnlyAfterCheckIn() {
        Fixtures.Recorder recorder = new Fixtures.Recorder();
        runFor(24, 0.5, 0.1, 120, recorder, 3);
        Map<Integer, Integer> step = new HashMap<>();   // passenger id -> 1 after check-in, 2 after security
        for (Fixtures.Wait w : recorder.waits) {
            int before = step.getOrDefault(w.passengerId(), 0);
            if (w.category().equals(SECURITY_LINE)) {
                assertEquals(1, before, "passenger " + w.passengerId() + " reached security before check-in");
                step.put(w.passengerId(), 2);
            } else {
                assertEquals(0, before, "passenger " + w.passengerId() + " checked in twice");
                step.put(w.passengerId(), 1);
            }
        }
        assertTrue(step.size() > 1000, "about 1440 passengers in 24 h, got " + step.size());
    }

    @Test
    void withStopShareOneEveryScreenedPassengerIsStopped() {
        Fixtures.Recorder recorder = new Fixtures.Recorder();
        runFor(10, 0.5, 1.0, 1e9, recorder, 4);   // stops last longer than the run, so nobody departs
        long screened = recorder.waits.stream().filter(w -> w.category().equals(SECURITY_LINE)).count();
        long stopped = count(recorder, PassengerState.STOPPED);
        assertTrue(screened > 500);
        assertTrue(stopped >= screened - 1, "only the passenger being screened at the end may be missing");
        assertEquals(0, count(recorder, PassengerState.DEPARTED));
    }

    @Test
    void withStopShareZeroNobodyIsStopped() {
        Fixtures.Recorder recorder = new Fixtures.Recorder();
        runFor(10, 0.5, 0.0, 1e9, recorder, 5);
        assertEquals(0, count(recorder, PassengerState.STOPPED));
        assertTrue(count(recorder, PassengerState.DEPARTED) > 500);
    }

    @Test
    void aStopShareBetweenStopsAboutThatShare() {
        Fixtures.Recorder recorder = new Fixtures.Recorder();
        runFor(48, 0.5, 0.3, 1e9, recorder, 6);
        double stopped = count(recorder, PassengerState.STOPPED);
        double done = stopped + count(recorder, PassengerState.DEPARTED);
        assertEquals(0.3, stopped / done, 0.03, stopped + " stopped of " + done);
    }

    @Test
    void kioskUsersGoToKioskLinesAndOthersToCounterLines() {
        Fixtures.Recorder allKiosk = new Fixtures.Recorder();
        runFor(5, 1.0, 0, 0, allKiosk, 7);
        assertTrue(allKiosk.idsServedAt(COUNTER_LINE).isEmpty());
        assertTrue(allKiosk.idsServedAt(KIOSK_LINE).size() > 200);

        Fixtures.Recorder noKiosk = new Fixtures.Recorder();
        runFor(5, 0.0, 0, 0, noKiosk, 7);
        assertTrue(noKiosk.idsServedAt(KIOSK_LINE).isEmpty());
        assertTrue(noKiosk.idsServedAt(COUNTER_LINE).size() > 200);
    }

    @Test
    void anArrivalAdmitsAPassengerAndSchedulesTheNextOne() {
        Simulator sim = new Simulator(1);
        Fixtures.Recorder recorder = new Fixtures.Recorder();
        Airport airport = Fixtures.airport(Fixtures.constant(100), 0, 0, Fixtures.constant(0), recorder);
        sim.schedule(new ArrivalEvent(0, airport));
        sim.run(450);
        assertEquals(5, recorder.arrived.size(), "arrivals at 0, 100, 200, 300 and 400");
        assertEquals(List.of(0.0, 100.0, 200.0, 300.0, 400.0),
                recorder.arrived.stream().map(Passenger::arrivalTime).toList());
    }

    /** Counts the passengers who finished screening and are now in the given state. */
    private static long count(Fixtures.Recorder recorder, PassengerState state) {
        return recorder.arrived.stream().filter(p -> p.state() == state).count();
    }
}
