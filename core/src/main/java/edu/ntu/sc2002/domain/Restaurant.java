package edu.ntu.sc2002.domain;

import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.customer.CustomerState;
import edu.ntu.sc2002.domain.menu.Menu;
import edu.ntu.sc2002.domain.staff.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root for the physical restaurant: tables, waiting queue, menu, staff and the Stage 3
 * Happy Hour policy. It exposes only the domain operations needed by staff/controller and leaves
 * accounting to {@link ScoreBoard}.
 */
public final class Restaurant {
    private final List<Table> tables;
    private final WaitingQueue waitingQueue = new WaitingQueue();
    private final Menu menu = new Menu();
    private final List<Staff> staffList = new ArrayList<>();
    private final HappyHourPolicy happyHourPolicy = new HappyHourPolicy();

    /**
     * Creates the base restaurant with two tables, one Waiter, Chef, Cashier and Host, and a new
     * Happy Hour policy.
     *
     * @param numTables required table count; the base game requires two
     * @throws IllegalArgumentException if {@code numTables} is not two
     */
    public Restaurant(int numTables) {
        if (numTables != 2) throw new IllegalArgumentException("The restaurant requires two tables");
        tables = List.of(new Table(1), new Table(2));
        addStaff(new Waiter("Waiter"));
        addStaff(new Chef("Chef"));
        addStaff(new Cashier("Cashier"));
        addStaff(new Host("Host"));
    }
    public List<Table> getTables() { return tables; }
    public WaitingQueue getWaitingQueue() { return waitingQueue; }
    public Menu getMenu() { return menu; }
    public HappyHourPolicy getHappyHourPolicy() { return happyHourPolicy; }
    /**
     * Registers one staff member, allowing at most one instance of each supported role.
     *
     * @param staff staff member to register
     * @throws IllegalArgumentException if the role is unsupported or already registered
     */
    public void addStaff(Staff staff) {
        Objects.requireNonNull(staff);
        if (!(staff instanceof Waiter || staff instanceof Chef || staff instanceof Cashier || staff instanceof Host)) {
            throw new IllegalArgumentException("The restaurant supports only Waiter, Chef, Cashier and Host");
        }
        if (staffList.stream().anyMatch(s -> s.getClass() == staff.getClass())) {
            throw new IllegalArgumentException("That staff role is already registered");
        }
        staffList.add(staff);
    }
    public List<Staff> getStaffList() { return List.copyOf(staffList); }

    /**
     * Finds the single registered staff member of the requested role.
     *
     * @param type staff role class
     * @param <T> concrete staff type
     * @return the matching staff member
     * @throws IllegalStateException if no staff member matches the role
     */
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
    /**
     * Adds a scheduled arrival to the waiting queue after checking that its ID is not already
     * present in the restaurant.
     *
     * @param customer arriving customer
     * @throws IllegalArgumentException if a customer with the same ID is already queued or seated
     */
    public void registerArrival(Customer customer) {
        Objects.requireNonNull(customer);
        if (waitingQueue.getQueuedCustomers().stream().anyMatch(c -> c.getId().equalsIgnoreCase(customer.getId()))
                || tables.stream().anyMatch(t -> t.isOccupied() && t.getSeatedCustomer().getId().equalsIgnoreCase(customer.getId()))) {
            throw new IllegalArgumentException("Customer already present");
        }
        waitingQueue.enqueueCustomer(customer);
    }

    /**
     * Finds the table currently occupied by the given customer.
     *
     * @param customer seated customer
     * @return the occupied table
     * @throws IllegalArgumentException if the customer is not seated at a table
     */
    public Table tableFor(Customer customer) {
        Objects.requireNonNull(customer);
        return tables.stream().filter(Table::isOccupied).filter(t -> t.getSeatedCustomer() == customer).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Customer is not at a table"));
    }
    /**
     * Returns the earliest-arriving seated customer who is eligible to pay this turn.
     *
     * @param turn current game turn
     * @return the oldest eligible payment candidate
     * @throws IllegalStateException if no seated customer is eligible to pay
     */
    public Customer oldestPaymentCandidate(int turn) {
        return tables.stream().filter(Table::isOccupied).map(Table::getSeatedCustomer).filter(c -> c.canPay(turn))
                .min(Comparator.comparingInt(Customer::arrivalOrdinal))
                .orElseThrow(() -> new IllegalStateException("No customer is eligible to pay this turn"));
    }

    /**
     * Completes payment for a customer already validated by the Cashier, creates the payment record,
     * marks the customer paid and frees the table.
     *
     * @param customer customer ready to pay
     * @return the immutable payment record
     * @throws IllegalStateException if the customer is not ready to pay or is not seated
     */
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
