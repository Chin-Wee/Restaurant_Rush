package edu.ntu.sc2002.domain.staff;
import edu.ntu.sc2002.domain.customer.Customer;
public final class Chef {
    public void advance(Customer customer, int turn) { customer.order().advance(turn); }
}
