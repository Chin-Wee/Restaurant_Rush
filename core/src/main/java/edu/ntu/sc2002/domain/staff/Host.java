package edu.ntu.sc2002.domain.staff;

import edu.ntu.sc2002.domain.Table;
import edu.ntu.sc2002.domain.WaitingQueue;
import edu.ntu.sc2002.domain.customer.Customer;
import java.util.Objects;

/**
 * Stage 3 greeter. The controller seats the queue head at the first free table
 * automatically at the start of each turn, in addition to the Waiter's task.
 */
public final class Host extends Staff {
    public Host(String staffId) { super(staffId); }

    /**
     * Seats the given queued customer at the given free table; mirrors Waiter.seatCustomer.
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
}
