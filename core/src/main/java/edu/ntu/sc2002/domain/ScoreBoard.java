package edu.ntu.sc2002.domain;

import java.util.Objects;

/** Statistics recorded only after successful service, payment or abandonment. */
public final class ScoreBoard {
    private int servedCount;
    private int unhappyDepartures;
    private int revenueCents;
    private int paidSatisfactionTotal;
    private int paidCount;

    public void recordServed() { servedCount++; }
    public void recordDeparture() { unhappyDepartures++; }
    public void recordPayment(PaymentRecord payment) {
        Objects.requireNonNull(payment);
        int revenue = Math.addExact(revenueCents, payment.lockedPriceCents());
        int satisfaction = Math.addExact(paidSatisfactionTotal, payment.satisfaction());
        revenueCents = revenue;
        paidSatisfactionTotal = satisfaction;
        paidCount++;
    }
    public int servedCount() { return servedCount; }
    public int unhappyDepartures() { return unhappyDepartures; }
    public int revenueCents() { return revenueCents; }
    public int paidCount() { return paidCount; }
    public double averagePaidSatisfaction() {
        return paidCount == 0 ? Double.NaN : (double) paidSatisfactionTotal / paidCount;
    }
    /** Current threshold evaluation; the controller's phase determines whether play has ended. */
    public GameResult evaluateResult() {
        return paidCount >= 4 && revenueCents >= 4500 && averagePaidSatisfaction() >= 60
                ? GameResult.VICTORY : GameResult.DEFEAT;
    }
}
