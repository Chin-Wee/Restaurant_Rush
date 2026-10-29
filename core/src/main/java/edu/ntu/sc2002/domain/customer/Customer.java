package edu.ntu.sc2002.domain.customer;

import edu.ntu.sc2002.domain.Order;
import edu.ntu.sc2002.domain.menu.Menu;
import edu.ntu.sc2002.domain.menu.MenuItem;
import java.util.Objects;
import java.util.Random;

/** Stage 1 regular customer; owns at most one order throughout its lifecycle. */
public final class Customer {
    private final String id;
    private int satisfaction = 100;
    private CustomerState state = CustomerState.WAITING;
    private final int arrivalOrdinal;
    private Order order;
    private final long choiceSeed;
    private Integer readyToPayTurn;

    public Customer(String id, int arrivalOrdinal, long choiceSeed) {
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
    public MenuItem chooseItem(Menu menu) {
        var items = Objects.requireNonNull(menu).getItems();
        if (items.isEmpty()) throw new IllegalStateException("Menu is empty");
        return items.get(new Random(choiceSeed).nextInt(items.size()));
    }
    public boolean isWaitingForTableOrFood() { return state == CustomerState.WAITING || state == CustomerState.SEATED; }
    public void applyWaitingLoss() { if (isWaitingForTableOrFood()) deductSatisfaction(8); }
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
        if (!isWaitingForTableOrFood() || satisfaction != 0) throw new IllegalStateException("Customer is not abandoning");
        if (order != null) order.cancel();
        state = CustomerState.LEFT;
    }
}
