package edu.ntu.sc2002.domain;

/**
 * Immutable record of one completed payment: paying customer, locked bill and satisfaction at
 * payment time. The compact constructor rejects blank IDs, negative prices and satisfaction
 * outside the 0..100 range.
 *
 * @param customerId paying customer's ID
 * @param lockedPriceCents final bill in cents
 * @param satisfaction satisfaction recorded at payment time
 */
public record PaymentRecord(String customerId, int lockedPriceCents, int satisfaction) {
    public PaymentRecord {
        if (customerId == null || customerId.isBlank() || lockedPriceCents < 0 || satisfaction < 0 || satisfaction > 100) {
            throw new IllegalArgumentException("Invalid payment record");
        }
    }
}
