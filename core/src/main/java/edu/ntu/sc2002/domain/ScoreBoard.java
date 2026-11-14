package edu.ntu.sc2002.domain;

import java.util.Objects;

/**
 * Mutable aggregate for service, payment and abandonment statistics. Only successful domain
 * operations update it; the controller consults it for snapshots and the final result.
 */
public final class ScoreBoard {
    private int servedCount;
    private int unhappyDepartures;
    private int revenueCents;
    private int paidSatisfactionTotal;
    private int paidCount;

    /** Records one successfully served order. */
    public void recordServed() { servedCount++; }

    /** Records one customer who left without paying. */
    public void recordDeparture() { unhappyDepartures++; }

    /**
     * Records a completed payment and adds its locked bill and paid-time satisfaction to the totals.
     *
     * @param payment completed payment record
     * @throws ArithmeticException if the accumulated integer totals overflow
     */
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

    /**
     * Returns the average satisfaction recorded when customers paid.
     *
     * @return paid-time average, or {@link Double#NaN} before the first payment
     */
    public double averagePaidSatisfaction() {
        return paidCount == 0 ? Double.NaN : (double) paidSatisfactionTotal / paidCount;
    }

    /**
     * Evaluates the three victory thresholds against the current totals. The controller's phase,
     * not this provisional result alone, determines whether play has ended.
     *
     * @return {@link GameResult#VICTORY} when all thresholds are met, otherwise {@link GameResult#DEFEAT}
     */
    public GameResult evaluateResult() {
        return paidCount >= 4 && revenueCents >= 4500 && averagePaidSatisfaction() >= 60
                ? GameResult.VICTORY : GameResult.DEFEAT;
    }
}
