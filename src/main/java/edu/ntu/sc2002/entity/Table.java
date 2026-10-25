package edu.ntu.sc2002.entity;
public final class Table {
    private final int tableId;
    private Customer occupant;
    public Table(int tableId) { this.tableId = tableId; }
    public int tableId() { return tableId; }
    public void assign(Customer customer) {
        if (isOccupied()) throw new IllegalStateException("Table is occupied");
        occupant = customer; customer.seat(tableId);
    }
    public void vacate() { occupant = null; }
    public boolean isOccupied() { return occupant != null; }
    public Customer getOccupant() { return occupant; }
}
