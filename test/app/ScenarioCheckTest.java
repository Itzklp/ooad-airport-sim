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
import results.StatisticsCollector;

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
        Properties settings = TestScenarios.settingsOf(TestScenarios.REAL_WORLD);
        settings.setProperty("group.AC.share", "0.0013");
        settings.setProperty("group.SW.counterTime", "54 + EXPO(0)");
        settings.setProperty("group.US.counterLines", "5");
        List<String> problems = builder.check(new Scenario(settings));
        assertEquals(3, problems.size(), problems.toString());
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("shares add up to")), problems.toString());
        assertTrue(problems.stream().anyMatch(problem -> problem.startsWith("group.SW.counterTime")), problems.toString());
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("group.US.counterLines: more lines than service points")), problems.toString());
    }

    @Test
    void anEmptyScenarioListsEveryMissingValue() {
        List<String> problems = builder.check(new Scenario(new Properties()));
        for (String key : List.of("runs", "hours", "seed", "arrival", "checkin.lineRule", "groups",
                "security.lanes", "security.lineRule", "security.time", "security.stopShare")) {
            assertTrue(problems.stream().anyMatch(problem -> problem.startsWith(key)), key + " not reported in " + problems);
        }
    }

    @Test
    void wrongValuesAreReported() throws IOException {
        Properties settings = TestScenarios.settingsOf(TestScenarios.REAL_WORLD);
        settings.setProperty("runs", "0");
        settings.setProperty("seed", "abc");
        settings.setProperty("checkin.lineRule", "fastest");
        settings.setProperty("security.stopShare", "1.5");
        settings.setProperty("group.AC.kiosks", "-1");
        List<String> problems = builder.check(new Scenario(settings));
        assertTrue(problems.contains("runs must be at least 1"), problems.toString());
        assertTrue(problems.contains("seed must be a whole number"), problems.toString());
        assertTrue(problems.contains("checkin.lineRule must be random or shortest"), problems.toString());
        assertTrue(problems.contains("security.stopShare must be between 0 and 1"), problems.toString());
        assertTrue(problems.contains("group.AC.kiosks cannot be negative"), problems.toString());
    }

    @Test
    void kiosksAreRequiredOnlyIfSomeoneUsesThem() throws IOException {
        Properties settings = TestScenarios.settingsOf(TestScenarios.REAL_WORLD);
        settings.setProperty("group.AC.kiosks", "0");
        settings.setProperty("group.AC.kioskLines", "0");
        assertTrue(builder.check(new Scenario(settings)).contains("group.AC.kiosks: at least one is needed"));
        settings.setProperty("group.AC.kioskShare", "0");
        assertEquals(List.of(), builder.check(new Scenario(settings)));
    }

    @Test
    void aMissingFileGivesAClearMessage() {
        IOException failure = assertThrows(IOException.class, () -> Scenario.load(Path.of("scenarios/no_such_file.properties")));
        assertTrue(failure.getMessage().startsWith("scenario file not found:"), failure.getMessage());
    }

    @Test
    void theBuiltAirportHasTheCountsOfTheScenario() throws IOException {
        Scenario scenario = Scenario.load(TestScenarios.REAL_WORLD);
        Airport airport = builder.build(scenario, new StatisticsCollector());
        int kiosks = 0;
        int counters = 0;
        for (String key : List.of("AC", "SW", "US")) {
            kiosks += scenario.getInt("group." + key + ".kiosks");
            counters += scenario.getInt("group." + key + ".counters");
        }
        assertEquals(kiosks, countOf(airport, ResourceType.SELF_CHECK_IN));
        assertEquals(counters, countOf(airport, ResourceType.MANNED_CHECK_IN));
        assertEquals(scenario.getInt("security.lanes"), countOf(airport, ResourceType.SECURITY));
        assertEquals(3, airport.groups().size());
    }

    @Test
    void splitLinesShareOneReportCategory() throws IOException {
        Airport airport = builder.build(Scenario.load(TestScenarios.TWO_LINES), new StatisticsCollector());
        long southwestCounterLines = airport.lines().stream()
                .filter(line -> line.category().equals("Southwest manned check-in")).count();
        assertEquals(2, southwestCounterLines, "two lines, reported together as in the paper");
    }

    private static long countOf(Airport airport, ResourceType type) {
        return airport.servicePoints().stream().map(ServicePoint::type).filter(pointType -> pointType == type).count();
    }
}
