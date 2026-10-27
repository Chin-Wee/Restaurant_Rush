package edu.ntu.sc2002.domain;

import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.menu.MenuItem;
import java.util.Objects;

public final class Order {
    private final int orderId;
    private final Customer customer;
    private final MenuItem menuItem;
    private int remainingPrepUnits;
    private OrderStatus status = OrderStatus.PENDING;
    private int currentTurn;
    private Integer readyTurn;
    private Integer servedTurn;

    public Order(int orderId, Customer customer, MenuItem menuItem) {
        if (orderId < 1) throw new IllegalArgumentException("Invalid order ID");
        this.orderId = orderId;
        this.customer = Objects.requireNonNull(customer);
        this.menuItem = Objects.requireNonNull(menuItem);
        remainingPrepUnits = menuItem.getPrepUnitsNeeded();
    }
    public int getOrderId() { return orderId; }
    public Customer getCustomer() { return customer; }
    public MenuItem getMenuItem() { return menuItem; }
    public OrderStatus getStatus() { return status; }
    public boolean isReady() { return status == OrderStatus.READY; }
    public int unitsDone() { return menuItem.getPrepUnitsNeeded() - remainingPrepUnits; }
    public Integer servedTurn() { return servedTurn; }
    public int currentTurn() { return currentTurn; }

    /** Turn context preserves timing while the UML operations remain parameterless. */
    public void startTurn(int turn) {
        if (turn < 1 || turn < currentTurn) throw new IllegalArgumentException("Invalid order turn");
        currentTurn = turn;
    }
    public void advancePreparation() {
        if (status != OrderStatus.PENDING && status != OrderStatus.IN_PREPARATION) {
            throw new IllegalStateException("Order is not being prepared");
        }
        remainingPrepUnits--;
        setStatus(remainingPrepUnits == 0 ? OrderStatus.READY : OrderStatus.IN_PREPARATION);
    }
    public void setStatus(OrderStatus next) {
        Objects.requireNonNull(next);
        if (next == status) return;
        boolean preparing = status == OrderStatus.PENDING || status == OrderStatus.IN_PREPARATION;
        switch (next) {
            case IN_PREPARATION -> {
                if (!preparing || remainingPrepUnits == 0) throw new IllegalStateException("Invalid preparation state");
            }
            case READY -> {
                if (!preparing || remainingPrepUnits != 0) throw new IllegalStateException("Preparation is incomplete");
                readyTurn = currentTurn;
            }
            case SERVED -> {
                if (!canServe(currentTurn)) throw new IllegalStateException("Order must be ready before this turn");
                servedTurn = currentTurn;
            }
            case PAID -> {
                if (status != OrderStatus.SERVED || !customer.canPay(currentTurn)) throw new IllegalStateException("Order is not payable");
            }
            case CANCELLED -> {
                if (status == OrderStatus.SERVED || status == OrderStatus.PAID) throw new IllegalStateException("Only unserved orders can be cancelled");
            }
            default -> throw new IllegalStateException("Order cannot return to pending");
        }
        status = next;
    }
    public boolean canServe(int turn) { return isReady() && readyTurn < turn; }
    public void serve(int turn) {
        if (!canServe(turn)) throw new IllegalStateException("Order must be ready before this turn");
        startTurn(turn);
        setStatus(OrderStatus.SERVED);
    }
    public void markPaid() { setStatus(OrderStatus.PAID); }
    public void cancel() { setStatus(OrderStatus.CANCELLED); }
}
