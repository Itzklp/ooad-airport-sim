package airport;

import java.util.List;
import java.util.Random;

/** The rule for choosing which line a passenger joins. (Strategy pattern) */
public interface LineChoice {

    WaitingLine choose(List<WaitingLine> lines, Random rng);
}
