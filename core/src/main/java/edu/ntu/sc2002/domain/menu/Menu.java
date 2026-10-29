package edu.ntu.sc2002.domain.menu;
import java.util.ArrayList;
import java.util.List;
public final class Menu {
    private final List<MenuItem> items = new ArrayList<>();
    public Menu() {
        for (FoodTypes type : FoodTypes.values()) items.add(switch (type) {
            case SALAD -> new MenuItem("Salad", 900, 1);
            case BURGER -> new MenuItem("Burger", 1500, 1);
            case PASTA -> new MenuItem("Pasta", 1800, 2);
        });
    }
    public void addItem(MenuItem item) {
        java.util.Objects.requireNonNull(item);
        items.add(new MenuItem(item.getName(), item.getPriceCents(), item.getPrepUnitsNeeded()));
    }
    public List<MenuItem> getItems() { return List.copyOf(items); }
    /** Requires the name of an existing menu item. */
    public MenuItem getItemByName(String name) {
        return items.stream().filter(i -> i.getName().equalsIgnoreCase(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown menu item: " + name));
    }
}
