package airport;

public enum ResourceType {
    MANNED_CHECK_IN("Manned check-in"),
    SELF_CHECK_IN("Self check-in"),
    SECURITY("Security checkpoint");

    private final String label;

    ResourceType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
