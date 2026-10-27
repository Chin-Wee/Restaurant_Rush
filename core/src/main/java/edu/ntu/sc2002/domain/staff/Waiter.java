package edu.ntu.sc2002.domain.staff;
import edu.ntu.sc2002.domain.BillCalculator;
import edu.ntu.sc2002.domain.Order;
import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.menu.Menu;
import edu.ntu.sc2002.domain.menu.MenuItem;
import edu.ntu.sc2002.domain.Restaurant;

public final class Waiter {
    public void seat(Restaurant restaurant, Customer customer) { restaurant.seat(customer); }

    public Order takeOrder(Customer customer, Menu menu, BillCalculator calculator, int turn) {
        MenuItem item = customer.chooseItem(menu);
        return customer.placeOrder(item, calculator.quote(item, customer, turn));
    }

    public void serve(Customer customer, int turn) { customer.serveOrder(turn); }
}
