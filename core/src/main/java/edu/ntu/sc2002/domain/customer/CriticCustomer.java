package edu.ntu.sc2002.domain.customer;

import edu.ntu.sc2002.domain.menu.Menu;
import edu.ntu.sc2002.domain.menu.MenuItem;
import java.util.Comparator;
import java.util.Objects;

/**
 * Loses 12 satisfaction per waiting turn and pays full price.
 * Always orders Pasta, the slowest item; if it is not on the menu, the most expensive item.
 */
public final class CriticCustomer extends Customer {
    private static final String PREFERRED_ITEM = "Pasta";

    public CriticCustomer(String id, int arrivalOrdinal, long choiceSeed) { super(id, arrivalOrdinal, choiceSeed); }

    @Override public CustomerType getType() { return CustomerType.CRITIC; }
    @Override public int satisfactionLossPerTurn() { return 12; }
    @Override public int calculateBill(int basePriceCents) {
        if (basePriceCents < 0) throw new IllegalArgumentException("Price must be nonnegative");
        return basePriceCents;
    }
    @Override public MenuItem chooseItem(Menu menu) {
        var items = Objects.requireNonNull(menu).getItems();
        return items.stream().filter(i -> i.getName().equalsIgnoreCase(PREFERRED_ITEM)).findFirst()
                .or(() -> items.stream().max(Comparator.comparingInt(MenuItem::getPriceCents)))
                .orElseThrow(() -> new IllegalStateException("Menu is empty"));
    }
}
