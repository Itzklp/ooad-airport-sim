package app;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * One airport layout and its run settings, read from a text file of "key = value" lines.
 * AirportBuilder checks the values; this class only stores and reads them.
 */
public class Scenario {

    private final Properties settings;

    public Scenario(Properties settings) {
        this.settings = settings;
    }

    public static Scenario load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) throw new FileNotFoundException("scenario file not found: " + file);
        Properties p = new Properties();
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(in);
        }
        return new Scenario(p);
    }

    /** The raw value of a key, or null if it is missing. */
    public String get(String key) {
        String v = settings.getProperty(key);
        return v == null ? null : v.trim();
    }

    public double getDouble(String key) {
        return Double.parseDouble(get(key));
    }

    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public String name() {
        String n = get("name");
        return n == null ? "unnamed" : n;
    }

    public String note() {
        return get("note");
    }

    public int runs() {
        return getInt("runs");
    }

    public long seed() {
        return Long.parseLong(get("seed"));
    }

    public double hours() {
        return getDouble("hours");
    }
}
