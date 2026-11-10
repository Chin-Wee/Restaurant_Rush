package edu.ntu.sc2002.application.contract;

import edu.ntu.sc2002.domain.HappyHourStatus;
import edu.ntu.sc2002.domain.OrderStatus;
import edu.ntu.sc2002.domain.customer.CustomerState;
import edu.ntu.sc2002.domain.customer.CustomerType;
import java.util.List;

/**
 * Immutable engine data for customers currently in the queue or at a table.
 * Paid and unhappy departure totals remain in the statistics after customers leave.
 * averagePaidSatisfaction is null until the first payment. Empty tables have a null occupantId;
 * customer tableId and queuePosition are null when that location does not apply.
 */
public record GameSnapshot(int turn, TurnPhase phase, int revenueCents, Double averagePaidSatisfaction,
        int servedCount, int paidCount, int unhappyDepartures, int turnsUsed,
        List<String> waitingQueue, List<TableView> tables, List<CustomerView> customers, List<OrderView> orders,
        List<WaiterAction> waiterActions, List<ChefChoiceDTO> chefActions, List<HostAction> hostActions,
        HappyHourStatus happyHourStatus, int happyHourRemainingTurns) {
    public GameSnapshot {
        waitingQueue = List.copyOf(waitingQueue);
        tables = List.copyOf(tables);
        customers = List.copyOf(customers);
        orders = List.copyOf(orders);
        waiterActions = List.copyOf(waiterActions);
        chefActions = List.copyOf(chefActions);
        hostActions = List.copyOf(hostActions);
    }

    /** Display row for one active customer; location fields are null when not applicable. */
    public record CustomerView(String id, int arrivalOrdinal, CustomerType type, CustomerState state, int satisfaction,
            Integer tableId, Integer queuePosition) {}

    /** Display row for one table; {@code occupantId} is null when the table is free. */
    public record TableView(int id, String occupantId) {}

    /** Display row for one active order. */
    public record OrderView(String customerId, String item, OrderStatus status,
            int unitsDone, int preparationUnits, int lockedPriceCents) {}
    /** Seating names the table the engine will allocate; callers do not choose a different table. */
    public record WaiterAction(WaiterChoiceDTO decision, Integer tableId, String customerId) {}
    /** The Host's available seat target and the table the engine will allocate, or WAIT with nulls. */
    public record HostAction(HostChoiceDTO decision, Integer tableId, String customerId) {}
}
