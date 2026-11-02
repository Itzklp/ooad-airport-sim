package app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import airport.ResourceType;
import results.ExperimentResult;

// M/M/1 queue: Wq = lambda / (mu (mu - lambda)) = 192 s = 3.20 minutes, utilization lambda / mu = 80%.
// 200 runs of 100 hours, so the empty start of each run hardly matters.
class QueueTheoryTest {

    private static final double ARRIVAL_RATE = 1.0 / 60;
    private static final double SERVICE_RATE = 1.0 / 48;

    private static ExperimentResult result;

    @BeforeAll
    static void runTheQueueOnce() {
        result = new ExperimentRunner(new AirportBuilder()).run(new Scenario(TestScenarios.singleQueue()), finishedRuns -> { });
    }

    @Test
    void averageWaitIsWithinFivePercentOfQueueingTheory() {
        double theoryMinutes = ARRIVAL_RATE / (SERVICE_RATE * (SERVICE_RATE - ARRIVAL_RATE)) / 60.0;
        assertEquals(3.20, theoryMinutes, 1e-9);
        assertEquals(theoryMinutes, result.averageWait("Test manned check-in"), 0.05 * theoryMinutes);
    }

    @Test
    void utilizationIsWithinTwoPointsOfEightyPercent() {
        assertEquals(100 * ARRIVAL_RATE / SERVICE_RATE, result.averageUtilization(ResourceType.MANNED_CHECK_IN), 2.0);
    }
}
