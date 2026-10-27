package edu.ntu.sc2002.domain.customer;

import edu.ntu.sc2002.domain.Order;
import edu.ntu.sc2002.domain.OrderStatus;
import edu.ntu.sc2002.domain.menu.MenuItem;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Phase 1 customer. Only customers waiting for a table or food lose satisfaction. */
public final class Customer {
    private final String id;
    private int satisfaction = 100;
    private CustomerState state = CustomerState.QUEUED;
    private int arrivalOrdinal;
    private final long choiceSeed;
    private Order order;
    private Integer readyToPayTurn;

    public Customer(String id) { this(id, 0, 2026); }
    public Customer(String id, int arrivalOrdinal, long choiceSeed) {
        if (id == null || id.isBlank() || arrivalOrdinal < 0) throw new IllegalArgumentException("Invalid customer identity");
        this.id = id;
        this.arrivalOrdinal = arrivalOrdinal;
        this.choiceSeed = choiceSeed;
    }

    public String getId() { return id; }
    public int getSatisfaction() { return satisfaction; }
    public void setSatisfaction(int satisfaction) {
        if (satisfaction < 0 || satisfaction > 100) throw new IllegalArgumentException("Satisfaction must be in 0..100");
        this.satisfaction = satisfaction;
    }
    public CustomerState getState() { return state; }
    public void setState(CustomerState next) {
        Objects.requireNonNull(next);
        if (next == state) return;
        switch (next) {
            case SEATED -> {
                if (state != CustomerState.QUEUED) throw new IllegalStateException("Customer cannot be seated");
                state = next;
            }
            case EATING -> {
                if (state != CustomerState.SEATED || requireOrder().getStatus() != OrderStatus.SERVED) {
                    throw new IllegalStateException("Customer must be served before eating");
                }
                state = next;
            }
            case READY_TO_PAY -> {
                int turn = requireOrder().currentTurn();
                if (state != CustomerState.EATING || order.servedTurn() >= turn) {
                    throw new IllegalStateException("Customer must finish a later eating turn");
                }
                finishEating(turn);
            }
            case PAID -> paid();
            case ABANDONED -> leave();
            default -> throw new IllegalStateException("Customer cannot return to the queue");
        }
    }
    public void applyTurnEndDecay() {
        if (isWaitingForTableOrFood()) satisfaction = Math.max(0, satisfaction - 8);
    }
    public int calculateBill(int basePriceCents) {
        if (basePriceCents < 0) throw new IllegalArgumentException("Price must be nonnegative");
        return basePriceCents;
    }

    public int arrivalOrdinal() { return arrivalOrdinal; }
    /** Assign arrival order once; zero denotes a customer not yet registered. */
    public void recordArrival(int ordinal) {
        if (ordinal < 1 || (arrivalOrdinal != 0 && arrivalOrdinal != ordinal)) {
            throw new IllegalArgumentException("Arrival order is already assigned or invalid");
        }
        arrivalOrdinal = ordinal;
    }
    public Order order() { return order; }
    public MenuItem chooseMenuItem(List<MenuItem> availableItems) {
        Objects.requireNonNull(availableItems);
        if (availableItems.isEmpty()) throw new IllegalStateException("Menu is empty");
        return availableItems.get(new Random(choiceSeed).nextInt(availableItems.size()));
    }
    public boolean isWaitingForTableOrFood() { return state == CustomerState.QUEUED || state == CustomerState.SEATED; }
    public void placeOrder(Order placed) {
        Objects.requireNonNull(placed);
        if (state != CustomerState.SEATED || order != null || placed.getCustomer() != this
                || placed.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Customer cannot place this order");
        }
        order = placed;
    }
    public void serveOrder(int turn) {
        if (state != CustomerState.SEATED || order == null) throw new IllegalStateException("Customer cannot be served");
        order.serve(turn);
        setState(CustomerState.EATING);
    }
    public void finishEating(int turn) {
        if (state == CustomerState.EATING && order.servedTurn() < turn) {
            state = CustomerState.READY_TO_PAY;
            readyToPayTurn = turn;
        }
    }
    public boolean canPay(int turn) { return state == CustomerState.READY_TO_PAY && readyToPayTurn < turn; }
    public void paid() {
        if (state != CustomerState.READY_TO_PAY || !canPay(requireOrder().currentTurn())) {
            throw new IllegalStateException("Customer is not eligible to pay this turn");
        }
        order.markPaid();
        state = CustomerState.PAID;
    }
    public void leave() {
        if (!isWaitingForTableOrFood() || satisfaction != 0) throw new IllegalStateException("Customer is not abandoning");
        if (order != null) order.cancel();
        state = CustomerState.ABANDONED;
    }
    private Order requireOrder() {
        if (order == null) throw new IllegalStateException("Customer has no order");
        return order;
    }
}
