package sim;

import java.util.PriorityQueue;
import java.util.Random;

/**
 * The discrete event engine: a clock, a list of future events sorted by time,
 * and the random number stream of one run. It knows nothing about airports.
 */
public class Simulator {

    private double clock = 0.0;
    private final PriorityQueue<Event> events = new PriorityQueue<>();
    private final Random rng;
    private long nextSeq = 0;

    public Simulator(long seed) {
        this.rng = new Random(seed);
    }

    /** Adds an event to the list. An event in the past is refused, so the clock never goes back. */
    public void schedule(Event e) {
        if (e.time() < clock) {
            throw new IllegalArgumentException("Event at " + e.time() + " is before the clock " + clock);
        }
        e.setSeq(nextSeq++);
        events.add(e);
    }

    /** Repeatedly takes the earliest event, moves the clock to it and executes it, until endTime. */
    public void run(double endTime) {
        while (!events.isEmpty() && events.peek().time() <= endTime) {
            Event e = events.poll();
            clock = e.time();
            e.execute(this);
        }
        clock = endTime;
    }

    public double now() {
        return clock;
    }

    public Random rng() {
        return rng;
    }
}
