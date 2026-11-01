package airport;

import java.util.List;
import java.util.Random;

/** Picks the shortest line; a tie goes to the first line. */
public class ShortestLine implements LineChoice {

    @Override
    public WaitingLine choose(List<WaitingLine> lines, Random rng) {
        WaitingLine best = lines.get(0);
        for (WaitingLine line : lines) {
            if (line.length() < best.length()) best = line;
        }
        return best;
    }
}
