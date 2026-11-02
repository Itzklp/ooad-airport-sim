package airport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import sim.Simulator;

class LineChoiceTest {

    private final Simulator sim = new Simulator(1);

    private WaitingLine lineWith(String name, int waitingPassengers) {
        WaitingLine line = new WaitingLine(name, name);
        for (int i = 0; i < waitingPassengers; i++) line.join(Fixtures.passenger(i), sim);
        return line;
    }

    @Test
    void shortestLinePicksTheShorterLine() {
        WaitingLine longer = lineWith("A", 3);
        WaitingLine shorter = lineWith("B", 1);
        assertSame(shorter, new ShortestLine().choose(List.of(longer, shorter), new Random(1)));
    }

    @Test
    void shortestLineGivesATieToTheFirstLine() {
        WaitingLine first = lineWith("A", 2);
        WaitingLine second = lineWith("B", 2);
        assertSame(first, new ShortestLine().choose(List.of(first, second), new Random(1)));

        WaitingLine longest = lineWith("C", 4);
        WaitingLine firstShortest = lineWith("D", 1);
        WaitingLine secondShortest = lineWith("E", 1);
        assertSame(firstShortest, new ShortestLine().choose(List.of(longest, firstShortest, secondShortest), new Random(1)));
    }

    @Test
    void randomSplitWithTheSameSeedIsRepeatable() {
        List<WaitingLine> lines = List.of(lineWith("A", 0), lineWith("B", 0), lineWith("C", 0));
        RandomSplit rule = new RandomSplit();
        Random firstRandom = new Random(42);
        Random secondRandom = new Random(42);
        List<WaitingLine> firstPicks = new ArrayList<>();
        List<WaitingLine> secondPicks = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            firstPicks.add(rule.choose(lines, firstRandom));
            secondPicks.add(rule.choose(lines, secondRandom));
        }
        assertEquals(firstPicks, secondPicks);
    }

    @Test
    void randomSplitIsRoughlyEven() {
        WaitingLine lineA = lineWith("A", 0);
        WaitingLine lineB = lineWith("B", 0);
        Random random = new Random(7);
        int sentToA = 0;
        for (int i = 0; i < 10_000; i++) {
            if (new RandomSplit().choose(List.of(lineA, lineB), random) == lineA) sentToA++;
        }
        // 10,000 fair picks: the count for one line has a standard deviation of 50
        assertTrue(sentToA > 4800 && sentToA < 5200, "line A got " + sentToA);
    }

    @Test
    void randomSplitIgnoresLineLength() {
        WaitingLine longLine = lineWith("Long", 50);
        WaitingLine emptyLine = lineWith("Empty", 0);
        Random random = new Random(3);
        int sentToLong = 0;
        for (int i = 0; i < 1000; i++) {
            if (new RandomSplit().choose(List.of(longLine, emptyLine), random) == longLine) sentToLong++;
        }
        assertTrue(sentToLong > 400, "the long line still got " + sentToLong + " of 1000");
    }
}
