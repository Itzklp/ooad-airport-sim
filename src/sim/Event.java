package sim;

public abstract class Event implements Comparable<Event> {

    private final double time;
    private long seq;   // set by the Simulator, so events at the same time run in the order they were scheduled

    protected Event(double time) {
        this.time = time;
    }

    public double time() {
        return time;
    }

    void setSeq(long seq) {
        this.seq = seq;
    }

    public abstract void execute(Simulator sim);

    @Override
    public int compareTo(Event other) {
        int byTime = Double.compare(time, other.time);
        return byTime != 0 ? byTime : Long.compare(seq, other.seq);
    }
}
