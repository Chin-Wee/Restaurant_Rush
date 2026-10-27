package edu.ntu.sc2002.domain.menu;

/** Stage 1's concrete menu item. */
public final class Dish extends MenuItem {
    public Dish(String name, int basePriceCents, int preparationUnits) {
        super(name, basePriceCents, preparationUnits);
    }

    @Override public String name() { return super.name(); }
    @Override public int priceCents() { return super.priceCents(); }
    @Override public int preparationUnits() { return super.preparationUnits(); }
}
