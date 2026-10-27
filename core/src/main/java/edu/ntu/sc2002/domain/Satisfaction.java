package edu.ntu.sc2002.domain;

/** Regular satisfaction is bounded to 0..100. */
public final class Satisfaction {
    private int value = 100;

    public int getValue() { return value; }

    public void deduct(int points) {
        if (points < 0) throw new IllegalArgumentException("Points must be nonnegative");
        value = (int) Math.max(0L, (long) value - points);
    }

    public void add(int points) {
        if (points < 0) throw new IllegalArgumentException("Points must be nonnegative");
        value = (int) Math.min(100L, (long) value + points);
    }
}
