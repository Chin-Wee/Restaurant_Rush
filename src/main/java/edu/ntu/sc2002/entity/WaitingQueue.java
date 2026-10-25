package edu.ntu.sc2002.entity;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;

public final class WaitingQueue {
    private final ArrayDeque<Customer> members = new ArrayDeque<>();

    public void enqueue(Customer customer) {
        Objects.requireNonNull(customer);
        if (customer.state() != CustomerState.WAITING || members.contains(customer)) {
            throw new IllegalArgumentException("Customer cannot join the queue");
        }
        members.addLast(customer);
    }

    public Customer dequeue() { return members.pollFirst(); }
    public boolean remove(Customer customer) { return members.remove(customer); }
    public List<Customer> getMembers() { return List.copyOf(members); }
    public boolean isEmpty() { return members.isEmpty(); }
}
