package edu.ntu.sc2002.domain.staff;

/** Staff identity. TurnController sequences the single task for each role. */
public abstract class Staff {
    private final String staffId;

    public Staff(String staffId) {
        if (staffId == null || staffId.isBlank()) throw new IllegalArgumentException("Invalid staff ID");
        this.staffId = staffId;
    }
}
