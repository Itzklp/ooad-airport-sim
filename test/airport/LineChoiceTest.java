package airport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import sim.Simulator;

/** Phase 2: the two line rules. Covers business rule 3 (the line is chosen by the scenario's rule). */
class LineChoiceTest {

    private final Simulator sim = new Simulator(1);

    /** A line with no service points, holding the given number of passengers. */
    private WaitingLine lineOf(String name, int length) {
        WaitingLine line = new WaitingLine(name, name);
        for (int i = 0; i < length; i++) line.join(Fixtures.passenger(i), sim);
        return line;
    }

    @Test
    void shortestLinePicksTheShorterLine() {
        WaitingLine a = lineOf("A", 3);
        WaitingLine b = lineOf("B", 1);
        assertSame(b, new ShortestLine().choose(List.of(a, b), new Random(1)));
    }

    @Test
    void shortestLineGivesATieToTheFirstLine() {
        WaitingLine a = lineOf("A", 2);
        WaitingLine b = lineOf("B", 2);
        assertSame(a, new ShortestLine().choose(List.of(a, b), new Random(1)));
        WaitingLine c = lineOf("C", 4);
        WaitingLine d = lineOf("D", 1);
        WaitingLine e = lineOf("E", 1);
        assertSame(d, new ShortestLine().choose(List.of(c, d, e), new Random(1)), "first of the shortest");
    }

    @Test
    void randomSplitWithTheSameSeedIsRepeatable() {
        List<WaitingLine> lines = List.of(lineOf("A", 0), lineOf("B", 0), lineOf("C", 0));
        RandomSplit rule = new RandomSplit();
        Random r1 = new Random(42);
        Random r2 = new Random(42);
        List<WaitingLine> first = new ArrayList<>();
        List<WaitingLine> second = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            first.add(rule.choose(lines, r1));
            second.add(rule.choose(lines, r2));
        }
        assertEquals(first, second);
    }

    @Test
    void randomSplitIsRoughlyEven() {
        WaitingLine a = lineOf("A", 0);
        WaitingLine b = lineOf("B", 0);
        Random rng = new Random(7);
        int toA = 0;
        for (int i = 0; i < 10_000; i++) {
            if (new RandomSplit().choose(List.of(a, b), rng) == a) toA++;
        }
        // 10,000 fair picks: the count for one line has a standard deviation of 50
        assertTrue(toA > 4800 && toA < 5200, "line A got " + toA);
    }

    @Test
    void randomSplitIgnoresLineLength() {
        WaitingLine longLine = lineOf("Long", 50);
        WaitingLine empty = lineOf("Empty", 0);
        Random rng = new Random(3);
        int toLong = 0;
        for (int i = 0; i < 1000; i++) {
            if (new RandomSplit().choose(List.of(longLine, empty), rng) == longLine) toLong++;
        }
        assertTrue(toLong > 400, "the long line still got " + toLong + " of 1000");
    }
}
