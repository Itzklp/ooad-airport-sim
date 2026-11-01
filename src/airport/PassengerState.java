package airport;

/** The states of a passenger (see the state diagram) and the moves allowed between them. */
public enum PassengerState {
    ARRIVED,
    WAITING_FOR_CHECK_IN,
    CHECKING_IN,
    WAITING_FOR_SCREENING,
    BEING_SCREENED,
    STOPPED,
    DEPARTED;

    /** True if the state diagram has an arrow from this state to next. */
    public boolean canMoveTo(PassengerState next) {
        return switch (this) {
            case ARRIVED -> next == WAITING_FOR_CHECK_IN;
            case WAITING_FOR_CHECK_IN -> next == CHECKING_IN;
            case CHECKING_IN -> next == WAITING_FOR_SCREENING;
            case WAITING_FOR_SCREENING -> next == BEING_SCREENED;
            case BEING_SCREENED -> next == STOPPED || next == DEPARTED;
            case STOPPED -> next == DEPARTED;
            case DEPARTED -> false;
        };
    }
}
