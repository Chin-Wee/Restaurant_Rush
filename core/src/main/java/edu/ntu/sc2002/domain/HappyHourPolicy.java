package edu.ntu.sc2002.domain;

/**
 * Stage 3 pricing policy. It can be switched on once per game, lasts for the
 * activation turn and the next turn, applies 20% off to orders taken while
 * active, and recovers 15 satisfaction for customers still waiting.
 */
public final class HappyHourPolicy {
    private static final int DISCOUNT_PERCENT = 20;
    private static final int RECOVERY_POINTS = 15;

    private int remainingTurns = 0;
    private HappyHourStatus status = HappyHourStatus.AVAILABLE;

    public HappyHourPolicy() {}

    /**
     * Activates the once-per-game policy. It remains active for the activation turn and the next.
     *
     * @param currentTurn turn on which activation is requested
     * @return true if activation succeeded; false if already active or exhausted
     * @throws IllegalArgumentException if {@code currentTurn} is less than one
     */
    public boolean activate(int currentTurn) {
        if (currentTurn < 1) throw new IllegalArgumentException("Invalid turn");
        if (status != HappyHourStatus.AVAILABLE) return false;
        status = HappyHourStatus.ACTIVE;
        remainingTurns = 2;
        return true;
    }
    public boolean isActive() { return status == HappyHourStatus.ACTIVE; }
    /** Called once at the end of every turn; the policy is exhausted after its last active turn. */
    public void decrementTurn() {
        if (status != HappyHourStatus.ACTIVE) return;
        remainingTurns--;
        if (remainingTurns <= 0) {
            remainingTurns = 0;
            status = HappyHourStatus.EXHAUSTED;
        }
    }
    public int getRemainingTurns() { return remainingTurns; }
    public HappyHourStatus getStatus() { return status; }
    /**
     * Applies the 20% Happy Hour discount after any customer-specific discount.
     *
     * @param priceCents pre-Happy-Hour price in cents
     * @return discounted price rounded to the nearest cent
     * @throws IllegalArgumentException if {@code priceCents} is negative
     */
    public int applyHappyHourDiscount(int priceCents) {
        if (priceCents < 0) throw new IllegalArgumentException("Price must be nonnegative");
        return (int) (((long) priceCents * (100 - DISCOUNT_PERCENT) + 50) / 100);
    }

    /** @return satisfaction recovered by each waiting/seated customer at the start of an active turn */
    public int recoveryAmount() { return RECOVERY_POINTS; }
}
