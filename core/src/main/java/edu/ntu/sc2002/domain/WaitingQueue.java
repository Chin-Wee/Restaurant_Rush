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
    public Customer dequeueCustomer() { return queue.poll(); }
    public List<Customer> getQueuedCustomers() { return List.copyOf(queue); }
    public boolean remove(Customer customer) { return queue.remove(customer); }
    public boolean isEmpty() { return queue.isEmpty(); }
}
