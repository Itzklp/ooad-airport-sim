package app;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Phase 3: the console menu, driven the way a user would drive it. */
class ConsoleMenuTest {

    private final PrintStream realOut = System.out;
    private final InputStream realIn = System.in;
    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();

    @BeforeEach
    void captureOutput() {
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restore() {
        System.setOut(realOut);
        System.setIn(realIn);
    }

    private String output() {
        return captured.toString(StandardCharsets.UTF_8);
    }

    @Test
    void runCommandPrintsTheReport() {
        ConsoleMenu.main(new String[] {"run", "scenarios/real_world.properties"});
        assertTrue(output().contains("Loaded scenario: Real world"), output());
        assertTrue(output().contains("Run 80 finished"), output());
        assertTrue(output().contains("Waiting time (minutes)"), output());
    }

    @Test
    void compareCommandPrintsOneColumnPerScenario() {
        ConsoleMenu.main(new String[] {"compare", "scenarios/real_world.properties", "scenarios/more_stations.properties"});
        String header = output().lines().findFirst().orElse("");
        assertTrue(header.contains("Real world") && header.contains("More stations"), output());
    }

    @Test
    void aMissingFileIsReportedNotThrown() {
        ConsoleMenu.main(new String[] {"run", "scenarios/no_such_file.properties"});
        assertTrue(output().contains("Error: scenario file not found:"), output());
    }

    @Test
    void theMenuLoadsRunsAndExits() {
        String keys = "2\n1\nscenarios/real_world.properties\n2\n9\n0\n";
        System.setIn(new ByteArrayInputStream(keys.getBytes(StandardCharsets.UTF_8)));
        ConsoleMenu.main(new String[0]);
        assertTrue(output().contains("Error: Set up a scenario first."), "run before load: " + output());
        assertTrue(output().contains("(scenario: Real world)"), output());
        assertTrue(output().contains("Run 80 finished"), output());
        assertTrue(output().contains("Please choose 0 to 5."), output());
    }
}
