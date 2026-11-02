package results;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WaitStatsTest {

    @Test
    void emptyStatsAreAllZero() {
        WaitStats stats = new WaitStats();
        assertEquals(0, stats.count());
        assertEquals(0.0, stats.average());
        assertEquals(0.0, stats.min());
        assertEquals(0.0, stats.max());
    }

    @Test
    void addedWaitsAreSummarised() {
        WaitStats stats = new WaitStats();
        for (double wait : new double[] {30, 0, 90, 60}) stats.add(wait);
        assertEquals(4, stats.count());
        assertEquals(45.0, stats.average(), 1e-9);
        assertEquals(0.0, stats.min());
        assertEquals(90.0, stats.max());
    }
}
