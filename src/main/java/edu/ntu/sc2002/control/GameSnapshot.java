package edu.ntu.sc2002.control;
import java.util.List;
public record GameSnapshot(int turn, TurnPhase phase, int revenueCents, double averagePaidSatisfaction,
        List<String> waitingQueue, List<String> tables, List<String> customers, List<String> orders,
        List<String> availableStaffTasks) {
    public GameSnapshot {
        waitingQueue = List.copyOf(waitingQueue); tables = List.copyOf(tables);
        customers = List.copyOf(customers); orders = List.copyOf(orders);
        availableStaffTasks = List.copyOf(availableStaffTasks);
    }
}
