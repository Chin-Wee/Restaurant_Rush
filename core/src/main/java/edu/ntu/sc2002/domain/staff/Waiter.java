package edu.ntu.sc2002.domain.staff;

import edu.ntu.sc2002.domain.HappyHourPolicy;
import edu.ntu.sc2002.domain.Order;
import edu.ntu.sc2002.domain.Table;
import edu.ntu.sc2002.domain.WaitingQueue;
import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.menu.Menu;
import java.util.Objects;

/** Waiter role: seats a chosen customer, takes an order with customer/policy pricing, or serves an order. */
public final class Waiter extends Staff {
    public Waiter(String staffId) { super(staffId); }
    /**
     * Seats an explicitly selected queued customer at a free table.
     *
     * @param customer queued customer selected by the manager
     * @param queue restaurant waiting queue
     * @param table free table selected by the controller
     * @return true if the customer was seated and dequeued; false if the table is occupied or the
     *         customer is not in the queue
     */
    public boolean seatCustomer(Customer customer, WaitingQueue queue, Table table) {
        Objects.requireNonNull(customer);
        Objects.requireNonNull(queue);
        Objects.requireNonNull(table);
        if (table.isOccupied() || !queue.getQueuedCustomers().contains(customer)) return false;
        table.assignCustomer(customer);
        queue.dequeueCustomer(customer);
        return true;
    }
    /**
     * Creates the customer's order. The customer-specific bill is calculated first, then any active
     * Happy Hour discount is applied; the resulting price is locked into the order.
     *
     * @param customer customer placing the order
     * @param menu menu to choose from
     * @param happyHourPolicy current pricing policy
     * @return the newly created order
     */
    public Order takeOrder(Customer customer, Menu menu, HappyHourPolicy happyHourPolicy) {
        Objects.requireNonNull(customer);
        Objects.requireNonNull(menu);
        Objects.requireNonNull(happyHourPolicy);
        var item = customer.chooseItem(menu);
        int price = customer.calculateBill(item.getPriceCents());
        if (happyHourPolicy.isActive()) price = happyHourPolicy.applyHappyHourDiscount(price);
        return customer.placeOrder(item, price);
    }
    /**
     * Serves a customer's ready order and starts their eating phase.
     *
     * @param customer customer to serve
     * @param turn current game turn
     */
    public void serve(Customer customer, int turn) { customer.serveOrder(turn); }
}
