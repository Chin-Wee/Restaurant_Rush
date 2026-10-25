package edu.ntu.sc2002.entity;
public record PaymentRecord(String customerId, int lockedPriceCents, int satisfaction) {}
