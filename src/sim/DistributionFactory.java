package sim;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DistributionFactory {

    // Arena notation: an optional constant, a name and its numbers, e.g. "54 + EXPO(60)" or "-0.001 + GAMM(39.2, 1)"
    private static final Pattern FORMAT = Pattern.compile(
            "^\\s*(?:(-?\\d+(?:\\.\\d+)?)\\s*\\+\\s*)?([A-Z]+)\\s*\\(([^)]*)\\)\\s*$");

    private DistributionFactory() {
    }

    public static Distribution parse(String text) {
        if (text == null) throw new IllegalArgumentException("no distribution given");
        Matcher match = FORMAT.matcher(text);
        if (!match.matches()) throw new IllegalArgumentException("cannot read distribution \"" + text + "\"");

        String[] pieces = match.group(3).split(",");
        double[] numbers = new double[pieces.length];
        for (int i = 0; i < pieces.length; i++) {
            String piece = pieces[i].trim();
            try {
                numbers[i] = Double.parseDouble(piece);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("\"" + piece + "\" is not a number in \"" + text + "\"");
            }
        }

        String name = match.group(2);
        Distribution base = switch (name) {
            case "EXPO" -> {
                expectCount(numbers, 1, text);
                yield new ExponentialDist(numbers[0]);
            }
            case "GAMM" -> {
                expectCount(numbers, 2, text);
                yield new GammaDist(numbers[0], numbers[1]);
            }
            case "WEIB" -> {
                expectCount(numbers, 2, text);
                yield new WeibullDist(numbers[0], numbers[1]);
            }
            default -> throw new IllegalArgumentException("unknown distribution " + name + " in \"" + text + "\"");
        };

        String constant = match.group(1);
        if (constant == null) return base;
        double offset = Double.parseDouble(constant);
        return offset == 0.0 ? base : new Shifted(offset, base);
    }

    private static void expectCount(double[] numbers, int count, String text) {
        if (numbers.length != count) {
            throw new IllegalArgumentException("expected " + count + " number(s) in \"" + text + "\"");
        }
    }
}
