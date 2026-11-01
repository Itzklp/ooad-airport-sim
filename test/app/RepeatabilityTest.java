package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import results.ExperimentResult;

/** Phase 3: repeatable runs. Covers business rule 6 and requirement 8. */
class RepeatabilityTest {

    private final ExperimentRunner runner = new ExperimentRunner(new AirportBuilder());

    private ExperimentResult run(long seed, int runs) throws IOException {
        Properties p = TestScenarios.settingsOf(TestScenarios.REAL_WORLD);
        p.setProperty("seed", Long.toString(seed));
        p.setProperty("runs", Integer.toString(runs));
        return runner.run(new Scenario(p), n -> { });
    }

    @Test
    void sameScenarioAndSeedGiveIdenticalResults() throws IOException {
        assertEquals(run(2026, 5).toCsv(), run(2026, 5).toCsv());
    }

    @Test
    void aDifferentSeedGivesDifferentResults() throws IOException {
        assertNotEquals(run(2026, 5).toCsv(), run(2027, 5).toCsv());
    }

    /** Run 2 of an experiment with seed 2026 uses seed 2027 and must not be affected by run 1. */
    @Test
    void eachRunStartsFreshWithItsOwnSeed() throws IOException {
        ExperimentResult both = run(2026, 2);
        ExperimentResult first = run(2026, 1);
        ExperimentResult second = run(2027, 1);
        for (String g : new String[] {"American/Continental", "Southwest", "US Airways"}) {
            assertEquals((first.averageArrivals(g) + second.averageArrivals(g)) / 2, both.averageArrivals(g), 1e-9, g);
        }
        for (String c : both.categories()) {
            assertEquals((first.averageWait(c) + second.averageWait(c)) / 2, both.averageWait(c), 1e-9, c);
        }
    }

    @Test
    void theRunnerDoesTheNumberOfRunsAndReportsProgress() throws IOException {
        int[] last = {0};
        Properties p = TestScenarios.settingsOf(TestScenarios.REAL_WORLD);
        p.setProperty("runs", "7");
        ExperimentResult r = runner.run(new Scenario(p), n -> last[0] = n);
        assertEquals(7, r.runCount());
        assertEquals(7, last[0]);
    }

    @Test
    void theRunnerRefusesAWrongScenario() throws IOException {
        Properties p = TestScenarios.settingsOf(TestScenarios.REAL_WORLD);
        p.setProperty("hours", "-1");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> runner.run(new Scenario(p), n -> { }));
        assertEquals("hours must be greater than 0", e.getMessage());
    }
}
