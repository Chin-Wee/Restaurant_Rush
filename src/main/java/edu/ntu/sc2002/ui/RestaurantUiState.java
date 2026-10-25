package edu.ntu.sc2002.ui;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

/** Immutable presentation snapshot. Values and legal choices come from the controller. */
public record RestaurantUiState(
        int stage,
        int turn,
        TurnPhase phase,
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
        GameResult result) {

    public RestaurantUiState {
        if (stage < 1 || stage > 3 || turn < 1 || !Double.isFinite(revenue) || revenue < 0) {
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
        if ((phase == TurnPhase.COMPLETE) != (result != null)) {
            throw new IllegalArgumentException("Completed snapshots must include a result");
        }
    }

    public static RestaurantUiState preview(int stage) {
        List<CustomerSummary> customers = switch (stage) {
            case 1 -> List.of(
                    new CustomerSummary("bob", "Bob", null, "Table 1", 76, null, "No order", "Not applicable"),
                    new CustomerSummary("mia", "Mia", null, "Queue #1", 100, null, "No order", "Not applicable"));
            case 2 -> List.of(
                    new CustomerSummary("bob", "Bob", "Regular", "Table 1", 76, "Burger", "READY", "Automatic when due"),
                    new CustomerSummary("mia", "Mia", "VIP", "Queue #1", 96, null, "No order", "Not applicable"),
                    new CustomerSummary("lee", "Lee", "Critic", "Queue #2", 88, null, "No order", "Not applicable"));
            case 3 -> List.of(
                    new CustomerSummary("bob", "Bob", "Regular", "Table 1", 76, "ComboMeal", "READY", "Automatic when due"),
                    new CustomerSummary("mia", "Mia", "VIP", "Table 2", 96, null, "No order", "Not applicable"),
                    new CustomerSummary("lee", "Lee", "Critic", "Queue #1", 88, null, "No order", "Not applicable"));
            default -> throw new IllegalArgumentException("Stage must be 1, 2, or 3");
        };
        List<TableSummary> tables = switch (stage) {
            case 1, 2 -> List.of(new TableSummary("Table 1", "Occupied", "Bob"),
                    new TableSummary("Table 2", "Available", null));
            case 3 -> List.of(new TableSummary("Table 1", "Occupied", "Bob"),
                    new TableSummary("Table 2", "Occupied", "Mia"));
            default -> throw new IllegalArgumentException("Stage must be 1, 2, or 3");
        };
        List<OrderSummary> orders = switch (stage) {
            case 1 -> List.of();
            case 2 -> List.of(new OrderSummary("order-bob", "Bob", "Burger", "READY", false));
            case 3 -> List.of(new OrderSummary("order-bob", "Bob", "ComboMeal", "READY", true));
            default -> throw new IllegalArgumentException("Stage must be 1, 2, or 3");
        };
        List<WaiterChoice> waiterChoices = switch (stage) {
            case 1 -> List.of(
                    new WaiterChoice(WaiterTask.SEAT, "mia", "table-2", null, "Seat Mia at Table 2"),
                    new WaiterChoice(WaiterTask.TAKE_ORDER, "bob", null, null, "Take Bob's order"),
                    new WaiterChoice(WaiterTask.WAIT, null, null, null, "Wait"));
            case 2 -> List.of(
                    new WaiterChoice(WaiterTask.SEAT, "mia", "table-2", null, "Seat Mia at Table 2"),
                    new WaiterChoice(WaiterTask.SERVE, null, null, "order-bob", "Serve Bob's Burger"),
                    new WaiterChoice(WaiterTask.WAIT, null, null, null, "Wait"));
            case 3 -> List.of(
                    new WaiterChoice(WaiterTask.TAKE_ORDER, "mia", null, null, "Take Mia's order"),
                    new WaiterChoice(WaiterTask.SERVE, null, null, "order-bob", "Serve ComboMeal"),
                    new WaiterChoice(WaiterTask.WAIT, null, null, null, "Wait"));
            default -> throw new IllegalArgumentException("Stage must be 1, 2, or 3");
        };
        StageThreeStatus stageThree = stage == 3
                ? new StageThreeStatus("Seated Mia at Table 2", true, 0)
                : null;
        return new RestaurantUiState(stage, 4, TurnPhase.WAITER, 0.00, OptionalDouble.empty(),
                customers, tables, orders, waiterChoices, List.of(), false, stageThree, null, null);
    }

    /** Preview returned after a waiter takes Bob's order; the Chef can select it this turn. */
    public static RestaurantUiState previewAfterWaiter(int stage) {
        RestaurantUiState base = preview(stage);
        List<CustomerSummary> customers = base.customers.stream()
                .map(customer -> customer.id.equals("bob")
                        ? new CustomerSummary(customer.id, customer.name, customer.type, customer.location,
                                customer.satisfaction, "Pasta", "PREPARING · 2 units", customer.paymentStatus)
                        : customer)
                .toList();
        return new RestaurantUiState(stage, base.turn, TurnPhase.CHEF, base.revenue,
                base.averagePaidSatisfaction, customers, base.tables,
                List.of(new OrderSummary("order-bob-pasta", "Bob", "Pasta", "PREPARING · 2 units", false)),
                List.of(), List.of(new ChefChoice(ChefTask.ADVANCE_ORDER, "order-bob-pasta",
                        "Advance Bob's Pasta"), new ChefChoice(ChefTask.WAIT, null, "Wait")),
                false, base.stageThree, null, null);
    }

    public static RestaurantUiState previewRejectedChoice(int stage) {
        RestaurantUiState base = preview(stage);
        return new RestaurantUiState(stage, base.turn, base.phase, base.revenue, base.averagePaidSatisfaction,
                base.customers, base.tables, base.orders, base.waiterChoices, base.chefChoices,
                base.canResolveTurn, base.stageThree,
                new Feedback("That choice was rejected. Select an available task to try again.", true), null);
    }

    public static RestaurantUiState previewReadyToResolve(int stage) {
        RestaurantUiState base = previewAfterWaiter(stage);
        return new RestaurantUiState(stage, base.turn, TurnPhase.RESOLVE, base.revenue,
                base.averagePaidSatisfaction, base.customers, base.tables, base.orders,
                List.of(), List.of(), true, base.stageThree, null, null);
    }

    public static RestaurantUiState previewHappyHourActive() {
        RestaurantUiState base = preview(3);
        return new RestaurantUiState(3, base.turn, base.phase, base.revenue, base.averagePaidSatisfaction,
                base.customers, base.tables, base.orders, base.waiterChoices, base.chefChoices,
                base.canResolveTurn, new StageThreeStatus(base.stageThree.hostActivity, false, 2),
                base.feedback, base.result);
    }

    public static RestaurantUiState previewResult(int stage, boolean victory) {
        RestaurantUiState base = preview(stage);
        GameResult result = new GameResult(5, victory ? 1 : 3, 54.60,
                OptionalDouble.of(victory ? 72.4 : 48.2), 18, victory);
        return new RestaurantUiState(stage, 18, TurnPhase.COMPLETE, result.revenue,
                result.averagePaidSatisfaction, base.customers, base.tables, base.orders,
                List.of(), List.of(), false, base.stageThree, null, result);
    }

    private static void checkSatisfaction(OptionalDouble satisfaction) {
        if (satisfaction.isPresent()
                && (!Double.isFinite(satisfaction.getAsDouble())
                || satisfaction.getAsDouble() < 0 || satisfaction.getAsDouble() > 100)) {
            throw new IllegalArgumentException("Satisfaction must be between 0 and 100");
        }
    }

    public enum TurnPhase { WAITER, CHEF, RESOLVE, COMPLETE }

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
            if (satisfaction < 0 || satisfaction > 100) {
                throw new IllegalArgumentException("Satisfaction must be between 0 and 100");
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
            if (happyHourTurnsRemaining < 0 || happyHourTurnsRemaining > 2
                    || (happyHourTurnsRemaining > 0 && happyHourAvailable)) {
                throw new IllegalArgumentException("Invalid Happy Hour status");
            }
        }
    }

    public record Feedback(String message, boolean rejected) {
        public Feedback { requireText(message); }
    }

    public record GameResult(int servedCount, int departures, double revenue,
            OptionalDouble averagePaidSatisfaction, int turnsUsed, boolean victory) {
        public GameResult {
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
