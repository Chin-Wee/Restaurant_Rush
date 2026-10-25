package edu.ntu.sc2002.entity;
public final class Cashier {
    public PaymentRecord collectOldest(Restaurant restaurant, int turn) {
        Customer customer = restaurant.oldestPaymentCandidate(turn);
        if (customer == null) return null;
        customer.paid();
        PaymentRecord record = new PaymentRecord(customer.id(), customer.order().lockedPriceCents(),
                customer.satisfaction().getValue());
        restaurant.completePayment(customer);
        return record;
    }
}
