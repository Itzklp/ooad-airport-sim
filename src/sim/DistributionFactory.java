package sim;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Makes a Distribution from text written in the paper's notation,
 * for example "54 + EXPO(60)" or "-0.001 + GAMM(39.2, 1)". (Factory)
 */
public final class DistributionFactory {

    private static final Pattern FORMAT = Pattern.compile(
            "^\\s*(?:(-?\\d+(?:\\.\\d+)?)\\s*\\+\\s*)?([A-Z]+)\\s*\\(([^)]*)\\)\\s*$");

    private DistributionFactory() {
    }

    /** Parses the text; throws IllegalArgumentException with a readable message if it is wrong. */
    public static Distribution parse(String text) {
        if (text == null) throw new IllegalArgumentException("no distribution given");
        Matcher m = FORMAT.matcher(text);
        if (!m.matches()) throw new IllegalArgumentException("cannot read distribution \"" + text + "\"");

        String[] parts = m.group(3).split(",");
        double[] args = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                args[i] = Double.parseDouble(parts[i].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("\"" + parts[i].trim() + "\" is not a number in \"" + text + "\"");
            }
        }

        Distribution base = switch (m.group(2)) {
            case "EXPO" -> {
                need(args, 1, text);
                yield new ExponentialDist(args[0]);
            }
            case "GAMM" -> {
                need(args, 2, text);
                yield new GammaDist(args[0], args[1]);
            }
            case "WEIB" -> {
                need(args, 2, text);
                yield new WeibullDist(args[0], args[1]);
            }
            default -> throw new IllegalArgumentException("unknown distribution " + m.group(2) + " in \"" + text + "\"");
        };

        if (m.group(1) == null) return base;
        double offset = Double.parseDouble(m.group(1));
        return offset == 0.0 ? base : new Shifted(offset, base);
    }

    private static void need(double[] args, int count, String text) {
        if (args.length != count) {
            throw new IllegalArgumentException("expected " + count + " number(s) in \"" + text + "\"");
        }
    }
}
