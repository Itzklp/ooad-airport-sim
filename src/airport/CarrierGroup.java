package airport;

/** One of the paper's airline groups: American/Continental, Southwest or US Airways. */
public class CarrierGroup {

    private final String name;
    private final double share;       // share of all arriving passengers
    private final double kioskShare;  // share of this group's passengers who use self check-in

    public CarrierGroup(String name, double share, double kioskShare) {
        this.name = name;
        this.share = share;
        this.kioskShare = kioskShare;
    }

    public String name() {
        return name;
    }

    public double share() {
        return share;
    }

    public double kioskShare() {
        return kioskShare;
    }
}
