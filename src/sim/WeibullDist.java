package sim;

import java.util.Random;

/** Weibull distribution, written WEIB(scale, shape) in Arena notation. */
public class WeibullDist implements Distribution {

    private final double scale;
    private final double shape;

    public WeibullDist(double scale, double shape) {
        if (scale <= 0 || shape <= 0) throw new IllegalArgumentException("WEIB scale and shape must be greater than 0");
        this.scale = scale;
        this.shape = shape;
    }

    @Override
    public double sample(Random rng) {
        return scale * Math.pow(-Math.log(1.0 - rng.nextDouble()), 1.0 / shape);
    }

    @Override
    public double mean() {
        return scale * gammaFunction(1.0 + 1.0 / shape);
    }

    /** Lanczos approximation of the gamma function, needed only for mean(). */
    private static double gammaFunction(double x) {
        double[] g = {0.99999999999980993, 676.5203681218851, -1259.1392167224028,
                771.32342877765313, -176.61502916214059, 12.507343278686905,
                -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7};
        if (x < 0.5) return Math.PI / (Math.sin(Math.PI * x) * gammaFunction(1.0 - x));
        x -= 1.0;
        double a = g[0];
        double t = x + 7.5;
        for (int i = 1; i < 9; i++) a += g[i] / (x + i);
        return Math.sqrt(2 * Math.PI) * Math.pow(t, x + 0.5) * Math.exp(-t) * a;
    }

    @Override
    public String toString() {
        return "WEIB(" + scale + ", " + shape + ")";
    }
}
