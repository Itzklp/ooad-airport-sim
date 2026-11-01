package sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Phase 1: each distribution draws values with the right mean and spread.
 * Requirement 6 (service times are sampled from the scenario's distributions).
 */
class DistributionTest {

    private static final int SAMPLES = 200_000;

    /** Each case: a label, the distribution, and its variance worked out by hand. */
    static Stream<Arguments> distributions() {
        return Stream.of(
                Arguments.of("EXPO(48)", new ExponentialDist(48), 48.0 * 48.0),
                Arguments.of("GAMM(39.2, 1)", new GammaDist(39.2, 1), 39.2 * 39.2),
                Arguments.of("GAMM(10, 0.5), shape below 1", new GammaDist(10, 0.5), 0.5 * 10 * 10),
                Arguments.of("GAMM(5, 3)", new GammaDist(5, 3), 3 * 5.0 * 5.0),
                Arguments.of("WEIB(40, 1.5)", new WeibullDist(40, 1.5), 601.1044557022908),
                Arguments.of("WEIB(10, 1), same as EXPO(10)", new WeibullDist(10, 1), 100.0),
                Arguments.of("54 + EXPO(60)", new Shifted(54, new ExponentialDist(60)), 60.0 * 60.0));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("distributions")
    void sampleMeanIsWithinOnePercentOfTheory(String label, Distribution d, double variance) {
        double mean = sampleMean(d, new Random(11));
        assertEquals(d.mean(), mean, 0.01 * d.mean(), label);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("distributions")
    void sampleVarianceIsWithinFourPercentOfTheory(String label, Distribution d, double variance) {
        Random rng = new Random(12);
        double sum = 0;
        double sumSq = 0;
        for (int i = 0; i < SAMPLES; i++) {
            double x = d.sample(rng);
            sum += x;
            sumSq += x * x;
        }
        double mean = sum / SAMPLES;
        double sampleVariance = (sumSq - SAMPLES * mean * mean) / (SAMPLES - 1);
        assertEquals(variance, sampleVariance, 0.04 * variance, label);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("distributions")
    void noSampleIsNegative(String label, Distribution d, double variance) {
        Random rng = new Random(13);
        for (int i = 0; i < SAMPLES; i++) {
            double x = d.sample(rng);
            assertTrue(x >= 0, label + " gave " + x);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("distributions")
    void sameSeedGivesSameSequence(String label, Distribution d, double variance) {
        Random a = new Random(2026);
        Random b = new Random(2026);
        for (int i = 0; i < 1000; i++) assertEquals(d.sample(a), d.sample(b), label);
    }

    @Test
    void weibullMeanUsesTheGammaFunction() {
        assertEquals(10.0, new WeibullDist(10, 1).mean(), 1e-9);                        // Gamma(2) = 1
        assertEquals(10.0 * Math.sqrt(Math.PI) / 2, new WeibullDist(10, 2).mean(), 1e-9); // Gamma(1.5) = sqrt(pi)/2
        assertEquals(36.10981171803735, new WeibullDist(40, 1.5).mean(), 1e-9);
    }

    @Test
    void shiftedAddsTheOffsetToEverySample() {
        Distribution base = new ExponentialDist(60);
        Distribution shifted = new Shifted(54, base);
        Random a = new Random(5);
        Random b = new Random(5);
        for (int i = 0; i < 1000; i++) assertEquals(54 + base.sample(a), shifted.sample(b), 1e-9);
        assertEquals(114.0, shifted.mean(), 1e-9);
    }

    @Test
    void parametersAtOrBelowZeroAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new ExponentialDist(0));
        assertThrows(IllegalArgumentException.class, () -> new ExponentialDist(-1));
        assertThrows(IllegalArgumentException.class, () -> new GammaDist(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new GammaDist(1, 0));
        assertThrows(IllegalArgumentException.class, () -> new WeibullDist(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new WeibullDist(1, -2));
    }

    private static double sampleMean(Distribution d, Random rng) {
        double sum = 0;
        for (int i = 0; i < SAMPLES; i++) sum += d.sample(rng);
        return sum / SAMPLES;
    }
}
