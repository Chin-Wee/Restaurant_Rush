package edu.ntu.sc2002.domain;
import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.customer.CustomerState;

import java.util.Objects;

public final class Table {
    private final int tableId;
    private Customer occupant;

    public Table(int tableId) {
        if (tableId < 1) throw new IllegalArgumentException("Invalid table ID");
        this.tableId = tableId;
    }

    public int tableId() { return tableId; }

    public void assign(Customer customer) {
        Objects.requireNonNull(customer);
        if (isOccupied()) throw new IllegalStateException("Table is occupied");
        customer.markSeated();
        occupant = customer;
    }

    public void vacate() {
        if (occupant != null && occupant.state() != CustomerState.PAID && occupant.state() != CustomerState.LEFT) {
            throw new IllegalStateException("Table remains occupied until payment or abandonment");
        }
        occupant = null;
    }
    public boolean isOccupied() { return occupant != null; }
    public Customer getOccupant() { return occupant; }
}
