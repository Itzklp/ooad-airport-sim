package app;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

// Usage:  java app.ConsoleMenu                       (interactive menu)
//         java app.ConsoleMenu run FILE              (run one scenario and print the results)
//         java app.ConsoleMenu compare FILE FILE ... (run and compare scenarios)
public class ConsoleMenu {

    private final SimulatorFacade facade = new SimulatorFacade();
    private final Scanner input = new Scanner(System.in);

    public static void main(String[] args) {
        ConsoleMenu menu = new ConsoleMenu();
        try {
            if (args.length >= 2 && args[0].equals("run")) {
                menu.load(args[1]);
                menu.runCurrent();
            } else if (args.length >= 3 && args[0].equals("compare")) {
                System.out.println(menu.facade.compare(Arrays.asList(args).subList(1, args.length)));
            } else {
                menu.loop();
            }
        } catch (IOException | RuntimeException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void loop() {
        while (true) {
            System.out.println("\nAirport Passenger Flow Simulator  (scenario: " + facade.currentScenarioName() + ")");
            System.out.println("1. Set up a scenario   2. Run the simulation   3. View results");
            System.out.println("4. Save results to CSV 5. Compare scenarios    0. Exit");
            System.out.print("> ");
            String choice = input.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> load(ask("Scenario file: "));
                    case "2" -> runCurrent();
                    case "3" -> System.out.println(facade.showResults());
                    case "4" -> {
                        String file = ask("CSV file name: ");
                        facade.saveCsv(file);
                        System.out.println("Saved to " + file);
                    }
                    case "5" -> {
                        List<String> files = Arrays.asList(ask("Scenario files, separated by spaces: ").split("\\s+"));
                        System.out.println(facade.compare(files));
                    }
                    case "0" -> {
                        return;
                    }
                    default -> System.out.println("Please choose 0 to 5.");
                }
            } catch (IOException | RuntimeException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void load(String file) throws IOException {
        List<String> problems = facade.loadScenario(file);
        if (problems.isEmpty()) {
            System.out.println("Loaded scenario: " + facade.currentScenarioName());
        } else {
            System.out.println("The scenario has problems:");
            problems.forEach(problem -> System.out.println("  - " + problem));
        }
    }

    private void runCurrent() {
        facade.run(finishedRuns -> System.out.print("\rRun " + finishedRuns + " finished"));
        System.out.println();
        System.out.println(facade.showResults());
    }

    private String ask(String prompt) {
        System.out.print(prompt);
        return input.nextLine().trim();
    }
}
