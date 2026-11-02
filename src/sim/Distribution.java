package sim;

import java.util.Random;

public interface Distribution {

    double sample(Random rng);

    double mean();
}
