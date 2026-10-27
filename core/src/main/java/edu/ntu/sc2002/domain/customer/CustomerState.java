package edu.ntu.sc2002.domain.customer;

public enum CustomerState {
    QUEUED, SEATED, EATING, READY_TO_PAY, PAID, ABANDONED;

    /** Compatibility names used by the existing scene. These are aliases, not additional states. */
    public static final CustomerState WAITING = QUEUED;
    public static final CustomerState LEFT = ABANDONED;
}
