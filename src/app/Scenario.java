package app;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Scenario {

    private final Properties settings;

    public Scenario(Properties settings) {
        this.settings = settings;
    }

    public static Scenario load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) throw new FileNotFoundException("scenario file not found: " + file);
        Properties settings = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            settings.load(reader);
        }
        return new Scenario(settings);
    }

    public String get(String key) {
        String value = settings.getProperty(key);
        return value == null ? null : value.trim();
    }

    public double getDouble(String key) {
        return Double.parseDouble(get(key));
    }

    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public String name() {
        String name = get("name");
        return name == null ? "unnamed" : name;
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
