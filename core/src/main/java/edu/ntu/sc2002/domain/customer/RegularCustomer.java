package edu.ntu.sc2002.domain.customer;

/** Loses 8 satisfaction per waiting turn, pays full price and picks a seeded random dish. */
public final class RegularCustomer extends Customer {
    public RegularCustomer(String id, int arrivalOrdinal, long choiceSeed) { super(id, arrivalOrdinal, choiceSeed); }

    @Override public CustomerType getType() { return CustomerType.REGULAR; }
    @Override public int satisfactionLossPerTurn() { return 8; }
    @Override public int calculateBill(int basePriceCents) {
        if (basePriceCents < 0) throw new IllegalArgumentException("Price must be nonnegative");
        return basePriceCents;
    }
}
