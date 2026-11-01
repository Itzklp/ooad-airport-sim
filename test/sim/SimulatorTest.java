package sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Phase 1: the event engine. Covers business rule 5 (equal times run in scheduling order). */
class SimulatorTest {

    /** A tiny event that writes its label, so the order of execution can be checked. */
    private static class Note extends Event {
        private final String label;
        private final StringBuilder out;

        Note(double time, String label, StringBuilder out) {
            super(time);
            this.label = label;
            this.out = out;
        }

        @Override
        public void execute(Simulator sim) {
            out.append(label);
        }
    }

    /** An event that schedules itself again every second, to show events can create events. */
    private static class Tick extends Event {
        private final List<Double> seen;

        Tick(double time, List<Double> seen) {
            super(time);
            this.seen = seen;
        }

        @Override
        public void execute(Simulator sim) {
            seen.add(sim.now());
            sim.schedule(new Tick(sim.now() + 1.0, seen));
        }
    }

    @Test
    void eventsRunInTimeOrder() {
        Simulator sim = new Simulator(1);
        StringBuilder order = new StringBuilder();
        sim.schedule(new Note(3.0, "C", order));
        sim.schedule(new Note(1.0, "A", order));
        sim.schedule(new Note(2.0, "B", order));
        sim.run(10.0);
        assertEquals("ABC", order.toString());
    }

    @Test
    void equalTimesRunInSchedulingOrder() {
        Simulator sim = new Simulator(1);
        StringBuilder order = new StringBuilder();
        for (String label : new String[] {"D", "B", "C", "A"}) {
            sim.schedule(new Note(5.0, label, order));
        }
        sim.run(10.0);
        assertEquals("DBCA", order.toString());
    }

    @Test
    void clockMovesToEachEventTime() {
        Simulator sim = new Simulator(1);
        List<Double> seen = new ArrayList<>();
        sim.schedule(new Tick(0.5, seen));
        sim.run(3.0);
        assertEquals(List.of(0.5, 1.5, 2.5), seen);
    }

    @Test
    void eventInThePastIsRefused() {
        Simulator sim = new Simulator(1);
        sim.schedule(new Note(5.0, "x", new StringBuilder()));
        sim.run(6.0);
        assertThrows(IllegalArgumentException.class, () -> sim.schedule(new Note(3.0, "past", new StringBuilder())));
    }

    @Test
    void eventAtTheCurrentTimeIsAllowed() {
        Simulator sim = new Simulator(1);
        sim.run(4.0);
        StringBuilder order = new StringBuilder();
        sim.schedule(new Note(4.0, "now", order));
        sim.run(4.0);
        assertEquals("now", order.toString());
    }

    @Test
    void clockEndsAtEndTime() {
        Simulator sim = new Simulator(1);
        sim.schedule(new Note(2.0, "x", new StringBuilder()));
        sim.run(10.0);
        assertEquals(10.0, sim.now());
    }

    @Test
    void eventsAfterEndTimeAreNotRunUntilTheNextRun() {
        Simulator sim = new Simulator(1);
        StringBuilder order = new StringBuilder();
        sim.schedule(new Note(5.0, "A", order));
        sim.schedule(new Note(15.0, "B", order));
        sim.run(10.0);
        assertEquals("A", order.toString());
        sim.run(20.0);
        assertEquals("AB", order.toString());
    }

    @Test
    void eventAtExactlyEndTimeIsRun() {
        Simulator sim = new Simulator(1);
        StringBuilder order = new StringBuilder();
        sim.schedule(new Note(10.0, "A", order));
        sim.run(10.0);
        assertEquals("A", order.toString());
    }

    @Test
    void sameSeedGivesSameRandomNumbers() {
        Simulator a = new Simulator(2026);
        Simulator b = new Simulator(2026);
        for (int i = 0; i < 100; i++) assertEquals(a.rng().nextDouble(), b.rng().nextDouble());
    }
}
