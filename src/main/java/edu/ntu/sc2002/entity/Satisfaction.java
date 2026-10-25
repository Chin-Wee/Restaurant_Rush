package edu.ntu.sc2002.entity;
public final class Satisfaction {
    private int value = 100;
    public int getValue() { return value; }
    public void deduct(int points) { value = Math.max(0, value - points); }
    public void add(int points) { value = Math.min(100, value + points); }
}
