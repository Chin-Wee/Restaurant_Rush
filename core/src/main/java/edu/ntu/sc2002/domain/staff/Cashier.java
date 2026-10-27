package edu.ntu.sc2002.domain.staff;

import edu.ntu.sc2002.domain.Order;
import edu.ntu.sc2002.domain.Restaurant;
import edu.ntu.sc2002.domain.customer.Customer;
import java.util.Objects;

public final class Cashier extends Staff {
    public Cashier(String staffId) { super(staffId); }
    public void processPayment(Customer customer, Order order, Restaurant restaurant) {
        requireAvailable();
        Objects.requireNonNull(customer);
        Objects.requireNonNull(order);
        Objects.requireNonNull(restaurant);
        if (customer.order() != order || !customer.canPay(currentTurn())) {
            throw new IllegalStateException("Customer is not eligible to pay this turn");
        }
        order.startTurn(currentTurn());
        restaurant.completePayment(customer);
        completeAction();
    }
}
