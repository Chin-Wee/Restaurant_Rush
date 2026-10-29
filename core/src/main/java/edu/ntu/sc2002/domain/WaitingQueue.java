package edu.ntu.sc2002.domain;

import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.customer.CustomerState;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

public final class WaitingQueue {
    private final Queue<Customer> queue = new ArrayDeque<>();

    public void enqueueCustomer(Customer customer) {
        Objects.requireNonNull(customer);
        if (customer.getState() != CustomerState.WAITING || queue.contains(customer)) {
            throw new IllegalArgumentException("Customer cannot join the queue");
        }
        queue.add(customer);
    }
    /** Removes the manager's selected customer without changing the remaining queue order. */
    public void dequeueCustomer(Customer customer) {
        Objects.requireNonNull(customer);
        if (!queue.remove(customer)) throw new IllegalArgumentException("Customer is not queued");
    }
    public List<Customer> getQueuedCustomers() { return List.copyOf(queue); }
    public boolean isEmpty() { return queue.isEmpty(); }
}
