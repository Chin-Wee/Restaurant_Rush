package edu.ntu.sc2002.domain;

import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.customer.CustomerState;
import edu.ntu.sc2002.domain.menu.Menu;
import edu.ntu.sc2002.domain.staff.Cashier;
import edu.ntu.sc2002.domain.staff.Chef;
import edu.ntu.sc2002.domain.staff.Staff;
import edu.ntu.sc2002.domain.staff.Waiter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public final class Restaurant {
    private int totalRevenueCents;
    private int paidCustomerCount;
    private int totalPaidSatisfaction;
    private int servedCount;
    private int unhappyDepartures;
    private final List<Table> tables;
    private final WaitingQueue waitingQueue = new WaitingQueue();
    private final List<Order> activeOrders = new ArrayList<>();
    private final List<Customer> customers = new ArrayList<>();
    private final Menu menu = new Menu();
    private final List<Staff> staffList = new ArrayList<>();

    public Restaurant() { this(2); }
    public Restaurant(int numTables) {
        if (numTables < 1) throw new IllegalArgumentException("At least one table is required");
        tables = IntStream.rangeClosed(1, numTables).mapToObj(Table::new).toList();
        addStaff(new Waiter("Waiter"));
        addStaff(new Chef("Chef"));
        addStaff(new Cashier("Cashier"));
    }
    public void addRevenue(int cents) {
        if (cents < 0) throw new IllegalArgumentException("Revenue must be nonnegative");
        totalRevenueCents = Math.addExact(totalRevenueCents, cents);
    }
    public void recordPaidCustomer(int satisfaction) {
        if (satisfaction < 0 || satisfaction > 100) throw new IllegalArgumentException("Invalid satisfaction");
        totalPaidSatisfaction = Math.addExact(totalPaidSatisfaction, satisfaction);
        paidCustomerCount++;
    }
    public double getAverageSatisfaction() {
        return paidCustomerCount == 0 ? Double.NaN : (double) totalPaidSatisfaction / paidCustomerCount;
    }
    public List<Table> getTables() { return tables; }
    public WaitingQueue getWaitingQueue() { return waitingQueue; }
    public List<Order> getActiveOrders() { return List.copyOf(activeOrders); }
    public List<Customer> customers() { return List.copyOf(customers); }
    public Menu menu() { return menu; }
    /** Register one staff member per concrete role. */
    public void addStaff(Staff staff) {
        Objects.requireNonNull(staff);
        if (staffList.stream().anyMatch(s -> s.getClass() == staff.getClass())) {
            throw new IllegalArgumentException("That staff role is already registered");
        }
        staffList.add(staff);
    }
    public List<Staff> getStaffList() { return List.copyOf(staffList); }
    /** Find exactly one matching role; missing or ambiguous lookups are rejected. */
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
    public int revenueCents() { return totalRevenueCents; }
    public int paidCount() { return paidCustomerCount; }
    public int servedCount() { return servedCount; }
    public int unhappyDepartures() { return unhappyDepartures; }
    public void recordServed() { servedCount++; }
    public GameResult evaluateResult() {
        return paidCustomerCount >= 4 && totalRevenueCents >= 4500 && getAverageSatisfaction() >= 60
                ? GameResult.VICTORY : GameResult.DEFEAT;
    }

    public void registerArrival(Customer customer) {
        Objects.requireNonNull(customer);
        if (customers.stream().anyMatch(c -> c.getId().equalsIgnoreCase(customer.getId()))) {
            throw new IllegalArgumentException("Customer already registered");
        }
        int ordinal = customers.size() + 1;
        if (customer.arrivalOrdinal() != 0 && customer.arrivalOrdinal() != ordinal) {
            throw new IllegalArgumentException("Customer arrival order differs from registration order");
        }
        waitingQueue.addCustomer(customer);
        customer.recordArrival(ordinal);
        customers.add(customer);
    }
    public void registerOrder(Order order) {
        Objects.requireNonNull(order);
        if (!customers.contains(order.getCustomer()) || order.getCustomer().order() != order
                || order.getStatus() != OrderStatus.PENDING || activeOrders.contains(order)
                || customers.stream().map(Customer::order).filter(Objects::nonNull)
                        .anyMatch(o -> o != order && o.getOrderId() == order.getOrderId())) {
            throw new IllegalArgumentException("Order is not new or its customer is unregistered");
        }
        activeOrders.add(order);
    }
    public Table seat(Customer customer) {
        if (!waitingQueue.getQueuedCustomers().contains(customer)) throw new IllegalArgumentException("Customer is not queued");
        Table table = tables.stream().filter(t -> !t.isOccupied()).findFirst()
                .orElseThrow(() -> new IllegalStateException("No table is free"));
        if (!getStaff(Waiter.class).seatCustomer(waitingQueue, table, customer)) throw new IllegalStateException("Customer cannot be seated");
        return table;
    }
    /** A table's customer reference is the sole source of assignment. */
    public Table tableFor(Customer customer) {
        Objects.requireNonNull(customer);
        return tables.stream().filter(t -> t.getSeatedCustomer() == customer).findFirst().orElse(null);
    }
    /** Oldest means earliest arrival among eligible customers. */
    public Customer oldestPaymentCandidate(int turn) {
        return customers.stream().filter(c -> c.canPay(turn))
                .min(Comparator.comparingInt(Customer::arrivalOrdinal)).orElse(null);
    }
    public PaymentRecord completePayment(Customer customer) {
        if (!customers.contains(customer) || customer.getState() != CustomerState.READY_TO_PAY
                || !activeOrders.contains(customer.order())) {
            throw new IllegalStateException("Customer is not ready to pay");
        }
        PaymentRecord record = new PaymentRecord(customer.getId(), customer.order().lockedPriceCents(), customer.getSatisfaction());
        // Reject accounting overflow before committing the customer's payment.
        Math.addExact(totalRevenueCents, record.lockedPriceCents());
        Math.addExact(totalPaidSatisfaction, record.satisfaction());
        customer.paid();
        addRevenue(record.lockedPriceCents());
        recordPaidCustomer(record.satisfaction());
        activeOrders.remove(customer.order());
        tables.stream().filter(t -> t.getSeatedCustomer() == customer).forEach(Table::releaseTable);
        return record;
    }
    public void abandon(Customer customer) {
        if (!customers.contains(customer)) throw new IllegalArgumentException("Customer is not registered");
        customer.leave();
        waitingQueue.remove(customer);
        activeOrders.remove(customer.order());
        tables.stream().filter(t -> t.getSeatedCustomer() == customer).forEach(Table::releaseTable);
        unhappyDepartures++;
    }
}
