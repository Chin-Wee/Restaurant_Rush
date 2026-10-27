package edu.ntu.sc2002.domain.customer;

/** Phase 1 concrete customer — loses 8 satisfaction per waiting turn. */
public class RegularCustomer extends Customer {

    public RegularCustomer(String id, int arrivalOrdinal, long choiceSeed) {
        super(id, arrivalOrdinal, choiceSeed);
    }

    @Override
    public int satisfactionLossPerTurn() { return 8; }
}
