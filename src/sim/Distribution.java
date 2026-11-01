package sim;

import java.util.Random;

/** A source of random times, such as service times or times between arrivals. (Strategy pattern) */
public interface Distribution {

    /** Draws one random value using the given random stream. */
    double sample(Random rng);

    /** The theoretical mean, used for checks and reports. */
    double mean();
}
