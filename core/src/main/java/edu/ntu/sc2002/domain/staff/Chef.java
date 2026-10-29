package edu.ntu.sc2002.domain.staff;

import edu.ntu.sc2002.domain.Order;

public final class Chef extends Staff {
    public Chef(String staffId) { super(staffId); }
    public void prepareOrder(Order order, int turn) { order.advance(turn); }
}
