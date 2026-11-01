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

/** Phase 3: one test per use case of the facade. Covers requirement 10. */
class SimulatorFacadeTest {

    private final SimulatorFacade facade = new SimulatorFacade();

    @TempDir
    Path tmp;

    @Test
    void runningBeforeSettingUpAScenarioIsRefused() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> facade.run(n -> { }));
        assertEquals("Set up a scenario first.", e.getMessage());
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
        Properties p = TestScenarios.settingsOf(TestScenarios.REAL_WORLD);
        p.setProperty("name", "Broken");
        p.setProperty("security.lanes", "0");
        Path broken = tmp.resolve("broken.properties");
        try (Writer out = Files.newBufferedWriter(broken)) {
            p.store(out, null);
        }
        List<String> errors = facade.loadScenario(broken.toString());
        assertFalse(errors.isEmpty());
        assertEquals("Real world", facade.currentScenarioName());
    }

    @Test
    void resultsCanBeViewedAndSavedAfterARun() throws IOException {
        assertEquals("No results yet. Run the simulation first.", facade.showResults());
        assertThrows(IllegalStateException.class, () -> facade.saveCsv(tmp.resolve("x.csv").toString()));

        facade.loadScenario(TestScenarios.REAL_WORLD.toString());
        ExperimentResult r = facade.run(n -> { });
        assertEquals(r.toText(), facade.showResults());

        Path csv = tmp.resolve("real_world.csv");
        facade.saveCsv(csv.toString());
        assertEquals(r.toCsv(), Files.readString(csv));
    }

    @Test
    void compareRunsEachScenario() throws IOException {
        String table = facade.compare(List.of(TestScenarios.REAL_WORLD.toString(), TestScenarios.TWO_LINES.toString()));
        String header = table.lines().findFirst().orElseThrow();
        assertTrue(header.contains("Real world") && header.contains("Two lines"), header);
        assertTrue(table.contains("Utilization (%)"));
    }
}
