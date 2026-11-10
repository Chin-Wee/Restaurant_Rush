package edu.ntu.sc2002.domain.customer;

import edu.ntu.sc2002.domain.Order;
import edu.ntu.sc2002.domain.menu.Menu;
import edu.ntu.sc2002.domain.menu.MenuItem;
import java.util.Objects;
import java.util.Random;

/**
 * Shared identity, satisfaction, seating, ordering, service and payment state for all customers.
 * Subclasses supply only the type-specific waiting-loss rate, bill calculation and, where needed,
 * dish-choice policy. The class enforces lifecycle preconditions so the controller cannot skip steps.
 */
public abstract class Customer {
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
    /**
     * Returns the customer type for display and booking. Gameplay decisions must use the
     * overridden behaviour methods rather than switching on this value.
     */
    public abstract CustomerType getType();

    /**
     * Returns the satisfaction points lost for one waiting turn.
     * Regular customers lose 8, VIPs 5 and critics 12.
     */
    public abstract int satisfactionLossPerTurn();

    /**
     * Calculates the customer-specific bill before any policy discount.
     * VIP customers return 90% of {@code basePriceCents}; other customers return the full price.
     *
     * @param basePriceCents menu item price in cents
     * @return the customer-adjusted price in cents
     * @throws IllegalArgumentException if {@code basePriceCents} is negative
     */
    public abstract int calculateBill(int basePriceCents);

    /**
     * Applies this turn's waiting penalty only while the customer is still waiting for a table or food.
     * The deduction rate is supplied by {@link #satisfactionLossPerTurn()}.
     */
    public void applySatisfactionLoss() {
        if (state == CustomerState.WAITING || state == CustomerState.SEATED) deductSatisfaction(satisfactionLossPerTurn());
    }

    /**
     * Chooses a dish using the customer's deterministic seed.
     * The same seed and menu therefore produce the same item across runs.
     *
     * @param menu menu to choose from
     * @return the selected item
     * @throws IllegalStateException if the menu is empty
     */
    public MenuItem chooseItem(Menu menu) {
        var items = Objects.requireNonNull(menu).getItems();
        if (items.isEmpty()) throw new IllegalStateException("Menu is empty");
        return items.get(new Random(choiceSeed).nextInt(items.size()));
    }

    /**
     * Reduces satisfaction, clamping at zero.
     *
     * @param points non-negative satisfaction points to remove
     * @throws IllegalArgumentException if {@code points} is negative
     */
    public void deductSatisfaction(int points) {
        if (points < 0) throw new IllegalArgumentException("Points must be nonnegative");
        satisfaction = (int) Math.max(0L, (long) satisfaction - points);
    }

    /**
     * Increases satisfaction, clamping at 100.
     *
     * @param points non-negative satisfaction points to add
     * @throws IllegalArgumentException if {@code points} is negative
     */
    public void addSatisfaction(int points) {
        if (points < 0) throw new IllegalArgumentException("Points must be nonnegative");
        satisfaction = (int) Math.min(100L, (long) satisfaction + points);
    }

    /**
     * Marks the customer as seated.
     *
     * @throws IllegalStateException if the customer is not currently waiting
     */
    public void markSeated() {
        if (state != CustomerState.WAITING) throw new IllegalStateException("Customer is not waiting");
        state = CustomerState.SEATED;
    }

    /**
     * Creates the customer's single order and locks its final bill.
     *
     * @param item selected menu item
     * @param lockedPriceCents final price after customer and policy discounts
     * @return the newly created order
     * @throws IllegalStateException if the customer is not seated or already has an order
     */
    public Order placeOrder(MenuItem item, int lockedPriceCents) {
        if (state != CustomerState.SEATED || order != null) throw new IllegalStateException("Customer cannot order");
        order = new Order(item, lockedPriceCents);
        return order;
    }

    /**
     * Serves the customer's order and starts the eating phase.
     *
     * @param turn current game turn
     * @throws IllegalStateException if the customer is not seated with an order
     */
    public void serveOrder(int turn) {
        if (state != CustomerState.SEATED || order == null) throw new IllegalStateException("Customer cannot be served");
        order.serve(turn);
        state = CustomerState.EATING;
    }

    /**
     * Advances an eating customer to {@code READY_TO_PAY} after a full eating turn.
     *
     * @param turn current game turn
     */
    public void finishEating(int turn) {
        if (state == CustomerState.EATING && order.servedTurn() < turn) {
            state = CustomerState.READY_TO_PAY;
            readyToPayTurn = turn;
        }
    }

    /**
     * Reports whether the Cashier may collect payment from this customer.
     * A customer who became ready during the current turn must wait until the next turn.
     *
     * @param turn current game turn
     * @return true when payment is allowed
     */
    public boolean canPay(int turn) { return state == CustomerState.READY_TO_PAY && readyToPayTurn < turn; }

    /**
     * Records a successful payment and moves the customer to {@code PAID}.
     * The Cashier must check {@link #canPay(int)} first.
     *
     * @throws IllegalStateException if the customer is not ready to pay
     */
    public void paid() {
        if (state != CustomerState.READY_TO_PAY) throw new IllegalStateException("Customer is not ready to pay");
        order.markPaid();
        state = CustomerState.PAID;
    }

    /**
     * Abandons the customer after satisfaction reaches zero, cancelling any unfinished order.
     *
     * @throws IllegalStateException if the customer is not waiting/seated or satisfaction is not zero
     */
    public void leave() {
        if ((state != CustomerState.WAITING && state != CustomerState.SEATED) || satisfaction != 0) {
            throw new IllegalStateException("Customer is not abandoning");
        }
        if (order != null) order.cancel();
        state = CustomerState.LEFT;
    }
}
