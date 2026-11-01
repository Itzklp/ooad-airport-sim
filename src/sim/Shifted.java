package sim;

import java.util.Random;

/**
 * Adds a constant to another distribution, as in the paper's "54 + EXPO(...)". (Decorator pattern)
 * It is itself a Distribution, so it can be used anywhere a Distribution is expected.
 */
public class Shifted implements Distribution {

    private final double offset;
    private final Distribution base;

    public Shifted(double offset, Distribution base) {
        this.offset = offset;
        this.base = base;
    }

    @Override
    public double sample(Random rng) {
        return offset + base.sample(rng);
    }

    @Override
    public double mean() {
        return offset + base.mean();
    }

    @Override
    public String toString() {
        return offset + " + " + base;
    }
}
