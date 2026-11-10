package edu.ntu.sc2002.domain.staff;

import edu.ntu.sc2002.domain.PaymentRecord;
import edu.ntu.sc2002.domain.Restaurant;
import edu.ntu.sc2002.domain.customer.Customer;

/** Automatic payment role: validates payment eligibility and completes at most one payment per turn. */
public final class Cashier extends Staff {
    public Cashier(String staffId) { super(staffId); }
    /**
     * Validates and completes one customer payment.
     *
     * @param customer customer selected for payment
     * @param restaurant restaurant that owns the customer's table
     * @param turn current game turn
     * @return the immutable payment record
     * @throws IllegalStateException if the customer is not eligible to pay this turn
     */
    public PaymentRecord processPayment(Customer customer, Restaurant restaurant, int turn) {
        if (!customer.canPay(turn)) throw new IllegalStateException("Customer is not eligible to pay this turn");
        return restaurant.completePayment(customer);
    }
}
