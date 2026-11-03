package edu.ntu.sc2002.domain.staff;

import edu.ntu.sc2002.domain.Order;
import edu.ntu.sc2002.domain.Table;
import edu.ntu.sc2002.domain.WaitingQueue;
import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.menu.Menu;
import java.util.Objects;

public final class Waiter extends Staff {
    public Waiter(String staffId) { super(staffId); }
    /** Seats the next queued customer; Restaurant.seat handles an explicitly selected customer. */
    public boolean seatCustomer(WaitingQueue queue, Table table) {
        Objects.requireNonNull(queue);
        Objects.requireNonNull(table);
        if (queue.isEmpty() || table.isOccupied()) return false;
        Customer customer = queue.getQueuedCustomers().get(0);
        table.assignCustomer(customer);
        queue.dequeueCustomer(customer);
        return true;
    }
    public Order takeOrder(Customer customer, Menu menu) {
        var item = customer.chooseItem(menu);
        return customer.placeOrder(item, customer.calculateBill(item.getPriceCents()));
    }
    public void serve(Customer customer, int turn) { customer.serveOrder(turn); }
}
