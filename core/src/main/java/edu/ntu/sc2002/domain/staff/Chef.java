package edu.ntu.sc2002.domain.staff;

import edu.ntu.sc2002.domain.Order;
import java.util.Objects;

public final class Chef extends Staff {
    public Chef(String staffId) { super(staffId); }
    public void prepareOrder(Order order) {
        requireAvailable();
        Objects.requireNonNull(order);
        if (currentTurn() > 0) order.startTurn(currentTurn());
        order.advancePreparation();
        completeAction();
    }
}
