package sim;

/**
 * Something that happens at one moment of simulated time.
 * Subclasses say what happens in execute(). (Command pattern)
 */
public abstract class Event implements Comparable<Event> {

    private final double time;
    private long seq;   // set by the Simulator; keeps events with equal times in scheduling order

    protected Event(double time) {
        this.time = time;
    }

    public double time() {
        return time;
    }

    void setSeq(long seq) {
        this.seq = seq;
    }

    /** Carries out the event. The simulator is passed in so the event can read the clock and schedule more events. */
    public abstract void execute(Simulator sim);

    @Override
    public int compareTo(Event other) {
        int byTime = Double.compare(time, other.time);
        return byTime != 0 ? byTime : Long.compare(seq, other.seq);
    }
}
