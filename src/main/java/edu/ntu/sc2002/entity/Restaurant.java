package edu.ntu.sc2002.entity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
public final class Restaurant {
    private final WaitingQueue waitingQueue = new WaitingQueue();
    private final List<Table> tables = List.of(new Table(1), new Table(2));
    private final List<Customer> customers = new ArrayList<>();
    private final Menu menu = new Menu();
    private final Waiter waiter = new Waiter();
    private final Chef chef = new Chef();
    private final Cashier cashier = new Cashier();
    public WaitingQueue waitingQueue() { return waitingQueue; }
    public List<Table> tables() { return tables; }
    public List<Customer> customers() { return List.copyOf(customers); }
    public Menu menu() { return menu; }
    public Waiter waiter() { return waiter; }
    public Chef chef() { return chef; }
    public Cashier cashier() { return cashier; }
    public void registerArrival(Customer customer) { customers.add(customer); waitingQueue.enqueue(customer); }
    public Table seat(Customer customer) {
        if (!waitingQueue.getMembers().contains(customer)) throw new IllegalArgumentException("Customer is not in the waiting queue.");
        Table table = tables.stream().filter(t -> !t.isOccupied()).findFirst().orElse(null);
        if (table == null) throw new IllegalStateException("No table is free.");
        waitingQueue.remove(customer); table.assign(customer); return table;
    }
    public Customer oldestPaymentCandidate(int turn) {
        return customers.stream().filter(c -> c.canPay(turn)).min(Comparator.comparingInt(Customer::arrivalOrdinal)).orElse(null);
    }
    public void completePayment(Customer customer) { tables.stream().filter(t -> t.getOccupant() == customer).forEach(Table::vacate); }
    public void abandon(Customer customer) {
        waitingQueue.remove(customer);
        tables.stream().filter(t -> t.getOccupant() == customer).forEach(Table::vacate);
        customer.leave();
    }
}
