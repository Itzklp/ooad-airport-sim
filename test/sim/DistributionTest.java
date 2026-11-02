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

class DistributionTest {

    private static final int SAMPLES = 200_000;

    // label, distribution, and its variance worked out by hand
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
    void sampleMeanIsWithinOnePercentOfTheory(String label, Distribution distribution, double variance) {
        double sampleMean = sampleMean(distribution, new Random(11));
        assertEquals(distribution.mean(), sampleMean, 0.01 * distribution.mean(), label);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("distributions")
    void sampleVarianceIsWithinFourPercentOfTheory(String label, Distribution distribution, double variance) {
        Random random = new Random(12);
        double sum = 0;
        double sumOfSquares = 0;
        for (int i = 0; i < SAMPLES; i++) {
            double value = distribution.sample(random);
            sum += value;
            sumOfSquares += value * value;
        }
        double mean = sum / SAMPLES;
        double sampleVariance = (sumOfSquares - SAMPLES * mean * mean) / (SAMPLES - 1);
        assertEquals(variance, sampleVariance, 0.04 * variance, label);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("distributions")
    void noSampleIsNegative(String label, Distribution distribution, double variance) {
        Random random = new Random(13);
        for (int i = 0; i < SAMPLES; i++) {
            double value = distribution.sample(random);
            assertTrue(value >= 0, label + " gave " + value);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("distributions")
    void sameSeedGivesSameSequence(String label, Distribution distribution, double variance) {
        Random first = new Random(2026);
        Random second = new Random(2026);
        for (int i = 0; i < 1000; i++) assertEquals(distribution.sample(first), distribution.sample(second), label);
    }

    @Test
    void weibullMeanUsesTheGammaFunction() {
        assertEquals(10.0, new WeibullDist(10, 1).mean(), 1e-9);
        assertEquals(10.0 * Math.sqrt(Math.PI) / 2, new WeibullDist(10, 2).mean(), 1e-9);
        assertEquals(36.10981171803735, new WeibullDist(40, 1.5).mean(), 1e-9);
    }

    @Test
    void shiftedAddsTheOffsetToEverySample() {
        Distribution base = new ExponentialDist(60);
        Distribution shifted = new Shifted(54, base);
        Random first = new Random(5);
        Random second = new Random(5);
        for (int i = 0; i < 1000; i++) assertEquals(54 + base.sample(first), shifted.sample(second), 1e-9);
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

    private static double sampleMean(Distribution distribution, Random random) {
        double sum = 0;
        for (int i = 0; i < SAMPLES; i++) sum += distribution.sample(random);
        return sum / SAMPLES;
    }
}
