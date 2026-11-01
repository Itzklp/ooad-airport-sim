package sim;

import java.util.Random;

/** Exponential distribution, written EXPO(mean) in the paper's Arena notation. */
public class ExponentialDist implements Distribution {

    private final double mean;

    public ExponentialDist(double mean) {
        if (mean <= 0) throw new IllegalArgumentException("EXPO mean must be greater than 0");
        this.mean = mean;
    }

    @Override
    public double sample(Random rng) {
        return -mean * Math.log(1.0 - rng.nextDouble());
    }

    @Override
    public double mean() {
        return mean;
    }

    @Override
    public String toString() {
        return "EXPO(" + mean + ")";
    }
}
