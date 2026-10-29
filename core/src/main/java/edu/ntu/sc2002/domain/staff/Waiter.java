package edu.ntu.sc2002.domain.staff;

import edu.ntu.sc2002.domain.Order;
import edu.ntu.sc2002.domain.Table;
import edu.ntu.sc2002.domain.WaitingQueue;
import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.menu.Menu;
import java.util.Objects;

public final class Waiter extends Staff {
    public Waiter(String staffId) { super(staffId); }
    public boolean seatCustomer(WaitingQueue queue, Table table) {
        return seatCustomer(queue, table, queue.getQueuedCustomers().stream().findFirst().orElse(null));
    }
    /** Selected-customer helper: the manager may prioritize any queued customer. */
    public boolean seatCustomer(WaitingQueue queue, Table table, Customer customer) {
        Objects.requireNonNull(queue);
        Objects.requireNonNull(table);
        if (customer == null || table.isOccupied() || !queue.getQueuedCustomers().contains(customer)) return false;
        table.assignCustomer(customer);
        queue.remove(customer);
        return true;
    }
    public Order takeOrder(Customer customer, Menu menu) {
        var item = customer.chooseItem(menu);
        return customer.placeOrder(item, item.getPriceCents());
    }
    public void serve(Customer customer, int turn) { customer.serveOrder(turn); }
}
