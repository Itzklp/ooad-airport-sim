package app;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Scenarios used by the app tests. */
final class TestScenarios {

    static final Path REAL_WORLD = Path.of("scenarios/real_world.properties");
    static final Path MORE_STATIONS = Path.of("scenarios/more_stations.properties");
    static final Path TWO_LINES = Path.of("scenarios/two_lines.properties");

    private TestScenarios() {
    }

    /** The settings of a scenario file, so a test can change some of them. */
    static Properties settingsOf(Path file) throws IOException {
        Properties p = new Properties();
        try (Reader in = Files.newBufferedReader(file)) {
            p.load(in);
        }
        return p;
    }

    /**
     * The known-answer queue of D1 Section 5: one counter, an arrival every 60 s and a service of 48 s
     * on average, both exponential. Security is made almost instant so it does not matter.
     */
    static Properties singleQueue() {
        Properties p = new Properties();
        p.setProperty("name", "M/M/1 check");
        p.setProperty("runs", "200");
        p.setProperty("seed", "11");
        p.setProperty("hours", "100");
        p.setProperty("arrival", "EXPO(60)");
        p.setProperty("checkin.lineRule", "random");
        p.setProperty("groups", "T");
        p.setProperty("group.T.name", "Test");
        p.setProperty("group.T.share", "1");
        p.setProperty("group.T.kioskShare", "0");
        p.setProperty("group.T.kiosks", "0");
        p.setProperty("group.T.kioskLines", "0");
        p.setProperty("group.T.counters", "1");
        p.setProperty("group.T.counterLines", "1");
        p.setProperty("group.T.counterTime", "EXPO(48)");
        p.setProperty("security.lanes", "1");
        p.setProperty("security.lines", "1");
        p.setProperty("security.lineRule", "random");
        p.setProperty("security.time", "EXPO(0.01)");
        p.setProperty("security.stopShare", "0");
        p.setProperty("security.stopTime", "EXPO(1)");
        return p;
    }
}
