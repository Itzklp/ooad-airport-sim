package results;

public class WaitStats {

    private long count = 0;
    private double total = 0.0;
    private double min = Double.POSITIVE_INFINITY;
    private double max = 0.0;

    public void add(double wait) {
        count++;
        total += wait;
        min = Math.min(min, wait);
        max = Math.max(max, wait);
    }

    public long count() {
        return count;
    }

    public double average() {
        return count == 0 ? 0.0 : total / count;
    }

    public double min() {
        return count == 0 ? 0.0 : min;
    }

    public double max() {
        return max;
    }
}
