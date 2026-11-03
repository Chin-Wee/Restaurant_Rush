package edu.ntu.sc2002.domain.customer;

import edu.ntu.sc2002.domain.Order;
import edu.ntu.sc2002.domain.menu.MenuItem;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Shared order and service flow; each customer type supplies its own decay, billing and dish choice. */
public abstract class Customer {
    private final String id;
    private int satisfaction = 100;
    private CustomerState state = CustomerState.WAITING;
    private final int arrivalOrdinal;
    private Order order;
    private final long choiceSeed;
    private Integer readyToPayTurn;

    protected Customer(String id, int arrivalOrdinal, long choiceSeed) {
        if (id == null || id.isBlank() || arrivalOrdinal < 1) throw new IllegalArgumentException("Invalid customer identity");
        this.id = id;
        this.arrivalOrdinal = arrivalOrdinal;
        this.choiceSeed = choiceSeed;
    }
    public String getId() { return id; }
    public int getSatisfaction() { return satisfaction; }
    public CustomerState getState() { return state; }
    public int arrivalOrdinal() { return arrivalOrdinal; }
    public Order order() { return order; }
    /** Display only: behaviour is dispatched through the overridden methods, never by checking the type. */
    public abstract CustomerType getType();
    public abstract void applyTurnEndDecay();
    public abstract int calculateBill(int basePriceCents);
    public abstract MenuItem chooseMenuItem(List<MenuItem> availableItems);

    /** The shared decay rule: only customers waiting for a table or food lose satisfaction. */
    protected final void loseSatisfactionIfWaiting(int points) {
        if (state == CustomerState.WAITING || state == CustomerState.SEATED) deductSatisfaction(points);
    }
    /** Reproducible random dish: the same seed always picks the same index. */
    protected MenuItem seededChoice(List<MenuItem> availableItems) {
        Objects.requireNonNull(availableItems);
        if (availableItems.isEmpty()) throw new IllegalStateException("Menu is empty");
        return availableItems.get(new Random(choiceSeed).nextInt(availableItems.size()));
    }
    public void deductSatisfaction(int points) {
        if (points < 0) throw new IllegalArgumentException("Points must be nonnegative");
        satisfaction = (int) Math.max(0L, (long) satisfaction - points);
    }
    public void addSatisfaction(int points) {
        if (points < 0) throw new IllegalArgumentException("Points must be nonnegative");
        satisfaction = (int) Math.min(100L, (long) satisfaction + points);
    }
    public void markSeated() {
        if (state != CustomerState.WAITING) throw new IllegalStateException("Customer is not waiting");
        state = CustomerState.SEATED;
    }
    public Order placeOrder(MenuItem item, int lockedPriceCents) {
        if (state != CustomerState.SEATED || order != null) throw new IllegalStateException("Customer cannot order");
        order = new Order(item, lockedPriceCents);
        return order;
    }
    public void serveOrder(int turn) {
        if (state != CustomerState.SEATED || order == null) throw new IllegalStateException("Customer cannot be served");
        order.serve(turn);
        state = CustomerState.EATING;
    }
    public void finishEating(int turn) {
        if (state == CustomerState.EATING && order.servedTurn() < turn) {
            state = CustomerState.READY_TO_PAY;
            readyToPayTurn = turn;
        }
    }
    public boolean canPay(int turn) { return state == CustomerState.READY_TO_PAY && readyToPayTurn < turn; }
    /** Cashier checks canPay(turn) before committing payment. */
    public void paid() {
        if (state != CustomerState.READY_TO_PAY) throw new IllegalStateException("Customer is not ready to pay");
        order.markPaid();
        state = CustomerState.PAID;
    }
    public void leave() {
        if ((state != CustomerState.WAITING && state != CustomerState.SEATED) || satisfaction != 0) {
            throw new IllegalStateException("Customer is not abandoning");
        }
        if (order != null) order.cancel();
        state = CustomerState.LEFT;
    }
}
