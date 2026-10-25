package edu.ntu.sc2002.entity;
import java.util.ArrayList;
import java.util.List;
public final class Menu {
    private final List<MenuItem> items = new ArrayList<>(List.of(new MenuItem("Salad", 900, 1),
            new MenuItem("Burger", 1500, 1), new MenuItem("Pasta", 1800, 2)));
    public void addItem(MenuItem item) { items.add(item); }
    public List<MenuItem> getItems() { return items; }
    public MenuItem getItemByName(String name) {
        return items.stream().filter(i -> i.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
}
