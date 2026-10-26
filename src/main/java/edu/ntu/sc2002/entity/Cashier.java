package edu.ntu.sc2002.entity;

public final class Cashier {
    public PaymentRecord collectOldest(Restaurant restaurant, int turn) {
        Customer customer = restaurant.oldestPaymentCandidate(turn);
        return customer == null ? null : restaurant.completePayment(customer);
    }
}
