package app;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class TestScenarios {

    static final Path REAL_WORLD = Path.of("scenarios/real_world.properties");
    static final Path MORE_STATIONS = Path.of("scenarios/more_stations.properties");
    static final Path TWO_LINES = Path.of("scenarios/two_lines.properties");

    private TestScenarios() {
    }

    static Properties settingsOf(Path file) throws IOException {
        Properties settings = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            settings.load(reader);
        }
        return settings;
    }

    // The known-answer M/M/1 queue: one counter, exponential arrivals every 60 s and service of 48 s.
    // Security is made almost instant so it does not affect the check-in wait.
    static Properties singleQueue() {
        Properties settings = new Properties();
        settings.setProperty("name", "M/M/1 check");
        settings.setProperty("runs", "200");
        settings.setProperty("seed", "11");
        settings.setProperty("hours", "100");
        settings.setProperty("arrival", "EXPO(60)");
        settings.setProperty("checkin.lineRule", "random");
        settings.setProperty("groups", "T");
        settings.setProperty("group.T.name", "Test");
        settings.setProperty("group.T.share", "1");
        settings.setProperty("group.T.kioskShare", "0");
        settings.setProperty("group.T.kiosks", "0");
        settings.setProperty("group.T.kioskLines", "0");
        settings.setProperty("group.T.counters", "1");
        settings.setProperty("group.T.counterLines", "1");
        settings.setProperty("group.T.counterTime", "EXPO(48)");
        settings.setProperty("security.lanes", "1");
        settings.setProperty("security.lines", "1");
        settings.setProperty("security.lineRule", "random");
        settings.setProperty("security.time", "EXPO(0.01)");
        settings.setProperty("security.stopShare", "0");
        settings.setProperty("security.stopTime", "EXPO(1)");
        return settings;
    }
}
