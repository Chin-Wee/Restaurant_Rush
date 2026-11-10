package edu.ntu.sc2002.domain.menu;
import java.util.ArrayList;
import java.util.List;
public final class Menu {
    private final List<MenuItem> items = new ArrayList<>();
    public Menu() {
        List<MenuItem> base = new ArrayList<>();
        for (FoodTypes type : FoodTypes.values()) base.add(switch (type) {
            case SALAD -> new MenuItem("Salad", 900, 1);
            case BURGER -> new MenuItem("Burger", 1500, 1);
            case PASTA -> new MenuItem("Pasta", 1800, 2);
        });
        items.addAll(base);
        // Stage 3: every pair of base items also appears as a combo.
        for (int first = 0; first < base.size(); first++) {
            for (int second = first + 1; second < base.size(); second++) {
                items.add(new ComboMeal(base.get(first).getName() + " + " + base.get(second).getName(),
                        base.get(first), base.get(second)));
            }
        }
    }
    public void addItem(MenuItem item) {
        java.util.Objects.requireNonNull(item);
        items.add(item);
    }
    public List<MenuItem> getItems() { return List.copyOf(items); }
    /** Requires the name of an existing menu item. */
    public MenuItem getItemByName(String name) {
        return items.stream().filter(i -> i.getName().equalsIgnoreCase(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown menu item: " + name));
    }
}
