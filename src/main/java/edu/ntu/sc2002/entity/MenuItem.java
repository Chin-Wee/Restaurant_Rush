package edu.ntu.sc2002.entity;
import java.util.Objects;
public record MenuItem(String name, int basePriceCents, int preparationUnits) {
    public MenuItem {
        Objects.requireNonNull(name);
        if (name.isBlank() || basePriceCents < 0 || preparationUnits < 1) throw new IllegalArgumentException();
    }
    public int priceCents() { return basePriceCents; }
}
