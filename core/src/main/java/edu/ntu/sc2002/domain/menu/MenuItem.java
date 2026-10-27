package edu.ntu.sc2002.domain.menu;
import java.util.Objects;

/** Something the restaurant sells. Concrete kinds of item extend this type. */
public abstract class MenuItem {
    private final String name;
    private final int basePriceCents;
    private final int preparationUnits;

    protected MenuItem(String name, int basePriceCents, int preparationUnits) {
        Objects.requireNonNull(name);
        if (name.isBlank() || basePriceCents < 0 || preparationUnits < 1) throw new IllegalArgumentException();
        this.name = name;
        this.basePriceCents = basePriceCents;
        this.preparationUnits = preparationUnits;
    }

    public String name() { return name; }
    public int priceCents() { return basePriceCents; }
    public int preparationUnits() { return preparationUnits; }
}
