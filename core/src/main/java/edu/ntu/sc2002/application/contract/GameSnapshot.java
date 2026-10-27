package edu.ntu.sc2002.application.contract;

import edu.ntu.sc2002.domain.OrderStatus;
import edu.ntu.sc2002.domain.customer.CustomerState;
import java.util.List;

/** Immutable engine data. No formatted labels, JavaFX types, or mutable domain objects. */
public record GameSnapshot(int turn, TurnPhase phase, int revenueCents, Double averagePaidSatisfaction,
        int servedCount, int paidCount, int unhappyDepartures, int turnsUsed,
        List<String> waitingQueue, List<TableView> tables, List<CustomerView> customers, List<OrderView> orders,
        List<WaiterAction> waiterActions, List<ChefChoiceDTO> chefActions) {
    public GameSnapshot {
        waitingQueue = List.copyOf(waitingQueue);
        tables = List.copyOf(tables);
        customers = List.copyOf(customers);
        orders = List.copyOf(orders);
        waiterActions = List.copyOf(waiterActions);
        chefActions = List.copyOf(chefActions);
    }

    public record CustomerView(String id, int arrivalOrdinal, CustomerState state, int satisfaction,
            Integer tableId, Integer queuePosition) {}
    public record TableView(int id, String occupantId) {}
    public record OrderView(int id, String customerId, String item, OrderStatus status,
            int unitsDone, int preparationUnits, int lockedPriceCents) {}
    /** Seating names the table the engine will allocate; callers do not choose a different table. */
    public record WaiterAction(WaiterChoiceDTO decision, Integer tableId, String customerId) {}
}
