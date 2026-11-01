package sim;

import java.util.Random;

/**
 * Gamma distribution, written GAMM(scale, shape) in Arena notation.
 * Sampling uses the method of Marsaglia and Tsang.
 */
public class GammaDist implements Distribution {

    private final double scale;
    private final double shape;

    public GammaDist(double scale, double shape) {
        if (scale <= 0 || shape <= 0) throw new IllegalArgumentException("GAMM scale and shape must be greater than 0");
        this.scale = scale;
        this.shape = shape;
    }

    @Override
    public double sample(Random rng) {
        if (shape < 1.0) {
            // boost: Gamma(a) = Gamma(a + 1) * U^(1/a)
            return standardGamma(shape + 1.0, rng) * Math.pow(rng.nextDouble(), 1.0 / shape) * scale;
        }
        return standardGamma(shape, rng) * scale;
    }

    private static double standardGamma(double a, Random rng) {
        double d = a - 1.0 / 3.0;
        double c = 1.0 / Math.sqrt(9.0 * d);
        while (true) {
            double x;
            double v;
            do {
                x = rng.nextGaussian();
                v = 1.0 + c * x;
            } while (v <= 0);
            v = v * v * v;
            double u = rng.nextDouble();
            if (u < 1.0 - 0.0331 * x * x * x * x) return d * v;
            if (Math.log(u) < 0.5 * x * x + d * (1.0 - v + Math.log(v))) return d * v;
        }
    }

    @Override
    public double mean() {
        return shape * scale;
    }

    @Override
    public String toString() {
        return "GAMM(" + scale + ", " + shape + ")";
    }
}
