package sim;

import java.util.PriorityQueue;
import java.util.Random;

public class Simulator {

    private double clock = 0.0;
    private final PriorityQueue<Event> events = new PriorityQueue<>();
    private final Random rng;
    private long nextSeq = 0;

    public Simulator(long seed) {
        this.rng = new Random(seed);
    }

    public void schedule(Event event) {
        if (event.time() < clock) {
            throw new IllegalArgumentException("Event at " + event.time() + " is before the clock " + clock);
        }
        event.setSeq(nextSeq++);
        events.add(event);
    }

    public void run(double endTime) {
        while (!events.isEmpty() && events.peek().time() <= endTime) {
            Event next = events.poll();
            clock = next.time();
            next.execute(this);
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
