package edu.ntu.sc2002.domain;

import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.customer.CustomerState;
import edu.ntu.sc2002.domain.menu.Menu;
import edu.ntu.sc2002.domain.staff.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Owns the room, queue, menu and staff; accounting belongs to ScoreBoard. */
public final class Restaurant {
    private final List<Table> tables;
    private final WaitingQueue waitingQueue = new WaitingQueue();
    private final Menu menu = new Menu();
    private final List<Staff> staffList = new ArrayList<>();

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
    public void addStaff(Staff staff) {
        Objects.requireNonNull(staff);
        if (!(staff instanceof Waiter || staff instanceof Chef || staff instanceof Cashier)) {
            throw new IllegalArgumentException("Stage 1 supports only Waiter, Chef and Cashier");
        }
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
        if (waitingQueue.getQueuedCustomers().stream().anyMatch(c -> c.getId().equalsIgnoreCase(customer.getId()))
                || tables.stream().anyMatch(t -> t.isOccupied() && t.getSeatedCustomer().getId().equalsIgnoreCase(customer.getId()))) {
            throw new IllegalArgumentException("Customer already present");
        }
        waitingQueue.enqueueCustomer(customer);
    }
    /** Requires the customer to occupy a table in this restaurant. */
    public Table tableFor(Customer customer) {
        Objects.requireNonNull(customer);
        return tables.stream().filter(Table::isOccupied).filter(t -> t.getSeatedCustomer() == customer).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Customer is not at a table"));
    }
    /** Requires an eligible customer; oldest means earliest arrival among those eligible this turn. */
    public Customer oldestPaymentCandidate(int turn) {
        return tables.stream().filter(Table::isOccupied).map(Table::getSeatedCustomer).filter(c -> c.canPay(turn))
                .min(Comparator.comparingInt(Customer::arrivalOrdinal))
                .orElseThrow(() -> new IllegalStateException("No customer is eligible to pay this turn"));
    }
    /** Cashier validates the explicit turn before calling this operation. */
    public PaymentRecord completePayment(Customer customer) {
        if (customer == null || customer.getState() != CustomerState.READY_TO_PAY) {
            throw new IllegalStateException("Customer is not ready to pay");
        }
        Table table = tableFor(customer);
        PaymentRecord record = new PaymentRecord(customer.getId(), customer.order().lockedPriceCents(), customer.getSatisfaction());
        customer.paid();
        table.releaseTable();
        return record;
    }
}
