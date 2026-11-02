package airport;

import java.util.List;
import java.util.Random;

public class ShortestLine implements LineChoice {

    @Override
    public WaitingLine choose(List<WaitingLine> lines, Random rng) {
        WaitingLine shortest = lines.get(0);
        for (WaitingLine line : lines) {
            if (line.length() < shortest.length()) shortest = line;   // strictly shorter, so a tie stays with the first line
        }
        return shortest;
    }
}
