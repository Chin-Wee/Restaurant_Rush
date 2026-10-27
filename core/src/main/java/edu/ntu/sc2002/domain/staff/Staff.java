package edu.ntu.sc2002.domain.staff;

/** One successful task per staff member per turn, including waiting. */
public abstract class Staff {
    private final String staffId;
    private boolean hasActedThisTurn;
    private int currentTurn;

    public Staff(String staffId) {
        if (staffId == null || staffId.isBlank()) throw new IllegalArgumentException("Invalid staff ID");
        this.staffId = staffId;
    }
    public boolean hasActed() { return hasActedThisTurn; }
    public void resetTurn() { hasActedThisTurn = false; }
    public void startTurn(int turn) {
        if (turn < 1 || turn <= currentTurn) throw new IllegalArgumentException("Staff turn must advance");
        currentTurn = turn;
        resetTurn();
    }
    public void waitTurn() { requireAvailable(); completeAction(); }
    protected int currentTurn() { return currentTurn; }
    protected void requireAvailable() {
        if (hasActedThisTurn) throw new IllegalStateException(staffId + " has already acted this turn");
    }
    protected void completeAction() { hasActedThisTurn = true; }
}
