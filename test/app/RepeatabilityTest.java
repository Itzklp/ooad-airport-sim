package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import results.ExperimentResult;

class RepeatabilityTest {

    private final ExperimentRunner runner = new ExperimentRunner(new AirportBuilder());

    private ExperimentResult run(long seed, int runs) throws IOException {
        Properties settings = TestScenarios.settingsOf(TestScenarios.REAL_WORLD);
        settings.setProperty("seed", Long.toString(seed));
        settings.setProperty("runs", Integer.toString(runs));
        return runner.run(new Scenario(settings), finishedRuns -> { });
    }

    @Test
    void sameScenarioAndSeedGiveIdenticalResults() throws IOException {
        assertEquals(run(2026, 5).toCsv(), run(2026, 5).toCsv());
    }

    @Test
    void aDifferentSeedGivesDifferentResults() throws IOException {
        assertNotEquals(run(2026, 5).toCsv(), run(2027, 5).toCsv());
    }

    // Run 2 of an experiment with seed 2026 uses seed 2027, and must not be affected by run 1.
    @Test
    void eachRunStartsFreshWithItsOwnSeed() throws IOException {
        ExperimentResult bothRuns = run(2026, 2);
        ExperimentResult firstAlone = run(2026, 1);
        ExperimentResult secondAlone = run(2027, 1);
        for (String group : new String[] {"American/Continental", "Southwest", "US Airways"}) {
            double expected = (firstAlone.averageArrivals(group) + secondAlone.averageArrivals(group)) / 2;
            assertEquals(expected, bothRuns.averageArrivals(group), 1e-9, group);
        }
        for (String category : bothRuns.categories()) {
            double expected = (firstAlone.averageWait(category) + secondAlone.averageWait(category)) / 2;
            assertEquals(expected, bothRuns.averageWait(category), 1e-9, category);
        }
    }

    @Test
    void theRunnerDoesTheNumberOfRunsAndReportsProgress() throws IOException {
        int[] lastReported = {0};
        Properties settings = TestScenarios.settingsOf(TestScenarios.REAL_WORLD);
        settings.setProperty("runs", "7");
        ExperimentResult result = runner.run(new Scenario(settings), finishedRuns -> lastReported[0] = finishedRuns);
        assertEquals(7, result.runCount());
        assertEquals(7, lastReported[0]);
    }

    @Test
    void theRunnerRefusesAWrongScenario() throws IOException {
        Properties settings = TestScenarios.settingsOf(TestScenarios.REAL_WORLD);
        settings.setProperty("hours", "-1");
        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> runner.run(new Scenario(settings), finishedRuns -> { }));
        assertEquals("hours must be greater than 0", refusal.getMessage());
    }
}
