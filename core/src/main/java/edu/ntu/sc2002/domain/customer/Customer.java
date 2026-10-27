package edu.ntu.sc2002.domain.customer;
import edu.ntu.sc2002.domain.Order;
import edu.ntu.sc2002.domain.Satisfaction;
import edu.ntu.sc2002.domain.menu.Menu;
import edu.ntu.sc2002.domain.menu.MenuItem;

import java.util.Objects;
import java.util.Random;

/** Abstract customer — concrete subtypes (e.g. RegularCustomer) define satisfaction behaviour. */
public abstract class Customer {
    private final String id;
    private final int arrivalOrdinal;
    private final Satisfaction satisfaction = new Satisfaction();
    private final long choiceSeed;
    private CustomerState state = CustomerState.WAITING;
    private Order order;
    private Integer readyToPayTurn;

    protected Customer(String id, int arrivalOrdinal, long choiceSeed) {
        if (id == null || id.isBlank() || arrivalOrdinal < 1) throw new IllegalArgumentException("Invalid customer identity");
        this.id = id;
        this.arrivalOrdinal = arrivalOrdinal;
        this.choiceSeed = choiceSeed;
    }

    public String id() { return id; }
    public int arrivalOrdinal() { return arrivalOrdinal; }
    public Satisfaction satisfaction() { return satisfaction; }
    public CustomerState state() { return state; }
    public Order order() { return order; }
    public Integer readyToPayTurn() { return readyToPayTurn; }

    public MenuItem chooseItem(Menu menu) {
        var items = Objects.requireNonNull(menu).getItems();
        if (items.isEmpty()) throw new IllegalStateException("Menu is empty");
        return items.get(new Random(choiceSeed).nextInt(items.size()));
    }

    public abstract int satisfactionLossPerTurn();
    public boolean isWaitingForTableOrFood() { return state == CustomerState.WAITING || state == CustomerState.SEATED; }
    public void applyWaitingLoss() { if (isWaitingForTableOrFood()) satisfaction.deduct(satisfactionLossPerTurn()); }
    public void recover(int amount) { satisfaction.add(amount); }

    public void markSeated() {
        if (state != CustomerState.WAITING) throw new IllegalStateException("Customer cannot be seated");
        state = CustomerState.SEATED;
    }

    public Order placeOrder(MenuItem item, int lockedPriceCents) {
        if (state != CustomerState.SEATED || order != null) throw new IllegalStateException("Customer cannot place an order");
        Order placed = new Order(item, lockedPriceCents);
        order = placed;
        return placed;
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

    public void paid() {
        if (state != CustomerState.READY_TO_PAY) throw new IllegalStateException("Customer is not ready to pay");
        order.markPaid();
        state = CustomerState.PAID;
    }

    public void leave() {
        if (!isWaitingForTableOrFood() || satisfaction.getValue() != 0) throw new IllegalStateException("Customer is not abandoning");
        if (order != null) order.cancel();
        state = CustomerState.LEFT;
    }
}
