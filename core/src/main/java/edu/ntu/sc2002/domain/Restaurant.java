package edu.ntu.sc2002.domain;

import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.customer.CustomerState;
import edu.ntu.sc2002.domain.menu.Menu;
import edu.ntu.sc2002.domain.staff.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Owns the room, queue, menu, staff and customer history; accounting belongs to ScoreBoard. */
public final class Restaurant {
    private final List<Table> tables;
    private final WaitingQueue waitingQueue = new WaitingQueue();
    private final Menu menu = new Menu();
    private final List<Staff> staffList = new ArrayList<>();
    private final List<Customer> customers = new ArrayList<>();

    public Restaurant(int numTables) {
        if (numTables != 2) throw new IllegalArgumentException("Stage 1 requires two tables");
        tables = List.of(new Table(1), new Table(2));
        addStaff(new Waiter("Waiter"));
        addStaff(new Chef("Chef"));
        addStaff(new Cashier("Cashier"));
    }
    public List<Table> getTables() { return tables; }
    public WaitingQueue getWaitingQueue() { return waitingQueue; }
    public Menu getMenu() { return menu; }
    public List<Customer> getCustomers() { return List.copyOf(customers); }
    public void addStaff(Staff staff) {
        Objects.requireNonNull(staff);
        if (staffList.stream().anyMatch(s -> s.getClass() == staff.getClass())) {
            throw new IllegalArgumentException("That staff role is already registered");
        }
        staffList.add(staff);
    }
    public List<Staff> getStaffList() { return List.copyOf(staffList); }
    public <T extends Staff> T getStaff(Class<T> type) {
        Objects.requireNonNull(type);
        T found = null;
        for (Staff staff : staffList) {
            if (type.isInstance(staff)) {
                if (found != null) throw new IllegalStateException("More than one staff member matches " + type.getSimpleName());
                found = type.cast(staff);
            }
        }
        if (found == null) throw new IllegalStateException("No staff member matches " + type.getSimpleName());
        return found;
    }
    public void registerArrival(Customer customer) {
        Objects.requireNonNull(customer);
        if (customers.stream().anyMatch(c -> c.getId().equalsIgnoreCase(customer.getId()))) {
            throw new IllegalArgumentException("Customer already registered");
        }
        if (customer.arrivalOrdinal() != customers.size() + 1) {
            throw new IllegalArgumentException("Customer arrival order differs from registration order");
        }
        waitingQueue.enqueueCustomer(customer);
        customers.add(customer);
    }
    public Table seat(Customer customer) {
        if (!waitingQueue.getQueuedCustomers().contains(customer)) throw new IllegalArgumentException("Customer is not queued");
        Table table = tables.stream().filter(t -> !t.isOccupied()).findFirst()
                .orElseThrow(() -> new IllegalStateException("No table is free"));
        if (!getStaff(Waiter.class).seatCustomer(waitingQueue, table, customer)) throw new IllegalStateException("Customer cannot be seated");
        return table;
    }
    public Table tableFor(Customer customer) {
        Objects.requireNonNull(customer);
        return tables.stream().filter(t -> t.getSeatedCustomer() == customer).findFirst().orElse(null);
    }
    /** Oldest means earliest arrival among customers eligible on this turn. */
    public Customer oldestPaymentCandidate(int turn) {
        return customers.stream().filter(c -> c.canPay(turn))
                .min(Comparator.comparingInt(Customer::arrivalOrdinal)).orElse(null);
    }
    /** Cashier validates the explicit turn before calling this operation. */
    public PaymentRecord completePayment(Customer customer) {
        if (!customers.contains(customer) || customer.getState() != CustomerState.READY_TO_PAY
                || tableFor(customer) == null) throw new IllegalStateException("Customer is not ready to pay");
        PaymentRecord record = new PaymentRecord(customer.getId(), customer.order().lockedPriceCents(), customer.getSatisfaction());
        customer.paid();
        tableFor(customer).releaseTable();
        return record;
    }
    public void abandon(Customer customer) {
        if (!customers.contains(customer)) throw new IllegalArgumentException("Customer is not registered");
        customer.leave();
        waitingQueue.remove(customer);
        Table table = tableFor(customer);
        if (table != null) table.releaseTable();
    }
}
