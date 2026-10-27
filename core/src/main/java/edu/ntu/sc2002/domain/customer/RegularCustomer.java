package edu.ntu.sc2002.domain.customer;

/** Stage 1's only customer type: loses 8 satisfaction per turn spent waiting. */
public final class RegularCustomer extends Customer {
    public RegularCustomer(String id, int arrivalOrdinal, long choiceSeed) {
        super(id, arrivalOrdinal, choiceSeed);
    }

    @Override public int satisfactionLossPerTurn() { return 8; }
}
