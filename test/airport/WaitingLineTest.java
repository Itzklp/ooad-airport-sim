package airport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sim.Simulator;

/** Phase 2: waiting lines. Covers business rule 2 (a passenger stays in the line they joined). */
class WaitingLineTest {

    private Simulator sim;
    private SecurityCheckpoint security;
    private Fixtures.Recorder recorder;

    @BeforeEach
    void setUp() {
        sim = new Simulator(1);
        security = Fixtures.fastSecurity();
        recorder = new Fixtures.Recorder();
    }

    private CheckInPoint counter(String name, double time, WaitingLine line) {
        CheckInPoint c = new CheckInPoint(name, ResourceType.MANNED_CHECK_IN, Fixtures.constant(time), line, security);
        c.addListener(recorder);
        return c;
    }

    private static Passenger waiting(int id) {
        Passenger p = Fixtures.passenger(id);
        p.changeState(PassengerState.WAITING_FOR_CHECK_IN);
        return p;
    }

    @Test
    void firstInFirstOut() {
        WaitingLine line = new WaitingLine("Counter line", "counters");
        counter("Counter 1", 10, line);
        for (int id = 1; id <= 4; id++) line.join(waiting(id), sim);
        sim.run(100);
        assertEquals(List.of(1, 2, 3, 4), recorder.idsServedAt("counters"));
        assertEquals(List.of(0.0, 10.0, 20.0, 30.0), recorder.waits.stream().map(Fixtures.Wait::seconds).toList());
    }

    @Test
    void aPassengerDoesNotChangeLine() {
        WaitingLine slow = new WaitingLine("Slow line", "slow");
        WaitingLine fast = new WaitingLine("Fast line", "fast");
        counter("Slow counter", 100, slow);
        CheckInPoint fastCounter = counter("Fast counter", 1, fast);
        for (int id = 1; id <= 3; id++) slow.join(waiting(id), sim);
        sim.run(50);
        assertEquals(2, slow.length(), "the two passengers behind stay in the slow line");
        assertEquals(0, fast.length());
        assertFalse(fastCounter.isBusy(), "the free counter of the other line does not take them");
    }

    @Test
    void oneLineFeedingTwoPointsServesTwoPassengersAtOnce() {
        WaitingLine line = new WaitingLine("Shared line", "shared");
        CheckInPoint a = counter("Counter 1", 10, line);
        CheckInPoint b = counter("Counter 2", 10, line);
        for (int id = 1; id <= 3; id++) line.join(waiting(id), sim);
        sim.run(5);
        assertTrue(a.isBusy());
        assertTrue(b.isBusy());
        assertEquals(1, line.length());
        sim.run(100);
        assertEquals(List.of(0.0, 0.0, 10.0), recorder.waits.stream().map(Fixtures.Wait::seconds).toList());
    }

    @Test
    void theWaitIsMeasuredFromTheTimeOfJoining() {
        WaitingLine line = new WaitingLine("Counter line", "counters");
        counter("Counter 1", 10, line);
        line.join(waiting(1), sim);
        sim.schedule(Fixtures.at(7, s -> line.join(waiting(2), s)));
        sim.run(100);
        assertEquals(3.0, recorder.waits.get(1).seconds(), 1e-9, "joined at 7, served at 10");
    }

    @Test
    void aLineWithNoPointsJustHoldsPassengers() {
        WaitingLine line = new WaitingLine("Line", "x");
        line.join(waiting(1), sim);
        line.join(waiting(2), sim);
        assertEquals(2, line.length());
        assertEquals(1, line.takeNext().id());
        assertEquals(2, line.takeNext().id());
        assertNull(line.takeNext());
    }
}
