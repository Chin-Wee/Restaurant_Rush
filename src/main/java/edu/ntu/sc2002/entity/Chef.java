package edu.ntu.sc2002.entity;
public final class Chef {
    public void advance(Customer customer, int turn) { customer.order().advance(turn); }
}
