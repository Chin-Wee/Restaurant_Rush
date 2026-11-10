package edu.ntu.sc2002.domain;

import edu.ntu.sc2002.domain.menu.MenuItem;
import java.util.Objects;

/**
 * A customer's dish, its locked bill and its preparation/service lifecycle. Turn stamps make the
 * "serve only after it was READY" and "eat for one full turn" rules enforceable independently of
 * controller call order.
 */
public final class Order {
    private final MenuItem menuItem;
    private final int lockedPriceCents;
    private int unitsDone;
    private OrderStatus status = OrderStatus.PLACED;
    private Integer readyTurn;
    private Integer servedTurn;

    /**
     * Creates an order in {@code PLACED} state with zero preparation units completed.
     *
     * @param item ordered menu item
     * @param lockedPriceCents final bill after customer and policy discounts
     * @throws IllegalArgumentException if the price is negative
     */
    public Order(MenuItem item, int lockedPriceCents) {
        menuItem = Objects.requireNonNull(item);
        if (lockedPriceCents < 0) throw new IllegalArgumentException("Price must be nonnegative");
        this.lockedPriceCents = lockedPriceCents;
    }
    public MenuItem getMenuItem() { return menuItem; }
    public int lockedPriceCents() { return lockedPriceCents; }
    public int unitsDone() { return unitsDone; }
    public Integer servedTurn() { return servedTurn; }
    public OrderStatus getStatus() { return status; }
    public boolean isReady() { return status == OrderStatus.READY; }

    /**
     * Adds one preparation unit and records the turn the order becomes {@code READY}.
     *
     * @param turn current game turn
     * @throws IllegalArgumentException if the turn is invalid
     * @throws IllegalStateException if the order is already ready, served, paid or cancelled
     */
    public void advance(int turn) {
        if (turn < 1) throw new IllegalArgumentException("Invalid preparation turn");
        if (status != OrderStatus.PLACED && status != OrderStatus.PREPARING) {
            throw new IllegalStateException("Order is not being prepared");
        }
        unitsDone++;
        status = unitsDone == menuItem.getPrepUnitsNeeded() ? OrderStatus.READY : OrderStatus.PREPARING;
        if (isReady()) readyTurn = turn;
    }

    /**
     * Reports whether the order may be served on the current turn. The order must have been
     * {@code READY} before this turn, so a dish finished this turn waits until the next turn.
     *
     * @param turn current game turn
     * @return true when the order is ready and an earlier turn completed preparation
     */
    public boolean canServe(int turn) { return isReady() && readyTurn < turn; }

    /**
     * Marks the order served and records the serving turn.
     *
     * @param turn current game turn
     * @throws IllegalStateException if {@link #canServe(int)} is false
     */
    public void serve(int turn) {
        if (!canServe(turn)) throw new IllegalStateException("Order must be ready before this turn");
        status = OrderStatus.SERVED;
        servedTurn = turn;
    }

    /**
     * Marks a served order as paid. Payment eligibility and the one-turn delay are validated by
     * {@link edu.ntu.sc2002.domain.customer.Customer} and the Cashier.
     *
     * @throws IllegalStateException if the order has not been served
     */
    public void markPaid() {
        if (status != OrderStatus.SERVED) throw new IllegalStateException("Order is not served");
        status = OrderStatus.PAID;
    }

    /**
     * Cancels an unfinished order. Served and paid orders cannot be cancelled.
     *
     * @throws IllegalStateException if the order is already served, paid or cancelled
     */
    public void cancel() {
        if (status != OrderStatus.PLACED && status != OrderStatus.PREPARING && status != OrderStatus.READY) {
            throw new IllegalStateException("Only unserved orders can be cancelled");
        }
        status = OrderStatus.CANCELLED;
    }
}
