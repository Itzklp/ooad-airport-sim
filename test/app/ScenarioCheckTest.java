package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import airport.Airport;
import airport.ResourceType;
import airport.ServicePoint;

/** Phase 3: reading and checking scenarios. Covers requirements 1 and 2. */
class ScenarioCheckTest {

    private final AirportBuilder builder = new AirportBuilder();

    @Test
    void theThreePaperScenariosPassTheCheck() throws IOException {
        for (Path file : List.of(TestScenarios.REAL_WORLD, TestScenarios.MORE_STATIONS, TestScenarios.TWO_LINES)) {
            assertEquals(List.of(), builder.check(Scenario.load(file)), file.toString());
        }
    }

    @Test
    void threePlantedErrorsAreAllReported() throws IOException {
        Properties p = TestScenarios.settingsOf(TestScenarios.REAL_WORLD);
        p.setProperty("group.AC.share", "0.0013");               // shares no longer add up to 1
        p.setProperty("group.SW.counterTime", "54 + EXPO(0)");   // the paper's printed zero
        p.setProperty("group.US.counterLines", "5");             // more lines than counters
        List<String> errors = builder.check(new Scenario(p));
        assertEquals(3, errors.size(), errors.toString());
        assertTrue(errors.stream().anyMatch(e -> e.contains("shares add up to")), errors.toString());
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("group.SW.counterTime")), errors.toString());
        assertTrue(errors.stream().anyMatch(e -> e.contains("group.US.counterLines: more lines than service points")), errors.toString());
    }

    @Test
    void anEmptyScenarioListsEveryMissingValue() {
        List<String> errors = builder.check(new Scenario(new Properties()));
        for (String key : List.of("runs", "hours", "seed", "arrival", "checkin.lineRule", "groups",
                "security.lanes", "security.lineRule", "security.time", "security.stopShare")) {
            assertTrue(errors.stream().anyMatch(e -> e.startsWith(key)), key + " not reported in " + errors);
        }
    }

    @Test
    void wrongValuesAreReported() throws IOException {
        Properties p = TestScenarios.settingsOf(TestScenarios.REAL_WORLD);
        p.setProperty("runs", "0");
        p.setProperty("seed", "abc");
        p.setProperty("checkin.lineRule", "fastest");
        p.setProperty("security.stopShare", "1.5");
        p.setProperty("group.AC.kiosks", "-1");
        List<String> errors = builder.check(new Scenario(p));
        assertTrue(errors.contains("runs must be at least 1"), errors.toString());
        assertTrue(errors.contains("seed must be a whole number"), errors.toString());
        assertTrue(errors.contains("checkin.lineRule must be random or shortest"), errors.toString());
        assertTrue(errors.contains("security.stopShare must be between 0 and 1"), errors.toString());
        assertTrue(errors.contains("group.AC.kiosks cannot be negative"), errors.toString());
    }

    @Test
    void kiosksAreRequiredOnlyIfSomeoneUsesThem() throws IOException {
        Properties p = TestScenarios.settingsOf(TestScenarios.REAL_WORLD);
        p.setProperty("group.AC.kiosks", "0");
        p.setProperty("group.AC.kioskLines", "0");
        assertTrue(builder.check(new Scenario(p)).contains("group.AC.kiosks: at least one is needed"));
        p.setProperty("group.AC.kioskShare", "0");
        assertEquals(List.of(), builder.check(new Scenario(p)));
    }

    @Test
    void aMissingFileGivesAClearMessage() {
        IOException e = assertThrows(IOException.class, () -> Scenario.load(Path.of("scenarios/no_such_file.properties")));
        assertTrue(e.getMessage().startsWith("scenario file not found:"), e.getMessage());
    }

    @Test
    void theBuiltAirportHasTheCountsOfTheScenario() throws IOException {
        Airport a = builder.build(Scenario.load(TestScenarios.REAL_WORLD), new results.StatisticsCollector());
        Scenario s = Scenario.load(TestScenarios.REAL_WORLD);
        int kiosks = 0;
        int counters = 0;
        for (String g : List.of("AC", "SW", "US")) {
            kiosks += s.getInt("group." + g + ".kiosks");
            counters += s.getInt("group." + g + ".counters");
        }
        assertEquals(kiosks, countOf(a, ResourceType.SELF_CHECK_IN));
        assertEquals(counters, countOf(a, ResourceType.MANNED_CHECK_IN));
        assertEquals(s.getInt("security.lanes"), countOf(a, ResourceType.SECURITY));
        assertEquals(3, a.groups().size());
    }

    @Test
    void splitLinesShareOneReportCategory() throws IOException {
        Airport a = builder.build(Scenario.load(TestScenarios.TWO_LINES), new results.StatisticsCollector());
        long southwestCounterLines = a.lines().stream().filter(l -> l.category().equals("Southwest manned check-in")).count();
        assertEquals(2, southwestCounterLines, "two lines, reported together as in the paper");
    }

    private static long countOf(Airport a, ResourceType type) {
        return a.servicePoints().stream().map(ServicePoint::type).filter(t -> t == type).count();
    }
}
