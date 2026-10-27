package edu.ntu.sc2002.domain.staff;
import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.PaymentRecord;
import edu.ntu.sc2002.domain.Restaurant;

public final class Cashier {
    public PaymentRecord collectOldest(Restaurant restaurant, int turn) {
        Customer customer = restaurant.oldestPaymentCandidate(turn);
        return customer == null ? null : restaurant.completePayment(customer);
    }
}
