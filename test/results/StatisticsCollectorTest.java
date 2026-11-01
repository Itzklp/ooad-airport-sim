package results;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import airport.Airport;
import airport.ArrivalEvent;
import airport.CarrierGroup;
import airport.CheckInArea;
import airport.CheckInPoint;
import airport.PassengerSource;
import airport.RandomSplit;
import airport.ResourceType;
import airport.ScreeningLane;
import airport.SecurityCheckpoint;
import airport.WaitingLine;
import sim.Distribution;
import sim.Simulator;

/** Phase 3: one run's statistics, worked out by hand on a tiny airport with constant times. */
class StatisticsCollectorTest {

    private static Distribution constant(double value) {
        return new Distribution() {
            @Override
            public double sample(Random rng) {
                return value;
            }

            @Override
            public double mean() {
                return value;
            }
        };
    }

    /**
     * Two airlines, but only "Used" gets passengers: one arrives every 100 s and goes to one counter (30 s),
     * then one screening lane (20 s). The kiosk line of "Used" and the airline "Empty" see nobody.
     */
    private static RunResult runTinyAirport(double endTime) {
        StatisticsCollector stats = new StatisticsCollector();
        CarrierGroup used = new CarrierGroup("Used", 1, 0);
        CarrierGroup empty = new CarrierGroup("Empty", 0, 0);
        PassengerSource source = new PassengerSource(constant(100), List.of(used, empty));
        source.addListener(stats);

        WaitingLine securityLine = new WaitingLine("Security 1", "Security 1");
        SecurityCheckpoint security = new SecurityCheckpoint(List.of(securityLine), new RandomSplit(), 0, constant(0));
        ScreeningLane lane = new ScreeningLane("Lane 1", constant(20), securityLine, security);
        WaitingLine kioskLine = new WaitingLine("Used self check-in", "Used self check-in");
        WaitingLine counterLine = new WaitingLine("Used manned check-in", "Used manned check-in");
        CheckInPoint kiosk = new CheckInPoint("Kiosk", ResourceType.SELF_CHECK_IN, constant(25), kioskLine, security);
        CheckInPoint counter = new CheckInPoint("Counter", ResourceType.MANNED_CHECK_IN, constant(30), counterLine, security);
        lane.addListener(stats);
        kiosk.addListener(stats);
        counter.addListener(stats);
        CheckInArea area = new CheckInArea(used, List.of(kioskLine), List.of(counterLine), new RandomSplit());

        Airport airport = new Airport("Tiny", List.of(used, empty), source, Map.of(used, area), security,
                List.of(kioskLine, counterLine, securityLine), List.of(lane, kiosk, counter));
        Simulator sim = new Simulator(1);
        sim.schedule(new ArrivalEvent(0, airport));
        sim.run(endTime);
        return stats.summary(airport, endTime);
    }

    @Test
    void arrivalsAreCountedByAirlineIncludingZero() {
        RunResult r = runTinyAirport(1000);
        assertEquals(Map.of("Used", 11, "Empty", 0), r.arrivals(), "arrivals at 0, 100, ..., 1000");
    }

    @Test
    void utilizationIsBusyTimeOverAvailableTime() {
        RunResult r = runTinyAirport(1000);
        // passengers at 0..900 are fully served; the one at 1000 has just started (0 s busy so far)
        assertEquals(10 * 30 / 1000.0, r.utilization().get(ResourceType.MANNED_CHECK_IN), 1e-9);
        assertEquals(0.0, r.utilization().get(ResourceType.SELF_CHECK_IN), 1e-9);
        assertEquals(10 * 20 / 1000.0, r.utilization().get(ResourceType.SECURITY), 1e-9);
    }

    @Test
    void aServiceInProgressAtTheEndCountsUpToTheEnd() {
        RunResult r = runTinyAirport(1010);
        assertEquals((10 * 30 + 10) / 1010.0, r.utilization().get(ResourceType.MANNED_CHECK_IN), 1e-9);
    }

    @Test
    void everyLineCategoryIsReportedEvenIfNobodyUsedIt() {
        RunResult r = runTinyAirport(1000);
        assertEquals(List.of("Used self check-in", "Used manned check-in", "Security 1"), List.copyOf(r.waits().keySet()));
        assertEquals(0, r.waits().get("Used self check-in").count());
        assertEquals(11, r.waits().get("Used manned check-in").count());
        assertEquals(0.0, r.waits().get("Used manned check-in").max(), "nobody waited: one arrival per 100 s");
        assertFalse(r.waits().containsKey("Empty manned check-in"), "Empty has no check-in area in this test");
    }
}
