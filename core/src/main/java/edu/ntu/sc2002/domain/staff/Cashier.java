package edu.ntu.sc2002.domain.staff;

import edu.ntu.sc2002.domain.PaymentRecord;
import edu.ntu.sc2002.domain.Restaurant;
import edu.ntu.sc2002.domain.customer.Customer;

public final class Cashier extends Staff {
    public Cashier(String staffId) { super(staffId); }
    public PaymentRecord processPayment(Customer customer, Restaurant restaurant, int turn) {
        if (!customer.canPay(turn)) throw new IllegalStateException("Customer is not eligible to pay this turn");
        return restaurant.completePayment(customer);
    }
}
