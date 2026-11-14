package edu.ntu.sc2002.domain.staff;

/** Staff identity. TurnController sequences the single task for each role. */
public abstract class Staff {
    private final String staffId;

    /**
     * Creates a staff identity.
     *
     * @param staffId non-blank role identifier
     * @throws IllegalArgumentException if the ID is null or blank
     */
    public Staff(String staffId) {
        if (staffId == null || staffId.isBlank()) throw new IllegalArgumentException("Invalid staff ID");
        this.staffId = staffId;
    }
}
