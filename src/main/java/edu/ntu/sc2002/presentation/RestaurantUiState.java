package edu.ntu.sc2002.presentation;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

/** Immutable presentation snapshot. Values and legal choices come from the controller. */
public record RestaurantUiState(
        int stage,
        int turn,
        UiPhase phase,
        double revenue,
        OptionalDouble averagePaidSatisfaction,
        List<CustomerSummary> customers,
        List<TableSummary> tables,
        List<OrderSummary> orders,
        List<WaiterChoice> waiterChoices,
        List<ChefChoice> chefChoices,
        boolean canResolveTurn,
        StageThreeStatus stageThree,
        Feedback feedback,
        ResultSummary result) {

    public RestaurantUiState {
        if (stage < 1 || turn < 1 || !Double.isFinite(revenue) || revenue < 0) {
            throw new IllegalArgumentException("Invalid stage, turn, or revenue");
        }
        Objects.requireNonNull(phase);
        averagePaidSatisfaction = Objects.requireNonNull(averagePaidSatisfaction);
        checkSatisfaction(averagePaidSatisfaction);
        customers = List.copyOf(customers);
        tables = List.copyOf(tables);
        orders = List.copyOf(orders);
        waiterChoices = List.copyOf(waiterChoices);
        chefChoices = List.copyOf(chefChoices);
        if ((stage == 3) != (stageThree != null)) {
            throw new IllegalArgumentException("Stage 3 status must match the selected stage");
        }
        if ((phase == UiPhase.COMPLETE) != (result != null)) {
            throw new IllegalArgumentException("Completed snapshots must include a result");
        }
    }

    private static void checkSatisfaction(OptionalDouble satisfaction) {
        if (satisfaction.isPresent()
                && (!Double.isFinite(satisfaction.getAsDouble())
                || satisfaction.getAsDouble() < 0)) {
            throw new IllegalArgumentException("Satisfaction must be finite and nonnegative");
        }
    }

    public enum UiPhase { WAITER, CHEF, RESOLVE, COMPLETE }

    public enum WaiterTask { SEAT, TAKE_ORDER, SERVE, WAIT }

    public enum ChefTask { ADVANCE_ORDER, WAIT }

    public sealed interface Command permits WaiterCommand, ChefCommand, ResolveTurn, ActivateHappyHour {}

    public record WaiterCommand(WaiterChoice choice) implements Command {
        public WaiterCommand { Objects.requireNonNull(choice); }
    }

    public record ChefCommand(ChefChoice choice) implements Command {
        public ChefCommand { Objects.requireNonNull(choice); }
    }

    public record ResolveTurn() implements Command {}

    public record ActivateHappyHour() implements Command {}

    public record WaiterChoice(WaiterTask task, String customerId, String tableId, String orderId, String label) {
        public WaiterChoice {
            Objects.requireNonNull(task);
            requireText(label);
            switch (task) {
                case SEAT -> { Objects.requireNonNull(customerId); Objects.requireNonNull(tableId); }
                case TAKE_ORDER -> Objects.requireNonNull(customerId);
                case SERVE -> Objects.requireNonNull(orderId);
                case WAIT -> {
                    if (customerId != null || tableId != null || orderId != null) {
                        throw new IllegalArgumentException("Wait has no target");
                    }
                }
            }
        }
    }

    public record ChefChoice(ChefTask task, String orderId, String label) {
        public ChefChoice {
            Objects.requireNonNull(task);
            requireText(label);
            if ((task == ChefTask.ADVANCE_ORDER) != (orderId != null)) {
                throw new IllegalArgumentException("Only order advances have an order id");
            }
        }
    }

    public record CustomerSummary(String id, String name, String type, String location, int satisfaction,
            String foodItem, String orderStatus, String paymentStatus) {
        public CustomerSummary {
            requireText(id);
            requireText(name);
            requireText(location);
            requireText(orderStatus);
            requireText(paymentStatus);
            if (satisfaction < 0) {
                throw new IllegalArgumentException("Satisfaction must be finite and nonnegative");
            }
        }
    }

    public record TableSummary(String name, String status, String customerName) {
        public TableSummary {
            requireText(name);
            requireText(status);
        }
    }

    public record OrderSummary(String id, String customerName, String item, String status, boolean comboMeal) {
        public OrderSummary {
            requireText(id);
            requireText(customerName);
            requireText(item);
            requireText(status);
        }
    }

    public record StageThreeStatus(String hostActivity, boolean happyHourAvailable, int happyHourTurnsRemaining) {
        public StageThreeStatus {
            requireText(hostActivity);
            if (happyHourTurnsRemaining < 0
                    || (happyHourTurnsRemaining > 0 && happyHourAvailable)) {
                throw new IllegalArgumentException("Invalid Happy Hour status");
            }
        }
    }

    public record Feedback(String message, boolean rejected) {
        public Feedback { requireText(message); }
    }

    public record ResultSummary(int servedCount, int departures, double revenue,
            OptionalDouble averagePaidSatisfaction, int turnsUsed, boolean victory) {
        public ResultSummary {
            if (servedCount < 0 || departures < 0 || turnsUsed < 0 || !Double.isFinite(revenue) || revenue < 0) {
                throw new IllegalArgumentException("Invalid result metrics");
            }
            averagePaidSatisfaction = Objects.requireNonNull(averagePaidSatisfaction);
            checkSatisfaction(averagePaidSatisfaction);
        }
    }

    private static void requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Text values must not be blank");
        }
    }
}
