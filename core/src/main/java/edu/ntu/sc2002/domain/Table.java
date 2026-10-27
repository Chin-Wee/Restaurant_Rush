package edu.ntu.sc2002.domain;

import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.customer.CustomerState;
import java.util.Objects;

public final class Table {
    private final int tableId;
    private Customer seatedCustomer;

    public Table(int tableId) {
        if (tableId < 1) throw new IllegalArgumentException("Invalid table ID");
        this.tableId = tableId;
    }

    public int tableId() { return tableId; }
    public void assignCustomer(Customer customer) {
        Objects.requireNonNull(customer);
        if (isOccupied()) throw new IllegalStateException("Table is occupied");
        if (customer.getState() != CustomerState.QUEUED) throw new IllegalStateException("Customer is not queued");
        customer.setState(CustomerState.SEATED);
        seatedCustomer = customer;
    }
    public void releaseTable() {
        if (seatedCustomer != null && seatedCustomer.getState() != CustomerState.PAID
                && seatedCustomer.getState() != CustomerState.ABANDONED) {
            throw new IllegalStateException("Table remains occupied until payment or abandonment");
        }
        seatedCustomer = null;
    }
    /** Occupancy is derived from the single customer reference. */
    public boolean isOccupied() { return seatedCustomer != null; }
    public Customer getSeatedCustomer() { return seatedCustomer; }
}
