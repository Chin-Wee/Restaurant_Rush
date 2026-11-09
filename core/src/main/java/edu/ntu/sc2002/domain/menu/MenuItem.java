package edu.ntu.sc2002.domain.menu;

import java.util.Objects;

/** A dish with a fixed price and preparation requirement; Stage 3 combos extend it. */
public class MenuItem {
    private final String name;
    private final int priceCents;
    private final int prepUnitsNeeded;

    public MenuItem(String name, int priceCents, int prepUnitsNeeded) {
        Objects.requireNonNull(name);
        if (name.isBlank() || priceCents < 0 || prepUnitsNeeded < 1) throw new IllegalArgumentException("Invalid menu item");
        this.name = name;
        this.priceCents = priceCents;
        this.prepUnitsNeeded = prepUnitsNeeded;
    }

    public String getName() { return name; }
    public int getPriceCents() { return priceCents; }
    public int getPrepUnitsNeeded() { return prepUnitsNeeded; }
}
