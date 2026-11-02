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

class FlowTest {

    private static final String COUNTER_LINE = "Test manned check-in";
    private static final String KIOSK_LINE = "Test self check-in";
    private static final String SECURITY_LINE = "Security 1";

    private static Airport runFor(double hours, double kioskShare, double stopShare, double stopSeconds,
                                  Fixtures.Recorder recorder, long seed) {
        Simulator sim = new Simulator(seed);
        Airport airport = Fixtures.airport(new ExponentialDist(60), kioskShare, stopShare,
                Fixtures.constant(stopSeconds), recorder);
        sim.schedule(new ArrivalEvent(airport.source().nextArrivalTime(sim), airport));
        sim.run(hours * 3600);
        return airport;
    }

    @Test
    void onePassengerGoesThroughEveryStateAtTheExpectedTimes() {
        Simulator sim = new Simulator(1);
        Airport airport = Fixtures.airport(Fixtures.constant(1e9), 0, 1.0, Fixtures.constant(50), new Fixtures.Recorder());
        Passenger passenger = airport.source().next(sim);
        airport.checkInFor(passenger.group()).admit(passenger, sim);

        sim.run(10);
        assertEquals(PassengerState.CHECKING_IN, passenger.state(), "counter from 0 to 30");
        sim.run(40);
        assertEquals(PassengerState.BEING_SCREENED, passenger.state(), "screening from 30 to 50");
        sim.run(60);
        assertEquals(PassengerState.STOPPED, passenger.state(), "stop from 50 to 100");
        sim.run(120);
        assertEquals(PassengerState.DEPARTED, passenger.state());
    }

    @Test
    void securityRefusesAPassengerWhoHasNotCheckedIn() {
        Airport airport = Fixtures.airport(Fixtures.constant(1e9), 0, 0, Fixtures.constant(0), new Fixtures.Recorder());
        Passenger passenger = Fixtures.passenger(1);
        assertThrows(IllegalStateException.class, () -> airport.security().admit(passenger, new Simulator(1)));
    }

    @Test
    void everyPassengerReachesSecurityOnlyAfterCheckIn() {
        Fixtures.Recorder recorder = new Fixtures.Recorder();
        runFor(24, 0.5, 0.1, 120, recorder, 3);
        Map<Integer, Integer> stepReached = new HashMap<>();   // 1 after check-in, 2 after security
        for (Fixtures.Wait wait : recorder.waits) {
            int before = stepReached.getOrDefault(wait.passengerId(), 0);
            if (wait.category().equals(SECURITY_LINE)) {
                assertEquals(1, before, "passenger " + wait.passengerId() + " reached security before check-in");
                stepReached.put(wait.passengerId(), 2);
            } else {
                assertEquals(0, before, "passenger " + wait.passengerId() + " checked in twice");
                stepReached.put(wait.passengerId(), 1);
            }
        }
        assertTrue(stepReached.size() > 1000, "about 1440 passengers in 24 h, got " + stepReached.size());
    }

    @Test
    void withStopShareOneEveryScreenedPassengerIsStopped() {
        Fixtures.Recorder recorder = new Fixtures.Recorder();
        runFor(10, 0.5, 1.0, 1e9, recorder, 4);   // stops outlast the run, so nobody departs
        long screened = recorder.waits.stream().filter(wait -> wait.category().equals(SECURITY_LINE)).count();
        long stopped = countIn(recorder, PassengerState.STOPPED);
        assertTrue(screened > 500);
        assertTrue(stopped >= screened - 1, "only the passenger being screened at the end may be missing");
        assertEquals(0, countIn(recorder, PassengerState.DEPARTED));
    }

    @Test
    void withStopShareZeroNobodyIsStopped() {
        Fixtures.Recorder recorder = new Fixtures.Recorder();
        runFor(10, 0.5, 0.0, 1e9, recorder, 5);
        assertEquals(0, countIn(recorder, PassengerState.STOPPED));
        assertTrue(countIn(recorder, PassengerState.DEPARTED) > 500);
    }

    @Test
    void aStopShareBetweenStopsAboutThatShare() {
        Fixtures.Recorder recorder = new Fixtures.Recorder();
        runFor(48, 0.5, 0.3, 1e9, recorder, 6);
        double stopped = countIn(recorder, PassengerState.STOPPED);
        double screened = stopped + countIn(recorder, PassengerState.DEPARTED);
        assertEquals(0.3, stopped / screened, 0.03, stopped + " stopped of " + screened);
    }

    @Test
    void kioskUsersGoToKioskLinesAndOthersToCounterLines() {
        Fixtures.Recorder allUseKiosks = new Fixtures.Recorder();
        runFor(5, 1.0, 0, 0, allUseKiosks, 7);
        assertTrue(allUseKiosks.idsServedAt(COUNTER_LINE).isEmpty());
        assertTrue(allUseKiosks.idsServedAt(KIOSK_LINE).size() > 200);

        Fixtures.Recorder noneUseKiosks = new Fixtures.Recorder();
        runFor(5, 0.0, 0, 0, noneUseKiosks, 7);
        assertTrue(noneUseKiosks.idsServedAt(KIOSK_LINE).isEmpty());
        assertTrue(noneUseKiosks.idsServedAt(COUNTER_LINE).size() > 200);
    }

    @Test
    void anArrivalAdmitsAPassengerAndSchedulesTheNextOne() {
        Simulator sim = new Simulator(1);
        Fixtures.Recorder recorder = new Fixtures.Recorder();
        Airport airport = Fixtures.airport(Fixtures.constant(100), 0, 0, Fixtures.constant(0), recorder);
        sim.schedule(new ArrivalEvent(0, airport));
        sim.run(450);
        assertEquals(List.of(0.0, 100.0, 200.0, 300.0, 400.0),
                recorder.arrived.stream().map(Passenger::arrivalTime).toList());
    }

    private static long countIn(Fixtures.Recorder recorder, PassengerState state) {
        return recorder.arrived.stream().filter(passenger -> passenger.state() == state).count();
    }
}
