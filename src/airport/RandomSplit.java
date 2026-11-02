package airport;

import java.util.List;
import java.util.Random;

public class RandomSplit implements LineChoice {

    @Override
    public WaitingLine choose(List<WaitingLine> lines, Random rng) {
        return lines.get(rng.nextInt(lines.size()));
    }
}
