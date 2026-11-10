package edu.ntu.sc2002.domain;

import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.customer.CustomerState;
import java.util.Objects;

/** A physical table that owns at most one seated customer and enforces payment/abandonment before release. */
public final class Table {
    private final int tableId;
    private Customer seatedCustomer;

    /**
     * Creates a table with a positive identifier.
     *
     * @param tableId positive table number
     * @throws IllegalArgumentException if the ID is less than one
     */
    public Table(int tableId) {
        if (tableId < 1) throw new IllegalArgumentException("Invalid table ID");
        this.tableId = tableId;
    }

    public int tableId() { return tableId; }

    /**
     * Seats a waiting customer at this table.
     *
     * @param customer customer to seat
     * @throws IllegalStateException if the table is occupied or the customer is not waiting
     */
    public void assignCustomer(Customer customer) {
        Objects.requireNonNull(customer);
        if (isOccupied()) throw new IllegalStateException("Table is occupied");
        if (customer.getState() != CustomerState.WAITING) throw new IllegalStateException("Customer is not queued");
        customer.markSeated();
        seatedCustomer = customer;
    }

    /**
     * Frees the table after payment or abandonment.
     *
     * @throws IllegalStateException if the seated customer has not paid or left
     */
    public void releaseTable() {
        if (seatedCustomer != null && seatedCustomer.getState() != CustomerState.PAID
                && seatedCustomer.getState() != CustomerState.LEFT) {
            throw new IllegalStateException("Table remains occupied until payment or abandonment");
        }
        seatedCustomer = null;
    }

    public boolean isOccupied() { return seatedCustomer != null; }

    /**
     * Returns the seated customer.
     *
     * @return the occupying customer
     * @throws IllegalStateException if the table is empty
     */
    public Customer getSeatedCustomer() {
        if (!isOccupied()) throw new IllegalStateException("Table is empty");
        return seatedCustomer;
    }
}
