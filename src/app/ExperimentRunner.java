package app;

import java.util.List;
import java.util.function.IntConsumer;

import airport.Airport;
import airport.ArrivalEvent;
import results.ExperimentResult;
import results.StatisticsCollector;
import sim.Simulator;

public class ExperimentRunner {

    private final AirportBuilder builder;

    public ExperimentRunner(AirportBuilder builder) {
        this.builder = builder;
    }

    public ExperimentResult run(Scenario scenario, IntConsumer progress) {
        List<String> problems = builder.check(scenario);
        if (!problems.isEmpty()) throw new IllegalArgumentException(String.join("\n", problems));

        ExperimentResult result = new ExperimentResult(scenario.name(), scenario.note());
        double endTime = scenario.hours() * 3600.0;   // all times in the model are in seconds
        for (int runIndex = 0; runIndex < scenario.runs(); runIndex++) {
            // every run gets its own seed, clock, airport and collector, so runs cannot affect each other
            Simulator sim = new Simulator(scenario.seed() + runIndex);
            StatisticsCollector stats = new StatisticsCollector();
            Airport airport = builder.build(scenario, stats);
            sim.schedule(new ArrivalEvent(airport.source().nextArrivalTime(sim), airport));
            sim.run(endTime);
            result.add(stats.summary(airport, endTime));
            progress.accept(runIndex + 1);
        }
        return result;
    }
}
