package edu.ntu.sc2002.domain;
public final class ScoreBoard {
    private int servedCount;
    private int unhappyDepartures;
    private int revenueCents;
    private int paidSatisfactionTotal;
    private int paidCount;
    public void recordServed() { servedCount++; }
    public void recordPayment(PaymentRecord record) {
        paidCount++; revenueCents += record.lockedPriceCents(); paidSatisfactionTotal += record.satisfaction();
    }
    public void recordDeparture() { unhappyDepartures++; }
    public int servedCount() { return servedCount; }
    public int unhappyDepartures() { return unhappyDepartures; }
    public int revenueCents() { return revenueCents; }
    public int paidCount() { return paidCount; }
    public double averagePaidSatisfaction() { return paidCount == 0 ? Double.NaN : (double) paidSatisfactionTotal / paidCount; }
    public double getAveragePaidSatisfaction() { return averagePaidSatisfaction(); }
    public GameResult evaluateResult() {
        return paidCount >= 4 && revenueCents >= 4500 && paidCount > 0 && averagePaidSatisfaction() >= 60
                ? GameResult.VICTORY : GameResult.DEFEAT;
    }
}
