package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import results.ExperimentResult;

class SimulatorFacadeTest {

    private final SimulatorFacade facade = new SimulatorFacade();

    @TempDir
    Path tempFolder;

    @Test
    void runningBeforeSettingUpAScenarioIsRefused() {
        IllegalStateException refusal = assertThrows(IllegalStateException.class, () -> facade.run(finishedRuns -> { }));
        assertEquals("Set up a scenario first.", refusal.getMessage());
        assertEquals("none", facade.currentScenarioName());
    }

    @Test
    void aGoodScenarioBecomesTheCurrentOne() throws IOException {
        assertEquals(List.of(), facade.loadScenario(TestScenarios.REAL_WORLD.toString()));
        assertEquals("Real world", facade.currentScenarioName());
    }

    @Test
    void aWrongScenarioIsReportedAndThePreviousOneIsKept() throws IOException {
        facade.loadScenario(TestScenarios.REAL_WORLD.toString());
        Properties settings = TestScenarios.settingsOf(TestScenarios.REAL_WORLD);
        settings.setProperty("name", "Broken");
        settings.setProperty("security.lanes", "0");
        Path brokenFile = tempFolder.resolve("broken.properties");
        try (Writer writer = Files.newBufferedWriter(brokenFile)) {
            settings.store(writer, null);
        }
        List<String> problems = facade.loadScenario(brokenFile.toString());
        assertFalse(problems.isEmpty());
        assertEquals("Real world", facade.currentScenarioName());
    }

    @Test
    void resultsCanBeViewedAndSavedAfterARun() throws IOException {
        assertEquals("No results yet. Run the simulation first.", facade.showResults());
        assertThrows(IllegalStateException.class, () -> facade.saveCsv(tempFolder.resolve("x.csv").toString()));

        facade.loadScenario(TestScenarios.REAL_WORLD.toString());
        ExperimentResult result = facade.run(finishedRuns -> { });
        assertEquals(result.toText(), facade.showResults());

        Path csvFile = tempFolder.resolve("real_world.csv");
        facade.saveCsv(csvFile.toString());
        assertEquals(result.toCsv(), Files.readString(csvFile));
    }

    @Test
    void compareRunsEachScenario() throws IOException {
        String table = facade.compare(List.of(TestScenarios.REAL_WORLD.toString(), TestScenarios.TWO_LINES.toString()));
        String header = table.lines().findFirst().orElseThrow();
        assertTrue(header.contains("Real world") && header.contains("Two lines"), header);
        assertTrue(table.contains("Utilization (%)"));
    }
}
