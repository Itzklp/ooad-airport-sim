# Airport Passenger Flow Simulator

Java re-implementation of the passenger flow model in Brown and Madhavan (2011),
"Examining Passenger Flow Choke Points at Airports Using Discrete Event Simulation".
SS G514 OOAD project, Area 42 (Simulation), 2025H1030209P.

The program follows the D1 design: 36 classes in 4 packages, built bottom-up
(sim, then airport, then results, then app), each package with its JUnit 5 tests.

## Requirements
JDK 17 or later. The main program uses only the standard Java library.
The tests use JUnit 5, which is included as a single jar in `lib/` (no Maven or Gradle needed).

## Build and test
Windows:
```
build.bat
```
Linux, macOS or Git Bash:
```
sh build.sh
```
Both compile the program into `out/`, compile the tests, and run all JUnit tests.
The same steps by hand (on Windows use `;` instead of `:` in the class path):
```
javac --release 17 -d out src/sim/*.java src/airport/*.java src/results/*.java src/app/*.java
javac --release 17 -d out -cp "out:lib/junit-platform-console-standalone-1.10.5.jar" test/sim/*.java test/airport/*.java test/results/*.java test/app/*.java
java -jar lib/junit-platform-console-standalone-1.10.5.jar execute -cp out --scan-classpath
```

## Run
```
java -cp out app.ConsoleMenu                                   # interactive menu
java -cp out app.ConsoleMenu run scenarios/real_world.properties
java -cp out app.ConsoleMenu compare scenarios/real_world.properties scenarios/more_stations.properties scenarios/two_lines.properties
```

## Packages
| Package | Classes | What it does |
|---|---|---|
| `sim` | 8 | General discrete event engine and random distributions. Uses no other package. |
| `airport` | 19 | The airport model: passengers, lines, kiosks, counters, security, events. Uses `sim`. |
| `results` | 4 | Statistics of a run and of a whole experiment. Uses `airport`. |
| `app` | 5 | Scenario files, building the airport, running experiments, console menu. |

## Tests
| Folder | Test classes | What they check |
|---|---|---|
| `test/sim` | SimulatorTest, DistributionTest, DistributionFactoryTest | event order and ties, clock rules, mean and spread of every distribution, the parser |
| `test/airport` | PassengerStateTest, WaitingLineTest, ServicePointTest, LineChoiceTest, FlowTest, PassengerSourceTest | both state diagrams, business rules 1 to 4, stops, routing, passenger creation |
| `test/results` | WaitStatsTest, StatisticsCollectorTest, ReportTest | statistics of one run, combining runs, text, CSV and comparison reports |
| `test/app` | ScenarioCheckTest, RepeatabilityTest, QueueTheoryTest, SimulatorFacadeTest, PaperScenarioTest, ConsoleMenuTest | scenario checks, same seed same results, the M/M/1 known answer, every use case |

## Important
The paper prints all its distribution parameters as zeros and does not give the number of
kiosks and counters, the kiosk shares or the stop settings. Every such value in
`scenarios/*.properties` is an assumption of this project, not a value from the paper, and
each report prints a note saying so. All times are in seconds; reports show minutes.
