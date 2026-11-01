package results;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Phase 3: the count, average, minimum and maximum of the waits of one line category. */
class WaitStatsTest {

    @Test
    void emptyStatsAreAllZero() {
        WaitStats w = new WaitStats();
        assertEquals(0, w.count());
        assertEquals(0.0, w.average());
        assertEquals(0.0, w.min());
        assertEquals(0.0, w.max());
    }

    @Test
    void addedWaitsAreSummarised() {
        WaitStats w = new WaitStats();
        for (double x : new double[] {30, 0, 90, 60}) w.add(x);
        assertEquals(4, w.count());
        assertEquals(45.0, w.average(), 1e-9);
        assertEquals(0.0, w.min());
        assertEquals(90.0, w.max());
    }
}
