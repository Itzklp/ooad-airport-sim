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

class PassengerStateTest {

    // every arrow of the passenger state diagram, as {from, to}
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
        for (PassengerState[] arrow : ARROWS) assertTrue(arrow[0].canMoveTo(arrow[1]), arrow[0] + " -> " + arrow[1]);
    }

    @Test
    void everyMoveThatIsNotAnArrowIsRefused() {
        for (PassengerState from : PassengerState.values()) {
            for (PassengerState to : PassengerState.values()) {
                boolean isArrow = ARROWS.stream().anyMatch(arrow -> arrow[0] == from && arrow[1] == to);
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
        Passenger passenger = Fixtures.passenger(1);
        assertThrows(IllegalStateException.class, () -> passenger.changeState(BEING_SCREENED));
        assertEquals(ARRIVED, passenger.state(), "a refused move leaves the state unchanged");
    }

    @Test
    void aMoveOutOfDepartedIsRefused() {
        Passenger passenger = Fixtures.checkedIn(1);
        passenger.changeState(WAITING_FOR_SCREENING);
        passenger.changeState(BEING_SCREENED);
        passenger.changeState(DEPARTED);
        for (PassengerState next : PassengerState.values()) {
            assertFalse(DEPARTED.canMoveTo(next));
            assertThrows(IllegalStateException.class, () -> passenger.changeState(next));
        }
    }

    @Test
    void theFullPathThroughAStopIsAllowed() {
        Passenger passenger = Fixtures.passenger(1);
        for (PassengerState next : List.of(WAITING_FOR_CHECK_IN, CHECKING_IN, WAITING_FOR_SCREENING,
                BEING_SCREENED, STOPPED, DEPARTED)) {
            passenger.changeState(next);
        }
        assertEquals(DEPARTED, passenger.state());
    }

    @Test
    void waitTimeIsMeasuredFromJoiningTheLine() {
        Passenger passenger = Fixtures.passenger(1);
        passenger.startWaiting(100.0);
        assertEquals(35.0, passenger.waitTime(135.0), 1e-9);
    }
}
