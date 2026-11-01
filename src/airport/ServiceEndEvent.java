package airport;

import sim.Event;
import sim.Simulator;

/** A kiosk, counter or screening lane finishes serving its current passenger. */
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
