package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import results.ExperimentResult;

/**
 * Phase 3: the real-world scenario against the figures the paper does give.
 * Passengers per day come from the paper (179.14, 1068.43, 957.09); the shares in the
 * scenario file are derived from them, so these must agree within a few percent.
 */
class PaperScenarioTest {

    private final ExperimentRunner runner = new ExperimentRunner(new AirportBuilder());

    @Test
    void passengersPerDayAreCloseToThePaper() throws IOException {
        ExperimentResult r = runner.run(Scenario.load(TestScenarios.REAL_WORLD), n -> { });
        assertEquals(80, r.runCount());
        assertEquals(179.14, r.averageArrivals("American/Continental"), 0.03 * 179.14);
        assertEquals(1068.43, r.averageArrivals("Southwest"), 0.03 * 1068.43);
        assertEquals(957.09, r.averageArrivals("US Airways"), 0.03 * 957.09);
    }

    @Test
    void eightyRunsOfEachScenarioFinishInAFewSeconds() {
        assertTimeout(Duration.ofSeconds(10), () -> {
            for (var file : new java.nio.file.Path[] {TestScenarios.REAL_WORLD, TestScenarios.MORE_STATIONS, TestScenarios.TWO_LINES}) {
                runner.run(Scenario.load(file), n -> { });
            }
        });
    }
}
