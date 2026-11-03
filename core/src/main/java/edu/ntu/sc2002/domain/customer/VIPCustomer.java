package edu.ntu.sc2002.domain.customer;

import edu.ntu.sc2002.domain.menu.MenuItem;
import java.util.List;

/** Loses 5 satisfaction per waiting turn, pays 10% less (rounded to the nearest cent) and picks a seeded random dish. */
public final class VIPCustomer extends Customer {
    public VIPCustomer(String id, int arrivalOrdinal, long choiceSeed) { super(id, arrivalOrdinal, choiceSeed); }

    @Override public CustomerType getType() { return CustomerType.VIP; }
    @Override public void applyTurnEndDecay() { loseSatisfactionIfWaiting(5); }
    @Override public int calculateBill(int basePriceCents) {
        if (basePriceCents < 0) throw new IllegalArgumentException("Price must be nonnegative");
        return (basePriceCents * 9 + 5) / 10;
    }
    @Override public MenuItem chooseMenuItem(List<MenuItem> availableItems) { return seededChoice(availableItems); }
}
