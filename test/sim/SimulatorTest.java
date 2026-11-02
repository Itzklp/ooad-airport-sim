package sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class SimulatorTest {

    private static class Note extends Event {
        private final String label;
        private final StringBuilder log;

        Note(double time, String label, StringBuilder log) {
            super(time);
            this.label = label;
            this.log = log;
        }

        @Override
        public void execute(Simulator sim) {
            log.append(label);
        }
    }

    private static class Tick extends Event {
        private final List<Double> seenTimes;

        Tick(double time, List<Double> seenTimes) {
            super(time);
            this.seenTimes = seenTimes;
        }

        @Override
        public void execute(Simulator sim) {
            seenTimes.add(sim.now());
            sim.schedule(new Tick(sim.now() + 1.0, seenTimes));
        }
    }

    @Test
    void eventsRunInTimeOrder() {
        Simulator sim = new Simulator(1);
        StringBuilder log = new StringBuilder();
        sim.schedule(new Note(3.0, "C", log));
        sim.schedule(new Note(1.0, "A", log));
        sim.schedule(new Note(2.0, "B", log));
        sim.run(10.0);
        assertEquals("ABC", log.toString());
    }

    @Test
    void equalTimesRunInSchedulingOrder() {
        Simulator sim = new Simulator(1);
        StringBuilder log = new StringBuilder();
        for (String label : new String[] {"D", "B", "C", "A"}) {
            sim.schedule(new Note(5.0, label, log));
        }
        sim.run(10.0);
        assertEquals("DBCA", log.toString());
    }

    @Test
    void clockMovesToEachEventTime() {
        Simulator sim = new Simulator(1);
        List<Double> seenTimes = new ArrayList<>();
        sim.schedule(new Tick(0.5, seenTimes));
        sim.run(3.0);
        assertEquals(List.of(0.5, 1.5, 2.5), seenTimes);
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
        StringBuilder log = new StringBuilder();
        sim.schedule(new Note(4.0, "now", log));
        sim.run(4.0);
        assertEquals("now", log.toString());
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
        StringBuilder log = new StringBuilder();
        sim.schedule(new Note(5.0, "A", log));
        sim.schedule(new Note(15.0, "B", log));
        sim.run(10.0);
        assertEquals("A", log.toString());
        sim.run(20.0);
        assertEquals("AB", log.toString());
    }

    @Test
    void eventAtExactlyEndTimeIsRun() {
        Simulator sim = new Simulator(1);
        StringBuilder log = new StringBuilder();
        sim.schedule(new Note(10.0, "A", log));
        sim.run(10.0);
        assertEquals("A", log.toString());
    }

    @Test
    void sameSeedGivesSameRandomNumbers() {
        Simulator first = new Simulator(2026);
        Simulator second = new Simulator(2026);
        for (int i = 0; i < 100; i++) assertEquals(first.rng().nextDouble(), second.rng().nextDouble());
    }
}
