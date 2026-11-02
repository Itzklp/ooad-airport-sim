package sim;

import java.util.Random;

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

    // Lanczos approximation of the gamma function
    private static double gammaFunction(double value) {
        double[] coefficients = {0.99999999999980993, 676.5203681218851, -1259.1392167224028,
                771.32342877765313, -176.61502916214059, 12.507343278686905,
                -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7};
        if (value < 0.5) return Math.PI / (Math.sin(Math.PI * value) * gammaFunction(1.0 - value));
        value -= 1.0;
        double sum = coefficients[0];
        double shifted = value + 7.5;
        for (int i = 1; i < coefficients.length; i++) sum += coefficients[i] / (value + i);
        return Math.sqrt(2 * Math.PI) * Math.pow(shifted, value + 0.5) * Math.exp(-shifted) * sum;
    }

    @Override
    public String toString() {
        return "WEIB(" + scale + ", " + shape + ")";
    }
}
