package app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import results.ExperimentResult;

/** The one class the console menu talks to: one method for each use case. (Facade, GRASP Controller) */
public class SimulatorFacade {

    private final AirportBuilder builder = new AirportBuilder();
    private final ExperimentRunner runner = new ExperimentRunner(builder);
    private Scenario current;
    private ExperimentResult lastResult;

    /** Set up a scenario (includes Check the scenario). Returns the problems found; empty means loaded. */
    public List<String> loadScenario(String file) throws IOException {
        Scenario s = Scenario.load(Path.of(file));
        List<String> errors = builder.check(s);
        if (errors.isEmpty()) current = s;
        return errors;
    }

    /** Run the simulation. */
    public ExperimentResult run(IntConsumer progress) {
        if (current == null) throw new IllegalStateException("Set up a scenario first.");
        lastResult = runner.run(current, progress);
        return lastResult;
    }

    /** View results. */
    public String showResults() {
        return lastResult == null ? "No results yet. Run the simulation first." : lastResult.toText();
    }

    /** Save results to a CSV file (extends View results). */
    public void saveCsv(String file) throws IOException {
        if (lastResult == null) throw new IllegalStateException("No results to save.");
        Files.writeString(Path.of(file), lastResult.toCsv());
    }

    /** Compare scenarios (includes Run the simulation for each one). */
    public String compare(List<String> files) throws IOException {
        List<ExperimentResult> results = new ArrayList<>();
        for (String f : files) {
            results.add(runner.run(Scenario.load(Path.of(f)), n -> { }));
        }
        return ExperimentResult.compare(results);
    }

    public String currentScenarioName() {
        return current == null ? "none" : current.name();
    }
}
