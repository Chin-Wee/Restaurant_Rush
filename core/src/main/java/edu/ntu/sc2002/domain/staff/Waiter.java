package edu.ntu.sc2002.domain.staff;

import edu.ntu.sc2002.domain.Order;
import edu.ntu.sc2002.domain.Table;
import edu.ntu.sc2002.domain.WaitingQueue;
import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.customer.CustomerState;
import edu.ntu.sc2002.domain.menu.MenuItem;
import java.util.Objects;

public final class Waiter extends Staff {
    private int nextOrderId = 1;

    public Waiter(String staffId) { super(staffId); }
    /** The existing manager API can select any queued customer. */
    public boolean seatCustomer(WaitingQueue queue, Table table, Customer customer) {
        requireAvailable();
        Objects.requireNonNull(queue);
        Objects.requireNonNull(table);
        if (customer == null || table.isOccupied() || !queue.getQueuedCustomers().contains(customer)) return false;
        table.assignCustomer(customer);
        queue.remove(customer);
        completeAction();
        return true;
    }
    public Order takeOrder(Customer customer, MenuItem item) {
        requireAvailable();
        Order order = new Order(nextOrderId, customer, item);
        if (currentTurn() > 0) order.startTurn(currentTurn());
        customer.placeOrder(order);
        nextOrderId++;
        completeAction();
        return order;
    }
    public boolean serveOrder(Order order) {
        requireAvailable();
        Objects.requireNonNull(order);
        Customer customer = order.getCustomer();
        if (customer.order() != order || customer.getState() != CustomerState.SEATED
                || !order.canServe(currentTurn())) return false;
        customer.serveOrder(currentTurn());
        completeAction();
        return true;
    }
}
