package airport;

import static airport.PassengerState.ARRIVED;
import static airport.PassengerState.BEING_SCREENED;
import static airport.PassengerState.CHECKING_IN;
import static airport.PassengerState.DEPARTED;
import static airport.PassengerState.STOPPED;
import static airport.PassengerState.WAITING_FOR_CHECK_IN;
import static airport.PassengerState.WAITING_FOR_SCREENING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Phase 2: the passenger state diagram, Figure 8(a) of D1. */
class PassengerStateTest {

    /** Every arrow of Figure 8(a), as {from, to}. */
    private static final List<PassengerState[]> ARROWS = List.of(
            new PassengerState[] {ARRIVED, WAITING_FOR_CHECK_IN},
            new PassengerState[] {WAITING_FOR_CHECK_IN, CHECKING_IN},
            new PassengerState[] {CHECKING_IN, WAITING_FOR_SCREENING},
            new PassengerState[] {WAITING_FOR_SCREENING, BEING_SCREENED},
            new PassengerState[] {BEING_SCREENED, STOPPED},
            new PassengerState[] {BEING_SCREENED, DEPARTED},
            new PassengerState[] {STOPPED, DEPARTED});

    @Test
    void everyArrowOfTheDiagramIsAllowed() {
        for (PassengerState[] a : ARROWS) assertTrue(a[0].canMoveTo(a[1]), a[0] + " -> " + a[1]);
    }

    @Test
    void everyMoveThatIsNotAnArrowIsRefused() {
        for (PassengerState from : PassengerState.values()) {
            for (PassengerState to : PassengerState.values()) {
                boolean isArrow = ARROWS.stream().anyMatch(a -> a[0] == from && a[1] == to);
                assertEquals(isArrow, from.canMoveTo(to), from + " -> " + to);
            }
        }
    }

    @Test
    void aNewPassengerHasArrived() {
        assertEquals(ARRIVED, Fixtures.passenger(1).state());
    }

    @Test
    void screeningBeforeCheckInIsRefused() {
        Passenger p = Fixtures.passenger(1);
        assertThrows(IllegalStateException.class, () -> p.changeState(BEING_SCREENED));
        assertEquals(ARRIVED, p.state(), "a refused move leaves the state unchanged");
    }

    @Test
    void aMoveOutOfDepartedIsRefused() {
        Passenger p = Fixtures.checkedIn(1);
        p.changeState(WAITING_FOR_SCREENING);
        p.changeState(BEING_SCREENED);
        p.changeState(DEPARTED);
        for (PassengerState next : PassengerState.values()) {
            assertFalse(DEPARTED.canMoveTo(next));
            assertThrows(IllegalStateException.class, () -> p.changeState(next));
        }
    }

    @Test
    void theFullPathThroughAStopIsAllowed() {
        Passenger p = Fixtures.passenger(1);
        for (PassengerState s : List.of(WAITING_FOR_CHECK_IN, CHECKING_IN, WAITING_FOR_SCREENING,
                BEING_SCREENED, STOPPED, DEPARTED)) {
            p.changeState(s);
        }
        assertEquals(DEPARTED, p.state());
    }

    @Test
    void waitTimeIsMeasuredFromJoiningTheLine() {
        Passenger p = Fixtures.passenger(1);
        p.startWaiting(100.0);
        assertEquals(35.0, p.waitTime(135.0), 1e-9);
    }
}
