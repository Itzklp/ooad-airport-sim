package airport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import sim.DistributionFactory;
import sim.Simulator;

/** Phase 2: creating passengers. Covers requirement 3. */
class PassengerSourceTest {

    @Test
    void passengersGetIncreasingIdsAndTheCurrentTime() {
        Simulator sim = new Simulator(1);
        sim.run(500);
        Fixtures.Recorder recorder = new Fixtures.Recorder();
        PassengerSource source = new PassengerSource(Fixtures.constant(10), List.of(new CarrierGroup("A", 1, 0)));
        source.addListener(recorder);
        Passenger first = source.next(sim);
        Passenger second = source.next(sim);
        assertEquals(1, first.id());
        assertEquals(2, second.id());
        assertEquals(500.0, first.arrivalTime());
        assertEquals(List.of(first, second), recorder.arrived, "the listener hears about each passenger");
    }

    @Test
    void airlinesArePickedByTheirShares() {
        CarrierGroup small = new CarrierGroup("Small", 0.25, 0);
        CarrierGroup big = new CarrierGroup("Big", 0.75, 0);
        PassengerSource source = new PassengerSource(Fixtures.constant(1), List.of(small, big));
        Simulator sim = new Simulator(8);
        int n = 20_000;
        int toSmall = 0;
        for (int i = 0; i < n; i++) {
            if (source.next(sim).group() == small) toSmall++;
        }
        assertEquals(0.25, toSmall / (double) n, 0.01);
    }

    @Test
    void kioskShareDecidesWhoUsesAKiosk() {
        assertEquals(0.0, kioskFraction(0.0));
        assertEquals(1.0, kioskFraction(1.0));
        assertEquals(0.4, kioskFraction(0.4), 0.015);
    }

    @Test
    void aGapBelowZeroCountsAsZero() {
        Simulator sim = new Simulator(1);
        sim.run(100);
        PassengerSource source = new PassengerSource(Fixtures.constant(-5), List.of(new CarrierGroup("A", 1, 0)));
        assertEquals(100.0, source.nextArrivalTime(sim));
    }

    @Test
    void thePapersArrivalFormulaNeverGoesBackInTime() {
        Simulator sim = new Simulator(9);
        PassengerSource source = new PassengerSource(DistributionFactory.parse("-0.001 + GAMM(39.2, 1)"),
                List.of(new CarrierGroup("A", 1, 0)));
        double sum = 0;
        int n = 50_000;
        for (int i = 0; i < n; i++) {
            double gap = source.nextArrivalTime(sim) - sim.now();
            assertTrue(gap >= 0);
            sum += gap;
        }
        assertEquals(39.2, sum / n, 0.6, "mean gap is about 39.2 s, i.e. about 2204 passengers a day");
    }

    private static double kioskFraction(double kioskShare) {
        PassengerSource source = new PassengerSource(Fixtures.constant(1), List.of(new CarrierGroup("A", 1, kioskShare)));
        Simulator sim = new Simulator(10);
        int n = 10_000;
        int kiosk = 0;
        for (int i = 0; i < n; i++) {
            if (source.next(sim).usesKiosk()) kiosk++;
        }
        return kiosk / (double) n;
    }
}
