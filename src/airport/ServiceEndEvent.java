package airport;

import sim.Event;
import sim.Simulator;

public class ServiceEndEvent extends Event {

    private final ServicePoint point;

    public ServiceEndEvent(double time, ServicePoint point) {
        super(time);
        this.point = point;
    }

    @Override
    public void execute(Simulator sim) {
        point.finishService(sim);
    }
}
