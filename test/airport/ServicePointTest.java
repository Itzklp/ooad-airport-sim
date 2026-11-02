package airport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sim.Simulator;

class ServicePointTest {

    private Simulator sim;
    private WaitingLine line;
    private CheckInPoint counter;
    private Fixtures.Recorder recorder;

    @BeforeEach
    void setUp() {
        sim = new Simulator(1);
        line = new WaitingLine("Counter line", "counters");
        counter = new CheckInPoint("Counter 1", ResourceType.MANNED_CHECK_IN, Fixtures.constant(10),
                line, Fixtures.fastSecurity());
        recorder = new Fixtures.Recorder();
        counter.addListener(recorder);
    }

    private Passenger join(int id) {
        Passenger passenger = Fixtures.passenger(id);
        passenger.changeState(PassengerState.WAITING_FOR_CHECK_IN);
        line.join(passenger, sim);
        return passenger;
    }

    @Test
    void servesOnePassengerAtATime() {
        Passenger first = join(1);
        Passenger second = join(2);
        sim.run(5);
        assertEquals(PassengerState.CHECKING_IN, first.state());
        assertEquals(PassengerState.WAITING_FOR_CHECK_IN, second.state());
        counter.startNext(sim);
        assertEquals(PassengerState.WAITING_FOR_CHECK_IN, second.state(), "a busy point does not start another service");
        assertEquals(1, line.length());
    }

    @Test
    void idleThenBusyThenIdle() {
        assertFalse(counter.isBusy(), "starts Idle");
        join(1);
        assertTrue(counter.isBusy(), "Idle -> Busy when a passenger is assigned");
        sim.run(15);
        assertFalse(counter.isBusy(), "Busy -> Idle when service is done and the line is empty");
    }

    @Test
    void busyStaysBusyWhenTheLineIsNotEmpty() {
        join(1);
        Passenger second = join(2);
        sim.run(10);
        assertTrue(counter.isBusy(), "Busy -> Busy: the next passenger is taken at once");
        assertEquals(PassengerState.CHECKING_IN, second.state());
        sim.run(25);
        assertFalse(counter.isBusy());
    }

    @Test
    void busyTimeAddsUp() {
        join(1);
        join(2);
        sim.run(15);
        assertEquals(15.0, counter.busyTime(15), 1e-9, "includes the service still in progress");
        sim.run(50);
        assertEquals(20.0, counter.busyTime(50), 1e-9, "two services of 10 s");
        sim.schedule(Fixtures.at(60, now -> join(3)));
        sim.run(65);
        assertEquals(25.0, counter.busyTime(65), 1e-9, "idle time between 20 and 60 is not counted");
    }

    @Test
    void theListenerIsToldEachWait() {
        join(1);
        join(2);
        sim.run(100);
        assertEquals(List.of(1, 2), recorder.idsServedAt("counters"));
        assertEquals(10.0, recorder.waits.get(1).seconds(), 1e-9);
        assertEquals(PassengerState.CHECKING_IN, recorder.waits.get(1).stateAfter());
    }

    @Test
    void aCheckInPointSendsThePassengerToSecurity() {
        Passenger passenger = join(1);
        sim.run(10);
        assertEquals(PassengerState.BEING_SCREENED, passenger.state(), "the security lane was free");
        sim.run(20);
        assertEquals(PassengerState.DEPARTED, passenger.state());
    }

    @Test
    void aNegativeSampledTimeCountsAsZero() {
        WaitingLine instantLine = new WaitingLine("Line", "x");
        new CheckInPoint("Instant", ResourceType.SELF_CHECK_IN, Fixtures.constant(-5), instantLine, Fixtures.fastSecurity());
        Passenger passenger = Fixtures.passenger(1);
        passenger.changeState(PassengerState.WAITING_FOR_CHECK_IN);
        instantLine.join(passenger, sim);
        sim.run(0);
        assertEquals(PassengerState.BEING_SCREENED, passenger.state(), "service ended at time 0, not in the past");
    }

    @Test
    void theResourceTypeIsKept() {
        assertEquals(ResourceType.MANNED_CHECK_IN, counter.type());
        WaitingLine securityLine = new WaitingLine("Security", "Security");
        SecurityCheckpoint checkpoint = new SecurityCheckpoint(List.of(securityLine), new RandomSplit(), 0, Fixtures.constant(0));
        assertEquals(ResourceType.SECURITY, new ScreeningLane("Lane", Fixtures.constant(1), securityLine, checkpoint).type());
    }
}
