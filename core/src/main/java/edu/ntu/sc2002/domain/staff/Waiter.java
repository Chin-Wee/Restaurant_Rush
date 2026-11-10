package edu.ntu.sc2002.domain.staff;

import edu.ntu.sc2002.domain.HappyHourPolicy;
import edu.ntu.sc2002.domain.Order;
import edu.ntu.sc2002.domain.Table;
import edu.ntu.sc2002.domain.WaitingQueue;
import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.menu.Menu;
import java.util.Objects;

public final class Waiter extends Staff {
    public Waiter(String staffId) { super(staffId); }
    /** Seats an explicitly selected queued customer at a free table. */
    public boolean seatCustomer(Customer customer, WaitingQueue queue, Table table) {
        Objects.requireNonNull(customer);
        Objects.requireNonNull(queue);
        Objects.requireNonNull(table);
        if (table.isOccupied() || !queue.getQueuedCustomers().contains(customer)) return false;
        table.assignCustomer(customer);
        queue.dequeueCustomer(customer);
        return true;
    }
    /** The customer's own discount applies first, then the active Happy Hour discount; the price is locked. */
    public Order takeOrder(Customer customer, Menu menu, HappyHourPolicy happyHourPolicy) {
        Objects.requireNonNull(customer);
        Objects.requireNonNull(menu);
        Objects.requireNonNull(happyHourPolicy);
        var item = customer.chooseItem(menu);
        int price = customer.calculateBill(item.getPriceCents());
        if (happyHourPolicy.isActive()) price = happyHourPolicy.applyHappyHourDiscount(price);
        return customer.placeOrder(item, price);
    }
    public void serve(Customer customer, int turn) { customer.serveOrder(turn); }
}
