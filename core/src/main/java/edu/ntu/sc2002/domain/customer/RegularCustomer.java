package edu.ntu.sc2002.domain.customer;

import edu.ntu.sc2002.domain.menu.MenuItem;
import java.util.List;

/** Loses 8 satisfaction per waiting turn, pays full price and picks a seeded random dish. */
public final class RegularCustomer extends Customer {
    public RegularCustomer(String id, int arrivalOrdinal, long choiceSeed) { super(id, arrivalOrdinal, choiceSeed); }

    @Override public CustomerType getType() { return CustomerType.REGULAR; }
    @Override public void applyTurnEndDecay() { loseSatisfactionIfWaiting(8); }
    @Override public int calculateBill(int basePriceCents) {
        if (basePriceCents < 0) throw new IllegalArgumentException("Price must be nonnegative");
        return basePriceCents;
    }
    @Override public MenuItem chooseMenuItem(List<MenuItem> availableItems) { return seededChoice(availableItems); }
}
