package edu.ntu.sc2002.entity;

public record PaymentRecord(String customerId, int lockedPriceCents, int satisfaction) {
    public PaymentRecord {
        if (customerId == null || customerId.isBlank() || lockedPriceCents < 0 || satisfaction < 0 || satisfaction > 100) {
            throw new IllegalArgumentException("Invalid payment record");
        }
    }
}
