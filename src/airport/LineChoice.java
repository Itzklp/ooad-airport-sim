package airport;

import java.util.List;
import java.util.Random;

public interface LineChoice {

    WaitingLine choose(List<WaitingLine> lines, Random rng);
}
