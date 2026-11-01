package app;

import java.util.List;
import java.util.function.IntConsumer;

import airport.Airport;
import airport.ArrivalEvent;
import results.ExperimentResult;
import results.StatisticsCollector;
import sim.Simulator;

/** Runs a scenario many times. Every run gets a new Simulator, airport and collector. */
public class ExperimentRunner {

    private final AirportBuilder builder;

    public ExperimentRunner(AirportBuilder builder) {
        this.builder = builder;
    }

    /**
     * Runs all the runs of the scenario. The progress callback is told after each run.
     * Throws IllegalArgumentException listing the problems if the scenario is not valid.
     */
    public ExperimentResult run(Scenario s, IntConsumer progress) {
        List<String> errors = builder.check(s);
        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join("\n", errors));

        ExperimentResult result = new ExperimentResult(s.name(), s.note());
        double endTime = s.hours() * 3600.0;   // all times are in seconds
        for (int i = 0; i < s.runs(); i++) {
            Simulator sim = new Simulator(s.seed() + i);
            StatisticsCollector stats = new StatisticsCollector();
            Airport airport = builder.build(s, stats);
            sim.schedule(new ArrivalEvent(airport.source().nextArrivalTime(sim), airport));
            sim.run(endTime);
            result.add(stats.summary(airport, endTime));
            progress.accept(i + 1);
        }
        return result;
    }
}
