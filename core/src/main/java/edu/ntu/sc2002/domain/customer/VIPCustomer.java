package edu.ntu.sc2002.domain.customer;

/** Loses 5 satisfaction per waiting turn, pays 10% less (rounded to the nearest cent) and picks a seeded random dish. */
public final class VIPCustomer extends Customer {
    private static final int DISCOUNT_PERCENT = 10;

    public VIPCustomer(String id, int arrivalOrdinal, long choiceSeed) { super(id, arrivalOrdinal, choiceSeed); }

    @Override public CustomerType getType() { return CustomerType.VIP; }
    @Override public int satisfactionLossPerTurn() { return 5; }
    @Override public int calculateBill(int basePriceCents) {
        if (basePriceCents < 0) throw new IllegalArgumentException("Price must be nonnegative");
        return (int) (((long) basePriceCents * (100 - DISCOUNT_PERCENT) + 50) / 100);
    }
}
