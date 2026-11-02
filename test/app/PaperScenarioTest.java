package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import results.ExperimentResult;

class PaperScenarioTest {

    private final ExperimentRunner runner = new ExperimentRunner(new AirportBuilder());

    // the paper's passengers per day: 179.14, 1068.43 and 957.09
    @Test
    void passengersPerDayAreCloseToThePaper() throws IOException {
        ExperimentResult result = runner.run(Scenario.load(TestScenarios.REAL_WORLD), finishedRuns -> { });
        assertEquals(80, result.runCount());
        assertEquals(179.14, result.averageArrivals("American/Continental"), 0.03 * 179.14);
        assertEquals(1068.43, result.averageArrivals("Southwest"), 0.03 * 1068.43);
        assertEquals(957.09, result.averageArrivals("US Airways"), 0.03 * 957.09);
    }

    @Test
    void eightyRunsOfEachScenarioFinishInAFewSeconds() {
        assertTimeout(Duration.ofSeconds(10), () -> {
            for (Path file : List.of(TestScenarios.REAL_WORLD, TestScenarios.MORE_STATIONS, TestScenarios.TWO_LINES)) {
                runner.run(Scenario.load(file), finishedRuns -> { });
            }
        });
    }
}
