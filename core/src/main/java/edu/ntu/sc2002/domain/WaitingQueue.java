package edu.ntu.sc2002.domain;

import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.customer.CustomerState;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

/** FIFO queue of customers waiting for a table; supports explicit removal of a chosen customer. */
public final class WaitingQueue {
    private final Queue<Customer> queue = new ArrayDeque<>();

    /**
     * Appends a waiting customer to the queue.
     *
     * @param customer customer to enqueue
     * @throws IllegalArgumentException if the customer is not waiting or is already queued
     */
    public void enqueueCustomer(Customer customer) {
        Objects.requireNonNull(customer);
        if (customer.getState() != CustomerState.WAITING || queue.contains(customer)) {
            throw new IllegalArgumentException("Customer cannot join the queue");
        }
        queue.add(customer);
    }

    /**
     * Removes the manager's selected customer without changing the remaining queue order.
     *
     * @param customer customer to remove
     * @throws IllegalArgumentException if the customer is not currently queued
     */
    public void dequeueCustomer(Customer customer) {
        Objects.requireNonNull(customer);
        if (!queue.remove(customer)) throw new IllegalArgumentException("Customer is not queued");
    }
    public List<Customer> getQueuedCustomers() { return List.copyOf(queue); }
    public boolean isEmpty() { return queue.isEmpty(); }
}
