package edu.ntu.sc2002.domain;

import edu.ntu.sc2002.domain.menu.MenuItem;
import java.util.Objects;

/** A customer's dish and the bill locked when that customer ordered it. */
public final class Order {
    private final MenuItem menuItem;
    private final int lockedPriceCents;
    private int unitsDone;
    private OrderStatus status = OrderStatus.PLACED;
    private Integer readyTurn;
    private Integer servedTurn;

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

    public void advance(int turn) {
        if (turn < 1) throw new IllegalArgumentException("Invalid preparation turn");
        if (status != OrderStatus.PLACED && status != OrderStatus.PREPARING) {
            throw new IllegalStateException("Order is not being prepared");
        }
        unitsDone++;
        status = unitsDone == menuItem.getPrepUnitsNeeded() ? OrderStatus.READY : OrderStatus.PREPARING;
        if (isReady()) readyTurn = turn;
    }
    public boolean canServe(int turn) { return isReady() && readyTurn < turn; }
    public void serve(int turn) {
        if (!canServe(turn)) throw new IllegalStateException("Order must be ready before this turn");
        status = OrderStatus.SERVED;
        servedTurn = turn;
    }
    /** Eligibility and the payment delay are checked by Customer and Cashier. */
    public void markPaid() {
        if (status != OrderStatus.SERVED) throw new IllegalStateException("Order is not served");
        status = OrderStatus.PAID;
    }
    public void cancel() {
        if (status != OrderStatus.PLACED && status != OrderStatus.PREPARING && status != OrderStatus.READY) {
            throw new IllegalStateException("Only unserved orders can be cancelled");
        }
        status = OrderStatus.CANCELLED;
    }
}
