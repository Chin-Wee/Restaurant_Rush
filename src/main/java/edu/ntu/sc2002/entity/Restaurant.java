package edu.ntu.sc2002.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public final class Restaurant {
    private final WaitingQueue waitingQueue = new WaitingQueue();
    private final List<Table> tables;
    private final List<Customer> customers = new ArrayList<>();
    private final Menu menu = new Menu();
    private final Waiter waiter = new Waiter();
    private final Chef chef = new Chef();
    private final Cashier cashier = new Cashier();

    public Restaurant() { this(2); }

    public Restaurant(int numTables) {
        if (numTables < 1) throw new IllegalArgumentException("At least one table is required");
        tables = IntStream.rangeClosed(1, numTables).mapToObj(Table::new).toList();
    }

    public WaitingQueue waitingQueue() { return waitingQueue; }
    public List<Table> tables() { return tables; }
    public List<Customer> customers() { return List.copyOf(customers); }
    public Menu menu() { return menu; }
    public Waiter waiter() { return waiter; }
    public Chef chef() { return chef; }
    public Cashier cashier() { return cashier; }

    public void registerArrival(Customer customer) {
        Objects.requireNonNull(customer);
        if (customers.stream().anyMatch(c -> c.id().equalsIgnoreCase(customer.id()))) {
            throw new IllegalArgumentException("Customer already registered");
        }
        waitingQueue.enqueue(customer);
        customers.add(customer);
    }

    public Table seat(Customer customer) {
        if (!waitingQueue.getMembers().contains(customer)) throw new IllegalArgumentException("Customer is not queued");
        Table table = tables.stream().filter(t -> !t.isOccupied()).findFirst()
                .orElseThrow(() -> new IllegalStateException("No table is free"));
        table.assign(customer);
        waitingQueue.remove(customer);
        return table;
    }

    /** Table.occupant is the sole source of table assignment. */
    public Table tableFor(Customer customer) {
        Objects.requireNonNull(customer);
        return tables.stream().filter(t -> t.getOccupant() == customer).findFirst().orElse(null);
    }

    /** "Oldest" means earliest arrival among customers eligible to pay, not earliest readiness. */
    public Customer oldestPaymentCandidate(int turn) {
        return customers.stream().filter(c -> c.canPay(turn))
                .min(Comparator.comparingInt(Customer::arrivalOrdinal)).orElse(null);
    }

    public PaymentRecord completePayment(Customer customer) {
        if (!customers.contains(customer) || customer.state() != CustomerState.READY_TO_PAY) {
            throw new IllegalStateException("Customer is not ready to pay");
        }
        PaymentRecord record = new PaymentRecord(customer.id(), customer.order().lockedPriceCents(),
                customer.satisfaction().getValue());
        customer.paid();
        tables.stream().filter(t -> t.getOccupant() == customer).forEach(Table::vacate);
        return record;
    }

    public void abandon(Customer customer) {
        if (!customers.contains(customer)) throw new IllegalArgumentException("Customer is not registered");
        customer.leave();
        waitingQueue.remove(customer);
        tables.stream().filter(t -> t.getOccupant() == customer).forEach(Table::vacate);
    }
}
