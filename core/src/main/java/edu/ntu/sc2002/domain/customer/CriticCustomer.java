package edu.ntu.sc2002.domain.customer;

import edu.ntu.sc2002.domain.menu.MenuItem;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Loses 12 satisfaction per waiting turn and pays full price.
 * Always orders their preferred dish; if it is not on the menu, the most expensive item.
 */
public final class CriticCustomer extends Customer {
    private final String preferredItemName;

    public CriticCustomer(String id, int arrivalOrdinal, String preferredItemName) {
        super(id, arrivalOrdinal, 0);
        Objects.requireNonNull(preferredItemName);
        if (preferredItemName.isBlank()) throw new IllegalArgumentException("Invalid preferred item");
        this.preferredItemName = preferredItemName;
    }

    @Override public CustomerType getType() { return CustomerType.CRITIC; }
    @Override public void applyTurnEndDecay() { loseSatisfactionIfWaiting(12); }
    @Override public int calculateBill(int basePriceCents) {
        if (basePriceCents < 0) throw new IllegalArgumentException("Price must be nonnegative");
        return basePriceCents;
    }
    @Override public MenuItem chooseMenuItem(List<MenuItem> availableItems) {
        Objects.requireNonNull(availableItems);
        return availableItems.stream().filter(i -> i.getName().equalsIgnoreCase(preferredItemName)).findFirst()
                .or(() -> availableItems.stream().max(Comparator.comparingInt(MenuItem::getPriceCents)))
                .orElseThrow(() -> new IllegalStateException("Menu is empty"));
    }
}
