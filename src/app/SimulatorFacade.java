package app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import results.ExperimentResult;

public class SimulatorFacade {

    private final AirportBuilder builder = new AirportBuilder();
    private final ExperimentRunner runner = new ExperimentRunner(builder);
    private Scenario current;
    private ExperimentResult lastResult;

    // A scenario with problems is not loaded; the previous one stays current.
    public List<String> loadScenario(String file) throws IOException {
        Scenario scenario = Scenario.load(Path.of(file));
        List<String> problems = builder.check(scenario);
        if (problems.isEmpty()) current = scenario;
        return problems;
    }

    public ExperimentResult run(IntConsumer progress) {
        if (current == null) throw new IllegalStateException("Set up a scenario first.");
        lastResult = runner.run(current, progress);
        return lastResult;
    }

    public String showResults() {
        return lastResult == null ? "No results yet. Run the simulation first." : lastResult.toText();
    }

    public void saveCsv(String file) throws IOException {
        if (lastResult == null) throw new IllegalStateException("No results to save.");
        Files.writeString(Path.of(file), lastResult.toCsv());
    }

    public String compare(List<String> files) throws IOException {
        List<ExperimentResult> results = new ArrayList<>();
        for (String file : files) {
            results.add(runner.run(Scenario.load(Path.of(file)), finishedRuns -> { }));
        }
        return ExperimentResult.compare(results);
    }

    public String currentScenarioName() {
        return current == null ? "none" : current.name();
    }
}
