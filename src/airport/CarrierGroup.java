package airport;

public class CarrierGroup {

    private final String name;
    private final double share;
    private final double kioskShare;   // share of this airline's passengers who use self check-in

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
