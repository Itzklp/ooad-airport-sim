package app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import airport.ResourceType;
import results.ExperimentResult;

/**
 * Phase 3: the known-answer check promised in D1 Section 5.
 * A single counter with exponential arrivals (mean 60 s) and exponential service (mean 48 s)
 * is the M/M/1 queue, for which Wq = lambda / (mu (mu - lambda)) = 192 s = 3.20 minutes
 * and the utilization is lambda / mu = 80%.
 * 200 runs of 100 hours each, so the empty start of each run hardly matters.
 */
class QueueTheoryTest {

    private static final double LAMBDA = 1.0 / 60;
    private static final double MU = 1.0 / 48;

    private static ExperimentResult result;

    @BeforeAll
    static void runTheQueueOnce() {
        result = new ExperimentRunner(new AirportBuilder()).run(new Scenario(TestScenarios.singleQueue()), n -> { });
    }

    @Test
    void averageWaitIsWithinFivePercentOfQueueingTheory() {
        double theoryMinutes = LAMBDA / (MU * (MU - LAMBDA)) / 60.0;
        assertEquals(3.20, theoryMinutes, 1e-9);
        assertEquals(theoryMinutes, result.averageWait("Test manned check-in"), 0.05 * theoryMinutes);
    }

    @Test
    void utilizationIsWithinTwoPointsOfEightyPercent() {
        assertEquals(100 * LAMBDA / MU, result.averageUtilization(ResourceType.MANNED_CHECK_IN), 2.0);
    }
}
