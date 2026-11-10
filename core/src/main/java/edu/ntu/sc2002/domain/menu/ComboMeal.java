package edu.ntu.sc2002.domain.menu;

import java.util.Objects;

/**
 * A Stage 3 combo made of two existing menu items: a full menu item priced at 90% of the
 * components' combined price (rounded to the nearest cent) needing the sum of their
 * preparation units. Example: Salad + Pasta = ($9 + $18) x 0.9 = $24.30, 3 units.
 */
public final class ComboMeal extends MenuItem {
    private final MenuItem item1;
    private final MenuItem item2;

    /**
     * Creates a combo from two existing items. The combo price is 90% of the component total,
     * rounded to the nearest cent, and its preparation requirement is the sum of both components.
     *
     * @param name display name for the combo
     * @param item1 first component
     * @param item2 second component
     */
    public ComboMeal(String name, MenuItem item1, MenuItem item2) {
        super(name, combinedPriceCents(item1, item2),
                item1.getPrepUnitsNeeded() + item2.getPrepUnitsNeeded());
        this.item1 = item1;
        this.item2 = item2;
    }

    public MenuItem getItem1() { return item1; }
    public MenuItem getItem2() { return item2; }

    private static int combinedPriceCents(MenuItem first, MenuItem second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        return (int) ((((long) first.getPriceCents() + second.getPriceCents()) * 90 + 50) / 100);
    }
}
