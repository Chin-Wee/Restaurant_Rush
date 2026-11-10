package edu.ntu.sc2002.domain.staff;

import edu.ntu.sc2002.domain.Order;

/** Kitchen role: advances one order by one preparation unit per turn. */
public final class Chef extends Staff {
    public Chef(String staffId) { super(staffId); }
    /**
     * Advances the given order by one preparation unit.
     *
     * @param order order being prepared
     * @param turn current game turn, recorded when the order becomes ready
     */
    public void prepareOrder(Order order, int turn) { order.advance(turn); }
}
