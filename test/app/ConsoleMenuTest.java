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

class ConsoleMenuTest {

    private final PrintStream realOutput = System.out;
    private final InputStream realInput = System.in;
    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();

    @BeforeEach
    void captureOutput() {
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restore() {
        System.setOut(realOutput);
        System.setIn(realInput);
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
        String keystrokes = "2\n1\nscenarios/real_world.properties\n2\n9\n0\n";
        System.setIn(new ByteArrayInputStream(keystrokes.getBytes(StandardCharsets.UTF_8)));
        ConsoleMenu.main(new String[0]);
        assertTrue(output().contains("Error: Set up a scenario first."), "run before load: " + output());
        assertTrue(output().contains("(scenario: Real world)"), output());
        assertTrue(output().contains("Run 80 finished"), output());
        assertTrue(output().contains("Please choose 0 to 5."), output());
    }
}
